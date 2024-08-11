package com.michaelsebero.portium.modern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What of the 1.13 mods couldn't run on 1.12: a mod that failed to load (and was left out, so the rest still load),
 * or a part of one that uses what Portium doesn't provide yet (switched off where it failed). Logged in full, and shown
 * to the player once on the main menu.
 */
public final class ModernFailures {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.13.2");
    private static final List<String> FAILURES = new ArrayList<>();
    private static final Set<String> SEEN = new LinkedHashSet<>();

    /** A mod or part of one that failed, once per distinct failure. */
    public static synchronized void add(String what, Throwable cause) {
        String line = what + ": " + describe(cause);
        if (!SEEN.add(line)) return;
        FAILURES.add(line);
        LOGGER.error("1.13.2 mod problem: {}", what, cause);
    }

    public static synchronized List<String> list() {
        return Collections.unmodifiableList(new ArrayList<>(FAILURES));
    }

    private static String describe(Throwable cause) {
        Throwable root = cause;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message == null ? "" : " " + message);
    }

    private ModernFailures() {}
}
