package com.michaelsebero.portium.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;

/** A 1.13 structure template rewritten as 1.12 stores one (plus Portium's waterlogged list), for 1.12's own template loading. */
public final class LegacyStructureNbt {

    /** The last 1.12.2 data version, so 1.12's data fixers leave the converted template alone. */
    private static final int DATA_VERSION = 1343;

    public static NBTTagCompound convert(NBTTagCompound modern) {
        NBTTagCompound tag = modern.copy();
        // Templates with several palettes (shipwrecks) pick one when placed; 1.12 reads a single palette, the first.
        NBTTagList palette = tag.hasKey("palette", 9) ? tag.getTagList("palette", 10)
                : tag.getTagList("palettes", 9).tagCount() > 0 ? (NBTTagList) tag.getTagList("palettes", 9).get(0) : new NBTTagList();
        NBTTagList legacyPalette = new NBTTagList();
        boolean[] waterlogged = new boolean[palette.tagCount()];
        for (int i = 0; i < palette.tagCount(); i++) {
            NBTTagCompound entry = palette.getCompoundTagAt(i);
            legacyPalette.appendTag(NBTUtil.writeBlockState(new NBTTagCompound(), LegacyEntityNbt.state(entry)));
            waterlogged[i] = "true".equals(entry.getCompoundTag("Properties").getString("waterlogged"));
        }
        tag.setTag("palette", legacyPalette);
        tag.removeTag("palettes");
        // 1.13 keeps waterlogged in the block state; Portium's templates list those blocks beside the palette.
        NBTTagList water = new NBTTagList();
        NBTTagList blocks = tag.getTagList("blocks", 10);
        for (int i = 0; i < blocks.tagCount(); i++) {
            NBTTagCompound block = blocks.getCompoundTagAt(i);
            int state = block.getInteger("state");
            if (state < waterlogged.length && waterlogged[state]) {
                NBTTagList pos = block.getTagList("pos", 3);
                water.appendTag(new NBTTagIntArray(new int[] {pos.getIntAt(0), pos.getIntAt(1), pos.getIntAt(2)}));
            }
        }
        if (!water.isEmpty()) tag.setTag("PortiumWaterlogged", water);
        convertNbt(tag.getTagList("blocks", 10));
        convertNbt(tag.getTagList("entities", 10));
        tag.setInteger("DataVersion", DATA_VERSION);
        return tag;
    }

    /** Block entity and entity data: items and entity ids in 1.12's form. */
    private static void convertNbt(NBTTagList entries) {
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            if (entry.hasKey("nbt", 10)) entry.setTag("nbt", LegacyEntityNbt.convert(entry.getCompoundTag("nbt")));
        }
    }

    private LegacyStructureNbt() {}
}
