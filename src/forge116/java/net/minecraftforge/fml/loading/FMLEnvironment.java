package net.minecraftforge.fml.loading;

import com.michaelsebero.portium.mods116.Mods116;
import net.minecraftforge.api.distmarker.Dist;

/**
 * Portium's replacement for Forge 1.13's, which asks its launcher: the physical side and the naming Minecraft runs
 * with, from Forge 1.12.
 */
public class FMLEnvironment {

    public static final Dist dist = Mods116.isClient() ? Dist.CLIENT : Dist.DEDICATED_SERVER;
    public static final String naming = Mods116.isDevelopment() ? "mcp" : "srg";
    /** Whether Minecraft runs as installed (SRG names) rather than in a development environment. */
    public static final boolean production = !Mods116.isDevelopment();
}
