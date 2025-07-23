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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;

/**
 * Default implementation of {@link Project} for the new Maven API architecture.
 * This implementation is completely independent of the legacy MavenProject and
 * represents a clean, immutable project representation.
 */
public class DefaultProject implements Project {

    private final InternalMavenSession session;
    private final Packaging packaging;

    // Immutable project data
    private final Model model;
    private final Path basedir;
    private final Path pomPath;
    private final List<ProducedArtifact> artifacts;
    private final List<Profile> activeProfiles;
    private final Project parent;

    /**
     * Creates a new Project from model and computed data.
     *
     * @param session the Maven session
     * @param model the project model
     * @param basedir the project base directory
     * @param pomPath the path to the POM file
     * @param activeProfiles the active profiles for this project
     * @param parent the parent project, or null if this is a root project
     */
    public DefaultProject(
            InternalMavenSession session,
            Model model,
            Path basedir,
            Path pomPath,
            List<ProducedArtifact> artifacts,
            List<Profile> activeProfiles,
            Project parent) {
        this.session = Objects.requireNonNull(session, "session cannot be null");
        this.model = Objects.requireNonNull(model, "model cannot be null");
        this.basedir = basedir != null ? basedir : Path.of(".");
        this.pomPath = pomPath;
        this.artifacts = List.copyOf(artifacts != null ? artifacts : Collections.emptyList());
        this.activeProfiles = List.copyOf(activeProfiles != null ? activeProfiles : Collections.emptyList());
        this.parent = parent;

        // Initialize packaging
        ClassLoader ttcl = Thread.currentThread().getContextClassLoader();
        try {
            this.packaging = session.requirePackaging(model.getPackaging());
        } finally {
            Thread.currentThread().setContextClassLoader(ttcl);
        }
    }

    public InternalMavenSession getSession() {
        return session;
    }

    @Nonnull
    @Override
    public String getGroupId() {
        return model.getGroupId();
    }

    @Nonnull
    @Override
    public String getArtifactId() {
        return model.getArtifactId();
    }

    @Nonnull
    @Override
    public String getVersion() {
        return model.getVersion();
    }

    @Nonnull
    @Override
    public Packaging getPackaging() {
        return packaging;
    }

    @Nonnull
    @Override
    public List<ProducedArtifact> getArtifacts() {
        return artifacts;
    }

    @Nonnull
    @Override
    public List<DependencyCoordinates> getDependencies() {
        // For now, return empty list until we implement proper dependency conversion
        // TODO: Implement dependency conversion from model to DependencyCoordinates
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<DependencyCoordinates> getManagedDependencies() {
        DependencyManagement dependencyManagement = getModel().getDependencyManagement();
        if (dependencyManagement != null) {
            // For now, return empty list until we implement proper dependency conversion
            // TODO: Implement dependency conversion from model to DependencyCoordinates
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Model getModel() {
        return model;
    }

    @Nonnull
    @Override
    public Path getPomPath() {
        return Objects.requireNonNull(pomPath, "pomPath cannot be null");
    }

    @Nonnull
    @Override
    public Path getBasedir() {
        return basedir;
    }

    @Override
    public Path getRootDirectory() {
        return session.getRootDirectory();
    }

    @Override
    public Optional<Project> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    @Nonnull
    public List<Profile> getDeclaredProfiles() {
        return model.getProfiles();
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
    @Override
    public String getId() {
        return model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion();
    }

    @Override
    public boolean isTopProject() {
        return getBasedir().equals(session.getTopDirectory());
    }

    @Override
    public boolean isRootProject() {
        return getBasedir().equals(getRootDirectory());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultProject that = (DefaultProject) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "DefaultProject{" + "groupId='"
                + getGroupId() + '\'' + ", artifactId='"
                + getArtifactId() + '\'' + ", version='"
                + getVersion() + '\'' + ", packaging='"
                + getPackaging().id() + '\'' + '}';
    }
}
