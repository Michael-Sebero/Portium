package com.michaelsebero.portium.mods116;

import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client events Forge 1.16 posts on each mod's own bus, which Forge 1.12 posts on its main bus: handed on to the 1.16
 * mods' buses. Model baking is where mods swap in their own models (Create hides a turning shaft's static model so
 * only its renderer's turning one shows), texture stitching where they add sprites, colour handlers where they tint.
 */
@SideOnly(Side.CLIENT)
final class ModernClientEvents {

    private final IModernHost host;

    ModernClientEvents(IModernHost host) {
        this.host = host;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void modelRegistry(ModelRegistryEvent event) {
        this.host.postToMods(event);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void modelBake(ModelBakeEvent event) {
        com.michaelsebero.portium.client.ModelShims116.bake(event.getModelRegistry());
        this.host.postToMods(event);
        if (Boolean.getBoolean("portium.dev.models")) {
            for (Object key : event.getModelRegistry().getKeys()) {
                String name = String.valueOf(key);
                if (name.startsWith("create:cogwheel#") || name.startsWith("create:shaft#")) {
                    org.apache.logging.log4j.LogManager.getLogger("Portium/1.16.5").info("Model {} is {}", name, event.getModelRegistry().getObject((net.minecraft.client.renderer.block.model.ModelResourceLocation) key).getClass().getName());
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void textureStitchPre(TextureStitchEvent.Pre event) {
        if (event.getMap() == net.minecraft.client.Minecraft.getMinecraft().getTextureMapBlocks()) com.michaelsebero.portium.client.ModelShims116.stitch(event.getMap());
        this.host.postToMods(event);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void textureStitchPost(TextureStitchEvent.Post event) {
        this.host.postToMods(event);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void blockColors(ColorHandlerEvent.Block event) {
        this.host.postToMods(event);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void itemColors(ColorHandlerEvent.Item event) {
        this.host.postToMods(event);
    }

    // 1.16 mods draw into the shared render buffers during these events; what they left is drawn once they're done.

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterWorld(net.minecraftforge.client.event.RenderWorldLastEvent event) {
        finishShared();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterOverlay(net.minecraftforge.client.event.RenderGameOverlayEvent.Post event) {
        finishShared();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterHand(net.minecraftforge.client.event.RenderSpecificHandEvent event) {
        finishShared();
    }

    private static java.lang.invoke.MethodHandle finishShared;

    private static void finishShared() {
        try {
            if (finishShared == null) {
                Class<?> glue = Class.forName("com.michaelsebero.portium.mods116.host.RenderGlue", true, ModernHooks.loader());
                finishShared = java.lang.invoke.MethodHandles.publicLookup().findStatic(glue, "finishShared", java.lang.invoke.MethodType.methodType(void.class));
            }
            finishShared.invoke();
        } catch (Throwable e) {
            org.apache.logging.log4j.LogManager.getLogger("Portium/1.16.5").warn("Couldn't draw 1.16 mods' buffers", e);
        }
    }
}
