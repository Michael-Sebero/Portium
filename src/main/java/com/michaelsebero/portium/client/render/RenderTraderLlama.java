package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import net.minecraft.client.model.ModelLlama;
import net.minecraft.client.renderer.entity.RenderLlama;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.util.ResourceLocation;

/** A llama with the trader's decor when it wears no carpet (1.14's {@code LlamaDecorLayer} for trader llamas). */
public class RenderTraderLlama extends RenderLlama {

    private static final ResourceLocation DECOR = new ResourceLocation(Tags.MOD_ID, "textures/entity/llama/decor/trader_llama.png");

    public RenderTraderLlama(RenderManager manager) {
        super(manager);
        RenderTraderLlama renderer = this;
        this.addLayer(new LayerRenderer<EntityLlama>() {
            private final ModelLlama model = new ModelLlama(0.5F);

            @Override
            public void doRenderLayer(EntityLlama llama, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                                      float headPitch, float scale) {
                if (llama.hasColor()) return;
                renderer.bindTexture(DECOR);
                this.model.setModelAttributes(renderer.getMainModel());
                this.model.render(llama, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            }

            @Override
            public boolean shouldCombineTextures() {
                return false;
            }
        });
    }
}
