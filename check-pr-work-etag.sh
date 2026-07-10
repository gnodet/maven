#!/usr/bin/env bash
# check-pr-work-etag.sh — Two-tier precondition for the PR review loop.
#
# Tier 1: ETag-based conditional request to GitHub Events API.
#   - 304 Not Modified → no repo activity since last check → exit 1 (free, no API cost)
#   - 200 OK → activity detected → fall through to tier 2
#   - Any error → fall through to tier 2 (never block the loop on ETag failures)
#
# Tier 2: Existing check-pr-work.sh — detailed PR-level check (1+ API calls).
#
# Net effect: idle periods cost zero API calls. Only repo activity triggers
# the detailed check. Safe to increase cadence (e.g. 30m) since idle polls are free.
#
# Usage: ./check-pr-work-etag.sh [path/to/STATE.md]

set -euo pipefail

REPO="apache/maven"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ETAG_FILE="${SCRIPT_DIR}/.pr-loop-etag"
HEADER_TMP=$(mktemp)
BODY_TMP=$(mktemp)
trap 'rm -f "$HEADER_TMP" "$BODY_TMP"' EXIT

# Load cached ETag from previous run (if any)
CACHED_ETAG=""
[ -f "$ETAG_FILE" ] && CACHED_ETAG=$(cat "$ETAG_FILE")

ETAG_HEADER=()
[ -n "$CACHED_ETAG" ] && ETAG_HEADER=(-H "If-None-Match: $CACHED_ETAG")

# Conditional request to Events API — 304 is free (no rate-limit cost)
HTTP_CODE=$(curl -s -o "$BODY_TMP" -D "$HEADER_TMP" -w "%{http_code}" \
  -H "Authorization: token $(gh auth token)" \
  -H "Accept: application/vnd.github+json" \
  "${ETAG_HEADER[@]}" \
  "https://api.github.com/repos/${REPO}/events?per_page=5" 2>/dev/null) || true

# Persist new ETag for next run
NEW_ETAG=$(grep -i '^etag:' "$HEADER_TMP" 2>/dev/null | awk '{print $2}' | tr -d '\r\n' || true)
[ -n "$NEW_ETAG" ] && echo -n "$NEW_ETAG" > "$ETAG_FILE"

case "$HTTP_CODE" in
  304)
    echo "No repo activity (ETag 304, free). Skipping."
    exit 1
    ;;
  200)
    echo "Activity detected. Running detailed check..."
    exec "${SCRIPT_DIR}/check-pr-work.sh" "$@"
    ;;
  *)
    echo "Events API returned ${HTTP_CODE:-error}. Falling through to detailed check..."
    exec "${SCRIPT_DIR}/check-pr-work.sh" "$@"
    ;;
esac
