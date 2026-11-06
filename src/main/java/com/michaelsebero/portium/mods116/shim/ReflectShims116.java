package com.michaelsebero.portium.mods116.shim;

import java.lang.reflect.Method;

/**
 * 1.16 mods' reflection into Forge 1.16's privates that Portium's Forge classes lack: answered by the glue's versions
 * (Botania makes its brew registry through {@code GameData.makeRegistry}).
 */
public final class ReflectShims116 {

    public static Method getDeclaredMethod(Class<?> owner, String name, Class<?>... parameters) throws NoSuchMethodException {
        try {
            return owner.getDeclaredMethod(name, parameters);
        } catch (NoSuchMethodException e) {
            Method substitute = substitute(owner, name, parameters);
            if (substitute != null) return substitute;
            throw e;
        }
    }

    private static Method substitute(Class<?> owner, String name, Class<?>... parameters) {
        if (!"net.minecraftforge.registries.GameData".equals(owner.getName()) || !"makeRegistry".equals(name)) return null;
        try {
            Class<?> glue = Class.forName("com.michaelsebero.portium.mods116.host.RegistryGlue", true, com.michaelsebero.portium.mods116.ModernHooks.loader());
            return glue.getMethod(name, parameters);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private ReflectShims116() {}
}
