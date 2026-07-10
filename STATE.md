# PR Review Loop State

## Last Run

- **Timestamp:** 2026-07-10T03:09:28Z
- **PRs checked:** 61
- **Reviews posted:** 48
- **Note:** Reviewed final 3 PRs: #2277 (modernize codebase, toList mutability concern), #1774 (cascading profiles, ReportSet merge bug found), #71 (mirrors in profiles, 10yr stale). ALL open PRs have now been reviewed.

## Reviewed PRs

<!-- Format: | PR# | Title | Author | Reviewed | Verdict | Notes | -->

| PR | Title | Author | Reviewed | Verdict | Notes |
|----|-------|--------|----------|---------|-------|
| #12419 | Avoid IllegalStateException on duplicate profile ids in DefaultModelBuilder | mvanhorn | 2026-07-09T00:00:00Z | APPROVE | Review posted; correct fix using positional indexing instead of map |
| #11770 | feat: Improve the matching of required versions | nielsbasjes | 2026-07-09T00:00:00Z | COMMENT | Review posted; regex bug, pattern precompilation, missing tests — reinforces prior feedback |
| #12417 | [MNG-8432] Inherit properties from imported BOMs | Hiteshsai007 | 2026-07-09T00:00:00Z | COMMENT | Review posted; CI failure (double interpolation), semantic concerns, no tests |
| #11743 | [MNG-11449] Add Mockito javaagent for maven-cli tests | arturobernalg | 2026-07-09T00:00:00Z | COMMENT | Review posted; PR superseded by #12369 (merged 2026-07-03) |
| #12330 | In failed build limit reactor summary to only failed modules | slawekjaranowski | 2026-07-09T00:00:00Z | COMMENT | Review posted; ellipsis gap in multi-failure scenario, suggested fix |
| #12446 | Fix deadlock in AbstractRequestCache when resolving parent POMs | gnodet | 2026-07-09T00:00:00Z | COMMENT | Review posted (own PR, can't APPROVE); clean deadlock fix, LGTM |
| #12454 | Fix #12430: mvnup upgrade strategies and compatibility improvements | gnodet | 2026-07-10T01:15:41Z | COMMENT | Re-review posted; 1 low finding confirmed (Javadoc inconsistency) |
| #12377 | Remove Profile getSource / setSource | gnodet | 2026-07-09T00:00:00Z | COMMENT | Review posted (own PR, can't APPROVE); LGTM — clean API removal, backward compat preserved |
| #11904 | Feat: Add id attribute (gav) support to Dependency, Exclusion, Mixin | rbygrave | 2026-07-09T00:00:00Z | COMMENT | Review posted; high: CI broken (IT compilation error), 6/7 findings false positive |
| #12416 | Fix BOM version resolution for sibling modules in dependencyManagement | Hiteshsai007 | 2026-07-09T00:00:00Z | COMMENT | Review posted; correct fix for #11147, minor: unnecessary list copy, duplicated loop logic |
| #12410 | reject path-traversal segments in coordinate ids and versions | jmestwa-coder | 2026-07-09T00:00:00Z | COMMENT | Re-review: all prior concerns addressed, tests added, CI passes, javadoc wording debate ongoing |
| #11818 | Add @Nullable annotations and NullAway profile for Maven 4 API | gnodet | 2026-07-09T00:00:00Z | COMMENT | Review posted; medium: getSource() @Nonnull→@Nullable may break callers, low: getRepositoryMerging() same pattern |
| #12418 | [MNG-8425] Fix mvnenc init saving invalid master source configuration | Hiteshsai007 | 2026-07-09T00:00:00Z | COMMENT | AI review on GitHub (prior session, 2026-07-07); no new commits since; will re-review when author pushes updates |
| #12332 | [MNG-8768] Add executable() function for conditional profile activation based on PATH | Hiteshsai007 | 2026-07-09T00:00:00Z | COMMENT | AI review on GitHub (prior session, 2026-06-23); no new commits since; will re-review when author pushes updates |
| #12145 | Avoid final logger injection in EventSpyDispatcher | Will-thom | 2026-07-09T00:00:00Z | COMMENT | AI review on GitHub (prior session, 2026-05-25); no new commits since; will re-review when author pushes updates |
| #12135 | [MNG-11642] Add JPMS module support to Maven 4 | gnodet | 2026-07-09T00:00:00Z | COMMENT | AI review on GitHub (prior session, 2026-07-03); no new commits since; will re-review when author pushes updates |
| #12069 | Fix site lifecycle reactor dependency resolution | Will-thom | 2026-07-09T00:00:00Z | COMMENT | AI review on GitHub (prior session, 2026-05-23); no new commits since; will re-review when author pushes updates |
| #11926 | Warn when profile ID matches a lifecycle phase name | utafrali | 2026-07-09T00:00:00Z | COMMENT | AI review on GitHub (prior session, 2026-05-21); no new commits since; will re-review when author pushes updates |
| #11692 | docs: fix broken reference links | amahi1568 | 2026-07-09T00:00:00Z | COMMENT | AI review on GitHub (prior session, 2026-05-23); no new commits since; will re-review when author pushes updates |
| #11686 | docs: clarify deprecation of RELEASE and LATEST version constants | amahi1568 | 2026-07-09T00:00:00Z | COMMENT | AI review on GitHub (prior session, 2026-05-21); no new commits since; will re-review when author pushes updates |
| #11682 | Improve README documentation clarity | amahi1568 | 2026-07-09T00:00:00Z | COMMENT | AI review on GitHub (prior session, 2026-05-21); no new commits since; will re-review when author pushes updates |
| #11609 | Bug: make all collected args use single quote | cstamas | 2026-07-09T00:00:00Z | COMMENT | AI review on GitHub (prior session, 2026-05-21); no new commits since; will re-review when author pushes updates |
| #11509 | Add tests | TheRealHaui | 2026-07-09T00:00:00Z | COMMENT | AI review on GitHub (prior session, 2026-06-07); no new commits since; will re-review when author pushes updates |
| #11410 | tests(maven#10389): Adding DefaultDependencyResolverResultTest | vijaykriishna | 2026-07-10T00:00:00Z | COMMENT | AI review on GitHub (prior session, 2026-06-07); no new commits since; will re-review when author pushes updates |
| #11502 | Added additional tests to ProjectModelResolverTest | TheRealHaui | 2026-07-10T01:15:53Z | COMMENT | Review posted; 2 low findings (assertion fragility on Aether messages) |
| #11405 | Introduce TempFileService and lifecycle cleanup participant | arturobernalg | 2026-07-10T01:16:16Z | COMMENT | Review posted; 1 high + 4 medium + 4 low findings (API design issues) |
| #2333 | [MNG-5913] Allow defining aliases for existing server configurations | slawekjaranowski | 2026-07-10T01:23:25Z | COMMENT | Review posted; clean implementation, no code issues — open policy discussion on backward compat |
| #10971 | Add test for prefixed Maven elements | elharo | 2026-07-10T01:23:31Z | COMMENT | Review posted; LGTM, no findings |
| #11186 | Issue #10985: Maven allows random namespaces on project | raupachz | 2026-07-10T01:23:44Z | COMMENT | Review posted; 2 high + 1 medium + 1 low — backward compat break, no tests, PR #11185 is better alternative |
| #11185 | Disallow arbitrary namespaces in Maven and Metadata readers | arturobernalg | 2026-07-10T01:32:16Z | COMMENT | Review posted; 3 high + 1 medium + 1 low — hardcoded tags in shared template, broken formatting, needs modello-based approach |
| #10906 | [MNG-8018] Fix MSYS/Git-Bash path mis-detection on Windows | arturobernalg | 2026-07-10T01:32:35Z | COMMENT | Review posted; 2 high + 3 medium + 3 low — System.out.println in prod, over-broad path detection, divergent impls |
| #669 | Use try-with-resources | garydgregory | 2026-07-10T01:32:44Z | COMMENT | Review posted; 4+ year old PR, 2/4 files obsolete, paths stale, recommend close and reopen with 2 valid conversions |
| #11102 | Add --processes to list running Maven builds | arturobernalg | 2026-07-10T00:00:00Z | COMMENT | AI review on GitHub (prior session, 2026-06-07); no new commits since; will re-review when author pushes updates |
| #11146 | Add support for MAVEN_PROJECTBASEDIR substitution | mguillem | 2026-07-10T01:47:22Z | APPROVE | Clean backport of MNG-8598 to 3.9.x; 1 low finding (tokens=* in for /F loop), CI green |
| #10952 | Keep Maven Namespace the same | elharo | 2026-07-10T01:47:43Z | COMMENT | 2 medium + 1 low findings; community decided against approach for 4.0; shade-plugin blocker; backward compat concern |
| #2106 | Add exclusion scope to repo | XenoAmess | 2026-07-10T01:47:59Z | COMMENT | 3 high + 1 medium + 1 low; companion resolver PR wontfix, won't compile, no tests, stale 17 months |
| #2023 | Clean up model description and API doc | elharo | 2026-07-10T01:55:05Z | COMMENT | 3 high + 1 medium; "Returns" prefix wrong for Modello, artifactId definition error, merge conflicts, prior review unaddressed |
| #1538 | Add test case for dependency exclusions | yuehcw | 2026-07-10T01:55:11Z | COMMENT | 2 high + 3 medium; JUnit 4/5 import mix, stale paths (MNG-8346), unrelated change bundled |
| #793 | Log shouldn't have been deprecated | rmannibucau | 2026-07-10T01:55:22Z | COMMENT | 1 high (stale paths); 4 files moved to compat/ by MNG-8346, needs rebase and community re-discussion |
| #1511 | Also print groupId and version in Reactor Build Order | chenchc6 | 2026-07-10T02:01:20Z | COMMENT | 2 high + 1 medium + 1 low; stale paths (MNG-8346), unaddressed gnodet review (make configurable), name dropped |
| #1035 | Protect master and maven-3.9.x branches | elharo | 2026-07-10T02:01:27Z | COMMENT | Superseded; branch protection already enabled for 8 branches via later commits. Recommend close. |
| #964 | Add artifact-type for Tycho | laeubi | 2026-07-10T02:01:41Z | COMMENT | Stale 3+ years; committer objection (rmannibucau -1), JIRA closed, file doesn't exist on master. Needs decision. |
| #1437 | Add ConsumerPomFile methods | laeubi | 2026-07-10T02:13:49Z | COMMENT | 1 medium (no tests); code follows patterns, design discussion with rmannibucau unresolved, targets 3.9.x |
| #1435 | Add failing projects if banned from reactor | laeubi | 2026-07-10T02:14:01Z | COMMENT | 1 high bug (`message += message` instead of `reason += message`) + 1 medium; targets 3.9.x, gnodet arch feedback unaddressed |
| #298 | Remember if Maven model problems encountered | wilx | 2026-07-10T02:14:11Z | COMMENT | 2 high + 1 medium; 7 years old, method deleted (MNG-7646), file moved (MNG-8346), needs complete rewrite |
| #144 | Update DefaultPluginManager (MNG-2893) | ChristianSchulte | 2026-07-10T02:23:20Z | COMMENT | 1 high + 2 medium; 8.5 years old, stale paths, possibly already addressed by session factory config |
| #1125 | Plugin Dependency Resolution improvement | cstamas | 2026-07-10T02:23:33Z | COMMENT | 5 high; hardcoded outdated versions (sisu 0.3.5, classworlds 2.6.0), Guava import (no dep), API migration needed |
| #281 | Dependency order should be nearest first | belingueres | 2026-07-10T02:23:41Z | COMMENT | 2 high; stale paths, test file conflict, behavioral change needs full IT validation |
| #995 | New flag to verify Maven installation status | mthmulders | 2026-07-10T02:34:25Z | COMMENT | 4 high + 4 medium; 3yr stale, all paths moved, MavenCli deprecated, TransporterProvider gone, needs CLIng rewrite |
| #147 | Dependency management import relocations | ChristianSchulte | 2026-07-10T02:34:42Z | COMMENT | 3 high + 2 medium; 8yr stale, cycle detection bugs (infinite recursion), null relocation fields, no tests |
| #143 | Declared execution in PluginMgmt lifecycle binding | ChristianSchulte | 2026-07-10T02:34:58Z | COMMENT | 5 high + 3 medium; 8.5yr stale, all paths moved, Plexus→JSR-330 migration, API-breaking interface/exception changes |
| #277 | IGNORE_MISSING policy for unreachable repos | suztomo | 2026-07-10T02:42:16Z | COMMENT | 3 high + 2 medium; stale paths, Wagon dep, semantic concern (DNS ≠ missing), unresolved CHANGES_REQUESTED |
| #159 | Import-scoped dependency resolution | clarkperkins | 2026-07-10T02:42:28Z | COMMENT | 3 high; issue MNG-4347 already closed, both paths stale, Maven 4 architecture eliminates the bug class. Recommend close. |
| #314 | Conditional overwrite version | zhaoyunxing92 | 2026-07-10T02:42:39Z | COMMENT | 3 high + 2 medium; stale path, code refactored into ModelVersionProcessor, projectDir approach questionable, no tests |
| #982 | Implement powershell mvn command (3.9.x) | JurrianFahner | 2026-07-10T02:54:11Z | COMMENT | 2 high + 5 medium; syntax error (unbalanced parens), Test-Path null, MAVEN_ARGS/DEBUG_OPTS scope bugs. #878 must merge first. |
| #878 | Implement powershell command (master) | JurrianFahner | 2026-07-10T02:54:37Z | COMMENT | 7 high + 6 medium; stale assembly paths, missing Maven 4.x features (native-access, mainClass, jline, Java 17 check), syntax error, needs complete rewrite |
| #33 | Add qualityManagement POM element | xaviou | 2026-07-10T02:54:43Z | COMMENT | 5 high + 3 medium; oldest PR (11yr), 6263 commits behind, dead code target, needs Maven 4 immutable model architecture |
| #2277 | Modernize codebase with Java improvements | gnodet | 2026-07-10T03:08:53Z | COMMENT | 2 medium; toList() mutability risk (ImplUtils.map, getProjects), elharo CHANGES_REQUESTED to split. File paths current. |
| #1774 | Cascading profile activation | gnodet | 2026-07-10T03:09:14Z | COMMENT | 1 bug (ReportSet merge discarded) + NPE risk + behavioral changes (CI-friendly parent removed, WARNING→ERROR). Bundles unrelated changes. |
| #71 | Mirror definitions inside profiles | nitram509 | 2026-07-10T03:09:28Z | COMMENT | 2 high + 3 medium; 10yr stale, all paths moved, feature still wanted but not in Maven 4. Inactive profiles lost in createFrom(). |

## Skipped PRs

<!-- PRs intentionally skipped (bot PRs, draft, etc.) -->

| PR | Reason | Since |
|----|--------|-------|
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
