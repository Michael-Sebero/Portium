package com.michaelsebero.portium.client;

import com.michaelsebero.portium.mods116.ModernHooks;
import com.michaelsebero.portium.mods116.adapter.ItemStackTileEntityRenderer116;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 1.16 mods' item rendering on 1.12: their item stack renderers, drawn with a 1.16 matrix stack and buffers of their
 * own (an item renderer may draw while a tile entity's vertices wait in the shared ones), and 1.16's calls to draw an
 * item, made with 1.12's item renderer under the matrix stack's transform. 1.12 doesn't tell an item renderer the
 * view it draws for; Forge's camera transform, applied just before, does (MixinForgeHooksClientView116). Client only.
 */
public final class ItemRendering116 {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");
    /** 1.16's {@code OverlayTexture.NO_OVERLAY}. */
    private static final int NO_OVERLAY = 655360;
    private static final Set<Class<?>> OFF = Collections.synchronizedSet(new HashSet<>());
    private static ItemCameraTransforms.TransformType view = ItemCameraTransforms.TransformType.GUI;

    private static MethodHandle matrixStack;
    private static MethodHandle itemBuffers;
    private static MethodHandle finishBuffers;

    /** The view Forge 1.12 last set an item's camera transform for. */
    public static void viewing(ItemCameraTransforms.TransformType type) {
        view = type;
    }

    public static void render(ItemStackTileEntityRenderer116 renderer, ItemStack stack) {
        if (!OFF.isEmpty() && OFF.contains(renderer.getClass())) return;
        try {
            link();
            Object stackOfMatrices = matrixStack.invoke(0.0D, 0.0D, 0.0D);
            Object buffers = itemBuffers.invoke();
            renderer.render(stack, view, stackOfMatrices, buffers, currentLight(), NO_OVERLAY);
            finishBuffers.invoke(buffers);
        } catch (Throwable e) {
            if (OFF.add(renderer.getClass())) LOGGER.error("1.16 mod problem: item renderer {} was switched off", renderer.getClass().getName(), e);
            RenderBridge116.end("");
        }
    }

    /** The light 1.12 last set for what it draws, packed as 1.16 packs it. */
    public static int currentLight() {
        return (int) OpenGlHelper.lastBrightnessX & 0xFFFF | (int) OpenGlHelper.lastBrightnessY << 16;
    }

    /**
     * 1.16's {@code itemRenderer.renderItem(stack, view, light, overlay, stack of matrices, buffers)}: 1.12's item
     * renderer under the matrices' transform (as a 16-float column-major matrix, which the glue reads out).
     */
    public static void renderItem(ItemStack stack, Object type, int light, float[] matrix) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        boolean lit = lightForGui(type);
        try {
            multiply(matrix);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF, light >>> 16);
            mc.getRenderItem().renderItem(stack, (ItemCameraTransforms.TransformType) type);
        } finally {
            if (lit) net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }
    }

    /**
     * 1.16 lights an item while drawing it (its render type does), where 1.12's screens light the items they draw
     * themselves: a 1.16 mod drawing an item into a screen gets 1.12's GUI item lighting for the item, if none is on.
     * Whether it was switched on here.
     */
    private static boolean lightForGui(Object type) {
        if (type != ItemCameraTransforms.TransformType.GUI || org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_LIGHTING)) return false;
        net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
        return true;
    }

    /** 1.16's {@code isSideLit()} of a model (lit from the side, as blocks are, or from the front, as flat items): 1.12's 3D in GUIs. */
    public static boolean sideLit(IBakedModel model) {
        return model.isGui3d();
    }

    /** 1.16's GUI light directions (flat items', 3D items'), which 1.12's GUI item lighting sets while an item draws. */
    public static void guiLightDirections() {}

    /** 1.16's {@code enableStandardItemLighting()}: 1.12's. */
    public static void standardLighting() {
        net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
    }

    /** 1.16's world light directions under a matrix: 1.12 sets its own when lighting is switched on. */
    public static void worldLightDirections(Object matrix) {}

    /** The same with the model given (already chosen, overrides applied). */
    public static void renderModel(ItemStack stack, Object type, boolean leftHand, int light, float[] matrix, Object model) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        boolean lit = lightForGui(type);
        try {
            multiply(matrix);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF, light >>> 16);
            mc.getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE);
            IBakedModel transformed = ForgeHooksClient.handleCameraTransforms((IBakedModel) model, (ItemCameraTransforms.TransformType) type, leftHand);
            mc.getRenderItem().renderItem(stack, transformed);
        } finally {
            if (lit) net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }
    }

    private static void multiply(float[] matrix) {
        java.nio.FloatBuffer buffer = org.lwjgl.BufferUtils.createFloatBuffer(16);
        buffer.put(matrix).flip();
        GlStateManager.multMatrix(buffer);
    }

    private static void link() throws ReflectiveOperationException {
        if (finishBuffers != null) return;
        Class<?> glue = Class.forName("com.michaelsebero.portium.mods116.host.RenderGlue", true, ModernHooks.loader());
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        matrixStack = lookup.findStatic(glue, "matrixStack", MethodType.methodType(Object.class, double.class, double.class, double.class));
        itemBuffers = lookup.findStatic(glue, "itemBuffers", MethodType.methodType(Object.class));
        finishBuffers = lookup.findStatic(glue, "finishBuffers", MethodType.methodType(void.class, Object.class));
    }

    private ItemRendering116() {}
}
