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
import java.time.Instant;
import java.util.List;

import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.exec.ActivationSettings;
import org.apache.maven.api.exec.MavenRequest;
import org.apache.maven.api.exec.ProfileActivation;
import org.apache.maven.api.exec.ProjectActivation;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.impl.InternalSession;

/**
 * Default implementation of {@link MavenRequest} that wraps a {@link MavenExecutionRequest}.
 *
 * @since 4.0.0
 */
public class DefaultMavenRequest implements MavenRequest {

    private final MavenExecutionRequest delegate;

    public DefaultMavenRequest(MavenExecutionRequest delegate) {
        this.delegate = delegate;
    }

    @Override
    public Session getSession() {
        return InternalSession.from(delegate.getProjectBuildingRequest().getRepositorySession());
    }

    @Override
    @Nullable
    public Path getPom() {
        return delegate.getPom() != null ? delegate.getPom().toPath() : null;
    }

    @Override
    @Nonnull
    public Path getRootDirectory() {
        return delegate.getRootDirectory();
    }

    @Override
    @Nonnull
    public Path getTopDirectory() {
        return delegate.getTopDirectory();
    }

    @Override
    @Nullable
    public String getBaseDirectory() {
        return delegate.getBaseDirectory();
    }

    @Override
    @Nonnull
    public List<String> getGoals() {
        return delegate.getGoals();
    }

    @Override
    @Nonnull
    public List<String> getSelectedProjects() {
        return delegate.getSelectedProjects();
    }

    @Override
    @Nonnull
    public List<String> getExcludedProjects() {
        return delegate.getExcludedProjects();
    }

    @Override
    @Nonnull
    public ProjectActivation getProjectActivation() {
        return new ProjectActivation(delegate.getProjectActivation().getActivations().stream()
                .map(projectActivationSettings -> new ProjectActivation.ProjectActivationSettings(
                        projectActivationSettings.selector(),
                        new ActivationSettings(
                                projectActivationSettings.activationSettings().active(),
                                projectActivationSettings.activationSettings().optional(),
                                projectActivationSettings.activationSettings().recurse())))
                .toList());
    }

    @Override
    public ProfileActivation getProfileActivation() {
        // return delegate.getProfileActivation();
        return null;
    }

    @Override
    public boolean isRecursive() {
        return delegate.isRecursive();
    }

    @Override
    public boolean isShowErrors() {
        return delegate.isShowErrors();
    }

    @Override
    @Nonnull
    public Instant getStartTime() {
        return delegate.getStartInstant();
    }

    @Override
    public boolean isProjectPresent() {
        return delegate.isProjectPresent();
    }

    /**
     * Gets the underlying MavenExecutionRequest.
     *
     * @return the wrapped MavenExecutionRequest
     */
    public MavenExecutionRequest getDelegate() {
        return delegate;
    }
}
