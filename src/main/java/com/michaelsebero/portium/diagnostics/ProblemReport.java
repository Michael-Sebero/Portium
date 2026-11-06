package com.michaelsebero.portium.diagnostics;

import com.michaelsebero.portium.Tags;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Portium's problem reports: every mod problem (a newer mod, or part of one, switched off) and every crash, written
 * as Markdown to {@code portium-reports/} in the game folder, for the player to hand to Portium's developer or an AI
 * assistant. Each problem says what's missing, whose code was running, and where in Portium such a gap is closed
 * ({@link ProblemAnalysis}), with the game's setup above. {@code latest.md} holds this session's problems; a crash
 * also gets a file of its own, which the crash report names.
 */
public final class ProblemReport {

    private static final Logger LOGGER = LogManager.getLogger("Portium");
    private static final int MOST = 200;
    private static final Map<String, Entry> PROBLEMS = new LinkedHashMap<>();
    @Nullable
    private static File lastCrash;
    @Nullable
    private static ProblemAnalysis lastCrashAnalysis;
    private static boolean announced;

    private static final class Entry {

        final String what;
        final ProblemAnalysis analysis;
        final String thread;
        final String time;
        int count = 1;

        Entry(String what, ProblemAnalysis analysis) {
            this.what = what;
            this.analysis = analysis;
            this.thread = Thread.currentThread().getName();
            this.time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        }
    }

    /** How many distinct problems this session has recorded (for test scripts). */
    public static int count() {
        synchronized (PROBLEMS) {
            return PROBLEMS.size();
        }
    }

    /** Each recorded problem in a line: what failed, the error, and how often (for test scripts). */
    public static java.util.List<String> summaries() {
        java.util.List<String> lines = new java.util.ArrayList<>();
        synchronized (PROBLEMS) {
            for (Entry entry : PROBLEMS.values()) lines.add(entry.what + " :: " + entry.analysis.error + (entry.count > 1 ? " (x" + entry.count + ")" : ""));
        }
        return lines;
    }

    /** A problem that didn't stop the game: a mod or part of one switched off. Once per distinct problem. */
    public static void record(String what, Throwable cause) {
        try {
            ProblemAnalysis analysis = new ProblemAnalysis(cause);
            String key = what + "|" + analysis.error;
            synchronized (PROBLEMS) {
                Entry known = PROBLEMS.get(key);
                if (known != null) {
                    known.count++;
                    return;
                }
                if (PROBLEMS.size() >= MOST) return;
                PROBLEMS.put(key, new Entry(what, analysis));
            }
            write(latest(), null, null);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.debug("Couldn't record a problem for the report", e);
        }
    }

    /** A crash (1.12's crash report being made): its own report, with the session's problems. */
    public static void crash(String description, Throwable cause) {
        // 1.12 makes a pretend report at start-up, to load the classes a real one needs,
        // and Forge one of its loading screen's details; neither is a crash.
        if ("ThisIsFake".equals(description) || "Loading screen debug info".equals(description)) return;
        try {
            ProblemAnalysis analysis = new ProblemAnalysis(cause);
            File file = new File(folder(), "crash-" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + ".md");
            write(file, description, analysis);
            write(latest(), description, analysis);
            lastCrash = file;
            lastCrashAnalysis = analysis;
        } catch (RuntimeException | LinkageError e) {
            LOGGER.debug("Couldn't write a crash report for Portium", e);
        }
    }

    /** For 1.12's crash report: what Portium makes of the crash, and where its full report is. */
    static String crashSection() {
        ProblemAnalysis analysis = lastCrashAnalysis;
        StringBuilder text = new StringBuilder();
        if (analysis != null) {
            text.append("\n\t\tPortium's reading: ").append(analysis.explanation.replace("\n", "\n\t\t"));
            if (analysis.missing != null) text.append("\n\t\tMissing: ").append(analysis.missing);
            if (analysis.bridged != null) text.append("\n\t\tIn: ").append(analysis.bridged);
            if (analysis.modFrame != null) text.append("\n\t\tMod code: ").append(analysis.modFrame);
            text.append("\n\t\tLayer: ").append(analysis.layer);
        }
        int problems;
        synchronized (PROBLEMS) {
            problems = PROBLEMS.size();
        }
        text.append("\n\t\tMod problems this session: ").append(problems);
        text.append("\n\t\tFull report for an AI assistant or the developer: ").append(lastCrash != null ? lastCrash.getAbsolutePath() : latest().getAbsolutePath());
        return text.toString();
    }

    private static File folder() {
        File game;
        try {
            game = Loader.instance().getConfigDir().getParentFile();
        } catch (RuntimeException | LinkageError e) {
            game = new File(".");
        }
        File folder = new File(game, "portium-reports");
        if (!folder.isDirectory() && !folder.mkdirs()) LOGGER.warn("Couldn't make {}", folder);
        return folder;
    }

    private static File latest() {
        return new File(folder(), "latest.md");
    }

    private static synchronized void write(File file, @Nullable String crash, @Nullable ProblemAnalysis crashAnalysis) {
        List<Entry> problems;
        synchronized (PROBLEMS) {
            problems = new ArrayList<>(PROBLEMS.values());
        }
        StringBuilder out = new StringBuilder();
        out.append("# Portium problem report\n\n");
        out.append("Written by Portium ").append(Tags.MOD_VERSION).append(" on ").append(new Date()).append(". Hand this whole file to Portium's ")
                .append("developer or an AI assistant working on Portium: each problem says what's missing, whose code was running and where in ")
                .append("Portium such gaps are closed. Paths are in Portium's source tree.\n\n");
        environment(out);
        if (crash != null && crashAnalysis != null) {
            out.append("## The crash\n\n");
            out.append("Minecraft's description: ").append(crash).append("\n\n");
            problem(out, "The game crashed", crashAnalysis, Thread.currentThread().getName(), new SimpleDateFormat("HH:mm:ss").format(new Date()), 1);
        }
        out.append("## Mod problems this session (").append(problems.size()).append(")\n\n");
        if (problems.isEmpty()) out.append("None.\n\n");
        out.append("Each was switched off where it failed; the game went on.\n\n");
        int index = 1;
        for (Entry entry : problems) {
            out.append("### ").append(index++).append(". ").append(entry.what).append("\n\n");
            problem(out, null, entry.analysis, entry.thread, entry.time, entry.count);
        }
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(out.toString());
        } catch (IOException e) {
            LOGGER.warn("Couldn't write {}", file, e);
            return;
        }
        if (!announced) {
            announced = true;
            LOGGER.warn("Portium wrote a problem report for the developer or an AI assistant: {}", file.getAbsolutePath());
        }
    }

    private static void problem(StringBuilder out, @Nullable String heading, ProblemAnalysis analysis, String thread, String time, int count) {
        if (heading != null) out.append("### ").append(heading).append("\n\n");
        out.append("- **Error:** `").append(analysis.error.replace("`", "'")).append("`\n");
        out.append("- **What it means:** ").append(analysis.explanation.replace("\n", "\n  ")).append("\n");
        if (analysis.missing != null) out.append("- **Missing:** ").append(analysis.missing).append("\n");
        if (analysis.bridged != null) out.append("- **Running:** ").append(analysis.bridged).append("\n");
        if (analysis.modFrame != null) out.append("- **Mod code:** `").append(analysis.modFrame).append("`\n");
        if (analysis.portiumFrame != null) out.append("- **Portium code:** `").append(analysis.portiumFrame).append("`\n");
        out.append("- **Layer:** ").append(analysis.layer).append("\n");
        out.append("- **Thread / time:** ").append(thread).append(", ").append(time).append(count > 1 ? " (" + count + " times)" : "").append("\n");
        out.append("- **Likely fix:**\n");
        for (String fix : analysis.fixes) out.append("  - ").append(fix).append("\n");
        out.append("\n```\n").append(analysis.error).append("\n");
        for (String frame : analysis.stack) out.append("    at ").append(frame).append("\n");
        out.append("```\n\n");
    }

    private static void environment(StringBuilder out) {
        out.append("## Setup\n\n");
        out.append("- Portium ").append(Tags.MOD_VERSION).append(", Minecraft 1.12.2, Forge ").append(ForgeVersion.getVersion()).append("\n");
        out.append("- Java ").append(System.getProperty("java.version")).append(" (").append(System.getProperty("java.vendor")).append("), ")
                .append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.arch")).append("\n");
        Object deobfuscated = net.minecraft.launchwrapper.Launch.blackboard == null ? null : net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment");
        out.append("- Names: ").append(Boolean.TRUE.equals(deobfuscated) ? "MCP (development)" : "SRG (a normal install)").append("\n");
        try {
            StringBuilder mods = new StringBuilder();
            for (ModContainer mod : Loader.instance().getActiveModList()) mods.append(mod.getModId()).append(' ').append(mod.getVersion()).append(", ");
            out.append("- 1.12.2 mods: ").append(mods.length() > 2 ? mods.substring(0, mods.length() - 2) : "none").append("\n");
        } catch (RuntimeException | LinkageError e) {
            out.append("- 1.12.2 mods: (not known yet)\n");
        }
        out.append("- 1.13.2 mods: ").append(mods(com.michaelsebero.portium.modern.ModernMods.descriptions())).append("\n");
        out.append("- 1.16.5 mods: ").append(mods(com.michaelsebero.portium.mods116.Mods116.descriptions())).append("\n");
        try {
            out.append("- World generation for new worlds: ").append(com.michaelsebero.portium.PortiumConfig.worldGen.defaultGeneration).append("\n");
        } catch (RuntimeException | LinkageError e) {
            // Config not loaded yet.
        }
        out.append("\n");
    }

    private static String mods(List<Map<String, String>> descriptions) {
        if (descriptions.isEmpty()) return "none";
        StringBuilder text = new StringBuilder();
        for (Map<String, String> mod : descriptions) text.append(mod.get("id")).append(' ').append(mod.get("version")).append(", ");
        return text.substring(0, text.length() - 2);
    }

    private ProblemReport() {}
}
