"""Lists references from Portium's common classes to Minecraft members that Forge strips on dedicated servers
(@SideOnly(Side.CLIENT)). Compiling can't catch these; they crash only on a server.

Usage: sideonly.py <build/classes/java/main> <build/rfg/recompiled_minecraft-1.12.2.jar>

References made inside Portium's own client-only methods (also @SideOnly(CLIENT), like addInformation) are reported
too and are safe; check any new entry by hand.
"""
import os
import re
import struct
import sys
import zipfile

classes_dir, mc_jar = sys.argv[1], sys.argv[2]
CLIENT_PATHS = ('com/michaelsebero/portium/client/', 'com/michaelsebero/portium/mixin/client/', 'com/michaelsebero/portium/proxy/ClientProxy')


def parse(data):
    """Constant pool, member annotations and references of one class file."""
    pos = 10
    count = struct.unpack('>H', data[8:10])[0]
    cp = [None] * count
    i = 1
    while i < count:
        tag = data[pos]
        if tag == 1:
            n = struct.unpack('>H', data[pos + 1:pos + 3])[0]
            cp[i] = ('utf8', data[pos + 3:pos + 3 + n].decode('utf8', 'replace'))
            pos += 3 + n
        elif tag in (3, 4):
            pos += 5
        elif tag in (5, 6):
            pos += 9
            i += 1
        elif tag in (7, 8, 16, 19, 20):
            cp[i] = (tag, struct.unpack('>H', data[pos + 1:pos + 3])[0])
            pos += 3
        elif tag in (9, 10, 11, 12, 17, 18):
            cp[i] = (tag, struct.unpack('>H', data[pos + 1:pos + 3])[0], struct.unpack('>H', data[pos + 3:pos + 5])[0])
            pos += 5
        elif tag == 15:
            pos += 4
        else:
            raise ValueError('constant pool tag %d' % tag)
        i += 1

    def utf(idx):
        return cp[idx][1]

    def class_name(idx):
        return utf(cp[idx][1]) if idx else None

    _, this_class, super_class = struct.unpack('>HHH', data[pos:pos + 6])
    pos += 6
    interface_count = struct.unpack('>H', data[pos:pos + 2])[0]
    pos += 2
    interfaces = [class_name(struct.unpack('>H', data[pos + 2 * k:pos + 2 * k + 2])[0]) for k in range(interface_count)]
    pos += 2 * interface_count

    def attributes(pos):
        n = struct.unpack('>H', data[pos:pos + 2])[0]
        pos += 2
        result = []
        for _ in range(n):
            name, length = struct.unpack('>HI', data[pos:pos + 6])
            result.append((utf(name), data[pos + 6:pos + 6 + length]))
            pos += 6 + length
        return result, pos

    def client_only(attrs):
        for name, body in attrs:
            if 'Annotations' not in name:
                continue
            # Annotations name their type and enum constant by constant-pool index.
            texts = set()
            for k in range(len(body) - 1):
                idx = struct.unpack('>H', body[k:k + 2])[0]
                if 0 < idx < len(cp) and cp[idx] and cp[idx][0] == 'utf8':
                    texts.add(cp[idx][1])
            if 'Lnet/minecraftforge/fml/relauncher/SideOnly;' in texts and 'CLIENT' in texts:
                return True
        return False

    members = {}
    for _ in ('fields', 'methods'):
        n = struct.unpack('>H', data[pos:pos + 2])[0]
        pos += 2
        for _ in range(n):
            _, name, desc = struct.unpack('>HHH', data[pos:pos + 6])
            pos += 6
            attrs, pos = attributes(pos)
            members[(utf(name), utf(desc))] = client_only(attrs)
    attrs, pos = attributes(pos)
    refs, types = set(), set()
    for entry in cp:
        if not entry:
            continue
        if entry[0] == 7:
            types.add(utf(entry[1]))
        elif entry[0] in (9, 10, 11):
            name_and_type = cp[entry[2]]
            refs.add((class_name(entry[1]), utf(name_and_type[1]), utf(name_and_type[2])))
        elif entry[0] == 'utf8':
            for match in re.finditer(r'L(net/minecraft/[\w/$]+);', entry[1]):
                types.add(match.group(1))
    return {'super': class_name(super_class), 'interfaces': interfaces, 'client': client_only(attrs), 'members': members, 'refs': refs, 'types': types}


minecraft = zipfile.ZipFile(mc_jar)
cache = {}


def minecraft_class(name):
    if name not in cache:
        try:
            cache[name] = parse(minecraft.read(name + '.class'))
        except KeyError:
            cache[name] = None
    return cache[name]


def member_is_client(owner, name, desc):
    """Looks the member up through superclasses and interfaces, as the JVM resolves it."""
    seen, todo = set(), [owner]
    while todo:
        current = todo.pop(0)
        if not current or current in seen:
            continue
        seen.add(current)
        info = minecraft_class(current)
        if info is None:
            return None
        if (name, desc) in info['members']:
            return info['members'][(name, desc)]
        todo += [info['super']] + info['interfaces']
    return None


problems = set()
for dirpath, _, files in os.walk(classes_dir):
    for f in files:
        if not f.endswith('.class'):
            continue
        rel = os.path.relpath(os.path.join(dirpath, f), classes_dir)
        if rel.startswith(CLIENT_PATHS):
            continue
        info = parse(open(os.path.join(dirpath, f), 'rb').read())
        for type_name in info['types']:
            type_name = type_name.lstrip('[').strip('L;')
            if type_name.startswith('net/minecraft/'):
                found = minecraft_class(type_name)
                if found and found['client']:
                    problems.add((rel, 'class ' + type_name))
        for owner, name, desc in info['refs']:
            if not owner or not owner.startswith('net/minecraft/'):
                continue
            found = minecraft_class(owner)
            if found and found['client']:
                problems.add((rel, 'class ' + owner))
            elif member_is_client(owner, name, desc):
                problems.add((rel, '%s.%s%s' % (owner, name, desc)))
for rel, what in sorted(problems):
    print(rel, '->', what)
print(len(problems), 'references to client-only Minecraft code')
