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

Always use the wrapper (`./gradlew` / `gradlew.bat`), Gradle 8.14.3, Java 21 toolchain.

```bash
./gradlew :neoforge:build
```

**Do not enable the Gradle daemon or parallel execution.** `gradle.properties` pins
`org.gradle.daemon=false` and `org.gradle.parallel=false` on purpose: ForgeGradle provisions
the patched Minecraft artifact lazily during dependency resolution, and in a reused daemon
(or under parallel execution) the project services it needs are already closed. The symptom
is `IllegalStateException: project services has been closed`, followed by
`Could not find net.minecraftforge:forge:...mapped_official...`. `neoforge` and `fabric`
build fine either way; only `forge` depends on this.

- Full build of everything, all four subprojects: `./gradlew build`.
- Run tasks exist for all three loaders and are also generated as IntelliJ run configs.
  Fabric and NeoForge use `runClient` / `runServer`; **ForgeGradle names its tasks without the
  `run` prefix**, so Forge's are `:forge:Client` and `:forge:Server`. Each has a
  `…WithMods` variant that uses a separate game directory (`run_with_mods` /
  `runs/*_with_mods`) for testing against other mods - see
  [Testing against the real mods](#testing-against-the-real-mods).
- Output jars land in `<loader>/build/libs/signpost-<loader>-<mc_version>-<version>.jar`.
- There is no test suite and no CI. Verification is done by compiling and launching the game -
  see [Runtime verification](#runtime-verification-required), which is mandatory, not optional.

### Data generation

`./gradlew :neoforge:runData` — the NeoForge `data` run is the canonical generator. It writes into
`common/src/generated/resources`, which every loader adds to its resource source dirs.

**Do not hand-edit anything under `common/src/generated/resources`** — change the providers in
`neoforge/src/main/java/gollorum/signpost/data/` and re-run datagen.

## Runtime verification (required)

**A green build proves almost nothing here. Always start the dedicated server and confirm it
loads a world before reporting a change as done.**

Minecraft's dedicated server does not ship the client-only classes, and nothing in the build
catches a bad reference to them - `common` compiles against a merged client+server artifact, so
a `net.minecraft.client.*` reference in shared code compiles happily and only explodes as
`NoClassDefFoundError` when the dedicated server touches that class. `PacketHandler.Context.Client`
in `common/src/main/java/gollorum/signpost/networking/PacketHandler.java` reaches straight into
`net.minecraft.client.Minecraft`, so this is a live hazard, not a theoretical one. Mixins are the
same story: `signpost.mixins.json` sets `defaultRequire: 1`, so a mixin whose target moved fails
at load time, not at compile time.

### What to run

This section is the **per-change** check. The **pre-release** gate is the full 12-run matrix
(3 loaders x client/server x with/without other mods), which is automated as the
`full-runtime-test` skill in `.claude/skills/full-runtime-test/` - run it immediately before
producing the final build of a change, and at no other time unless asked.

Run the server for **every loader you touched**, and for all three if you changed `common`.
Note that the Forge task names differ - ForgeGradle uses the bare run name:

| Loader | Server | Server + other mods |
| --- | --- | --- |
| fabric | `:fabric:runServer` | `:fabric:runServerWithMods` |
| neoforge | `:neoforge:runServer` | `:neoforge:runServerWithMods` |
| forge | `:forge:Server` | `:forge:ServerWithMods` |

```bash
./gradlew :neoforge:runServer --console=plain
```

Use the `*WithMods` variant whenever the change touches `compat/`, networking, or anything
Waystones or Repurposed Structures interact with.

### What counts as passing

Load a world written by an **older, released** build - that is what catches serialization and
compatibility breakage. A freshly generated world proves nothing. The authoritative saves live
in `testsaves/<version>/world/` (gitignored, see `testsaves/README.md`); the worlds sitting in
the run directories are dev scratch, and the full-runtime-test harness moves any it finds aside
to `world.preserved-<timestamp>` the first time it plants a corpus save, rather than deleting
them.

The run passes only if all of these hold:

1. The server reaches `Done (…s)! For help, type "help"`.
2. The existing world loads - no world-load failure, and the server does not fall back to
   generating a fresh one. Note the line `Loading N persistent chunks`: since 1.21.11 a
   dedicated server loads **only** force-loaded chunks at startup, not a radius around
   spawn, so if that says `0` no block-entity NBT was deserialized and only the mod's
   `SavedData` was actually exercised. A corpus save should `/forceload` the area it wants
   covered - see `testsaves/README.md`.
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

#### Codec field names are on-disk data

`fieldOf("...")` strings are persisted NBT keys. **Never let them follow a class rename.** The
1.21.11 port renamed `ResourceLocation` to `Identifier` and swept the string literals along with
it in four codecs (`VillageWaystone.ChunkEntryKey`, `ResourceLocationSerializer`, `Texture`,
`FluidTint`), which made every pre-existing world fail with
`Failed to parse saved data for 'SavedDataType[signpost_WaystoneLibrary]': No key Identifier`
and silently drop the entire waystone library. The key stays `"ResourceLocation"`; only the Java
type changed. Note the failure did **not** crash the server - it logged one ERROR line and carried
on, which is precisely why the log must be read rather than just watching for a clean startup.

#### Before "fixing" a compatibility break, establish what actually shipped

Not every save on disk comes from a released build, and matching a never-released format is worse
than useless. Check provenance first:

- The last release is a tag; compare against it directly, e.g.
  `git show v2.03.0:common/src/main/java/.../Texture.java`.
- Cross-check what is actually public (the mod is on Modrinth and CurseForge) - a version bump in
  `gradle.properties` is not evidence of a release.
- The server log prints the writing version on load (`signpost (version 2.03.0 -> 2.03.1)`), which
  tells you which build produced the save.
- **`1.21-dynamic-post-types` is an experimental branch with deliberately broken compatibility and
  is not merged into `1.21`.** Never treat its format as something to support.

### Stopping the server afterwards

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

Current targets: Minecraft 1.21.11, Java 21, NeoForge 21.11.45, Fabric Loader 0.18.6 /
Fabric API 0.141.6, Forge 61.2.1, Parchment 1.21.10.

Integration dependency versions live there too (`waystones_version`, `modmenu_version`,
`cloth_config_version`, `repurposed_structures_fabric_version`,
`repurposed_structures_neoforge_version`) so the loaders cannot drift apart. The build scripts
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

- **Gradle stays on 8.14.3** because ForgeGradle 6 needs it. The build already reports
  "Deprecated Gradle features were used ... incompatible with Gradle 9.0".
- **Loom is pinned to `1.13.6`** (a release, not the upstream template's `1.13-SNAPSHOT`).
  Loom 1.14 and newer require Gradle 9, so 1.13.6 is the newest usable line here.
- **Repurposed Structures on Fabric is pinned to `7.5.22+1.21.9-fabric`**, behind the current
  release, because every RS Fabric build for 1.21.11 is published with Loom 1.14 and Loom
  refuses to consume a mod built by a newer Loom (`Mod was built with a newer version of
  Loom (1.14.10), you are using Loom (1.13.6)`). It is compile-only and only
  `RSConditionsRegistry` is used, so the adapter still works against current RS at runtime.
- Consequence: **dropping the Forge subproject is what unblocks Gradle 9, Loom 1.14+, and a
  current RS-Fabric pin.** They cannot be done separately.

## Optional mod integrations

All are `compileOnly` / `modCompileOnly` and guarded at runtime by
`Services.PLATFORM.isModLoaded(...)`; none is bundled into the jars.

| Integration | fabric | neoforge | forge |
| --- | --- | --- | --- |
| Waystones | yes | yes | yes |
| Repurposed Structures | yes | yes | **no build exists** |

Waystones publishes all three loaders from the TwelveIterations maven, so its version is a
single shared property. Repurposed Structures is versioned separately per loader and its last
Forge build was for 1.20.1 — so `forge/.../compat/RepurposedStructuresAdapter.java` and the
matching `Compat.register()` call are commented out there, and `forge/build.gradle` declares
no RS repository or dependency. That asymmetry is intentional; do not "fix" it.

RS-Fabric declares MidnightLib and a second (`maven.modrinth`) Mod Menu as transitive mod
dependencies, so the Fabric dependency is declared with `transitive = false` to keep a
duplicate Mod Menu off the classpath.

### Testing against the real mods

The `*WithMods` runs load real mod jars from a `mods/` folder inside their game directory.
These directories are gitignored, so they are per-developer and must be populated by hand:

| Run | Game directory | Installed |
| --- | --- | --- |
| fabric `clientWithMods` | `fabric/runs/client_with_mods/mods` | Balm, Waystones, Repurposed Structures, MidnightLib, Sodium, Iris |
| fabric `serverWithMods` | `fabric/runs/server_with_mods/mods` | Balm, Waystones, Repurposed Structures, MidnightLib |
| neoforge `clientWithMods` / `serverWithMods` | `neoforge/run_with_mods/mods` | Balm, Waystones, Repurposed Structures |
| forge `clientWithMods` | `forge/runs/client_with_mods/mods` | Balm, Waystones |
| forge `serverWithMods` | `forge/runs/server_with_mods/mods` | Balm, Waystones |

Required chains when refreshing these, all downloadable from Modrinth: **Waystones needs Balm**
(`>=21.11.3`), **RS on Fabric needs MidnightLib** (`>=1.5.7`), and **Iris needs Sodium** (`0.8.x`).

**Do not just take the newest of everything** - check each jar's `breaks` block, not only its
`depends`. Sodium declares which Iris versions it breaks, and the newest pair is *mutually
incompatible*: Sodium `0.8.13`/`0.8.14` break `iris <=1.10.7`, while `1.10.7` is the newest Iris
for 1.21.11. Sodium `0.8.12` breaks only `iris <=1.10.6`, so **Iris 1.10.7 + Sodium 0.8.12** is
the working pair. Fabric refuses to launch otherwise, with a modal error dialog that blocks the
run until someone clicks Exit.
Sodium and Iris are client-only. Forge has no Repurposed Structures build, so its folders only
carry Balm and Waystones. Keep these jars on the same Minecraft version as `minecraft_version`;
the plain `client`/`server` runs deliberately have empty `mods/` folders.

## Forge

The upstream MultiLoader-Template dropped Forge, but this repo keeps the subproject alive and it
still has to be maintained. It is wired up by hand: ForgeGradle (`net.minecraftforge.gradle`) plus
the SpongePowered `mixin` Gradle plugin and explicit refmap/`MixinConfigs` manifest handling, instead
of ModDevGradle/Loom.

It builds on 1.21.11 (Forge 61.2.1), but only because of three settings in `gradle.properties`
that exist solely for it. If `forge` breaks, suspect the build environment before the Java sources:

1. `org.gradle.daemon=false` and `org.gradle.parallel=false` - see [Build & run](#build--run).
2. `systemProp.net.minecraftforge.gradle.repo.sources.force=true`. ForgeGradle 6.0.54 cannot
   assemble the patched Minecraft jar for Forge 1.21.11 from binaries: `MinecraftUserRepo.findRaw`
   copies the binpatched jar and then adds the recompiled MCP inject classes over it, and for this
   version both contain `mcp/client/Start.class`, so it dies with
   `java.util.zip.ZipException: duplicate entry: mcp/client/Start.class`. Forcing FG down its
   sources path decompiles and recompiles Forge instead, avoiding that merge. The first build is
   slow (a full Forge decompile); afterwards it is cached in `forge/build/fg_cache`.

Forge-specific quirks:

- Its access transformer uses **SRG names** (`m_125977_`) while NeoForge's uses official names, so
  the file cannot be shared.
- **Custom-named run configurations need `main` and `args` set by hand.** ForgeGradle merges the
  userdev run template (main class, `--launchTarget`, asset paths) only into runs literally named
  `client`, `server` or `data`. `clientWithMods` / `serverWithMods` match no template, so without
  explicit values they die with `No main class specified and classpath is not an executable jar`.
  The values in `forge/build.gradle` mirror the templates in forge's userdev `config.json`; if a
  future Forge version changes them, copy the new ones from there.
- The jar declares its mixin configs through the `MixinConfigs` manifest attribute rather than a
  `[[mixins]]` block. It ships **no** `signpost.refmap.json` even though `signpost.mixins.json`
  names one - that is long-standing and fine, since Forge runs on official mappings.
- `forge` registers **no** data providers (no `GatherDataEvent` listener), so its `data` run
  generates nothing and `data/StructurePoolElementProvider.java` is unused. Datagen is NeoForge's
  job. `registry/VillageRegistry.java` and `compat/RepurposedStructuresAdapter.java` are
  fully commented out on purpose.

When changing `common`, keep the Forge sources consistent (new service files, registry entries,
packet handler changes) and verify with `./gradlew :forge:build` followed by `./gradlew :forge:Server`
(see [Runtime verification](#runtime-verification-required) - note Forge's run task is `Server`,
not `runServer`).

## Conventions

- Java 21, 4-space indent, mod code under `gollorum.signpost.*`.
- `Signpost.LOGGER` (SLF4J) for logging; `Signpost.MOD_ID` for the namespace — build
  `Identifier`s via `Identifier.fromNamespaceAndPath(Signpost.MOD_ID, …)`.
- `.gitattributes` enforces LF for `*.java`, but **CRLF for `*.gradle`** — keep that in mind when
  rewriting build scripts.
- Don't commit or push; leave that to the maintainer.
