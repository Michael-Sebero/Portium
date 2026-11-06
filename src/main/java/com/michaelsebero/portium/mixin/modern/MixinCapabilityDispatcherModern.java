package com.michaelsebero.portium.mixin.modern;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.michaelsebero.portium.diagnostics.ProblemReport;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.util.INBTSerializable;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A newer mod's capability that fails to save or load (one that reaches a part of newer Minecraft Portium doesn't
 * cover yet, or whose mod failed to set up) is skipped and reported, rather than crashing the world's save.
 */
@Mixin(value = CapabilityDispatcher.class, remap = false)
public abstract class MixinCapabilityDispatcherModern {

    private static final Set<Class<?>> PORTIUM$FAILED = Collections.synchronizedSet(new HashSet<>());

    private static boolean portium$isModern(Object provider) {
        ClassLoader loader = provider.getClass().getClassLoader();
        return loader != null && loader != CapabilityDispatcher.class.getClassLoader() && loader.getClass().getName().startsWith("com.michaelsebero.portium.");
    }

    private static void portium$report(Object provider, String what, Throwable error) {
        if (!PORTIUM$FAILED.add(provider.getClass())) return;
        LogManager.getLogger("Portium/Mods").error("Mod problem: the capability {} couldn't {}; it's skipped", provider.getClass().getName(), what, error);
        ProblemReport.record("The capability " + provider.getClass().getName() + " couldn't " + what, error);
    }

    @WrapOperation(method = "serializeNBT()Lnet/minecraft/nbt/NBTTagCompound;",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/util/INBTSerializable;serializeNBT()Lnet/minecraft/nbt/NBTBase;"))
    private NBTBase portium$saveModern(INBTSerializable<NBTBase> provider, Operation<NBTBase> original) {
        if (!portium$isModern(provider)) return original.call(provider);
        try {
            NBTBase saved = original.call(provider);
            return saved != null ? saved : new NBTTagCompound();
        } catch (RuntimeException | LinkageError e) {
            portium$report(provider, "be saved", e);
            return new NBTTagCompound();
        }
    }

    @WrapOperation(method = "deserializeNBT(Lnet/minecraft/nbt/NBTTagCompound;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/util/INBTSerializable;deserializeNBT(Lnet/minecraft/nbt/NBTBase;)V"))
    private void portium$loadModern(INBTSerializable<NBTBase> provider, NBTBase nbt, Operation<Void> original) {
        if (!portium$isModern(provider)) {
            original.call(provider, nbt);
            return;
        }
        try {
            original.call(provider, nbt);
        } catch (RuntimeException | LinkageError e) {
            portium$report(provider, "be loaded", e);
        }
    }
}
