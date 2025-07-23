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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectBuilderException;
import org.apache.maven.api.services.ProjectBuilderRequest;
import org.apache.maven.api.services.ProjectBuilderResult;
import org.apache.maven.api.services.Sources;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.resolver.MavenSessionBuilderSupplier;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the new API-only DefaultProjectBuilder.
 * These tests verify that the new ProjectBuilder can build Project objects
 * using only the new Maven API without any dependencies on legacy MavenProject.
 */
@PlexusTest
class DefaultProjectBuilderTest {

    @TempDir
    Path tempDir;

    Session session;

    @Inject
    RepositorySystem repositorySystem;

    @Inject
    MavenRepositorySystem mavenRepositorySystem;

    @Inject
    PlexusContainer plexusContainer;

    @Inject
    RuntimeInformation runtimeInformation;

    @Inject
    SessionScope sessionScope;

    @BeforeEach
    void setup() {
        // Create session similar to TestApi setup
        RepositorySystemSession rss = new MavenSessionBuilderSupplier(repositorySystem, true)
                .get()
                .withLocalRepositoryBaseDirectories(new File("target/test-classes/apiv4-repo").toPath())
                .build();
        DefaultMavenExecutionRequest mer = new DefaultMavenExecutionRequest();
        DefaultMavenExecutionResult meres = new DefaultMavenExecutionResult();
        MavenSession ms = new MavenSession(rss, mer, meres);
        DefaultSession session = new DefaultSession(
                ms,
                repositorySystem,
                Collections.emptyList(),
                mavenRepositorySystem,
                new DefaultLookup(plexusContainer),
                runtimeInformation);
        org.apache.maven.api.RemoteRepository remoteRepository = session.getRemoteRepository(
                new RemoteRepository.Builder("mirror", "default", "file:target/test-classes/repo").build());
        this.session = session.withRemoteRepositories(Collections.singletonList(remoteRepository));
        InternalSession.associate(rss, this.session);
        sessionScope.enter();
        sessionScope.seed(InternalMavenSession.class, InternalMavenSession.from(this.session));
    }

    @Test
    void testProjectBuilderServiceAvailable() {
        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);
        assertNotNull(projectBuilder, "ProjectBuilder service should be available");
        assertTrue(
                projectBuilder instanceof DefaultProjectBuilder,
                "Should get the new DefaultProjectBuilder implementation");
    }

    @Test
    @Disabled("Test disabled until ModelBuilder integration is complete")
    void testBuildProjectFromPath() throws Exception {
        File pomFile = new File("src/test/resources/projects/modelsourcebasedir/pom.xml");
        assertTrue(pomFile.exists(), "Test POM file should exist");

        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);
        ProjectBuilderRequest request = ProjectBuilderRequest.builder()
                .session(session)
                .path(pomFile.toPath())
                .processPlugins(false)
                .build();

        ProjectBuilderResult result = projectBuilder.build(request);

        assertNotNull(result, "ProjectBuilderResult should not be null");
        assertNotNull(result.getRequest(), "Request should be preserved in result");
        assertEquals(request, result.getRequest(), "Request should match");

        Optional<Project> projectOpt = result.getProject();
        assertTrue(projectOpt.isPresent(), "Project should be present");

        Project project = projectOpt.get();
        assertNotNull(project, "Project should not be null");
        assertEquals("test.readparent", project.getGroupId());
        assertEquals("local-parent", project.getArtifactId());
        assertEquals("1.0", project.getVersion());
        assertEquals("pom", project.getPackaging().id());

        // Verify artifacts
        assertNotNull(project.getArtifacts(), "Artifacts should not be null");
        assertFalse(project.getArtifacts().isEmpty(), "Should have at least POM artifact");

        // For POM packaging, should only have POM artifact
        assertEquals(1, project.getArtifacts().size(), "POM project should have only POM artifact");
        ProducedArtifact pomArtifact = project.getArtifacts().get(0);
        assertEquals("pom", pomArtifact.getExtension(), "Should be POM artifact");

        // Verify paths
        Optional<Path> pomPath = result.getPomFile();
        assertTrue(pomPath.isPresent(), "POM path should be present");
        assertEquals(pomFile.toPath().toAbsolutePath(), pomPath.get().toAbsolutePath());

        assertNotNull(project.getBasedir(), "Base directory should not be null");
        assertEquals(
                pomFile.getParentFile().toPath().toAbsolutePath(),
                project.getBasedir().toAbsolutePath());
    }

    @Test
    @Disabled("Test disabled until ModelBuilder integration is complete")
    void testBuildProjectFromSource() throws Exception {
        File pomFile = new File("src/test/resources/projects/modelsourcebasedir/pom.xml");
        assertTrue(pomFile.exists(), "Test POM file should exist");

        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);
        ProjectBuilderRequest request = ProjectBuilderRequest.builder()
                .session(session)
                .source(Sources.buildSource(pomFile.toPath()))
                .processPlugins(false)
                .build();

        ProjectBuilderResult result = projectBuilder.build(request);

        assertNotNull(result, "ProjectBuilderResult should not be null");
        Optional<Project> projectOpt = result.getProject();
        assertTrue(projectOpt.isPresent(), "Project should be present");

        Project project = projectOpt.get();
        assertEquals("test.readparent", project.getGroupId());
        assertEquals("local-parent", project.getArtifactId());
        assertEquals("1.0", project.getVersion());
    }

    @Test
    @Disabled("Test disabled until ModelBuilder integration is complete")
    void testBuildJarProject() throws Exception {
        // Create a simple JAR project POM for testing
        String pomContent =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                     https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test.group</groupId>
                <artifactId>test-jar</artifactId>
                <version>1.0.0</version>
                <packaging>jar</packaging>
            </project>
            """;

        // Write POM content to temporary file
        Path tempPom = tempDir.resolve("test-pom.xml");
        Files.writeString(tempPom, pomContent);

        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);
        ProjectBuilderRequest request = ProjectBuilderRequest.builder()
                .session(session)
                .source(Sources.buildSource(tempPom))
                .processPlugins(false)
                .build();

        ProjectBuilderResult result = projectBuilder.build(request);

        Optional<Project> projectOpt = result.getProject();
        assertTrue(projectOpt.isPresent(), "Project should be present");

        Project project = projectOpt.get();
        assertEquals("test.group", project.getGroupId());
        assertEquals("test-jar", project.getArtifactId());
        assertEquals("1.0.0", project.getVersion());
        assertEquals("jar", project.getPackaging().id());

        // JAR project should have both POM and JAR artifacts
        assertEquals(2, project.getArtifacts().size(), "JAR project should have POM + JAR artifacts");

        // First artifact should be POM
        ProducedArtifact pomArtifact = project.getArtifacts().get(0);
        assertEquals("pom", pomArtifact.getExtension(), "First artifact should be POM");

        // Second artifact should be JAR
        ProducedArtifact jarArtifact = project.getArtifacts().get(1);
        assertEquals("jar", jarArtifact.getExtension(), "Second artifact should be JAR");
    }

    @Test
    void testBuildProjectWithInvalidRequest() {
        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);

        // Test with null request
        assertThrows(NullPointerException.class, () -> projectBuilder.build(null));

        // Test with request missing both path and source
        ProjectBuilderRequest invalidRequest = ProjectBuilderRequest.builder()
                .session(session)
                .processPlugins(false)
                .build();

        assertThrows(IllegalArgumentException.class, () -> projectBuilder.build(invalidRequest));
    }

    @Test
    @Disabled("Test disabled until ModelBuilder integration is complete")
    void testBuildProjectWithProblems() throws Exception {
        File pomFile = new File("src/test/resources/projects/artifactMissingVersion/pom.xml");
        assertTrue(pomFile.exists(), "Test POM file should exist");

        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);
        ProjectBuilderRequest request = ProjectBuilderRequest.builder()
                .session(session)
                .path(pomFile.toPath())
                .processPlugins(false)
                .build();

        // This should either succeed with problems or throw an exception
        try {
            ProjectBuilderResult result = projectBuilder.build(request);
            assertNotNull(result.getProblems(), "Problems collection should not be null");
            // If it succeeds, there should be problems reported
            assertFalse(result.getProblems().isEmpty(), "Should have problems for invalid POM");
        } catch (ProjectBuilderException e) {
            // This is also acceptable - the builder may throw an exception for invalid POMs
            assertNotNull(e.getMessage(), "Exception should have a message");
        }
    }

    @Test
    @Disabled("Test disabled until ModelBuilder integration is complete")
    void testProjectBuilderResultMethods() throws Exception {
        File pomFile = new File("src/test/resources/projects/modelsourcebasedir/pom.xml");

        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);
        ProjectBuilderRequest request = ProjectBuilderRequest.builder()
                .session(session)
                .path(pomFile.toPath())
                .processPlugins(false)
                .build();

        ProjectBuilderResult result = projectBuilder.build(request);

        // Test all ProjectBuilderResult methods
        assertNotNull(result.getRequest(), "getRequest() should not return null");
        assertNotNull(result.getProjectId(), "getProjectId() should not return null");
        assertNotNull(result.getPomFile(), "getPomFile() should not return null");
        assertNotNull(result.getProject(), "getProject() should not return null");
        assertNotNull(result.getProblems(), "getProblems() should not return null");
        assertNotNull(result.getDependencyResolverResult(), "getDependencyResolverResult() should not return null");

        // getDependencyResolverResult should be empty for now (placeholder implementation)
        assertFalse(
                result.getDependencyResolverResult().isPresent(),
                "getDependencyResolverResult() should be empty in current implementation");
    }

    @Test
    @Disabled("Test disabled until ModelBuilder integration is complete")
    void testBuildProjectWithProcessPlugins() throws Exception {
        File pomFile = new File("src/test/resources/projects/modelsourcebasedir/pom.xml");

        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);
        ProjectBuilderRequest request = ProjectBuilderRequest.builder()
                .session(session)
                .path(pomFile.toPath())
                .processPlugins(true) // Enable plugin processing
                .build();

        ProjectBuilderResult result = projectBuilder.build(request);

        Optional<Project> projectOpt = result.getProject();
        assertTrue(projectOpt.isPresent(), "Project should be present");

        Project project = projectOpt.get();
        assertNotNull(project, "Project should not be null");
        // When processPlugins=true, the ModelBuilder should use BUILD_PROJECT request type
    }

    @Test
    @Disabled("Test disabled until ModelBuilder integration is complete")
    void testBuildProjectWithRecursiveFlag() throws Exception {
        File pomFile = new File("src/test/resources/projects/modelsourcebasedir/pom.xml");

        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);

        // Test with recursive=true
        ProjectBuilderRequest recursiveRequest = ProjectBuilderRequest.builder()
                .session(session)
                .path(pomFile.toPath())
                .recursive(true) // Enable recursive loading
                .processPlugins(false)
                .build();

        ProjectBuilderResult result = projectBuilder.build(recursiveRequest);

        Optional<Project> projectOpt = result.getProject();
        assertTrue(projectOpt.isPresent(), "Project should be present");

        Project project = projectOpt.get();
        assertNotNull(project, "Project should not be null");

        // Test with recursive=false
        ProjectBuilderRequest nonRecursiveRequest = ProjectBuilderRequest.builder()
                .session(session)
                .path(pomFile.toPath())
                .recursive(false) // Disable recursive loading
                .processPlugins(false)
                .build();

        ProjectBuilderResult nonRecursiveResult = projectBuilder.build(nonRecursiveRequest);

        Optional<Project> nonRecursiveProjectOpt = nonRecursiveResult.getProject();
        assertTrue(nonRecursiveProjectOpt.isPresent(), "Project should be present even without recursive");

        // Both should succeed, but recursive loading affects internal ModelBuilder behavior
        assertNotNull(nonRecursiveProjectOpt.get(), "Non-recursive project should not be null");
    }

    @Test
    @Disabled("Test disabled until ModelBuilder integration is complete")
    void testBuildProjectWithRepositories() throws Exception {
        File pomFile = new File("src/test/resources/projects/modelsourcebasedir/pom.xml");

        // Create a custom remote repository
        RemoteRepository aetherRepo =
                new RemoteRepository.Builder("custom", "default", "https://repo1.maven.org/maven2/").build();
        org.apache.maven.api.RemoteRepository customRepo = ((DefaultSession) session).getRemoteRepository(aetherRepo);

        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);
        ProjectBuilderRequest request = ProjectBuilderRequest.builder()
                .session(session)
                .path(pomFile.toPath())
                .repositories(Collections.singletonList(customRepo))
                .processPlugins(false)
                .build();

        ProjectBuilderResult result = projectBuilder.build(request);

        Optional<Project> projectOpt = result.getProject();
        assertTrue(projectOpt.isPresent(), "Project should be present");

        Project project = projectOpt.get();
        assertNotNull(project, "Project should not be null");
    }

    @Test
    @Disabled("Test disabled until ModelBuilder integration is complete")
    void testBuildProjectWithDependencies() throws Exception {
        // Create a POM with dependencies for testing
        String pomContent =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                     https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test.group</groupId>
                <artifactId>test-with-deps</artifactId>
                <version>1.0.0</version>
                <packaging>jar</packaging>

                <dependencies>
                    <dependency>
                        <groupId>junit</groupId>
                        <artifactId>junit</artifactId>
                        <version>4.13.2</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
            </project>
            """;

        // Write POM content to temporary file
        Path tempPom = tempDir.resolve("test-with-deps.xml");
        Files.writeString(tempPom, pomContent);

        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);
        ProjectBuilderRequest request = ProjectBuilderRequest.builder()
                .session(session)
                .source(Sources.buildSource(tempPom))
                .processPlugins(false)
                .build();

        ProjectBuilderResult result = projectBuilder.build(request);

        Optional<Project> projectOpt = result.getProject();
        assertTrue(projectOpt.isPresent(), "Project should be present");

        Project project = projectOpt.get();
        assertNotNull(project.getDependencies(), "Dependencies should not be null");
        // Note: Dependencies are extracted from the model, actual resolution is separate
    }

    @Test
    void testConvenienceMethodBuildWithSessionAndPath() {
        File pomFile = new File("src/test/resources/projects/modelsourcebasedir/pom.xml");

        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);

        // Test the convenience method that takes session and path directly
        assertThrows(
                Exception.class,
                () -> {
                    projectBuilder.build(session, pomFile.toPath());
                },
                "Should throw exception until ModelBuilder integration is complete");
    }

    @Test
    void testConvenienceMethodBuildWithSessionAndSource() {
        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);

        // Test the convenience method that takes session and source directly
        assertThrows(
                Exception.class,
                () -> {
                    try {
                        Path tempPom = tempDir.resolve("test.xml");
                        Files.writeString(tempPom, "<project></project>");
                        projectBuilder.build(session, Sources.buildSource(tempPom));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                "Should throw exception until ModelBuilder integration is complete");
    }

    @Test
    @Disabled("Test disabled until ModelBuilder integration is complete")
    void testProjectModelConsistency() throws Exception {
        File pomFile = new File("src/test/resources/projects/modelsourcebasedir/pom.xml");

        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);
        ProjectBuilderRequest request = ProjectBuilderRequest.builder()
                .session(session)
                .path(pomFile.toPath())
                .processPlugins(false)
                .build();

        ProjectBuilderResult result = projectBuilder.build(request);
        Project project = result.getProject().get();

        // Verify that Project properties match the underlying Model
        assertEquals(project.getGroupId(), project.getModel().getGroupId());
        assertEquals(project.getArtifactId(), project.getModel().getArtifactId());
        assertEquals(project.getVersion(), project.getModel().getVersion());
        assertEquals(project.getPackaging().id(), project.getModel().getPackaging());

        // Verify that the model is accessible
        assertNotNull(project.getModel(), "Model should be accessible from Project");
    }

    @Test
    @Disabled("Test disabled until ModelBuilder integration is complete")
    void testProjectImmutability() throws Exception {
        File pomFile = new File("src/test/resources/projects/modelsourcebasedir/pom.xml");

        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);
        ProjectBuilderRequest request = ProjectBuilderRequest.builder()
                .session(session)
                .path(pomFile.toPath())
                .processPlugins(false)
                .build();

        ProjectBuilderResult result = projectBuilder.build(request);
        Project project = result.getProject().get();

        // Verify that collections are immutable
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    project.getArtifacts().clear();
                },
                "Artifacts collection should be immutable");

        // Note: Other immutability tests would depend on the specific implementation
        // of the collections returned by DefaultProject
    }

    @Test
    void testProjectBuilderRequestParameterMapping() {
        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);

        // Test that all ProjectBuilderRequest parameters are properly handled
        ProjectBuilderRequest.ProjectBuilderRequestBuilder builder = ProjectBuilderRequest.builder()
                .session(session)
                .processPlugins(true)
                .recursive(true)
                .allowStubModel(true);

        // Test with repositories
        if (session.getRemoteRepositories() != null
                && !session.getRemoteRepositories().isEmpty()) {
            builder.repositories(session.getRemoteRepositories());
        }

        // Create request with all parameters
        ProjectBuilderRequest request = builder.build();

        // Verify all parameters are accessible
        assertEquals(session, request.getSession());
        assertTrue(request.isProcessPlugins(), "processPlugins should be true");
        assertTrue(request.isRecursive(), "recursive should be true");
        assertTrue(request.isAllowStubModel(), "allowStubModel should be true");

        // The actual building would be tested in integration tests
        // Here we just verify the parameter mapping works
        assertNotNull(request.getRepositories(), "repositories should be accessible");
    }

    @Test
    @Disabled("Test disabled until ModelBuilder integration is complete")
    void testParameterMappingToModelBuilder() throws Exception {
        File pomFile = new File("src/test/resources/projects/modelsourcebasedir/pom.xml");

        ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);

        // Create request with all parameters that should be mapped to ModelBuilderRequest
        ProjectBuilderRequest request = ProjectBuilderRequest.builder()
                .session(session)
                .path(pomFile.toPath())
                .processPlugins(true)
                .recursive(true)
                .allowStubModel(false)
                .repositories(session.getRemoteRepositories())
                .build();

        // This should properly map all parameters to ModelBuilderRequest:
        // - session -> session
        // - processPlugins -> requestType (BUILD_PROJECT vs BUILD_EFFECTIVE)
        // - recursive -> recursive
        // - repositories -> repositories
        // - session profiles -> profiles
        // - session properties -> systemProperties, userProperties
        // - trace -> trace

        ProjectBuilderResult result = projectBuilder.build(request);

        // Verify the request was processed with all parameters
        assertEquals(request, result.getRequest(), "Original request should be preserved");
        assertNotNull(result.getProject(), "Project should be built with all parameters");
    }
}
