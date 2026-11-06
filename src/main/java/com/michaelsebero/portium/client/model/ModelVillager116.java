package com.michaelsebero.portium.client.model;

import com.michaelsebero.portium.entity.merchant.EntityAbstractVillager116;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** 1.14's {@code VillagerModel}: 1.12's villager with a hat layer and brim, a clothing layer, and head shaking. */
public class ModelVillager116 extends ModelBase implements IVillagerHatModel {

    public final ModelRenderer villagerHead;
    public final ModelRenderer hat;
    public final ModelRenderer hatBrim;
    public final ModelRenderer villagerBody;
    public final ModelRenderer clothing;
    public final ModelRenderer villagerArms;
    public final ModelRenderer rightVillagerLeg;
    public final ModelRenderer leftVillagerLeg;
    public final ModelRenderer villagerNose;

    public ModelVillager116(float scale) {
        this(scale, 64, 64);
    }

    public ModelVillager116(float scale, int textureWidth, int textureHeight) {
        this.villagerHead = new ModelRenderer(this).setTextureSize(textureWidth, textureHeight);
        this.villagerHead.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.villagerHead.setTextureOffset(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8, scale);
        this.hat = new ModelRenderer(this).setTextureSize(textureWidth, textureHeight);
        this.hat.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.hat.setTextureOffset(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8, scale + 0.5F);
        this.villagerHead.addChild(this.hat);
        this.hatBrim = new ModelRenderer(this).setTextureSize(textureWidth, textureHeight);
        this.hatBrim.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.hatBrim.setTextureOffset(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16, 16, 1, scale);
        this.hatBrim.rotateAngleX = -((float) Math.PI / 2F);
        this.hat.addChild(this.hatBrim);
        this.villagerNose = new ModelRenderer(this).setTextureSize(textureWidth, textureHeight);
        this.villagerNose.setRotationPoint(0.0F, -2.0F, 0.0F);
        this.villagerNose.setTextureOffset(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2, 4, 2, scale);
        this.villagerHead.addChild(this.villagerNose);
        this.villagerBody = new ModelRenderer(this).setTextureSize(textureWidth, textureHeight);
        this.villagerBody.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.villagerBody.setTextureOffset(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8, 12, 6, scale);
        this.clothing = new ModelRenderer(this).setTextureSize(textureWidth, textureHeight);
        this.clothing.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.clothing.setTextureOffset(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8, 18, 6, scale + 0.5F);
        this.villagerBody.addChild(this.clothing);
        this.villagerArms = new ModelRenderer(this).setTextureSize(textureWidth, textureHeight);
        this.villagerArms.setRotationPoint(0.0F, 2.0F, 0.0F);
        this.villagerArms.setTextureOffset(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4, 8, 4, scale);
        this.villagerArms.mirror = true;
        this.villagerArms.setTextureOffset(44, 22).addBox(4.0F, -2.0F, -2.0F, 4, 8, 4, scale);
        this.villagerArms.mirror = false;
        this.villagerArms.setTextureOffset(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8, 4, 4, scale);
        this.rightVillagerLeg = new ModelRenderer(this, 0, 22).setTextureSize(textureWidth, textureHeight);
        this.rightVillagerLeg.setRotationPoint(-2.0F, 12.0F, 0.0F);
        this.rightVillagerLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, scale);
        this.leftVillagerLeg = new ModelRenderer(this, 0, 22).setTextureSize(textureWidth, textureHeight);
        this.leftVillagerLeg.mirror = true;
        this.leftVillagerLeg.setRotationPoint(2.0F, 12.0F, 0.0F);
        this.leftVillagerLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, scale);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.villagerHead.render(scale);
        this.villagerBody.render(scale);
        this.rightVillagerLeg.render(scale);
        this.leftVillagerLeg.render(scale);
        this.villagerArms.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        boolean shaking = entity instanceof EntityAbstractVillager116 && ((EntityAbstractVillager116) entity).getShakeHeadTicks() > 0;
        this.villagerHead.rotateAngleY = netHeadYaw * 0.017453292F;
        this.villagerHead.rotateAngleX = headPitch * 0.017453292F;
        if (shaking) {
            this.villagerHead.rotateAngleZ = 0.3F * MathHelper.sin(0.45F * ageInTicks);
            this.villagerHead.rotateAngleX = 0.4F;
        } else {
            this.villagerHead.rotateAngleZ = 0.0F;
        }
        this.villagerArms.rotationPointY = 3.0F;
        this.villagerArms.rotationPointZ = -1.0F;
        this.villagerArms.rotateAngleX = -0.75F;
        this.rightVillagerLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount * 0.5F;
        this.leftVillagerLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount * 0.5F;
        this.rightVillagerLeg.rotateAngleY = 0.0F;
        this.leftVillagerLeg.rotateAngleY = 0.0F;
    }

    @Override
    public void setHatVisible(boolean visible) {
        this.hat.showModel = visible;
    }
}
