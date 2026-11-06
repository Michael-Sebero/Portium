package com.michaelsebero.portium.client.gui;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.inventory.ContainerLoom;
import com.michaelsebero.portium.item.BannerPatterns116;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BannerTextures;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The loom's screen (1.14): the patterns as a 4-wide grid of little banners, a scroll bar, and the finished banner.
 * 1.14 draws the banner model; this draws the flag from 1.12's own combined banner textures, the same picture.
 */
public class GuiLoom extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/gui/container/loom.png");
    /** The flag's front in the 64x64 banner texture. */
    private static final int FLAG_U = 1, FLAG_V = 1, FLAG_W = 20, FLAG_H = 40;

    private final ContainerLoom container;
    private final InventoryPlayer playerInventory;
    @Nullable
    private ResourceLocation result;
    private ItemStack bannerStack = ItemStack.EMPTY;
    private ItemStack dyeStack = ItemStack.EMPTY;
    private ItemStack patternStack = ItemStack.EMPTY;
    private boolean displayPatterns;
    private boolean displayItemPattern;
    private boolean tooManyPatterns;
    private float sliderProgress;
    private boolean scrolling;
    private int indexStarting = 1;

    public GuiLoom(ContainerLoom container, InventoryPlayer playerInventory) {
        super(container);
        this.container = container;
        this.playerInventory = playerInventory;
        container.setUpdateListener(this::containerChanged);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(I18n.format("container.loom"), 8, 4, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 94, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int left = this.guiLeft;
        int top = this.guiTop;
        this.drawTexturedModalRect(left, top, 0, 0, this.xSize, this.ySize);
        Slot banner = this.container.getBannerSlot();
        Slot dye = this.container.getDyeSlot();
        Slot pattern = this.container.getPatternSlot();
        Slot output = this.container.getOutputSlot();
        if (!banner.getHasStack()) this.drawTexturedModalRect(left + banner.xPos, top + banner.yPos, this.xSize, 0, 16, 16);
        if (!dye.getHasStack()) this.drawTexturedModalRect(left + dye.xPos, top + dye.yPos, this.xSize + 16, 0, 16, 16);
        if (!pattern.getHasStack()) this.drawTexturedModalRect(left + pattern.xPos, top + pattern.yPos, this.xSize + 32, 0, 16, 16);
        int slider = (int) (41.0F * this.sliderProgress);
        this.drawTexturedModalRect(left + 119, top + 13 + slider, 232 + (this.displayPatterns ? 0 : 12), 0, 12, 15);
        if (this.result != null && !this.tooManyPatterns) {
            this.drawFlag(this.result, left + 141, top + 8, 20, 40);
        } else if (this.tooManyPatterns) {
            this.mc.getTextureManager().bindTexture(TEXTURE);
            this.drawTexturedModalRect(left + output.xPos - 2, top + output.yPos - 2, this.xSize, 17, 17, 16);
        }
        if (this.displayPatterns) {
            int gridLeft = left + 60;
            int gridTop = top + 13;
            int last = this.indexStarting + 16;
            for (int i = this.indexStarting; i < last && i < BannerPatterns116.ORDER.length - BannerPatterns116.WITH_ITEMS; i++) {
                int index = i - this.indexStarting;
                int x = gridLeft + index % 4 * 14;
                int y = gridTop + index / 4 * 14;
                this.mc.getTextureManager().bindTexture(TEXTURE);
                int v = this.ySize;
                if (i == this.container.getSelectedPattern()) v += 14;
                else if (mouseX >= x && mouseY >= y && mouseX < x + 14 && mouseY < y + 14) v += 28;
                this.drawTexturedModalRect(x, y, 0, v, 14, 14);
                this.drawPatternPreview(i, x, y);
            }
        } else if (this.displayItemPattern) {
            int x = left + 60;
            int y = top + 13;
            this.mc.getTextureManager().bindTexture(TEXTURE);
            this.drawTexturedModalRect(x, y, 0, this.ySize, 14, 14);
            this.drawPatternPreview(this.container.getSelectedPattern(), x, y);
        }
    }

    /** A grey banner with the pattern in white, as 1.14's buttons show it. */
    private void drawPatternPreview(int index, int x, int y) {
        BannerPattern pattern = BannerPatterns116.byIndex(index);
        if (pattern == null) return;
        List<BannerPattern> patterns = new ArrayList<>();
        List<EnumDyeColor> colors = new ArrayList<>();
        patterns.add(BannerPattern.BASE);
        colors.add(EnumDyeColor.GRAY);
        patterns.add(pattern);
        colors.add(EnumDyeColor.WHITE);
        String key = "b" + EnumDyeColor.GRAY.getDyeDamage() + pattern.getHashname() + EnumDyeColor.WHITE.getDyeDamage();
        ResourceLocation texture = BannerTextures.BANNER_DESIGNS.getResourceLocation(key, patterns, colors);
        if (texture != null) this.drawFlag(texture, x + 5, y + 2, 5, 10);
    }

    private void drawFlag(ResourceLocation texture, int x, int y, int width, int height) {
        this.mc.getTextureManager().bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawScaledCustomSizeModalRect(x, y, FLAG_U, FLAG_V, FLAG_W, FLAG_H, width, height, 64.0F, 64.0F);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        this.scrolling = false;
        if (this.displayPatterns) {
            int gridLeft = this.guiLeft + 60;
            int gridTop = this.guiTop + 13;
            for (int i = this.indexStarting; i < this.indexStarting + 16; i++) {
                int index = i - this.indexStarting;
                double dx = mouseX - (double) (gridLeft + index % 4 * 14);
                double dy = mouseY - (double) (gridTop + index / 4 * 14);
                if (dx >= 0.0D && dy >= 0.0D && dx < 14.0D && dy < 14.0D && this.container.enchantItem(this.mc.player, i)) {
                    this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(PortiumSounds.UI_LOOM_SELECT_PATTERN, 1.0F));
                    this.mc.playerController.sendEnchantPacket(this.container.windowId, i);
                    return;
                }
            }
            int scrollLeft = this.guiLeft + 119;
            int scrollTop = this.guiTop + 9;
            if (mouseX >= scrollLeft && mouseX < scrollLeft + 12 && mouseY >= scrollTop && mouseY < scrollTop + 56) this.scrolling = true;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (this.scrolling && this.displayPatterns) {
            int top = this.guiTop + 13;
            int bottom = top + 56;
            this.sliderProgress = MathHelper.clamp(((float) mouseY - (float) top - 7.5F) / ((float) (bottom - top) - 15.0F), 0.0F, 1.0F);
            int hidden = BannerPatterns116.ROWS - 4;
            this.indexStarting = 1 + Math.max(0, (int) ((double) (this.sliderProgress * (float) hidden) + 0.5D)) * 4;
            return;
        }
        super.mouseClickMove(mouseX, mouseY, button, timeSinceLastClick);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && this.displayPatterns) {
            int hidden = BannerPatterns116.ROWS - 4;
            this.sliderProgress = MathHelper.clamp(this.sliderProgress - (float) Integer.signum(wheel) / (float) hidden, 0.0F, 1.0F);
            this.indexStarting = 1 + (int) ((double) (this.sliderProgress * (float) hidden) + 0.5D) * 4;
        }
    }

    /** 1.14's containerChange: what to show for the slots' contents. */
    private void containerChanged() {
        ItemStack output = this.container.getOutputSlot().getStack();
        this.result = output.isEmpty() ? null : bannerTexture(output);
        ItemStack banner = this.container.getBannerSlot().getStack();
        ItemStack dye = this.container.getDyeSlot().getStack();
        ItemStack pattern = this.container.getPatternSlot().getStack();
        this.tooManyPatterns = !banner.isEmpty() && ContainerLoom.patternCount(banner) >= 6;
        if (this.tooManyPatterns) this.result = null;
        if (!ItemStack.areItemStacksEqual(banner, this.bannerStack) || !ItemStack.areItemStacksEqual(dye, this.dyeStack)
                || !ItemStack.areItemStacksEqual(pattern, this.patternStack)) {
            this.displayPatterns = !banner.isEmpty() && !dye.isEmpty() && pattern.isEmpty() && !this.tooManyPatterns;
            this.displayItemPattern = !this.tooManyPatterns && !pattern.isEmpty() && !banner.isEmpty() && !dye.isEmpty();
        }
        this.bannerStack = banner.copy();
        this.dyeStack = dye.copy();
        this.patternStack = pattern.copy();
    }

    /** The combined banner texture for a banner item: its base colour and its layers. */
    @Nullable
    private static ResourceLocation bannerTexture(ItemStack banner) {
        EnumDyeColor base = ItemBanner.getBaseColor(banner);
        List<BannerPattern> patterns = new ArrayList<>();
        List<EnumDyeColor> colors = new ArrayList<>();
        patterns.add(BannerPattern.BASE);
        colors.add(base);
        StringBuilder key = new StringBuilder("b").append(base.getDyeDamage());
        NBTTagCompound tag = banner.getSubCompound("BlockEntityTag");
        if (tag != null && tag.hasKey("Patterns", 9)) {
            NBTTagList list = tag.getTagList("Patterns", 10);
            for (int i = 0; i < list.tagCount() && i < 6; i++) {
                NBTTagCompound layer = list.getCompoundTagAt(i);
                BannerPattern pattern = BannerPattern.byHash(layer.getString("Pattern"));
                if (pattern == null) continue;
                EnumDyeColor color = EnumDyeColor.byDyeDamage(layer.getInteger("Color"));
                patterns.add(pattern);
                colors.add(color);
                key.append(pattern.getHashname()).append(color.getDyeDamage());
            }
        }
        return BannerTextures.BANNER_DESIGNS.getResourceLocation(key.toString(), patterns, colors);
    }
}
