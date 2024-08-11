package com.michaelsebero.portium.mods116.host;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;

/**
 * Flywheel's world rendering events, which Flywheel (the render library under Create's contraptions) fires from its
 * mixins into 1.16's world renderer; those can't reach 1.12's, so Portium fires them at the same points of 1.12's: a
 * frame begins, then after each block layer the mods draw theirs into the shared buffers, which are drawn at once, as
 * Flywheel does. Positions are relative to the camera and 1.12's view rotation is already in place, so the matrix
 * stack starts empty. Nothing happens without Flywheel; a failure switches the events off rather than the game.
 */
final class FlywheelGlue {

    private static final String EVENTS = "com.jozufozu.flywheel.event.";
    private static boolean looked;
    private static boolean off;
    @Nullable
    private static Constructor<?> beginFrame;
    @Nullable
    private static Constructor<?> renderLayer;
    @Nullable
    private static Constructor<?> reload;

    private static boolean present() {
        if (!looked) {
            looked = true;
            beginFrame = constructor("BeginFrameEvent");
            renderLayer = constructor("RenderLayerEvent");
            reload = constructor("ReloadRenderersEvent");
            if (renderLayer != null) {
                org.apache.logging.log4j.LogManager.getLogger("Portium/1.16.5").info("Firing Flywheel's world rendering events from 1.12's world renderer");
            }
        }
        return !off && renderLayer != null;
    }

    @Nullable
    private static Constructor<?> constructor(String name) {
        try {
            // The one taking arguments: Forge's event transformer adds every event a public one taking none.
            Constructor<?> found = null;
            for (Constructor<?> constructor : Class.forName(EVENTS + name, true, FlywheelGlue.class.getClassLoader()).getConstructors()) {
                if (constructor.getParameterCount() > 0 && (found == null || constructor.getParameterCount() > found.getParameterCount())) found = constructor;
            }
            return found;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (LinkageError e) {
            org.apache.logging.log4j.LogManager.getLogger("Portium/1.16.5").warn("Couldn't load Flywheel's {}", name, e);
            return null;
        }
    }

    static void beginFrame() {
        if (!present() || beginFrame == null || Minecraft.func_71410_x().field_71441_e == null) return;
        post(beginFrame, Minecraft.func_71410_x().field_71441_e, ActiveRenderInfo116.game(),
                com.michaelsebero.portium.mods116.shim.RenderShims116.clippingHelper());
    }

    static void renderLayer(int layer, double cameraX, double cameraY, double cameraZ) {
        if (!present() || Minecraft.func_71410_x().field_71441_e == null) return;
        RenderType type = type(layer);
        // A null RenderTypeBuffers: asked for its source, Portium gives the shared one (RenderGlue.getBufferSource).
        if (!post(renderLayer, Minecraft.func_71410_x().field_71441_e, type, new MatrixStack(), null, cameraX, cameraY, cameraZ)) return;
        LegacyRenderBuffers.shared().func_228462_a_(type);
    }

    static void reloadRenderers() {
        if (!present() || reload == null || Minecraft.func_71410_x().field_71441_e == null) return;
        post(reload, Minecraft.func_71410_x().field_71441_e);
    }

    /** 1.12's block layers in order: solid, cutout mipped, cutout, translucent. */
    private static RenderType type(int layer) {
        switch (layer) {
            case 0:
                return RenderType.func_228639_c_();
            case 1:
                return RenderType.func_228641_d_();
            case 2:
                return RenderType.func_228643_e_();
            default:
                return RenderType.func_228645_f_();
        }
    }

    private static boolean post(Constructor<?> constructor, Object... arguments) {
        try {
            MinecraftForge.EVENT_BUS.post((Event) constructor.newInstance(arguments));
            return true;
        } catch (Throwable e) {
            // Anything a listener throws (errors from a cache included): the events stop, the game goes on.
            off = true;
            Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e;
            com.michaelsebero.portium.mods116.ModernFailures.add("Flywheel's world rendering events (contraptions and the like) were switched off", cause);
            return false;
        }
    }

    private FlywheelGlue() {}
}
