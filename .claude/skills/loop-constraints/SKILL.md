---
name: loop-constraints
description: >
  Read loop-constraints.md at the start of every run and enforce every rule.
  This skill runs BEFORE triage or any action skill. Constraints are binding.
user_invocable: true
---

# Loop Constraints Enforcer

Before any other work begins, you MUST:
1. Read loop-constraints.md from the project root.
2. Load every rule into your working memory.
3. Check if loop-pause-all is active --> exit immediately.
4. Apply these rules to EVERY action that follows.

## Default constraints (when no file exists)
- Never edit .env, .env.*, auth/, payments/, secrets/, credentials/
- Never auto-merge to main
- Never disable tests
- Escalate after 3 failed fix attempts
