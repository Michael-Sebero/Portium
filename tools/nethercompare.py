#!/usr/bin/env python3
"""Compares the Nether of a vanilla 1.16.5 world (tools/vanilla116.sh) with Portium's (1.12 format), same seed, over a
chunk area:

    tools/nethercompare.py VANILLA_WORLD PORTIUM_WORLD SEED X1,Z1,X2,Z2 [--terrain]

Prints how many column biomes agree (1.16's biome per block, taken at y 128 as Portium's columns are) and the most
common block differences. Portium decorates 8 blocks along from where 1.16 does, so feature blocks differ in pairs
both ways. --terrain checks the terrain and surfaces alone: where vanilla has a block no feature makes (netherrack,
bedrock, soul soil, nylium), Portium must have it too, unless Portium's own features changed it (so only another such
block, or air, counts as a difference). Structures still shape vanilla's terrain (fortresses, and the ground around
nether fossils). Use chunks the force-loaded area fully generated: leave out its edge."""
import hashlib, math, os, struct, sys
from collections import Counter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import oceancompare as oc

RENAME = {'cave_air': 'air', 'void_air': 'air', 'nether_quartz_ore': 'quartz_ore', 'magma_block': 'magma', 'nether_bricks': 'nether_brick',
          'spawner': 'mob_spawner', 'hell': 'nether_wastes'}
VANILLA_BIOMES = {8: 'nether_wastes', 170: 'soul_sand_valley', 171: 'crimson_forest', 172: 'warped_forest', 173: 'basalt_deltas'}
NEVER_FROM_FEATURES = {'netherrack', 'bedrock', 'soul_soil', 'crimson_nylium', 'warped_nylium'}


def norm(name):
    name = name.split(':')[-1]
    return RENAME.get(name, name)


class Vanilla:
    """A 1.16 world: palettes whose entries don't span longs."""

    def __init__(self, world):
        self.world = world
        self.cache = {}

    def chunk(self, cx, cz):
        if (cx, cz) not in self.cache:
            level = oc.chunk(os.path.join(self.world, 'DIM-1'), cx, cz)['Level']
            sections = {}
            for s in level.get('Sections', []):
                palette, states = s.get('Palette'), s.get('BlockStates')
                if not palette or not states: continue
                bits = max(4, math.ceil(math.log2(len(palette))))
                per, mask = 64 // bits, (1 << bits) - 1
                names = [norm(p['Name']) for p in palette]
                sections[s['Y']] = [names[((states[i // per] & 0xFFFFFFFFFFFFFFFF) >> ((i % per) * bits)) & mask] for i in range(4096)]
            self.cache[(cx, cz)] = (sections, level.get('Biomes', []))
        return self.cache[(cx, cz)]

    def block(self, x, y, z):
        return section_block(self.chunk(x >> 4, z >> 4)[0], x & 15, y, z & 15)

    def cell_biome(self, qx, qz):
        biomes = self.chunk(qx >> 2, qz >> 2)[1]
        return VANILLA_BIOMES.get(biomes[(32 << 4) | ((qz & 3) << 2) | (qx & 3)], '?')


class Portium:
    def __init__(self, world):
        self.world = world
        self.legacy = oc.Legacy(world)
        self.cache = {}

    def chunk(self, cx, cz):
        if (cx, cz) not in self.cache:
            level = oc.chunk(os.path.join(self.world, 'DIM-1'), cx, cz)['Level']
            sections = {}
            for s in level.get('Sections', []):
                blocks, add = s['Blocks'], s.get('Add')
                names = []
                for i in range(4096):
                    b = blocks[i] & 255
                    if add: b |= ((add[i >> 1] >> ((i & 1) * 4)) & 15) << 8
                    names.append(norm(self.legacy.blocks.get(b, str(b))))
                sections[s['Y']] = names
            biomes = [norm(self.legacy.biomes.get(b & 255, str(b & 255))) for b in level.get('Biomes', b'')]
            self.cache[(cx, cz)] = (sections, biomes)
        return self.cache[(cx, cz)]

    def block(self, x, y, z):
        return section_block(self.chunk(x >> 4, z >> 4)[0], x & 15, y, z & 15)

    def biome(self, x, z):
        return self.chunk(x >> 4, z >> 4)[1][(x & 15) + (z & 15) * 16]


def section_block(sections, x, y, z):
    s = sections.get(y >> 4)
    return 'air' if s is None else s[((y & 15) << 8) | (z << 4) | x]


# 1.16's biome fuzz (FuzzedBiomeMagnifier), for the biome 1.16 gives a block from its 4x4x4 cells.
MASK = (1 << 64) - 1


def signed(v):
    v &= MASK
    return v - (1 << 64) if v >> 63 else v


def mix(seed, value):
    return signed(seed * (seed * 6364136223846793005 + 1442695040888963407) + value)


def offset(h):
    return (((h >> 24) % 1024) / 1024.0 - 0.5) * 0.9


def fuzzed_cell(hashed, x, y, z):
    bx, by, bz = x - 2, y - 2, z - 2
    cx, cy, cz = bx >> 2, by >> 2, bz >> 2
    fx, fy, fz = (bx & 3) / 4.0, (by & 3) / 4.0, (bz & 3) / 4.0
    best, best_distance = 0, None
    for corner in range(8):
        ox, oy, oz = corner >> 2 & 1, corner >> 1 & 1, corner & 1
        h = hashed
        for v in (cx + ox, cy + oy, cz + oz, cx + ox, cy + oy, cz + oz): h = mix(h, v)
        dx = offset(h); h = mix(h, hashed); dy = offset(h); h = mix(h, hashed); dz = offset(h)
        distance = (fz - oz + dz) ** 2 + (fy - oy + dy) ** 2 + (fx - ox + dx) ** 2
        if best_distance is None or best_distance > distance: best, best_distance = corner, distance
    return cx + (best >> 2 & 1), cz + (best & 1)


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    terrain = '--terrain' in sys.argv
    vanilla, portium, seed = Vanilla(args[0]), Portium(args[1]), int(args[2])
    x0, z0, x1, z1 = map(int, args[3].split(','))
    hashed = int.from_bytes(hashlib.sha256(struct.pack('<q', seed)).digest()[:8], 'little', signed=True)
    biomes_same = biomes_total = same = total = 0
    biome_pairs, pairs = Counter(), Counter()
    for cx in range(x0, x1 + 1):
        for cz in range(z0, z1 + 1):
            for lz in range(16):
                for lx in range(16):
                    x, z = cx * 16 + lx, cz * 16 + lz
                    expected = vanilla.cell_biome(*fuzzed_cell(hashed, x, 128, z))
                    biomes_total += 1
                    if expected == portium.biome(x, z): biomes_same += 1
                    else: biome_pairs[(expected, portium.biome(x, z))] += 1
                    for y in range(128):
                        a, b = vanilla.block(x, y, z), portium.block(x, y, z)
                        if terrain and (a not in NEVER_FROM_FEATURES or b != a and b not in NEVER_FROM_FEATURES and b != 'air'): continue
                        total += 1
                        if a == b: same += 1
                        else: pairs[(a, b)] += 1
    print('column biomes: %d/%d the same %s' % (biomes_same, biomes_total, dict(biome_pairs.most_common(5))))
    print('%s: %d/%d the same' % ('blocks no feature makes' if terrain else 'blocks below y 128', same, total))
    for (a, b), n in pairs.most_common(20): print('  vanilla %-22s portium %-22s %d' % (a, b, n))


if __name__ == '__main__':
    main()
