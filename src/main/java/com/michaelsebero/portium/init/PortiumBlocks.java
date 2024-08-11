package com.michaelsebero.portium.init;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.block.*;
import com.michaelsebero.portium.block.building.*;
import com.michaelsebero.portium.block.coral.*;
import com.michaelsebero.portium.block.nether.*;
import com.michaelsebero.portium.item.ItemStackableBlock;
import com.michaelsebero.portium.tile.TileEntityConduit;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSlab;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Portium's blocks. Each group is registered only when {@link PortiumConfig#blocks} has it on; the fields of a group
 * that's off stay null (and its maps empty), so code using a block checks for that first.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PortiumBlocks {

    public static Block BLUE_ICE;
    public static Block BUBBLE_COLUMN;
    public static Block CONDUIT;
    public static Block DRIED_KELP_BLOCK;
    public static Block KELP;
    public static Block KELP_PLANT;
    public static Block SEAGRASS;
    public static Block TALL_SEAGRASS;
    public static Block SEA_PICKLE;
    public static Block TURTLE_EGG;
    public static Block PUMPKIN;

    public static Block RED_MUSHROOM_BLOCK;
    public static Block BROWN_MUSHROOM_BLOCK;
    public static Block MUSHROOM_STEM;

    public static Block SMOOTH_STONE;
    public static Block SMOOTH_SANDSTONE;
    public static Block SMOOTH_RED_SANDSTONE;
    public static Block SMOOTH_QUARTZ;

    public static Block PRISMARINE_STAIRS;
    public static Block PRISMARINE_BRICK_STAIRS;
    public static Block DARK_PRISMARINE_STAIRS;
    public static BlockPortiumSlab PRISMARINE_SLAB;
    public static BlockPortiumSlab PRISMARINE_BRICK_SLAB;
    public static BlockPortiumSlab DARK_PRISMARINE_SLAB;

    /** Buttons, pressure plates and trapdoors of the five non-oak woods. */
    public static final Map<BlockPlanks.EnumType, Block> BUTTONS = new EnumMap<>(BlockPlanks.EnumType.class);
    public static final Map<BlockPlanks.EnumType, Block> PRESSURE_PLATES = new EnumMap<>(BlockPlanks.EnumType.class);
    public static final Map<BlockPlanks.EnumType, Block> TRAPDOORS = new EnumMap<>(BlockPlanks.EnumType.class);

    /** Stripped logs, wood and stripped wood for all six woods. */
    public static final Map<BlockPlanks.EnumType, Block> STRIPPED_LOGS = new EnumMap<>(BlockPlanks.EnumType.class);
    public static final Map<BlockPlanks.EnumType, Block> WOOD = new EnumMap<>(BlockPlanks.EnumType.class);
    public static final Map<BlockPlanks.EnumType, Block> STRIPPED_WOOD = new EnumMap<>(BlockPlanks.EnumType.class);

    // 1.16's Nether.
    public static Block CRIMSON_NYLIUM;
    public static Block WARPED_NYLIUM;
    public static Block CRIMSON_STEM;
    public static Block WARPED_STEM;
    public static Block STRIPPED_CRIMSON_STEM;
    public static Block STRIPPED_WARPED_STEM;
    public static Block CRIMSON_HYPHAE;
    public static Block WARPED_HYPHAE;
    public static Block STRIPPED_CRIMSON_HYPHAE;
    public static Block STRIPPED_WARPED_HYPHAE;
    public static Block WARPED_WART_BLOCK;
    public static Block SHROOMLIGHT;
    public static Block CRIMSON_FUNGUS;
    public static Block WARPED_FUNGUS;
    public static Block CRIMSON_ROOTS;
    public static Block WARPED_ROOTS;
    public static Block NETHER_SPROUTS;
    public static Block WEEPING_VINES;
    public static Block WEEPING_VINES_PLANT;
    public static Block TWISTING_VINES;
    public static Block TWISTING_VINES_PLANT;
    public static Block SOUL_SOIL;
    public static Block SOUL_FIRE;
    public static Block BASALT;
    public static Block POLISHED_BASALT;
    public static Block BLACKSTONE;
    public static Block GILDED_BLACKSTONE;
    public static Block NETHER_GOLD_ORE;
    public static Block ANCIENT_DEBRIS;
    public static Block CRYING_OBSIDIAN;

    public static final Map<CoralType, Block> CORAL_BLOCKS = new EnumMap<>(CoralType.class);
    public static final Map<CoralType, Block> DEAD_CORAL_BLOCKS = new EnumMap<>(CoralType.class);
    public static final Map<CoralType, Block> CORALS = new EnumMap<>(CoralType.class);
    public static final Map<CoralType, Block> DEAD_CORALS = new EnumMap<>(CoralType.class);
    public static final Map<CoralType, Block> CORAL_FANS = new EnumMap<>(CoralType.class);
    public static final Map<CoralType, Block> DEAD_CORAL_FANS = new EnumMap<>(CoralType.class);

    /** Blocks that get an item, and how to make it. Filled during block registration, read by {@link PortiumItems}. */
    static final Map<Block, Function<Block, Item>> ITEM_BLOCKS = new LinkedHashMap<>();
    private static final List<Block> ALL = new ArrayList<>();

    public static List<Block> all() {
        return Collections.unmodifiableList(ALL);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();

        PortiumConfig.Blocks enabled = PortiumConfig.blocks;
        // Each switch registers a group that belongs together; fields of groups that are off stay null.
        if (enabled.blueIce) BLUE_ICE = register(registry, "blue_ice", new BlockBlueIce());
        if (enabled.bubbleColumns) BUBBLE_COLUMN = registerNoItem(registry, "bubble_column", new BlockBubbleColumn());
        if (enabled.conduit) {
            CONDUIT = register(registry, "conduit", new BlockConduit());
            GameRegistry.registerTileEntity(TileEntityConduit.class, new ResourceLocation(Tags.MOD_ID, "conduit"));
        }
        if (enabled.kelp) {
            DRIED_KELP_BLOCK = register(registry, "dried_kelp_block", new BlockDriedKelp());
            KELP = register(registry, "kelp", new BlockKelp());
            KELP_PLANT = registerNoItem(registry, "kelp_plant", new BlockKelpPlant());
        }
        if (enabled.seagrass) {
            SEAGRASS = register(registry, "seagrass", new BlockSeagrass());
            TALL_SEAGRASS = registerNoItem(registry, "tall_seagrass", new BlockTallSeagrass());
        }
        if (enabled.seaPickles) SEA_PICKLE = register(registry, "sea_pickle", new BlockSeaPickle(), ItemStackableBlock::new);
        if (enabled.turtleEggs) TURTLE_EGG = register(registry, "turtle_egg", new BlockTurtleEgg(), ItemStackableBlock::new);
        if (enabled.uncarvedPumpkin) PUMPKIN = register(registry, "pumpkin", new BlockUncarvedPumpkin());

        if (enabled.mushroomBlocks) {
            RED_MUSHROOM_BLOCK = register(registry, "red_mushroom_block", new BlockMushroomFaces(MapColor.RED, Blocks.RED_MUSHROOM));
            BROWN_MUSHROOM_BLOCK = register(registry, "brown_mushroom_block", new BlockMushroomFaces(MapColor.DIRT, Blocks.BROWN_MUSHROOM));
            MUSHROOM_STEM = register(registry, "mushroom_stem", new BlockMushroomFaces(MapColor.CLOTH, null));
        }

        if (enabled.smoothStone) {
            SMOOTH_STONE = register(registry, "smooth_stone", BlockSimple.stone(MapColor.STONE));
            SMOOTH_SANDSTONE = register(registry, "smooth_sandstone", BlockSimple.stone(MapColor.SAND));
            SMOOTH_RED_SANDSTONE = register(registry, "smooth_red_sandstone", BlockSimple.stone(MapColor.ADOBE));
            SMOOTH_QUARTZ = register(registry, "smooth_quartz", BlockSimple.stone(MapColor.QUARTZ));
        }

        if (enabled.prismarineStairsAndSlabs) {
            PRISMARINE_STAIRS = register(registry, "prismarine_stairs", new BlockPortiumStairs(Blocks.PRISMARINE.getStateFromMeta(0)));
            PRISMARINE_BRICK_STAIRS = register(registry, "prismarine_brick_stairs", new BlockPortiumStairs(Blocks.PRISMARINE.getStateFromMeta(1)));
            DARK_PRISMARINE_STAIRS = register(registry, "dark_prismarine_stairs", new BlockPortiumStairs(Blocks.PRISMARINE.getStateFromMeta(2)));
            PRISMARINE_SLAB = registerSlab(registry, "prismarine_slab", MapColor.CYAN);
            PRISMARINE_BRICK_SLAB = registerSlab(registry, "prismarine_brick_slab", MapColor.DIAMOND);
            DARK_PRISMARINE_SLAB = registerSlab(registry, "dark_prismarine_slab", MapColor.DIAMOND);
        }

        for (BlockPlanks.EnumType wood : BlockPlanks.EnumType.values()) {
            String name = wood.getName();
            if (wood != BlockPlanks.EnumType.OAK) {
                if (enabled.woodenButtonsAndPressurePlates) {
                    BUTTONS.put(wood, register(registry, name + "_button", new BlockWoodButton()));
                    PRESSURE_PLATES.put(wood, register(registry, name + "_pressure_plate", new BlockWoodPressurePlate(wood.getMapColor())));
                }
                if (enabled.woodenTrapdoors) TRAPDOORS.put(wood, register(registry, name + "_trapdoor", new BlockWoodTrapDoor(wood.getMapColor())));
            }
            if (enabled.strippedLogsAndWood) {
                STRIPPED_LOGS.put(wood, register(registry, "stripped_" + name + "_log", new BlockPortiumLog(wood.getMapColor())));
                WOOD.put(wood, register(registry, name + "_wood", new BlockPortiumLog(barkColor(wood))));
                STRIPPED_WOOD.put(wood, register(registry, "stripped_" + name + "_wood", new BlockPortiumLog(wood.getMapColor())));
            }
        }

        registerNether(registry, enabled);

        if (enabled.coral) {
            for (CoralType type : CoralType.values()) {
                String name = type.getName();
                Block deadBlock = register(registry, "dead_" + name + "_coral_block", new BlockCoralBlock(type, null));
                DEAD_CORAL_BLOCKS.put(type, deadBlock);
                CORAL_BLOCKS.put(type, register(registry, name + "_coral_block", new BlockCoralBlock(type, deadBlock)));
                Block deadCoral = register(registry, "dead_" + name + "_coral", new BlockCoralPlant(type, null));
                DEAD_CORALS.put(type, deadCoral);
                CORALS.put(type, register(registry, name + "_coral", new BlockCoralPlant(type, deadCoral)));
                Block deadFan = register(registry, "dead_" + name + "_coral_fan", new BlockCoralFan(type, null));
                DEAD_CORAL_FANS.put(type, deadFan);
                CORAL_FANS.put(type, register(registry, name + "_coral_fan", new BlockCoralFan(type, deadFan)));
            }
        }
    }

    private static void registerNether(IForgeRegistry<Block> registry, PortiumConfig.Blocks enabled) {
        if (enabled.netherForests) {
            CRIMSON_NYLIUM = register(registry, "crimson_nylium", new BlockNylium(MapColor.RED, false));
            WARPED_NYLIUM = register(registry, "warped_nylium", new BlockNylium(MapColor.CYAN, true));
            CRIMSON_STEM = register(registry, "crimson_stem", new BlockNetherStem(MapColor.MAGENTA));
            WARPED_STEM = register(registry, "warped_stem", new BlockNetherStem(MapColor.CYAN));
            STRIPPED_CRIMSON_STEM = register(registry, "stripped_crimson_stem", new BlockNetherStem(MapColor.MAGENTA));
            STRIPPED_WARPED_STEM = register(registry, "stripped_warped_stem", new BlockNetherStem(MapColor.CYAN));
            CRIMSON_HYPHAE = register(registry, "crimson_hyphae", new BlockNetherStem(MapColor.RED));
            WARPED_HYPHAE = register(registry, "warped_hyphae", new BlockNetherStem(MapColor.PURPLE));
            STRIPPED_CRIMSON_HYPHAE = register(registry, "stripped_crimson_hyphae", new BlockNetherStem(MapColor.RED));
            STRIPPED_WARPED_HYPHAE = register(registry, "stripped_warped_hyphae", new BlockNetherStem(MapColor.PURPLE));
            WARPED_WART_BLOCK = register(registry, "warped_wart_block", new BlockSimple(Material.GRASS, MapColor.CYAN, PortiumSoundTypes.WART).setHardness(1.0F));
            SHROOMLIGHT = register(registry, "shroomlight", new BlockSimple(Material.GRASS, MapColor.RED, PortiumSoundTypes.SHROOMLIGHT).setHardness(1.0F));
            SHROOMLIGHT.setLightLevel(1.0F);
            CRIMSON_FUNGUS = register(registry, "crimson_fungus", new BlockNetherFungus(MapColor.NETHERRACK, false));
            WARPED_FUNGUS = register(registry, "warped_fungus", new BlockNetherFungus(MapColor.CYAN, true));
            CRIMSON_ROOTS = register(registry, "crimson_roots", new BlockNetherRoots(MapColor.NETHERRACK, false));
            WARPED_ROOTS = register(registry, "warped_roots", new BlockNetherRoots(MapColor.CYAN, false));
            NETHER_SPROUTS = register(registry, "nether_sprouts", new BlockNetherRoots(MapColor.CYAN, true));
            BlockNetherVine weeping = new BlockNetherVine(MapColor.NETHERRACK, EnumFacing.DOWN);
            WEEPING_VINES = register(registry, "weeping_vines", weeping);
            WEEPING_VINES_PLANT = registerNoItem(registry, "weeping_vines_plant", new BlockNetherVinePlant(MapColor.NETHERRACK, weeping));
            BlockNetherVine twisting = new BlockNetherVine(MapColor.CYAN, EnumFacing.UP);
            TWISTING_VINES = register(registry, "twisting_vines", twisting);
            TWISTING_VINES_PLANT = registerNoItem(registry, "twisting_vines_plant", new BlockNetherVinePlant(MapColor.CYAN, twisting));
        }
        if (enabled.soulSoil) {
            Block soil = new BlockSimple(Material.GROUND, MapColor.BROWN, PortiumSoundTypes.SOUL_SOIL).setHardness(0.5F);
            soil.setHarvestLevel("shovel", 0);
            SOUL_SOIL = register(registry, "soul_soil", soil);
            SOUL_FIRE = registerNoItem(registry, "soul_fire", new BlockSoulFire());
        }
        if (enabled.basalt) {
            BASALT = register(registry, "basalt", new BlockBasalt());
            POLISHED_BASALT = register(registry, "polished_basalt", new BlockBasalt());
        }
        if (enabled.blackstone) {
            Block blackstone = new BlockSimple(Material.ROCK, MapColor.BLACK, SoundType.STONE).setHardness(1.5F).setResistance(10.0F);
            blackstone.setHarvestLevel("pickaxe", 0);
            BLACKSTONE = register(registry, "blackstone", blackstone);
            GILDED_BLACKSTONE = register(registry, "gilded_blackstone", BlockNetherNuggetOre.gildedBlackstone(PortiumSoundTypes.GILDED_BLACKSTONE));
        }
        if (enabled.netherGoldOre) NETHER_GOLD_ORE = register(registry, "nether_gold_ore", BlockNetherNuggetOre.goldOre(PortiumSoundTypes.NETHER_GOLD));
        if (enabled.ancientDebris) {
            Block debris = new BlockSimple(Material.IRON, MapColor.BLACK, PortiumSoundTypes.ANCIENT_DEBRIS).setHardness(30.0F).setResistance(2000.0F);
            debris.setHarvestLevel("pickaxe", 3);
            ANCIENT_DEBRIS = register(registry, "ancient_debris", debris);
        }
        if (enabled.cryingObsidian) CRYING_OBSIDIAN = register(registry, "crying_obsidian", new BlockCryingObsidian());
    }

    /** Bark matches the log side: the same colours vanilla uses for the side of each log. */
    private static MapColor barkColor(BlockPlanks.EnumType wood) {
        switch (wood) {
            case SPRUCE: return MapColor.OBSIDIAN;
            case BIRCH: return MapColor.QUARTZ;
            case JUNGLE: return MapColor.DIRT;
            case ACACIA: return MapColor.STONE;
            case DARK_OAK: return MapColor.BROWN;
            default: return MapColor.WOOD;
        }
    }

    private static BlockPortiumSlab registerSlab(IForgeRegistry<Block> registry, String name, MapColor mapColor) {
        BlockPortiumSlab half = new BlockPortiumSlab.Half(Material.ROCK, mapColor);
        half.setHardness(1.5F).setResistance(10.0F);
        half.setHarvestLevel("pickaxe", 0);
        BlockPortiumSlab doubleSlab = new BlockPortiumSlab.Double(Material.ROCK, mapColor, half);
        doubleSlab.setHardness(1.5F).setResistance(10.0F);
        doubleSlab.setHarvestLevel("pickaxe", 0);
        register(registry, name, half, block -> new ItemSlab(block, half, doubleSlab));
        registerNoItem(registry, name + "_double", doubleSlab);
        return half;
    }

    private static <T extends Block> T register(IForgeRegistry<Block> registry, String name, T block) {
        return register(registry, name, block, ItemBlock::new);
    }

    private static <T extends Block> T register(IForgeRegistry<Block> registry, String name, T block, Function<Block, Item> itemFactory) {
        registerNoItem(registry, name, block);
        ITEM_BLOCKS.put(block, itemFactory);
        return block;
    }

    private static <T extends Block> T registerNoItem(IForgeRegistry<Block> registry, String name, T block) {
        block.setRegistryName(new ResourceLocation(Tags.MOD_ID, name));
        block.setTranslationKey(Tags.MOD_ID + "." + name);
        registry.register(block);
        ALL.add(block);
        return block;
    }

    private PortiumBlocks() {}
}
