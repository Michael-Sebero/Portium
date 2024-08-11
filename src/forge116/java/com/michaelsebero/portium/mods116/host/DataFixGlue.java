package com.michaelsebero.portium.mods116.host;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import net.minecraft.nbt.CompoundNBT;

/**
 * 1.16's {@code NBTUtil.update}: data as saved, brought up to 1.16's format by Mojang's data fixers. Portium keeps
 * 1.12's format, which 1.16's fixers can't read, so data comes back as it was.
 */
public final class DataFixGlue {

    public static CompoundNBT update(DataFixer fixer, DSL.TypeReference type, CompoundNBT data, int version) {
        return data;
    }

    public static CompoundNBT update(DataFixer fixer, DSL.TypeReference type, CompoundNBT data, int version, int newVersion) {
        return data;
    }

    private DataFixGlue() {}
}
