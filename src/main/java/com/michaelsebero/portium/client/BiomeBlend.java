package com.michaelsebero.portium.client;

import com.michaelsebero.portium.PortiumConfig;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.client.EnumHelperClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 1.13's Biome Blend video setting. Grass, foliage and water colours are the average of the biomes in a square
 * around each block, from 1x1 (off) to 15x15. 1.12 always averaged 3x3; 1.13 defaults to 5x5. Portium defaults to
 * 15x15, the smoothest borders between biomes, most visible between ocean waters. Saved in options.txt as
 * {@code biomeBlendRadius}, as in 1.13.
 */
@SideOnly(Side.CLIENT)
public final class BiomeBlend {

    public static final String KEY = "biomeBlendRadius";
    public static final String TRANSLATION = "options.biomeBlendRadius";
    private static final int MAX_RADIUS = 7;

    private static final int DEFAULT_RADIUS = MAX_RADIUS;

    private static volatile int radius = DEFAULT_RADIUS;

    public static int getRadius() {
        return radius;
    }

    /** Sets the radius, clamped to 0-7. Returns whether it changed. */
    public static boolean setRadius(int value) {
        int clamped = Math.max(0, Math.min(MAX_RADIUS, value));
        if (clamped == radius) return false;
        radius = clamped;
        return true;
    }

    /** The radius colours are blended with: the setting's, or 1.12's 1 (3x3) when the feature is switched off in Portium's config. */
    public static int effectiveRadius() {
        return PortiumConfig.visuals.biomeBlend ? radius : 1;
    }

    /** The number of blocks averaged on each side, as the setting reads: 1 when off, up to 15. */
    public static int width() {
        return radius * 2 + 1;
    }

    public static boolean isOption(GameSettings.Options option) {
        return option != null && TRANSLATION.equals(option.getTranslation());
    }

    /** The slider. Made on first use, so options.txt can be read before Forge's enum helper runs. */
    public static GameSettings.Options option() {
        return Holder.OPTION;
    }

    /** "Biome Blend: 5x5", or "Biome Blend: OFF", as 1.13 labels it. */
    public static String label() {
        String name = I18n.hasKey(TRANSLATION) ? I18n.format(TRANSLATION) : "Biome Blend";
        return name + ": " + (radius == 0 ? I18n.format("options.off") : width() + "x" + width());
    }

    /** Reads the setting from options.txt. 1.12's own loading skips keys it doesn't know. */
    public static void load(File options) {
        if (!options.isFile()) return;
        try {
            for (String line : Files.readAllLines(options.toPath(), StandardCharsets.UTF_8)) {
                if (!line.startsWith(KEY + ":")) continue;
                try {
                    setRadius(Integer.parseInt(line.substring(KEY.length() + 1).trim()));
                } catch (NumberFormatException ignored) {
                    // 1.13 also skips a value it can't read.
                }
            }
        } catch (IOException e) {
            // Read while the game starts, before Portium itself loads.
            LogManager.getLogger("Portium").warn("Couldn't read {} from {}", KEY, options, e);
        }
    }

    private static final class Holder {
        static final GameSettings.Options OPTION = EnumHelperClient.addOptions("PORTIUM_BIOME_BLEND_RADIUS", TRANSLATION, true, false,
                0.0F, MAX_RADIUS, 1.0F);
    }

    private BiomeBlend() {}
}
