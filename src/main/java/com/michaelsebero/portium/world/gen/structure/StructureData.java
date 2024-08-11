package com.michaelsebero.portium.world.gen.structure;

import com.michaelsebero.portium.Tags;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * Remembers the height each structure piece settled at, so the parts of a structure placed in different chunks line
 * up even though the terrain under it changes as chunks are decorated.
 */
public class StructureData extends WorldSavedData {

    private static final String NAME = Tags.MOD_ID + "_structures";
    private final NBTTagCompound heights = new NBTTagCompound();

    public StructureData(String name) {
        super(name);
    }

    public static StructureData get(World world) {
        MapStorage storage = world.getPerWorldStorage();
        StructureData data = (StructureData) storage.getOrLoadData(StructureData.class, NAME);
        if (data == null) {
            data = new StructureData(NAME);
            storage.setData(NAME, data);
        }
        return data;
    }

    private static String key(OceanStructure structure, int chunkX, int chunkZ, int piece) {
        return structure.getName() + "/" + chunkX + "/" + chunkZ + "/" + piece;
    }

    public boolean hasHeight(OceanStructure structure, int chunkX, int chunkZ, int piece) {
        return this.heights.hasKey(key(structure, chunkX, chunkZ, piece), 3);
    }

    public int getHeight(OceanStructure structure, int chunkX, int chunkZ, int piece) {
        return this.heights.getInteger(key(structure, chunkX, chunkZ, piece));
    }

    public void setHeight(OceanStructure structure, int chunkX, int chunkZ, int piece, int y) {
        this.heights.setInteger(key(structure, chunkX, chunkZ, piece), y);
        this.markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        NBTTagCompound saved = compound.getCompoundTag("Heights");
        for (String key : saved.getKeySet()) this.heights.setInteger(key, saved.getInteger(key));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("Heights", this.heights.copy());
        return compound;
    }
}
