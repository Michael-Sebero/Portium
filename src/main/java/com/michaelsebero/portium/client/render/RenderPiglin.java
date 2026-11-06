package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelPiglin;
import com.michaelsebero.portium.entity.monster.piglin.EntityAbstractPiglin;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/** 1.16's piglin (and piglin brute) renderer: the piglin model, held items, armour, a worn head; shakes while turning into a zombie pigman. */
public class RenderPiglin<T extends EntityAbstractPiglin> extends RenderBiped<T> {

    private final ResourceLocation texture;

    public RenderPiglin(RenderManager manager, String texture) {
        super(manager, new ModelPiglin(0.0F), 0.5F);
        this.texture = new ResourceLocation(Tags.MOD_ID, "textures/entity/" + texture + ".png");
        this.addLayer(new LayerBipedArmor(this) {
            @Override
            protected void initArmor() {
                this.modelLeggings = new ModelBiped(0.5F);
                this.modelArmor = new ModelBiped(1.02F);
            }
        });
    }

    @Override
    protected void preRenderCallback(T piglin, float partialTicks) {
        GlStateManager.scale(1.0019531F, 1.0F, 1.0019531F);
    }

    /** 1.16's shaking: a mob about to convert jitters side to side. */
    @Override
    protected void applyRotations(T piglin, float ageInTicks, float rotationYaw, float partialTicks) {
        if (isShaking(piglin)) rotationYaw += (float) (Math.cos(piglin.ticksExisted * 3.25D) * Math.PI * 0.4D);
        super.applyRotations(piglin, ageInTicks, rotationYaw, partialTicks);
    }

    static boolean isShaking(EntityAbstractPiglin piglin) {
        return piglin.isConverting();
    }

    @Override
    protected ResourceLocation getEntityTexture(T piglin) {
        return this.texture;
    }
}
