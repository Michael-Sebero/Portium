package com.michaelsebero.portium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Where 1.16 mods' rendering meets 1.12's: vertices 1.16's own buffer builders made (in 1.16's layouts) drawn with
 * 1.12's client arrays, and the GL state of a 1.16 render type set up from its description (texture, transparency,
 * alpha test, culling, light map, lighting, depth, line width) with 1.12's state manager, so 1.12's cache stays true.
 * Client only.
 */
public final class RenderBridge116 {

    private static final Pattern TEXTURE = Pattern.compile("texture\\[Optional\\[([^\\]]+)\\]");
    private static final Pattern ALPHA = Pattern.compile("alpha\\[([0-9.Ee-]+)\\]");
    private static final Pattern LINE_WIDTH = Pattern.compile("line_width\\[([0-9.]+)\\]");
    private static final Map<String, State> STATES = new ConcurrentHashMap<>();

    /** A 1.16 render type's GL state, read once from its description. */
    private static final class State {

        final ResourceLocation texture;
        final int transparency;
        final float alpha;
        final boolean cull;
        final boolean lightmap;
        final boolean lighting;
        final boolean depthTest;
        final boolean depthWrite;
        final float lineWidth;

        State(String description) {
            Matcher texture = TEXTURE.matcher(description);
            this.texture = texture.find() ? new ResourceLocation(texture.group(1)) : null;
            if (description.contains("translucent_transparency") || description.contains("crumbling_transparency")) {
                this.transparency = 1;
            } else if (description.contains("additive_transparency") || description.contains("glint_transparency")) {
                this.transparency = 2;
            } else if (description.contains("lightning_transparency")) {
                this.transparency = 3;
            } else {
                this.transparency = 0;
            }
            Matcher alpha = ALPHA.matcher(description);
            this.alpha = alpha.find() ? Float.parseFloat(alpha.group(1)) : -1.0F;
            this.cull = !description.contains("cull[false]");
            this.lightmap = description.contains("lightmap[true]");
            this.lighting = description.contains("diffuse_lighting[true]");
            this.depthTest = !description.contains("depth_test[always]");
            this.depthWrite = !description.contains("writeDepth=false");
            Matcher width = LINE_WIDTH.matcher(description);
            this.lineWidth = width.find() ? Float.parseFloat(width.group(1)) : -1.0F;
        }
    }

    /** Sets up the GL state a 1.16 render type (by its description) draws in. */
    public static void begin(String description) {
        State state = STATES.computeIfAbsent(description, State::new);
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (state.texture != null) {
            GlStateManager.enableTexture2D();
            mc.getTextureManager().bindTexture(state.texture);
        } else {
            GlStateManager.disableTexture2D();
        }
        switch (state.transparency) {
            case 1:
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                break;
            case 2:
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                break;
            case 3:
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                break;
            default:
                GlStateManager.disableBlend();
        }
        // 1.16's zero alpha test (and none at all) draws every pixel.
        if (state.alpha > 0.0F) {
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, state.alpha);
        } else {
            GlStateManager.disableAlpha();
        }
        if (state.cull) {
            GlStateManager.enableCull();
        } else {
            GlStateManager.disableCull();
        }
        if (state.lightmap) {
            mc.entityRenderer.enableLightmap();
        } else {
            mc.entityRenderer.disableLightmap();
        }
        if (state.lighting) {
            RenderHelper.enableStandardItemLighting();
        } else {
            RenderHelper.disableStandardItemLighting();
        }
        if (state.depthTest) {
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
        } else {
            GlStateManager.disableDepth();
        }
        GlStateManager.depthMask(state.depthWrite);
        if (state.lineWidth > 0.0F) GlStateManager.glLineWidth(state.lineWidth);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
    }

    /** Puts back what 1.12 expects while drawing entities and tile entities. */
    public static void end(String description) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.glLineWidth(1.0F);
        mc.entityRenderer.enableLightmap();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Draws vertices a 1.16 buffer builder made: {@code count} of them from the buffer in the format (one of 1.12's
     * formats, built to 1.16's layout), as 1.16's vertex uploader would. 1.16 numbers the light map's coordinates 2
     * and the overlay's 1; 1.12 draws the light map from its second texture unit and has no overlay.
     */
    public static void drawArrays(ByteBuffer buffer, int mode, Object vertexFormat, int count) {
        if (count <= 0) return;
        VertexFormat format = (VertexFormat) vertexFormat;
        int stride = format.getSize();
        List<VertexFormatElement> elements = format.getElements();
        for (int i = 0; i < elements.size(); i++) {
            VertexFormatElement element = elements.get(i);
            buffer.position(format.getOffset(i));
            if (element.getUsage() == VertexFormatElement.EnumUsage.UV) {
                coordinates(element, stride, buffer, true);
            } else {
                element.getUsage().preDraw(format, i, stride, buffer);
            }
        }
        GlStateManager.glDrawArrays(mode, 0, count);
        for (int i = 0; i < elements.size(); i++) {
            VertexFormatElement element = elements.get(i);
            if (element.getUsage() == VertexFormatElement.EnumUsage.UV) {
                coordinates(element, stride, buffer, false);
            } else {
                element.getUsage().postDraw(format, i, stride, buffer);
            }
        }
        buffer.position(0);
    }

    private static void coordinates(VertexFormatElement element, int stride, ByteBuffer buffer, boolean on) {
        int unit;
        if (element.getIndex() == 0) {
            unit = OpenGlHelper.defaultTexUnit;
        } else if (element.getIndex() == 2) {
            unit = OpenGlHelper.lightmapTexUnit;
        } else {
            return;
        }
        OpenGlHelper.setClientActiveTexture(unit);
        if (on) {
            GlStateManager.glTexCoordPointer(element.getElementCount(), element.getType().getGlConstant(), stride, buffer);
            GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        } else {
            GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        }
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    // The camera, for 1.16's ActiveRenderInfo (in the glue, as it hands out 1.16's own vectors).

    /** A number that changes from one frame to the next (the camera is read once per frame). */
    public static long frame() {
        Minecraft mc = Minecraft.getMinecraft();
        long time = mc.world == null ? 0L : mc.world.getTotalWorldTime();
        return time << 32 | (Float.floatToIntBits(mc.getRenderPartialTicks()) & 0xFFFFFFFFL);
    }

    /** The view: x, y, z, yaw, pitch, and 1 when in third person (1.12's camera offset included). */
    public static double[] camera() {
        Minecraft mc = Minecraft.getMinecraft();
        net.minecraft.entity.Entity entity = mc.getRenderViewEntity();
        if (entity == null) return new double[6];
        double[] eyes = eyes(entity, mc.getRenderPartialTicks());
        net.minecraft.util.math.Vec3d offset = net.minecraft.client.renderer.ActiveRenderInfo.getCameraPosition();
        int view = mc.gameSettings.thirdPersonView;
        float yaw = (float) eyes[3] + (view == 2 ? 180.0F : 0.0F);
        float pitch = view == 2 ? -(float) eyes[4] : (float) eyes[4];
        return new double[] {eyes[0] + offset.x, eyes[1] + offset.y, eyes[2] + offset.z, yaw, pitch, view != 0 ? 1.0D : 0.0D};
    }

    /** An entity's eyes at a partial tick: x, y, z, yaw, pitch. */
    public static double[] eyes(Object entityObject, float partialTicks) {
        net.minecraft.entity.Entity entity = (net.minecraft.entity.Entity) entityObject;
        double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + entity.getEyeHeight();
        double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        return new double[] {x, y, z, yaw, pitch};
    }

    /** The fluid at a position in the client's world (for the camera's fluid). */
    public static Object fluidAt(Object pos) {
        Minecraft mc = Minecraft.getMinecraft();
        return com.michaelsebero.portium.modern.shim.FluidShims.getFluidState(mc.world, (net.minecraft.util.math.BlockPos) pos);
    }

    /** The packed light at the view entity, for hand rendering. */
    public static int viewLight() {
        Minecraft mc = Minecraft.getMinecraft();
        net.minecraft.entity.Entity entity = mc.getRenderViewEntity();
        return entity == null || mc.world == null ? 0xF000F0 : mc.world.getCombinedLight(new net.minecraft.util.math.BlockPos(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ), 0);
    }

    // Baked models, for 1.16 code that renders them into its own vertex builders (Forge 1.16's renderModel).

    /** A block state's model, as 1.12 bakes it. */
    public static Object modelFor(Object state) {
        return Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState((net.minecraft.block.state.IBlockState) state);
    }

    /** All a 1.12 model's quads for a state: each side's and the rest. */
    public static List<Object> quads(Object model, Object state, long seed) {
        if (model == null) return java.util.Collections.emptyList();
        net.minecraft.client.renderer.block.model.IBakedModel baked = (net.minecraft.client.renderer.block.model.IBakedModel) model;
        net.minecraft.block.state.IBlockState blockState = (net.minecraft.block.state.IBlockState) state;
        List<Object> quads = new java.util.ArrayList<>();
        for (net.minecraft.util.EnumFacing side : net.minecraft.util.EnumFacing.values()) quads.addAll(baked.getQuads(blockState, side, seed));
        quads.addAll(baked.getQuads(blockState, null, seed));
        return quads;
    }

    /**
     * A 1.12 quad's four vertices for 1.16 code: per vertex x, y, z, u, v, the normal's x, y and z, and the colour
     * (packed ARGB, as float bits): the block's tint for the quad, shaded by side as 1.12 shades it.
     */
    /** As {@link #vertices(Object, Object)}, for an item's quad: tinted by the item's colours. */
    public static float[] itemVertices(Object quad, Object stack) {
        net.minecraft.client.renderer.block.model.BakedQuad baked = (net.minecraft.client.renderer.block.model.BakedQuad) quad;
        int tint = baked.hasTintIndex() ? Minecraft.getMinecraft().getItemColors().colorMultiplier((net.minecraft.item.ItemStack) stack, baked.getTintIndex()) : -1;
        return vertices(baked, tint, false);
    }

    public static float[] vertices(Object quad, Object state) {
        net.minecraft.client.renderer.block.model.BakedQuad baked = (net.minecraft.client.renderer.block.model.BakedQuad) quad;
        int tint = baked.hasTintIndex() ? Minecraft.getMinecraft().getBlockColors().colorMultiplier((net.minecraft.block.state.IBlockState) state, null, null,
                baked.getTintIndex()) : -1;
        return vertices(baked, tint, true);
    }

    private static float[] vertices(net.minecraft.client.renderer.block.model.BakedQuad baked, int tint, boolean shaded) {
        VertexFormat format = baked.getFormat();
        int[] data = baked.getVertexData();
        int stride = format.getIntegerSize();
        int position = -1;
        int uv = -1;
        List<VertexFormatElement> elements = format.getElements();
        for (int i = 0; i < elements.size(); i++) {
            VertexFormatElement element = elements.get(i);
            if (element.getUsage() == VertexFormatElement.EnumUsage.POSITION && position < 0) position = format.getOffset(i) / 4;
            if (element.getUsage() == VertexFormatElement.EnumUsage.UV && element.getIndex() == 0 && uv < 0) uv = format.getOffset(i) / 4;
        }
        int color = tint == -1 ? 0xFFFFFFFF : 0xFF000000 | tint;
        net.minecraft.util.EnumFacing face = baked.getFace();
        float shade = 1.0F;
        if (shaded && baked.shouldApplyDiffuseLighting() && face != null) {
            shade = face == net.minecraft.util.EnumFacing.UP ? 1.0F : face == net.minecraft.util.EnumFacing.DOWN ? 0.5F : face.getAxis() == net.minecraft.util.EnumFacing.Axis.Z ? 0.8F : 0.6F;
        }
        int r = (int) ((color >> 16 & 255) * shade);
        int g = (int) ((color >> 8 & 255) * shade);
        int b = (int) ((color & 255) * shade);
        int argb = 0xFF000000 | r << 16 | g << 8 | b;
        float nx = face == null ? 0.0F : face.getXOffset();
        float ny = face == null ? 1.0F : face.getYOffset();
        float nz = face == null ? 0.0F : face.getZOffset();
        float[] out = new float[36];
        for (int v = 0; v < 4; v++) {
            int base = v * stride;
            int o = v * 9;
            out[o] = position < 0 ? 0.0F : Float.intBitsToFloat(data[base + position]);
            out[o + 1] = position < 0 ? 0.0F : Float.intBitsToFloat(data[base + position + 1]);
            out[o + 2] = position < 0 ? 0.0F : Float.intBitsToFloat(data[base + position + 2]);
            out[o + 3] = uv < 0 ? 0.0F : Float.intBitsToFloat(data[base + uv]);
            out[o + 4] = uv < 0 ? 0.0F : Float.intBitsToFloat(data[base + uv + 1]);
            out[o + 5] = nx;
            out[o + 6] = ny;
            out[o + 7] = nz;
            out[o + 8] = Float.intBitsToFloat(argb);
        }
        return out;
    }

    private RenderBridge116() {}
}
