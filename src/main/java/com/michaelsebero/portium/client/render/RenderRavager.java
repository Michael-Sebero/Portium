package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelRavager;
import com.michaelsebero.portium.entity.monster.EntityRavager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class RenderRavager extends RenderLiving<EntityRavager> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/illager/ravager.png");

    public RenderRavager(RenderManager manager) {
        super(manager, new ModelRavager(), 1.1F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityRavager ravager) {
        return TEXTURE;
    }
}
