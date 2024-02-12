/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.internal.impl;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.*;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.*;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;

import static org.apache.maven.internal.impl.Utils.cast;
import static org.apache.maven.internal.impl.Utils.map;
import static org.apache.maven.internal.impl.Utils.nonNull;

@Named
@Singleton
public class DefaultDependencyResolver implements DependencyResolver {

    @Override
    public List<Node> flatten(Session s, Node node, PathScope scope) throws DependencyResolverException {
        InternalSession session = InternalSession.from(s);
        DependencyNode root = cast(AbstractNode.class, node, "node").getDependencyNode();
        List<DependencyNode> dependencies = session.getRepositorySystem()
                .flattenDependencyNodes(session.getSession(), root, getScopeDependencyFilter(scope));
        dependencies.remove(root);
        return map(dependencies, session::getNode);
    }

    private static DependencyFilter getScopeDependencyFilter(PathScope scope) {
        Set<String> scopes =
                scope.dependencyScopes().stream().map(DependencyScope::id).collect(Collectors.toSet());
        return (n, p) -> {
            org.eclipse.aether.graph.Dependency d = n.getDependency();
            return d == null || scopes.contains(d.getScope());
        };
    }

    /**
     * Collects, flattens and resolves the dependencies.
     *
     * @param request the request to resolve
     * @return the result of the resolution
     */
    @Override
    public DependencyResolverResult resolve(DependencyResolverRequest request)
            throws DependencyCollectorException, DependencyResolverException, ArtifactResolverException {
        nonNull(request, "request");
        InternalSession session = InternalSession.from(request.getSession());
        Predicate<PathType> filter = request.getPathTypeFilter();
        PathModularizationCache cache = new PathModularizationCache(); // TODO: should be project-wide cache.
        if (request.getProject().isPresent()) {
            DependencyResolutionResult resolved = resolveDependencies(
                    request.getSession(), request.getProject().get(), request.getPathScope());

            Map<org.eclipse.aether.graph.Dependency, org.eclipse.aether.graph.DependencyNode> nodes = stream(
                            resolved.getDependencyGraph())
                    .filter(n -> n.getDependency() != null)
                    .collect(Collectors.toMap(DependencyNode::getDependency, n -> n));

            Node root = session.getNode(resolved.getDependencyGraph());
            List<org.eclipse.aether.graph.Dependency> dependencies = resolved.getResolvedDependencies();
            DefaultDependencyResolverResult result =
                    new DefaultDependencyResolverResult(resolved.getCollectionErrors(), root, dependencies.size());
            for (org.eclipse.aether.graph.Dependency dep : dependencies) {
                Node node = session.getNode(nodes.get(dep));
                Path path = dep.getArtifact().getFile().toPath();
                try {
                    result.addDependency(node, session.getDependency(dep), filter, path, cache);
                } catch (IOException e) {
                    throw cannotReadModuleInfo(path, e);
                }
            }
            return result;
        }

        DependencyCollectorResult collectorResult =
                session.getService(DependencyCollector.class).collect(request);
        List<Node> nodes = flatten(session, collectorResult.getRoot(), request.getPathScope());
        List<ArtifactCoordinate> coordinates = nodes.stream()
                .map(Node::getDependency)
                .filter(Objects::nonNull)
                .map(Artifact::toCoordinate)
                .collect(Collectors.toList());
        Map<Artifact, Path> artifacts = session.resolveArtifacts(coordinates);
        DefaultDependencyResolverResult result = new DefaultDependencyResolverResult(
                collectorResult.getExceptions(), collectorResult.getRoot(), nodes.size());
        for (Node node : nodes) {
            Dependency d = node.getDependency();
            Path path = (d != null) ? artifacts.get(d) : null;
            try {
                result.addDependency(node, d, filter, path, cache);
            } catch (IOException e) {
                throw cannotReadModuleInfo(path, e);
            }
        }
        return result;
    }

    private DependencyResolutionResult resolveDependencies(Session session, Project project, PathScope scope) {
        Collection<String> toResolve = toScopes(scope);
        try {
            LifecycleDependencyResolver lifecycleDependencyResolver =
                    session.getService(Lookup.class).lookup(LifecycleDependencyResolver.class);
            return lifecycleDependencyResolver.getProjectDependencyResolutionResult(
                    getMavenProject(project),
                    toResolve,
                    toResolve,
                    InternalSession.from(session).getMavenSession(),
                    false,
                    Collections.emptySet());
        } catch (LifecycleExecutionException e) {
            throw new DependencyResolverException("Unable to resolve project dependencies", e);
        }
    }

    private static Stream<DependencyNode> stream(DependencyNode node) {
        return Stream.concat(Stream.of(node), node.getChildren().stream().flatMap(DefaultDependencyResolver::stream));
    }

    private static MavenProject getMavenProject(Project project) {
        return ((DefaultProject) project).getProject();
    }

    private static Collection<String> toScopes(PathScope scope) {
        return map(scope.dependencyScopes(), DependencyScope::id);
    }

    private static DependencyResolverException cannotReadModuleInfo(Path path, IOException cause) {
        return new DependencyResolverException("Cannot read module information of " + path, cause);
    }
}
