package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.block.sign.IDyedSign;
import com.michaelsebero.portium.item.BannerPatterns116;
import net.minecraft.block.BlockSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.14: a dye used on a sign colours its text (one dye used up, unless in creative). */
@Mixin(BlockSign.class)
public abstract class MixinBlockSignDye {

    @Inject(method = "onBlockActivated", at = @At("HEAD"), cancellable = true)
    private void portium$dye(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY,
                             float hitZ, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = player.getHeldItem(hand);
        EnumDyeColor color = stack.isEmpty() ? null : BannerPatterns116.dyeColor(stack);
        if (color == null || !player.capabilities.allowEdit) return;
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof IDyedSign)) return;
        if (!world.isRemote && ((IDyedSign) tile).portium$setColor(color)) {
            tile.markDirty();
            world.notifyBlockUpdate(pos, state, state, 3);
            if (!player.capabilities.isCreativeMode) stack.shrink(1);
        }
        cir.setReturnValue(true);
    }
}
