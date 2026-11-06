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

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
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
            Object legacy = legacyMainModel(layer.renderer());
            if (legacy != null) {
                linkProxies();
                syncModel.invoke(legacy);
            }
            layer.render(stack, buffers, entity.getBrightnessForRender(), entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
            finishBuffers.invoke(buffers);
        } catch (Throwable e) {
            if (OFF.add(layer.getClass())) LOGGER.error("1.16.5 mod problem: entity layer {} was switched off", layer.getClass().getName(), e);
            RenderBridge116.end("");
        }
    }

    /** A 1.12 living renderer's model, or null for any other renderer (a 1.16 mod's has its own 1.16 model). */
    @Nullable
    public static Object legacyMainModel(Object renderer) {
        return renderer instanceof RenderLivingBase ? ((RenderLivingBase<?>) renderer).getMainModel() : null;
    }

    @Nullable
    private static RenderLivingBase<net.minecraft.entity.EntityLivingBase> standIn;

    /**
     * The renderer of living render events a 1.16 mod's renderer posts itself: Forge 1.12's events take a 1.12 living
     * renderer, which a mod's isn't; a model-less stand-in, so listeners asking for its model get one.
     */
    private static RenderLivingBase<net.minecraft.entity.EntityLivingBase> standIn() {
        if (standIn == null) {
            standIn = new RenderLivingBase<net.minecraft.entity.EntityLivingBase>(net.minecraft.client.Minecraft.getMinecraft().getRenderManager(),
                    new net.minecraft.client.model.ModelBiped(), 0.0F) {
                @Override
                @Nullable
                protected net.minecraft.util.ResourceLocation getEntityTexture(net.minecraft.entity.EntityLivingBase entity) {
                    return null;
                }
            };
        }
        return standIn;
    }

    public static net.minecraftforge.client.event.RenderLivingEvent.Pre<?> livingPre(Object entity, Object renderer, float partialTicks, Object stack, Object buffers,
            int light) {
        net.minecraft.entity.EntityLivingBase living = (net.minecraft.entity.EntityLivingBase) entity;
        return new net.minecraftforge.client.event.RenderLivingEvent.Pre<>(living, standIn(), partialTicks, living.posX, living.posY, living.posZ);
    }

    public static net.minecraftforge.client.event.RenderLivingEvent.Post<?> livingPost(Object entity, Object renderer, float partialTicks, Object stack, Object buffers,
            int light) {
        net.minecraft.entity.EntityLivingBase living = (net.minecraft.entity.EntityLivingBase) entity;
        return new net.minecraftforge.client.event.RenderLivingEvent.Post<>(living, standIn(), partialTicks, living.posX, living.posY, living.posZ);
    }

    @Nullable
    private static java.lang.reflect.Field legacyMainModelField;
    private static final Map<Class<?>, java.lang.reflect.Field> MODEL_FIELDS = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * A write to a living renderer's model ({@code livingRenderer.entityModel = model}). 1.12's renderers take 1.12
     * models only: a mod swapping one for a 1.16 model of its own (Alex's Mobs' wandering trader riding an elephant)
     * leaves 1.12's model in place. 1.16 renderers (mods') get theirs set.
     */
    public static void setMainModel(Object renderer, @Nullable Object model) {
        try {
            if (renderer instanceof RenderLivingBase) {
                if (!(model instanceof net.minecraft.client.model.ModelBase)) return;
                if (legacyMainModelField == null) {
                    legacyMainModelField = net.minecraftforge.fml.common.ObfuscationReflectionHelper.findField(RenderLivingBase.class, "field_77045_g");
                }
                legacyMainModelField.set(renderer, model);
                return;
            }
            java.lang.reflect.Field field = MODEL_FIELDS.computeIfAbsent(renderer.getClass(), type -> {
                // 1.16's living renderer's one model field (named so in production, by MCP in a development run).
                for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                    if (!c.getName().endsWith(".client.renderer.entity.LivingRenderer")) continue;
                    for (java.lang.reflect.Field each : c.getDeclaredFields()) {
                        if (each.getType().getName().endsWith(".client.renderer.entity.model.EntityModel")) {
                            each.setAccessible(true);
                            return each;
                        }
                    }
                }
                return null;
            });
            if (field != null && (model == null || field.getType().isInstance(model))) {
                field.set(renderer, model);
            } else {
                LOGGER.warn("1.16.5 mod problem: model {} of {} wasn't set", model == null ? null : model.getClass().getName(), renderer.getClass().getName());
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.debug("Couldn't set the model of {}", renderer.getClass().getName(), e);
        }
    }

    /**
     * A 1.12 biped model's pose for its 1.16 stand-in: head, body, right arm, left arm, right leg, left leg, each as
     * its angles, pivot and whether it shows; then sneaking and childhood. Null for other models.
     */
    @Nullable
    public static float[] bipedPose(Object model) {
        if (!(model instanceof net.minecraft.client.model.ModelBiped)) return null;
        net.minecraft.client.model.ModelBiped biped = (net.minecraft.client.model.ModelBiped) model;
        ModelRenderer[] parts = {biped.bipedHead, biped.bipedBody, biped.bipedRightArm, biped.bipedLeftArm, biped.bipedRightLeg, biped.bipedLeftLeg};
        float[] pose = new float[44];
        for (int i = 0; i < parts.length; i++) {
            ModelRenderer part = parts[i];
            int at = i * 7;
            pose[at] = part.rotateAngleX;
            pose[at + 1] = part.rotateAngleY;
            pose[at + 2] = part.rotateAngleZ;
            pose[at + 3] = part.rotationPointX;
            pose[at + 4] = part.rotationPointY;
            pose[at + 5] = part.rotationPointZ;
            pose[at + 6] = part.showModel ? 1.0F : 0.0F;
        }
        pose[42] = biped.isSneak ? 1.0F : 0.0F;
        pose[43] = biped.isChild ? 1.0F : 0.0F;
        return pose;
    }

    @Nullable
    private static MethodHandle proxyModel;
    @Nullable
    private static MethodHandle syncModel;

    private static void linkProxies() throws ReflectiveOperationException {
        if (syncModel != null) return;
        Class<?> glue = Class.forName("com.michaelsebero.portium.mods116.host.ModelProxyGlue", true, ModernHooks.loader());
        proxyModel = MethodHandles.publicLookup().findStatic(glue, "proxy", MethodType.methodType(glue.getMethod("proxy", Object.class).getReturnType(), Object.class));
        syncModel = MethodHandles.publicLookup().findStatic(glue, "sync", MethodType.methodType(void.class, Object.class));
    }

    /** The 1.16 stand-in for a 1.12 renderer's model (see the glue's ModelProxyGlue); null if it has none. */
    @Nullable
    public static Object proxyModel(RenderLivingBase<?> renderer) {
        try {
            linkProxies();
            return proxyModel.invoke((Object) renderer.getMainModel());
        } catch (Throwable e) {
            return null;
        }
    }

    /** A 1.16 renderer's call to one of its layers: a mod's (adapted) layer draws into the renderer's stack and buffers. */
    public static void renderLayer116(Object layer, Object stack, Object buffers, int light, net.minecraft.entity.Entity entity, float limbSwing,
                                      float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(layer instanceof LayerRenderer116) || !OFF.isEmpty() && OFF.contains(layer.getClass())) return;
        try {
            ((LayerRenderer116<?>) layer).render(stack, buffers, light, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        } catch (RuntimeException | LinkageError e) {
            if (OFF.add(layer.getClass())) LOGGER.error("1.16.5 mod problem: entity layer {} was switched off", layer.getClass().getName(), e);
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
