# Mixed aggregation verification

Baseline Same-Type commit: `3076966941a293ec720e1bf6d4d382539d4f98ad` (not amended).
Core target: NeoForge 26.1.2.109, Java 25. Create/Gacha target: 26.1.2.114.

Production code lives in the separate `mixed` package and one ItemEntity lifecycle mixin.
The custom entity holds independent records and never masquerades as an ItemEntity.
Player pickup materializes real registered ItemEntities and invokes vanilla pickup,
including NeoForge pre/post hooks. Transactional extraction commits once; simulation
rolls back. Serialization during a transaction uses the last committed ledger.
Block-entity automation, nearby non-player living entities, recognized magnets,
fluids, unsupported ground, and portals trigger conservative ordinary-item fallback.
This is not a universal magnet/mod compatibility claim.

## Reproducible checks

- `./gradlew :neoforge:build :neoforge:exportVerificationLaunches`
- `python3 verification/mixed_release_checks.py` runs the final regression suite in
  fresh timestamped worlds, plus client, no-Stxck control and steady-state FPS checks.
  Never delete a production world to reset a test.
- `python3 verification/mixed_checks.py --client-only` repeats the connected client check.
- `python3 verification/mixed_launch.py mixedHoppers run-mixed-hoppers`
- `python3 verification/mixed_launch.py mixedBenchmark run-mixed-matrix`
- Integration launches use `-Pneo_version=26.1.2.114`, then re-export launch arguments.
  Never change the platform while an exported launch is running.

The benchmark includes the first 100 ticks after spawning 5,000 items at one point,
with zero initial velocity and no gravity. Spawn time is separate. Percentiles use
nearest ranks 95 and 99 over 100 samples; max records the transient merge spike.
CPU/allocated bytes are server-thread counters; peak heap is sampled used JVM heap.
Extraction is a timed transaction loop, not a complete factory throughput benchmark.
`vanilla-policy` disables Stxck aggregation through empty whitelist configuration;
it still loads Stxck and is not a pristine vanilla installation.

Evidence and limitations belong in the final report; a test source or successful
compilation does not establish runtime compatibility.

The no-Stxck control uses only the isolated `MixedVanillaBenchmark` verification
class. Other verification classes reference Stxck types and must not be discovered
when the release JAR is absent. Integration results belong to .114; re-export .109
launch arguments afterward. The default Gradle target remains .109.
