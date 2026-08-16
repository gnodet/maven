# PR Review Loop State

## Last Run

- **Timestamp:** 2026-08-16T10:54:00Z
- **PRs checked:** 3
- **Reviews posted:** 2
- **Note:** Reviewed 3 PRs: #12662 (PathConflictResolver default - suppressed, all FPs), #12654 (AsyncDrainWriter - COMMENT), #12655 (location tracking/subproject discovery - COMMENT).

## Reviewed PRs

<!-- Format: | PR# | Title | Author | Reviewed | Verdict | Notes | -->

| PR | Title | Author | Reviewed | Verdict | Notes |
|----|-------|--------|----------|---------|-------|
| #12659 | Resolve classified POM artifacts from the reactor in Maven 3.x | wilx | 2026-08-16T01:50:00Z | APPROVE | Clean classifier guard fix for 3.10.x ReactorReader. 2 low non-blocking notes. |
| #12652 | Optimize reactor sort, model pool, and phase comparator performance | gnodet | 2026-08-16T04:21:00Z | COMMENT | Own PR. Clean O(n²)→O(n) optimizations. 1 confirmed (null safety inconsistency), 1 FP dropped. |
| #12653 | Optimize model building pipeline: defer Dependency.build() and reduce allocations | gnodet | 2026-08-16T04:32:00Z | COMMENT | Own PR. 40% benchmark improvement. 1 confirmed (getModifiable footgun), 2 FPs dropped. |
| #12629 | Fix #12607: DefaultMaven session scope leak on constructor exception | elharo | 2026-08-16T01:34:00Z | APPROVE | Correct try-finally fix for scope leak. 3 low test suggestions (non-blocking). |
| #12629 | Fix #12607: DefaultMaven session scope leak on constructor exception | elharo | 2026-08-16T01:34:00Z | APPROVE | Correct try-finally fix for scope leak. 3 low test suggestions (non-blocking). |
| #12626 | Fix field cache to let child class fields take precedence over parent | gnodet | 2026-08-16T01:39:00Z | COMMENT | Own PR. Clean putIfAbsent fix. Verifier: 1 finding FP (assertion redundant due to Map semantics). Review suppressed. |
| #12626 | Fix field cache to let child class fields take precedence over parent | gnodet | 2026-08-16T01:39:00Z | COMMENT | Own PR. Clean putIfAbsent fix. Verifier: 1 finding FP (assertion redundant due to Map semantics). Review suppressed. |
| #12641 | Fix #12640: BOM consumer POM must not include inherited dependency management | gnodet | 2026-08-16T01:39:00Z | COMMENT | Own PR. Correct raw+effective model hybrid. 1 confirmed (import-scope test gap), 3 FPs dropped. |
| #12641 | Fix #12640: BOM consumer POM must not include inherited dependency management | gnodet | 2026-08-16T01:39:00Z | COMMENT | Own PR. Correct raw+effective model hybrid. 1 confirmed (import-scope test gap), 3 FPs dropped. |
| #12620 | [MNG-6797] Remember if Maven model problems were encountered in Maven 4 | wilx | 2026-08-16T01:23:00Z | APPROVE | Well-structured port of model-problem retention from Maven 3→4. Thread-safe, backward compatible, thorough tests. 2 low observations (enum coupling, default method asymmetry). |
| #12620 | [MNG-6797] Remember if Maven model problems were encountered in Maven 4 | wilx | 2026-08-16T01:23:00Z | APPROVE | Well-structured port of model-problem retention from Maven 3→4. Thread-safe, backward compatible, thorough tests. 2 low observations (enum coupling, default method asymmetry). |
| #12620 | [MNG-6797] Remember if Maven model problems were encountered in Maven 4 | wilx | 2026-08-16T01:23:00Z | APPROVE | Well-structured port of model-problem retention from Maven 3→4. Thread-safe, backward compatible, thorough tests. 2 low observations (enum coupling, default method asymmetry). |
| #12620 | [MNG-6797] Remember if Maven model problems were encountered in Maven 4 | wilx | 2026-08-16T01:23:00Z | APPROVE | Well-structured port of model-problem retention from Maven 3→4. Thread-safe, backward compatible, thorough tests. 2 low observations (enum coupling, default method asymmetry). |
| #12616 | Fail on misconfigured plugin-requested toolchains | wilx | 2026-08-16T01:23:00Z | APPROVE | Correct fail-fast fix aligning 3.10.x with Maven 4. IllegalStateException (unchecked) preserves API compat. Thorough tests. |
| #12616 | Fail on misconfigured plugin-requested toolchains | wilx | 2026-08-16T01:23:00Z | APPROVE | Correct fail-fast fix aligning 3.10.x with Maven 4. IllegalStateException (unchecked) preserves API compat. Thorough tests. |
| #12616 | Fail on misconfigured plugin-requested toolchains | wilx | 2026-08-16T01:23:00Z | APPROVE | Correct fail-fast fix aligning 3.10.x with Maven 4. IllegalStateException (unchecked) preserves API compat. Thorough tests. |
| #12616 | Fail on misconfigured plugin-requested toolchains | wilx | 2026-08-16T01:23:00Z | APPROVE | Correct fail-fast fix aligning 3.10.x with Maven 4. IllegalStateException (unchecked) preserves API compat. Thorough tests. |
| #12581 | [ci][gh][workflow] explicit action hash and permissions | rmannibucau | 2026-08-16T01:23:00Z | COMMENT | Permissions hardening correct. Stale pinned hash (7 commits behind v5 HEAD). Policy debate on hash-pinning org-internal workflows remains open. |
| #12581 | [ci][gh][workflow] explicit action hash and permissions | rmannibucau | 2026-08-16T01:23:00Z | COMMENT | Permissions hardening correct. Stale pinned hash (7 commits behind v5 HEAD). Policy debate on hash-pinning org-internal workflows remains open. |
| #12581 | [ci][gh][workflow] explicit action hash and permissions | rmannibucau | 2026-08-16T01:23:00Z | COMMENT | Permissions hardening correct. Stale pinned hash (7 commits behind v5 HEAD). Policy debate on hash-pinning org-internal workflows remains open. |
| #12581 | [ci][gh][workflow] explicit action hash and permissions | rmannibucau | 2026-08-16T01:23:00Z | COMMENT | Permissions hardening correct. Stale pinned hash (7 commits behind v5 HEAD). Policy debate on hash-pinning org-internal workflows remains open. |
| #12503 | Preserve dependency scope in plugin artifacts - fix #12497 | slawekjaranowski | 2026-07-18T13:30:00Z | APPROVE | Correct regression fix for 3.10.x. Null-safe Optional chain. 1 Spotless fix needed, 2 suggestions (Javadoc, test). |
| #12503 | Preserve dependency scope in plugin artifacts - fix #12497 | slawekjaranowski | 2026-07-18T13:30:00Z | APPROVE | Correct regression fix for 3.10.x. Null-safe Optional chain. 1 Spotless fix needed, 2 suggestions (Javadoc, test). |
| #12503 | Preserve dependency scope in plugin artifacts - fix #12497 | slawekjaranowski | 2026-07-18T13:30:00Z | APPROVE | Correct regression fix for 3.10.x. Null-safe Optional chain. 1 Spotless fix needed, 2 suggestions (Javadoc, test). |
| #12503 | Preserve dependency scope in plugin artifacts - fix #12497 | slawekjaranowski | 2026-07-18T13:30:00Z | APPROVE | Correct regression fix for 3.10.x. Null-safe Optional chain. 1 Spotless fix needed, 2 suggestions (Javadoc, test). |
| #12503 | Preserve dependency scope in plugin artifacts - fix #12497 | slawekjaranowski | 2026-07-18T13:30:00Z | APPROVE | Correct regression fix for 3.10.x. Null-safe Optional chain. 1 Spotless fix needed, 2 suggestions (Javadoc, test). |
| #12503 | Preserve dependency scope in plugin artifacts - fix #12497 | slawekjaranowski | 2026-07-18T13:30:00Z | APPROVE | Correct regression fix for 3.10.x. Null-safe Optional chain. 1 Spotless fix needed, 2 suggestions (Javadoc, test). |
| #12503 | Preserve dependency scope in plugin artifacts - fix #12497 | slawekjaranowski | 2026-07-18T13:30:00Z | APPROVE | Correct regression fix for 3.10.x. Null-safe Optional chain. 1 Spotless fix needed, 2 suggestions (Javadoc, test). |
| #12503 | Preserve dependency scope in plugin artifacts - fix #12497 | slawekjaranowski | 2026-07-18T13:30:00Z | APPROVE | Correct regression fix for 3.10.x. Null-safe Optional chain. 1 Spotless fix needed, 2 suggestions (Javadoc, test). |
| #12502 | Refactor MNG-11133 test to use maven-it-plugin-expression | gnodet | 2026-07-17T15:30:00Z | COMMENT | Own PR. Clean test refactoring — effective-pom string matching → expression plugin. No findings. LGTM. |
| #12502 | Refactor MNG-11133 test to use maven-it-plugin-expression | gnodet | 2026-07-17T15:30:00Z | COMMENT | Own PR. Clean test refactoring — effective-pom string matching → expression plugin. No findings. LGTM. |
| #12502 | Refactor MNG-11133 test to use maven-it-plugin-expression | gnodet | 2026-07-17T15:30:00Z | COMMENT | Own PR. Clean test refactoring — effective-pom string matching → expression plugin. No findings. LGTM. |
| #12502 | Refactor MNG-11133 test to use maven-it-plugin-expression | gnodet | 2026-07-17T15:30:00Z | COMMENT | Own PR. Clean test refactoring — effective-pom string matching → expression plugin. No findings. LGTM. |
| #12502 | Refactor MNG-11133 test to use maven-it-plugin-expression | gnodet | 2026-07-17T15:30:00Z | COMMENT | Own PR. Clean test refactoring — effective-pom string matching → expression plugin. No findings. LGTM. |
| #12502 | Refactor MNG-11133 test to use maven-it-plugin-expression | gnodet | 2026-07-17T15:30:00Z | COMMENT | Own PR. Clean test refactoring — effective-pom string matching → expression plugin. No findings. LGTM. |
| #12502 | Refactor MNG-11133 test to use maven-it-plugin-expression | gnodet | 2026-07-17T15:30:00Z | COMMENT | Own PR. Clean test refactoring — effective-pom string matching → expression plugin. No findings. LGTM. |
| #12502 | Refactor MNG-11133 test to use maven-it-plugin-expression | gnodet | 2026-07-17T15:30:00Z | COMMENT | Own PR. Clean test refactoring — effective-pom string matching → expression plugin. No findings. LGTM. |
| #12478 | Add cross-thread deadlock regression test for #12472 | gnodet | 2026-07-17T12:30:00Z | COMMENT | Own PR. Excellent regression test using CyclicBarrier. 2 low findings (assertion precision). LGTM. |
| #12478 | Add cross-thread deadlock regression test for #12472 | gnodet | 2026-07-17T12:30:00Z | COMMENT | Own PR. Excellent regression test using CyclicBarrier. 2 low findings (assertion precision). LGTM. |
| #12478 | Add cross-thread deadlock regression test for #12472 | gnodet | 2026-07-17T12:30:00Z | COMMENT | Own PR. Excellent regression test using CyclicBarrier. 2 low findings (assertion precision). LGTM. |
| #12478 | Add cross-thread deadlock regression test for #12472 | gnodet | 2026-07-17T12:30:00Z | COMMENT | Own PR. Excellent regression test using CyclicBarrier. 2 low findings (assertion precision). LGTM. |
| #12478 | Add cross-thread deadlock regression test for #12472 | gnodet | 2026-07-17T12:30:00Z | COMMENT | Own PR. Excellent regression test using CyclicBarrier. 2 low findings (assertion precision). LGTM. |
| #12478 | Add cross-thread deadlock regression test for #12472 | gnodet | 2026-07-17T12:30:00Z | COMMENT | Own PR. Excellent regression test using CyclicBarrier. 2 low findings (assertion precision). LGTM. |
| #12478 | Add cross-thread deadlock regression test for #12472 | gnodet | 2026-07-17T12:30:00Z | COMMENT | Own PR. Excellent regression test using CyclicBarrier. 2 low findings (assertion precision). LGTM. |
| #12478 | Add cross-thread deadlock regression test for #12472 | gnodet | 2026-07-17T12:30:00Z | COMMENT | Own PR. Excellent regression test using CyclicBarrier. 2 low findings (assertion precision). LGTM. |
| #12419 | Avoid IllegalStateException on duplicate profile ids in DefaultModelBuilder | mvanhorn | 2026-07-10T05:58:12Z | APPROVE | Re-review: new commit is Spotless formatting only, no logic change. Core fix remains correct. |
| #12419 | Avoid IllegalStateException on duplicate profile ids in DefaultModelBuilder | mvanhorn | 2026-07-10T05:58:12Z | APPROVE | Re-review: new commit is Spotless formatting only, no logic change. Core fix remains correct. |
| #12419 | Avoid IllegalStateException on duplicate profile ids in DefaultModelBuilder | mvanhorn | 2026-07-10T05:58:12Z | APPROVE | Re-review: new commit is Spotless formatting only, no logic change. Core fix remains correct. |
| #12419 | Avoid IllegalStateException on duplicate profile ids in DefaultModelBuilder | mvanhorn | 2026-07-10T05:58:12Z | APPROVE | Re-review: new commit is Spotless formatting only, no logic change. Core fix remains correct. |
| #12419 | Avoid IllegalStateException on duplicate profile ids in DefaultModelBuilder | mvanhorn | 2026-07-10T05:58:12Z | APPROVE | Re-review: new commit is Spotless formatting only, no logic change. Core fix remains correct. |
| #12419 | Avoid IllegalStateException on duplicate profile ids in DefaultModelBuilder | mvanhorn | 2026-07-10T05:58:12Z | APPROVE | Re-review: new commit is Spotless formatting only, no logic change. Core fix remains correct. |
| #12419 | Avoid IllegalStateException on duplicate profile ids in DefaultModelBuilder | mvanhorn | 2026-07-10T05:58:12Z | APPROVE | Re-review: new commit is Spotless formatting only, no logic change. Core fix remains correct. |
| #12419 | Avoid IllegalStateException on duplicate profile ids in DefaultModelBuilder | mvanhorn | 2026-07-10T05:58:12Z | APPROVE | Re-review: new commit is Spotless formatting only, no logic change. Core fix remains correct. |
| #11770 | feat: Improve the matching of required versions | nielsbasjes | 2026-07-09T00:00:00Z | COMMENT | Review posted; regex bug, pattern precompilation, missing tests — reinforces prior feedback |
| #11770 | feat: Improve the matching of required versions | nielsbasjes | 2026-07-09T00:00:00Z | COMMENT | Review posted; regex bug, pattern precompilation, missing tests — reinforces prior feedback |
| #11770 | feat: Improve the matching of required versions | nielsbasjes | 2026-07-09T00:00:00Z | COMMENT | Review posted; regex bug, pattern precompilation, missing tests — reinforces prior feedback |
| #11770 | feat: Improve the matching of required versions | nielsbasjes | 2026-07-09T00:00:00Z | COMMENT | Review posted; regex bug, pattern precompilation, missing tests — reinforces prior feedback |
| #11770 | feat: Improve the matching of required versions | nielsbasjes | 2026-07-09T00:00:00Z | COMMENT | Review posted; regex bug, pattern precompilation, missing tests — reinforces prior feedback |
| #11770 | feat: Improve the matching of required versions | nielsbasjes | 2026-07-09T00:00:00Z | COMMENT | Review posted; regex bug, pattern precompilation, missing tests — reinforces prior feedback |
| #11770 | feat: Improve the matching of required versions | nielsbasjes | 2026-07-09T00:00:00Z | COMMENT | Review posted; regex bug, pattern precompilation, missing tests — reinforces prior feedback |
| #11770 | feat: Improve the matching of required versions | nielsbasjes | 2026-07-09T00:00:00Z | COMMENT | Review posted; regex bug, pattern precompilation, missing tests — reinforces prior feedback |
| #12417 | [MNG-8432] Inherit properties from imported BOMs | Hiteshsai007 | 2026-07-10T05:58:32Z | COMMENT | Re-review: double-interpolation fixed, opt-in flag added. Still no tests, no CI, BOM filter condition bug (pre-existing). 2 high + 1 medium + 1 low confirmed (2 FPs dropped). |
| #12417 | [MNG-8432] Inherit properties from imported BOMs | Hiteshsai007 | 2026-07-10T05:58:32Z | COMMENT | Re-review: double-interpolation fixed, opt-in flag added. Still no tests, no CI, BOM filter condition bug (pre-existing). 2 high + 1 medium + 1 low confirmed (2 FPs dropped). |
| #12417 | [MNG-8432] Inherit properties from imported BOMs | Hiteshsai007 | 2026-07-10T05:58:32Z | COMMENT | Re-review: double-interpolation fixed, opt-in flag added. Still no tests, no CI, BOM filter condition bug (pre-existing). 2 high + 1 medium + 1 low confirmed (2 FPs dropped). |
| #12417 | [MNG-8432] Inherit properties from imported BOMs | Hiteshsai007 | 2026-07-10T05:58:32Z | COMMENT | Re-review: double-interpolation fixed, opt-in flag added. Still no tests, no CI, BOM filter condition bug (pre-existing). 2 high + 1 medium + 1 low confirmed (2 FPs dropped). |
| #12417 | [MNG-8432] Inherit properties from imported BOMs | Hiteshsai007 | 2026-07-10T05:58:32Z | COMMENT | Re-review: double-interpolation fixed, opt-in flag added. Still no tests, no CI, BOM filter condition bug (pre-existing). 2 high + 1 medium + 1 low confirmed (2 FPs dropped). |
| #12417 | [MNG-8432] Inherit properties from imported BOMs | Hiteshsai007 | 2026-07-10T05:58:32Z | COMMENT | Re-review: double-interpolation fixed, opt-in flag added. Still no tests, no CI, BOM filter condition bug (pre-existing). 2 high + 1 medium + 1 low confirmed (2 FPs dropped). |
| #12417 | [MNG-8432] Inherit properties from imported BOMs | Hiteshsai007 | 2026-07-10T05:58:32Z | COMMENT | Re-review: double-interpolation fixed, opt-in flag added. Still no tests, no CI, BOM filter condition bug (pre-existing). 2 high + 1 medium + 1 low confirmed (2 FPs dropped). |
| #12417 | [MNG-8432] Inherit properties from imported BOMs | Hiteshsai007 | 2026-07-10T05:58:32Z | COMMENT | Re-review: double-interpolation fixed, opt-in flag added. Still no tests, no CI, BOM filter condition bug (pre-existing). 2 high + 1 medium + 1 low confirmed (2 FPs dropped). |
| #11743 | [MNG-11449] Add Mockito javaagent for maven-cli tests | arturobernalg | 2026-07-09T00:00:00Z | COMMENT | Review posted; PR superseded by #12369 (merged 2026-07-03) |
| #11743 | [MNG-11449] Add Mockito javaagent for maven-cli tests | arturobernalg | 2026-07-09T00:00:00Z | COMMENT | Review posted; PR superseded by #12369 (merged 2026-07-03) |
| #11743 | [MNG-11449] Add Mockito javaagent for maven-cli tests | arturobernalg | 2026-07-09T00:00:00Z | COMMENT | Review posted; PR superseded by #12369 (merged 2026-07-03) |
| #11743 | [MNG-11449] Add Mockito javaagent for maven-cli tests | arturobernalg | 2026-07-09T00:00:00Z | COMMENT | Review posted; PR superseded by #12369 (merged 2026-07-03) |
| #11743 | [MNG-11449] Add Mockito javaagent for maven-cli tests | arturobernalg | 2026-07-09T00:00:00Z | COMMENT | Review posted; PR superseded by #12369 (merged 2026-07-03) |
| #11743 | [MNG-11449] Add Mockito javaagent for maven-cli tests | arturobernalg | 2026-07-09T00:00:00Z | COMMENT | Review posted; PR superseded by #12369 (merged 2026-07-03) |
| #11743 | [MNG-11449] Add Mockito javaagent for maven-cli tests | arturobernalg | 2026-07-09T00:00:00Z | COMMENT | Review posted; PR superseded by #12369 (merged 2026-07-03) |
| #11743 | [MNG-11449] Add Mockito javaagent for maven-cli tests | arturobernalg | 2026-07-09T00:00:00Z | COMMENT | Review posted; PR superseded by #12369 (merged 2026-07-03) |
| #12330 | In failed build limit reactor summary to only failed modules | slawekjaranowski | 2026-07-11T16:04:59Z | APPROVE | Re-review: all prior feedback addressed — ellipsis gap fixed with lastWasSkipped, test visibility corrected, new separated-failure test added. |
| #12330 | In failed build limit reactor summary to only failed modules | slawekjaranowski | 2026-07-11T16:04:59Z | APPROVE | Re-review: all prior feedback addressed — ellipsis gap fixed with lastWasSkipped, test visibility corrected, new separated-failure test added. |
| #12330 | In failed build limit reactor summary to only failed modules | slawekjaranowski | 2026-07-11T16:04:59Z | APPROVE | Re-review: all prior feedback addressed — ellipsis gap fixed with lastWasSkipped, test visibility corrected, new separated-failure test added. |
| #12330 | In failed build limit reactor summary to only failed modules | slawekjaranowski | 2026-07-11T16:04:59Z | APPROVE | Re-review: all prior feedback addressed — ellipsis gap fixed with lastWasSkipped, test visibility corrected, new separated-failure test added. |
| #12330 | In failed build limit reactor summary to only failed modules | slawekjaranowski | 2026-07-11T16:04:59Z | APPROVE | Re-review: all prior feedback addressed — ellipsis gap fixed with lastWasSkipped, test visibility corrected, new separated-failure test added. |
| #12330 | In failed build limit reactor summary to only failed modules | slawekjaranowski | 2026-07-11T16:04:59Z | APPROVE | Re-review: all prior feedback addressed — ellipsis gap fixed with lastWasSkipped, test visibility corrected, new separated-failure test added. |
| #12330 | In failed build limit reactor summary to only failed modules | slawekjaranowski | 2026-07-11T16:04:59Z | APPROVE | Re-review: all prior feedback addressed — ellipsis gap fixed with lastWasSkipped, test visibility corrected, new separated-failure test added. |
| #12330 | In failed build limit reactor summary to only failed modules | slawekjaranowski | 2026-07-11T16:04:59Z | APPROVE | Re-review: all prior feedback addressed — ellipsis gap fixed with lastWasSkipped, test visibility corrected, new separated-failure test added. |
| #12446 | Fix deadlock in AbstractRequestCache when resolving parent POMs | gnodet | 2026-07-09T00:00:00Z | COMMENT | Review posted (own PR, can't APPROVE); clean deadlock fix, LGTM |
| #12446 | Fix deadlock in AbstractRequestCache when resolving parent POMs | gnodet | 2026-07-09T00:00:00Z | COMMENT | Review posted (own PR, can't APPROVE); clean deadlock fix, LGTM |
| #12446 | Fix deadlock in AbstractRequestCache when resolving parent POMs | gnodet | 2026-07-09T00:00:00Z | COMMENT | Review posted (own PR, can't APPROVE); clean deadlock fix, LGTM |
| #12446 | Fix deadlock in AbstractRequestCache when resolving parent POMs | gnodet | 2026-07-09T00:00:00Z | COMMENT | Review posted (own PR, can't APPROVE); clean deadlock fix, LGTM |
| #12446 | Fix deadlock in AbstractRequestCache when resolving parent POMs | gnodet | 2026-07-09T00:00:00Z | COMMENT | Review posted (own PR, can't APPROVE); clean deadlock fix, LGTM |
| #12446 | Fix deadlock in AbstractRequestCache when resolving parent POMs | gnodet | 2026-07-09T00:00:00Z | COMMENT | Review posted (own PR, can't APPROVE); clean deadlock fix, LGTM |
| #12446 | Fix deadlock in AbstractRequestCache when resolving parent POMs | gnodet | 2026-07-09T00:00:00Z | COMMENT | Review posted (own PR, can't APPROVE); clean deadlock fix, LGTM |
| #12446 | Fix deadlock in AbstractRequestCache when resolving parent POMs | gnodet | 2026-07-09T00:00:00Z | COMMENT | Review posted (own PR, can't APPROVE); clean deadlock fix, LGTM |
| #12662 | Enable PathConflictResolver by default | gnodet | 2026-08-16T10:54:00Z | COMMENT | Own PR. Clean config change. 2 low FPs (upstream doc). Review suppressed. |
| #12654 | Add AsyncDrainWriter to eliminate PrintWriter lock contention | gnodet | 2026-08-16T10:54:00Z | COMMENT | Own PR. Lock-free drain. 1 medium confirmed (no tests), 2 low confirmed. |
| #12655 | Wire ModelBuilderRequest.isLocationTracking() to XML parser | gnodet | 2026-08-16T10:54:00Z | COMMENT | Own PR. 2 medium confirmed (stale description, behavioral change), 2 low confirmed. |

## Skipped PRs

<!-- PRs intentionally skipped (bot PRs, draft, etc.) -->

| PR | Reason | Since |
|----|--------|-------|
| #12501 | Bot PR (dependabot), setup-java 5.5.0→5.6.0 | 2026-07-17 |
| #12500 | Bot PR (dependabot), setup-java 5.5.0→5.6.0 | 2026-07-17 |
| #12499 | Bot PR (dependabot), setup-java 5.5.0→5.6.0 | 2026-07-17 |
| #12498 | Bot PR (dependabot), setup-java 5.5.0→5.6.0 | 2026-07-17 |
| #12481 | Draft PR | 2026-07-17 |
| #12496 | Bot PR (dependabot), GH Actions bump | 2026-07-14 |
| #12495 | Bot PR (dependabot), GH Actions bump | 2026-07-14 |
| #12494 | Bot PR (dependabot), GH Actions bump | 2026-07-14 |
| #12493 | Bot PR (dependabot), GH Actions bump | 2026-07-14 |
| #12492 | Bot PR (dependabot), GH Actions bump | 2026-07-14 |
| #12491 | Bot PR (dependabot), GH Actions bump | 2026-07-14 |
| #12490 | Bot PR (dependabot), GH Actions bump | 2026-07-14 |
| #12489 | Bot PR (dependabot), GH Actions bump | 2026-07-14 |
| #12488 | Bot PR (dependabot), GH Actions bump | 2026-07-14 |
| #12487 | Bot PR (dependabot), GH Actions bump | 2026-07-14 |
| #12477 | Bot PR (dependabot), dependencies label | 2026-07-13 |
| #12458 | Bot PR (dependabot), dependencies label | 2026-07-10 |
| #12457 | Bot PR (dependabot), dependencies label | 2026-07-10 |
| #12452 | Bot PR (dependabot), dependencies label | 2026-07-09 |
| #12451 | Bot PR (dependabot), dependencies label | 2026-07-09 |
| #12439 | Bot PR (dependabot), dependencies label | 2026-07-09 |
| #12438 | Bot PR (dependabot), dependencies label | 2026-07-09 |
| #12437 | Bot PR (dependabot), dependencies label | 2026-07-09 |
| #12436 | Bot PR (dependabot), dependencies label | 2026-07-09 |
| #12402 | WIP in title | 2026-07-09 |

## Review Queue

<!-- PRs that need review but haven't been processed yet -->

| PR | Title | Author | Priority | Queued |
|----|-------|--------|----------|-------|
