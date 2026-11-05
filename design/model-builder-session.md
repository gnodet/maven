<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# `ModelBuilderSession` Exception Contract Design

This document describes the design of `ModelBuilderSession` methods and the `mvnval` validation tool architecture.

## Two Orthogonal Dimensions

| | Throws on First Error | Collects All Problems |
|---|---|---|
| Raw phase (file + validateRawModel) | `build(request)` with FILE_PROJECT | `buildRaw(request)` |
| Effective phase (full pipeline) | `build(request)` with BUILD_PROJECT | _missing_ |

## The Naming Principle

The meaningful distinction is the **exception contract**, not the phase:
- `build()` — throws `ModelBuilderException` as soon as validation reports an error.
- `buildRaw()` — collects all problems, only throws when the model cannot be read at all.

A more general name would be `validate(request)`, since the intent is "assess what's wrong" rather than "produce a model for building".

## Session Isolation

Use a fresh `ModelBuilderSession` per validation job. Do not share sessions between `validate()` and `build()` operations — they are conceptually independent.

## Pipeline Stages

1. `readFileModel()` → parse XML, `validateFileModel()`
2. `readRawModel()` → file-based parent resolution, `validateRawModel()`
3. `buildBuildPom()` → consumer POM transform
4. `buildEffectiveModel()` → profile injection, inheritance, interpolation, `validateEffectiveModel()`

## Validation Tool Design: `mvnval`

### `--mode raw|effective`
- `--mode effective` (recommended default): full pipeline, parents resolved from local repository.
- `--mode raw`: file-only, no network, offline/pre-commit use case.

### `RequestType` Mapping
- `--mode raw` → `RequestType.FILE_PROJECT`
- `--mode effective` → `RequestType.BUILD_EFFECTIVE` (skips `loadFromRoot` reactor walk, session cache effective)

Do NOT use `BUILD_PROJECT` for `mvnval` — it triggers `loadFromRoot()`, which is the Maven reactor discovery walk.

### Multi-POM Validation
Create one `ModelBuilderSession` for the entire batch with `BUILD_EFFECTIVE`. The session-level `RgavCacheKey` cache avoids re-downloading the same parent POM across files.

### Bundle Validation
When a deployment bundle contains multiple inter-referencing POMs, use a `BundleWorkspaceReader` backed by a bundle index. Two-pass algorithm: scan for GA→File index, then validate with workspace reader installed.

### `ModelResolver` SPI
`ApiRunner.createSession(injectorConsumer, localRepo)` accepts a `Path` for the local repository. Passing a temp directory enables effective-model validation without touching `~/.m2`.
