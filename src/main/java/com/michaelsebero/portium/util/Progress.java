package com.michaelsebero.portium.util;

import net.minecraftforge.fml.common.ProgressManager;

/**
 * A bar on Forge's loading screen, for the slow work Portium does the first time it starts (downloading Minecraft
 * versions, importing their textures) so the player sees what it's doing. Closing it finishes any steps not taken, as
 * Forge won't take a bar down unfinished.
 */
public final class Progress implements AutoCloseable {

    private final ProgressManager.ProgressBar bar;
    private final int steps;
    private int done;

    private Progress(String title, int steps) {
        this.steps = Math.max(1, steps);
        this.bar = ProgressManager.push(title, this.steps);
    }

    public static Progress start(String title, int steps) {
        return new Progress(title, steps);
    }

    /** The next step, shown with this message. */
    public void step(String message) {
        if (this.done >= this.steps) return;
        this.done++;
        this.bar.step(message);
    }

    /** Takes steps up to {@code fraction} of the way (0 to 1), all shown with this message. */
    public void reach(double fraction, String message) {
        int target = (int) Math.min(this.steps, Math.floor(fraction * this.steps));
        while (this.done < target) step(message);
    }

    @Override
    public void close() {
        while (this.done < this.steps) step("");
        ProgressManager.pop(this.bar);
    }
}
