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

import javax.inject.Inject;
import javax.inject.Named;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Language;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.SourceRoot;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.SessionScoped;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.impl.DefaultSourceRoot;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.PropertiesAsMap;
import org.eclipse.sisu.Typed;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.internal.impl.CoreUtils.map;

@Named
@Typed
@SessionScoped
public class DefaultProjectManager implements ProjectManager {

    private final InternalMavenSession session;
    private final ArtifactManager artifactManager;

    // Store mutable state per project
    private final Map<String, List<org.apache.maven.artifact.Artifact>> attachedArtifacts = new ConcurrentHashMap<>();
    private final Map<String, Properties> projectProperties = new ConcurrentHashMap<>();
    private final Map<String, List<SourceRoot>> sourceRoots = new ConcurrentHashMap<>();
    private final Map<String, List<RemoteRepository>> remoteProjectRepositories = new ConcurrentHashMap<>();
    private final Map<String, List<RemoteRepository>> remotePluginRepositories = new ConcurrentHashMap<>();

    @Inject
    public DefaultProjectManager(InternalMavenSession session, ArtifactManager artifactManager) {
        this.session = session;
        this.artifactManager = artifactManager;
    }

    @Nonnull
    @Override
    public Optional<Path> getPath(@Nonnull Project project) {
        requireNonNull(project, "project" + " cannot be null");
        Optional<ProducedArtifact> mainArtifact = project.getMainArtifact();
        return mainArtifact.flatMap(artifactManager::getPath);
    }

    @Nonnull
    @Override
    public Collection<ProducedArtifact> getAttachedArtifacts(@Nonnull Project project) {
        requireNonNull(project, "project" + " cannot be null");
        String projectId = project.getId();
        List<org.apache.maven.artifact.Artifact> artifacts =
                attachedArtifacts.getOrDefault(projectId, Collections.emptyList());
        Collection<ProducedArtifact> attached = map(
                artifacts, a -> getSession(project).getArtifact(ProducedArtifact.class, RepositoryUtils.toArtifact(a)));
        return Collections.unmodifiableCollection(attached);
    }

    @Override
    @Nonnull
    public Collection<ProducedArtifact> getAllArtifacts(@Nonnull Project project) {
        requireNonNull(project, "project cannot be null");
        ArrayList<ProducedArtifact> result = new ArrayList<>(2);
        result.addAll(project.getArtifacts());
        result.addAll(getAttachedArtifacts(project));
        return Collections.unmodifiableCollection(result);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void attachArtifact(@Nonnull Project project, @Nonnull ProducedArtifact artifact, @Nonnull Path path) {
        requireNonNull(project, "project cannot be null");
        requireNonNull(artifact, "artifact cannot be null");
        requireNonNull(path, "path cannot be null");
        if (artifact.getGroupId().isEmpty()
                || artifact.getArtifactId().isEmpty()
                || artifact.getBaseVersion().toString().isEmpty()) {
            artifact = session.createProducedArtifact(
                    artifact.getGroupId().isEmpty() ? project.getGroupId() : artifact.getGroupId(),
                    artifact.getArtifactId().isEmpty() ? project.getArtifactId() : artifact.getArtifactId(),
                    artifact.getBaseVersion().toString().isEmpty()
                            ? session.parseVersion(project.getVersion()).toString()
                            : artifact.getBaseVersion().toString(),
                    artifact.getClassifier(),
                    artifact.getExtension(),
                    null);
        }
        if (!Objects.equals(project.getGroupId(), artifact.getGroupId())
                || !Objects.equals(project.getArtifactId(), artifact.getArtifactId())
                || !Objects.equals(
                        project.getVersion(), artifact.getBaseVersion().toString())) {
            throw new IllegalArgumentException(
                    "The produced artifact must have the same groupId/artifactId/version than the project it is attached to. Expecting "
                            + project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion()
                            + " but received " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
                            + artifact.getBaseVersion());
        }
        // Store the attached artifact in our internal storage
        String projectId = project.getId();
        org.apache.maven.artifact.Artifact mavenArtifact =
                RepositoryUtils.toArtifact(getSession(project).toArtifact(artifact));
        attachedArtifacts.computeIfAbsent(projectId, k -> new ArrayList<>()).add(mavenArtifact);
        artifactManager.setPath(artifact, path);
    }

    @Nonnull
    @Override
    public Collection<SourceRoot> getSourceRoots(@Nonnull Project project) {
        requireNonNull(project, "project" + " cannot be null");
        String projectId = project.getId();
        List<SourceRoot> roots = sourceRoots.computeIfAbsent(projectId, k -> {
            // Initialize with model source roots
            List<SourceRoot> initialRoots = new ArrayList<>();
            // Add source roots from the model
            project.getModel().getBuild().getSources().forEach(source -> {
                initialRoots.add(new DefaultSourceRoot(getSession(project), project.getBasedir(), source));
            });
            return initialRoots;
        });
        return Collections.unmodifiableCollection(roots);
    }

    @Nonnull
    @Override
    public Stream<SourceRoot> getEnabledSourceRoots(@Nonnull Project project, ProjectScope scope, Language language) {
        return getSourceRoots(project).stream()
                .filter(SourceRoot::enabled)
                .filter(source -> scope.equals(source.scope()) && language.equals(source.language()));
    }

    @Override
    public void addSourceRoot(@Nonnull Project project, @Nonnull SourceRoot source) {
        requireNonNull(project, "project" + " cannot be null");
        requireNonNull(source, "source" + " cannot be null");
        String projectId = project.getId();
        sourceRoots.computeIfAbsent(projectId, k -> new ArrayList<>()).add(source);
    }

    @Override
    public void addSourceRoot(
            @Nonnull Project project,
            @Nonnull ProjectScope scope,
            @Nonnull Language language,
            @Nonnull Path directory) {
        requireNonNull(project, "project" + " cannot be null");
        requireNonNull(scope, "scope" + " cannot be null");
        requireNonNull(language, "language" + " cannot be null");
        requireNonNull(directory, "directory" + " cannot be null");

        Path resolvedDirectory = project.getBasedir().resolve(directory).normalize();
        SourceRoot sourceRoot = new DefaultSourceRoot(scope, language, resolvedDirectory);
        addSourceRoot(project, sourceRoot);
    }

    @Override
    @Nonnull
    public List<RemoteRepository> getRemoteProjectRepositories(@Nonnull Project project) {
        requireNonNull(project, "project cannot be null");
        String projectId = project.getId();
        return Collections.unmodifiableList(remoteProjectRepositories.computeIfAbsent(projectId, k -> {
            // Initialize with model repositories converted to RemoteRepository
            List<RemoteRepository> repos = new ArrayList<>();
            project.getModel().getRepositories().forEach(repo -> {
                repos.add(getSession(project)
                        .getService(org.apache.maven.api.services.RepositoryFactory.class)
                        .createRemote(repo));
            });
            return repos;
        }));
    }

    @Override
    @Nonnull
    public List<RemoteRepository> getRemotePluginRepositories(@Nonnull Project project) {
        requireNonNull(project, "project cannot be null");
        String projectId = project.getId();
        return Collections.unmodifiableList(remotePluginRepositories.computeIfAbsent(projectId, k -> {
            // Initialize with model plugin repositories converted to RemoteRepository
            List<RemoteRepository> repos = new ArrayList<>();
            project.getModel().getPluginRepositories().forEach(repo -> {
                repos.add(getSession(project)
                        .getService(org.apache.maven.api.services.RepositoryFactory.class)
                        .createRemote(repo));
            });
            return repos;
        }));
    }

    @Override
    public void setProperty(@Nonnull Project project, @Nonnull String key, String value) {
        String projectId = project.getId();
        Properties properties = projectProperties.computeIfAbsent(projectId, k -> {
            // Initialize with model properties
            Properties props = new Properties();
            props.putAll(project.getModel().getProperties());
            return props;
        });
        if (value == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value);
        }
    }

    @Override
    @Nonnull
    public Map<String, String> getProperties(@Nonnull Project project) {
        String projectId = project.getId();
        Properties properties = projectProperties.computeIfAbsent(projectId, k -> {
            // Initialize with model properties
            Properties props = new Properties();
            props.putAll(project.getModel().getProperties());
            return props;
        });
        return Collections.unmodifiableMap(new PropertiesAsMap(properties));
    }

    @Override
    @Nonnull
    public Optional<Project> getExecutionProject(@Nonnull Project project) {
        // For now, return empty until we implement execution project support
        // TODO: Implement execution project support in the new architecture
        return Optional.empty();
    }

    // Helper methods for setting repositories during project building
    public void setRemoteProjectRepositories(@Nonnull Project project, @Nonnull List<RemoteRepository> repositories) {
        requireNonNull(project, "project cannot be null");
        requireNonNull(repositories, "repositories cannot be null");
        String projectId = project.getId();
        remoteProjectRepositories.put(projectId, new ArrayList<>(repositories));
    }

    public void setRemotePluginRepositories(@Nonnull Project project, @Nonnull List<RemoteRepository> repositories) {
        requireNonNull(project, "project cannot be null");
        requireNonNull(repositories, "repositories cannot be null");
        String projectId = project.getId();
        remotePluginRepositories.put(projectId, new ArrayList<>(repositories));
    }

    private static InternalSession getSession(Project project) {
        if (project instanceof DefaultProject) {
            return ((DefaultProject) project).getSession();
        } else if (project instanceof DefaultLegacyProject) {
            return ((DefaultLegacyProject) project).getSession();
        }
        throw new IllegalArgumentException("Unsupported project type: " + project.getClass());
    }
}
