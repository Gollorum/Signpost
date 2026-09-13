---
name: full-prod-test
description: Build Signpost and run the production runtime loading test - the game launched the way a player launches it, from an installed .minecraft profile on intermediary mappings, loading a pristine save written by a previous released version. Run this ONLY when the user explicitly asks for it by name. Never run it on your own initiative - not before a release, not after an edit, not as a check that a fix worked, not "to be safe". It builds every loader, opens real Minecraft windows on the user's desktop and takes a long time. For everyday verification compile; for the pre-release gate use full-runtime-test.
---

# Production runtime loading test

## When to run this

**Only when the user explicitly asks for it** — by name (`/full-prod-test`), or in words that
plainly mean this harness ("run the production test", "test it in real Minecraft").

Do not run it on your own initiative, for any reason. Not before a release, not after an
edit, not to confirm a fix you just made, not because a bug looked mapping-related, not
because the user seems like they would want it. It builds all three loaders, launches real
Minecraft windows on the user's desktop, and takes a long time. If you think it ought to run,
say so and let the user decide.

This is stricter than `full-runtime-test`, which may also run as the pre-release gate. This
one has no automatic trigger at all.

## What it proves that nothing else can

`full-runtime-test` starts the game through the Gradle run tasks, i.e. from the dev
environment. On Fabric that means **named** mappings. Players get **intermediary**. Any code
whose behaviour depends on a Minecraft member's *name* therefore behaves differently for a
player than it does in every dev run, and the entire 12-run matrix will pass while the mod is
completely broken for everyone.

That is not hypothetical — it is issue #118. `MapColorSerializer` read map colour names off
`MapColor`'s fields with reflection. In dev that found `WOOD`; in production it found
`field_16019`, so every `post_model_types` JSON failed to parse, the datapack registry failed
to load, and **no world could be opened or created at all**. Every dev run passed throughout.

This harness closes that hole by launching the same jars a player launches:

- the installed `.minecraft` profile for each loader, through `launch-prod.py`;
- remapped mod jars out of `<loader>/build/libs`, not the dev classpath;
- booting straight into a save written by a **previously released** version.

It does not replace `full-runtime-test`. That one covers combinations this one cannot reach,
and runs far more cheaply.

## Prerequisites

**The save corpus** — the one thing that cannot be downloaded, because it has to be a world
written by a previously released build. See below.

**Java 21**, **python**, and a `.minecraft` directory. Everything else is fetched.

The loader profiles the harness looks for, and installs when they are absent:

| Loader | Profile |
| --- | --- |
| fabric | any `fabric-loader-*-<minecraft_version>` (the loader version is a floor, not a pin) |
| neoforge | `neoforge-<neoforge_version>` |
| forge | `<minecraft_version>-forge-<forge_version>` |

One caveat on a profile the harness installed itself: it fetches the client jar and libraries
but **not the assets**, which are hundreds of megabytes and belong to the launcher. The game
starts and loads worlds without them; sounds and translated strings are missing. Launch the
profile once from the official launcher if that matters.

The corpus lives at `testsaves/<signpost>/<minecraft>/world/`, exactly as
`full-runtime-test` requires it. It is gitignored; see `testsaves/README.md`. A corpus written
by the version in `gradle.properties` is rejected — it would prove nothing about compatibility.

`-Java` overrides the JDK; otherwise the harness picks one matching the `javaVersion` the
Minecraft version asks for, preferring Mojang's own bundled runtime.

## Provisioning

The harness launches the game the way a player does, so there is no Gradle dependency graph
to pull mods from. `provision.py` fetches whatever is missing, before the matrix is built:

| What | Where it lands | Source |
| --- | --- | --- |
| Required mods (fabric-api, cloth-config) | `prodtest/deps/<loader>/` | Modrinth |
| Companion mods (Waystones, Repurposed Structures, and their dependencies) | `prodtest/extras/<loader>/` | Modrinth |
| A dedicated server install + `start.cmd` | `prodtest/servers/<loader>/` | the loader's own installer |
| A loader profile | **`<MinecraftHome>/versions/`** — outside the repo | the loader's own installer |
| Vanilla `<mc>` client jar and libraries, if absent | `<MinecraftHome>/` | piston-meta.mojang.com |

That last pair writes into the user's real `.minecraft`, alongside the profiles the official
launcher puts there. **`-NoDownload` turns all of it off** and restores plain `SKIP`
reporting, which is the right switch when the machine is offline or the user does not want
`.minecraft` touched.

Design points worth keeping:

- **The mod list is read off the built jar, not hard-coded.** `fabric.mod.json`'s `depends`
  and `suggests`, or `mods.toml`'s required/optional blocks, decide what gets fetched, so a
  dependency added in a later version is picked up without anyone remembering to update the
  script. A mod id with no entry in `MODRINTH_SLUGS` is a **hard error** for a required
  dependency, rather than being silently left out.
- **Transitive dependencies resolve through Modrinth**, so Waystones brings Balm and
  Repurposed Structures brings MidnightLib without either being listed.
- **Missing optional companions degrade, missing required ones fail.** Repurposed Structures
  has no Forge build for 1.21.1; the `forge-*-mods` runs happen anyway, with one fewer mod,
  and say so. A required dependency that cannot be found stops that loader instead.
- **Every download is checksum-verified** — sha512 from the Modrinth API, sha1 from maven and
  piston-meta — and a mismatch aborts rather than being used.
- **Stale jars are deleted.** A leftover older fabric-api beside a new one makes the loader
  refuse to start with a duplicate-mod error, which would look like a Signpost failure.
- **It is idempotent**: a second run costs one API call per project and downloads nothing.
- **Provisioning follows `-Only` / `-Loaders`.** Asking for `-Only fabric-client` must not
  spend minutes installing Forge.

## Running it

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/skills/full-prod-test/run-full-prod-test.ps1
```

| Switch | Effect |
| --- | --- |
| `-Only fabric-client,neoforge` | Filter on run ids: an exact id selects just that run, anything else is a substring match. |
| `-Loaders fabric,neoforge` | Restrict to these loaders even where others are installed. |
| `-SkipBuild` | Reuse the jars already in `<loader>/build/libs`. |
| `-NoDownload` | Download nothing; report missing pieces as `SKIP` (offline, or to leave `.minecraft` alone). |
| `-SaveVersion 2.03.0` | Load a specific corpus version (default: highest-sorting). |
| `-MinecraftHome` | A different `.minecraft` (default `%APPDATA%\.minecraft`). |
| `-Java` | The JDK to run the game with. |
| `-ClientTimeoutSec` / `-ServerTimeoutSec` | Defaults 600 / 420 seconds. |

Run it in the background and watch for completion. Windows only — it uses CIM/WMI to find and
stop the launched game JVMs.

`launch-prod.py` is usable on its own when you want one ad-hoc production launch:

```bash
python .claude/skills/full-prod-test/launch-prod.py \
  --version fabric-loader-0.19.5-1.21.1 --game-dir build/prod-test/scratch \
  --java "C:/Program Files/Java/jdk-21/bin/java.exe" --quick-play-world world
```

## How it stays stable

Deliberate, and worth preserving if you edit the script:

- **It replicates the launcher rather than out-thinking it.** `launch-prod.py` merges the
  profile with the version it inherits from (child libraries first, so a loader can override
  a vanilla library), evaluates the OS `rules`, and expands the same argument templates. Where
  its behaviour looks arbitrary, copy it rather than reason about it. The client jar is the
  case that bites: the launcher copies the inherited `versions/<mc>/<mc>.jar` to
  `versions/<profile>/<profile>.jar` and puts *that* on the classpath, so `${version_name}.jar`
  - which expands to the profile id - names it. NeoForge's `-DignoreList` is exactly that
  string, and it is the whole reason the vanilla jar stays off the module path. Using
  `<mc>.jar` directly looks equivalent and is not: the jar goes unignored, becomes automatic
  module `_1._21._1`, and collides with NeoForge's own srg `minecraft` module before a single
  mod class loads. A profile the official launcher has never run has no copy yet, so the
  harness makes one.
- **A short classpath is fatal, not a warning.** A missing library otherwise surfaces much
  later as a baffling `NoClassDefFoundError`; the launcher exits 2 and names the files.
- **Offline auth** (`--accessToken 0`). Singleplayer never contacts the session server, so the
  harness needs no credentials. Multiplayer, Realms and skins do not work under it.
- **Throwaway game directories** under `build/prod-test/<timestamp>/<run-id>/`, so the user's
  real `.minecraft` — saves, options, mods — is never written to. Assets are reused read-only.
- **The corpus is never written to.** Each run gets a fresh `robocopy` of the pristine save.
- **Game JVMs are matched on `-Dminecraft.launcher.brand=signpost-prod-test`**, which only
  this harness sets. A game the user started by hand, and Gradle's daemon, cannot be caught by
  the cleanup. If you kill the harness mid-run, sweep by hand with the same filter.
- **Severity-based failure matching.** Fabric does *not* tag log lines with the mod id outside
  the dev environment, so `full-runtime-test`'s `/ERROR] (signpost)` pattern can never match
  here. This harness fails on any `/ERROR]` or `/FATAL]` the benign list does not excuse —
  practical only because these runs load Signpost and its dependencies and nothing else. Every
  entry added to `$BenignPatterns` is a hole in that net; justify it in a comment.
  `No data fixer registered for` is the one that must stay: vanilla logs it at ERROR for every
  modded block entity in release builds, including Signpost's own three.
- **`Failed to load level data or datapacks` is an explicit failure pattern.** It is the line
  vanilla logs from `WorldOpenFlows` before showing the "safe mode" screen — the #118
  signature, and the thing a datapack-registry regression looks like.
- **Entering the world is the pass condition, not reaching the menu.** Servers pass on
  `Done (`, clients on **`joined the game`** — and specifically *not* on
  `Loaded <n> advancements`, which looks like the same thing but fires once while the world
  stem's datapacks load, before vanilla can still stop on a confirmation screen. Using it
  passed a client that was sitting on "Worlds using Experimental Settings are not supported".
- **Two modal screens are dismissed automatically**, because both wait for a human and both
  otherwise burn the whole timeout:
  - *Accessibility onboarding* ("Would you like to enable the Narrator?"), shown in a game
    directory the client has never run in. Handled by writing `onboardAccessibility:false`
    into `options.txt` before launch — data, not input, so it cannot go wrong.
  - *"Worlds using Experimental Settings are not supported"*, shown by `WorldOpenFlows` when
    `worldGenSettingsLifecycle()` is not stable, which a modded world often is not. This one
    is drawn inside the GL window with no OS dialog to find, so it is dismissed with
    synthesized keystrokes: Tab, Tab, Enter. That is deliberate — the buttons are "Create
    Backup and Load", "I know what I'm doing!", "Cancel", so both of the first two proceed and
    only a third Tab would reach the one that aborts.
  - The keystrokes are only sent once a run has loaded its datapacks and then failed to enter
    the world for 20 seconds, and at most three times, so a client that is merely slow is
    never typed at and one that is genuinely stuck still fails rather than being nudged
    forever.
- **A blocking error dialog fails the run immediately.** A loader's modal Swing dialog makes
  the JVM wait forever for a human to click Exit; the run would otherwise burn its whole
  timeout and leave a window on the user's screen.
- **Strictly sequential.** Two clients would contend for the GPU, two servers for port 25565.

## Reading the result

Exit `0` = everything that ran passed, `1` = at least one run failed, `2` = setup problem
(no profile installed, no corpus, bad filter). Per-run logs and `summary.txt` land in
`build/prod-test/<timestamp>/`; each run keeps both the launcher's streams
(`<id>.launcher.log`) and the game's own `latest.log` (`<id>.game.log`).

**`SKIP` is not `PASS`.** Report which runs actually ran, which were skipped and why, and
which loaders were not installed. A matrix that was mostly skipped is not a green light, and
saying "the production test passed" without that breakdown is a false report.

When a run fails, open its `.game.log` and find the first failure-pattern hit. Check the
failure against a **released** version before "fixing" compatibility — see AGENTS.md,
*Before "fixing" a compatibility break*.
