package com.michaelsebero.portium.mixin.item;

import com.llamalad7.mixinextras.sugar.Local;
import com.michaelsebero.portium.PortiumConfig;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBanner;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Placing a banner makes the wood placing sound, which 1.12 forgot. */
@Mixin(ItemBanner.class)
public abstract class MixinItemBanner {

    @Inject(method = "onItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;shrink(I)V"))
    private void portium$playPlaceSound(EntityPlayer player, World world, BlockPos clicked, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ,
                                        CallbackInfoReturnable<EnumActionResult> cir, @Local(argsOnly = true) BlockPos pos) {
        if (!PortiumConfig.mechanics.bannerPlaceSound) return;
        IBlockState state = world.getBlockState(pos);
        SoundType sound = state.getBlock().getSoundType(state, world, pos, player);
        world.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
    }
}
