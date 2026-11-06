package com.michaelsebero.portium.entity.villager;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.init.PortiumEntities;
import com.michaelsebero.portium.merchant.MerchantOffer;
import com.michaelsebero.portium.merchant.MerchantOffers;
import com.michaelsebero.portium.world.WorldGeneration;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Villagers in 1.16.5 worlds: 1.12's villagers (from villages, eggs, cures) become 1.14 villagers on their first tick,
 * keeping their job, level and trades; 1.14 villagers killed by zombies become zombie villagers that remember all of
 * it; villagers who see a player kill one of their own hold it against them.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class VillagerEvents116 {

    /** Where a zombie villager keeps the villager it was (data, offers, experience, gossip). */
    public static final String ZOMBIE_TAG = "portium:Villager";

    private static final List<EntityVillager> PENDING = new ArrayList<>();

    public static boolean isActive(World world) {
        return PortiumConfig.mobs.villagers && PortiumEntities.isRegistered(EntityVillager116.class) && WorldGeneration.is116(world);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity.world.isRemote) return;
        if (entity instanceof net.minecraft.entity.EntityCreature && PortiumEntities.isRegistered(EntityVillager116.class)) {
            huntNewVillagers((net.minecraft.entity.EntityCreature) entity);
        }
        if (!isActive(entity.world)) return;
        if (entity instanceof net.minecraft.entity.monster.EntityIronGolem) {
            net.minecraft.entity.monster.EntityIronGolem golem = (net.minecraft.entity.monster.EntityIronGolem) entity;
            boolean has = false;
            for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry entry : golem.targetTasks.taskEntries) {
                if (entry.action instanceof com.michaelsebero.portium.entity.ai.EntityAIGolemDefendVillagers) has = true;
            }
            if (!has) golem.targetTasks.addTask(1, new com.michaelsebero.portium.entity.ai.EntityAIGolemDefendVillagers(golem));
            return;
        }
        if (entity instanceof EntityZombieVillager && entity instanceof IVillagerDataHolder) {
            dressZombie((EntityZombieVillager) entity);
            return;
        }
        if (entity.getClass() != EntityVillager.class) return;
        synchronized (PENDING) {
            PENDING.add((EntityVillager) entity);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        List<EntityVillager> villagers;
        synchronized (PENDING) {
            if (PENDING.isEmpty()) return;
            villagers = new ArrayList<>(PENDING);
            PENDING.clear();
        }
        for (EntityVillager old : villagers) {
            if (old.isDead || old.world.getEntityByID(old.getEntityId()) != old) continue;
            VillagerProfession116 profession = professionOf(old);
            if (profession == null) continue; // A modded profession: left as 1.12's villager, which knows its trades.
            convert(old, profession);
        }
    }

    private static void convert(EntityVillager old, VillagerProfession116 profession) {
        NBTTagCompound tag = old.writeToNBT(new NBTTagCompound());
        Entity mount = old.getRidingEntity();
        old.dismountRidingEntity();
        old.setDead();
        EntityVillager116 villager = new EntityVillager116(old.world);
        villager.setLocationAndAngles(old.posX, old.posY, old.posZ, old.rotationYaw, old.rotationPitch);
        villager.setGrowingAge(old.getGrowingAge());
        villager.setHealth(old.getHealth());
        if (old.hasCustomName()) {
            villager.setCustomNameTag(old.getCustomNameTag());
            villager.setAlwaysRenderNameTag(old.getAlwaysRenderNameTag());
        }
        if (old.isAIDisabled()) villager.setNoAI(true);
        if (tag.getBoolean("PersistenceRequired")) villager.enablePersistence();
        int level = Math.max(1, Math.min(5, tag.getInteger("CareerLevel")));
        VillagerType116 type = VillagerType116.byBiome(old.world.getBiome(new BlockPos(old)));
        villager.setVillagerData(new VillagerData116(type, profession, profession.hasWorkstation() ? level : 1));
        // Trades it already offered stay (with their uses), so players keep what they found; it keeps its job without a workstation.
        MerchantOffers offers = offers(tag.getCompoundTag("Offers"));
        if (!offers.isEmpty()) villager.setOffersForConversion(offers);
        if (profession.hasWorkstation()) villager.setXP(Math.max(1, com.michaelsebero.portium.merchant.MerchantLevels.getExperiencePrevious(level)));
        old.world.spawnEntity(villager);
        if (mount != null) villager.startRiding(mount, true);
    }

    /**
     * Mobs that hunt 1.12's villagers (zombies, illagers, Portium's raiders) hunt 1.14's too: each of their target
     * goals for {@code EntityVillager} gets a twin for {@code EntityVillager116} at the same priority.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void huntNewVillagers(net.minecraft.entity.EntityCreature creature) {
        List<net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry> twins = new ArrayList<>();
        for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry entry : creature.targetTasks.taskEntries) {
            if (!(entry.action instanceof net.minecraft.entity.ai.EntityAINearestAttackableTarget)) continue;
            com.michaelsebero.portium.mixin.accessor.EntityAINearestAttackableTargetAccessor target =
                    (com.michaelsebero.portium.mixin.accessor.EntityAINearestAttackableTargetAccessor) entry.action;
            if (target.portium$getTargetClass() == EntityVillager116.class) return;
            if (target.portium$getTargetClass() != EntityVillager.class) continue;
            com.michaelsebero.portium.mixin.accessor.EntityAITargetAccessor base = (com.michaelsebero.portium.mixin.accessor.EntityAITargetAccessor) entry.action;
            twins.add(creature.targetTasks.new EntityAITaskEntry(entry.priority, new net.minecraft.entity.ai.EntityAINearestAttackableTarget(creature,
                    EntityVillager116.class, target.portium$getTargetChance(), base.portium$getShouldCheckSight(), base.portium$getNearbyOnly(), null)));
        }
        for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry twin : twins) creature.targetTasks.addTask(twin.priority, twin.action);
    }

    /** A zombie villager shows the villager it was, or (born a zombie) its biome's type and its 1.12 job. */
    private static void dressZombie(EntityZombieVillager zombie) {
        IVillagerDataHolder holder = (IVillagerDataHolder) zombie;
        NBTTagCompound saved = zombie.getEntityData().getCompoundTag(ZOMBIE_TAG);
        if (saved.hasKey("VillagerData", 10)) {
            holder.setVillagerData(VillagerData116.read(saved.getCompoundTag("VillagerData")));
            return;
        }
        if (holder.hasVillagerData()) return;
        EntityVillager stand = new EntityVillager(zombie.world);
        stand.setProfession(zombie.getForgeProfession());
        VillagerProfession116 profession = professionOf(stand);
        VillagerType116 type = VillagerType116.byBiome(zombie.world.getBiome(new BlockPos(zombie)));
        VillagerData116 data = new VillagerData116(type, profession == null ? VillagerProfession116.NONE : profession, 1);
        holder.setVillagerData(data);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("VillagerData", data.write());
        zombie.getEntityData().setTag(ZOMBIE_TAG, tag);
    }

    /** 1.12's profession and career as a 1.14 profession; null for professions 1.14 doesn't have (other mods'). */
    @Nullable
    static VillagerProfession116 professionOf(EntityVillager villager) {
        VillagerRegistry.VillagerProfession forge = villager.getProfessionForge();
        String name = forge == null || forge.getRegistryName() == null ? "minecraft:farmer" : forge.getRegistryName().toString();
        int career = villager.writeToNBT(new NBTTagCompound()).getInteger("Career");
        if (career <= 0) career = 1 + villager.getRNG().nextInt(Math.max(1, careerCount(name)));
        switch (name) {
            case "minecraft:farmer":
                return career == 2 ? VillagerProfession116.FISHERMAN : career == 3 ? VillagerProfession116.SHEPHERD
                        : career == 4 ? VillagerProfession116.FLETCHER : VillagerProfession116.FARMER;
            case "minecraft:librarian":
                return career == 2 ? VillagerProfession116.CARTOGRAPHER : VillagerProfession116.LIBRARIAN;
            case "minecraft:priest":
                return VillagerProfession116.CLERIC;
            case "minecraft:smith":
                return career == 2 ? VillagerProfession116.WEAPONSMITH : career == 3 ? VillagerProfession116.TOOLSMITH : VillagerProfession116.ARMORER;
            case "minecraft:butcher":
                return career == 2 ? VillagerProfession116.LEATHERWORKER : VillagerProfession116.BUTCHER;
            case "minecraft:nitwit":
                return VillagerProfession116.NITWIT;
            default:
                return null;
        }
    }

    private static int careerCount(String profession) {
        switch (profession) {
            case "minecraft:farmer":
                return 4;
            case "minecraft:librarian":
            case "minecraft:butcher":
                return 2;
            case "minecraft:smith":
                return 3;
            default:
                return 1;
        }
    }

    /** 1.12's merchant recipes as 1.14 offers. */
    private static MerchantOffers offers(NBTTagCompound tag) {
        MerchantOffers offers = new MerchantOffers();
        NBTTagList recipes = tag.getTagList("Recipes", 10);
        for (int i = 0; i < recipes.tagCount(); i++) {
            NBTTagCompound recipe = recipes.getCompoundTagAt(i);
            ItemStack buy = new ItemStack(recipe.getCompoundTag("buy"));
            ItemStack buyB = recipe.hasKey("buyB", 10) ? new ItemStack(recipe.getCompoundTag("buyB")) : ItemStack.EMPTY;
            ItemStack sell = new ItemStack(recipe.getCompoundTag("sell"));
            if (buy.isEmpty() || sell.isEmpty()) continue;
            int maxUses = recipe.hasKey("maxUses", 99) ? recipe.getInteger("maxUses") : 7;
            offers.add(new MerchantOffer(buy, buyB, sell, recipe.getInteger("uses"), maxUses, 1, 0.05F));
        }
        return offers;
    }

    /** 1.14: zombies (of every kind) turn villagers they kill into zombie villagers on normal (half the time) and hard. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityVillager116) || event.isCanceled()) return;
        EntityVillager116 villager = (EntityVillager116) event.getEntityLiving();
        World world = villager.world;
        if (world.isRemote || !(event.getSource().getTrueSource() instanceof EntityZombie)) return;
        EnumDifficulty difficulty = world.getDifficulty();
        if (difficulty != EnumDifficulty.NORMAL && difficulty != EnumDifficulty.HARD) return;
        if (difficulty == EnumDifficulty.NORMAL && world.rand.nextBoolean()) return;
        EntityZombieVillager zombie = new EntityZombieVillager(world);
        zombie.copyLocationAndAnglesFrom(villager);
        zombie.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(zombie)), null);
        zombie.setChild(villager.isChild());
        zombie.setNoAI(villager.isAIDisabled());
        if (villager.hasCustomName()) {
            zombie.setCustomNameTag(villager.getCustomNameTag());
            zombie.setAlwaysRenderNameTag(villager.getAlwaysRenderNameTag());
        }
        zombie.enablePersistence();
        zombie.getEntityData().setTag(ZOMBIE_TAG, villager.writeConversionData());
        world.spawnEntity(zombie);
        world.playEvent(null, 1026, new BlockPos(villager), 0);
        event.setCanceled(true);
        villager.releaseAllPois();
        villager.setDead();
    }

    /** Villagers that see a player kill a villager remember it as a major grudge. */
    static void witnessMurder(EntityVillager116 victim, EntityPlayer killer) {
        for (EntityVillager116 witness : victim.world.getEntitiesWithinAABB(EntityVillager116.class, victim.getEntityBoundingBox().grow(16.0D))) {
            if (witness != victim && witness.canEntityBeSeen(victim)) witness.getGossips().add(killer.getUniqueID(), Gossips116.Type.MAJOR_NEGATIVE, 25);
        }
    }

    /**
     * A zombie villager cured in a 1.16.5 world: the villager it was, with its gossip; the curing player is remembered
     * fondly (and trades cheaper). Zombie villagers that never were 1.14 villagers come back with their 1.12 job.
     */
    public static boolean cure(EntityZombieVillager zombie, @Nullable UUID curer) {
        if (!isActive(zombie.world)) return false;
        World world = zombie.world;
        EntityVillager116 villager = new EntityVillager116(world);
        villager.copyLocationAndAnglesFrom(zombie);
        NBTTagCompound saved = zombie.getEntityData().getCompoundTag(ZOMBIE_TAG);
        if (!saved.isEmpty()) {
            villager.readConversionData(saved);
        } else {
            EntityVillager stand = new EntityVillager(world);
            stand.setProfession(zombie.getForgeProfession());
            VillagerProfession116 profession = professionOf(stand);
            VillagerType116 type = VillagerType116.byBiome(world.getBiome(new BlockPos(zombie)));
            villager.setVillagerData(new VillagerData116(type, profession == null ? VillagerProfession116.NONE : profession, 1));
        }
        villager.setGrowingAge(zombie.isChild() ? -24000 : 0);
        villager.setNoAI(zombie.isAIDisabled());
        if (zombie.hasCustomName()) {
            villager.setCustomNameTag(zombie.getCustomNameTag());
            villager.setAlwaysRenderNameTag(zombie.getAlwaysRenderNameTag());
        }
        if (curer != null) {
            villager.getGossips().add(curer, Gossips116.Type.MAJOR_POSITIVE, 20);
            villager.getGossips().add(curer, Gossips116.Type.MINOR_POSITIVE, 25);
        }
        zombie.setDead();
        world.spawnEntity(villager);
        villager.addPotionEffect(new net.minecraft.potion.PotionEffect(net.minecraft.init.MobEffects.NAUSEA, 200, 0));
        world.playEvent(null, 1027, new BlockPos(villager), 0);
        return true;
    }

    private VillagerEvents116() {}
}
