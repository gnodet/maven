#!/usr/bin/env bash
# check-pr-work.sh — lightweight precondition for the PR review loop.
# Exits 0 (and prints actionable PR count) if there is work to do.
# Exits 1 if no actionable PRs found — the loop should skip this iteration.
#
# Costs: 1 GitHub API call + 1 per candidate that looks updated (no LLM tokens).
# Usage: ./check-pr-work.sh [path/to/STATE.md]
#
# Dependencies: gh CLI only (uses gh's built-in --jq, no standalone jq needed).

set -euo pipefail

REPO="apache/maven"
STATE_FILE="${1:-STATE.md}"

# 1. Fetch open, non-draft PRs, filter bots and dependency PRs in one call.
#    Output: one "number|updatedAt" per line.
candidates=$(gh pr list --repo "$REPO" \
  --search "is:pr is:open -is:draft" \
  --limit 100 \
  --json number,author,updatedAt,labels \
  --jq '[
    .[] |
    select(
      (.author.login != "dependabot") and
      (.author.login != "renovate") and
      (.author.login != "github-actions") and
      ([.labels[]?.name // empty] | index("dependencies") | not)
    )
  ] | .[] | "\(.number)|\(.updatedAt)"')

if [ -z "$candidates" ]; then
  echo "No open non-draft PRs from humans. Skipping."
  exit 1
fi

total=$(echo "$candidates" | wc -l)

# 2. Extract the "Reviewed PRs" and "Skipped PRs" sections from STATE.md.
reviewed_section=""
skipped_section=""
if [ -f "$STATE_FILE" ]; then
  reviewed_section=$(sed -n '/^## Reviewed PRs/,/^## /p' "$STATE_FILE" | head -n -1)
  skipped_section=$(sed -n '/^## Skipped PRs/,/^## /p' "$STATE_FILE" | head -n -1)
fi

# 3. Compare against reviewed and skipped PRs.
#    A PR needs review if it is:
#      - NOT in the Reviewed table, OR reviewed but updated since
#      - NOT in the Skipped table (permanent skips: bots, too large, etc.)
#    Postponed PRs (in Review Queue but not Reviewed) count as needing work.
needs_review=0
while IFS='|' read -r pr_num pr_updated; do
  [ -z "$pr_num" ] && continue

  # Check if permanently skipped
  if [ -n "$skipped_section" ]; then
    skipped_line=$(echo "$skipped_section" | grep -E "^\| #?${pr_num} \|" 2>/dev/null | head -1 || true)
    if [ -n "$skipped_line" ]; then
      continue  # permanently skipped, don't count
    fi
  fi

  if [ -n "$reviewed_section" ]; then
    reviewed_line=$(echo "$reviewed_section" | grep -E "^\| #?${pr_num} \|" 2>/dev/null | head -1 || true)

    if [ -z "$reviewed_line" ]; then
      # Not reviewed yet (includes postponed PRs from Review Queue)
      needs_review=$((needs_review + 1))
    else
      # Already reviewed — check if updated since
      review_ts=$(echo "$reviewed_line" | awk -F'|' '{print $5}' | xargs)
      # Backward compat: bare dates (2026-07-09) get start-of-day; full
      # timestamps (2026-07-09T23:02:00Z) are compared directly.
      if [[ "$review_ts" != *T* ]]; then
        review_ts="${review_ts}T00:00:00Z"
      fi
      if [[ "$pr_updated" > "$review_ts" ]]; then
        # updatedAt is newer than review — but that could be a comment/label
        # bump rather than a new commit. Check the last commit date to be sure.
        last_commit_ts=$(gh api "repos/$REPO/pulls/$pr_num/commits" \
          --jq '.[-1].commit.committer.date' 2>/dev/null || true)
        if [ -n "$last_commit_ts" ] && [[ "$last_commit_ts" > "$review_ts" ]]; then
          needs_review=$((needs_review + 1))
        fi
        # else: updatedAt bumped by non-commit activity — skip
      fi
    fi
  else
    # No STATE.md or no reviewed section — everything is actionable
    needs_review=$((needs_review + 1))
  fi
done <<< "$candidates"

if [ "$needs_review" -eq 0 ]; then
  echo "All $total PRs already reviewed and up-to-date. Skipping."
  exit 1
fi

echo "$needs_review PRs need review (out of $total open)."
exit 0
