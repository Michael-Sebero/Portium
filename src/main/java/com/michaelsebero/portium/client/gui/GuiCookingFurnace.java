package com.michaelsebero.portium.client.gui;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.inventory.ContainerCookingFurnace;
import com.michaelsebero.portium.recipe.CookingRecipes;
import com.michaelsebero.portium.tile.TileEntityCookingFurnace;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;

/** The smoker's and blast furnace's screen (1.14), drawn like 1.12's furnace with 1.14's own textures. */
public class GuiCookingFurnace extends GuiContainer {

    private static final ResourceLocation SMOKER = new ResourceLocation(Tags.MOD_ID, "textures/gui/container/smoker.png");
    private static final ResourceLocation BLAST_FURNACE = new ResourceLocation(Tags.MOD_ID, "textures/gui/container/blast_furnace.png");

    private final InventoryPlayer playerInventory;
    private final IInventory furnace;
    private final ResourceLocation texture;

    public GuiCookingFurnace(InventoryPlayer playerInventory, TileEntityCookingFurnace furnace) {
        super(new ContainerCookingFurnace(playerInventory, furnace));
        this.playerInventory = playerInventory;
        this.furnace = furnace;
        this.texture = furnace.getType() == CookingRecipes.Type.BLASTING ? BLAST_FURNACE : SMOKER;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = this.furnace.getDisplayName().getUnformattedText();
        this.fontRenderer.drawString(title, this.xSize / 2 - this.fontRenderer.getStringWidth(title) / 2, 6, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(this.texture);
        int left = (this.width - this.xSize) / 2;
        int top = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(left, top, 0, 0, this.xSize, this.ySize);
        if (this.furnace.getField(0) > 0) {
            int burn = this.burnLeftScaled(13);
            this.drawTexturedModalRect(left + 56, top + 36 + 12 - burn, 176, 12 - burn, 14, burn + 1);
        }
        int cook = this.cookProgressScaled(24);
        this.drawTexturedModalRect(left + 79, top + 34, 176, 14, cook + 1, 16);
    }

    private int cookProgressScaled(int pixels) {
        int cookTime = this.furnace.getField(2);
        int total = this.furnace.getField(3);
        return total != 0 && cookTime != 0 ? cookTime * pixels / total : 0;
    }

    private int burnLeftScaled(int pixels) {
        int itemBurnTime = this.furnace.getField(1);
        if (itemBurnTime == 0) itemBurnTime = 100;
        return this.furnace.getField(0) * pixels / itemBurnTime;
    }
}
