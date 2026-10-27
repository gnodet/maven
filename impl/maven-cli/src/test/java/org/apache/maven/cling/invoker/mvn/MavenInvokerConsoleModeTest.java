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
package org.apache.maven.cling.invoker.mvn;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.cisupport.CIInfo;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MavenInvoker#determineConsoleMode(MavenContext)}.
 */
class MavenInvokerConsoleModeTest {

    /**
     * Builds the minimal MavenInvoker needed to call determineConsoleMode().
     * ProtoLookup with no entries is sufficient — determineConsoleMode() does not use the lookup.
     */
    private MavenInvoker invoker() {
        return new MavenInvoker(ProtoLookup.builder().build(), null);
    }

    /**
     * Builds a MavenContext whose options().console() returns the given value and whose
     * InvokerRequest reports no CI environment and is not embedded.
     * interactive and terminal are left at their zero/null defaults.
     */
    private MavenContext context(String consoleValue) {
        return context(consoleValue, false, false);
    }

    private MavenContext context(String consoleValue, boolean ci, boolean interactive) {
        // --- mock MavenOptions ---
        MavenOptions options = mock(MavenOptions.class);
        when(options.console()).thenReturn(Optional.ofNullable(consoleValue));
        when(options.forceInteractive()).thenReturn(Optional.empty());

        // --- mock ParserRequest (needed for InvokerRequest.embedded()) ---
        ParserRequest parserRequest = mock(ParserRequest.class);
        when(parserRequest.embedded()).thenReturn(false);

        // --- mock InvokerRequest ---
        InvokerRequest request = mock(InvokerRequest.class);
        when(request.parserRequest()).thenReturn(parserRequest);
        when(request.cwd()).thenReturn(Path.of("."));
        when(request.installationDirectory()).thenReturn(Path.of("."));
        when(request.userHomeDirectory()).thenReturn(Path.of(System.getProperty("user.home")));
        when(request.topDirectory()).thenReturn(Path.of("."));
        when(request.rootDirectory()).thenReturn(Optional.empty());
        when(request.userProperties()).thenReturn(Map.of());
        when(request.systemProperties()).thenReturn(Map.of());
        when(request.ciInfo()).thenReturn(ci ? Optional.of(mock(CIInfo.class)) : Optional.<CIInfo>empty());

        // --- build real MavenContext so invokerRequest field is properly set ---
        MavenContext ctx = new MavenContext(request, false, options);
        ctx.interactive = interactive;
        // ctx.terminal stays null (non-TTY)
        return ctx;
    }

    // -------------------------------------------------------------------------
    // Unknown value → InvokerException
    // -------------------------------------------------------------------------

    @Test
    void unknownConsoleModeThrows() {
        InvokerException ex =
                assertThrows(InvokerException.class, () -> invoker().determineConsoleMode(context("foo")));
        assertTrue(ex.getMessage().contains("foo"), "error message should mention the bad value");
        assertTrue(
                ex.getMessage().contains("plain") && ex.getMessage().contains("rich"),
                "error message should list valid options");
    }

    @Test
    void anotherUnknownValueThrows() {
        assertThrows(InvokerException.class, () -> invoker().determineConsoleMode(context("typo")));
    }

    // -------------------------------------------------------------------------
    // Known values — case-insensitive, returned in lower-case
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"plain", "PLAIN", "Plain", "rich", "RICH", "verbose", "VERBOSE", "machine", "MACHINE"})
    void knownConsoleModeReturnedLowerCase(String input) {
        String result = invoker().determineConsoleMode(context(input));
        assertEquals(input.toLowerCase(), result, "--console=" + input + " should return lowercase canonical value");
    }

    // -------------------------------------------------------------------------
    // Auto-detection paths
    // -------------------------------------------------------------------------

    @Test
    void autoModeWithCiReturnsPlain() {
        String result = invoker().determineConsoleMode(context("auto", /* ci= */ true, /* interactive= */ false));
        assertEquals("plain", result, "--console=auto in CI should resolve to 'plain'");
    }

    @Test
    void autoModeWithoutCiAndNoTtyReturnsVerbose() {
        String result = invoker().determineConsoleMode(context("auto", /* ci= */ false, /* interactive= */ false));
        assertEquals("verbose", result, "--console=auto without CI/TTY should resolve to 'verbose'");
    }

    @Test
    void nullConsoleDefaultsToAutoPath() {
        // null console → defaults to "auto" internally; no CI, no TTY → verbose
        String result = invoker().determineConsoleMode(context(null));
        assertEquals("verbose", result, "absent --console with no CI/TTY should resolve to 'verbose'");
    }
}
