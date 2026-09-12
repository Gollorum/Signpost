---
name: upgrade-minecraft-version
description: Upgrade the Signpost mod to a target Minecraft version end to end - bump Minecraft, all three loaders, Parchment/NeoForm, the Gradle plugins, the wrapper and JDK if needed, and every mod dependency; fix the resulting compile errors on fabric, neoforge and forge; re-run NeoForge datagen; refresh the mod jars in the run folders; and verify with the full runtime test. Use when the user asks to update, upgrade or port the project to a specific Minecraft version (e.g. "upgrade to 26.2", "port to 1.21.12"). Not for routine dependency bumps within the same Minecraft version.
---

# Upgrade to a target Minecraft version

The goal is narrow and absolute: **the project targets the requested Minecraft version, all
four subprojects build, every mod feature still exists, and the mod integrations still work.**
An upgrade that compiles but silently drops a feature or breaks save compatibility is a failed
upgrade.

Take the target version from the user. If they did not give one, ask - never guess.

## Phase 0 - Probe before touching anything

```bash
python .claude/skills/upgrade-minecraft-version/probe-versions.py <target>
```

It changes nothing. It reports, for the target version: NeoForm, NeoForge, Forge, Fabric
loader/API, Parchment, the three Gradle plugins, the Gradle version the newest Loom demands,
and every mod integration and dev-environment mod - each against what `gradle.properties`
says today. Exit code 1 means it found a blocker.

**Resolve the blockers with the user before editing anything.** When a loader or an
integration has no build for the target yet, waiting, dropping the integration, and dropping a
loader are all legitimate answers, but they are the user's to pick. See AGENTS.md, *Toolchain
constraints*.

Two traps the probe cannot fully protect you from:

- **Probe the exact patch version, not the line.** Minecraft's versioning changed: after
  1.21.11 come `26.1`, `26.1.1`, `26.1.2`, `26.2`. These are separate Minecraft versions with
  separate NeoForm/NeoForge/Forge builds, and the probe's prefix matching will happily mix
  them - it can report the newest NeoForge from `26.1.2` beside the newest Forge from bare
  `26.1`. If the target the user names is a line rather than a version, run the probe again on
  the newest patch in it and compare. Do not assume a `1.x.y` shape anywhere.
- **A Gradle floor is not automatically fatal to `forge`.** Loom's newest line sets a Gradle
  minimum; ForgeGradle 6 refuses to apply on Gradle 9 at all. But ForgeGradle **7** is the
  Gradle 9 line, and it is published under a *different maven artifact* -
  `net.minecraftforge:forgegradle` (lowercase) rather than `net.minecraftforge:ForgeGradle` -
  behind the same `net.minecraftforge.gradle` plugin id. Check both coordinates before
  concluding that Gradle 9 costs you the forge subproject. FG7's DSL differs substantially from
  FG6's; the Forge MDK and https://github.com/MinecraftForge/MDKExamples (`*/fg7/`) are the
  reference.

## Phase 1 - Read what actually changed

Do not guess at API changes; every loader publishes a porting document.

| Source | Use it for |
| --- | --- |
| [NeoForge primers](https://github.com/neoforged/.github/tree/main/primers) (`primers/<version>/index.md`) | The vanilla-side changes, in Mojang mappings. Applies to **all** loaders, not just NeoForge - this is the single most useful document. Also indexed at [docs.neoforged.net/primer](https://docs.neoforged.net/primer/docs/). |
| [NeoForge `docs/PORTING.md`](https://github.com/neoforged/NeoForge/blob/1.21.11/docs/PORTING.md) (swap the branch) | NeoForge-specific migrations. |
| [Fabric porting docs](https://docs.fabricmc.net/develop/porting/) and [Fabric API porting](https://docs.fabricmc.net/develop/porting/fabric-api) | Fabric-side changes. |
| [fabricmc.net news posts](https://fabricmc.net/) | Each release post states the **required Loom and Gradle versions** for that Minecraft version. Check this before bumping Loom. |
| [MultiLoader-Template branches](https://github.com/jaredlll08/MultiLoader-Template/branches/all) | This project is built on it. It has a branch per Minecraft version, so diffing the target branch's `build.gradle`, `settings.gradle`, `gradle.properties` and `buildSrc/` against ours shows exactly which build-script changes the upgrade needs. **Do not blindly copy** - our build has deliberate local divergences (forge subproject, quickPlay hooks, group wiring). |
| Forge: the [MinecraftForge repo](https://github.com/MinecraftForge/MinecraftForge) and its maven metadata | Forge publishes no primer; rely on the NeoForge primer for vanilla changes and read Forge's own commits for loader API churn. |

## Phase 2 - Bump the configuration

All versions live in `gradle.properties`; build scripts must never hard-code one. Anything new
you add there must also be added to the `expandProps` map in
`buildSrc/src/main/groovy/multiloader-common.gradle`.

Work through: `minecraft_version`, `minecraft_version_range`, `neo_form_version`,
`parchment_minecraft` / `parchment_version` if they still exist (keep the previous MC version
if the target has no **stable** Parchment, never pin a nightly - and drop the layer entirely if
Parchment has abandoned the line, as it has for 26.x, where vanilla ships deobfuscated with
Mojang's own parameter names anyway), `neoforge_version`, `neoforge_loader_version_range`,
`forge_version`, `forge_loader_version_range`, `fabric_version`, `fabric_loader_version`,
`cloth_config_version`, `modmenu_version`, `waystones_fabric_version` /
`waystones_neoforge_version`, `repurposed_structures_*_version`, `java_version`, and
`java_bytecode_version`.

Then the things outside `gradle.properties`:

- plugin versions in the root `build.gradle` (`fabric-loom`, `net.neoforged.moddev`) and
  `forge/build.gradle` (`net.minecraftforge.gradle`, `org.spongepowered.mixin`);
- `gradle/wrapper/gradle-wrapper.properties` if the toolchain demands it - use
  `./gradlew wrapper --gradle-version <v>` rather than hand-editing;
- the JDK: `java_version` drives the toolchain, and `compatibilityLevel` in all four
  `*.mixins.json` files must match it;
- `fabric.mod.json` `depends.java` / `depends.minecraft`.

## Phase 3 - Make it compile, one loader at a time

`./gradlew :common:build` first - everything else depends on it - then `:neoforge:`,
`:fabric:`, `:forge:`. Remember the constraints from AGENTS.md: the Gradle daemon and parallel
execution must stay off, and `forge` needs
`systemProp.net.minecraftforge.gradle.repo.sources.force=true`.

While fixing errors:

- **Never let a persisted NBT key follow a class rename.** `fieldOf("...")` strings are
  on-disk data. The 1.21.11 port renamed `ResourceLocation` to `Identifier` and swept four
  codec field names along with it, which silently destroyed every existing world's waystone
  library. Rename the Java type; leave the string alone. See AGENTS.md, *Codec field names are
  on-disk data*.
- **Re-verify every mixin target.** `signpost.mixins.json` uses `defaultRequire: 1`, so a
  moved or renamed target fails at load, not at compile. Check each `@Mixin` class and each
  accessor field name against the new Minecraft sources
  (`common/build/moddev/artifacts/vanilla-<ver>-sources.jar`).
- **Re-check `DataFixersInjector` by hand.** It injects into `DataFixers.addFixers`, which is
  `private static` - Mojang keeps no promises about it, and `defaultRequire: 1` turns a moved
  target into a load-time failure. Two assumptions have to still hold, and neither is a compile
  error if it stops being true:
  - `addFixers` still takes the `DataFixerBuilder` and still runs inside `DataFixers.<clinit>`
    before `build()`. That is the last moment a fixer can be added.
  - No vanilla schema is registered at the current data version. `SignpostDataFixes` uses
    sub-version 1 precisely so it cannot collide, but check the new version's last
    `builder.addSchema(...)` call anyway - a silent collision would replace a vanilla schema.

  The fixes re-run on every upgrade, because a save from the previous version is still behind
  the new data version. That is intended and safe: both are idempotent. See AGENTS.md,
  *Post types are data, and the pre-2.04 ids are migrated by a DataFixer*.
- **Keep the three loaders in step.** A new `Services` interface needs an implementation *and*
  a `META-INF/services` file in fabric, neoforge and forge. A missing provider throws at
  runtime, not at compile time.
- Preserve behaviour. If an API disappears, find the replacement - do not delete the feature.
  If something genuinely cannot be ported, stop and tell the user rather than quietly dropping
  it.

## Phase 4 - Regenerate data

**Datagen is NeoForge's job, and only NeoForge's** - verified: `DataGeneration` in
`neoforge/src/main/java/gollorum/signpost/data/` is the only `GatherDataEvent` listener in the
repo, Fabric declares no datagen entrypoint, and Forge registers no providers (its `data` run
is inert and `forge/.../data/StructurePoolElementProvider.java` is unused).

```bash
./gradlew :neoforge:runData
```

It writes into `common/src/generated/resources`, which every loader consumes. Then
**`git diff` the generated output and read it** - a datagen run that silently produces fewer
files than before means a provider stopped registering. Never hand-edit generated resources.

## Phase 5 - Refresh the run-folder mods

Every jar in the `*WithMods` folders is for the *previous* Minecraft version at this point, and
a stale jar is worse than none - the loader may still accept it, and the run then tests the
wrong thing while reporting PASS. Refresh them with:

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/skills/full-runtime-test/fetch-mod-jars.ps1
```

It reads `minecraft_version` from `gradle.properties`, downloads into the cache beside the repo
(`../mod-jar-cache/<minecraft_version>/`), verifies every jar against the Modrinth SHA-1, clears
each run folder and installs the right set. Cached jars are not re-downloaded, so re-running it
is cheap. `-List` shows candidates without downloading; `-NoInstall` fills the cache only.

**It does not resolve dependency conflicts for you** - it prints each Fabric jar's `depends` and
`breaks`, and you read them:

- **Never take "newest of everything" on trust.** Sodium declares which Iris versions it breaks
  and the newest pair is regularly incompatible. On 26.1.2, Sodium `0.9.2` breaks
  `iris <=1.11.3` while `1.11.3` is the newest Iris, so the run needs
  `-Pin sodium=mc26.1.2-0.9.1-fabric`. Check this every upgrade; it changes.
- **Chains move between Minecraft versions.** Waystones gained a second BlayTheNinth library
  (Shogi) on 26.x, and dropped Forge entirely. Read the `depends` block rather than assuming
  last version's chain.
- **A project may not have a build yet.** The script prints `NOT PUBLISHED for <version>` and
  installs nothing for it. Say so explicitly in the final report - that loader loses that
  coverage until it ships.
- **Version tags are not uniform.** Most projects publish against the full `26.1.2`; Repurposed
  Structures tags `26.1`. The script falls back to the `<major>.<minor>` prefix and flags it as
  `tagged 26.1, not 26.1.2`. Glance at those.

If the set of mods itself changes - a new required library, a loader losing a build - edit
`.claude/skills/full-runtime-test/mod-jars.json` and the matching table in AGENTS.md together.

## Phase 6 - Prove it runs

Compiling proves nothing. Finish with the full runtime test:

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/skills/full-runtime-test/run-full-runtime-test.ps1
```

All 12 runs must pass. The corpus save must come from the **last release made on the previous
Minecraft version** - after this upgrade, that is the release you were on before starting. A
save two Minecraft versions back is still not a valid test: the only DataFixers the mod ships
cover the pre-2.04 post block and item ids, and nothing migrates its codecs across Minecraft
versions. Audit the corpus first (`audit-test-save.py`) and check `loaded on start: N of M` is
not `0 of M`.

While the corpus predates 2.04, the run also exercises the post-id migration, and it is worth
confirming rather than assuming: after a server run, the world it left behind should hold
`signpost:post_wood` / `post_stone` / `post_metal` / `post_mushroom` in its chunk palettes and
no `signpost:post_<wood type>` anywhere. If the injector stopped applying, the world loads
without an error and the signposts simply turn into the wrong post type - so a clean log does
not prove this one.

## Phase 7 - Report

State plainly: the target version reached; the final version of every component; which
subprojects build; the datagen diff; which integrations had no build for the target; every
runtime run that passed or failed; and anything you could not verify. An upgrade is not done
because it compiles.

## Also update

- `AGENTS.md` - the *Versions*, *Toolchain constraints* and *Optional mod integrations*
  sections carry concrete version numbers and pin rationales that go stale on every upgrade.
- `testsaves/README.md` if the corpus expectations change.
- `.gitattributes`/`.gitignore` only if the upgrade introduces new generated paths.
