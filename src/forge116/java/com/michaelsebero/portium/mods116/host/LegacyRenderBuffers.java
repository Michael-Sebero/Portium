package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.client.RenderBridge116;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.16's render type buffers drawn on 1.12: a buffer of 1.16's own per render type, filled by 1.16 code and drawn
 * through {@link RenderBridge116}, which sets each render type's GL state up with 1.12's state manager instead of
 * 1.16's render system (whose state cache 1.12 doesn't share). One source is shared on the render thread, as 1.16
 * shares Minecraft's; buffers are kept for reuse and grow as needed.
 */
public class LegacyRenderBuffers extends IRenderTypeBuffer.Impl {

    private static LegacyRenderBuffers shared;

    private final Map<RenderType, BufferBuilder> buffers = new HashMap<>();
    /** Render types begun and not yet drawn, in the order they began. */
    private final Map<RenderType, BufferBuilder> open = new LinkedHashMap<>();

    public LegacyRenderBuffers() {
        super(new BufferBuilder(256), new HashMap<>());
    }

    public static LegacyRenderBuffers shared() {
        if (shared == null) shared = new LegacyRenderBuffers();
        return shared;
    }

    @Override
    public IVertexBuilder getBuffer(RenderType type) {
        BufferBuilder buffer = this.open.get(type);
        if (buffer != null) return buffer;
        buffer = this.buffers.computeIfAbsent(type, key -> new BufferBuilder(256));
        buffer.func_181668_a(type.func_228664_q_(), type.func_228663_p_());
        this.open.put(type, buffer);
        return buffer;
    }

    /** {@code finish()}: draws everything begun. */
    @Override
    public void func_228461_a_() {
        List<RenderType> types = new ArrayList<>(this.open.keySet());
        for (RenderType type : types) func_228462_a_(type);
    }

    /** {@code finish(type)}. */
    @Override
    public void func_228462_a_(RenderType type) {
        BufferBuilder buffer = this.open.remove(type);
        if (buffer == null) return;
        RenderGlue.finish(type, buffer);
    }
}
