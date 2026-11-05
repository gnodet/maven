<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Maven Resolver Local Repository Tracking System

This document describes the enhanced local repository manager's artifact provenance tracking.

## Tracking File Format

The `_remote.repositories` file in each artifact version directory tracks which remote repository an artifact was downloaded from:

```properties
maven-core-3.8.6.jar>central=
maven-core-3.8.6.pom>central=
```

Format: `<filename>><repository-key>=`. Empty key means locally installed.

## Repository Key Functions

| Type | Key Format | Description |
|------|-----------|-------------|
| `simple` | repo id | Basic ID matching |
| `nid` | normalized repo id | Normalized form |
| `hurl` | SHA-1 of repo URL | URL-based security |
| `nid_hurl` | id + URL hash | Combined (new default since 2.0.23) |
| `ngurk` | id + SHA-1 of URL+type+policies | Full descriptor |

Since resolver 2.0.23, the tracking default changed from `nid` to `nid_hurl` for security. This invalidates all existing tracking entries — cached artifacts appear "present but unavailable" and get re-downloaded once.

## Impact on CI

CI environments with cached `~/.m2/repository` will see mass failures after the `nid_hurl` migration because ITs using fake repos can't re-serve artifacts. Set `aether.lrm.enhanced.trackingRepositoryKeyFunction=nid` to restore old behavior for backward-compat branches.
