package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.ItemFrames;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Item frames can face up or down, as in 1.13. Horizontal frames and paintings behave as in 1.12; a vertical frame
 * saves as a 1.12 frame plus a tag naming its facing, so the world still loads without Portium.
 */
@Mixin(EntityHanging.class)
public abstract class MixinEntityHanging extends Entity {

    @Shadow public EnumFacing facingDirection;
    @Shadow protected BlockPos hangingPosition;

    @Shadow protected abstract void updateBoundingBox();

    @Shadow protected abstract void updateFacingWithBoundingBox(EnumFacing facing);

    @Shadow public abstract int getWidthPixels();

    private MixinEntityHanging(World world) {
        super(world);
    }

    private boolean portium$isVerticalFrame() {
        return (Object) this instanceof EntityItemFrame && this.facingDirection != null && this.facingDirection.getAxis() == EnumFacing.Axis.Y;
    }

    /** A vertical frame looks straight up or down: pitch -90 facing up, 90 facing down. */
    @Inject(method = "updateFacingWithBoundingBox", at = @At("HEAD"), cancellable = true)
    private void portium$faceVertically(EnumFacing facing, CallbackInfo ci) {
        if (!((Object) this instanceof EntityItemFrame)) return;
        this.rotationPitch = facing.getAxis() == EnumFacing.Axis.Y ? -90.0F * facing.getAxisDirection().getOffset() : 0.0F;
        this.prevRotationPitch = this.rotationPitch;
        if (facing.getAxis() != EnumFacing.Axis.Y) return;
        this.facingDirection = facing;
        this.rotationYaw = 0.0F;
        this.prevRotationYaw = 0.0F;
        this.updateBoundingBox();
        ci.cancel();
    }

    /** 1.13's frame box: 12 pixels square and 1 pixel thick along the facing axis, set against the block behind. */
    @Inject(method = "updateBoundingBox", at = @At("HEAD"), cancellable = true)
    private void portium$verticalBox(CallbackInfo ci) {
        if (!this.portium$isVerticalFrame()) return;
        this.posX = this.hangingPosition.getX() + 0.5D;
        this.posY = this.hangingPosition.getY() + 0.5D - this.facingDirection.getYOffset() * 0.46875D;
        this.posZ = this.hangingPosition.getZ() + 0.5D;
        double halfX = this.getWidthPixels() / 32.0D;
        double halfY = 1.0D / 32.0D;
        double halfZ = this.getWidthPixels() / 32.0D;
        this.setEntityBoundingBox(new AxisAlignedBB(this.posX - halfX, this.posY - halfY, this.posZ - halfZ, this.posX + halfX, this.posY + halfY, this.posZ + halfZ));
        ci.cancel();
    }

    /** A vertical frame needs a solid block behind it (no redstone diodes, unlike walls) and no other hanging entity in its space. */
    @Inject(method = "onValidSurface", at = @At("HEAD"), cancellable = true)
    private void portium$verticalSurface(CallbackInfoReturnable<Boolean> cir) {
        if (!this.portium$isVerticalFrame()) return;
        if (!this.world.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        boolean solid = this.world.getBlockState(this.hangingPosition.offset(this.facingDirection.getOpposite())).getMaterial().isSolid();
        cir.setReturnValue(solid && this.world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox(), ItemFrames.IS_HANGING).isEmpty());
    }

    @Inject(method = "writeEntityToNBT", at = @At("TAIL"))
    private void portium$writeVerticalFacing(NBTTagCompound compound, CallbackInfo ci) {
        if (!this.portium$isVerticalFrame()) return;
        compound.setByte("Facing", (byte) 0);
        compound.setByte(ItemFrames.VERTICAL_TAG, (byte) this.facingDirection.getIndex());
    }

    @Inject(method = "readEntityFromNBT", at = @At("TAIL"))
    private void portium$readVerticalFacing(NBTTagCompound compound, CallbackInfo ci) {
        if ((Object) this instanceof EntityItemFrame && compound.hasKey(ItemFrames.VERTICAL_TAG, 99)) {
            this.updateFacingWithBoundingBox(compound.getByte(ItemFrames.VERTICAL_TAG) == 0 ? EnumFacing.DOWN : EnumFacing.UP);
        }
    }
}
