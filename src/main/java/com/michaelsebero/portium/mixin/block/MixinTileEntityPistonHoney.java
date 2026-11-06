package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.block.bee.BlockHoney;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** 1.15: a honey block pushed or pulled sideways carries the mobs standing on it. */
@Mixin(TileEntityPiston.class)
public abstract class MixinTileEntityPistonHoney extends TileEntity {

    @Shadow private IBlockState pistonState;
    @Shadow private EnumFacing pistonFacing;
    @Shadow private boolean extending;
    @Shadow private float progress;

    @Inject(method = "moveCollidedEntities", at = @At("RETURN"))
    private void portium$carryOnHoney(float newProgress, CallbackInfo ci) {
        if (!(this.pistonState.getBlock() instanceof BlockHoney)) return;
        EnumFacing direction = this.extending ? this.pistonFacing : this.pistonFacing.getOpposite();
        if (!direction.getAxis().isHorizontal()) return;
        double top = this.pistonState.getBoundingBox(this.world, this.pos).maxY;
        double extended = this.extending ? this.progress - 1.0F : 1.0F - this.progress;
        AxisAlignedBB area = new AxisAlignedBB(0.0D, top, 0.0D, 1.0D, 1.5000000999999998D, 1.0D).offset(
                this.pos.getX() + extended * this.pistonFacing.getXOffset(), this.pos.getY() + extended * this.pistonFacing.getYOffset(),
                this.pos.getZ() + extended * this.pistonFacing.getZOffset());
        double distance = newProgress - this.progress;
        List<Entity> riders = this.world.getEntitiesInAABBexcluding(null, area, entity -> entity != null && entity.getPushReaction() == EnumPushReaction.NORMAL
                && entity.onGround && entity.posX >= area.minX && entity.posX <= area.maxX && entity.posZ >= area.minZ && entity.posZ <= area.maxZ);
        for (Entity entity : riders) {
            entity.move(MoverType.PISTON, distance * direction.getXOffset(), distance * direction.getYOffset(), distance * direction.getZOffset());
        }
    }
}
