package com.michaelsebero.portium.entity.raid;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.villager.VillagerEvents116;
import com.michaelsebero.portium.init.PortiumPotions;
import com.michaelsebero.portium.world.PortiumGameRules;
import com.michaelsebero.portium.world.Villages116;
import com.michaelsebero.portium.world.poi.PoiManager116;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.14's raid manager for a world (saved as raids.dat): ticks its raids, starts or raises one when a player with Bad
 * Omen stands in a village, and takes raiders out of their raid when they die. Raids happen in 1.16.5 worlds, in the
 * overworld, unless the disableRaids rule is on.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class RaidManager116 extends WorldSavedData {

    private static final String NAME = "raids";

    private final Map<Integer, Raid116> raids = new LinkedHashMap<>();
    private int nextId = 1;
    private long tick;
    @Nullable
    private WorldServer world;
    /** Raids read from disk, made once the world is known. */
    private final List<NBTTagCompound> unread = new ArrayList<>();

    public RaidManager116(String name) {
        super(name);
    }

    /** The manager of a world where raids can happen, or null. */
    @Nullable
    public static RaidManager116 get(World world) {
        if (!(world instanceof WorldServer) || world.provider.getDimension() != 0 || !VillagerEvents116.isActive(world)) return null;
        RaidManager116 manager = (RaidManager116) world.getPerWorldStorage().getOrLoadData(RaidManager116.class, NAME);
        if (manager == null) {
            manager = new RaidManager116(NAME);
            world.getPerWorldStorage().setData(NAME, manager);
        }
        if (manager.world == null) {
            manager.world = (WorldServer) world;
            for (NBTTagCompound tag : manager.unread) {
                Raid116 raid = new Raid116(manager.world, tag);
                manager.raids.put(raid.getId(), raid);
            }
            manager.unread.clear();
        }
        return manager;
    }

    @Nullable
    public Raid116 getRaid(int id) {
        return this.raids.get(id);
    }

    /** The raid nearest a position within a distance, if any. */
    @Nullable
    public Raid116 findRaid(BlockPos pos, double distance) {
        Raid116 best = null;
        double bestSq = distance * distance;
        for (Raid116 raid : this.raids.values()) {
            double sq = raid.getCenter().distanceSq(pos);
            if (raid.isActive() && sq < bestSq) {
                best = raid;
                bestSq = sq;
            }
        }
        return best;
    }

    private void tick() {
        this.tick++;
        boolean disabled = this.world.getGameRules().getBoolean(PortiumGameRules.DISABLE_RAIDS);
        Iterator<Raid116> it = this.raids.values().iterator();
        while (it.hasNext()) {
            Raid116 raid = it.next();
            if (disabled) raid.stop();
            if (raid.isStopped()) {
                it.remove();
                this.markDirty();
            } else {
                raid.tick();
            }
        }
        if (this.tick % 200L == 0L) this.markDirty();
    }

    /**
     * A player with Bad Omen in a village: a raid starts there (centred on the village's taken points within 64
     * blocks), or an unstarted one there grows by the omen's level.
     */
    private void badOmenTick(EntityPlayer player) {
        if (player.isSpectator() || this.world.getDifficulty() == EnumDifficulty.PEACEFUL) return;
        if (this.world.getGameRules().getBoolean(PortiumGameRules.DISABLE_RAIDS)) return;
        BlockPos pos = new BlockPos(player);
        PoiManager116 pois = PoiManager116.get(this.world);
        List<BlockPos> taken = new ArrayList<>();
        for (BlockPos point : pois.find(type -> true, pos, 64, false)) {
            if (pois.isTaken(point)) taken.add(point);
        }
        BlockPos center = pos;
        if (!taken.isEmpty()) {
            long x = 0, y = 0, z = 0;
            for (BlockPos point : taken) {
                x += point.getX();
                y += point.getY();
                z += point.getZ();
            }
            center = new BlockPos(x / taken.size(), y / taken.size(), z / taken.size());
        }
        Raid116 raid = this.findRaid(center, 96.0D);
        if (raid == null) {
            raid = new Raid116(this.nextId++, this.world, center);
            this.raids.put(raid.getId(), raid);
        } else if (raid.isStarted() && raid.getBadOmenLevel() >= raid.getMaxLevel()) {
            player.removePotionEffect(PortiumPotions.BAD_OMEN);
            return;
        }
        raid.increaseLevel(player);
        this.markDirty();
    }

    // ---- Events ----

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;
        RaidManager116 manager = get(event.world);
        if (manager != null) manager.tick();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || !PortiumPotions.isRegistered(PortiumPotions.BAD_OMEN)) return;
        if (event.player.getActivePotionEffect(PortiumPotions.BAD_OMEN) == null) return;
        if (!Villages116.isVillage(event.player.world, new BlockPos(event.player))) return;
        RaidManager116 manager = get(event.player.world);
        if (manager != null) manager.badOmenTick(event.player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntityLiving() instanceof IRaider)) return;
        EntityLiving raider = (EntityLiving) event.getEntityLiving();
        Raid116 raid = raidOf(raider);
        if (raid != null) raid.removeRaider(raider, false);
    }

    /** A raider loaded from disk finds its raid again. */
    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity.world.isRemote || !(entity instanceof IRaider) || !(entity instanceof EntityLiving)) return;
        EntityLiving raider = (EntityLiving) entity;
        if (IRaider.state(raider).getRaidId() < 0) return;
        Raid116 raid = raidOf(raider);
        if (raid != null) {
            raid.readopt(raider);
        }
        com.michaelsebero.portium.entity.ai.EntityAIMoveTowardsRaid.addTo(raider);
    }

    @Nullable
    public static Raid116 raidOf(EntityLiving raider) {
        int id = IRaider.state(raider).getRaidId();
        if (id < 0) return null;
        RaidManager116 manager = get(raider.world);
        return manager == null ? null : manager.getRaid(id);
    }

    // ---- Saving ----

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        this.nextId = Math.max(1, tag.getInteger("NextAvailableID"));
        this.tick = tag.getLong("Tick");
        NBTTagList list = tag.getTagList("Raids", 10);
        this.unread.clear();
        for (int i = 0; i < list.tagCount(); i++) this.unread.add(list.getCompoundTagAt(i));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setInteger("NextAvailableID", this.nextId);
        tag.setLong("Tick", this.tick);
        NBTTagList list = new NBTTagList();
        for (Raid116 raid : this.raids.values()) list.appendTag(raid.write(new NBTTagCompound()));
        for (NBTTagCompound unread : this.unread) list.appendTag(unread);
        tag.setTag("Raids", list);
        return tag;
    }
}
