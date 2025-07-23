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
package org.apache.maven.project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for remote repository functionality in both legacy and new architectures.
 */
public class RemoteRepositoriesTest {

    @TempDir
    Path tempDir;

    @Test
    public void testRemoteRepositoriesLegacyMode() throws Exception {
        // Test the legacy mode (MavenProject without delegation)
        MavenProject project = createLegacyMavenProject();

        // Create test repositories
        List<RemoteRepository> repositories = createTestRepositories();

        // Set the repositories
        project.setRemoteProjectRepositories(repositories);

        // Verify they were set correctly
        List<RemoteRepository> retrievedRepos = project.getRemoteProjectRepositories();
        assertNotNull(retrievedRepos);
        assertEquals(2, retrievedRepos.size());
        assertEquals("central", retrievedRepos.get(0).getId());
        assertEquals("test-repo", retrievedRepos.get(1).getId());
    }

    @Test
    public void testRemotePluginRepositoriesLegacyMode() throws Exception {
        // Test the legacy mode (MavenProject without delegation)
        MavenProject project = createLegacyMavenProject();

        // Create test repositories
        List<RemoteRepository> repositories = createTestRepositories();

        // Set the plugin repositories
        project.setRemotePluginRepositories(repositories);

        // Verify they were set correctly
        List<RemoteRepository> retrievedRepos = project.getRemotePluginRepositories();
        assertNotNull(retrievedRepos);
        assertEquals(2, retrievedRepos.size());
        assertEquals("central", retrievedRepos.get(0).getId());
        assertEquals("test-repo", retrievedRepos.get(1).getId());
    }

    private MavenProject createLegacyMavenProject() throws Exception {
        Model model = new Model();
        model.setGroupId("org.apache.maven.test");
        model.setArtifactId("test-project");
        model.setVersion("1.0.0");
        model.setPackaging("jar");

        MavenProject project = new MavenProject(model);
        project.setBasedir(tempDir.toFile());

        return project;
    }

    private List<RemoteRepository> createTestRepositories() {
        List<RemoteRepository> repositories = new ArrayList<>();

        // Add central repository
        RemoteRepository central =
                new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/").build();
        repositories.add(central);

        // Add test repository
        RemoteRepository testRepo =
                new RemoteRepository.Builder("test-repo", "default", "https://test.example.com/maven2/").build();
        repositories.add(testRepo);

        return repositories;
    }
}
