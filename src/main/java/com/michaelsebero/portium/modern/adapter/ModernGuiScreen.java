package com.michaelsebero.portium.modern.adapter;

import com.google.common.collect.Lists;
import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.client.ModernKeys;
import com.michaelsebero.portium.modern.mc.client.gui.IGuiEventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

/**
 * What a 1.13 mod's {@code GuiScreen} subclasses extend instead of 1.12's: 1.12's screen with 1.13's input. 1.12's
 * mouse and keyboard events become 1.13's calls ({@code mouseClicked(double, double, int)}, {@code keyPressed} with
 * GLFW codes, {@code charTyped}), which go to the screen's children as in 1.13.
 */
@SideOnly(Side.CLIENT)
public class ModernGuiScreen extends GuiScreen implements IGuiEventListener {

    @Srg("field_195124_j")
    protected final List<IGuiEventListener> children = Lists.newArrayList();
    /** 1.16's screen title; empty for 1.13's screens. */
    @Srg("field_230704_d_")
    protected final net.minecraft.util.text.ITextComponent title;

    public ModernGuiScreen() {
        this(new net.minecraft.util.text.TextComponentString(""));
    }

    /** 1.16's: a screen has a title. */
    public ModernGuiScreen(net.minecraft.util.text.ITextComponent title) {
        this.title = title;
    }

    @Srg("func_231171_q_")
    public net.minecraft.util.text.ITextComponent getTitle() {
        return this.title;
    }
    private final ModernGuiInput input = new ModernGuiInput();

    @Srg("func_195074_b")
    public List<? extends IGuiEventListener> getChildren() {
        return this.children;
    }

    @Nullable
    public IGuiEventListener getFocused() {
        return this.input.focused;
    }

    @Srg("func_195073_a")
    protected void setFocused(@Nullable IGuiEventListener child) {
        this.input.focus(child);
    }

    @Srg("func_205725_b")
    public void focusOn(@Nullable IGuiEventListener child) {
        this.input.focus(child);
    }

    @Srg("func_195072_d")
    protected final void setDragging(boolean dragging) {
        this.input.dragging = dragging;
    }

    @Override
    protected <T extends GuiButton> T addButton(T button) {
        super.addButton(button);
        if (button instanceof IGuiEventListener) this.children.add((IGuiEventListener) button);
        return button;
    }

    @Override
    public void setWorldAndResolution(Minecraft mc, int width, int height) {
        this.children.clear();
        this.input.reset();
        super.setWorldAndResolution(mc, width, height);
    }

    @Srg("func_195120_Y_")
    public boolean allowCloseWithEscape() {
        return true;
    }

    @Srg("func_195122_V_")
    public void close() {
        this.mc.displayGuiScreen(null);
    }

    // 1.13's input, as 1.13's screen handles it.

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        return this.input.mouseClicked(getChildren(), x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        return this.input.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return this.input.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256 && allowCloseWithEscape()) {
            close();
            return true;
        }
        return this.input.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        return this.input.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return this.input.charTyped(character, modifiers);
    }

    // 1.12's events, made 1.13's.

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        this.input.lastX = x;
        this.input.lastY = y;
        mouseClicked((double) x, (double) y, button);
    }

    @Override
    protected void mouseReleased(int x, int y, int button) {
        mouseReleased((double) x, (double) y, button);
    }

    @Override
    protected void mouseClickMove(int x, int y, int button, long time) {
        double dx = x - this.input.lastX;
        double dy = y - this.input.lastY;
        this.input.lastX = x;
        this.input.lastY = y;
        mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) mouseScrolled(Math.signum(wheel));
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        if (!Keyboard.getEventKeyState() && Keyboard.getEventKey() != 0) {
            keyReleased(ModernKeys.toGlfw(Keyboard.getEventKey()), Keyboard.getEventKey(), ModernKeys.modifiers());
            this.mc.dispatchKeypresses();
            return;
        }
        super.handleKeyboardInput();
    }

    @Override
    protected void keyTyped(char character, int key) throws IOException {
        ModernGuiInput.keyTyped(this, character, key);
    }
}
