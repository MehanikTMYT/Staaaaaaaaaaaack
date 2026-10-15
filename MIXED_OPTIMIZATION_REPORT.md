# Mixed Aggregation latency optimization — mixed.2

Platform: Minecraft 26.1.2, NeoForge 26.1.2.109, Java 25, NL (Ubuntu 24.04).
Baseline artifact: mixed.1, commit b05118d9934f30322f36462bff6c106b68f45de4,
SHA-256 cd510d537827cf20766be4d345cb7bf89837946485644365feccb5f7a6ad2f93.
The independent Same-Type baseline commit 3076966941a293ec720e1bf6d4d382539d4f98ad
was not amended. No push or release publication was performed.

## Evidence-led changes

JFR was recorded before optimization, with 5 ms execution sampling and separate
creation, initial aggregation, serialization and extraction events. The fixture
creates 5,000 ordinary ItemEntities at one position: 100 base types, each with a
unique CUSTOM_NAME component. No gravity prevents dispersion. Headless and real
connected-client runs are separate; both use 1,000 measured active ticks and assert
exactly 5,000 items every tick, then extract exactly 5,000 and assert zero remaining.
All mutations happen on the server thread. Test fixtures force their own test chunk;
production code never adds chunk tickets or requests unloaded chunks.

The baseline headless JFR had 6,576 server execution samples: 4,611 in entity
collision searches, 436 in Same-Type neighbour merging, 303 in other Mixed search/
merge work, 77 in source NBT capture and 8 in sync-summary preparation. Full stacks
showed repeated EntitySection traversal through ItemEntity.tick/noCollision and
Entity.move/collide. This established collision candidate traversal as the dominant
cost; serialization and network traffic were not assumed to be the cause.

Changes:

- A section-maintained non-item collision candidate set retains custom ItemEntity
  subclasses and other entities while omitting non-collidable vanilla drops/piles.
- A tick-local full-component key cache avoids repeatedly testing all distinct
  stacks in Same-Type candidate searches while Mixed is enabled. Live checks remain.
- Loaded mixed piles are indexed in four-block cells and by item resource, excluding
  full piles unless a matching record could accept more. Lifecycle hooks maintain it.
- A weak-reference FIFO performs aggregation in cooperative 8 ms slices per dimension,
  at most 256 absorbed entries per tick. An initial pair completes atomically and may
  overrun the time slice. This is not a hard bound on total server tick duration.
- Batched absorption publishes one summary update per batch; immutable resource keys
  and internal transaction metadata are reused. External snapshots remain copies.

Player pickup, item-handler transactions and fallback are immediate, independently
of the queue. Queued ordinary items continue ticking and interacting normally.
Mixed-disabled paths use the original Same-Type search and collision behavior.

## Comparable 1,000-tick JFR runs

Milliseconds unless stated otherwise. Percentiles use nearest rank; with only
1,000 samples, p99.9 is near the maximum and is not a population estimate.

| Scenario | Mean MSPT | p95 | p99 | p99.9 | Maximum | Aggregation wall time | Final piles |
|---|---:|---:|---:|---:|---:|---:|---:|
| Baseline headless | 37.380 | 5.038 | 42.563 | 3529.572 | 7915.603 | 34.276 s | 79 |
| Optimized headless | 10.370 | 41.240 | 92.116 | 186.593 | 711.410 | 10.638 s | 79 |
| Baseline connected | 73.614 | 28.493 | 130.926 | 6927.882 | 15837.424 | 60.784 s | 79 |
| Optimized connected | 21.991 | 74.254 | 155.427 | 619.077 | 1174.882 | 18.407 s | 79 |

The queue spreads work across more ticks: completion changes from tick 10 to 193
headless / 279 connected, but wall time improves because catastrophic ticks are
removed. p95 and p99 worsen across the full window; p99.9 and maximum improve sharply.
The first 100 ticks average 344.073 → 56.808 ms headless and 611.212 → 102.950 ms
connected. This is a latency-distribution tradeoff, not an improvement in every metric.

Creation is outside these active-tick statistics and measured separately:

| Phase | Baseline headless | Optimized headless | Baseline connected | Optimized connected |
|---|---:|---:|---:|---:|
| Create/add 5,000 entities, ms | 332.700 | 362.271 | 1936.754 | 1297.252 |
| Serialize all ledgers + NBT text, ms | 582.754 | 507.441 | 740.916 | 549.522 |
| Synchronously extract all 5,000, ms | 630.626 | 1292.471 | 1490.390 | 1207.503 |
| Server-thread active-window CPU, ms | 36652.759 | 9850.123 | 58625.649 | 17874.157 |
| Server-thread allocated MiB | 553.837 | 1148.200 | 698.887 | 1874.368 |

Creation of 5,000 entities by a caller in one synchronous operation can still stall
a connected server for about 1.3 seconds. The largest active tick is still 1.17 s.
This eliminates the observed multi-second aggregation pauses, not all visible lag.
The artificial synchronous drain of all records can also exceed one second; normal
individual mechanism transfers are not queued. Serialization includes text conversion
for verification, not disk save time. Using the wall interval from aggregation start
to serialization start, measured server-thread allocation rates are 7.85 → 22.52
MiB/s headless and 6.95 → 35.04 MiB/s connected. These exclude the verification
assertions outside the measured tick body. The matrix unique-component peak sampled
heap is 345.4 MiB (baseline 286.9 MiB); the optimized 100-tick matrix still contains
ordinary remainders. Exact objects/second was not instrumented; JFR sampled allocation
weights and class breakdowns are retained instead. Increased allocation reflects the longer-lived
ordinary population and budgeted processing; bytes are not object counts.

Final headless JFR: 2,035 server samples, 285 collision, 19 Same-Type neighbour,
144 other Mixed search/merge, 78 source capture, 25 summary preparation. Connected
packet bytes over the full recording increased from 910,792 to 1,058,335, consistent
with the longer-lived ordinary population and incremental updates. Packet totals
include tracking/control traffic; server-thread network samples do not measure all
Netty thread CPU. Raw JFR preserves the full thread/event evidence.

## Ordinary scenarios and ongoing flow

Same 100-active-tick matrix methodology as mixed.1. Fresh final run:

| Scenario (5,000 items) | Mean MSPT | p95 | p99 | Max | Mixed/ordinary entities after 100 ticks |
|---|---:|---:|---:|---:|---:|
| Mixed, one type | 2.765 | 0.834 | 6.910 | 234.635 | 0 / 1 |
| Mixed, 10 types | 4.963 | 1.374 | 40.430 | 385.890 | 2 / 0 |
| Mixed, 100 types | 4.381 | 9.643 | 10.324 | 312.800 | 2 / 0 |
| Mixed, 5,000 unique components | 37.192 | 58.806 | 123.613 | 160.477 | 43 / 2250 |
| Flow, 100 items/s | 3.705 | 5.383 | 7.061 | 40.428 | 78 / 45 |
| Flow, 500 items/s | 2.732 | 4.521 | 7.508 | 10.573 | 25 / 100 |

10-type and 100-type completion occurs at tick 11 / 15. Baseline means were 7.744 /
18.509 ms and maxima 589.507 / 1598.995 ms. The unique-component row is explicitly
an incomplete 100-tick snapshot; use the JFR runs above for full completion. Flow
remainders include fresh ordinary items still within the stability delay.

Mixed-disabled control: 10 types mean 9.204 ms, p95 3.554, p99 5.160, max 694.970,
10 entities; 100 types mean 23.025 ms, p95 6.503, p99 34.736, max 1914.260,
100 entities. The intentionally unoptimized 5,000-unique Same-Type control retains
5,000 entities and has very long ticks (85.775 s maximum). It is isolated from the
Mixed measurements. No-Stxck controls from mixed.1 remain in mixed-evidence; they
were not rerun because their code/environment path was unchanged.

Single runs on a shared four-vCPU host, no confidence intervals. Cold JIT, GC,
software-rendered client contention and scenario order affect absolute numbers.
JFR/CSV creation is verification-only and is absent from the release JAR.

## Correctness and lifecycle verification

The optimized artifact passed fresh-world checks for full component key identity,
in-place mutation revalidation/next-tick discovery, cross-section movement without
duplicate references, removed candidates, and collision equivalence (including
custom collidable ItemEntities). Core checks passed automatic aggregation, partial
player pickup, simulated extraction rollback, nested transactions, committed
extraction, stale handlers, save during an open transaction, metadata serialization,
cancelled fallback spawn and disabling Mixed.

Safety, independent timers/ownership, ordinary gravity/support fallback, repeated
removal/damage and two-hopper extraction passed. Persistence used a saved world,
a new server process, actual chunk unload/reload, then disabling Mixed to unpack.
The original Same-Type suite passed 6,140 functional assertions plus hopper and benchmark checks with Mixed disabled. The stress
runs conserved all 5,000 items at every measured tick and on complete extraction.
These are finite tests, not a proof against every third-party interaction.

## Artifacts and reproducibility

Build: `./gradlew :neoforge:build :neoforge:exportVerificationLaunches` (no skipped tests).
Regression driver: `python3 verification/mixed_optimization_checks.py`.
Client: `python3 verification/mixed_optimization_client.py`; FPS:
`python3 verification/mixed_optimization_fps.py`. Export launches with
`-Pneo_version=26.1.2.114` before `mixed_optimization_integrations.py`, then restore
the default .109 export. Integration JARs must already exist in run-create/run-gacha.
Connected profile: `python3 verification/mixed_profile_connected.py <fresh-label>`.
Set STXCK_JAR to the archived mixed.1 JAR to reproduce the baseline. Use fresh run
labels/worlds and run performance cases sequentially without competing profiling jobs.

Evidence: `verification/optimization-evidence/`, with raw timelines, results, logs,
JFR summaries and `profile-comparison.json`. Raw recordings are retained on NL in
`artifacts/mixed-profiles/` with SHA256SUMS, outside Git. Historical mixed.1 evidence
and its artifact remain intact. Final artifact/checksum and integration/client results
are recorded below.

Recommended initial settings: Same-Type mergeDelayTicks=0; Mixed radius=2,
delayTicks=10, maxEntries=64, maxTotalItems=4096, maxMergeMillisPerTick=8,
maxMergedEntriesPerTick=256, stable-position/automation-exclusion/fallback enabled.
Do not increase radius or entry count without profiling. This budget is per dimension,
so simultaneous bursts in multiple dimensions can consume multiple slices.

## Final client test (.109)

Real client connection passed: count_sync=true, inventory=1000. A screenshot of
the mixed-pile overlay is preserved as optimization-evidence/mixed-count.png.
The connected unique-component test separately observed all 5,000 synchronized
items. The ledger remains authoritative on the server.

Steady-state FPS (Mesa software rendering under Xvfb, distance 2, cached Minecraft
FPS counter, 5,000 items):

| Mode | 10 types mean FPS | 100 types mean FPS |
|---|---:|---:|
| Vanilla-policy | 2.37 | 10.25 |
| Same-Type | 16.34 | 10.89 |
| Mixed | 15.38 | 18.39 |

These are within-run comparisons on a CPU-rendered client sharing server resources.
They are not hardware-GPU estimates. Values across separate dated runs vary; all
three modes were lower than the historical run, so no cross-run FPS speedup is
claimed. Client logs show successful rendering/networking rather than a complete
visual regression suite.

## Final artifact

NL path: `/root/home/forks/Staaaaaaaaaaaack/artifacts/mixed/staaaaaaaaaaaack-neoforge-26.1.2-1.8-port.2-mixed.2.jar`

SHA-256: `9d44c37b1e27180baf93a22bb2a7cc4c45918076bb1fdaf31393f7d901bb387e`

89,513 bytes. Contains the MIT license and NeoForge implementation; no Fabric
loader metadata or verification classes. modId remains `staaaaaaaaaaaack`.
The original author and license notices remain unchanged.

## Create integration (.114)

The exact .109-built release JAR was loaded on NeoForge 26.1.2.114 with the available
Create 6.0.11 port. Dedicated startup reached Done. Both real Andesite and Brass
Funnel entityInside callbacks received materialized drops; dual inputs extracted
1,000 items, leaving zero, including repeated stale-source calls and custom names.
Real chute and stationary belt handler callbacks also extracted exactly 1,000.
Fan BLASTING converted 64 raw iron to 64 ingots and left the 936-item reserve intact.
These exercise real mod callbacks with downstream extraction fixtures, not a moving
factory or a complete fan airflow scene. See mixed-create-results.txt and its log.

The Create loading environment emits dedicated-server class-cleaner diagnostics for
four client classes, also present in the baseline mixed.1 integration log. They did
not prevent startup or the tests. Fresh flat-world presets emit “No key layers in
MapLike[{}]”; offline clients warn about missing chat-session keys. These are retained
in raw evidence rather than presented as completely clean logs.

Untested Mixed integrations remain AE2 storage networks, Mekanism, Modern
Industrialization, Ender IO, Lootr and MineColonies AI, plus accessory-slot magnets.
Nearby detectable automation uses conservative ordinary-ItemEntity fallback; this
is not a guarantee for unknown remote interactions. Keep fallback/exclusions enabled.

## GachaAddiction integration (.114)

Both the real GachaCommand baseline and the Mixed-aware reward test passed.
Free inventory received 64 rewards alongside 1,000 existing ground items. With a
full inventory, the total was 1,064 after 40 world ticks; the 64 reward items retained
owner restrictions and custom components, fake display items were excluded, and
unpacking conserved the exact total. This uses a FakePlayer and the real command,
not the Gacha UI. See gacha-results.txt and mixed-gacha-results.txt.

## Release validation summary

The final .109 build includes two JUnit tests (zero failures/skips), dedicated runtime
regressions, real client connection/pickup, FPS matrix, and JFR stress cases. Dedicated
servers reached Done on both .109 and .114. No tests were bypassed with `-x test`.
`verification/audit_optimization.py` checks final logs for mixin/linkage, packet,
serialization and assertion failures and verifies the artifact packaging. Its result
is retained in optimization-evidence/audit.txt. The final .109 export restores the
normal development target after .114 integration runs; gradle.properties remains .109.

The separate conservation report is `verification/MIXED_OPTIMIZATION_NO_DUPES.md`.
Changed files are listed in `verification/MIXED_OPTIMIZATION_CHANGED_FILES.txt`.
Remaining limits include 0.7–1.3 s cold burst/creation stalls, higher full-window
p95/p99 and allocation rate, cooperative rather than hard tick budgeting, a possible
one-tick candidate-discovery delay after in-place component mutation, software-only
FPS evidence, and the untested third-party integrations listed above. The measured
multi-second aggregation pauses were removed in this fixture; universal latency or
compatibility guarantees are not claimed.
