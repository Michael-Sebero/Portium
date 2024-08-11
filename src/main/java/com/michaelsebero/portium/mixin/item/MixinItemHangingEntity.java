package com.michaelsebero.portium.mixin.item;

import com.michaelsebero.portium.PortiumConfig;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemHangingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Item frames can be placed on the top and bottom of blocks (1.13). Paintings still only go on walls. */
@Mixin(ItemHangingEntity.class)
public abstract class MixinItemHangingEntity {

    @Shadow @Final private Class<? extends EntityHanging> hangingEntityClass;

    @Inject(method = "onItemUse", at = @At("HEAD"), cancellable = true)
    private void portium$placeOnFloorOrCeiling(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY,
                                               float hitZ, CallbackInfoReturnable<EnumActionResult> cir) {
        if (!PortiumConfig.mechanics.floorItemFrames || this.hangingEntityClass != EntityItemFrame.class || facing.getAxis() != EnumFacing.Axis.Y) return;
        ItemStack stack = player.getHeldItem(hand);
        BlockPos framePos = pos.offset(facing);
        if (!player.canPlayerEdit(framePos, facing, stack)) {
            cir.setReturnValue(EnumActionResult.FAIL);
            return;
        }
        EntityItemFrame frame = new EntityItemFrame(world, framePos, facing);
        if (frame.onValidSurface()) {
            if (!world.isRemote) {
                frame.playPlaceSound();
                world.spawnEntity(frame);
            }
            stack.shrink(1);
        }
        cir.setReturnValue(EnumActionResult.SUCCESS);
    }
}
