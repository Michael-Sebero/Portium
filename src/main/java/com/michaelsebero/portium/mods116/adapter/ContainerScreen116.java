package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * What a 1.16 mod's menu screens extend instead of 1.16's {@code ContainerScreen}: 1.12's container screen with
 * 1.16's title, widgets, listeners and calls (as {@link Screen116} gives plain screens). 1.12 draws the frame: its
 * background and foreground layers call 1.16's {@code drawGuiContainerBackgroundLayer} and
 * {@code drawGuiContainerForegroundLayer}, its slots and held stack are its own. 1.16's {@code render} and input
 * calls, which mods override and then call up to, end in 1.12's container screen, so slots draw and click as in
 * 1.12. The menu, size and position fields share 1.12's SRG names; the matrix stack is taken as an object.
 */
@SideOnly(Side.CLIENT)
public abstract class ContainerScreen116 extends GuiContainer {

    @Srg("field_230704_d_")
    protected final ITextComponent title;
    @Srg("field_230705_e_")
    protected final List<Object> children = new ArrayList<>();
    @Srg("field_230710_m_")
    protected final List<Widget116> buttons = new ArrayList<>();
    @Srg("field_230711_n_")
    public boolean passEvents;
    @Srg("field_213127_e")
    protected final InventoryPlayer playerInventory;
    @Srg("field_238742_p_")
    protected int titleX = 8;
    @Srg("field_238743_q_")
    protected int titleY = 6;
    @Srg("field_238744_r_")
    protected int playerInventoryTitleX = 8;
    @Srg("field_238745_s_")
    protected int playerInventoryTitleY;

    private boolean dragging;
    private Object focused;
    /** The character of the key being handled, which 1.16's key call doesn't carry and 1.12's screen wants. */
    private char typed;
    /** Inside 1.16's render, drawing through 1.12's frame: 1.12's drawing doesn't call back into it. */
    private boolean drawing;

    public ContainerScreen116(Container container, InventoryPlayer playerInventory, ITextComponent title) {
        super(container);
        this.title = title;
        this.playerInventory = playerInventory;
        this.playerInventoryTitleY = this.ySize - 94;
    }

    @Srg("func_231171_q_")
    public ITextComponent getTitle() {
        return this.title;
    }

    /** 1.16's {@code getContainer()}: the menu. */
    @Srg("func_212873_a_")
    public Container getContainer() {
        return this.inventorySlots;
    }

    // Lifecycle

    @Srg("func_231158_b_")
    public void init(Minecraft minecraft, int width, int height) {
        setWorldAndResolution(minecraft, width, height);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttons.clear();
        this.children.clear();
        this.focused = null;
        Keyboard.enableRepeatEvents(true);
        init();
    }

    /** 1.16's: sets the screen up; 1.12's already placed it (guiLeft, guiTop) before this is called. */
    @Srg("func_231160_c_")
    protected void init() {}

    @Override
    public void updateScreen() {
        super.updateScreen();
        tick();
    }

    @Srg("func_231023_e_")
    public void tick() {}

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        onClose();
    }

    /** 1.16's: the screen closes; the menu is closed as 1.12's container screen closes it. */
    @Srg("func_231164_f_")
    public void onClose() {
        super.onGuiClosed();
    }

    @Srg("func_231175_as__")
    public void closeScreen() {
        this.mc.player.closeScreen();
    }

    @Srg("func_231178_ax__")
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Srg("func_231177_au__")
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return isPauseScreen();
    }

    @Srg("func_231152_a_")
    public void resize(Minecraft minecraft, int width, int height) {
        setWorldAndResolution(minecraft, width, height);
    }

    // Widgets

    @Srg("func_230480_a_")
    protected Widget116 addButton(Widget116 button) {
        this.buttons.add(button);
        this.children.add(button);
        return button;
    }

    @Srg("func_230481_d_")
    protected Object addListener(Object listener) {
        this.children.add(listener);
        return listener;
    }

    @Srg("func_231039_at__")
    public List<Object> getEventListeners() {
        return this.children;
    }

    // Drawing: 1.12's frame calls 1.16's render, which mods override and call up to; that ends in 1.12's frame.

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.drawing) {
            super.drawScreen(mouseX, mouseY, partialTicks);
        } else {
            render(ScreenRendering116.matrixStack(), mouseX, mouseY, partialTicks);
        }
    }

    /** 1.16's: draws the background, widgets, slots and foreground, through 1.12's container screen. */
    @Srg("func_230430_a_")
    public void render(Object matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.drawing = true;
        try {
            super.drawScreen(mouseX, mouseY, partialTicks);
        } finally {
            this.drawing = false;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        Object stack = ScreenRendering116.matrixStack();
        drawGuiContainerBackgroundLayer(stack, partialTicks, mouseX, mouseY);
        // 1.16 draws its widgets between the background and the slots.
        for (Widget116 button : this.buttons) button.render(stack, mouseX, mouseY, partialTicks);
    }

    /** 1.16's: the menu's background. */
    @Srg("func_230450_a_")
    protected void drawGuiContainerBackgroundLayer(Object matrixStack, float partialTicks, int mouseX, int mouseY) {}

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawGuiContainerForegroundLayer(ScreenRendering116.matrixStack(), mouseX, mouseY);
    }

    /** 1.16's: the title and the player's inventory's name, at their places (relative to the menu). */
    @Srg("func_230451_b_")
    protected void drawGuiContainerForegroundLayer(Object matrixStack, int mouseX, int mouseY) {
        this.fontRenderer.drawString(this.title.getFormattedText(), this.titleX, this.titleY, 0x404040);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getFormattedText(), this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040);
    }

    /** 1.16's: the tooltip of the stack under the mouse. */
    @Srg("func_230459_a_")
    protected void renderHoveredTooltip(Object matrixStack, int mouseX, int mouseY) {
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Srg("func_230446_a_")
    public void renderBackground(Object matrixStack) {
        drawDefaultBackground();
    }

    @Srg("func_238651_a_")
    public void renderBackground(Object matrixStack, int offset) {
        drawDefaultBackground();
    }

    // Tooltips: 1.16's, drawn by 1.12's (lines as formatted text).

    @Srg("func_243308_b")
    public void renderTooltip(Object matrixStack, List<?> lines, int mouseX, int mouseY) {
        drawHoveringText(FontShims116.strings(lines), mouseX, mouseY, this.fontRenderer);
    }

    @Srg("func_238654_b_")
    public void renderReorderedTooltip(Object matrixStack, List<?> lines, int mouseX, int mouseY) {
        drawHoveringText(FontShims116.strings(lines), mouseX, mouseY, this.fontRenderer);
    }

    @Srg("func_238652_a_")
    public void renderTooltip(Object matrixStack, ITextComponent text, int mouseX, int mouseY) {
        drawHoveringText(java.util.Collections.singletonList(text.getFormattedText()), mouseX, mouseY, this.fontRenderer);
    }

    /** 1.16's item tooltip, with the lines of {@code getTooltipFromItem} (which mods override). */
    @Srg("func_230457_a_")
    protected void renderTooltip(Object matrixStack, net.minecraft.item.ItemStack stack, int mouseX, int mouseY) {
        net.minecraft.client.gui.FontRenderer font = stack.getItem().getFontRenderer(stack);
        net.minecraftforge.fml.client.config.GuiUtils.preItemToolTip(stack);
        drawHoveringText(FontShims116.strings(getTooltipFromItem(stack)), mouseX, mouseY, font == null ? this.fontRenderer : font);
        net.minecraftforge.fml.client.config.GuiUtils.postItemToolTip();
    }

    @Srg("func_231151_a_")
    public List<ITextComponent> getTooltipFromItem(net.minecraft.item.ItemStack stack) {
        List<ITextComponent> lines = new ArrayList<>();
        for (String line : getItemToolTip(stack)) lines.add(new net.minecraft.util.text.TextComponentString(line));
        return lines;
    }

    // Input: 1.12's events become 1.16's calls, whose defaults are 1.12's container screen's (slots).

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            double x = Mouse.getEventX() * this.width / (double) this.mc.displayWidth;
            double y = this.height - Mouse.getEventY() * this.height / (double) this.mc.displayHeight - 1;
            mouseScrolled(x, y, Math.signum(wheel));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        mouseClicked((double) mouseX, (double) mouseY, button);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        mouseReleased((double) mouseX, (double) mouseY, button);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long time) {
        mouseDragged(mouseX, mouseY, button, 0, 0);
    }

    @Override
    protected void keyTyped(char character, int key) throws IOException {
        this.typed = character;
        if (keyPressed(ScreenRendering116.glfwKey(key), key, 0)) return;
        if (character >= ' ') charTyped(character, 0);
    }

    @Srg("func_231044_a_")
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Object child : this.children) {
            if (child instanceof Widget116 && ((Widget116) child).mouseClicked(mouseX, mouseY, button)) {
                this.focused = child;
                if (button == 0) this.dragging = true;
                return true;
            }
        }
        try {
            super.mouseClicked((int) mouseX, (int) mouseY, button);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Srg("func_231048_c_")
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragging = false;
        if (this.focused instanceof Widget116 && ((Widget116) this.focused).mouseReleased(mouseX, mouseY, button)) return true;
        super.mouseReleased((int) mouseX, (int) mouseY, button);
        return true;
    }

    @Srg("func_231045_a_")
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.focused instanceof Widget116 && this.dragging && ((Widget116) this.focused).mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        super.mouseClickMove((int) mouseX, (int) mouseY, button, 0L);
        return true;
    }

    @Srg("func_231043_a_")
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        for (Object child : this.children) {
            if (child instanceof Widget116 && ((Widget116) child).isMouseOver(mouseX, mouseY) && ((Widget116) child).mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return false;
    }

    /** 1.16's: a focused widget first, then 1.12's container keys (closing, number keys to swap stacks). */
    @Srg("func_231046_a_")
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (this.focused instanceof Widget116 && ((Widget116) this.focused).keyPressed(key, scanCode, modifiers)) return true;
        try {
            super.keyTyped(this.typed, scanCode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Srg("func_223281_a_")
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        return this.focused instanceof Widget116 && ((Widget116) this.focused).keyReleased(key, scanCode, modifiers);
    }

    @Srg("func_231042_a_")
    public boolean charTyped(char character, int modifiers) {
        return this.focused instanceof Widget116 && ((Widget116) this.focused).charTyped(character, modifiers);
    }

    @Srg("func_231047_b_")
    public boolean isMouseOver(double mouseX, double mouseY) {
        return true;
    }

    @Srg("func_231041_ay__")
    public boolean isDragging() {
        return this.dragging;
    }

    @Srg("func_231037_b__")
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Srg("func_241217_q_")
    public Object getListener() {
        return this.focused;
    }

    @Srg("func_231035_a_")
    public void setListener(Object listener) {
        this.focused = listener;
    }

    @Srg("func_231173_s_")
    public static boolean hasShiftDown() {
        return isShiftKeyDown();
    }

    @Srg("func_231172_r_")
    public static boolean hasControlDown() {
        return isCtrlKeyDown();
    }

    @Srg("func_231174_t_")
    public static boolean hasAltDown() {
        return isAltKeyDown();
    }

    /** 1.12's {@code Gui} drawing, for widgets that draw through their screen. */
    protected Gui gui() {
        return this;
    }
}
