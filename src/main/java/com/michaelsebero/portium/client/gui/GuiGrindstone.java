package com.michaelsebero.portium.client.gui;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.inventory.ContainerGrindstone;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.gui.inventory.GuiContainer;

/** The grindstone's screen (1.14), with the crossed-out arrow when the items can't be ground. */
public class GuiGrindstone extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/gui/container/grindstone.png");

    private final InventoryPlayer playerInventory;

    public GuiGrindstone(ContainerGrindstone container, InventoryPlayer playerInventory) {
        super(container);
        this.playerInventory = playerInventory;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(I18n.format("container.grindstone_title"), 8, 6, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(left, top, 0, 0, this.xSize, this.ySize);
        if ((this.inventorySlots.getSlot(0).getHasStack() || this.inventorySlots.getSlot(1).getHasStack()) && !this.inventorySlots.getSlot(2).getHasStack()) {
            this.drawTexturedModalRect(left + 92, top + 31, this.xSize, 0, 28, 21);
        }
    }
}
