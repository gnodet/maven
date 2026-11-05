<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Maven 4 API Conventions

This document describes the naming and design conventions used across Maven 4 API interfaces.

## Noun-Style Accessor Pattern

Maven 4 API uses noun-style accessors (`name()`, `value()`, `children()`) for immutable value types, following modern JDK conventions since Java 8. The `getX()` JavaBeans convention is reserved for mutable components.

### Key Decisions

- **Hand-crafted API interfaces** (XmlNode, Artifact, Project, etc.): noun-style accessors as primary, `getX()` deprecated as default bridges.
- **Modello-generated model classes** (Model, Plugin, model.Dependency): keep `getX()` for now — too much ecosystem churn, records not viable (inheritance, 39+ fields, no native Builder).
- **Boolean queries** (`isSnapshot()`, `isOptional()`): keep `is` prefix — matches JDK convention.
- **Conversion methods** (`toCoordinates()`, `toString()`): keep `to` prefix.

### JDK Evidence

The noun-style pattern is established across the JDK since Java 8: `Stream.count()`, `Optional.empty()`, `Path.root()`, `Duration.toMillis()`, `Record` components, etc. See [PR #11582](https://github.com/apache/maven/pull/11582) for the comprehensive JDK 8–25 survey.

## Migration Pattern

For each `getX()` on an immutable interface:

1. Add `x()` as the new primary abstract method (copy javadoc).
2. Convert `getX()` to `default` delegating to `x()`, with `@Deprecated(since = "4.1.0", forRemoval = true)`.
3. Update all internal callers (impl classes) to use noun-style.
4. Update all **test** callers — anonymous classes, stubs, and records implementing the interface must override the new abstract noun-style methods.
5. Update **Mockito stubs** — `when(mock.getX())` must become `when(mock.x())` because Mockito stubs the specific method, and the `default getX()` delegates to `x()` which is unstubbed on the mock.
6. Run full **verify** (not just compile): `mvn verify -B -T4 -Dformatter.skip -Dimpsort.skip -Drat.skip`.

Always run `verify` (includes test compilation and execution), not just `compile`. The main sources will compile fine because `getX()` becomes a default method, but test stubs/anonymous classes that implemented the old abstract `getX()` will fail to compile because they don't implement the new abstract `x()` method.

## Migration Status

### Tier 1 (done — #13035, PR #13036)
Artifact, ArtifactCoordinates, Dependency, DependencyCoordinates, DownloadedArtifact, Exclusion, Project, VersionConstraint, VersionRange.

### Tier 2 (done)
MojoExecution — noun-style, `getX()` bridges removed (pre-GA clean break). `lifecyclePhase()` / `executionId()` return `Optional<String>`.

### Tier 2 (future)
Session, ProtoSession.

### Tier 3 (in progress)
- **Done**: `Node`, `Repository`, `RemoteRepository`, `LocalRepository`.
- **Remaining**: Toolchain, JavaToolchain, Plugin, Type, CacheMetadata.

### Tier 4 (future)
Request, Result, and ~20 service interfaces.

## `@Immutable` Annotation Convention

API interfaces whose implementations are expected to be immutable (noun-style accessors, no setters, record implementations) must carry the `@Immutable` annotation (`org.apache.maven.api.annotations.Immutable`). This signals to consumers and tooling that instances are safe to share across threads.

**Indicators an interface needs `@Immutable`:**
- Uses noun-style accessors (not `getX()`)
- Implementation is a `record`
- No mutating methods

## Javadoc `@since` Tag Convention

When a type (interface/class) is annotated with `@since X.Y.Z`, individual members introduced **in the same version** must not repeat the `@since` tag — it's redundant. Only add `@since` to members added in a **later** version than the enclosing type.

## Why Not Records for Model Objects

- `Model extends ModelBase` — records cannot extend classes.
- 39+ fields → unusable canonical constructor.
- No native Builder pattern, no `withX()` methods.
- Package-private fields vs records' forced public components.

## Accessor Naming: Scope of the Noun-style Convention

The noun-style accessor rule applies to types annotated `@Immutable` or implemented as Java `record`s. It does NOT extend to:

- **SPI provider interfaces** (`ModelDialectProvider`, `ToolchainFactory`, `PackagingProvider`, etc.) — these are component-providers, not value-holders.
- **Stateful service interfaces** (`ModelDialectManager`, `ProjectManager`, `ArtifactManager`) — these use `getX()` for query methods that may reflect mutable state.
