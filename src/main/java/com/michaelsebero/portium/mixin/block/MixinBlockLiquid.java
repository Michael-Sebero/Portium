package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Water dims light by one level per block instead of three; lava meeting a waterlogged block hardens as against water
 * (1.13); lava over soul soil beside blue ice turns to basalt (1.16's basalt generator).
 */
@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquid extends Block {

    private MixinBlockLiquid(Material material) {
        super(material);
    }

    @Override
    public int getLightOpacity(IBlockState state) {
        if (this.material == Material.WATER && PortiumConfig.mechanics.clearerWater) return 1;
        return this.lightOpacity;
    }

    @Redirect(method = "checkForMixing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"))
    private IBlockState portium$waterloggedNeighborMixes(World world, BlockPos pos) {
        return Waterlogging.withWater(world, pos);
    }

    @Shadow
    protected abstract void triggerMixEffects(World world, BlockPos pos);

    /** 1.16: lava (that water didn't harden) over soul soil, with blue ice beside or above it, becomes basalt. */
    @Inject(method = "checkForMixing", at = @At("RETURN"), cancellable = true)
    private void portium$basalt(World world, BlockPos pos, IBlockState state, CallbackInfoReturnable<Boolean> cir) {
        // 1.12 answers true when the liquid has mixed (with water) already.
        if (cir.getReturnValueZ() || this.material != Material.LAVA) return;
        net.minecraft.block.Block soulSoil = com.michaelsebero.portium.init.PortiumBlocks.SOUL_SOIL;
        net.minecraft.block.Block blueIce = com.michaelsebero.portium.init.PortiumBlocks.BLUE_ICE;
        net.minecraft.block.Block basalt = com.michaelsebero.portium.init.PortiumBlocks.BASALT;
        if (soulSoil == null || blueIce == null || basalt == null || world.getBlockState(pos.down()).getBlock() != soulSoil) return;
        for (net.minecraft.util.EnumFacing side : net.minecraft.util.EnumFacing.values()) {
            if (side == net.minecraft.util.EnumFacing.DOWN || world.getBlockState(pos.offset(side)).getBlock() != blueIce) continue;
            world.setBlockState(pos, net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(world, pos, pos, basalt.getDefaultState()));
            this.triggerMixEffects(world, pos);
            cir.setReturnValue(true);
            return;
        }
    }
}
