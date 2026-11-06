package com.michaelsebero.portium.world;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.IFixableData;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * Portium's changes to saved data, run by Forge on what was saved before them (each world remembers Portium's data
 * version). 1: 1.14's cat split, tamed ocelots (those with a cat coat) becoming cats, as 1.14's own fixer does.
 */
public final class PortiumDataFixes {

    public static final int VERSION = 1;

    public static void register() {
        ModFixs fixes = FMLCommonHandler.instance().getDataFixer().init(Tags.MOD_ID, VERSION);
        fixes.registerFix(FixTypes.ENTITY, new IFixableData() {
            @Override
            public int getFixVersion() {
                return 1;
            }

            @Override
            public NBTTagCompound fixTagCompound(NBTTagCompound tag) {
                if (PortiumConfig.mobs.cats && "minecraft:ocelot".equals(tag.getString("id")) && tag.getInteger("CatType") > 0) {
                    tag.setString("id", Tags.MOD_ID + ":cat");
                }
                return tag;
            }
        });
    }

    private PortiumDataFixes() {}
}
