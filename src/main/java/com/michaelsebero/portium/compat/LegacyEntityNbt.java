package com.michaelsebero.portium.compat;

import com.michaelsebero.portium.command.arguments.selector.EntityTypes;
import com.michaelsebero.portium.entity.ItemFrames;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Rewrites entity NBT written for 1.13+ into 1.12's layout: entity ids for the mobs Portium adds, JSON custom names,
 * items with flattened ids, and block states for falling blocks, minecart contents and endermen.
 */
public final class LegacyEntityNbt {

    private static final String[] ITEM_KEYS = {"Item", "SaddleItem", "ArmorItem", "DecorItem", "FireworksItem", "Potion"};
    private static final String[] ITEM_LIST_KEYS = {"Items", "HandItems", "ArmorItems", "Inventory", "EnderItems"};

    public static NBTTagCompound convert(NBTTagCompound modern) {
        NBTTagCompound tag = modern.copy();
        // 1.16 keeps UUIDs as four ints; 1.12 as two longs, and an owner's as a string.
        if (tag.hasKey("UUID", 11)) {
            java.util.UUID uuid = uuid(tag.getIntArray("UUID"));
            tag.removeTag("UUID");
            if (uuid != null) tag.setUniqueId("UUID", uuid);
        }
        if (tag.hasKey("Owner", 11)) {
            java.util.UUID owner = uuid(tag.getIntArray("Owner"));
            tag.removeTag("Owner");
            if (owner != null) tag.setString("OwnerUUID", owner.toString());
        }
        // 1.16 names a player's dimension; 1.12 numbers it.
        if (tag.hasKey("Dimension", 8)) tag.setInteger("Dimension", dimension(tag.getString("Dimension")));
        if (tag.hasKey("id", 8)) {
            ResourceLocation legacy = EntityTypes.toLegacy(new ResourceLocation(tag.getString("id")));
            if (legacy != null) tag.setString("id", legacy.toString());
            if ("minecraft:item_frame".equals(tag.getString("id"))) ItemFrames.convertModernFacing(tag);
        }
        if (tag.hasKey("CustomName", 8)) tag.setString("CustomName", LegacyItemNbt.textToLegacy(tag.getString("CustomName")));
        for (String key : ITEM_KEYS) {
            if (tag.hasKey(key, 10)) tag.setTag(key, LegacyItemNbt.convertStack(tag.getCompoundTag(key)));
        }
        for (String key : ITEM_LIST_KEYS) {
            if (!tag.hasKey(key, 9)) continue;
            NBTTagList list = tag.getTagList(key, 10);
            NBTTagList converted = new NBTTagList();
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound stack = list.getCompoundTagAt(i);
                converted.appendTag(stack.isEmpty() ? stack : LegacyItemNbt.convertStack(stack));
            }
            tag.setTag(key, converted);
        }
        if (tag.hasKey("BlockState", 10)) {
            IBlockState state = state(tag.getCompoundTag("BlockState"));
            tag.removeTag("BlockState");
            tag.setString("Block", Block.REGISTRY.getNameForObject(state.getBlock()).toString());
            tag.setByte("Data", (byte) state.getBlock().getMetaFromState(state));
        }
        if (tag.hasKey("DisplayState", 10)) {
            IBlockState state = state(tag.getCompoundTag("DisplayState"));
            tag.removeTag("DisplayState");
            tag.setString("DisplayTile", Block.REGISTRY.getNameForObject(state.getBlock()).toString());
            tag.setInteger("DisplayData", state.getBlock().getMetaFromState(state));
        }
        if (tag.hasKey("carriedBlockState", 10)) {
            IBlockState state = state(tag.getCompoundTag("carriedBlockState"));
            tag.removeTag("carriedBlockState");
            tag.setShort("carried", (short) Block.getIdFromBlock(state.getBlock()));
            tag.setShort("carriedData", (short) state.getBlock().getMetaFromState(state));
        }
        if (tag.hasKey("inBlockState", 10)) {
            IBlockState state = state(tag.getCompoundTag("inBlockState"));
            tag.removeTag("inBlockState");
            tag.setString("inTile", Block.REGISTRY.getNameForObject(state.getBlock()).toString());
            tag.setByte("inData", (byte) state.getBlock().getMetaFromState(state));
        }
        if (tag.hasKey("Motive", 8)) tag.setString("Motive", paintingToLegacy(tag.getString("Motive")));
        if (tag.hasKey("Offers", 10)) {
            NBTTagList recipes = tag.getCompoundTag("Offers").getTagList("Recipes", 10);
            for (int i = 0; i < recipes.tagCount(); i++) {
                NBTTagCompound recipe = recipes.getCompoundTagAt(i);
                for (String key : new String[] {"buy", "buyB", "sell"}) {
                    if (recipe.hasKey(key, 10)) recipe.setTag(key, LegacyItemNbt.convertStack(recipe.getCompoundTag(key)));
                }
            }
        }
        if (tag.hasKey("TileEntityData", 10)) tag.setTag("TileEntityData", LegacyBlockEntityNbt.convert(tag.getCompoundTag("TileEntityData"), null, new HashMap<>()));
        for (String key : new String[] {"ShoulderEntityLeft", "ShoulderEntityRight"}) {
            if (tag.hasKey(key, 10) && !tag.getCompoundTag(key).isEmpty()) tag.setTag(key, convert(tag.getCompoundTag(key)));
        }
        if (tag.hasKey("RootVehicle", 10) && tag.getCompoundTag("RootVehicle").hasKey("Entity", 10)) {
            NBTTagCompound vehicle = tag.getCompoundTag("RootVehicle");
            vehicle.setTag("Entity", convert(vehicle.getCompoundTag("Entity")));
        }
        if (tag.hasKey("Passengers", 9)) {
            NBTTagList passengers = tag.getTagList("Passengers", 10);
            NBTTagList converted = new NBTTagList();
            for (int i = 0; i < passengers.tagCount(); i++) converted.appendTag(convert(passengers.getCompoundTagAt(i)));
            tag.setTag("Passengers", converted);
        }
        return tag;
    }

    @Nullable
    private static java.util.UUID uuid(int[] ints) {
        if (ints.length != 4) return null;
        return new java.util.UUID((long) ints[0] << 32 | ints[1] & 0xFFFFFFFFL, (long) ints[2] << 32 | ints[3] & 0xFFFFFFFFL);
    }

    /** 1.12's number for a dimension 1.16 names; other dimensions (a data pack's) put the player in the Overworld. */
    static int dimension(String name) {
        switch (name) {
            case "minecraft:the_nether":
                return -1;
            case "minecraft:the_end":
                return 1;
            default:
                return 0;
        }
    }

    /** 1.13 names paintings like {@code minecraft:skull_and_roses}; 1.12 like {@code SkullAndRoses}. */
    private static String paintingToLegacy(String motive) {
        String name = motive.substring(motive.indexOf(':') + 1).replace("_", "");
        for (EntityPainting.EnumArt art : EntityPainting.EnumArt.values()) {
            if (art.title.equalsIgnoreCase(name)) return art.title;
        }
        return motive;
    }

    /** A 1.13 block state compound ({@code {Name:..., Properties:{...}}}) as a 1.12 state. */
    public static IBlockState state(NBTTagCompound tag) {
        Map<String, String> properties = new HashMap<>();
        NBTTagCompound props = tag.getCompoundTag("Properties");
        for (String key : props.getKeySet()) properties.put(key, props.getString(key));
        return LegacyBlockStates.convert(tag.getString("Name"), properties);
    }

    private LegacyEntityNbt() {}
}
