package com.michaelsebero.portium.world.spawn;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.passive.EntityCat;
import com.michaelsebero.portium.init.PortiumEntities;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.Village;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Random;

/**
 * 1.14's cat spawner: once a minute, near a random player, a stray cat comes to a village with more than four homes
 * (1.12's village doors stand in for 1.14's claimed beds) if fewer than five cats live there, or to a witch hut that has
 * none (it comes out black there).
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class CatSpawner {

    private static int ticksUntilSpawn;

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) event.world;
        if (world.provider.getDimensionType() != DimensionType.OVERWORLD || !world.getGameRules().getBoolean("doMobSpawning")) return;
        if (!PortiumEntities.isRegistered(EntityCat.class) || !world.getWorldInfo().isInitialized()) return;
        if (!MobVersions.spawnsIn(world, EntityCat.class)) return;
        if (--ticksUntilSpawn > 0) return;
        ticksUntilSpawn = 1200;
        List<EntityPlayer> players = world.playerEntities;
        if (players.isEmpty()) return;
        Random random = world.rand;
        EntityPlayer player = players.get(random.nextInt(players.size()));
        int dx = (8 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
        int dz = (8 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
        BlockPos pos = new BlockPos(player).add(dx, 0, dz);
        if (!world.isAreaLoaded(pos.add(-10, -10, -10), pos.add(10, 10, 10))) return;
        if (!WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, world, pos)) return;
        Village village = world.getVillageCollection().getNearestVillage(pos, 32);
        if (village != null && village.isBlockPosWithinSqVillageRadius(pos)) {
            if (village.getNumVillageDoors() > 4 && world.getEntitiesWithinAABB(EntityCat.class, new AxisAlignedBB(pos).grow(48.0D, 8.0D, 48.0D)).size() < 5) {
                spawnCat(world, pos);
            }
        } else if (EntityCat.isInSwampHut(world, pos) && world.getEntitiesWithinAABB(EntityCat.class, new AxisAlignedBB(pos).grow(16.0D, 8.0D, 16.0D)).isEmpty()) {
            spawnCat(world, pos);
        }
    }

    private static void spawnCat(WorldServer world, BlockPos pos) {
        EntityCat cat = new EntityCat(world);
        cat.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        cat.onInitialSpawn(world.getDifficultyForLocation(pos), null);
        world.spawnEntity(cat);
    }

    private CatSpawner() {}
}
