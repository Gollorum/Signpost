# Runtime-test save corpus

Pristine Minecraft saves used by the **full runtime loading test**
(`.claude/skills/full-runtime-test/`). The harness copies a save out of here into every
run directory before each of its 12 runs, so the game can never write back into this
folder — these copies stay authoritative.

## Layout

```
testsaves/
  2.03.0/
    world/          <- a complete save folder: level.dat, region/, data/, entities/, ...
  2.04.0/
    world/
```

The directory name is the **Signpost version that last wrote the save**. The harness picks
the highest-sorting version by default; override with `-SaveVersion 2.03.0`.

## Why this is not in git

A real world is 16–250 MB (region files are ~95% of it), which would bloat the repository.
`.gitignore` therefore excludes everything here except this README. That means a fresh
clone cannot run the test until the corpus is populated — the harness detects this and
exits with instructions rather than silently passing.

## Adding a save for a new release

Do this **once per release**, from the **published jar** — not from a tag you build
yourself, and never from the working tree. Building the tag looks equivalent but is not:
it cannot prove the jar matches what players actually run, and a save made from a
"nearly released" tree is exactly the trap described below.

1. Download the released jar from Modrinth or CurseForge. Note its filename and URL.
2. Put it in a plain Minecraft instance (or an empty `mods/` folder) on the Minecraft
   version that release targeted, with its dependencies — Balm + Waystones, and
   Repurposed Structures if you want that path covered.
3. **Use NeoForge or Forge, not Fabric.** Only those write an `fml` mod list into
   `level.dat`, which is what lets the audit verify afterwards which versions wrote the
   save. A Fabric-made save is unverifiable after the fact.
4. Create a **new, small** world. Stay near spawn — region files are ~95% of the size,
   and this gets copied 12 times per test run.
5. Place what is worth regression-testing: several signposts of different post types with
   sign parts actually filled in, a waystone with a name, a waystone generator, and a sign
   pointing at that waystone. The GUI work is why this step is manual — `/setblock` places
   the block but leaves the block-entity data empty, which is the data that matters.
6. Quit to title and exit **cleanly**, so the save is flushed and consistent.
7. Copy the world folder to `testsaves/<version>/world/`.
8. Write `testsaves/<version>/SOURCE.txt` (see below).
9. Audit it: `python .claude/skills/full-runtime-test/audit-test-save.py testsaves/<version>`
10. Prove it loads before trusting it:
    `... run-full-runtime-test.ps1 -SaveVersion <version> -ServersOnly`

## SOURCE.txt

Required beside every corpus entry. `level.dat` records only a version *string*, and that
string does not change during development — so it cannot tell a release apart from a dev
build that carried the same number. Provenance has to be recorded when the save is made:

```
released_version: 2.03.0
source_url:       https://modrinth.com/mod/signpost/version/xxxxxxxx
artifact:         signpost-neoforge-1.21.10-2.03.0.jar
sha256:           <hash of the downloaded jar>
minecraft:        1.21.10
loader:           neoforge 21.10.8
created:          2026-08-30
notes:            fresh flat world, 4 posts (oak/stone/iron/warped) + 1 named waystone near spawn
```

The auditor fails a corpus entry that has no `SOURCE.txt`, and fails one whose
`released_version` disagrees with the `fml` mod list in `level.dat`.

Keep the saves small. Explore as little as possible: region files dominate the size, and a
world that only ever loaded the spawn area is a few MB rather than a few hundred.

## A save from an unreleased state is worse than no save

The corpus must come from a **tagged, published** release. A world written by some
intermediate development state will fail in ways that look exactly like real bugs but are
not, and chasing them wastes a lot of time.

This is not hypothetical. A save that looked fine by its folder name fails with:

```
Failed to parse saved data for 'SavedDataType[signpost_WaystoneLibrary]': Not a list
```

Its `level.dat` says why: it is a **Minecraft 1.21.5** world, two Minecraft versions behind
the current target. Signpost registers no DataFixers, so nothing migrates its data across
the 1.21.5 -> 1.21.10 -> 1.21.11 ports, and the mod's codecs changed along the way. Nothing
in the shipped code is wrong - the save is simply too old to be a meaningful corpus entry.

Use a save from the **release immediately before** the one under test, on the Minecraft
version that release targeted. `audit-test-save.py` prints both, so you can check at a
glance rather than guessing from a folder name.

The nastier case has nothing to give away. A second world — correct Minecraft version
(1.21.10), `level.dat` reporting `signpost 2.03.0`, 92 signposts and 4 waystones in it —
also fails, with `Not a list`. It was written six days *before* the `v2.03.0` tag, and the
commit in between (`80fb316`, 2025-11-02) changed `PlayerHandle` to `UUIDUtil.CODEC`, which
writes the player id as an int array instead of a compound. The version string never moved,
so nothing in the save distinguishes it from the real release.

That is the whole reason `SOURCE.txt` exists. Neither the version string, nor the file
dates (a `SavedData` that fails to parse is left untouched on disk, so its mtime keeps
advancing while its contents stay stale), nor the folder name can establish authenticity
after the fact. Only recording the published artifact at creation time can.

Before trusting a new corpus entry:

1. Confirm the version really shipped - it should have a git tag *and* be published
   (Modrinth / CurseForge). A bumped `version=` in `gradle.properties` proves nothing.
2. Check the writing version, which the server prints on load, e.g.
   `signpost (version 2.03.0 -> 2.03.1)`.
3. Load it once with the harness and confirm it passes *before* relying on it to judge new
   code. A corpus entry that has never passed cannot tell you anything.

Related: the branch `1.21-dynamic-post-types` deliberately broke compatibility and is not
merged into `1.21`. Never build a corpus entry from it. Its merged form does read old saves - see
"The pre-2.04 post blocks must stay registered" in `AGENTS.md` - but a corpus entry still has to
come from a published release, and that branch never was one.

## Covering auto-generated village signposts

Signpost injects a waystone (weight 1) and a signpost (weight 3) into the
`village/<type>/houses` pool of all five village types - desert, plains, savanna, snowy,
taiga - plus their zombie variants. A reasonably sized village therefore contains a few
generated pieces, and those are worth loading in the test because nothing else exercises
the worldgen path.

**Putting the village at spawn is not what makes it load.** Since 1.21.11 a dedicated
server no longer loads a radius around the world spawn on startup:
`MinecraftServer.prepareLevels()` only re-activates *persistent tickets*. Our own runs prove
it - every one of them logs `Loading 0 persistent chunks` and finishes the spawn area in
~12 ms, which means the server-side tests currently deserialize **no chunks at all** and
validate only the `WaystoneLibrary` SavedData.

Two different levers, and you want both:

| Coverage | Lever |
| --- | --- |
| Server runs | `/forceload add` over the village. The tickets are saved in `data/chunks.dat` and re-activated on every load, so those chunks and their block entities are deserialized at startup. |
| Client runs | The **player position** stored in the save. A client loads chunks around the player, so leave the player standing in the village before quitting (`/setworldspawn` there too, for a fresh profile). |

### Recipe

With the released jar running (see above), and village generation enabled in the config:

```
/locate structure minecraft:village_plains
/tp @s <x> ~ <z>
```

Fly around the whole village so every piece generates, then:

```
/setworldspawn <x> <y> <z>
/forceload add <x1> <z1> <x2> <z2>
```

`/forceload add` takes **block** coordinates and marks whole chunks; it covers at most 256
chunks per command, which is far more than a village needs. Include the area where you
hand-placed your own signposts as well. Then stand in the village, quit to title, and exit.

`/forceload query` lists what is marked, and the auditor reports it as `forced chunks`
together with `loaded on start: N of M signpost block entities are in forced chunks`. If
that says `0 of M`, the save will not validate any block-entity NBT on a server run.

## What a good test save contains

The point is to catch serialization and compatibility breakage, so the save should exercise
the data that actually gets persisted:

- `data/signpost_WaystoneLibrary.dat` — the mod's `SavedData`. This is the file that broke
  when a codec field name was renamed during the 1.21.11 port.
- Placed signposts and waystones, so their block-entity NBT is deserialized on chunk load.
- Ideally more than one post model/type, and a sign pointing at a waystone.
