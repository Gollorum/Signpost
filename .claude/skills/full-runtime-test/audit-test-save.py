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
# Where the waystone library lives on disk. 26.1 turned SavedDataType's id into an
# Identifier and gave every dimension its own folder, so the file moved from a flat
# <world>/data/signpost_WaystoneLibrary.dat to a namespaced path under the overworld's
# folder. A corpus can be written by either era, so both are accepted.
LEGACY_LIB_REL  = os.path.join('data', 'signpost_WaystoneLibrary.dat')
CURRENT_LIB_REL = os.path.join('dimensions', 'minecraft', 'overworld', 'data',
                               'signpost', 'waystone_library.dat')


def overworld_regions(world):
    """
    Region files of the overworld, in either layout.

    26.1 gave every dimension its own folder, so the overworld's chunks moved from
    <world>/region to <world>/dimensions/minecraft/overworld/region. Looking only in the old
    place makes a 26.1-written save report zero signpost block entities - which reads as
    "this corpus exercises nothing" when in fact the scan simply missed them.
    """
    for rel in (os.path.join('dimensions', 'minecraft', 'overworld', 'region'),
                'region'):
        found = glob.glob(os.path.join(world, rel, '*.mca'))
        if found:
            return found
    return []


def overworld_chunk_tickets(world):
    """
    The overworld's forced-chunk file, in either layout, or None.

    26.1 gave every dimension its own folder AND renamed the file, so
    <world>/data/chunks.dat became
    <world>/dimensions/minecraft/overworld/data/minecraft/chunk_tickets.dat.
    """
    for rel in (os.path.join('dimensions', 'minecraft', 'overworld', 'data', 'minecraft',
                             'chunk_tickets.dat'),
                os.path.join('data', 'chunks.dat')):
        path = os.path.join(world, rel)
        if os.path.exists(path):
            return path
    return None


def _unpack_chunk_pos(packed):
    """ChunkPos.toLong packs x into the low 32 bits and z into the high 32, both signed."""
    x = packed & 0xFFFFFFFF
    z = (packed >> 32) & 0xFFFFFFFF
    if x >= 0x80000000:
        x -= 0x100000000
    if z >= 0x80000000:
        z -= 0x100000000
    return (x, z)


def forced_chunks(world):
    """
    The (x, z) chunks this save force-loads, and the file they came from.

    Two eras, two encodings, and understanding only one of them yields an empty set that
    the audit then reports as "this save force-loads nothing" - the exact opposite of the
    truth, and worse than an error because it reads as a finding:

      * <= 1.21.x   data.Forced  - a list of packed ChunkPos longs
      * 26.1+       data.tickets - compounds of {chunk_pos: [x, z], level, type}

    Both are read, so a corpus written by either era audits correctly.
    """
    path = overworld_chunk_tickets(world)
    if path is None:
        return set(), None
    data = nbt_load(path).get('data') or {}
    out = set()
    for ticket in (data.get('tickets') or []):
        pos = ticket.get('chunk_pos') if isinstance(ticket, dict) else None
        if isinstance(pos, list) and len(pos) == 2:
            out.add((pos[0], pos[1]))
    for packed in (data.get('Forced') or []):
        if isinstance(packed, int):
            out.add(_unpack_chunk_pos(packed))
    return out, path


# A forced ticket is registered at level 31 and propagates outwards one level per chunk
# until it passes 33, the level at which a chunk stops being loaded - so each ticket brings
# in the 5x5 around it, not just its own chunk. Verified against the server's own
# "Loading N persistent chunks" line on two corpus entries: 6 tickets -> 91 chunks on
# 2.04.0/26.1.2, and 5 -> 89 on 2.04.0/1.21.1, both exact.
FORCED_TICKET_RADIUS = 2


def loaded_on_start(tickets, radius=FORCED_TICKET_RADIUS):
    """Every chunk a dedicated server pulls in at startup for these forced tickets."""
    out = set()
    for (x, z) in tickets:
        for dx in range(-radius, radius + 1):
            for dz in range(-radius, radius + 1):
                out.add((x + dx, z + dz))
    return out


def find_waystone_library(world):
    """The waystone library file in this world, whichever layout it uses, or None."""
    for rel in (CURRENT_LIB_REL, LEGACY_LIB_REL):
        p = os.path.join(world, rel)
        if os.path.exists(p):
            return p
    return None


def count_waystones(world):
    """Waystones in this world's library. -1 when there is no library at all."""
    lib = find_waystone_library(world)
    if lib is None:
        return -1
    try:
        nbt = nbt_load(lib)
        inner = nbt.get('data', nbt)
        ways = inner.get('Waystones')
        return len(ways) if isinstance(ways, list) else 0
    except Exception:
        return -1


def repo_root():
    return os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '..'))


def corpus_root():
    return os.path.join(repo_root(), 'testsaves')


def corpus_worlds(root=None):
    """Every save folder in the corpus, both layouts.

    An entry is testsaves/<signpost>/<minecraft>/world - one Signpost release can have a
    save per Minecraft version, because a port breaks serialization just as readily as a
    mod release does. The older flat testsaves/<signpost>/world is still recognised so a
    half-migrated corpus audits instead of silently reporting nothing.
    """
    root = root or corpus_root()
    found = glob.glob(os.path.join(root, '*', 'world'))
    found += glob.glob(os.path.join(root, '*', '*', 'world'))
    return sorted(set(os.path.normpath(f) for f in found))


def entry_id(entry_dir):
    """'2.04.0/1.21.11' for a corpus entry, else the path as given."""
    rel = os.path.relpath(entry_dir, corpus_root())
    return entry_dir if rel.startswith('..') else rel.replace(os.sep, '/')


def in_corpus(entry_dir):
    rel = os.path.relpath(entry_dir, corpus_root())
    if rel.startswith('..') or os.path.isabs(rel):
        return False
    return len(rel.split(os.sep)) in (1, 2)


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
    """Releases between two Minecraft versions, e.g. 1.21.5 vs 1.21.11 -> 6.

    Returns None when the distance cannot be established. Minecraft changed numbering
    schemes (1.21.11 was followed by 26.1), so versions from different schemes are not
    comparable by arithmetic: 1.21.11 -> 26.1.2 is a single release, 1.20.1 -> 26.1.2 is
    many, and nothing in the strings says which. Guessing "far apart" there produced a
    warning telling you to use the very save you were already using.
    """
    try:
        pa = [int(x) for x in a.split('.')]
        pb = [int(x) for x in b.split('.')]
    except Exception:
        return None
    if pa[:2] != pb[:2]:
        return None
    return abs((pa[2] if len(pa) > 2 else 0) - (pb[2] if len(pb) > 2 else 0))


def audit(world, props):
    r = Report()
    name = entry_id(os.path.dirname(world)) or os.path.relpath(world, repo_root())
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
        if sp_save != sp_now:
            r.i('upgrade tested : %s -> %s' % (sp_save, sp_now))
        elif mc_save and mc_now and mc_save != mc_now:
            # Same mod version is fine as long as Minecraft moved. A port carries just as
            # much serialization risk as a mod release - vanilla's own DataFixers rewrite
            # the chunk and level formats underneath us - so this still tests something
            # real, namely that the current build reads its own data across the port.
            r.i('upgrade tested : Minecraft only (Signpost stays %s)' % sp_save)
        else:
            r.f('save was written by Signpost %s on Minecraft %s, which is exactly what '
                'gradle.properties builds. Neither version moves across the load, so the '
                'run proves nothing. Use an older Signpost build, or a save from the '
                'Minecraft version you are porting from.' % (sp_save, mc_save or 'unknown'))

    if mc_save and mc_now and mc_save != mc_now:
        # A cross-version load is the point of the exercise, so this is not a warning by
        # itself. It only becomes one when the gap is wider than a single release, since
        # Signpost ships no DataFixers to carry its data across two ports.
        r.i('cross-version  : %s -> %s (the upgrade path under test)' % (mc_save, mc_now))
        gap = _mc_gap(mc_save, mc_now)
        if gap is None:
            r.i('release gap    : not computable across the %s / %s numbering schemes - '
                'check by hand that this is the release you mean to port from' % (mc_save, mc_now))
        elif gap > 1:
            r.w('the save is %d Minecraft releases behind %s. Signpost registers no '
                'DataFixers, so its data is not migrated across that gap and a failure '
                'here would say nothing about the current change. Use a save from the '
                'release immediately before this one.' % (gap, mc_now))

    # --- declared provenance ----------------------------------------------------------
    # level.dat records only a version *string*, and Signpost carried "2.03.0" for months
    # of development before the release. Two saves can both claim 2.03.0 and have
    # incompatible formats (UUIDUtil.CODEC landed in 80fb316, the day before the v2.03.0
    # tag). So authenticity cannot be inferred - it has to be recorded when the save is
    # made, from a published artifact.
    entry_dir = os.path.dirname(world)
    is_corpus = in_corpus(entry_dir)

    # In the nested layout the folder names assert what the save is; level.dat knows the
    # truth. Disagreement means the corpus is mislabelled, and a mislabelled entry is how
    # you end up believing you tested a port you never tested.
    rel = os.path.relpath(entry_dir, corpus_root()).split(os.sep)
    if is_corpus and len(rel) == 2:
        if mc_save and rel[1] != mc_save:
            r.w('folder says Minecraft %s but level.dat says %s - rename the entry to '
                'testsaves/%s/%s.' % (rel[1], mc_save, rel[0], mc_save))
        if sp_save and rel[0] != sp_save:
            r.w('folder says Signpost %s but level.dat says %s - rename the entry to '
                'testsaves/%s/%s.' % (rel[0], sp_save, sp_save, rel[1]))

    src = os.path.join(entry_dir, 'SOURCE.txt')
    if os.path.exists(src):
        decl = {}
        for line in open(src, encoding='utf-8'):
            m = re.match(r'\s*([A-Za-z_]+)\s*:\s*(.+?)\s*$', line)
            if m:
                decl[m.group(1).lower()] = m.group(2)
        need = [k for k in ('released_version', 'source_url') if not decl.get(k)]
        if need:
            r.w('SOURCE.txt is missing: %s' % ', '.join(need))
        else:
            r.i('declared source: %s from %s' % (decl['released_version'], decl['source_url']))
        if sp_save and decl.get('released_version') and decl['released_version'] != sp_save:
            r.f('SOURCE.txt declares %s but level.dat says the save was written by %s.'
                % (decl['released_version'], sp_save))
    elif is_corpus:
        r.f('no SOURCE.txt beside the save. A corpus entry must record which *published* '
            'artifact produced it - a version string in level.dat cannot distinguish a '
            'release from a dev build that carried the same version. See '
            'testsaves/README.md.')

    # --- mod content ------------------------------------------------------------------
    lib_waystones = 0
    lib = find_waystone_library(world)
    if lib is None:
        r.f('no Signpost waystone library in the save (looked for %s and %s) - it carries no '
            'Signpost SavedData, which is the single most valuable thing this test exercises.'
            % (LEGACY_LIB_REL, CURRENT_LIB_REL))
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
    ticket_file = None
    try:
        forced, ticket_file = forced_chunks(world)
    except Exception as e:
        r.w('the forced-chunk file could not be parsed: %s' % e)
    loaded = loaded_on_start(forced)
    if ticket_file is None:
        r.i('forced chunks  : 0 (no chunk-ticket file in this save)')
    else:
        r.i('forced chunks  : %d ticket(s) -> %d chunk(s) loaded at startup (from %s)'
            % (len(forced), len(loaded),
               os.path.relpath(ticket_file, world).replace(os.sep, '/')))

    # --- block entities ---------------------------------------------------------------
    regions = sorted(overworld_regions(world))
    posts = waystones = generators = 0
    in_forced = 0
    for f in regions:
        for ch in region_chunks(f):
            cx, cz = ch.get('xPos'), ch.get('zPos')
            hot = (cx, cz) in loaded
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
    r.i('loaded on start: %d of %d signpost block entities are in chunks the server '
        'loads' % (in_forced, total_be))

    if total_be > 0 and not forced:
        r.w('the save force-loads no chunks, so a dedicated-server run deserializes none of '
            'its %d signpost block entities - only the WaystoneLibrary SavedData is '
            'exercised. Use /forceload add over the area you want covered (a village, for '
            'instance) and re-save; the tickets persist in the chunk-ticket file and are '
            're-activated on every load.' % total_be)
    elif total_be > 0 and in_forced == 0:
        r.w('%d chunk(s) are loaded at startup but none contain signpost block entities, '
            'so none are deserialized on a server load.' % len(loaded))
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
    # --count-waystones <world> prints just the number, for the harness to assert on after a
    # run. A library that silently comes up empty is exactly the failure a clean startup hides.
    if len(argv) > 2 and argv[1] == '--count-waystones':
        print(count_waystones(os.path.abspath(argv[2])))
        return 0

    props = current_props()
    targets = []
    if len(argv) > 1:
        for a in argv[1:]:
            a = os.path.abspath(a)
            if os.path.isdir(os.path.join(a, 'world')):
                targets.append(os.path.join(a, 'world'))
            else:
                # A Signpost-version directory holding one entry per Minecraft version:
                # audit all of them. This is what the harness passes for -SaveVersion 2.04.0.
                nested = corpus_worlds(a)
                targets.extend(nested if nested else [a])
    else:
        targets = corpus_worlds()
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
