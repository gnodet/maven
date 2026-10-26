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

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.BuildInvocation;
import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.build.report.BuildReport;
import org.apache.maven.api.build.report.BuildStatus;
import org.apache.maven.api.build.report.FailureReport;
import org.apache.maven.api.build.report.MojoReport;
import org.apache.maven.api.build.report.SubprojectReport;
import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.logging.ProjectBuildLogAppender;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Collects build lifecycle events and produces a structured {@link BuildReport}
 * at the end of the session.
 * <p>
 * Registered as an {@link org.apache.maven.eventspy.EventSpy} via {@code @Named}/{@code @Singleton},
 * following the same pattern as {@code DefaultPluginValidationManager}.
 * <p>
 * Thread-safe: concurrent subproject builds (with {@code -T}) each write to their
 * own entry in a {@link ConcurrentHashMap}.
 * <p>
 * Log capture: registers a callback on {@link ProjectBuildLogAppender} to
 * receive the already-formed {@link LogEvent} objects produced by the main
 * logging pipeline. Uses thread-based tracking to associate events with
 * the currently-executing mojo or subproject.
 *
 * @since 4.1.0
 */
@Singleton
@Named
public final class BuildReportCollector extends AbstractEventSpy {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildReportCollector.class);

    static final String REPORT_DIR = "build-reports";
    static final String REPORT_LATEST = "build-report-latest.json";
    static final String TRACE_ID_MDC_KEY = "otel.trace_id";
    static final String SPAN_ID_MDC_KEY = "otel.span_id";

    private static final int MAX_STACKTRACE_LINES = 30;

    /**
     * Maximum number of log events captured per scope (mojo, subproject, or build).
     * Beyond this, events are silently dropped to prevent unbounded memory growth.
     */
    static final int MAX_LOG_EVENTS_PER_SCOPE = 500;

    // ---- Mutable state, populated during the build ----

    /** Per-project mojo tracking: project key -> list of in-flight/completed mojos. */
    private final Map<String, List<MojoTiming>> mojoTimings = new ConcurrentHashMap<>();

    /** Per-project start instants for duration computation. */
    private final Map<String, Instant> projectStartTimes = new ConcurrentHashMap<>();

    /** Per-project trace context captured when the project starts. */
    private final Map<String, TraceContext> projectTraceContexts = new ConcurrentHashMap<>();

    /** Per-mojo start instants for duration computation. */
    private final Map<String, Instant> mojoStartTimes = new ConcurrentHashMap<>();

    /** Per-mojo trace context captured when the mojo starts. */
    private final Map<String, TraceContext> mojoTraceContexts = new ConcurrentHashMap<>();

    /** Session-level state - set once on SessionStarted. */
    private volatile MavenSession session;

    /** Build invocation - captured once at SessionStarted, immutable thereafter. */
    private volatile BuildInvocation buildInvocation;

    // ---- Log capture state ----

    /**
     * Maps thread ID -> mojo key for the currently-executing mojo on that thread.
     * Lifecycle events and mojo execution run on the same thread, so this is safe
     * for parallel builds with {@code -T}.
     */
    private final Map<Long, String> currentMojoByThread = new ConcurrentHashMap<>();

    /** Per-mojo log buffers: mojo key -> captured log events. */
    private final Map<String, List<LogEvent>> mojoLogBuffers = new ConcurrentHashMap<>();

    /**
     * Maps thread ID -> project key for the currently-building project on that thread.
     * Used to route log events that occur between mojo executions to the subproject-level buffer.
     */
    private final Map<Long, String> currentProjectByThread = new ConcurrentHashMap<>();

    /** Per-subproject log buffers: project key -> events captured outside any mojo. */
    private final Map<String, List<LogEvent>> subprojectLogBuffers = new ConcurrentHashMap<>();

    /** Build-level log buffer: events captured outside any subproject lifecycle. */
    private final List<LogEvent> buildLogBuffer = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onEvent(Object event) {
        if (event instanceof ExecutionEvent executionEvent) {
            switch (executionEvent.getType()) {
                case SessionStarted:
                    onSessionStarted(executionEvent);
                    break;
                case SessionEnded:
                    onSessionEnded(executionEvent);
                    break;
                case ProjectStarted:
                    onProjectStarted(executionEvent);
                    break;
                case ProjectSucceeded:
                case ProjectFailed:
                case ProjectSkipped:
                    onProjectFinished(executionEvent);
                    break;
                case MojoStarted:
                    onMojoStarted(executionEvent);
                    break;
                case MojoSucceeded:
                case MojoFailed:
                    onMojoFinished(executionEvent);
                    break;
                case MojoSkipped:
                    onMojoSkipped(executionEvent);
                    break;
                default:
                    break;
            }
        }
    }

    // ---- Event handlers ----

    private void onSessionStarted(ExecutionEvent event) {
        this.session = event.getSession();
        this.buildInvocation = DefaultBuildInvocation.from(this.session);
        installLogCapture();
    }

    private void onSessionEnded(ExecutionEvent event) {
        removeLogCapture();

        MavenSession endSession = event.getSession();
        if (endSession == null) {
            return;
        }

        try {
            BuildReport report = buildReport(endSession);
            writeReport(report, endSession);
        } catch (Exception e) {
            // Never let the report collector crash the build
            LOGGER.debug("Failed to produce build report: {}", e.getMessage(), e);
        }
    }

    private void onProjectStarted(ExecutionEvent event) {
        String key = projectKey(event.getProject());
        projectStartTimes.put(key, MonotonicClock.now());
        projectTraceContexts.put(key, currentTraceContext());
        mojoTimings.putIfAbsent(key, Collections.synchronizedList(new ArrayList<>()));
        subprojectLogBuffers.put(key, Collections.synchronizedList(new ArrayList<>()));
        currentProjectByThread.put(Thread.currentThread().getId(), key);
    }

    private void onProjectFinished(ExecutionEvent event) {
        // Unregister the project from this thread so subsequent log events
        // fall through to the build-level buffer
        currentProjectByThread.remove(Thread.currentThread().getId());
    }

    private void onMojoStarted(ExecutionEvent event) {
        String mKey = mojoKey(event.getProject(), event.getMojoExecution());
        mojoStartTimes.put(mKey, MonotonicClock.now());
        mojoTraceContexts.put(mKey, currentTraceContext());

        // Register the current mojo for this thread so the log event sink
        // can associate events with this mojo execution
        currentMojoByThread.put(Thread.currentThread().getId(), mKey);
        mojoLogBuffers.put(mKey, Collections.synchronizedList(new ArrayList<>()));
    }

    private void onMojoFinished(ExecutionEvent event) {
        MojoExecution mojo = event.getMojoExecution();
        MavenProject project = event.getProject();
        String mKey = mojoKey(project, mojo);
        String pKey = projectKey(project);

        // Unregister the mojo from this thread
        currentMojoByThread.remove(Thread.currentThread().getId());

        Instant now = MonotonicClock.now();
        Instant startInstant = mojoStartTimes.remove(mKey);
        TraceContext traceContext = mojoTraceContexts.remove(mKey);
        if (traceContext == null) {
            traceContext = TraceContext.EMPTY;
        }
        if (startInstant == null) {
            startInstant = now;
        }
        Duration duration = Duration.between(startInstant, now);

        BuildStatus status =
                event.getType() == ExecutionEvent.Type.MojoSucceeded ? BuildStatus.SUCCESS : BuildStatus.FAILURE;

        // Drain the log buffer for this mojo
        List<LogEvent> logBuffer = mojoLogBuffers.remove(mKey);
        List<LogEvent> output = logBuffer != null ? List.copyOf(logBuffer) : List.of();

        MojoTiming timing = new MojoTiming(
                mojo.getGroupId(),
                mojo.getArtifactId(),
                mojo.getVersion(),
                mojo.getGoal(),
                mojo.getExecutionId(),
                mojo.getLifecyclePhase(),
                status,
                startInstant,
                duration,
                traceContext.traceId,
                traceContext.spanId,
                output);

        mojoTimings
                .computeIfAbsent(pKey, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(timing);
    }

    private void onMojoSkipped(ExecutionEvent event) {
        MojoExecution mojo = event.getMojoExecution();
        MavenProject project = event.getProject();
        String pKey = projectKey(project);

        Instant now = MonotonicClock.now();
        TraceContext traceContext = TraceContext.EMPTY;

        MojoTiming timing = new MojoTiming(
                mojo.getGroupId(),
                mojo.getArtifactId(),
                mojo.getVersion(),
                mojo.getGoal(),
                mojo.getExecutionId(),
                mojo.getLifecyclePhase(),
                BuildStatus.SKIPPED,
                now,
                Duration.ZERO,
                traceContext.traceId,
                traceContext.spanId,
                List.of());

        mojoTimings
                .computeIfAbsent(pKey, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(timing);
    }

    // ---- Structured log capture ----

    /**
     * Registers a callback on {@link ProjectBuildLogAppender} to receive the
     * already-formed {@link LogEvent} objects from the main logging pipeline.
     * This eliminates the need for a separate capture path and ensures the
     * build report captures the same enriched events (with sequence number,
     * source metadata) as the console output.
     */
    private void installLogCapture() {
        ProjectBuildLogAppender.setReportCapture(this::captureLogEvent);
    }

    private void removeLogCapture() {
        ProjectBuildLogAppender.setReportCapture(null);
    }

    /**
     * Routes a pre-formed {@link LogEvent} to the appropriate buffer
     * (mojo, subproject, or build-level) based on the current thread's
     * lifecycle context.
     */
    private void captureLogEvent(LogEvent event) {
        long threadId = Thread.currentThread().getId();

        // 1. Mojo-level: event belongs to the currently-executing mojo on this thread
        String mKey = currentMojoByThread.get(threadId);
        if (mKey != null) {
            List<LogEvent> buffer = mojoLogBuffers.get(mKey);
            if (buffer != null) {
                synchronized (buffer) {
                    if (buffer.size() < MAX_LOG_EVENTS_PER_SCOPE) {
                        buffer.add(event);
                    }
                }
            }
            return;
        }

        // 2. Module-level: project is active but no mojo is running
        String pKey = currentProjectByThread.get(threadId);
        if (pKey != null) {
            List<LogEvent> buffer = subprojectLogBuffers.get(pKey);
            if (buffer != null) {
                synchronized (buffer) {
                    if (buffer.size() < MAX_LOG_EVENTS_PER_SCOPE) {
                        buffer.add(event);
                    }
                }
            }
            return;
        }

        // 3. Build-level: no project active (startup, reactor summary, post-build)
        // synchronizedList requires external synchronization for the compound check-then-add
        // to avoid TOCTOU: two threads can both observe size() < MAX and both add().
        synchronized (buildLogBuffer) {
            if (buildLogBuffer.size() < MAX_LOG_EVENTS_PER_SCOPE) {
                buildLogBuffer.add(event);
            }
        }
    }

    // ---- Report assembly ----

    BuildReport buildReport(MavenSession endSession) {
        Instant now = MonotonicClock.now();
        Instant startInstant = endSession.getRequest().getStartInstant();
        if (startInstant == null) {
            startInstant = now;
        }
        Duration totalDuration = Duration.between(startInstant, now);

        MavenExecutionResult result = endSession.getResult();
        boolean hasFailures = result != null && result.hasExceptions();
        BuildStatus overallStatus = hasFailures ? BuildStatus.FAILURE : BuildStatus.SUCCESS;

        // Collect subproject reports
        List<SubprojectReport> subprojectReports = new ArrayList<>();
        for (MavenProject project : endSession.getProjects()) {
            subprojectReports.add(buildSubprojectReport(project, endSession));
        }

        // Collect failures
        List<FailureReport> failureReports = new ArrayList<>();
        if (result != null) {
            for (MavenProject project : endSession.getProjects()) {
                BuildSummary summary = result.getBuildSummary(project);
                if (summary instanceof BuildFailure buildFailure) {
                    failureReports.add(buildFailureReport(project, buildFailure));
                }
            }
        }

        // Metadata
        String mavenVersion = endSession.getSystemProperties().getProperty("maven.version", "unknown");
        String javaVersion = System.getProperty("java.version", "unknown");
        List<String> goals = endSession.getGoals();
        MavenProject topProject = endSession.getTopLevelProject();
        String projectId = topProject != null
                ? topProject.getGroupId() + ":" + topProject.getArtifactId() + ":" + topProject.getVersion()
                : "unknown";
        boolean multiSubproject = endSession.getProjects().size() > 1;
        int threads = endSession.getRequest().getDegreeOfConcurrency();

        // Build-level log events (outside any subproject lifecycle)
        List<LogEvent> buildOutput;
        synchronized (buildLogBuffer) {
            buildOutput = List.copyOf(buildLogBuffer);
        }

        return new DefaultBuildReport(
                buildInvocation != null ? buildInvocation : DefaultBuildInvocation.from(endSession),
                overallStatus,
                totalDuration,
                startInstant,
                mavenVersion,
                javaVersion,
                goals,
                projectId,
                multiSubproject,
                threads,
                subprojectReports,
                failureReports,
                // TODO: wire to Maven's problem collector (BuilderProblem events) in a follow-up PR
                List.of(),
                buildOutput);
    }

    private SubprojectReport buildSubprojectReport(MavenProject project, MavenSession endSession) {
        String key = projectKey(project);

        // Duration from BuildSummary (preferred) or fallback to our own tracking
        MavenExecutionResult result = endSession.getResult();
        Duration duration = Duration.ZERO;
        BuildStatus status = BuildStatus.SKIPPED;
        Instant subprojectStartTime =
                projectStartTimes.getOrDefault(key, endSession.getRequest().getStartInstant());

        if (result != null) {
            BuildSummary summary = result.getBuildSummary(project);
            if (summary instanceof BuildSuccess) {
                status = BuildStatus.SUCCESS;
                duration = summary.getExecTime();
            } else if (summary instanceof BuildFailure) {
                status = BuildStatus.FAILURE;
                duration = summary.getExecTime();
            } else if (summary != null) {
                // Unknown summary type - use its timing
                duration = summary.getExecTime();
            } else {
                // No summary means skipped
                Instant start = projectStartTimes.get(key);
                if (start != null) {
                    duration = Duration.between(start, MonotonicClock.now());
                }
            }
        }

        // Mojo reports
        List<MojoTiming> timings = mojoTimings.getOrDefault(key, Collections.emptyList());
        List<MojoReport> mojoReports;
        synchronized (timings) {
            mojoReports = timings.stream()
                    .map(t -> (MojoReport) new DefaultMojoReport(
                            t.groupId,
                            t.artifactId,
                            t.version,
                            t.goal,
                            t.executionId,
                            t.phase,
                            t.status,
                            t.startTime,
                            t.duration,
                            t.traceId,
                            t.spanId,
                            t.output))
                    .toList();
        }

        // Module-level log events (between mojos)
        List<LogEvent> subprojectLogBuffer = subprojectLogBuffers.getOrDefault(key, Collections.emptyList());
        List<LogEvent> subprojectOutput;
        synchronized (subprojectLogBuffer) {
            subprojectOutput = List.copyOf(subprojectLogBuffer);
        }

        TraceContext traceContext = projectTraceContexts.getOrDefault(key, TraceContext.EMPTY);

        return new DefaultSubprojectReport(
                project.getGroupId(),
                project.getArtifactId(),
                project.getVersion(),
                status,
                subprojectStartTime,
                duration,
                traceContext.traceId,
                traceContext.spanId,
                mojoReports,
                subprojectOutput);
    }

    private FailureReport buildFailureReport(MavenProject project, BuildFailure buildFailure) {
        String subproject = project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();

        // Try to find which mojo failed — walk in reverse to pick the last (proximate) failure
        String mojoId = null;
        List<MojoTiming> timings = mojoTimings.getOrDefault(projectKey(project), Collections.emptyList());
        synchronized (timings) {
            for (int i = timings.size() - 1; i >= 0; i--) {
                MojoTiming t = timings.get(i);
                if (t.status == BuildStatus.FAILURE) {
                    mojoId = t.artifactId + ":" + t.version + ":" + t.goal;
                    break;
                }
            }
        }

        Throwable cause = buildFailure.getCause();
        String message = cause != null ? cause.getMessage() : "Unknown error";
        String stackTrace = cause != null ? truncateStackTrace(cause) : null;

        Instant failureTimestamp = MonotonicClock.now();
        String exceptionType = cause != null ? cause.getClass().getSimpleName() : null;

        return new DefaultFailureReport(
                subproject,
                mojoId,
                failureTimestamp,
                exceptionType,
                message != null ? message : "Unknown error",
                stackTrace);
    }

    // ---- JSON persistence ----

    void writeReport(BuildReport report, MavenSession endSession) {
        Path topDirectory = endSession.getTopDirectory();
        if (topDirectory == null) {
            LOGGER.debug("No top directory available, skipping build report");
            return;
        }

        Path reportsDir = topDirectory.resolve("target").resolve(REPORT_DIR);

        try {
            Files.createDirectories(reportsDir);
            String json = BuildReportJsonWriter.toJson(report);

            // Timestamped file: build-report-20250729T143000Z.json
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                    .withZone(ZoneOffset.UTC)
                    .format(report.startTime());
            Path timestampedFile = reportsDir.resolve("build-report-" + timestamp + ".json");

            // Write to a temp file, then atomic-move into place so a crash
            // never leaves a half-written report on disk.
            Path tmpFile = Files.createTempFile(reportsDir, ".build-report-", ".tmp");
            try {
                Files.writeString(tmpFile, json);
                atomicMove(tmpFile, timestampedFile);
            } catch (IOException e) {
                Files.deleteIfExists(tmpFile);
                throw e;
            }

            // Latest symlink (or copy on filesystems that don't support symlinks)
            Path latestFile = reportsDir.resolve(REPORT_LATEST);
            try {
                // Atomic symlink swap: create new link, then rename over the old one.
                // The tiny window between Files.delete(tmpLink) and Files.createSymbolicLink
                // is safe here: tmpLink lives in the build's own target/ directory with a
                // randomised prefix, so no external process is expected to collide on it.
                Path tmpLink = Files.createTempFile(reportsDir, ".latest-", ".tmp");
                Files.delete(tmpLink); // createTempFile creates a regular file
                Files.createSymbolicLink(tmpLink, timestampedFile.getFileName());
                atomicMove(tmpLink, latestFile);
            } catch (UnsupportedOperationException | IOException symEx) {
                // Windows or restricted filesystem - fall back to a plain copy
                Files.writeString(latestFile, json);
            }

            LOGGER.debug("Build report written to {}", timestampedFile);
        } catch (IOException e) {
            LOGGER.warn("Failed to write build report to {}: {}", reportsDir, e.getMessage());
        }
    }

    /**
     * Attempts an atomic move; falls back to a plain move if the filesystem
     * does not support {@code ATOMIC_MOVE}.
     */
    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ---- Utility methods ----

    private static String projectKey(MavenProject project) {
        return project.getGroupId() + ":" + project.getArtifactId();
    }

    private static String mojoKey(MavenProject project, MojoExecution mojo) {
        return projectKey(project) + "#" + mojo.getGoal() + "@" + mojo.getExecutionId();
    }

    private static TraceContext currentTraceContext() {
        return new TraceContext(nonBlank(MDC.get(TRACE_ID_MDC_KEY)), nonBlank(MDC.get(SPAN_ID_MDC_KEY)));
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    static String truncateStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String full = sw.toString();
        String[] lines = full.split("\\r?\\n");
        if (lines.length <= MAX_STACKTRACE_LINES) {
            // Normalise line endings (PrintWriter uses \r\n on Windows)
            return full.replace("\r\n", "\n");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MAX_STACKTRACE_LINES; i++) {
            sb.append(lines[i]).append('\n');
        }
        sb.append("... ").append(lines.length - MAX_STACKTRACE_LINES).append(" more lines truncated\n");
        return sb.toString();
    }

    // ---- Internal records ----

    record MojoTiming(
            String groupId,
            String artifactId,
            String version,
            String goal,
            String executionId,
            String phase,
            BuildStatus status,
            Instant startTime,
            Duration duration,
            String traceId,
            String spanId,
            List<LogEvent> output) {}

    private record TraceContext(String traceId, String spanId) {
        private static final TraceContext EMPTY = new TraceContext(null, null);
    }
}
