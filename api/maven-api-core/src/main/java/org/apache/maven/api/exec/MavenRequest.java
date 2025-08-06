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

import java.util.stream.Stream;
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

    String REACTOR_MAKE_UPSTREAM = "make-upstream";

    String REACTOR_MAKE_DOWNSTREAM = "make-downstream";

    String REACTOR_MAKE_BOTH = "make-both";

    enum MakeBehavior {
        DEFAULT(""),
        UPSTREAM(REACTOR_MAKE_UPSTREAM),
        DOWNSTREAM(REACTOR_MAKE_DOWNSTREAM),
        BOTH(REACTOR_MAKE_BOTH);

        private final String name;

        MakeBehavior(String name) {
            this.name = name;
        }

        public static MakeBehavior of(String name) {
            return Stream.of(values()).filter(mb -> mb.name.equals(name)).findFirst().orElseThrow();
        }
    }

    String REACTOR_FAIL_FAST = "FAIL_FAST";

    String REACTOR_FAIL_AT_END = "FAIL_AT_END";

    String REACTOR_FAIL_NEVER = "FAIL_NEVER";

    enum FailureBehavior {
        FAIL_FAST(REACTOR_FAIL_FAST),

        FAIL_AT_END(REACTOR_FAIL_AT_END),

        FAIL_NEVER(REACTOR_FAIL_NEVER);

        private final String name;

        FailureBehavior(String name) {
            this.name = name;
        }

        public static FailureBehavior of(String name) {
            return Stream.of(values()).filter(fb -> fb.name.equals(name)).findFirst().orElseThrow();
        }
    }

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

    @Nonnull
    MakeBehavior getMakeBehavior();

    @Nonnull
    FailureBehavior getFailureBehavior();

    @Nullable
    String getResumeFrom();

    boolean isResume();

}
