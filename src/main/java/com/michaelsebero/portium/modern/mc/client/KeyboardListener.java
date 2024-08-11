package com.michaelsebero.portium.modern.mc.client;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/** 1.13's {@code net.minecraft.client.KeyboardListener}, {@code Minecraft.keyboardListener}: key repeat and the clipboard. */
@SideOnly(Side.CLIENT)
public final class KeyboardListener {

    static final KeyboardListener INSTANCE = new KeyboardListener();

    public static KeyboardListener get() {
        return INSTANCE;
    }

    private KeyboardListener() {}

    @Srg("func_197967_a")
    public void enableRepeatEvents(boolean repeat) {
        Keyboard.enableRepeatEvents(repeat);
    }

    @Srg("func_197965_a")
    public String getClipboardString() {
        return GuiScreen.getClipboardString();
    }

    @Srg("func_197960_a")
    public void setClipboardString(String text) {
        GuiScreen.setClipboardString(text);
    }
}
