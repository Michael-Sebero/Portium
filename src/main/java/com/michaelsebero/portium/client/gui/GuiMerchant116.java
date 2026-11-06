package com.michaelsebero.portium.client.gui;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.merchant.ContainerMerchant116;
import com.michaelsebero.portium.merchant.MerchantLevels;
import com.michaelsebero.portium.merchant.MerchantOffer;
import com.michaelsebero.portium.merchant.MerchantOffers;
import com.michaelsebero.portium.network.PortiumNetwork;
import com.michaelsebero.portium.network.SelectTradeMessage;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.Collections;

/**
 * 1.14's {@code MerchantScreen}: offers listed on the left (seven at a time, scrolled by wheel or bar), picking one
 * fills the payment from the inventory, prices raised or lowered show the old count struck through, and villagers'
 * level and experience show above the result.
 */
public class GuiMerchant116 extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/gui/container/villager2.png");
    private static final int VISIBLE_OFFERS = 7;

    private final ContainerMerchant116 container;
    private final InventoryPlayer playerInventory;
    private final String title;
    private int selectedMerchantRecipe;
    private int scrollOff;
    private boolean isDragging;

    public GuiMerchant116(ContainerMerchant116 container, InventoryPlayer playerInventory, String title) {
        super(container);
        this.container = container;
        this.playerInventory = playerInventory;
        this.title = title;
        this.xSize = 276;
    }

    private void postButtonClick() {
        this.container.setCurrentRecipeIndex(this.selectedMerchantRecipe);
        this.container.fillPaymentFor(this.selectedMerchantRecipe);
        PortiumNetwork.CHANNEL.sendToServer(new SelectTradeMessage(this.selectedMerchantRecipe));
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        int y = this.guiTop + 16 + 2;
        for (int i = 0; i < VISIBLE_OFFERS; ++i) {
            GuiButton button = new GuiButton(i, this.guiLeft + 5, y, 89, 20, "");
            button.visible = false;
            this.buttonList.add(button);
            y += 20;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id >= 0 && button.id < VISIBLE_OFFERS) {
            this.selectedMerchantRecipe = button.id + this.scrollOff;
            this.postButtonClick();
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int level = this.container.getMerchantLevel();
        String title = this.title;
        if (level > 0 && level <= 5 && this.container.showProgressBar()) title = title + " - " + I18n.format("merchant.level." + level);
        this.fontRenderer.drawString(title, 49 + this.xSize / 2 - this.fontRenderer.getStringWidth(title) / 2, 6, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 107, this.ySize - 94, 4210752);
        String trades = I18n.format("merchant.trades");
        this.fontRenderer.drawString(trades, 5 - this.fontRenderer.getStringWidth(trades) / 2 + 48, 6, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        blit(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        MerchantOffers offers = this.container.getOffers();
        if (offers.isEmpty()) return;
        int selected = this.selectedMerchantRecipe;
        if (selected < 0 || selected >= offers.size()) return;
        if (offers.get(selected).hasNoUsesLeft()) blit(this.guiLeft + 83 + 99, this.guiTop + 35, 311, 0, 28, 21);
    }

    /** Draws from the 512x256 trade sheet. */
    private static void blit(int x, int y, int u, int v, int width, int height) {
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, 512.0F, 256.0F);
    }

    private void drawLevelBar(int left, int top) {
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int level = this.container.getMerchantLevel();
        int xp = this.container.getXp();
        if (level >= 5) return;
        blit(left + 136, top + 16, 0, 186, 102, 5);
        int previous = MerchantLevels.getExperiencePrevious(level);
        if (xp < previous || !MerchantLevels.canLevelUp(level)) return;
        float scale = 100.0F / (float) (MerchantLevels.getExperienceNext(level) - previous);
        int filled = Math.min(MathHelper.floor(scale * (float) (xp - previous)), 100);
        blit(left + 136, top + 16, 0, 191, filled + 1, 5);
        int pending = this.container.getPendingExp();
        if (pending > 0) {
            int extra = Math.min(MathHelper.floor((float) pending * scale), 100 - filled);
            blit(left + 136 + filled + 1, top + 16 + 1, 2, 182, extra, 3);
        }
    }

    private void drawScroller(int left, int top, MerchantOffers offers) {
        int steps = offers.size() + 1 - VISIBLE_OFFERS;
        if (steps > 1) {
            int gap = 139 - (27 + (steps - 1) * 139 / steps);
            int step = 1 + gap / steps + 139 / steps;
            int offset = Math.min(113, this.scrollOff * step);
            if (this.scrollOff == steps - 1) offset = 113;
            blit(left + 94, top + 18 + offset, 0, 199, 6, 27);
        } else {
            blit(left + 94, top + 18, 6, 199, 6, 27);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        MerchantOffers offers = this.container.getOffers();
        for (GuiButton button : this.buttonList) button.visible = button.id < offers.size();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!offers.isEmpty()) {
            int left = this.guiLeft;
            int top = this.guiTop;
            int y = top + 16 + 1;
            int x = left + 5 + 5;
            GlStateManager.pushMatrix();
            GlStateManager.enableRescaleNormal();
            this.mc.getTextureManager().bindTexture(TEXTURE);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawScroller(left, top, offers);
            RenderHelper.enableGUIStandardItemLighting();
            int index = 0;
            for (MerchantOffer offer : offers) {
                if (this.canScroll(offers.size()) && (index < this.scrollOff || index >= VISIBLE_OFFERS + this.scrollOff)) {
                    ++index;
                    continue;
                }
                ItemStack base = offer.getBuyingStackFirst();
                ItemStack price = offer.getDiscountedBuyingStackFirst();
                ItemStack second = offer.getBuyingStackSecond();
                ItemStack selling = offer.getSellingStack();
                this.itemRender.zLevel = 100.0F;
                int rowY = y + 2;
                this.drawPrice(price, base, x, rowY);
                if (!second.isEmpty()) {
                    this.itemRender.renderItemAndEffectIntoGUI(second, left + 5 + 35, rowY);
                    this.itemRender.renderItemOverlays(this.fontRenderer, second, left + 5 + 35, rowY);
                }
                this.drawArrow(offer, left, rowY);
                this.itemRender.renderItemAndEffectIntoGUI(selling, left + 5 + 68, rowY);
                this.itemRender.renderItemOverlays(this.fontRenderer, selling, left + 5 + 68, rowY);
                this.itemRender.zLevel = 0.0F;
                y += 20;
                ++index;
            }
            RenderHelper.disableStandardItemLighting();
            int selected = this.selectedMerchantRecipe;
            if (selected >= 0 && selected < offers.size()) {
                MerchantOffer offer = offers.get(selected);
                if (this.container.showProgressBar()) this.drawLevelBar(left, top);
                if (offer.hasNoUsesLeft() && this.isPointInRegion(186, 35, 22, 21, mouseX, mouseY) && this.container.canRestock()) {
                    this.drawHoveringText(Collections.singletonList(I18n.format("merchant.portium.deprecated")), mouseX, mouseY);
                }
            }
            for (GuiButton button : this.buttonList) {
                if (button.isMouseOver()) this.drawOfferTooltip(button, mouseX, mouseY);
            }
            GlStateManager.popMatrix();
            GlStateManager.enableDepth();
        }
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    private void drawArrow(MerchantOffer offer, int left, int y) {
        GlStateManager.enableBlend();
        this.mc.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.disableLighting();
        blit(left + 5 + 35 + 20, y + 3, offer.hasNoUsesLeft() ? 25 : 15, 171, 10, 9);
        RenderHelper.enableGUIStandardItemLighting();
    }

    /** The price; when demand or a special price changed it, the old count struck through beside the new. */
    private void drawPrice(ItemStack price, ItemStack base, int x, int y) {
        this.itemRender.renderItemAndEffectIntoGUI(price, x, y);
        if (base.getCount() == price.getCount()) {
            this.itemRender.renderItemOverlays(this.fontRenderer, price, x, y);
            return;
        }
        this.itemRender.renderItemOverlayIntoGUI(this.fontRenderer, base, x, y, base.getCount() == 1 ? "1" : null);
        this.itemRender.renderItemOverlayIntoGUI(this.fontRenderer, price, x + 14, y, price.getCount() == 1 ? "1" : null);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.disableLighting();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 300.0F);
        blit(x + 7, y + 12, 0, 176, 9, 2);
        GlStateManager.popMatrix();
        RenderHelper.enableGUIStandardItemLighting();
    }

    private void drawOfferTooltip(GuiButton button, int mouseX, int mouseY) {
        MerchantOffers offers = this.container.getOffers();
        int index = button.id + this.scrollOff;
        if (index >= offers.size()) return;
        MerchantOffer offer = offers.get(index);
        if (mouseX < button.x + 20) {
            this.renderToolTip(offer.getDiscountedBuyingStackFirst(), mouseX, mouseY);
        } else if (mouseX < button.x + 50 && mouseX > button.x + 30) {
            if (!offer.getBuyingStackSecond().isEmpty()) this.renderToolTip(offer.getBuyingStackSecond(), mouseX, mouseY);
        } else if (mouseX > button.x + 65) {
            this.renderToolTip(offer.getSellingStack(), mouseX, mouseY);
        }
    }

    private boolean canScroll(int size) {
        return size > VISIBLE_OFFERS;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        int size = this.container.getOffers().size();
        if (wheel != 0 && this.canScroll(size)) {
            this.scrollOff = MathHelper.clamp(this.scrollOff - Integer.signum(wheel), 0, size - VISIBLE_OFFERS);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        this.isDragging = false;
        if (this.canScroll(this.container.getOffers().size()) && mouseX > this.guiLeft + 94 && mouseX < this.guiLeft + 94 + 6 && mouseY > this.guiTop + 18
                && mouseY <= this.guiTop + 18 + 139 + 1) {
            this.isDragging = true;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long held) {
        if (!this.isDragging) {
            super.mouseClickMove(mouseX, mouseY, button, held);
            return;
        }
        int size = this.container.getOffers().size();
        int start = this.guiTop + 18;
        int end = start + 139;
        int steps = size - VISIBLE_OFFERS;
        float position = ((float) mouseY - (float) start - 13.5F) / ((float) (end - start) - 27.0F);
        position = position * (float) steps + 0.5F;
        this.scrollOff = MathHelper.clamp((int) position, 0, steps);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        this.isDragging = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    /** For tests: the offers shown and the selected one. */
    public int getSelectedMerchantRecipe() {
        return this.selectedMerchantRecipe;
    }
}
