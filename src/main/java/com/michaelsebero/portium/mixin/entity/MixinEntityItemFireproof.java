package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.item.IFireproofItem;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dropped fireproof items (1.16's netherite gear, its block and ancient debris; see IFireproofItem) don't burn in fire
 * or lava, and in lava they rise to the surface as 1.16's do (slowed sideways, a little lift each tick, no gravity)
 * instead of 1.12's hop out of the lava.
 */
@Mixin(EntityItem.class)
public abstract class MixinEntityItemFireproof extends Entity {

    @Shadow
    public abstract ItemStack getItem();

    private MixinEntityItemFireproof(World world) {
        super(world);
    }

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void portium$fireproof(CallbackInfo ci) {
        ItemStack stack = getItem();
        boolean fireproof = !stack.isEmpty() && stack.getItem() instanceof IFireproofItem;
        this.isImmuneToFire = fireproof;
        if (fireproof && this.isBurning()) this.extinguish();
    }

    // Rising through lava is in MixinEntityItem's gravity redirect, which can't be redirected twice.

    /** 1.12's dropped items burn whatever their immunity (their dealFireDamage skips the check): fire doesn't hurt these. */
    @Inject(method = "attackEntityFrom", at = @At("HEAD"), cancellable = true)
    private void portium$noFireDamage(net.minecraft.util.DamageSource source, float amount,
                                      org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (source.isFireDamage() && !getItem().isEmpty() && getItem().getItem() instanceof IFireproofItem) cir.setReturnValue(false);
    }

    @Redirect(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/state/IBlockState;getMaterial()Lnet/minecraft/block/material/Material;"))
    private Material portium$noLavaHop(IBlockState state) {
        Material material = state.getMaterial();
        return this.isImmuneToFire && material == Material.LAVA ? Material.AIR : material;
    }
}
