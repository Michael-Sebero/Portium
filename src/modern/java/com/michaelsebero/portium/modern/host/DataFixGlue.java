package com.michaelsebero.portium.modern.host;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 1.13's {@code NBTUtil.update}: data as saved, brought up to 1.13's format by Mojang's data fixers. Portium keeps
 * 1.12's format, which 1.13's fixers can't read, so data comes back as it was; 1.13 code reading saved chunks finds
 * them unfinished (no 1.13 status) and passes them by.
 */
public final class DataFixGlue {

    public static NBTTagCompound update(DataFixer fixer, DSL.TypeReference type, NBTTagCompound data, int version) {
        return data;
    }

    public static NBTTagCompound update(DataFixer fixer, DSL.TypeReference type, NBTTagCompound data, int version, int newVersion) {
        return data;
    }

    private DataFixGlue() {}
}
