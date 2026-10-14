# Staaaaaaaaaaaack [![](https://img.shields.io/modrinth/dt/myL7fuqp?label=modrinth)](https://modrinth.com/mod/staaaaaaaaaaaack) [![](https://cf.way2muchnoise.eu/short_staaaaaaaaaaaack_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/staaaaaaaaaaaack) 

This mod allows dropped items to merge, even if their stacks are already full. This can help reduce lag caused by a large number of drops in a small area.

It should be compatible with most mods, but please report any issues if it does not work for the mod you're playing with.

Current features:
- Merging drops over their stack limit without changing the actual maximum stack size
- Customizable merge distance for drops.
- Customizable maximum size for drops.
- Customizable overlay size and render distance.
- Blacklist or whitelist configuration for items that you don't want to exceed their default stack limit by name, 
  tag or dimension. You can add them from the common config file or assign them a specific item tag.

The name contains 12 'a's btw. 🍔

![Mod show case](https://cdn.modrinth.com/data/myL7fuqp/images/ae86decac06068d8aed8af5aa82648d83b2ddb6e.gif)


| MC version | NeoForge | Fabric |  Forge   |
|:----------:|:--------:|:------:|:--------:|
|   1.21.8   |   main   |  main  |    -     |
|   1.21.1   |  active  | active |    -     |

## Minecraft 26.1.2 / NeoForge port

The `port/26.1.2` branch builds **NeoForge 26.1.2.114** with **Java 25 / Gradle 9.2.1**.
Run `./gradlew :neoforge:build`. The release JAR is in `neoforge/build/libs/`.
Fabric sources remain in the repository but are not built by this NeoForge-specific branch.

The reserve remains saved under `StxckExtraItemCount`. A new optional
`StxckExtraItemTemplate` field preserves its components if another mod changes the exposed stack.
Existing configuration keys are retained. `maxSize` now caps the **total pile count**, instead of
only the extra reserve; already saved larger piles are preserved. `mergeDelayTicks=0` keeps immediate
merging; setting it to `10` enables deferred merging without adding a pickup delay. Item lists also
accept `#namespace:tag` entries. The explicit `staaaaaaaaaaaack:blacklist` tag always excludes items.

See [verification instructions](verification/README.md) and [the port report](PORT_REPORT.md) for
runtime evidence and the limits of the compatibility and performance testing.
