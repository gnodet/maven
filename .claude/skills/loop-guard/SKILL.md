---
name: loop-guard
description: >
  Circuit breaker for fix-capable loops. Before each iteration, append the last
  attempt to loop-ledger.json and run loop-context --check; if it escalates,
  stop and hand the human a clean summary instead of looping in vain.
user_invocable: true
---

# Loop Guard (Circuit Breaker)

Keeps a fix loop from burning tokens on unsolvable problems.
Tracks attempts in loop-ledger.json.
Uses deterministic exit-code-based circuit breaker:
- Exit 0 --> continue
- Exit 2 --> STOP (same error Nx, too many failures, budget exceeded)

Defaults: 3x same error, 5 consecutive failures, 10 iterations.
