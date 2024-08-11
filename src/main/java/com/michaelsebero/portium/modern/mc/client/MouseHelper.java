package com.michaelsebero.portium.modern.mc.client;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

/**
 * 1.13's {@code net.minecraft.client.MouseHelper}, {@code Minecraft.mouseHelper}: the cursor in window pixels from
 * the top left, as GLFW gives it, and whether the mouse is held or grabbed. 1.12 reads LWJGL 2's mouse, whose
 * height counts from the bottom.
 */
@SideOnly(Side.CLIENT)
public final class MouseHelper {

    static final MouseHelper INSTANCE = new MouseHelper();

    public static MouseHelper get() {
        return INSTANCE;
    }

    private MouseHelper() {}

    @Srg("func_198024_e")
    public double getMouseX() {
        return Mouse.getX();
    }

    @Srg("func_198026_f")
    public double getMouseY() {
        return Minecraft.getMinecraft().displayHeight - Mouse.getY() - 1;
    }

    @Srg("func_198030_b")
    public boolean isLeftDown() {
        return Mouse.isButtonDown(0);
    }

    @Srg("func_198031_d")
    public boolean isRightDown() {
        return Mouse.isButtonDown(1);
    }

    @Srg("func_198035_h")
    public boolean isMouseGrabbed() {
        return Minecraft.getMinecraft().inGameHasFocus;
    }

    @Srg("func_198034_i")
    public void grabMouse() {
        Minecraft.getMinecraft().setIngameFocus();
    }

    @Srg("func_198032_j")
    public void ungrabMouse() {
        Minecraft.getMinecraft().setIngameNotInFocus();
    }

    /** 1.12 skips the first move after grabbing by itself. */
    @Srg("func_198021_g")
    public void setIgnoreFirstMove() {}

    /** 1.12 turns the player in its own loop. */
    @Srg("func_198028_a")
    public void updatePlayerLook() {}
}
