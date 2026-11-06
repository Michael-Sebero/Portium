package com.michaelsebero.portium.block.workstation;

import com.michaelsebero.portium.block.BlockSupport;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;

import java.util.Locale;

/** 1.14's {@code AttachFace}: the floor, a wall or the ceiling a grindstone (or a bell) hangs from. */
public enum AttachFace implements IStringSerializable {
    FLOOR, WALL, CEILING;

    @Override
    public String getName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /**
     * 1.14's {@code HorizontalFaceBlock.getStateForPlacement}: the first direction the placer looks along (the clicked
     * side first) decides: down for the floor, up for the ceiling (facing where the placer faces), else the wall
     * opposite. Returns the face and the facing.
     */
    public static Object[] forPlacement(EntityLivingBase placer, EnumFacing clicked) {
        EnumFacing direction = BlockSupport.nearestLookingDirections(placer, clicked)[0];
        if (direction.getAxis() == EnumFacing.Axis.Y) {
            return new Object[] {direction == EnumFacing.UP ? CEILING : FLOOR, placer.getHorizontalFacing()};
        }
        return new Object[] {WALL, direction.getOpposite()};
    }
}
