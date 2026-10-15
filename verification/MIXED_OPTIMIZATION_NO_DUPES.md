# Optimized Mixed conservation evidence

Artifact: mixed.2, SHA-256
`9d44c37b1e27180baf93a22bb2a7cc4c45918076bb1fdaf31393f7d901bb387e`.

| Check | Observed result | Evidence under optimization-evidence/ |
|---|---|---|
| Automatic aggregation and partial player pickup | PASS; only accepted inventory quantity removed | mixed-results.txt, mixed.log |
| Simulated extraction / rollback / nested transactions | PASS; uncommitted quantity restored | mixed-results.txt, mixed.log |
| Save inside open transaction | PASS; committed state serialized | mixed-results.txt, mixed.log |
| Repeated stale-handler extraction | PASS; no repeated output | mixed-results.txt, mixed-lifecycle.txt |
| Real ticking pair of vanilla hoppers | PASS; direct capability and ordinary fallback, 1,000 items each | mixed-hoppers.txt |
| Server restart and chunk unload/reload | PASS; quantities, components and entity UUID preserved | mixed-persistence.txt, mixedPersistence-save.log, mixedPersistence.log |
| Disable Mixed with saved piles | PASS; loaded records safely materialize | mixed-persistence.txt |
| Damage / discard / repeated removal | PASS; 3,000 input = 2,000 surviving ordinary items + 1,000 explicit kill destruction | mixed-lifecycle.txt |
| Independent ownership/timers and damage components | PASS | mixed-safety.txt |
| Ground/support fallback | PASS; all 128 items materialize | mixed-ground.txt |
| Key cache lifecycle and mutation | PASS; no duplicate references, live component revalidation | mixed-key.txt |
| Unique-component stress | Exactly 5,000 before/after every one of 1,000 ticks; final extraction 5,000, remainder 0 | optimized-headless-ticks.csv, optimized-connected-ticks.csv, corresponding profile-result.txt |
| Real client pickup | Synchronized 1,000; inventory receives 1,000 | mixed-client.txt |

Collision-query optimization is separately checked against vanilla results for
ordinary/custom items, boats, section movements, removals, null/non-item sources
and unloaded areas. None of these indexes owns item quantities; the existing
transactional ledger remains the authority.

Third-party integration results and their limited scope are recorded in
../MIXED_OPTIMIZATION_REPORT.md. Tests do not prove compatibility with arbitrary
mods or every possible component payload. The worst-case performance fixture uses
unique CUSTOM_NAME components, not 5,000 deeply nested inventory components.
