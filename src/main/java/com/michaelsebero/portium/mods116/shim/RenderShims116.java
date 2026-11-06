package com.michaelsebero.portium.mods116.shim;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;

/** 1.16's rendering classes that 1.12 has under another shape. Client only. */
public final class RenderShims116 {

    /** {@code new VertexFormat(elements)}: 1.12 adds a format's elements one at a time. */
    public static VertexFormat vertexFormat(ImmutableList<VertexFormatElement> elements) {
        VertexFormat format = new VertexFormat();
        for (VertexFormatElement element : elements) {
            // 1.12 numbers texture coordinates without gaps; 1.16's entity formats leave one (the overlay as 1, the
            // light map as 2, which 1.12 calls 1). Unused numbers point at the element after them.
            if (element.getUsage() == VertexFormatElement.EnumUsage.UV) {
                java.util.List<Integer> uvs = uvOffsets(format);
                while (uvs != null && uvs.size() < element.getIndex()) uvs.add(format.getSize());
            }
            format.addElement(element);
        }
        return format;
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<Integer> uvOffsets(VertexFormat format) {
        for (String name : new String[] {"uvOffsetsById", "field_177351_f"}) {
            try {
                java.lang.reflect.Field field = VertexFormat.class.getDeclaredField(name);
                field.setAccessible(true);
                return (java.util.List<Integer>) field.get(format);
            } catch (ReflectiveOperationException e) {
                // The other name.
            }
        }
        return null;
    }

    /** 1.16's light map coordinates, numbered 2 (1.12 numbers them 1). */
    private static final VertexFormatElement LIGHTMAP_116 = new VertexFormatElement(2, VertexFormatElement.EnumType.SHORT,
            VertexFormatElement.EnumUsage.UV, 2);
    private static VertexFormat block116;
    private static VertexFormat particle116;

    /**
     * 1.16's {@code DefaultVertexFormats.BLOCK}: position, colour, texture, light map, normal and padding. 1.12's
     * field of the same id lacks the normal, and 1.16's buffer builders write blocks in exactly 1.16's layout.
     */
    public static synchronized VertexFormat block116() {
        if (block116 == null) {
            block116 = vertexFormat(ImmutableList.of(net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_3F,
                    net.minecraft.client.renderer.vertex.DefaultVertexFormats.COLOR_4UB, net.minecraft.client.renderer.vertex.DefaultVertexFormats.TEX_2F,
                    LIGHTMAP_116, net.minecraft.client.renderer.vertex.DefaultVertexFormats.NORMAL_3B,
                    net.minecraft.client.renderer.vertex.DefaultVertexFormats.PADDING_1B));
        }
        return block116;
    }

    /** 1.16's particle format: as 1.12's, with the light map numbered 2. */
    public static synchronized VertexFormat particle116() {
        if (particle116 == null) {
            particle116 = vertexFormat(ImmutableList.of(net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_3F,
                    net.minecraft.client.renderer.vertex.DefaultVertexFormats.TEX_2F, net.minecraft.client.renderer.vertex.DefaultVertexFormats.COLOR_4UB,
                    LIGHTMAP_116));
        }
        return particle116;
    }

    /** {@code format.getElements()}, as 1.16's immutable list. */
    public static ImmutableList<VertexFormatElement> elements(VertexFormat format) {
        return ImmutableList.copyOf(format.getElements());
    }

    private RenderShims116() {}

    /** A capability of instanced drawing, read by a 1.16 mod: not there (see ModernRedirects). */
    public static boolean noInstancing(Object capabilities) {
        return false;
    }

    /** 1.16's {@code WorldRenderer.getCombinedLight(world, pos)}: sky and block light packed as 1.12 packs them. */
    public static int combinedLight(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return packedLight(world, world.getBlockState(pos), pos);
    }

    /** 1.16's {@code WorldRenderer.getPackedLightmapCoords(world, state, pos)}: the state's own light counts as block light. */
    public static int packedLight(net.minecraft.world.IBlockAccess world, net.minecraft.block.state.IBlockState state, net.minecraft.util.math.BlockPos pos) {
        return world.getCombinedLight(pos, state.getLightValue(world, pos));
    }

    /** 1.12's frustum for this frame, as 1.16's clipping helper (1.12's class of the same name). */
    public static Object clippingHelper() {
        return net.minecraft.client.renderer.culling.ClippingHelperImpl.getInstance();
    }

    /** 1.16's {@code clippingHelper.isVisible(box)}, the box in world coordinates; 1.12's frustum is the camera's. */
    public static boolean isVisible(@javax.annotation.Nullable net.minecraft.client.renderer.culling.ClippingHelper helper,
                                    net.minecraft.util.math.AxisAlignedBB box) {
        if (helper == null) return true;
        double x = net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.staticPlayerX;
        double y = net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.staticPlayerY;
        double z = net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.staticPlayerZ;
        return helper.isBoxInFrustum(box.minX - x, box.minY - y, box.minZ - z, box.maxX - x, box.maxY - y, box.maxZ - z);
    }

    /**
     * LWJGL 3's {@code glTexImage3D(..., long pixels)}: 0 is no data (an empty texture to fill later). LWJGL 2's long
     * overload means an offset into a bound pixel buffer, and refuses without one; no data there is a null buffer.
     */
    public static void texImage3D(int target, int level, int internalFormat, int width, int height, int depth, int border, int format, int type,
                                  long pixels) {
        if (pixels == 0L) {
            org.lwjgl.opengl.GL12.glTexImage3D(target, level, internalFormat, width, height, depth, border, format, type, (java.nio.ByteBuffer) null);
        } else {
            org.lwjgl.opengl.GL12.glTexImage3D(target, level, internalFormat, width, height, depth, border, format, type, pixels);
        }
    }

    /** LWJGL 3's {@code glMapBufferRange(target, offset, length, access)}; LWJGL 2's also takes a buffer to reuse. */
    public static java.nio.ByteBuffer mapBufferRange(int target, long offset, long length, int access) {
        return org.lwjgl.opengl.GL30.glMapBufferRange(target, offset, length, access, null);
    }

    /**
     * Flywheel's GPU backend (instanced and VBO drawing with its own shaders) needs LWJGL 3 and 1.16's renderer; asked
     * whether it can be used, the answer is no, and Flywheel's mods draw the way they do without it.
     */
    public static boolean flywheelBackend(Object backend) {
        return false;
    }

    public static boolean flywheelBackend(Object backend, Object world) {
        return false;
    }

    /** 1.12's render layer by name (or none), as Forge 1.16's render type for it is set. */
    public static void setRenderLayer(@javax.annotation.Nullable String layer) {
        net.minecraftforge.client.ForgeHooksClient.setRenderLayer(layer == null ? null : net.minecraft.util.BlockRenderLayer.valueOf(layer));
    }

    /** Forge 1.16's {@code format.getOffset(index)}: an element's byte offset, 1.12's own (under an SRG id outside development). */
    public static int offset(VertexFormat format, int index) {
        return format.getOffset(index);
    }

    /**
     * 1.16's {@code renderer.render(tileEntity, partialTicks, stack, buffers, light, overlay)} on whatever renderer
     * the dispatcher gave: a 1.16 mod's draws so; 1.12's renderers draw through 1.12's own calls, not a matrix stack, and
     * are left out here (Create's ponder scenes draw their tile entities this way).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void renderTileEntity(Object renderer, net.minecraft.tileentity.TileEntity tileEntity, float partialTicks, Object matrixStack, Object buffers,
                                        int light, int overlay) {
        if (renderer instanceof com.michaelsebero.portium.mods116.adapter.TileEntityRenderer116) {
            ((com.michaelsebero.portium.mods116.adapter.TileEntityRenderer116) renderer).render(tileEntity, partialTicks, matrixStack, buffers, light, overlay);
        }
    }

    /** 1.16's {@code quad.isShade()}: whether it takes 1.12's diffuse lighting. */
    public static boolean shade(net.minecraft.client.renderer.block.model.BakedQuad quad) {
        return quad.shouldApplyDiffuseLighting();
    }

    /**
     * 1.16's {@code new BakedQuad(data, tint, face, sprite, shade)}: 1.12's, in the format the data has (7 ints a
     * vertex, as 1.12's models make them, or 8, 1.16's block layout). Create copies quads so.
     */
    public static net.minecraft.client.renderer.block.model.BakedQuad bakedQuad(int[] data, int tint, net.minecraft.util.EnumFacing face,
                                                                                  net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, boolean shade) {
        VertexFormat format = data.length == 32 ? block116() : net.minecraft.client.renderer.vertex.DefaultVertexFormats.ITEM;
        return new net.minecraft.client.renderer.block.model.BakedQuad(data, tint, face, sprite, shade, format);
    }

    /** Quads already given 1.16's layout, by the 1.12 quad they came from (quads are shared, so this stays small). */
    private static final java.util.Map<net.minecraft.client.renderer.block.model.BakedQuad, net.minecraft.client.renderer.block.model.BakedQuad> IN_116_LAYOUT =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /**
     * 1.16's {@code model.getQuads(state, side, random)} for 1.16 code: 1.12's quads, in 1.16's block layout (8 ints a
     * vertex), which 1.16 code reads directly. Create's connected textures rewrite a quad's texture coordinates at
     * 1.16's offsets, and 1.12's models make quads of 7 ints a vertex.
     */
    public static java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads116(net.minecraft.client.renderer.block.model.IBakedModel model,
            @javax.annotation.Nullable net.minecraft.block.state.IBlockState state, @javax.annotation.Nullable net.minecraft.util.EnumFacing side, java.util.Random random) {
        return quads116(model.getQuads(state, side, random.nextLong()));
    }

    /** Quads in 1.16's block layout; those already in it are kept. */
    public static java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads116(java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads) {
        java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> converted = null;
        for (int i = 0; i < quads.size(); i++) {
            net.minecraft.client.renderer.block.model.BakedQuad quad = quads.get(i);
            net.minecraft.client.renderer.block.model.BakedQuad in116 = to116(quad);
            if (in116 == quad) continue;
            if (converted == null) converted = new java.util.ArrayList<>(quads);
            converted.set(i, in116);
        }
        return converted != null ? converted : quads;
    }

    private static net.minecraft.client.renderer.block.model.BakedQuad to116(net.minecraft.client.renderer.block.model.BakedQuad quad) {
        VertexFormat from = quad.getFormat();
        VertexFormat block = block116();
        if (from == block || from.equals(block)) return quad;
        net.minecraft.client.renderer.block.model.BakedQuad known = IN_116_LAYOUT.get(quad);
        if (known != null) return known;
        int position = -1, color = -1, uv = -1, light = -1, normal = -1;
        for (int i = 0; i < from.getElementCount(); i++) {
            VertexFormatElement element = from.getElement(i);
            int offset = from.getOffset(i) / 4;
            switch (element.getUsage()) {
                case POSITION:
                    if (position < 0) position = offset;
                    break;
                case COLOR:
                    if (color < 0) color = offset;
                    break;
                case UV:
                    if (element.getIndex() == 0) {
                        if (uv < 0) uv = offset;
                    } else if (light < 0) {
                        light = offset;
                    }
                    break;
                case NORMAL:
                    if (normal < 0) normal = offset;
                    break;
                default:
                    break;
            }
        }
        int[] data = quad.getVertexData();
        int stride = from.getIntegerSize();
        int vertices = data.length / stride;
        int[] out = new int[vertices * 8];
        for (int v = 0; v < vertices; v++) {
            int s = v * stride;
            int d = v * 8;
            if (position >= 0) {
                out[d] = data[s + position];
                out[d + 1] = data[s + position + 1];
                out[d + 2] = data[s + position + 2];
            }
            out[d + 3] = color >= 0 ? data[s + color] : 0xFFFFFFFF;
            if (uv >= 0) {
                out[d + 4] = data[s + uv];
                out[d + 5] = data[s + uv + 1];
            }
            out[d + 6] = light >= 0 ? data[s + light] : 0;
            out[d + 7] = normal >= 0 ? data[s + normal] & 0x00FFFFFF : 0;
        }
        net.minecraft.client.renderer.block.model.BakedQuad in116 = new net.minecraft.client.renderer.block.model.BakedQuad(out, quad.getTintIndex(), quad.getFace(),
                quad.getSprite(), quad.shouldApplyDiffuseLighting(), block);
        IN_116_LAYOUT.put(quad, in116);
        return in116;
    }
}
