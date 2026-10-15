# Stxck Mixed Item Aggregation — NL verification report

Target: Minecraft 26.1.2, Java 25, NeoForge 26.1.2.109; Create/Gacha integration
checks use 26.1.2.114. Branch: `port/26.1.2`. Same-Type baseline remains commit
`3076966941a293ec720e1bf6d4d382539d4f98ad`; it has not been amended.

Status: functional verification completed for this artifact. The 5,000-unique-component
case still causes multi-second pauses; a separate JFR-guided optimization was requested
and this commit is its measured baseline, not a claim that the latency goal is met.

## Implementation and migration

The NeoForge-only `mixed` package registers a real custom entity with a bounded list
of independent item records. It preserves Data Components and original entity data,
separate ownership, pickup delay, age and lifespan. Original item/tag/dimension
filters apply. Same-Type remains the original implementation and defaults to delay 0.
The only shared refactor extracts its existing filters into `Stxck.isFiltered` so
Mixed can retain distinct unstackable items without changing Same-Type's policy.

The port uses `Identifier`/registry keys, `ValueInput`/`ValueOutput` with codecs,
`SynchedEntityData.Builder`, NeoForge `ModConfigSpec` and the current transactional
`ResourceHandler<ItemResource>` entity capability. The client renderer uses
`ItemClusterRenderState`, `ItemModelResolver` and `SubmitNodeCollector`. It synchronizes
only a representative item and count/short preview, not the complete inventory.
No original required Mixin is disabled. One new ItemEntity lifecycle mixin tracks
stability/cooldown and bounds merge searches when Mixed is enabled.

Ordinary player pickup is executed through real registered ItemEntities and vanilla
`playerTouch`, preserving NeoForge hooks and normal inventory stack limits. Automation
can extract transactionally or use conservative materialization near processing.
Multiple consumers debit a single server-authoritative ledger. Saving during an open
transaction uses committed quantities. Disabling Mixed materializes existing saved
piles when they load/tick. Keep Stxck installed until conversion is finished.

A normal-gravity runtime test exposed a stability bug that no-gravity stress fixtures
could not detect: vanilla accumulates vertical gravity while skipping movement updates
for grounded items. Stability now checks horizontal velocity and unchanged position
on the ground; floating items still require low velocity on all axes.

## Performance methodology

All runs use isolated worlds on NL, one scenario at a time, with no production-world
changes. Burst tests spawn 5,000 single items at one point, no gravity/initial velocity,
and measure the first 100 server ticks. Thus mean/p95/p99 include search and merge work;
`max_mspt` exposes a first-tick spike that p99 can omit with only 100 samples. Creation
is separately timed as `spawn_ms`. Flow tests include creation during the measured tick.
The final quantity and quantity extracted are asserted for every Mixed scenario.

These are single-run measurements, not confidence intervals; JVM warmup and shared
host scheduling can affect comparisons. Per-scenario p95/p99 describe ticks within
that run, not repeated experiment variability.

Server CPU and allocated bytes come from the server thread's JVM counters. Peak heap
is sampled used heap, not RSS or a post-GC retained-size measurement. Extraction timing
is the transactional loop, not a factory's sustained throughput. Flow entity counts
include recently spawned ordinary items still inside the ten-tick stability delay.

`vanilla-policy` in the initial matrix means Stxck loaded with an empty whitelist;
it is not the separate no-Stxck control. The no-Stxck control still uses NeoForge and a
verification mod. Its first attempt failed because unrelated Mixed test classes were
loaded without Stxck; the launch harness now isolates the baseline test classes.

Initial dense-drop timing trials motivated bounding repeated ordinary neighbor
searches. Mixed now limits those searches to 256 per chunk per tick, and avoids full NBT
capture for piles already at capacity. Mixed disabled bypasses this budget entirely.
The earlier 128-search trial regressed the 100-type case and was superseded. Trial logs
are retained as evidence, not release results. No global index or chunk ticket is used.

Initial vs optimized no-gravity component-distinct trial (both 5,000 items, 100 ticks):
mean 1,113.826 → 323.016 MSPT; max 70,035.021 → 7,691.968 ms; allocated 12,489.494 →
576.603 MiB; final 79 Mixed entities. This comparison predates the grounded-stability
fix; release metrics are recorded separately. Very dense distinct-component storms
still cause multi-second pauses and are a known performance limit.

The supplied historical 730.5 / 182.6 / 4.9 MSPT numbers use another measurement phase;
they are not treated as a directly comparable baseline for these burst measurements.

## Compatibility boundaries

Vanilla hoppers exercise both the direct transactional capability and ordinary-drop
fallback. Create checks target real funnel callbacks with competing extraction, then
chute/belt/fan regression callbacks; they are not a complete running factory. Gacha
checks use its actual reward command with a FakePlayer, including individual reward
metadata. Their final result files determine whether they passed on this artifact.

Previous Same-Type AE2/MineColonies checks do not establish Mixed compatibility.
Mekanism, Modern Industrialization, Ender IO, Lootr, complete AE2 networks and
MineColonies job AI are not claimed tested for Mixed. Conservative proximity fallback
covers nearby block entities/NPCs, not arbitrary undocumented item protocols.
Accessory-slot magnets and remote processing may require explicit exclusions/adapters.

## Recommended configuration

Retain Same-Type `mergeDelayTicks=0`. Mixed defaults: enabled=true, radius=2.0,
delayTicks=10, maxEntries=64, maxTotalItems=4096, requireStablePosition=true,
excludeAutomation=true, fallbackToVanilla=true. These are conservative tested settings;
turn Mixed off for a problematic area/modpack until its custom integration is verified.
Do not disable automation fallback merely to maximize aggregation near machines.

See `MIXED_AGGREGATION.md` for exact TOML, `verification/MIXED_NO_DUPES.md` for
conservation checks, and `verification/mixed-evidence/` for logs/CSV/screenshot.
MIT LICENSE and original attribution are retained. The release JAR contains NeoForge
implementation/common classes only; verification classes and Fabric metadata are not
release contents. No push or publication is performed.

## Release stress results (.109)

All rows total 5,000 items; quantity conserved through extraction.

| Scenario | Mean MSPT | p95 ms | p99 ms | Max ms | Mixed / ordinary entities |
|---|---:|---:|---:|---:|---:|
| Mixed 1 types | 6.662 | 3.557 | 5.063 | 488.058 | 0 / 1 |
| Mixed 10 types | 7.744 | 3.860 | 39.733 | 589.507 | 2 / 0 |
| Mixed 100 types | 18.509 | 13.052 | 79.667 | 1598.995 | 2 / 0 |
| 5,000 distinct components | 381.198 | 3279.203 | 3386.542 | 8754.296 | 79 / 0 |
| flow100 items/s | 9.205 | 16.621 | 20.540 | 34.777 | 78 / 45 |
| flow500 items/s | 8.959 | 20.122 | 23.328 | 27.135 | 25 / 100 |

| Scenario | Spawn ms | Server CPU ms | Allocated MiB | Peak heap MiB | Extract ms |
|---|---:|---:|---:|---:|---:|
| mixed/1 | 340.607 | 592.892 | 13.415 | 330.038 | 0.066 |
| mixed/10 | 100.089 | 765.133 | 20.879 | 236.288 | 27.053 |
| mixed/100 | 52.888 | 1797.194 | 116.000 | 241.801 | 31.368 |
| mixed/5000 | 56.773 | 37918.137 | 516.849 | 286.887 | 591.482 |
| flow100/100 | 178.052 | 9074.573 | 1137.419 | 352.538 | 474.995 |
| flow500/100 | 45.331 | 1754.151 | 218.376 | 353.780 | 149.669 |

Mixed ten/100-type and distinct-component bursts complete aggregation at tick 10.
The one-type scenario remains one Same-Type entity and merges at tick 1. Allocation
measurement is allocated bytes, not an exact count of allocated objects per second.

Same-Type disabled-Mixed regression passed 6,149 runtime assertions. Its single-type
5,000-item scenario measured 3.504 MSPT with delay 0 versus 141.510 with delay 10.
These are separate runs from the table above, with their own JIT/warmup state.

## Separate ten/100-type controls (.109)

| Mode | Types | Mean MSPT | p95 ms | p99 ms | Max ms | Final entities |
|---|---:|---:|---:|---:|---:|---:|
| No Stxck | 10 | 1188.082 | 3311.897 | 3551.587 | 3900.863 | 80 |
| No Stxck | 100 | 1004.735 | 2899.494 | 3092.688 | 3131.944 | 100 |
| Same-Type only | 10 | 9.757 | 4.071 | 4.599 | 724.674 | 10 |
| Same-Type only | 100 | 27.434 | 9.612 | 35.573 | 2233.174 | 100 |
| Mixed | 10 | 7.744 | 3.860 | 39.733 | 589.507 | 2 |
| Mixed | 100 | 18.509 | 13.052 | 79.667 | 1598.995 | 2 |

Each row starts with 5,000 items and verifies all 5,000 remain. Mixed reduces entity
count, but its delayed consolidation can make p99 worse than Same-Type alone for ten
types. Mean values alone are not evidence of uniformly lower latency.

## Client results (.109)

The real client connects to the dedicated server, receives 1,000 items / two entries,
and receives all 1,000 into its normal inventory. `mixed-count.png` shows the rendered
pile/count label. Mean FPS in that smoke test was 18.65.

Separate six-scenario steady-state comparison (5,000 items, Xvfb + Mesa software
rendering, render distance 2, simple test platform):

| Mode/types | Mean FPS | Min cached FPS | Max cached FPS | Samples |
|---|---:|---:|---:|---:|
| vanilla-policy-10 | 7.81 | 0 | 14 | 119 |
| vanilla-policy-100 | 10.44 | 8 | 13 | 120 |
| same-10 | 21.77 | 21 | 23 | 120 |
| same-100 | 14.03 | 12 | 16 | 120 |
| mixed-10 | 21.27 | 19 | 23 | 120 |
| mixed-100 | 23.22 | 22 | 25 | 119 |

Samples read Minecraft's cached FPS counter once per client tick; they are not
individual frame-time samples. The first vanilla-policy interval includes an initial
zero counter. These are steady-state render comparisons, not measurements of spawn
packet bursts or predictions for a hardware GPU/modpack scene. Full transient client
profiling and exact allocation object counts are not established by these numbers.

## Integration results (.114)

Create 6.0.11: PASS dual Andesite and Brass funnel callbacks after Mixed fallback,
1,000 items each, including stale-source reuse and custom names. PASS chute callback
and stationary belt extraction (1,000 each). PASS fan BLASTING callback: 64 raw iron
becomes 64 ingots while 936 raw reserve remains unchanged. See
`mixed-create-results.txt`; no full moving factory or airflow scene is claimed.

GachaAddiction: PASS baseline actual reward command tests and a new mixed-world test:
1,000 existing items + 64 actual reward items = 1,064; owner restriction on exactly
64 rewards and custom name components survive aggregation/materialization. Fake
display drops are excluded. This uses a FakePlayer, not the mod's client UI.

Artifact hashes for the integration dependencies are recorded in
`verification/mixed-evidence/integration-artifacts.json`. The Stxck JAR was compiled
against .109 and the identical artifact ran on .114.

## Final artifact and checks

- `:neoforge:build`: BUILD SUCCESSFUL, two JUnit tests, zero failures/skips; no `-x test`.
- Dedicated servers reach `Done` on .109 and .114.
- Final lifecycle test: discard/repeated discard, stale extraction, nonfatal damage/
  repeated damage and deliberate kill pass: 3,000 input = 2,000 ordinary items +
  1,000 intentionally destroyed by explicit kill.
- Core, safety, two hoppers, restart, actual chunk unload/reload, disable conversion,
  Same-Type regression, all six Mixed benchmarks, controls, real client and six FPS
  scenarios pass. Create/Gacha scope is detailed above.
- Targeted fatal-pattern log audit passes; the isolated flat-world generator warning
  and dense-test overload warnings are documented in `release-log-audit.txt`.
- Gradle target/export restored to .109 after integrations. No production world used.

JAR on NL: `/root/home/forks/Staaaaaaaaaaaack/artifacts/mixed/staaaaaaaaaaaack-neoforge-26.1.2-1.8-port.2-mixed.1.jar`

SHA-256: `cd510d537827cf20766be4d345cb7bf89837946485644365feccb5f7a6ad2f93`

Changed files: `verification/MIXED_CHANGED_FILES.txt`. This commit preserves the
original Same-Type commit and serves as the baseline for subsequent latency work.
