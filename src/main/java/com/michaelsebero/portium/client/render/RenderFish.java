package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelCod;
import com.michaelsebero.portium.client.model.ModelSalmon;
import com.michaelsebero.portium.entity.fish.EntityAbstractFish;
import com.michaelsebero.portium.entity.fish.EntityCod;
import com.michaelsebero.portium.entity.fish.EntitySalmon;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;

/** Cod and salmon (1.13): the body sways side to side, and a fish out of water lies on its side. */
public class RenderFish<T extends EntityAbstractFish> extends RenderLiving<T> {

    private final ResourceLocation texture;
    private final boolean salmon;

    private RenderFish(RenderManager manager, ModelBase model, float shadow, String texture, boolean salmon) {
        super(manager, model, shadow);
        this.texture = new ResourceLocation(Tags.MOD_ID, "textures/entity/fish/" + texture + ".png");
        this.salmon = salmon;
    }

    public static RenderFish<EntityCod> cod(RenderManager manager) {
        return new RenderFish<>(manager, new ModelCod(), 0.3F, "cod", false);
    }

    public static RenderFish<EntitySalmon> salmon(RenderManager manager) {
        return new RenderFish<>(manager, new ModelSalmon(), 0.4F, "salmon", true);
    }

    @Override
    protected void applyRotations(T fish, float ageInTicks, float rotationYaw, float partialTicks) {
        super.applyRotations(fish, ageInTicks, rotationYaw, partialTicks);
        boolean inWater = fish.isInWater();
        if (this.salmon) {
            float amplitude = inWater ? 1.0F : 1.3F;
            float frequency = inWater ? 1.0F : 1.7F;
            GlStateManager.rotate(amplitude * 4.3F * MathHelper.sin(frequency * 0.6F * ageInTicks), 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, 0.0F, -0.4F);
            if (!inWater) {
                GlStateManager.translate(0.2F, 0.1F, 0.0F);
                GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
            }
        } else {
            GlStateManager.rotate(4.3F * MathHelper.sin(0.6F * ageInTicks), 0.0F, 1.0F, 0.0F);
            if (!inWater) {
                GlStateManager.translate(0.1F, 0.1F, -0.1F);
                GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
            }
        }
    }

    @Nonnull
    @Override
    protected ResourceLocation getEntityTexture(T fish) {
        return this.texture;
    }
}
