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
package org.apache.maven.its.gh13522;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

/**
 * Verifies that a {@link LogEvent} emitted from a DI-injected {@link Log}
 * carries the correct {@code loggerName}, {@code projectId}, and {@code mojoId}
 * fields as stamped by {@code DefaultBuildPluginManager} before mojo execution.
 *
 * <p>The injected {@code Log} is a pre-constructed logger whose {@code projectId}
 * and {@code mojoId} are set at injection time from the execution context, so they
 * are already present on every event without requiring any implementation-layer
 * class (such as {@code ProjectBuildLogAppender}).</p>
 *
 * <p>Results are written to {@code target/log-context.properties} for the
 * IT verifier.</p>
 */
@Mojo(name = "check-context", defaultPhase = org.apache.maven.api.Lifecycle.Phase.VALIDATE)
public class CheckContextMojo implements org.apache.maven.api.plugin.Mojo {

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private String buildDirectory;

    @Inject
    private Log log;

    @Override
    public void execute() {
        // Intercept one log event emitted through the injected Log.
        // DefaultBuildPluginManager pre-stamps the logger with projectId and mojoId
        // at injection time, so those fields are already on the raw event.
        AtomicReference<LogEvent> captured = new AtomicReference<>();
        LoggerManager mgr = LoggerManager.get();
        LogSink previousSink = mgr.getLogSink();
        mgr.setLogSink(event -> {
            captured.compareAndSet(null, event);
            if (previousSink != null) {
                previousSink.accept(event);
            }
        });

        try {
            log.info("gh-13522: checking logger context fields");
        } finally {
            mgr.setLogSink(previousSink);
        }

        LogEvent event = captured.get();
        String loggerName = event != null ? nvl(event.loggerName()) : "";
        String projectId = event != null ? nvl(event.projectId()) : "";
        String mojoId = event != null ? nvl(event.mojoId()) : "";

        Properties props = new Properties();
        props.setProperty("loggerName", loggerName);
        props.setProperty("projectId", projectId);
        props.setProperty("mojoId", mojoId);

        Path out = Paths.get(buildDirectory).resolve("log-context.properties");
        try {
            Files.createDirectories(out.getParent());
            try (OutputStream os = Files.newOutputStream(out)) {
                props.store(os, "Captured from gh-13522 IT");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write log-context.properties", e);
        }

        log.info("gh-13522: loggerName=" + loggerName + ", projectId=" + projectId + ", mojoId=" + mojoId);
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
