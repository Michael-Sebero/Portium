package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.client.RenderLayers116;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderType;

import java.util.function.Predicate;

/** Forge 1.16's render layer lookup, onto 1.12's block render layers. The translator points the calls here. */
public final class RenderGlue {

    /** {@code RenderTypeLookup.setRenderLayer(block, type)}. */
    public static void setRenderLayer(Block block, RenderType type) {
        RenderLayers116.set(block, layer(type::equals));
    }

    /** {@code RenderTypeLookup.setRenderLayer(block, predicate)}: 1.12 draws a block in one layer, the last it takes. */
    public static void setRenderLayer(Block block, Predicate<RenderType> types) {
        RenderLayers116.set(block, layer(types));
    }

    /** Fluids draw as 1.12 draws them. */
    public static void setRenderLayer(Object fluid, RenderType type) {}

    private static String layer(Predicate<RenderType> types) {
        if (types.test(RenderType.func_228645_f_())) return "TRANSLUCENT";
        if (types.test(RenderType.func_228643_e_())) return "CUTOUT";
        if (types.test(RenderType.func_228641_d_())) return "CUTOUT_MIPPED";
        return "SOLID";
    }

    /** The render type Forge 1.16 says is being drawn ({@code ForgeHooksClient.setRenderLayer}), for its getter. */
    @javax.annotation.Nullable
    private static RenderType forgeLayer;

    /** Forge 1.16's {@code ForgeHooksClient.setRenderLayer(type)}: 1.12's by the type's block layer, the type kept too. */
    public static void setForgeRenderLayer(@javax.annotation.Nullable RenderType type) {
        forgeLayer = type;
        com.michaelsebero.portium.mods116.shim.RenderShims116.setRenderLayer(type == null ? null : layer(type::equals));
    }

    /** Forge 1.16's {@code MinecraftForgeClient.getRenderLayer()}. */
    @javax.annotation.Nullable
    public static RenderType getForgeRenderLayer() {
        return forgeLayer;
    }

    // 1.16's drawing on 1.12 (RenderBridge116): its buffers and render types, with 1.12's GL state.

    /** {@code WorldVertexBufferUploader.draw(buffer)}: the buffer's finished vertices, drawn by 1.12. */
    public static void draw(net.minecraft.client.renderer.BufferBuilder buffer) {
        com.mojang.datafixers.util.Pair<net.minecraft.client.renderer.BufferBuilder.DrawState, java.nio.ByteBuffer> next = buffer.func_227832_f_();
        net.minecraft.client.renderer.BufferBuilder.DrawState state = next.getFirst();
        com.michaelsebero.portium.client.RenderBridge116.drawArrays(next.getSecond(), state.func_227840_c_(), state.func_227838_a_(), state.func_227839_b_());
    }

    /** {@code type.finish(buffer, x, y, z)}: the buffer finished and drawn in the render type's state. */
    public static void finish(RenderType type, net.minecraft.client.renderer.BufferBuilder buffer) {
        if (!buffer.func_227834_j_()) return;
        buffer.func_178977_d();
        String description = type.toString();
        com.michaelsebero.portium.client.RenderBridge116.begin(description);
        try {
            draw(buffer);
        } finally {
            com.michaelsebero.portium.client.RenderBridge116.end(description);
        }
    }

    public static void finish(RenderType type, net.minecraft.client.renderer.BufferBuilder buffer, int x, int y, int z) {
        finish(type, buffer);
    }

    /** {@code type.setupRenderState()}. */
    public static void setupRenderState(RenderType type) {
        com.michaelsebero.portium.client.RenderBridge116.begin(type.toString());
    }

    /** {@code type.clearRenderState()}. */
    public static void clearRenderState(RenderType type) {
        com.michaelsebero.portium.client.RenderBridge116.end(type.toString());
    }

    /** {@code IRenderTypeBuffer.getImpl(buffer)}: a source drawn by 1.12. */
    public static net.minecraft.client.renderer.IRenderTypeBuffer.Impl getImpl(net.minecraft.client.renderer.BufferBuilder buffer) {
        return new LegacyRenderBuffers();
    }

    public static net.minecraft.client.renderer.IRenderTypeBuffer.Impl getImpl(java.util.Map<RenderType, net.minecraft.client.renderer.BufferBuilder> fixed,
                                                                                net.minecraft.client.renderer.BufferBuilder buffer) {
        return new LegacyRenderBuffers();
    }

    /**
     * {@code minecraft.getRenderTypeBuffers()}: none, as 1.12 has no such buffers; what's asked of them comes here too
     * ({@link #getBufferSource}), so the missing object is never used.
     */
    public static net.minecraft.client.renderer.RenderTypeBuffers getRenderTypeBuffers(net.minecraft.client.Minecraft minecraft) {
        return null;
    }

    /** {@code renderTypeBuffers.getBufferSource()} and its crumbling source: the shared source drawn by 1.12. */
    public static net.minecraft.client.renderer.IRenderTypeBuffer.Impl getBufferSource(net.minecraft.client.renderer.RenderTypeBuffers buffers) {
        return LegacyRenderBuffers.shared();
    }

    /** A new 1.16 matrix stack moved to a tile entity's position relative to the camera (for Portium's renderers). */
    public static Object matrixStack(double x, double y, double z) {
        com.mojang.blaze3d.matrix.MatrixStack stack = new com.mojang.blaze3d.matrix.MatrixStack();
        stack.func_227861_a_(x, y, z);
        return stack;
    }

    /**
     * 1.16's {@code modelRenderer.translateRotate(stack)} for a model part of 1.12's: to its pivot (in pixels), then
     * turned by its angles about z, y and x.
     */
    public static void translateRotate(Object stack, float pivotX, float pivotY, float pivotZ, float angleX, float angleY, float angleZ) {
        com.mojang.blaze3d.matrix.MatrixStack matrices = (com.mojang.blaze3d.matrix.MatrixStack) stack;
        matrices.func_227861_a_(pivotX / 16.0F, pivotY / 16.0F, pivotZ / 16.0F);
        if (angleZ != 0.0F) matrices.func_227863_a_(net.minecraft.util.math.vector.Vector3f.field_229183_f_.func_229193_c_(angleZ));
        if (angleY != 0.0F) matrices.func_227863_a_(net.minecraft.util.math.vector.Vector3f.field_229181_d_.func_229193_c_(angleY));
        if (angleX != 0.0F) matrices.func_227863_a_(net.minecraft.util.math.vector.Vector3f.field_229179_b_.func_229193_c_(angleX));
    }

    /** A 1.16 matrix stack's top pose, column by column (as GL takes it), for drawing 1.16 GUIs through 1.12. */
    public static float[] pose(Object stack) {
        java.nio.FloatBuffer buffer = java.nio.FloatBuffer.allocate(16);
        ((com.mojang.blaze3d.matrix.MatrixStack) stack).func_227866_c_().func_227870_a_().func_195879_b(buffer);
        return buffer.array();
    }

    /** A 1.16 matrix, column by column, for drawing what a 1.16 mod places with one through 1.12. */
    public static float[] matrix(Object matrix) {
        java.nio.FloatBuffer buffer = java.nio.FloatBuffer.allocate(16);
        ((net.minecraft.util.math.vector.Matrix4f) matrix).func_195879_b(buffer);
        return buffer.array();
    }

    /** The shared source, for Portium's renderers. */
    public static Object renderBuffers() {
        return LegacyRenderBuffers.shared();
    }

    /** Draws what a source holds. */
    public static void finishBuffers(Object buffers) {
        ((LegacyRenderBuffers) buffers).func_228461_a_();
    }

    /** Forge 1.16's {@code RenderTypeLookup.canRenderInLayer(state, type)}: whether that is the block's one 1.12 layer. */
    public static boolean canRenderInLayer(net.minecraft.block.BlockState state, RenderType type) {
        return layer(type::equals).equals(com.michaelsebero.portium.client.RenderLayers116.layerName(state.func_177230_c())) && isBlockType(type);
    }

    private static boolean isBlockType(RenderType type) {
        return type == RenderType.func_228639_c_() || type == RenderType.func_228641_d_() || type == RenderType.func_228643_e_()
                || type == RenderType.func_228645_f_();
    }

    /** Forge 1.16's {@code bufferBuilder.getVertexFormat()}: 1.16's own buffer builders are vanilla, without it. */
    public static net.minecraft.client.renderer.vertex.VertexFormat getVertexFormat(net.minecraft.client.renderer.BufferBuilder buffer) {
        // Its field shares 1.12's id, so in development it goes by 1.12's readable name.
        for (String name : new String[] {"field_179011_q", "vertexFormat"}) {
            try {
                java.lang.reflect.Field field = net.minecraft.client.renderer.BufferBuilder.class.getDeclaredField(name);
                field.setAccessible(true);
                return (net.minecraft.client.renderer.vertex.VertexFormat) field.get(buffer);
            } catch (ReflectiveOperationException e) {
                // The other name.
            }
        }
        throw new IllegalStateException("No vertex format field in the buffer builder");
    }

    /**
     * Forge 1.16's {@code BlockModelRenderer.renderModel(world, model, state, pos, stack, builder, checkSides, random,
     * seed, overlay, data)}: the model's quads (1.12's), put through the stack into the builder, at full light. 1.16's
     * light and occlusion from the world aren't worked out; code rendering models this way (into buffers it draws
     * later) sets its own light.
     */
    public static boolean renderModel(net.minecraft.client.renderer.BlockModelRenderer renderer, net.minecraft.world.IBlockDisplayReader world,
                                      net.minecraft.client.renderer.model.IBakedModel model, net.minecraft.block.BlockState state,
                                      net.minecraft.util.math.BlockPos pos, com.mojang.blaze3d.matrix.MatrixStack stack,
                                      com.mojang.blaze3d.vertex.IVertexBuilder builder, boolean checkSides, java.util.Random random, long seed, int overlay,
                                      net.minecraftforge.client.model.data.IModelData data) {
        putModel(model, state, stack, builder, 0xF000F0, overlay, seed, ModelGlue.modelData(model, world, pos, state, data));
        return true;
    }

    /** Forge 1.16's {@code BlockRendererDispatcher.renderModel(state, pos, world, stack, builder, checkSides, random, data)}. */
    public static boolean renderModel(net.minecraft.client.renderer.BlockRendererDispatcher dispatcher, net.minecraft.block.BlockState state,
                                      net.minecraft.util.math.BlockPos pos, net.minecraft.world.IBlockDisplayReader world,
                                      com.mojang.blaze3d.matrix.MatrixStack stack, com.mojang.blaze3d.vertex.IVertexBuilder builder, boolean checkSides,
                                      java.util.Random random, net.minecraftforge.client.model.data.IModelData data) {
        Object model = com.michaelsebero.portium.client.RenderBridge116.modelFor(state);
        putModel(model, state, stack, builder, 0xF000F0, 655360, 42L, ModelGlue.modelData(model, world, pos, state, data));
        return true;
    }

    /** Forge 1.16's {@code BlockRendererDispatcher.renderBlock(state, stack, buffers, light, overlay, data)}: in the block's layer. */
    public static void renderBlock(net.minecraft.client.renderer.BlockRendererDispatcher dispatcher, net.minecraft.block.BlockState state,
                                   com.mojang.blaze3d.matrix.MatrixStack stack, net.minecraft.client.renderer.IRenderTypeBuffer buffers, int light, int overlay,
                                   net.minecraftforge.client.model.data.IModelData data) {
        String layer = com.michaelsebero.portium.client.RenderLayers116.layerName(state.func_177230_c());
        RenderType type = "TRANSLUCENT".equals(layer) ? RenderType.func_228645_f_() : "CUTOUT".equals(layer) ? RenderType.func_228643_e_()
                : "CUTOUT_MIPPED".equals(layer) ? RenderType.func_228641_d_() : RenderType.func_228639_c_();
        putModel(com.michaelsebero.portium.client.RenderBridge116.modelFor(state), state, stack, buffers.getBuffer(type), light, overlay, 42L, data);
    }

    private static void putModel(Object model, Object state, com.mojang.blaze3d.matrix.MatrixStack stack, com.mojang.blaze3d.vertex.IVertexBuilder builder,
                                 int light, int overlay, long seed, net.minecraftforge.client.model.data.IModelData data) {
        com.mojang.blaze3d.matrix.MatrixStack.Entry entry = stack.func_227866_c_();
        net.minecraft.util.math.vector.Matrix4f matrix = entry.func_227870_a_();
        net.minecraft.util.math.vector.Matrix3f normal = entry.func_227872_b_();
        boolean debug = DEBUG_QUADS && String.valueOf(state).contains("casing");
        if (debug) org.apache.logging.log4j.LogManager.getLogger("Portium/quads").info("{} model {} data {}", state, model == null ? null : model.getClass().getName(), data);
        for (Object quad : quads(model, state, seed, data)) {
            float[] v = com.michaelsebero.portium.client.RenderBridge116.vertices(quad, state);
            if (debug) {
                net.minecraft.client.renderer.model.BakedQuad baked = (net.minecraft.client.renderer.model.BakedQuad) quad;
                org.apache.logging.log4j.LogManager.getLogger("Portium/quads").info("  {} {} uv {},{} {},{}", baked.func_178210_d(), "",
                        v[3], v[4], v[21], v[22]);
            }
            for (int i = 0; i < 4; i++) {
                int o = i * 9;
                int color = Float.floatToRawIntBits(v[o + 8]);
                builder.func_227888_a_(matrix, v[o], v[o + 1], v[o + 2]);
                builder.func_225586_a_(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24 & 255);
                builder.func_225583_a_(v[o + 3], v[o + 4]);
                builder.func_227891_b_(overlay);
                builder.func_227886_a_(light);
                builder.func_227887_a_(normal, v[o + 5], v[o + 6], v[o + 7]);
                builder.func_181675_d();
            }
        }
    }

    private static final boolean DEBUG_QUADS = Boolean.getBoolean("portium.dev.quads");

    private static final java.util.Map<Class<?>, java.util.Optional<java.lang.reflect.Method>> QUADS_WITH_DATA = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * A model's quads for a state. A 1.16 mod's model takes Forge's model data (Create's turning parts give their
     * whole model only to data that marks the baking of their buffers), so it's asked with the data given; 1.12's
     * models are asked as 1.12 asks them.
     */
    private static java.util.List<?> quads(Object model, Object state, long seed, net.minecraftforge.client.model.data.IModelData data) {
        java.lang.reflect.Method withData = model == null || data == null ? null : QUADS_WITH_DATA.computeIfAbsent(model.getClass(), type -> {
            for (java.lang.reflect.Method method : type.getMethods()) {
                if (method.getName().equals("getQuads") && method.getParameterCount() == 4 && method.getParameterTypes()[3].getName().endsWith("IModelData")) {
                    return java.util.Optional.of(method);
                }
            }
            return java.util.Optional.empty();
        }).orElse(null);
        if (withData == null) return com.michaelsebero.portium.client.RenderBridge116.quads(model, state, seed);
        // Through ModelGlue, which knows when a model's getQuads is calling super: asked directly, Create's
        // connected-texture models shifted their quads a second time on the way back up.
        net.minecraft.client.renderer.model.IBakedModel baked = (net.minecraft.client.renderer.model.IBakedModel) model;
        net.minecraft.block.BlockState blockState = (net.minecraft.block.BlockState) state;
        java.util.List<Object> all = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random();
        for (net.minecraft.util.Direction side : net.minecraft.util.Direction.values()) {
            random.setSeed(seed);
            all.addAll(ModelGlue.getQuads(baked, blockState, side, random, data));
        }
        random.setSeed(seed);
        all.addAll(ModelGlue.getQuads(baked, blockState, null, random, data));
        return all;
    }

    // Forge 1.16's render events hand out a matrix stack and buffers; 1.12's events don't have them. The stack starts
    // where 1.12's view already is; the shared buffers are drawn once the event's handlers are done (RenderEventsGlue).

    /** {@code RenderWorldLastEvent.getMatrixStack()}. */
    public static com.mojang.blaze3d.matrix.MatrixStack matrixStack(net.minecraftforge.client.event.RenderWorldLastEvent event) {
        return new com.mojang.blaze3d.matrix.MatrixStack();
    }

    /** {@code RenderGameOverlayEvent.getMatrixStack()}. */
    public static com.mojang.blaze3d.matrix.MatrixStack matrixStack(net.minecraftforge.client.event.RenderGameOverlayEvent event) {
        return new com.mojang.blaze3d.matrix.MatrixStack();
    }

    /** {@code RenderHandEvent.getMatrixStack()} (1.12's specific hand event). */
    public static com.mojang.blaze3d.matrix.MatrixStack matrixStack(net.minecraftforge.client.event.RenderHandEvent event) {
        return new com.mojang.blaze3d.matrix.MatrixStack();
    }

    /** {@code RenderHandEvent.getBuffers()}. */
    public static net.minecraft.client.renderer.IRenderTypeBuffer buffers(net.minecraftforge.client.event.RenderHandEvent event) {
        return LegacyRenderBuffers.shared();
    }

    /** {@code RenderHandEvent.getLight()}. */
    public static int light(net.minecraftforge.client.event.RenderHandEvent event) {
        return com.michaelsebero.portium.client.RenderBridge116.viewLight();
    }

    private static LegacyRenderBuffers items;

    /** Buffers of item renderers' own, as an item may be drawn while a tile entity's vertices wait in the shared ones. */
    public static Object itemBuffers() {
        if (items == null) items = new LegacyRenderBuffers();
        return items;
    }

    private static float[] matrix(com.mojang.blaze3d.matrix.MatrixStack stack) {
        java.nio.FloatBuffer buffer = java.nio.FloatBuffer.allocate(16);
        stack.func_227866_c_().func_227870_a_().func_195879_b(buffer);
        float[] values = new float[16];
        buffer.rewind();
        buffer.get(values);
        return values;
    }

    /** {@code itemRenderer.renderItem(stack, view, light, overlay, matrices, buffers)}: 1.12 draws it under the matrices. */
    public static void renderItem(net.minecraft.client.renderer.ItemRenderer renderer, net.minecraft.item.ItemStack stack,
                                  net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType view, int light, int overlay,
                                  com.mojang.blaze3d.matrix.MatrixStack matrices, net.minecraft.client.renderer.IRenderTypeBuffer buffers) {
        com.michaelsebero.portium.client.ItemRendering116.renderItem(stack, view, light, matrix(matrices));
    }

    /** {@code itemRenderer.renderItem(stack, view, leftHand, matrices, buffers, light, overlay, model)}. */
    public static void renderItemModel(net.minecraft.client.renderer.ItemRenderer renderer, net.minecraft.item.ItemStack stack,
                                       net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType view, boolean leftHand,
                                       com.mojang.blaze3d.matrix.MatrixStack matrices, net.minecraft.client.renderer.IRenderTypeBuffer buffers, int light, int overlay,
                                       net.minecraft.client.renderer.model.IBakedModel model) {
        com.michaelsebero.portium.client.ItemRendering116.renderModel(stack, view, leftHand, light, matrix(matrices), model);
    }

    /** {@code itemRenderer.renderQuads(matrices, builder, quads, stack, light, overlay)}: 1.12's quads, tinted for the item. */
    public static void renderQuads(net.minecraft.client.renderer.ItemRenderer renderer, com.mojang.blaze3d.matrix.MatrixStack matrices,
                                   com.mojang.blaze3d.vertex.IVertexBuilder builder, java.util.List<?> quads, net.minecraft.item.ItemStack stack, int light,
                                   int overlay) {
        com.mojang.blaze3d.matrix.MatrixStack.Entry entry = matrices.func_227866_c_();
        net.minecraft.util.math.vector.Matrix4f matrix = entry.func_227870_a_();
        net.minecraft.util.math.vector.Matrix3f normal = entry.func_227872_b_();
        for (Object quad : quads) {
            float[] v = com.michaelsebero.portium.client.RenderBridge116.itemVertices(quad, stack);
            put(builder, matrix, normal, v, light, overlay);
        }
    }

    private static void put(com.mojang.blaze3d.vertex.IVertexBuilder builder, net.minecraft.util.math.vector.Matrix4f matrix,
                            net.minecraft.util.math.vector.Matrix3f normal, float[] v, int light, int overlay) {
        for (int i = 0; i < 4; i++) {
            int o = i * 9;
            int color = Float.floatToRawIntBits(v[o + 8]);
            builder.func_227888_a_(matrix, v[o], v[o + 1], v[o + 2]);
            builder.func_225586_a_(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24 & 255);
            builder.func_225583_a_(v[o + 3], v[o + 4]);
            builder.func_227891_b_(overlay);
            builder.func_227886_a_(light);
            builder.func_227887_a_(normal, v[o + 5], v[o + 6], v[o + 7]);
            builder.func_181675_d();
        }
    }

    /** Draws what handlers left in the shared buffers (after a render event's handlers). */
    public static void finishShared() {
        LegacyRenderBuffers.shared().func_228461_a_();
    }

    /** 1.16's {@code gameSettings.getPointOfView()}: from 1.12's third person view number (0, 1 back, 2 front). */
    public static net.minecraft.client.settings.PointOfView pointOfView(Object settings) {
        net.minecraft.client.settings.PointOfView[] views = net.minecraft.client.settings.PointOfView.values();
        int view = com.michaelsebero.portium.mods116.shim.ClientShims116.thirdPersonView();
        return views[Math.max(0, Math.min(views.length - 1, view))];
    }

    /** 1.16's {@code gameSettings.setPointOfView(view)}. */
    public static void setPointOfView(Object settings, net.minecraft.client.settings.PointOfView view) {
        com.michaelsebero.portium.mods116.shim.ClientShims116.setThirdPersonView(view.ordinal());
    }

    private RenderGlue() {}

    /**
     * {@code gameRenderer.getLightTexture()}: none, as 1.12's renderer keeps its lightmap itself; what 1.16 code asks
     * of the light texture (switching it on and off) goes to 1.12's (ClientShims116).
     */
    @javax.annotation.Nullable
    public static net.minecraft.client.renderer.LightTexture lightTexture(net.minecraft.client.renderer.GameRenderer renderer) {
        return null;
    }

    /** Forge 1.16's {@code builder.applyBakedLighting(light, data)}: the brighter of the light given and the quad's own. */
    public static int applyBakedLighting(Object builder, int lightmapCoord, java.nio.ByteBuffer data) {
        int block = lightmapCoord & 0xFFFF;
        int sky = lightmapCoord >> 16 & 0xFFFF;
        // The first vertex's light, in 1.16's block layout (8 ints a vertex, light the 7th).
        int offset = 6 * 4;
        if (data.limit() >= offset + 4) {
            block = Math.max(block, Short.toUnsignedInt(data.getShort(offset)));
            sky = Math.max(sky, Short.toUnsignedInt(data.getShort(offset + 2)));
        }
        return block | sky << 16;
    }

    /** Forge 1.16's {@code builder.applyBakedNormals(generated, data, transform)}: the quad's own normal, if it has one. */
    public static void applyBakedNormals(Object builder, net.minecraft.util.math.vector.Vector3f generated, java.nio.ByteBuffer data,
                                         net.minecraft.util.math.vector.Matrix3f normalTransform) {
        if (data.limit() < 31) return;
        byte x = data.get(28);
        byte y = data.get(29);
        byte z = data.get(30);
        if (x != 0 || y != 0 || z != 0) {
            generated.func_195905_a(x / 127.0F, y / 127.0F, z / 127.0F);
            generated.func_229188_a_(normalTransform);
        }
    }

    /** 1.16's {@code options.graphicsMode}: fancy or fast, as 1.12 is set (1.12 has no fabulous). */
    public static net.minecraft.client.settings.GraphicsFanciness graphics(net.minecraft.client.GameSettings settings) {
        return com.michaelsebero.portium.mods116.shim.ClientShims116.fancyGraphics(settings) ? net.minecraft.client.settings.GraphicsFanciness.FANCY
                : net.minecraft.client.settings.GraphicsFanciness.FAST;
    }

    /** 1.15's {@code minecraft.loadingGui}: no loading overlay once 1.16 code runs. */
    public static net.minecraft.client.gui.LoadingGui loadingGui(net.minecraft.client.Minecraft minecraft) {
        return null;
    }

    /** {@code FontRenderer.getFontSet(id)}: none here (1.12 draws the text; see the glyph stand-ins below). */
    @javax.annotation.Nullable
    public static net.minecraft.client.gui.fonts.Font fontSet(net.minecraft.client.gui.FontRenderer font, net.minecraft.util.ResourceLocation id) {
        return null;
    }

    /** {@code Font.whiteGlyph()} of a font set from {@link #fontSet}: none. */
    @javax.annotation.Nullable
    public static net.minecraft.client.gui.fonts.TexturedGlyph whiteGlyph(@javax.annotation.Nullable net.minecraft.client.gui.fonts.Font fonts) {
        return null;
    }

    /** {@code TexturedGlyph.renderType(seeThrough)}: the solid layer, which flushing harms nothing. */
    public static RenderType glyphType(@javax.annotation.Nullable net.minecraft.client.gui.fonts.TexturedGlyph glyph, boolean seeThrough) {
        return RenderType.func_228639_c_();
    }

    /**
     * 1.16's {@code ModelBakery.LOCATIONS_BUILTIN_TEXTURES}, its materials (Botania adds its own through an accessor):
     * 1.12's set of always-stitched textures, each material added as its texture.
     */
    public static java.util.Set<Object> builtinMaterials() {
        java.util.Set<Object> textures = (java.util.Set<Object>) (Object) com.michaelsebero.portium.client.ModelShims116.builtinTextures();
        return new java.util.AbstractSet<Object>() {
            @Override
            public boolean add(Object material) {
                return textures.add(material instanceof net.minecraft.client.renderer.model.RenderMaterial
                        ? ((net.minecraft.client.renderer.model.RenderMaterial) material).func_229313_b_() : material);
            }

            @Override
            public boolean contains(Object material) {
                return textures.contains(material instanceof net.minecraft.client.renderer.model.RenderMaterial
                        ? ((net.minecraft.client.renderer.model.RenderMaterial) material).func_229313_b_() : material);
            }

            @Override
            public java.util.Iterator<Object> iterator() {
                return textures.iterator();
            }

            @Override
            public int size() {
                return textures.size();
            }
        };
    }
}
