<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# IDE Embedder SPI Gap Analysis

This document describes the gap in Maven 4's SPI for IDE embedders and the `WorkspaceReader` SPI solution.

## The Problem

Maven 4's `maven-api-spi` covers build definition extensibility (model, lifecycle, types, packaging) but has **no SPI for embedders** — IDEs running Maven in-process with workspace context. All three major IDEs override internal components via DI priority or subclassing.

### What Each IDE Overrides

- **IntelliJ**: `PluginDependenciesResolver` (retry logic on resolution failures)
- **Eclipse m2e** (historical): `PluginDependenciesResolver`, 5+ cache impls, `BuildContext`, `ClassRealmManagerDelegate`
- **NetBeans**: `PluginDependenciesResolver` (extends `Default...`)

The reason three IDEs override the same internal component: Maven 4 has no supported extension point for plugin/extension resolution.

## Missing SPI Categories

1. Plugin/extension resolution interception
2. Cache lifecycle hooks
3. ClassRealm management
4. Build context (incremental build)
5. Repository event observation

## Solution: WorkspaceReader SPI + PluginRealmCache.invalidate()

### `WorkspaceReader` SPI (Maven 4.1.0+, PR #13094, MNG-8766)

`org.apache.maven.api.spi.WorkspaceReader` in `maven-api-spi`. Uses Maven API types (`org.apache.maven.api.Artifact` / `java.nio.file.Path`), no maven-resolver-api dependency. Bridge: `SpiWorkspaceReaderAdapter` in `maven-core/resolver/`.

### `PluginRealmCache.invalidate(Artifact)`

Added as a `default` no-op on `PluginRealmCache` so existing implementations don't break. `DefaultPluginRealmCache` implements it by evicting entries whose resolved artifacts list contains a matching GAV, then disposing the realm.

## Design Pivot

An earlier design with `isApplicableForPluginResolution()` on `WorkspaceReader` was rejected — IDEs trust their own build. The right API is cache invalidation, not exclusion.

## Key Rule

Never add abstract methods to interfaces like `PluginDependenciesResolver` — use `default` methods that delegate to existing abstract methods. IntelliJ implements these interfaces directly, so new abstract methods produce `AbstractMethodError` at runtime.
