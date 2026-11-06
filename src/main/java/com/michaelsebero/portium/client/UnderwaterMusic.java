package com.michaelsebero.portium.client;

import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.mixin.client.MusicTickerAccessor;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.EnumHelperClient;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * 1.13's underwater music (Shuniji, Dragon Fish, Axolotl): in the overworld's game music, while the player's eyes are
 * under water in an ocean or river, and on until the track ends, as 1.16's {@code getBackgroundMusicSelector} has it.
 */
@SideOnly(Side.CLIENT)
public final class UnderwaterMusic {

    private static MusicTicker.MusicType type;

    @Nullable
    public static MusicTicker.MusicType music(Minecraft mc, MusicTicker.MusicType vanilla) {
        if (vanilla != MusicTicker.MusicType.GAME && vanilla != MusicTicker.MusicType.CREATIVE) return null;
        if (type == null) type = EnumHelperClient.addMusicType("PORTIUM_UNDER_WATER", PortiumSounds.MUSIC_UNDER_WATER, 12000, 24000);
        ISound playing = ((MusicTickerAccessor) mc.getMusicTicker()).portium$getCurrentMusic();
        if (playing != null && playing.getSoundLocation().equals(PortiumSounds.MUSIC_UNDER_WATER.getSoundName()) && mc.getSoundHandler().isSoundPlaying(playing)) {
            return type;
        }
        if (mc.player == null || !mc.player.isInsideOfMaterial(Material.WATER)) return null;
        Biome biome = mc.world.getBiome(new BlockPos(mc.player));
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER) ? type : null;
    }

    private UnderwaterMusic() {}
}
