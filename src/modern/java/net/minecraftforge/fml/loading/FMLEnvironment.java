package net.minecraftforge.fml.loading;

import com.michaelsebero.portium.modern.ModernMods;
import net.minecraftforge.api.distmarker.Dist;

/**
 * Portium's replacement for Forge 1.13's, which asks its launcher: the physical side and the naming Minecraft runs
 * with, from Forge 1.12.
 */
public class FMLEnvironment {

    public static final Dist dist = ModernMods.isClient() ? Dist.CLIENT : Dist.DEDICATED_SERVER;
    public static final String naming = ModernMods.isDevelopment() ? "mcp" : "srg";
}
