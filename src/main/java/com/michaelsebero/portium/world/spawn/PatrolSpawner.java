package com.michaelsebero.portium.world.spawn;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.monster.EntityPillager;
import com.michaelsebero.portium.entity.raid.IRaider;
import com.michaelsebero.portium.entity.raid.RaiderSpawns;
import com.michaelsebero.portium.entity.raid.RaiderState;
import com.michaelsebero.portium.entity.raid.Raiders116;
import com.michaelsebero.portium.init.PortiumEntities;
import com.michaelsebero.portium.mixin.accessor.WorldAccessor;
import com.michaelsebero.portium.world.PortiumGameRules;
import com.michaelsebero.portium.world.Villages116;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.Random;

/**
 * 1.14's patrol spawner: every 10-11 minutes of daylight from day five, one time in five, a patrol of pillagers (one
 * more per point of local difficulty, rounded up) appears 24-47 blocks from a random player who isn't near a village,
 * led by a banner-carrying captain.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PatrolSpawner {

    private static int cooldown;

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) event.world;
        if (world.provider.getDimensionType() != DimensionType.OVERWORLD || !world.getGameRules().getBoolean("doMobSpawning")) return;
        if (!((WorldAccessor) world).portium$spawnsHostileMobs() || !PortiumEntities.isRegistered(EntityPillager.class)) return;
        if (!MobVersions.spawnsIn(world, EntityPillager.class)) return;
        if (!world.getGameRules().getBoolean(PortiumGameRules.DO_PATROL_SPAWNING)) return;
        spawn(world);
    }

    private static int spawn(WorldServer world) {
        Random random = world.rand;
        if (--cooldown > 0) return 0;
        cooldown += 12000 + random.nextInt(1200);
        long day = world.getWorldTime() / 24000L;
        if (day < 5L || !world.isDaytime() || random.nextInt(5) != 0) return 0;
        List<EntityPlayer> players = world.playerEntities;
        if (players.isEmpty()) return 0;
        EntityPlayer player = players.get(random.nextInt(players.size()));
        if (player.isSpectator() || Villages116.isNearVillage(world, new BlockPos(player), 2)) return 0;
        return spawnPatrolNear(world, player);
    }

    /** The patrol itself, once the timers and the roll say so; also run by DevScript's {@code spawnpatrol}. */
    public static int spawnPatrolNear(WorldServer world, EntityPlayer player) {
        Random random = world.rand;
        int dx = (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
        int dz = (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(new BlockPos(player).add(dx, 0, dz));
        if (!world.isAreaLoaded(pos.add(-10, -10, -10), pos.add(10, 10, 10))) return 0;
        if (BiomeDictionary.hasType(world.getBiome(pos), BiomeDictionary.Type.MUSHROOM)) return 0;
        int spawned = 0;
        int size = (int) Math.ceil(world.getDifficultyForLocation(pos).getAdditionalDifficulty()) + 1;
        for (int i = 0; i < size; ++i) {
            ++spawned;
            pos.setY(Raiders116.surface(world, pos).getY());
            if (i == 0) {
                if (!spawnPatroller(world, pos.toImmutable(), true)) break;
            } else {
                spawnPatroller(world, pos.toImmutable(), false);
            }
            pos.setPos(pos.getX() + random.nextInt(5) - random.nextInt(5), pos.getY(), pos.getZ() + random.nextInt(5) - random.nextInt(5));
        }
        return spawned;
    }

    private static boolean spawnPatroller(WorldServer world, BlockPos pos, boolean leader) {
        IBlockState state = world.getBlockState(pos);
        if (!WorldEntitySpawner.isValidEmptySpawnBlock(state)) return false;
        if (world.getLightFor(EnumSkyBlock.BLOCK, pos) > 8 || world.getDifficulty() == EnumDifficulty.PEACEFUL) return false;
        BlockPos below = pos.down();
        if (!world.getBlockState(below).getBlock().canCreatureSpawn(world.getBlockState(below), world, below, EntityLiving.SpawnPlacementType.ON_GROUND)) {
            return false;
        }
        EntityPillager pillager = new EntityPillager(world);
        RaiderState raider = IRaider.state(pillager);
        if (leader) {
            // As in 1.14, before the patroller has a position: the first target is picked around the world's origin.
            raider.setLeader(true);
            raider.resetPatrolTarget(pillager);
        }
        pillager.setPosition(pos.getX(), pos.getY(), pos.getZ());
        if (ForgeEventFactory.canEntitySpawn(pillager, world, pos.getX(), pos.getY(), pos.getZ(), false) == Event.Result.DENY) return false;
        RaiderSpawns.spawning(RaiderSpawns.Reason.PATROL, () -> pillager.onInitialSpawn(world.getDifficultyForLocation(pos), null));
        world.spawnEntity(pillager);
        return true;
    }

    private PatrolSpawner() {}
}
