package net.minecraftforge.fml.loading;

import com.michaelsebero.portium.modern.ModernMods;
import net.minecraftforge.api.distmarker.Dist;

import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * Portium's replacement for Forge 1.13's: what mods ask of the loader. Forge 1.12 launched the game, so the
 * launcher's own parts (ModLauncher's class loader, transformers) aren't here.
 */
public class FMLLoader {

    public static Dist getDist() {
        return FMLEnvironment.dist;
    }

    public static LoadingModList getLoadingModList() {
        return LoadingModList.get();
    }

    public static Path getGamePath() {
        return FMLPaths.GAMEDIR.get();
    }

    public static Path getForgePath() {
        return FMLPaths.GAMEDIR.get();
    }

    public static Path[] getMCPaths() {
        return new Path[0];
    }

    public static Predicate<String> getClassLoaderExclusions() {
        return name -> false;
    }

    public static String getNaming() {
        return ModernMods.isDevelopment() ? "mcp" : "srg";
    }
}
