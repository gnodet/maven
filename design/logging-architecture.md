<!-- Extracted from Hermes forgebot skill: maven-api-design -->

# Maven 4 Logging Foundation Architecture

This document describes the structured logging architecture introduced in Maven 4.1.0 (PR chain #12694–#12714).

## Why `LogEvent`/`LogEventSink` on Top of JUL

JUL **is** the backend. The thin abstraction exists because JUL alone cannot address three Maven-specific concerns:

### 1. Session Isolation for mvnd (Strongest Argument)
mvnd runs as a daemon with multiple concurrent Maven sessions in the same JVM. A JUL `FileHandler` is global — it writes all sessions' logs to the same stream. `BuildReportCollector` is scoped to a single `MavenSession` via the `reportCapture` callback, giving each build its own isolated report.

### 2. Flush-Conditional Rendering
`RichBuildEventListener.projectLogMessage()` uses `LogEvent.level()` to suppress INFO/DEBUG/TRACE and count WARN/ERROR separately — direct enum comparison, no text parsing.

### 3. Thread-Based Log Routing
A single plugin ClassRealm is shared across multiple parallel executions (e.g., `maven-compiler-plugin` on 4 modules with `-T4`). ClassRealm-based routing mixes their logs. Thread-based routing via `ThreadLocal` MDC tied to the current mojo execution context is the correct axis.

## MDC as the Key Design Axis

If all context is in MDC (`module`, `execution`, `phase`, `result`, `duration_ms`...), a custom JUL `Formatter` serializing MDC fields covers the structured output use case. This is simpler than a full in-memory `BuildReportCollector`:

- MDC as the carrier (thread-local, populated by lifecycle events)
- Smart `Formatter` for output (logfmt or JSON)
- Thin `EventSpy` just to populate/clear MDC entries at lifecycle boundaries
- No in-memory collector; hierarchy reconstructed by consumers from grouping keys

What still requires state: the aggregated build summary (total timing, overall result, module count).

## Logfmt Integration Point

logfmt is flat (key=value); the build report has nested hierarchy. But with MDC fields, a consumer can reconstruct it by grouping. `--console=machine` mode is the right integration point for a logfmt output format.

## PR Chain

| PR | Feature |
|---|---|
| #12694 | Logging foundation: `LogEvent`, JUL handler, Log API enhancements |
| #12695 | Build report: `BuildReportCollector` (EventSpy), `BuildReportJsonWriter` |
| #12697 | Console modes: `--console=plain/rich/verbose/machine` |
| #12699 | `mvnlog` viewer |
| #12702 | Structured problems pipeline (`DiagnosticCollector`) |
| #12714 | TRACE level migration |

## BuildReport Completeness — `BuildEnvironment`

`BuildEnvironment` captures invocation parameters as a frozen snapshot at `SessionStarted`:

```java
@Experimental @Immutable
public interface BuildEnvironment {
    List<String> goals();
    Map<String, String> userProperties();    // sensitive keys → "***"
    Map<String, String> systemInfo();        // curated: os/java/maven/processors
    String localRepository();
    List<String> activeProfiles();
    List<String> selectedProjects();
    String resumeFrom();
    String reactorFailureBehavior();
    boolean offline();
    boolean updateSnapshots();
    boolean noTransferProgress();
    boolean batchMode();
    int threads();
}
```

Two access paths, one type:
- `Session.buildEnvironment()` — live, for plugins.
- `BuildReport.environment()` — frozen snapshot.

Sensitive key redaction uses a denylist on lowercase key fragments: `password`, `passwd`, `secret`, `token`, `apikey`, `api_key`, `credential`, `passphrase`.

## JUL→SLF4J Bridge Recursion

Custom JUL handlers that call `LoggerFactory.getLogger()` inside `publish()` can crash Maven with `"Recursive update"` when running with `-X`. The root cause is `ConcurrentHashMap.computeIfAbsent()` reentrancy in SLF4J's logger cache. A `ThreadLocal` guard, `putIfAbsent`, or lazy handler installation are more robust fixes.
