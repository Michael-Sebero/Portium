#!/usr/bin/env python3
"""Generates Portium's blockstates, models, recipes, sounds.json and lang from compact tables.

Usage: genres.py <portium repo root> <vanilla 1.12 assets/minecraft dir>

The vanilla directory only needs 1.12.2's blockstates, which serve as templates. Extract them from the client jar
RetroFuturaGradle caches, e.g. unzip ~/.gradle/caches/retro_futura_gradle/mc-vanilla/1.12.2/client.jar
'assets/minecraft/blockstates/*' -d <dir>, then pass <dir>/assets/minecraft.
"""
import json
import os
import sys

ROOT = sys.argv[1]
VANILLA = sys.argv[2]
MOD = "portium"
RES = os.path.join(ROOT, "src/main/resources/assets", MOD)

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak"]
WOOD_META = {"oak": 0, "spruce": 1, "birch": 2, "jungle": 3, "acacia": 4, "dark_oak": 5}
LOG_ITEM = {"oak": ("minecraft:log", 0), "spruce": ("minecraft:log", 1), "birch": ("minecraft:log", 2),
            "jungle": ("minecraft:log", 3), "acacia": ("minecraft:log2", 0), "dark_oak": ("minecraft:log2", 1)}
LOG_TEX = {"oak": "log_oak", "spruce": "log_spruce", "birch": "log_birch", "jungle": "log_jungle",
           "acacia": "log_acacia", "dark_oak": "log_big_oak"}
PLANK_TEX = {"oak": "planks_oak", "spruce": "planks_spruce", "birch": "planks_birch", "jungle": "planks_jungle",
             "acacia": "planks_acacia", "dark_oak": "planks_big_oak"}
CORALS = ["tube", "brain", "bubble", "fire", "horn"]
PRISMARINE = {"prismarine": "prismarine_rough", "prismarine_brick": "prismarine_bricks", "dark_prismarine": "prismarine_dark"}
PRISMARINE_META = {"prismarine": 0, "prismarine_brick": 1, "dark_prismarine": 2}

written = []


def write(path, data):
    full = os.path.join(RES, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w") as f:
        if isinstance(data, str):
            f.write(data)
        else:
            json.dump(data, f, indent=2)
            f.write("\n")
    written.append(path)


def tex(path):
    return MOD + ":" + path


def blockstate(name, variants):
    write("blockstates/%s.json" % name, {"variants": variants})


def block_model(name, parent, textures=None):
    data = {"parent": parent}
    if textures:
        data["textures"] = textures
    write("models/block/%s.json" % name, data)


def item_model(name, parent=None, layer0=None, extra=None):
    if layer0:
        data = {"parent": parent or "item/generated", "textures": {"layer0": layer0}}
    else:
        data = {"parent": parent or (MOD + ":block/" + name)}
    if extra:
        data.update(extra)
    write("models/item/%s.json" % name, data)


def rotatable_button(variants):
    """Adds 1.13's rotated floor and ceiling buttons (rotated=true turns the button 90 degrees)."""
    result = {}
    for key, value in variants.items():
        result[key + ",rotated=false"] = value
        rotated = dict(value)
        if "facing=up" in key or "facing=down" in key:
            rotated["y"] = (rotated.get("y", 0) + 90) % 360
        result[key + ",rotated=true"] = rotated
    return result


def vanilla_variants(template, rename):
    with open(os.path.join(VANILLA, "blockstates", template + ".json")) as f:
        variants = json.load(f)["variants"]
    for value in variants.values():
        for v in (value if isinstance(value, list) else [value]):
            v["model"] = rename(v["model"])
    return variants


def bools(*names):
    """All combinations of boolean properties, as variant keys."""
    combos = [{}]
    for n in names:
        combos = [dict(c, **{n: b}) for c in combos for b in ("false", "true")]
    return combos


def key(props):
    return ",".join("%s=%s" % (k, props[k]) for k in sorted(props))


# ---------------------------------------------------------------- simple cubes

def cube_all(name, texture):
    block_model(name, "block/cube_all", {"all": texture})
    blockstate(name, {"normal": {"model": MOD + ":" + name}})
    item_model(name)


cube_all("blue_ice", tex("blocks/blue_ice"))
cube_all("smooth_stone", "blocks/stone_slab_top")
cube_all("smooth_sandstone", "blocks/sandstone_top")
cube_all("smooth_red_sandstone", "blocks/red_sandstone_top")
cube_all("smooth_quartz", "blocks/quartz_block_bottom")

block_model("dried_kelp_block", "block/cube_bottom_top", {"top": tex("blocks/dried_kelp_top"), "bottom": tex("blocks/dried_kelp_bottom"), "side": tex("blocks/dried_kelp_side")})
blockstate("dried_kelp_block", {"normal": {"model": MOD + ":dried_kelp_block"}})
item_model("dried_kelp_block")

block_model("pumpkin", "block/cube_column", {"end": "blocks/pumpkin_top", "side": "blocks/pumpkin_side"})
blockstate("pumpkin", {"normal": {"model": MOD + ":pumpkin"}})
item_model("pumpkin")

# ---------------------------------------------------------------- wood blocks

for wood in WOODS:
    side, top = "blocks/" + LOG_TEX[wood], "blocks/" + LOG_TEX[wood] + "_top"
    stripped_side, stripped_top = tex("blocks/stripped_%s_log" % wood), tex("blocks/stripped_%s_log_top" % wood)
    for name, end, s in (("stripped_%s_log" % wood, stripped_top, stripped_side),
                         ("%s_wood" % wood, side, side),
                         ("stripped_%s_wood" % wood, stripped_side, stripped_side)):
        block_model(name, "block/cube_column", {"end": end, "side": s})
        blockstate(name, {"axis=y": {"model": MOD + ":" + name},
                          "axis=z": {"model": MOD + ":" + name, "x": 90},
                          "axis=x": {"model": MOD + ":" + name, "x": 90, "y": 90}})
        item_model(name)

for wood in WOODS[1:]:
    planks = "blocks/" + PLANK_TEX[wood]
    button = wood + "_button"
    block_model(button, "block/button", {"texture": planks})
    block_model(button + "_pressed", "block/button_pressed", {"texture": planks})
    block_model(button + "_inventory", "block/button_inventory", {"texture": planks})
    blockstate(button, rotatable_button(vanilla_variants("wooden_button", lambda m, b=button: MOD + ":" + m.replace("wooden_button", b))))
    item_model(button, MOD + ":block/" + button + "_inventory")

    plate = wood + "_pressure_plate"
    block_model(plate + "_up", "block/pressure_plate_up", {"texture": planks})
    block_model(plate + "_down", "block/pressure_plate_down", {"texture": planks})
    blockstate(plate, vanilla_variants("wooden_pressure_plate", lambda m, p=plate: MOD + ":" + m.replace("wooden_pressure_plate", p)))
    item_model(plate, MOD + ":block/" + plate + "_up")

    trapdoor = wood + "_trapdoor"
    for part in ("bottom", "top", "open"):
        block_model(trapdoor + "_" + part, "block/trapdoor_" + part, {"texture": tex("blocks/" + trapdoor)})
    blockstate(trapdoor, vanilla_variants("trapdoor", lambda m, t=trapdoor: MOD + ":" + m.replace("wooden_trapdoor", t)))
    item_model(trapdoor, MOD + ":block/" + trapdoor + "_bottom")

# ---------------------------------------------------------------- prismarine

for name, texture in PRISMARINE.items():
    t = "blocks/" + texture
    textures = {"bottom": t, "top": t, "side": t}
    stairs = name + "_stairs"
    block_model(stairs, "block/stairs", textures)
    block_model(name + "_inner_stairs", "block/inner_stairs", textures)
    block_model(name + "_outer_stairs", "block/outer_stairs", textures)
    blockstate(stairs, vanilla_variants("purpur_stairs", lambda m, n=name: MOD + ":" + m.replace("purpur", n)))
    item_model(stairs)

    slab = name + "_slab"
    block_model(slab, "block/half_slab", textures)
    block_model(slab + "_top", "block/upper_slab", textures)
    block_model(slab + "_double", "block/cube_all", {"all": t})
    blockstate(slab, {"half=bottom": {"model": MOD + ":" + slab}, "half=top": {"model": MOD + ":" + slab + "_top"}})
    blockstate(slab + "_double", {"normal": {"model": MOD + ":" + slab + "_double"}})
    item_model(slab)

# ---------------------------------------------------------------- mushroom blocks (faces from neighbours)

write("models/block/template_single_face.json", {
    "parent": "block/block",
    "elements": [{"from": [0, 0, 0], "to": [16, 16, 16],
                  "faces": {"north": {"texture": "#texture", "cullface": "north"}}}]})
block_model("mushroom_block_inside", MOD + ":block/template_single_face", {"texture": "blocks/mushroom_block_inside", "particle": "blocks/mushroom_block_inside"})
for name, skin in (("red_mushroom_block", "blocks/mushroom_block_skin_red"),
                   ("brown_mushroom_block", "blocks/mushroom_block_skin_brown"),
                   ("mushroom_stem", "blocks/mushroom_block_skin_stem")):
    block_model(name, MOD + ":block/template_single_face", {"texture": skin, "particle": skin})
    block_model(name + "_inventory", "block/cube_all", {"all": skin})
    parts = []
    rotations = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    for face, rot in rotations.items():
        parts.append({"when": {face: "true"}, "apply": dict({"model": MOD + ":" + name, "uvlock": True}, **rot)})
        parts.append({"when": {face: "false"}, "apply": dict({"model": MOD + ":mushroom_block_inside"}, **rot)})
    write("blockstates/%s.json" % name, {"multipart": parts})
    item_model(name, MOD + ":block/" + name + "_inventory")

# ---------------------------------------------------------------- ocean blocks

write("models/block/water_particle.json", {"textures": {"particle": "blocks/water_still"}})
blockstate("bubble_column", {"normal": {"model": MOD + ":water_particle"}})

write("models/block/conduit.json", {"textures": {"particle": tex("blocks/conduit")}})
blockstate("conduit", {"waterlogged=false": {"model": MOD + ":conduit"}, "waterlogged=true": {"model": MOD + ":conduit"}})
item_model("conduit", "builtin/entity", extra={"display": {
    "gui": {"rotation": [30, 45, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [75, 315, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 315, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]}}})

block_model("kelp", "block/cross", {"cross": tex("blocks/kelp")})
block_model("kelp_plant", "block/cross", {"cross": tex("blocks/kelp_plant")})
blockstate("kelp", {"age=%d" % a: {"model": MOD + ":kelp"} for a in range(16)})
blockstate("kelp_plant", {"normal": {"model": MOD + ":kelp_plant"}})
item_model("kelp", layer0=tex("items/kelp"))

block_model("seagrass", MOD + ":block/template_seagrass", {"texture": tex("blocks/seagrass"), "particle": tex("blocks/seagrass")})
block_model("tall_seagrass_bottom", MOD + ":block/template_seagrass", {"texture": tex("blocks/tall_seagrass_bottom"), "particle": tex("blocks/tall_seagrass_bottom")})
block_model("tall_seagrass_top", MOD + ":block/template_seagrass", {"texture": tex("blocks/tall_seagrass_top"), "particle": tex("blocks/tall_seagrass_top")})
blockstate("seagrass", {"normal": {"model": MOD + ":seagrass"}})
blockstate("tall_seagrass", {"half=lower": {"model": MOD + ":tall_seagrass_bottom"}, "half=upper": {"model": MOD + ":tall_seagrass_top"}})
item_model("seagrass", layer0=tex("items/seagrass"))

PICKLE_MODELS = {1: "sea_pickle", 2: "two_sea_pickles", 3: "three_sea_pickles", 4: "four_sea_pickles"}
DEAD_PICKLE_MODELS = {1: "dead_sea_pickle", 2: "two_dead_sea_pickles", 3: "three_dead_sea_pickles", 4: "four_dead_sea_pickles"}
pickle_textures = {"all": tex("blocks/sea_pickle"), "particle": tex("blocks/sea_pickle")}
pickle_variants = {}
for count in range(1, 5):
    for waterlogged in ("true", "false"):
        template = (PICKLE_MODELS if waterlogged == "true" else DEAD_PICKLE_MODELS)[count]
        name = "portium_" + template
        block_model(name, MOD + ":block/" + template, pickle_textures)
        pickle_variants["pickles=%d,waterlogged=%s" % (count, waterlogged)] = {"model": MOD + ":" + name}
blockstate("sea_pickle", pickle_variants)
item_model("sea_pickle", layer0=tex("items/sea_pickle"))

EGG_TEMPLATES = {1: "template_turtle_egg", 2: "template_two_turtle_eggs", 3: "template_three_turtle_eggs", 4: "template_four_turtle_eggs"}
EGG_TEXTURES = {0: "turtle_egg", 1: "turtle_egg_slightly_cracked", 2: "turtle_egg_very_cracked"}
egg_variants = {}
for eggs, template in EGG_TEMPLATES.items():
    for hatch, texture in EGG_TEXTURES.items():
        name = "%s_%s" % (texture, eggs)
        block_model(name, MOD + ":block/" + template, {"all": tex("blocks/" + texture), "particle": tex("blocks/" + texture)})
        egg_variants["eggs=%d,hatch=%d" % (eggs, hatch)] = [dict({"model": MOD + ":" + name}, **({"y": y} if y else {})) for y in (0, 90, 180, 270)]
blockstate("turtle_egg", egg_variants)
item_model("turtle_egg", layer0=tex("items/turtle_egg"))

FAN_ROTATION = {"north": 0, "east": 90, "south": 180, "west": 270}
for coral in CORALS:
    for prefix in ("", "dead_"):
        plant = "%s%s_coral" % (prefix, coral)
        block_model(plant, "block/cross", {"cross": tex("blocks/" + plant)})
        blockstate(plant, {"waterlogged=%s" % w: {"model": MOD + ":" + plant} for w in ("true", "false")})
        item_model(plant, layer0=tex("blocks/" + plant))

        coral_block = "%s%s_coral_block" % (prefix, coral)
        cube_all(coral_block, tex("blocks/" + coral_block))

        fan = "%s%s_coral_fan" % (prefix, coral)
        fan_texture = tex("blocks/" + fan)
        block_model(fan, MOD + ":block/coral_fan", {"fan": fan_texture, "particle": fan_texture})
        block_model(fan.replace("_fan", "_wall_fan"), MOD + ":block/coral_wall_fan", {"fan": fan_texture, "particle": fan_texture})
        fan_variants = {}
        for w in ("true", "false"):
            fan_variants["facing=up,waterlogged=%s" % w] = {"model": MOD + ":" + fan}
            for facing, y in FAN_ROTATION.items():
                v = {"model": MOD + ":" + fan.replace("_fan", "_wall_fan")}
                if y:
                    v["y"] = y
                fan_variants["facing=%s,waterlogged=%s" % (facing, w)] = v
        blockstate(fan, fan_variants)
        item_model(fan, layer0=fan_texture)

# ---------------------------------------------------------------- 1.16 nether blocks (textures come from 1.16.5)

def axis_states(name, model=None):
    model = model or name
    blockstate(name, {"axis=y": {"model": MOD + ":" + model},
                      "axis=z": {"model": MOD + ":" + model, "x": 90},
                      "axis=x": {"model": MOD + ":" + model, "x": 90, "y": 90}})


def column(name, end, side, axis=True):
    block_model(name, "block/cube_column", {"end": end, "side": side})
    if axis:
        axis_states(name)
    else:
        blockstate(name, {"normal": {"model": MOD + ":" + name}})
    item_model(name)


def cross_plant(name, texture, states=None):
    block_model(name, "block/cross", {"cross": texture})
    blockstate(name, states or {"normal": {"model": MOD + ":" + name}})


for kind in ("crimson", "warped"):
    nylium = kind + "_nylium"
    block_model(nylium, "block/cube_bottom_top", {"top": tex("blocks/" + nylium), "bottom": "blocks/netherrack", "side": tex("blocks/" + nylium + "_side")})
    blockstate(nylium, {"normal": {"model": MOD + ":" + nylium}})
    item_model(nylium)
    for prefix in ("", "stripped_"):
        stem = prefix + kind + "_stem"
        column(stem, tex("blocks/" + stem + "_top"), tex("blocks/" + stem))
        column(prefix + kind + "_hyphae", tex("blocks/" + stem), tex("blocks/" + stem))
    for plant in (kind + "_fungus", kind + "_roots"):
        cross_plant(plant, tex("blocks/" + plant))
        item_model(plant, layer0=tex("blocks/" + plant))
cross_plant("nether_sprouts", tex("blocks/nether_sprouts"))
item_model("nether_sprouts", layer0=tex("items/nether_sprouts"))
for vine in ("weeping_vines", "twisting_vines"):
    cross_plant(vine, tex("blocks/" + vine), {"age=%d" % age: {"model": MOD + ":" + vine} for age in range(26)})
    item_model(vine, layer0=tex("blocks/" + vine))
    cross_plant(vine + "_plant", tex("blocks/" + vine + "_plant"))

for name, texture in (("warped_wart_block", "warped_wart_block"), ("shroomlight", "shroomlight"), ("soul_soil", "soul_soil"),
                      ("gilded_blackstone", "gilded_blackstone"), ("nether_gold_ore", "nether_gold_ore"), ("crying_obsidian", "crying_obsidian")):
    cube_all(name, tex("blocks/" + texture))
column("basalt", tex("blocks/basalt_top"), tex("blocks/basalt_side"))
column("polished_basalt", tex("blocks/polished_basalt_top"), tex("blocks/polished_basalt_side"))
column("blackstone", tex("blocks/blackstone_top"), tex("blocks/blackstone"), axis=False)
column("ancient_debris", tex("blocks/ancient_debris_top"), tex("blocks/ancient_debris_side"), axis=False)

# Soul fire: 1.12's fire shapes, always burning on its block, flickering between two frames like fire.
for part in ("floor", "side", "side_alt"):
    for frame in (0, 1):
        block_model("soul_fire_%s%d" % (part, frame), "block/fire_" + part,
                    {"particle": tex("blocks/soul_fire_%d" % frame), "fire": tex("blocks/soul_fire_%d" % frame)})
soul_fire = [{"apply": [{"model": MOD + ":soul_fire_floor0"}, {"model": MOD + ":soul_fire_floor1"}]}]
for y in (0, 90, 180, 270):
    soul_fire.append({"apply": [dict({"model": MOD + ":soul_fire_" + m}, **({"y": y} if y else {}))
                                for m in ("side0", "side1", "side_alt0", "side_alt1")]})
write("blockstates/soul_fire.json", {"multipart": soul_fire})

NETHER_NAMES = ["crimson_nylium", "warped_nylium", "crimson_stem", "warped_stem", "stripped_crimson_stem", "stripped_warped_stem",
                "crimson_hyphae", "warped_hyphae", "stripped_crimson_hyphae", "stripped_warped_hyphae", "warped_wart_block", "shroomlight",
                "crimson_fungus", "warped_fungus", "crimson_roots", "warped_roots", "nether_sprouts", "weeping_vines", "weeping_vines_plant",
                "twisting_vines", "twisting_vines_plant", "soul_soil", "soul_fire", "basalt", "polished_basalt", "blackstone",
                "gilded_blackstone", "nether_gold_ore", "ancient_debris", "crying_obsidian"]

# ---------------------------------------------------------------- items

for name, texture in (("dried_kelp", "items/dried_kelp"), ("heart_of_the_sea", "items/heart_of_the_sea"),
                      ("nautilus_shell", "items/nautilus_shell"), ("phantom_membrane", "items/phantom_membrane"),
                      ("scute", "items/turtle_scute"), ("turtle_helmet", "items/turtle_helmet")):
    item_model(name, layer0=tex(texture))
item_model("debug_stick", "item/handheld", "items/stick")
for bucket in ("cod_bucket", "salmon_bucket", "pufferfish_bucket", "tropical_fish_bucket"):
    item_model(bucket, layer0=tex("items/" + bucket))
item_model("trident_icon", layer0=tex("items/trident"))

TRIDENT_HAND = {
    "thirdperson_righthand": {"rotation": [0, 60, 0], "translation": [11, 17, -2], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [0, 60, 0], "translation": [3, 17, 12], "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [-3, 17, 1], "scale": [1, 1, 1]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [13, 17, 1], "scale": [1, 1, 1]},
    "head": {"rotation": [0, 0, 0], "translation": [8, 20, 8], "scale": [1, 1, 1]},
}
TRIDENT_THROWING = dict(TRIDENT_HAND)
TRIDENT_THROWING["thirdperson_righthand"] = {"rotation": [0, 90, 180], "translation": [8, -17, 9], "scale": [1, 1, 1]}
TRIDENT_THROWING["thirdperson_lefthand"] = {"rotation": [0, 90, 180], "translation": [8, -17, -7], "scale": [1, 1, 1]}
write("models/item/trident.json", {"parent": "builtin/entity", "textures": {"particle": tex("items/trident")},
                                   "display": TRIDENT_HAND,
                                   "overrides": [{"predicate": {"throwing": 1}, "model": MOD + ":item/trident_throwing"}]})
write("models/item/trident_throwing.json", {"parent": "builtin/entity", "textures": {"particle": tex("items/trident")},
                                            "display": TRIDENT_THROWING})

# ---------------------------------------------------------------- recipes

def portium_items(value, found):
    """Every Portium item a recipe names, in order."""
    if isinstance(value, dict):
        name = value.get("item")
        if isinstance(name, str) and name.startswith(MOD + ":") and name not in found:
            found.append(name)
        for v in value.values():
            portium_items(v, found)
    elif isinstance(value, list):
        for v in value:
            portium_items(v, found)
    return found


def recipe(name, data, requires=()):
    # Blocks and items can be switched off in Portium's config; their recipes then don't load.
    conditions = [{"type": "minecraft:item_exists", "item": i} for i in portium_items(data, list(requires))]
    if conditions:
        data = dict(conditions=conditions, **data)
    write("recipes/%s.json" % name, data)


def shaped(name, result, pattern, keys, count=1, group=None, requires=()):
    data = {"type": "minecraft:crafting_shaped", "pattern": pattern, "key": keys, "result": dict(result, count=count)}
    if group:
        data["group"] = group
    recipe(name, data, requires)


def shapeless(name, result, ingredients, count=1, group=None):
    data = {"type": "minecraft:crafting_shapeless", "ingredients": ingredients, "result": dict(result, count=count)}
    if group:
        data["group"] = group
    recipe(name, data)


def item(name, data=None):
    d = {"item": name}
    if data is not None:
        d["data"] = data
    return d


P = lambda n: item(MOD + ":" + n)

shaped("blue_ice", P("blue_ice"), ["###", "###", "###"], {"#": item("minecraft:packed_ice")})
# Part of blue ice: packed ice is what it's made from.
shaped("packed_ice", item("minecraft:packed_ice"), ["###", "###", "###"], {"#": item("minecraft:ice")}, requires=[MOD + ":blue_ice"])
shaped("conduit", P("conduit"), ["###", "#X#", "###"], {"#": P("nautilus_shell"), "X": P("heart_of_the_sea")})
shaped("turtle_helmet", P("turtle_helmet"), ["###", "# #"], {"#": P("scute")})
shaped("dried_kelp_block", P("dried_kelp_block"), ["###", "###", "###"], {"#": P("dried_kelp")})
shapeless("dried_kelp", P("dried_kelp"), [P("dried_kelp_block")], count=9)
shapeless("pumpkin_seeds", item("minecraft:pumpkin_seeds"), [P("pumpkin")], count=4)
shapeless("pumpkin_pie", item("minecraft:pumpkin_pie"), [P("pumpkin"), item("minecraft:sugar"), item("minecraft:egg")])

for wood in WOODS[1:]:
    planks = item("minecraft:planks", WOOD_META[wood])
    shapeless(wood + "_button", P(wood + "_button"), [planks], group="wooden_button")
    shaped(wood + "_pressure_plate", P(wood + "_pressure_plate"), ["##"], {"#": planks}, group="wooden_pressure_plate")
    shaped(wood + "_trapdoor", P(wood + "_trapdoor"), ["###", "###"], {"#": planks}, count=2, group="wooden_trapdoor")

for wood in WOODS:
    log_name, log_meta = LOG_ITEM[wood]
    planks = item("minecraft:planks", WOOD_META[wood])
    shaped(wood + "_wood", P(wood + "_wood"), ["##", "##"], {"#": item(log_name, log_meta)}, count=3, group="bark")
    shaped("stripped_%s_wood" % wood, P("stripped_%s_wood" % wood), ["##", "##"], {"#": P("stripped_%s_log" % wood)}, count=3, group="bark")
    for source in ("stripped_%s_log" % wood, "%s_wood" % wood, "stripped_%s_wood" % wood):
        shapeless("%s_planks_from_%s" % (wood, source), planks, [P(source)], count=4, group="planks")

for name in PRISMARINE:
    source = item("minecraft:prismarine", PRISMARINE_META[name])
    shaped(name + "_stairs", P(name + "_stairs"), ["#  ", "## ", "###"], {"#": source}, count=4)
    # Slab items have subtypes (the half and double slab share them), so 1.12 needs the data value.
    shaped(name + "_slab", item(MOD + ":" + name + "_slab", 0), ["###"], {"#": source}, count=6)

for vanilla_button in ("stone_button", "wooden_button"):
    full = os.path.join(ROOT, "src/main/resources/assets/minecraft/blockstates", vanilla_button + ".json")
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w") as f:
        json.dump({"variants": rotatable_button(vanilla_variants(vanilla_button, lambda m: m))}, f, indent=2)
        f.write("\n")

# ---------------------------------------------------------------- sounds.json

def s(path, **kw):
    entry = {"name": MOD + ":" + path}
    entry.update(kw)
    return entry


def numbered(path, count, **kw):
    return [s("%s%d" % (path, i), **kw) for i in range(1, count + 1)]


SOUNDS = {
    "ambient.underwater.enter": {"sounds": numbered("ambient/underwater/enter", 3, volume=0.8), "subtitle": "subtitles.portium.ambient.underwater.enter"},
    "ambient.underwater.exit": {"sounds": numbered("ambient/underwater/exit", 3, volume=0.5), "subtitle": "subtitles.portium.ambient.underwater.exit"},
    "ambient.underwater.loop": {"sounds": [s("ambient/underwater/underwater_ambience", stream=True, volume=0.65)]},
    "ambient.underwater.loop.additions": {"sounds": numbered("ambient/underwater/additions/bubbles", 6) + numbered("ambient/underwater/additions/water", 2)},
    "ambient.underwater.loop.additions.rare": {"sounds": numbered("ambient/underwater/additions/animal", 1) + numbered("ambient/underwater/additions/bass_whale", 2, volume=0.45)
                                               + numbered("ambient/underwater/additions/crackles", 2) + numbered("ambient/underwater/additions/driplets", 2) + [s("ambient/underwater/additions/earth_crack")]},
    "ambient.underwater.loop.additions.ultra_rare": {"sounds": [s("ambient/underwater/additions/animal2")] + numbered("ambient/underwater/additions/dark", 4, volume=0.7)},
    "block.bubble_column.bubble_pop": {"sounds": numbered("block/bubble_column/bubble", 3, volume=0.1), "subtitle": "subtitles.portium.block.bubble_column.bubble_pop"},
    "block.bubble_column.upwards_ambient": {"sounds": numbered("block/bubble_column/upwards_ambient", 5, volume=0.6), "subtitle": "subtitles.portium.block.bubble_column.upwards_ambient"},
    "block.bubble_column.upwards_inside": {"sounds": [s("block/bubble_column/upwards_inside", volume=0.7)], "subtitle": "subtitles.portium.block.bubble_column.upwards_inside"},
    "block.bubble_column.whirlpool_ambient": {"sounds": numbered("block/bubble_column/whirlpool_ambient", 5, volume=0.6), "subtitle": "subtitles.portium.block.bubble_column.whirlpool_ambient"},
    "block.bubble_column.whirlpool_inside": {"sounds": [s("block/bubble_column/whirlpool_inside", volume=0.7)], "subtitle": "subtitles.portium.block.bubble_column.whirlpool_inside"},
    "block.conduit.activate": {"sounds": [s("block/conduit/activate", volume=0.9)], "subtitle": "subtitles.portium.block.conduit.activate"},
    "block.conduit.ambient": {"sounds": [s("block/conduit/ambient", attenuation_distance=8)], "subtitle": "subtitles.portium.block.conduit.ambient"},
    "block.conduit.ambient.short": {"sounds": numbered("block/conduit/short", 9, attenuation_distance=8), "subtitle": "subtitles.portium.block.conduit.ambient"},
    "block.conduit.attack.target": {"sounds": numbered("block/conduit/attack", 3), "subtitle": "subtitles.portium.block.conduit.attack.target"},
    "block.conduit.deactivate": {"sounds": [s("block/conduit/deactivate", volume=0.7)], "subtitle": "subtitles.portium.block.conduit.deactivate"},
    "block.coral_block.break": {"sounds": numbered("dig/coral", 4), "subtitle": "subtitles.block.generic.break"},
    "block.coral_block.fall": {"sounds": numbered("step/coral", 6)},
    "block.coral_block.hit": {"sounds": numbered("step/coral", 6), "subtitle": "subtitles.block.generic.hit"},
    "block.coral_block.place": {"sounds": numbered("dig/coral", 4), "subtitle": "subtitles.block.generic.place"},
    "block.coral_block.step": {"sounds": numbered("step/coral", 6), "subtitle": "subtitles.block.generic.footsteps"},
    "block.wet_grass.break": {"sounds": numbered("dig/wet_grass", 4, volume=0.8), "subtitle": "subtitles.block.generic.break"},
    "block.wet_grass.fall": {"sounds": numbered("step/wet_grass", 6)},
    "block.wet_grass.hit": {"sounds": numbered("step/wet_grass", 6), "subtitle": "subtitles.block.generic.hit"},
    "block.wet_grass.place": {"sounds": numbered("dig/wet_grass", 4, volume=0.8), "subtitle": "subtitles.block.generic.place"},
    "block.wet_grass.step": {"sounds": numbered("step/wet_grass", 6), "subtitle": "subtitles.block.generic.footsteps"},
    "block.pumpkin.carve": {"sounds": numbered("block/pumpkin/carve", 2), "subtitle": "subtitles.portium.block.pumpkin.carve"},
    "entity.turtle.egg_break": {"sounds": numbered("mob/turtle/egg/egg_break", 2), "subtitle": "subtitles.portium.entity.turtle.egg_break"},
    "entity.turtle.egg_crack": {"sounds": numbered("mob/turtle/egg/egg_crack", 5), "subtitle": "subtitles.portium.entity.turtle.egg_crack"},
    "entity.turtle.egg_hatch": {"sounds": numbered("mob/turtle/baby/egg_hatched", 3), "subtitle": "subtitles.portium.entity.turtle.egg_hatch"},
    "entity.squid.squirt": {"sounds": numbered("entity/squid/squirt", 3), "subtitle": "subtitles.portium.entity.squid.squirt"},
    "entity.husk.converted_to_zombie": {"sounds": numbered("mob/husk/convert", 2, volume=0.8), "subtitle": "subtitles.portium.entity.husk.converted_to_zombie"},
    "item.armor.equip_turtle": {"sounds": [s("mob/turtle/armor"), s("mob/turtle/armor", pitch=0.85), s("mob/turtle/armor", pitch=1.1)], "subtitle": "subtitles.portium.item.armor.equip_turtle"},
    "item.axe.strip": {"sounds": numbered("item/axe/strip", 4, volume=0.9), "subtitle": "subtitles.portium.item.axe.strip"},
    "item.trident.hit": {"sounds": numbered("item/trident/pierce", 3), "subtitle": "subtitles.portium.item.trident.hit"},
    "item.trident.hit_ground": {"sounds": numbered("item/trident/ground_impact", 4, volume=0.9), "subtitle": "subtitles.portium.item.trident.hit_ground"},
    "item.trident.return": {"sounds": numbered("item/trident/return", 3, volume=0.8), "subtitle": "subtitles.portium.item.trident.return"},
    "item.trident.riptide_1": {"sounds": [s("item/trident/riptide1")], "subtitle": "subtitles.portium.item.trident.riptide"},
    "item.trident.riptide_2": {"sounds": [s("item/trident/riptide2")], "subtitle": "subtitles.portium.item.trident.riptide"},
    "item.trident.riptide_3": {"sounds": [s("item/trident/riptide3")], "subtitle": "subtitles.portium.item.trident.riptide"},
    "item.trident.throw": {"sounds": numbered("item/trident/throw", 2), "subtitle": "subtitles.portium.item.trident.throw"},
    "item.trident.thunder": {"sounds": numbered("item/trident/thunder", 2), "subtitle": "subtitles.portium.item.trident.thunder"},
}
def fish_sounds(prefix, flop_volume=0.3):
    return {
        prefix + ".death": {"sounds": numbered("entity/fish/hurt", 4), "subtitle": "subtitles.portium.entity.fish.death"},
        prefix + ".flop": {"sounds": numbered("entity/fish/flop", 4, volume=flop_volume), "subtitle": "subtitles.portium.entity.fish.flop"},
        prefix + ".hurt": {"sounds": numbered("entity/fish/hurt", 4), "subtitle": "subtitles.portium.entity.fish.hurt"},
    }


for prefix in ("entity.cod", "entity.salmon", "entity.tropical_fish"):
    SOUNDS.update(fish_sounds(prefix))
SOUNDS.update({
    "entity.fish.swim": {"sounds": numbered("entity/fish/swim", 7), "subtitle": "subtitles.portium.entity.fish.swim"},
    "entity.puffer_fish.blow_out": {"sounds": numbered("entity/pufferfish/blow_out", 2), "subtitle": "subtitles.portium.entity.puffer_fish.blow_out"},
    "entity.puffer_fish.blow_up": {"sounds": numbered("entity/pufferfish/blow_up", 2), "subtitle": "subtitles.portium.entity.puffer_fish.blow_up"},
    "entity.puffer_fish.death": {"sounds": numbered("entity/pufferfish/death", 2), "subtitle": "subtitles.portium.entity.puffer_fish.death"},
    "entity.puffer_fish.flop": {"sounds": numbered("entity/pufferfish/flop", 4, volume=0.3), "subtitle": "subtitles.portium.entity.puffer_fish.flop"},
    "entity.puffer_fish.hurt": {"sounds": numbered("entity/pufferfish/hurt", 2), "subtitle": "subtitles.portium.entity.puffer_fish.hurt"},
    "entity.puffer_fish.sting": {"sounds": numbered("entity/pufferfish/sting", 2), "subtitle": "subtitles.portium.entity.puffer_fish.sting"},

    "entity.dolphin.ambient": {"sounds": numbered("mob/dolphin/idle", 6), "subtitle": "subtitles.portium.entity.dolphin.ambient"},
    "entity.dolphin.ambient_water": {"sounds": numbered("mob/dolphin/idle_water", 10, volume=0.8), "subtitle": "subtitles.portium.entity.dolphin.ambient_water"},
    "entity.dolphin.attack": {"sounds": numbered("mob/dolphin/attack", 3), "subtitle": "subtitles.portium.entity.dolphin.attack"},
    "entity.dolphin.death": {"sounds": numbered("mob/dolphin/death", 2), "subtitle": "subtitles.portium.entity.dolphin.death"},
    "entity.dolphin.eat": {"sounds": numbered("mob/dolphin/eat", 3), "subtitle": "subtitles.portium.entity.dolphin.eat"},
    "entity.dolphin.hurt": {"sounds": numbered("mob/dolphin/hurt", 3), "subtitle": "subtitles.portium.entity.dolphin.hurt"},
    "entity.dolphin.jump": {"sounds": numbered("mob/dolphin/jump", 3, volume=0.75), "subtitle": "subtitles.portium.entity.dolphin.jump"},
    "entity.dolphin.play": {"sounds": numbered("mob/dolphin/play", 2), "subtitle": "subtitles.portium.entity.dolphin.play"},
    "entity.dolphin.splash": {"sounds": numbered("mob/dolphin/splash", 3), "subtitle": "subtitles.portium.entity.dolphin.splash"},
    "entity.dolphin.swim": {"sounds": numbered("mob/dolphin/swim", 4), "subtitle": "subtitles.portium.entity.dolphin.swim"},

    "entity.turtle.ambient_land": {"sounds": numbered("mob/turtle/idle", 3, volume=0.8), "subtitle": "subtitles.portium.entity.turtle.ambient_land"},
    "entity.turtle.death": {"sounds": numbered("mob/turtle/death", 3), "subtitle": "subtitles.portium.entity.turtle.death"},
    "entity.turtle.death_baby": {"sounds": numbered("mob/turtle/baby/death", 2), "subtitle": "subtitles.portium.entity.turtle.death_baby"},
    "entity.turtle.hurt": {"sounds": numbered("mob/turtle/hurt", 5), "subtitle": "subtitles.portium.entity.turtle.hurt"},
    "entity.turtle.hurt_baby": {"sounds": numbered("mob/turtle/baby/hurt", 2), "subtitle": "subtitles.portium.entity.turtle.hurt_baby"},
    "entity.turtle.lay_egg": {"sounds": numbered("mob/turtle/egg/drop_egg", 2), "subtitle": "subtitles.portium.entity.turtle.lay_egg"},
    "entity.turtle.shamble": {"sounds": numbered("mob/turtle/walk", 5), "subtitle": "subtitles.portium.entity.turtle.shamble"},
    "entity.turtle.shamble_baby": {"sounds": numbered("mob/turtle/baby/shamble", 4), "subtitle": "subtitles.portium.entity.turtle.shamble_baby"},
    "entity.turtle.swim": {"sounds": numbered("mob/turtle/swim/swim", 5), "subtitle": "subtitles.portium.entity.turtle.swim"},

    "entity.drowned.ambient": {"sounds": numbered("mob/drowned/idle", 5, volume=0.9), "subtitle": "subtitles.portium.entity.drowned.ambient"},
    "entity.drowned.ambient_water": {"sounds": numbered("mob/drowned/water/idle", 4, volume=0.9), "subtitle": "subtitles.portium.entity.drowned.ambient_water"},
    "entity.drowned.death": {"sounds": numbered("mob/drowned/death", 2, volume=0.9), "subtitle": "subtitles.portium.entity.drowned.death"},
    "entity.drowned.death_water": {"sounds": numbered("mob/drowned/water/death", 2, volume=0.9), "subtitle": "subtitles.portium.entity.drowned.death"},
    "entity.drowned.hurt": {"sounds": numbered("mob/drowned/hurt", 3, volume=0.9), "subtitle": "subtitles.portium.entity.drowned.hurt"},
    "entity.drowned.hurt_water": {"sounds": numbered("mob/drowned/water/hurt", 3, volume=0.9), "subtitle": "subtitles.portium.entity.drowned.hurt"},
    "entity.drowned.shoot": {"sounds": numbered("item/trident/throw", 2), "subtitle": "subtitles.portium.entity.drowned.shoot"},
    "entity.drowned.step": {"sounds": numbered("mob/drowned/step", 5), "subtitle": "subtitles.portium.entity.drowned.step"},
    # Drowned swim with the generic swimming sounds 1.12 already has.
    "entity.drowned.swim": {"sounds": [{"name": "minecraft:liquid/swim%d" % i} for i in range(1, 5)], "subtitle": "subtitles.portium.entity.drowned.swim"},
    "entity.zombie.converted_to_drowned": {"sounds": numbered("mob/drowned/convert", 3), "subtitle": "subtitles.portium.entity.zombie.converted_to_drowned"},
    "entity.zombie.destroy_egg": {"sounds": numbered("mob/turtle/egg/jump_egg", 4), "subtitle": "subtitles.portium.entity.zombie.destroy_egg"},

    "entity.phantom.ambient": {"sounds": numbered("mob/phantom/idle", 5, volume=0.8), "subtitle": "subtitles.portium.entity.phantom.ambient"},
    # Parrots imitating 1.13's mobs, pitched up like 1.12's own imitations; the subtitles are 1.13's keys.
    "entity.parrot.imitate.drowned": {"sounds": numbered("mob/drowned/idle", 5, pitch=1.8, volume=0.6), "subtitle": "subtitles.entity.parrot.imitate.drowned"},
    "entity.parrot.imitate.phantom": {"sounds": numbered("mob/phantom/idle", 5, pitch=1.7, volume=0.6), "subtitle": "subtitles.entity.parrot.imitate.phantom"},
    "entity.phantom.bite": {"sounds": numbered("mob/phantom/bite", 2), "subtitle": "subtitles.portium.entity.phantom.bite"},
    "entity.phantom.death": {"sounds": numbered("mob/phantom/death", 3), "subtitle": "subtitles.portium.entity.phantom.death"},
    "entity.phantom.flap": {"sounds": numbered("mob/phantom/flap", 6, volume=0.9), "subtitle": "subtitles.portium.entity.phantom.flap"},
    "entity.phantom.hurt": {"sounds": numbered("mob/phantom/hurt", 3), "subtitle": "subtitles.portium.entity.phantom.hurt"},
    "entity.phantom.swoop": {"sounds": numbered("mob/phantom/swoop", 4, volume=0.7), "subtitle": "subtitles.portium.entity.phantom.swoop"},

    "item.bucket.empty_fish": {"sounds": numbered("item/bucket/empty_fish", 3), "subtitle": "subtitles.portium.item.bucket.empty_fish"},
    "item.bucket.fill_fish": {"sounds": numbered("item/bucket/fill_fish", 3), "subtitle": "subtitles.portium.item.bucket.fill_fish"},
})

# ---------------------------------------------------------------- 1.16.5 sounds
# Events taken whole from 1.16.5's own sounds.json (./gradlew download116 fetches it into build/ref116), moved into
# Portium's namespace. Their files are listed in sounds_1_16.txt, which Portium has AssetMover fetch from 1.16.5.

V116_BLOCK_SOUND_SETS = ["nylium", "stem", "wart_block", "shroomlight", "fungus", "roots", "weeping_vines", "soul_soil", "soul_sand",
                         "netherrack", "basalt", "ancient_debris", "nether_gold_ore", "gilded_blackstone", "nether_ore", "nether_sprouts"]
V116_NETHER_BIOMES = ["basalt_deltas", "crimson_forest", "nether_wastes", "soul_sand_valley", "warped_forest"]
V116_EVENTS = (["block.%s.%s" % (block, sound) for block in V116_BLOCK_SOUND_SETS for sound in ("break", "step", "place", "hit", "fall")]
               + ["ambient.%s.%s" % (biome, kind) for biome in V116_NETHER_BIOMES for kind in ("loop", "mood", "additions")]
               + ["music.nether." + biome for biome in V116_NETHER_BIOMES])
# Sounds 1.12 already has, played from its own assets instead of fetched again.
V112_SOUNDS = {"music/game/nether/nether%d" % i for i in range(1, 5)}


def v116_sounds(events):
    path = os.path.join(ROOT, "build/ref116/sounds-1.16.5.json")
    if not os.path.isfile(path):
        sys.exit("Missing %s: run ./gradlew download116 first." % path)
    with open(path) as f:
        vanilla = json.load(f)
    files = set()
    for event in events:
        entry = dict(vanilla[event])
        sounds = []
        for sound in entry["sounds"]:
            sound = {"name": sound} if isinstance(sound, str) else dict(sound)
            if sound.get("type") == "event":
                sound["name"] = MOD + ":" + sound["name"]
            elif sound["name"] in V112_SOUNDS:
                sound["name"] = "minecraft:" + sound["name"]
            else:
                files.add(sound["name"])
                sound["name"] = MOD + ":" + sound["name"]
            sounds.append(sound["name"] if len(sound) == 1 else sound)
        entry["sounds"] = sounds
        SOUNDS[event] = entry
    write("sounds_1_16.txt", "".join(name + "\n" for name in sorted(files)))


v116_sounds(V116_EVENTS)
write("sounds.json", SOUNDS)

print("wrote %d files" % len(written))

# ---------------------------------------------------------------- lang

def title(name):
    small = {"of", "the"}
    words = name.split("_")
    return " ".join(w if (i and w in small) else w.capitalize() for i, w in enumerate(words))


lang = {}
BLOCK_NAMES = {
    "blue_ice": "Blue Ice", "bubble_column": "Bubble Column", "conduit": "Conduit", "dried_kelp_block": "Dried Kelp Block",
    "kelp": "Kelp", "kelp_plant": "Kelp Plant", "seagrass": "Seagrass", "tall_seagrass": "Tall Seagrass", "sea_pickle": "Sea Pickle",
    "turtle_egg": "Turtle Egg", "pumpkin": "Pumpkin", "red_mushroom_block": "Red Mushroom Block",
    "brown_mushroom_block": "Brown Mushroom Block", "mushroom_stem": "Mushroom Stem", "smooth_stone": "Smooth Stone",
    "smooth_sandstone": "Smooth Sandstone", "smooth_red_sandstone": "Smooth Red Sandstone", "smooth_quartz": "Smooth Quartz",
    "prismarine_stairs": "Prismarine Stairs", "prismarine_brick_stairs": "Prismarine Brick Stairs",
    "dark_prismarine_stairs": "Dark Prismarine Stairs", "prismarine_slab": "Prismarine Slab",
    "prismarine_brick_slab": "Prismarine Brick Slab", "dark_prismarine_slab": "Dark Prismarine Slab",
}
for name in NETHER_NAMES:
    BLOCK_NAMES[name] = title(name)
for wood in WOODS:
    w = title(wood)
    if wood != "oak":
        BLOCK_NAMES[wood + "_button"] = w + " Button"
        BLOCK_NAMES[wood + "_pressure_plate"] = w + " Pressure Plate"
        BLOCK_NAMES[wood + "_trapdoor"] = w + " Trapdoor"
    BLOCK_NAMES["stripped_%s_log" % wood] = "Stripped %s Log" % w
    BLOCK_NAMES["%s_wood" % wood] = "%s Wood" % w
    BLOCK_NAMES["stripped_%s_wood" % wood] = "Stripped %s Wood" % w
for coral in CORALS:
    c = title(coral)
    for prefix, label in (("", ""), ("dead_", "Dead ")):
        BLOCK_NAMES["%s%s_coral" % (prefix, coral)] = "%s%s Coral" % (label, c)
        BLOCK_NAMES["%s%s_coral_block" % (prefix, coral)] = "%s%s Coral Block" % (label, c)
        BLOCK_NAMES["%s%s_coral_fan" % (prefix, coral)] = "%s%s Coral Fan" % (label, c)
for name, label in BLOCK_NAMES.items():
    lang["tile.%s.%s.name" % (MOD, name)] = label
for name, label in (("dried_kelp", "Dried Kelp"), ("heart_of_the_sea", "Heart of the Sea"), ("nautilus_shell", "Nautilus Shell"),
                    ("phantom_membrane", "Phantom Membrane"), ("scute", "Scute"), ("turtle_helmet", "Turtle Shell"),
                    ("trident", "Trident"), ("debug_stick", "Debug Stick")):
    lang["item.%s.%s.name" % (MOD, name)] = label

lang["item.portium.debug_stick.tooltip"] = "Left click: select property, right click: change value"
lang["item.portium.debug_stick.empty"] = "%s has no properties"
lang["item.portium.debug_stick.select"] = "selected \"%s\" (%s)"
lang["item.portium.debug_stick.update"] = "\"%s\" to %s"

for name, label in (("conduit_power", "Conduit Power"), ("dolphins_grace", "Dolphin's Grace"), ("slow_falling", "Slow Falling")):
    lang["effect.%s.%s" % (MOD, name)] = label
for base, label in (("slow_falling", "Slow Falling"), ("turtle_master", "the Turtle Master")):
    lang["potion.effect.%s.%s" % (MOD, base)] = "Potion of " + label
    lang["splash_potion.effect.%s.%s" % (MOD, base)] = "Splash Potion of " + label
    lang["lingering_potion.effect.%s.%s" % (MOD, base)] = "Lingering Potion of " + label
    lang["tipped_arrow.effect.%s.%s" % (MOD, base)] = "Arrow of " + label

for name, label in (("impaling", "Impaling"), ("loyalty", "Loyalty"), ("riptide", "Riptide"), ("channeling", "Channeling")):
    lang["enchantment.%s.%s" % (MOD, name)] = label
    lang["enchantment.%s.%s.desc" % (MOD, name)] = {
        "impaling": "Deals extra damage to aquatic mobs.",
        "loyalty": "A thrown trident returns to its thrower.",
        "riptide": "Throwing the trident launches you instead, but only in water or rain.",
        "channeling": "A thrown trident summons lightning on its target during thunderstorms."}[name]

lang["entity.portium.trident.name"] = "Trident"
for name, label in (("cod", "Cod"), ("salmon", "Salmon"), ("pufferfish", "Pufferfish"), ("tropical_fish", "Tropical Fish"),
                    ("turtle", "Turtle"), ("dolphin", "Dolphin"), ("drowned", "Drowned"), ("phantom", "Phantom")):
    lang["entity.%s.%s.name" % (MOD, name)] = label
for name, label in (("cod_bucket", "Bucket of Cod"), ("salmon_bucket", "Bucket of Salmon"), ("pufferfish_bucket", "Bucket of Pufferfish"),
                    ("tropical_fish_bucket", "Bucket of Tropical Fish")):
    lang["item.%s.%s.name" % (MOD, name)] = label
for i, label in enumerate(("Anemone", "Black Tang", "Blue Tang", "Butterflyfish", "Cichlid", "Clownfish", "Cotton Candy Betta", "Dottyback",
                           "Emperor Red Snapper", "Goatfish", "Moorish Idol", "Ornate Butterflyfish", "Parrotfish", "Queen Angelfish",
                           "Red Cichlid", "Red Lipped Blenny", "Red Snapper", "Threadfin", "Tomato Clownfish", "Triggerfish",
                           "Yellowtail Parrotfish", "Yellow Tang")):
    lang["entity.%s.tropical_fish.predefined.%d" % (MOD, i)] = label
for kind in ("kob", "sunstreak", "snooper", "dasher", "brinely", "spotty", "flopper", "stripey", "glitter", "blockfish", "betty", "clayfish"):
    lang["entity.%s.tropical_fish.type.%s" % (MOD, kind)] = kind.capitalize()
# 1.13 renamed the vanilla fish.
lang["item.fish.cod.raw.name"] = "Raw Cod"
lang["item.fish.cod.cooked.name"] = "Cooked Cod"
lang["item.fish.clownfish.raw.name"] = "Tropical Fish"
lang["stat.timeSinceRest"] = "Time Since Last Rest"
lang["death.attack.dryout"] = "%1$s died from dehydration"
lang["death.attack.dryout.player"] = "%1$s died from dehydration whilst trying to escape %2$s"
lang["filled_map.buried_treasure"] = "Buried Treasure Map"
lang["death.attack.trident"] = "%1$s was impaled by %2$s"
lang["death.attack.trident.item"] = "%1$s was impaled by %2$s with %3$s"
lang["death.attack.portium.bad_respawn_point.message"] = "%1$s was killed by %2$s"
lang["death.attack.portium.bad_respawn_point.link"] = "Intentional Game Design"
# The Create World screen's world generation choice ("World Generation: 1.13.2").
lang["selectWorld.portium.generation"] = "World Generation:"
# Opening a world saved by 1.13.
lang["selectWorld.portium.modern.title"] = "This world was saved in %s"
lang["selectWorld.portium.modern.info"] = "Portium converts it to 1.12.2's format as it is played. Back it up first to keep a copy as %s saved it."
lang["selectWorld.portium.modern.backup"] = "Back Up and Load"
lang["selectWorld.portium.modern.load"] = "Load Without Backup"
lang["selectWorld.portium.modern.backingUp"] = "Backing up the world..."
lang["selectWorld.portium.modern.backupFailed"] = "Couldn't back up the world: %s"
lang["selectWorld.portium.modern.disabled"] = "Opening worlds saved by newer versions is switched off in Portium's config (compatibility > modernWorlds)."

SUBTITLES = {
    "ambient.underwater.enter": "Diving", "ambient.underwater.exit": "Air bubbles pop",
    "block.bubble_column.bubble_pop": "Bubbles pop", "block.bubble_column.upwards_ambient": "Bubbles flow",
    "block.bubble_column.upwards_inside": "Bubbles woosh", "block.bubble_column.whirlpool_ambient": "Bubbles whirl",
    "block.bubble_column.whirlpool_inside": "Bubbles zoom", "block.conduit.activate": "Conduit activates",
    "block.conduit.ambient": "Conduit pulses", "block.conduit.attack.target": "Conduit attacks",
    "block.conduit.deactivate": "Conduit deactivates", "block.pumpkin.carve": "Shears carve",
    "entity.turtle.egg_break": "Turtle Egg breaks", "entity.turtle.egg_crack": "Turtle Egg cracks",
    "entity.turtle.egg_hatch": "Turtle Egg hatches", "entity.squid.squirt": "Squid shoots ink",
    "entity.husk.converted_to_zombie": "Husk converts to Zombie", "item.armor.equip_turtle": "Turtle Shell thunks",
    "item.axe.strip": "Axe strips", "item.trident.hit": "Trident stabs", "item.trident.hit_ground": "Trident vibrates",
    "item.trident.return": "Trident returns", "item.trident.riptide": "Trident zooms", "item.trident.throw": "Trident clangs",
    "item.trident.thunder": "Trident thunder cracks",
    "entity.fish.death": "Fish dies", "entity.fish.flop": "Fish flops", "entity.fish.hurt": "Fish hurts", "entity.fish.swim": "Splashes",
    "entity.puffer_fish.blow_out": "Pufferfish deflates", "entity.puffer_fish.blow_up": "Pufferfish inflates",
    "entity.puffer_fish.death": "Pufferfish dies", "entity.puffer_fish.flop": "Pufferfish flops", "entity.puffer_fish.hurt": "Pufferfish hurts",
    "entity.puffer_fish.sting": "Pufferfish stings",
    "entity.dolphin.ambient": "Dolphin chirps", "entity.dolphin.ambient_water": "Dolphin whistles", "entity.dolphin.attack": "Dolphin attacks",
    "entity.dolphin.death": "Dolphin dies", "entity.dolphin.eat": "Dolphin eats", "entity.dolphin.hurt": "Dolphin hurts",
    "entity.dolphin.jump": "Dolphin jumps", "entity.dolphin.play": "Dolphin plays", "entity.dolphin.splash": "Dolphin splashes",
    "entity.dolphin.swim": "Dolphin swims",
    "entity.turtle.ambient_land": "Turtle chirps", "entity.turtle.death": "Turtle dies", "entity.turtle.death_baby": "Turtle baby dies",
    "entity.turtle.hurt": "Turtle hurts", "entity.turtle.hurt_baby": "Turtle baby hurts", "entity.turtle.lay_egg": "Turtle lays egg",
    "entity.turtle.shamble": "Turtle shambles", "entity.turtle.shamble_baby": "Turtle baby shambles", "entity.turtle.swim": "Turtle swims",
    "entity.drowned.ambient": "Drowned gurgles", "entity.drowned.ambient_water": "Drowned gurgles", "entity.drowned.death": "Drowned dies",
    "entity.drowned.hurt": "Drowned hurts", "entity.drowned.shoot": "Drowned throws Trident", "entity.drowned.step": "Drowned steps",
    "entity.drowned.swim": "Drowned swims", "entity.zombie.converted_to_drowned": "Zombie converts to Drowned",
    "entity.zombie.destroy_egg": "Turtle Egg stomped",
    "entity.phantom.ambient": "Phantom screeches", "entity.phantom.bite": "Phantom bites", "entity.phantom.death": "Phantom dies",
    "entity.phantom.flap": "Phantom flaps", "entity.phantom.hurt": "Phantom hurts", "entity.phantom.swoop": "Phantom swoops",
    "item.bucket.empty_fish": "Bucket empties", "item.bucket.fill_fish": "Fish captured",
}
for key_name, label in SUBTITLES.items():
    lang["subtitles.%s.%s" % (MOD, key_name)] = label

write("lang/en_us.lang", "".join("%s=%s\n" % (k, v) for k, v in sorted(lang.items())))
print("lang entries: %d" % len(lang))
