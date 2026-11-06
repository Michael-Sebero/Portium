package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.IBeeStingers;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/** 1.15's {@code BeeStingerLayer}: stingers stuck in a player, placed on its model as arrows are. */
public class LayerBeeStingers implements LayerRenderer<EntityLivingBase> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/bee/bee_stinger.png");

    private final RenderLivingBase<?> renderer;

    public LayerBeeStingers(RenderLivingBase<?> renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                              float headPitch, float scale) {
        int count = entity instanceof IBeeStingers ? ((IBeeStingers) entity).portium$getBeeStingers() : 0;
        if (count <= 0) return;
        Random random = new Random(entity.getEntityId());
        this.renderer.bindTexture(TEXTURE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableCull();
        for (int i = 0; i < count; i++) {
            GlStateManager.pushMatrix();
            ModelRenderer part = this.renderer.getMainModel().getRandomModelBox(random);
            ModelBox box = part.cubeList.get(random.nextInt(part.cubeList.size()));
            part.postRender(0.0625F);
            float fx = random.nextFloat();
            float fy = random.nextFloat();
            float fz = random.nextFloat();
            GlStateManager.translate((box.posX1 + (box.posX2 - box.posX1) * fx) / 16.0F, (box.posY1 + (box.posY2 - box.posY1) * fy) / 16.0F,
                    (box.posZ1 + (box.posZ2 - box.posZ1) * fz) / 16.0F);
            this.stinger(-(fx * 2.0F - 1.0F), -(fy * 2.0F - 1.0F), -(fz * 2.0F - 1.0F));
            GlStateManager.popMatrix();
        }
        GlStateManager.enableCull();
        RenderHelper.enableStandardItemLighting();
    }

    /** A stinger pointing along a direction: four crossed quads, as 1.15 draws it. */
    private void stinger(float x, float y, float z) {
        float horizontal = MathHelper.sqrt(x * x + z * z);
        float yaw = (float) (Math.atan2(x, z) * (180D / Math.PI));
        float pitch = (float) (Math.atan2(y, horizontal) * (180D / Math.PI));
        GlStateManager.rotate(yaw - 90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(pitch, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(45.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.03125F, 0.03125F, 0.03125F);
        GlStateManager.translate(2.5F, 0.0F, 0.0F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (int i = 0; i < 4; i++) {
            GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(-4.5D, -1.0D, 0.0D).tex(0.0D, 0.0D).endVertex();
            buffer.pos(4.5D, -1.0D, 0.0D).tex(0.125D, 0.0D).endVertex();
            buffer.pos(4.5D, 1.0D, 0.0D).tex(0.125D, 0.0625D).endVertex();
            buffer.pos(-4.5D, 1.0D, 0.0D).tex(0.0D, 0.0625D).endVertex();
            tessellator.draw();
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
