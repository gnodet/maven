<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Classloading, ClassRealm, and JPMS Architecture

This document describes Maven's classloader architecture, the ClassRealm/JPMS design, and the planned classloader improvements.

## ClassRealm / JPMS Design Tension

`ClassRealm` (`org.codehaus.plexus.classworlds.realm.ClassRealm`) extends `URLClassLoader`, conflating two concerns:
- **Loading policy**: import/export graph, strategy, JPMS layer configuration.
- **ClassLoader identity**: the actual object code is loaded from.

For modular realms created with `ModuleLayer.defineModulesWithOneLoader()`, the effective ClassLoader is owned by the `ModuleLayer`, not by the `URLClassLoader` shell. The canonical fix: override `getClassLoader()` to return the module layer's loader when modular.

```java
@Override
public ClassLoader getClassLoader() {
    if (isModular()) {
        return moduleLayer.modules().iterator().next().getClassLoader();
    }
    return this;
}
```

## Three-Tier Realm Lifecycle

| Tier | Source | Trigger | Export Target |
|---|---|---|---|
| Core extensions | `.mvn/extensions.xml` | Reactor bootstrap | `CoreExports` (reactor-global, frozen at bootstrap) |
| Project extensions | `project/build/extensions` | Project build start | Project realm only |
| Plugin realms | First mojo execution | Lazy, per-execution | None — independent |

**`CoreExports` is frozen at bootstrap.** The only way to get a package into `CoreExports` is to be in `.mvn/extensions.xml`.

## Classloader Architecture — Design Plan

### Layer 1: Per-Realm Import Descriptor

Extend `META-INF/maven/extension.xml`:

```xml
<extension>
  <exportedPackages>...</exportedPackages>
  <importedPackages>
    <importedPackage>org.slf4j</importedPackage>
  </importedPackages>
  <importedArtifacts>
    <importedArtifact>com.google.inject:guice</importedArtifact>
  </importedArtifacts>
  <classLoaderStrategy>self-first</classLoaderStrategy>
</extension>
```

`importedPackages` restricted against `CoreExports` — only packages already exported by a core extension are importable.

### Layer 2: Non-JPMS Module Access

```xml
<extension>
  <moduleAccess>
    <addOpens>java.base/sun.nio.ch</addOpens>
    <addExports>jdk.internal.misc/jdk.internal.misc</addExports>
  </moduleAccess>
</extension>
```

Targeted opens/exports to the realm's unnamed module — not `ALL-UNNAMED`.

### Layer 3: Fix `maven.ext.class.path`

Fix path dedup and descriptor discovery in `PlexusContainerCapsuleFactory` (not the deprecated `MavenCli`).

## Opt-In SPI Delegation

Replaces the dummy-plugin pattern (e.g., Tycho ships a plugin solely to export SPI packages). Instead, bootstrap extensions can explicitly delegate export authority to specific dependency JARs:

```xml
<extension>
  <exportDelegates>
    <artifact>org.eclipse.tycho:tycho-spi</artifact>
  </exportDelegates>
</extension>
```

Export authority must flow from the explicit declaration — never auto-discover from transitive deps.

## OSGi Reference Model

**Take from OSGi:** package-centric wiring, symmetric export, declared visibility.
**Leave behind:** full SAT resolver, peer-to-peer plugin wiring, version range wiring on packages.

## `classLoaderStrategy` Design Tension

`.mvn/extensions.xml` `classLoadingStrategy` wins over `extension.xml` `classLoaderStrategy` (operator override > artifact default). The artifact author provides a hint/default; the project operator can override it.

## Tool Dependency Isolation via `m2.conf` Realms

Named realm sections in `m2.conf` isolate tool-specific dependencies (e.g., `domtrip` for `mvnup`) from the build engine classpath. Each tool gets its own `[plexus.mvnX]` section with targeted `import` lines, keeping tool jars invisible to `plexus.core`.
