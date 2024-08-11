package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.advancements.FishingRodHookedTrigger;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reeling in a hooked entity counts for the {@code fishing_rod_hooked} advancement trigger; bobbers float, and fish
 * bite, in waterlogged blocks (1.13).
 */
@Mixin(EntityFishHook.class)
public abstract class MixinEntityFishHook extends Entity {

    @Shadow public Entity caughtEntity;

    private MixinEntityFishHook(World world) {
        super(world);
    }

    @Inject(method = "handleHookRetraction", at = @At("HEAD"))
    private void portium$triggerHooked(CallbackInfoReturnable<Integer> cir) {
        if (!this.world.isRemote && this.caughtEntity != null) FishingRodHookedTrigger.onEntityReeled((EntityFishHook) (Object) this);
    }

    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"))
    private IBlockState portium$floatInWaterlogged(World world, BlockPos pos) {
        return Waterlogging.withWater(world, pos);
    }

    @Redirect(method = "catchingFish", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"))
    private IBlockState portium$fishInWaterlogged(WorldServer world, BlockPos pos) {
        return Waterlogging.withWater(world, pos);
    }
}
