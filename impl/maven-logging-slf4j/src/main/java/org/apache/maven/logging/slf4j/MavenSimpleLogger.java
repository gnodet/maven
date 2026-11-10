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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.api.logging.LogSink;
import org.apache.maven.api.logging.LoggerManager;
import org.apache.maven.api.services.MessageBuilder;
import org.apache.maven.logging.impl.DefaultLoggerManager;

import static org.apache.maven.jline.MessageUtils.builder;

/**
 * Logger for Maven, that support colorization of levels and stacktraces. This class implements 2 methods introduced in
 * slf4j-simple provider local copy.
 *
 * @since 3.5.0
 */
public class MavenSimpleLogger extends MavenBaseLogger {

    private String traceRenderedLevel;
    private String debugRenderedLevel;
    private String infoRenderedLevel;
    private String warnRenderedLevel;
    private String errorRenderedLevel;

    public static final String DEFAULT_LOG_LEVEL_KEY = "org.slf4j.simpleLogger.defaultLogLevel";

    MavenSimpleLogger(String name) {
        super(name);
    }

    MavenSimpleLogger(String name, String pluginId) {
        super(name, pluginId);
    }

    @Override
    protected String renderLevel(int level) {
        if (traceRenderedLevel == null) {
            traceRenderedLevel = builder().trace("TRACE").build();
            debugRenderedLevel = builder().debug("DEBUG").build();
            infoRenderedLevel = builder().info("INFO").build();
            warnRenderedLevel = builder().warning("WARNING").build();
            errorRenderedLevel = builder().error("ERROR").build();
        }
        return switch (level) {
            case LOG_LEVEL_TRACE -> traceRenderedLevel;
            case LOG_LEVEL_DEBUG -> debugRenderedLevel;
            case LOG_LEVEL_INFO -> infoRenderedLevel;
            case LOG_LEVEL_WARN -> warnRenderedLevel;
            default -> errorRenderedLevel;
        };
    }

    @Override
    protected void write(
            int level,
            String loggerName,
            String messageTemplate,
            Object[] messageArgs,
            StringBuilder formattedBuf,
            Throwable t) {
        LogSink sink = LoggerManager.get().getLogSink();
        if (sink != null) {
            sink.accept(MavenSimpleLoggerUtils.toLogEvent(
                    level, loggerName, messageTemplate, messageArgs, t, pluginId, formattedBuf.toString()));
        } else {
            super.write(formattedBuf, t);
        }
    }

    @Override
    protected void writeThrowable(Throwable t, PrintStream stream) {
        writeThrowable(t, stream::println);
    }

    protected void writeThrowable(Throwable t, Consumer<String> stream) {
        if (t == null) {
            return;
        }
        MessageBuilder builder = builder().failure(t.getClass().getName());
        if (t.getMessage() != null) {
            builder.a(": ").failure(t.getMessage());
        }
        stream.accept(builder.toString());

        printStackTrace(t, stream, "");
    }

    protected void printStackTrace(Throwable t, Consumer<String> stream, String prefix) {
        StackTraceElement[] frames = t.getStackTrace();
        int i = 0;
        boolean firstFrame = true;
        while (i < frames.length) {
            StackTraceElement e = frames[i];
            if (isFrameworkFrame(e)) {
                int start = i;
                while (i < frames.length && isFrameworkFrame(frames[i])) {
                    i++;
                }
                int collapsed = i - start;
                MessageBuilder builder = builder();
                builder.a(prefix);
                builder.a("    ");
                builder.style(STYLE_FAINT, "... " + collapsed + " framework frame" + (collapsed > 1 ? "s" : ""));
                stream.accept(builder.toString());
            } else {
                writeFrame(e, stream, prefix, firstFrame);
                firstFrame = false;
                i++;
            }
        }
        for (Throwable se : t.getSuppressed()) {
            writeThrowable(se, stream, "Suppressed", prefix + "    ");
        }
        Throwable cause = t.getCause();
        if (cause != null && t != cause) {
            writeThrowable(cause, stream, "Caused by", prefix);
        }
    }

    protected void writeFrame(StackTraceElement e, Consumer<String> stream, String prefix, boolean linkSource) {
        MessageBuilder builder = builder();
        builder.a(prefix);
        builder.a("    ");
        builder.style(STYLE_FAINT, "at ");
        String className = e.getClassName();
        int dot = className.lastIndexOf('.');
        if (dot >= 0) {
            builder.style(STYLE_FAINT, className.substring(0, dot + 1));
            builder.a(className.substring(dot + 1));
        } else {
            builder.a(className);
        }
        builder.a(".");
        builder.strong(e.getMethodName());
        builder.a("(");
        String sourceUrl = linkSource ? resolveSourceUrl(e) : null;
        if (sourceUrl != null) {
            builder.a(OSC_LINK_START + sourceUrl + BEL);
            builder.strong(getLocation(e));
            builder.a(OSC_LINK_END);
        } else {
            builder.strong(getLocation(e));
        }
        builder.a(")");
        stream.accept(builder.toString());
    }

    protected void writeThrowable(Throwable t, Consumer<String> stream, String caption, String prefix) {
        MessageBuilder builder = builder()
                .a(prefix)
                .warning(caption)
                .a(": ")
                .failure(t.getClass().getName());
        if (t.getMessage() != null) {
            builder.a(": ").failure(t.getMessage());
        }
        stream.accept(builder.toString());

        printStackTrace(t, stream, prefix);
    }

    private static final String STYLE_FAINT = "faint";

    private static final String OSC_LINK_START = "\033]8;;";
    private static final String OSC_LINK_END = "\033]8;;\007";
    private static final String BEL = "\007";

    private static final String MAVEN_GITHUB_BASE = "https://github.com/apache/maven";
    private static final String MAVEN_GROUP_ID = "org.apache.maven";

    private static final Map<URL, String[]> GAV_CACHE = new ConcurrentHashMap<>();

    static String resolveSourceUrl(StackTraceElement e) {
        if (e.getFileName() == null || e.getLineNumber() < 0) {
            return null;
        }
        try {
            Class<?> cls = Class.forName(e.getClassName(), false, MavenSimpleLogger.class.getClassLoader());
            if (cls.getProtectionDomain() == null
                    || cls.getProtectionDomain().getCodeSource() == null
                    || cls.getProtectionDomain().getCodeSource().getLocation() == null) {
                return null;
            }
            URL jarUrl = cls.getProtectionDomain().getCodeSource().getLocation();
            String[] gav = GAV_CACHE.computeIfAbsent(jarUrl, MavenSimpleLogger::readGav);
            if (gav == null || !MAVEN_GROUP_ID.equals(gav[0])) {
                return null;
            }
            String artifactId = gav[1];
            String version = gav[2];
            String ref = version.endsWith("-SNAPSHOT") ? "master" : "maven-" + version;
            String moduleParent = moduleParent(artifactId);
            String sourcePath = e.getClassName().replace('.', '/');
            int lastSlash = sourcePath.lastIndexOf('/');
            if (lastSlash >= 0) {
                sourcePath = sourcePath.substring(0, lastSlash + 1) + e.getFileName();
            } else {
                sourcePath = e.getFileName();
            }
            return MAVEN_GITHUB_BASE + "/blob/" + ref + "/" + moduleParent + artifactId + "/src/main/java/" + sourcePath
                    + "#L" + e.getLineNumber();
        } catch (Exception ex) {
            return null;
        }
    }

    private static final Set<String> COMPAT_ARTIFACTS = Set.of(
            "maven-plugin-api",
            "maven-builder-support",
            "maven-model",
            "maven-model-builder",
            "maven-settings",
            "maven-settings-builder",
            "maven-artifact",
            "maven-resolver-provider",
            "maven-repository-metadata",
            "maven-embedder",
            "maven-compat",
            "maven-toolchain-model",
            "maven-toolchain-builder");

    static String moduleParent(String artifactId) {
        if (artifactId.startsWith("maven-api-")) {
            return "api/";
        } else if (COMPAT_ARTIFACTS.contains(artifactId)) {
            return "compat/";
        } else {
            return "impl/";
        }
    }

    private static String[] readGav(URL jarUrl) {
        String path = jarUrl.getPath();
        if (!path.endsWith(".jar")) {
            return null;
        }
        try (JarFile jar = new JarFile(path)) {
            for (var entries = jar.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith("/pom.properties")
                        && entry.getName().startsWith("META-INF/maven/")) {
                    Properties props = new Properties();
                    try (InputStream is = jar.getInputStream(entry)) {
                        props.load(is);
                    }
                    String groupId = props.getProperty("groupId");
                    String artifactId = props.getProperty("artifactId");
                    String version = props.getProperty("version");
                    if (groupId != null && artifactId != null && version != null) {
                        return new String[] {groupId, artifactId, version};
                    }
                }
            }
        } catch (IOException ex) {
            // fall through
        }
        return null;
    }

    private static final Set<String> FRAMEWORK_PREFIXES = Set.of(
            "java.lang.reflect.",
            "java.lang.invoke.",
            "jdk.internal.",
            "sun.reflect.",
            "com.google.inject.",
            "org.codehaus.plexus.",
            "org.eclipse.sisu.",
            "com.google.common.");

    private static final List<String> FRAMEWORK_CONTAINS = List.of("$$", "$Proxy", "GeneratedMethodAccessor");

    static boolean isFrameworkFrame(StackTraceElement e) {
        String cls = e.getClassName();
        for (String prefix : FRAMEWORK_PREFIXES) {
            if (cls.startsWith(prefix)) {
                return true;
            }
        }
        for (String marker : FRAMEWORK_CONTAINS) {
            if (cls.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    protected String getLocation(final StackTraceElement e) {
        assert e != null;

        if (e.isNativeMethod()) {
            return "Native Method";
        } else if (e.getFileName() == null) {
            return "Unknown Source";
        } else if (e.getLineNumber() >= 0) {
            return e.getFileName() + ":" + e.getLineNumber();
        } else {
            return e.getFileName();
        }
    }

    public void configure(int defaultLogLevel) {
        LoggerManager mgr = LoggerManager.get();
        if (mgr instanceof DefaultLoggerManager dlm) {
            this.currentLogLevel = dlm.getLoggerLevel(name);
        } else {
            this.currentLogLevel = defaultLogLevel;
        }
        traceRenderedLevel = builder().trace("TRACE").build();
        debugRenderedLevel = builder().debug("DEBUG").build();
        infoRenderedLevel = builder().info("INFO").build();
        warnRenderedLevel = builder().warning("WARNING").build();
        errorRenderedLevel = builder().error("ERROR").build();
    }

    public void setLogLevel(int logLevel) {
        this.currentLogLevel = logLevel;
    }
}
