package com.michaelsebero.portium.compat;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rewrites a 1.13 block entity into 1.12's layout. 1.13 moved some data into block names: a bed's or banner's colour,
 * a skull's kind and rotation, a flower pot's plant and a note block's note. Those come back from the block the
 * entity sits on; pots and note blocks, which have no block entity in 1.13, get one made for them.
 */
public final class LegacyBlockEntityNbt {

    private static final List<String> COLORS = Arrays.asList("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
    private static final List<String> SKULLS = Arrays.asList("skeleton", "wither_skeleton", "zombie", "player", "creeper", "dragon");
    private static final String[] ITEM_LIST_KEYS = {"Items"};
    private static final String[] ITEM_KEYS = {"RecordItem"};
    private static final Map<String, String> RENAMED = new HashMap<>();
    /** 1.13's potted plants as 1.12's pot contents: item id and data. */
    private static final Map<String, Object[]> POTTED = new HashMap<>();

    static {
        RENAMED.put("minecraft:trapped_chest", "minecraft:chest");
        RENAMED.put("minecraft:conduit", "portium:conduit");
        String[] flowers = {"poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy"};
        for (int i = 0; i < flowers.length; i++) POTTED.put(flowers[i], new Object[] {"minecraft:red_flower", i});
        POTTED.put("dandelion", new Object[] {"minecraft:yellow_flower", 0});
        String[] woods = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
        for (int i = 0; i < woods.length; i++) POTTED.put(woods[i] + "_sapling", new Object[] {"minecraft:sapling", i});
        POTTED.put("red_mushroom", new Object[] {"minecraft:red_mushroom", 0});
        POTTED.put("brown_mushroom", new Object[] {"minecraft:brown_mushroom", 0});
        POTTED.put("fern", new Object[] {"minecraft:tallgrass", 2});
        POTTED.put("dead_bush", new Object[] {"minecraft:deadbush", 0});
        POTTED.put("cactus", new Object[] {"minecraft:cactus", 0});
    }

    /**
     * Rewrites a 1.13 block entity. {@code block} and {@code properties} describe the 1.13 block at its position, if
     * known.
     */
    public static NBTTagCompound convert(NBTTagCompound modern, @Nullable String block, Map<String, String> properties) {
        NBTTagCompound tag = modern.copy();
        String id = tag.getString("id");
        if (RENAMED.containsKey(id)) tag.setString("id", RENAMED.get(id));
        if (tag.hasKey("CustomName", 8)) tag.setString("CustomName", LegacyItemNbt.textToLegacy(tag.getString("CustomName")));
        for (String key : ITEM_LIST_KEYS) {
            if (!tag.hasKey(key, 9)) continue;
            NBTTagList items = tag.getTagList(key, 10);
            NBTTagList converted = new NBTTagList();
            for (int i = 0; i < items.tagCount(); i++) converted.appendTag(LegacyItemNbt.convertStack(items.getCompoundTagAt(i)));
            tag.setTag(key, converted);
        }
        for (String key : ITEM_KEYS) {
            if (tag.hasKey(key, 10)) tag.setTag(key, LegacyItemNbt.convertStack(tag.getCompoundTag(key)));
        }
        String path = block == null ? "" : block.substring(block.indexOf(':') + 1);
        switch (id) {
            case "minecraft:banner":
                // 1.13 numbers banner colours like wool (white 0); 1.12 like dye (white 15).
                int base = colorOf(path.replace("_wall_banner", "").replace("_banner", ""));
                if (base >= 0) tag.setInteger("Base", 15 - base);
                if (tag.hasKey("Patterns", 9)) {
                    NBTTagList patterns = tag.getTagList("Patterns", 10);
                    for (int i = 0; i < patterns.tagCount(); i++) {
                        NBTTagCompound pattern = patterns.getCompoundTagAt(i);
                        pattern.setInteger("Color", 15 - pattern.getInteger("Color"));
                    }
                }
                break;
            case "minecraft:bed":
                int color = colorOf(path.replace("_bed", ""));
                if (color >= 0) tag.setInteger("color", color);
                break;
            case "minecraft:skull":
                int kind = SKULLS.indexOf(path.replace("_wall_skull", "").replace("_wall_head", "").replace("_skull", "").replace("_head", ""));
                if (kind >= 0) tag.setByte("SkullType", (byte) kind);
                if (properties.containsKey("rotation")) tag.setByte("Rot", (byte) parse(properties.get("rotation")));
                break;
            case "minecraft:mob_spawner":
                if (tag.hasKey("SpawnData", 10)) tag.setTag("SpawnData", LegacyEntityNbt.convert(tag.getCompoundTag("SpawnData")));
                if (tag.hasKey("SpawnPotentials", 9)) {
                    NBTTagList potentials = tag.getTagList("SpawnPotentials", 10);
                    for (int i = 0; i < potentials.tagCount(); i++) {
                        NBTTagCompound potential = potentials.getCompoundTagAt(i);
                        if (potential.hasKey("Entity", 10)) potential.setTag("Entity", LegacyEntityNbt.convert(potential.getCompoundTag("Entity")));
                    }
                }
                break;
            case "minecraft:piston":
                if (tag.hasKey("blockState", 10)) {
                    IBlockState state = LegacyEntityNbt.state(tag.getCompoundTag("blockState"));
                    tag.removeTag("blockState");
                    tag.setInteger("blockId", Block.getIdFromBlock(state.getBlock()));
                    tag.setInteger("blockData", state.getBlock().getMetaFromState(state));
                }
                break;
            default:
        }
        return tag;
    }

    /** The block entity 1.12 keeps for a flower pot, or null if the block isn't one. */
    @Nullable
    public static NBTTagCompound flowerPot(String block, int x, int y, int z) {
        String path = block.substring(block.indexOf(':') + 1);
        if (!path.equals("flower_pot") && !path.startsWith("potted_")) return null;
        Object[] plant = POTTED.get(path.replace("potted_", ""));
        NBTTagCompound tag = position("minecraft:flower_pot", x, y, z);
        tag.setString("Item", plant == null ? "" : (String) plant[0]);
        tag.setInteger("Data", plant == null ? 0 : (Integer) plant[1]);
        return tag;
    }

    /** The block entity 1.12 keeps for a note block's note, or null if the block isn't one. */
    @Nullable
    public static NBTTagCompound noteBlock(String block, Map<String, String> properties, int x, int y, int z) {
        if (!block.endsWith(":note_block")) return null;
        NBTTagCompound tag = position("minecraft:noteblock", x, y, z);
        tag.setByte("note", (byte) parse(properties.get("note")));
        tag.setBoolean("powered", "true".equals(properties.get("powered")));
        return tag;
    }

    private static NBTTagCompound position(String id, int x, int y, int z) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", id);
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        return tag;
    }

    private static int colorOf(String name) {
        return COLORS.indexOf(name);
    }

    private static int parse(@Nullable String number) {
        try {
            return number == null ? 0 : Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private LegacyBlockEntityNbt() {}
}
