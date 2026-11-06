package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelBoar;
import com.michaelsebero.portium.entity.monster.EntityHoglin;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

/** 1.16's hoglin and zoglin renderers (the same boar model, two textures); a hoglin about to become a zoglin shakes. */
public class RenderBoar<T extends EntityLiving> extends RenderLiving<T> {

    private final ResourceLocation texture;

    public RenderBoar(RenderManager manager, String texture) {
        super(manager, new ModelBoar(), 0.7F);
        this.texture = new ResourceLocation(Tags.MOD_ID, "textures/entity/" + texture + ".png");
    }

    @Override
    protected void applyRotations(T entity, float ageInTicks, float rotationYaw, float partialTicks) {
        if (entity instanceof EntityHoglin && ((EntityHoglin) entity).isConverting()) {
            rotationYaw += (float) (Math.cos(entity.ticksExisted * 3.25D) * Math.PI * 0.4D);
        }
        super.applyRotations(entity, ageInTicks, rotationYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return this.texture;
    }
}
