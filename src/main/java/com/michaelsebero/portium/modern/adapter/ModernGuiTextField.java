package com.michaelsebero.portium.modern.adapter;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.client.ModernKeys;
import com.michaelsebero.portium.modern.mc.client.gui.IGuiEventListener;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * What 1.13 code makes and extends of {@code GuiTextField}: 1.12's text field, taking 1.13's keys and typed
 * characters, telling 1.13's responder about changes.
 */
@SideOnly(Side.CLIENT)
public class ModernGuiTextField extends GuiTextField implements IGuiEventListener {

    @Nullable
    private BiConsumer<Integer, String> responder;

    public ModernGuiTextField(int id, FontRenderer font, int x, int y, int width, int height) {
        super(id, font, x, y, width, height);
    }

    public ModernGuiTextField(int id, FontRenderer font, int x, int y, int width, int height, @Nullable GuiTextField previous) {
        super(id, font, x, y, width, height);
        if (previous != null) setText(previous.getText());
    }

    @Srg("func_195609_a")
    public void setTextAcceptHandler(BiConsumer<Integer, String> responder) {
        this.responder = responder;
    }

    /** 1.13 can colour what's typed; 1.12's field draws it plain. */
    @Srg("func_195607_a")
    public void setTextFormatter(BiFunction<String, Integer, String> formatter) {}

    @Srg("func_200675_a")
    public void setValidator(Predicate<String> validator) {
        super.setValidator(validator::test);
    }

    @Srg("func_195612_c")
    public void setSuggestion(@Nullable String suggestion) {}

    @Srg("func_195608_a")
    public void drawTextField(int mouseX, int mouseY, float partialTicks) {
        drawTextBox();
    }

    private void changed() {
        if (this.responder != null) this.responder.accept(getId(), getText());
    }

    @Override
    public void setText(String text) {
        String before = getText();
        super.setText(text);
        if (!getText().equals(before)) changed();
    }

    @Override
    public void writeText(String text) {
        String before = getText();
        super.writeText(text);
        if (!getText().equals(before)) changed();
    }

    @Override
    public void deleteFromCursor(int count) {
        String before = getText();
        super.deleteFromCursor(count);
        if (!getText().equals(before)) changed();
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        return getVisible() && isFocused() && textboxKeyTyped('\0', ModernKeys.toLwjgl(key));
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return getVisible() && isFocused() && textboxKeyTyped(character, 0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return mouseClicked((int) mouseX, (int) mouseY, button);
    }

    @Override
    public void focusChanged(boolean focused) {
        setFocused(focused);
    }

    @Override
    public boolean canFocus() {
        return getVisible() && getEnableBackgroundDrawing();
    }
}
