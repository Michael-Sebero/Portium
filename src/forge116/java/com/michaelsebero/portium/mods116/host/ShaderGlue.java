package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.mods116.shim.ShaderShims116;
import net.minecraft.client.shader.IShaderManager;

/** 1.16's {@code ShaderLinkHelper.linkProgram(manager)} of a mod's own shader manager (see {@link ShaderShims116}). */
public final class ShaderGlue {

    public static void linkProgram(IShaderManager manager) {
        ShaderShims116.link(manager.func_147986_h(), manager.func_147989_e(), manager.func_147994_f());
    }

    private ShaderGlue() {}
}
