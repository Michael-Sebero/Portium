package com.michaelsebero.portium.client.gui;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.inventory.ContainerCartography;
import com.michaelsebero.portium.item.LockedMaps;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.MapData;

import javax.annotation.Nullable;

/** The cartography table's screen (1.14): the map as it will come out, zoomed out, doubled or locked. */
public class GuiCartography extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/gui/container/cartography_table.png");

    private final InventoryPlayer playerInventory;

    public GuiCartography(ContainerCartography container, InventoryPlayer playerInventory) {
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
        this.fontRenderer.drawString(I18n.format("container.cartography_table"), 8, 4, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 94, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int left = this.guiLeft;
        int top = this.guiTop;
        this.drawTexturedModalRect(left, top, 0, 0, this.xSize, this.ySize);
        Item second = this.inventorySlots.getSlot(1).getStack().getItem();
        boolean copy = second == Items.MAP;
        boolean zoom = second == Items.PAPER;
        boolean lock = second == Item.getItemFromBlock(Blocks.GLASS_PANE);
        ItemStack map = this.inventorySlots.getSlot(0).getStack();
        boolean refused = false;
        MapData data = null;
        if (map.getItem() == Items.FILLED_MAP) {
            data = Items.FILLED_MAP.getMapData(map, this.mc.world);
            if (data != null) {
                if (LockedMaps.isLocked(data)) {
                    refused = true;
                    if (zoom || lock) this.drawTexturedModalRect(left + 35, top + 31, this.xSize + 50, 132, 28, 21);
                }
                if (zoom && data.scale >= 4) {
                    refused = true;
                    this.drawTexturedModalRect(left + 35, top + 31, this.xSize + 50, 132, 28, 21);
                }
            }
        }
        this.drawPreview(data, copy, zoom, lock, refused);
    }

    private void drawPreview(@Nullable MapData data, boolean copy, boolean zoom, boolean lock, boolean refused) {
        int left = this.guiLeft;
        int top = this.guiTop;
        if (zoom && !refused) {
            this.drawTexturedModalRect(left + 67, top + 13, this.xSize, 66, 66, 66);
            this.drawMap(data, left + 85, top + 31, 0.226F);
        } else if (copy) {
            this.drawTexturedModalRect(left + 67 + 16, top + 13, this.xSize, 132, 50, 66);
            this.drawMap(data, left + 86, top + 16, 0.34F);
            this.mc.getTextureManager().bindTexture(TEXTURE);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.0F, 1.0F);
            this.drawTexturedModalRect(left + 67, top + 13 + 16, this.xSize, 132, 50, 66);
            this.drawMap(data, left + 70, top + 32, 0.34F);
            GlStateManager.popMatrix();
        } else if (lock) {
            this.drawTexturedModalRect(left + 67, top + 13, this.xSize, 0, 66, 66);
            this.drawMap(data, left + 71, top + 17, 0.45F);
            this.mc.getTextureManager().bindTexture(TEXTURE);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.0F, 1.0F);
            this.drawTexturedModalRect(left + 66, top + 12, 0, this.ySize, 66, 66);
            GlStateManager.popMatrix();
        } else {
            this.drawTexturedModalRect(left + 67, top + 13, this.xSize, 0, 66, 66);
            this.drawMap(data, left + 71, top + 17, 0.45F);
        }
    }

    private void drawMap(@Nullable MapData data, int x, int y, float scale) {
        if (data == null) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, 1.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        this.mc.entityRenderer.getMapItemRenderer().renderMap(data, true);
        GlStateManager.popMatrix();
        this.mc.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
