package com.michaelsebero.portium.client.gui;

import com.michaelsebero.portium.inventory.ContainerLectern;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWrittenBook;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A lectern's book, read but not written in (1.14's LecternScreen on 1.14's ReadBookScreen). Turning pages asks the
 * server, which turns the lectern's page for everyone and sends it back; players who may build can take the book.
 */
@SideOnly(Side.CLIENT)
public class GuiLectern extends GuiScreen {

    private static final ResourceLocation BOOK_TEXTURES = new ResourceLocation("textures/gui/book.png");
    private static final int DONE = 0;
    private static final int TAKE_BOOK = 1;
    private static final int NEXT = 2;
    private static final int PREVIOUS = 3;
    private static final int LINE_WIDTH = 114;

    private final ContainerLectern container;
    private List<String> pages = Collections.emptyList();
    private boolean written;
    private int currPage;
    private List<ITextComponent> cachedPageLines = Collections.emptyList();
    private int cachedPage = -1;
    private String pageIndicator = "";
    private PageButton buttonNextPage;
    private PageButton buttonPreviousPage;

    public GuiLectern(ContainerLectern container) {
        this.container = container;
        container.setOnChange(this::onContainerChanged);
    }

    /** The book or page the server sent: shows that book, at the lectern's page. */
    private void onContainerChanged() {
        this.setBook(this.container.getBook());
        this.showPage(this.container.getPage());
    }

    private void setBook(ItemStack book) {
        NBTTagCompound tag = book.getTagCompound();
        this.written = book.getItem() == Items.WRITTEN_BOOK;
        if (this.written) {
            this.pages = tag != null && ItemWrittenBook.validBookTagContents(tag) ? pages(tag)
                    : Collections.singletonList(ITextComponent.Serializer.componentToJson(
                            new TextComponentTranslation("book.invalid.tag").setStyle(new net.minecraft.util.text.Style().setColor(TextFormatting.DARK_RED))));
        } else if (book.getItem() == Items.WRITABLE_BOOK) {
            this.pages = tag != null ? pages(tag) : Collections.emptyList();
        } else {
            this.pages = Collections.emptyList();
        }
        this.currPage = MathHelper.clamp(this.currPage, 0, this.pages.size());
        this.updateButtons();
        this.cachedPage = -1;
    }

    private static List<String> pages(NBTTagCompound tag) {
        NBTTagList list = tag.getTagList("pages", 8);
        List<String> pages = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) pages.add(list.getStringTagAt(i));
        return pages;
    }

    private ITextComponent page(int index) {
        if (index < 0 || index >= this.pages.size()) return new TextComponentString("");
        String text = this.pages.get(index);
        if (this.written) {
            try {
                ITextComponent component = ITextComponent.Serializer.jsonToComponent(text);
                if (component != null) return component;
            } catch (RuntimeException ignored) {
                // Shown as written.
            }
        }
        return new TextComponentString(text);
    }

    private boolean showPage(int page) {
        int clamped = MathHelper.clamp(page, 0, this.pages.size() - 1);
        if (clamped == this.currPage) return false;
        this.currPage = clamped;
        this.updateButtons();
        this.cachedPage = -1;
        return true;
    }

    /** A page link in a written book: asks the lectern to turn there. */
    private boolean requestPage(int page) {
        if (page == this.container.getPage()) return false;
        this.sendButton(ContainerLectern.PAGE_OFFSET + page);
        return true;
    }

    private void sendButton(int id) {
        this.mc.playerController.sendEnchantPacket(this.container.windowId, id);
    }

    // ---- Screen ----

    @Override
    public void initGui() {
        this.mc.player.openContainer = this.container;
        this.buttonList.clear();
        if (this.mc.player.isAllowEdit()) {
            this.buttonList.add(new GuiButton(DONE, this.width / 2 - 100, 196, 98, 20, I18n.format("gui.done")));
            this.buttonList.add(new GuiButton(TAKE_BOOK, this.width / 2 + 2, 196, 98, 20, I18n.format("lectern.take_book")));
        } else {
            this.buttonList.add(new GuiButton(DONE, this.width / 2 - 100, 196, 200, 20, I18n.format("gui.done")));
        }
        int left = (this.width - 192) / 2;
        this.buttonNextPage = this.addButton(new PageButton(NEXT, left + 116, 159, true));
        this.buttonPreviousPage = this.addButton(new PageButton(PREVIOUS, left + 43, 159, false));
        this.updateButtons();
    }

    private void updateButtons() {
        if (this.buttonNextPage == null) return;
        this.buttonNextPage.visible = this.currPage < this.pages.size() - 1;
        this.buttonPreviousPage.visible = this.currPage > 0;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case DONE:
                this.close();
                break;
            case TAKE_BOOK:
                this.sendButton(ContainerLectern.TAKE_BOOK);
                break;
            case NEXT:
                this.sendButton(ContainerLectern.NEXT_PAGE);
                break;
            case PREVIOUS:
                this.sendButton(ContainerLectern.PREVIOUS_PAGE);
                break;
            default:
                break;
        }
    }

    /** Closes the book and tells the server, so the lectern's container closes there too. */
    private void close() {
        this.mc.player.closeScreen();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.close();
        } else if (keyCode == Keyboard.KEY_PRIOR) {
            this.actionPerformed(this.buttonPreviousPage);
        } else if (keyCode == Keyboard.KEY_NEXT) {
            this.actionPerformed(this.buttonNextPage);
        }
    }

    @Override
    public void onGuiClosed() {
        this.container.setOnChange(null);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BOOK_TEXTURES);
        int left = (this.width - 192) / 2;
        this.drawTexturedModalRect(left, 2, 0, 0, 192, 192);
        if (this.cachedPage != this.currPage) {
            this.cachedPageLines = GuiUtilRenderComponents.splitText(this.page(this.currPage), LINE_WIDTH, this.fontRenderer, true, true);
            this.pageIndicator = I18n.format("book.pageIndicator", this.currPage + 1, Math.max(this.pages.size(), 1));
        }
        this.cachedPage = this.currPage;
        int indicatorWidth = this.fontRenderer.getStringWidth(this.pageIndicator);
        this.fontRenderer.drawString(this.pageIndicator, left - indicatorWidth + 192 - 44, 18, 0);
        int lines = Math.min(128 / this.fontRenderer.FONT_HEIGHT, this.cachedPageLines.size());
        for (int i = 0; i < lines; i++) {
            this.fontRenderer.drawString(this.cachedPageLines.get(i).getFormattedText(), left + 36, 32 + i * this.fontRenderer.FONT_HEIGHT, 0);
        }
        ITextComponent hovered = this.componentAt(mouseX, mouseY);
        if (hovered != null) this.handleComponentHover(hovered, mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            ITextComponent clicked = this.componentAt(mouseX, mouseY);
            if (clicked != null && this.handleComponentClick(clicked)) return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean handleComponentClick(ITextComponent component) {
        ClickEvent click = component.getStyle().getClickEvent();
        if (click == null) return false;
        if (click.getAction() == ClickEvent.Action.CHANGE_PAGE) {
            try {
                return this.requestPage(Integer.parseInt(click.getValue()) - 1);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        boolean handled = super.handleComponentClick(component);
        if (handled && click.getAction() == ClickEvent.Action.RUN_COMMAND) this.close();
        return handled;
    }

    /** The piece of text under the mouse on the page, if any. */
    @Nullable
    private ITextComponent componentAt(int mouseX, int mouseY) {
        if (this.cachedPageLines.isEmpty()) return null;
        int x = MathHelper.floor(mouseX - (this.width - 192) / 2 - 36.0D);
        int y = MathHelper.floor(mouseY - 2.0D - 30.0D);
        if (x < 0 || y < 0) return null;
        int height = this.fontRenderer.FONT_HEIGHT;
        int lines = Math.min(128 / height, this.cachedPageLines.size());
        if (x > LINE_WIDTH || y >= height * lines + lines) return null;
        int line = y / height;
        if (line < 0 || line >= this.cachedPageLines.size()) return null;
        int width = 0;
        for (ITextComponent part : this.cachedPageLines.get(line)) {
            if (part instanceof TextComponentString) {
                width += this.fontRenderer.getStringWidth(part.getStyle().getFormattingCode() + ((TextComponentString) part).getText());
                if (width > x) return part;
            }
        }
        return null;
    }

    /** 1.14's ChangePageButton: the book's arrows; the server plays the page turning. */
    private static class PageButton extends GuiButton {

        private final boolean forward;

        PageButton(int id, int x, int y, boolean forward) {
            super(id, x, y, 23, 13, "");
            this.forward = forward;
        }

        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) return;
            this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(BOOK_TEXTURES);
            int u = this.hovered ? 23 : 0;
            int v = this.forward ? 192 : 205;
            this.drawTexturedModalRect(this.x, this.y, u, v, 23, 13);
        }
    }
}
