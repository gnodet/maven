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
package org.apache.maven.cling.event;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.api.ExecutionListener;
import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.internal.impl.InternalMavenSession;

/**
 * Base class for console execution event loggers providing common duration tracking,
 * project resolution, and session result synchronization.
 *
 * @since 4.1.0
 */
public abstract class AbstractExecutionEventLogger implements ExecutionListener, BuildStatsSink {

    protected final Map<String, Instant> projectStartTimes = new ConcurrentHashMap<>();
    protected final Map<String, Duration> projectDurations = new ConcurrentHashMap<>();
    protected final AtomicInteger passedCount = new AtomicInteger();
    protected final AtomicInteger failedCount = new AtomicInteger();
    protected final AtomicInteger skippedCount = new AtomicInteger();
    protected volatile boolean buildFailed;

    /** Log level threshold for --fail-on-severity; {@code null} means disabled. */
    protected LogLevel failOnSeverity;

    /**
     * Sets the log level threshold for {@code --fail-on-severity}.
     *
     * @param failOnSeverity the threshold level, or {@code null} to disable
     * @return this instance for fluent chaining
     */
    public AbstractExecutionEventLogger withFailOnSeverity(LogLevel failOnSeverity) {
        this.failOnSeverity = failOnSeverity;
        return this;
    }

    /**
     * If {@code --fail-on-severity} is active and the threshold was reached, injects a failure
     * exception into the session result so that the build exits with a non-zero code.
     * Must be called from {@code sessionEnded()} before {@code logResult()}.
     */
    protected void checkFailOnSeverity(org.apache.maven.api.ExecutionEvent event) {
        if (failOnSeverity == null) {
            return;
        }
        if (!LoggerManager.get().hasReached(failOnSeverity)) {
            return;
        }
        buildFailed = true;
        Session session = event != null ? event.session() : null;
        if (session instanceof InternalMavenSession ims) {
            org.apache.maven.execution.MavenSession ms = ims.getMavenSession();
            if (ms != null && ms.getResult() != null) {
                ms.getResult()
                        .addException(new Exception("Build failed due to log statements with a higher severity than"
                                + " allowed. Fix the logged issues or remove flag --fail-on-severity (-fos)."));
            }
        }
    }

    @Override
    public void recordProjectDuration(String key, Duration duration) {
        if (key != null && duration != null) {
            projectDurations.put(key, duration);
        }
    }

    @Override
    public void syncResult(boolean hasExceptions, int passed, int failed, int skipped) {
        if (hasExceptions) {
            buildFailed = true;
        }
        if (passedCount.get() == 0 && failedCount.get() == 0 && skippedCount.get() == 0) {
            passedCount.set(passed);
            failedCount.set(failed);
            skippedCount.set(skipped);
        }
    }

    protected static String projectKey(Project project) {
        if (project == null) {
            return "";
        }
        String gid = project.groupId();
        String aid = project.artifactId();
        return (gid != null ? gid : "") + ":" + (aid != null ? aid : "");
    }

    protected static String getProjectDisplayName(Project project) {
        if (project == null) {
            return "";
        }
        String name = null;
        try {
            if (project.model() != null) {
                name = project.model().getName();
            }
        } catch (Exception ignored) {
        }
        if (name != null && !name.isBlank()) {
            return name;
        }
        return project.artifactId() != null ? project.artifactId() : "";
    }

    protected Duration getProjectDuration(Project project) {
        if (project == null) {
            return null;
        }
        Duration recorded = projectDurations.remove(projectKey(project));
        if (recorded != null) {
            return recorded;
        }
        Instant start = projectStartTimes.remove(projectKey(project));
        return (start != null) ? Duration.between(start, MonotonicClock.now()) : null;
    }

    protected static List<Project> getAllProjects(Session session) {
        if (session == null) {
            return null;
        }
        if (session instanceof InternalMavenSession ims) {
            try {
                org.apache.maven.execution.MavenSession ms = ims.getMavenSession();
                if (ms != null && ms.getAllProjects() != null) {
                    return ms.getAllProjects().stream()
                            .map(ims::getProject)
                            .filter(Objects::nonNull)
                            .toList();
                }
            } catch (Exception ignored) {
            }
        }
        if (session instanceof ExecutionListenerAdapter.SessionAdapterHelper helper) {
            return helper.getAllProjects();
        }
        return null;
    }
}
