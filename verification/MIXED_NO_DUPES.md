# Mixed conservation verification

Release candidate: `cd510d537827cf20766be4d345cb7bf89837946485644365feccb5f7a6ad2f93`.
Runtime checks use the built JAR in an isolated dedicated server, not the production
source folder. Test classes are a separate verification mod and are absent from the JAR.

Same-Type regression with Mixed disabled: 6,149 runtime assertions pass, including
real hoppers; independent Same-Type delay 0 is unchanged.

## Passed on NeoForge 26.1.2.109

- Ordinary gravity drops settle and aggregate; removing their supporting block
  materializes all 128 items.
- Automatic aggregation of 1,000 items of ten types; quantity retained.
- Nearly full player inventory: one available slot item transferred, remainder retained.
- Transaction simulation/rollback, execute, nested commit followed by outer abort.
- Serialization during an open transaction retains committed counts.
- Repeated extraction and stale handler after complete removal cannot extract twice.
- Cancelled ordinary-entity spawn retains the corresponding ledger quantity.
- Individual owner, pickup delay, age/lifespan and damaged unstackable item records.
- Two real ticking vanilla hoppers: direct capability route 26 stored + 974 on ground;
  conservative fallback route 640 stored + 360 on ground; each starts with 1,000.
- A fresh process saves a mixed pile, another process reloads it, forces actual chunk
  unload and reload, verifies UUID/components/1,000 items, then disables aggregation
  and verifies ordinary items total 1,000.
- Real connected client receives total 1,000 / two entries; server and client both
  report inventory 1,000 after pickup. Screenshot confirms the count overlay.
- Final stress matrix asserts quantity before and after extraction for all six
  scenarios: one/ten/100 types, 5,000 different components, streams of 100/500 items/s.

Evidence: `mixed-evidence/release-mixed-results.txt`, `release-mixed-safety.txt`,
`release-mixed-hoppers.txt`, `release-mixed-persistence.txt`, `release-mixed-matrix.txt`,
corresponding logs and `mixed-client.txt`.

These results establish the tested invariants, not a proof against arbitrary external
mods mutating item/entity state. Unknown processing protocols require exclusions or
adapters. Deliberate vanilla destruction (`/kill`, fire after materialization, normal
expiry) is a legitimate removal and is not treated as an inventory transfer.

## Passed on NeoForge 26.1.2.114

- Dual Andesite/Brass funnels after Mixed materialization: 1,000 input = 1,000 stored,
  repeated stale-source callbacks do not duplicate; component names retained.
- Chute, stationary belt, fan processing regression callback checks pass.
- Real Gacha command reward: 1,000 existing + 64 owner-restricted = 1,064 after
  automatic aggregation and unpack; individual reward components/owner retained.

See `mixed-create-results.txt`, `gacha-results.txt`, `mixed-gacha-results.txt`.

Lifecycle release test (.109): repeated discard/damage and stale extraction pass;
3,000 input = 2,000 ordinary + 1,000 deliberate kill. See `release-mixed-lifecycle.txt`.
