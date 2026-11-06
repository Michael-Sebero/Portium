package com.michaelsebero.portium.item;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.init.PortiumItems;
import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.crafting.CompoundIngredient;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.OreIngredient;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Making 1.14's dye items ({@link ItemDye114}) dye what 1.12's dyes dye. Forge's own dye handling (banners, shulker
 * boxes, fireworks, leather) goes by the ore dictionary; vanilla's crafting recipes that colour things (wool, glass,
 * terracotta, concrete powder, beds, mixing dyes) name 1.12's dye items, so those recipes also take the new dye. Recipes
 * using lapis, bone meal, cocoa or ink sacs as themselves (lapis blocks, cookies, books) don't, as in 1.14.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class Dyes114 {

    /** The dye colour of a dye stack, 1.12's or one of 1.14's separate dyes; null for anything else. */
    @Nullable
    public static EnumDyeColor colorOf(net.minecraft.item.ItemStack stack) {
        if (stack.getItem() instanceof ItemDye114) return ((ItemDye114) stack.getItem()).getColor();
        if (stack.getItem() == net.minecraft.init.Items.DYE) return EnumDyeColor.byDyeDamage(stack.getMetadata());
        return null;
    }

    /** The new dye for a 1.12 dye item, if it has one. */
    @Nullable
    public static Item forLegacy(Item item, int meta) {
        if (item != Items.DYE) return null;
        if (meta == EnumDyeColor.WHITE.getDyeDamage()) return PortiumItems.WHITE_DYE;
        if (meta == EnumDyeColor.BLUE.getDyeDamage()) return PortiumItems.BLUE_DYE;
        if (meta == EnumDyeColor.BROWN.getDyeDamage()) return PortiumItems.BROWN_DYE;
        if (meta == EnumDyeColor.BLACK.getDyeDamage()) return PortiumItems.BLACK_DYE;
        return null;
    }

    /** Whether a recipe making this item uses dyes as dyes. */
    public static boolean isColouring(Item result) {
        Block block = Block.getBlockFromItem(result);
        return result == Items.DYE || result == Items.BED || block == Blocks.WOOL || block == Blocks.STAINED_GLASS || block == Blocks.STAINED_GLASS_PANE
                || block == Blocks.STAINED_HARDENED_CLAY || block == Blocks.CONCRETE_POWDER || block == Blocks.CARPET;
    }

    public static void registerOreDictionary() {
        for (Item dye : new Item[] {PortiumItems.WHITE_DYE, PortiumItems.BLUE_DYE, PortiumItems.BROWN_DYE, PortiumItems.BLACK_DYE}) {
            if (dye == null) continue;
            EnumDyeColor color = ((ItemDye114) dye).getColor();
            OreDictionary.registerOre("dye" + upperFirst(color.getTranslationKey()), dye);
            OreDictionary.registerOre("dye", dye);
        }
    }

    private static String upperFirst(String name) {
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    /** After the game's JSON recipes load: colouring recipes that take a 1.12 dye take the new one too. */
    public static void extendRecipes() {
        if (PortiumItems.BLUE_DYE == null) return;
        int changed = 0;
        for (IRecipe recipe : ForgeRegistries.RECIPES) {
            if (!isColouring(recipe.getRecipeOutput().getItem())) continue;
            NonNullList<Ingredient> ingredients;
            try {
                ingredients = recipe.getIngredients();
            } catch (RuntimeException e) {
                continue;
            }
            for (int i = 0; i < ingredients.size(); i++) {
                Ingredient extended = extend(ingredients.get(i));
                if (extended == null) continue;
                try {
                    ingredients.set(i, extended);
                    changed++;
                } catch (UnsupportedOperationException e) {
                    break;
                }
            }
        }
        Portium.LOGGER.debug("1.14's dyes added to {} recipe ingredients", changed);
    }

    /** An ingredient that also takes the new dye, or null if it names no 1.12 dye that has one. */
    @Nullable
    private static Ingredient extend(Ingredient ingredient) {
        if (ingredient instanceof OreIngredient) return null;
        Set<Item> extra = new HashSet<>();
        for (ItemStack stack : ingredient.getMatchingStacks()) {
            if (stack.getMetadata() == OreDictionary.WILDCARD_VALUE) continue;
            Item dye = forLegacy(stack.getItem(), stack.getMetadata());
            if (dye != null && !ingredient.apply(new ItemStack(dye))) extra.add(dye);
        }
        if (extra.isEmpty()) return null;
        Ingredient added = Ingredient.fromItems(extra.toArray(new Item[0]));
        return new CompoundIngredient(Arrays.asList(ingredient, added)) {};
    }

    /** 1.14 dyes colour tamed wolves' collars, as 1.12's do. */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ItemDye114) || !(event.getTarget() instanceof EntityWolf)) return;
        EntityWolf wolf = (EntityWolf) event.getTarget();
        EntityPlayer player = event.getEntityPlayer();
        EnumDyeColor color = ((ItemDye114) stack.getItem()).getColor();
        if (!wolf.isTamed() || wolf.getCollarColor() == color) return;
        if (!event.getWorld().isRemote) wolf.setCollarColor(color);
        if (!player.capabilities.isCreativeMode) stack.shrink(1);
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    private Dyes114() {}
}
