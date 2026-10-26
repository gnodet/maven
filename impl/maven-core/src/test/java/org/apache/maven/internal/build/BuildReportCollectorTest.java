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
package org.apache.maven.internal.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.build.report.BuildReport;
import org.apache.maven.api.build.report.BuildStatus;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildReportCollectorTest {

    @TempDir
    Path tempDir;

    private BuildReportCollector collector;

    @BeforeEach
    void setUp() {
        collector = new BuildReportCollector();
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void testBuildReportAssembly() {
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        MavenExecutionResult result = session.getResult();

        // Simulate: session started → project started → mojo started → mojo succeeded → project succeeded → session
        // ended
        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, project, null));

        MojoExecution mojo = createMojoExecution(
                "org.apache.maven.plugins", "maven-compiler-plugin", "3.15.0", "compile", "default-compile", "compile");
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoStarted, session, project, mojo));
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoSucceeded, session, project, mojo));

        // Record build success in the result
        result.addBuildSummary(new BuildSuccess(project, 5000));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, project, null));

        // Build the report
        BuildReport report = collector.buildReport(session);

        assertNotNull(report);
        assertEquals(BuildStatus.SUCCESS, report.status());
        assertEquals("1.0.0", report.mavenVersion());
        assertFalse(report.multiSubproject());
        assertEquals(1, report.threads());
        assertEquals(1, report.subprojects().size());
        assertEquals("org.example", report.subprojects().get(0).groupId());
        assertEquals("my-app", report.subprojects().get(0).artifactId());
        assertEquals(BuildStatus.SUCCESS, report.subprojects().get(0).status());
        assertEquals(1, report.subprojects().get(0).mojos().size());
        assertEquals("compile", report.subprojects().get(0).mojos().get(0).goal());
        assertEquals(
                BuildStatus.SUCCESS, report.subprojects().get(0).mojos().get(0).status());
        assertNull(report.subprojects().get(0).traceId());
        assertNull(report.subprojects().get(0).spanId());
        assertNull(report.subprojects().get(0).mojos().get(0).traceId());
        assertNull(report.subprojects().get(0).mojos().get(0).spanId());
        assertTrue(report.failures().isEmpty());
        assertTrue(report.problems().isEmpty());

        // invocation assertions
        assertNotNull(report.invocation());
        assertEquals(List.of("clean", "install"), report.invocation().goals());
        assertFalse(report.invocation().offline());
        assertFalse(report.invocation().updateSnapshots());
        assertNotNull(report.invocation().systemInfo());
        assertFalse(report.invocation().systemInfo().isEmpty());
        assertTrue(report.invocation().systemInfo().containsKey("available.processors"));
    }

    @Test
    void testTraceContextIsCapturedFromMdc() {
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));

        MDC.put(BuildReportCollector.TRACE_ID_MDC_KEY, "module-trace");
        MDC.put(BuildReportCollector.SPAN_ID_MDC_KEY, "module-span");
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, project, null));

        MojoExecution mojo = createMojoExecution(
                "org.apache.maven.plugins", "maven-compiler-plugin", "3.15.0", "compile", "default-compile", "compile");
        MDC.put(BuildReportCollector.TRACE_ID_MDC_KEY, "mojo-trace");
        MDC.put(BuildReportCollector.SPAN_ID_MDC_KEY, "mojo-span");
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoStarted, session, project, mojo));
        MDC.clear();
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoSucceeded, session, project, mojo));

        session.getResult().addBuildSummary(new BuildSuccess(project, 1000));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, project, null));

        BuildReport report = collector.buildReport(session);
        var subproject = report.subprojects().get(0);
        assertEquals("module-trace", subproject.traceId());
        assertEquals("module-span", subproject.spanId());
        assertEquals("mojo-trace", subproject.mojos().get(0).traceId());
        assertEquals("mojo-span", subproject.mojos().get(0).spanId());
    }

    @Test
    void testBuildReportWithFailure() {
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        MavenExecutionResult result = session.getResult();

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, project, null));

        MojoExecution mojo = createMojoExecution(
                "org.apache.maven.plugins", "maven-compiler-plugin", "3.15.0", "compile", "default-compile", "compile");
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoStarted, session, project, mojo));
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoFailed, session, project, mojo));

        RuntimeException failure = new RuntimeException("Compilation failure: 3 errors");
        result.addBuildSummary(new org.apache.maven.execution.BuildFailure(project, 3000, failure));
        result.addException(failure);
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectFailed, session, project, null));

        BuildReport report = collector.buildReport(session);

        assertEquals(BuildStatus.FAILURE, report.status());
        assertEquals(1, report.failures().size());
        assertEquals("org.example:my-app:1.0.0", report.failures().get(0).subproject());
        assertTrue(report.failures().get(0).message().contains("Compilation failure"));
        assertNotNull(report.failures().get(0).timestamp(), "failure should have a timestamp");
        assertEquals("RuntimeException", report.failures().get(0).exceptionType(), "exceptionType from cause");

        // Navigate from failure -> subproject -> mojo using lookup methods
        var failureReport = report.failures().get(0);
        var subprojectOpt = report.findSubproject(failureReport);
        assertTrue(subprojectOpt.isPresent(), "findSubproject(FailureReport) should find the subproject");
        assertEquals("my-app", subprojectOpt.get().artifactId());
        assertEquals("org.example:my-app:1.0.0", subprojectOpt.get().id());

        assertNotNull(failureReport.mojo(), "failure should reference a mojo");
        var mojoOpt = subprojectOpt.get().findMojo(failureReport.mojo());
        assertTrue(mojoOpt.isPresent(), "findMojo should find the failed mojo");
        assertEquals("compile", mojoOpt.get().goal());
        assertEquals(BuildStatus.FAILURE, mojoOpt.get().status());
        assertEquals("maven-compiler-plugin:3.15.0:compile", mojoOpt.get().id());
    }

    @Test
    void testMojoSkippedIsRecorded() {
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        MavenExecutionResult result = session.getResult();

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, project, null));

        // A skipped mojo has no MojoStarted event — MojoSkipped fires directly
        MojoExecution mojo = createMojoExecution(
                "org.apache.maven.plugins", "maven-surefire-plugin", "3.0.0", "test", "default-test", "test");
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoSkipped, session, project, mojo));

        result.addBuildSummary(new BuildSuccess(project, 1000));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, project, null));

        BuildReport report = collector.buildReport(session);

        assertEquals(1, report.subprojects().size());
        var subproject = report.subprojects().get(0);
        assertEquals(1, subproject.mojos().size(), "skipped mojo should appear in the report");
        var mojoReport = subproject.mojos().get(0);
        assertEquals("test", mojoReport.goal());
        assertEquals(BuildStatus.SKIPPED, mojoReport.status());
        assertEquals(Duration.ZERO, mojoReport.duration());
        assertTrue(mojoReport.output().isEmpty());
    }

    @Test
    void testWriteReportToFile() throws IOException {
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        session.getResult().addBuildSummary(new BuildSuccess(project, 1000));

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, project, null));

        BuildReport report = collector.buildReport(session);
        collector.writeReport(report, session);

        Path reportsDir = tempDir.resolve("target").resolve(BuildReportCollector.REPORT_DIR);
        Path latestFile = reportsDir.resolve(BuildReportCollector.REPORT_LATEST);
        assertTrue(Files.exists(latestFile), "build-report-latest.json should exist");

        String content = Files.readString(latestFile);
        assertTrue(content.contains("\"status\": \"SUCCESS\""));
        assertTrue(content.contains("\"artifactId\": \"my-app\""));
    }

    @Test
    void testMultiModuleBuild() {
        MavenProject parent = createProject("org.example", "parent", "1.0.0");
        MavenProject child1 = createProject("org.example", "child-api", "1.0.0");
        MavenProject child2 = createProject("org.example", "child-impl", "1.0.0");

        MavenSession session = createSession(parent, child1, child2);
        MavenExecutionResult result = session.getResult();

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, parent, null));

        // Build each subproject
        for (MavenProject p : List.of(parent, child1, child2)) {
            collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, p, null));
            result.addBuildSummary(new BuildSuccess(p, 1000));
            collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, p, null));
        }

        BuildReport report = collector.buildReport(session);

        assertEquals(BuildStatus.SUCCESS, report.status());
        assertTrue(report.multiSubproject());
        assertEquals(3, report.subprojects().size());
        assertEquals("parent", report.subprojects().get(0).artifactId());
        assertEquals("child-api", report.subprojects().get(1).artifactId());
        assertEquals("child-impl", report.subprojects().get(2).artifactId());
    }

    // ---- Warning mode tests ----

    @Test
    void testWarningModeNoneSuppressesSummary() {
        // Register a diagnostic so there's something to print
        collector.getDiagnosticCollector().report(warning("test-key", "test warning", "test-source"));

        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        session.getUserProperties().setProperty("maven.build.warningMode", "none");
        session.getResult().addBuildSummary(new BuildSuccess(project, 1000));

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.SessionEnded, session, project, null));

        // No exceptions means summary was suppressed (in "none" mode)
        assertFalse(session.getResult().hasExceptions());
    }

    @Test
    void testWarningModeFailAddsException() {
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        session.getUserProperties().setProperty("maven.build.warningMode", "fail");
        session.getResult().addBuildSummary(new BuildSuccess(project, 1000));

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));
        // Report the diagnostic after SessionStarted so it is not wiped by reset()
        collector.getDiagnosticCollector().report(warning("test-key", "test warning", "test-source"));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.SessionEnded, session, project, null));

        assertTrue(session.getResult().hasExceptions(), "Build should fail with --warning-mode=fail and warnings");
        String msg = session.getResult().getExceptions().get(0).getMessage();
        assertTrue(
                msg.contains("warning(s)") && msg.contains("--warning-mode=fail"),
                "Exception message should say 'warning(s)' and mention the flag, got: " + msg);
    }

    @Test
    void testWarningModeFailNoWarningsNoException() {
        // No warnings registered — fail mode should not add exception
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        session.getUserProperties().setProperty("maven.build.warningMode", "fail");
        session.getResult().addBuildSummary(new BuildSuccess(project, 1000));

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.SessionEnded, session, project, null));

        assertFalse(session.getResult().hasExceptions(), "No warnings, no exception");
    }

    @Test
    void testWarningModeSummaryIsDefault() {
        collector.getDiagnosticCollector().report(warning("test-key", "test warning", "test-source"));

        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        // No warningMode property set — defaults to "summary"
        session.getResult().addBuildSummary(new BuildSuccess(project, 1000));

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.SessionEnded, session, project, null));

        // Summary mode should NOT fail the build
        assertFalse(session.getResult().hasExceptions());
    }

    // ---- Synthetic diagnostic key tests ----

    @Test
    void testSyntheticDiagnosticKeyStripsPaths() {
        String key1 = BuildReportCollector.syntheticDiagnosticKey("compiler", "Foo.java:42: unchecked cast");
        String key2 = BuildReportCollector.syntheticDiagnosticKey("compiler", "Bar.java:99: unchecked cast");
        assertEquals(key1, key2, "Same warning from different files should deduplicate");
    }

    @Test
    void testSyntheticDiagnosticKeyDifferentMessages() {
        String key1 = BuildReportCollector.syntheticDiagnosticKey("compiler", "unchecked cast");
        String key2 = BuildReportCollector.syntheticDiagnosticKey("compiler", "deprecated API");
        assertFalse(key1.equals(key2), "Different messages should produce different keys");
    }

    @Test
    void testSyntheticDiagnosticKeyIncludesLoggerSuffix() {
        String key = BuildReportCollector.syntheticDiagnosticKey(
                "org.apache.maven.plugins.compiler.CompilerMojo", "unchecked cast");
        assertTrue(key.startsWith("auto:CompilerMojo:"), "Key should include short logger suffix");
    }

    // ---- Diagnostic suppression wiring tests ----

    @Test
    void testDiagnosticSuppressionFromUserProperty() {
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        session.getUserProperties().setProperty("maven.diagnostic.suppress", "key-a,key-b");
        session.getResult().addBuildSummary(new BuildSuccess(project, 1000));

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));

        // Report diagnostics — key-a and key-b should be suppressed
        collector.getDiagnosticCollector().report(warning("key-a", "warning a", null));
        collector.getDiagnosticCollector().report(warning("key-b", "warning b", null));
        collector.getDiagnosticCollector().report(warning("key-c", "warning c", null));

        assertEquals(1, collector.getDiagnosticCollector().getProblems().size());
        assertEquals(
                "key-c", collector.getDiagnosticCollector().getProblems().get(0).getKey());
    }

    @Test
    void testDiagnosticSuppressionWildcard() {
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        session.getUserProperties().setProperty("maven.diagnostic.suppress", "auto:*");
        session.getResult().addBuildSummary(new BuildSuccess(project, 1000));

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));

        collector.getDiagnosticCollector().report(warning("auto:Compiler:abc", "unchecked", null));
        collector.getDiagnosticCollector().report(warning("explicit-key", "explicit", null));

        assertEquals(1, collector.getDiagnosticCollector().getProblems().size());
        assertEquals(
                "explicit-key",
                collector.getDiagnosticCollector().getProblems().get(0).getKey());
    }

    @Test
    void testBuildInvocationSensitiveKeyRedaction() {
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);

        // Inject user properties with sensitive and non-sensitive keys
        Properties userProps = new Properties();
        userProps.setProperty("my.password", "s3cr3t");
        userProps.setProperty("db.secret", "hunter2");
        userProps.setProperty("api.token", "abc123");
        userProps.setProperty("maven.compiler.release", "21");
        session.getRequest().setUserProperties(userProps);

        var env = DefaultBuildInvocation.from(session);

        assertEquals("***", env.userProperties().get("my.password"), "password key must be redacted");
        assertEquals("***", env.userProperties().get("db.secret"), "secret key must be redacted");
        assertEquals("***", env.userProperties().get("api.token"), "token key must be redacted");
        assertEquals("21", env.userProperties().get("maven.compiler.release"), "non-sensitive key must be kept");
    }

    @Test
    void testWarningModeFailDoesNotAddExceptionWhenBuildAlreadyFailed() {
        // --warning-mode=fail must not stack a second exception when the build already failed
        // for a different reason (e.g. compile error) — that produces confusing double-failure output.
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        session.getUserProperties().setProperty("maven.build.warningMode", "fail");
        session.getResult().addBuildSummary(new BuildFailure(project, 1000, new RuntimeException("compile error")));
        // Pre-existing exception — simulates a compile/test failure
        session.getResult().addException(new RuntimeException("compile error"));

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));
        // Report the diagnostic after SessionStarted so it is not wiped by reset()
        collector.getDiagnosticCollector().report(warning("test-key", "test warning", "test-source"));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.SessionEnded, session, project, null));

        // Should still have exactly 1 exception (the pre-existing one), not 2
        assertEquals(
                1,
                session.getResult().getExceptions().size(),
                "Should not add a second exception when build already failed");
        assertEquals("compile error", session.getResult().getExceptions().get(0).getMessage());
    }

    @Test
    void testDiagnosticCollectorResetBetweenSessions() {
        // Simulate mvnd: two sequential builds in the same JVM sharing one @Singleton collector.
        // The second session must start with a clean diagnostic state.
        collector.getDiagnosticCollector().report(warning("key-from-build-1", "leftover warning", "src1"));

        // First session
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session1 = createSession(project);
        session1.getResult().addBuildSummary(new BuildSuccess(project, 1000));
        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session1, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.SessionEnded, session1, project, null));

        // Second session — must start clean; the warning from build 1 should be gone
        MavenSession session2 = createSession(project);
        session2.getUserProperties().setProperty("maven.build.warningMode", "fail");
        session2.getResult().addBuildSummary(new BuildSuccess(project, 500));
        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session2, project, null));
        // No new warnings reported in session 2
        collector.onEvent(createEvent(ExecutionEvent.Type.SessionEnded, session2, project, null));

        assertFalse(
                session2.getResult().hasExceptions(),
                "No warnings in session 2 — fail mode must not carry over warnings from session 1");
    }

    @Test
    void testBuildInvocationOfflineAndProfiles() {
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        session.getRequest().setOffline(true);
        session.getRequest().setUpdateSnapshots(true);
        session.getRequest().setActiveProfiles(List.of("ci", "!slow-tests"));
        session.getRequest().setSelectedProjects(List.of(":my-app"));
        session.getRequest().setResumeFrom(":other-module");

        var env = DefaultBuildInvocation.from(session);

        assertTrue(env.offline());
        assertTrue(env.updateSnapshots());
        assertEquals(List.of("ci", "!slow-tests"), env.activeProfiles());
        assertEquals(List.of(":my-app"), env.selectedProjects());
        assertEquals(":other-module", env.resumeFrom());
    }

    // ---- Test helpers ----

    private static BuilderProblem warning(String key, String message, String source) {
        return BuilderProblem.builder()
                .source(source)
                .message(message)
                .severity(BuilderProblem.Severity.WARNING)
                .key(key)
                .build();
    }

    private MavenProject createProject(String groupId, String artifactId, String version) {
        MavenProject project = new MavenProject();
        project.setGroupId(groupId);
        project.setArtifactId(artifactId);
        project.setVersion(version);
        return project;
    }

    private MavenSession createSession(MavenProject... projects) {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setStartInstant(MonotonicClock.now());
        request.setGoals(List.of("clean", "install"));
        request.setTopDirectory(tempDir);

        Properties systemProperties = new Properties();
        systemProperties.setProperty("maven.version", "1.0.0");
        request.setSystemProperties(systemProperties);

        MavenExecutionResult result = new DefaultMavenExecutionResult();

        @SuppressWarnings("deprecation")
        MavenSession session = new MavenSession(null, null, request, result);
        session.setProjects(List.of(projects));
        return session;
    }

    private MojoExecution createMojoExecution(
            String groupId, String artifactId, String version, String goal, String executionId, String phase) {
        @SuppressWarnings("deprecation")
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId(groupId);
        pluginDescriptor.setArtifactId(artifactId);
        pluginDescriptor.setVersion(version);

        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal(goal);
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);

        MojoExecution execution = new MojoExecution(mojoDescriptor, executionId);
        execution.setLifecyclePhase(phase);

        return execution;
    }

    private ExecutionEvent createEvent(
            ExecutionEvent.Type type, MavenSession session, MavenProject project, MojoExecution mojo) {
        return new ExecutionEvent() {
            @Override
            public Type getType() {
                return type;
            }

            @Override
            public MavenSession getSession() {
                return session;
            }

            @Override
            public MavenProject getProject() {
                return project;
            }

            @Override
            public MojoExecution getMojoExecution() {
                return mojo;
            }

            @Override
            public Exception getException() {
                return null;
            }
        };
    }
}
