package com.michaelsebero.portium.modern.mc.client.gui;

import com.michaelsebero.portium.modern.Srg;

/**
 * 1.13's {@code net.minecraft.client.gui.IGuiEventListener}: something on a screen that takes the mouse and keyboard,
 * with 1.13's input: mouse positions as doubles, GLFW key codes, typed characters apart from keys.
 */
public interface IGuiEventListener {

    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    default boolean mouseScrolled(double delta) {
        return false;
    }

    default boolean keyPressed(int key, int scanCode, int modifiers) {
        return false;
    }

    default boolean keyReleased(int key, int scanCode, int modifiers) {
        return false;
    }

    default boolean charTyped(char character, int modifiers) {
        return false;
    }

    @Srg("func_205700_b")
    default void focusChanged(boolean focused) {}

    @Srg("func_207704_ae_")
    default boolean canFocus() {
        return false;
    }
}
