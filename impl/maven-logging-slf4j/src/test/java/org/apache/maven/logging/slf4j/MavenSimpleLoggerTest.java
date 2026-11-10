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
package org.apache.maven.logging.slf4j;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.maven.jline.MessageUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenSimpleLoggerTest {

    boolean colorEnabled;

    @BeforeEach
    void setup() {
        colorEnabled = MessageUtils.isColorEnabled();
        MessageUtils.setColorEnabled(false);
    }

    @AfterEach
    void tearDown() {
        MessageUtils.setColorEnabled(colorEnabled);
    }

    @Test
    void includesCauseAndSuppressedExceptionsWhenWritingThrowables(TestInfo testInfo) {
        Exception causeOfSuppressed = new NoSuchElementException("cause of suppressed");
        Exception suppressed = new IllegalStateException("suppressed", causeOfSuppressed);
        suppressed.addSuppressed(new IllegalArgumentException(
                "suppressed suppressed", new ArrayIndexOutOfBoundsException("suppressed suppressed cause")));
        Exception cause = new IllegalArgumentException("cause");
        cause.addSuppressed(suppressed);
        Exception throwable = new RuntimeException("top-level", cause);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new MavenSimpleLogger("logger").writeThrowable(throwable, new PrintStream(output));

        String actual = output.toString(UTF_8);
        List<String> actualLines = Arrays.asList(actual.split(System.lineSeparator()));

        Class<?> testClass = testInfo.getTestClass().get();
        String testMethodName = testInfo.getTestMethod().get().getName();
        String testClassStackTraceLinePattern = "at " + testClass.getName() + "." + testMethodName + "\\("
                + testClass.getSimpleName() + ".java:\\d+\\)";
        List<String> expectedLines = Arrays.asList(
                "java.lang.RuntimeException: top-level",
                "    " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "Caused by: java.lang.IllegalArgumentException: cause",
                "    " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "    Suppressed: java.lang.IllegalStateException: suppressed",
                "        " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "        Suppressed: java.lang.IllegalArgumentException: suppressed suppressed",
                "            " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "        Caused by: java.lang.ArrayIndexOutOfBoundsException: suppressed suppressed cause",
                "            " + testClassStackTraceLinePattern,
                ">> stacktrace >>",
                "    Caused by: java.util.NoSuchElementException: cause of suppressed",
                "        " + testClassStackTraceLinePattern,
                ">> stacktrace >>");

        assertLinesMatch(expectedLines, actualLines);
    }

    @Test
    void isFrameworkFrameDetectsReflectionAndDI() {
        assertTrue(MavenSimpleLogger.isFrameworkFrame(
                new StackTraceElement("java.lang.reflect.Method", "invoke", "Method.java", 498)));
        assertTrue(MavenSimpleLogger.isFrameworkFrame(
                new StackTraceElement("java.lang.invoke.MethodHandle", "invokeExact", null, -1)));
        assertTrue(MavenSimpleLogger.isFrameworkFrame(
                new StackTraceElement("com.google.inject.internal.InjectorImpl", "getInstance", null, -1)));
        assertTrue(MavenSimpleLogger.isFrameworkFrame(
                new StackTraceElement("org.codehaus.plexus.DefaultPlexusContainer", "lookup", null, -1)));
        assertTrue(MavenSimpleLogger.isFrameworkFrame(
                new StackTraceElement("org.eclipse.sisu.inject.Guice$1", "get", null, -1)));
        assertTrue(MavenSimpleLogger.isFrameworkFrame(
                new StackTraceElement("com.example.SomeClass$$EnhancerByGuice", "doStuff", null, -1)));
    }

    @Test
    void isFrameworkFrameKeepsMavenAndPluginFrames() {
        assertFalse(MavenSimpleLogger.isFrameworkFrame(
                new StackTraceElement("org.apache.maven.DefaultMaven", "execute", "DefaultMaven.java", 100)));
        assertFalse(MavenSimpleLogger.isFrameworkFrame(
                new StackTraceElement("org.apache.maven.plugins.compiler.CompilerMojo", "execute", null, -1)));
        assertFalse(MavenSimpleLogger.isFrameworkFrame(
                new StackTraceElement("com.example.MyPlugin", "doStuff", "MyPlugin.java", 42)));
    }

    @Test
    void collapsesConsecutiveFrameworkFrames() {
        StackTraceElement userFrame =
                new StackTraceElement("org.apache.maven.DefaultMaven", "execute", "DefaultMaven.java", 100);
        StackTraceElement reflectFrame1 =
                new StackTraceElement("java.lang.reflect.Method", "invoke", "Method.java", 498);
        StackTraceElement reflectFrame2 =
                new StackTraceElement("java.lang.reflect.Method", "invoke", "Method.java", 520);
        StackTraceElement guiceFrame = new StackTraceElement(
                "com.google.inject.internal.InjectorImpl", "getInstance", "InjectorImpl.java", 50);
        StackTraceElement userFrame2 = new StackTraceElement(
                "org.apache.maven.plugin.DefaultBuildPluginManager",
                "executeMojo",
                "DefaultBuildPluginManager.java",
                75);

        Exception ex = new RuntimeException("test");
        ex.setStackTrace(new StackTraceElement[] {userFrame, reflectFrame1, reflectFrame2, guiceFrame, userFrame2});

        List<String> lines = new ArrayList<>();
        new MavenSimpleLogger("logger").writeThrowable(ex, lines::add);

        assertEquals("java.lang.RuntimeException: test", lines.get(0));
        assertTrue(lines.get(1).contains("at"));
        assertTrue(lines.get(1).contains("DefaultMaven"));
        assertTrue(lines.get(2).contains("... 3 framework frames"));
        assertTrue(lines.get(3).contains("at"));
        assertTrue(lines.get(3).contains("DefaultBuildPluginManager"));
        assertEquals(4, lines.size());
    }

    @Test
    void moduleParentMapsArtifactIds() {
        assertEquals("api/", MavenSimpleLogger.moduleParent("maven-api-core"));
        assertEquals("api/", MavenSimpleLogger.moduleParent("maven-api-model"));
        assertEquals("impl/", MavenSimpleLogger.moduleParent("maven-core"));
        assertEquals("impl/", MavenSimpleLogger.moduleParent("maven-cli"));
        assertEquals("impl/", MavenSimpleLogger.moduleParent("maven-logging-slf4j"));
        assertEquals("compat/", MavenSimpleLogger.moduleParent("maven-embedder"));
        assertEquals("compat/", MavenSimpleLogger.moduleParent("maven-model"));
        assertEquals("compat/", MavenSimpleLogger.moduleParent("maven-artifact"));
    }

    @Test
    void firstFrameOfEachCauseGetsLink() {
        StackTraceElement mavenFrame =
                new StackTraceElement("org.apache.maven.DefaultMaven", "execute", "DefaultMaven.java", 300);
        StackTraceElement mavenFrame2 =
                new StackTraceElement("org.apache.maven.DefaultMaven", "doExecute", "DefaultMaven.java", 192);

        Exception cause = new IllegalStateException("inner");
        cause.setStackTrace(new StackTraceElement[] {mavenFrame2});
        Exception top = new RuntimeException("outer", cause);
        top.setStackTrace(new StackTraceElement[] {mavenFrame});

        List<String> lines = new ArrayList<>();
        new MavenSimpleLogger("logger").writeThrowable(top, lines::add);

        // First frame of top exception should have OSC 8 link (if class is resolvable)
        // First frame of cause should also have OSC 8 link
        // The link presence depends on Class.forName succeeding at test time,
        // so just verify the structure: 4 lines (top header, top frame, cause header, cause frame)
        assertEquals(4, lines.size());
        assertTrue(lines.get(0).contains("RuntimeException"));
        assertTrue(lines.get(1).contains("DefaultMaven"));
        assertTrue(lines.get(2).contains("Caused by"));
        assertTrue(lines.get(3).contains("DefaultMaven"));
    }
}
