<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Maven 4 Design Documentation

This directory contains design and architecture documentation for Apache Maven 4.
Each document covers a specific subsystem or design decision area.

## Documents

| File | Description |
|------|-------------|
| [api-conventions.md](api-conventions.md) | Noun-style accessor patterns, `@Immutable` annotation rules, migration tiers, Javadoc conventions |
| [immutability-contracts.md](immutability-contracts.md) | Decision flowchart for `@Immutable` on wrapper types, snapshot pattern, `MojoExecution` case study |
| [dependency-scope-design.md](dependency-scope-design.md) | `api`/`implementation` scopes (MNG-8099, 4.2.0+): additive design, scope table, model version gating, consumer POM mapping |
| [lifecycle-dag-design.md](lifecycle-dag-design.md) | Phase tree structure, reactor-scope constraint, extension hooks, `.mvn/build.xml` consolidated config, mojo filter syntax, `LifecycleProcessor` SPI |
| [logging-architecture.md](logging-architecture.md) | `LogEvent`/`LogEventSink` rationale, MDC design axis, `BuildEnvironment` interface, JUL→SLF4J bridge recursion |
| [classloading-realms.md](classloading-realms.md) | ClassRealm/JPMS tension, three-tier realm lifecycle, per-realm import descriptors, opt-in SPI delegation, tool isolation via `m2.conf` |
| [event-listener-architecture.md](event-listener-architecture.md) | Three listener subsystems, type hierarchy, SPI auto-discovery, module-layering constraints, `ReactorTransformer` SPI |
| [di-scope-architecture.md](di-scope-architecture.md) | `@MojoExecutionScoped` behavior (v4 plugins vs extensions), `ThreadLocal` rationale, two DI namespaces, singleton injection trap |
| [version-ordering-architecture.md](version-ordering-architecture.md) | Two independent implementations (`ComparableVersion` vs `GenericVersion`), semantic differences, fix-target decision table |
| [consumer-pom-profile-activation.md](consumer-pom-profile-activation.md) | Security hardening for repository-resolved models: activation type decision table, input sandbox approach |
| [ide-embedder-spi-gap.md](ide-embedder-spi-gap.md) | Missing embedder SPI, IDE override inventory, `WorkspaceReader` SPI solution, `PluginRealmCache.invalidate()` |
| [extensible-registry-architecture.md](extensible-registry-architecture.md) | Two registry families, project-extension visibility problem, fix pattern |
| [feature-flags.md](feature-flags.md) | `Features` pattern, warning suppression properties, FATAL vs WARNING placement rules |
| [model-builder-session.md](model-builder-session.md) | Exception contract design, `mvnval` tool architecture, `RequestType` mapping, bundle validation |
| [plugin-version-externalization.md](plugin-version-externalization.md) | Externalizing default lifecycle plugin versions into POM properties for Dependabot/Renovate |
| [resolver-tracking-system.md](resolver-tracking-system.md) | Local repository artifact provenance tracking, key functions, `nid_hurl` migration impact |
| [concurrent-builder.md](concurrent-builder.md) | Parallel build step semantics, scope expansion, `isFatal` contract, null-file anti-pattern |

## Origin

These documents were extracted from the `maven-api-design` operational skill used by
[forgebot](https://github.com/gnodet) for code review and development automation.
The skill contains additional operational procedures (review checklists, build recipes,
debugging workflows) that are not included here — this directory covers only the
design and architecture documentation.
