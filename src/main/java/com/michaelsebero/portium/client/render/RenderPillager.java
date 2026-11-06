package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelIllager116;
import com.michaelsebero.portium.entity.monster.EntityPillager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;

/** 1.14's pillager renderer: 1.16's illager model, held items, and whatever is worn on the head (a leader's banner). */
public class RenderPillager extends RenderLiving<EntityPillager> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/illager/pillager.png");

    public RenderPillager(RenderManager manager) {
        super(manager, new ModelIllager116(0.0F, 0.0F, 64, 64, entity -> ((EntityPillager) entity).getArmPose116()), 0.5F);
        ModelIllager116 model = (ModelIllager116) this.mainModel;
        this.addLayer(new LayerCustomHead(model.head));
        this.addLayer(new LayerHeldItem(this) {
            @Override
            protected void translateToHand(EnumHandSide side) {
                model.getArm(side).postRender(0.0625F);
            }
        });
    }

    @Override
    protected void preRenderCallback(EntityPillager pillager, float partialTicks) {
        GlStateManager.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityPillager pillager) {
        return TEXTURE;
    }
}
