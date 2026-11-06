package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.client.model.ModelIllager116;
import com.michaelsebero.portium.entity.monster.IllagerArmPose116;
import com.michaelsebero.portium.entity.raid.IRaider;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;

/**
 * 1.14's vindicator and evoker renderers: 1.16's illager model with its celebrating pose, what's worn on the head (a
 * raid captain's banner), and the held item only while attacking (vindicator) or casting (evoker).
 */
public class RenderIllager116 extends RenderLiving<AbstractIllager> {

    private final ResourceLocation texture;

    private RenderIllager116(RenderManager manager, String texture, AbstractIllager.IllagerArmPose showsItem) {
        super(manager, new ModelIllager116(0.0F, 0.0F, 64, 64, RenderIllager116::pose), 0.5F);
        this.texture = new ResourceLocation("textures/entity/illager/" + texture + ".png");
        ModelIllager116 model = (ModelIllager116) this.mainModel;
        this.addLayer(new LayerCustomHead(model.head));
        this.addLayer(new LayerHeldItem(this) {
            @Override
            public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                      float netHeadYaw, float headPitch, float scale) {
                if (((AbstractIllager) entity).getArmPose() == showsItem) {
                    super.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
                }
            }

            @Override
            protected void translateToHand(EnumHandSide side) {
                model.getArm(side).postRender(0.0625F);
            }
        });
    }

    public static RenderIllager116 vindicator(RenderManager manager) {
        return new RenderIllager116(manager, "vindicator", AbstractIllager.IllagerArmPose.ATTACKING);
    }

    public static RenderIllager116 evoker(RenderManager manager) {
        return new RenderIllager116(manager, "evoker", AbstractIllager.IllagerArmPose.SPELLCASTING);
    }

    /** 1.12's pose as 1.16 has it, and 1.14's celebration when the illager would otherwise stand with arms crossed. */
    private static IllagerArmPose116 pose(EntityLivingBase entity) {
        AbstractIllager illager = (AbstractIllager) entity;
        switch (illager.getArmPose()) {
            case ATTACKING:
                return IllagerArmPose116.ATTACKING;
            case SPELLCASTING:
                return IllagerArmPose116.SPELLCASTING;
            case BOW_AND_ARROW:
                return IllagerArmPose116.BOW_AND_ARROW;
            default:
                return illager instanceof IRaider && ((IRaider) illager).portium$isCelebrating() ? IllagerArmPose116.CELEBRATING : IllagerArmPose116.CROSSED;
        }
    }

    @Override
    protected void preRenderCallback(AbstractIllager illager, float partialTicks) {
        GlStateManager.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    protected ResourceLocation getEntityTexture(AbstractIllager illager) {
        return this.texture;
    }
}
