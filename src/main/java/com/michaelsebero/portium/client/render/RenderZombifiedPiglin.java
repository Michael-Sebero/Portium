package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelPiglin;
import com.michaelsebero.portium.entity.monster.EntityZombifiedPiglin;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.util.ResourceLocation;

/** 1.16's zombified piglin: the piglin model with zombie arms, its own texture, armour and held items. */
public class RenderZombifiedPiglin extends RenderBiped<EntityZombifiedPiglin> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/piglin/zombified_piglin.png");

    public RenderZombifiedPiglin(RenderManager manager) {
        super(manager, new ModelPiglin(0.0F), 0.5F);
        this.addLayer(new LayerBipedArmor(this) {
            @Override
            protected void initArmor() {
                this.modelLeggings = new ModelBiped(0.5F);
                this.modelArmor = new ModelBiped(1.02F);
            }
        });
    }

    @Override
    protected void preRenderCallback(EntityZombifiedPiglin piglin, float partialTicks) {
        GlStateManager.scale(1.0019531F, 1.0F, 1.0019531F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityZombifiedPiglin piglin) {
        return TEXTURE;
    }
}
