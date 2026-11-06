package com.michaelsebero.portium.client.model;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelZombieVillager;

/** 1.14's {@code ZombieVillagerModel}: 1.12's zombie villager with the villager hat (8x10x8) and its brim in place of the biped's hat, and arms from 1.14's skin layout. */
public class ModelZombieVillager116 extends ModelZombieVillager implements IVillagerHatModel {

    public ModelZombieVillager116(float scale) {
        super(scale, 0.0F, false);
        this.bipedHeadwear = new ModelRenderer(this).setTextureSize(64, 64);
        this.bipedHeadwear.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.bipedHeadwear.setTextureOffset(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8, scale + 0.5F);
        ModelRenderer brim = new ModelRenderer(this).setTextureSize(64, 64);
        brim.setRotationPoint(0.0F, 0.0F, 0.0F);
        brim.setTextureOffset(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16, 16, 1, scale);
        brim.rotateAngleX = -((float) Math.PI / 2F);
        this.bipedHeadwear.addChild(brim);
        // 1.14's skins have the arms at (44, 22); 1.12's model reads them from (44, 38), blank in those skins.
        this.bipedRightArm = new ModelRenderer(this, 44, 22).setTextureSize(64, 64);
        this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, scale);
        this.bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
        this.bipedLeftArm = new ModelRenderer(this, 44, 22).setTextureSize(64, 64);
        this.bipedLeftArm.mirror = true;
        this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, scale);
        this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
    }

    @Override
    public void setHatVisible(boolean visible) {
        this.bipedHeadwear.showModel = visible;
    }
}
