package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.block.BlockScaffolding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Climbing as 1.14 climbs: holding jump climbs a ladder (or vines, or scaffolding) as walking into it does, and
 * sneaking inside scaffolding climbs down instead of holding on.
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseClimbing extends Entity {

    @Shadow
    protected boolean isJumping;

    protected MixinEntityLivingBaseClimbing(World world) {
        super(world);
    }

    /** The {@code collidedHorizontally && isOnLadder()} that climbs: jumping counts too. */
    @Redirect(method = "travel", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;collidedHorizontally:Z",
            opcode = Opcodes.GETFIELD, ordinal = 1))
    private boolean portium$jumpToClimb(EntityLivingBase self) {
        return self.collidedHorizontally || this.isJumping;
    }

    /** Sneaking holds a player still on a ladder, but not in scaffolding. */
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isSneaking()Z"))
    private boolean portium$sneakOnLadder(EntityLivingBase self) {
        return self.isSneaking() && !(self.world.getBlockState(new BlockPos(self)).getBlock() instanceof BlockScaffolding);
    }
}
