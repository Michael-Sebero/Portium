package com.michaelsebero.portium.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.michaelsebero.portium.world.BedExplosions;
import net.minecraft.block.BlockBed;
import net.minecraft.entity.Entity;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBed.class)
public abstract class MixinBlockBed {

    @WrapOperation(method = "onBlockActivated", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;newExplosion(Lnet/minecraft/entity/Entity;DDDFZZ)Lnet/minecraft/world/Explosion;"))
    private Explosion portium$markBedExplosion(World world, Entity entity, double x, double y, double z, float strength, boolean fire, boolean smoke, Operation<Explosion> original) {
        BedExplosions.begin();
        try {
            return original.call(world, entity, x, y, z, strength, fire, smoke);
        } finally {
            BedExplosions.end();
        }
    }
}
