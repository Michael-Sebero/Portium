package com.michaelsebero.portium.modern.mc.client;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.14's {@code net.minecraft.client.ClipboardHelper}: the clipboard, through GLFW in 1.14 on (which 1.12 lacks), so
 * through 1.12's screens here. Create's Ponder screen makes one.
 */
@SideOnly(Side.CLIENT)
public class ClipboardHelper {

    public ClipboardHelper() {}

    /** 1.16's: the clipboard's text; the window and GLFW error callback are 1.16's, unused here. */
    @Srg("func_197965_a")
    public String getClipboardString(long window, Object errorCallback) {
        return GuiScreen.getClipboardString();
    }

    @Srg("func_197960_a")
    public void setClipboardString(long window, String text) {
        GuiScreen.setClipboardString(text);
    }
}
