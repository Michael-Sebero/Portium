package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelTurtle;
import com.michaelsebero.portium.entity.passive.EntityTurtle;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class RenderTurtle extends RenderLiving<EntityTurtle> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/turtle/big_sea_turtle.png");

    public RenderTurtle(RenderManager manager) {
        super(manager, new ModelTurtle(), 0.7F);
    }

    @Override
    public void doRender(EntityTurtle turtle, double x, double y, double z, float entityYaw, float partialTicks) {
        this.shadowSize = turtle.isChild() ? 0.35F : 0.7F;
        super.doRender(turtle, x, y, z, entityYaw, partialTicks);
    }

    @Nonnull
    @Override
    protected ResourceLocation getEntityTexture(EntityTurtle turtle) {
        return TEXTURE;
    }
}
