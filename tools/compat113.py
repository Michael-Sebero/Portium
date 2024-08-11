#!/usr/bin/env python3
"""Measures what a 1.13.2 mod needs from 1.12.2, for Portium's compatibility layer.

  compat113.py index            builds build/ref113/index12.json and index13.json: the Minecraft and Forge API of
                                1.12.2 and 1.13.2, by the SRG names mods are compiled against
  compat113.py scan MOD.jar...  lists the Minecraft references in 1.13.2 mod jars and whether 1.12.2 has each one
                                under the same name and descriptor ("same"), or not ("missing")
  compat113.py classes          writes Portium's table of 1.13 Minecraft classes that 1.12 has under another name
                                (src/main/resources/.../modern/classes-1.13.2.txt), matched by the SRG ids they share

Inputs (see build/ref113/): MCPConfig 1.13.2 (joined.tsrg) and Mojang's 1.13.2 client jar for 1.13's Minecraft;
Forge 1.13.2's universal jar; RFG's Forge-patched 1.12.2 jar with mcp-srg.srg for 1.12.
"""
import io, json, os, re, struct, sys, zipfile
from collections import defaultdict

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
REF = os.path.join(ROOT, 'build', 'ref113')
SRG12 = os.path.expanduser('~/.gradle/caches/minecraft/de/oceanlabs/mcp/mcp_stable/39/rfg_srgs/mcp-srg.srg')
JAR12 = os.path.join(ROOT, 'build', 'rfg', 'recompiled_minecraft-1.12.2.jar')
CLIENT13 = os.path.join(REF, 'client-1.13.2.jar')
FORGE13 = os.path.join(REF, 'forge-1.13.2-25.0.223-universal.jar')
MCPCONFIG13 = os.path.join(REF, 'mcp_config-1.13.2-20190213.203750.zip')

# Forge 1.13 adds its methods to Minecraft classes through these interfaces, which the patched classes implement.
EXTENSIONS = {
    'net/minecraft/block/Block': 'net/minecraftforge/common/extensions/IForgeBlock',
    'net/minecraft/block/state/IBlockState': 'net/minecraftforge/common/extensions/IForgeBlockState',
    'net/minecraft/item/Item': 'net/minecraftforge/common/extensions/IForgeItem',
    'net/minecraft/item/ItemStack': 'net/minecraftforge/common/extensions/IForgeItemStack',
    'net/minecraft/entity/Entity': 'net/minecraftforge/common/extensions/IForgeEntity',
    'net/minecraft/tileentity/TileEntity': 'net/minecraftforge/common/extensions/IForgeTileEntity',
    'net/minecraft/world/World': 'net/minecraftforge/common/extensions/IForgeWorld',
    'net/minecraft/world/dimension/Dimension': 'net/minecraftforge/common/extensions/IForgeDimension',
    'net/minecraft/world/IWorldReader': 'net/minecraftforge/common/extensions/IForgeWorldReader',
    'net/minecraft/world/biome/Biome': 'net/minecraftforge/common/extensions/IForgeBiome',
    'net/minecraft/potion/Potion': 'net/minecraftforge/common/extensions/IForgePotion',
    'net/minecraft/enchantment/Enchantment': 'net/minecraftforge/common/extensions/IForgeEnchantment',
    'net/minecraft/fluid/Fluid': 'net/minecraftforge/common/extensions/IForgeFluid',
    'net/minecraft/fluid/IFluidState': 'net/minecraftforge/common/extensions/IForgeFluidState',
    'net/minecraft/client/settings/KeyBinding': 'net/minecraftforge/client/extensions/IForgeKeybinding',
}


# ---- Class files ----

def read_class(data):
    """this, super, interfaces, fields [(access, name, desc)], methods [(access, name, desc)], and the constant pool's
    class names, member references [(kind, owner, name, desc)] and UTF-8 strings."""
    pos = 8
    count = struct.unpack_from('>H', data, pos)[0]
    pos += 2
    cp = [None] * count
    i = 1
    while i < count:
        tag = data[pos]
        pos += 1
        if tag == 1:
            n = struct.unpack_from('>H', data, pos)[0]
            cp[i] = ('utf8', data[pos + 2:pos + 2 + n].decode('utf-8', 'replace'))
            pos += 2 + n
        elif tag in (3, 4):
            pos += 4
        elif tag in (5, 6):
            pos += 8
            i += 1
        elif tag in (7, 8, 16, 19, 20):
            cp[i] = (tag, struct.unpack_from('>H', data, pos)[0])
            pos += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            cp[i] = (tag,) + struct.unpack_from('>HH', data, pos)
            pos += 4
        elif tag == 15:
            pos += 3
        else:
            raise ValueError('constant tag %d' % tag)
        i += 1

    def utf(index):
        return cp[index][1]

    def cls(index):
        return utf(cp[index][1]) if index else None

    access, this, sup, n = struct.unpack_from('>HHHH', data, pos)
    pos += 8
    interfaces = [cls(struct.unpack_from('>H', data, pos + 2 * k)[0]) for k in range(n)]
    pos += 2 * n

    def members(pos):
        n = struct.unpack_from('>H', data, pos)[0]
        pos += 2
        out = []
        for _ in range(n):
            acc, name, desc, attrs = struct.unpack_from('>HHHH', data, pos)
            pos += 8
            for _ in range(attrs):
                length = struct.unpack_from('>I', data, pos + 2)[0]
                pos += 6 + length
            out.append((acc, utf(name), utf(desc)))
        return out, pos

    fields, pos = members(pos)
    methods, pos = members(pos)
    classes, refs, strings = set(), [], []
    for entry in cp:
        if entry is None:
            continue
        if entry[0] == 7:
            classes.add(utf(entry[1]))
        elif entry[0] in (9, 10, 11):
            nat = cp[entry[2]]
            refs.append(({9: 'field', 10: 'method', 11: 'method'}[entry[0]], cls(entry[1]), utf(nat[1]), utf(nat[2])))
        elif entry[0] == 'utf8':
            strings.append(entry[1])
    return {'name': cls(this), 'super': cls(sup), 'interfaces': interfaces, 'access': access,
            'fields': fields, 'methods': methods, 'classes': classes, 'refs': refs, 'strings': strings}


def classes_in(jar, prefixes=None):
    with zipfile.ZipFile(jar) as z:
        for entry in z.namelist():
            if not entry.endswith('.class') or (prefixes and not entry.startswith(prefixes)):
                continue
            yield read_class(z.read(entry))


# ---- Name maps ----

TYPE = re.compile(r'L([^;]+);')


def remap_desc(desc, classes):
    return TYPE.sub(lambda m: 'L' + classes.get(m.group(1), m.group(1)) + ';', desc)


def read_srg12():
    """1.12: MCP class/member names to SRG."""
    methods, fields = {}, {}
    for line in open(SRG12):
        parts = line.split()
        if parts[0] == 'FD:':
            owner, name = parts[1].rsplit('/', 1)
            fields[(owner, name)] = parts[2].rsplit('/', 1)[1]
        elif parts[0] == 'MD:':
            owner, name = parts[1].rsplit('/', 1)
            methods[(owner, name, parts[2])] = parts[3].rsplit('/', 1)[1]
    return methods, fields


def read_tsrg13():
    """1.13: obfuscated class/member names to SRG, from MCPConfig's joined.tsrg."""
    with zipfile.ZipFile(MCPCONFIG13) as z:
        text = z.read('config/joined.tsrg').decode()
    classes, members, owner = {}, defaultdict(dict), None
    for line in text.splitlines():
        if not line.startswith('\t'):
            obf, srg = line.split()
            classes[obf] = srg
            owner = obf
        else:
            parts = line.split()
            if len(parts) == 2:
                members[owner][('field', parts[0], None)] = parts[1]
            else:
                members[owner][('method', parts[0], parts[1])] = parts[2]
    return classes, members


# ---- Indexes ----

def index_entry(info):
    return {'super': info['super'], 'interfaces': info['interfaces'], 'access': info['access'],
            'methods': sorted({m[1] + m[2] for m in info['methods']}),
            'fields': sorted({f[1] + ':' + f[2] for f in info['fields']})}


def build_index12():
    methods, fields = read_srg12()
    raw = {info['name']: info for info in classes_in(JAR12, ('net/minecraft/', 'net/minecraftforge/'))}

    def ancestors(name, seen=None):
        seen = seen if seen is not None else set()
        info = raw.get(name)
        if not info or name in seen:
            return seen
        seen.add(name)
        for parent in [info['super']] + info['interfaces']:
            if parent:
                ancestors(parent, seen)
        return seen

    index = {}
    for name, info in raw.items():
        lineage = ancestors(name)
        renamed = dict(info)
        renamed['methods'] = []
        for acc, mname, desc in info['methods']:
            srg = mname
            for owner in lineage:
                if (owner, mname, desc) in methods:
                    srg = methods[(owner, mname, desc)]
                    break
            renamed['methods'].append((acc, srg, desc))
        renamed['fields'] = [(acc, fields.get((name, fname), fname), desc) for acc, fname, desc in info['fields']]
        index[name] = index_entry(renamed)
    return index


def build_index13():
    classes, members = read_tsrg13()
    index = {}
    for info in classes_in(CLIENT13):
        obf = info['name']
        if obf not in classes:
            continue
        own = members.get(obf, {})
        renamed = {
            'name': classes[obf],
            'super': classes.get(info['super'], info['super']),
            'interfaces': [classes.get(i, i) for i in info['interfaces']],
            'access': info['access'],
            'methods': [(acc, own.get(('method', name, desc), name), remap_desc(desc, classes)) for acc, name, desc in info['methods']],
            'fields': [(acc, own.get(('field', name, None), name), remap_desc(desc, classes)) for acc, name, desc in info['fields']],
        }
        index[renamed['name']] = index_entry(renamed)
    for info in classes_in(FORGE13, ('net/minecraftforge/',)):
        index[info['name']] = index_entry(info)
    for owner, extension in EXTENSIONS.items():
        if owner in index and extension in index:
            index[owner]['interfaces'] = sorted(set(index[owner]['interfaces']) | {extension})
    return index


def command_index():
    for name, build in (('index12.json', build_index12), ('index13.json', build_index13)):
        index = build()
        with open(os.path.join(REF, name), 'w') as out:
            json.dump(index, out, separators=(',', ':'), sort_keys=True)
        print('%s: %d classes' % (name, len(index)))


# ---- Classes 1.12 has under another name ----

CLASSES_OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'com', 'michaelsebero', 'portium', 'modern', 'classes-1.13.2.txt')

# Matches the SRG ids get wrong: unrelated classes sharing an id or two, or classes that 1.13 rewrote.
NOT_RENAMED = {
    'net/minecraft/block/BlockFlowingFluid', 'net/minecraft/client/renderer/FogRenderer', 'net/minecraft/client/renderer/model/IUnbakedModel',
    'net/minecraft/command/ICommandSource', 'net/minecraft/inventory/IRecipeHelperPopulator', 'net/minecraft/inventory/IRecipeHolder',
    'net/minecraft/network/NetworkManager$QueuedPacket', 'net/minecraft/potion/PotionUtil', 'net/minecraft/world/ISaveDataAccess',
    'net/minecraft/world/gen/feature/Feature', 'net/minecraft/world/gen/feature/structure/Structure',
}
NOT_RENAMED_PACKAGES = ('net/minecraft/fluid/',)

# Values of 1.13's shared block state properties that are 1.12's own properties' values, for blocks 1.12 has.
VALUE_CLASSES = {
    'net/minecraft/state/properties/DoubleBlockHalf': 'net/minecraft/block/BlockDoor$EnumDoorHalf',
    'net/minecraft/state/properties/DoorHingeSide': 'net/minecraft/block/BlockDoor$EnumHingePosition',
    'net/minecraft/state/properties/Half': 'net/minecraft/block/BlockStairs$EnumHalf',
    'net/minecraft/state/properties/StairsShape': 'net/minecraft/block/BlockStairs$EnumShape',
    'net/minecraft/entity/CreatureAttribute': 'net/minecraft/entity/EnumCreatureAttribute',
}


def declared_ids(info):
    ids = {m.split('(')[0] for m in info['methods'] if m.startswith('func_')}
    return ids | {f.split(':')[0] for f in info['fields'] if f.startswith('field_')}


def introduced_ids(index):
    """The SRG ids each class declares that none of its supertypes does: what it adds, not what it overrides."""
    inherited = {}

    def ancestors(name):
        if name in inherited:
            return inherited[name]
        inherited[name] = set()
        info = index.get(name)
        found = set()
        for parent in ([info['super']] + info['interfaces']) if info else []:
            if parent in index:
                found |= declared_ids(index[parent]) | ancestors(parent)
        inherited[name] = found
        return found

    return {name: declared_ids(info) - ancestors(name) for name, info in index.items()}


def command_classes():
    index12, index13 = load('index12.json'), load('index13.json')
    added12, added13 = introduced_ids(index12), introduced_ids(index13)
    owners = defaultdict(set)
    for name, ids in added12.items():
        for i in ids:
            owners[i].add(name)
    by_simple = defaultdict(list)
    for name in index12:
        by_simple[name.rsplit('/', 1)[-1]].append(name)
    table = dict(VALUE_CLASSES)
    for name in sorted(index13):
        if name in index12 or name in table or not name.startswith('net/minecraft/') or re.search(r'\$\d+$', name):
            continue
        if name in NOT_RENAMED or name.startswith(NOT_RENAMED_PACKAGES):
            continue
        counts = defaultdict(int)
        for i in added13[name]:
            for owner in owners.get(i, ()):
                counts[owner] += 1
        ranked = sorted(counts.items(), key=lambda kv: -kv[1])
        same_name = by_simple.get(name.rsplit('/', 1)[-1], [])
        if ranked:
            best, shared = ranked[0]
            runner_up = ranked[1][1] if len(ranked) > 1 else 0
            if best in same_name or shared >= 1 and shared > runner_up and (shared >= 2 or len(added13[name]) <= 2):
                if shared >= 2 or best in same_name or len(added13[name]) <= 2:
                    table[name] = best
        elif not added13[name] and len(same_name) == 1:
            table[name] = same_name[0]
    # 1.13 code can't name a class that isn't public, which it would once renamed: those stay 1.13.2's own.
    table = {name: target for name, target in table.items() if index12[target]['access'] & 0x0001}
    os.makedirs(os.path.dirname(CLASSES_OUT), exist_ok=True)
    with open(CLASSES_OUT, 'w') as out:
        out.write('# 1.13.2 Minecraft classes that 1.12.2 has under another name, as "1.13 1.12". Generated by tools/compat113.py classes.\n')
        for name in sorted(table):
            out.write('%s %s\n' % (name, table[name]))
    print('%s: %d classes' % (os.path.relpath(CLASSES_OUT, ROOT), len(table)))


# ---- Scanning mods ----

def load(name):
    with open(os.path.join(REF, name)) as f:
        return json.load(f)


def has_member(index, owner, kind, key, seen=None):
    """Whether a class has a method (name+desc) or field (name:desc), itself or through its ancestors."""
    seen = seen if seen is not None else set()
    info = index.get(owner)
    if info is None or owner in seen:
        return False
    seen.add(owner)
    if key in info['methods' if kind == 'method' else 'fields']:
        return True
    return any(has_member(index, parent, kind, key, seen) for parent in [info['super']] + info['interfaces'] if parent)


def is_api(name):
    return name and (name.startswith('net/minecraft/') or name.startswith('net/minecraftforge/') or name.startswith('com/mojang/'))


def command_scan(jars):
    index12, index13 = load('index12.json'), load('index13.json')
    result = {'classes': defaultdict(set), 'members': defaultdict(set)}
    for jar in jars:
        for info in classes_in(jar):
            for name in info['classes'] | {info['super']} | set(info['interfaces']):
                if is_api(name) and not name.startswith('['):
                    result['classes'][name].add(info['name'])
            for kind, owner, name, desc in info['refs']:
                if is_api(owner) and not owner.startswith('['):
                    key = name + desc if kind == 'method' else name + ':' + desc
                    result['members'][(owner, kind, key)].add(info['name'])
    same_classes = {c for c in result['classes'] if c in index12}
    print('Classes referenced: %d, present in 1.12 by name: %d' % (len(result['classes']), len(same_classes)))
    for name in sorted(result['classes']):
        where = 'same' if name in index12 else ('forge13' if name.startswith('net/minecraftforge/') else 'missing')
        if where != 'same':
            print('  class %-8s %s' % (where, name))
    same = missing = 0
    lines = []
    for (owner, kind, key) in sorted(result['members']):
        ok = has_member(index12, owner, kind, key)
        known13 = has_member(index13, owner, kind, key)
        same += ok
        missing += not ok
        if not ok:
            lines.append('  %-6s %-8s %s.%s' % (kind, 'missing' if known13 else '?', owner, key))
    print('Members referenced: %d, same in 1.12: %d, missing: %d' % (same + missing, same, missing))
    print('\n'.join(lines))


if __name__ == '__main__':
    if len(sys.argv) < 2 or sys.argv[1] not in ('index', 'scan', 'classes'):
        sys.exit(__doc__)
    if sys.argv[1] == 'index':
        command_index()
    elif sys.argv[1] == 'classes':
        command_classes()
    else:
        command_scan(sys.argv[2:])
