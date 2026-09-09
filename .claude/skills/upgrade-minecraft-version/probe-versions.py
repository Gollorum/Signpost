#!/usr/bin/env python3
"""
Probe every upstream component for a target Minecraft version.

This is the mechanical half of a Minecraft upgrade: before touching anything, find out
what actually exists for the target version, what the current values are, and where the
upgrade is blocked. It changes nothing on disk.

    python probe-versions.py 26.2
    python probe-versions.py 26.2 --json

Output is a table of  component | current | available-for-target | note, followed by the
blockers it found. Exit code 0 = everything needed exists, 1 = at least one blocker.

Stdlib only. Network access required.
"""
import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request

TIMEOUT = 45
UA = {"User-Agent": "signpost-upgrade-probe"}


def repo_root():
    return os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '..'))


def props():
    p = os.path.join(repo_root(), 'gradle.properties')
    out = {}
    if os.path.exists(p):
        for line in open(p, encoding='utf-8'):
            m = re.match(r'\s*([A-Za-z_.]+)\s*=\s*(.+?)\s*$', line)
            if m:
                out[m.group(1)] = m.group(2)
    return out


def fetch(url):
    try:
        return urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=TIMEOUT).read()
    except Exception:
        return None


def maven_versions(url):
    """All <version> entries from a maven-metadata.xml, oldest first."""
    raw = fetch(url)
    if not raw:
        return []
    return re.findall(r'<version>([^<]+)</version>', raw.decode('utf-8', 'replace'))


def newest(cands):
    return cands[-1] if cands else None


def modrinth(project, mc, loader=None):
    """Newest release-channel version of a Modrinth project for this MC version."""
    url = ('https://api.modrinth.com/v2/project/%s/version?game_versions=%%5B%%22%s%%22%%5D' % (project, mc))
    if loader:
        url += '&loaders=%%5B%%22%s%%22%%5D' % loader
    raw = fetch(url)
    if not raw:
        return None, None
    try:
        data = json.loads(raw)
    except ValueError:
        return None, None
    if not data:
        return None, None
    rel = [v for v in data if v.get('version_type') == 'release'] or data
    v = rel[0]
    f = ([x for x in v['files'] if x.get('primary')] or v['files'])[0]
    return v['version_number'], f['filename']


def gradle_requirement_of_plugin(module_url):
    """Read org.gradle.plugin.api-version out of a Gradle Module Metadata file."""
    raw = fetch(module_url)
    if not raw:
        return None
    try:
        d = json.loads(raw)
    except ValueError:
        return None
    for var in d.get('variants', []):
        v = (var.get('attributes') or {}).get('org.gradle.plugin.api-version')
        if v:
            return v
    return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('target', help='target Minecraft version, e.g. 26.2 or 1.21.11')
    ap.add_argument('--json', action='store_true', help='machine-readable output')
    args = ap.parse_args()
    mc = args.target
    cur = props()

    rows = []      # (component, current, found, note)
    blockers = []
    notes = []

    def row(name, current, found, note=''):
        rows.append((name, current or '-', found or 'NOT FOUND', note))

    # ---- Minecraft-side toolchain ---------------------------------------------------
    neoform = [v for v in maven_versions('https://maven.neoforged.net/releases/net/neoforged/neoform/maven-metadata.xml')
               if v.startswith(mc + '-')]
    nf = newest(neoform)
    row('neo_form_version', cur.get('neo_form_version'), nf)
    if not nf:
        blockers.append('No NeoForm for %s - common/ cannot be compiled against vanilla yet.' % mc)

    neoforge = [v for v in maven_versions('https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml')
                if v.startswith(mc.replace('1.', '', 1) if mc.startswith('1.21') else mc)]
    # NeoForge numbering tracks MC loosely; also try a direct prefix match.
    if not neoforge:
        neoforge = [v for v in maven_versions('https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml')
                    if v.startswith(mc)]
    stable_nf = [v for v in neoforge if 'beta' not in v and 'alpha' not in v]
    nfg = newest(stable_nf) or newest(neoforge)
    row('neoforge_version', cur.get('neoforge_version'), nfg,
        'beta only' if nfg and 'beta' in nfg else '')
    if not nfg:
        blockers.append('No NeoForge build found for %s.' % mc)

    forge = [v for v in maven_versions('https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml')
             if v.startswith(mc + '-')]
    fg = newest(forge)
    row('forge_version', cur.get('forge_version'), fg.split('-', 1)[1] if fg else None,
        'full coord %s' % fg if fg else 'the forge subproject cannot be updated')
    if not fg:
        blockers.append('No Forge build for %s. Either wait, or drop/park the forge subproject.')

    parch_all = maven_versions('https://maven.parchmentmc.org/org/parchmentmc/data/parchment-%s/maven-metadata.xml' % mc)
    parch_rel = [v for v in parch_all if 'SNAPSHOT' not in v and 'nightly' not in v]
    parch_note = 'parchment often lags; falling back to the previous MC version is normal'
    if not parch_rel and parch_all:
        parch_note = 'ONLY nightly/SNAPSHOT builds exist - do not pin one; keep the previous ' \
                     'MC version in parchment_minecraft instead'
        notes.append('Parchment has no stable release for %s yet (newest is %s). Leave '
                     'parchment_minecraft on the previous version rather than pinning a '
                     'snapshot.' % (mc, newest(parch_all)))
    row('parchment (%s)' % mc, '%s / %s' % (cur.get('parchment_minecraft'), cur.get('parchment_version')),
        newest(parch_rel) or newest(parch_all), parch_note)

    # ---- Fabric ---------------------------------------------------------------------
    floader = [v for v in maven_versions('https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml')
               if re.match(r'^\d+\.\d+\.\d+$', v)]
    row('fabric_loader_version', cur.get('fabric_loader_version'), newest(floader))

    fapi = [v for v in maven_versions('https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml')
            if v.endswith('+' + mc)]
    fa = newest(fapi)
    row('fabric_version', cur.get('fabric_version'), fa)
    if not fa:
        blockers.append('No Fabric API for %s.' % mc)

    # ---- Gradle plugins, and the Gradle version they demand -------------------------
    loom = [v for v in maven_versions('https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml')
            if re.match(r'^\d+\.\d+\.\d+$', v)]
    lm = newest(loom)
    loom_gradle = gradle_requirement_of_plugin(
        'https://maven.fabricmc.net/net/fabricmc/fabric-loom/%s/fabric-loom-%s.module' % (lm, lm)) if lm else None
    row('fabric-loom (root build.gradle)', 'see build.gradle', lm,
        'needs Gradle %s' % loom_gradle if loom_gradle else '')

    mdg = [v for v in maven_versions('https://maven.neoforged.net/releases/net/neoforged/moddev-gradle/maven-metadata.xml')
           if re.match(r'^\d+\.\d+\.\d+$', v)]
    row('net.neoforged.moddev', 'see build.gradle', newest(mdg))

    # ForgeGradle 7 is published under a DIFFERENT artifact than 6 - lowercase 'forgegradle',
    # not 'ForgeGradle' - behind the same plugin id. Looking only at the old coordinate reports
    # "6.0.54" forever and makes Gradle 9 look impossible for the forge subproject.
    fg6 = [v for v in maven_versions('https://maven.minecraftforge.net/net/minecraftforge/gradle/ForgeGradle/maven-metadata.xml')
           if re.match(r'^\d+\.\d+\.\d+$', v)]
    fg7 = [v for v in maven_versions('https://maven.minecraftforge.net/net/minecraftforge/forgegradle/maven-metadata.xml')
           if re.match(r'^\d+\.\d+\.\d+$', v)]
    fgl_newest = newest(fg7) or newest(fg6)
    row('ForgeGradle', 'see forge/build.gradle', fgl_newest,
        'artifact net.minecraftforge:forgegradle (7.x)' if newest(fg7)
        else 'artifact net.minecraftforge:ForgeGradle (6.x)')

    # ---- Mod integrations and dev-env mods ------------------------------------------
    for label, proj, loader, prop in [
        ('waystones (fabric)',   'waystones', 'fabric',   'waystones_version'),
        ('waystones (neoforge)', 'waystones', 'neoforge', 'waystones_version'),
        ('waystones (forge)',    'waystones', 'forge',    'waystones_version'),
        ('cloth-config',         '9s6osm5g',  'fabric',   'cloth_config_version'),
        ('modmenu',              'modmenu',   'fabric',   'modmenu_version'),
        ('RS (fabric)',          'muf0XoRe',  'fabric',   'repurposed_structures_fabric_version'),
        ('RS (neoforge)',        'QDNS5oAT',  'neoforge', 'repurposed_structures_neoforge_version'),
        ('balm (run folders)',   'balm',      'fabric',   None),
        ('midnightlib (run)',    'midnightlib', 'fabric', None),
        ('sodium (run)',         'sodium',    'fabric',   None),
        ('iris (run)',           'iris',      'fabric',   None),
    ]:
        ver, fname = modrinth(proj, mc, loader)
        row(label, cur.get(prop) if prop else '(run folder)', ver)
        if not ver:
            notes.append('%s has no %s build yet - integration/dev-env coverage for it will be '
                         'missing until it ships.' % (label, mc))

    # ---- Gradle: Loom's floor, and whether ForgeGradle can meet it ------------------
    # ForgeGradle 6 caps out at Gradle 8 and refuses to apply on 9; ForgeGradle 7 is the
    # Gradle 9 line. So a Loom bump only threatens the forge subproject when no FG7 exists.
    wrapper = os.path.join(repo_root(), "gradle", "wrapper", "gradle-wrapper.properties")
    cur_gradle = None
    if os.path.exists(wrapper):
        m = re.search(r"gradle-([0-9.]+)-(?:bin|all)[.]zip", open(wrapper, encoding="utf-8").read())
        cur_gradle = m.group(1) if m else None
    has_forge = os.path.isdir(os.path.join(repo_root(), "forge"))
    row("gradle (wrapper)", cur_gradle,
        ("needs >= %s for Loom %s" % (loom_gradle, lm)) if loom_gradle else "?")

    if loom_gradle and cur_gradle:
        try:
            need_major = int(str(loom_gradle).split(".")[0])
            have_major = int(cur_gradle.split(".")[0])
        except ValueError:
            need_major = have_major = 0
        fg_major = int(fgl_newest.split('.')[0]) if fgl_newest else 0
        if need_major > have_major and has_forge and fg_major < 7:
            blockers.append(
                "Loom %s requires Gradle %s but the wrapper is on %s, and only ForgeGradle %s "
                "exists - FG6 refuses to apply on Gradle %d. You cannot bump Loom and keep "
                "forge in the same build. Decide before starting: pin the newest Loom that "
                "still supports Gradle %d, or drop/park the forge subproject and move the "
                "whole build to Gradle %d."
                % (lm, loom_gradle, cur_gradle, fgl_newest, need_major, have_major, need_major))
        elif need_major > have_major and has_forge:
            notes.append("Gradle must go to %s for Loom %s; ForgeGradle %s supports Gradle 9, so "
                         "the forge subproject comes along - bump its plugin to the 7.x range "
                         "(the artifact is net.minecraftforge:forgegradle, lowercase)."
                         % (loom_gradle, lm, fgl_newest))
        elif need_major > have_major:
            notes.append("Loom %s needs Gradle %s; bump the wrapper (no forge subproject is "
                         "present to block it)." % (lm, loom_gradle))

    # ---- Sodium/Iris mutual compatibility -------------------------------------------
    # Sodium declares which Iris versions it *breaks*; taking the newest of each is a
    # known way to produce a pair Fabric refuses to launch.
    notes.append('Check sodium\'s fabric.mod.json "breaks" -> iris before pairing them; the '
                 'newest of each is often mutually incompatible.')

    if args.json:
        print(json.dumps({'target': mc,
                          'rows': [dict(zip(('component', 'current', 'available', 'note'), r)) for r in rows],
                          'blockers': blockers, 'notes': notes}, indent=2))
        return 1 if blockers else 0

    w = max(len(r[0]) for r in rows) + 2
    print('=' * 100)
    print('UPGRADE PROBE  ->  Minecraft %s      (current project: %s)' % (mc, cur.get('minecraft_version')))
    print('=' * 100)
    print('%-*s %-26s %-28s %s' % (w, 'component', 'current', 'available for target', 'note'))
    print('-' * 100)
    for name, current, found, note in rows:
        print('%-*s %-26s %-28s %s' % (w, name, current[:26], found[:28], note))

    if notes:
        print('\nNOTES')
        for n in notes:
            print('  - %s' % n)
    if blockers:
        print('\nBLOCKERS')
        for b in blockers:
            print('  ! %s' % b)
        print('\n-> %d blocker(s). Resolve or consciously accept these before starting.' % len(blockers))
        return 1
    if notes:
        print()
        print('-> No hard blockers for %s, but %d note(s) above need a decision.' % (mc, len(notes)))
    else:
        print()
        print('-> Everything required exists for %s.' % mc)
    return 0


if __name__ == '__main__':
    sys.exit(main())
