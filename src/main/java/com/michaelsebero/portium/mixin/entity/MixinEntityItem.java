package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.block.water.WaterUtil;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Items float up to the surface of water instead of sinking (1.13). */
@Mixin(EntityItem.class)
public abstract class MixinEntityItem extends Entity {

    private MixinEntityItem(World world) {
        super(world);
    }

    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EntityItem;hasNoGravity()Z"))
    private boolean portium$floatInWater(EntityItem self) {
        if (self.hasNoGravity()) return true;
        if (!PortiumConfig.mechanics.itemBuoyancy) return false;
        double eyeY = this.posY + this.getEyeHeight();
        BlockPos eyePos = new BlockPos(this.posX, eyeY, this.posZ);
        IBlockState state = this.world.getBlockState(eyePos);
        if (!WaterUtil.isWater(state) || !state.getPropertyKeys().contains(BlockLiquid.LEVEL)) return false;
        if (eyeY >= eyePos.getY() + BlockLiquid.getBlockLiquidHeight(state, this.world, eyePos) + 0.11111111F) return false;
        // Rise gently instead of falling, which the caller would otherwise apply.
        if (this.motionY < 0.06D) this.motionY += 5.0E-4D;
        this.motionX *= 0.99D;
        this.motionZ *= 0.99D;
        return true;
    }
}
