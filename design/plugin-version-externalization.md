<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Default Lifecycle Plugin Version Externalization

This document describes how default lifecycle plugin versions are externalized from Java constants into POM properties for bot-driven updates.

## Problem

Maven 4.x hardcodes default lifecycle plugin versions as Java `String` constants in `AbstractLifecycleMappingProvider` (11 plugins) and `DefaultLifecycleRegistry` (2 plugins). Unlike 3.10.x, these constants have no refresh mechanism and no visibility to dependency-update bots.

## Solution

1. **POM properties** in `impl/maven-core/pom.xml` with naming convention `version.maven-<name>-plugin`.
2. **Filtered resource** `plugin-versions.properties` using Maven resource filtering.
3. **Runtime loader** `PluginVersions.java` loading properties from classpath with `ExceptionInInitializerError` fail-fast.
4. **Consumer classes** delegate to `PluginVersions.*` constants.

## Why POM Properties

- **Dependabot** scans POM `<properties>` and matches `version.maven-*-plugin` naming.
- **Renovate** has flexible matching with `regexManagers` but POM properties work out of the box.
- The naming convention is unambiguous: `groupId=org.apache.maven.plugins`, `artifactId=maven-<name>-plugin`.
