package com.michaelsebero.portium.modern.client;

import com.michaelsebero.portium.modern.ModernFailures;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/** Shown once before the main menu when some of the 1.13.2 mods couldn't run (see {@link ModernFailures}). */
@SideOnly(Side.CLIENT)
public final class ModernFailureScreen extends GuiScreen {

    private static boolean shown;
    private final GuiScreen next;
    private final List<String> lines = new ArrayList<>();

    private ModernFailureScreen(GuiScreen next) {
        this.next = next;
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onOpen(GuiOpenEvent event) {
                if (shown || !(event.getGui() instanceof GuiMainMenu) || ModernFailures.list().isEmpty()) return;
                shown = true;
                event.setGui(new ModernFailureScreen(event.getGui()));
            }
        });
    }

    @Override
    public void initGui() {
        this.lines.clear();
        int width = Math.min(this.width - 40, 420);
        for (String failure : ModernFailures.list()) this.lines.addAll(this.fontRenderer.listFormattedStringToWidth("- " + failure, width));
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, Math.min(this.height - 30, 70 + this.lines.size() * 10 + 10), "Continue"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        proceed();
    }

    /** Goes on to the screen the notice came before. */
    public void proceed() {
        this.mc.displayGuiScreen(this.next);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(this.fontRenderer, "Some 1.13.2 mods couldn't fully run on 1.12.2", this.width / 2, 30, 0xFFFF55);
        drawCenteredString(this.fontRenderer, "The game works without the parts below; the log has the details.", this.width / 2, 45, 0xAAAAAA);
        int y = 65;
        int left = this.width / 2 - Math.min(this.width - 40, 420) / 2;
        for (String line : this.lines) {
            if (y > this.height - 40) break;
            this.fontRenderer.drawString(line, left, y, 0xFFFFFF);
            y += 10;
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
