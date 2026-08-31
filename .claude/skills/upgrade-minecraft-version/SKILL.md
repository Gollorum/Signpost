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

**Resolve the blockers with the user before editing anything.** The recurring one for this
repo is the Gradle triangle:

> Loom >= 1.14 requires Gradle 9. ForgeGradle 6 requires Gradle 8. They cannot coexist.

So a Loom bump that needs Gradle 9 is a decision about whether the `forge` subproject
survives, not a version bump. Same class of question when a loader or an integration has no
build for the target yet: waiting, dropping the integration, and dropping a loader are all
legitimate answers, but they are the user's to pick. See AGENTS.md, *Toolchain constraints*.

Note Minecraft's versioning changed: after 1.21.11 come `26.1`, `26.2`, ... Do not assume a
`1.x.y` shape anywhere.

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
`parchment_minecraft` / `parchment_version` (keep the previous MC version if the target has no
**stable** Parchment - never pin a nightly), `neoforge_version`,
`neoforge_loader_version_range`, `forge_version`, `forge_loader_version_range`,
`fabric_version`, `fabric_loader_version`, `cloth_config_version`, `modmenu_version`,
`waystones_version`, `repurposed_structures_*_version`, and `java_version`.

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

Update every jar in the `*WithMods` folders to the target version - see AGENTS.md, *Testing
against the real mods*, for the folder table and required chains.

**Read each jar's `breaks` block, not just its `depends`.** Taking the newest of everything is
how you get a pair Fabric refuses to launch: Sodium declares which Iris versions it breaks, and
the newest Sodium and newest Iris are routinely incompatible. Verify downloads by the SHA-1
that the Modrinth API publishes.

If an integration has no build for the target yet, say so explicitly in the final report -
that loader loses that coverage until it ships.

## Phase 6 - Prove it runs

Compiling proves nothing. Finish with the full runtime test:

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/skills/full-runtime-test/run-full-runtime-test.ps1
```

All 12 runs must pass. The corpus save must come from the **last release made on the previous
Minecraft version** - after this upgrade, that is the release you were on before starting. A
save two Minecraft versions back is not a valid test: the mod ships no DataFixers. Audit the
corpus first (`audit-test-save.py`) and check `loaded on start: N of M` is not `0 of M`.

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
