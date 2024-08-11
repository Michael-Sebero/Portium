#!/usr/bin/env python3
"""Measures what a 1.16.5 mod needs from 1.12.2, for Portium's 1.16.5 compatibility layer (as compat113.py does for
1.13.2, whose readers and matching it reuses).

  compat116.py index            builds build/ref116/index12.json and index16.json: the Minecraft and Forge API of
                                1.12.2 and 1.16.5, by the SRG names mods are compiled against
  compat116.py scan MOD.jar...  lists the Minecraft references in 1.16.5 mod jars and whether 1.12.2 has each one
  compat116.py classes          writes Portium's table of 1.16 Minecraft classes that 1.12 has under another name
                                (src/main/resources/.../mods116/classes-1.16.5.txt), matched by the SRG ids they share

Inputs: MCPConfig 1.16.5 and Mojang's 1.16.5 client jar (./gradlew download116), Forge 1.16.5's universal jar (from
Gradle's cache once the forge116 source set has compiled), and RFG's Forge-patched 1.12.2 jar with mcp-srg.srg.
"""
import glob, json, os, re, sys, zipfile
from collections import defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import compat113 as c

ROOT = c.ROOT
REF = os.path.join(ROOT, 'build', 'ref116')
CLIENT16 = os.path.join(REF, 'client-1.16.5.jar')


def cached(pattern):
    found = glob.glob(os.path.expanduser('~/.gradle/caches/modules-2/files-2.1/' + pattern))
    if not found:
        sys.exit('Missing %s in Gradle\'s cache: build the forge116 source set first.' % pattern)
    return found[0]


FORGE16 = cached('net.minecraftforge/forge/1.16.5-36.2.39/*/forge-1.16.5-36.2.39-universal.jar')
MCPCONFIG16 = cached('de.oceanlabs.mcp/mcp_config/1.16.5-20210115.111550/*/mcp_config-1.16.5-20210115.111550.zip')

# Forge 1.16 adds its methods to Minecraft classes through these interfaces, which the patched classes implement.
EXTENSIONS = {
    'net/minecraft/block/Block': 'net/minecraftforge/common/extensions/IForgeBlock',
    'net/minecraft/block/BlockState': 'net/minecraftforge/common/extensions/IForgeBlockState',
    'net/minecraft/item/Item': 'net/minecraftforge/common/extensions/IForgeItem',
    'net/minecraft/item/ItemStack': 'net/minecraftforge/common/extensions/IForgeItemStack',
    'net/minecraft/entity/Entity': 'net/minecraftforge/common/extensions/IForgeEntity',
    'net/minecraft/tileentity/TileEntity': 'net/minecraftforge/common/extensions/IForgeTileEntity',
    'net/minecraft/world/World': 'net/minecraftforge/common/extensions/IForgeWorld',
    'net/minecraft/world/server/ServerWorld': 'net/minecraftforge/common/extensions/IForgeWorldServer',
    'net/minecraft/fluid/Fluid': 'net/minecraftforge/common/extensions/IForgeFluid',
    'net/minecraft/fluid/FluidState': 'net/minecraftforge/common/extensions/IForgeFluidState',
    'net/minecraft/potion/Effect': 'net/minecraftforge/common/extensions/IForgeEffect',
    'net/minecraft/network/PacketBuffer': 'net/minecraftforge/common/extensions/IForgePacketBuffer',
    'net/minecraft/client/settings/KeyBinding': 'net/minecraftforge/client/extensions/IForgeKeybinding',
}

CLASSES_OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'com', 'michaelsebero', 'portium', 'mods116', 'classes-1.16.5.txt')

# Matches the SRG ids get wrong, or 1.16 classes so changed that 1.16 code needs Minecraft 1.16.5's own; grown as they
# turn up.
NOT_RENAMED = set(c.NOT_RENAMED) | {
    # A concrete holder made from a supplier in 1.16; 1.12's LazyLoadBase is abstract.
    'net/minecraft/util/LazyValue',
    # Interfaces that mods' own classes implement, matched to 1.12 classes by an id or two.
    'net/minecraft/entity/IRendersAsItem', 'net/minecraft/entity/IAngerable',
}
NOT_RENAMED_PACKAGES = c.NOT_RENAMED_PACKAGES

# Values of 1.16's shared block state properties that are 1.12's own properties' values, for blocks 1.12 has.
VALUE_CLASSES = dict(c.VALUE_CLASSES)
# Enums 1.12 has under another name, which no SRG id ties together (their constants have none).
VALUE_CLASSES.update({
    'net/minecraft/util/Hand': 'net/minecraft/util/EnumHand',
    'net/minecraft/util/ActionResultType': 'net/minecraft/util/EnumActionResult',
    'net/minecraft/util/Direction$Plane': 'net/minecraft/util/EnumFacing$Plane',
    'net/minecraft/item/UseAction': 'net/minecraft/item/EnumAction',
    'net/minecraft/block/BlockRenderType': 'net/minecraft/util/EnumBlockRenderType',
    'net/minecraft/block/material/PushReaction': 'net/minecraft/block/material/EnumPushReaction',
    'net/minecraft/client/renderer/model/ItemCameraTransforms$TransformType': 'net/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType',
    'net/minecraft/network/PacketDirection': 'net/minecraft/network/EnumPacketDirection',
})


def read_tsrg16():
    with zipfile.ZipFile(MCPCONFIG16) as z:
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


def build_index16():
    classes, members = read_tsrg16()
    index = {}
    for info in c.classes_in(CLIENT16):
        obf = info['name']
        if obf not in classes:
            continue
        own = members.get(obf, {})
        renamed = {
            'name': classes[obf],
            'super': classes.get(info['super'], info['super']),
            'interfaces': [classes.get(i, i) for i in info['interfaces']],
            'access': info['access'],
            'methods': [(acc, own.get(('method', name, desc), name), c.remap_desc(desc, classes)) for acc, name, desc in info['methods']],
            'fields': [(acc, own.get(('field', name, None), name), c.remap_desc(desc, classes)) for acc, name, desc in info['fields']],
        }
        index[renamed['name']] = c.index_entry(renamed)
    for info in c.classes_in(FORGE16, ('net/minecraftforge/',)):
        index[info['name']] = c.index_entry(info)
    for owner, extension in EXTENSIONS.items():
        if owner in index and extension in index:
            index[owner]['interfaces'] = sorted(set(index[owner]['interfaces']) | {extension})
    return index


def load(name):
    with open(os.path.join(REF, name)) as f:
        return json.load(f)


def command_index():
    for name, build in (('index12.json', c.build_index12), ('index16.json', build_index16)):
        index = build()
        with open(os.path.join(REF, name), 'w') as out:
            json.dump(index, out, separators=(',', ':'), sort_keys=True)
        print('%s: %d classes' % (name, len(index)))


def command_classes():
    # compat113's matching, on 1.16's index and with 1.16's exceptions.
    c.REF, c.CLASSES_OUT, c.NOT_RENAMED, c.NOT_RENAMED_PACKAGES, c.VALUE_CLASSES = REF, CLASSES_OUT, NOT_RENAMED, NOT_RENAMED_PACKAGES, VALUE_CLASSES
    original = c.load
    c.load = lambda name: load('index16.json' if name == 'index13.json' else name)
    try:
        c.command_classes()
    finally:
        c.load = original
    with open(CLASSES_OUT) as f:
        text = f.read().replace('1.13.2 Minecraft classes', '1.16.5 Minecraft classes').replace('"1.13 1.12"', '"1.16 1.12"') \
            .replace('tools/compat113.py classes', 'tools/compat116.py classes')
    text = without_native_ancestors(text)
    with open(CLASSES_OUT, 'w') as f:
        f.write(text)


def portium_mapped():
    """1.16 classes Portium maps by hand: its stand-ins (modern/mc) and ModernMappings' RENAMED table."""
    mapped = set()
    shims = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'michaelsebero', 'portium', 'modern', 'mc')
    for folder, _, files in os.walk(shims):
        for name in files:
            if name.endswith('.java'):
                mapped.add('net/minecraft/' + os.path.relpath(os.path.join(folder, name[:-5]), shims).replace(os.sep, '/'))
    source = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'michaelsebero', 'portium', 'mods116', 'ModernMappings.java')
    with open(source) as f:
        mapped |= set(re.findall(r'RENAMED\.put\("([^"]+)"', f.read()))
    return mapped


def without_native_ancestors(text):
    """
    Drops pairings of 1.16 classes that extend a class set apart to load as Minecraft 1.16.5's own (NOT_RENAMED):
    1.12's class isn't one of those, so 1.16 code handing it on as one fails (a desert well feature
    paired with 1.12's well generator, registered as a 1.16 feature).
    """
    index12, index16 = load('index12.json'), load('index16.json')
    lines = text.splitlines()
    table = dict(line.split() for line in lines if line and not line.startswith('#'))
    mapped = portium_mapped()

    def native(name):
        # Only those set apart as 1.16's own by hand: others are native by accident of matching, and their 1.12 pairs
        # (a button for a face-attached block) are worth more than the hierarchy.
        return (name in NOT_RENAMED or name.startswith(NOT_RENAMED_PACKAGES)) and name not in mapped

    changed = True
    while changed:
        changed = False
        for name in list(table):
            if name in VALUE_CLASSES:
                continue
            ancestor = index16.get(name, {}).get('super')
            while ancestor and ancestor != 'java/lang/Object':
                if native(ancestor):
                    del table[name]
                    changed = True
                    break
                ancestor = index16.get(ancestor, {}).get('super')
    header = [line for line in lines if line.startswith('#')]
    return '\n'.join(header + ['%s %s' % (name, table[name]) for name in sorted(table)]) + '\n'



def command_scan(jars):
    c.REF = REF
    original = c.load
    c.load = lambda name: load('index16.json' if name == 'index13.json' else name)
    try:
        c.command_scan(jars)
    finally:
        c.load = original


if __name__ == '__main__':
    if len(sys.argv) < 2 or sys.argv[1] not in ('index', 'scan', 'classes'):
        sys.exit(__doc__)
    if sys.argv[1] == 'index':
        command_index()
    elif sys.argv[1] == 'classes':
        command_classes()
    else:
        command_scan(sys.argv[2:])
