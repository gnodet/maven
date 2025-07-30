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

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Container for storing the request from the user to activate or deactivate certain projects and optionally fail the
 * build if those projects do not exist.
 */
public class ProjectActivation {

    /**
     * ProjectActivationSettings
     * @param selector the selector of a project, which can be the project directory, [groupId]:[artifactId] or :[artifactId]
     * @param activationSettings describes how/when to active or deactivate the project
     */
    public record ProjectActivationSettings(String selector, ActivationSettings activationSettings) {}

    /**
     * List of activated and deactivated projects.
     */
    private final List<ProjectActivationSettings> activations;

    public ProjectActivation(List<ProjectActivationSettings> activations) {
        this.activations = List.copyOf(activations);
    }

    public List<ProjectActivationSettings> getActivations() {
        return activations;
    }

    private Stream<ProjectActivationSettings> getProjects(Predicate<ActivationSettings> predicate) {
        return this.activations.stream().filter(activation -> predicate.test(activation.activationSettings));
    }

    private Set<String> getProjectSelectors(Predicate<ActivationSettings> predicate) {
        return getProjects(predicate).map(activation -> activation.selector).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * @return Required active project selectors, never {@code null}.
     */
    public Set<String> getRequiredActiveProjectSelectors() {
        return getProjectSelectors(pa -> !pa.optional() && pa.active());
    }

    /**
     * @return Optional active project selectors, never {@code null}.
     */
    public Set<String> getOptionalActiveProjectSelectors() {
        return getProjectSelectors(pa -> pa.optional() && pa.active());
    }

    /**
     * @return Required inactive project selectors, never {@code null}.
     */
    public Set<String> getRequiredInactiveProjectSelectors() {
        return getProjectSelectors(pa -> !pa.optional() && !pa.active());
    }

    /**
     * @return Optional inactive project selectors, never {@code null}.
     */
    public Set<String> getOptionalInactiveProjectSelectors() {
        return getProjectSelectors(pa -> pa.optional() && !pa.active());
    }

    public boolean isEmpty() {
        return this.activations.isEmpty();
    }
}
