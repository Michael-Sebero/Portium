#!/usr/bin/env python3
"""Prints a gzipped NBT file (level.dat, playerdata) or one chunk of a region file as indented text.
   tools/nbtdump.py FILE [PATH.TO.TAG]   |   tools/nbtdump.py REGION.mca CHUNK_X CHUNK_Z [PATH]"""
import gzip, struct, sys, zlib

def read(b, i, t):
    if t == 1: return struct.unpack('>b', b[i:i+1])[0], i + 1
    if t == 2: return struct.unpack('>h', b[i:i+2])[0], i + 2
    if t == 3: return struct.unpack('>i', b[i:i+4])[0], i + 4
    if t == 4: return struct.unpack('>q', b[i:i+8])[0], i + 8
    if t == 5: return struct.unpack('>f', b[i:i+4])[0], i + 4
    if t == 6: return struct.unpack('>d', b[i:i+8])[0], i + 8
    if t == 7:
        n = struct.unpack('>i', b[i:i+4])[0]; return '<%d bytes>' % n, i + 4 + n
    if t == 8:
        n = struct.unpack('>H', b[i:i+2])[0]; return b[i+2:i+2+n].decode('utf-8', 'replace'), i + 2 + n
    if t == 9:
        et = b[i]; n = struct.unpack('>i', b[i+1:i+5])[0]; i += 5; out = []
        for _ in range(n):
            v, i = read(b, i, et); out.append(v)
        return out, i
    if t == 10:
        out = {}
        while True:
            tt = b[i]; i += 1
            if tt == 0: return out, i
            n = struct.unpack('>H', b[i:i+2])[0]; name = b[i+2:i+2+n].decode('utf-8', 'replace'); i += 2 + n
            v, i = read(b, i, tt); out[name] = v
    if t == 11:
        n = struct.unpack('>i', b[i:i+4])[0]; return '<%d ints>' % n, i + 4 + 4 * n
    if t == 12:
        n = struct.unpack('>i', b[i:i+4])[0]; return '<%d longs>' % n, i + 4 + 8 * n
    raise ValueError('tag type %d' % t)

def root(data):
    n = struct.unpack('>H', data[1:3])[0]
    return read(data, 3 + n, 10)[0]

args = sys.argv[1:]
if args[0].endswith('.mca'):
    f = open(args[0], 'rb').read()
    x, z = int(args[1]) & 31, int(args[2]) & 31
    loc = struct.unpack('>I', f[4 * (x + z * 32):4 * (x + z * 32) + 4])[0]
    sector = loc >> 8
    if sector == 0: sys.exit('chunk not present')
    length = struct.unpack('>I', f[sector * 4096:sector * 4096 + 4])[0]
    tag = root(zlib.decompress(f[sector * 4096 + 5:sector * 4096 + 4 + length]))
    path = args[3] if len(args) > 3 else ''
else:
    tag = root(gzip.open(args[0]).read())
    path = args[1] if len(args) > 1 else ''
for part in filter(None, path.split('.')):
    tag = tag[int(part)] if isinstance(tag, list) else tag[part]

def show(v, indent=0):
    pad = '  ' * indent
    if isinstance(v, dict):
        for k, w in v.items():
            if isinstance(w, (dict, list)) and w:
                print(pad + k + ':'); show(w, indent + 1)
            else:
                print(pad + k + ': ' + repr(w))
    elif isinstance(v, list):
        for n, w in enumerate(v):
            if isinstance(w, (dict, list)):
                print(pad + '[%d]' % n); show(w, indent + 1)
            else:
                print(pad + '[%d] %r' % (n, w))
    else:
        print(pad + repr(v))
show(tag)
