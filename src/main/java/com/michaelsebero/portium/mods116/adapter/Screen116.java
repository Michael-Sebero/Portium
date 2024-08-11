package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * What a 1.16 mod's screens extend instead of 1.16's {@code Screen}: 1.12's screen with 1.16's title, widgets,
 * listeners and methods. 1.12's frame and input go to 1.16's calls ({@code init}, {@code render}, {@code tick},
 * {@code mouseClicked} and so on). 1.16's own fields 1.12's screen has too (its game, size, font and item renderer)
 * are 1.12's, read and written through Portium's field redirects. 1.16's matrix stack is taken as an object.
 */
@SideOnly(Side.CLIENT)
public class Screen116 extends GuiScreen {

    @Srg("field_230704_d_")
    protected final ITextComponent title;
    /** 1.16's listeners: its widgets and whatever else takes input. */
    @Srg("field_230705_e_")
    protected final List<Object> children = new ArrayList<>();
    @Srg("field_230710_m_")
    protected final List<Widget116> buttons = new ArrayList<>();
    @Srg("field_230711_n_")
    public boolean passEvents;
    private boolean dragging;
    private Object focused;

    public Screen116(ITextComponent title) {
        this.title = title;
    }

    @Srg("func_231171_q_")
    public ITextComponent getTitle() {
        return this.title;
    }

    // Lifecycle

    @Srg("func_231158_b_")
    public void init(Minecraft minecraft, int width, int height) {
        setWorldAndResolution(minecraft, width, height);
    }

    @Override
    public void initGui() {
        this.buttons.clear();
        this.children.clear();
        this.focused = null;
        Keyboard.enableRepeatEvents(true);
        init();
    }

    @Srg("func_231160_c_")
    protected void init() {}

    @Override
    public void updateScreen() {
        tick();
    }

    @Srg("func_231023_e_")
    public void tick() {}

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        onClose();
    }

    @Srg("func_231164_f_")
    public void onClose() {}

    @Srg("func_231175_as__")
    public void closeScreen() {
        this.mc.displayGuiScreen(null);
    }

    @Srg("func_231178_ax__")
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return isPauseScreen();
    }

    @Srg("func_231177_au__")
    public boolean isPauseScreen() {
        return true;
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

    /** 1.16 adds any listener; typed loosely (1.16's listener type is its own). */
    @Srg("func_230481_d_")
    protected Object addListener(Object listener) {
        this.children.add(listener);
        return listener;
    }

    @Srg("func_231039_at__")
    public List<Object> getEventListeners() {
        return this.children;
    }

    // Drawing

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        render(ScreenRendering116.matrixStack(), mouseX, mouseY, partialTicks);
    }

    @Srg("func_230430_a_")
    public void render(Object matrixStack, int mouseX, int mouseY, float partialTicks) {
        for (Widget116 button : this.buttons) button.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Srg("func_230446_a_")
    public void renderBackground(Object matrixStack) {
        drawDefaultBackground();
    }

    @Srg("func_238651_a_")
    public void renderBackground(Object matrixStack, int offset) {
        drawDefaultBackground();
    }

    @Srg("func_231165_f_")
    public void renderDirtBackground(int offset) {
        drawBackground(offset);
    }

    // Input: 1.12's events become 1.16's calls.

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
        int glfw = ScreenRendering116.glfwKey(key);
        if (keyPressed(glfw, key, 0)) return;
        if (key == Keyboard.KEY_ESCAPE && shouldCloseOnEsc()) {
            closeScreen();
            return;
        }
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
        return false;
    }

    @Srg("func_231048_c_")
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragging = false;
        return this.focused instanceof Widget116 && ((Widget116) this.focused).mouseReleased(mouseX, mouseY, button);
    }

    @Srg("func_231045_a_")
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.focused instanceof Widget116 && this.dragging && ((Widget116) this.focused).mouseDragged(mouseX, mouseY, button, dragX, dragY);
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

    @Srg("func_231046_a_")
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        return this.focused instanceof Widget116 && ((Widget116) this.focused).keyPressed(key, scanCode, modifiers);
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

    // 1.16's key state helpers.

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
