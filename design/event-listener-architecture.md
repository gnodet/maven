<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Maven 4 Event/Listener Architecture

This document describes the three event/listener layers in Maven 4 and their design rationale.

## Three Listener Subsystems

### 1. Build Lifecycle Events (Maven 4 API — since 4.0.0)

- `Event` — base interface for all events.
- `ExecutionEvent extends Event` — typed execution event with `type()`, `project()`, `mojoExecution()`, `exception()`.
- `Listener` — `@FunctionalInterface`: single abstract `onEvent(Event)` method.
- `ExecutionListener extends Listener` — typed callbacks with `default onEvent()` dispatch switch.
- Registration: `Session.registerListener(Listener)` / `unregisterListener(Listener)`.

### 2. Repository Events (since 4.1.0, MNG-8547)

- `RepositoryEvent extends Event` — artifact/metadata operation event.
- `RepositoryEventType` — 19 values covering resolve/download/install/deploy.
- `RepositoryListener extends Listener` — typed callbacks with `default onEvent()` dispatch switch.
- Wraps Resolver types without leaking `org.eclipse.aether` through the public API.

### 3. EventSpy (Maven 3 Compat — since 3.0.2)

- `EventSpy` — untyped `onEvent(Object event)` receives all event kinds.
- Registration: SPI/DI (loaded as core extensions).

## Type Hierarchy

```
Event (@Immutable, base — session() + deprecated getX() bridges)
├── ExecutionEvent (type(), project(), mojoExecution(), exception())
└── RepositoryEvent (type(), artifact(), metadata(), path(), repository(), ...)

Listener (@FunctionalInterface, @Consumer — abstract onEvent(Event))
├── ExecutionListener (default onEvent() dispatches to typed callbacks)
└── RepositoryListener (default onEvent() dispatches to typed callbacks)
```

## Design Points

1. `Event` is the base with noun-style `session()` accessor + deprecated `getX()` bridges.
2. `Listener` keeps `@FunctionalInterface` with abstract `onEvent(Event)` — lambdas work.
3. Each sub-interface provides `default onEvent()` with dispatch switch.
4. Single registration: `Session.registerListener(Listener)` handles all types.
5. No `SessionEvent`, no `TypedListener` — unnecessary intermediate types.
6. New types use noun-style accessors — no `getX()`.

## SPI Auto-Discovery

Look up `Listener` (the base type in `maven-api-core`) directly — do not create duplicate marker interfaces in `maven-api-spi`. Any `ExecutionListener`, `RepositoryListener`, or combined listener is already a `Listener`.

## Module-Layering Constraint

`Listener` lives in `maven-api-core`; `SpiService` lives in `maven-api-spi`. Making `Listener extends SpiService` would require `maven-api-core` to import from `maven-api-spi` — a circular dependency.

For new lifecycle/session extension SPIs, put the interface in `maven-api-spi` extending both `SpiService` and `Listener`.

## `ReactorTransformer` SPI

The Maven 4 replacement for `afterProjectsRead`. A `ReactorTransformer extends SpiService` in `maven-api-spi`, not a listener callback, because:
- No session exists yet at that point.
- Checked exception contract needed for build abortion.

```java
@Experimental @Consumer @ThreadSafe
public interface ReactorTransformer extends SpiService {
    void transformReactor(@Nonnull Session session, @Nonnull List<Project> projects)
            throws MavenException;
}
```
