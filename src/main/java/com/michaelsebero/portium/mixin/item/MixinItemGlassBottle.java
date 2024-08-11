package com.michaelsebero.portium.mixin.item;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemGlassBottle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Glass bottles fill from waterlogged blocks (1.13). */
@Mixin(ItemGlassBottle.class)
public abstract class MixinItemGlassBottle {

    @Redirect(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"))
    private IBlockState portium$seeWaterloggedAsWater(World world, BlockPos pos) {
        return Waterlogging.withWater(world, pos);
    }
}
