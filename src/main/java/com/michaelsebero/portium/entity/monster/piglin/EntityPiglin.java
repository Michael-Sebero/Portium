package com.michaelsebero.portium.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.michaelsebero.portium.entity.Conversions116;
import com.michaelsebero.portium.entity.ICrossbowUser;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.sensor.SensorType;
import com.michaelsebero.portium.entity.monster.EntityHoglin;
import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.item.ItemCrossbow;
import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 1.16's piglin: barters for gold ingots, loves gold, hunts hoglins, attacks players not wearing gold, flees zombified
 * mobs and soul fire, fights with a golden sword or a crossbow, keeps an 8-slot inventory. Run by its brain
 * ({@link PiglinTasks}).
 */
public class EntityPiglin extends EntityAbstractPiglin implements ICrossbowUser, IRangedAttackMob, Conversions116.IChildSettable {

    public static final ResourceLocation LOOT_TABLE = com.michaelsebero.portium.init.PortiumLootTables.PIGLIN;
    private static final DataParameter<Boolean> BABY = EntityDataManager.createKey(EntityPiglin.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> CHARGING_CROSSBOW = EntityDataManager.createKey(EntityPiglin.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DANCING = EntityDataManager.createKey(EntityPiglin.class, DataSerializers.BOOLEAN);
    private static final UUID BABY_SPEED_ID = UUID.fromString("766bfa64-11f3-11ea-8d71-362b9e155667");
    private static final AttributeModifier BABY_SPEED = new AttributeModifier(BABY_SPEED_ID, "Baby speed boost", 0.2D, 1).setSaved(false);

    private static final ImmutableList<SensorType<?>> SENSORS = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS,
            SensorType.NEAREST_ITEMS, SensorType.HURT_BY, PiglinMobsSensor.TYPE);
    private static final ImmutableList<MemoryModuleType<?>> MEMORIES = ImmutableList.of(MemoryModuleType.LOOK_TARGET, MemoryModuleType.OPENED_DOORS,
            MemoryModuleType.MOBS, MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
            MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, MemoryModuleType.NEAREST_ADULT_PIGLINS, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
            MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryModuleType.INTERACTION_TARGET, MemoryModuleType.PATH,
            MemoryModuleType.ANGRY_AT, MemoryModuleType.UNIVERSAL_ANGER, MemoryModuleType.AVOID_TARGET, MemoryModuleType.ADMIRING_ITEM,
            MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM, MemoryModuleType.ADMIRING_DISABLED, MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM,
            MemoryModuleType.CELEBRATE_LOCATION, MemoryModuleType.DANCING, MemoryModuleType.HUNTED_RECENTLY, MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN,
            MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, MemoryModuleType.RIDE_TARGET,
            MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN,
            MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, MemoryModuleType.ATE_RECENTLY,
            MemoryModuleType.NEAREST_REPELLENT);

    private final InventoryBasic inventory = new InventoryBasic("Inventory", false, 8);
    private boolean cannotHunt;
    private final Brain<EntityPiglin> brain;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public EntityPiglin(World world) {
        super(world);
        this.setSize(0.6F, 1.95F);
        this.experienceValue = 5;
        this.brain = PiglinTasks.makeBrain(this, new Brain<EntityPiglin>(MEMORIES, (List) SENSORS));
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(BABY, false);
        this.dataManager.register(CHARGING_CROSSBOW, false);
        this.dataManager.register(DANCING, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(16.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.35D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(5.0D);
    }

    @Override
    public Brain<EntityPiglin> getBrain() {
        return this.brain;
    }

    // ---- Saving ----

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (this.isChild()) compound.setBoolean("IsBaby", true);
        if (this.cannotHunt) compound.setBoolean("CannotHunt", true);
        NBTTagList items = new NBTTagList();
        for (int slot = 0; slot < this.inventory.getSizeInventory(); ++slot) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) items.appendTag(stack.writeToNBT(new NBTTagCompound()));
        }
        compound.setTag("Inventory", items);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setChild116(compound.getBoolean("IsBaby"));
        this.cannotHunt = compound.getBoolean("CannotHunt");
        NBTTagList items = compound.getTagList("Inventory", 10);
        for (int i = 0; i < items.tagCount(); ++i) {
            ItemStack stack = new ItemStack(items.getCompoundTagAt(i));
            if (!stack.isEmpty()) this.inventory.addItem(stack);
        }
    }

    // ---- Inventory and equipment ----

    @Override
    protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier) {
        super.dropEquipment(wasRecentlyHit, lootingModifier);
        for (ItemStack stack : this.removeAllItems()) this.entityDropItem(stack, 0.0F);
    }

    private List<ItemStack> removeAllItems() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < this.inventory.getSizeInventory(); ++slot) {
            ItemStack stack = this.inventory.removeStackFromSlot(slot);
            if (!stack.isEmpty()) stacks.add(stack);
        }
        return stacks;
    }

    ItemStack addToInventory(ItemStack stack) {
        return this.inventory.addItem(stack);
    }

    /** 1.16's {@code Inventory.canAddItem}: an empty slot, or a matching stack with room. */
    boolean canAddToInventory(ItemStack stack) {
        for (int slot = 0; slot < this.inventory.getSizeInventory(); ++slot) {
            ItemStack existing = this.inventory.getStackInSlot(slot);
            if (existing.isEmpty() || ItemStack.areItemsEqual(existing, stack) && ItemStack.areItemStackTagsEqual(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    void holdInMainHand(ItemStack stack) {
        this.setItemSlotAndDropWhenKilled(EntityEquipmentSlot.MAINHAND, stack);
    }

    /** Admired gold ingots stay droppable but don't make it persistent; anything else is like picked-up gear. */
    void holdInOffHand(ItemStack stack) {
        if (stack.getItem() == PiglinTasks.BARTERING_ITEM) {
            this.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, stack);
            this.setDropChance(EntityEquipmentSlot.OFFHAND, 2.0F);
        } else {
            this.setItemSlotAndDropWhenKilled(EntityEquipmentSlot.OFFHAND, stack);
        }
    }

    /** 1.16's {@code setItemStackToSlot} + guaranteed drop + persistence. */
    private void setItemSlotAndDropWhenKilled(EntityEquipmentSlot slot, ItemStack stack) {
        this.setItemStackToSlot(slot, stack);
        this.setDropChance(slot, 2.0F);
        this.enablePersistence();
    }

    boolean canReplaceCurrentItem(ItemStack stack) {
        EntityEquipmentSlot slot = EntityLiving.getSlotForItemStack(stack);
        return this.shouldExchangeEquipment(stack, this.getItemStackFromSlot(slot));
    }

    /** 1.16's {@code func_233665_g_}: wears or wields the item if better than what's there. */
    boolean equipIfBetter(ItemStack stack) {
        EntityEquipmentSlot slot = EntityLiving.getSlotForItemStack(stack);
        ItemStack current = this.getItemStackFromSlot(slot);
        if (!this.shouldExchangeEquipment(stack, current)) return false;
        float chance = com.michaelsebero.portium.entity.DropChances.get(this, slot);
        if (!current.isEmpty() && Math.max(this.rand.nextFloat() - 0.1F, 0.0F) < chance) this.entityDropItem(current, 0.0F);
        this.setItemSlotAndDropWhenKilled(slot, stack);
        this.playEquipSound(stack);
        return true;
    }

    /** Loved items and crossbows win; an adult keeps its crossbow over anything but a crossbow. */
    private boolean shouldExchangeEquipment(ItemStack candidate, ItemStack current) {
        if (EnchantmentHelper.hasBindingCurse(current)) return false;
        boolean candidatePreferred = PiglinTasks.isLovedItem(candidate) || candidate.getItem() instanceof ItemCrossbow;
        boolean currentPreferred = PiglinTasks.isLovedItem(current) || current.getItem() instanceof ItemCrossbow;
        if (candidatePreferred && !currentPreferred) return true;
        if (!candidatePreferred && currentPreferred) return false;
        if (this.isAdult() && !(candidate.getItem() instanceof ItemCrossbow) && current.getItem() instanceof ItemCrossbow) return false;
        return isBetterGear(candidate, current);
    }

    /** 1.16's {@code MobEntity.shouldExchangeEquipment}: better swords, armour and tools; anything beats nothing. */
    static boolean isBetterGear(ItemStack candidate, ItemStack current) {
        if (current.isEmpty()) return true;
        if (candidate.getItem() instanceof ItemSword) {
            if (!(current.getItem() instanceof ItemSword)) return true;
            float a = ((ItemSword) candidate.getItem()).getAttackDamage();
            float b = ((ItemSword) current.getItem()).getAttackDamage();
            return a != b ? a > b : moreValuable(candidate, current);
        }
        if (candidate.getItem() instanceof ItemBow && current.getItem() instanceof ItemBow) return moreValuable(candidate, current);
        if (candidate.getItem() instanceof ItemCrossbow && current.getItem() instanceof ItemCrossbow) return moreValuable(candidate, current);
        if (candidate.getItem() instanceof ItemArmor) {
            if (EnchantmentHelper.hasBindingCurse(current)) return false;
            if (!(current.getItem() instanceof ItemArmor)) return true;
            ItemArmor a = (ItemArmor) candidate.getItem();
            ItemArmor b = (ItemArmor) current.getItem();
            if (a.damageReduceAmount != b.damageReduceAmount) return a.damageReduceAmount > b.damageReduceAmount;
            if (a.toughness != b.toughness) return a.toughness > b.toughness;
            return moreValuable(candidate, current);
        }
        if (candidate.getItem() instanceof ItemTool) {
            if (current.getItem() instanceof net.minecraft.item.ItemBlock) return true;
            if (current.getItem() instanceof ItemTool) {
                float a = attackDamage(candidate);
                float b = attackDamage(current);
                return a != b ? a > b : moreValuable(candidate, current);
            }
        }
        return false;
    }

    private static float attackDamage(ItemStack stack) {
        return (float) stack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND).get(SharedMonsterAttributes.ATTACK_DAMAGE.getName()).stream()
                .mapToDouble(AttributeModifier::getAmount).sum();
    }

    private static boolean moreValuable(ItemStack candidate, ItemStack current) {
        if (candidate.getItemDamage() < current.getItemDamage() || candidate.hasTagCompound() && !current.hasTagCompound()) return true;
        if (candidate.hasTagCompound() && current.hasTagCompound()) {
            return candidate.getTagCompound().getKeySet().stream().anyMatch(key -> !key.equals("Damage"))
                    && !current.getTagCompound().getKeySet().stream().anyMatch(key -> !key.equals("Damage"));
        }
        return false;
    }

    /** 1.16's item pickup: only what it wants, then the piglin's own handling. */
    @Override
    protected void updateEquipmentIfNeeded(EntityItem item) {
        if (!this.wantsToPickUp(item.getItem())) return;
        PiglinTasks.pickUpItem(this, item);
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        return net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.world, this) && this.canPickUpLoot() && PiglinTasks.wantsToPickUp(this, stack);
    }

    // ---- Spawning ----

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        data = super.onInitialSpawn(difficulty, data);
        if (com.michaelsebero.portium.entity.raid.RaiderSpawns.current() != com.michaelsebero.portium.entity.raid.RaiderSpawns.Reason.STRUCTURE) {
            if (this.world.rand.nextFloat() < 0.2F) {
                this.setChild116(true);
            } else if (this.isAdult()) {
                this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, this.createSpawnWeapon());
            }
        }
        PiglinTasks.initMemories(this);
        this.setEquipmentBasedOnDifficulty(difficulty);
        this.setEnchantmentBasedOnDifficulty(difficulty);
        return data;
    }

    @Override
    protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
        if (this.isAdult()) {
            this.maybeWear(EntityEquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            this.maybeWear(EntityEquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
            this.maybeWear(EntityEquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS));
            this.maybeWear(EntityEquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
        }
    }

    private void maybeWear(EntityEquipmentSlot slot, ItemStack stack) {
        if (this.world.rand.nextFloat() < 0.1F) this.setItemStackToSlot(slot, stack);
    }

    private ItemStack createSpawnWeapon() {
        return this.rand.nextFloat() < 0.5F && PortiumItems.CROSSBOW != null ? new ItemStack(PortiumItems.CROSSBOW) : new ItemStack(Items.GOLDEN_SWORD);
    }

    /** 1.16's spawn rule: anywhere but on a nether wart block. */
    @Override
    public boolean getCanSpawnHere() {
        Block below = this.world.getBlockState(new BlockPos(this).down()).getBlock();
        return below != Blocks.NETHER_WART_BLOCK && this.world.checkNoEntityCollision(this.getEntityBoundingBox())
                && this.world.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty() && !this.world.containsAnyLiquid(this.getEntityBoundingBox());
    }

    @Override
    public boolean isCreatureType(net.minecraft.entity.EnumCreatureType type, boolean forSpawnCount) {
        return type == net.minecraft.entity.EnumCreatureType.MONSTER;
    }

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return LOOT_TABLE;
    }

    // ---- Brain and behaviour ----

    @Override
    protected void updateAITasks() {
        this.world.profiler.startSection("piglinBrain");
        this.brain.tick((WorldServer) this.world, this);
        this.world.profiler.endSection();
        PiglinTasks.updateActivity(this);
        super.updateAITasks();
    }

    @Override
    protected void zombify(WorldServer world) {
        PiglinTasks.cancelAdmiring(this);
        for (ItemStack stack : this.removeAllItems()) this.entityDropItem(stack, 0.0F);
        super.zombify(world);
    }

    @Override
    public boolean canHunt() {
        return !this.cannotHunt;
    }

    public void setCannotHunt(boolean cannotHunt) {
        this.cannotHunt = cannotHunt;
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (super.processInteract(player, hand)) return true;
        if (!this.world.isRemote) return PiglinTasks.mobInteract(this, player, hand);
        return PiglinTasks.canAdmire(this, player.getHeldItem(hand)) && this.getAction() != PiglinAction.ADMIRING_ITEM;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        boolean hurt = super.attackEntityFrom(source, amount);
        if (this.world.isRemote) return false;
        if (hurt && source.getTrueSource() instanceof EntityLivingBase) PiglinTasks.wasHurtBy(this, (EntityLivingBase) source.getTrueSource());
        return hurt;
    }

    // ---- Baby ----

    @Override
    public void setChild116(boolean child) {
        this.dataManager.set(BABY, child);
        if (!this.world.isRemote) {
            IAttributeInstance speed = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
            speed.removeModifier(BABY_SPEED);
            if (child) speed.applyModifier(BABY_SPEED);
        }
        this.updateSize();
    }

    @Override
    public boolean isChild() {
        return this.dataManager.get(BABY);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (BABY.equals(key)) this.updateSize();
    }

    private void updateSize() {
        float scale = this.isChild() ? 0.5F : 1.0F;
        this.setSize(0.6F * scale, 1.95F * scale);
    }

    @Override
    public float getEyeHeight() {
        return this.isChild() ? 0.93F : 1.74F;
    }

    @Override
    public double getMountedYOffset() {
        return this.height * 0.92D;
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return this.experienceValue;
    }

    /** A baby piglin climbing onto a hoglin goes to the top of the pile (up to three high). */
    @Override
    public boolean startRiding(Entity mount, boolean force) {
        if (this.isChild() && mount instanceof EntityHoglin) mount = topOfStack(mount, 3);
        return super.startRiding(mount, force);
    }

    private static Entity topOfStack(Entity mount, int levels) {
        List<Entity> passengers = mount.getPassengers();
        return levels != 1 && !passengers.isEmpty() ? topOfStack(passengers.get(0), levels - 1) : mount;
    }

    // ---- Arms ----

    public boolean isDancing() {
        return this.dataManager.get(DANCING);
    }

    public void setDancing(boolean dancing) {
        this.dataManager.set(DANCING, dancing);
    }

    private boolean isChargingCrossbow() {
        return this.dataManager.get(CHARGING_CROSSBOW);
    }

    @Override
    public PiglinAction getAction() {
        if (this.isDancing()) return PiglinAction.DANCING;
        if (PiglinTasks.isLovedItem(this.getHeldItemOffhand())) return PiglinAction.ADMIRING_ITEM;
        if (this.isAggressive() && this.holdsMeleeWeapon()) return PiglinAction.ATTACKING_WITH_MELEE_WEAPON;
        if (this.isChargingCrossbow()) return PiglinAction.CROSSBOW_CHARGE;
        boolean crossbow = this.getHeldItemMainhand().getItem() instanceof ItemCrossbow || this.getHeldItemOffhand().getItem() instanceof ItemCrossbow;
        return this.isAggressive() && crossbow ? PiglinAction.CROSSBOW_HOLD : PiglinAction.DEFAULT;
    }

    // ---- Crossbow ----

    @Override
    public void setCharging(boolean charging) {
        this.dataManager.set(CHARGING_CROSSBOW, charging);
    }

    @Nullable
    @Override
    public EntityLivingBase getCrossbowTarget() {
        return this.getAttackTarget();
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.idleTime = 0;
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
        ICrossbowUser.performCrossbowAttack(this, 1.6F);
    }

    @Override
    public void setSwingingArms(boolean swingingArms) {}

    @Override
    public void shootCrossbowProjectile(@Nullable EntityLivingBase target, ItemStack crossbow, Entity projectile, float angle) {
        if (target != null) ICrossbowUser.shootAt(this, target, projectile, angle, 1.6F);
    }

    // ---- Sounds ----

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.world.isRemote ? null : PiglinTasks.getSoundForCurrentActivity(this).orElse(null);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_PIGLIN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        this.playSound(PortiumSounds.ENTITY_PIGLIN_STEP, 0.15F, 1.0F);
    }

    void playActivitySound(SoundEvent sound) {
        this.playSound(sound, this.getSoundVolume(), this.getSoundPitch());
    }

    @Override
    protected void playConvertedSound() {
        this.playActivitySound(PortiumSounds.ENTITY_PIGLIN_CONVERTED_TO_ZOMBIFIED);
    }
}
