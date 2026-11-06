package com.michaelsebero.portium.world.spawn;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.merchant.EntityTraderLlama;
import com.michaelsebero.portium.entity.merchant.EntityWanderingTrader;
import com.michaelsebero.portium.entity.raid.RaiderSpawns;
import com.michaelsebero.portium.init.PortiumEntities;
import com.michaelsebero.portium.world.ITraderSpawnInfo;
import com.michaelsebero.portium.world.PortiumGameRules;
import com.michaelsebero.portium.world.Villages116;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 1.14's wandering trader spawner: each in-game day, a chance (25%, rising by 25 to 75% each day it fails) that a
 * trader and two llamas turn up within 48 blocks of a random player (or of the nearest village meeting point), sent to
 * wander to that point and leave after 40 minutes. Its countdown and chance live in level.dat, as in 1.16.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class WanderingTraderSpawner {

    private static final Random RANDOM = new Random();
    @Nullable
    private static WanderingTraderSpawner instance;

    private final WorldServer world;
    private int tickDelay = 1200;
    private int spawnDelay;
    private int spawnChance;

    private WanderingTraderSpawner(WorldServer world) {
        this.world = world;
        ITraderSpawnInfo info = ITraderSpawnInfo.of(world.getWorldInfo());
        this.spawnDelay = info.portium$getTraderSpawnDelay();
        this.spawnChance = info.portium$getTraderSpawnChance();
        if (this.spawnDelay == 0 && this.spawnChance == 0) {
            this.spawnDelay = 24000;
            info.portium$setTraderSpawnDelay(this.spawnDelay);
            this.spawnChance = 25;
            info.portium$setTraderSpawnChance(this.spawnChance);
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld() instanceof WorldServer && event.getWorld().provider.getDimensionType() == DimensionType.OVERWORLD) {
            instance = new WanderingTraderSpawner((WorldServer) event.getWorld());
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (instance != null && instance.world == event.getWorld()) instance = null;
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || instance == null || instance.world != event.world) return;
        if (!PortiumEntities.isRegistered(EntityWanderingTrader.class) || !MobVersions.spawnsIn(event.world, EntityWanderingTrader.class)) return;
        instance.tick();
    }

    private int tick() {
        if (!this.world.getGameRules().getBoolean(PortiumGameRules.DO_TRADER_SPAWNING)) return 0;
        if (--this.tickDelay > 0) return 0;
        this.tickDelay = 1200;
        ITraderSpawnInfo info = ITraderSpawnInfo.of(this.world.getWorldInfo());
        this.spawnDelay -= 1200;
        info.portium$setTraderSpawnDelay(this.spawnDelay);
        if (this.spawnDelay > 0) return 0;
        this.spawnDelay = 24000;
        if (!this.world.getGameRules().getBoolean("doMobSpawning")) return 0;
        int chance = this.spawnChance;
        this.spawnChance = MathHelper.clamp(this.spawnChance + 25, 25, 75);
        info.portium$setTraderSpawnChance(this.spawnChance);
        if (RANDOM.nextInt(100) > chance) return 0;
        if (this.spawn()) {
            this.spawnChance = 25;
            return 1;
        }
        return 0;
    }

    private boolean spawn() {
        EntityPlayer player = this.randomPlayer();
        if (player == null) return true;
        if (RANDOM.nextInt(10) != 0) return false;
        return spawnNear(this.world, player) != null;
    }

    @Nullable
    private EntityPlayer randomPlayer() {
        List<EntityPlayer> alive = new ArrayList<>();
        for (EntityPlayer player : this.world.playerEntities) {
            if (player.isEntityAlive()) alive.add(player);
        }
        return alive.isEmpty() ? null : alive.get(this.world.rand.nextInt(alive.size()));
    }

    /** The spawn itself, once the day's roll says so; also DevScript's {@code spawntrader}. */
    @Nullable
    public static EntityWanderingTrader spawnNear(WorldServer world, EntityPlayer player) {
        BlockPos playerPos = new BlockPos(player);
        BlockPos meeting = Villages116.meetingPoint(world, playerPos, 48);
        BlockPos target = meeting != null ? meeting : playerPos;
        BlockPos pos = findSpawnPositionNear(world, target, 48);
        if (pos == null || !hasEnoughSpace(world, pos) || world.getBiome(pos) == Biomes.VOID) return null;
        EntityWanderingTrader trader = new EntityWanderingTrader(world);
        place(world, trader, pos);
        for (int i = 0; i < 2; ++i) spawnLlama(world, trader, 4);
        ITraderSpawnInfo.of(world.getWorldInfo()).portium$setTraderId(trader.getUniqueID());
        trader.setDespawnDelay(48000);
        trader.setWanderTarget(target);
        trader.setHomePosAndDistance(target, 16);
        return trader;
    }

    private static void spawnLlama(WorldServer world, EntityWanderingTrader trader, int distance) {
        BlockPos pos = findSpawnPositionNear(world, new BlockPos(trader), distance);
        if (pos == null) return;
        EntityTraderLlama llama = new EntityTraderLlama(world);
        place(world, llama, pos);
        llama.makeAdult();
        llama.setLeashHolder(trader, true);
    }

    /** 1.14's {@code EntityType.spawn} for the EVENT reason: centred on the block, random facing, set up, added. */
    private static void place(WorldServer world, EntityLiving entity, BlockPos pos) {
        entity.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, MathHelper.wrapDegrees(world.rand.nextFloat() * 360.0F), 0.0F);
        entity.rotationYawHead = entity.rotationYaw;
        entity.renderYawOffset = entity.rotationYaw;
        RaiderSpawns.spawning(RaiderSpawns.Reason.EVENT, () -> entity.onInitialSpawn(world.getDifficultyForLocation(pos), null));
        world.spawnEntity(entity);
    }

    @Nullable
    private static BlockPos findSpawnPositionNear(WorldServer world, BlockPos around, int distance) {
        for (int i = 0; i < 10; ++i) {
            int x = around.getX() + RANDOM.nextInt(distance * 2) - distance;
            int z = around.getZ() + RANDOM.nextInt(distance * 2) - distance;
            BlockPos pos = world.getHeight(new BlockPos(x, 0, z));
            if (WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, world, pos)) return pos;
        }
        return null;
    }

    /** Two blocks of head room over a 2x2 footprint, nothing solid. */
    private static boolean hasEnoughSpace(WorldServer world, BlockPos pos) {
        for (BlockPos at : BlockPos.getAllInBoxMutable(pos, pos.add(1, 2, 1))) {
            IBlockState state = world.getBlockState(at);
            if (state.getCollisionBoundingBox(world, at) != null) return false;
        }
        return true;
    }
}
