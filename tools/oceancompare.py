#!/usr/bin/env python3
"""Compares the oceans of worlds generated from one seed: a vanilla 1.13.2 world (tools/vanilla113.sh) and a
Portium world (1.12 format with Forge's id registry), over the same block area.

    tools/oceancompare.py WORLD [WORLD...] --area X1 Z1 X2 Z2 [--map]

Make the worlds with tools/vanilla113.sh (force-load the area) and tools/portiumgen.sh, from the same seed.

For each world: biomes, water depth over the ocean floor, what the floor is made of, and the ocean plants and
blocks found (kelp, seagrass, sea pickles, coral, magma, ice...). --map prints a depth map (one character per
4x4 columns). Use an area of fully decorated chunks: in a force-loaded square, leave out the edge chunks."""
import gzip, math, os, struct, sys, zlib
from collections import Counter

SEA_LEVEL = 63


# ---- NBT ----

def read(b, i, t):
    if t == 1: return struct.unpack_from('>b', b, i)[0], i + 1
    if t == 2: return struct.unpack_from('>h', b, i)[0], i + 2
    if t == 3: return struct.unpack_from('>i', b, i)[0], i + 4
    if t == 4: return struct.unpack_from('>q', b, i)[0], i + 8
    if t == 5: return struct.unpack_from('>f', b, i)[0], i + 4
    if t == 6: return struct.unpack_from('>d', b, i)[0], i + 8
    if t == 7:
        n = struct.unpack_from('>i', b, i)[0]; return b[i + 4:i + 4 + n], i + 4 + n
    if t == 8:
        n = struct.unpack_from('>H', b, i)[0]; return b[i + 2:i + 2 + n].decode('utf-8', 'replace'), i + 2 + n
    if t == 9:
        et = b[i]; n = struct.unpack_from('>i', b, i + 1)[0]; i += 5; out = []
        for _ in range(n):
            v, i = read(b, i, et); out.append(v)
        return out, i
    if t == 10:
        out = {}
        while True:
            tt = b[i]; i += 1
            if tt == 0: return out, i
            n = struct.unpack_from('>H', b, i)[0]; name = b[i + 2:i + 2 + n].decode('utf-8', 'replace'); i += 2 + n
            v, i = read(b, i, tt); out[name] = v
    if t == 11:
        n = struct.unpack_from('>i', b, i)[0]; return list(struct.unpack_from('>%di' % n, b, i + 4)), i + 4 + 4 * n
    if t == 12:
        n = struct.unpack_from('>i', b, i)[0]; return list(struct.unpack_from('>%dq' % n, b, i + 4)), i + 4 + 8 * n
    raise ValueError('tag type %d' % t)


def root(data):
    n = struct.unpack_from('>H', data, 1)[0]
    return read(data, 3 + n, 10)[0]


def chunk(world, cx, cz):
    path = os.path.join(world, 'region', 'r.%d.%d.mca' % (cx >> 5, cz >> 5))
    if not os.path.exists(path): return None
    with open(path, 'rb') as f:
        header = f.read(4096)
        entry = struct.unpack_from('>I', header, 4 * ((cx & 31) + (cz & 31) * 32))[0]
        if entry == 0: return None
        f.seek((entry >> 8) * 4096)
        length, kind = struct.unpack('>IB', f.read(5))
        data = f.read(length - 1)
    return root(zlib.decompress(data) if kind == 2 else gzip.decompress(data))


# ---- Block lookups, for each format ----

VANILLA_BIOMES = {0: 'ocean', 1: 'plains', 2: 'desert', 3: 'mountains', 4: 'forest', 5: 'taiga', 6: 'swamp', 7: 'river',
                  10: 'frozen_ocean', 11: 'frozen_river', 12: 'snowy_tundra', 16: 'beach', 18: 'wooded_hills', 24: 'deep_ocean',
                  25: 'stone_shore', 26: 'snowy_beach', 27: 'birch_forest', 29: 'dark_forest', 30: 'snowy_taiga', 32: 'giant_tree_taiga',
                  35: 'savanna', 44: 'warm_ocean', 45: 'lukewarm_ocean', 46: 'cold_ocean', 47: 'deep_warm_ocean',
                  48: 'deep_lukewarm_ocean', 49: 'deep_cold_ocean', 50: 'deep_frozen_ocean'}


class Modern:
    """A 1.13 world: block palettes per section."""

    def __init__(self, world):
        self.world = world

    def column_source(self, cx, cz):
        c = chunk(self.world, cx, cz)
        if c is None: return None
        level = c['Level']
        sections = {}
        for s in level.get('Sections', []):
            palette = s.get('Palette'); states = s.get('BlockStates')
            if not palette or not states: continue
            bits = max(4, math.ceil(math.log2(len(palette))))
            mask = (1 << bits) - 1
            names = [p['Name'].replace('minecraft:', '') for p in palette]
            blocks = []
            for idx in range(4096):
                bit = idx * bits; word = bit >> 6; offset = bit & 63
                value = (states[word] & 0xFFFFFFFFFFFFFFFF) >> offset
                if offset + bits > 64: value |= (states[word + 1] & 0xFFFFFFFFFFFFFFFF) << (64 - offset)
                blocks.append(names[value & mask])
            sections[s['Y']] = blocks
        biomes = [VANILLA_BIOMES.get(b, str(b)) for b in level.get('Biomes', [])]
        return sections, biomes

    @staticmethod
    def name(sections, x, y, z):
        s = sections.get(y >> 4)
        return 'air' if s is None else s[((y & 15) << 8) | (z << 4) | x]


class Legacy:
    """A 1.12 world with Forge's block registry in level.dat."""

    def __init__(self, world):
        self.world = world
        with gzip.open(os.path.join(world, 'level.dat')) as f:
            level = root(f.read())
        registries = level['FML']['Registries']
        self.blocks = {e['V']: e['K'].replace('minecraft:', '') for e in registries['minecraft:blocks']['ids']}
        self.biomes = {e['V']: e['K'].replace('minecraft:', '') for e in registries['minecraft:biomes']['ids']}

    def column_source(self, cx, cz):
        c = chunk(self.world, cx, cz)
        if c is None: return None
        level = c['Level']
        sections = {}
        for s in level.get('Sections', []):
            blocks, data = s['Blocks'], s['Data']
            add = s.get('Add')
            names = []
            for idx in range(4096):
                bid = blocks[idx] & 255
                if add: bid |= ((add[idx >> 1] >> ((idx & 1) * 4)) & 15) << 8
                meta = (data[idx >> 1] >> ((idx & 1) * 4)) & 15
                names.append(self.legacy_name(self.blocks.get(bid, str(bid)), meta))
            sections[s['Y']] = names
        biomes = [self.biomes.get(b & 255, str(b & 255)) for b in level.get('Biomes', b'')]
        return sections, biomes

    @staticmethod
    def legacy_name(name, meta):
        # The 1.12 blocks the comparison cares about, by their 1.13 names.
        if name in ('water', 'flowing_water'): return 'water'
        if name == 'sand': return 'red_sand' if meta == 1 else 'sand'
        if name == 'stone': return ['stone', 'granite', 'polished_granite', 'diorite', 'polished_diorite', 'andesite', 'polished_andesite'][meta % 7]
        if name == 'dirt': return ['dirt', 'coarse_dirt', 'podzol'][meta % 3]
        if name == 'grass': return 'grass_block'
        if name == 'tallgrass': return 'grass' if meta == 1 else 'fern'
        if name == 'magma': return 'magma_block'
        if name == 'prismarine': return ['prismarine', 'prismarine_bricks', 'dark_prismarine'][meta % 3]
        if name == 'planks': return ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak'][meta % 6] + '_planks'
        if name == 'web': return 'cobweb'
        return name.replace('portium:', '')

    name = staticmethod(Modern.name)


# ---- Comparison ----

PLANTS = ('kelp', 'kelp_plant', 'seagrass', 'tall_seagrass', 'sea_pickle')
FLUIDS = ('water', 'bubble_column', 'ice') + PLANTS


def is_fluid(name):
    return name in FLUIDS or name.endswith('coral') or name.endswith('coral_fan') or name.endswith('coral_wall_fan')


def survey(world, area, show_map):
    fmt = Modern(world) if not os.path.exists(os.path.join(world, 'level.dat')) or 'FML' not in open_level(world) else Legacy(world)
    x1, z1, x2, z2 = area
    biomes, floors, depths, plants, special = Counter(), Counter(), [], Counter(), Counter()
    kelp_columns = 0
    depth_map = {}
    for cx in range(x1 >> 4, (x2 >> 4) + 1):
        for cz in range(z1 >> 4, (z2 >> 4) + 1):
            source = fmt.column_source(cx, cz)
            if source is None: continue
            sections, column_biomes = source
            for lx in range(16):
                for lz in range(16):
                    x, z = cx * 16 + lx, cz * 16 + lz
                    if not (x1 <= x <= x2 and z1 <= z <= z2): continue
                    if column_biomes: biomes[column_biomes[lz * 16 + lx]] += 1
                    top = fmt.name(sections, lx, SEA_LEVEL - 1, lz)
                    if top not in ('water', 'ice', 'kelp', 'kelp_plant', 'seagrass', 'tall_seagrass', 'bubble_column'):
                        continue
                    y = SEA_LEVEL - 1
                    has_kelp = False
                    while y > 0:
                        n = fmt.name(sections, lx, y, lz)
                        if n in ('kelp', 'kelp_plant'): has_kelp = True
                        if n in PLANTS: plants[n] += 1
                        elif n.endswith('coral') or n.endswith('coral_block') or 'coral_fan' in n: special['coral (any)'] += 1
                        elif n == 'bubble_column': special['bubble_column'] += 1
                        if not is_fluid(n): break
                        y -= 1
                    floor = fmt.name(sections, lx, y, lz)
                    floors[floor] += 1
                    if floor == 'magma_block': special['magma_block floor'] += 1
                    depth = SEA_LEVEL - 1 - y
                    depths.append(depth)
                    depth_map[(x, z)] = depth
                    if has_kelp: kelp_columns += 1
    total = len(depths)
    print('== %s (%s)' % (world, type(fmt).__name__))
    print('  biomes:', ', '.join('%s %d%%' % (b, 100 * n // max(1, sum(biomes.values()))) for b, n in biomes.most_common(8)))
    if not total:
        print('  no water columns'); return
    depths.sort()
    print('  water columns: %d; depth to floor: mean %.1f, median %d, 90th %d, max %d' % (
        total, sum(depths) / total, depths[total // 2], depths[total * 9 // 10], depths[-1]))
    print('  floor:', ', '.join('%s %.1f%%' % (b, 100.0 * n / total) for b, n in floors.most_common(8)))
    print('  plants: kelp in %.1f%% of columns;' % (100.0 * kelp_columns / total),
          ', '.join('%s %d' % (p, n) for p, n in plants.most_common()))
    if special: print('  other:', ', '.join('%s %d' % (p, n) for p, n in special.most_common()))
    if show_map:
        chars = ' .:-=+*#%@'
        for z in range(z1, z2 + 1, 4):
            row = ''
            for x in range(x1, x2 + 1, 4):
                cells = [depth_map[(x + i, z + j)] for i in range(4) for j in range(4) if (x + i, z + j) in depth_map]
                row += '^' if not cells else chars[min(9, (sum(cells) // len(cells)) // 4)]
            print('  |' + row)


def open_level(world):
    with gzip.open(os.path.join(world, 'level.dat')) as f:
        return root(f.read())


def main():
    args = sys.argv[1:]
    show_map = '--map' in args
    if show_map: args.remove('--map')
    i = args.index('--area')
    area = tuple(int(v) for v in args[i + 1:i + 5])
    for world in args[:i]:
        survey(world, area, show_map)


if __name__ == '__main__':
    main()
