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
    public static Block NETHERITE_BLOCK;
    public static Block SMITHING_TABLE;
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

    private static java.util.Set<Block> ownSet;

    /** Whether a block is one of Portium's (whatever namespace its id took). */
    public static boolean isOwn(Block block) {
        if (ownSet == null || ownSet.size() != ALL.size()) {
            java.util.Set<Block> set = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            set.addAll(ALL);
            ownSet = set;
        }
        return ownSet.contains(block);
    }

    private static boolean registered;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        registerBlocks(event.getRegistry());
    }

    /**
     * Registers Portium's blocks, once: at construction ({@link com.michaelsebero.portium.Portium#construct}), so
     * newer mods' code that runs as they load (static initialisers naming {@code Blocks.CRYING_OBSIDIAN}) finds them,
     * as 1.16 mods find vanilla's blocks there. Forge 1.12 doesn't lock its registries before the registry events.
     */
    public static synchronized void registerBlocks(IForgeRegistry<Block> registry) {
        if (registered) return;
        registered = true;

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
        registerLights(registry, enabled);
        registerPlants(registry, enabled);
        registerWorkstations(registry, enabled);
        if (enabled.stoneVariants) registerStoneVariants(registry);
        if (enabled.woodSigns) registerWoodSigns(registry);
        if (!SIGNS.isEmpty()) {
            GameRegistry.registerTileEntity(com.michaelsebero.portium.tile.TileEntityPortiumSign.class, new ResourceLocation(Tags.MOD_ID, "sign"));
        }

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
            ANCIENT_DEBRIS = register(registry, "ancient_debris", debris, com.michaelsebero.portium.item.ItemBlockFireproof::new);
        }
        if (enabled.ancientDebris && PortiumConfig.items.netherite) {
            NETHERITE_BLOCK = register(registry, "netherite_block", new com.michaelsebero.portium.block.nether.BlockNetherite(),
                    com.michaelsebero.portium.item.ItemBlockFireproof::new);
            SMITHING_TABLE = register(registry, "smithing_table", new com.michaelsebero.portium.block.BlockSmithingTable());
        }
        if (enabled.cryingObsidian) CRYING_OBSIDIAN = register(registry, "crying_obsidian", new BlockCryingObsidian());
        if (enabled.netherForests && enabled.netherWoods) {
            registerNetherWood(registry, "crimson", MapColor.MAGENTA);
            registerNetherWood(registry, "warped", MapColor.CYAN);
        }
        if (enabled.blackstone && enabled.blackstoneFamily) registerBlackstoneFamily(registry);
        if (enabled.netherBricks116) {
            CRACKED_NETHER_BRICKS = register(registry, "cracked_nether_bricks", stone(MapColor.NETHERRACK, 2.0F, 6.0F));
            register(registry, "chiseled_nether_bricks", stone(MapColor.NETHERRACK, 2.0F, 6.0F));
            register(registry, "quartz_bricks", stone(MapColor.QUARTZ, 0.8F, 0.8F));
        }
    }

    public static Block LANTERN;
    public static Block SOUL_LANTERN;
    public static Block CHAIN;
    public static Block SOUL_TORCH;
    public static Block CAMPFIRE;
    public static Block SOUL_CAMPFIRE;
    public static Block TARGET;
    public static Block CORNFLOWER;
    public static Block LILY_OF_THE_VALLEY;
    public static Block WITHER_ROSE;
    public static Block SWEET_BERRY_BUSH;
    public static Block BAMBOO;
    public static Block BAMBOO_SAPLING;
    public static Block BARREL;
    public static Block SMOKER;
    public static Block BLAST_FURNACE;
    public static Block COMPOSTER;
    public static Block FLETCHING_TABLE;
    public static Block GRINDSTONE;
    public static Block STONECUTTER;
    public static Block LOOM;
    public static Block CARTOGRAPHY_TABLE;
    public static Block BELL;
    public static Block LECTERN;
    public static Block JIGSAW;
    public static Block SCAFFOLDING;
    public static Block HONEY_BLOCK;
    public static Block BEE_NEST;
    public static Block BEEHIVE;
    public static Block HONEYCOMB_BLOCK;
    public static Block LODESTONE;
    public static Block RESPAWN_ANCHOR;

    /** Lanterns, chains, soul torches and campfires (1.14, 1.16); target blocks, lodestones and respawn anchors (1.16). */
    private static void registerLights(IForgeRegistry<Block> registry, PortiumConfig.Blocks enabled) {
        if (enabled.soulTorches) SOUL_TORCH = register(registry, "soul_torch", new com.michaelsebero.portium.block.light.BlockSoulTorch());
        if (enabled.lanterns) {
            LANTERN = register(registry, "lantern", new com.michaelsebero.portium.block.light.BlockLantern(15));
            SOUL_LANTERN = register(registry, "soul_lantern", new com.michaelsebero.portium.block.light.BlockLantern(10));
        }
        if (enabled.chains) CHAIN = register(registry, "chain", new com.michaelsebero.portium.block.light.BlockChain());
        if (enabled.campfires) {
            CAMPFIRE = register(registry, "campfire", new com.michaelsebero.portium.block.light.BlockCampfire(true, 1, 15));
            SOUL_CAMPFIRE = register(registry, "soul_campfire", new com.michaelsebero.portium.block.light.BlockCampfire(false, 2, 10));
            GameRegistry.registerTileEntity(com.michaelsebero.portium.tile.TileEntityCampfire.class, new ResourceLocation(Tags.MOD_ID, "campfire"));
        }
        if (enabled.target) TARGET = register(registry, "target", new com.michaelsebero.portium.block.BlockTarget());
        if (enabled.lodestone) {
            Block lodestone = new BlockSimple(Material.ANVIL, MapColor.IRON, PortiumSoundTypes.LODESTONE).setHardness(3.5F);
            lodestone.setHarvestLevel("pickaxe", 0);
            LODESTONE = register(registry, "lodestone", lodestone);
        }
        if (enabled.respawnAnchor) RESPAWN_ANCHOR = register(registry, "respawn_anchor", new com.michaelsebero.portium.block.nether.BlockRespawnAnchor());
    }

    /** 1.14's job blocks: barrel, smoker, blast furnace, composter, fletching table. */
    private static void registerWorkstations(IForgeRegistry<Block> registry, PortiumConfig.Blocks enabled) {
        if (enabled.barrel) {
            BARREL = register(registry, "barrel", new com.michaelsebero.portium.block.workstation.BlockBarrel());
            GameRegistry.registerTileEntity(com.michaelsebero.portium.tile.TileEntityBarrel.class, new ResourceLocation(Tags.MOD_ID, "barrel"));
        }
        if (enabled.smokerAndBlastFurnace) {
            SMOKER = register(registry, "smoker", new com.michaelsebero.portium.block.workstation.BlockCookingFurnace(
                    com.michaelsebero.portium.recipe.CookingRecipes.Type.SMOKING, com.michaelsebero.portium.inventory.PortiumGuis.SMOKER, PortiumStats.INTERACT_WITH_SMOKER));
            BLAST_FURNACE = register(registry, "blast_furnace", new com.michaelsebero.portium.block.workstation.BlockCookingFurnace(
                    com.michaelsebero.portium.recipe.CookingRecipes.Type.BLASTING, com.michaelsebero.portium.inventory.PortiumGuis.BLAST_FURNACE,
                    PortiumStats.INTERACT_WITH_BLAST_FURNACE));
            GameRegistry.registerTileEntity(com.michaelsebero.portium.tile.TileEntityCookingFurnace.Smoker.class, new ResourceLocation(Tags.MOD_ID, "smoker"));
            GameRegistry.registerTileEntity(com.michaelsebero.portium.tile.TileEntityCookingFurnace.BlastFurnace.class, new ResourceLocation(Tags.MOD_ID, "blast_furnace"));
        }
        if (enabled.composter) {
            COMPOSTER = register(registry, "composter", new com.michaelsebero.portium.block.workstation.BlockComposter());
            GameRegistry.registerTileEntity(com.michaelsebero.portium.tile.TileEntityComposter.class, new ResourceLocation(Tags.MOD_ID, "composter"));
        }
        if (enabled.fletchingTable) {
            Block table = new BlockSimple(Material.WOOD, MapColor.WOOD, SoundType.WOOD).setHardness(2.5F).setCreativeTab(net.minecraft.creativetab.CreativeTabs.DECORATIONS);
            table.setHarvestLevel("axe", 0);
            FLETCHING_TABLE = register(registry, "fletching_table", table);
        }
        if (enabled.grindstone) GRINDSTONE = register(registry, "grindstone", new com.michaelsebero.portium.block.workstation.BlockGrindstone());
        if (enabled.stonecutter) STONECUTTER = register(registry, "stonecutter", new com.michaelsebero.portium.block.workstation.BlockStonecutter());
        if (enabled.loom) {
            com.michaelsebero.portium.item.BannerPatterns116.register();
            LOOM = register(registry, "loom", new com.michaelsebero.portium.block.workstation.BlockLoom());
        }
        if (enabled.cartographyTable) {
            CARTOGRAPHY_TABLE = register(registry, "cartography_table", new com.michaelsebero.portium.block.workstation.BlockCartographyTable());
        }
        if (enabled.bell) {
            BELL = register(registry, "bell", new com.michaelsebero.portium.block.village.BlockBell());
            GameRegistry.registerTileEntity(com.michaelsebero.portium.tile.TileEntityBell.class, new ResourceLocation(Tags.MOD_ID, "bell"));
        }
        // The jigsaw block (1.14): for operators, so in no creative tab, as in 1.16.
        JIGSAW = register(registry, "jigsaw", new com.michaelsebero.portium.block.jigsaw.BlockJigsaw());
        GameRegistry.registerTileEntity(com.michaelsebero.portium.tile.TileEntityJigsaw.class, new ResourceLocation(Tags.MOD_ID, "jigsaw"));
        if (enabled.lectern) {
            LECTERN = register(registry, "lectern", new com.michaelsebero.portium.block.village.BlockLectern());
            GameRegistry.registerTileEntity(com.michaelsebero.portium.tile.TileEntityLectern.class, new ResourceLocation(Tags.MOD_ID, "lectern"));
        }
        if (enabled.honey) {
            HONEY_BLOCK = register(registry, "honey_block", new com.michaelsebero.portium.block.bee.BlockHoney());
            Block honeycomb = new BlockSimple(Material.CLAY, MapColor.ADOBE, com.michaelsebero.portium.init.PortiumSoundTypes.CORAL).setHardness(0.6F)
                    .setCreativeTab(net.minecraft.creativetab.CreativeTabs.DECORATIONS);
            HONEYCOMB_BLOCK = register(registry, "honeycomb_block", honeycomb);
        }
        if (enabled.beehives) {
            BEE_NEST = register(registry, "bee_nest", new com.michaelsebero.portium.block.bee.BlockBeehive(true), com.michaelsebero.portium.item.ItemBeehive::new);
            BEEHIVE = register(registry, "beehive", new com.michaelsebero.portium.block.bee.BlockBeehive(false), com.michaelsebero.portium.item.ItemBeehive::new);
            GameRegistry.registerTileEntity(com.michaelsebero.portium.tile.TileEntityBeehive.class, new ResourceLocation(Tags.MOD_ID, "beehive"));
        }
        if (enabled.scaffolding) {
            SCAFFOLDING = register(registry, "scaffolding", new com.michaelsebero.portium.block.BlockScaffolding(),
                    com.michaelsebero.portium.item.ItemScaffolding::new);
        }
    }

    /** 1.14's flowers, sweet berry bushes and bamboo, and flower pots of Portium's plants (1.13 gives each its block). */
    private static void registerPlants(IForgeRegistry<Block> registry, PortiumConfig.Blocks enabled) {
        if (enabled.flowers114) {
            CORNFLOWER = register(registry, "cornflower", new com.michaelsebero.portium.block.plant.BlockPortiumFlower(net.minecraft.init.MobEffects.JUMP_BOOST, 6));
            LILY_OF_THE_VALLEY = register(registry, "lily_of_the_valley",
                    new com.michaelsebero.portium.block.plant.BlockPortiumFlower(net.minecraft.init.MobEffects.POISON, 12));
            WITHER_ROSE = register(registry, "wither_rose", new com.michaelsebero.portium.block.plant.BlockWitherRose());
        }
        if (enabled.sweetBerries) SWEET_BERRY_BUSH = registerNoItem(registry, "sweet_berry_bush", new com.michaelsebero.portium.block.plant.BlockSweetBerryBush());
        if (enabled.bamboo) {
            BAMBOO = register(registry, "bamboo", new com.michaelsebero.portium.block.plant.BlockBamboo());
            BAMBOO_SAPLING = registerNoItem(registry, "bamboo_sapling", new com.michaelsebero.portium.block.plant.BlockBambooSapling());
        }
        for (Block plant : new Block[] {CORNFLOWER, LILY_OF_THE_VALLEY, WITHER_ROSE, BAMBOO, CRIMSON_FUNGUS, WARPED_FUNGUS, CRIMSON_ROOTS, WARPED_ROOTS}) {
            if (plant == null) continue;
            com.michaelsebero.portium.block.plant.BlockPortiumFlowerPot pot = new com.michaelsebero.portium.block.plant.BlockPortiumFlowerPot(plant);
            registerNoItem(registry, "potted_" + plant.getRegistryName().getPath(), pot);
            com.michaelsebero.portium.block.plant.FlowerPots.register(plant, pot);
        }
    }

    public static Block CRACKED_NETHER_BRICKS;
    public static Block POLISHED_BLACKSTONE;
    public static Block POLISHED_BLACKSTONE_BRICKS;
    public static Block CRACKED_POLISHED_BLACKSTONE_BRICKS;

    /** A plain block of a stone: pickaxe needed, hardness and blast resistance as 1.16 gives them. */
    private static Block stone(MapColor mapColor, float hardness, float resistance116) {
        Block block = new BlockSimple(Material.ROCK, mapColor, SoundType.STONE).setHardness(hardness).setResistance(resistance116 * 5.0F / 3.0F);
        block.setHarvestLevel("pickaxe", 0);
        return block;
    }

    /** 1.16's blackstone family: polished blackstone, its bricks, and their stairs, slabs, walls, button and plate. */
    private static void registerBlackstoneFamily(IForgeRegistry<Block> registry) {
        stoneFamily(registry, "blackstone", BLACKSTONE.getDefaultState(), MapColor.BLACK, 2.0F, 6.0F, true, true, true);
        POLISHED_BLACKSTONE = register(registry, "polished_blackstone", stone(MapColor.BLACK, 2.0F, 6.0F));
        stoneFamily(registry, "polished_blackstone", POLISHED_BLACKSTONE.getDefaultState(), MapColor.BLACK, 2.0F, 6.0F, true, true, true);
        register(registry, "polished_blackstone_button", new com.michaelsebero.portium.block.building.BlockStoneButton());
        register(registry, "polished_blackstone_pressure_plate", new com.michaelsebero.portium.block.building.BlockStonePressurePlate(MapColor.BLACK));
        POLISHED_BLACKSTONE_BRICKS = register(registry, "polished_blackstone_bricks", stone(MapColor.BLACK, 1.5F, 6.0F));
        CRACKED_POLISHED_BLACKSTONE_BRICKS = register(registry, "cracked_polished_blackstone_bricks", stone(MapColor.BLACK, 1.5F, 6.0F));
        register(registry, "chiseled_polished_blackstone", stone(MapColor.BLACK, 1.5F, 6.0F));
        stoneFamily(registry, "polished_blackstone_brick", POLISHED_BLACKSTONE_BRICKS.getDefaultState(), MapColor.BLACK, 2.0F, 6.0F, true, true, true);
    }

    /** 1.14's stairs, slabs and walls of the stones 1.12 has (and Portium's smooth ones). */
    private static void registerStoneVariants(IForgeRegistry<Block> registry) {
        net.minecraft.block.state.IBlockState stone = Blocks.STONE.getDefaultState();
        stoneFamily(registry, "stone", stone, MapColor.STONE, 2.0F, 6.0F, true, true, false);
        String[] stones = {"granite", "polished_granite", "diorite", "polished_diorite", "andesite", "polished_andesite"};
        MapColor[] colors = {MapColor.DIRT, MapColor.DIRT, MapColor.QUARTZ, MapColor.QUARTZ, MapColor.STONE, MapColor.STONE};
        for (int i = 0; i < stones.length; i++) {
            boolean polished = stones[i].startsWith("polished");
            stoneFamily(registry, stones[i], Blocks.STONE.getStateFromMeta(i + 1), colors[i], 1.5F, 6.0F, true, true, !polished);
        }
        stoneFamily(registry, "mossy_cobblestone", Blocks.MOSSY_COBBLESTONE.getDefaultState(), MapColor.STONE, 2.0F, 6.0F, true, true, false);
        stoneFamily(registry, "mossy_stone_brick", Blocks.STONEBRICK.getStateFromMeta(1), MapColor.STONE, 1.5F, 6.0F, true, true, true);
        if (SMOOTH_SANDSTONE != null) {
            stoneFamily(registry, "smooth_sandstone", SMOOTH_SANDSTONE.getDefaultState(), MapColor.SAND, 2.0F, 6.0F, true, true, false);
            stoneFamily(registry, "smooth_red_sandstone", SMOOTH_RED_SANDSTONE.getDefaultState(), MapColor.ADOBE, 2.0F, 6.0F, true, true, false);
            stoneFamily(registry, "smooth_quartz", SMOOTH_QUARTZ.getDefaultState(), MapColor.QUARTZ, 2.0F, 6.0F, true, true, false);
        }
        stoneFamily(registry, "end_stone_brick", Blocks.END_BRICKS.getDefaultState(), MapColor.SAND, 3.0F, 9.0F, true, true, true);
        stoneFamily(registry, "red_nether_brick", Blocks.RED_NETHER_BRICK.getDefaultState(), MapColor.NETHERRACK, 2.0F, 6.0F, true, true, true);
        stoneFamily(registry, "cut_sandstone", Blocks.SANDSTONE.getStateFromMeta(2), MapColor.SAND, 2.0F, 6.0F, false, true, false);
        stoneFamily(registry, "cut_red_sandstone", Blocks.RED_SANDSTONE.getStateFromMeta(2), MapColor.ADOBE, 2.0F, 6.0F, false, true, false);
        stoneFamily(registry, "brick", Blocks.BRICK_BLOCK.getDefaultState(), MapColor.RED, 2.0F, 6.0F, false, false, true);
        stoneFamily(registry, "prismarine", Blocks.PRISMARINE.getDefaultState(), MapColor.CYAN, 1.5F, 6.0F, false, false, true);
        stoneFamily(registry, "red_sandstone", Blocks.RED_SANDSTONE.getDefaultState(), MapColor.ADOBE, 0.8F, 0.8F, false, false, true);
        stoneFamily(registry, "sandstone", Blocks.SANDSTONE.getDefaultState(), MapColor.SAND, 0.8F, 0.8F, false, false, true);
        stoneFamily(registry, "stone_brick", Blocks.STONEBRICK.getDefaultState(), MapColor.STONE, 1.5F, 6.0F, false, false, true);
        stoneFamily(registry, "nether_brick", Blocks.NETHER_BRICK.getDefaultState(), MapColor.NETHERRACK, 2.0F, 6.0F, false, false, true);
    }

    /** A stone's stairs (from its state), slab and wall, as 1.14 and 1.16 add them; the slab's hardness is 1.16's. */
    private static void stoneFamily(IForgeRegistry<Block> registry, String prefix, net.minecraft.block.state.IBlockState base, MapColor mapColor, float slabHardness,
                                    float slabResistance116, boolean stairs, boolean slab, boolean wall) {
        if (stairs) register(registry, prefix + "_stairs", new BlockPortiumStairs(base));
        if (slab) registerSlab(registry, prefix + "_slab", mapColor, slabHardness, slabResistance116, base.getBlock().getSoundType());
        if (wall) register(registry, prefix + "_wall", new BlockPortiumWall(base.getBlock(), mapColor));
    }

    /** 1.14's signs of 1.12's other woods (oak's is 1.12's own). */
    private static void registerWoodSigns(IForgeRegistry<Block> registry) {
        for (String wood : new String[] {"spruce", "birch", "jungle", "acacia", "dark_oak"}) {
            com.michaelsebero.portium.block.sign.PortiumSigns.Wall wall = new com.michaelsebero.portium.block.sign.PortiumSigns.Wall(wood);
            com.michaelsebero.portium.block.sign.PortiumSigns.Standing standing = new com.michaelsebero.portium.block.sign.PortiumSigns.Standing(wood);
            registerNoItem(registry, wood + "_wall_sign", wall);
            register(registry, wood + "_sign", standing,
                    block -> new com.michaelsebero.portium.item.ItemPortiumSign(standing, wall).setTranslationKey(Tags.MOD_ID + "." + wood + "_sign"));
            SIGNS.add(standing);
        }
    }

    /** Portium's standing signs (crimson, warped, and 1.14's woods), for the oak sign's recipe to narrow to oak. */
    public static final List<Block> SIGNS = new ArrayList<>();

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

    /** Planks by wood (1.16's crimson and warped), for recipes and ore names. */
    public static final java.util.Map<String, Block> NETHER_PLANKS = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, Block> NETHER_SLABS = new java.util.LinkedHashMap<>();
    public static final java.util.Map<String, Block> NETHER_STAIRS = new java.util.LinkedHashMap<>();

    /**
     * A Nether wood's set (1.16): planks, slab, stairs, fence, fence gate, door, trapdoor, button, pressure plate and
     * sign, none of which burn (Nether wood).
     */
    private static void registerNetherWood(IForgeRegistry<Block> registry, String wood, MapColor mapColor) {
        Block planks = register(registry, wood + "_planks",
                new BlockSimple(com.michaelsebero.portium.block.nether.NetherMaterials.NETHER_WOOD, mapColor, SoundType.WOOD).setHardness(2.0F).setResistance(5.0F));
        planks.setHarvestLevel("axe", 0);
        NETHER_PLANKS.put(wood, planks);
        BlockPortiumSlab half = new BlockPortiumSlab.Half(com.michaelsebero.portium.block.nether.NetherMaterials.NETHER_WOOD, mapColor).withSound(SoundType.WOOD);
        half.setHardness(2.0F).setResistance(5.0F);
        half.setHarvestLevel("axe", 0);
        BlockPortiumSlab doubleSlab = new BlockPortiumSlab.Double(com.michaelsebero.portium.block.nether.NetherMaterials.NETHER_WOOD, mapColor, half)
                .withSound(SoundType.WOOD);
        doubleSlab.setHardness(2.0F).setResistance(5.0F);
        doubleSlab.setHarvestLevel("axe", 0);
        register(registry, wood + "_slab", half, block -> new ItemSlab(block, half, doubleSlab));
        registerNoItem(registry, wood + "_slab_double", doubleSlab);
        NETHER_SLABS.put(wood, half);
        NETHER_STAIRS.put(wood, register(registry, wood + "_stairs", new BlockPortiumStairs(planks.getDefaultState())));
        register(registry, wood + "_fence", new com.michaelsebero.portium.block.building.BlockPortiumFence(mapColor));
        register(registry, wood + "_fence_gate", new com.michaelsebero.portium.block.building.BlockPortiumFenceGate(mapColor));
        register(registry, wood + "_door", new com.michaelsebero.portium.block.building.BlockPortiumDoor(mapColor),
                block -> new net.minecraft.item.ItemDoor(block).setTranslationKey(Tags.MOD_ID + "." + wood + "_door"));
        register(registry, wood + "_trapdoor", new BlockWoodTrapDoor(mapColor));
        register(registry, wood + "_button", new BlockWoodButton());
        register(registry, wood + "_pressure_plate", new BlockWoodPressurePlate(mapColor));
        com.michaelsebero.portium.block.sign.PortiumSigns.Wall wall = new com.michaelsebero.portium.block.sign.PortiumSigns.Wall(wood);
        com.michaelsebero.portium.block.sign.PortiumSigns.Standing standing = new com.michaelsebero.portium.block.sign.PortiumSigns.Standing(wood);
        registerNoItem(registry, wood + "_wall_sign", wall);
        register(registry, wood + "_sign", standing,
                block -> new com.michaelsebero.portium.item.ItemPortiumSign(standing, wall).setTranslationKey(Tags.MOD_ID + "." + wood + "_sign"));
        SIGNS.add(standing);
    }

    private static BlockPortiumSlab registerSlab(IForgeRegistry<Block> registry, String name, MapColor mapColor) {
        return registerSlab(registry, name, mapColor, 1.5F, 6.0F, SoundType.STONE);
    }

    /** A stone slab and its double block, with 1.16's hardness and blast resistance. */
    private static BlockPortiumSlab registerSlab(IForgeRegistry<Block> registry, String name, MapColor mapColor, float hardness, float resistance116,
                                                 SoundType sound) {
        BlockPortiumSlab half = new BlockPortiumSlab.Half(Material.ROCK, mapColor).withSound(sound);
        half.setHardness(hardness).setResistance(resistance116 * 5.0F / 3.0F);
        half.setHarvestLevel("pickaxe", 0);
        BlockPortiumSlab doubleSlab = new BlockPortiumSlab.Double(Material.ROCK, mapColor, half).withSound(sound);
        doubleSlab.setHardness(hardness).setResistance(resistance116 * 5.0F / 3.0F);
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
        block.setRegistryName(PortiumNames.of(registry, name));
        block.setTranslationKey(Tags.MOD_ID + "." + name);
        registry.register(block);
        PortiumNames.alias(registry, block.getRegistryName());
        ALL.add(block);
        return block;
    }

    private PortiumBlocks() {}
}
