package com.michaelsebero.portium.mixin.world;

import com.llamalad7.mixinextras.sugar.Local;
import com.michaelsebero.portium.compat.ModernSaves;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.IFixType;
import net.minecraftforge.common.util.CompoundDataFixer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Rewrites data saved by 1.13 into 1.12's layout before 1.12's (and mods') own fixes look at it. */
@Mixin(value = CompoundDataFixer.class, remap = false)
public abstract class MixinCompoundDataFixer {

    // CompoundDataFixer is Forge's, but this method overrides vanilla's DataFixer.process, so outside a development
    // workspace it carries vanilla's SRG name; the mappings don't know Forge classes, so both names are given.
    @ModifyVariable(method = {"process(Lnet/minecraft/util/datafix/IFixType;Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/nbt/NBTTagCompound;",
            "func_188257_a(Lnet/minecraft/util/datafix/IFixType;Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/nbt/NBTTagCompound;"},
            at = @At("HEAD"), argsOnly = true)
    private NBTTagCompound portium$downgradeModern(NBTTagCompound tag, @Local(argsOnly = true) IFixType type) {
        return ModernSaves.downgrade(type, tag);
    }
}
