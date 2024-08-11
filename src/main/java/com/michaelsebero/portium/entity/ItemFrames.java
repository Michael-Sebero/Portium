package com.michaelsebero.portium.entity;

import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

/** Item frames on floors and ceilings (1.13), stored in a way 1.12 can still load. */
public final class ItemFrames {

    /** Present only on vertical frames: the facing's index (0 down, 1 up). Their 1.12 {@code Facing} stays a valid wall facing. */
    public static final String VERTICAL_TAG = "PortiumVerticalFacing";

    public static final Predicate<Entity> IS_HANGING = entity -> entity instanceof EntityHanging;

    /** In spawn packets a frame's facing goes as 1.12's wall index (0-3), or 4 plus the index of up or down. */
    public static int toSpawnData(EnumFacing facing) {
        return facing.getAxis() == EnumFacing.Axis.Y ? 4 + facing.getIndex() : facing.getHorizontalIndex();
    }

    public static EnumFacing fromSpawnData(int data) {
        return data >= 4 ? EnumFacing.byIndex(data - 4) : EnumFacing.byHorizontalIndex(data);
    }

    /** 1.13 saves a frame's facing as a direction index (0-5); 1.12 saves wall frames by a wall index. */
    public static void convertModernFacing(NBTTagCompound tag) {
        if (!tag.hasKey("Facing", 99)) return;
        EnumFacing facing = EnumFacing.byIndex(tag.getByte("Facing"));
        if (facing.getAxis() == EnumFacing.Axis.Y) {
            tag.setByte("Facing", (byte) 0);
            tag.setByte(VERTICAL_TAG, (byte) facing.getIndex());
        } else {
            tag.setByte("Facing", (byte) facing.getHorizontalIndex());
        }
    }

    private ItemFrames() {}
}
