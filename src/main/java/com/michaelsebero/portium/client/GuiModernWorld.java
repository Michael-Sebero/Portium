package com.michaelsebero.portium.client;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.compat.ModernSaves;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Shown instead of 1.12's version warning when opening a world 1.13 saved: Portium can load it, converting it as it
 * goes, and offers to back it up first, as 1.14 does for worlds from other versions.
 */
@SideOnly(Side.CLIENT)
public class GuiModernWorld extends GuiScreen {

    private static final int BACKUP = 0;
    private static final int LOAD = 1;
    private static final int CANCEL = 2;

    private final GuiScreen parent;
    private final String folder;
    private final String version;
    private final Runnable load;
    private String error;

    public GuiModernWorld(GuiScreen parent, String folder, String version, Runnable load) {
        this.parent = parent;
        this.folder = folder;
        this.version = version;
        this.load = load;
    }

    /** Whether Portium's config allows converting such worlds; if not, the screen only explains and goes back. */
    private static boolean allowed() {
        return PortiumConfig.compatibility.modernWorlds;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int y = this.height / 6 + 96;
        if (!allowed()) {
            this.addButton(new GuiButton(CANCEL, this.width / 2 - 75, y + 24, 150, 20, I18n.format("gui.back")));
            return;
        }
        this.addButton(new GuiButton(BACKUP, this.width / 2 - 155, y, 150, 20, I18n.format("selectWorld.portium.modern.backup")));
        this.addButton(new GuiButton(LOAD, this.width / 2 + 5, y, 150, 20, I18n.format("selectWorld.portium.modern.load")));
        this.addButton(new GuiButton(CANCEL, this.width / 2 - 75, y + 24, 150, 20, I18n.format("gui.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BACKUP:
                try {
                    File zip = ModernSaves.backup(new File(this.mc.gameDir, "saves"), this.folder);
                    Portium.LOGGER.info("Backed up {} to {}", this.folder, zip);
                } catch (IOException | RuntimeException e) {
                    Portium.LOGGER.error("Couldn't back up {}", this.folder, e);
                    this.error = I18n.format("selectWorld.portium.modern.backupFailed", e.getMessage());
                    return;
                }
                this.load.run();
                break;
            case LOAD:
                this.load.run();
                break;
            case CANCEL:
                this.mc.displayGuiScreen(this.parent);
                break;
            default:
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, I18n.format("selectWorld.portium.modern.title", this.version), this.width / 2, 70, 0xFFFFFF);
        String info = allowed() ? I18n.format("selectWorld.portium.modern.info", this.version) : I18n.format("selectWorld.portium.modern.disabled");
        List<String> lines = this.fontRenderer.listFormattedStringToWidth(info, this.width - 50);
        for (int i = 0; i < lines.size(); i++) this.drawCenteredString(this.fontRenderer, lines.get(i), this.width / 2, 90 + i * this.fontRenderer.FONT_HEIGHT, 0xA0A0A0);
        if (this.error != null) this.drawCenteredString(this.fontRenderer, TextFormatting.RED + this.error, this.width / 2, this.height / 6 + 150, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
