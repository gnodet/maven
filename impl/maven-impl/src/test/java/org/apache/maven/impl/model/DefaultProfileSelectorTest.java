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
package org.apache.maven.impl.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.ActivationProperty;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.model.Properties;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.ProfileActivator;
import org.apache.maven.impl.model.profile.SimpleProblemCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link DefaultProfileSelector} with focus on cascading activation behavior.
 */
public class DefaultProfileSelectorTest {

    private DefaultProfileSelector selector;
    private SimpleProblemCollector problems;

    @BeforeEach
    void setUp() {
        selector = new DefaultProfileSelector();
        // Add a simple property-based activator for testing
        selector.addProfileActivator(new PropertyProfileActivator());
        problems = new SimpleProblemCollector();
    }

    @Test
    void testNonCascadingActivation() {
        // Create profiles with property-based activation
        Profile profile1 = createProfile("profile1", "prop1", "value1", Profile.SOURCE_POM);
        Profile profile2 = createProfile("profile2", "prop2", "value2", Profile.SOURCE_POM);

        List<Profile> profiles = Arrays.asList(profile1, profile2);

        // Create context with prop1 set
        DefaultProfileActivationContext context = new DefaultProfileActivationContext();
        context.setSystemProperties(Map.of("prop1", "value1"));

        // Test non-cascading mode
        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);

        assertEquals(1, activeProfiles.size());
        assertEquals("profile1", activeProfiles.get(0).getId());
        assertTrue(problems.getErrors().isEmpty());
    }

    @Test
    void testCascadingActivation() {
        // Create profiles where one activates another through properties
        Profile profile1 = createProfileWithProperties(
                "profile1", "prop1", "value1", Map.of("prop2", "value2"), Profile.SOURCE_POM);
        Profile profile2 = createProfile("profile2", "prop2", "value2", Profile.SOURCE_POM);

        List<Profile> profiles = Arrays.asList(profile1, profile2);

        // Create context with prop1 set (should activate profile1, which sets prop2, which activates profile2)
        DefaultProfileActivationContext context = new DefaultProfileActivationContext();
        context.setSystemProperties(Map.of("prop1", "value1"));

        // Test cascading mode
        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);

        assertEquals(2, activeProfiles.size());
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile1".equals(p.getId())));
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile2".equals(p.getId())));
        assertTrue(problems.getErrors().isEmpty());
    }

    @Test
    void testCascadingVsNonCascadingDifference() {
        // Create profiles where cascading would activate more profiles
        Profile profile1 = createProfileWithProperties(
                "profile1", "prop1", "value1", Map.of("prop2", "value2"), Profile.SOURCE_POM);
        Profile profile2 = createProfile("profile2", "prop2", "value2", Profile.SOURCE_POM);

        List<Profile> profiles = Arrays.asList(profile1, profile2);

        DefaultProfileActivationContext context = new DefaultProfileActivationContext();
        context.setSystemProperties(Map.of("prop1", "value1"));

        // Cascading should activate both profile1 and profile2
        List<Profile> cascading = selector.getActiveProfiles(profiles, context, problems);
        assertEquals(2, cascading.size());
    }

    @Test
    void testActiveByDefaultProfiles() {
        Profile defaultProfile = createActiveByDefaultProfile("default-profile", Profile.SOURCE_POM);
        Profile conditionalProfile = createProfile("conditional", "prop1", "value1", Profile.SOURCE_POM);

        List<Profile> profiles = Arrays.asList(defaultProfile, conditionalProfile);

        DefaultProfileActivationContext context = new DefaultProfileActivationContext();

        // Should activate default profile when no conditions are met
        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);
        assertEquals(1, activeProfiles.size());
        assertEquals("default-profile", activeProfiles.get(0).getId());

        // Should not activate default profile when conditional profile is active
        context.setSystemProperties(Map.of("prop1", "value1"));
        activeProfiles = selector.getActiveProfiles(profiles, context, problems);
        assertEquals(1, activeProfiles.size());
        assertEquals("conditional", activeProfiles.get(0).getId());
    }

    @Test
    void testMixedSourceProfiles() {
        Profile pomProfile = createProfile("pom-profile", "prop1", "value1", Profile.SOURCE_POM);
        Profile settingsProfile = createProfile("settings-profile", "prop2", "value2", Profile.SOURCE_SETTINGS);

        List<Profile> profiles = Arrays.asList(pomProfile, settingsProfile);

        DefaultProfileActivationContext context = new DefaultProfileActivationContext();
        context.setSystemProperties(Map.of("prop1", "value1", "prop2", "value2"));

        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);
        assertEquals(2, activeProfiles.size());

        // Settings profiles should come after POM profiles in the result
        assertEquals("pom-profile", activeProfiles.get(0).getId());
        assertEquals("settings-profile", activeProfiles.get(1).getId());
    }

    @Test
    void testEmptyProfilesList() {
        List<Profile> profiles = Collections.emptyList();
        DefaultProfileActivationContext context = new DefaultProfileActivationContext();

        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);
        assertTrue(activeProfiles.isEmpty());
    }

    @Test
    void testExplicitlyActivatedProfiles() {
        Profile profile1 = createProfile("profile1", "nonexistent", "value", Profile.SOURCE_POM);
        Profile profile2 = createProfile("profile2", "prop2", "value2", Profile.SOURCE_POM);

        List<Profile> profiles = Arrays.asList(profile1, profile2);

        DefaultProfileActivationContext context = new DefaultProfileActivationContext();
        context.setActiveProfileIds(List.of("profile1"));
        context.setSystemProperties(Map.of("prop2", "value2"));

        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);
        assertEquals(2, activeProfiles.size());
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile1".equals(p.getId())));
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile2".equals(p.getId())));
    }

    // Helper methods for creating test profiles

    private Profile createProfile(String id, String propName, String propValue, String source) {
        Profile profile = Profile.newBuilder()
                .id(id)
                .activation(Activation.newBuilder()
                        .property(ActivationProperty.newBuilder()
                                .name(propName)
                                .value(propValue)
                                .build())
                        .build())
                .build();
        profile.setSource(source);
        return profile;
    }

    private Profile createProfileWithProperties(
            String id, String propName, String propValue, Map<String, String> profileProperties, String source) {
        Profile profile = Profile.newBuilder()
                .id(id)
                .activation(Activation.newBuilder()
                        .property(ActivationProperty.newBuilder()
                                .name(propName)
                                .value(propValue)
                                .build())
                        .build())
                .properties(profileProperties)
                .build();
        profile.setSource(source);
        return profile;
    }

    private Profile createActiveByDefaultProfile(String id, String source) {
        return Profile.newBuilder()
                .id(id)
                .source(source)
                .activation(Activation.newBuilder().activeByDefault(true).build())
                .build();
    }

    /**
     * Simple property-based profile activator for testing.
     */
    private static class PropertyProfileActivator implements ProfileActivator {
        @Override
        public boolean isActive(
                Profile profile,
                ProfileActivationContext context,
                org.apache.maven.api.services.ModelProblemCollector problems) {
            Activation activation = profile.getActivation();
            if (activation == null || activation.getProperty() == null) {
                return false;
            }

            ActivationProperty property = activation.getProperty();
            String name = property.getName();
            String expectedValue = property.getValue();

            if (name == null) {
                return false;
            }

            // Check user properties first, then system properties
            String actualValue = context.getUserProperty(name);
            if (actualValue == null) {
                actualValue = context.getSystemProperty(name);
            }

            if (expectedValue == null || expectedValue.isEmpty()) {
                return actualValue != null;
            }

            return expectedValue.equals(actualValue);
        }

        @Override
        public boolean presentInConfig(
                Profile profile,
                ProfileActivationContext context,
                org.apache.maven.api.services.ModelProblemCollector problems) {
            return profile.getActivation() != null && profile.getActivation().getProperty() != null;
        }
    }
}
