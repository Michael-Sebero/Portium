package com.michaelsebero.portium.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.michaelsebero.portium.compat.LegacyBiomes;
import com.michaelsebero.portium.world.buffet.BuffetSettings;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.13's "Buffet world customization" screen: a button cycling the world generator (surface, caves, floating
 * islands) and a list of every biome, one of which must be picked before Done.
 */
@SideOnly(Side.CLIENT)
public class GuiCreateBuffetWorld extends GuiScreen {

    private static final BuffetSettings.Generator[] GENERATORS = BuffetSettings.Generator.values();
    private static final int DONE = 0;
    private static final int CANCEL = 1;
    private static final int GENERATOR = 2;

    private final GuiCreateWorld parent;
    private final List<Biome> biomes = new ArrayList<>();
    private int generator;
    @Nullable
    private Biome selected;
    private BiomeList list;
    private GuiButton generatorButton;
    private GuiButton doneButton;

    public GuiCreateBuffetWorld(GuiCreateWorld parent, String options) {
        this.parent = parent;
        for (Biome biome : Biome.REGISTRY) this.biomes.add(biome);
        this.read(options);
    }

    /** Picks up the generator and biome a previous visit chose. */
    private void read(String options) {
        if (options == null || options.isEmpty()) return;
        try {
            JsonElement parsed = new JsonParser().parse(options);
            if (!parsed.isJsonObject()) return;
            JsonObject json = parsed.getAsJsonObject();
            if (json.has("chunk_generator") && json.getAsJsonObject("chunk_generator").has("type")) {
                String type = json.getAsJsonObject("chunk_generator").get("type").getAsString();
                for (int i = 0; i < GENERATORS.length; i++) {
                    if (GENERATORS[i].id.equals(type)) this.generator = i;
                }
            }
            if (json.has("biome_source") && json.getAsJsonObject("biome_source").has("options")) {
                JsonObject sourceOptions = json.getAsJsonObject("biome_source").getAsJsonObject("options");
                if (sourceOptions.has("biomes") && sourceOptions.getAsJsonArray("biomes").size() > 0) {
                    String id = LegacyBiomes.toLegacy(sourceOptions.getAsJsonArray("biomes").get(0).getAsString());
                    this.selected = ForgeRegistries.BIOMES.getValue(new ResourceLocation(id));
                }
            }
        } catch (RuntimeException ignored) {
            // Settings from elsewhere that don't fit the screen: start afresh.
        }
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.generatorButton = this.addButton(new GuiButton(GENERATOR, (this.width - 200) / 2, 40, 200, 20, this.generatorLabel()));
        this.list = new BiomeList();
        this.doneButton = this.addButton(new GuiButton(DONE, this.width / 2 - 155, this.height - 28, 150, 20, I18n.format("gui.done")));
        this.addButton(new GuiButton(CANCEL, this.width / 2 + 5, this.height - 28, 150, 20, I18n.format("gui.cancel")));
        this.updateButtons();
    }

    private String generatorLabel() {
        return I18n.format("createWorld.customize.buffet.generatortype") + " " + I18n.format("generator." + GENERATORS[this.generator].id.replace(':', '.'));
    }

    private void updateButtons() {
        this.doneButton.enabled = this.selected != null;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case GENERATOR:
                this.generator = (this.generator + 1) % GENERATORS.length;
                this.generatorButton.displayString = this.generatorLabel();
                break;
            case DONE:
                if (this.selected == null) return;
                this.parent.chunkProviderSettingsJson = BuffetSettings.write(GENERATORS[this.generator], this.selected);
                this.mc.displayGuiScreen(this.parent);
                break;
            case CANCEL:
                this.mc.displayGuiScreen(this.parent);
                break;
            default:
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.list.handleMouseInput();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.list.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRenderer, I18n.format("createWorld.customize.buffet.title"), this.width / 2, 8, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer, I18n.format("createWorld.customize.buffet.generator"), this.width / 2, 30, 0xA0A0A0);
        this.drawCenteredString(this.fontRenderer, I18n.format("createWorld.customize.buffet.biome"), this.width / 2, 68, 0xA0A0A0);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** A biome's 1.13 name, or its 1.12 one for biomes 1.13 has no name for. */
    private static String nameOf(Biome biome) {
        String key = "biome." + LegacyBiomes.toModern(biome.getRegistryName()).replace(':', '.');
        return I18n.hasKey(key) ? I18n.format(key) : biome.getBiomeName();
    }

    private final class BiomeList extends GuiSlot {

        BiomeList() {
            super(GuiCreateBuffetWorld.this.mc, GuiCreateBuffetWorld.this.width, GuiCreateBuffetWorld.this.height, 80, GuiCreateBuffetWorld.this.height - 37, 16);
        }

        @Override
        protected int getSize() {
            return GuiCreateBuffetWorld.this.biomes.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
            GuiCreateBuffetWorld.this.selected = GuiCreateBuffetWorld.this.biomes.get(index);
            GuiCreateBuffetWorld.this.updateButtons();
        }

        @Override
        protected boolean isSelected(int index) {
            return GuiCreateBuffetWorld.this.biomes.get(index) == GuiCreateBuffetWorld.this.selected;
        }

        @Override
        protected void drawBackground() {}

        @Override
        protected void drawSlot(int index, int x, int y, int height, int mouseX, int mouseY, float partialTicks) {
            GuiCreateBuffetWorld.this.drawString(GuiCreateBuffetWorld.this.fontRenderer, nameOf(GuiCreateBuffetWorld.this.biomes.get(index)), x + 5, y + 2, 0xFFFFFF);
        }
    }
}
