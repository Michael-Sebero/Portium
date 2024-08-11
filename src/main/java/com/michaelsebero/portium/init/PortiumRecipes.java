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
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

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
        if (PortiumItems.KELP != null) OreDictionary.registerOre("cropKelp", PortiumItems.KELP);
    }

    public static void registerFurnaceAndBrewing() {
        if (PortiumItems.KELP != null) GameRegistry.addSmelting(PortiumItems.KELP, new ItemStack(PortiumItems.DRIED_KELP), 0.1F);
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
        if (PortiumBlocks.DRIED_KELP_BLOCK != null && event.getItemStack().getItem() == Item.getItemFromBlock(PortiumBlocks.DRIED_KELP_BLOCK)) {
            event.setBurnTime(4001);
        }
    }

    private PortiumRecipes() {}
}
