package com.michaelsebero.portium.modern.adapter;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.client.gui.IGuiEventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * What 1.13 code makes and extends of {@code GuiButton}: 1.12's button with 1.13's, which reacts to clicks itself
 * ({@code onClick}) rather than through its screen, and draws with {@code render}.
 */
@SideOnly(Side.CLIENT)
public class ModernGuiButton extends GuiButton implements IGuiEventListener {

    public ModernGuiButton(int id, int x, int y, String text) {
        super(id, x, y, text);
    }

    public ModernGuiButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    /** 1.13's drawing; 1.12's draws the button. */
    @Srg("func_194828_a")
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY, partialTicks);
    }

    @Srg("func_194829_a")
    public void onClick(double mouseX, double mouseY) {}

    @Srg("func_194831_b")
    public void onRelease(double mouseX, double mouseY) {}

    @Srg("func_194827_a")
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {}

    @Srg("func_199400_c")
    protected boolean isPressable(double mouseX, double mouseY) {
        return this.enabled && this.visible && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isPressable(mouseX, mouseY)) return false;
        playPressSound(Minecraft.getMinecraft().getSoundHandler());
        onClick(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        onRelease(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 0 || !isPressable(mouseX, mouseY)) return false;
        onDrag(mouseX, mouseY, deltaX, deltaY);
        return true;
    }

    /** A 1.12 screen pressing it (it was put in one, or 1.12 code asks): 1.13's click. */
    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        boolean pressed = super.mousePressed(mc, mouseX, mouseY);
        if (pressed) onClick(mouseX, mouseY);
        return pressed;
    }
}
