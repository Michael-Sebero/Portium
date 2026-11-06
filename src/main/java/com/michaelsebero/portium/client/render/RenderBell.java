package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.tile.TileEntityBell;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * The bell itself, under its frame (1.14's BellTileEntityRenderer): a cup and a rim, tipping away from the side it
 * was struck on, less with each swing.
 */
public class RenderBell extends TileEntitySpecialRenderer<TileEntityBell> {

    public static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/bell/bell_body.png");

    private final ModelRenderer body;

    public RenderBell() {
        ModelBase model = new ModelBase() {};
        model.textureWidth = 32;
        model.textureHeight = 32;
        this.body = new ModelRenderer(model, 0, 0);
        this.body.addBox(-3.0F, -6.0F, -3.0F, 6, 7, 6);
        this.body.setRotationPoint(8.0F, 12.0F, 8.0F);
        ModelRenderer rim = new ModelRenderer(model, 0, 13);
        rim.addBox(4.0F, 4.0F, 4.0F, 8, 2, 8);
        rim.setRotationPoint(-8.0F, -12.0F, -8.0F);
        this.body.addChild(rim);
    }

    @Override
    public void render(TileEntityBell bell, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        float ticks = (float) bell.ringingTicks + partialTicks;
        float tiltX = 0.0F;
        float tiltZ = 0.0F;
        if (bell.isRinging) {
            float swing = MathHelper.sin(ticks / (float) Math.PI) / (4.0F + ticks / 3.0F);
            if (bell.ringDirection == EnumFacing.NORTH) tiltX = -swing;
            else if (bell.ringDirection == EnumFacing.SOUTH) tiltX = swing;
            else if (bell.ringDirection == EnumFacing.EAST) tiltZ = -swing;
            else if (bell.ringDirection == EnumFacing.WEST) tiltZ = swing;
        }
        this.body.rotateAngleX = tiltX;
        this.body.rotateAngleZ = tiltZ;
        if (destroyStage >= 0) {
            this.bindTexture(DESTROY_STAGES[destroyStage]);
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.pushMatrix();
            GlStateManager.scale(2.0F, 2.0F, 1.0F);
            GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        } else {
            this.bindTexture(TEXTURE);
        }
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.translate(x, y, z);
        this.body.render(0.0625F);
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        if (destroyStage >= 0) {
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        }
    }
}
