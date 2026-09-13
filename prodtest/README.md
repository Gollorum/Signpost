# Production-test staging area

Jars and server installs used by the **production runtime test**
(`.claude/skills/full-prod-test/`). That harness launches the game from an installed
`.minecraft` profile instead of from Gradle, so unlike the dev run tasks it has no
dependency graph to pull mods from — everything that is not built from this repo has to
be put here by hand.

Everything below is gitignored except this README; a fresh clone starts empty.

## Layout

```
prodtest/
  deps/<loader>/       dependencies Signpost needs in order to load at all
  extras/<loader>/     optional companion mods -> enables the "-mods" runs
  servers/<loader>/    a prepared production server install -> enables the server runs
```

`<loader>` is `fabric`, `neoforge` or `forge`.

### deps/

What `fabric.mod.json` / `neoforge.mods.toml` list as required, for the Minecraft version in
`gradle.properties`. Today that is Fabric only:

```
prodtest/deps/fabric/fabric-api-<version>.jar
prodtest/deps/fabric/cloth-config-<version>-fabric.jar
```

NeoForge and Forge builds need nothing extra. Without `deps/fabric` the Fabric client runs
fail outright, which is a real failure and not a skip — so populate this one.

### extras/

Waystones, Repurposed Structures and whatever they pull in (Balm, MidnightLib), matching the
Minecraft version. These enable the `*-client-mods` and `*-server-mods` runs; with the folder
empty those runs are reported as `SKIP`.

**Version them deliberately.** The jars in the dev `runs/*/mods` folders belong to whichever
branch was last checked out and are usually for a different Minecraft version — do not copy
them across without checking.

### servers/

A production server install per loader, prepared once from the loader's own installer, plus a
`start.cmd` that launches it in the foreground:

```
prodtest/servers/neoforge/start.cmd
prodtest/servers/fabric/start.cmd
```

A minimal Fabric one:

```bat
@echo off
java -Xmx4G -jar fabric-server-launch.jar nogui
```

NeoForge and Forge servers ship their own argument files, so theirs is roughly:

```bat
@echo off
java -Xmx4G @libraries/net/neoforged/neoforge/<version>/win_args.txt nogui
```

The harness overwrites `mods/`, `world/` and `eula.txt` in that directory on every run and
leaves the rest alone. Without a `start.cmd` the server runs are reported as `SKIP`.

## Why this is not in git

Third-party mod jars are not ours to redistribute, and a server install is hundreds of
megabytes. `.gitignore` therefore excludes everything here except this README.
