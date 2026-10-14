# Stxck 26.1.2 verification

All verification code lives in `neoforge/src/verification`, outside the release source set and JAR.
Do not use a production world. The probes create/delete their own test entities, place hopper blocks,
force their test chunks, and stop their test server. The production mod does none of this.

## Build and unit tests

```sh
./gradlew :neoforge:build :neoforge:exportVerificationLaunches
```

Java 25 is required. The wrapper uses Gradle 9.2.1. `build` runs JUnit tests; no `-x test` is used.

## Runtime tests

A development launch can be run with `./gradlew :neoforge:runVerifyServer` after creating
`neoforge/run-verify/eula.txt` and `server.properties`. The task fails if its result is incomplete.
Use an isolated loopback port and an empty test world. The functional suite also ticks two real
hopper blocks and then benchmarks immediate/deferred merging of 100, 1000 and 5000 items.

`verification/launch.py` runs exported launch metadata without keeping a Gradle JVM resident:

```sh
python3 verification/launch.py runVerifyServer --artifact --directory run-final-verify
python3 verification/launch.py runPersistenceServer --artifact
python3 verification/launch.py runPersistenceServer --artifact
python3 verification/launch.py runVanillaBenchmark
```

For `--artifact`, place the built Stxck JAR in that run directory's `mods/` folder. This option
excludes production source folders from mod discovery, so the actual JAR is loaded. The separate
verification mod remains present. Without `--artifact`, development source folders are loaded.
`runVanillaBenchmark` excludes Stxck entirely. `--without-mod` does the same for other launch types
(provided their run directory contains no Stxck JAR).

The persistence probe's first process writes a 1000-item named pile and `saved.marker`; its second
process checks the same saved world, removes the test chunk's forced flag, waits until the entity
is actually unloaded, then reloads it and checks count and components. Keep both process logs.

For a real client/server pickup smoke test, launch `runSmokeServer --artifact` (loopback 25580), then
`xvfb-run -a ... python3 verification/launch.py runVerifyClient --artifact`. The verification client
connects, checks the synchronized total, captures a screenshot, and checks 1000 items in inventory.
The server independently checks the pickup and stops itself. See `ClientProbe` and `ClientServer`.

`python3 verification/client_matrix.py` performs the steady-state FPS comparison under Xvfb/llvmpipe.
It first runs without Stxck, then with the release JAR. It prepares post-merge states containing
100/1000/5000 items (2/16/79 vanilla entities, one Stxck entity). It does **not** measure transient FPS
while thousands of entities are spawning. Both immediate and deferred configurations are measured;
once merging has finished their render states should be equivalent. This is not a GPU benchmark.

For the optional MineColonies component integration, place its compatible JAR and dependencies in
`run-integration/mods`, together with Stxck, and set `JAVA_TOOL_OPTIONS=-Dstxck.minecolonies=true`
when launching `runSmokeServer --artifact --directory run-integration`. The probe invokes the real
`CitizenItemUtils.tryPickupItemEntity` and `InventoryCitizen`, with an `ICitizenData` test fixture.
It does not simulate an established colony, worker AI, construction jobs, or pathfinding.

## Results and failure handling

Do not infer PASS from a Java or Gradle exit status alone: inspect the result file and explicit
`STXCK_* PASS` markers. `Verification` writes RUNNING first, records functional results immediately,
then appends benchmark rows. Any caught assertion writes FAIL. The Gradle runtime task additionally
checks that all six benchmark rows completed. A watchdog abort is a failure, not a benchmark result.

`evidence/` contains the recorded logs/CSVs and screenshot; the port report states which are final
artifact tests and which are earlier diagnostic runs. Benchmarks run on a shared host, once per
scenario, and are observational rather than statistically controlled performance claims.


## NL integration runs (NeoForge 26.1.2.114)

The final workspace is on SSH host `nl`, at the same repository path. FI evidence is historical.
`python3 verification/nl_checks.py` runs Create/AE2 callbacks, the functional matrix, a vanilla
baseline, persistence, real client pickup, MineColonies, Gacha rewards and the FPS matrix sequentially.
`--reuse-baseline` reuses the existing same-platform vanilla CSV without repeating its costly dense case.
Integration JARs are local test dependencies under the ignored `neoforge/run-*/mods` directories;
they are not included in the Stxck artifact. Their hashes are recorded in evidence.

CreateProbe exercises actual funnel collision handlers (two consumers), chute fall input, a stationary
belt's collision and inventory path, and fan blasting processing. Inventory draining and fan callback
invocation are test fixtures; this is not a complete rotating factory/airflow scene.
Ae2Probe calls real ItemPickupStrategy with a counting PickupSink (no powered ME network).
GachaProbe invokes the actual reward command using FakePlayer and a named deterministic loot table;
it checks inventory awards, fake visual drops, full-inventory overflow and pickup ownership, not the UI.
All test classes and loot tables remain in the verification source set, outside the release JAR.
