<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Maven 4 DI Scope Architecture

This document describes scope behavior in the maven-di framework, covering `@MojoExecutionScoped`, `@SessionScoped`, and threading concerns.

## Why `MojoExecutionScope` Is `ThreadLocal`-Based

The `ThreadLocal<LinkedList<ScopeState>>` has been there since MNG-5530 (2012). The motive is **parallel build isolation** (`-T N`): multiple `executeMojo()` calls run concurrently on different threads, each with its own `MojoExecution`, `MavenProject`, etc. The `ThreadLocal` gives each thread independent scope state.

`SessionScope` does not need a `ThreadLocal` — session state is shared across the entire build.

Mojo instances are NOT reused across invocations. Each `executeMojo()` call creates a new mojo instance. The `ThreadLocal` is exclusively about concurrent execution isolation.

## `@MojoExecutionScoped` in v4 Plugins: Effectively a No-Op

For v4 plugins, `DefaultMavenPluginManager` creates a **fresh local `Injector`** per execution. The `MojoExecutionScope` `ThreadLocal` is not involved at all. The mojo and every injected component have a clean, per-execution context via direct `bindInstance`. Annotating a v4 component with `@MojoExecutionScoped` is harmless but meaningless.

## `@MojoExecutionScoped` in Core Extensions: Works via Sisu/Guice

Core extensions loaded via `PlexusContainerCapsuleFactory` get the scope registered properly. A `@Singleton` extension component injecting a `@MojoExecutionScoped` type through the Sisu/Guice path gets a **Guice scope proxy** automatically.

## Two DI Annotation Namespaces

| Namespace | Purpose | Recognized by |
|---|---|---|
| `org.apache.maven.api.di.*` | Maven 4 API mojos and components | `maven-di` (`InjectorImpl`) |
| `javax.inject.*` | Sisu/Plexus components (JSR-330) | `BeanLocator` (Sisu/Guice) |

Maven 4 API mojo fields MUST use `org.apache.maven.api.di.Named` and `org.apache.maven.api.di.Inject`, never `javax.inject.*` annotations directly.

## Plugin DI Pipeline (`loadV4Mojo`)

```
DefaultMavenPluginManager.loadV4Mojo()
  → discoverComponents(pluginRealm, ...) — registers Sisu components
  → injector = SisuDiBridgeModule.newBridgeInjector(beanLocator)
  → injector.discover(pluginRealm) — reads maven-di index
  → injector.bindInstance(Session, Project, MojoExecution, Log)
  → mojo = injector.getInstance(mojoClass)
```

The `SisuDiBridgeModule` / `BridgeInjectorImpl` bridges both systems: resolves from own bindings first, falls back to `BeanLocator` for unregistered keys.

## Singleton Injection Trap in maven-di

maven-di has **no proxy generation**. When a singleton's field is injected, `binding.get()` is called **eagerly** at construction time. If the singleton is constructed before the mojo execution scope is entered, the `ThreadLocal` is empty → `DIException`.

**Rule:** in the maven-di path, do not inject `@MojoExecutionScoped` types into `@Singleton` components. Pass the value explicitly at call time, or use the Guice/Sisu extension path where the proxy is generated.

## Cross-Thread Scope Propagation Options

| Option | Approach | Recommended? |
|---|---|---|
| A | `MojoExecutionContext` wrapping `Runnable`/`Executor` | Maven 4 only, clean |
| B | Design components to not need scope | Preferred for new code |
| C | Queue work to the mojo thread | Works with all Maven versions |

For plugins supporting both Maven 3.9 and 4.x, Option B or C is the viable path.
