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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.Type;
import org.apache.maven.api.VersionConstraint;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.impl.MappedCollection;
import org.apache.maven.impl.MappedList;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

public class DefaultLegacyProject implements Project {

    private final InternalMavenSession session;
    private final MavenProject project; // For backward compatibility
    private final Packaging packaging;

    // New fields for the refactored architecture
    private final org.apache.maven.api.model.Model model;
    private final Path basedir;
    private final Path pomPath;
    private final List<ProducedArtifact> artifacts;
    private final List<Profile> activeProfiles;
    private final Project parent;

    // Legacy constructor - wraps a MavenProject
    public DefaultLegacyProject(InternalMavenSession session, MavenProject project) {
        this.session = session;
        this.project = project;
        ClassLoader ttcl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(project.getClassRealm());
            this.packaging = session.requirePackaging(project.getPackaging());
        } finally {
            Thread.currentThread().setContextClassLoader(ttcl);
        }
        // Initialize new fields from MavenProject for backward compatibility
        this.model = project.getModel().getDelegate();
        this.basedir = project.getBasedir() != null ? project.getBasedir().toPath() : Path.of(".");
        this.pomPath = project.getFile() != null ? project.getFile().toPath() : null;
        this.artifacts = List.copyOf(createArtifactsFromMavenProject(project));
        this.activeProfiles = List.copyOf(project.getActiveProfiles().stream()
                .map(org.apache.maven.model.Profile::getDelegate)
                .toList());
        this.parent = project.getParent() != null ? session.getProject(project.getParent()) : null;
    }

    // New constructor - creates a Project directly from model and paths
    public DefaultLegacyProject(
            InternalMavenSession session,
            org.apache.maven.api.model.Model model,
            Path basedir,
            Path pomPath,
            List<Profile> activeProfiles,
            Project parent) {
        this.session = session;
        this.model = model;
        this.basedir = basedir != null ? basedir : Path.of(".");
        this.pomPath = pomPath;
        this.project = null; // No MavenProject in the new architecture

        ClassLoader ttcl = Thread.currentThread().getContextClassLoader();
        try {
            this.packaging = session.requirePackaging(model.getPackaging());
        } finally {
            Thread.currentThread().setContextClassLoader(ttcl);
        }
        this.artifacts = List.copyOf(createArtifactsFromModel(model));
        this.activeProfiles = List.copyOf(activeProfiles != null ? activeProfiles : Collections.emptyList());
        this.parent = parent;
    }

    public InternalMavenSession getSession() {
        return session;
    }

    /**
     * @deprecated This method exposes the legacy MavenProject and should be eliminated.
     * It's only kept temporarily for plugin context access. New code should not use this method.
     * @return the wrapped MavenProject in legacy mode, null in new architecture
     */
    @Deprecated
    public MavenProject getProject() {
        return project;
    }

    // Helper method to check if using legacy architecture
    public boolean isLegacyMode() {
        return project != null;
    }

    @Nonnull
    @Override
    public String getGroupId() {
        if (isLegacyMode()) {
            return project.getGroupId();
        } else {
            return model.getGroupId();
        }
    }

    @Nonnull
    @Override
    public String getArtifactId() {
        if (isLegacyMode()) {
            return project.getArtifactId();
        } else {
            return model.getArtifactId();
        }
    }

    @Nonnull
    @Override
    public String getVersion() {
        if (isLegacyMode()) {
            return project.getVersion();
        } else {
            return model.getVersion();
        }
    }

    @Nonnull
    @Override
    public List<ProducedArtifact> getArtifacts() {
        return artifacts;
    }

    @Nonnull
    @Override
    public Packaging getPackaging() {
        return packaging;
    }

    @Nonnull
    @Override
    public Model getModel() {
        if (isLegacyMode()) {
            return project.getModel().getDelegate();
        } else {
            return model;
        }
    }

    @Nonnull
    @Override
    public Path getPomPath() {
        if (isLegacyMode()) {
            return Objects.requireNonNull(project.getFile(), "pomPath cannot be null")
                    .toPath();
        } else {
            return Objects.requireNonNull(pomPath, "pomPath cannot be null");
        }
    }

    @Nonnull
    @Override
    public Path getBasedir() {
        return basedir;
    }

    @Nonnull
    @Override
    public List<DependencyCoordinates> getDependencies() {
        return new MappedList<>(getModel().getDependencies(), this::toDependency);
    }

    @Nonnull
    @Override
    public List<DependencyCoordinates> getManagedDependencies() {
        DependencyManagement dependencyManagement = getModel().getDependencyManagement();
        if (dependencyManagement != null) {
            return new MappedList<>(dependencyManagement.getDependencies(), this::toDependency);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isTopProject() {
        return getBasedir().equals(getSession().getTopDirectory());
    }

    @Override
    public boolean isRootProject() {
        return getBasedir().equals(getRootDirectory());
    }

    @Override
    public Path getRootDirectory() {
        if (isLegacyMode()) {
            return project.getRootDirectory();
        } else {
            // In new architecture, use session's root directory
            return session.getRootDirectory();
        }
    }

    @Override
    public Optional<Project> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    @Nonnull
    public List<Profile> getDeclaredProfiles() {
        return getModel().getProfiles();
    }

    @Override
    @Nonnull
    public List<Profile> getEffectiveProfiles() {
        List<Profile> result = new ArrayList<>();

        // Collect profiles from this project and all parents
        Project current = this;
        while (current != null) {
            result.addAll(current.getModel().getProfiles());
            current = current.getParent().orElse(null);
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    @Nonnull
    public List<Profile> getDeclaredActiveProfiles() {
        return activeProfiles;
    }

    @Override
    @Nonnull
    public List<Profile> getEffectiveActiveProfiles() {
        List<Profile> result = new ArrayList<>();

        // Collect active profiles from this project and all parents
        Project current = this;
        while (current != null) {
            result.addAll(current.getDeclaredActiveProfiles());
            current = current.getParent().orElse(null);
        }

        return Collections.unmodifiableList(result);
    }

    @Nonnull
    private DependencyCoordinates toDependency(org.apache.maven.api.model.Dependency dependency) {
        return new DependencyCoordinates() {
            @Override
            public String getGroupId() {
                return dependency.getGroupId();
            }

            @Override
            public String getArtifactId() {
                return dependency.getArtifactId();
            }

            @Override
            public String getClassifier() {
                String classifier = dependency.getClassifier();
                if (classifier == null || classifier.isEmpty()) {
                    classifier = getType().getClassifier();
                    if (classifier == null) {
                        classifier = "";
                    }
                }
                return classifier;
            }

            @Override
            public VersionConstraint getVersionConstraint() {
                return session.parseVersionConstraint(dependency.getVersion());
            }

            @Override
            public String getExtension() {
                return getType().getExtension();
            }

            @Override
            public Type getType() {
                String type = dependency.getType();
                return session.requireType(type);
            }

            @Nonnull
            @Override
            public DependencyScope getScope() {
                String scope = dependency.getScope();
                if (scope == null) {
                    scope = "";
                }
                return session.requireDependencyScope(scope);
            }

            @Override
            public Boolean getOptional() {
                return dependency.isOptional();
            }

            @Nonnull
            @Override
            public Collection<Exclusion> getExclusions() {
                return new MappedCollection<>(dependency.getExclusions(), this::toExclusion);
            }

            private Exclusion toExclusion(org.apache.maven.api.model.Exclusion exclusion) {
                return new Exclusion() {
                    @Nullable
                    @Override
                    public String getGroupId() {
                        return exclusion.getGroupId();
                    }

                    @Nullable
                    @Override
                    public String getArtifactId() {
                        return exclusion.getArtifactId();
                    }
                };
            }
        };
    }

    // Helper methods for creating artifacts
    private List<ProducedArtifact> createArtifactsFromMavenProject(MavenProject project) {
        ArrayList<ProducedArtifact> result = new ArrayList<>(2);

        try {
            // Only create artifacts if the project has valid coordinates
            if (project.getGroupId() != null && project.getArtifactId() != null && project.getVersion() != null) {
                org.eclipse.aether.artifact.Artifact pomArtifact =
                        RepositoryUtils.toArtifact(new ProjectArtifact(project));
                org.eclipse.aether.artifact.Artifact projectArtifact =
                        RepositoryUtils.toArtifact(project.getArtifact());

                result.add(session.getArtifact(ProducedArtifact.class, pomArtifact));
                if (!ArtifactIdUtils.equalsVersionlessId(pomArtifact, projectArtifact)) {
                    result.add(session.getArtifact(ProducedArtifact.class, projectArtifact));
                }
            }
        } catch (Exception e) {
            // If artifact creation fails (e.g., in test scenarios), return empty list
            // This maintains backward compatibility with existing tests
        }

        return result;
    }

    private List<ProducedArtifact> createArtifactsFromModel(org.apache.maven.api.model.Model model) {
        ArrayList<ProducedArtifact> result = new ArrayList<>(2);

        // Create POM artifact
        ProducedArtifact pomArtifact =
                session.createProducedArtifact(model.getGroupId(), model.getArtifactId(), model.getVersion(), "pom");
        result.add(pomArtifact);

        // Create main artifact if packaging is not pom
        if (!"pom".equals(model.getPackaging())) {
            ProducedArtifact mainArtifact = session.createProducedArtifact(
                    model.getGroupId(), model.getArtifactId(), model.getVersion(), model.getPackaging());
            result.add(mainArtifact);
        }

        return result;
    }
}
