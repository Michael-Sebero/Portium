package com.michaelsebero.portium.mods116.shim;

import net.minecraft.client.resources.Language;

/** 1.16's client methods 1.12 has otherwise. Client only. */
public final class ClientShims116 {

    /** Mojang's {@code language.getCode()}: 1.12's language code. */
    public static String languageCode(Language language) {
        return language.getLanguageCode();
    }

    public static String languageName(Language language) {
        return language.toString();
    }

    /** Forge 1.16's {@code item.getItemStackTileEntityRenderer()}: Forge 1.12's. */
    public static net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer itemRenderer(net.minecraft.item.Item item) {
        return item.getTileEntityItemStackRenderer();
    }

    public static boolean shiftDown() {
        return net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
    }

    public static boolean controlDown() {
        return net.minecraft.client.gui.GuiScreen.isCtrlKeyDown();
    }

    public static boolean altDown() {
        return net.minecraft.client.gui.GuiScreen.isAltKeyDown();
    }

    private ClientShims116() {}

    /**
     * Forge 1.16 registers an entity's renderer by its type; Forge 1.12 by its class, and only before the render
     * manager is made, which 1.16 mods' client setup comes after: so it also goes straight into the render manager.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerEntityRenderer(com.michaelsebero.portium.modern.mc.entity.EntityType type,
                                              net.minecraftforge.fml.client.registry.IRenderFactory factory) {
        Class entityClass = type.getEntityClass();
        if (entityClass == null) return;
        net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(entityClass, factory);
        net.minecraft.client.renderer.entity.RenderManager manager = net.minecraft.client.Minecraft.getMinecraft().getRenderManager();
        if (manager == null) return;
        try {
            java.util.Map renderers = net.minecraftforge.fml.common.ObfuscationReflectionHelper.getPrivateValue(
                    net.minecraft.client.renderer.entity.RenderManager.class, manager, "field_78729_o");
            renderers.put(entityClass, factory.createRenderFor(manager));
        } catch (RuntimeException | LinkageError e) {
            org.apache.logging.log4j.LogManager.getLogger("Portium/1.16.5").warn("Couldn't make the renderer for {}", entityClass.getName(), e);
        }
    }

    /**
     * Forge 1.16's widgets of a screen being set up. 1.12's screens have buttons, which 1.16 code can't take as its
     * widgets, so none.
     */
    @SuppressWarnings("rawtypes")
    public static java.util.List widgets(net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent event) {
        return new java.util.ArrayList();
    }

    /** Adding a 1.16 widget to a 1.12 screen being set up: 1.12's screens only draw buttons, so it isn't shown yet. */
    public static void addWidget(net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent event,
                                 com.michaelsebero.portium.mods116.adapter.Widget116 widget) {}

    /** 1.16's {@code global} sound (heard the same everywhere): 1.12's sound without attenuation. */
    public static boolean isGlobal(net.minecraft.client.audio.PositionedSound sound) {
        return sound.getAttenuationType() == net.minecraft.client.audio.ISound.AttenuationType.NONE;
    }

    public static void setGlobal(net.minecraft.client.audio.PositionedSound sound, boolean global) {
        net.minecraftforge.fml.common.ObfuscationReflectionHelper.setPrivateValue(net.minecraft.client.audio.PositionedSound.class, sound,
                global ? net.minecraft.client.audio.ISound.AttenuationType.NONE : net.minecraft.client.audio.ISound.AttenuationType.LINEAR, "field_147666_i");
    }

    /** Forge 1.16's {@code ClientRegistry.bindTileEntityRenderer(type, factory)}: the renderer, made now, bound to the type. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void bindTileEntityRenderer(com.michaelsebero.portium.modern.mc.tileentity.TileEntityType type, java.util.function.Function factory) {
        Object renderer = factory.apply(net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.instance);
        if (renderer instanceof net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer) {
            com.michaelsebero.portium.client.TileEntityRendering116.register(type, (net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer) renderer);
        }
    }

    /** 1.16's {@code atlas.getTextureLocation()}: 1.12's blocks atlas goes by the same name 1.16's does. */
    public static net.minecraft.util.ResourceLocation atlasLocation(net.minecraft.client.renderer.texture.TextureMap map) {
        if (map == net.minecraft.client.Minecraft.getMinecraft().getTextureMapBlocks()) return net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE;
        return new net.minecraft.util.ResourceLocation(map.getBasePath());
    }

    /** 1.16's {@code atlas.getSprite(name)}. */
    public static net.minecraft.client.renderer.texture.TextureAtlasSprite sprite(net.minecraft.client.renderer.texture.TextureMap map,
                                                                              net.minecraft.util.ResourceLocation name) {
        return map.getAtlasSprite(name.toString());
    }

    /** Forge 1.16's {@code TextureStitchEvent.Pre.addSprite(name)}. */
    public static boolean addSprite(net.minecraftforge.client.event.TextureStitchEvent.Pre event, net.minecraft.util.ResourceLocation name) {
        event.getMap().registerSprite(name);
        return true;
    }
}
