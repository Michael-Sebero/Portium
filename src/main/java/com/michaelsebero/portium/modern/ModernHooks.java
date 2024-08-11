package com.michaelsebero.portium.modern;

/** Static methods the translator points 1.13 code at where 1.12 behaves differently from Forge 1.13's launcher. */
public final class ModernHooks {

    static volatile ClassLoader loader;
    static volatile ModernTranslator translator;

    /**
     * Stands in for {@link Thread#getContextClassLoader()}. Forge 1.13 code expects its transforming class loader
     * there, which sees mod classes; on 1.12 that's the class loader for 1.13 mods, whatever thread asks.
     */
    public static ClassLoader contextClassLoader(Thread thread) {
        ClassLoader modern = loader;
        return modern != null ? modern : thread.getContextClassLoader();
    }

    /**
     * For the glue: a 1.13 class as it will be loaded, so annotations scanned from it name Forge 1.13's classes the way
     * the loaded classes do.
     */
    public static byte[] translate(byte[] bytes) {
        return translator.translateForScan(bytes);
    }

    /** The name a Minecraft method or field of a class goes by at runtime, given its SRG name. */
    public static String memberName(Class<?> owner, String srg) {
        ModernTranslator current = translator;
        return current == null ? srg : current.memberName(owner.getName().replace('.', '/'), srg);
    }

    private ModernHooks() {}
}
