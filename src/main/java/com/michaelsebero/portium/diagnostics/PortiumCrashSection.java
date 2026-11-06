package com.michaelsebero.portium.diagnostics;

import net.minecraftforge.fml.common.ICrashCallable;

/** Portium's part of every 1.12 crash report: what it makes of the crash, and where its full report is. */
public final class PortiumCrashSection implements ICrashCallable {

    @Override
    public String getLabel() {
        return "Portium diagnostics";
    }

    @Override
    public String call() {
        return ProblemReport.crashSection();
    }
}
