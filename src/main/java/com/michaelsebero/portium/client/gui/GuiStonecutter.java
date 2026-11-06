package com.michaelsebero.portium.client.gui;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.inventory.ContainerStonecutter;
import com.michaelsebero.portium.recipe.StonecuttingRecipes;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/** The stonecutter's screen (1.14): its recipes as a 4-wide grid of buttons, three rows at a time with a scroll bar. */
public class GuiStonecutter extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/gui/container/stonecutter.png");

    private final ContainerStonecutter container;
    private final InventoryPlayer playerInventory;
    private float sliderProgress;
    private boolean clickedOnScroll;
    private int recipeIndexOffset;
    private boolean hasItemsInInputSlot;

    public GuiStonecutter(ContainerStonecutter container, InventoryPlayer playerInventory) {
        super(container);
        this.container = container;
        this.playerInventory = playerInventory;
        container.setUpdateListener(this::onInventoryUpdate);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
        this.renderRecipeTooltip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(I18n.format("container.stonecutter"), 8, 4, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 94, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int left = this.guiLeft;
        int top = this.guiTop;
        this.drawTexturedModalRect(left, top, 0, 0, this.xSize, this.ySize);
        int slider = (int) (41.0F * this.sliderProgress);
        this.drawTexturedModalRect(left + 119, top + 15 + slider, 176 + (this.canScroll() ? 0 : 12), 0, 12, 15);
        int gridLeft = this.guiLeft + 52;
        int gridTop = this.guiTop + 14;
        int last = this.recipeIndexOffset + 12;
        this.drawRecipeButtons(mouseX, mouseY, gridLeft, gridTop, last);
        this.drawRecipeItems(gridLeft, gridTop, last);
    }

    private void drawRecipeButtons(int mouseX, int mouseY, int gridLeft, int gridTop, int last) {
        List<StonecuttingRecipes.Recipe> recipes = this.container.getRecipeList();
        for (int i = this.recipeIndexOffset; i < last && i < recipes.size(); i++) {
            int index = i - this.recipeIndexOffset;
            int x = gridLeft + index % 4 * 16;
            int y = gridTop + index / 4 * 18 + 2;
            int v = this.ySize;
            if (i == this.container.getSelectedRecipe()) v += 18;
            else if (mouseX >= x && mouseY >= y && mouseX < x + 16 && mouseY < y + 18) v += 36;
            this.drawTexturedModalRect(x, y - 1, 0, v, 16, 18);
        }
    }

    private void drawRecipeItems(int gridLeft, int gridTop, int last) {
        List<StonecuttingRecipes.Recipe> recipes = this.container.getRecipeList();
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = this.recipeIndexOffset; i < last && i < recipes.size(); i++) {
            int index = i - this.recipeIndexOffset;
            this.itemRender.renderItemAndEffectIntoGUI(recipes.get(i).getResult(), gridLeft + index % 4 * 16, gridTop + index / 4 * 18 + 2);
        }
        RenderHelper.disableStandardItemLighting();
    }

    private void renderRecipeTooltip(int mouseX, int mouseY) {
        if (!this.hasItemsInInputSlot) return;
        int gridLeft = this.guiLeft + 52;
        int gridTop = this.guiTop + 14;
        List<StonecuttingRecipes.Recipe> recipes = this.container.getRecipeList();
        for (int i = this.recipeIndexOffset; i < this.recipeIndexOffset + 12 && i < recipes.size(); i++) {
            int index = i - this.recipeIndexOffset;
            int x = gridLeft + index % 4 * 16;
            int y = gridTop + index / 4 * 18 + 2;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 18) this.renderToolTip(recipes.get(i).getResult(), mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        this.clickedOnScroll = false;
        if (this.hasItemsInInputSlot) {
            int gridLeft = this.guiLeft + 52;
            int gridTop = this.guiTop + 14;
            for (int i = this.recipeIndexOffset; i < this.recipeIndexOffset + 12; i++) {
                int index = i - this.recipeIndexOffset;
                double dx = mouseX - (double) (gridLeft + index % 4 * 16);
                double dy = mouseY - (double) (gridTop + index / 4 * 18);
                if (dx >= 0.0D && dy >= 0.0D && dx < 16.0D && dy < 18.0D && this.container.enchantItem(this.mc.player, i)) {
                    this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(PortiumSounds.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                    this.mc.playerController.sendEnchantPacket(this.container.windowId, i);
                    return;
                }
            }
            int scrollLeft = this.guiLeft + 119;
            int scrollTop = this.guiTop + 9;
            if (mouseX >= scrollLeft && mouseX < scrollLeft + 12 && mouseY >= scrollTop && mouseY < scrollTop + 54) this.clickedOnScroll = true;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (this.clickedOnScroll && this.canScroll()) {
            int top = this.guiTop + 14;
            int bottom = top + 54;
            this.sliderProgress = MathHelper.clamp(((float) mouseY - (float) top - 7.5F) / ((float) (bottom - top) - 15.0F), 0.0F, 1.0F);
            this.recipeIndexOffset = (int) ((double) (this.sliderProgress * (float) this.hiddenRows()) + 0.5D) * 4;
            return;
        }
        super.mouseClickMove(mouseX, mouseY, button, timeSinceLastClick);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && this.canScroll()) {
            int hidden = this.hiddenRows();
            this.sliderProgress = MathHelper.clamp(this.sliderProgress - (float) Integer.signum(wheel) / (float) hidden, 0.0F, 1.0F);
            this.recipeIndexOffset = (int) ((double) (this.sliderProgress * (float) hidden) + 0.5D) * 4;
        }
    }

    private boolean canScroll() {
        return this.hasItemsInInputSlot && this.container.getRecipeList().size() > 12;
    }

    private int hiddenRows() {
        return (this.container.getRecipeList().size() + 4 - 1) / 4 - 3;
    }

    private void onInventoryUpdate() {
        this.hasItemsInInputSlot = this.container.hasItemsInInputSlot();
        if (!this.hasItemsInInputSlot) {
            this.sliderProgress = 0.0F;
            this.recipeIndexOffset = 0;
        }
    }
}
