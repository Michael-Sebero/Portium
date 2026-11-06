package com.michaelsebero.portium.init;

import com.michaelsebero.portium.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPlanks;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.potion.PotionHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.common.crafting.CompoundIngredient;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.OreIngredient;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Recipes that can't be plain JSON: furnace, brewing, fuel, ore dictionary, and the vanilla oak recipes
 * that 1.13 narrowed to oak planks once the other woods got their own buttons, pressure plates and trapdoors.
 * Ordinary crafting recipes live in {@code assets/portium/recipes}.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PortiumRecipes {

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        IForgeRegistry<IRecipe> registry = event.getRegistry();
        ItemStack oakPlanks = new ItemStack(Blocks.PLANKS, 1, BlockPlanks.EnumType.OAK.getMetadata());
        // With every wood's own, 1.12's oak ones take oak planks only.
        if (!PortiumBlocks.BUTTONS.isEmpty()) {
            override(registry, "wooden_button", new ShapelessOreRecipe(null, new ItemStack(Blocks.WOODEN_BUTTON), oakPlanks));
            override(registry, "wooden_pressure_plate", new ShapedOreRecipe(null, new ItemStack(Blocks.WOODEN_PRESSURE_PLATE), "##", '#', oakPlanks));
        }
        if (!PortiumBlocks.TRAPDOORS.isEmpty()) {
            override(registry, "trapdoor", new ShapedOreRecipe(null, new ItemStack(Blocks.TRAPDOOR, 2), "###", "###", '#', oakPlanks));
        }
        // With other woods' signs (1.14's, and 1.16's crimson and warped), 1.12's sign is oak's only.
        if (!PortiumBlocks.SIGNS.isEmpty()) {
            override(registry, "sign", new ShapedOreRecipe(null, new ItemStack(net.minecraft.init.Items.SIGN, 3), "###", "###", " X ", '#', oakPlanks,
                    'X', "stickWood"));
        }
        registerFireRecipes(registry);
        if (PortiumItems.LEATHER_HORSE_ARMOR != null) {
            registry.register(new com.michaelsebero.portium.recipe.RecipeHorseArmorDye().setRegistryName(new net.minecraft.util.ResourceLocation(Tags.MOD_ID, "horse_armor_dye")));
        }
        if (PortiumItems.SUSPICIOUS_STEW != null) {
            registry.register(new com.michaelsebero.portium.recipe.RecipeSuspiciousStew().setRegistryName(new net.minecraft.util.ResourceLocation(Tags.MOD_ID, "suspicious_stew")));
        }
    }

    /**
     * Recipes naming 1.16's tags of things Portium may have switched off: soul fire base blocks (soul sand, soul soil),
     * logs (with 1.16's stems and hyphae) and coals (coal, charcoal).
     */
    private static void registerFireRecipes(IForgeRegistry<IRecipe> registry) {
        Ingredient coals = Ingredient.fromStacks(new ItemStack(Items.COAL, 1, OreDictionary.WILDCARD_VALUE));
        Ingredient soulFireBase = soulFireBase();
        if (PortiumBlocks.SOUL_TORCH != null) {
            add(registry, "soul_torch", new ShapedOreRecipe(null, new ItemStack(PortiumBlocks.SOUL_TORCH, 4), "X", "#", "S", 'X', coals, '#', "stickWood",
                    'S', soulFireBase));
        }
        if (PortiumBlocks.SMOKER != null) {
            add(registry, "smoker", new ShapedOreRecipe(null, new ItemStack(PortiumBlocks.SMOKER), " # ", "#X#", " # ", '#', logs(), 'X', Blocks.FURNACE));
        }
        if (PortiumBlocks.CAMPFIRE != null) {
            Ingredient logs = logs();
            add(registry, "campfire", new ShapedOreRecipe(null, new ItemStack(PortiumBlocks.CAMPFIRE), " S ", "SCS", "LLL", 'S', "stickWood", 'C', coals,
                    'L', logs));
            add(registry, "soul_campfire", new ShapedOreRecipe(null, new ItemStack(PortiumBlocks.SOUL_CAMPFIRE), " S ", "S#S", "LLL", 'S', "stickWood",
                    '#', soulFireBase, 'L', logs));
        }
    }

    /** 1.16's {@code soul_fire_base_blocks}. */
    private static Ingredient soulFireBase() {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(Blocks.SOUL_SAND));
        if (PortiumBlocks.SOUL_SOIL != null) stacks.add(new ItemStack(PortiumBlocks.SOUL_SOIL));
        return Ingredient.fromStacks(stacks.toArray(new ItemStack[0]));
    }

    /** 1.16's {@code logs}: every log and wood ("logWood"), and the Nether's stems and hyphae, which don't burn. */
    private static Ingredient logs() {
        List<ItemStack> stems = new ArrayList<>();
        for (Block block : new Block[] {PortiumBlocks.CRIMSON_STEM, PortiumBlocks.WARPED_STEM, PortiumBlocks.STRIPPED_CRIMSON_STEM, PortiumBlocks.STRIPPED_WARPED_STEM,
                PortiumBlocks.CRIMSON_HYPHAE, PortiumBlocks.WARPED_HYPHAE, PortiumBlocks.STRIPPED_CRIMSON_HYPHAE, PortiumBlocks.STRIPPED_WARPED_HYPHAE}) {
            if (block != null) stems.add(new ItemStack(block));
        }
        Ingredient logWood = new OreIngredient("logWood");
        if (stems.isEmpty()) return logWood;
        List<Ingredient> parts = Arrays.asList(logWood, Ingredient.fromStacks(stems.toArray(new ItemStack[0])));
        return new CompoundIngredient(parts) {};
    }

    private static void add(IForgeRegistry<IRecipe> registry, String name, IRecipe recipe) {
        registry.register(recipe.setRegistryName(new ResourceLocation(Tags.MOD_ID, name)));
    }

    private static void override(IForgeRegistry<IRecipe> registry, String vanillaName, IRecipe recipe) {
        registry.register(recipe.setRegistryName(new ResourceLocation("minecraft", vanillaName)));
    }

    /** Called once items exist, before JSON recipes are loaded, so recipes may use these entries. */
    static void registerOreDictionary() {
        for (Block log : PortiumBlocks.STRIPPED_LOGS.values()) OreDictionary.registerOre("logWood", log);
        for (Block wood : PortiumBlocks.WOOD.values()) OreDictionary.registerOre("logWood", wood);
        for (Block wood : PortiumBlocks.STRIPPED_WOOD.values()) OreDictionary.registerOre("logWood", wood);
        OreDictionary.registerOre("blockPrismarine", Blocks.PRISMARINE);
        // 1.16's Nether planks are planks to every planks recipe (sticks, crafting tables, chests, tools), as in 1.16.
        for (Block planks : PortiumBlocks.NETHER_PLANKS.values()) OreDictionary.registerOre("plankWood", planks);
        // 1.16's wooden_slabs and wooden_stairs tags include them (barrels, composters, ...).
        for (Block slab : PortiumBlocks.NETHER_SLABS.values()) OreDictionary.registerOre("slabWood", slab);
        for (Block stairs : PortiumBlocks.NETHER_STAIRS.values()) OreDictionary.registerOre("stairWood", stairs);
        if (PortiumItems.KELP != null) OreDictionary.registerOre("cropKelp", PortiumItems.KELP);
        com.michaelsebero.portium.item.Dyes114.registerOreDictionary();
    }

    public static void registerFurnaceAndBrewing() {
        // The game's JSON recipes are loaded by now: let 1.14's dyes into the colouring ones.
        com.michaelsebero.portium.item.Dyes114.extendRecipes();
        if (PortiumItems.KELP != null) GameRegistry.addSmelting(PortiumItems.KELP, new ItemStack(PortiumItems.DRIED_KELP), 0.1F);
        // 1.16 smelts (or blasts) ancient debris into netherite scrap.
        if (PortiumBlocks.ANCIENT_DEBRIS != null && PortiumItems.NETHERITE_SCRAP != null) {
            GameRegistry.addSmelting(PortiumBlocks.ANCIENT_DEBRIS, new ItemStack(PortiumItems.NETHERITE_SCRAP), 2.0F);
        }
        // 1.16's cracked bricks.
        if (PortiumBlocks.CRACKED_NETHER_BRICKS != null) {
            GameRegistry.addSmelting(Blocks.NETHER_BRICK, new ItemStack(PortiumBlocks.CRACKED_NETHER_BRICKS), 0.1F);
        }
        if (PortiumBlocks.CRACKED_POLISHED_BLACKSTONE_BRICKS != null) {
            GameRegistry.addSmelting(PortiumBlocks.POLISHED_BLACKSTONE_BRICKS, new ItemStack(PortiumBlocks.CRACKED_POLISHED_BLACKSTONE_BRICKS), 0.1F);
        }
        if (PortiumBlocks.SMOOTH_STONE != null) {
            GameRegistry.addSmelting(new ItemStack(Blocks.STONE, 1, 0), new ItemStack(PortiumBlocks.SMOOTH_STONE), 0.1F);
            GameRegistry.addSmelting(new ItemStack(Blocks.SANDSTONE, 1, 0), new ItemStack(PortiumBlocks.SMOOTH_SANDSTONE), 0.1F);
            GameRegistry.addSmelting(new ItemStack(Blocks.RED_SANDSTONE, 1, 0), new ItemStack(PortiumBlocks.SMOOTH_RED_SANDSTONE), 0.1F);
            GameRegistry.addSmelting(new ItemStack(Blocks.QUARTZ_BLOCK, 1, 0), new ItemStack(PortiumBlocks.SMOOTH_QUARTZ), 0.1F);
        }
        ItemStack charcoal = new ItemStack(Items.COAL, 1, 1);
        for (Block log : PortiumBlocks.STRIPPED_LOGS.values()) GameRegistry.addSmelting(log, charcoal, 0.15F);
        for (Block wood : PortiumBlocks.WOOD.values()) GameRegistry.addSmelting(wood, charcoal, 0.15F);
        for (Block wood : PortiumBlocks.STRIPPED_WOOD.values()) GameRegistry.addSmelting(wood, charcoal, 0.15F);

        Ingredient redstone = Ingredient.fromItem(Items.REDSTONE);
        if (PortiumPotions.SLOW_FALLING_TYPE != null) {
            if (PortiumItems.PHANTOM_MEMBRANE != null) PotionHelper.addMix(PotionTypes.AWKWARD, PortiumItems.PHANTOM_MEMBRANE, PortiumPotions.SLOW_FALLING_TYPE);
            PotionHelper.addMix(PortiumPotions.SLOW_FALLING_TYPE, redstone, PortiumPotions.LONG_SLOW_FALLING_TYPE);
        }
        if (PortiumPotions.TURTLE_MASTER_TYPE != null) {
            if (PortiumItems.TURTLE_HELMET != null) PotionHelper.addMix(PotionTypes.AWKWARD, PortiumItems.TURTLE_HELMET, PortiumPotions.TURTLE_MASTER_TYPE);
            PotionHelper.addMix(PortiumPotions.TURTLE_MASTER_TYPE, redstone, PortiumPotions.LONG_TURTLE_MASTER_TYPE);
            PotionHelper.addMix(PortiumPotions.TURTLE_MASTER_TYPE, Items.GLOWSTONE_DUST, PortiumPotions.STRONG_TURTLE_MASTER_TYPE);
        }
    }

    /** Dried kelp blocks burn for 4001 ticks, a little longer than 20 smelts (1.13). */
    @SubscribeEvent
    public static void onFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        Item item = event.getItemStack().getItem();
        if (PortiumBlocks.DRIED_KELP_BLOCK != null && item == Item.getItemFromBlock(PortiumBlocks.DRIED_KELP_BLOCK)) {
            event.setBurnTime(4001);
        } else if (PortiumBlocks.BAMBOO != null && item == Item.getItemFromBlock(PortiumBlocks.BAMBOO)) {
            // Bamboo: half an item (1.14).
            event.setBurnTime(50);
        } else if (PortiumBlocks.SCAFFOLDING != null && item == Item.getItemFromBlock(PortiumBlocks.SCAFFOLDING)) {
            event.setBurnTime(400);
        }
    }

    private PortiumRecipes() {}
}
