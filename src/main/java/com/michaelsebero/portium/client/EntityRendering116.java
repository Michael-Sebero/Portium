package com.michaelsebero.portium.client;

import com.michaelsebero.portium.mods116.ModernHooks;
import com.michaelsebero.portium.mods116.adapter.EntityRenderer116;
import com.michaelsebero.portium.mods116.adapter.LayerRenderer116;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Draws a 1.16 mod's entity as 1.12 asks: its renderer's 1.16 {@code render} with a matrix stack at the entity
 * (relative to the camera, as 1.12 gives it), the shared render buffers, and the entity's light (1.12 packs it as 1.16
 * does), then the buffers are drawn. A renderer that fails is switched off, not the game.
 */
@SideOnly(Side.CLIENT)
public final class EntityRendering116 {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");
    private static final Set<Class<?>> OFF = Collections.synchronizedSet(new HashSet<>());

    private static MethodHandle matrixStack;
    private static MethodHandle renderBuffers;
    private static MethodHandle finishBuffers;
    private static MethodHandle translateRotate;
    private static MethodHandle pose;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void render(EntityRenderer116 renderer, Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        if (!OFF.isEmpty() && OFF.contains(renderer.getClass())) return;
        try {
            link();
            Object stack = matrixStack.invoke(x, y, z);
            Object buffers = renderBuffers.invoke();
            renderer.render(entity, yaw, partialTicks, stack, buffers, entity.getBrightnessForRender());
            finishBuffers.invoke(buffers);
        } catch (Throwable e) {
            if (OFF.add(renderer.getClass())) LOGGER.error("1.16.5 mod problem: entity renderer {} was switched off", renderer.getClass().getName(), e);
            RenderBridge116.end("");
        }
    }

    /**
     * A 1.16 mod's layer, as 1.12 draws layers: in the entity's model space (1.12's model view already moved, turned
     * and flipped as 1.16's stack would be), so its stack starts empty, and its buffers are drawn before 1.12 goes on.
     */
    public static void renderLayer(LayerRenderer116<?> layer, EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
                                   float ageInTicks, float netHeadYaw, float headPitch) {
        if (!OFF.isEmpty() && OFF.contains(layer.getClass())) return;
        try {
            link();
            Object stack = matrixStack.invoke(0.0D, 0.0D, 0.0D);
            Object buffers = renderBuffers.invoke();
            layer.render(stack, buffers, entity.getBrightnessForRender(), entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
            finishBuffers.invoke(buffers);
        } catch (Throwable e) {
            if (OFF.add(layer.getClass())) LOGGER.error("1.16.5 mod problem: entity layer {} was switched off", layer.getClass().getName(), e);
            RenderBridge116.end("");
        }
    }

    /** 1.16's {@code playerRenderer.renderRightArm(stack, buffers, light, player)}: 1.12's, under the stack's pose. */
    public static void renderRightArm(net.minecraft.client.renderer.entity.RenderPlayer renderer, Object stack, Object buffers, int light,
                                      net.minecraft.client.entity.AbstractClientPlayer player) {
        boolean pushed = pushPose(stack);
        renderer.renderRightArm(player);
        if (pushed) net.minecraft.client.renderer.GlStateManager.popMatrix();
    }

    public static void renderLeftArm(net.minecraft.client.renderer.entity.RenderPlayer renderer, Object stack, Object buffers, int light,
                                     net.minecraft.client.entity.AbstractClientPlayer player) {
        boolean pushed = pushPose(stack);
        renderer.renderLeftArm(player);
        if (pushed) net.minecraft.client.renderer.GlStateManager.popMatrix();
    }

    /** A 1.16 stack's pose multiplied onto 1.12's model view, pushed; false (nothing pushed) for none. */
    private static boolean pushPose(Object stack) {
        try {
            if (pose == null) {
                Class<?> glue = Class.forName("com.michaelsebero.portium.mods116.host.RenderGlue", true, ModernHooks.loader());
                pose = MethodHandles.publicLookup().findStatic(glue, "pose", MethodType.methodType(float[].class, Object.class));
            }
            float[] matrix = (float[]) pose.invoke(stack);
            java.nio.FloatBuffer buffer = org.lwjgl.BufferUtils.createFloatBuffer(16);
            buffer.put(matrix).flip();
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.multMatrix(buffer);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /** 1.16's {@code livingRenderer.getEntityModel()}: 1.12's main model. */
    public static ModelBase mainModel(RenderLivingBase<?> renderer) {
        return renderer.getMainModel();
    }

    /** 1.16's {@code modelRenderer.translateRotate(stack)}: the stack moved to the part's pivot and turned by its angles. */
    public static void translateRotate(ModelRenderer part, Object stack) {
        try {
            if (translateRotate == null) {
                Class<?> glue = Class.forName("com.michaelsebero.portium.mods116.host.RenderGlue", true, ModernHooks.loader());
                translateRotate = MethodHandles.publicLookup().findStatic(glue, "translateRotate", MethodType.methodType(void.class, Object.class,
                        float.class, float.class, float.class, float.class, float.class, float.class));
            }
            translateRotate.invoke(stack, part.rotationPointX, part.rotationPointY, part.rotationPointZ, part.rotateAngleX, part.rotateAngleY, part.rotateAngleZ);
        } catch (Throwable e) {
            throw new IllegalStateException("Couldn't move a 1.16 matrix stack to a model part", e);
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

    private EntityRendering116() {}
}
