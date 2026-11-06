package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.item.IMapDecorationName;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.storage.MapDecoration;
import org.lwjgl.opengl.GL11;

/**
 * 1.13's map markers: drawn from 1.13's icon sheet (1.16.5's, 16 icons to a row, which has the banner markers and the
 * red X), and a marker's name under it on a dark plate, as 1.13's map renderer does.
 */
public final class MapIcons116 {

    private static final ResourceLocation ICONS = new ResourceLocation(Tags.MOD_ID, "textures/map/map_icons.png");

    /** Draws the marker (the {@code index}th drawn on this map); always true, as 1.12's sheet can't draw 1.13's icons. */
    public static boolean render(MapDecoration decoration, int index) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.getTextureManager().bindTexture(ICONS);
        GlStateManager.pushMatrix();
        GlStateManager.translate(decoration.getX() / 2.0F + 64.0F, decoration.getY() / 2.0F + 64.0F, -0.02F);
        GlStateManager.rotate(decoration.getRotation() * 360 / 16.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(4.0F, 4.0F, 3.0F);
        GlStateManager.translate(-0.125F, 0.125F, 0.0F);
        int icon = decoration.getImage() & 255;
        float u1 = (icon % 16) / 16.0F;
        float v1 = (icon / 16) / 16.0F;
        float u2 = (icon % 16 + 1) / 16.0F;
        float v2 = (icon / 16 + 1) / 16.0F;
        float depth = index * -0.001F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(-1.0D, 1.0D, depth).tex(u1, v1).endVertex();
        buffer.pos(1.0D, 1.0D, depth).tex(u2, v1).endVertex();
        buffer.pos(1.0D, -1.0D, depth).tex(u2, v2).endVertex();
        buffer.pos(-1.0D, -1.0D, depth).tex(u1, v2).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        ITextComponent name = ((IMapDecorationName) decoration).portium$getName();
        if (name != null) {
            FontRenderer font = minecraft.fontRenderer;
            String text = name.getFormattedText();
            float width = font.getStringWidth(text);
            float scale = MathHelper.clamp(25.0F / width, 0.0F, 6.0F / 9.0F);
            GlStateManager.pushMatrix();
            GlStateManager.translate(decoration.getX() / 2.0F + 64.0F - width * scale / 2.0F, decoration.getY() / 2.0F + 64.0F + 4.0F, -0.025F);
            GlStateManager.scale(scale, scale, 1.0F);
            Gui.drawRect(-1, -1, (int) width, font.FONT_HEIGHT - 1, Integer.MIN_VALUE);
            GlStateManager.translate(0.0F, 0.0F, -0.1F);
            font.drawString(text, 0, 0, -1);
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
        return true;
    }

    private MapIcons116() {}
}
