package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelBee;
import com.michaelsebero.portium.entity.passive.bee.EntityBee;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

/** 1.15's bee renderer: red-eyed when angry, pollen-dusted when carrying nectar. */
public class RenderBee extends RenderLiving<EntityBee> {

    private static final ResourceLocation BEE = texture("bee");
    private static final ResourceLocation NECTAR = texture("bee_nectar");
    private static final ResourceLocation ANGRY = texture("bee_angry");
    private static final ResourceLocation ANGRY_NECTAR = texture("bee_angry_nectar");

    public RenderBee(RenderManager manager) {
        super(manager, new ModelBee(), 0.4F);
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Tags.MOD_ID, "textures/entity/bee/" + name + ".png");
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityBee bee) {
        if (bee.isAngry()) return bee.hasNectar() ? ANGRY_NECTAR : ANGRY;
        return bee.hasNectar() ? NECTAR : BEE;
    }
}
