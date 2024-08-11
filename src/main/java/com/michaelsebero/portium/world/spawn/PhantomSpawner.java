package com.michaelsebero.portium.world.spawn;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.monster.EntityPhantom;
import com.michaelsebero.portium.init.PortiumEntities;
import com.michaelsebero.portium.init.PortiumStats;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

/**
 * Phantom spawning (1.13). Every one to two minutes at night, each player out under the open sky who hasn't slept in
 * three in-game days has a growing chance of phantoms appearing 20-34 blocks above them. More appear on harder
 * difficulties. Sleeping or dying resets the "time since rest" statistic this is based on.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PhantomSpawner {

    private static int ticksUntilSpawn;

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) event.world;
        if (world.provider.getDimensionType() != DimensionType.OVERWORLD || !world.getGameRules().getBoolean("doMobSpawning")) return;
        if (world.getDifficulty() == EnumDifficulty.PEACEFUL) return;
        if (!PortiumConfig.mechanics.phantomSpawning || !PortiumEntities.isRegistered(EntityPhantom.class)) return;
        tick(world);
    }

    private static void tick(WorldServer world) {
        Random random = world.rand;
        if (--ticksUntilSpawn > 0) return;
        ticksUntilSpawn += (60 + random.nextInt(60)) * 20;
        if (world.getSkylightSubtracted() < 5 && world.provider.hasSkyLight()) return;
        for (EntityPlayer player : world.playerEntities) {
            if (player.isSpectator() || !(player instanceof EntityPlayerMP)) continue;
            BlockPos pos = new BlockPos(player);
            if (world.provider.hasSkyLight() && (pos.getY() < world.getSeaLevel() || !world.canSeeSky(pos))) continue;
            DifficultyInstance difficulty = world.getDifficultyForLocation(pos);
            if (!difficulty.isHarderThan(random.nextFloat() * 3.0F)) continue;
            int sinceRest = MathHelper.clamp(((EntityPlayerMP) player).getStatFile().readStat(PortiumStats.TIME_SINCE_REST), 1, Integer.MAX_VALUE);
            if (random.nextInt(sinceRest) < 72000) continue;
            BlockPos spawnPos = pos.up(20 + random.nextInt(15)).east(-10 + random.nextInt(21)).south(-10 + random.nextInt(21));
            IBlockState state = world.getBlockState(spawnPos);
            if (!WorldEntitySpawner.isValidEmptySpawnBlock(state)) continue;
            IEntityLivingData data = null;
            int count = 1 + random.nextInt(world.getDifficulty().getId() + 1);
            for (int i = 0; i < count; ++i) {
                EntityPhantom phantom = new EntityPhantom(world);
                phantom.moveToBlockPosAndAngles(spawnPos, 0.0F, 0.0F);
                data = phantom.onInitialSpawn(difficulty, data);
                world.spawnEntity(phantom);
            }
        }
    }

    /** Counts up the time since the player last slept; it stays at zero while they're in bed. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (player.isPlayerSleeping()) {
            player.takeStat(PortiumStats.TIME_SINCE_REST);
        } else if (player.isEntityAlive()) {
            player.addStat(PortiumStats.TIME_SINCE_REST);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP) ((EntityPlayerMP) event.getEntityLiving()).takeStat(PortiumStats.TIME_SINCE_REST);
    }

    private PhantomSpawner() {}
}
