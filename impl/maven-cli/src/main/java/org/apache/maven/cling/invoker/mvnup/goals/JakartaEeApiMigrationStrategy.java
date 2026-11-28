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
package org.apache.maven.cling.invoker.mvnup.goals;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.ARTIFACT_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.BUILD;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCY;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCY_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.GROUP_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGINS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.VERSION;

/**
 * Strategy for migrating deprecated Java EE API dependencies to their Jakarta EE successors.
 *
 * <p>Java 11 (JEP 320) removed several Java EE and CORBA modules from the JDK that were
 * previously bundled in {@code rt.jar}. Projects that declared these APIs as explicit Maven
 * dependencies using their legacy {@code javax.*} groupIds will fail to compile on JDK 11+
 * unless they migrate to the standalone Jakarta EE equivalents.
 *
 * <p>This strategy detects legacy Java EE API artifacts in {@code <dependencies>},
 * {@code <dependencyManagement>}, profile dependencies, and plugin dependencies, and
 * replaces them with their Jakarta EE equivalents:
 *
 * <table>
 *   <caption>Legacy to Jakarta EE migration map</caption>
 *   <tr><th>Legacy (removed in JDK 11)</th><th>Jakarta EE replacement</th></tr>
 *   <tr><td>{@code javax.xml.bind:jaxb-api}</td>
 *       <td>{@code jakarta.xml.bind:jakarta.xml.bind-api:2.3.3}</td></tr>
 *   <tr><td>{@code com.sun.xml.bind:jaxb-core}</td>
 *       <td>{@code org.glassfish.jaxb:jaxb-core:2.3.0.1}</td></tr>
 *   <tr><td>{@code com.sun.xml.bind:jaxb-impl}</td>
 *       <td>{@code org.glassfish.jaxb:jaxb-runtime:2.3.9}</td></tr>
 *   <tr><td>{@code javax.xml.ws:jaxws-api}</td>
 *       <td>{@code jakarta.xml.ws:jakarta.xml.ws-api:2.3.3}</td></tr>
 *   <tr><td>{@code javax.jws:javax.jws-api}</td>
 *       <td>{@code jakarta.jws:jakarta.jws-api:1.1.1}</td></tr>
 *   <tr><td>{@code javax.annotation:javax.annotation-api}</td>
 *       <td>{@code jakarta.annotation:jakarta.annotation-api:1.3.5}</td></tr>
 *   <tr><td>{@code javax.activation:activation}</td>
 *       <td>{@code jakarta.activation:jakarta.activation-api:1.2.2}</td></tr>
 *   <tr><td>{@code javax.activation:javax.activation-api}</td>
 *       <td>{@code jakarta.activation:jakarta.activation-api:1.2.2}</td></tr>
 * </table>
 *
 * <p>The {@code javax.*} API versions listed above are the last Jakarta EE releases that preserve
 * the {@code javax.*} package namespace (i.e. they are source-compatible with code written for
 * the removed JDK modules), making them a drop-in replacement without requiring import changes.
 * The JAXB RI ({@code com.sun.xml.bind:*}) replacements use the {@code org.glassfish.jaxb}
 * groupId and retain binary compatibility.
 *
 * @see <a href="https://openjdk.org/jeps/320">JEP 320: Remove the Java EE and CORBA Modules</a>
 * @see <a href="https://github.com/apache/maven/issues/14163">#14163</a>
 */
@Named
@Singleton
@Priority(16)
public class JakartaEeApiMigrationStrategy extends AbstractUpgradeStrategy {

    /**
     * Maps legacy groupId:artifactId to the Jakarta EE replacement coordinates.
     * Values are {@code groupId:artifactId:version} triples for the last compatible
     * Jakarta EE release that still uses the {@code javax.*} package namespace.
     */
    static final Map<String, Replacement> LEGACY_TO_JAKARTA = buildMigrationMap();

    private static Map<String, Replacement> buildMigrationMap() {
        Map<String, Replacement> map = new LinkedHashMap<>();
        // JAXB — javax.xml.bind
        map.put("javax.xml.bind:jaxb-api", new Replacement("jakarta.xml.bind", "jakarta.xml.bind-api", "2.3.3"));
        map.put("com.sun.xml.bind:jaxb-core", new Replacement("org.glassfish.jaxb", "jaxb-core", "2.3.0.1"));
        map.put("com.sun.xml.bind:jaxb-impl", new Replacement("org.glassfish.jaxb", "jaxb-runtime", "2.3.9"));
        // JAX-WS — javax.xml.ws
        map.put("javax.xml.ws:jaxws-api", new Replacement("jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3"));
        // JAX-WS annotations — javax.jws
        map.put("javax.jws:javax.jws-api", new Replacement("jakarta.jws", "jakarta.jws-api", "1.1.1"));
        // javax.annotation — from Common Annotations (JSR-250)
        map.put(
                "javax.annotation:javax.annotation-api",
                new Replacement("jakarta.annotation", "jakarta.annotation-api", "1.3.5"));
        // JavaBeans Activation Framework — javax.activation
        map.put(
                "javax.activation:activation",
                new Replacement("jakarta.activation", "jakarta.activation-api", "1.2.2"));
        map.put(
                "javax.activation:javax.activation-api",
                new Replacement("jakarta.activation", "jakarta.activation-api", "1.2.2"));
        return Map.copyOf(map);
    }

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);

        if (options.all().orElse(false)) {
            return true;
        }

        boolean noOptionsSpecified = options.all().isEmpty()
                && options.infer().isEmpty()
                && options.model().isEmpty()
                && options.plugins().isEmpty()
                && options.modelVersion().isEmpty();

        boolean allOptionsDisabled = options.all().map(v -> !v).orElse(false)
                && options.infer().map(v -> !v).orElse(false)
                && options.model().map(v -> !v).orElse(false)
                && options.plugins().map(v -> !v).orElse(false)
                && options.modelVersion().isEmpty();

        if (noOptionsSpecified || allOptionsDisabled) {
            return true;
        }

        if (options.model().isPresent()) {
            return options.model().get();
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "Migrating legacy Java EE API dependencies to Jakarta EE equivalents (JEP 320)";
    }

    @Override
    protected UpgradeResult doApply(UpgradeContext context, Map<Path, Document> pomMap) {
        Set<Path> processedPoms = new HashSet<>();
        Set<Path> modifiedPoms = new HashSet<>();
        Set<Path> errorPoms = new HashSet<>();

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();
            processedPoms.add(pomPath);

            context.info(pomPath + " (checking for legacy Java EE API dependencies)");
            context.indent();

            try {
                boolean modified = migrateLegacyJavaEeApis(pomDocument, context);

                if (modified) {
                    logChange(context, "Legacy Java EE API migration to Jakarta EE equivalents");
                    modifiedPoms.add(pomPath);
                } else {
                    context.success("No legacy Java EE API dependencies found");
                }
            } catch (Exception e) {
                context.failure("Failed to migrate Java EE API dependencies: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Scans all dependency sections in the POM and replaces legacy Java EE artifacts
     * with their Jakarta EE equivalents.
     *
     * @param pomDocument the POM document to modify
     * @param context     the upgrade context for logging
     * @return {@code true} if any dependencies were migrated
     */
    boolean migrateLegacyJavaEeApis(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();
        boolean modified = false;

        // <dependencies>
        modified |= migrateDependenciesSection(root.childElement(DEPENDENCIES).orElse(null), context);

        // <dependencyManagement><dependencies>
        modified |= root.childElement(DEPENDENCY_MANAGEMENT).stream()
                .flatMap(dm -> dm.childElement(DEPENDENCIES).stream())
                .reduce(false, (acc, deps) -> acc | migrateDependenciesSection(deps, context), Boolean::logicalOr);

        // profiles
        Element profiles = root.childElement(PROFILES).orElse(null);
        if (profiles != null) {
            for (Element profile : profiles.childElements(PROFILE).toList()) {
                modified |= migrateDependenciesSection(
                        profile.childElement(DEPENDENCIES).orElse(null), context);
                modified |= profile.childElement(DEPENDENCY_MANAGEMENT).stream()
                        .flatMap(dm -> dm.childElement(DEPENDENCIES).stream())
                        .reduce(
                                false,
                                (acc, deps) -> acc | migrateDependenciesSection(deps, context),
                                Boolean::logicalOr);
            }
        }

        // plugin-level dependencies (e.g. maven-antrun-plugin dependencies)
        Stream<Element> pluginContainers = Stream.concat(
                root.childElement(BUILD).stream()
                        .flatMap(build -> Stream.concat(
                                build.childElement(PLUGINS).stream(),
                                build.childElement(PLUGIN_MANAGEMENT).stream()
                                        .flatMap(pm -> pm.childElement(PLUGINS).stream()))),
                root.childElement(PROFILES).stream()
                        .flatMap(profs -> profs.childElements(PROFILE))
                        .flatMap(profile -> profile.childElement(BUILD).stream())
                        .flatMap(build -> Stream.concat(
                                build.childElement(PLUGINS).stream(),
                                build.childElement(PLUGIN_MANAGEMENT).stream()
                                        .flatMap(pm -> pm.childElement(PLUGINS).stream()))));

        for (Element pluginsElem : pluginContainers.toList()) {
            for (Element plugin : pluginsElem.childElements(PLUGIN).toList()) {
                modified |= migrateDependenciesSection(
                        plugin.childElement(DEPENDENCIES).orElse(null), context);
            }
        }

        return modified;
    }

    private boolean migrateDependenciesSection(Element dependenciesElement, UpgradeContext context) {
        if (dependenciesElement == null) {
            return false;
        }

        boolean modified = false;
        for (Element dependency : dependenciesElement.childElements(DEPENDENCY).toList()) {
            modified |= migrateDependency(dependency, context);
        }
        return modified;
    }

    /**
     * Checks whether a dependency element matches a legacy Java EE artifact and,
     * if so, updates its groupId and artifactId in-place.
     *
     * <p>The version element is updated only when it is explicitly declared in this
     * dependency. If the version is managed via {@code <dependencyManagement>} and
     * absent here, it is left untouched.
     *
     * @param dependency the dependency element to inspect and potentially modify
     * @param context    the upgrade context for logging
     * @return {@code true} if the dependency was migrated
     */
    boolean migrateDependency(Element dependency, UpgradeContext context) {
        String groupId = dependency.childTextTrimmed(GROUP_ID);
        String artifactId = dependency.childTextTrimmed(ARTIFACT_ID);

        if (groupId == null || artifactId == null) {
            return false;
        }

        String key = groupId + ":" + artifactId;
        Replacement replacement = LEGACY_TO_JAKARTA.get(key);
        if (replacement == null) {
            return false;
        }

        // Update groupId
        dependency.childElement(GROUP_ID).ifPresent(e -> e.textContent(replacement.groupId()));

        // Update artifactId
        dependency.childElement(ARTIFACT_ID).ifPresent(e -> e.textContent(replacement.artifactId()));

        // Update version only if it is explicitly declared in this element
        Element versionElement = dependency.childElement(VERSION).orElse(null);
        if (versionElement != null) {
            versionElement.textContent(replacement.version());
        }

        context.detail("Migrated " + key + " → " + replacement.groupId() + ":" + replacement.artifactId() + ":"
                + replacement.version());
        context.warning(key + " uses APIs that were removed from the JDK in Java 11 (JEP 320)."
                + " Replaced with " + replacement.groupId() + ":" + replacement.artifactId() + ":"
                + replacement.version()
                + " which provides the same API as a standalone dependency.");

        return true;
    }

    /**
     * Immutable record holding the Jakarta EE replacement coordinates for a legacy Java EE artifact.
     */
    record Replacement(String groupId, String artifactId, String version) {}
}
