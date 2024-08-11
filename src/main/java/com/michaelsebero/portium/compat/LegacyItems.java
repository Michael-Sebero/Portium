package com.michaelsebero.portium.compat;

import com.michaelsebero.portium.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Translates 1.13+ item ids into 1.12 items and metadata, for datapack recipes, loot tables, tags and commands.
 * Block items follow {@link LegacyBlockStates}; the item-only renames (dyes, fish, music discs, heads) are listed here.
 */
public final class LegacyItems {

    private static final Map<String, LegacyItem> RENAMES = new HashMap<>();
    /** Set once the tables are complete; chunks are converted on Forge's chunk-loading threads. */
    private static volatile boolean ready;
    private static boolean initializing;

    @Nullable
    public static LegacyItem resolve(String name) {
        if (!ready) init();
        ResourceLocation id = new ResourceLocation(name);
        LegacyItem renamed = RENAMES.get(id.toString());
        if (renamed != null) return renamed;
        if ("minecraft".equals(id.getNamespace())) {
            if (id.getPath().endsWith("_spawn_egg")) return spawnEgg(id.getPath().substring(0, id.getPath().length() - "_spawn_egg".length()));
            if (LegacyBlockStates.isExplicitlyMapped(id.toString())) {
                LegacyItem block = fromBlock(id.toString());
                if (block != null) return block;
            }
        }
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item != null && item != Items.AIR) return new LegacyItem(item, LegacyItem.ANY_META, null);
        if ("minecraft".equals(id.getNamespace())) {
            Item portium = ForgeRegistries.ITEMS.getValue(new ResourceLocation(Tags.MOD_ID, id.getPath()));
            if (portium != null && portium != Items.AIR) return new LegacyItem(portium, LegacyItem.ANY_META, null);
        }
        return fromBlock(id.toString());
    }

    /** Whether a name is a 1.13 item or block name Portium translates, rather than a 1.12 registry name it passes through. */
    public static boolean isModernName(String name) {
        if (!ready) init();
        ResourceLocation id = new ResourceLocation(name);
        return RENAMES.containsKey(id.toString()) || LegacyBlockStates.isExplicitlyMapped(id.toString())
                || ("minecraft".equals(id.getNamespace()) && id.getPath().endsWith("_spawn_egg"));
    }

    /**
     * The item for a block: the one picking the block gives (podzol, not the dirt it drops). Picking may need the
     * world, which isn't available here; then the item the block drops stands in.
     */
    @Nullable
    private static LegacyItem fromBlock(String name) {
        if (!LegacyBlockStates.exists(name)) return null;
        IBlockState state = LegacyBlockStates.convert(name, Collections.emptyMap());
        Block block = state.getBlock();
        Item item = Item.getItemFromBlock(block);
        if (item == Items.AIR) return null;
        int meta = item.getHasSubtypes() ? block.damageDropped(state) : 0;
        if (item.getHasSubtypes()) {
            try {
                ItemStack picked = block.getItem(null, BlockPos.ORIGIN, state);
                if (picked.getItem() == item) meta = picked.getMetadata();
            } catch (RuntimeException ignored) {
                // Needs the world; keep the dropped item's data value.
            }
        }
        return new LegacyItem(item, meta, null);
    }

    /** 1.13 spawn eggs are separate items; 1.12 has one spawn egg that stores the entity in NBT. */
    @Nullable
    private static LegacyItem spawnEgg(String entity) {
        ResourceLocation type = new ResourceLocation(entity);
        if (!EntityList.isRegistered(type)) type = new ResourceLocation(Tags.MOD_ID, entity);
        if (!EntityList.isRegistered(type)) return null;
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound entityTag = new NBTTagCompound();
        entityTag.setString("id", type.toString());
        tag.setTag("EntityTag", entityTag);
        return new LegacyItem(Items.SPAWN_EGG, 0, tag);
    }

    private static void rename(String name, Item item, int meta) {
        RENAMES.put("minecraft:" + name, new LegacyItem(item, meta, null));
    }

    private static synchronized void init() {
        if (ready || initializing) return;
        initializing = true;
        rename("coal", Items.COAL, 0);
        rename("charcoal", Items.COAL, 1);
        rename("golden_apple", Items.GOLDEN_APPLE, 0);
        rename("enchanted_golden_apple", Items.GOLDEN_APPLE, 1);
        rename("cod", Items.FISH, 0);
        rename("salmon", Items.FISH, 1);
        rename("tropical_fish", Items.FISH, 2);
        rename("pufferfish", Items.FISH, 3);
        rename("cooked_cod", Items.COOKED_FISH, 0);
        rename("cooked_salmon", Items.COOKED_FISH, 1);
        // 1.13's dye names, then the 1.14 ones, all onto 1.12's dye metadata.
        String[] dyes13 = {"ink_sac", "rose_red", "cactus_green", "cocoa_beans", "lapis_lazuli", "purple_dye", "cyan_dye", "light_gray_dye", "gray_dye",
                "pink_dye", "lime_dye", "dandelion_yellow", "light_blue_dye", "magenta_dye", "orange_dye", "bone_meal"};
        for (int i = 0; i < dyes13.length; i++) rename(dyes13[i], Items.DYE, i);
        rename("black_dye", Items.DYE, 0);
        rename("red_dye", Items.DYE, 1);
        rename("green_dye", Items.DYE, 2);
        rename("brown_dye", Items.DYE, 3);
        rename("blue_dye", Items.DYE, 4);
        rename("yellow_dye", Items.DYE, 11);
        rename("white_dye", Items.DYE, 15);
        rename("oak_door", Items.OAK_DOOR, 0);
        rename("oak_boat", Items.BOAT, 0);
        rename("sign", Items.SIGN, 0);
        rename("oak_sign", Items.SIGN, 0);
        rename("sugar_cane", Items.REEDS, 0);
        rename("melon_slice", Items.MELON, 0);
        rename("glistering_melon_slice", Items.SPECKLED_MELON, 0);
        rename("firework_rocket", Items.FIREWORKS, 0);
        rename("firework_star", Items.FIREWORK_CHARGE, 0);
        rename("nether_brick", Items.NETHERBRICK, 0);
        rename("popped_chorus_fruit", Items.CHORUS_FRUIT_POPPED, 0);
        rename("repeater", Items.REPEATER, 0);
        rename("comparator", Items.COMPARATOR, 0);
        rename("cauldron", Items.CAULDRON, 0);
        rename("brewing_stand", Items.BREWING_STAND, 0);
        rename("flower_pot", Items.FLOWER_POT, 0);
        rename("cake", Items.CAKE, 0);
        rename("hopper", Item.getItemFromBlock(Blocks.HOPPER), 0);
        rename("wheat", Items.WHEAT, 0);
        rename("nether_wart", Items.NETHER_WART, 0);
        rename("iron_door", Items.IRON_DOOR, 0);
        rename("redstone", Items.REDSTONE, 0);
        rename("string", Items.STRING, 0);
        String[] heads = {"skeleton_skull", "wither_skeleton_skull", "zombie_head", "player_head", "creeper_head", "dragon_head"};
        for (int i = 0; i < heads.length; i++) rename(heads[i], Items.SKULL, i);
        for (String disc : new String[] {"13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal", "strad", "ward", "11", "wait"}) {
            Item record = ForgeRegistries.ITEMS.getValue(new ResourceLocation("record_" + disc));
            if (record != null) rename("music_disc_" + disc, record, 0);
        }
        for (EnumDyeColor color : EnumDyeColor.values()) {
            String name = color == EnumDyeColor.SILVER ? "light_gray" : color.getName();
            rename(name + "_bed", Items.BED, color.getMetadata());
            rename(name + "_banner", Items.BANNER, color.getDyeDamage());
        }
        ready = true;
    }

    private LegacyItems() {}
}
