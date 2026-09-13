"""Builds and runs the command the official Minecraft launcher would run, for a PRODUCTION game.

The Gradle run tasks in `full-runtime-test` start the game from the dev environment, which on
Fabric means **named mappings**. Players get **intermediary**. Anything that depends on a
Minecraft member's *name* therefore behaves differently for a player than it does in any dev
run - see Signpost issue #118, where reflection over `MapColor`'s fields found `WOOD` in dev
and `field_16019` for everyone else, and no world could be loaded.

This script closes that gap by launching the same jars a player launches: it reads an installed
profile out of `.minecraft/versions/`, merges it with the vanilla version it inherits from,
evaluates the library rules for this OS, and expands the argument templates exactly as the
launcher does. Replicating the launcher is the point - where its behaviour looks odd (see
`version_name` below), copy it rather than out-thinking it.

Authentication is offline (`--accessToken 0`). That is enough for singleplayer, which never
contacts the session server, and keeps the harness from needing the user's real credentials.
Multiplayer, Realms and skins do not work under it.

Used by run-full-prod-test.ps1; also usable on its own:

  python launch-prod.py --version fabric-loader-0.19.5-1.21.1 \
                        --game-dir build/prod-test/scratch --quick-play-world world
"""
import argparse
import json
import os
import platform
import shutil
import subprocess
import sys

SEP = ";" if platform.system() == "Windows" else ":"
OSNAME = {"Windows": "windows", "Linux": "linux", "Darwin": "osx"}[platform.system()]


def default_mc_home():
    if platform.system() == "Windows":
        return os.path.expandvars(r"%APPDATA%\.minecraft").replace("\\", "/")
    if platform.system() == "Darwin":
        return os.path.expanduser("~/Library/Application Support/minecraft")
    return os.path.expanduser("~/.minecraft")


def load_version(mc, vid, _seen=None):
    """Read versions/<id>/<id>.json, merging any profile it inherits from.

    Modded profiles (fabric-loader-*, neoforge-*, <mc>-forge-*) carry only their own
    libraries and arguments and inherit the rest. The child's libraries must come FIRST on
    the classpath: that is how a loader overrides a vanilla library version.
    """
    _seen = _seen or set()
    if vid in _seen:
        raise SystemExit(f"version profile cycle at {vid}")
    _seen.add(vid)

    path = f"{mc}/versions/{vid}/{vid}.json"
    if not os.path.exists(path):
        raise SystemExit(f"no such profile: {path}")
    with open(path, encoding="utf-8") as f:
        v = json.load(f)

    if not v.get("inheritsFrom"):
        v["jar"] = v.get("jar", vid)
        return v

    parent = load_version(mc, v["inheritsFrom"], _seen)
    merged = dict(parent)
    merged.update({k: val for k, val in v.items() if k not in ("libraries", "arguments")})
    merged["libraries"] = v.get("libraries", []) + parent.get("libraries", [])
    merged["arguments"] = {
        k: v.get("arguments", {}).get(k, []) + parent.get("arguments", {}).get(k, [])
        for k in ("game", "jvm")
    }
    merged["jar"] = parent.get("jar", v["inheritsFrom"])
    return merged


def allowed(rules):
    """Vanilla rule evaluation: default deny once any rule exists, last match wins.

    Feature-gated entries (demo mode, custom resolution, the launcher's own quick-play slots)
    are skipped - we pass quick play ourselves, after the templated arguments.
    """
    if not rules:
        return True
    result = False
    for rule in rules:
        os_rule = rule.get("os", {})
        if os_rule.get("name") and os_rule["name"] != OSNAME:
            continue
        if rule.get("features"):
            continue
        result = rule["action"] == "allow"
    return result


def maven_path(name):
    """group:artifact:version[:classifier] -> the path the launcher stores it at."""
    parts = name.split(":")
    group, artifact, version = parts[0], parts[1], parts[2]
    classifier = parts[3] if len(parts) > 3 else None
    fn = f"{artifact}-{version}" + (f"-{classifier}" if classifier else "") + ".jar"
    return "/".join([group.replace(".", "/"), artifact, version, fn])


def client_jar(mc, vid, v):
    """The client jar the launcher puts on the classpath, named after the PROFILE.

    The launcher does not put the inherited versions/<mc>/<mc>.jar on the classpath: it copies
    it to versions/<profile>/<profile>.jar and uses that. The name is not cosmetic. NeoForge's
    -DignoreList is `client-extra,${version_name}.jar`, and ${version_name} expands to the
    profile id - verified against this machine's real launcher_log.txt, which shows the Mojang
    launcher passing `-DignoreList=client-extra,neoforge-21.1.250.jar`. So the entry matches,
    and the vanilla jar stays off the module path, ONLY when the jar is named after the
    profile. Point at <mc>.jar instead and bootstraplauncher turns it into an automatic module
    that collides with NeoForge's own srg minecraft module, before any mod class loads:

        ResolutionException: Module minecraft contains package net.minecraft.client,
        module _1._21._1 exports package net.minecraft.client to minecraft

    A profile the official launcher has never run - one this harness installed itself - has no
    copy yet, so make it, which is what the launcher does on first launch.
    """
    dst = f"{mc}/versions/{vid}/{vid}.jar"
    if os.path.exists(dst):
        return dst
    src = f"{mc}/versions/{v['jar']}/{v['jar']}.jar"
    if not os.path.exists(src):
        print(f"no client jar to launch: {src}", file=sys.stderr)
        raise SystemExit(2)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    print(f"copying {src} -> {dst} (the launcher does this on first launch)", file=sys.stderr)
    shutil.copyfile(src, dst)
    return dst


def classpath(mc, vid, v):
    seen, out, missing = set(), [], []
    for lib in v["libraries"]:
        if not allowed(lib.get("rules")):
            continue
        art = lib.get("downloads", {}).get("artifact")
        rel = art["path"] if art else maven_path(lib["name"])
        if rel in seen:
            continue
        seen.add(rel)
        p = f"{mc}/libraries/{rel}"
        (out if os.path.exists(p) else missing).append(p)
    if missing:
        # A silently short classpath fails much later as a baffling NoClassDefFoundError.
        print("missing libraries - run this profile once from the official launcher:",
              file=sys.stderr)
        for m in missing:
            print(f"  {m}", file=sys.stderr)
        raise SystemExit(2)
    out.append(client_jar(mc, vid, v))
    return SEP.join(out)


def expand(args, subs):
    out = []
    for a in args:
        if isinstance(a, dict):
            if not allowed(a.get("rules")):
                continue
            vals = a["value"]
            out.extend(vals if isinstance(vals, list) else [vals])
        else:
            out.append(a)
    return [substitute(s, subs) for s in out]


def substitute(s, subs):
    for k, val in subs.items():
        s = s.replace("${" + k + "}", val)
    return s


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--version", required=True,
                    help="installed profile id, e.g. fabric-loader-0.19.5-1.21.1 or neoforge-21.1.250")
    ap.add_argument("--game-dir", required=True, help="game directory; created if absent")
    ap.add_argument("--mc-home", default=default_mc_home(),
                    help="the .minecraft holding versions/ libraries/ assets/")
    ap.add_argument("--java", default="java", help="java executable (Java 21 for 1.21+)")
    ap.add_argument("--quick-play-world",
                    help="save folder name to boot straight into, skipping the menus")
    ap.add_argument("--username", default="SignpostTest")
    ap.add_argument("--max-memory", default="4G")
    ap.add_argument("--print-only", action="store_true", help="print the command, run nothing")
    a = ap.parse_args()

    mc = a.mc_home.replace("\\", "/")
    v = load_version(mc, a.version)

    game_dir = os.path.abspath(a.game_dir).replace("\\", "/")
    natives = f"{game_dir}/natives"
    for d in (f"{game_dir}/mods", natives):
        os.makedirs(d, exist_ok=True)

    subs = {
        "auth_player_name": a.username,
        # The PROFILE id, not the version it inherits from - confirmed against the real
        # launcher_log.txt. NeoForge's -DignoreList is `${version_name}.jar` and depends on
        # it matching the client jar's filename, which is why client_jar() names that jar
        # after the profile too. Change one and you must change the other.
        "version_name": a.version,
        "game_directory": game_dir,
        "assets_root": f"{mc}/assets",
        "game_assets": f"{mc}/assets",
        "assets_index_name": v["assetIndex"]["id"],
        "auth_uuid": "00000000000040008000000000000000",
        "auth_access_token": "0",
        "auth_session": "0",
        "clientid": "0",
        "auth_xuid": "0",
        "user_type": "legacy",
        "version_type": v.get("type", "release"),
        "natives_directory": natives,
        "launcher_name": "signpost-prod-test",
        "launcher_version": "1",
        "classpath": classpath(mc, a.version, v),
        "library_directory": f"{mc}/libraries",
        "classpath_separator": SEP,
    }

    cmd = [a.java, f"-Xmx{a.max_memory}"]
    cmd += expand(v["arguments"]["jvm"], subs)
    cmd += [v["mainClass"]]
    cmd += expand(v["arguments"]["game"], subs)
    if a.quick_play_world:
        cmd += ["--quickPlaySingleplayer", a.quick_play_world]

    if a.print_only:
        print(" ".join(f'"{c}"' if " " in c else c for c in cmd))
        return
    print(f"launching {a.version} in {game_dir}", file=sys.stderr)
    sys.exit(subprocess.call(cmd, cwd=game_dir))


if __name__ == "__main__":
    main()
