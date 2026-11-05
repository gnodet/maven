<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Maven Extensible Registry Architecture

This document describes the two registry families in Maven 4 and the project-extension visibility problem.

## Two Registry Families

### `maven-impl` Registries (Session-Scoped)

`DefaultPathScopeRegistry`, `DefaultProjectScopeRegistry`, `DefaultLanguageRegistry`:
- Providers injected via constructor.
- SISU resolves providers at injection time — **no project-extension visibility**.
- This is intentional: these are built-in enums with no realistic project-extension contribution.

### `maven-core` Registries (Plexus/SISU Layer)

`DefaultPackagingRegistry`, `DefaultTypeRegistry`, `DefaultLifecycleRegistry`:
- `@Singleton` (javax.inject).
- Each has a legacy fallback path that does NOT see project-extension components.

## Project-Extension Visibility Problem

SISU visibility filtering only fires on an explicit `lookup.lookupList(Provider.class)` call at resolution time. Constructor-injected `List<Provider>` resolves at singleton initialization, before any project classloader is active.

## Fix Pattern

Override `lookup(String id)` to call `lookup.lookupList(Provider.class)` first (SISU filtering applies), then fall through to built-in values, then the legacy fallback:

```java
@Override
public Optional<Packaging> lookup(String id) {
    // 1. Project-extension providers (SISU filtering applies)
    Optional<Packaging> result = lookup.lookupList(PackagingProvider.class)
        .stream().flatMap(p -> p.provides().stream())
        .filter(p -> p.id().equalsIgnoreCase(id))
        .findFirst();
    if (result.isPresent()) return result;
    // 2. Constructor-injected (session-level) providers
    result = Optional.ofNullable(values.get(id));
    if (result.isPresent()) return result;
    // 3. Legacy fallback
    return legacyLookup(id);
}
```
