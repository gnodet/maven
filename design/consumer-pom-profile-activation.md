<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Consumer POM Profile Activation Design

This document describes the security hardening design for profile activation in repository-resolved models.

## Core Principle

Profiles in published POMs must only activate based on facts stable across all consumer environments — not the publisher's build machine state. Consumer `-D` flags and file system state must not activate dependency profiles.

## Activation Type Decision Table

| Type | Consumer POM | Reason |
|---|---|---|
| `activeByDefault` | ✅ Re-activate | Publisher's explicit intent |
| `jdk` | ✅ Re-activate | Platform fact, universal semantics |
| `os` | ✅ Re-activate | Platform fact, universal semantics |
| `file` | ❌ Strip (pre-filter) | Publisher-local paths, meaningless on consumer machine |
| `property` on POM `<properties>` | ✅ Re-activate | Part of artifact's published identity |
| `property` on consumer `-D` flag | ❌ Suppress | Consumer injection has no transitive semantics |
| `property` on system prop | ✅ Re-activate | Platform fact |

## Correct Approach: Input Sandbox

Don't filter profiles by activation TYPE — evaluate all profiles against a **restricted context**. File-activated profiles still get pre-filtered separately.

The restricted context:
- `getSystemProperties()` — **keep** (JDK + OS activators read from context).
- `getUserProperties()` — **empty** (blocks consumer `-D` injection).
- `getProjectProperties()` / `getModelProperty()` — **keep** (POM-declared properties are part of the artifact's identity).
- File profiles — **pre-filter** before passing to selector.

## Two Stacks

The fix must apply to both:
1. **Legacy/compat stack** — `compat/maven-model-builder/`
2. **New Maven 4 stack** — `impl/maven-impl/`

## `PropertyProfileActivator` Change

Add `getProjectProperties()` / `getModelProperty()` as a third fallback after user and system properties. This ensures POM-declared properties activate profiles correctly even when user properties are suppressed.

## Repository Stripping (Separate Concern)

Stripping repositories from all surviving active profiles (commit `03c947d`) is a separate bug from the activation issue. Once activation is correctly sandboxed, `withoutRepositories()` should be removed — if a profile fires through the sandbox, its repos are part of the artifact's published identity.
