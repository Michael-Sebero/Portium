package com.michaelsebero.portium.client;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.block.BlockBubbleColumn;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;

/**
 * 1.13 underwater sounds for the local player: a splash going under and coming up, a looping ambience that fades
 * in and out, occasional distant creaks and animal calls, and the rush of entering a bubble column.
 */
public final class UnderwaterAmbience {

    private static boolean wasUnderwater;
    private static LoopSound loop;
    private static int bubbleColumn;

    public static void tick(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) {
            wasUnderwater = false;
            loop = null;
            return;
        }
        boolean underwater = player.isInsideOfMaterial(Material.WATER);
        if (PortiumConfig.visuals.underwaterAmbience) {
            if (underwater && !wasUnderwater) {
                play(mc, PortiumSounds.AMBIENT_UNDERWATER_ENTER);
                if (loop == null || loop.isDonePlaying()) {
                    loop = new LoopSound(player);
                    mc.getSoundHandler().playSound(loop);
                }
            } else if (!underwater && wasUnderwater) {
                play(mc, PortiumSounds.AMBIENT_UNDERWATER_EXIT);
            }
            if (underwater) {
                float roll = player.getRNG().nextFloat();
                if (roll < 1.0E-4F) play(mc, PortiumSounds.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE);
                else if (roll < 0.001F) play(mc, PortiumSounds.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE);
                else if (roll < 0.01F) play(mc, PortiumSounds.AMBIENT_UNDERWATER_LOOP_ADDITIONS);
            }
        }
        wasUnderwater = underwater;

        IBlockState atEyes = mc.world.getBlockState(new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ));
        int column = 0;
        if (atEyes.getBlock() == PortiumBlocks.BUBBLE_COLUMN) column = atEyes.getValue(BlockBubbleColumn.DRAG) ? 2 : 1;
        if (column != 0 && column != bubbleColumn) {
            SoundEvent sound = column == 1 ? PortiumSounds.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE : PortiumSounds.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE;
            mc.world.playSound(player.posX, player.posY, player.posZ, sound, SoundCategory.BLOCKS, 0.7F, 1.0F, false);
        }
        bubbleColumn = column;
    }

    private static void play(Minecraft mc, SoundEvent sound) {
        mc.getSoundHandler().playSound(new PositionedSoundRecord(sound.getSoundName(), SoundCategory.AMBIENT, 1.0F, 1.0F,
                false, 0, ISound.AttenuationType.NONE, 0.0F, 0.0F, 0.0F));
    }

    /** The underwater loop, fading over two seconds as the player dives or surfaces. */
    private static final class LoopSound extends MovingSound {

        private final EntityPlayerSP player;
        private int fade;

        LoopSound(EntityPlayerSP player) {
            super(PortiumSounds.AMBIENT_UNDERWATER_LOOP, SoundCategory.AMBIENT);
            this.player = player;
            this.repeat = true;
            this.repeatDelay = 0;
            this.volume = 1.0F;
            this.attenuationType = AttenuationType.NONE;
        }

        @Override
        public void update() {
            if (this.player.isDead || !PortiumConfig.visuals.underwaterAmbience) {
                this.donePlaying = true;
                return;
            }
            if (this.player.isInsideOfMaterial(Material.WATER)) ++this.fade;
            else this.fade -= 2;
            this.fade = Math.min(this.fade, 40);
            this.volume = Math.max(0.0F, Math.min(this.fade / 40.0F, 1.0F));
            if (this.fade < 0) this.donePlaying = true;
        }
    }

    private UnderwaterAmbience() {}
}
