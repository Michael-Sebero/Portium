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
}
