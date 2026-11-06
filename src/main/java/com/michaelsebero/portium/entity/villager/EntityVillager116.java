package com.michaelsebero.portium.entity.villager;

import com.google.common.collect.ImmutableList;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.IBrainHolder;
import com.michaelsebero.portium.entity.ai.brain.memory.GlobalPos;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.sensor.SensorType;
import com.michaelsebero.portium.entity.merchant.EntityAbstractVillager116;
import com.michaelsebero.portium.merchant.MerchantLevels;
import com.michaelsebero.portium.merchant.MerchantOffer;
import com.michaelsebero.portium.merchant.MerchantOffers;
import com.michaelsebero.portium.merchant.MerchantScreens;
import com.michaelsebero.portium.merchant.trade.ITrade;
import com.michaelsebero.portium.merchant.trade.VillagerTrades116;
import com.michaelsebero.portium.world.poi.PoiManager116;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 1.14's villager: a biome type, a profession taken at a workstation it claims, and a level that rises with trading,
 * each level adding two trades of its profession. Run by a brain on 1.14's daily schedule (wander, work at its job
 * site, meet at the bell, sleep in its bed), it restocks at its workstation, breeds when fed and a bed is free, flees
 * hostiles, and remembers what players did (gossip), which sets its prices.
 */
public class EntityVillager116 extends EntityAbstractVillager116 implements IBrainHolder, IVillagerDataHolder {

    private static final DataParameter<NBTTagCompound> VILLAGER_DATA = EntityDataManager.createKey(EntityVillager116.class, DataSerializers.COMPOUND_TAG);
    private static final DataParameter<com.google.common.base.Optional<BlockPos>> SLEEPING_POS = EntityDataManager.createKey(EntityVillager116.class,
            DataSerializers.OPTIONAL_BLOCK_POS);

    /** Food a villager eats to breed, and how much each is worth (1.14's FOOD_VALUES). */
    public static final Map<Item, Integer> FOOD_VALUES = com.google.common.collect.ImmutableMap.of(Items.BREAD, 4, Items.POTATO, 1, Items.CARROT, 1,
            Items.BEETROOT, 1);

    static final ImmutableList<MemoryModuleType<?>> MEMORIES = ImmutableList.of(MemoryModuleType.HOME, MemoryModuleType.JOB_SITE,
            MemoryModuleType.POTENTIAL_JOB_SITE, MemoryModuleType.MEETING_POINT, MemoryModuleType.MOBS, MemoryModuleType.VISIBLE_MOBS,
            MemoryModuleType.VISIBLE_VILLAGER_BABIES, MemoryModuleType.NEAREST_PLAYERS, MemoryModuleType.NEAREST_VISIBLE_PLAYER,
            MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleType.WALK_TARGET,
            MemoryModuleType.LOOK_TARGET, MemoryModuleType.INTERACTION_TARGET, MemoryModuleType.BREED_TARGET, MemoryModuleType.PATH,
            MemoryModuleType.INTERACTABLE_DOORS, MemoryModuleType.OPENED_DOORS, MemoryModuleType.NEAREST_BED, MemoryModuleType.HURT_BY,
            MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.NEAREST_HOSTILE, MemoryModuleType.SECONDARY_JOB_SITE, MemoryModuleType.HIDING_PLACE,
            MemoryModuleType.HEARD_BELL_TIME, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.LAST_SLEPT, MemoryModuleType.LAST_WOKEN,
            MemoryModuleType.LAST_WORKED_AT_POI, MemoryModuleType.GOLEM_DETECTED_RECENTLY);
    @SuppressWarnings({"rawtypes", "unchecked"})
    static final ImmutableList<SensorType> SENSORS = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS,
            SensorType.NEAREST_ITEMS, SensorType.HURT_BY, VillagerSensors116.HOSTILES, VillagerSensors116.BABIES, VillagerSensors116.SECONDARY_POIS,
            VillagerSensors116.GOLEM);

    private final Brain<EntityVillager116> brain;
    private final Gossips116 gossips = new Gossips116();
    private int xp;
    private int foodLevel;
    private boolean leveledUp;
    private int levelUpTimer;
    @Nullable
    private EntityPlayer previousCustomer;
    private long lastRestock;
    private int restocksToday;
    private long lastRestockDay;
    private long lastGossipDecay;
    private long lastGossipTime;
    private long lastGolemCheck;

    public EntityVillager116(World world) {
        super(world);
        this.setSize(0.6F, 1.95F);
        if (this.getNavigator() instanceof PathNavigateGround) {
            ((PathNavigateGround) this.getNavigator()).setBreakDoors(true);
            ((PathNavigateGround) this.getNavigator()).setEnterDoors(true);
        }
        this.setCanPickUpLoot(true);
        this.brain = VillagerTasks116.makeBrain(this, new Brain<EntityVillager116>(MEMORIES, (List) SENSORS));
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(VILLAGER_DATA, new VillagerData116(VillagerType116.PLAINS, VillagerProfession116.NONE, 1).write());
        this.dataManager.register(SLEEPING_POS, com.google.common.base.Optional.absent());
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(48.0D);
    }

    @Override
    public Brain<EntityVillager116> getBrain() {
        return this.brain;
    }

    // ---- Data ----

    @Override
    public VillagerData116 getVillagerData() {
        return VillagerData116.read(this.dataManager.get(VILLAGER_DATA));
    }

    @Override
    public void setVillagerData(VillagerData116 data) {
        VillagerData116 old = this.getVillagerData();
        if (old.getProfession() != data.getProfession()) this.offers = null;
        this.dataManager.set(VILLAGER_DATA, data.write());
    }

    @Override
    public int getXp() {
        return this.xp;
    }

    @Override
    public void setXP(int xp) {
        this.xp = xp;
    }

    public Gossips116 getGossips() {
        return this.gossips;
    }

    public int getPlayerReputation(EntityPlayer player) {
        return this.gossips.getReputation(player.getUniqueID());
    }

    public UUID getId() {
        return this.getUniqueID();
    }

    // ---- Brain ----

    @Override
    protected void updateAITasks() {
        this.world.profiler.startSection("villagerBrain");
        this.brain.tick((WorldServer) this.world, this);
        this.world.profiler.endSection();
        if (!this.hasCustomer() && this.levelUpTimer > 0) {
            if (--this.levelUpTimer <= 0) {
                if (this.leveledUp) {
                    this.levelUp();
                    this.leveledUp = false;
                }
                this.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 200, 0));
            }
        }
        if (this.previousCustomer != null && this.world instanceof WorldServer) {
            this.previousCustomer = null;
        }
        if (!this.isAIDisabled() && this.rand.nextInt(100) == 0) {
            long day = this.world.getTotalWorldTime() / 24000L;
            if (day != this.lastRestockDay) {
                this.lastRestockDay = day;
                this.restocksToday = 0;
            }
        }
        this.decayGossip();
        VillagerTasks116.updateActivities(this);
        super.updateAITasks();
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        Item item = stack.getItem();
        return (FOOD_VALUES.containsKey(item) || this.getVillagerData().getProfession().getSpecificItems().contains(item))
                && hasRoomFor(this.getVillagerInventory(), stack);
    }

    private static boolean hasRoomFor(InventoryBasic inventory, ItemStack stack) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (slot.isEmpty() || slot.getItem() == stack.getItem() && slot.getCount() < slot.getMaxStackSize()) return true;
        }
        return false;
    }

    @Override
    protected void updateEquipmentIfNeeded(EntityItem item) {
        ItemStack stack = item.getItem();
        if (!this.wantsToPickUp(stack)) return;
        ItemStack left = this.getVillagerInventory().addItem(stack);
        this.onItemPickup(item, stack.getCount() - left.getCount());
        if (left.isEmpty()) {
            item.setDead();
        } else {
            stack.setCount(left.getCount());
        }
    }

    @Override
    public void onLivingUpdate() {
        if (this.getShakeHeadTicks() > 0) this.setShakeHeadTicks(this.getShakeHeadTicks() - 1);
        super.onLivingUpdate();
        if (this.isSleeping() && !this.world.isRemote) {
            BlockPos bed = this.getSleepingPos().get();
            if (!(this.world.getBlockState(bed).getBlock() instanceof BlockBed)) this.wakeUp();
        }
    }

    // ---- Trading ----

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.getItem() == Items.SPAWN_EGG || !this.isEntityAlive() || this.hasCustomer() || this.isSleeping() || player.isSneaking()) {
            return super.processInteract(player, hand);
        }
        if (this.isChild()) {
            this.shakeHead();
            return true;
        }
        boolean noOffers = this.getOffers().isEmpty();
        if (hand == EnumHand.MAIN_HAND) {
            if (noOffers && !this.world.isRemote) this.shakeHead();
            player.addStat(StatList.TALKED_TO_VILLAGER);
        }
        if (noOffers) return true;
        if (!this.world.isRemote) {
            this.updateSpecialPrices(player);
            this.setCustomer(player);
            MerchantScreens.open(player, this, this.getVillagerData().getLevel());
        }
        return true;
    }

    private void shakeHead() {
        this.setShakeHeadTicks(40);
        if (!this.world.isRemote) this.playSound(SoundEvents.ENTITY_VILLAGER_NO, this.getSoundVolume(), this.getSoundPitch());
    }

    /** 1.14's prices for a player: cheaper with a good reputation, dearer with a bad one; heroes of the village get more off. */
    private void updateSpecialPrices(EntityPlayer player) {
        int reputation = this.getPlayerReputation(player);
        if (reputation != 0) {
            for (MerchantOffer offer : this.getOffers()) offer.increaseSpecialPrice(-((int) Math.floor((float) reputation * offer.getPriceMultiplier())));
        }
        PotionEffect hero = com.michaelsebero.portium.entity.raid.HeroOfTheVillage.effect(player);
        if (hero != null) {
            double discount = 0.3D + 0.0625D * hero.getAmplifier();
            for (MerchantOffer offer : this.getOffers()) {
                int off = (int) Math.floor(discount * offer.getBuyingStackFirst().getCount());
                offer.increaseSpecialPrice(-Math.max(off, 1));
            }
        }
    }

    @Override
    public void setCustomer(@Nullable EntityPlayer player) {
        boolean ended = this.getCustomer() != null && player == null;
        super.setCustomer(player);
        if (ended) {
            for (MerchantOffer offer : this.getOffers()) offer.resetSpecialPrice();
        }
    }

    @Override
    protected void onVillagerTrade(MerchantOffer offer) {
        int reward = 3 + this.rand.nextInt(4);
        this.xp += offer.getGivenExp();
        this.previousCustomer = this.getCustomer();
        if (this.canLevelUp()) {
            this.levelUpTimer = 40;
            this.leveledUp = true;
            reward += 5;
        }
        if (offer.getDoesRewardExp()) this.world.spawnEntity(new EntityXPOrb(this.world, this.posX, this.posY + 0.5D, this.posZ, reward));
        if (this.getCustomer() != null) this.gossips.add(this.getCustomer().getUniqueID(), Gossips116.Type.TRADING, 2);
        this.brain.setMemory(MemoryModuleType.LAST_WORKED_AT_POI, this.world.getTotalWorldTime());
    }

    private boolean canLevelUp() {
        int level = this.getVillagerData().getLevel();
        return MerchantLevels.canLevelUp(level) && this.xp >= MerchantLevels.getExperienceNext(level);
    }

    private void levelUp() {
        this.setVillagerData(this.getVillagerData().withLevel(this.getVillagerData().getLevel() + 1));
        this.populateTradeData();
    }

    @Override
    protected void populateTradeData() {
        VillagerData116 data = this.getVillagerData();
        ITrade[] trades = VillagerTrades116.forLevel(data.getProfession(), data.getLevel());
        if (trades.length > 0) this.addTrades(this.getOffers(), trades, 2);
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = new MerchantOffers();
            // Each level reached so far, in order, as a villager that levelled up would have them.
            VillagerData116 data = this.getVillagerData();
            for (int level = 1; level <= data.getLevel(); level++) {
                ITrade[] trades = VillagerTrades116.forLevel(data.getProfession(), level);
                if (trades.length > 0) this.addTrades(this.offers, trades, 2);
            }
        }
        return this.offers;
    }

    /** Whether trades can be restocked now: once more than two minutes after the last, twice a day. */
    public boolean canRestock() {
        if (this.offers == null || this.offers.isEmpty()) return false;
        boolean used = false;
        for (MerchantOffer offer : this.offers) {
            if (offer.hasBeenUsed()) used = true;
        }
        return used && (this.restocksToday == 0 || this.restocksToday < 2 && this.world.getTotalWorldTime() > this.lastRestock + 2400L);
    }

    /** Restocks every trade at the workstation, adjusting prices for demand. */
    public void restock() {
        for (MerchantOffer offer : this.getOffers()) {
            offer.calculateDemand();
            offer.resetUses();
        }
        this.lastRestock = this.world.getTotalWorldTime();
        this.restocksToday++;
    }

    @Override
    protected SoundEvent getVillagerYesNoSound(boolean yes) {
        return yes ? SoundEvents.ENTITY_VILLAGER_YES : SoundEvents.ENTITY_VILLAGER_NO;
    }

    // ---- Jobs ----

    /** Takes a profession (from a workstation it just claimed). */
    public void takeProfession(VillagerProfession116 profession) {
        if (this.getVillagerData().getProfession() == profession) return;
        this.setVillagerData(this.getVillagerData().withProfession(profession).withLevel(1));
    }

    /** 1.14: a villager that never traded loses its job with its job site. */
    public boolean hasTraded() {
        return this.xp > 0 || this.getVillagerData().getLevel() > 1;
    }

    /** Releases a remembered point back to the world's point-of-interest manager. */
    void releasePoi(MemoryModuleType<GlobalPos> memory) {
        this.brain.getMemory(memory).ifPresent(pos -> {
            if (this.world instanceof WorldServer && pos.getDimension() == this.world.provider.getDimension()) {
                PoiManager116.get((WorldServer) this.world).release(pos.getPos(), this.getUniqueID());
            }
        });
        this.brain.removeMemory(memory);
    }

    // ---- Food and breeding ----

    public int getFoodLevel() {
        return this.foodLevel;
    }

    /** Whether the villager has had (or carries) enough food to breed: 12 points, as 1.14. */
    public boolean canBreed() {
        return this.foodLevel + this.inventoryFoodValue() >= 12 && !this.isSleeping() && this.getGrowingAge() == 0;
    }

    private int inventoryFoodValue() {
        int total = 0;
        InventoryBasic inventory = this.getVillagerInventory();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            Integer value = FOOD_VALUES.get(stack.getItem());
            if (value != null) total += value * stack.getCount();
        }
        return total;
    }

    /** Eats from the inventory until fed enough to breed, then spends the food. */
    public void eatAndBreed() {
        InventoryBasic inventory = this.getVillagerInventory();
        for (int i = 0; i < inventory.getSizeInventory() && this.foodLevel < 12; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            Integer value = FOOD_VALUES.get(stack.getItem());
            while (value != null && !stack.isEmpty() && this.foodLevel < 12) {
                this.foodLevel += value;
                stack.shrink(1);
            }
        }
        this.foodLevel = Math.max(0, this.foodLevel - 12);
    }

    /** Whether the villager has more food than it needs, to share with others (farmers and those with bread). */
    public boolean hasFoodToShare() {
        return this.inventoryFoodValue() >= 24;
    }

    @Nullable
    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        EntityVillager116 child = new EntityVillager116(this.world);
        VillagerType116 type;
        if (this.rand.nextDouble() < 0.5D) {
            type = VillagerType116.byBiome(this.world.getBiome(new BlockPos(this)));
        } else if (mate instanceof EntityVillager116 && this.rand.nextBoolean()) {
            type = ((EntityVillager116) mate).getVillagerData().getType();
        } else {
            type = this.getVillagerData().getType();
        }
        child.setVillagerData(new VillagerData116(type, VillagerProfession116.NONE, 1));
        child.setGrowingAge(-24000);
        return child;
    }

    // ---- Sleeping ----

    public Optional<BlockPos> getSleepingPos() {
        return Optional.ofNullable(this.dataManager.get(SLEEPING_POS).orNull());
    }

    public boolean isSleeping() {
        return this.getSleepingPos().isPresent();
    }

    /** Lies down in a bed (its head block), marking it occupied. */
    public void startSleeping(BlockPos bed) {
        IBlockState state = this.world.getBlockState(bed);
        if (!(state.getBlock() instanceof BlockBed)) return;
        if (this.isRiding()) this.dismountRidingEntity();
        this.world.setBlockState(bed, state.withProperty(BlockBed.OCCUPIED, true), 3);
        this.setPosition(bed.getX() + 0.5D, bed.getY() + 0.6875D, bed.getZ() + 0.5D);
        this.motionX = this.motionY = this.motionZ = 0.0D;
        this.dataManager.set(SLEEPING_POS, com.google.common.base.Optional.of(bed));
        this.getNavigator().clearPath();
        this.brain.setMemory(MemoryModuleType.LAST_SLEPT, this.world.getTotalWorldTime());
        this.brain.removeMemory(MemoryModuleType.WALK_TARGET);
        this.brain.removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    /** Gets up, freeing the bed and standing beside it. */
    public void wakeUp() {
        Optional<BlockPos> sleeping = this.getSleepingPos();
        if (!sleeping.isPresent()) return;
        BlockPos bed = sleeping.get();
        IBlockState state = this.world.getBlockState(bed);
        if (state.getBlock() instanceof BlockBed) {
            this.world.setBlockState(bed, state.withProperty(BlockBed.OCCUPIED, false), 3);
            BlockPos exit = BlockBed.getSafeExitLocation(this.world, bed, 0);
            if (exit == null) exit = bed.up();
            this.setPosition(exit.getX() + 0.5D, exit.getY() + 0.1D, exit.getZ() + 0.5D);
        }
        this.dataManager.set(SLEEPING_POS, com.google.common.base.Optional.absent());
        this.brain.setMemory(MemoryModuleType.LAST_WOKEN, this.world.getTotalWorldTime());
    }

    @Override
    public boolean isPlayerSleeping() {
        return this.isSleeping();
    }

    /** Lies still while asleep (its brain keeps running, so it wakes when its schedule says). */
    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (this.isSleeping()) {
            this.motionX = 0.0D;
            this.motionZ = 0.0D;
            if (this.motionY > 0.0D) this.motionY = 0.0D;
            this.getNavigator().clearPath();
            return;
        }
        super.travel(strafe, vertical, forward);
    }

    @Override
    public boolean canBePushed() {
        return !this.isSleeping() && super.canBePushed();
    }

    // ---- Gossip and golems ----

    private void decayGossip() {
        long time = this.world.getTotalWorldTime();
        if (this.lastGossipDecay == 0L) {
            this.lastGossipDecay = time;
        } else if (time >= this.lastGossipDecay + 24000L) {
            this.gossips.decay();
            this.lastGossipDecay = time;
        }
    }

    /** Talks with another villager: swaps gossip, at most once a minute (and checks whether they want a golem). */
    public void gossipWith(EntityVillager116 other, long gameTime) {
        if (gameTime >= this.lastGossipTime && gameTime < this.lastGossipTime + 1200L
                || gameTime >= other.lastGossipTime && gameTime < other.lastGossipTime + 1200L) {
            return;
        }
        this.gossips.transferTo(other.gossips, this.rand, 10);
        other.gossips.transferTo(this.gossips, this.rand, 10);
        this.lastGossipTime = gameTime;
        other.lastGossipTime = gameTime;
        VillagerGolems116.trySummonFromGossip(this, gameTime);
    }

    /** 1.14: villagers that slept and worked recently, without a golem nearby, want one. */
    public boolean wantsGolem(long gameTime) {
        if (this.brain.hasMemory(MemoryModuleType.GOLEM_DETECTED_RECENTLY)) return false;
        Optional<Long> slept = this.brain.getMemory(MemoryModuleType.LAST_SLEPT);
        return slept.isPresent() && gameTime - slept.get() < 24000L;
    }

    public long getLastGolemCheck() {
        return this.lastGolemCheck;
    }

    public void setLastGolemCheck(long time) {
        this.lastGolemCheck = time;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source.getTrueSource() instanceof EntityPlayer && !this.world.isRemote) {
            this.gossips.add(source.getTrueSource().getUniqueID(), Gossips116.Type.MINOR_NEGATIVE, 25);
        }
        if (this.isSleeping() && !this.world.isRemote) this.wakeUp();
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public void onDeath(DamageSource cause) {
        if (!this.world.isRemote) {
            if (cause.getTrueSource() instanceof EntityPlayer) VillagerEvents116.witnessMurder(this, (EntityPlayer) cause.getTrueSource());
            this.releaseAllPois();
        }
        super.onDeath(cause);
    }

    /** Leaves its bed and gives back every point it holds (dying, or becoming a zombie or witch). */
    void releaseAllPois() {
        this.wakeUp();
        this.releasePoi(MemoryModuleType.HOME);
        this.releasePoi(MemoryModuleType.JOB_SITE);
        this.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
        this.brain.removeMemory(MemoryModuleType.MEETING_POINT);
    }

    /** What a zombie villager keeps of it: its data, trades, experience and gossip. */
    NBTTagCompound writeConversionData() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("VillagerData", this.getVillagerData().write());
        MerchantOffers offers = this.getOffers();
        if (!offers.isEmpty()) tag.setTag("Offers", offers.write());
        tag.setInteger("Xp", this.xp);
        tag.setTag("Gossips", this.gossips.write());
        return tag;
    }

    void readConversionData(NBTTagCompound tag) {
        if (tag.hasKey("VillagerData", 10)) this.setVillagerData(VillagerData116.read(tag.getCompoundTag("VillagerData")));
        if (tag.hasKey("Offers", 10)) this.offers = new MerchantOffers(tag.getCompoundTag("Offers"));
        this.xp = tag.getInteger("Xp");
        this.gossips.read(tag.getTagList("Gossips", 10));
    }

    /** Offers carried over from a 1.12 villager. */
    void setOffersForConversion(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void handleStatusUpdate(byte id) {
        switch (id) {
            case 12:
                this.spawnParticles(EnumParticleTypes.HEART);
                break;
            case 13:
                this.spawnParticles(EnumParticleTypes.VILLAGER_ANGRY);
                break;
            case 14:
                this.spawnParticles(EnumParticleTypes.VILLAGER_HAPPY);
                break;
            case 18:
                this.spawnParticles(EnumParticleTypes.HEART);
                break;
            default:
                super.handleStatusUpdate(id);
        }
    }

    @Override
    public void onStruckByLightning(EntityLightningBolt bolt) {
        if (this.world.isRemote || this.isDead) return;
        EntityWitch witch = new EntityWitch(this.world);
        witch.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
        witch.onInitialSpawn(this.world.getDifficultyForLocation(new BlockPos(witch)), null);
        witch.setNoAI(this.isAIDisabled());
        if (this.hasCustomName()) {
            witch.setCustomNameTag(this.getCustomNameTag());
            witch.setAlwaysRenderNameTag(this.getAlwaysRenderNameTag());
        }
        witch.enablePersistence();
        this.world.spawnEntity(witch);
        this.releaseAllPois();
        this.setDead();
    }

    // ---- Spawning, sounds, saving ----

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        this.setVillagerData(this.getVillagerData().withType(VillagerType116.byBiome(this.world.getBiome(new BlockPos(this)))));
        return super.onInitialSpawn(difficulty, data);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isSleeping()) return null;
        return this.hasCustomer() ? SoundEvents.ENTITY_VILLAGER_TRADING : SoundEvents.ENTITY_VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VILLAGER_DEATH;
    }

    /** The work sound of its profession, played while working at its job site. */
    public void playWorkSound() {
        SoundEvent sound = this.getVillagerData().getProfession().getSound();
        if (sound != null) this.playSound(sound, this.getSoundVolume(), this.getSoundPitch());
    }

    @Override
    public ITextComponent getDisplayName() {
        if (this.hasCustomName()) return super.getDisplayName();
        VillagerProfession116 profession = this.getVillagerData().getProfession();
        if (profession == VillagerProfession116.NONE) return new TextComponentTranslation("entity.Villager.name");
        TextComponentTranslation name = new TextComponentTranslation("entity.minecraft.villager." + profession.getName());
        name.getStyle().setHoverEvent(this.getHoverEvent());
        name.getStyle().setInsertion(this.getCachedUniqueIdString());
        return name;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setTag("VillagerData", this.getVillagerData().write());
        tag.setByte("FoodLevel", (byte) this.foodLevel);
        tag.setTag("Gossips", this.gossips.write());
        tag.setInteger("Xp", this.xp);
        tag.setLong("LastRestock", this.lastRestock);
        tag.setLong("LastGossipDecay", this.lastGossipDecay);
        tag.setInteger("RestocksToday", this.restocksToday);
        tag.setTag("Brain", this.brain.write());
        this.getSleepingPos().ifPresent(pos -> {
            tag.setInteger("SleepingX", pos.getX());
            tag.setInteger("SleepingY", pos.getY());
            tag.setInteger("SleepingZ", pos.getZ());
        });
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("VillagerData", 10)) this.setVillagerData(VillagerData116.read(tag.getCompoundTag("VillagerData")));
        if (tag.hasKey("Offers", 10)) this.offers = new MerchantOffers(tag.getCompoundTag("Offers"));
        this.foodLevel = tag.getByte("FoodLevel");
        this.gossips.read(tag.getTagList("Gossips", 10));
        this.xp = tag.getInteger("Xp");
        this.lastRestock = tag.getLong("LastRestock");
        this.lastGossipDecay = tag.getLong("LastGossipDecay");
        this.restocksToday = tag.getInteger("RestocksToday");
        if (tag.hasKey("Brain", 10)) this.brain.read(tag.getCompoundTag("Brain"));
        if (tag.hasKey("SleepingX")) this.dataManager.set(SLEEPING_POS, com.google.common.base.Optional.of(new BlockPos(tag.getInteger("SleepingX"), tag.getInteger("SleepingY"),
                tag.getInteger("SleepingZ"))));
        this.setCanPickUpLoot(true);
    }

    /** Claims again the points its memories name, as a villager loaded from disk still holds them. */
    void reclaimPois() {
        if (!(this.world instanceof WorldServer)) return;
        PoiManager116 pois = PoiManager116.get((WorldServer) this.world);
        for (MemoryModuleType<GlobalPos> memory : new MemoryModuleType[] {MemoryModuleType.HOME, MemoryModuleType.JOB_SITE,
                MemoryModuleType.POTENTIAL_JOB_SITE}) {
            this.brain.getMemory(memory).ifPresent(pos -> {
                if (pos.getDimension() == this.world.provider.getDimension()) pois.claim(pos.getPos(), this.getUniqueID());
            });
        }
    }

    @Override
    public boolean canDespawn() {
        return false;
    }
}
