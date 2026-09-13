# Signpost — Agent Guide

Multiloader Minecraft mod ("Signpost", mod id `signpost`, group `gollorum.signpost`), built with Gradle
on top of [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template).
Shared code lives in `common`; each mod loader gets a thin subproject that plugs loader-specific
implementations into it.

## Layout

| Path | Purpose |
| --- | --- |
| `common/` | All shared game logic. Compiled against vanilla only (NeoForm + Parchment), **no loader APIs**. |
| `neoforge/` | NeoForge loader entry point (`SignpostNeoforge`), contains the datagen for this plugin. |
| `forge/` | Forge loader entry point (`SignpostForge`) — see [Forge](#forge) below. |
| `fabric/` | Fabric loader entry point (`SignpostFabric`). |
| `buildSrc/` | Convention plugins `multiloader-common` and `multiloader-loader`. |
| `build.gradle` | Root: only declares the `fabric-loom` and `net.neoforged.moddev` plugin versions. |
| `gradle.properties` | Single source of truth for **all** versions (MC, loaders, deps, mod version). |
| `settings.gradle` | Includes `common`, `fabric`, `neoforge`, `forge` + plugin repositories. |

`common` is not shipped as a jar. `multiloader-loader.gradle` exposes `common`'s source and resource
directories through the `commonJava` / `commonResources` configurations, and each loader project
**compiles the common sources directly into its own jar**. Consequence: a change in `common` is
compiled once per loader, so a common-code change can break one loader and not another.

## Build & run

Always use the wrapper (`./gradlew` / `gradlew.bat`), Gradle 9.5.0, Java 25 toolchain.

```bash
./gradlew :neoforge:build
```

**The mod compiles to Java 21 bytecode on a Java 25 toolchain.** The game needs Java 25, but
Forge bundles SpongePowered Mixin 0.8.7, whose highest compatibility level is `JAVA_21` - it
refuses both a `"compatibilityLevel": "JAVA_25"` and any mixin class file newer than v65.
Fabric and NeoForge bundle Fabric's mixin fork, which goes to `JAVA_25`. So
`java_bytecode_version=21` in `gradle.properties` drives `options.release` and is expanded into
the `compatibilityLevel` of all four `*.mixins.json`. Because lowering the bytecode version also
lowers `TARGET_JVM_VERSION` - which makes dependency resolution reject NeoForm's Java 25 variant
- `multiloader-common.gradle` puts that attribute back on the resolve-side configurations. Raise
both once Forge ships a newer mixin.

The daemon and parallel execution are fine now. ForgeGradle 6 could not tolerate them - it
provisioned the patched Minecraft artifact lazily during dependency resolution and died with
`project services has been closed` in a reused daemon - but ForgeGradle 7 provisions differently
and `org.gradle.parallel=true` is set.

- Full build of everything, all four subprojects: `./gradlew build`.
- Run tasks exist for all three loaders and are also generated as IntelliJ run configs.
  All three use `runClient` / `runServer` - ForgeGradle 7 dropped FG6's prefix-less naming, so
  Forge's old `:forge:Client` / `:forge:Server` are now `:forge:runClient` / `:forge:runServer`.
  Each has a
  `…WithMods` variant that uses a separate game directory (`run_with_mods` /
  `runs/*_with_mods`) for testing against other mods - see
  [Testing against the real mods](#testing-against-the-real-mods).
- Output jars land in `<loader>/build/libs/signpost-<loader>-<mc_version>-<version>.jar`.
- There is no test suite and no CI. The routine check is a compile; launching the game is a
  separate, deliberate step - see [Runtime verification](#runtime-verification), which is **not**
  a routine check and is not run without being asked.

### Data generation

`./gradlew :neoforge:runData` — the NeoForge `data` run is the canonical generator. It writes into
`common/src/generated/resources`, which every loader adds to its resource source dirs.

**Do not hand-edit anything under `common/src/generated/resources`** — change the providers in
`neoforge/src/main/java/gollorum/signpost/data/` and re-run datagen.

## Runtime verification

**Do not launch the game as a routine check.** A server or client run is slow, needs a real
Minecraft process, and leaves a JVM behind that outlives the Gradle task. Start one **only** when:

- the user explicitly asks for a runtime test, **or**
- you are immediately about to produce the final build or release artifacts of a change.

Everything else - after an edit, after datagen, before reporting progress - is checked by
compiling (`./gradlew :neoforge:compileJava`, or `:neoforge:runData` when a data provider changed).
A green build proves less here than it does elsewhere, so when a change *would* warrant a runtime
test and you have not been asked for one, **say so and let the user decide**. Never describe a
change as verified at runtime when you only compiled it, and never launch the game to "make sure"
on your own initiative.

**If you start a server or client, you own stopping it.** Kill the game JVM and confirm the port
is free in the *same* turn you started it, before you write your summary - see [Stopping the
server afterwards](#stopping-the-server-afterwards). A leftover server keeps holding
`world/session.lock` and port 25565 and breaks the next run, including the user's own.

Why a run catches things a build cannot: Minecraft's dedicated server does not ship the
client-only classes, and nothing in the build catches a bad reference to them - `common`
compiles against a merged client+server artifact, so
a `net.minecraft.client.*` reference in shared code compiles happily and only explodes as
`NoClassDefFoundError` when the dedicated server touches that class. `PacketHandler.Context.Client`
in `common/src/main/java/gollorum/signpost/networking/PacketHandler.java` reaches straight into
`net.minecraft.client.Minecraft`, so this is a live hazard, not a theoretical one. Mixins are the
same story: `signpost.mixins.json` sets `defaultRequire: 1`, so a mixin whose target moved fails
at load time, not at compile time.

### What to run

This section is the **per-change** check, for when one has been asked for. The **pre-release**
gate is the full 12-run matrix (3 loaders x client/server x with/without other mods), which is
automated as the `full-runtime-test` skill in `.claude/skills/full-runtime-test/` - run it
immediately before producing the final build of a change, and at no other time unless asked.

Run the server for **every loader you touched**, and for all three if you changed `common`:

| Loader | Server | Server + other mods |
| --- | --- | --- |
| fabric | `:fabric:runServer` | `:fabric:runServerWithMods` |
| neoforge | `:neoforge:runServer` | `:neoforge:runServerWithMods` |
| forge | `:forge:runServer` | `:forge:runServerWithMods` |

Run it in the background - the task never returns on its own, and **`echo stop | ./gradlew ...`
does not stop it**, the run tasks do not forward stdin to the forked game:

```bash
./gradlew :neoforge:runServer --console=plain
```

Watch `<loader>/run*/logs/latest.log` rather than the Gradle output, then kill the game JVM as
described in [Stopping the server afterwards](#stopping-the-server-afterwards).

Use the `*WithMods` variant whenever the change touches `compat/`, networking, or anything
Waystones or Repurposed Structures interact with.

### What counts as passing

Load a world written by an **older, released** build - that is what catches serialization and
compatibility breakage. A freshly generated world proves nothing. The authoritative saves live
in `testsaves/<version>/world/` (gitignored, see `testsaves/README.md`); the worlds sitting in
the run directories are dev scratch, and the full-runtime-test harness moves any it finds aside
to `world.preserved-<timestamp>` the first time it plants a corpus save, rather than deleting
them. **Do the same by hand** - `mv` the existing `world` to `world.preserved-<timestamp>` before
copying a corpus save over it. Those scratch worlds are gitignored, so an `rm -rf` is
unrecoverable.

The run passes only if all of these hold:

1. The server reaches `Done (…s)! For help, type "help"`.
2. The existing world loads - no world-load failure, and the server does not fall back to
   generating a fresh one. Note the line `Loading N persistent chunks`: since 1.21.11 a
   dedicated server loads **only** force-loaded chunks at startup, not a radius around
   spawn, so if that says `0` no block-entity NBT was deserialized and only the mod's
   `SavedData` was actually exercised. Every corpus entry force-loads, so this should be a
   three-digit number, not `0` - `2.04.0/26.1.2` logs `Loading 91 persistent chunks`.
   **Trust that line over the auditor.** `audit-test-save.py` reported `forced chunks: 0`
   for every entry until 2026-09-13, because 26.1 renamed *and* moved the chunk-ticket file
   and changed its encoding; it now reads both eras and its count is cross-checked against
   this log line. A corpus save should `/forceload` the area it wants covered - see
   `testsaves/README.md`.
3. The log contains no exception, and specifically none of: `NoClassDefFoundError`,
   `ClassNotFoundException` (client classes on the server), `Mixin apply failed` /
   `InvalidInjectionException`, `Failed to load` / codec or NBT deserialization errors,
   `Registry` or datapack load errors, and no `signpost` entry at `ERROR`/`FATAL`.
4. Nothing in the mod list is missing or refused to load (the log prints the resolved mod set).

There is no graceful shutdown to assert on - see [Stopping the server
afterwards](#stopping-the-server-afterwards) - so startup and world load are the whole bar.

Known-benign log noise, do not chase these:

- Fabric logs `No data fixer registered for post` / `waystone` / `waystone_generator` at ERROR
  during registry bootstrap, before the world is touched. Vanilla logs this for every modded block
  entity type that ships no DataFixer; NeoForge suppresses it, Fabric does not.
- `Reference map 'signpost.refmap.json' ... could not be read` on NeoForge and Forge. Those loaders
  run on official mappings and need no refmap; only the Fabric jar ships one.
- On NeoForge, `The following mods have version differences that were not resolved` when the save
  was written by an older build. That line is useful - it tells you which version wrote the save.
- On Windows, `NoClassDefFoundError: Could not initialize class io.netty.channel.kqueue.Native`
  (and `.epoll.Native`). Netty probes for the macOS and Linux native transports and falls back to
  NIO; harmless everywhere except as log noise.
- Forge logs a long `Object did not get ID it asked for` block from `ForgeRegistry` at boot.

Serialization and save-compatibility errors are the priority: any change to block entity data,
data components, packet codecs, `utils/serialization/`, or the waystone storage in
`minecraft/storage/WaystoneLibraryStorage.java` **must** be checked by loading a pre-existing save,
not just a freshly created one. A new world hides exactly the migration bugs that matter.

#### Post types are data, and the pre-2.04 ids are migrated by a DataFixer

Up to 2.03.x every post type was its own block and item (`signpost:post_oak`, `post_spruce`, ...).
They are now four material blocks - `post_wood`, `post_stone`, `post_metal`, `post_mushroom` - with the
type held in the block entity and in the `signpost:post_data` component, and the types themselves
living in the `signpost:post_model_types` datapack registry. `post_stone` is the one id that carries
over unchanged; the other fifteen are gone from the registry entirely.

They are translated away by `migration/SignpostDataFixes`, which
`mixin/DataFixersInjector` appends to Minecraft's own fixer chain. **Read this before touching it:**

- **A DataFixer only runs when the save's *Minecraft* data version is behind the current one.** That
  means this only works when Signpost never shipped for the targeted minecraft version, so every world
  that can contain the old format was written by an older Minecraft and goes through the chain. The same trick is **not** available
  for a format change made within a Minecraft version that Signpost has already shipped for - there
  the fixer is simply never invoked, whatever you register. Plan such a change around a Minecraft
  upgrade, or migrate at load time in the block entity instead.
- The schema is registered at `SharedConstants.getCurrentVersion().dataVersion()`, not at a pinned
  4671. `DataFixerBuilder.addSchema` parents each schema onto the last one added, so a pinned older
  version would get the wrong parent once Minecraft adds schemas past it. Following the current
  version means the fixes re-run on every later Minecraft upgrade; both are idempotent, so that is
  harmless, and it keeps them working for saves that skip a version.
- A block id missing from the registry does not merely disappear from a loaded chunk - it shifts the
  section palette and scrambles the blocks around it. The rename map is the only thing standing
  between an old save and that, so do not drop entries from `migration/LegacyPostTypes`.

Four shapes of old data meet in the current code and all four have to keep working:

- `PostData` with no `ModelType` field (everything before 2.04). `PostTile.deriveModelType()` works it
  out from the parts - a sign part names its model type outright, and failing that the post part's
  texture is matched against the registry. Nothing visible rides on it: every part stores its own
  textures, so the derived type only decides what later edits default to.
- A sign part's `CoreData.ModelType` as a bare `"spruce"` rather than an id. `ModelTypeRegistry
  .KEY_CODEC` completes a namespace-less name with `signpost:`, not with `minecraft:`.
- `PostData` data version 1, which the village structure templates are still written in.
- Item stacks whose id was the type. `migration/PostItemStackFix` writes the type into the components
  before renaming, so a stockpiled spruce post stays a spruce post.

#### Codec field names are on-disk data

`fieldOf("...")` strings are persisted NBT keys. **Never let them follow a class rename.** The
1.21.11 port renamed `ResourceLocation` to `Identifier` and swept the string literals along with
it in four codecs (`VillageWaystone.ChunkEntryKey`, `ResourceLocationSerializer`, `Texture`,
`FluidTint`), which made every pre-existing world fail with
`Failed to parse saved data for 'SavedDataType[signpost_WaystoneLibrary]': No key Identifier`
and silently drop the entire waystone library. The key stays `"ResourceLocation"`; only the Java
type changed. Note the failure did **not** crash the server - it logged one ERROR line and carried
on, which is precisely why the log must be read rather than just watching for a clean startup.

#### The saved-data file path is on-disk data too

26.1 changed `SavedDataType`'s id from a plain `String` to an `Identifier`, and
`SavedDataStorage` resolves it as `data/<namespace>/<path>.dat` - the namespace directory is not
optional, so **no `Identifier` can name the old flat `data/signpost_WaystoneLibrary.dat`** (and
the old name is not even a legal path, having capitals in it). The library therefore had to move
on disk. `WaystoneLibraryStorage.migrateLegacyFile`, called from `WaystoneLibrary.initializeServer`
before the storage is queried, copies the old file to the new location once; it copies rather than
moves, so an older version of the mod can still read the world. Without it every pre-26.1 world
would come up with no waystones at all - and, exactly as with the codec keys above, it would do so
*quietly*, since a missing saved-data file is not an error.

#### Before "fixing" a compatibility break, establish what actually shipped

Not every save on disk comes from a released build, and matching a never-released format is worse
than useless. Check provenance first:

- The last release is a tag; compare against it directly, e.g.
  `git show v2.03.0:common/src/main/java/.../Texture.java`.
- Cross-check what is actually public (the mod is on Modrinth and CurseForge) - a version bump in
  `gradle.properties` is not evidence of a release.
- The server log prints the writing version on load (`signpost (version 2.03.0 -> 2.03.1)`), which
  tells you which build produced the save.

### Stopping the server afterwards

**A run is not finished until the game JVM is gone and port 25565 is free.** Do this before
writing your summary, in the same turn - not "later", and not only when the run succeeded. This
applies just as much to a crashed or abandoned run.

The run task never returns on its own, and stopping it is fiddlier than it looks:

- **Piping `stop` in does not work.** `echo stop | ./gradlew :neoforge:runServer` is ignored - the
  run tasks do not forward stdin to the forked game process.
- **Killing the Gradle task is not enough.** The server runs in a *separate* JVM that survives it,
  keeps holding `world/session.lock`, and makes the next run die with
  `Failed to start the minecraft server: java.io.IOException: ... another process has locked part
  of the file` plus `FatalStartupException: Couldn't find Minecraft server thread`. If you see
  that, a previous server is still alive - it is not a regression in your change.

So: run the server in the background, watch the log until `Done (…s)!` or a crash, then terminate
the **game** JVM directly. Identify it by its command line rather than killing every `java.exe` -
Gradle's daemon and wrapper are also `java.exe`. Each loader launches differently, so match all
three markers: NeoForge has `fml.modFolders`, Fabric has `net.fabricmc.loader`, and Forge has
neither - it boots via `net.minecraftforge.bootstrap.ForgeBootstrap`:

```bash
powershell -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -match 'fml\.modFolders|fabricmc\.loader|ForgeBootstrap' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }"
```

Confirm with `(Get-NetTCPConnection -LocalPort 25565 -ErrorAction SilentlyContinue).Count` - a
leftover server still holds that port and will block the next loader's run.

Force-killing an idle dev server is safe: nothing has changed in the world since load, and a
`SavedData` that failed to parse is *not* written back over the good file on disk (verified - the
original `signpost_WaystoneLibrary.dat` survived a failed load untouched). Always confirm the
process is gone before starting the next loader's server, and never leave one running.

Testing other runtime behaviour (actually placing signs, teleporting, GUI interaction) is **not**
expected of an agent. A clean startup and a clean load of the pre-existing world is the bar. If you
could not run a server, say so explicitly rather than implying the change was verified.

## Cross-loader architecture

Two mechanisms bridge `common` → loader:

1. **`ServiceLoader`** (`gollorum.signpost.platform.Services`). Common declares interfaces
   (`IPlatformHelper`, `IFluidTextureProvider`, `IBlockEntityTypeFactory`,
   `ILootItemConditionRegistry`, `IWaystoneDiscoveryEventListener`, `BiomeAccessor`); each loader
   provides an implementation **and** a matching file in
   `<loader>/src/main/resources/META-INF/services/<fully.qualified.Interface>`.
   Adding a service means touching *every* loader — a missing provider file throws at class-init of
   `Services`, not at compile time.
2. **Abstract base classes** the loader subclasses, most notably
   `gollorum.signpost.networking.PacketHandler` → `NeoForgePacketHandler` / `FabricPacketHandler` /
   `ForgePacketHandler`. Packets are registered once in `PacketHandler.init()`; the loader class only
   implements transport (`sendToServer`, `sendToPlayer`, `sendToTracing`, …).

Per-loader duplicated packages, all mirroring the same shape: `registry/` (blocks, items, tile
entities, recipes, data components, creative tab, commands, loot conditions), `config/`, `compat/`,
`networking/`, `platform/`, `services/`, `client/`, `block/`.

Config: common defines `IConfig` / `IPermissionConfig` / `ITeleportConfig` / `IWorldGenConfig`;
NeoForge implements them with `ModConfigSpec`, Fabric with Cloth Config (+ Mod Menu integration).

Mod compat (`compat/` in each loader) is optional and reflection-free — the dependencies are
`compileOnly`, and `Compat`/`WaystonesAdapter`/`RepurposedStructuresAdapter` are only wired up when
`Services.PLATFORM.isModLoaded(...)` reports the mod present. Supported: **Waystones** and
**Repurposed Structures**.

## Mixins & access transformers

- Common mixins live in `gollorum.signpost.mixin` and are listed in
  `common/src/main/resources/signpost.mixins.json`. **New mixin classes must be added to that JSON**
  (`mixins` for common-side, `client` for client-only) or they silently do nothing.
- Each loader additionally ships an (currently empty) `signpost.<loader>.mixins.json` for
  loader-specific mixins; these are referenced from `fabric.mod.json` / `neoforge.mods.toml` /
  the Forge jar manifest. All four configs declare `compatibilityLevel: JAVA_21`.
- Fabric sets `loom.mixin.useLegacyMixinAp = true`. Loom 1.13 turned the mixin annotation
  processor off by default, but the mod ships and relies on `signpost.refmap.json`; leaving the
  AP on keeps the refmap being generated instead of silently switching to remap-time mixin
  rewriting. Migrating off it is a real change that needs in-game testing.
- Access transformers: `neoforge/src/main/resources/META-INF/accesstransformer.cfg` and
  `forge/src/main/resources/META-INF/accesstransformer.cfg`. There is currently **no**
  `common` AT and no `signpost.accesswidener`; the build scripts probe for those files and simply
  skip them when absent, so creating one is enough to activate it (a Fabric access widener must be
  named `common/src/main/resources/signpost.accesswidener`).
- The Forge AT uses **SRG names** (`m_125977_`), NeoForge's uses official names — they are not
  interchangeable, which is why the file is not shared.

## Versions

Everything version-shaped belongs in `gradle.properties`, never inline in a build script. Values are
expanded into `fabric.mod.json`, `neoforge.mods.toml`, `pack.mcmeta` and the `*.mixins.json` files by
the `processResources` block in `buildSrc/src/main/groovy/multiloader-common.gradle`.

> **Any new property added to `gradle.properties` must also be added to the `expandProps` map in
> `multiloader-common.gradle`**, otherwise resource expansion fails.

Current targets: Minecraft 26.2, Java 25 (21 bytecode), NeoForge 26.2.0.87, Fabric Loader
0.19.5 / Fabric API 0.160.0, Forge 65.1.3. **No Parchment** - it has published nothing for the
26.x line, and 26.x ships deobfuscated vanilla carrying Mojang's own parameter names, so the
layer has no purpose; the `parchment { }` blocks are gone from `common` and `neoforge`.

Minecraft's versioning changed with 26.1: after 1.21.11 come `26.1`, `26.1.1`, `26.1.2`, `26.2`.
Do not assume a `1.x.y` shape, and do not treat `26.1` as the whole line - `26.1.2` is a distinct
Minecraft version with its own NeoForm, NeoForge and Forge builds. `26.2` in turn has no patch
releases: NeoForm, NeoForge and Forge all publish against the bare `26.2`.

Integration dependency versions live there too (`waystones_fabric_version`,
`waystones_neoforge_version`, `modmenu_version`, `cloth_config_version`,
`repurposed_structures_fabric_version`, `repurposed_structures_neoforge_version`) so the loaders
cannot drift apart. The build scripts
reference them, never a literal version.

`group` is set from `group_id` in `multiloader-common.gradle`. Without that assignment Gradle
falls back to the root project name, which is `1.21` — that is what published coordinates and
capabilities used to say.

Do not bump Minecraft, loader, or dependency versions on your own — ask first.

Moving to a **new Minecraft version** is a whole procedure, not a version bump: it drags in
the loaders, Parchment/NeoForm, the Gradle plugins, possibly the wrapper and JDK, every
integration, a datagen re-run and a fresh corpus. Use the `upgrade-minecraft-version` skill
in `.claude/skills/upgrade-minecraft-version/`; its `probe-versions.py` reports what exists
for a target version and which blockers stand in the way, without changing anything.

## Toolchain constraints

These pins are load-bearing and interlock; changing one breaks another:

- **Gradle 9.5.0 / Java 25.** Minecraft 26.x requires Java 25, and Loom 1.17 requires Gradle
  9.5. Both are floors, not preferences.
- **ForgeGradle 7, not 6.** FG6 refuses to apply on Gradle 9 outright ("Found Gradle version
  Gradle 9.5.0. Versions Gradle 9.0 and newer are not supported yet"), and it is published
  under a *different* artifact (`net.minecraftforge:ForgeGradle`) than FG7
  (`net.minecraftforge:forgegradle`) even though the plugin id `net.minecraftforge.gradle` is
  the same. Looking only at the old coordinate makes FG7 invisible and Gradle 9 look like a
  dead end for Forge - it is not.
- **Loom's plugin id changed to `net.fabricmc.fabric-loom`** with Loom 1.15. 26.x ships
  deobfuscated, so this - the non-remapping variant - is the right one;
  `net.fabricmc.fabric-loom-remap` is for 1.21.11 and older. Because nothing is remapped, mod
  dependencies use the plain `implementation`/`api`/`compileOnly` configurations rather than
  `modImplementation` and friends, there is no `mappings` block, and no refmaps are generated -
  which is why the old Repurposed-Structures-on-Fabric pin (Loom refusing a mod built by a newer
  Loom) is gone and both RS builds now track the current release.
- **Java 21 bytecode** - see [Build & run](#build--run). Forge's mixin cannot read v69 class
  files.
- **Forge dev runs need `--mixin.config` on the command line.** FG7 has no equivalent of FG6's
  `org.spongepowered.mixin` Gradle plugin, and in a dev run the mod loads from `build/classes`,
  so the jar's `MixinConfigs` manifest attribute is never read. Without those args every mixin
  silently fails to apply and the first accessor called throws an `AssertionError` from its own
  body - which looks nothing like a mixin problem.

## Optional mod integrations

All are `compileOnly` / `modCompileOnly` and guarded at runtime by
`Services.PLATFORM.isModLoaded(...)`; none is bundled into the jars.

| Integration | fabric | neoforge | forge |
| --- | --- | --- | --- |
| Waystones | yes | yes | **no 26.x build exists** |
| Repurposed Structures | yes | yes | **no build exists** |

**The Forge subproject has no optional integrations at all on 26.x.** Waystones dropped Forge
after 21.11.9, so `forge/.../compat/WaystonesAdapter.java` is commented out alongside
`RepurposedStructuresAdapter`, together with the matching `Compat.register()` and
`Compat.getEvents()` calls, and `forge/build.gradle` declares no integration repository or
dependency at all. Because Waystones is no longer published for every loader, its version is two
properties (`waystones_fabric_version`, `waystones_neoforge_version`) rather than one shared
one. Repurposed Structures is versioned separately per loader and its last
Forge build was for 1.20.1 — so `forge/.../compat/RepurposedStructuresAdapter.java` and the
matching `Compat.register()` call are commented out there, and `forge/build.gradle` declares
no RS repository or dependency. That asymmetry is intentional; do not "fix" it.

RS-Fabric declares MidnightLib and a second (`maven.modrinth`) Mod Menu as transitive mod
dependencies, so the Fabric dependency is declared with `transitive = false` to keep a
duplicate Mod Menu off the classpath.

### Testing against the real mods

The `*WithMods` runs load real mod jars from a `mods/` folder inside their game directory.
These directories are gitignored, so they are per-developer and start out empty - in a fresh
clone, a new worktree, and after every Minecraft upgrade.

**An empty `mods/` folder does not fail a run.** It just turns that run into a second copy of
the plain one, so half the matrix reports PASS while proving nothing. Populate them before
running the test:

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/skills/full-runtime-test/fetch-mod-jars.ps1
```

That downloads what each folder needs into a **cache beside the repo**,
`../mod-jar-cache/<minecraft_version>/`, verifies every jar against the SHA-1 the Modrinth API
publishes, installs them, and prints each Fabric jar's `depends`/`breaks` so the chain can be
checked rather than assumed. The cache lives outside the working tree deliberately: it survives
`git clean`, branch switches and worktree deletion, and one download serves every checkout on
the machine. Add `-List` to see candidates without downloading, `-Pin <project>=<version>` to
hold one back, `-NoInstall` to fill the cache only.

Which project goes in which folder is `.claude/skills/full-runtime-test/mod-jars.json`; the
table below is its human-readable twin, so change both together.

| Run | Game directory | Installed |
| --- | --- | --- |
| fabric `clientWithMods` | `fabric/runs/client_with_mods/mods` | Balm, Shogi, Waystones, Repurposed Structures, MidnightLib, Sodium, Iris |
| fabric `serverWithMods` | `fabric/runs/server_with_mods/mods` | Balm, Shogi, Waystones, Repurposed Structures, MidnightLib |
| neoforge `clientWithMods` / `serverWithMods` | `neoforge/run_with_mods/mods` | Balm, Shogi, Waystones, Repurposed Structures |
| forge `clientWithMods` | `forge/runs/client_with_mods/mods` | Balm — **no 26.2 build yet, folder is empty** |
| forge `serverWithMods` | `forge/runs/server_with_mods/mods` | Balm — **no 26.2 build yet, folder is empty** |

Required chains when refreshing these, all downloadable from Modrinth: **Waystones needs both
Balm and Shogi** (26.2.0.12 wants `balm >=26.2.0.7` and `shogi >=26.2.0.5`; Shogi is a second
BlayTheNinth library that Waystones started requiring on 26.x, and it is easy to miss because
nothing else references it), **RS on Fabric needs MidnightLib** (`>=1.5.7`), and **Iris needs
Sodium** (`0.9.x`). Read the `depends` block of each jar you install rather than assuming the
chain is the same as last time - these move.

**Do not just take the newest of everything** - check each jar's `breaks` block, not only its
`depends`. The Sodium/Iris pair has now needed a pin on three Minecraft versions running. On
26.2, Sodium `0.9.2` declares `breaks iris <=1.11.2` and `1.11.2` is the newest Iris, so plain
"newest of each" installs a pair Fabric refuses. `fetch-mod-jars.ps1` prints every `breaks` line
but does not resolve the conflict; this version needs
`-Pin sodium=mc26.2-0.9.1-fabric`. Sodium `0.9.1` breaks only `iris <=1.11.1`, and Iris `1.11.2`
requires `sodium 0.9.x`, so **Iris 1.11.2 + Sodium 0.9.1** is the working pair on 26.2. Which
version has to be held back changes every time - check again on every upgrade. Fabric refuses to
launch on a bad pair, with a modal error dialog that blocks the run until someone clicks Exit.
Projects do not agree on how precisely they tag a Minecraft version. `fetch-mod-jars.ps1` falls
back to the `<major>.<minor>` prefix when the exact version returns nothing and says so in its
output - a looser match is worth a glance, not a silent drop. On 26.2 every project tags the bare
`26.2`, so nothing currently needs the fallback.

Sodium and Iris are client-only. Forge has neither Repurposed Structures nor (since 26.x)
Waystones, and **as of 26.2 Balm has no Forge build either, so both Forge `*WithMods` folders are
empty** and those two runs currently test the same thing as the plain `client`/`server` runs. Put
Balm back the moment it ships for 26.2. Keep these jars on the same Minecraft version as `minecraft_version`;
the plain `client`/`server` runs deliberately have empty `mods/` folders.

## Forge

The upstream MultiLoader-Template dropped Forge, but this repo keeps the subproject alive and it
still has to be maintained. It is wired up by hand with ForgeGradle
(`net.minecraftforge.gradle`), instead of ModDevGradle/Loom.

It builds on 26.2 (Forge 65.1.3) with **ForgeGradle 7**. FG6 cannot be used at all here: it
refuses to apply on Gradle 9, and FG7 is published under a different artifact coordinate
(`net.minecraftforge:forgegradle`, not `net.minecraftforge:ForgeGradle`) behind the same plugin
id - so a version probe that only knows the old coordinate reports "6.0.54 is the newest" and
makes Forge look unsalvageable. FG7 also retired both workarounds this subproject used to need:
the daemon/parallel ban and `systemProp.net.minecraftforge.gradle.repo.sources.force`.

Its DSL differs from FG6: no `mappings channel:`, no `reobf`, `accessTransformer = true` instead
of a path, `runs { register('name') { ... } }` with `workingDir` and `mainClass`, repositories
declared via `minecraft.mavenizer(it)` / `fg.forgeMaven` / `fg.minecraftLibsMaven`, and
`implementation minecraft.dependency(...)` instead of a `minecraft` configuration. There is no
publishing helper (`fg.component`) any more.

Forge-specific quirks:

- **It has no access transformer.** The old one used SRG names (`m_125977_`), which do not exist
  in deobfuscated 26.x, and every member it unlocked was for datagen this subproject does not do.
  It was deleted rather than translated.
- **Dev runs need `--mixin.config` passed explicitly**, since FG7 has no mixin Gradle plugin - see
  [Toolchain constraints](#toolchain-constraints). It also means no refmap is produced, which is
  correct: 26.x is deobfuscated and the `refmap` key is gone from all four mixin configs.
- **Its mixin is the vanilla SpongePowered one (0.8.7), not Fabric's fork**, which is why the
  whole mod targets Java 21 bytecode - see [Build & run](#build--run).
- **Custom-named run configurations need `main` and `args` set by hand.** ForgeGradle merges the
  userdev run template (main class, `--launchTarget`, asset paths) only into runs literally named
  `client`, `server` or `data`. `clientWithMods` / `serverWithMods` match no template, so without
  explicit values they die with `No main class specified and classpath is not an executable jar`.
  The values in `forge/build.gradle` mirror the templates in forge's userdev `config.json`; if a
  future Forge version changes them, copy the new ones from there.
- The jar declares its mixin configs through the `MixinConfigs` manifest attribute rather than a
  `[[mixins]]` block. That attribute is read from the built jar only; a dev run needs the
  command-line args above.
- `forge` registers **no** data providers (no `GatherDataEvent` listener), so its `data` run
  generates nothing and `data/StructurePoolElementProvider.java` is unused. Datagen is NeoForge's
  job. `registry/VillageRegistry.java`, `compat/RepurposedStructuresAdapter.java` and (since
  26.x) `compat/WaystonesAdapter.java` are fully commented out on purpose.

When changing `common`, keep the Forge sources consistent (new service files, registry entries,
packet handler changes) and verify with `./gradlew :forge:build`. Forge is the loader most likely
to need a runtime run on top of that - its task is `:forge:runServer` (see
[Runtime verification](#runtime-verification) for when to run one at all).

## Conventions

- Java 25 toolchain compiling to Java 21 bytecode, 4-space indent, mod code under
  `gollorum.signpost.*`.
- `Signpost.LOGGER` (SLF4J) for logging; `Signpost.MOD_ID` for the namespace — build
  `Identifier`s via `Identifier.fromNamespaceAndPath(Signpost.MOD_ID, …)`.
- `.gitattributes` enforces LF for `*.java`, but **CRLF for `*.gradle`** — keep that in mind when
  rewriting build scripts.
- Don't commit or push; leave that to the maintainer.
