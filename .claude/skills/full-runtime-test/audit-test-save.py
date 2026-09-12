#!/usr/bin/env python3
"""
Audit a runtime-test save before trusting it as a corpus entry.

Answers the questions you cannot answer by looking at the folder:

  * which Signpost / loader / Minecraft version actually wrote it
  * whether it still holds the mod data the test is supposed to exercise
  * whether it is small enough to keep around

Usage:
    python audit-test-save.py                     # audit every testsaves/<version>/world
    python audit-test-save.py testsaves/2.03.0    # audit one entry
    python audit-test-save.py path/to/world       # audit an arbitrary save folder

Exit code 0 = usable, 1 = at least one FAIL. Warnings alone do not fail.
No third-party packages; only the standard library.
"""
import glob
import gzip
import os
import re
import struct
import sys
import zlib

# --------------------------------------------------------------------------------------
# Minimal NBT reader
# --------------------------------------------------------------------------------------
T_END, T_BYTE, T_SHORT, T_INT, T_LONG, T_FLOAT, T_DOUBLE, T_BA, T_STR, T_LIST, T_COMP, T_IA, T_LA = range(13)


class _R:
    def __init__(self, b):
        self.b = b
        self.i = 0

    def u(self, fmt, n):
        v = struct.unpack_from(fmt, self.b, self.i)[0]
        self.i += n
        return v

    def s(self):
        n = self.u('>H', 2)
        v = self.b[self.i:self.i + n]
        self.i += n
        return v.decode('utf-8', 'replace')


def _payload(r, t):
    if t == T_BYTE:   return r.u('>b', 1)
    if t == T_SHORT:  return r.u('>h', 2)
    if t == T_INT:    return r.u('>i', 4)
    if t == T_LONG:   return r.u('>q', 8)
    if t == T_FLOAT:  return r.u('>f', 4)
    if t == T_DOUBLE: return r.u('>d', 8)
    if t == T_BA:
        n = r.u('>i', 4); r.i += n; return '<bytes %d>' % n
    if t == T_STR:    return r.s()
    if t == T_LIST:
        it = r.u('>b', 1); n = r.u('>i', 4)
        return [_payload(r, it) for _ in range(n)]
    if t == T_COMP:
        d = {}
        while True:
            tt = r.u('>b', 1)
            if tt == T_END:
                return d
            nm = r.s()
            d[nm] = _payload(r, tt)
    if t == T_IA:
        n = r.u('>i', 4); return [r.u('>i', 4) for _ in range(n)]
    if t == T_LA:
        n = r.u('>i', 4); return [r.u('>q', 8) for _ in range(n)]
    raise ValueError('unknown tag %d' % t)


def nbt_load(path):
    raw = open(path, 'rb').read()
    if raw[:2] == b'\x1f\x8b':
        raw = gzip.decompress(raw)
    r = _R(raw)
    t = r.u('>b', 1)
    if t == T_END:
        return {}
    r.s()
    return _payload(r, t)


def region_chunks(path):
    b = open(path, 'rb').read()
    if len(b) < 8192:
        return
    for i in range(1024):
        off = struct.unpack_from('>I', b, i * 4)[0] >> 8
        cnt = b[i * 4 + 3]
        if off == 0 or cnt == 0:
            continue
        p = off * 4096
        if p + 5 > len(b):
            continue
        ln = struct.unpack_from('>I', b, p)[0]
        comp = b[p + 4]
        raw = b[p + 5:p + 4 + ln]
        try:
            if comp == 1:
                raw = gzip.decompress(raw)
            elif comp == 2:
                raw = zlib.decompress(raw)
            elif comp != 3:
                continue
            r = _R(raw)
            t = r.u('>b', 1)
            if t == T_END:
                continue
            r.s()
            yield _payload(r, t)
        except Exception:
            continue


# --------------------------------------------------------------------------------------
def repo_root():
    return os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '..'))


def current_props():
    p = os.path.join(repo_root(), 'gradle.properties')
    out = {}
    if os.path.exists(p):
        for line in open(p, encoding='utf-8'):
            m = re.match(r'\s*([A-Za-z_.]+)\s*=\s*(.+?)\s*$', line)
            if m:
                out[m.group(1)] = m.group(2)
    return out


def dir_size(path):
    total = 0
    for root, _, files in os.walk(path):
        for f in files:
            try:
                total += os.path.getsize(os.path.join(root, f))
            except OSError:
                pass
    return total


class Report:
    def __init__(self):
        self.fail = []
        self.warn = []
        self.info = []

    def f(self, m): self.fail.append(m)
    def w(self, m): self.warn.append(m)
    def i(self, m): self.info.append(m)


def _mc_gap(a, b):
    """Rough distance between two Minecraft versions, e.g. 1.21.5 vs 1.21.11 -> 6."""
    try:
        pa = [int(x) for x in a.split('.')]
        pb = [int(x) for x in b.split('.')]
        if pa[:2] != pb[:2]:
            return 99
        return abs((pa[2] if len(pa) > 2 else 0) - (pb[2] if len(pb) > 2 else 0))
    except Exception:
        return 99


def audit(world, props):
    r = Report()
    name = os.path.relpath(world, repo_root())
    print('=' * 78)
    print('AUDIT  %s' % name)
    print('=' * 78)

    level = os.path.join(world, 'level.dat')
    if not os.path.exists(level):
        r.f('no level.dat - this is not a save folder')
        return r

    try:
        d = nbt_load(level)
    except Exception as e:
        r.f('level.dat could not be parsed: %s' % e)
        return r

    data = d.get('Data', {})
    mc_save = (data.get('Version') or {}).get('Name')
    brands = data.get('ServerBrands') or []
    r.i('minecraft      : %s (DataVersion %s)' % (mc_save, data.get('DataVersion')))
    r.i('written by     : %s' % (', '.join(brands) if brands else 'unknown'))

    # --- provenance -------------------------------------------------------------------
    modlist = (d.get('fml') or {}).get('LoadingModList')
    mods = {m.get('ModId'): m.get('ModVersion') for m in modlist} if modlist else None
    sp_save = None
    if mods:
        sp_save = mods.get('signpost')
        r.i('signpost       : %s' % sp_save)
        others = {k: v for k, v in mods.items() if k not in ('minecraft', 'signpost', 'neoforge', 'forge')}
        if others:
            r.i('other mods     : %s' % ', '.join('%s %s' % kv for kv in sorted(others.items())))
    else:
        r.w('level.dat has no fml mod list, so the writing Signpost version cannot be '
            'verified. Fabric does not record one - create corpus saves on NeoForge or '
            'Forge instead, where the version is provable.')

    sp_now = props.get('version')
    mc_now = props.get('minecraft_version')

    if sp_save and sp_now:
        if sp_save == sp_now and (not mc_save or not mc_now or mc_save == mc_now):
            r.f('save was written by Signpost %s, which is the version in gradle.properties. '
                'A corpus entry must come from an OLDER released build, otherwise it tests '
                'nothing about compatibility.' % sp_save)
        elif sp_save == sp_now:
            # Same mod version, different Minecraft version: one release ported across a
            # Minecraft upgrade. Loading a save written by 2.04.0 on 1.21.1 with 2.04.0 on
            # 1.21.11 tests exactly the thing a backport has to get right - that both trees
            # describe the same bytes - so this is a meaningful corpus entry even though the
            # version string did not move.
            r.i('port tested    : Signpost %s, %s -> %s' % (sp_save, mc_save, mc_now))
        else:
            r.i('upgrade tested : %s -> %s' % (sp_save, sp_now))

    if mc_save and mc_now and mc_save != mc_now:
        # A cross-version load is the point of the exercise, so this is not a warning by
        # itself. It only becomes one when the gap is wider than a single release, since
        # Signpost ships no DataFixers to carry its data across two ports.
        r.i('cross-version  : %s -> %s (the upgrade path under test)' % (mc_save, mc_now))
        if _mc_gap(mc_save, mc_now) > 1:
            r.w('the save is more than one Minecraft release behind %s. Signpost registers '
                'no DataFixers, so its data is not migrated across that gap and a failure '
                'here would say nothing about the current change. Use a save from the '
                'release immediately before this one.' % mc_now)

    # --- declared provenance ----------------------------------------------------------
    # level.dat records only a version *string*, and Signpost carried "2.03.0" for months
    # of development before the release. Two saves can both claim 2.03.0 and have
    # incompatible formats (UUIDUtil.CODEC landed in 80fb316, the day before the v2.03.0
    # tag). So authenticity cannot be inferred - it has to be recorded when the save is
    # made, from a published artifact.
    entry_dir = os.path.dirname(world)
    in_corpus = os.path.basename(os.path.dirname(entry_dir)) == 'testsaves'
    src = os.path.join(entry_dir, 'SOURCE.txt')
    if os.path.exists(src):
        decl = {}
        for line in open(src, encoding='utf-8'):
            m = re.match(r'\s*([A-Za-z_]+)\s*:\s*(.+?)\s*$', line)
            if m:
                decl[m.group(1).lower()] = m.group(2)
        rv = (decl.get('released_version') or '').strip()
        # A backport is the one case where the save legitimately comes from an UNRELEASED
        # build: the branch under test is what has to prove it writes data the already-shipped
        # newer versions can read, and there is no published artifact to make the save from.
        # Such an entry declares `released_version: none` plus the commit it was built at, so
        # the thing that cannot be recovered later - which code wrote these bytes - is on
        # record. Everything else still has to name a published artifact.
        if rv.lower() in ('none', 'unreleased', 'wip', ''):
            commit = decl.get('source_commit')
            if not commit:
                r.f('SOURCE.txt declares no released_version, so it must record '
                    'source_commit: <sha> instead - which build wrote the save is otherwise '
                    'unrecoverable. See testsaves/README.md.')
            else:
                r.i('unreleased     : built at commit %s' % commit)
                r.w('this entry was written by an UNRELEASED build, so it is evidence about '
                    'that branch and nothing else. If a newer Minecraft version fails to load '
                    'it, the branch that wrote it is what gets fixed and the save recreated - '
                    'never the newer version, which has already shipped.')
        else:
            if not decl.get('source_url'):
                r.w('SOURCE.txt is missing: source_url')
            else:
                r.i('declared source: %s from %s' % (rv, decl['source_url']))
            if sp_save and rv != sp_save:
                r.f('SOURCE.txt declares %s but level.dat says the save was written by %s.'
                    % (rv, sp_save))
    elif in_corpus:
        r.f('no SOURCE.txt beside the save. A corpus entry must record which *published* '
            'artifact produced it - a version string in level.dat cannot distinguish a '
            'release from a dev build that carried the same version. See '
            'testsaves/README.md.')

    # --- mod content ------------------------------------------------------------------
    lib_waystones = 0
    lib = os.path.join(world, 'data', 'signpost_WaystoneLibrary.dat')
    if not os.path.exists(lib):
        r.f('data/signpost_WaystoneLibrary.dat is missing - the save carries no Signpost '
            'SavedData, which is the single most valuable thing this test exercises.')
    else:
        try:
            lib_nbt = nbt_load(lib)
            inner = lib_nbt.get('data', lib_nbt)
            ways = inner.get('Waystones')
            n = len(ways) if isinstance(ways, list) else 0
            lib_waystones = n
            if n == 0:
                r.w('WaystoneLibrary parses but holds no waystones; it will not catch much.')
            else:
                r.i('waystones      : %d in WaystoneLibrary' % n)
            # Deliberately no schema heuristics here. Whether current code can read an
            # old library is decided by the codecs, not by which field names are present,
            # and guessing produces false positives on perfectly good saves. The
            # definitive check is one harness run - see README.
            village = inner.get('villageWaystones')
            nv = len(village) if isinstance(village, list) else 0
            r.i('village gen    : %d auto-generated village waystone(s) recorded' % nv)
            if nv == 0:
                r.w('no village waystones recorded, so the worldgen path (signposts and '
                    'waystones placed into vanilla villages) is not covered by this save.')
            r.i('library fields : %s' % ', '.join(sorted(inner.keys())))
        except Exception as e:
            r.f('WaystoneLibrary.dat could not be parsed: %s' % e)

    # --- forced chunks ----------------------------------------------------------------
    # Since 1.21.11 a dedicated server does NOT load a radius around spawn on startup;
    # MinecraftServer.prepareLevels() only re-activates persistent tickets. So the only
    # chunks whose block entities get deserialized on a server load test are the ones
    # /forceload has marked, stored in data/chunks.dat.
    forced = set()
    cd = os.path.join(world, 'data', 'chunks.dat')
    if os.path.exists(cd):
        try:
            data = nbt_load(cd).get('data') or {}
            # Two shapes on disk. 1.21.10+ writes a `tickets` list of compounds carrying
            # chunk_pos. Older versions - 1.21.1 among them - write `Forced`, a long array of
            # ChunkPos.asLong() values: x in the low 32 bits, z in the high 32, both signed.
            # Reading only the newer shape reports "forced chunks: 0" for a save that force-loads
            # plenty, which understates the coverage of every server run against it.
            for t in (data.get('tickets') or []):
                cp = t.get('chunk_pos') if isinstance(t, dict) else None
                if isinstance(cp, list) and len(cp) == 2:
                    forced.add((cp[0], cp[1]))
            for packed in (data.get('Forced') or []):
                x = packed & 0xFFFFFFFF
                z = (packed >> 32) & 0xFFFFFFFF
                if x >= 0x80000000: x -= 0x100000000
                if z >= 0x80000000: z -= 0x100000000
                forced.add((x, z))
        except Exception as e:
            r.w('data/chunks.dat could not be parsed: %s' % e)
    r.i('forced chunks  : %d' % len(forced))

    # --- block entities ---------------------------------------------------------------
    regions = sorted(glob.glob(os.path.join(world, 'region', '*.mca')))
    posts = waystones = generators = 0
    in_forced = 0
    for f in regions:
        for ch in region_chunks(f):
            cx, cz = ch.get('xPos'), ch.get('zPos')
            hot = (cx, cz) in forced
            for be in (ch.get('block_entities') or []):
                bid = str(be.get('id', ''))
                if not bid.startswith('signpost:'):
                    continue
                if bid == 'signpost:post':
                    posts += 1
                elif bid == 'signpost:waystone':
                    waystones += 1
                else:
                    generators += 1
                if hot:
                    in_forced += 1
    total_be = posts + waystones + generators
    r.i('block entities : %d post, %d waystone, %d other signpost' % (posts, waystones, generators))
    r.i('loaded on start: %d of %d signpost block entities are in forced chunks' % (in_forced, total_be))

    if total_be > 0 and not forced:
        r.w('the save force-loads no chunks, so a dedicated-server run deserializes none of '
            'its %d signpost block entities - only the WaystoneLibrary SavedData is '
            'exercised. Use /forceload add over the area you want covered (a village, for '
            'instance) and re-save; the tickets persist in data/chunks.dat and are '
            're-activated on every load.' % total_be)
    elif total_be > 0 and in_forced == 0:
        r.w('%d chunk(s) are force-loaded but none of them contain signpost block entities, '
            'so none are deserialized on a server load.' % len(forced))
    if posts == 0:
        r.w('no signpost:post block entities in any region - block-entity NBT will not be '
            'exercised on chunk load.')
    if posts == 0 and waystones == 0 and generators == 0 and lib_waystones == 0:
        r.f('the save contains no Signpost content at all: no block entities and an empty '
            'WaystoneLibrary. Loading it proves nothing.')

    # --- size --------------------------------------------------------------------------
    size = dir_size(world)
    r.i('size           : %.1f MB across %d region file(s)' % (size / 1048576.0, len(regions)))
    if size > 60 * 1048576:
        r.w('save is large. Region files dominate; a world that only ever loaded the spawn '
            'area is a few MB and copies far faster (it is copied once per run, 12 times).')

    return r


def main(argv):
    props = current_props()
    targets = []
    if len(argv) > 1:
        for a in argv[1:]:
            a = os.path.abspath(a)
            targets.append(os.path.join(a, 'world') if os.path.isdir(os.path.join(a, 'world')) else a)
    else:
        root = os.path.join(repo_root(), 'testsaves')
        targets = sorted(glob.glob(os.path.join(root, '*', 'world')))
        if not targets:
            print('No corpus entries found under testsaves/. See testsaves/README.md.')
            return 1

    worst = 0
    for t in targets:
        rep = audit(t, props)
        for m in rep.info:
            print('  %s' % m)
        for m in rep.warn:
            print('  WARN  %s' % m)
        for m in rep.fail:
            print('  FAIL  %s' % m)
        print('  ->', 'UNUSABLE' if rep.fail else ('USABLE (with warnings)' if rep.warn else 'USABLE'))
        print()
        worst = max(worst, 1 if rep.fail else 0)
    return worst


if __name__ == '__main__':
    sys.exit(main(sys.argv))
