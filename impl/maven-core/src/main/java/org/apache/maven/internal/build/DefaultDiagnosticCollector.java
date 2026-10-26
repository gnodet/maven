/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.internal.build;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.apache.maven.api.services.BuilderProblem;

import static java.util.Objects.requireNonNull;

/**
 * Thread-safe collector for {@link BuilderProblem}s with deduplication support.
 * <p>
 * Problems with a non-null {@link BuilderProblem#getKey()} are deduplicated:
 * the first occurrence is stored, subsequent duplicates only increment the
 * counter. Problems without a key are always stored (up to the cap).
 * <p>
 * This implementation is safe for use from parallel module builds
 * ({@code -T}) and from any thread within a plugin execution.
 *
 * @since 4.1.0
 */
@Named
@Singleton
public final class DefaultDiagnosticCollector {

    /**
     * Maximum number of unique problems to store.
     * Protects against runaway plugins that produce unbounded problems.
     */
    static final int MAX_DIAGNOSTICS = 1000;

    /**
     * Preserves insertion order: key → first problem.
     * Using ConcurrentHashMap for thread safety; insertion order is tracked
     * separately in {@link #orderedKeys}.
     */
    private final Map<String, BuilderProblem> uniqueProblems = new ConcurrentHashMap<>();

    /** Counts per key (including the first occurrence). */
    private final Map<String, LongAdder> counts = new ConcurrentHashMap<>();

    /**
     * Insertion-order tracking. Access must be guarded by {@code synchronized (orderedKeys)}.
     * The key list mirrors {@link #uniqueProblems} keys in insertion order (up to the cap).
     */
    private final List<String> orderedKeys = new ArrayList<>();

    /**
     * Atomic counter for the number of entries added to {@link #orderedKeys}.
     * Used to make cap enforcement atomic with the {@code orderedKeys.add()} call
     * without holding the {@code orderedKeys} lock during the {@link #uniqueProblems}
     * insertion — preserving {@code @ThreadSafe} guarantees.
     */
    private final AtomicInteger orderedSize = new AtomicInteger();

    /** Counter for problems without a key, used to generate synthetic keys. */
    private final AtomicLong noKeyCounter = new AtomicLong();

    /**
     * Keys to suppress. Problems with a key in this set are silently dropped.
     * Configured via {@link #setSuppressedKeys(Set)}, typically from the
     * {@code maven.diagnostic.suppress} user property.
     */
    private volatile Set<String> suppressedKeys = Set.of();

    /**
     * Sets the keys to suppress. Problems with a matching key will be
     * silently dropped from {@link #report(BuilderProblem)}.
     * <p>
     * Supports both exact keys ({@code "deprecated-source-target"}) and
     * prefix matching with wildcard ({@code "auto:*"} to suppress all
     * auto-collected warnings from Maven 3 plugins).
     *
     * @param keys the set of keys to suppress; must not be null
     */
    public void setSuppressedKeys(Set<String> keys) {
        this.suppressedKeys = Set.copyOf(requireNonNull(keys, "keys"));
    }

    /**
     * Reports a problem.
     * <p>
     * If the problem has a non-null {@link BuilderProblem#getKey()} and a
     * problem with the same key has already been reported, the duplicate is
     * counted but not stored again.
     *
     * @param problem the problem to report
     */
    public void report(BuilderProblem problem) {
        requireNonNull(problem, "problem");
        String key = problem.getKey();

        // Problems without a key get a synthetic key for storage
        if (key == null) {
            key = "__no_key__" + noKeyCounter.incrementAndGet();
        }

        // Check suppression: exact match or prefix wildcard (e.g. "auto:*")
        if (isSuppressed(key)) {
            return;
        }

        // Store the first occurrence only (if within cap).
        //
        // Concurrency contract: the counts entry must be visible to any thread
        // that observes the key in uniqueProblems (the else branch below).
        // To guarantee this, we create the counter BEFORE publishing the key via
        // putIfAbsent. If we later discover we are over cap and undo the insertion,
        // we also remove the pre-created counter.
        //
        // Timeline (Thread A = first occurrence, Thread B = duplicate):
        //   1. A: computeIfAbsent → counter created in counts
        //   2. A: putIfAbsent    → key published in uniqueProblems
        //   3. B: putIfAbsent    → sees existing key, takes else branch
        //   4. B: counts.get     → guaranteed non-null (created in step 1)
        //   5. A: synchronized   → cap check, add to orderedKeys, increment counter
        // Without step 1 before step 2, step 4 could return null and B's increment
        // would be silently lost.
        LongAdder counter = counts.computeIfAbsent(key, k -> new LongAdder());
        if (uniqueProblems.putIfAbsent(key, problem) == null) {
            synchronized (orderedKeys) {
                if (orderedSize.incrementAndGet() <= MAX_DIAGNOSTICS) {
                    orderedKeys.add(key);
                    // Count only problems that made it into the cap
                    counter.increment();
                } else {
                    // Over cap — undo the reservation and the map insertion;
                    // also remove the pre-created counter so it doesn't leak.
                    orderedSize.decrementAndGet();
                    uniqueProblems.remove(key);
                    counts.remove(key);
                }
            }
        } else {
            // Duplicate key: increment count only if the key is within the cap
            // (i.e. it was already stored). Over-cap keys have no counts entry
            // because their counter was removed during the undo above.
            LongAdder existing = counts.get(key);
            if (existing != null) {
                existing.increment();
            }
        }
    }

    private boolean isSuppressed(String key) {
        Set<String> suppressed = this.suppressedKeys;
        if (suppressed.isEmpty()) {
            return false;
        }
        if (suppressed.contains(key)) {
            return true;
        }
        // Check prefix wildcards: "auto:*" matches "auto:Compiler:1a2b3c"
        for (String pattern : suppressed) {
            if (pattern.endsWith("*") && key.startsWith(pattern.substring(0, pattern.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all unique problems reported so far, in the order they
     * were first reported.
     *
     * @return an unmodifiable list of unique problems, never {@code null}
     */
    public List<BuilderProblem> getProblems() {
        List<BuilderProblem> result;
        synchronized (orderedKeys) {
            result = new ArrayList<>(orderedKeys.size());
            for (String key : orderedKeys) {
                BuilderProblem p = uniqueProblems.get(key);
                if (p != null) {
                    result.add(p);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns a deduplicated summary of all reported problems.
     * Each entry contains the problem and the number of times it was
     * reported (across all modules).
     *
     * @return an unmodifiable list of summaries, never {@code null}
     */
    public List<DefaultDiagnosticSummary> getSummary() {
        List<DefaultDiagnosticSummary> result;
        synchronized (orderedKeys) {
            result = new ArrayList<>(orderedKeys.size());
            for (String key : orderedKeys) {
                BuilderProblem p = uniqueProblems.get(key);
                LongAdder counter = counts.get(key);
                if (p != null && counter != null) {
                    result.add(new DefaultDiagnosticSummary(p, counter.intValue()));
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns {@code true} if at least one problem with severity
     * {@link BuilderProblem.Severity#WARNING WARNING} or higher has been reported.
     */
    public boolean hasWarnings() {
        return hasAtSeverity(BuilderProblem.Severity.WARNING);
    }

    /**
     * Returns {@code true} if at least one problem with severity
     * {@link BuilderProblem.Severity#ERROR ERROR} has been reported.
     */
    public boolean hasErrors() {
        return hasAtSeverity(BuilderProblem.Severity.ERROR);
    }

    private boolean hasAtSeverity(BuilderProblem.Severity targetSeverity) {
        for (BuilderProblem p : uniqueProblems.values()) {
            // Severity enum is ordered most severe first: FATAL, ERROR, WARNING, INFO
            // A problem "has" the target severity if its ordinal is <= target ordinal
            if (p.getSeverity().ordinal() <= targetSeverity.ordinal()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears all collected diagnostics, counts and ordering state.
     * <p>
     * Must be called at the start of each build session when running inside
     * Maven Daemon (mvnd), where multiple builds share the same JVM and
     * therefore the same {@code @Singleton} instance. Without a reset,
     * diagnostics from a previous build accumulate into the current one.
     * <p>
     * The suppressed-keys configuration is intentionally <em>not</em> cleared —
     * the caller is responsible for re-setting it via
     * {@link #setSuppressedKeys(Set)} if needed.
     */
    public void reset() {
        uniqueProblems.clear();
        counts.clear();
        synchronized (orderedKeys) {
            orderedKeys.clear();
            orderedSize.set(0);
        }
        noKeyCounter.set(0);
    }
}
