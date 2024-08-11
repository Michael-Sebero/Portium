package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Water doesn't draw a face against the water of a waterlogged neighbour, as with any water. */
@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquidSides extends Block {

    private MixinBlockLiquidSides(Material material) {
        super(material);
    }

    @Inject(method = "shouldSideBeRendered", at = @At("HEAD"), cancellable = true)
    private void portium$noFaceAgainstWaterlogged(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        if (this.material == Material.WATER && Waterlogging.isWaterlogged(world, pos.offset(side))) cir.setReturnValue(false);
    }
}
