---
name: full-runtime-test
description: Run the full 12-run runtime loading test for Signpost - every loader (fabric/neoforge/forge), both sides (client/server), with and without other mods - each loading a pristine save written by a previous released version. Use this ONLY immediately before producing the final build/release artifacts of a change, or when the user explicitly asks for the full runtime test. Do NOT run it during ordinary development, after individual edits, or as a routine check - it takes a long time and opens real Minecraft client windows. For everyday verification use the single-server check in AGENTS.md instead.
---

# Full runtime loading test

## When to run this

Run it in exactly two situations:

1. **Immediately before the final build** of a change — after the code is settled and you
   are about to produce release artifacts. This is the gate; nothing ships without it.
2. **When the user explicitly asks** for the full runtime test.

Do **not** run it at any other point. It launches 12 Minecraft instances sequentially,
takes a long time, and opens real client windows on the user's desktop. During ordinary
development, verify by compiling. The single dedicated-server run described in AGENTS.md
(*Runtime verification*) is itself not a routine check either - it is run when the user asks
for it, or as the last step before a final build.

If you are mid-task and unsure whether you are at "the final build" yet, you are not.

## What it proves

Building only proves the code compiles. This harness proves the mod actually **loads**:

- every loader starts on both the dedicated server and the client;
- it does so both alone and alongside Waystones / Repurposed Structures;
- and in all 12 combinations it loads a save written by a **previously released version**,
  without serialization, codec, registry, or mixin errors.

That last point is the whole reason this exists. Compilation cannot catch a renamed NBT key,
a client-only class referenced from common code, or a mixin whose target moved — those only
fail at load time, and some of them (a `SavedData` that fails to parse) do not even crash the
game. They just silently discard the player's data.

## Prerequisites

The corpus at `testsaves/<signpost>/<minecraft>/world/` must exist. It is gitignored, so a fresh clone
has none, and the harness will exit with code 2 and instructions rather than pass vacuously.
See `testsaves/README.md` for how to populate it. **Never** point the corpus at a save
produced by the current working tree — it must come from a released build, otherwise the
test proves nothing about compatibility.

## Auditing the corpus

`audit-test-save.py` (stdlib Python only) reads `level.dat`, the mod's `SavedData` and the
region files, and reports what a save actually is:

```bash
python .claude/skills/full-runtime-test/audit-test-save.py            # every corpus entry
python .claude/skills/full-runtime-test/audit-test-save.py some/world # any save folder
```

It prints the Minecraft version, the loader, the exact Signpost version from `level.dat`'s
`fml` mod list, the number of waystones in the library and of `signpost:post` /
`signpost:waystone` block entities, and the size. It **fails** a save that was written by
the version currently in `gradle.properties` (that tests nothing), that has no Signpost
content at all, or that is a corpus entry with no `SOURCE.txt`.

The harness runs it automatically before the matrix and refuses to start on a failure, so a
bad corpus costs seconds rather than an hour.

**What it cannot do is prove authenticity.** `level.dat` stores a version *string*, and that
string does not move during development, so a dev build and the release it became are
indistinguishable afterwards — one such save fails with `Not a list` despite reporting the
right version on the right Minecraft version. That is what `SOURCE.txt` is for, and why
corpus saves must be made from the *published jar*. See `testsaves/README.md`.

## Running it

```bash
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/skills/full-runtime-test/run-full-runtime-test.ps1
```

Useful switches while diagnosing a failure — never for the pre-release gate, which must be
the full matrix:

| Switch | Effect |
| --- | --- |
| `-ServersOnly` | The 6 server runs only; opens no windows. |
| `-Only fabric,forge-server` | Filter on run ids: an exact id selects just that run, anything else is a substring match. |
| `-SaveVersion 2.03.0` | Load a specific corpus version (default: highest-sorting). |
| `-ServerTimeoutSec` / `-ClientTimeoutSec` | Defaults 420 / 600 seconds. |

Run it in the background and watch for completion — a full matrix takes a while. It is
Windows-only (it uses CIM/WMI to find the forked game JVMs).

## How it stays stable

These are deliberate, and worth preserving if you edit the script:

- **Strictly sequential.** Two servers would collide on port 25565; two clients would
  contend for the GPU. Before each run it kills leftover game JVMs and waits for 25565.
- **The corpus is never written to.** Each run gets a fresh `robocopy` of the pristine save
  into the run directory, so a run that corrupts or migrates a world cannot poison later
  runs or the next invocation.
- **Game JVMs are matched by command line, not by name.** NeoForge has `fml.modFolders`,
  Fabric has `net.fabricmc.loader`, Forge has `ForgeBootstrap`. Gradle's daemon and wrapper
  are also `java.exe` and must not be killed.
- **Benign noise is filtered before failure matching**, not after. Netty's kqueue/epoll
  probes on Windows literally contain `NoClassDefFoundError`, so order matters.
- **Clients boot straight into the save** via `--quickPlaySingleplayer`, injected by the
  `-PsignpostQuickPlay=<save>` property that the loader build scripts read. Without the
  property the run configurations are untouched, so normal dev runs are unaffected.
- **Success markers differ per side**: a server has loaded the world at `Done (`; a client
  has only really entered it at `Loaded <n> advancements`. Reaching the main menu is not a
  pass for a client.
- **A blocking error dialog is treated as a failure and killed.** When mods do not resolve,
  Fabric opens a modal Swing window ("Incompatible mods found!", titled `Fabric Loader <ver>`)
  and the JVM waits for a human to click Exit - the run would otherwise burn its full timeout
  and leave a window on the user's screen. Each poll checks running `java` processes for an
  *error dialog* title and fails the run immediately. The match list deliberately excludes the
  game's own window title, and the loader-specific log lines (`Some of your mods are
  incompatible`, ...) are in the fail patterns as a second net. If you ever kill the harness
  mid-run, sweep manually - no PowerShell cleanup runs when the script itself is terminated:
  `Get-Process java | Where-Object MainWindowTitle | Stop-Process -Force`.
- **Server runs only load force-loaded chunks.** Since 1.21.11 `prepareLevels()` re-activates
  persistent tickets instead of loading a spawn radius, so a corpus save that force-loads
  nothing exercises the mod's `SavedData` and no block-entity NBT. The auditor reports this
  as `loaded on start: N of M`. Clients differ - they load around the stored player
  position, so leave the player somewhere interesting.

## Reading the result

Exit code `0` = all runs passed, `1` = at least one failed, `2` = setup problem (no corpus,
bad filter). Per-run logs and a `summary.txt` land in `build/runtime-test/<timestamp>/`.

When a run fails, open its log and find the first failure-pattern hit. Treat these as real
until proven otherwise, and check the failure against a **released** version before
"fixing" compatibility — see AGENTS.md, *Before "fixing" a compatibility break*. In
particular the branch `1.21-dynamic-post-types` has deliberately broken compatibility and
is not merged into `1.21`; never chase its format.

Report honestly: state which of the 12 ran, which passed, and what you did not verify. A
partial run is not a pass.
