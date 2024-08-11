package com.michaelsebero.portium.world.buffet;

import com.michaelsebero.portium.client.GuiCreateBuffetWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.13's Buffet world type: one generator (surface, caves or floating islands) and the biomes of its choice,
 * customized on the Create World screen. It registers like any mod's world type, taking a free id, so 1.12's world
 * types and other mods' keep theirs; worlds store it by name ({@code buffet}) as 1.13 does.
 */
public class WorldTypeBuffet extends WorldType {

    public static WorldTypeBuffet buffet;

    private WorldTypeBuffet() {
        super("buffet");
    }

    public static void register() {
        if (!com.michaelsebero.portium.PortiumConfig.worldGen.buffetWorldType) return;
        if (buffet == null) buffet = new WorldTypeBuffet();
    }

    @Override
    public BiomeProvider getBiomeProvider(World world) {
        return BuffetSettings.parse(world.getWorldInfo().getGeneratorOptions()).createBiomeProvider(world);
    }

    @Override
    public IChunkGenerator getChunkGenerator(World world, String generatorOptions) {
        return BuffetSettings.parse(generatorOptions).createChunkGenerator(world);
    }

    @Override
    public boolean isCustomizable() {
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void onCustomizeButton(Minecraft mc, GuiCreateWorld screen) {
        mc.displayGuiScreen(new GuiCreateBuffetWorld(screen, screen.chunkProviderSettingsJson));
    }
}
