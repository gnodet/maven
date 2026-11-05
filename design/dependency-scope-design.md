<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Dependency Scope Design (MNG-8099, 4.2.0+)

This document describes the design of Maven 4.2.0's `api` and `implementation` dependency scopes.

## Core Principle: Additive, Not Rename

Never flip the semantics of an existing scope across model versions — 20+ years of behavior, silent runtime breakage (`ClassNotFoundException`) on model bump without migration. Always add new scopes instead.

## Rejected Approach: Flip Compile Semantics

Changing `compile` from `transitive=true` to `false` was rejected because:
- Breaks 20+ years of behavior silently.
- Bumping `modelVersion` without explicit migration causes silent runtime failures.
- `DefaultConsumerPomBuilder.hasDependencyScope()` using `!scope.isTransitive()` would strip ALL compile deps from published consumer POMs — catastrophic regression.

## Accepted Design: Purely Additive

Keep `compile` unchanged in all model versions. Introduce two new scopes for 4.2.0+:

### Scope Table

| Scope | `isTransitive` | Path Scopes | Consumer POM | Min Version |
|---|---|---|---|---|
| `compile` | true | all | `compile` | 4.0.0 |
| `runtime` | true | runtime+test | `runtime` | 4.0.0 |
| `provided` | false | compile+test | stripped | 4.0.0 |
| `test` | false | test | stripped | 4.0.0 |
| `compile-only` | false | compile | stripped | 4.1.0 |
| `test-only` | false | test-compile | stripped | 4.1.0 |
| `test-runtime` | false | test-runtime | stripped | 4.1.0 |
| `api` | true | all | `compile` | 4.2.0 |
| `implementation` | false | all (declaring project) | `runtime` | 4.2.0 |

### Consumer POM Compatibility

- `api` → `compile`: semantically identical, no information loss.
- `implementation` → `runtime`: consumers can't compile against it (correct intent); present at runtime (correct); slightly wider transitivity in mixed Maven 3/4 chains but documented and acceptable.
- Neither scope forces a consumer POM model version bump — both have clean 4.0.0 equivalents.

### `<defaultScope>` Flag — NOT Implemented

A `<defaultScope>` POM element to change what an omitted `<scope>` resolves to was discussed and decided NOT to implement in 4.2.0. Projects adopt `api`/`implementation` explicitly at their own pace.

## Model Version Gating

- `compile-only`, `test-only`, `test-runtime` → require 4.1.0 (`is41OrBeyond` flag).
- `api`, `implementation` → require 4.2.0 (`is42OrBeyond` flag, separate check block in `validate20RawDependencies`).
- `is42OrBeyond = Objects.equals(ModelBuilder.MODEL_VERSION_4_2_0, model.getModelVersion())` (exact match — 4.1.0 POMs are NOT 4.2.0).

## Model-Version Detection via `model-version.vm`

To force model version detection for scopes not representable as Modello field version ranges, inject a custom check into the generated `is_X_Y_Z()` method via the Velocity template. Helper methods check for `api`/`implementation` scope strings in dependencies and dependency management across profiles.

## Consumer POM Scope Mapping

`DefaultConsumerPomBuilder` must use an explicit allowlist for scope filtering (not `!isTransitive()`), followed by scope mapping: `api` → `compile`, `implementation` → `runtime`.

## PathScope Entries

Both `api` and `implementation` must appear on all four path scopes of the declaring project (`MAIN_COMPILE`, `MAIN_RUNTIME`, `TEST_COMPILE`, `TEST_RUNTIME`) — the declaring project needs both on its own classpath. Transitivity enforcement is handled by the resolver, not path scope omission.

## Implementation Map

- `DependencyScope.java` (`api/maven-api-core`): new `API` and `IMPLEMENTATION` enum values.
- `PathScope.java` (`api/maven-api-core`): add both to all four path scopes.
- `Maven4ScopeManagerConfiguration.java` (**two copies** in `impl/maven-impl` and `compat/maven-resolver-provider`).
- `DefaultModelValidator.java` (`impl/maven-impl`): add `is42OrBeyond` validation.
- `src/mdo/model-version.vm`: inject custom scope detection.
- `DefaultConsumerPomBuilder.java` (`impl/maven-core`): allowlist filter + scope mapping.

## Scope Transitivity Semantics

### `provided` Is NOT Transitive
`provided` deps are stripped from the consumer POM. If library A declares B as `provided`, consumers of A do NOT get B on their classpath.

### Scope Mediation
When a dep is `provided` and it transitively pulls in C (also `provided`), Maven's scope mediation gives `provided` + `provided` → `provided`. So `provided` deps of a `provided` dep propagate to the compile+test classpath of the declaring project — they just don't appear in the consumer POM.
