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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.ARTIFACT_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.BUILD;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.CONFIGURATION;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.GROUP_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGINS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.VERSION;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Plugins.DEFAULT_MAVEN_PLUGIN_GROUP_ID;

/**
 * Strategy for fixing version ranges in the {@code maven-enforcer-plugin} for Maven 4 compatibility.
 *
 * <p>Handles two types of version range issues:
 *
 * <p><strong>1. Widening {@code requireMavenVersion} ranges</strong>: many projects use version
 * ranges with an exclusive upper bound at version 4 (e.g., {@code [3.8.8,4)}, {@code [3,4)}),
 * which blocks Maven 4 builds. This strategy widens such ranges to allow Maven 4 by changing
 * the upper bound to 5 (e.g., {@code [3.8.8,4)} becomes {@code [3.8.8,5)}).
 *
 * <p><strong>2. Merging overlapping version ranges</strong>: Maven 4's version range parser rejects
 * ranges with overlapping sub-ranges (e.g., {@code [1.8,1.9),[1.8,9),[11,12)} where {@code [1.8,1.9)}
 * is contained within {@code [1.8,9)}). This strategy detects and merges overlapping sub-ranges
 * into a simplified equivalent (e.g., {@code [1.8,9),[11,12)}). This applies to both
 * {@code requireMavenVersion} and {@code requireJavaVersion} rules.
 *
 * <p>The strategy handles:
 * <ul>
 *   <li>{@code <configuration><rules><requireMavenVersion>} in plugin declarations</li>
 *   <li>{@code <configuration><rules><requireJavaVersion>} in plugin declarations</li>
 *   <li>{@code <executions><execution><configuration><rules>} in executions</li>
 *   <li>Both {@code build/plugins} and {@code build/pluginManagement/plugins} sections</li>
 *   <li>Profile-scoped enforcer plugin declarations</li>
 * </ul>
 */
@Named
@Singleton
@Priority(18)
public class EnforcerVersionRangeStrategy extends AbstractUpgradeStrategy {

    private static final String ENFORCER_ARTIFACT_ID = "maven-enforcer-plugin";

    private static final String RULES = "rules";
    private static final String REQUIRE_MAVEN_VERSION = "requireMavenVersion";
    private static final String REQUIRE_JAVA_VERSION = "requireJavaVersion";
    private static final String EXECUTIONS = "executions";
    private static final String EXECUTION = "execution";

    /**
     * Pattern to split a multi-range version string into individual sub-ranges.
     * Splits at positions where a closing bracket/paren is followed by a comma and
     * an opening bracket/paren (e.g., {@code ),[} or {@code ],[}).
     */
    private static final Pattern SUB_RANGE_SPLIT = Pattern.compile("(?<=[\\)\\]])\\s*,\\s*(?=[\\[\\(])");

    /**
     * Pattern to parse a single version range like {@code [1.8,9)} into its components.
     * Group 1: opening bracket ({@code [} or {@code (})
     * Group 2: lower bound version
     * Group 3: upper bound version
     * Group 4: closing bracket ({@code ]} or {@code )})
     */
    private static final Pattern SINGLE_RANGE = Pattern.compile("^([\\[\\(])\\s*(.+?)\\s*,\\s*(.+?)\\s*([\\]\\)])$");

    /**
     * Pattern to match Maven version ranges with an exclusive upper bound at any 4.x version.
     * Captures:
     *   Group 1: opening bracket ([ or ()
     *   Group 2: lower bound (e.g., 3.8.8, 3, 3.6.3)
     *   Group 3: upper bound starting with 4 (e.g., 4, 4.0, 4.0.0, 4.1, 4.2.1)
     *
     * Examples matched: [3.8.8,4), [3,4), (3.6.3,4), [3.8.8,4.0), [3.8.8,4.0.0), [3.8.8,4.1)
     * Examples not matched: [3.8.8,), 3.8.8, [3.8.8,5), [3.8.8,4]
     */
    static final Pattern MAVEN4_EXCLUSIVE_UPPER_BOUND = Pattern.compile("^(\\[|\\()(.+?),\\s*(4(?:\\.\\d+)*)\\s*\\)$");

    /**
     * Pattern to match version ranges where the upper bound has a major version below 4,
     * which blocks Maven 4. This includes exact version pins like {@code [3.8.6,3.8.6]}
     * and ranges like {@code [3.8.0,3.9)}, {@code (,3.9]}.
     * Captures:
     *   Group 1: opening bracket ([ or ()
     *   Group 2: lower bound (may be empty for unbounded lower ranges like {@code (,3.9]})
     *   Group 3: upper bound (numeric version like 3.8.6, 3.9, 3)
     *   Group 4: closing bracket () or ])
     *
     * Examples matched: [3.8.6,3.8.6], [3.8.0,3.9), (,3.9], [3.0,3.0], [3.9.0,3.9.0]
     * Examples not matched: [3.8.8,), 3.8.8, [3.8.8,5), [3.8.8,4) (handled by MAVEN4_EXCLUSIVE_UPPER_BOUND)
     */
    static final Pattern UPPER_BOUND_BELOW_MAVEN4 =
            Pattern.compile("^(\\[|\\()(.*?),\\s*(\\d+(?:\\.\\d+)*)\\s*(\\)|\\])$");

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);
        return isOptionEnabled(options, options.model(), true);
    }

    @Override
    public String getDescription() {
        return "Fixing enforcer version ranges for Maven 4 compatibility";
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

            context.info(pomPath + " (checking for RequireMavenVersion range restrictions)");
            context.indent();

            try {
                boolean hasUpgrades = widenEnforcerVersionRanges(pomDocument, context);

                if (hasUpgrades) {
                    modifiedPoms.add(pomPath);
                    context.success("RequireMavenVersion ranges widened to allow Maven 4");
                } else {
                    context.success("No RequireMavenVersion range restrictions found");
                }
            } catch (Exception e) {
                context.failure("Failed to widen RequireMavenVersion ranges: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Finds and widens all RequireMavenVersion ranges in the POM document.
     */
    private boolean widenEnforcerVersionRanges(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();
        boolean hasUpgrades = false;

        // Process root-level build/plugins and build/pluginManagement/plugins
        hasUpgrades |= processPluginSections(root, context);

        // Process profile-scoped plugins
        Element profiles = root.childElement(PROFILES).orElse(null);
        if (profiles != null) {
            for (Element profile : profiles.childElements(PROFILE).toList()) {
                hasUpgrades |= processPluginSections(profile, context);
            }
        }

        return hasUpgrades;
    }

    /**
     * Processes both build/plugins and build/pluginManagement/plugins sections.
     */
    private boolean processPluginSections(Element parent, UpgradeContext context) {
        Element buildElement = parent.childElement(BUILD).orElse(null);
        if (buildElement == null) {
            return false;
        }

        boolean hasUpgrades = false;

        // Check build/plugins
        Element pluginsElement = buildElement.childElement(PLUGINS).orElse(null);
        if (pluginsElement != null) {
            hasUpgrades |= processPluginsForEnforcer(pluginsElement, context);
        }

        // Check build/pluginManagement/plugins
        Element pluginManagement = buildElement.childElement(PLUGIN_MANAGEMENT).orElse(null);
        if (pluginManagement != null) {
            Element managedPlugins = pluginManagement.childElement(PLUGINS).orElse(null);
            if (managedPlugins != null) {
                hasUpgrades |= processPluginsForEnforcer(managedPlugins, context);
            }
        }

        return hasUpgrades;
    }

    /**
     * Finds enforcer plugin elements and processes their requireMavenVersion rules.
     */
    private boolean processPluginsForEnforcer(Element pluginsElement, UpgradeContext context) {
        return pluginsElement
                .childElements(PLUGIN)
                .filter(this::isEnforcerPlugin)
                .map(plugin -> processEnforcerPlugin(plugin, context))
                .reduce(false, Boolean::logicalOr);
    }

    /**
     * Checks whether a plugin element is the maven-enforcer-plugin.
     */
    private boolean isEnforcerPlugin(Element plugin) {
        String artifactId = plugin.childTextTrimmed(ARTIFACT_ID);
        if (!ENFORCER_ARTIFACT_ID.equals(artifactId)) {
            return false;
        }
        String groupId = plugin.childTextTrimmed(GROUP_ID);
        return groupId == null || groupId.isEmpty() || DEFAULT_MAVEN_PLUGIN_GROUP_ID.equals(groupId);
    }

    /**
     * Processes an enforcer plugin element, checking both top-level configuration
     * and per-execution configurations for requireMavenVersion rules.
     */
    private boolean processEnforcerPlugin(Element plugin, UpgradeContext context) {
        boolean hasUpgrades = false;

        // Check top-level <configuration><rules><requireMavenVersion>
        Element configuration = plugin.childElement(CONFIGURATION).orElse(null);
        if (configuration != null) {
            hasUpgrades |= processRulesElement(configuration, context);
        }

        // Check <executions><execution><configuration><rules><requireMavenVersion>
        Element executionsElement = plugin.childElement(EXECUTIONS).orElse(null);
        if (executionsElement != null) {
            for (Element execution : executionsElement.childElements(EXECUTION).toList()) {
                Element execConfig = execution.childElement(CONFIGURATION).orElse(null);
                if (execConfig != null) {
                    hasUpgrades |= processRulesElement(execConfig, context);
                }
            }
        }

        return hasUpgrades;
    }

    /**
     * Processes a configuration element's rules for requireMavenVersion and requireJavaVersion.
     */
    private boolean processRulesElement(Element configuration, UpgradeContext context) {
        Element rules = configuration.childElement(RULES).orElse(null);
        if (rules == null) {
            return false;
        }

        boolean hasUpgrades = false;

        // Process requireMavenVersion (widening + overlap merging)
        hasUpgrades |= processVersionRule(rules, REQUIRE_MAVEN_VERSION, context, true);

        // Process requireJavaVersion (overlap merging only)
        hasUpgrades |= processVersionRule(rules, REQUIRE_JAVA_VERSION, context, false);

        return hasUpgrades;
    }

    /**
     * Processes a version rule element, optionally widening for Maven 4 and merging overlapping ranges.
     *
     * @param rules the {@code <rules>} element
     * @param ruleName the rule element name (e.g., "requireMavenVersion" or "requireJavaVersion")
     * @param context the upgrade context for logging
     * @param widenForMaven4 whether to widen sub-ranges that block Maven 4
     * @return true if the version range was modified
     */
    private boolean processVersionRule(Element rules, String ruleName, UpgradeContext context, boolean widenForMaven4) {
        Element rule = rules.childElement(ruleName).orElse(null);
        if (rule == null) {
            return false;
        }

        Element versionElement = rule.childElement(VERSION).orElse(null);
        if (versionElement == null) {
            return false;
        }

        String versionRange = versionElement.textContentTrimmed();
        if (versionRange == null || versionRange.isEmpty()) {
            return false;
        }

        String result = versionRange;
        boolean changed = false;

        // Step 1: For requireMavenVersion, widen sub-ranges that block Maven 4
        if (widenForMaven4) {
            String widened = widenMultiRange(result);
            if (widened != null) {
                result = widened;
                changed = true;
            }
        }

        // Step 2: Merge overlapping sub-ranges (applies to both rule types)
        String merged = mergeOverlappingRanges(result);
        if (merged != null) {
            result = merged;
            changed = true;
        }

        if (changed) {
            Editor editor = new Editor(versionElement.document());
            editor.setTextContent(versionElement, result);
            context.detail("Fixed " + ruleName + " range: " + versionRange + " → " + result);
            return true;
        }

        return false;
    }

    /**
     * Widens a Maven version range that blocks Maven 4.
     *
     * <p>Handles three cases:
     * <ul>
     *   <li>Ranges with an exclusive upper bound at 4.x (e.g., {@code [3.8.8,4)} → {@code [3.8.8,5)})</li>
     *   <li>Exact version pins (e.g., {@code [3.8.6,3.8.6]} → {@code [3.8.6,5)})</li>
     *   <li>Ranges with an upper bound below 4 (e.g., {@code [3.8.0,3.9)} → {@code [3.8.0,5)})</li>
     * </ul>
     *
     * @param versionRange the version range string (e.g., "[3.8.8,4)")
     * @return the widened range (e.g., "[3.8.8,5)"), or null if no widening is needed
     */
    static String widenVersionRange(String versionRange) {
        // Check for ranges with exclusive upper bound at 4.x
        Matcher matcher = MAVEN4_EXCLUSIVE_UPPER_BOUND.matcher(versionRange);
        if (matcher.matches()) {
            String openBracket = matcher.group(1);
            String lowerBound = matcher.group(2);
            return openBracket + lowerBound + ",5)";
        }

        // Check for ranges where the upper bound's major version is below 4,
        // which block Maven 4. This catches exact version pins like [3.8.6,3.8.6]
        // and ranges like [3.8.0,3.9), (,3.9].
        Matcher belowMatcher = UPPER_BOUND_BELOW_MAVEN4.matcher(versionRange);
        if (belowMatcher.matches()) {
            String openBracket = belowMatcher.group(1);
            String lowerBound = belowMatcher.group(2);
            String upperBound = belowMatcher.group(3);
            if (getMajorVersion(upperBound) < 4) {
                return openBracket + lowerBound + ",5)";
            }
        }

        return null;
    }

    /**
     * Extracts the major version number from a version string.
     *
     * @param version the version string (e.g., "3.8.6", "3", "4.0.0")
     * @return the major version number, or {@link Integer#MAX_VALUE} if unparseable
     */
    private static int getMajorVersion(String version) {
        try {
            return Integer.parseInt(version.split("\\.")[0]);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Widens version ranges in a multi-range string. Splits the string into individual
     * sub-ranges, applies {@link #widenVersionRange(String)} to each, and reassembles.
     *
     * @param multiRangeStr the version range string, possibly containing multiple sub-ranges
     * @return the widened string, or null if no widening was needed
     */
    private String widenMultiRange(String multiRangeStr) {
        String[] subRanges = SUB_RANGE_SPLIT.split(multiRangeStr);
        boolean changed = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < subRanges.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            String trimmed = subRanges[i].trim();
            String widened = widenVersionRange(trimmed);
            if (widened != null) {
                sb.append(widened);
                changed = true;
            } else {
                sb.append(trimmed);
            }
        }
        return changed ? sb.toString() : null;
    }

    /**
     * Merges overlapping sub-ranges in a multi-range version string.
     *
     * <p>Maven 4's version range parser rejects ranges with overlapping sub-ranges.
     * For example, {@code [1.8,1.9),[1.8,9),[11,12)} contains the sub-range
     * {@code [1.8,1.9)} which is fully contained within {@code [1.8,9)}.
     * This method merges such overlapping ranges into a simplified equivalent:
     * {@code [1.8,9),[11,12)}.
     *
     * <p>The algorithm uses the classic interval merge approach:
     * <ol>
     *   <li>Split the string into individual sub-ranges</li>
     *   <li>Sort by lower bound</li>
     *   <li>Merge ranges whose bounds overlap</li>
     *   <li>Reconstruct the string</li>
     * </ol>
     *
     * @param multiRangeStr the version range string (e.g., "[1.8,1.9),[1.8,9),[11,12)")
     * @return the merged string (e.g., "[1.8,9),[11,12)"), or null if no merging was needed
     */
    static String mergeOverlappingRanges(String multiRangeStr) {
        String[] parts = SUB_RANGE_SPLIT.split(multiRangeStr);
        if (parts.length <= 1) {
            return null; // single range, no overlap possible
        }

        // Parse each sub-range
        List<ParsedSubRange> ranges = new ArrayList<>();
        for (String part : parts) {
            ParsedSubRange range = parseSubRange(part.trim());
            if (range == null) {
                return null; // unparseable, bail out
            }
            if (range.lower.isEmpty() || range.upper.isEmpty()) {
                return null; // unbounded range, don't attempt merge
            }
            ranges.add(range);
        }

        // Sort by lower bound, then by inclusivity (inclusive first)
        ranges.sort(Comparator.<ParsedSubRange, String>comparing(
                        r -> r.lower, EnforcerVersionRangeStrategy::compareVersions)
                .thenComparing(r -> !r.lowerInclusive));

        // Interval merge
        List<ParsedSubRange> merged = new ArrayList<>();
        merged.add(ranges.get(0));
        for (int i = 1; i < ranges.size(); i++) {
            ParsedSubRange prev = merged.get(merged.size() - 1);
            ParsedSubRange curr = ranges.get(i);

            if (rangesOverlap(prev, curr)) {
                merged.set(merged.size() - 1, mergeRanges(prev, curr));
            } else {
                merged.add(curr);
            }
        }

        // If count didn't change, no overlaps were found
        if (merged.size() == ranges.size()) {
            return null;
        }

        // Reconstruct
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < merged.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(merged.get(i).toRangeString());
        }
        return sb.toString();
    }

    /**
     * Checks whether two sorted ranges overlap. Assumes {@code prev.lower <= curr.lower}.
     */
    private static boolean rangesOverlap(ParsedSubRange prev, ParsedSubRange curr) {
        int cmp = compareVersions(prev.upper, curr.lower);
        if (cmp > 0) {
            return true; // prev's upper is past curr's lower — definitely overlap
        }
        if (cmp < 0) {
            return false; // prev ends before curr starts — no overlap
        }
        // Versions are equal: overlap only if the boundary is included by both
        // e.g., [1,2] and [2,3) overlap at 2; [1,2) and [2,3) do NOT overlap
        return prev.upperInclusive && curr.lowerInclusive;
    }

    /**
     * Merges two overlapping ranges. Takes the lower bound of {@code prev}
     * (since ranges are sorted) and the maximum upper bound.
     */
    private static ParsedSubRange mergeRanges(ParsedSubRange prev, ParsedSubRange curr) {
        // Upper bound: take the maximum
        int upperCmp = compareVersions(prev.upper, curr.upper);
        String newUpper;
        boolean newUpperInclusive;
        if (upperCmp > 0) {
            newUpper = prev.upper;
            newUpperInclusive = prev.upperInclusive;
        } else if (upperCmp < 0) {
            newUpper = curr.upper;
            newUpperInclusive = curr.upperInclusive;
        } else {
            // Equal: inclusive wins
            newUpper = prev.upper;
            newUpperInclusive = prev.upperInclusive || curr.upperInclusive;
        }
        return new ParsedSubRange(prev.lower, prev.lowerInclusive, newUpper, newUpperInclusive);
    }

    /**
     * Parses a single version range string like {@code [1.8,9)} into its components.
     *
     * @param range the range string
     * @return the parsed range, or null if unparseable
     */
    static ParsedSubRange parseSubRange(String range) {
        Matcher m = SINGLE_RANGE.matcher(range);
        if (!m.matches()) {
            return null;
        }
        return new ParsedSubRange(
                m.group(2), "[".equals(m.group(1)),
                m.group(3), "]".equals(m.group(4)));
    }

    /**
     * Compares two version strings numerically, segment by segment.
     * Each version is split on dots and segments are compared as integers.
     * Missing segments are treated as zero (so {@code "1.8" equals "1.8.0"}).
     *
     * @return negative, zero, or positive as {@code v1} is less than, equal to, or greater than {@code v2}
     */
    static int compareVersions(String v1, String v2) {
        int[] s1 = parseVersionSegments(v1);
        int[] s2 = parseVersionSegments(v2);
        int len = Math.max(s1.length, s2.length);
        for (int i = 0; i < len; i++) {
            int a = i < s1.length ? s1[i] : 0;
            int b = i < s2.length ? s2[i] : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return 0;
    }

    /**
     * Parses a version string into an array of integer segments.
     */
    private static int[] parseVersionSegments(String version) {
        return Arrays.stream(version.split("\\."))
                .mapToInt(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .toArray();
    }

    /**
     * A parsed version sub-range with lower/upper bounds and inclusivity flags.
     */
    record ParsedSubRange(String lower, boolean lowerInclusive, String upper, boolean upperInclusive) {
        String toRangeString() {
            return (lowerInclusive ? "[" : "(") + lower + "," + upper + (upperInclusive ? "]" : ")");
        }
    }
}
