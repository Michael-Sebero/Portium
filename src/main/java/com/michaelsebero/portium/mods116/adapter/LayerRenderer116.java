package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.client.EntityRendering116;
import com.michaelsebero.portium.modern.Srg;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * What a 1.16 mod's entity layers extend in place of 1.16's {@code LayerRenderer}, a class where 1.12's of the same
 * name is an interface: 1.12's layer, which a mod adds to 1.12's living renderers as 1.16's, drawing through 1.16's
 * {@code render} with a 1.16 matrix stack and render buffers ({@link EntityRendering116#renderLayer}). Its renderer's
 * model is 1.12's (1.16's entity model is 1.12's model base here).
 */
@SideOnly(Side.CLIENT)
public abstract class LayerRenderer116<T extends EntityLivingBase> implements LayerRenderer<T> {

    @Nullable
    private static Method texture;
    private final Render<?> renderer;

    protected LayerRenderer116(Render<?> renderer) {
        this.renderer = renderer;
    }

    /** 1.16's: draws the layer for the entity with the stack into the buffers; nothing of its own here. */
    @Srg("func_225628_a_")
    public void render(Object matrixStack, Object buffers, int light, Entity entity, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {}

    /** 1.16's: the model of the renderer the layer is on. */
    @Srg("func_215332_c")
    @Nullable
    public ModelBase getEntityModel() {
        return this.renderer instanceof RenderLivingBase ? ((RenderLivingBase<?>) this.renderer).getMainModel() : null;
    }

    /** 1.16's: the renderer's texture for the entity. */
    @Srg("func_229139_a_")
    @Nullable
    protected ResourceLocation getEntityTexture(Entity entity) {
        try {
            if (texture == null) texture = ObfuscationReflectionHelper.findMethod(Render.class, "func_110775_a", ResourceLocation.class, Entity.class);
            return (ResourceLocation) texture.invoke(this.renderer, entity);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Override
    public void doRenderLayer(T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch,
                              float scale) {
        EntityRendering116.renderLayer(this, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
