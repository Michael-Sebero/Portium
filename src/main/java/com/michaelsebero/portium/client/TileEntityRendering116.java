package com.michaelsebero.portium.client;

import com.michaelsebero.portium.modern.mc.tileentity.TileEntityType;
import com.michaelsebero.portium.mods116.ModernHooks;
import com.michaelsebero.portium.mods116.adapter.TileEntityRenderer116;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.16 mods' tile entity renderers on 1.12: bound by tile entity type (Forge 1.16's {@code bindTileEntityRenderer},
 * which 1.12's dispatcher finds through {@code MixinTileEntityRendererDispatcher116}), and drawn with a 1.16 matrix
 * stack and render buffers made by the 1.16 glue ({@code RenderGlue}). A renderer that fails is switched off, logged
 * once, rather than taking the game down. Client only.
 */
public final class TileEntityRendering116 {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");
    /** 1.16's {@code OverlayTexture.NO_OVERLAY}. */
    private static final int NO_OVERLAY = 655360;
    private static final Map<TileEntityType<?>, TileEntitySpecialRenderer<?>> BY_TYPE = new ConcurrentHashMap<>();
    private static final Set<Class<?>> OFF = Collections.synchronizedSet(new HashSet<>());

    private static MethodHandle matrixStack;
    private static MethodHandle renderBuffers;
    private static MethodHandle finishBuffers;

    /** Forge 1.16's {@code ClientRegistry.bindTileEntityRenderer(type, factory)}, the renderer made at once. */
    public static void register(TileEntityType<?> type, TileEntitySpecialRenderer<?> renderer) {
        renderer.setRendererDispatcher(TileEntityRendererDispatcher.instance);
        BY_TYPE.put(type, renderer);
    }

    @Nullable
    public static TileEntitySpecialRenderer<?> forType(@Nullable TileEntityType<?> type) {
        return type == null ? null : BY_TYPE.get(type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void render(TileEntityRenderer116 renderer, TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!OFF.isEmpty() && OFF.contains(renderer.getClass())) return;
        Object buffers = null;
        try {
            link();
            Object stack = matrixStack.invoke(x, y, z);
            buffers = renderBuffers.invoke();
            int light = tile.getWorld() == null ? 0xF000F0 : tile.getWorld().getCombinedLight(tile.getPos(), 0);
            renderer.render(tile, partialTicks, stack, buffers, light, NO_OVERLAY);
            finishBuffers.invoke(buffers);
        } catch (Throwable e) {
            if (OFF.add(renderer.getClass())) LOGGER.error("1.16 mod problem: tile entity renderer {} was switched off", renderer.getClass().getName(), e);
            RenderBridge116.end("");
        }
    }

    private static void link() throws ReflectiveOperationException {
        if (finishBuffers != null) return;
        Class<?> glue = Class.forName("com.michaelsebero.portium.mods116.host.RenderGlue", true, ModernHooks.loader());
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        matrixStack = lookup.findStatic(glue, "matrixStack", MethodType.methodType(Object.class, double.class, double.class, double.class));
        renderBuffers = lookup.findStatic(glue, "renderBuffers", MethodType.methodType(Object.class));
        finishBuffers = lookup.findStatic(glue, "finishBuffers", MethodType.methodType(void.class, Object.class));
    }

    private TileEntityRendering116() {}
}
