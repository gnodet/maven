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
package org.apache.maven.logging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;
import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.logging.impl.DefaultLogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ProjectBuildLogAppenderTest {

    private final BuildEventListener listener = mock(BuildEventListener.class);

    @AfterEach
    void cleanup() {
        ProjectBuildLogAppender.setReportCapture(null);
    }

    /** Sends a synthetic log event through the {@link LogSink} installed by the appender. */
    private static void sendEvent(LogLevel level, String loggerName, String message) {
        LogSink sink = LoggerManager.get().getLogSink();
        LogEvent event = new DefaultLogEvent(Instant.now(), level, message, loggerName, null);
        sink.accept(event);
    }

    @Test
    void summaryModeSuppressesInlineWarningsButCapturesThemForReport() throws Exception {
        List<LogEvent> captured = new ArrayList<>();
        ProjectBuildLogAppender.setReportCapture(captured::add);

        try (ProjectBuildLogAppender appender = new ProjectBuildLogAppender(listener, "summary")) {
            // Clear invocations caused by bootstrap events drained when the sink was installed.
            clearInvocations(listener);
            captured.clear();

            sendEvent(LogLevel.WARN, "test.logger", "warning message");

            verify(listener, never()).projectLogMessage(any(), any());
            assertEquals(1, captured.size());
        }
    }

    @Test
    void allModeForwardsWarningsAndCapturesThem() throws Exception {
        List<LogEvent> captured = new ArrayList<>();
        ProjectBuildLogAppender.setReportCapture(captured::add);

        try (ProjectBuildLogAppender appender = new ProjectBuildLogAppender(listener, "all")) {

            sendEvent(LogLevel.WARN, "test.logger", "warning message");
            verify(listener).projectLogMessage(any(), any());
            assertEquals(1, captured.size());
        }
    }

    @Test
    void summaryModeStillForwardsInfoMessages() throws Exception {
        try (ProjectBuildLogAppender appender = new ProjectBuildLogAppender(listener, "summary")) {

            sendEvent(LogLevel.INFO, "test.logger", "info message");

            verify(listener).projectLogMessage(any(), any());
        }
    }

    @Test
    void summaryModeMatchingIsCaseInsensitive() throws Exception {
        try (ProjectBuildLogAppender appender = new ProjectBuildLogAppender(listener, "SUMMARY")) {
            clearInvocations(listener);
            sendEvent(LogLevel.WARN, "test.logger", "warning message");

            verify(listener, never()).projectLogMessage(any(), any());
        }
    }
}
