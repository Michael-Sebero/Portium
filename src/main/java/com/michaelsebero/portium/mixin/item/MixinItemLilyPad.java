package com.michaelsebero.portium.mixin.item;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemLilyPad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lily pads can be placed on the water of a waterlogged block (1.13). */
@Mixin(ItemLilyPad.class)
public abstract class MixinItemLilyPad {

    @Redirect(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;", ordinal = 0))
    private IBlockState portium$waterlogged(World world, BlockPos pos) {
        return Waterlogging.withWater(world, pos);
    }
}
