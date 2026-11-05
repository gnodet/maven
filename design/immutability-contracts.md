<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Immutability Contracts for API Wrapper Types

This document describes the decision process for applying `@Immutable` to Maven 4 API wrapper types that back mutable legacy delegates.

## The `@Immutable` Annotation

`@Immutable` (from `maven-api-annotations`) implies `@ThreadSafe` and means **no field can change after construction**. It is a hard, verifiable contract — not a soft "probably stable by the time you see it" claim.

## Decision Flowchart

```
Does the interface have a mutable legacy delegate?
├─ No  → evaluate the impl directly → apply @Immutable if all fields are final
└─ Yes → Is the delegate fully configured BEFORE the API wrapper is ever constructed?
         ├─ No  → DO NOT add @Immutable. The annotation would be a lie.
         └─ Yes → Two options:
                  A. Leave as-is (annotation omitted, contract undocumented)
                  B. Snapshot option: copy all exposed state at construction time,
                     store as final fields, drop getDelegate(). Enables @Immutable.
```

## Anatomy Check: Count Instantiation Sites First

Before deciding on option B (snapshot), grep all production instantiation sites. Verify:
- **When** in the lifecycle are they created? (plan calculation vs. execution time)
- **How many** per build? (determines if copy cost matters — typically trivial)
- **Who holds the delegate after?** (`getDelegate()` callers — can they be removed?)

## Snapshot Implementation Pattern (Option B)

Structure the impl as a value object with all-final fields and static factory helpers:

```java
@Immutable
public class DefaultMojoExecution implements MojoExecution {
    private final Plugin plugin;
    private final PluginExecution model;
    private final MojoDescriptor descriptor;
    private final Optional<String> executionId;
    private final String goal;
    private final Optional<String> lifecyclePhase;
    private final XmlNode configuration;

    public DefaultMojoExecution(InternalMavenSession session,
                                 o.a.m.plugin.MojoExecution delegate) {
        // Snapshot all values at construction time
        this.descriptor    = delegate.getMojoDescriptor().getMojoDescriptorV4();
        this.executionId   = Optional.ofNullable(delegate.getExecutionId());
        this.goal          = delegate.getGoal();
        this.lifecyclePhase= Optional.ofNullable(delegate.getLifecyclePhase());
        this.configuration = delegate.getConfiguration() != null
                             ? delegate.getConfiguration().getDom() : null;
        this.plugin = buildPlugin(session, delegate);
        this.model  = buildModel(delegate);
    }
}
```

Key properties:
- **`getDelegate()` is removed** — no live link, nothing to expose.
- **Configuration stored as `XmlNode`** (nullable), exposed as `Optional<XmlNode>` via the accessor.
- **Anonymous inner classes** capture values as local `final`-equivalent variables — the compiler enforces the snapshot nature.

## Case Study: `MojoExecution`

`DefaultMojoExecution` wraps `o.a.maven.plugin.MojoExecution` (mutable delegate with `setConfiguration()`, `setLifecyclePhase()`, etc.). The delegate is fully baked by execution time, and only 3 production sites instantiate the wrapper. Option B was applied successfully.

Additional changes made during the snapshot migration:
- `Plugin.getModel()` and `Plugin.getArtifact()` changed from `@Nonnull` to `@Nullable` (CLI-invoked goals produce null).
- `lifecyclePhase()` and `executionId()` changed to `Optional<String>` (CLI-invoked goals have neither).

## Guidelines

- When a field in an `@Immutable` snapshot is **legitimately absent** for a defined subset of inputs, prefer `Optional<T>` over `@Nullable T` for the noun-accessor. Reserve `@Nullable` for legacy `getX()` bridge methods only.
- When changing `@Nonnull` to `@Nullable`, audit all callers before accepting the change.
- Bridge method removal is only safe **before** a GA release ships.
