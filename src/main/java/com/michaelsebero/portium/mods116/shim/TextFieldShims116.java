package com.michaelsebero.portium.mods116.shim;

import net.minecraft.client.gui.GuiTextField;

/**
 * 1.16's widget members of a text field. 1.16's text field is a widget; 1.12's isn't, so what 1.16 code reads
 * through {@code Widget} on a text field (Quark's chest search bar) goes to 1.12's text field's own.
 */
public final class TextFieldShims116 {

    private TextFieldShims116() {}

    public static boolean isFocused(GuiTextField field) {
        return field.isFocused();
    }

    public static void setFocused(GuiTextField field, boolean focused) {
        field.setFocused(focused);
    }

    public static boolean visible(GuiTextField field) {
        return field.getVisible();
    }

    public static void setVisible(GuiTextField field, boolean visible) {
        field.setVisible(visible);
    }

    public static int x(GuiTextField field) {
        return field.x;
    }

    public static void setX(GuiTextField field, int x) {
        field.x = x;
    }

    public static int y(GuiTextField field) {
        return field.y;
    }

    public static void setY(GuiTextField field, int y) {
        field.y = y;
    }

    public static int width(GuiTextField field) {
        return field.width;
    }

    public static void setWidth(GuiTextField field, int width) {
        field.width = width;
    }

    public static int height(GuiTextField field) {
        return field.height;
    }

    public static void setHeight(GuiTextField field, int height) {
        field.height = height;
    }

    /** {@code render(matrices, mouseX, mouseY, partialTicks)}: 1.12's text box draws itself where it is. */
    public static void render(GuiTextField field, Object matrices, int mouseX, int mouseY, float partialTicks) {
        field.drawTextBox();
    }

    public static boolean mouseClicked(GuiTextField field, double mouseX, double mouseY, int button) {
        return field.mouseClicked((int) mouseX, (int) mouseY, button);
    }

    public static boolean keyPressed(GuiTextField field, int key, int scanCode, int modifiers) {
        return field.getVisible() && field.isFocused() && field.textboxKeyTyped('\0', com.michaelsebero.portium.modern.client.ModernKeys.toLwjgl(key));
    }

    public static boolean charTyped(GuiTextField field, char character, int modifiers) {
        return field.getVisible() && field.isFocused() && field.textboxKeyTyped(character, 0);
    }
}
