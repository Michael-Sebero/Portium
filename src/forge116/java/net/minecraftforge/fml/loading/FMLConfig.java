package net.minecraftforge.fml.loading;

/**
 * Portium's replacement for Forge 1.16's, which its launcher reads from {@code config/fml.toml}: Forge's defaults,
 * since Forge 1.12 launched the game and its own settings cover the rest.
 */
public class FMLConfig {

    public static void load() {
    }

    public static boolean splashScreenEnabled() {
        return false;
    }

    public static int loadingThreadCount() {
        return 1;
    }

    public static boolean runVersionCheck() {
        return false;
    }

    /** Where mods' default configs are copied from into a world's server configs: {@code defaultconfigs}. */
    public static String defaultConfigPath() {
        return "defaultconfigs";
    }
}
