package com.michaelsebero.portium.entity.raid;

import com.michaelsebero.portium.entity.monster.EntityPillager;
import com.michaelsebero.portium.entity.monster.EntityRavager;
import com.michaelsebero.portium.entity.villager.EntityVillager116;
import com.michaelsebero.portium.init.PortiumPotions;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.world.poi.PoiManager116;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.monster.EntityEvoker;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * 1.14's raid: set off by a player with Bad Omen in a village. After a countdown, waves of raiders (3, 5 or 7 by
 * difficulty, and a bonus wave above Bad Omen I) come from 32 blocks out; each wave's leader carries the ominous
 * banner. A boss bar shows the raiders' health and the countdown. Won when every wave is beaten (Hero of the Village
 * for the players there), lost if the village is gone; either way it ends after a celebration.
 */
public class Raid116 {

    private static final int PRE_RAID_TICKS = 300;
    private static final int CELEBRATION_TICKS = 600;
    private static final int MAX_TICKS = 48000;
    private static final double RAIDER_RANGE_SQ = 112.0D * 112.0D;

    public enum Status { ONGOING, VICTORY, LOSS, STOPPED }

    /** 1.16.5's wave members: how many of each come in waves 0 to 7. */
    enum WaveMember {
        VINDICATOR(world -> new EntityVindicator(world), new int[] {0, 0, 2, 0, 1, 4, 2, 5}),
        EVOKER(world -> new EntityEvoker(world), new int[] {0, 0, 0, 0, 0, 1, 1, 2}),
        PILLAGER(world -> new EntityPillager(world), new int[] {0, 4, 3, 3, 4, 4, 4, 2}),
        WITCH(world -> new EntityWitch(world), new int[] {0, 0, 0, 0, 3, 0, 0, 1}),
        RAVAGER(world -> new EntityRavager(world), new int[] {0, 0, 0, 1, 0, 1, 0, 2});

        final Function<WorldServer, EntityLiving> factory;
        final int[] counts;

        WaveMember(Function<WorldServer, EntityLiving> factory, int[] counts) {
            this.factory = factory;
            this.counts = counts;
        }
    }

    private final int id;
    private final WorldServer world;
    private BlockPos center;
    private final Map<Integer, Set<EntityLiving>> raiders = new HashMap<>();
    private final Map<Integer, EntityLiving> leaders = new HashMap<>();
    private final Set<UUID> heroes = new HashSet<>();
    private final BossInfoServer bossInfo = new BossInfoServer(new TextComponentTranslation("event.minecraft.raid"), BossInfo.Color.RED,
            BossInfo.Overlay.NOTCHED_10);
    private final Random random = new Random();
    private final int numGroups;
    private long ticksActive;
    private boolean started;
    private boolean active;
    private int badOmenLevel;
    private int groupsSpawned;
    private float totalHealth;
    private int preRaidTicks;
    private int postRaidTicks;
    private int celebrationTicks;
    private Status status = Status.ONGOING;
    @Nullable
    private BlockPos waveSpawnPos;

    public Raid116(int id, WorldServer world, BlockPos center) {
        this.id = id;
        this.world = world;
        this.center = center;
        this.active = true;
        this.preRaidTicks = PRE_RAID_TICKS;
        this.bossInfo.setPercent(0.0F);
        this.numGroups = groupsFor(world.getDifficulty());
    }

    public Raid116(WorldServer world, NBTTagCompound tag) {
        this.world = world;
        this.id = tag.getInteger("Id");
        this.started = tag.getBoolean("Started");
        this.active = tag.getBoolean("Active");
        this.ticksActive = tag.getLong("TicksActive");
        this.badOmenLevel = tag.getInteger("BadOmenLevel");
        this.groupsSpawned = tag.getInteger("GroupsSpawned");
        this.preRaidTicks = tag.getInteger("PreRaidTicks");
        this.postRaidTicks = tag.getInteger("PostRaidTicks");
        this.totalHealth = tag.getFloat("TotalHealth");
        this.center = new BlockPos(tag.getInteger("CX"), tag.getInteger("CY"), tag.getInteger("CZ"));
        this.numGroups = tag.getInteger("NumGroups");
        this.status = statusByName(tag.getString("Status"));
        NBTTagList heroes = tag.getTagList("HeroesOfTheVillage", 10);
        for (int i = 0; i < heroes.tagCount(); i++) this.heroes.add(heroes.getCompoundTagAt(i).getUniqueId("Id"));
    }

    private static Status statusByName(String name) {
        for (Status status : Status.values()) {
            if (status.name().equalsIgnoreCase(name)) return status;
        }
        return Status.ONGOING;
    }

    public NBTTagCompound write(NBTTagCompound tag) {
        tag.setInteger("Id", this.id);
        tag.setBoolean("Started", this.started);
        tag.setBoolean("Active", this.active);
        tag.setLong("TicksActive", this.ticksActive);
        tag.setInteger("BadOmenLevel", this.badOmenLevel);
        tag.setInteger("GroupsSpawned", this.groupsSpawned);
        tag.setInteger("PreRaidTicks", this.preRaidTicks);
        tag.setInteger("PostRaidTicks", this.postRaidTicks);
        tag.setFloat("TotalHealth", this.totalHealth);
        tag.setInteger("NumGroups", this.numGroups);
        tag.setString("Status", this.status.name().toLowerCase(java.util.Locale.ROOT));
        tag.setInteger("CX", this.center.getX());
        tag.setInteger("CY", this.center.getY());
        tag.setInteger("CZ", this.center.getZ());
        NBTTagList heroes = new NBTTagList();
        for (UUID hero : this.heroes) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setUniqueId("Id", hero);
            heroes.appendTag(entry);
        }
        tag.setTag("HeroesOfTheVillage", heroes);
        return tag;
    }

    static int groupsFor(EnumDifficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return 3;
            case NORMAL:
                return 5;
            case HARD:
                return 7;
            default:
                return 0;
        }
    }

    // ---- State ----

    public int getId() {
        return this.id;
    }

    public BlockPos getCenter() {
        return this.center;
    }

    public boolean isStarted() {
        return this.started;
    }

    public boolean isActive() {
        return this.active;
    }

    public boolean isOver() {
        return this.status == Status.VICTORY || this.status == Status.LOSS;
    }

    public boolean isStopped() {
        return this.status == Status.STOPPED;
    }

    public boolean isVictory() {
        return this.status == Status.VICTORY;
    }

    public boolean isLoss() {
        return this.status == Status.LOSS;
    }

    public int getBadOmenLevel() {
        return this.badOmenLevel;
    }

    public int getMaxLevel() {
        return 5;
    }

    public int getGroupsSpawned() {
        return this.groupsSpawned;
    }

    private boolean hasBonusWave() {
        return this.badOmenLevel > 1;
    }

    private boolean hasSpawnedBonusWave() {
        return this.groupsSpawned > this.numGroups;
    }

    private boolean shouldSpawnBonusGroup() {
        return this.isFinalWave() && this.getRaiderCount() == 0 && this.hasBonusWave() && !this.hasSpawnedBonusWave();
    }

    private boolean isFinalWave() {
        return this.groupsSpawned == this.numGroups;
    }

    private boolean hasMoreWaves() {
        return this.hasBonusWave() ? !this.hasSpawnedBonusWave() : !this.isFinalWave();
    }

    /** Raises the raid's level by the player's Bad Omen (which it takes away). */
    public void increaseLevel(EntityPlayer player) {
        PotionEffect omen = player.getActivePotionEffect(PortiumPotions.BAD_OMEN);
        if (omen != null) {
            this.badOmenLevel = MathHelper.clamp(this.badOmenLevel + omen.getAmplifier() + 1, 0, this.getMaxLevel());
            player.removePotionEffect(PortiumPotions.BAD_OMEN);
        }
    }

    public void stop() {
        this.active = false;
        for (EntityPlayerMP player : new ArrayList<>(this.bossInfo.getPlayers())) this.bossInfo.removePlayer(player);
        this.status = Status.STOPPED;
    }

    private boolean isVillage(BlockPos pos) {
        return com.michaelsebero.portium.world.Villages116.isVillage(this.world, pos);
    }

    // ---- Ticking ----

    public void tick() {
        if (this.isStopped()) return;
        if (this.status == Status.ONGOING) {
            boolean wasActive = this.active;
            this.active = this.world.isBlockLoaded(this.center);
            if (this.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
                this.stop();
                return;
            }
            if (wasActive != this.active) this.bossInfo.setVisible(this.active);
            if (!this.active) return;
            if (!this.isVillage(this.center)) this.moveToNearbyVillage();
            if (!this.isVillage(this.center)) {
                if (this.groupsSpawned > 0) {
                    this.status = Status.LOSS;
                } else {
                    this.stop();
                }
            }
            if (++this.ticksActive >= MAX_TICKS) {
                this.stop();
                return;
            }
            int raiderCount = this.getRaiderCount();
            if (raiderCount == 0 && this.hasMoreWaves()) {
                if (this.preRaidTicks <= 0) {
                    if (this.preRaidTicks == 0 && this.groupsSpawned > 0) {
                        this.preRaidTicks = PRE_RAID_TICKS;
                        this.bossInfo.setName(new TextComponentTranslation("event.minecraft.raid"));
                        return;
                    }
                } else {
                    boolean findSpawn = this.waveSpawnPos == null && this.preRaidTicks % 5 == 0
                            || this.waveSpawnPos != null && !this.world.isBlockLoaded(this.waveSpawnPos);
                    if (findSpawn) this.waveSpawnPos = this.findSpawnPos(this.preRaidTicks < 100 ? 1 : 0, 20);
                    if (this.preRaidTicks == PRE_RAID_TICKS || this.preRaidTicks % 20 == 0) this.updatePlayers();
                    --this.preRaidTicks;
                    this.bossInfo.setPercent(MathHelper.clamp((float) (PRE_RAID_TICKS - this.preRaidTicks) / PRE_RAID_TICKS, 0.0F, 1.0F));
                }
            }
            if (this.ticksActive % 20L == 0L) {
                this.updatePlayers();
                this.updateRaiders();
                if (raiderCount > 0) {
                    if (raiderCount <= 2) {
                        this.bossInfo.setName(new TextComponentTranslation("event.minecraft.raid").appendText(" - ")
                                .appendSibling(new TextComponentTranslation("event.minecraft.raid.raiders_remaining", raiderCount)));
                    } else {
                        this.bossInfo.setName(new TextComponentTranslation("event.minecraft.raid"));
                    }
                } else {
                    this.bossInfo.setName(new TextComponentTranslation("event.minecraft.raid"));
                }
            }
            boolean played = false;
            int tries = 0;
            while (this.shouldSpawnGroup()) {
                BlockPos pos = this.waveSpawnPos != null ? this.waveSpawnPos : this.findSpawnPos(tries, 20);
                if (pos != null) {
                    this.started = true;
                    this.spawnWave(pos);
                    if (!played) {
                        this.playWaveSound(pos);
                        played = true;
                    }
                } else {
                    tries++;
                }
                if (tries > 3) {
                    this.stop();
                    break;
                }
            }
            if (this.started && !this.hasMoreWaves() && raiderCount == 0) {
                if (this.postRaidTicks < 40) {
                    this.postRaidTicks++;
                } else {
                    this.status = Status.VICTORY;
                    this.rewardHeroes();
                }
            }
        } else if (this.isOver()) {
            if (++this.celebrationTicks >= CELEBRATION_TICKS) {
                this.stop();
                return;
            }
            if (this.celebrationTicks % 20 == 0) {
                this.updatePlayers();
                this.bossInfo.setVisible(true);
                this.bossInfo.setPercent(0.0F);
                this.bossInfo.setName(new TextComponentTranslation("event.minecraft.raid").appendText(" - ")
                        .appendSibling(new TextComponentTranslation(this.isVictory() ? "event.minecraft.raid.victory" : "event.minecraft.raid.defeat")));
                this.celebrate();
            }
        }
    }

    private boolean shouldSpawnGroup() {
        return (!this.isFinalWave() || this.shouldSpawnBonusGroup()) && this.preRaidTicks == 0 && this.getRaiderCount() == 0
                && this.groupsSpawned < this.numGroups + (this.hasBonusWave() ? 1 : 0);
    }

    /** Victory: Hero of the Village for the players who were there, by the raid's level. */
    private void rewardHeroes() {
        if (!PortiumPotions.isRegistered(PortiumPotions.HERO_OF_THE_VILLAGE)) return;
        for (UUID id : this.heroes) {
            EntityPlayer player = this.world.getPlayerEntityByUUID(id);
            if (player == null || player.isSpectator()) continue;
            player.addPotionEffect(new PotionEffect(PortiumPotions.HERO_OF_THE_VILLAGE, 48000, Math.max(0, this.badOmenLevel - 1), false, true));
            if (player instanceof EntityPlayerMP) com.michaelsebero.portium.advancements.ModernTriggers116.HERO_OF_THE_VILLAGE.trigger((EntityPlayerMP) player);
        }
    }

    /** After a win villagers cheer and set off fireworks; after a loss the raiders celebrate. */
    private void celebrate() {
        if (this.isVictory()) {
            for (EntityVillager116 villager : this.world.getEntitiesWithinAABB(EntityVillager116.class,
                    new net.minecraft.util.math.AxisAlignedBB(this.center).grow(48.0D))) {
                if (villager.isChild() || this.random.nextInt(3) != 0) continue;
                villager.playSound(PortiumSounds.ENTITY_VILLAGER_CELEBRATE, 1.0F, 1.0F);
                if (this.random.nextInt(4) == 0) this.firework(villager);
            }
        } else {
            for (Set<EntityLiving> wave : this.raiders.values()) {
                for (EntityLiving raider : wave) ((IRaider) raider).portium$setCelebrating(true);
            }
        }
    }

    private void firework(EntityVillager116 villager) {
        ItemStack rocket = new ItemStack(Items.FIREWORKS);
        NBTTagCompound fireworks = rocket.getOrCreateSubCompound("Fireworks");
        NBTTagCompound explosion = new NBTTagCompound();
        explosion.setByte("Type", (byte) this.random.nextInt(5));
        explosion.setIntArray("Colors", new int[] {net.minecraft.item.EnumDyeColor.byDyeDamage(this.random.nextInt(16)).getColorValue()});
        NBTTagList explosions = new NBTTagList();
        explosions.appendTag(explosion);
        fireworks.setTag("Explosions", explosions);
        fireworks.setByte("Flight", (byte) 1);
        this.world.spawnEntity(new EntityFireworkRocket(this.world, villager.posX, villager.posY + villager.getEyeHeight(), villager.posZ, rocket));
    }

    /** Moves the raid's centre to the middle of the village points within 64 blocks, if the old centre is no village. */
    private void moveToNearbyVillage() {
        List<BlockPos> points = PoiManager116.get(this.world).find(type -> true, this.center, 64, false);
        List<BlockPos> taken = new ArrayList<>();
        PoiManager116 pois = PoiManager116.get(this.world);
        for (BlockPos pos : points) {
            if (pois.isTaken(pos)) taken.add(pos);
        }
        if (taken.isEmpty()) return;
        long x = 0, y = 0, z = 0;
        for (BlockPos pos : taken) {
            x += pos.getX();
            y += pos.getY();
            z += pos.getZ();
        }
        this.center = new BlockPos(x / taken.size(), y / taken.size(), z / taken.size());
    }

    /** Players within 96 blocks see the boss bar; those there are heroes if it's won. */
    private void updatePlayers() {
        Set<EntityPlayerMP> current = new HashSet<>(this.bossInfo.getPlayers());
        for (EntityPlayer player : this.world.playerEntities) {
            if (!(player instanceof EntityPlayerMP) || !player.isEntityAlive()) continue;
            EntityPlayerMP mp = (EntityPlayerMP) player;
            boolean near = player.getDistanceSq(this.center) < 96.0D * 96.0D;
            if (near) {
                if (!current.remove(mp)) this.bossInfo.addPlayer(mp);
                this.heroes.add(player.getUniqueID());
            }
        }
        for (EntityPlayerMP gone : current) this.bossInfo.removePlayer(gone);
    }

    /** Drops raiders that died, left, or wandered off too far (or idled outside the village too long). */
    private void updateRaiders() {
        for (Set<EntityLiving> wave : this.raiders.values()) {
            for (EntityLiving raider : new ArrayList<>(wave)) {
                RaiderState state = IRaider.state(raider);
                if (raider.isDead || raider.world != this.world || raider.getDistanceSq(this.center) >= RAIDER_RANGE_SQ) {
                    this.removeRaider(raider, true);
                    continue;
                }
                if (raider.ticksExisted > 600) {
                    if (!this.isVillage(new BlockPos(raider)) && raider.getIdleTime() > 2400) state.setJoinDelay(state.getJoinDelay() + 1);
                    if (state.getJoinDelay() >= 30) this.removeRaider(raider, true);
                }
            }
        }
        this.updateBar();
    }

    private void updateBar() {
        this.bossInfo.setPercent(this.totalHealth <= 0.0F ? 0.0F : MathHelper.clamp(this.getCurrentHealth() / this.totalHealth, 0.0F, 1.0F));
    }

    private float getCurrentHealth() {
        float health = 0.0F;
        for (Set<EntityLiving> wave : this.raiders.values()) {
            for (EntityLiving raider : wave) health += raider.getHealth();
        }
        return health;
    }

    public int getRaiderCount() {
        int count = 0;
        for (Set<EntityLiving> wave : this.raiders.values()) count += wave.size();
        return count;
    }

    // ---- Waves ----

    /** A spot on the ground 32 blocks (or less, after failed tries) out from the centre, outside the village. */
    @Nullable
    private BlockPos findSpawnPos(int offsetMultiplier, int attempts) {
        int scale = offsetMultiplier == 0 ? 2 : 2 - offsetMultiplier;
        for (int attempt = 0; attempt < attempts; attempt++) {
            float angle = this.random.nextFloat() * ((float) Math.PI * 2F);
            int x = this.center.getX() + MathHelper.floor(MathHelper.cos(angle) * 32.0F * scale) + this.random.nextInt(5);
            int z = this.center.getZ() + MathHelper.floor(MathHelper.sin(angle) * 32.0F * scale) + this.random.nextInt(5);
            if (!this.world.isBlockLoaded(new BlockPos(x, 0, z))) continue;
            BlockPos pos = this.world.getHeight(new BlockPos(x, 0, z));
            if (this.isVillage(pos) && offsetMultiplier < 2) continue;
            if (this.world.isAreaLoaded(pos.add(-10, -10, -10), pos.add(10, 10, 10))
                    && (this.world.getBlockState(pos.down()).isSideSolid(this.world, pos.down(), net.minecraft.util.EnumFacing.UP)
                    || this.world.getBlockState(pos).getBlock() == net.minecraft.init.Blocks.SNOW_LAYER)) {
                return pos;
            }
        }
        return null;
    }

    private void spawnWave(BlockPos pos) {
        boolean leaderChosen = false;
        int wave = this.groupsSpawned + 1;
        this.totalHealth = 0.0F;
        boolean bonus = this.shouldSpawnBonusGroup();
        EnumDifficulty difficulty = this.world.getDifficulty();
        for (WaveMember member : WaveMember.values()) {
            int count = (bonus ? member.counts[Math.min(this.numGroups, 7)] : member.counts[Math.min(wave, 7)]) + this.bonusSpawns(member, wave, difficulty, bonus);
            int riders = 0;
            for (int i = 0; i < count; i++) {
                EntityLiving raider = member.factory.apply(this.world);
                if (!(raider instanceof IRaider)) continue;
                if (!leaderChosen && ((IRaider) raider).portium$canBeLeader()) {
                    this.makeLeader(wave, raider);
                    leaderChosen = true;
                }
                this.join(wave, raider, pos);
                if (member == WaveMember.RAVAGER) {
                    EntityLiving rider = null;
                    if (wave == groupsFor(EnumDifficulty.NORMAL)) {
                        rider = new EntityPillager(this.world);
                    } else if (wave >= groupsFor(EnumDifficulty.HARD)) {
                        rider = riders == 0 ? new EntityEvoker(this.world) : new EntityVindicator(this.world);
                    }
                    riders++;
                    if (rider instanceof IRaider) {
                        this.join(wave, rider, pos);
                        rider.setLocationAndAngles(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 0.0F, 0.0F);
                        rider.startRiding(raider);
                    }
                }
            }
        }
        this.waveSpawnPos = null;
        this.groupsSpawned++;
        this.updateBar();
    }

    private int bonusSpawns(WaveMember member, int wave, EnumDifficulty difficulty, boolean bonus) {
        boolean easy = difficulty == EnumDifficulty.EASY;
        boolean normal = difficulty == EnumDifficulty.NORMAL;
        int max;
        switch (member) {
            case WITCH:
                if (easy || wave <= 2 || wave == 4) return 0;
                max = 1;
                break;
            case PILLAGER:
            case VINDICATOR:
                max = easy ? this.random.nextInt(2) : normal ? 1 : 2;
                break;
            case RAVAGER:
                max = !easy && bonus ? 1 : 0;
                break;
            default:
                return 0;
        }
        return max > 0 ? this.random.nextInt(max + 1) : 0;
    }

    /** Adds a raider to a wave and puts it in the world at the wave's spawn. */
    private void join(int wave, EntityLiving raider, BlockPos pos) {
        RaiderState state = IRaider.state(raider);
        state.setRaidId(this.id);
        state.setWave(wave);
        state.setCanJoinRaid(true);
        state.setJoinDelay(0);
        this.raiders.computeIfAbsent(wave, key -> new HashSet<>()).add(raider);
        raider.setLocationAndAngles(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, this.random.nextFloat() * 360.0F, 0.0F);
        RaiderSpawns.spawning(RaiderSpawns.Reason.EVENT, () -> raider.onInitialSpawn(this.world.getDifficultyForLocation(pos), null));
        ((IRaider) raider).portium$applyWaveBonus(wave, state.isLeader());
        raider.onGround = true;
        raider.enablePersistence();
        this.world.spawnEntity(raider);
        this.totalHealth += raider.getHealth();
    }

    private void makeLeader(int wave, EntityLiving raider) {
        IRaider.state(raider).setLeader(true);
        raider.setItemStackToSlot(EntityEquipmentSlot.HEAD, OminousBanner.create());
        raider.setDropChance(EntityEquipmentSlot.HEAD, 2.0F);
        this.leaders.put(wave, raider);
    }

    /** Takes a raider out of its wave (it died or left). */
    public void removeRaider(EntityLiving raider, boolean wanderedOff) {
        Set<EntityLiving> wave = this.raiders.get(IRaider.state(raider).getWave());
        if (wave != null && wave.remove(raider)) {
            if (wanderedOff) this.totalHealth -= raider.getHealth();
            IRaider.state(raider).setRaidId(-1);
            this.updateBar();
        }
    }

    /** A raider of this raid (by id) that's in the world now, found again after loading. */
    public void readopt(EntityLiving raider) {
        int wave = IRaider.state(raider).getWave();
        Set<EntityLiving> members = this.raiders.computeIfAbsent(wave, key -> new HashSet<>());
        if (members.add(raider) && IRaider.state(raider).isLeader()) this.leaders.put(wave, raider);
    }

    private void playWaveSound(BlockPos pos) {
        for (EntityPlayer player : this.world.playerEntities) {
            if (!(player instanceof EntityPlayerMP)) continue;
            double dx = pos.getX() - player.posX;
            double dz = pos.getZ() - player.posZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (player.getDistanceSq(this.center) > 64.0D * 64.0D) continue;
            double x = player.posX + 13.0D * dx / Math.max(distance, 1.0D);
            double z = player.posZ + 13.0D * dz / Math.max(distance, 1.0D);
            ((EntityPlayerMP) player).connection.sendPacket(new SPacketSoundEffect(PortiumSounds.EVENT_RAID_HORN, SoundCategory.NEUTRAL, x, player.posY, z, 64.0F,
                    1.0F));
        }
    }

    public Set<EntityLiving> getAllRaiders() {
        Set<EntityLiving> all = new HashSet<>();
        for (Set<EntityLiving> wave : this.raiders.values()) all.addAll(wave);
        return all;
    }

    @Nullable
    public EntityLiving getLeader(int wave) {
        return this.leaders.get(wave);
    }

    public Optional<Entity> anyRaider() {
        return this.getAllRaiders().stream().map(raider -> (Entity) raider).findAny();
    }

    /** Debug text for commands and tests. */
    public TextComponentString describe() {
        return new TextComponentString("raid " + this.id + " at " + this.center + ": " + this.status + ", level " + this.badOmenLevel + ", waves "
                + this.groupsSpawned + "/" + this.numGroups + ", raiders " + this.getRaiderCount() + ", countdown " + this.preRaidTicks);
    }
}
