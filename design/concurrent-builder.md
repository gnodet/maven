<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Concurrent Builder Internals

This document describes the design of Maven's concurrent (parallel) build executor.

## Key Classes

- `BuildPlanExecutor` — orchestrates parallel build steps.
- `MojoExecutor` (concurrent variant, `@Named("concurrent")`) — extends base `MojoExecutor`, only overrides `useProjectLock() → false`.
- `MavenSession.currentProject` — already a `ThreadLocal<MavenProject>`, NOT racy between threads.

## `after:*` Step Semantics (try/finally)

The `after:*` steps follow try/finally semantics: if `before:compile` executed, then `after:compile` MUST execute regardless of whether the `compile` phase step succeeded, failed, or was skipped.

## Scope Expansion — Single Source of Truth

`MojoExecutor.toScopes(String)` is the canonical scope expansion method. `BuildPlanExecutor` should call it, NOT duplicate the logic.

Two distinct use cases with different semantics:
- **Dependency resolution**: "which artifact scopes to collect/resolve?"
- **Build ordering** (`filterByScope`): "which upstream reactor projects must be built first?"

These happen to produce similar results but are conceptually different.

## `isFatal` Contract

`isFatal(List<Throwable> failures)` returns `true` when ANY failure is a `RuntimeException` or non-`Exception` (`Error`). Both halt the build. Checked exceptions are soft failures — they record the project as failed but do not halt the reactor.

## Null-File Fallbacks — Anti-Pattern

When `artifact.getFile()` returns null during a concurrent build, the root cause is almost always incorrect build ordering. Do not add fallback mechanisms (output-dir fallback, GAV-based reactor lookup, local-repo file lookup) — fix the ordering constraints instead.
