package com.michaelsebero.portium.client.gui;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.inventory.ContainerSmithing;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The smithing table's screen (1.16): "Upgrade Gear", with a red cross over the arrow while the inputs make nothing. */
@SideOnly(Side.CLIENT)
public class GuiSmithing extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/gui/container/smithing.png");

    public GuiSmithing(ContainerSmithing container) {
        super(container);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(I18n.format("container.portium.upgrade"), 60, 20, 0x404040);
        this.fontRenderer.drawString(this.mc.player.inventory.getDisplayName().getUnformattedText(), 8, this.ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        boolean inputs = this.inventorySlots.getSlot(0).getHasStack() || this.inventorySlots.getSlot(1).getHasStack();
        if (inputs && !this.inventorySlots.getSlot(2).getHasStack()) drawTexturedModalRect(this.guiLeft + 99, this.guiTop + 45, this.xSize, 0, 28, 21);
    }
}
