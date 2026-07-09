---
name: review-loop
description: >
  PR review loop for Apache Maven. Lists open PRs, filters already-reviewed
  ones, triages new/updated PRs, and reviews them using sub-agents with
  a maker/checker pattern. Tracks state in STATE.md.
user-invocable: true
---

# PR Review Loop

Automated loop that discovers, triages, and reviews open PRs on Apache Maven.
Uses sub-agents for parallel review and verification, and the oss-helper
`review-pr.md` guideline for the actual review logic.

## Architecture

    Main loop (orchestrator)
      |-- Step 1-4: Triage (inline -- fast, low cost)
      |
      |-- Step 5: Review (sub-agents, parallel)
      |     |-- Reviewer agent PR #1234
      |     |-- Reviewer agent PR #5678
      |     |-- Reviewer agent PR #9012
      |
      |-- Step 6: Verify (sub-agents, one per review)
      |     |-- Verifier agent PR #1234  <-- checks reviewer's findings
      |     |-- Verifier agent PR #5678
      |     |-- Verifier agent PR #9012
      |
      |-- Step 7-8: Post & update state (inline)

- **Reviewer agents** do the deep review work (read diff, git history, project rules).
  Each runs in parallel to avoid serializing expensive diff analysis.
- **Verifier agents** independently check each reviewer's findings for false positives
  before anything is posted to GitHub. This is the maker/checker split.
- **No worktrees needed** -- reviews are read-only (diffs come from `gh pr diff`).

## Execution Steps

### 1. Read Current State
Read `STATE.md` from the repository root. Parse:
- The list of already-reviewed PRs (by number) and their review dates
- The list of skipped PRs (by number) and their skip reasons
- The timestamp of the last run

### 2. Fetch Open PRs
    gh pr list --repo apache/maven \
      --search "is:pr is:open -is:draft" \
      --limit 30 \
      --json number,title,author,createdAt,updatedAt,labels,reviewDecision,additions,deletions,changedFiles

### 3. Filter PRs
Remove from the list:
- PRs already reviewed (unless updatedAt is newer than review date)
- PRs in Skipped table (but re-include CHANGES_REQUESTED PRs that were updated)
- Bot PRs: dependabot, renovate, github-actions
- PRs with label `dependencies`
- PRs that already have an oss-helper review (detect via attribution string in review body), unless updated since

### 4. Triage and Prioritize
Sort by priority:
1. High: REVIEW_REQUIRED with no activity, or >7 days old without review
2. Medium: Recent updates
3. Low: PRs that already have reviews from others

Select top 3 for review.

### 5. Review PRs (Parallel Sub-agents)
For each PR selected for review, spawn one reviewer sub-agent using the Agent tool.
All sub-agents MUST be launched in a **single message** (multiple Agent tool calls)
so they run in parallel.

Each reviewer sub-agent receives:
- The PR number, title, author
- Instructions to use `/oss-review-pr` or the oss-helper `review-pr` skill
- The repository: `apache/maven`
- A reminder to follow the project's code style and Maven conventions

Each reviewer returns structured output:
- **VERDICT**: APPROVE | CHANGES_REQUESTED | COMMENT_ONLY
- **SUMMARY**: 2-3 sentence overview
- **FINDINGS**: list of {file, line, severity, message, suggestion}
- **SUGGESTION_BLOCKS**: GitHub suggestion blocks ready to post
- **GENERAL_COMMENTS**: high-level observations

### 6. Verify Findings (Parallel Sub-agents)
For each review that produced findings (CHANGES_REQUESTED or findings list non-empty),
spawn one verifier sub-agent. All verifiers launched in a single message for parallelism.

Each verifier:
- Receives the reviewer's findings and the PR diff
- Independently re-checks each finding against the actual diff and git history
- Marks each finding as CONFIRMED or FALSE_POSITIVE with reasoning
- Returns VERIFIED_FINDINGS list

### 7. Post Reviews to GitHub
For each PR with verified findings:

If verdict is APPROVE:
    gh api repos/apache/maven/pulls/<NUMBER>/reviews \
      --method POST \
      -f event=APPROVE \
      -f body="<summary + attribution>"

If verdict is CHANGES_REQUESTED or has confirmed findings:
    gh api repos/apache/maven/pulls/<NUMBER>/reviews \
      --method POST \
      -f event=COMMENT \
      -f body="<findings + suggestions + attribution>"

**IMPORTANT**: Never post `event=REQUEST_CHANGES` — always use `COMMENT` for
findings.

### 8. Update State
Update `STATE.md`:
- Set Last Run timestamp to now
- Add reviewed PRs to Reviewed table with verdict and notes
- Add newly skipped PRs to Skipped table
- Clear Review Queue of processed PRs
- Update PRs checked count

### 9. Push State to Fork
    git add STATE.md loop-run-log.md loop-ledger.json
    git commit -m "chore: update PR review loop state"
    git push origin pr-review-loop-state

### 10. Summary
Output a brief summary:
- Number of PRs triaged
- Number of reviews posted
- Number of findings (total vs confirmed vs false positive)
- Any PRs escalated for manual review

## Constraints

- Read STATE.md before fetching PRs
- Spawn reviewer agents in parallel (all in one message)
- Spawn verifier agents in parallel after reviewers complete
- Only post verified findings
- Post reviews with AI disclaimer
- Update STATE.md after every run
- Respect 3-PR-per-iteration limit
- NEVER merge, close, or label PRs
- NEVER review draft PRs
