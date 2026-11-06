package com.michaelsebero.portium.mods116.shim;

import net.minecraft.client.resources.Language;

/** 1.16's client methods 1.12 has otherwise. Client only. */
public final class ClientShims116 {

    /** Mojang's {@code language.getCode()}: 1.12's language code. */
    public static String languageCode(Language language) {
        return language.getLanguageCode();
    }

    /** {@code blockColors.getColor(state, world, pos, tintIndex)}. */
    public static int blockColor(net.minecraft.client.renderer.color.BlockColors colors, net.minecraft.block.state.IBlockState state,
                                 @javax.annotation.Nullable net.minecraft.world.IBlockAccess world, @javax.annotation.Nullable net.minecraft.util.math.BlockPos pos, int tintIndex) {
        return colors.colorMultiplier(state, world, pos, tintIndex);
    }

    /** 1.16's particle atlas: its sprites are in 1.12's block atlas. */
    public static net.minecraft.util.ResourceLocation particleAtlas() {
        return net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE;
    }

    public static boolean fancyGraphics(Object settings) {
        return ((net.minecraft.client.settings.GameSettings) settings).fancyGraphics;
    }

    public static net.minecraft.client.settings.KeyBinding self(net.minecraft.client.settings.KeyBinding binding) {
        return binding;
    }

    private static Language english;

    /** {@code languageManager.getSelected()}: never null (English until the languages have loaded). */
    public static Language currentLanguage(net.minecraft.client.resources.LanguageManager manager) {
        Language current = manager.getCurrentLanguage();
        if (current != null) return current;
        if (english == null) english = new Language("en_us", "US", "English", false);
        return english;
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

    /** 1.16's {@code keyBinding.func_238171_j_()}: the bound key's name, as a component (1.12's display name). */
    public static net.minecraft.util.text.ITextComponent keyMessage(net.minecraft.client.settings.KeyBinding binding) {
        return new net.minecraft.util.text.TextComponentString(binding.getDisplayName());
    }

    /** 1.16's {@code BiomeColors}: 1.12's blended biome colours at a position. */
    public static int grassColor(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return net.minecraft.world.biome.BiomeColorHelper.getGrassColorAtPos(world, pos);
    }

    public static int foliageColor(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return net.minecraft.world.biome.BiomeColorHelper.getFoliageColorAtPos(world, pos);
    }

    public static int waterColor(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return net.minecraft.world.biome.BiomeColorHelper.getWaterColorAtPos(world, pos);
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
    /**
     * 1.16's atlases besides the block atlas, whose stitch events mods add their sprites in (a chest's in the chest
     * atlas): 1.12 has only the block atlas, so its stitch is announced to mods once as each of them.
     */
    public static final String[] ATLASES_116 = {"textures/atlas/chest.png", "textures/atlas/shulker_boxes.png", "textures/atlas/beds.png",
            "textures/atlas/banner_patterns.png", "textures/atlas/shield_patterns.png", "textures/atlas/signs.png"};
    /** While the block atlas is announced as another of 1.16's atlases: that atlas's name. */
    @javax.annotation.Nullable
    public static net.minecraft.util.ResourceLocation announcedAtlas;

    public static net.minecraft.util.ResourceLocation atlasLocation(net.minecraft.client.renderer.texture.TextureMap map) {
        if (map == net.minecraft.client.Minecraft.getMinecraft().getTextureMapBlocks()) {
            return announcedAtlas != null ? announcedAtlas : net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE;
        }
        return new net.minecraft.util.ResourceLocation(map.getBasePath());
    }

    /** 1.16's {@code atlas.getSprite(name)}. */
    public static net.minecraft.client.renderer.texture.TextureAtlasSprite sprite(net.minecraft.client.renderer.texture.TextureMap map,
                                                                              net.minecraft.util.ResourceLocation name) {
        return map.getAtlasSprite(name.toString());
    }

    /** 1.16's {@code minecraft.getAtlasSpriteGetter(atlas)}: every atlas's sprites are in 1.12's block atlas. */
    public static java.util.function.Function<net.minecraft.util.ResourceLocation, net.minecraft.client.renderer.texture.TextureAtlasSprite> atlasSprites(
            net.minecraft.client.Minecraft minecraft, net.minecraft.util.ResourceLocation atlas) {
        return name -> minecraft.getTextureMapBlocks().getAtlasSprite(name.toString());
    }

    /** Forge 1.16's {@code TextureStitchEvent.Pre.addSprite(name)}. */
    public static boolean addSprite(net.minecraftforge.client.event.TextureStitchEvent.Pre event, net.minecraft.util.ResourceLocation name) {
        event.getMap().registerSprite(name);
        return true;
    }

    /**
     * 1.16's {@code dispatcher.getModelForState(state)}: 1.12's, but nothing where 1.12 answers an invisible block
     * (air) with the missing model; 1.16's air has an empty model. Create's shafts and cogs ask for air's model when
     * they have no bracket, and draw it.
     */
    public static net.minecraft.client.renderer.block.model.IBakedModel modelForState(net.minecraft.client.renderer.BlockRendererDispatcher dispatcher,
                                                                                      net.minecraft.block.state.IBlockState state) {
        return orEmpty(dispatcher.getBlockModelShapes(), state, dispatcher.getModelForState(state));
    }

    /** 1.16's {@code shapes.getModel(state)}, as {@link #modelForState}. */
    public static net.minecraft.client.renderer.block.model.IBakedModel modelFor(net.minecraft.client.renderer.BlockModelShapes shapes,
                                                                                 net.minecraft.block.state.IBlockState state) {
        return orEmpty(shapes, state, shapes.getModelForState(state));
    }

    private static net.minecraft.client.renderer.block.model.IBakedModel orEmpty(net.minecraft.client.renderer.BlockModelShapes shapes,
                                                                                 net.minecraft.block.state.IBlockState state,
                                                                                 net.minecraft.client.renderer.block.model.IBakedModel model) {
        if (state != null && state.getRenderType() == net.minecraft.util.EnumBlockRenderType.INVISIBLE && model == shapes.getModelManager().getMissingModel()) {
            return com.michaelsebero.portium.client.model.EmptyBakedModel.INSTANCE;
        }
        return model;
    }

    /** 1.16's {@code lightTexture.enableLightmap()}: 1.12's lightmap, which its renderer keeps (Create's ponder particles). */
    public static void enableLightmap(Object lightTexture) {
        net.minecraft.client.Minecraft.getMinecraft().entityRenderer.enableLightmap();
    }

    /** 1.16's {@code lightTexture.disableLightmap()}. */
    public static void disableLightmap(Object lightTexture) {
        net.minecraft.client.Minecraft.getMinecraft().entityRenderer.disableLightmap();
    }

    /** 1.16's {@code framebuffer.framebufferClear(onMac)}: 1.12's clear. */
    public static void clearFramebuffer(net.minecraft.client.shader.Framebuffer framebuffer, boolean onMac) {
        framebuffer.framebufferClear();
    }

    /** 1.16's {@code framebuffer.resize(width, height, onMac)}: 1.12's remaking at that size. */
    public static void resizeFramebuffer(net.minecraft.client.shader.Framebuffer framebuffer, int width, int height, boolean onMac) {
        framebuffer.createBindFramebuffer(width, height);
    }

    /** 1.16's {@code tickableSound.finishPlaying()}: done, as 1.12's moving sounds end (Create's machine sounds stop so). */
    public static void finishPlaying(net.minecraft.client.audio.MovingSound sound) {
        ((com.michaelsebero.portium.mixin.client.MovingSoundAccessor) sound).portium$setDonePlaying(true);
    }

    /** Forge 1.16's {@code event.getWindow()} on the overlay events: the window, as 1.16 code sees it. */
    public static com.michaelsebero.portium.modern.mc.client.MainWindow overlayWindow(net.minecraftforge.client.event.RenderGameOverlayEvent event) {
        return com.michaelsebero.portium.modern.mc.client.MainWindow.get();
    }

    /** 1.12's third person view setting (0 first person, 1 behind, 2 in front). */
    public static int thirdPersonView() {
        return net.minecraft.client.Minecraft.getMinecraft().gameSettings.thirdPersonView;
    }

    public static void setThirdPersonView(int view) {
        net.minecraft.client.Minecraft.getMinecraft().gameSettings.thirdPersonView = view;
    }
}
