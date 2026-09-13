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

Run the server for **every loader you touched**, and for all three if you changed `common`.
Note that the Forge task names differ - ForgeGradle uses the bare run name:

| Loader | Server | Server + other mods |
| --- | --- | --- |
| fabric | `:fabric:runServer` | `:fabric:runServerWithMods` |
| neoforge | `:neoforge:runServer` | `:neoforge:runServerWithMods` |
| forge | `:forge:Server` | `:forge:ServerWithMods` |

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
in `testsaves/<signpost>/<minecraft>/world/` (gitignored, see `testsaves/README.md`); the worlds sitting in
the run directories are dev scratch, and the full-runtime-test harness moves any it finds aside
to `world.preserved-<timestamp>` the first time it plants a corpus save, rather than deleting
them. **Do the same by hand** - `mv` the existing `world` to `world.preserved-<timestamp>` before
copying a corpus save over it. Those scratch worlds are gitignored, so an `rm -rf` is
unrecoverable.

The run passes only if all of these hold:

1. The server reaches `Done (…s)! For help, type "help"`.
2. The existing world loads - no world-load failure, and the server does not fall back to
   generating a fresh one. On 1.21.1 a dedicated server still loads a radius of spawn chunks
   at startup (the "force-loaded chunks only" behaviour that makes `Loading N persistent
   chunks` worth watching arrived in 1.21.11), so block-entity NBT near spawn is deserialized
   without the corpus having to `/forceload` anything. A corpus that force-loads its area
   anyway is still the more reliable shape - see `testsaves/README.md`.
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

#### This branch ships 27 post model types, not 28

`pale_oak` is absent. Pale oak wood, `minecraft:pale_oak_sign`, the `pale_oak_logs` item tag and
both of its block textures arrived in 1.21.4, so the type cannot exist on 1.21.1 at all. It is
dropped from `neoforge/.../data/PostModelTypes.java`; nothing else refers to it, and because post
model types live in a datapack registry rather than in the block registry, no save can contain one
- so unlike a missing *block* id this drops nothing and shifts no palette. Its four lang keys are
deliberately left in place so the language files stay identical to the 1.21 branch.

For the same reason the `diorite` type's secondary (accent) texture is
`minecraft:block/quartz_block_side` here rather than the `stripped_pale_oak_log` used on 1.21.11.
If a texture is ever added to a model type, check it against the 1.21.1 asset set first - the
whole set can be listed out of
`common/build/moddev/artifacts/vanilla-<ver>-client-extra-aka-minecraft-resources.jar`.

#### Codec field names are on-disk data

`fieldOf("...")` strings are persisted NBT keys. **Never let them follow a class rename.** The
1.21.11 port renamed `ResourceLocation` to `Identifier` and swept the string literals along with
it in four codecs (`VillageWaystone.ChunkEntryKey`, `ResourceLocationSerializer`, `Texture`,
`FluidTint`), which made every pre-existing world fail with
`Failed to parse saved data for 'SavedDataType[signpost_WaystoneLibrary]': No key Identifier`
and silently drop the entire waystone library. The key stays `"ResourceLocation"`; only the Java
type changed. On this branch the type is called `ResourceLocation` again (1.21.1 predates the
rename), and the backport reversed the type name **without** touching those four key strings -
they still read `"ResourceLocation"`, which is what keeps 1.21.11-written and 1.21.1-written
saves describing the same bytes. Note the failure did **not** crash the server - it logged one ERROR line and carried
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

## Client rendering on 1.21.1

Two things the 1.21.11 branch gets from vanilla APIs that do not exist here. Both fail *silently* -
they compile, load and log nothing, the thing simply does not appear on screen.

### Post items are drawn by a BEWLR that each loader must bind

1.21.11 names a renderer straight from the item model (`special` + `SpecialModelRenderers`). 1.21.1
has neither, so `PostItemRenderer` is a `BlockEntityWithoutLevelRenderer`, the generated item models
parent `builtin/entity` (see `neoforge/.../data/ItemModels.java`), and **the binding is per loader**:

| Loader | Mechanism |
| --- | --- |
| fabric | `BuiltinItemRendererRegistry.INSTANCE.register(item, ...)` in `SignpostFabricClient` |
| neoforge | `RegisterClientExtensionsEvent#registerItem` in `SignpostNeoforge.ModBusEvents` |
| forge | `Item#initializeClient` - Forge has no such event, hence `forge/.../block/PostItemImpl.java` |

`builtin/entity` with nothing bound to it renders **nothing at all** - no model, no error. If post
items go invisible, check these three call sites before suspecting the renderer.

`builtin/entity` also inherits **no display transforms**, so `ItemModels.builtinEntity` writes them
out by hand, and they must stay equal to `minecraft:block/block`'s - that is what the item inherited
on 1.21.11 through `block/cube_all`. `PostItemRenderer` applies its own per-context rotation *on top*
of them, so the two are a matched pair: change one and the post renders facing the wrong way. (The
abandoned 2.03.0 backport carries different numbers here; they are wrong, and produce a post rotated
roughly 180 degrees in the GUI.)

### Text shadows are suppressed on the Font, not on the widget

1.21.1's `EditBox` has no `setTextShadow` and always calls the shadowed `GuiGraphics.drawString`
overload, so `InputBox` gives itself a private `Font` copy and turns shadows off on it through the
`ConfigurableFont` mixin / `IConfigurableFont` duck interface. Copying is the point: the flag lives on
the `Font` instance, so setting it on Minecraft's shared font would strip shadows from all text.

Two traps here, both hit during the 1.21.1 backport:

- **Do not wrap `GuiGraphics` to intercept `drawString`.** Its `(Minecraft, PoseStack, BufferSource)`
  constructor is private, so a subclass can only reach the public one, which builds a **fresh identity
  `PoseStack`**. Everything drawn through the wrapper - text *and* `fill` - then lands outside the
  screen's transform and is invisible, while input handling keeps working, so the widget looks dead
  rather than broken.
- **Name both `drawInternal` overloads explicitly**, not `drawInternal*`. `EditBox` draws through the
  `FormattedCharSequence` overload, and a wildcard produces a single Fabric refmap entry - which
  resolves to the `String` overload and silently leaves the one that matters uninjected. Check
  `signpost.refmap.json` for two `drawInternal` entries after touching that mixin.

### GUI widgets compete on depth, not on draw order

Everything a widget batches into the screen's buffer source is flushed at the end of the frame, so
anything that flushes *earlier* - notably `GuiModelRenderer`, which draws the 3D sign preview - is
already in the depth buffer by then. At equal depth the model wins and the widget disappears.

`InputBox` therefore takes a `zOffset`, and `SignGui` passes `100` to the six sign text boxes that sit
on top of the sign model. `AngleInputBox`, `ColorInputBox` and `ImageInputBox` forward the offset their
callers already pass. **These parameters exist for exactly this reason** - they look vestigial, because
1.21.11 does not need them and leaves them unused, and dropping them on this branch makes the sign text
invisible.

The symptom is very misleading, so recognise it:

- the box takes keystrokes and the caret moves through the text normally;
- the caret is *visible* - `EditBox` draws it with `RenderType.guiOverlay()`, which ignores depth - and
  in its own hardcoded light grey (`-3092272`), **not** the configured text colour, so a box set to
  black text shows a white caret;
- the hover highlight is missing too, since it is batched the same way;
- sibling boxes render fine whenever no model overlaps them.

Two things that are *not* the cause, both of which look plausible: the `ConfigurableFont` shadow
suppression (it works - and if the mixin had failed to apply, the `IConfigurableFont` cast in
`InputBox` would throw rather than render blank), and `Font.SHADOW_OFFSET`. Vanilla does offset the
main text pass by +0.03 z when it draws a shadow, but 0.03 is nowhere near enough to clear a 3D model.
`setBordered(false)` is also worth checking before blaming rendering: the hover highlight is drawn only
when the box is unbordered, which is why `AngleInputBox` and `ColorInputBox` legitimately have none.

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

Current targets: Minecraft 1.21.1, Java 21, NeoForge 21.1.250, Fabric Loader 0.17.3 /
Fabric API 0.116.17, Forge 52.1.16, Parchment 1.21.1.

This branch is the **1.21.1 backport of 2.04.0**, made from the 1.21.11 branch. Signpost has
never had a release on 1.21.1 (2.02.0 was 1.20.1, 2.03.0 was 1.21.10), which is what makes the
post-id DataFixer viable here - see [Post types are data](#post-types-are-data-and-the-pre-204-ids-are-migrated-by-a-datafixer).

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
- **Waystones is pinned to `21.1.15+1.21.1`**, behind the current release, because Waystones
  rebuilt every 1.21.1 artifact from `21.1.36` onwards with Loom 1.14.10, and Loom refuses to
  consume a mod built by a newer Loom (`Mod was built with a newer version of Loom (1.14.10),
  you are using Loom (1.13.6)`). `21.1.15` is the newest 1.21.1 build still made with Loom 1.7.
  It is compile-only and only the adapter API is used, so the integration still works against
  current Waystones at runtime - the run folders can carry a newer jar. The Fabric dependency is
  also declared `transitive = false`, because Waystones 21.1.x pulls JourneyMap and other
  CurseForge artifacts in as transitive mod dependencies.
- Repurposed Structures needs **no** pin here, unlike on the 1.21.11 branch: its 1.21.1 builds
  are made with Loom 1.6, so both loaders track the current `7.5.22+1.21.1-*`.
- Consequence: **dropping the Forge subproject is what unblocks Gradle 9, Loom 1.14+, and a
  current Waystones pin.** They cannot be done separately.

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

The set currently installed for 1.21.1, all from Modrinth and all verified against the SHA-1 the
API publishes:

| Mod | Version |
| --- | --- |
| Balm | `21.0.65` (`+fabric-`/`+neoforge-`/`+forge-1.21.1`) |
| Waystones | `21.1.44+1.21.1`, all three loaders |
| Repurposed Structures | `7.5.22+1.21.1-fabric` / `-neoforge` |
| MidnightLib | `1.9.3+1.21.1` (fabric) |
| Sodium | `0.6.13+mc1.21.1` (fabric, client) |
| Iris | `1.8.8+mc1.21.1` (fabric, client) |

Required chains when refreshing these: **Waystones needs Balm** (`>=21.0.39`), **RS on Fabric
needs MidnightLib** (`>=1.5.7`), and **Iris needs Sodium**.

**Do not just take the newest of everything** - check each jar's `breaks` block, not only its
`depends`. Two traps on 1.21.1, both of which make Fabric refuse to launch:

- **Sodium and Iris.** The newest of each are *mutually* incompatible: Sodium `0.8.13` breaks
  `iris <1.8.13`, while the newest Iris *release* for 1.21.1 (`1.8.8`) depends on `sodium 0.6.x`
  and so cannot satisfy it. The working pair is one line back on Sodium: **Sodium `0.6.13` +
  Iris `1.8.8`** - `0.6.13` breaks only `iris <1.8.7`. Taking Sodium `0.8.x` would force Iris
  onto a beta.
- **Waystones and the Fabric Loader.** Every 1.21.1 Waystones from `21.1.36` up requires
  `fabricloader >=0.17.3`, which is why `fabric_loader_version` is `0.17.3` rather than a 0.16
  release. Dropping the loader below that means dropping the run-folder Waystones to `21.1.15`
  as well. (That pairing does work - `21.1.15` wants a `balm-fabric` dependency, which current
  Balm still satisfies via `provides: ["balm-fabric"]` - but it would test a much older mod.)

Note the run-folder Waystones (`21.1.44`) is deliberately newer than the `waystones_version`
compile pin (`21.1.15`); the pin is a Loom constraint, not a runtime one - see
[Toolchain constraints](#toolchain-constraints).
Sodium and Iris are client-only. Forge has no Repurposed Structures build, so its folders only
carry Balm and Waystones. Keep these jars on the same Minecraft version as `minecraft_version`;
the plain `client`/`server` runs deliberately have empty `mods/` folders.

## Forge

The upstream MultiLoader-Template dropped Forge, but this repo keeps the subproject alive and it
still has to be maintained. It is wired up by hand: ForgeGradle (`net.minecraftforge.gradle`) plus
the SpongePowered `mixin` Gradle plugin and explicit refmap/`MixinConfigs` manifest handling, instead
of ModDevGradle/Loom.

It builds on 1.21.1 (Forge 52.1.16). One setting in `gradle.properties` exists solely for it, so
if `forge` breaks, suspect the build environment before the Java sources:
`org.gradle.daemon=false` and `org.gradle.parallel=false` - see [Build & run](#build--run).

Two things the 1.21.11 branch needs and this one deliberately does **not**:

- No `systemProp.net.minecraftforge.gradle.repo.sources.force=true`. That works around a
  ForgeGradle bug assembling the patched jar for Forge 61 (`ZipException: duplicate entry:
  mcp/client/Start.class`); Forge 52 assembles from binaries cleanly, so forcing the sources path
  would only cost a full Forge decompile on every fresh checkout. Verified from a cleared
  `fg_cache`.
- No `net.minecraftforge:eventbus-validator` annotation processor. That is an EventBus 7
  artifact; Forge 52 ships EventBus 6 and the validator dies during `init` with
  `NullPointerException ... Elements.getTypeElement(...) is null`.

Forge 52 uses **EventBus 6**: `net.minecraftforge.eventbus.api.IEventBus` and
`net.minecraftforge.eventbus.api.SubscribeEvent`, `context.getModEventBus()`,
`MinecraftForge.EVENT_BUS` (in `net.minecraftforge.common`), and `bus.addListener(...)` /
`bus.register(obj)` - not the `BusGroup` / `Event.BUS` / `register(MethodHandles.lookup(), ...)`
API of EventBus 7.

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
packet handler changes) and verify with `./gradlew :forge:build`. Forge is the loader most likely
to need a runtime run on top of that - if one is called for, note its run task is `Server`, not
`runServer` (see [Runtime verification](#runtime-verification) for when to run one at all).

## Conventions

- Java 21, 4-space indent, mod code under `gollorum.signpost.*`.
- `Signpost.LOGGER` (SLF4J) for logging; `Signpost.MOD_ID` for the namespace — build
  `Identifier`s via `Identifier.fromNamespaceAndPath(Signpost.MOD_ID, …)`.
- `.gitattributes` enforces LF for `*.java`, but **CRLF for `*.gradle`** — keep that in mind when
  rewriting build scripts.
- Don't commit or push; leave that to the maintainer.
