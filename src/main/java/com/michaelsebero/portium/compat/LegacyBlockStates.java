package com.michaelsebero.portium.compat;

import com.google.common.base.Optional;
import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Translates 1.13+ ("flattened") block states into 1.12 ones, so structure templates and datapack content written for
 * 1.13 can be used. 1.13 gave every block variant its own name ({@code minecraft:mossy_stone_bricks}); 1.12 stores the
 * variant in metadata ({@code minecraft:stonebrick[variant=mossy_stonebrick]}). Blocks Portium adds keep their 1.13
 * names under the {@code portium} namespace and are found automatically. Properties carry over by name where the
 * 1.12 block has them; 1.13-only properties such as {@code waterlogged} are dropped unless the block supports them.
 */
public final class LegacyBlockStates {

    private static final Map<String, Function<Map<String, String>, IBlockState>> MAPPINGS = new HashMap<>();
    /** Set once the tables are complete; chunks are converted on Forge's chunk-loading threads. */
    private static volatile boolean ready;
    private static boolean initializing;
    private static final Set<String> WARNED = Collections.synchronizedSet(new HashSet<>());
    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
            "cyan", "purple", "blue", "brown", "green", "red", "black"};
    private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};

    /** 1.12 properties that pick a 1.13 block out of a shared 1.12 block, rather than describing its state. */
    private static final Set<String> VARIANT_KEYS = new HashSet<>(java.util.Arrays.asList("variant", "color", "type", "wet", "damage"));

    /** Whether the name is one 1.13 renamed or split, as opposed to one that kept its 1.12 meaning. */
    static boolean isExplicitlyMapped(String name) {
        if (!ready) init();
        return MAPPINGS.containsKey(new ResourceLocation(name).toString());
    }

    /** Every 1.13 block name with an equivalent here, for command suggestions. */
    public static java.util.SortedSet<String> modernNames() {
        if (!ready) init();
        java.util.SortedSet<String> names = new java.util.TreeSet<>(MAPPINGS.keySet());
        for (ResourceLocation id : ForgeRegistries.BLOCKS.getKeys()) {
            String modern = (Tags.MOD_ID.equals(id.getNamespace()) ? "minecraft:" + id.getPath() : id.toString());
            if (exists(modern)) names.add(modern);
        }
        return names;
    }

    /** Whether a 1.13 name means anything in 1.12 or Portium. */
    public static boolean exists(String name) {
        return convert(name, Collections.emptyMap(), false).getBlock() != Blocks.AIR || new ResourceLocation(name).getPath().endsWith("air");
    }

    /** The block (and variant) a 1.13 name stands for, for block predicates and tags; null if unknown. */
    @javax.annotation.Nullable
    public static LegacyBlock resolve(String name) {
        IBlockState state = convert(name, Collections.emptyMap(), false);
        if (state.getBlock() == Blocks.AIR && !new ResourceLocation(name).getPath().endsWith("air")) return null;
        Map<IProperty<?>, Comparable<?>> variant = new HashMap<>();
        for (IProperty<?> property : state.getPropertyKeys()) {
            if (VARIANT_KEYS.contains(property.getName())) variant.put(property, state.getValue(property));
        }
        return new LegacyBlock(state.getBlock(), variant.isEmpty() ? Collections.emptyMap() : variant);
    }

    /** The 1.12 state for a 1.13 block name and properties; air if the block doesn't exist in 1.12 or Portium. */
    public static IBlockState convert(String name, Map<String, String> properties) {
        return convert(name, properties, true);
    }

    private static IBlockState convert(String name, Map<String, String> properties, boolean warn) {
        if (!ready) init();
        ResourceLocation id = new ResourceLocation(name);
        if (properties.containsKey("face")) properties = unfold(id.getPath(), properties);
        Function<Map<String, String>, IBlockState> mapping = MAPPINGS.get(id.toString());
        if (mapping != null) return normalize(mapping.apply(properties));
        if ("minecraft".equals(id.getNamespace())) {
            Block portium = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(Tags.MOD_ID, id.getPath()));
            if (portium != null && portium != Blocks.AIR) return normalize(copy(portium.getDefaultState(), properties));
        }
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        if (block != null && (block != Blocks.AIR || "air".equals(id.getPath()))) return normalize(copy(block.getDefaultState(), properties));
        if (warn && WARNED.add(id.toString())) Portium.LOGGER.warn("No 1.12 equivalent for block {}; using air", id);
        return Blocks.AIR.getDefaultState();
    }

    /**
     * 1.13 buttons and levers say which face they are on ({@code floor}, {@code wall}, {@code ceiling}) separately
     * from their horizontal facing. 1.12 buttons face up or down on floors and ceilings (with Portium's
     * {@code rotated} flag for east-west ones), and 1.12 levers have combined values like {@code up_x}.
     */
    private static Map<String, String> unfold(String path, Map<String, String> properties) {
        String face = properties.get("face");
        if ("wall".equals(face)) return properties;
        Map<String, String> unfolded = new HashMap<>(properties);
        boolean alongX = "east".equals(properties.get("facing")) || "west".equals(properties.get("facing"));
        boolean floor = "floor".equals(face);
        if (path.endsWith("lever")) {
            unfolded.put("facing", (floor ? "up_" : "down_") + (alongX ? "x" : "z"));
        } else {
            unfolded.put("facing", floor ? "up" : "down");
            unfolded.put("rotated", String.valueOf(alongX));
        }
        return unfolded;
    }

    /** Copies every property the 1.12 block shares with the 1.13 one (by name), ignoring the rest. */
    public static IBlockState copy(IBlockState state, Map<String, String> properties) {
        for (IProperty<?> property : state.getPropertyKeys()) {
            String value = properties.get(property.getName());
            if (value != null) state = with(state, property, value);
        }
        return state;
    }

    private static <T extends Comparable<T>> IBlockState with(IBlockState state, IProperty<T> property, String value) {
        Optional<T> parsed = property.parseValue(value);
        return parsed.isPresent() ? state.withProperty(property, parsed.get()) : state;
    }

    /** Drops properties 1.12 only computes when rendering (fence connections, stair shapes), which the world can't store. */
    private static IBlockState normalize(IBlockState state) {
        Block block = state.getBlock();
        return block.getStateFromMeta(block.getMetaFromState(state));
    }

    private static IBlockState meta(Block block, int meta) {
        return block.getStateFromMeta(meta);
    }

    private static void fixed(String name, IBlockState state) {
        MAPPINGS.put("minecraft:" + name, properties -> state);
    }

    private static void fixed(String name, Block block, int meta) {
        fixed(name, meta(block, meta));
    }

    /** A 1.13 block that is a 1.12 block plus a fixed variant, with any other properties copied. */
    private static void variant(String name, Block block, int meta) {
        IBlockState base = meta(block, meta);
        MAPPINGS.put("minecraft:" + name, properties -> copy(base, properties));
    }

    /** Portium's block of this 1.13+ name if it has one, else 1.12's stand-in; either with the properties both share. */
    private static IBlockState ownOr(String name, Map<String, String> properties, Block fallback) {
        Block portium = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(Tags.MOD_ID, name));
        Block block = portium != null && portium != Blocks.AIR ? portium : fallback;
        return copy(block.getDefaultState(), properties);
    }

    private static void rename(String name, Block block) {
        MAPPINGS.put("minecraft:" + name, properties -> copy(block.getDefaultState(), properties));
    }

    /** 1.13 slabs: {@code type=bottom|top|double}; 1.12 uses {@code half} and a separate double-slab block. */
    private static void slab(String name, Block single, Block doubleSlab, int variantMeta) {
        MAPPINGS.put("minecraft:" + name, properties -> {
            String type = properties.getOrDefault("type", "bottom");
            if ("double".equals(type)) return meta(doubleSlab, variantMeta);
            return meta(single, variantMeta | ("top".equals(type) ? 8 : 0));
        });
    }

    /** 1.13 logs keep their axis; 1.12 splits the six woods over two blocks. */
    private static void log(String name, Block block, int variantMeta) {
        MAPPINGS.put("minecraft:" + name, properties -> {
            String axis = properties.getOrDefault("axis", "y");
            int axisBits = "x".equals(axis) ? 4 : "z".equals(axis) ? 8 : 0;
            return meta(block, variantMeta | axisBits);
        });
    }

    private static synchronized void init() {
        if (ready || initializing) return;
        initializing = true;
        fixed("air", Blocks.AIR.getDefaultState());
        fixed("cave_air", Blocks.AIR.getDefaultState());
        fixed("void_air", Blocks.AIR.getDefaultState());

        String[] stones = {"stone", "granite", "polished_granite", "diorite", "polished_diorite", "andesite", "polished_andesite"};
        for (int i = 0; i < stones.length; i++) fixed(stones[i], Blocks.STONE, i);
        fixed("grass_block", Blocks.GRASS, 0);
        fixed("dirt", Blocks.DIRT, 0);
        fixed("coarse_dirt", Blocks.DIRT, 1);
        fixed("podzol", Blocks.DIRT, 2);
        fixed("sand", Blocks.SAND, 0);
        fixed("red_sand", Blocks.SAND, 1);
        fixed("sponge", Blocks.SPONGE, 0);
        fixed("wet_sponge", Blocks.SPONGE, 1);
        fixed("sandstone", Blocks.SANDSTONE, 0);
        fixed("chiseled_sandstone", Blocks.SANDSTONE, 1);
        fixed("cut_sandstone", Blocks.SANDSTONE, 2);
        fixed("red_sandstone", Blocks.RED_SANDSTONE, 0);
        fixed("chiseled_red_sandstone", Blocks.RED_SANDSTONE, 1);
        fixed("cut_red_sandstone", Blocks.RED_SANDSTONE, 2);
        fixed("stone_bricks", Blocks.STONEBRICK, 0);
        fixed("mossy_stone_bricks", Blocks.STONEBRICK, 1);
        fixed("cracked_stone_bricks", Blocks.STONEBRICK, 2);
        fixed("chiseled_stone_bricks", Blocks.STONEBRICK, 3);
        String[] infested = {"infested_stone", "infested_cobblestone", "infested_stone_bricks", "infested_mossy_stone_bricks",
                "infested_cracked_stone_bricks", "infested_chiseled_stone_bricks"};
        for (int i = 0; i < infested.length; i++) fixed(infested[i], Blocks.MONSTER_EGG, i);
        fixed("prismarine", Blocks.PRISMARINE, 0);
        fixed("prismarine_bricks", Blocks.PRISMARINE, 1);
        fixed("dark_prismarine", Blocks.PRISMARINE, 2);
        fixed("quartz_block", Blocks.QUARTZ_BLOCK, 0);
        fixed("chiseled_quartz_block", Blocks.QUARTZ_BLOCK, 1);
        MAPPINGS.put("minecraft:quartz_pillar", properties -> {
            String axis = properties.getOrDefault("axis", "y");
            return meta(Blocks.QUARTZ_BLOCK, "x".equals(axis) ? 3 : "z".equals(axis) ? 4 : 2);
        });
        fixed("bricks", Blocks.BRICK_BLOCK, 0);
        fixed("nether_bricks", Blocks.NETHER_BRICK, 0);
        fixed("red_nether_bricks", Blocks.RED_NETHER_BRICK, 0);
        fixed("end_stone_bricks", Blocks.END_BRICKS, 0);
        fixed("terracotta", Blocks.HARDENED_CLAY, 0);
        fixed("magma_block", Blocks.MAGMA, 0);
        fixed("melon", Blocks.MELON_BLOCK, 0);
        fixed("cobweb", Blocks.WEB, 0);
        fixed("snow_block", Blocks.SNOW, 0);
        rename("snow", Blocks.SNOW_LAYER);
        fixed("slime_block", Blocks.SLIME_BLOCK, 0);
        fixed("spawner", Blocks.MOB_SPAWNER, 0);
        fixed("note_block", Blocks.NOTEBLOCK, 0);
        fixed("nether_quartz_ore", Blocks.QUARTZ_ORE, 0);
        rename("lily_pad", Blocks.WATERLILY);
        rename("sugar_cane", Blocks.REEDS);
        rename("dead_bush", Blocks.DEADBUSH);
        fixed("grass", Blocks.TALLGRASS, 1);
        fixed("fern", Blocks.TALLGRASS, 2);
        fixed("dandelion", Blocks.YELLOW_FLOWER, 0);
        String[] flowers = {"poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy"};
        for (int i = 0; i < flowers.length; i++) fixed(flowers[i], Blocks.RED_FLOWER, i);
        String[] tallPlants = {"sunflower", "lilac", "tall_grass", "large_fern", "rose_bush", "peony"};
        for (int i = 0; i < tallPlants.length; i++) {
            int variant = i;
            MAPPINGS.put("minecraft:" + tallPlants[i], properties -> "upper".equals(properties.get("half")) ? meta(Blocks.DOUBLE_PLANT, 8) : meta(Blocks.DOUBLE_PLANT, variant));
        }
        MAPPINGS.put("minecraft:torch", properties -> Blocks.TORCH.getDefaultState());
        rename("wall_torch", Blocks.TORCH);
        MAPPINGS.put("minecraft:redstone_torch", properties -> "false".equals(properties.get("lit")) ? Blocks.UNLIT_REDSTONE_TORCH.getDefaultState() : Blocks.REDSTONE_TORCH.getDefaultState());
        MAPPINGS.put("minecraft:redstone_wall_torch", properties -> copy(("false".equals(properties.get("lit")) ? Blocks.UNLIT_REDSTONE_TORCH : Blocks.REDSTONE_TORCH).getDefaultState(), properties));
        rename("sign", Blocks.STANDING_SIGN);
        rename("wall_sign", Blocks.WALL_SIGN);
        // 1.14 gave every wood its own signs; 1.12 has one sign (unless Portium has the wood's).
        for (String wood : new String[] {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "crimson", "warped"}) {
            String standing = wood + "_sign";
            String wall = wood + "_wall_sign";
            MAPPINGS.put("minecraft:" + standing, properties -> ownOr(standing, properties, Blocks.STANDING_SIGN));
            MAPPINGS.put("minecraft:" + wall, properties -> ownOr(wall, properties, Blocks.WALL_SIGN));
        }
        MAPPINGS.put("minecraft:furnace", properties -> copy(("true".equals(properties.get("lit")) ? Blocks.LIT_FURNACE : Blocks.FURNACE).getDefaultState(), properties));
        MAPPINGS.put("minecraft:redstone_ore", properties -> ("true".equals(properties.get("lit")) ? Blocks.LIT_REDSTONE_ORE : Blocks.REDSTONE_ORE).getDefaultState());
        MAPPINGS.put("minecraft:redstone_lamp", properties -> ("true".equals(properties.get("lit")) ? Blocks.LIT_REDSTONE_LAMP : Blocks.REDSTONE_LAMP).getDefaultState());
        MAPPINGS.put("minecraft:repeater", properties -> copy(("true".equals(properties.get("powered")) ? Blocks.POWERED_REPEATER : Blocks.UNPOWERED_REPEATER).getDefaultState(), properties));
        MAPPINGS.put("minecraft:comparator", properties -> copy(("true".equals(properties.get("powered")) ? Blocks.POWERED_COMPARATOR : Blocks.UNPOWERED_COMPARATOR).getDefaultState(), properties));
        MAPPINGS.put("minecraft:daylight_detector", properties -> copy(("true".equals(properties.get("inverted")) ? Blocks.DAYLIGHT_DETECTOR_INVERTED : Blocks.DAYLIGHT_DETECTOR).getDefaultState(), properties));
        rename("powered_rail", Blocks.GOLDEN_RAIL);
        rename("nether_portal", Blocks.PORTAL);
        rename("moving_piston", Blocks.PISTON_EXTENSION);
        rename("carved_pumpkin", Blocks.PUMPKIN);
        rename("jack_o_lantern", Blocks.LIT_PUMPKIN);
        MAPPINGS.put("minecraft:anvil", properties -> copy(Blocks.ANVIL.getDefaultState(), properties));
        MAPPINGS.put("minecraft:chipped_anvil", properties -> copy(meta(Blocks.ANVIL, 4), properties));
        MAPPINGS.put("minecraft:damaged_anvil", properties -> copy(meta(Blocks.ANVIL, 8), properties));
        fixed("cobblestone_wall", Blocks.COBBLESTONE_WALL, 0);
        fixed("mossy_cobblestone_wall", Blocks.COBBLESTONE_WALL, 1);
        // 1.13 names skeleton skulls "_skull" and the rest "_head".
        for (String skull : new String[] {"skeleton_skull", "wither_skeleton_skull", "zombie_head", "player_head", "creeper_head", "dragon_head"}) {
            String wall = skull.replace("_skull", "_wall_skull").replace("_head", "_wall_head");
            MAPPINGS.put("minecraft:" + skull, properties -> meta(Blocks.SKULL, 1));
            MAPPINGS.put("minecraft:" + wall, properties -> copy(Blocks.SKULL.getDefaultState(), properties));
        }
        // 1.13 has a block per potted plant; 1.12 has one flower pot that keeps its plant in a block entity.
        String[] potted = {"poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip",
                "oxeye_daisy", "dandelion", "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling", "acacia_sapling",
                "dark_oak_sapling", "red_mushroom", "brown_mushroom", "dead_bush", "fern", "cactus"};
        for (String plant : potted) MAPPINGS.put("minecraft:potted_" + plant, properties -> Blocks.FLOWER_POT.getDefaultState());
        MAPPINGS.put("minecraft:water", properties -> copy(Blocks.WATER.getDefaultState(), properties));
        MAPPINGS.put("minecraft:lava", properties -> copy(Blocks.LAVA.getDefaultState(), properties));
        // Where Portium's block is switched off: a bubble column is just water, an uncarved pumpkin 1.12's pumpkin.
        MAPPINGS.put("minecraft:bubble_column", properties -> {
            Block column = portiumBlock("bubble_column");
            return column == null ? Blocks.WATER.getDefaultState() : copy(column.getDefaultState(), properties);
        });
        MAPPINGS.put("minecraft:pumpkin", properties -> {
            Block pumpkin = portiumBlock("pumpkin");
            return (pumpkin == null ? Blocks.PUMPKIN : pumpkin).getDefaultState();
        });

        // Wood families. 1.12 splits logs and leaves across two blocks (oak/spruce/birch/jungle, then acacia/dark oak).
        for (int i = 0; i < WOODS.length; i++) {
            String wood = WOODS[i];
            boolean second = i >= 4;
            int meta = second ? i - 4 : i;
            fixed(wood + "_planks", Blocks.PLANKS, i);
            fixed(wood + "_sapling", Blocks.SAPLING, i);
            log(wood + "_log", second ? Blocks.LOG2 : Blocks.LOG, meta);
            Block leaves = second ? Blocks.LEAVES2 : Blocks.LEAVES;
            int leafMeta = meta | 4;
            MAPPINGS.put("minecraft:" + wood + "_leaves", properties -> meta(leaves, leafMeta));
            slab(wood + "_slab", Blocks.WOODEN_SLAB, Blocks.DOUBLE_WOODEN_SLAB, i);
        }
        rename("oak_fence", Blocks.OAK_FENCE);
        rename("oak_fence_gate", Blocks.OAK_FENCE_GATE);
        rename("oak_door", Blocks.OAK_DOOR);
        rename("oak_trapdoor", Blocks.TRAPDOOR);
        rename("oak_button", Blocks.WOODEN_BUTTON);
        rename("oak_pressure_plate", Blocks.WOODEN_PRESSURE_PLATE);
        rename("stone_pressure_plate", Blocks.STONE_PRESSURE_PLATE);
        rename("cobblestone_stairs", Blocks.STONE_STAIRS);

        slab("stone_slab", Blocks.STONE_SLAB, Blocks.DOUBLE_STONE_SLAB, 0);
        // 1.14 named 1.13's stone slab smooth (and gave plain stone a slab of its own, which 1.12 lacks).
        slab("smooth_stone_slab", Blocks.STONE_SLAB, Blocks.DOUBLE_STONE_SLAB, 0);
        slab("sandstone_slab", Blocks.STONE_SLAB, Blocks.DOUBLE_STONE_SLAB, 1);
        slab("petrified_oak_slab", Blocks.STONE_SLAB, Blocks.DOUBLE_STONE_SLAB, 2);
        slab("cobblestone_slab", Blocks.STONE_SLAB, Blocks.DOUBLE_STONE_SLAB, 3);
        slab("brick_slab", Blocks.STONE_SLAB, Blocks.DOUBLE_STONE_SLAB, 4);
        slab("stone_brick_slab", Blocks.STONE_SLAB, Blocks.DOUBLE_STONE_SLAB, 5);
        slab("nether_brick_slab", Blocks.STONE_SLAB, Blocks.DOUBLE_STONE_SLAB, 6);
        slab("quartz_slab", Blocks.STONE_SLAB, Blocks.DOUBLE_STONE_SLAB, 7);
        slab("red_sandstone_slab", Blocks.STONE_SLAB2, Blocks.DOUBLE_STONE_SLAB2, 0);
        slab("purpur_slab", Blocks.PURPUR_SLAB, Blocks.PURPUR_DOUBLE_SLAB, 0);
        for (String prismarine : new String[] {"prismarine", "prismarine_brick", "dark_prismarine"}) {
            Block single = portiumBlock(prismarine + "_slab");
            Block doubleSlab = portiumBlock(prismarine + "_slab_double");
            if (single != null && doubleSlab != null) slab(prismarine + "_slab", single, doubleSlab, 0);
        }

        // Colour families. 1.13 renamed "silver" to light gray.
        for (int i = 0; i < COLORS.length; i++) {
            String color = COLORS[i];
            fixed(color + "_wool", Blocks.WOOL, i);
            fixed(color + "_carpet", Blocks.CARPET, i);
            fixed(color + "_stained_glass", Blocks.STAINED_GLASS, i);
            fixed(color + "_stained_glass_pane", Blocks.STAINED_GLASS_PANE, i);
            fixed(color + "_terracotta", Blocks.STAINED_HARDENED_CLAY, i);
            fixed(color + "_concrete", Blocks.CONCRETE, i);
            fixed(color + "_concrete_powder", Blocks.CONCRETE_POWDER, i);
            String legacy = EnumDyeColor.byMetadata(i).getName();
            Block glazed = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(legacy + "_glazed_terracotta"));
            if (glazed != null) rename(color + "_glazed_terracotta", glazed);
            Block shulker = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(legacy + "_shulker_box"));
            if (shulker != null) rename(color + "_shulker_box", shulker);
            rename(color + "_bed", Blocks.BED);
            rename(color + "_banner", Blocks.STANDING_BANNER);
            rename(color + "_wall_banner", Blocks.WALL_BANNER);
        }
        rename("shulker_box", Blocks.PURPLE_SHULKER_BOX);

        // Coral wall fans share Portium's coral fan blocks, which have a facing property.
        for (String coral : new String[] {"tube", "brain", "bubble", "fire", "horn"}) {
            for (String prefix : new String[] {"", "dead_"}) {
                String fan = prefix + coral + "_coral_fan";
                MAPPINGS.put("minecraft:" + prefix + coral + "_coral_wall_fan", properties -> {
                    Block block = portiumBlock(fan);
                    return block == null ? Blocks.AIR.getDefaultState() : copy(block.getDefaultState(), properties);
                });
            }
        }
        ready = true;
    }

    /** Portium's block of that name, or null if it isn't registered (switched off in the config). */
    @javax.annotation.Nullable
    private static Block portiumBlock(String name) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(Tags.MOD_ID, name));
        return block == null || block == Blocks.AIR ? null : block;
    }

    private LegacyBlockStates() {}
}
