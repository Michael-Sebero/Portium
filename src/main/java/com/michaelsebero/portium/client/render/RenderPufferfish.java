package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelPufferfish;
import com.michaelsebero.portium.entity.fish.EntityPufferfish;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;

/** The pufferfish (1.13), switching model as it puffs up, and bobbing gently. */
public class RenderPufferfish extends RenderLiving<EntityPufferfish> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/fish/pufferfish.png");
    private static final ModelPufferfish[] MODELS = {ModelPufferfish.small(), ModelPufferfish.medium(), ModelPufferfish.big()};

    public RenderPufferfish(RenderManager manager) {
        super(manager, MODELS[0], 0.1F);
    }

    @Override
    public void doRender(EntityPufferfish fish, double x, double y, double z, float entityYaw, float partialTicks) {
        int state = MathHelper.clamp(fish.getPuffState(), 0, 2);
        this.mainModel = MODELS[state];
        this.shadowSize = 0.1F + 0.1F * state;
        super.doRender(fish, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected void applyRotations(EntityPufferfish fish, float ageInTicks, float rotationYaw, float partialTicks) {
        GlStateManager.translate(0.0F, MathHelper.cos(ageInTicks * 0.05F) * 0.08F, 0.0F);
        super.applyRotations(fish, ageInTicks, rotationYaw, partialTicks);
    }

    @Nonnull
    @Override
    protected ResourceLocation getEntityTexture(EntityPufferfish fish) {
        return TEXTURE;
    }
}
