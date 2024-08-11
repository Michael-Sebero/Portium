package com.michaelsebero.portium.modern.mc.client;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.Display;

/**
 * 1.13's {@code net.minecraft.client.MainWindow}, {@code Minecraft.mainWindow}: the game window's sizes, in pixels
 * and scaled for the GUI. 1.12 keeps them on {@code Minecraft} and {@code ScaledResolution}; this reads them there.
 */
@SideOnly(Side.CLIENT)
public final class MainWindow {

    static final MainWindow INSTANCE = new MainWindow();

    public static MainWindow get() {
        return INSTANCE;
    }

    private MainWindow() {}

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    private static ScaledResolution scaled() {
        return new ScaledResolution(mc());
    }

    /** LWJGL 2 has no window handle to give; 0 is GLFW's "no window". */
    @Srg("func_198092_i")
    public long getHandle() {
        return 0L;
    }

    @Srg("func_198113_j")
    public boolean isFullscreen() {
        return mc().isFullScreen();
    }

    @Srg("func_198077_g")
    public void toggleFullscreen() {
        mc().toggleFullscreen();
    }

    @Srg("func_198109_k")
    public int getFramebufferWidth() {
        return mc().displayWidth;
    }

    @Srg("func_198091_l")
    public int getFramebufferHeight() {
        return mc().displayHeight;
    }

    @Srg("func_198105_m")
    public int getWidth() {
        return mc().displayWidth;
    }

    @Srg("func_198083_n")
    public int getHeight() {
        return mc().displayHeight;
    }

    @Srg("func_198107_o")
    public int getScaledWidth() {
        return scaled().getScaledWidth();
    }

    @Srg("func_198087_p")
    public int getScaledHeight() {
        return scaled().getScaledHeight();
    }

    @Srg("func_198099_q")
    public int getWindowX() {
        return Display.getX();
    }

    @Srg("func_198079_r")
    public int getWindowY() {
        return Display.getY();
    }

    @Srg("func_198100_s")
    public double getGuiScaleFactor() {
        return scaled().getScaleFactor();
    }

    /** The GUI scale a setting gives (0 is "auto"), as 1.12's {@code ScaledResolution} works it out. */
    @Srg("func_198078_c")
    public int calcGuiScale(int setting) {
        Minecraft mc = mc();
        int scale = 1;
        while (scale != setting && scale < mc.displayWidth && scale < mc.displayHeight
                && mc.displayWidth / (scale + 1) >= 320 && mc.displayHeight / (scale + 1) >= 240) {
            scale++;
        }
        if (mc.isUnicode() && scale % 2 != 0) scale++;
        return scale;
    }

    /** 1.12 works out the scale itself whenever it's asked. */
    @Srg("func_198098_h")
    public void updateGuiScale() {}

    @Srg("func_198094_a")
    public void setLogOnGlError() {}
}
