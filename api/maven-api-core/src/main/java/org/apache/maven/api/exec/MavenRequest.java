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
package org.apache.maven.api.exec;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Represents a Maven execution request in the new API.
 * This interface provides access to the configuration and parameters
 * needed for Maven project building and execution.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface MavenRequest {

    @Nonnull
    Session getSession();

    /**
     * Gets the POM file for the request.
     *
     * @return the POM file path, or null if not set
     */
    @Nullable
    Path getPom();

    /**
     * Gets the root directory of the top project.
     *
     * @return the root directory path
     * @throws IllegalStateException if the root directory could not be found
     */
    @Nonnull
    Path getRootDirectory();

    /**
     * Gets the top directory of the request.
     *
     * @return the top directory path
     */
    @Nonnull
    Path getTopDirectory();

    /**
     * Gets the base directory for the request.
     *
     * @return the base directory path, or null if not set
     */
    @Nullable
    String getBaseDirectory();

    /**
     * Gets the goals for this request.
     *
     * @return the list of goals
     */
    @Nonnull
    List<String> getGoals();

    /**
     * Gets the selected projects for this request.
     *
     * @return the list of selected projects
     */
    @Nonnull
    List<String> getSelectedProjects();

    /**
     * Gets the excluded projects for this request.
     *
     * @return the list of excluded projects
     */
    @Nonnull
    List<String> getExcludedProjects();

    /**
     * Gets the project activation map.
     *
     * @return the project activation map
     */
    @Nonnull
    ProjectActivation getProjectActivation();

    /**
     * Gets the profile activation map.
     *
     * @return the profile activation map
     */
    @Nonnull
    ProfileActivation getProfileActivation();

    /**
     * Checks if the build should be recursive.
     *
     * @return true if recursive, false otherwise
     */
    boolean isRecursive();

    /**
     * Checks if errors should be shown.
     *
     * @return true if errors should be shown, false otherwise
     */
    boolean isShowErrors();

    /**
     * Gets the start time of the request.
     *
     * @return the start time
     */
    @Nonnull
    Instant getStartTime();

    /**
     * Checks if the project is present.
     *
     * @return true if project is present, false otherwise
     */
    boolean isProjectPresent();
}
