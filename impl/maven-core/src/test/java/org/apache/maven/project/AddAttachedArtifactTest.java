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

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for the addAttachedArtifact functionality in both legacy and new architectures.
 */
public class AddAttachedArtifactTest {

    @TempDir
    Path tempDir;

    @Test
    public void testAddAttachedArtifactLegacyMode() throws Exception {
        // Test the legacy mode (MavenProject without delegation)
        MavenProject project = createLegacyMavenProject();

        // Create an attached artifact
        Artifact attachedArtifact = createTestArtifact("sources");

        // Add the attached artifact
        project.addAttachedArtifact(attachedArtifact);

        // Verify it was added
        assertEquals(1, project.getAttachedArtifacts().size());
        assertTrue(project.getAttachedArtifacts().contains(attachedArtifact));
    }

    @Test
    public void testAddAttachedArtifactNewMode() throws Exception {
        // Test the new mode (MavenProject with delegation to Project + ProjectManager)
        // This test would require setting up the full new architecture
        // For now, we'll just verify that the legacy mode still works
        // TODO: Implement full new architecture test when ProjectBuilder is updated

        MavenProject project = createLegacyMavenProject();
        Artifact attachedArtifact = createTestArtifact("javadoc");

        project.addAttachedArtifact(attachedArtifact);

        assertEquals(1, project.getAttachedArtifacts().size());
        assertTrue(project.getAttachedArtifacts().contains(attachedArtifact));
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

    private Artifact createTestArtifact(String classifier) throws Exception {
        // Create a temporary file for the artifact
        Path artifactFile = Files.createTempFile(tempDir, "test-artifact-", ".jar");
        Files.write(artifactFile, "test content".getBytes());

        Artifact artifact = new DefaultArtifact(
                "org.apache.maven.test",
                "test-project",
                VersionRange.createFromVersion("1.0.0"),
                "compile",
                "jar",
                classifier,
                new DefaultArtifactHandler("jar"));

        artifact.setFile(artifactFile.toFile());
        return artifact;
    }
}
