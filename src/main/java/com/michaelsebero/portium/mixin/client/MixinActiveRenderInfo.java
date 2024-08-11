package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A camera below the surface of a waterlogged cell sees water: fog, colour and the underwater view. */
@Mixin(ActiveRenderInfo.class)
public abstract class MixinActiveRenderInfo {

    @Inject(method = "getBlockStateAtEntityViewpoint", at = @At("RETURN"), cancellable = true)
    private static void portium$waterloggedView(World world, Entity entity, float partialTicks, CallbackInfoReturnable<IBlockState> cir) {
        if (cir.getReturnValue().getMaterial().isLiquid()) return;
        Vec3d view = ActiveRenderInfo.projectViewFromEntity(entity, partialTicks);
        BlockPos pos = new BlockPos(view);
        if (view.y < pos.getY() + Waterlogging.SURFACE && Waterlogging.isWaterlogged(world, pos)) cir.setReturnValue(Blocks.WATER.getDefaultState());
    }
}
