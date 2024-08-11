package com.michaelsebero.portium.potion;

import com.michaelsebero.portium.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A mob effect with its own 18x18 icon texture ({@code textures/mob_effect/<name>.png}) instead of a slot in
 * vanilla's shared icon sheet.
 */
public class PotionPortium extends Potion {

    private final ResourceLocation icon;

    public PotionPortium(String name, boolean harmful, int color) {
        super(harmful, color);
        this.icon = new ResourceLocation(Tags.MOD_ID, "textures/mob_effect/" + name + ".png");
        this.setPotionName("effect." + Tags.MOD_ID + "." + name);
        if (!harmful) this.setBeneficial();
    }

    @Override
    public boolean hasStatusIcon() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderInventoryEffect(PotionEffect effect, Gui gui, int x, int y, float z) {
        this.drawIcon(x + 6, y + 7, 1.0F);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) {
        this.drawIcon(x + 3, y + 3, alpha);
    }

    @SideOnly(Side.CLIENT)
    private void drawIcon(int x, int y, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(this.icon);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 18, 18, 18, 18);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
