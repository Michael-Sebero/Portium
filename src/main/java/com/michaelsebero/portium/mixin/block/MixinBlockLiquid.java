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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Water dims light by one level per block instead of three; lava meeting a waterlogged block hardens as against water (1.13). */
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
}
