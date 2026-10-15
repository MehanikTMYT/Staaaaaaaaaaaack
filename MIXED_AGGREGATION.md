# Mixed Item Aggregation

An optional NeoForge-only extension to Stxck. The existing Same-Type implementation
remains independent, with `mergeDelayTicks = 0`. The base implementation is preserved
in commit `3076966941a293ec720e1bf6d4d382539d4f98ad`.

## Configuration

`config/staaaaaaaaaaaack-common.toml`:

```toml
[mixedAggregation]
enabled = true
radius = 2.0
delayTicks = 10
maxEntries = 64
maxTotalItems = 4096
requireStablePosition = true
excludeAutomation = true
fallbackToVanilla = true
maxMergeMillisPerTick = 8.0
maxMergedEntriesPerTick = 256
```

The delay applies only to aggregation. It never adds a pickup delay. Existing piles
are not truncated when limits decrease. Disabling Mixed unpacks saved piles when
loaded/ticked, even if `fallbackToVanilla` is false. Keep the mod installed to unpack
its custom entities before removing it from a world.

## Data and interactions

`staaaaaaaaaaaack:mixed_pile` is a real custom Entity, not an ItemEntity subclass or
an invisible block container. Each entry stores a component-complete item template,
quantity, original entity data, owner restrictions, independent age, pickup delay,
and lifespan. Equal components alone are insufficient to coalesce records: their
remaining metadata and timers must also agree. This can create multiple entries
for the same visual item, deliberately preserving their distinct rules.

The NeoForge `ENTITY_AUTOMATION` capability exposes an extraction-only transactional
`ResourceHandler<ItemResource>`. Rollback restores records; entity removal and
network updates occur after commit. A stale handler cannot extract from a removed
pile. Pending simulated counts are not written into world saves.

Player contact temporarily materializes real registered ItemEntities and invokes
ordinary `playerTouch`, preserving NeoForge pickup hooks and inventory limits.
Uncollected remainders return to the ledger; event-modified items that no longer fit
remain ordinary drops. Cancelled materialization retains the original ledger.
Despawn decisions and damage are delegated to materialized ordinary items.

## Conservative automation fallback

Aggregation is excluded near block entities (4 blocks), Create fans (32 blocks),
non-player living entities (8 blocks), and nearby players carrying inventory items
whose registry IDs contain `magnet` or `attractor`. Existing piles unpack if these
conditions appear. Fluids, portals and loss of supporting terrain also trigger
fallback. Materialized items have a 100-tick re-aggregation cooldown, without an
artificial pickup delay. No chunk tickets or global per-tick item scans are used. While Mixed is enabled,
ordinary neighbour searches have a per-chunk, per-tick budget of 256 to prevent
quadratic first-tick work on thousands of component-distinct drops. This limit does
not apply with Mixed disabled or to materialized items during their cooldown.

This is not an exhaustive integration registry. Magnets in accessory slots,
remote/custom processing with no detectable nearby entity or block entity, and
unusual mod-specific ItemEntity protocols require an adapter or an exclusion.
Do not claim blanket compatibility for untested mods. Custom ItemEntity subclasses
and custom entity types are excluded. Shared Stxck item/tag/dimension filters apply.

## Client

Only a representative item, total count, entry count and short composition preview
are synchronized. The authoritative full ledger stays on the server. One renderer
uses one model state with up to three visual copies. Text is limited to 8 blocks;
holding Shift shows the first three composition entries instead of the total label.

See `verification/MIXED_TEST_PLAN.md`, `verification/mixed-evidence/`, and the final
Mixed report for measured results and the exact scope of integration tests.

## Bounded aggregation work (mixed.2)

Stable ordinary drops enter a server-thread FIFO queue. Each dimension spends at
most the configured cooperative time slice on aggregation, with a separate entry
limit. An initial two-item operation finishes atomically, so the time limit is not
a hard deadline. Ordinary ticking, pickup and automation continue while queued;
eligibility is checked again before transfer. No entity mutation runs in background
threads. The queue uses weak references and does not keep chunks loaded.

Loaded mixed piles use a four-block spatial index, updated on addition, removal,
movement and content changes. Full piles are skipped unless a matching item key
could coalesce. Internal transaction snapshots share immutable source metadata;
external snapshots remain defensive copies. Summary synchronization happens once
per processed batch rather than once per absorbed source.

While Mixed is enabled, ordinary-item collision queries exclude exact vanilla
ItemEntities and mixed piles, which cannot collide, while retaining custom item
subclasses and all other candidates. A section-local, tick-scoped full-component
key cache accelerates Same-Type candidate lookup. Every candidate is revalidated
against its live components. An in-place component mutation can delay discovery
until the next tick; it cannot permit merging different components. Disabling
Mixed restores the original collision and Same-Type query paths.

See `MIXED_OPTIMIZATION_REPORT.md` for profiling, latency and conservation evidence.
