package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.GolemCracks;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderIronGolem;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.util.ResourceLocation;

/** 1.15's iron golem cracks, drawn over the golem by how hurt it is. */
public class LayerGolemCracks implements LayerRenderer<EntityIronGolem> {

    private static final ResourceLocation[] TEXTURES = {null, texture("low"), texture("medium"), texture("high")};

    private final RenderIronGolem renderer;

    public LayerGolemCracks(RenderIronGolem renderer) {
        this.renderer = renderer;
    }

    private static ResourceLocation texture(String level) {
        return new ResourceLocation(Tags.MOD_ID, "textures/entity/iron_golem/iron_golem_crackiness_" + level + ".png");
    }

    @Override
    public void doRenderLayer(EntityIronGolem golem, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                              float headPitch, float scale) {
        if (golem.isInvisible()) return;
        GolemCracks.Cracks cracks = GolemCracks.of(golem);
        if (cracks == GolemCracks.Cracks.NONE) return;
        this.renderer.bindTexture(TEXTURES[cracks.ordinal()]);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        this.renderer.getMainModel().render(golem, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.disableBlend();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
