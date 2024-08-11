package com.michaelsebero.portium.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.michaelsebero.portium.PortiumConfig;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Floor and ceiling buttons turn to face the player who places them (1.13). The extra {@code rotated} state uses the
 * button metadata values 6 and 7 (14 and 15 when pressed), which 1.12 leaves unused, so worlds stay compatible.
 */
@Mixin(BlockButton.class)
public abstract class MixinBlockButton extends BlockDirectional {

    @Unique
    private static final PropertyBool PORTIUM_ROTATED = PropertyBool.create("rotated");

    private MixinBlockButton(Material material) {
        super(material);
    }

    @Unique
    private boolean portium$hasRotation(IBlockState state) {
        return state.getPropertyKeys().contains(PORTIUM_ROTATED);
    }

    @Unique
    private static boolean portium$isFloorOrCeiling(EnumFacing facing) {
        return facing == EnumFacing.UP || facing == EnumFacing.DOWN;
    }

    @ModifyReturnValue(method = "createBlockState", at = @At("RETURN"))
    private BlockStateContainer portium$addRotation(BlockStateContainer container) {
        return new BlockStateContainer(this, new IProperty[] {FACING, BlockButton.POWERED, PORTIUM_ROTATED});
    }

    @ModifyReturnValue(method = "getStateFromMeta", at = @At("RETURN"))
    private IBlockState portium$rotationFromMeta(IBlockState state, @Local(argsOnly = true) int meta) {
        if (!this.portium$hasRotation(state) || (meta & 7) < 6) return state;
        return state.withProperty(FACING, (meta & 7) == 6 ? EnumFacing.UP : EnumFacing.DOWN).withProperty(PORTIUM_ROTATED, true);
    }

    @ModifyReturnValue(method = "getMetaFromState", at = @At("RETURN"))
    private int portium$rotationToMeta(int meta, @Local(argsOnly = true) IBlockState state) {
        if (!this.portium$hasRotation(state) || !state.getValue(PORTIUM_ROTATED) || !portium$isFloorOrCeiling(state.getValue(FACING))) return meta;
        return (meta & 8) | (state.getValue(FACING) == EnumFacing.UP ? 6 : 7);
    }

    @ModifyReturnValue(method = "getStateForPlacement", at = @At("RETURN"))
    private IBlockState portium$rotateForPlacer(IBlockState state, @Local(argsOnly = true) EntityLivingBase placer) {
        if (!PortiumConfig.mechanics.rotatingButtons) return state;
        if (!this.portium$hasRotation(state) || placer == null || !portium$isFloorOrCeiling(state.getValue(FACING))) return state;
        return state.withProperty(PORTIUM_ROTATED, placer.getHorizontalFacing().getAxis() == EnumFacing.Axis.X);
    }

    @ModifyReturnValue(method = "getBoundingBox", at = @At("RETURN"))
    private AxisAlignedBB portium$rotateBox(AxisAlignedBB box, @Local(argsOnly = true) IBlockState state) {
        if (!this.portium$hasRotation(state) || !state.getValue(PORTIUM_ROTATED) || !portium$isFloorOrCeiling(state.getValue(FACING))) return box;
        return new AxisAlignedBB(box.minZ, box.minY, box.minX, box.maxZ, box.maxY, box.maxX);
    }

    @ModifyReturnValue(method = "withRotation", at = @At("RETURN"))
    private IBlockState portium$rotateWithStructure(IBlockState rotated, @Local(argsOnly = true) Rotation rotation) {
        if (!this.portium$hasRotation(rotated) || !portium$isFloorOrCeiling(rotated.getValue(FACING))) return rotated;
        if (rotation != Rotation.CLOCKWISE_90 && rotation != Rotation.COUNTERCLOCKWISE_90) return rotated;
        return rotated.cycleProperty(PORTIUM_ROTATED);
    }
}
