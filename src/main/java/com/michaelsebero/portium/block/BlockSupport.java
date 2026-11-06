package com.michaelsebero.portium.block;

import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

/** 1.14+ placement rules for Portium's blocks, in 1.12 terms. */
public final class BlockSupport {

    /**
     * 1.16's {@code Block.hasEnoughSolidSide}: whether the face can hold something by its middle, like a lantern or a
     * torch: a whole face, or a post down the middle (fences, walls, panes, bars, chains).
     */
    public static boolean hasCenterSupport(IBlockAccess world, BlockPos pos, EnumFacing face) {
        IBlockState state = world.getBlockState(pos);
        BlockFaceShape shape = state.getBlockFaceShape(world, pos, face);
        switch (shape) {
            case SOLID:
            case CENTER:
            case CENTER_BIG:
            case CENTER_SMALL:
                return true;
            case BOWL:
                // A hopper's rim and floor cover its top, as 1.16 sees it.
                return face == EnumFacing.UP;
            case MIDDLE_POLE:
            case MIDDLE_POLE_THICK:
            case MIDDLE_POLE_THIN:
                return face.getAxis() != EnumFacing.Axis.Y;
            default:
                return false;
        }
    }

    /**
     * 1.16's {@code BlockItemUseContext.getNearestLookingDirections}: the six directions in the order the placer
     * looks along them ({@code Direction.getFacingDirections}), with the side facing into the clicked block first.
     */
    public static EnumFacing[] nearestLookingDirections(@Nullable EntityLivingBase placer, EnumFacing clickedFace) {
        EnumFacing[] directions = facingDirections(placer);
        EnumFacing into = clickedFace.getOpposite();
        int i = 0;
        while (i < directions.length && directions[i] != into) i++;
        if (i > 0 && i < directions.length) {
            System.arraycopy(directions, 0, directions, 1, i);
            directions[0] = into;
        }
        return directions;
    }

    /** 1.16's {@code Direction.getFacingDirections(entity)}. */
    public static EnumFacing[] facingDirections(@Nullable EntityLivingBase entity) {
        if (entity == null) return new EnumFacing[] {EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.SOUTH, EnumFacing.UP};
        float pitch = entity.rotationPitch * 0.017453292F;
        float yaw = -entity.rotationYaw * 0.017453292F;
        float pitchSin = MathHelper.sin(pitch);
        float pitchCos = MathHelper.cos(pitch);
        float yawSin = MathHelper.sin(yaw);
        float yawCos = MathHelper.cos(yaw);
        boolean east = yawSin > 0.0F;
        boolean up = pitchSin < 0.0F;
        boolean south = yawCos > 0.0F;
        float x = east ? yawSin : -yawSin;
        float y = up ? -pitchSin : pitchSin;
        float z = south ? yawCos : -yawCos;
        float xLook = x * pitchCos;
        float zLook = z * pitchCos;
        EnumFacing alongX = east ? EnumFacing.EAST : EnumFacing.WEST;
        EnumFacing alongY = up ? EnumFacing.UP : EnumFacing.DOWN;
        EnumFacing alongZ = south ? EnumFacing.SOUTH : EnumFacing.NORTH;
        if (x > z) {
            if (y > xLook) return compose(alongY, alongX, alongZ);
            return zLook > y ? compose(alongX, alongZ, alongY) : compose(alongX, alongY, alongZ);
        }
        if (y > zLook) return compose(alongY, alongZ, alongX);
        return xLook > y ? compose(alongZ, alongX, alongY) : compose(alongZ, alongY, alongX);
    }

    private static EnumFacing[] compose(EnumFacing first, EnumFacing second, EnumFacing third) {
        return new EnumFacing[] {first, second, third, third.getOpposite(), second.getOpposite(), first.getOpposite()};
    }

    private BlockSupport() {}
}
