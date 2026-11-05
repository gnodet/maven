<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Maven 4 Lifecycle DAG Design

This document describes the phase tree structure, extension hooks, and design rules for Maven 4's lifecycle DAG.

## The Phase Tree (Not a Flat List)

The Maven 4 `default` lifecycle is a deep tree of nested `Lifecycle.Phase` objects:

```
all
  each
    validate → initialize
    build (after validate): sources, resources, compile, ready, package
    verify (after validate):
      unit-test:
        test-sources, test-resources, test-compile
        test (after test-compile, after test-resources)
      integration-test
    install (after package)
    deploy (after package)
```

Key distinctions:
- `phases()` returns only the single top-level phase (`all`). Never use this to look for leaf phases.
- `allPhases()` is recursive — streams all phases at all nesting levels.
- `computePhases(lifecycle)` operates on `v3phases()` and returns the topological sort order.
- `v3phases()` is a separate flat-ish representation for Maven 3-compatible builders.

## Reactor-Scope Constraint

The lifecycle DAG is **reactor-level**, resolved once before any project builds. Individual subproject POMs cannot add phases because:
- Other modules don't know about them.
- The concurrent builder schedules across the whole reactor using the shared DAG.
- Phase order must be consistent or inter-project dependencies break.

All user-facing phase customization must be **reactor-scoped** (`.mvn/` directory or core extensions).

## Three-Layer Architecture

| Layer | Tool | Responsibility |
|---|---|---|
| Wrapper | mvx (Go binary) | Aliases, tool management, environment bootstrap |
| Extension | `.mvn/extensions.xml` + Java jar | Custom phases in the DAG, mojo filtering/skipping |
| Core | maven-core PRs | `--skip-phases` CLI, lifecycle DAG fixes |

## Extension Hook: Custom Phases

To add phases to the reactor DAG from an extension, implement `javax.inject.Provider<Lifecycle>` (the `AbstractLifecycleProvider` base class) and register it as a Sisu component. `DefaultLifecycleRegistry` picks it up automatically. The extension jar must be in `.mvn/extensions.xml` (core extension), not a plugin dependency, so it loads before the DAG is built.

## DAG Rules

### Dependency Direction
`INSTALL` and `DEPLOY` must declare `after(VERIFY)`, NOT `after(PACKAGE)`. The concurrent builder uses the declared DAG literally. `VERIFY` already transitively covers `PACKAGE`.

### Phase Skipping: Mojo Inhibition, Not Graph Surgery
Skipped phases stay in the DAG; only the mojo executions bound to those phases are suppressed. This mirrors what `-DskipTests` does for Surefire but as a first-class feature.

### Phase Injection Rules
- The `parent` attribute specifies which container node receives the new phase as a child.
- `after`/`before` anchors are **sibling names within the specified parent's children list**.
- Only emit `Kind.AFTER` links — NEVER `Kind.BEFORE` (creates cycles).
- The injected phase's `AFTER` link should point to its **immediate left sibling** in the final list.

## `.mvn/build.xml` — Consolidated Reactor Configuration

A proposed consolidated configuration file that absorbs `maven.config` and `extensions.xml`:

```xml
<build>
  <options>-B -Dmaven.test.failure.ignore=false</options>
  <extensions>
    <extension>
      <groupId>io.takari.polyglot</groupId>
      <artifactId>polyglot-scala</artifactId>
      <version>0.4.6</version>
    </extension>
  </extensions>
  <aliases>
    <alias name="quick">install -DskipTests --skip-phases=verify</alias>
    <alias name="ci">verify -B</alias>
  </aliases>
  <phases>
    <phase name="generate-proto" parent="build" after="sources"/>
  </phases>
  <filters>
    <skip>:maven-enforcer-plugin</skip>
  </filters>
</build>
```

Processing is split between pre-DI (options, extensions, aliases) and post-DI (phases, filters).

## Mojo Execution Filter

The `maven.lifecycle.filter` user property supports pattern-based mojo skipping:

| Pattern | Meaning |
|---|---|
| `*` | All executions |
| `:A` | By artifactId |
| `G:A` | Exact groupId:artifactId |
| `P` | Plugin prefix |
| `P:v:g@e` | Prefix + version + goal + executionId |
| `phase(name)` | All mojos bound to a phase |

The filter applies at **execution time** in `MojoExecutor`, not at plan calculation time.

## `LifecycleProcessor` SPI

The chosen SPI for lifecycle transformation:

```java
@Experimental @Consumer @Named
public interface LifecycleProcessor extends SpiService {
    @Nonnull
    Lifecycle process(@Nonnull Lifecycle lifecycle);
}
```

Processors chain: each receives the output of the previous one. They are contributed by extensions or reactor configuration.

## Packaging Lifecycle Bindings vs Reactor-Level Phases

Two distinct mechanisms:
- **`LifecycleMapping`**: maps phases → plugin goals for a packaging type. For plugin/tooling authors.
- **`<phases>` in `.mvn/build.xml`**: injects nodes into the reactor DAG. For project maintainers.
