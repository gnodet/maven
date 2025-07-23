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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.ArtifactFactory;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectBuilderException;
import org.apache.maven.api.services.ProjectBuilderRequest;
import org.apache.maven.api.services.ProjectBuilderResult;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.Sources;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.RequestTraceHelper;

/**
 * Default implementation of {@link ProjectBuilder} that builds {@link Project} objects
 * using only the new Maven API, without any dependencies on legacy MavenProject.
 *
 * This implementation creates clean, immutable Project instances using the new
 * DefaultProject class and leverages the ModelBuilder service for model processing.
 */
@Named
@Singleton
public class DefaultProjectBuilder implements ProjectBuilder {

    private final ModelBuilder modelBuilder;
    private final ArtifactFactory artifactFactory;

    @Inject
    public DefaultProjectBuilder(ModelBuilder modelBuilder, ArtifactFactory artifactFactory) {
        this.modelBuilder = Objects.requireNonNull(modelBuilder, "modelBuilder cannot be null");
        this.artifactFactory = Objects.requireNonNull(artifactFactory, "artifactFactory cannot be null");
    }

    @Nonnull
    @Override
    public ProjectBuilderResult build(ProjectBuilderRequest request)
            throws ProjectBuilderException, IllegalArgumentException {
        Objects.requireNonNull(request, "request cannot be null");
        InternalSession session = InternalSession.from(request.getSession());
        return session.request(request, this::doBuild);
    }

    protected ProjectBuilderResult doBuild(ProjectBuilderRequest request)
            throws ProjectBuilderException, IllegalArgumentException {
        RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(request.getSession(), request);
        try {
            // Build the model using ModelBuilder with recursive support
            ModelBuilderRequest modelRequest = createModelBuilderRequest(request);
            ModelBuilder.ModelBuilderSession session = modelBuilder.newSession();
            ModelBuilderResult modelResult = session.build(modelRequest);

            // Extract information from the model result
            Model effectiveModel = modelResult.getEffectiveModel();
            Path pomPath = extractPomPath(request, modelResult);
            Path basedir = extractBasedir(pomPath);

            // Create artifacts for the project
            List<ProducedArtifact> artifacts = createArtifacts(request.getSession(), effectiveModel);

            // Get active profiles from the model result
            List<Profile> activeProfiles = extractActiveProfiles(modelResult);

            // Resolve parent project if needed
            Project parent = resolveParentProject(request, modelResult, session);

            // Create the Project instance
            Project project = new DefaultProject(
                    (InternalMavenSession) InternalSession.from(request.getSession()),
                    effectiveModel,
                    basedir,
                    pomPath,
                    artifacts,
                    activeProfiles,
                    parent);

            // Convert model problems to builder problems
            Collection<BuilderProblem> problems =
                    convertProblems(modelResult.getProblemCollector().problems());

            return new DefaultProjectBuilderResult(request, project, pomPath, problems);

        } catch (ModelBuilderException e) {
            throw new ProjectBuilderException("Failed to build project: " + e.getMessage(), e);
        } finally {
            RequestTraceHelper.exit(trace);
        }
    }

    private ModelBuilderRequest createModelBuilderRequest(ProjectBuilderRequest request) {
        Session session = request.getSession();

        ModelBuilderRequest.ModelBuilderRequestBuilder builder = ModelBuilderRequest.builder()
                .session(session)
                .trace(request.getTrace())
                .locationTracking(true)
                .requestType(
                        request.isProcessPlugins()
                                ? ModelBuilderRequest.RequestType.BUILD_PROJECT
                                : ModelBuilderRequest.RequestType.BUILD_EFFECTIVE)
                .locationTracking(true)
                .recursive(request.isRecursive()); // Use recursive flag from ProjectBuilderRequest

        // Set source or path
        if (request.getPath().isPresent()) {
            builder.source(Sources.buildSource(request.getPath().get()));
        } else if (request.getSource().isPresent()) {
            // Convert Source to ModelSource if needed
            Source source = request.getSource().get();
            if (source instanceof ModelSource modelSource) {
                builder.source(modelSource);
            } else {
                // Create a ModelSource from the Source path
                Path sourcePath = source.getPath();
                if (sourcePath != null) {
                    builder.source(Sources.buildSource(sourcePath));
                } else {
                    throw new IllegalArgumentException("Source must have a path to be converted to ModelSource");
                }
            }
        } else {
            throw new IllegalArgumentException("Either path or source must be specified");
        }

        // Set repositories if specified
        if (request.getRepositories() != null) {
            builder.repositories(request.getRepositories());
        }

        // Map session-level properties and profiles
        mapSessionProperties(builder, session);

        return builder.build();
    }

    private void mapSessionProperties(ModelBuilderRequest.ModelBuilderRequestBuilder builder, Session session) {
        // Map profiles from session settings
        if (session.getSettings() != null && session.getSettings().getProfiles() != null) {
            builder.profiles(session.getSettings().getProfiles());
        }

        // Map active and inactive profile IDs from session
        if (session.getRequest() != null) {
            builder.activeProfileIds(session.getRequest().getActiveProfiles());
            builder.inactiveProfileIds(session.getRequest().getInactiveProfiles());
        }

        // Map system properties from session
        builder.systemProperties(session.getSystemProperties());

        // Map user properties from session
        builder.userProperties(session.getUserProperties());

        // Set repository merging strategy (default to POM_DOMINANT like legacy)
        builder.repositoryMerging(ModelBuilderRequest.RepositoryMerging.POM_DOMINANT);
    }

    private Path extractPomPath(ProjectBuilderRequest request, ModelBuilderResult modelResult) {
        if (request.getPath().isPresent()) {
            return request.getPath().get();
        } else {
            // For source-based builds, try to get path from model source
            return modelResult.getSource().getPath();
        }
    }

    private Path extractBasedir(Path pomPath) {
        if (pomPath != null) {
            return pomPath.getParent() != null ? pomPath.getParent() : Path.of(".");
        }
        return Path.of(".");
    }

    private List<ProducedArtifact> createArtifacts(Session session, Model model) {
        List<ProducedArtifact> artifacts = new ArrayList<>();

        // Always create POM artifact
        ProducedArtifact pomArtifact = artifactFactory.createProduced(
                session, model.getGroupId(), model.getArtifactId(), model.getVersion(), "pom");
        artifacts.add(pomArtifact);

        // Create main artifact if packaging is not POM
        if (!"pom".equals(model.getPackaging())) {
            String extension = model.getPackaging(); // Default extension is packaging
            ProducedArtifact mainArtifact = artifactFactory.createProduced(
                    session, model.getGroupId(), model.getArtifactId(), model.getVersion(), extension);
            artifacts.add(mainArtifact);
        }

        return Collections.unmodifiableList(artifacts);
    }

    private List<Profile> extractActiveProfiles(ModelBuilderResult modelResult) {
        // For now, return empty list - active profiles would need to be extracted
        // from the ModelBuilderResult or tracked during model building
        // TODO: Implement proper active profile extraction from ModelBuilderResult
        return Collections.emptyList();
    }

    private Project resolveParentProject(
            ProjectBuilderRequest request, ModelBuilderResult modelResult, ModelBuilder.ModelBuilderSession session) {

        Model parentModel = modelResult.getParentModel();
        if (parentModel == null) {
            return null;
        }

        try {
            // Create artifacts for the parent project
            List<ProducedArtifact> parentArtifacts = createArtifacts(request.getSession(), parentModel);

            // Get active profiles for parent (simplified for now)
            List<Profile> parentActiveProfiles = Collections.emptyList();

            // Determine parent paths
            Path parentPomPath = parentModel.getPomFile();
            Path parentBasedir = parentPomPath != null ? parentPomPath.getParent() : null;

            // Recursively resolve parent's parent if needed
            Project grandParent = null;
            if (parentModel.getParent() != null) {
                // For now, we don't recursively resolve grandparents to avoid infinite recursion
                // This could be enhanced later with proper cycle detection
                grandParent = null;
            }

            // Create the parent Project instance
            return new DefaultProject(
                    (InternalMavenSession) InternalSession.from(request.getSession()),
                    parentModel,
                    parentBasedir,
                    parentPomPath,
                    parentArtifacts,
                    parentActiveProfiles,
                    grandParent);

        } catch (Exception e) {
            // If parent resolution fails, log and continue without parent
            // This matches the behavior of the legacy project builder
            return null;
        }
    }

    private Collection<BuilderProblem> convertProblems(java.util.stream.Stream<? extends ModelProblem> modelProblems) {
        // ModelProblem already extends BuilderProblem, so no conversion needed
        return modelProblems.map(BuilderProblem.class::cast).toList();
    }

    /**
     * Default implementation of ProjectBuilderResult
     */
    private static class DefaultProjectBuilderResult implements ProjectBuilderResult {
        private final ProjectBuilderRequest request;
        private final Project project;
        private final Path pomPath;
        private final Collection<BuilderProblem> problems;

        DefaultProjectBuilderResult(
                ProjectBuilderRequest request, Project project, Path pomPath, Collection<BuilderProblem> problems) {
            this.request = request;
            this.project = project;
            this.pomPath = pomPath;
            this.problems = problems;
        }

        @Override
        public ProjectBuilderRequest getRequest() {
            return request;
        }

        @Nonnull
        @Override
        public String getProjectId() {
            return project != null ? project.getId() : "unknown";
        }

        @Nonnull
        @Override
        public Optional<Path> getPomFile() {
            return Optional.ofNullable(pomPath);
        }

        @Nonnull
        @Override
        public Optional<Project> getProject() {
            return Optional.ofNullable(project);
        }

        @Nonnull
        @Override
        public Collection<BuilderProblem> getProblems() {
            return problems;
        }

        @Nonnull
        @Override
        public Optional<DependencyResolverResult> getDependencyResolverResult() {
            // For now, return empty - dependency resolution would be handled separately
            // TODO: Implement dependency resolution integration
            return Optional.empty();
        }
    }
}
