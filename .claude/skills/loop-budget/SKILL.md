---
name: loop-budget
description: Check token budget and run-log spend before and after a loop run. Enforces early exit when over budget or when there is no actionable work.
---

# Loop Budget Guard

Run at the **start** and **end** of every loop iteration.

## Start of run
1. Read loop-budget.md for daily caps and kill-switch flags.
2. Read recent entries in loop-run-log.md (last 24h).
3. Sum tokens_estimate for the active pattern today.
4. If spend >= 80% of daily cap --> report-only mode.
5. If spend >= 100% or loop-pause-all is set --> exit immediately.
6. If no actionable items --> exit in <5k tokens.

## End of run
Append one JSON object to loop-run-log.md with run stats.

## Rules
- Never exceed max sub-agent spawns/run from loop-budget.md.
- High-cadence patterns must early-exit when nothing is actionable.
- On self-throttle, append to loop-budget.md under Alerts This Period.
