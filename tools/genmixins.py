"""Regenerates the mixin lists in mixins.portium.json from the mixin source tree. Usage: genmixins.py <repo>"""
import json, os, sys
repo = sys.argv[1]
root = os.path.join(repo, 'src/main/java/com/michaelsebero/portium/mixin')
cfg_path = os.path.join(repo, 'src/main/resources/mixins.portium.json')
common, client = [], []
for dirpath, _, files in os.walk(root):
    for f in files:
        if not f.endswith('.java'):
            continue
        name = os.path.relpath(os.path.join(dirpath, f[:-5]), root).replace(os.sep, '.')
        (client if name.startswith('client.') else common).append(name)
cfg = json.load(open(cfg_path))
cfg['mixins'] = sorted(common)
cfg['client'] = sorted(client)
with open(cfg_path, 'w') as out:
    json.dump(cfg, out, indent=2)
    out.write('\n')
print(len(common), 'common,', len(client), 'client')
