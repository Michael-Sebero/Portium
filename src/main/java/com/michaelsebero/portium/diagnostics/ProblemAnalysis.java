package com.michaelsebero.portium.diagnostics;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a failure means for Portium, worked out from its exception: what kind of gap it is (a member 1.12 lacks, a
 * type that crossed between 1.12 and newer code, a loop), whose code was running (a newer mod's, Portium's, or
 * Minecraft 1.13.2's or 1.16.5's own code Portium runs in place of 1.12's), and where in Portium such a gap is
 * usually closed. Written for someone (or an AI assistant) fixing Portium from the report alone.
 */
final class ProblemAnalysis {

    /** Bridge frames: Minecraft 1.16.5's (or 1.13.2's) own code, run for a member 1.12's class lacks. */
    private static final Pattern BRIDGE_SOURCE = Pattern.compile("Minecraft (1\\.\\d+\\.\\d+) (.+)");
    private static final Pattern MISSING_METHOD = Pattern.compile("([\\w.$/]+)\\.([\\w$<>]+)(\\(.*)");
    private static final Pattern CAST = Pattern.compile("([\\w.$]+) cannot be cast to ([\\w.$]+)");

    final Throwable root;
    final String kind;
    final String error;
    @Nullable
    final String missing;
    @Nullable
    final String bridged;
    @Nullable
    final String modFrame;
    @Nullable
    final String portiumFrame;
    final String layer;
    final String explanation;
    final List<String> fixes = new ArrayList<>();
    final List<String> stack = new ArrayList<>();
    @Nullable
    final String loop;

    ProblemAnalysis(Throwable thrown) {
        Throwable cause = thrown;
        Set<Throwable> seen = new LinkedHashSet<>();
        // The innermost cause says what went wrong (wrappers: reported exceptions, execution exceptions, invocations).
        while (cause.getCause() != null && cause.getCause() != cause && seen.add(cause)) cause = cause.getCause();
        this.root = cause;
        this.kind = cause.getClass().getSimpleName();
        this.error = cause.getClass().getName() + (cause.getMessage() == null ? "" : ": " + cause.getMessage());
        StackTraceElement[] frames = cause.getStackTrace();
        String bridgedCode = null;
        String mod = null;
        String portium = null;
        boolean layer116 = false;
        boolean layer113 = false;
        for (StackTraceElement frame : frames) {
            String type = frame.getClassName();
            if (type.contains("PortiumBridge116$") || type.contains("PortiumBridge$")) {
                Matcher source = BRIDGE_SOURCE.matcher(String.valueOf(frame.getFileName()));
                if (bridgedCode == null && source.matches()) {
                    bridgedCode = "Minecraft " + source.group(1) + "'s own `" + source.group(2) + "." + frame.getMethodName() + "` (line " + frame.getLineNumber() + ")";
                }
            } else if (portium == null && type.startsWith("com.michaelsebero.portium.")) {
                portium = frame.toString();
            } else if (mod == null && isModCode(type)) {
                mod = frame.toString();
            }
            layer116 |= type.contains(".mods116.") || type.startsWith("forge116.") || type.startsWith("mc116.") || type.contains("PortiumBridge116$");
            layer113 |= type.contains(".portium.modern.") || type.startsWith("forge113.") || type.startsWith("mc113.");
        }
        this.bridged = bridgedCode;
        this.modFrame = mod;
        this.portiumFrame = portium;
        this.layer = layer116 ? "Forge 1.16.5 mod layer (mods116, glue in src/forge116)" : layer113 ? "Forge 1.13.2 mod layer (modern, glue in src/modern)" : "Portium itself or 1.12.2";
        this.missing = missing(cause);
        this.loop = loop(frames);
        this.explanation = explain();
        for (int i = 0; i < frames.length && this.stack.size() < 40; i++) {
            String frame = frames[i].toString();
            if (frame.startsWith("sun.reflect.") || frame.startsWith("java.lang.reflect.Method")) continue;
            this.stack.add(frame);
        }
        if (frames.length > 40) this.stack.add("... " + (frames.length - 40) + " more");
    }

    private static boolean isModCode(String type) {
        for (String prefix : new String[] {"java.", "javax.", "sun.", "jdk.", "net.minecraft.", "net.minecraftforge.", "com.mojang.", "org.lwjgl.", "org.apache.",
                "org.spongepowered.", "com.google.", "it.unimi.", "io.netty.", "forge116.", "forge113.", "mc116.", "mc113.", "com.michaelsebero.portium.",
                "zone.rong.", "org.prismlauncher.", "GradleStart"}) {
            if (type.startsWith(prefix)) return false;
        }
        return true;
    }

    @Nullable
    private static String missing(Throwable cause) {
        String message = cause.getMessage();
        if (message == null) return null;
        if (cause instanceof NoSuchMethodError || cause instanceof AbstractMethodError) {
            Matcher method = MISSING_METHOD.matcher(message.trim());
            if (method.find()) return "method `" + method.group(2) + method.group(3) + "` on `" + method.group(1).replace('/', '.') + "`";
        }
        if (cause instanceof NoSuchFieldError) return "field `" + message + "`";
        if (cause instanceof NoClassDefFoundError || cause instanceof ClassNotFoundException) return "class `" + message.replace('/', '.') + "`";
        if (cause instanceof ClassCastException) {
            Matcher cast = CAST.matcher(message);
            if (cast.find()) return "`" + cast.group(1) + "` was used as `" + cast.group(2) + "`";
        }
        return null;
    }

    /** For a stack overflow, the frames that repeat. */
    @Nullable
    private static String loop(StackTraceElement[] frames) {
        if (frames.length < 64) return null;
        for (int length = 1; length <= 12; length++) {
            boolean repeats = true;
            for (int i = 0; i < length * 4 && repeats; i++) repeats = frames[i].equals(frames[i + length]);
            if (repeats) {
                StringBuilder cycle = new StringBuilder();
                for (int i = 0; i < length; i++) cycle.append("\n    ").append(frames[i]);
                return cycle.toString();
            }
        }
        return null;
    }

    private String explain() {
        boolean inBridge = this.bridged != null;
        switch (this.kind) {
            case "NoSuchMethodError":
            case "NoSuchFieldError":
                if (inBridge) {
                    this.fixes.add("The failing code is " + this.bridged + ", which Portium runs because 1.12's class lacks that method. Redirect that 1.16 "
                            + "method to a Portium shim instead (`method(declarer, srgName, desc, shimOwner, shimName[, shimDesc])` in "
                            + "`src/main/java/com/michaelsebero/portium/mods116/ModernRedirects.java`, by 1.16 names; register it on every class 1.16 "
                            + "calls it through, e.g. both `AbstractBlock$AbstractBlockState` and `BlockState`).");
                    this.fixes.add("If the member is one Forge 1.16 patches into Minecraft's class, add it in `mods116/ForgePatches116.java` instead.");
                    return "Minecraft 1.16.5's own code, run in place of a method 1.12 lacks, needs " + nonNull(this.missing) + ", which 1.12's classes "
                            + "don't have (1.16's classes keep state in fields 1.12's lack).";
                }
                this.fixes.add("Add a redirect for it in `mods116/ModernRedirects.java` (1.16 names, matched before renaming) to a shim in "
                        + "`mods116/shim/` or the glue (`src/forge116/.../host/`), or give Portium's adapter of the class the mod extends that method "
                        + "with `@Srg(\"<srg id>\")` (adapters are in `mods116/adapter/`).");
                this.fixes.add("Check the redirect isn't shadowed: for one declarer the first entry registered wins (`-Dportium.dev.redirects=<file>` "
                        + "dumps the table).");
                return "Code written for a newer Minecraft calls " + nonNull(this.missing) + ", which 1.12.2 doesn't have and Portium doesn't provide yet.";
            case "AbstractMethodError":
                this.fixes.add("The class overrides the newer method but not 1.12's: bridge it (`ModernLinker.bridgeOverrides`, `ModernCompletion.RENAMED`, "
                        + "or `ModernSignatures` for renamed parameters), or implement 1.12's method on Portium's adapter calling the newer one.");
                return "A class built for a newer Minecraft doesn't implement " + nonNull(this.missing) + " that 1.12 calls.";
            case "ClassCastException":
                this.fixes.add("Convert at the boundary: a shim or redirect turning the newer object into 1.12's (text components into strings, "
                        + "1.16 types into 1.12's), or map the newer class onto 1.12's in `mods116/ModernMappings.java` (RENAMED).");
                return "An object of one game version's type reached code expecting another's: " + nonNull(this.missing) + ".";
            case "IncompatibleClassChangeError":
                this.fixes.add("If a newer class extends something that is an interface in 1.12 (or the reverse), give it an adapter: "
                        + "`ADAPTED.put(newerName, adapter)` in `mods116/ModernRedirects.java`, the adapter in `mods116/adapter/`.");
                return "A newer class's shape doesn't fit 1.12's (a class where 1.12 has an interface, or a static/instance mismatch).";
            case "NoClassDefFoundError":
            case "ClassNotFoundException":
                this.fixes.add("Map the class (`mods116/ModernMappings.java` RENAMED or the generated `classes-1.16.5.txt`), add a stand-in under "
                        + "`modern/mc/`, or check the class loads from the 1.16.5 jar (natives).");
                return "A class newer code needs couldn't be found or loaded: " + nonNull(this.missing) + ".";
            case "VerifyError":
                this.fixes.add("A redirect's target descriptor doesn't fit the call site (a stand-in returning Object where a class type is expected); "
                        + "return the exact type, or check the translator's output with `-Dportium.forge116.dump=<dir>`.");
                return "Translated bytecode failed the JVM's verifier.";
            case "LambdaConversionException":
            case "BootstrapMethodError":
                this.fixes.add("A method reference or lambda (`list::add`) in the mod points at a method of a type 1.12's version of the class "
                        + "doesn't have (1.16's list tag is a Java list; 1.12's isn't). `ModernTranslator.redirect(InvokeDynamicInsnNode, ...)` "
                        + "rewrites such handles through the redirect table: add a redirect for the receiver's class in `mods116/ModernRedirects.java`.");
                return "A method reference in newer code doesn't fit 1.12's classes.";
            case "StackOverflowError":
                this.fixes.add("Two of Portium's pieces call each other (a shim asks the block, the block's adapter asks the shim). The repeating frames "
                        + "are below; break the cycle by answering from 1.12's own method on one side.");
                return "Endless recursion" + (this.loop == null ? "." : ", repeating:" + this.loop);
            case "NullPointerException":
                if (inBridge) {
                    this.fixes.add("A static field 1.16 builds in a loop or from other classes can't be lifted into a bridge and reads as null: "
                            + "redirect the 1.16 method that reads it (" + this.bridged + ") to a Portium shim.");
                    return "Minecraft 1.16.5's own code, run in place of a method 1.12 lacks, read a value that isn't set up on 1.12.";
                }
                this.fixes.add("Look at the first mod or Portium frame below for the value that's null; often a 1.16 registry, tag or event "
                        + "field Portium hasn't filled.");
                return "A value was null where code expected one.";
            default:
                this.fixes.add("Start from the first mod or Portium frame in the stack below.");
                return "An unexpected " + this.kind + ".";
        }
    }

    private static String nonNull(@Nullable String text) {
        return text == null ? "something" : text;
    }
}
