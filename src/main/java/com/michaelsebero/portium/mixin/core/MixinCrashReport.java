package com.michaelsebero.portium.mixin.core;

import com.michaelsebero.portium.diagnostics.ProblemReport;
import net.minecraft.crash.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Each crash report 1.12 makes also gets Portium's analysis, written to {@code portium-reports/} (see ProblemReport). */
@Mixin(CrashReport.class)
public abstract class MixinCrashReport {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void portium$report(String description, Throwable cause, CallbackInfo ci) {
        if (cause != null) ProblemReport.crash(description, cause);
    }
}
