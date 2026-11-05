<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Maven Version Ordering Architecture

This document describes the two independent version ordering implementations in Maven and their key differences.

## Two Separate Implementations

### 1. Compat API — `ComparableVersion` (maven-artifact)
- Location: `compat/maven-artifact/`
- Used by: `DefaultArtifactVersion`, `VersionRange` (compat API)
- Complex item model with `IntegerItem`, `StringItem`, `ListItem`, `CombinationItem`
- This is the "Maven version ordering" described in most docs
- **The `impl/` tree has zero usage** — purely a compat concern in Maven 4

### 2. Maven 4 Main API — `GenericVersion` / `GenericVersionScheme` (maven-resolver)
- Location: **apache/maven-resolver** (not apache/maven)
- `MavenVersionScheme` in `impl/maven-impl` is a thin `@Singleton` DI wrapper, 100% delegates to `GenericVersionScheme`
- Simpler item model (no `CombinationItem`)

## Key Semantic Difference

Unknown string qualifiers (e.g., `x`) sort **below** numeric versions in `GenericVersion` (`x < 0-alpha < 0`), but **above** `ga` in `ComparableVersion` (`0-alpha < 0 < x`).

| Version | GenericVersion | ComparableVersion (fixed) |
|---------|---------------|---------------------------|
| `x` position | pre-release (`x < 0-alpha`) | post-release (`x > 0`) |
| Order | `x < 0-alpha < 0` | `0-alpha < 0 < x` |

## Bug Scope

A bug fix in `ComparableVersion` (compat) does NOT fix the same bug in Maven 4's main API. If the same bug exists in `GenericVersion`, the fix must go to **apache/maven-resolver**.

## `compat/ComparableVersion` vs Maven 3.10.x

These are **NOT** the same file:
- `maven-3.10.x`: 718 lines, the classic Maven 3 algorithm.
- `compat/maven-artifact/` on master: 928 lines, substantially rewritten with `CombinationItem`.

The MNG-6568 cycle bug does NOT exist in Maven 3.10.x — it is a regression introduced by the compat rewrite.

## Decision Table

| Symptom | Fix target |
|---------|------------|
| `ComparableVersion` ordering bug | apache/maven (compat module) |
| `GenericVersion` ordering bug | **apache/maven-resolver** |
| Both affected | File in both repos separately |
