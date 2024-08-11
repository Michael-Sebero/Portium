package com.michaelsebero.portium.modern.adapter;

import com.google.common.collect.Lists;
import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.client.ModernKeys;
import com.michaelsebero.portium.modern.mc.client.gui.IGuiEventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

/**
 * What a 1.13 mod's {@code GuiContainer} subclasses extend instead of 1.12's: 1.12's container screen with 1.13's
 * input (see {@link ModernGuiScreen}). What 1.13's container screen does with input none of its children takes,
 * clicking and dragging slots, closing, hotbar keys, is 1.12's container screen's.
 */
@SideOnly(Side.CLIENT)
public abstract class ModernGuiContainer extends GuiContainer implements IGuiEventListener {

    @Srg("field_195124_j")
    protected final List<IGuiEventListener> children = Lists.newArrayList();
    private final ModernGuiInput input = new ModernGuiInput();
    /** The character of the 1.12 key event being handled, for 1.12's own key handling. */
    private char typing;
    private long clickTime;

    /** 1.16's screen title; empty for 1.13's screens. */
    @Srg("field_230704_d_")
    protected final net.minecraft.util.text.ITextComponent title;

    public ModernGuiContainer(Container container) {
        this(container, null, new net.minecraft.util.text.TextComponentString(""));
    }

    /** 1.16's: the container, the player's inventory, and a title. */
    public ModernGuiContainer(Container container, @Nullable net.minecraft.entity.player.InventoryPlayer inventory, net.minecraft.util.text.ITextComponent title) {
        super(container);
        this.title = title;
    }

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
        this.mc.player.closeScreen();
    }

    // 1.13's input: children first, then 1.12's container screen.

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (this.input.mouseClicked(getChildren(), x, y, button)) return true;
        try {
            super.mouseClicked((int) x, (int) y, button);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        boolean handled = this.input.mouseReleased(x, y, button);
        super.mouseReleased((int) x, (int) y, button);
        return handled;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (this.input.mouseDragged(x, y, button, dx, dy)) return true;
        super.mouseClickMove((int) x, (int) y, button, Minecraft.getSystemTime() - this.clickTime);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (this.input.keyPressed(key, scanCode, modifiers)) return true;
        try {
            super.keyTyped(this.typing, ModernKeys.toLwjgl(key));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return true;
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
    protected void mouseClicked(int x, int y, int button) {
        this.input.lastX = x;
        this.input.lastY = y;
        this.clickTime = Minecraft.getSystemTime();
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
    protected void keyTyped(char character, int key) {
        this.typing = character;
        ModernGuiInput.keyTyped(this, character, key);
        this.typing = 0;
    }
}
