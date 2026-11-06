package com.michaelsebero.portium.client;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.block.workstation.BlockComposter;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.proxy.ParticleKind;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * 1.14+'s world events (1.12's playEvent ids it doesn't know), played on clients as 1.14's WorldRenderer does. Sent
 * with the ordinary {@code World.playEvent}, so the server needs nothing new; {@code MixinRenderGlobalEvents} routes
 * them here.
 */
public final class WorldEvents116 {

    /** Whether the event was one of these. */
    public static boolean play(WorldClient world, int type, BlockPos pos, int data) {
        if (type == BlockComposter.FILL_EVENT) {
            composterFill(world, pos, data > 0);
            return true;
        }
        if (type == com.michaelsebero.portium.inventory.ContainerGrindstone.GRIND_EVENT) {
            world.playSound(pos, PortiumSounds.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 1.0F, world.rand.nextFloat() * 0.1F + 0.9F, false);
            return true;
        }
        if (type == com.michaelsebero.portium.block.village.BlockLectern.PAGE_TURN_EVENT) {
            world.playSound(pos, PortiumSounds.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1.0F, world.rand.nextFloat() * 0.1F + 0.9F, false);
            return true;
        }
        return false;
    }

    /** 1.14's ComposterBlock.playEvent: the fill sound and ten specks above the compost. */
    private static void composterFill(WorldClient world, BlockPos pos, boolean raised) {
        IBlockState state = world.getBlockState(pos);
        world.playSound(pos.getX(), pos.getY(), pos.getZ(), raised ? PortiumSounds.BLOCK_COMPOSTER_FILL_SUCCESS : PortiumSounds.BLOCK_COMPOSTER_FILL,
                SoundCategory.BLOCKS, 1.0F, 1.0F, false);
        double fill = BlockComposter.fillHeight(state) + 0.03125D;
        Random random = world.rand;
        for (int i = 0; i < 10; i++) {
            double dx = random.nextGaussian() * 0.02D;
            double dy = random.nextGaussian() * 0.02D;
            double dz = random.nextGaussian() * 0.02D;
            Portium.proxy.spawnParticle(ParticleKind.COMPOSTER, world, (double) pos.getX() + 0.13124999403953552D + 0.737500011920929D * (double) random.nextFloat(),
                    (double) pos.getY() + fill + (double) random.nextFloat() * (1.0D - fill),
                    (double) pos.getZ() + 0.13124999403953552D + 0.737500011920929D * (double) random.nextFloat(), dx, dy, dz);
        }
    }

    private WorldEvents116() {}
}
