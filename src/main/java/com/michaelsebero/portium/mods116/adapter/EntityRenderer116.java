package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.client.EntityRendering116;
import com.michaelsebero.portium.modern.Srg;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * What a 1.16 mod's entity renderers extend in place of 1.16's {@code EntityRenderer}: 1.12's renderer, whose drawing
 * calls 1.16's {@code render} with a 1.16 matrix stack at the entity, render buffers drawn by 1.12 and the entity's
 * light ({@link EntityRendering116}). 1.16 asks a renderer whether to draw with a clipping helper, which is 1.12's
 * class of that name; 1.12's own frustum check is added after.
 */
@SideOnly(Side.CLIENT)
public abstract class EntityRenderer116<T extends Entity> extends Render<T> {

    protected EntityRenderer116(RenderManager manager) {
        super(manager);
    }

    /** 1.16's: draws the entity with the stack (at it) into the buffers, at that light; nothing of its own here. */
    @Srg("func_225623_a_")
    public void render(T entity, float yaw, float partialTicks, Object matrixStack, Object buffers, int light) {}

    /** 1.16's: whether the entity is drawn this frame, by its distance (1.12's override checks the frustum too). */
    @Srg("func_225626_a_")
    public boolean shouldRender(T entity, @Nullable ClippingHelper camera, double cameraX, double cameraY, double cameraZ) {
        return entity.isInRangeToRender3d(cameraX, cameraY, cameraZ);
    }

    @Override
    public boolean shouldRender(T entity, ICamera camera, double cameraX, double cameraY, double cameraZ) {
        if (!shouldRender(entity, (ClippingHelper) null, cameraX, cameraY, cameraZ)) return false;
        return entity.ignoreFrustumCheck || camera.isBoundingBoxInFrustum(entity.getRenderBoundingBox().grow(0.5D));
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float yaw, float partialTicks) {
        EntityRendering116.render(this, entity, x, y, z, yaw, partialTicks);
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }
}
