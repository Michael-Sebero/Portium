package com.michaelsebero.portium.modern.adapter;

import com.michaelsebero.portium.modern.client.ModernKeys;
import com.michaelsebero.portium.modern.mc.client.gui.IGuiEventListener;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.13's handling of a screen's children ({@code GuiEventHandler}), shared by Portium's 1.13 screens: the child a
 * click lands on takes focus (and drags with the left button), and keys and typed characters go to the focused one.
 */
@SideOnly(Side.CLIENT)
final class ModernGuiInput {

    @Nullable
    IGuiEventListener focused;
    boolean dragging;
    double lastX;
    double lastY;

    boolean mouseClicked(List<? extends IGuiEventListener> children, double x, double y, int button) {
        for (IGuiEventListener child : children) {
            if (child.mouseClicked(x, y, button)) {
                focus(child);
                if (button == 0) this.dragging = true;
                return true;
            }
        }
        return false;
    }

    boolean mouseReleased(double x, double y, int button) {
        this.dragging = false;
        return this.focused != null && this.focused.mouseReleased(x, y, button);
    }

    boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return this.focused != null && this.dragging && button == 0 && this.focused.mouseDragged(x, y, button, dx, dy);
    }

    boolean keyPressed(int key, int scanCode, int modifiers) {
        return this.focused != null && this.focused.keyPressed(key, scanCode, modifiers);
    }

    boolean keyReleased(int key, int scanCode, int modifiers) {
        return this.focused != null && this.focused.keyReleased(key, scanCode, modifiers);
    }

    boolean charTyped(char character, int modifiers) {
        return this.focused != null && this.focused.charTyped(character, modifiers);
    }

    void focus(@Nullable IGuiEventListener child) {
        if (this.focused == child) return;
        if (this.focused != null) this.focused.focusChanged(false);
        this.focused = child;
        if (child != null) child.focusChanged(true);
    }

    void reset() {
        this.focused = null;
        this.dragging = false;
    }

    /** A 1.12 key press as 1.13 sees it: the key, then the character typed. */
    static void keyTyped(IGuiEventListener screen, char character, int key) {
        int modifiers = ModernKeys.modifiers();
        if (key != 0) screen.keyPressed(ModernKeys.toGlfw(key), key, modifiers);
        if (character >= ' ' && character != 127) screen.charTyped(character, modifiers);
    }
}
