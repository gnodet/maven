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
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.api.Project;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectBuilderException;
import org.apache.maven.api.services.ProjectBuilderRequest;
import org.apache.maven.api.services.ProjectBuilderResult;
import org.apache.maven.api.services.Source;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.impl.DefaultDependencyResolverResult;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.MappedCollection;
import org.apache.maven.impl.RequestTraceHelper;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;

@Named
@Singleton
public class DefaultProjectBuilder implements ProjectBuilder {

    private final org.apache.maven.project.ProjectBuilder builder;

    @Inject
    public DefaultProjectBuilder(org.apache.maven.project.ProjectBuilder builder) {
        this.builder = builder;
    }

    @SuppressWarnings("MethodLength")
    @Nonnull
    @Override
    public ProjectBuilderResult build(ProjectBuilderRequest request)
            throws ProjectBuilderException, IllegalArgumentException {
        InternalSession session = InternalSession.from(request.getSession());
        return session.request(request, this::doBuild);
    }

    protected ProjectBuilderResult doBuild(ProjectBuilderRequest request)
            throws ProjectBuilderException, IllegalArgumentException {
        InternalMavenSession session = InternalMavenSession.from(request.getSession());
        RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(request.getSession(), request);
        try {
            List<ArtifactRepository> repositories = session.toArtifactRepositories(
                    request.getRepositories() != null ? request.getRepositories() : session.getRemoteRepositories());
            ProjectBuildingRequest req = new DefaultProjectBuildingRequest()
                    .setRepositorySession(session.getSession())
                    .setRemoteRepositories(repositories)
                    .setPluginArtifactRepositories(repositories)
                    .setProcessPlugins(request.isProcessPlugins());
            ProjectBuildingResult res;
            if (request.getPath().isPresent()) {
                Path path = request.getPath().get();
                if (request.isRecursive()) {
                    // Use the multi-file method for recursive building
                    List<ProjectBuildingResult> results = builder.build(List.of(path.toFile()), true, req);
                    if (results.isEmpty()) {
                        throw new ProjectBuildingException("No projects found", "No projects found", (Throwable) null);
                    }
                    res = results.get(0); // The main project is the first result
                } else {
                    res = builder.build(path.toFile(), req);
                }
            } else if (request.getSource().isPresent()) {
                Source source = request.getSource().get();
                ModelSource2 modelSource = new SourceWrapper(source);
                res = builder.build(modelSource, req);
            } else {
                throw new IllegalArgumentException("Invalid request");
            }
            return new ProjectBuilderResult() {
                @Override
                public ProjectBuilderRequest getRequest() {
                    return request;
                }

                @Nonnull
                @Override
                public String getProjectId() {
                    return res.getProjectId();
                }

                @Nonnull
                @Override
                public Optional<Path> getPomFile() {
                    return Optional.ofNullable(res.getPomFile()).map(File::toPath);
                }

                @Nonnull
                @Override
                public Optional<Project> getProject() {
                    return Optional.ofNullable(session.getProject(res.getProject()));
                }

                @Nonnull
                @Override
                public Collection<BuilderProblem> getProblems() {
                    return new MappedCollection<>(res.getProblems(), this::toProblem);
                }

                @Override
                public List<? extends ProjectBuilderResult> getChildren() {
                    return getChildResults(request, res, session);
                }

                private BuilderProblem toProblem(ModelProblem problem) {
                    return new org.apache.maven.api.services.ModelProblem() {
                        @Override
                        public String getModelId() {
                            return problem.getModelId();
                        }

                        @Override
                        public Version getVersion() {
                            return Version.valueOf(problem.getVersion().name());
                        }

                        @Override
                        public String getSource() {
                            return problem.getSource();
                        }

                        @Override
                        public int getLineNumber() {
                            return problem.getLineNumber();
                        }

                        @Override
                        public int getColumnNumber() {
                            return problem.getColumnNumber();
                        }

                        @Override
                        public String getLocation() {
                            StringBuilder buffer = new StringBuilder(256);

                            if (!getSource().isEmpty()) {
                                buffer.append(getSource());
                            }

                            if (getLineNumber() > 0) {
                                if (!buffer.isEmpty()) {
                                    buffer.append(", ");
                                }
                                buffer.append("line ").append(getLineNumber());
                            }

                            if (getColumnNumber() > 0) {
                                if (!buffer.isEmpty()) {
                                    buffer.append(", ");
                                }
                                buffer.append("column ").append(getColumnNumber());
                            }

                            return buffer.toString();
                        }

                        @Override
                        public Exception getException() {
                            return problem.getException();
                        }

                        @Override
                        public String getMessage() {
                            return problem.getMessage();
                        }

                        @Override
                        public Severity getSeverity() {
                            return Severity.valueOf(problem.getSeverity().name());
                        }
                    };
                }

                @Nonnull
                @Override
                public Optional<DependencyResolverResult> getDependencyResolverResult() {
                    return Optional.ofNullable(res.getDependencyResolutionResult())
                            .map(r -> new DefaultDependencyResolverResult(
                                    // TODO: this should not be null
                                    null, null, r.getCollectionErrors(), session.getNode(r.getDependencyGraph()), 0));
                }
            };
        } catch (ProjectBuildingException e) {
            throw new ProjectBuilderException(e.getProjectId(), "Unable to build project", e);
        } finally {
            RequestTraceHelper.exit(trace);
        }
    }

    private static class SourceWrapper implements ModelSource2 {
        private final Source source;

        SourceWrapper(Source source) {
            this.source = source;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return source.openStream();
        }

        @Override
        public String getLocation() {
            return source.getLocation();
        }

        @Override
        public ModelSource2 getRelatedSource(String relPath) {
            Source rel = source.resolve(relPath);
            return rel != null ? new SourceWrapper(rel) : null;
        }

        @Override
        public URI getLocationURI() {
            Path path = source.getPath();
            return path != null ? path.toUri() : URI.create(source.getLocation());
        }
    }

    /**
     * Creates child ProjectBuilderResult instances for recursive builds.
     */
    private List<? extends ProjectBuilderResult> getChildResults(
            ProjectBuilderRequest request, ProjectBuildingResult res, InternalSession session) {
        // If this was a recursive build and we have a path, we need to get all child projects
        if (request.isRecursive() && request.getPath().isPresent()) {
            try {
                // Re-run the recursive build to get all results
                Path path = request.getPath().get();
                List<ArtifactRepository> repositories = InternalMavenSession.from(session)
                        .toArtifactRepositories(
                                request.getRepositories() != null
                                        ? request.getRepositories()
                                        : InternalMavenSession.from(session).getRemoteRepositories());
                ProjectBuildingRequest req = new DefaultProjectBuildingRequest()
                        .setRepositorySession(InternalMavenSession.from(session).getSession())
                        .setRemoteRepositories(repositories)
                        .setPluginArtifactRepositories(repositories)
                        .setProcessPlugins(request.isProcessPlugins());

                List<ProjectBuildingResult> allResults = builder.build(List.of(path.toFile()), true, req);

                // Skip the first result (which is the main project) and return the rest as children
                return allResults.stream()
                        .skip(1) // Skip the main project
                        .map(childResult -> createChildResult(request, childResult, session))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                // If we can't get children, return empty list
                return List.of();
            }
        }
        return List.of();
    }

    /**
     * Creates a ProjectBuilderResult for a child project.
     */
    private ProjectBuilderResult createChildResult(
            ProjectBuilderRequest request, ProjectBuildingResult childResult, InternalSession session) {
        return new ProjectBuilderResult() {
            @Override
            public ProjectBuilderRequest getRequest() {
                return request;
            }

            @Nonnull
            @Override
            public String getProjectId() {
                return childResult.getProjectId();
            }

            @Nonnull
            @Override
            public Optional<Path> getPomFile() {
                return Optional.ofNullable(childResult.getPomFile()).map(File::toPath);
            }

            @Nonnull
            @Override
            public Optional<Project> getProject() {
                return Optional.ofNullable(InternalMavenSession.from(session).getProject(childResult.getProject()));
            }

            @Nonnull
            @Override
            public Collection<BuilderProblem> getProblems() {
                // For simplicity, return empty list for child projects
                // The main project will have the important problems
                return List.of();
            }

            @Override
            public List<? extends ProjectBuilderResult> getChildren() {
                return List.of(); // Nested children not supported in this simple implementation
            }

            @Nonnull
            @Override
            public Optional<DependencyResolverResult> getDependencyResolverResult() {
                return Optional.empty();
            }
        };
    }
}
