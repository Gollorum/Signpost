"""Fetches everything the production test needs but does not build itself.

The harness launches the game the way a player does, so it has no Gradle dependency graph to
pull mods from and no dev environment to run in. Whatever is missing has to be downloaded:
the mods Signpost declares, the companion mods the "-mods" runs exist to test against, the
loader profile in .minecraft, and a dedicated server install.

Everything here is idempotent - already-present files with the right hash are left alone -
and every download is checksum-verified where the source publishes one.

Sources, all first-party:
  api.modrinth.com          mod jars (sha512 from the API, verified)
  maven.fabricmc.net        Fabric installer (sha1 from maven, verified)
  maven.neoforged.net       NeoForge installer (sha1 from maven, verified)
  maven.minecraftforge.net  Forge installer (sha1 from maven, verified)
  piston-meta.mojang.com    vanilla version manifest, client jar and libraries (sha1, verified)

Subcommands:
  mods    --loader <l> --mc <v> --deps-dir <d> --extras-dir <d> [--jar <built jar>]
  client  --loader <l> --mc <v> --loader-version <v> --mc-home <d>
  server  --loader <l> --mc <v> --loader-version <v> --server-dir <d>
"""
import argparse
import hashlib
import json
import os
import platform
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

UA = {"User-Agent": "Gollorum/Signpost-prod-test (+https://github.com/Gollorum/Signpost)"}

# Mod id (as the jar's own metadata spells it) -> Modrinth slug. A dict value selects the slug
# per loader, which some projects need: Repurposed Structures publishes Fabric and
# NeoForge/Forge as two separate projects.
MODRINTH_SLUGS = {
    "fabric-api": "fabric-api",
    "cloth-config": "cloth-config",
    "waystones": "waystones",
    "repurposed_structures": {
        "fabric": "repurposed-structures-fabric",
        "neoforge": "repurposed-structures-forge",
        "forge": "repurposed-structures-forge",
    },
}

# Ids that are the platform itself, never a downloadable mod.
PLATFORM_IDS = {"minecraft", "java", "fabricloader", "fabric", "neoforge", "forge", "forgeloader"}

# Companions the "-mods" runs exist to test against, when a jar declares none of its own.
DEFAULT_EXTRAS = ["waystones", "repurposed_structures"]


def log(msg):
    print("  " + msg, flush=True)


def http_json(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA)) as r:
        return json.load(r)


def http_bytes(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA)) as r:
        return r.read()


def digest(path, algo):
    h = hashlib.new(algo)
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def download(url, dest, expect=None, algo="sha512"):
    """Download to dest, verifying the checksum. Returns False when it was already there."""
    if os.path.exists(dest) and expect and digest(dest, algo) == expect.lower():
        return False
    os.makedirs(os.path.dirname(dest) or ".", exist_ok=True)
    tmp = dest + ".part"
    data = http_bytes(url)
    with open(tmp, "wb") as f:
        f.write(data)
    if expect:
        got = digest(tmp, algo)
        if got != expect.lower():
            os.remove(tmp)
            raise SystemExit("checksum mismatch for %s\n  expected %s\n  got      %s"
                             % (url, expect, got))
    os.replace(tmp, dest)
    return True


# ---------------------------------------------------------------------------------------
# Mods
# ---------------------------------------------------------------------------------------

def declared_mods(jar_path):
    """(required, optional) mod ids the built jar itself declares, platform ids removed.

    Reading them off the jar rather than hard-coding a list means a dependency added in a
    later version is fetched without anyone remembering to update this script - and an
    unmappable one fails loudly instead of being silently left out.
    """
    import zipfile
    required, optional = [], []
    with zipfile.ZipFile(jar_path) as z:
        names = z.namelist()
        if "fabric.mod.json" in names:
            meta = json.loads(z.read("fabric.mod.json"))
            required = list(meta.get("depends", {}).keys())
            optional = list(meta.get("suggests", {}).keys())
        else:
            toml = next((n for n in names if n.endswith("mods.toml")), None)
            if toml:
                text = z.read(toml).decode("utf-8", "replace")
                for block in re.split(r"\[\[dependencies[^\]]*\]\]", text)[1:]:
                    mid = re.search(r'modId\s*=\s*"([^"]+)"', block)
                    typ = re.search(r'type\s*=\s*"([^"]+)"', block)
                    if not mid:
                        continue
                    (required if (typ and typ.group(1) == "required") else optional).append(mid.group(1))
    strip = lambda ids: [m for m in ids if m.lower() not in PLATFORM_IDS]
    return strip(required), strip(optional)


def slug_for(mod_id, loader):
    entry = MODRINTH_SLUGS.get(mod_id)
    if isinstance(entry, dict):
        return entry.get(loader)
    return entry


def modrinth_version(slug, loader, mc):
    q = urllib.parse.urlencode({"loaders": json.dumps([loader]),
                                "game_versions": json.dumps([mc])})
    versions = http_json("https://api.modrinth.com/v2/project/%s/version?%s" % (slug, q))
    return versions[0] if versions else None


_project_cache = {}


def project_slug(project_id):
    if project_id not in _project_cache:
        _project_cache[project_id] = http_json(
            "https://api.modrinth.com/v2/project/%s" % project_id)["slug"]
    return _project_cache[project_id]


def resolve(slug, loader, mc, seen, out, required):
    """Collect this project's file and, recursively, everything it requires."""
    if slug in seen:
        return True
    seen.add(slug)
    version = modrinth_version(slug, loader, mc)
    if not version:
        if required:
            raise SystemExit(
                "no %s build of '%s' for Minecraft %s on Modrinth.\n"
                "Signpost requires it, so this loader cannot be tested until it exists."
                % (loader, slug, mc))
        # An optional companion with no build for this loader is not an error: the "-mods"
        # run still happens, with one fewer mod in it.
        log("%-30s no %s build for %s - the -mods runs go without it" % (slug, loader, mc))
        return False
    primary = next((f for f in version["files"] if f.get("primary")), version["files"][0])
    out.append((slug, version["version_number"], primary))
    for dep in version["dependencies"]:
        if dep.get("dependency_type") != "required" or not dep.get("project_id"):
            continue
        resolve(project_slug(dep["project_id"]), loader, mc, seen, out, True)
    return True


def sync_dir(target, wanted):
    """Make `target` hold exactly `wanted`, downloading and removing as needed.

    Removing is the point: a stale fabric-api left beside a new one makes the loader refuse
    to start with a duplicate-mod error, which would look like a Signpost failure.
    """
    os.makedirs(target, exist_ok=True)
    keep = set()
    for slug, number, f in wanted:
        dest = os.path.join(target, f["filename"])
        keep.add(f["filename"])
        fetched = download(f["url"], dest, f["hashes"].get("sha512"), "sha512")
        log("%-30s %-26s %s" % (slug, number, "downloaded" if fetched else "already present"))
    for name in os.listdir(target):
        if name.endswith(".jar") and name not in keep:
            os.remove(os.path.join(target, name))
            log("%-30s %s" % ("(stale)", "removed " + name))


def cmd_mods(a):
    required, optional = ([], [])
    if a.jar and os.path.exists(a.jar):
        required, optional = declared_mods(a.jar)
    if not optional:
        # NeoForge and Forge metadata declares no optional companions, but the "-mods" runs
        # are defined by what they test against, not by what the toml happens to mention.
        optional = list(DEFAULT_EXTRAS)

    # Carried across both passes: a companion that pulls in something already installed as a
    # required dependency (Waystones wants fabric-api, so does Signpost) must not be fetched
    # into both directories, or the same jar lands in mods/ twice.
    seen = set()
    for label, ids, target, is_required in (
        ("required", required, a.deps_dir, True),
        ("companion", optional, a.extras_dir, False),
    ):
        print("%s mods for %s (%s):" % (label, a.loader, a.mc), flush=True)
        wanted = []
        for mod_id in ids:
            slug = slug_for(mod_id, a.loader)
            if not slug:
                if is_required:
                    raise SystemExit(
                        "'%s' is required by the built jar but has no Modrinth slug in "
                        "provision.py's MODRINTH_SLUGS. Add it there." % mod_id)
                log("%-30s no Modrinth slug known - skipped" % mod_id)
                continue
            resolve(slug, a.loader, a.mc, seen, wanted, is_required)
        if not ids:
            log("(none declared)")
        if a.dry_run:
            for slug, number, f in wanted:
                log("%-30s %-26s %s" % (slug, number, f["filename"]))
            if ids and not wanted:
                log("(nothing resolved)")
            continue
        sync_dir(target, wanted)
    return 0


# ---------------------------------------------------------------------------------------
# Loader installers
# ---------------------------------------------------------------------------------------

def maven_latest(metadata_url):
    root = ET.fromstring(http_bytes(metadata_url))
    latest = root.findtext("./versioning/release") or root.findtext("./versioning/latest")
    return latest


def maven_get(url, dest):
    """Download a maven artifact, verifying the .sha1 the repository publishes beside it."""
    try:
        expect = http_bytes(url + ".sha1").decode().split()[0]
    except Exception:
        expect = None
    download(url, dest, expect, "sha1")
    return dest


def installer_jar(loader, mc, loader_version, workdir):
    if loader == "fabric":
        base = "https://maven.fabricmc.net/net/fabricmc/fabric-installer"
        v = maven_latest(base + "/maven-metadata.xml")
        return maven_get("%s/%s/fabric-installer-%s.jar" % (base, v, v),
                         os.path.join(workdir, "fabric-installer.jar"))
    if loader == "neoforge":
        url = ("https://maven.neoforged.net/releases/net/neoforged/neoforge/"
               "%s/neoforge-%s-installer.jar" % (loader_version, loader_version))
        return maven_get(url, os.path.join(workdir, "neoforge-installer.jar"))
    if loader == "forge":
        tag = "%s-%s" % (mc, loader_version)
        url = ("https://maven.minecraftforge.net/net/minecraftforge/forge/"
               "%s/forge-%s-installer.jar" % (tag, tag))
        return maven_get(url, os.path.join(workdir, "forge-installer.jar"))
    raise SystemExit("unknown loader " + loader)


def ensure_vanilla(mc, mc_home, java):
    """The vanilla version has to be on disk before a loader profile can inherit from it.

    Fabric's profile inherits the jar and libraries outright; the NeoForge and Forge client
    installers refuse to run without versions/<mc>/<mc>.jar. Assets are left alone - they are
    huge, shared, and only needed for sound and language files.
    """
    vdir = os.path.join(mc_home, "versions", mc)
    vjson, vjar = os.path.join(vdir, mc + ".json"), os.path.join(vdir, mc + ".jar")
    if os.path.exists(vjson) and os.path.exists(vjar):
        return
    log("vanilla %s is missing - fetching it from piston-meta" % mc)
    manifest = http_json("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
    entry = next((v for v in manifest["versions"] if v["id"] == mc), None)
    if not entry:
        raise SystemExit("Minecraft %s is not in Mojang's version manifest." % mc)
    meta = http_json(entry["url"])
    os.makedirs(vdir, exist_ok=True)
    with open(vjson, "w", encoding="utf-8") as f:
        json.dump(meta, f)
    client = meta["downloads"]["client"]
    download(client["url"], vjar, client["sha1"], "sha1")
    for lib in meta.get("libraries", []):
        art = lib.get("downloads", {}).get("artifact")
        if art and art.get("url"):
            download(art["url"], os.path.join(mc_home, "libraries", art["path"]),
                     art.get("sha1"), "sha1")
    log("vanilla %s installed (assets not fetched - the launcher owns those)" % mc)


def run_installer(cmd, what, cwd=None):
    # Run from the installer's own scratch directory: Forge writes "<installer>.jar.log" into
    # the working directory, which would otherwise litter the repo root on every install.
    log("running %s installer: %s" % (what, " ".join(os.path.basename(c) for c in cmd[:3])))
    proc = subprocess.run(cmd, capture_output=True, text=True,
                          cwd=cwd or os.path.dirname(cmd[2]) or None)
    if proc.returncode != 0:
        sys.stderr.write(proc.stdout[-4000:] + "\n" + proc.stderr[-4000:] + "\n")
        raise SystemExit("%s installer failed with exit %d" % (what, proc.returncode))


def cmd_client(a):
    with tempfile.TemporaryDirectory() as work:
        ensure_vanilla(a.mc, a.mc_home, a.java)
        jar = installer_jar(a.loader, a.mc, a.loader_version, work)
        if a.loader == "fabric":
            run_installer([a.java, "-jar", jar, "client", "-dir", a.mc_home,
                           "-mcversion", a.mc, "-loader", a.loader_version, "-noprofile"],
                          "fabric")
        elif a.loader == "neoforge":
            run_installer([a.java, "-jar", jar, "--install-client", a.mc_home], "neoforge")
        else:
            run_installer([a.java, "-jar", jar, "--installClient", a.mc_home], "forge")
    log("client profile installed into %s" % os.path.join(a.mc_home, "versions"))
    return 0


SERVER_START = """@echo off
rem Written by provision.py for the Signpost production test. Foreground, no GUI.
{cmd}
"""

# Stamped onto every server JVM this harness starts. Client JVMs already carry it through
# -Dminecraft.launcher.brand, and the harness's cleanup matches on the string - without it a
# server from a previous run keeps running, holds its library jars open, and the next install
# fails with "the process cannot access the file".
BRAND = "signpost-prod-test"
BRAND_ARG = "-Dsignpost.prodtest=" + BRAND


def server_installed(server_dir):
    """True when a server install is already present, so the installer can be skipped."""
    if os.path.exists(os.path.join(server_dir, "fabric-server-launch.jar")):
        return True
    for root, _dirs, files in os.walk(os.path.join(server_dir, "libraries")):
        if "win_args.txt" in files or "unix_args.txt" in files:
            return True
    return False


def cmd_server(a):
    os.makedirs(a.server_dir, exist_ok=True)
    already = server_installed(a.server_dir)
    if already:
        log("server already installed - refreshing start.cmd and settings only")
    with tempfile.TemporaryDirectory() as work:
        if a.loader == "fabric":
            if not already:
                jar = installer_jar(a.loader, a.mc, a.loader_version, work)
                run_installer([a.java, "-jar", jar, "server", "-dir", a.server_dir,
                               "-mcversion", a.mc, "-loader", a.loader_version,
                               "-downloadMinecraft"], "fabric")
            cmd = '"%s" -Xmx4G %s -jar fabric-server-launch.jar nogui' % (a.java, BRAND_ARG)
        else:
            if not already:
                jar = installer_jar(a.loader, a.mc, a.loader_version, work)
                flag = "--install-server" if a.loader == "neoforge" else "--installServer"
                run_installer([a.java, "-jar", jar, flag, a.server_dir], a.loader)
            cmd = _args_file_cmd(a.server_dir, a.java)
    with open(os.path.join(a.server_dir, "start.cmd"), "w", encoding="ascii") as f:
        f.write(SERVER_START.format(cmd=cmd))
    with open(os.path.join(a.server_dir, "eula.txt"), "w", encoding="ascii") as f:
        f.write("eula=true\n")
    # Written up front because a server with no server.properties logs "Failed to load
    # properties from file" at ERROR on its first start, before writing the defaults - which
    # the harness would otherwise have to excuse for every genuinely-first run.
    props = os.path.join(a.server_dir, "server.properties")
    if not os.path.exists(props):
        with open(props, "w", encoding="ascii") as f:
            f.write("level-name=world\n"
                    "online-mode=false\n"
                    "max-players=4\n"
                    "view-distance=10\n"
                    "simulation-distance=10\n"
                    "motd=Signpost production test\n")
    log("server installed in %s" % a.server_dir)
    return 0


def _args_file_cmd(server_dir, java):
    """NeoForge and Forge servers launch through a generated @argument file, not a jar.

    Both files are installed side by side and differ in classpath separator and path shape,
    so the platform's own has to be picked deliberately - taking whichever os.walk happens to
    yield first gets the unix one on Windows, and the server dies on an unusable classpath.
    """
    wanted = "win_args.txt" if platform.system() == "Windows" else "unix_args.txt"
    for root, _dirs, files in os.walk(os.path.join(server_dir, "libraries")):
        if wanted in files:
            rel = os.path.relpath(os.path.join(root, wanted), server_dir).replace("\\", "/")
            return '"%s" -Xmx4G %s @%s nogui' % (java, BRAND_ARG, rel)
    raise SystemExit("no %s under %s/libraries - the server install looks incomplete"
                     % (wanted, server_dir))


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)

    m = sub.add_parser("mods", help="download required and companion mod jars")
    m.add_argument("--loader", required=True, choices=["fabric", "neoforge", "forge"])
    m.add_argument("--mc", required=True)
    m.add_argument("--deps-dir", required=True)
    m.add_argument("--extras-dir", required=True)
    m.add_argument("--jar", help="the built Signpost jar to read declared dependencies from")
    m.add_argument("--dry-run", action="store_true")
    m.set_defaults(func=cmd_mods)

    c = sub.add_parser("client", help="install a loader profile into .minecraft")
    c.add_argument("--loader", required=True, choices=["fabric", "neoforge", "forge"])
    c.add_argument("--mc", required=True)
    c.add_argument("--loader-version", required=True)
    c.add_argument("--mc-home", required=True)
    c.add_argument("--java", default="java")
    c.set_defaults(func=cmd_client)

    s = sub.add_parser("server", help="install a dedicated server and write start.cmd")
    s.add_argument("--loader", required=True, choices=["fabric", "neoforge", "forge"])
    s.add_argument("--mc", required=True)
    s.add_argument("--loader-version", required=True)
    s.add_argument("--server-dir", required=True)
    s.add_argument("--java", default="java")
    s.set_defaults(func=cmd_server)

    a = ap.parse_args()
    return a.func(a)


if __name__ == "__main__":
    sys.exit(main())
