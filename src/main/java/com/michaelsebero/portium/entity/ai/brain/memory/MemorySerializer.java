package com.michaelsebero.portium.entity.ai.brain.memory;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;

import java.util.UUID;

/**
 * How a saved memory is written to NBT, in place of 1.16's codecs. The tags match what 1.16.5 writes: booleans as
 * bytes, UUIDs as four-int arrays, positions as {@code {dimension, pos:[I;x,y,z]}}.
 */
public interface MemorySerializer<U> {

    NBTBase write(U value);

    U read(NBTBase tag);

    MemorySerializer<Boolean> BOOL = new MemorySerializer<Boolean>() {
        @Override
        public NBTBase write(Boolean value) {
            return new NBTTagByte((byte) (value ? 1 : 0));
        }

        @Override
        public Boolean read(NBTBase tag) {
            return tag instanceof NBTPrimitive && ((NBTPrimitive) tag).getByte() != 0;
        }
    };

    MemorySerializer<Long> LONG = new MemorySerializer<Long>() {
        @Override
        public NBTBase write(Long value) {
            return new NBTTagLong(value);
        }

        @Override
        public Long read(NBTBase tag) {
            return tag instanceof NBTPrimitive ? ((NBTPrimitive) tag).getLong() : 0L;
        }
    };

    MemorySerializer<UUID> UUID_ARRAY = new MemorySerializer<UUID>() {
        @Override
        public NBTBase write(UUID value) {
            long most = value.getMostSignificantBits();
            long least = value.getLeastSignificantBits();
            return new NBTTagIntArray(new int[]{(int) (most >> 32), (int) most, (int) (least >> 32), (int) least});
        }

        @Override
        public UUID read(NBTBase tag) {
            int[] ints = tag instanceof NBTTagIntArray ? ((NBTTagIntArray) tag).getIntArray() : new int[0];
            if (ints.length != 4) return null;
            return new UUID((long) ints[0] << 32 | ints[1] & 0xFFFFFFFFL, (long) ints[2] << 32 | ints[3] & 0xFFFFFFFFL);
        }
    };

    MemorySerializer<GlobalPos> GLOBAL_POS = new MemorySerializer<GlobalPos>() {
        @Override
        public NBTBase write(GlobalPos value) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("dimension", new NBTTagString(dimensionName(value.getDimension())));
            BlockPos pos = value.getPos();
            tag.setTag("pos", new NBTTagIntArray(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
            return tag;
        }

        @Override
        public GlobalPos read(NBTBase tag) {
            if (!(tag instanceof NBTTagCompound)) return null;
            NBTTagCompound compound = (NBTTagCompound) tag;
            int[] pos = compound.getIntArray("pos");
            if (pos.length != 3) return null;
            Integer dimension = dimensionId(compound.getString("dimension"));
            return dimension == null ? null : GlobalPos.of(dimension, new BlockPos(pos[0], pos[1], pos[2]));
        }
    };

    /** 1.16's name for a 1.12 dimension: vanilla's three by their 1.16 ids, others by number. */
    static String dimensionName(int id) {
        if (id == 0) return "minecraft:overworld";
        if (id == -1) return "minecraft:the_nether";
        if (id == 1) return "minecraft:the_end";
        try {
            return "portium:" + DimensionType.getById(id).getName() + "/" + id;
        } catch (IllegalArgumentException missing) {
            return "portium:dim/" + id;
        }
    }

    static Integer dimensionId(String name) {
        switch (name) {
            case "minecraft:overworld": return 0;
            case "minecraft:the_nether": return -1;
            case "minecraft:the_end": return 1;
            default:
                int slash = name.lastIndexOf('/');
                if (!name.startsWith("portium:") || slash < 0) return null;
                try {
                    return Integer.parseInt(name.substring(slash + 1));
                } catch (NumberFormatException bad) {
                    return null;
                }
        }
    }
}
