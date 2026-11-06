package com.michaelsebero.portium.entity.passive;

import com.google.common.base.Optional;
import com.michaelsebero.portium.block.plant.BlockSweetBerryBush;
import com.michaelsebero.portium.entity.MobAttack;
import com.michaelsebero.portium.entity.ai.EntityAIBreed116;
import com.michaelsebero.portium.entity.ai.EntityAIMoveToBlock116;
import com.michaelsebero.portium.entity.ai.LookHelper116;
import com.michaelsebero.portium.entity.fish.EntityAbstractFish;
import com.michaelsebero.portium.entity.fish.EntityAbstractGroupFish;
import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.Village;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The fox (1.14). Red foxes live in taigas, snow foxes in snowy taigas. They sleep by day in the shade, and come out
 * at night to hunt chickens, rabbits, baby turtles on land and fish: stalking up crouched, then pouncing (and maybe
 * landing face-first in snow). They carry one item in their mouth (spat out for a better one, eaten if it's food),
 * pick sweet berries, run from players, wolves and polar bears, and trust whoever bred them, defending them when hurt.
 */
public class EntityFox extends EntityAnimal {

    private static final DataParameter<Integer> FOX_TYPE = EntityDataManager.createKey(EntityFox.class, DataSerializers.VARINT);
    private static final DataParameter<Byte> FOX_FLAGS = EntityDataManager.createKey(EntityFox.class, DataSerializers.BYTE);
    private static final DataParameter<Optional<UUID>> TRUSTED_UUID_SECONDARY = EntityDataManager.createKey(EntityFox.class, DataSerializers.OPTIONAL_UNIQUE_ID);
    private static final DataParameter<Optional<UUID>> TRUSTED_UUID_MAIN = EntityDataManager.createKey(EntityFox.class, DataSerializers.OPTIONAL_UNIQUE_ID);

    private static final int FLAG_SITTING = 1;
    private static final int FLAG_CROUCHING = 4;
    private static final int FLAG_INTERESTED = 8;
    private static final int FLAG_POUNCING = 16;
    private static final int FLAG_SLEEPING = 32;
    private static final int FLAG_STUCK = 64;
    private static final int FLAG_AGGROED = 128;

    private static final Predicate<EntityItem> TRUSTED_TARGET_SELECTOR = item -> !item.cannotPickup() && item.isEntityAlive();
    /** A mob that attacked something in the last 30 seconds: who a fox defends its trusted player from. */
    private static final Predicate<Entity> STALKABLE_PREY = entity -> entity instanceof EntityLivingBase
            && ((EntityLivingBase) entity).getLastAttackedEntity() != null
            && ((EntityLivingBase) entity).getLastAttackedEntityTime() < entity.ticksExisted + 600;
    private static final Predicate<Entity> IS_PREY = entity -> entity instanceof EntityChicken || entity instanceof EntityRabbit;
    private static final Predicate<Entity> SHOULD_AVOID = entity -> !entity.isSneaking() && EntitySelectors.CAN_AI_TARGET.apply(entity);

    private EntityAIBase attackAnimals;
    private EntityAIBase attackTurtles;
    private EntityAIBase attackFish;
    private float interestedAngle;
    private float interestedAngleO;
    private float crouchAmount;
    private float crouchAmountO;
    private int eatTicks;

    public EntityFox(World world) {
        super(world);
        this.setSize(0.6F, 0.7F);
        ((com.michaelsebero.portium.mixin.accessor.EntityLivingAccessor) this).portium$setLookHelper(new FoxLookHelper());
        this.moveHelper = new FoxMoveHelper();
        this.setPathPriority(PathNodeType.DANGER_OTHER, 0.0F);
        this.setPathPriority(PathNodeType.DAMAGE_OTHER, 0.0F);
        this.setCanPickUpLoot(true);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(TRUSTED_UUID_SECONDARY, Optional.absent());
        this.dataManager.register(TRUSTED_UUID_MAIN, Optional.absent());
        this.dataManager.register(FOX_TYPE, 0);
        this.dataManager.register(FOX_FLAGS, (byte) 0);
    }

    @Override
    protected void initEntityAI() {
        this.attackAnimals = new EntityAINearestAttackableTarget<>(this, EntityAnimal.class, 10, false, false,
                entity -> entity instanceof EntityChicken || entity instanceof EntityRabbit);
        this.attackTurtles = new EntityAINearestAttackableTarget<>(this, EntityTurtle.class, 10, false, false, EntityTurtle.TARGET_DRY_BABY::test);
        this.attackFish = new EntityAINearestAttackableTarget<>(this, EntityAbstractFish.class, 20, false, false,
                entity -> entity instanceof EntityAbstractGroupFish);
        this.tasks.addTask(0, new SwimGoal());
        this.tasks.addTask(1, new JumpGoal());
        this.tasks.addTask(2, new PanicGoal(2.2D));
        this.tasks.addTask(3, new MateGoal(1.0D));
        this.tasks.addTask(4, new EntityAIAvoidEntity<>(this, EntityPlayer.class,
                player -> SHOULD_AVOID.test(player) && !this.isTrustedUUID(player.getUniqueID()) && !this.isFoxAggroed(), 16.0F, 1.6D, 1.4D));
        this.tasks.addTask(4, new EntityAIAvoidEntity<>(this, EntityWolf.class, wolf -> !wolf.isTamed() && !this.isFoxAggroed(), 8.0F, 1.6D, 1.4D));
        this.tasks.addTask(4, new EntityAIAvoidEntity<>(this, EntityPolarBear.class, bear -> !this.isFoxAggroed(), 8.0F, 1.6D, 1.4D));
        this.tasks.addTask(5, new FollowTargetGoal());
        this.tasks.addTask(6, new PounceGoal());
        this.tasks.addTask(6, new FindShelterGoal(1.25D));
        this.tasks.addTask(7, new BiteGoal(1.2000000476837158D, true));
        this.tasks.addTask(7, new SleepGoal());
        this.tasks.addTask(8, new FollowGoal(1.25D));
        this.tasks.addTask(9, new StrollGoal(200));
        this.tasks.addTask(10, new EatBerriesGoal(1.2000000476837158D, 12, 2));
        this.tasks.addTask(10, new EntityAILeapAtTarget(this, 0.4F));
        this.tasks.addTask(11, new EntityAIWanderAvoidWater(this, 1.0D));
        this.tasks.addTask(11, new FindItemsGoal());
        this.tasks.addTask(12, new WatchGoal(24.0F));
        this.tasks.addTask(13, new SitAndLookGoal());
        this.targetTasks.addTask(3, new RevengeGoal(entity -> STALKABLE_PREY.test(entity) && !this.isTrustedUUID(entity.getUniqueID())));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.30000001192092896D);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(2.0D);
    }

    // ---- Ticking ----

    @Override
    public void onLivingUpdate() {
        if (!this.world.isRemote && this.isEntityAlive() && this.isServerWorld()) {
            ++this.eatTicks;
            ItemStack held = this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
            if (this.canEatItem(held)) {
                if (this.eatTicks > 600) {
                    ItemStack left = held.onItemUseFinish(this.world, this);
                    if (!left.isEmpty()) this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, left);
                    this.eatTicks = 0;
                } else if (this.eatTicks > 560 && this.rand.nextFloat() < 0.1F) {
                    this.playSound(PortiumSounds.ENTITY_FOX_EAT, 1.0F, 1.0F);
                    this.world.setEntityState(this, (byte) 45);
                }
            }
            EntityLivingBase target = this.getAttackTarget();
            if (target == null || !target.isEntityAlive()) {
                this.setCrouching(false);
                this.setInterested(false);
            }
        }
        if (this.isSleeping() || this.isMovementBlocked()) {
            this.isJumping = false;
            this.moveStrafing = 0.0F;
            this.moveForward = 0.0F;
        }
        super.onLivingUpdate();
        if (this.isFoxAggroed() && this.rand.nextFloat() < 0.05F) this.playSound(PortiumSounds.ENTITY_FOX_AGGRO, 1.0F, 1.0F);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.isServerWorld()) {
            boolean inWater = this.isInWater();
            if (inWater || this.getAttackTarget() != null || this.world.isThundering()) this.wakeUp();
            if (inWater || this.isSleeping()) this.setSitting(false);
            if (this.isStuck() && this.world.rand.nextFloat() < 0.2F) {
                BlockPos pos = new BlockPos(this);
                this.world.playEvent(2001, pos, Block.getStateId(this.world.getBlockState(pos)));
            }
        }
        this.interestedAngleO = this.interestedAngle;
        if (this.isInterested()) this.interestedAngle += (1.0F - this.interestedAngle) * 0.4F;
        else this.interestedAngle += (0.0F - this.interestedAngle) * 0.4F;
        this.crouchAmountO = this.crouchAmount;
        if (this.isCrouching()) {
            this.crouchAmount += 0.2F;
            if (this.crouchAmount > 3.0F) this.crouchAmount = 3.0F;
        } else {
            this.crouchAmount = 0.0F;
        }
    }

    private boolean canEatItem(ItemStack stack) {
        return stack.getItem() instanceof ItemFood && this.getAttackTarget() == null && this.onGround && !this.isSleeping();
    }

    @Override
    public void handleStatusUpdate(byte id) {
        if (id == 45) {
            ItemStack held = this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
            if (!held.isEmpty()) {
                Vec3d look = this.getLookVec();
                for (int i = 0; i < 8; ++i) {
                    Vec3d speed = new Vec3d((this.rand.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D)
                            .rotatePitch(-this.rotationPitch * 0.017453292F).rotateYaw(-this.rotationYaw * 0.017453292F);
                    this.world.spawnParticle(EnumParticleTypes.ITEM_CRACK, this.posX + look.x / 2.0D, this.posY, this.posZ + look.z / 2.0D,
                            speed.x, speed.y + 0.05D, speed.z, Item.getIdFromItem(held.getItem()), held.getMetadata());
                }
            }
        } else {
            super.handleStatusUpdate(id);
        }
    }

    // ---- Spawning ----

    /** 20% of foxes spawn with something in their mouth. */
    @Override
    protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
        if (this.rand.nextFloat() >= 0.2F) return;
        float roll = this.rand.nextFloat();
        ItemStack stack;
        if (roll < 0.05F) stack = new ItemStack(Items.EMERALD);
        else if (roll < 0.2F) stack = new ItemStack(Items.EGG);
        else if (roll < 0.4F) stack = this.rand.nextBoolean() ? new ItemStack(Items.RABBIT_FOOT) : new ItemStack(Items.RABBIT_HIDE);
        else if (roll < 0.6F) stack = new ItemStack(Items.WHEAT);
        else if (roll < 0.8F) stack = new ItemStack(Items.LEATHER);
        else stack = new ItemStack(Items.FEATHER);
        this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, stack);
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        Type type = Type.forBiome(this.world.getBiome(new BlockPos(this)));
        boolean baby = false;
        if (data instanceof FoxData) {
            type = ((FoxData) data).type;
            if (((FoxData) data).indexInGroup >= 2) baby = true;
        } else {
            data = new FoxData(type);
        }
        ((FoxData) data).indexInGroup++;
        this.setVariantType(type);
        if (baby) this.setGrowingAge(-24000);
        if (this.world instanceof WorldServer) this.setAttackGoals();
        this.setEquipmentBasedOnDifficulty(difficulty);
        return super.onInitialSpawn(difficulty, data);
    }

    /** Red foxes hunt land animals first, snow foxes fish. */
    private void setAttackGoals() {
        this.targetTasks.removeTask(this.attackAnimals);
        this.targetTasks.removeTask(this.attackTurtles);
        this.targetTasks.removeTask(this.attackFish);
        if (this.getVariantType() == Type.RED) {
            this.targetTasks.addTask(4, this.attackAnimals);
            this.targetTasks.addTask(4, this.attackTurtles);
            this.targetTasks.addTask(6, this.attackFish);
        } else {
            this.targetTasks.addTask(4, this.attackFish);
            this.targetTasks.addTask(6, this.attackAnimals);
            this.targetTasks.addTask(6, this.attackTurtles);
        }
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        EntityFox baby = new EntityFox(this.world);
        baby.setVariantType(this.rand.nextBoolean() ? this.getVariantType() : ((EntityFox) mate).getVariantType());
        return baby;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return PortiumItems.SWEET_BERRIES != null && stack.getItem() == PortiumItems.SWEET_BERRIES;
    }

    @Override
    protected void consumeItemFromStack(EntityPlayer player, ItemStack stack) {
        if (this.isBreedingItem(stack)) this.playSound(PortiumSounds.ENTITY_FOX_EAT, 1.0F, 1.0F);
        super.consumeItemFromStack(player, stack);
    }

    @Override
    public float getEyeHeight() {
        return this.isChild() ? this.height * 0.85F : 0.4F;
    }

    // ---- State ----

    public Type getVariantType() {
        return Type.byIndex(this.dataManager.get(FOX_TYPE));
    }

    private void setVariantType(Type type) {
        this.dataManager.set(FOX_TYPE, type.index);
    }

    private List<UUID> getTrustedUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        uuids.add(this.dataManager.get(TRUSTED_UUID_SECONDARY).orNull());
        uuids.add(this.dataManager.get(TRUSTED_UUID_MAIN).orNull());
        return uuids;
    }

    private void addTrustedUUID(@Nullable UUID uuid) {
        if (this.dataManager.get(TRUSTED_UUID_SECONDARY).isPresent()) this.dataManager.set(TRUSTED_UUID_MAIN, Optional.fromNullable(uuid));
        else this.dataManager.set(TRUSTED_UUID_SECONDARY, Optional.fromNullable(uuid));
    }

    private boolean isTrustedUUID(UUID uuid) {
        return this.getTrustedUUIDs().contains(uuid);
    }

    public boolean isSitting() {
        return this.getFoxFlag(FLAG_SITTING);
    }

    public void setSitting(boolean sitting) {
        this.setFoxFlag(FLAG_SITTING, sitting);
    }

    public boolean isStuck() {
        return this.getFoxFlag(FLAG_STUCK);
    }

    private void setStuck(boolean stuck) {
        this.setFoxFlag(FLAG_STUCK, stuck);
    }

    private boolean isFoxAggroed() {
        return this.getFoxFlag(FLAG_AGGROED);
    }

    private void setFoxAggroed(boolean aggroed) {
        this.setFoxFlag(FLAG_AGGROED, aggroed);
    }

    public boolean isSleeping() {
        return this.getFoxFlag(FLAG_SLEEPING);
    }

    private void setSleeping(boolean sleeping) {
        this.setFoxFlag(FLAG_SLEEPING, sleeping);
    }

    public boolean isPouncing() {
        return this.getFoxFlag(FLAG_POUNCING);
    }

    public void setPouncing(boolean pouncing) {
        this.setFoxFlag(FLAG_POUNCING, pouncing);
    }

    public boolean isFullyCrouched() {
        return this.crouchAmount == 3.0F;
    }

    public void setCrouching(boolean crouching) {
        this.setFoxFlag(FLAG_CROUCHING, crouching);
    }

    public boolean isCrouching() {
        return this.getFoxFlag(FLAG_CROUCHING);
    }

    public void setInterested(boolean interested) {
        this.setFoxFlag(FLAG_INTERESTED, interested);
    }

    public boolean isInterested() {
        return this.getFoxFlag(FLAG_INTERESTED);
    }

    /** The head's tilt while watching prey, for the model. */
    public float getInterestedAngle(float partialTicks) {
        return (this.interestedAngleO + (this.interestedAngle - this.interestedAngleO) * partialTicks) * 0.11F * (float) Math.PI;
    }

    public float getCrouchAmount(float partialTicks) {
        return this.crouchAmountO + (this.crouchAmount - this.crouchAmountO) * partialTicks;
    }

    private void setFoxFlag(int flag, boolean on) {
        byte flags = this.dataManager.get(FOX_FLAGS);
        this.dataManager.set(FOX_FLAGS, (byte) (on ? flags | flag : flags & ~flag));
    }

    private boolean getFoxFlag(int flag) {
        return (this.dataManager.get(FOX_FLAGS) & flag) != 0;
    }

    private void wakeUp() {
        this.setSleeping(false);
    }

    /** Back to standing: no sitting, crouching, sleeping, defending, or nose in the snow. */
    private void clearStates() {
        this.setInterested(false);
        this.setCrouching(false);
        this.setSitting(false);
        this.setSleeping(false);
        this.setFoxAggroed(false);
        this.setStuck(false);
    }

    /** Not sleeping, sitting or stuck: free to move. */
    private boolean canMove() {
        return !this.isSleeping() && !this.isSitting() && !this.isStuck();
    }

    @Override
    public void setAttackTarget(@Nullable EntityLivingBase target) {
        if (this.isFoxAggroed() && target == null) this.setFoxAggroed(false);
        super.setAttackTarget(target);
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        return MobAttack.attack(this, target);
    }

    // ---- Saving ----

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        NBTTagList trusted = new NBTTagList();
        for (UUID uuid : this.getTrustedUUIDs()) {
            if (uuid != null) trusted.appendTag(uuidTag(uuid));
        }
        compound.setTag("Trusted", trusted);
        compound.setBoolean("Sleeping", this.isSleeping());
        compound.setString("Type", this.getVariantType().name);
        compound.setBoolean("Sitting", this.isSitting());
        compound.setBoolean("Crouching", this.isCrouching());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        NBTTagList trusted = compound.getTagList("Trusted", 11);
        for (int i = 0; i < trusted.tagCount(); ++i) this.addTrustedUUID(readUuid(trusted.get(i)));
        // Older Portium saves wrote UUIDs as compounds.
        NBTTagList legacy = compound.getTagList("Trusted", 10);
        for (int i = 0; i < legacy.tagCount(); ++i) this.addTrustedUUID(NBTUtil.getUUIDFromTag(legacy.getCompoundTagAt(i)));
        this.setSleeping(compound.getBoolean("Sleeping"));
        this.setVariantType(Type.byName(compound.getString("Type")));
        this.setSitting(compound.getBoolean("Sitting"));
        this.setCrouching(compound.getBoolean("Crouching"));
        if (this.world instanceof WorldServer) this.setAttackGoals();
    }

    /** 1.16's UUID tag: four ints. */
    private static NBTTagIntArray uuidTag(UUID uuid) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        return new NBTTagIntArray(new int[] {(int) (most >> 32), (int) most, (int) (least >> 32), (int) least});
    }

    @Nullable
    private static UUID readUuid(NBTBase tag) {
        if (!(tag instanceof NBTTagIntArray)) return null;
        int[] ints = ((NBTTagIntArray) tag).getIntArray();
        if (ints.length != 4) return null;
        return new UUID((long) ints[0] << 32 | ints[1] & 0xFFFFFFFFL, (long) ints[2] << 32 | ints[3] & 0xFFFFFFFFL);
    }

    // ---- Items in its mouth ----

    private boolean canPickUpItem(ItemStack stack) {
        EntityEquipmentSlot slot = getSlotForItemStack(stack);
        return this.getItemStackFromSlot(slot).isEmpty() && slot == EntityEquipmentSlot.MAINHAND;
    }

    @Override
    protected boolean canEquipItem(ItemStack stack) {
        ItemStack held = this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        return held.isEmpty() || this.eatTicks > 0 && stack.getItem() instanceof ItemFood && !(held.getItem() instanceof ItemFood);
    }

    private void spitOutItem(ItemStack stack) {
        if (stack.isEmpty() || this.world.isRemote) return;
        Vec3d look = this.getLookVec();
        EntityItem item = new EntityItem(this.world, this.posX + look.x, this.posY + 1.0D, this.posZ + look.z, stack);
        item.setPickupDelay(40);
        item.setThrower(this.getUniqueID().toString());
        this.playSound(PortiumSounds.ENTITY_FOX_SPIT, 1.0F, 1.0F);
        this.world.spawnEntity(item);
    }

    private void spawnItem(ItemStack stack) {
        this.world.spawnEntity(new EntityItem(this.world, this.posX, this.posY, this.posZ, stack));
    }

    @Override
    protected void updateEquipmentIfNeeded(EntityItem itemEntity) {
        ItemStack stack = itemEntity.getItem();
        if (!this.canPickUpItem(stack) || !this.canEquipItem(stack)) return;
        int count = stack.getCount();
        if (count > 1) this.spawnItem(stack.splitStack(count - 1));
        this.spitOutItem(this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND));
        this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, stack.splitStack(1));
        this.inventoryHandsDropChances[EntityEquipmentSlot.MAINHAND.getIndex()] = 2.0F;
        this.onItemPickup(itemEntity, stack.getCount());
        itemEntity.setDead();
        this.eatTicks = 0;
    }

    /** It drops what's in its mouth when it dies, whatever it is. */
    @Override
    protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier) {
        ItemStack held = this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        if (!held.isEmpty()) {
            this.entityDropItem(held, 0.0F);
            this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        super.dropEquipment(wasRecentlyHit, lootingModifier);
    }

    // ---- Sounds and damage ----

    @Override
    public void playLivingSound() {
        SoundEvent sound = this.getAmbientSound();
        if (sound == PortiumSounds.ENTITY_FOX_SCREECH) this.playSound(sound, 2.0F, this.getSoundPitch());
        else super.playLivingSound();
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isSleeping()) return PortiumSounds.ENTITY_FOX_SLEEP;
        if (!this.world.isDaytime() && this.rand.nextFloat() < 0.1F
                && this.world.getEntitiesWithinAABB(EntityPlayer.class, this.getEntityBoundingBox().grow(16.0D, 16.0D, 16.0D), EntitySelectors.NOT_SPECTATING).isEmpty()) {
            return PortiumSounds.ENTITY_FOX_SCREECH;
        }
        return PortiumSounds.ENTITY_FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_FOX_DEATH;
    }

    /** Foxes take fall damage only from 5 blocks. */
    @Override
    public void fall(float distance, float damageMultiplier) {
        super.fall(distance - 2.0F, damageMultiplier);
    }

    /** Whether nothing solid stands in the way of a pounce at {@code target}, 3 blocks high. */
    public static boolean isPathClear(EntityFox fox, EntityLivingBase target) {
        double dz = target.posZ - fox.posZ;
        double dx = target.posX - fox.posX;
        double slope = dz / dx;
        for (int i = 0; i < 6; ++i) {
            double z = slope == 0.0D ? 0.0D : dz * (double) ((float) i / 6.0F);
            double x = slope == 0.0D ? dx * (double) ((float) i / 6.0F) : z / slope;
            for (int y = 1; y < 4; ++y) {
                if (!fox.world.getBlockState(new BlockPos(fox.posX + x, fox.posY + y, fox.posZ + z)).getMaterial().isReplaceable()) return false;
            }
        }
        return true;
    }

    /** Mobs that keep a fox awake or wary: anything but other foxes, prey, monsters, tame pets, trusted or hidden players. */
    private boolean isAlertable(EntityLivingBase entity) {
        if (entity instanceof EntityFox) return false;
        if (entity instanceof EntityChicken || entity instanceof EntityRabbit || entity instanceof EntityMob) return true;
        if (entity instanceof EntityTameable) return !((EntityTameable) entity).isTamed();
        if (entity instanceof EntityPlayer && (((EntityPlayer) entity).isSpectator() || ((EntityPlayer) entity).isCreative())) return false;
        if (this.isTrustedUUID(entity.getUniqueID())) return false;
        return !(entity instanceof EntityPlayer && ((EntityPlayer) entity).isPlayerSleeping()) && !entity.isSneaking();
    }

    // ---- Variants ----

    public enum Type {
        RED(0, "red"),
        SNOW(1, "snow");

        private final int index;
        private final String name;

        Type(int index, String name) {
            this.index = index;
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        static Type byIndex(int index) {
            return index == 1 ? SNOW : RED;
        }

        static Type byName(String name) {
            return Arrays.stream(values()).filter(type -> type.name.equals(name)).findFirst().orElse(RED);
        }

        /** Snow foxes in snowy taigas, red elsewhere. */
        static Type forBiome(Biome biome) {
            return biome == Biomes.COLD_TAIGA || biome == Biomes.COLD_TAIGA_HILLS || biome == Biomes.MUTATED_TAIGA_COLD ? SNOW : RED;
        }
    }

    /** A group spawning together: one type, and the third fox on is a cub. */
    public static class FoxData implements IEntityLivingData {
        final Type type;
        int indexInGroup;

        FoxData(Type type) {
            this.type = type;
        }
    }

    // ---- Controllers ----

    private class FoxLookHelper extends LookHelper116 {
        FoxLookHelper() {
            super(EntityFox.this);
        }

        @Override
        public void onUpdateLook() {
            if (!EntityFox.this.isSleeping()) super.onUpdateLook();
        }

        @Override
        protected boolean shouldResetPitch() {
            return !EntityFox.this.isPouncing() && !EntityFox.this.isCrouching() && !EntityFox.this.isInterested() && !EntityFox.this.isStuck();
        }
    }

    private class FoxMoveHelper extends EntityMoveHelper {
        FoxMoveHelper() {
            super(EntityFox.this);
        }

        @Override
        public void onUpdateMoveHelper() {
            if (EntityFox.this.canMove()) super.onUpdateMoveHelper();
        }
    }

    // ---- Goals ----

    private class WatchGoal extends EntityAIWatchClosest {
        WatchGoal(float distance) {
            super(EntityFox.this, EntityPlayer.class, distance);
        }

        @Override
        public boolean shouldExecute() {
            return super.shouldExecute() && !EntityFox.this.isStuck() && !EntityFox.this.isInterested();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return super.shouldContinueExecuting() && !EntityFox.this.isStuck() && !EntityFox.this.isInterested();
        }
    }

    private class FollowGoal extends EntityAIFollowParent {
        FollowGoal(double speed) {
            super(EntityFox.this, speed);
        }

        @Override
        public boolean shouldExecute() {
            return !EntityFox.this.isFoxAggroed() && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !EntityFox.this.isFoxAggroed() && super.shouldContinueExecuting();
        }

        @Override
        public void startExecuting() {
            EntityFox.this.clearStates();
            super.startExecuting();
        }
    }

    /** Fully crouched behind prey with a clear way to it: leap up and on to it. Not interrupted midair. */
    private class PounceGoal extends EntityAIBase {
        PounceGoal() {
            this.setMutexBits(5);
        }

        @Override
        public boolean shouldExecute() {
            if (!EntityFox.this.isFullyCrouched()) return false;
            EntityLivingBase target = EntityFox.this.getAttackTarget();
            if (target == null || !target.isEntityAlive()) return false;
            if (target.getAdjustedHorizontalFacing() != target.getHorizontalFacing()) return false;
            boolean clear = isPathClear(EntityFox.this, target);
            if (!clear) {
                EntityFox.this.getNavigator().getPathToEntityLiving(target);
                EntityFox.this.setCrouching(false);
                EntityFox.this.setInterested(false);
            }
            return clear;
        }

        @Override
        public boolean shouldContinueExecuting() {
            EntityLivingBase target = EntityFox.this.getAttackTarget();
            if (target == null || !target.isEntityAlive()) return false;
            double motionY = EntityFox.this.motionY;
            return (motionY * motionY >= 0.05000000074505806D || Math.abs(EntityFox.this.rotationPitch) >= 15.0F || !EntityFox.this.onGround)
                    && !EntityFox.this.isStuck();
        }

        @Override
        public boolean isInterruptible() {
            return false;
        }

        @Override
        public void startExecuting() {
            EntityFox.this.setJumping(true);
            EntityFox.this.setPouncing(true);
            EntityFox.this.setInterested(false);
            EntityLivingBase target = EntityFox.this.getAttackTarget();
            EntityFox.this.getLookHelper().setLookPositionWithEntity(target, 60.0F, 30.0F);
            Vec3d direction = new Vec3d(target.posX - EntityFox.this.posX, target.posY - EntityFox.this.posY, target.posZ - EntityFox.this.posZ).normalize();
            EntityFox.this.motionX += direction.x * 0.8D;
            EntityFox.this.motionY += 0.9D;
            EntityFox.this.motionZ += direction.z * 0.8D;
            EntityFox.this.getNavigator().clearPath();
        }

        @Override
        public void resetTask() {
            EntityFox.this.setCrouching(false);
            EntityFox.this.crouchAmount = 0.0F;
            EntityFox.this.crouchAmountO = 0.0F;
            EntityFox.this.setInterested(false);
            EntityFox.this.setPouncing(false);
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = EntityFox.this.getAttackTarget();
            if (target != null) EntityFox.this.getLookHelper().setLookPositionWithEntity(target, 60.0F, 30.0F);
            if (!EntityFox.this.isStuck()) {
                double horizontal = Math.sqrt(EntityFox.this.motionX * EntityFox.this.motionX + EntityFox.this.motionZ * EntityFox.this.motionZ);
                double motionY = EntityFox.this.motionY;
                if (motionY * motionY < 0.029999999329447746D && EntityFox.this.rotationPitch != 0.0F) {
                    EntityFox.this.rotationPitch = rotLerp(EntityFox.this.rotationPitch, 0.0F, 0.2F);
                } else {
                    double length = Math.sqrt(horizontal * horizontal + motionY * motionY);
                    EntityFox.this.rotationPitch = (float) (Math.signum(-motionY) * Math.acos(horizontal / length) * 57.2957763671875D);
                }
            }
            if (target != null && EntityFox.this.getDistance(target) <= 2.0F) {
                EntityFox.this.attackEntityAsMob(target);
            } else if (EntityFox.this.rotationPitch > 0.0F && EntityFox.this.onGround && (float) EntityFox.this.motionY != 0.0F
                    && EntityFox.this.world.getBlockState(new BlockPos(EntityFox.this)).getBlock() == Blocks.SNOW_LAYER) {
                EntityFox.this.rotationPitch = 60.0F;
                EntityFox.this.setAttackTarget(null);
                EntityFox.this.setStuck(true);
            }
        }
    }

    private static float rotLerp(float from, float to, float amount) {
        return from + amount * MathHelper.wrapDegrees(to - from);
    }

    private class SwimGoal extends EntityAISwimming {
        SwimGoal() {
            super(EntityFox.this);
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            EntityFox.this.clearStates();
        }

        /** In water above a quarter block deep, or in lava. */
        @Override
        public boolean shouldExecute() {
            if (EntityFox.this.isInLava()) return true;
            if (!EntityFox.this.isInWater()) return false;
            IBlockState above = EntityFox.this.world.getBlockState(new BlockPos(EntityFox.this.posX, EntityFox.this.posY + 0.25D, EntityFox.this.posZ));
            return above.getMaterial() == net.minecraft.block.material.Material.WATER;
        }
    }

    /** At night in a village, wander through it. */
    private class StrollGoal extends EntityAIBase {
        private final int chance;
        @Nullable
        private BlockPos travelPosition;

        StrollGoal(int chance) {
            this.chance = chance;
            this.setMutexBits(1);
        }

        private boolean free() {
            return !EntityFox.this.isSleeping() && !EntityFox.this.isSitting() && !EntityFox.this.isFoxAggroed() && EntityFox.this.getAttackTarget() == null;
        }

        @Override
        public boolean shouldExecute() {
            if (!this.free() || EntityFox.this.isBeingRidden() || EntityFox.this.world.isDaytime()) return false;
            if (EntityFox.this.getRNG().nextInt(this.chance) != 0) return false;
            Village village = EntityFox.this.world.getVillageCollection().getNearestVillage(new BlockPos(EntityFox.this), 0);
            if (village == null || !village.isBlockPosWithinSqVillageRadius(new BlockPos(EntityFox.this))) return false;
            this.travelPosition = null;
            for (int i = 0; i < 10 && this.travelPosition == null; i++) {
                Vec3d target = RandomPositionGenerator.findRandomTarget(EntityFox.this, 15, 7);
                if (target != null && village.isBlockPosWithinSqVillageRadius(new BlockPos(target))) this.travelPosition = new BlockPos(target);
            }
            return this.travelPosition != null;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.free() && this.travelPosition != null && !EntityFox.this.getNavigator().noPath();
        }

        @Override
        public void startExecuting() {
            EntityFox.this.clearStates();
        }

        @Override
        public void updateTask() {
            if (this.travelPosition == null || !EntityFox.this.getNavigator().noPath()) return;
            Vec3d here = EntityFox.this.getPositionVector();
            if (here.squareDistanceTo(new Vec3d(this.travelPosition)) < 100.0D) return;
            Vec3d goal = new Vec3d(this.travelPosition.getX() + 0.5D, this.travelPosition.getY(), this.travelPosition.getZ() + 0.5D);
            goal = here.subtract(goal).scale(0.4D).add(goal);
            Vec3d step = goal.subtract(here).normalize().scale(10.0D).add(here);
            BlockPos ground = EntityFox.this.world.getTopSolidOrLiquidBlock(new BlockPos(step));
            if (!EntityFox.this.getNavigator().tryMoveToXYZ(ground.getX(), ground.getY(), ground.getZ(), 1.0D)) {
                BlockPos random = EntityFox.this.world.getTopSolidOrLiquidBlock(new BlockPos(EntityFox.this).add(-8 + EntityFox.this.rand.nextInt(16), 0,
                        -8 + EntityFox.this.rand.nextInt(16)));
                EntityFox.this.getNavigator().tryMoveToXYZ(random.getX(), random.getY(), random.getZ(), 1.0D);
            }
        }
    }

    private class PanicGoal extends EntityAIPanic {
        PanicGoal(double speed) {
            super(EntityFox.this, speed);
        }

        @Override
        public boolean shouldExecute() {
            return !EntityFox.this.isFoxAggroed() && super.shouldExecute();
        }
    }

    /** Stuck face-first in snow: thrash for 2 seconds. */
    private class JumpGoal extends EntityAIBase {
        private int delay;

        JumpGoal() {
            this.setMutexBits(7);
        }

        @Override
        public boolean shouldExecute() {
            return EntityFox.this.isStuck();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.shouldExecute() && this.delay > 0;
        }

        @Override
        public void startExecuting() {
            this.delay = 40;
        }

        @Override
        public void resetTask() {
            EntityFox.this.setStuck(false);
        }

        @Override
        public void updateTask() {
            --this.delay;
        }
    }

    /** Ripe sweet berry bushes within 12 blocks: walk up, sniff, pick them (one in the mouth, the rest dropped). */
    private class EatBerriesGoal extends EntityAIMoveToBlock116 {
        private int eatTicks;

        EatBerriesGoal(double speed, int length, int height) {
            super(EntityFox.this, speed, length, height);
        }

        @Override
        public double getTargetDistanceSq() {
            return 2.0D;
        }

        @Override
        public boolean shouldMove() {
            return this.timeoutCounter % 100 == 0;
        }

        @Override
        protected boolean shouldMoveTo(World world, BlockPos pos) {
            IBlockState state = world.getBlockState(pos);
            return state.getBlock() instanceof BlockSweetBerryBush && state.getValue(BlockSweetBerryBush.AGE) >= 2;
        }

        @Override
        public void updateTask() {
            if (this.getIsAboveDestination()) {
                if (this.eatTicks >= 40) this.eatBerry();
                else ++this.eatTicks;
            } else if (EntityFox.this.rand.nextFloat() < 0.05F) {
                EntityFox.this.playSound(PortiumSounds.ENTITY_FOX_SNIFF, 1.0F, 1.0F);
            }
            super.updateTask();
        }

        private void eatBerry() {
            World world = EntityFox.this.world;
            if (!world.getGameRules().getBoolean("mobGriefing") || PortiumItems.SWEET_BERRIES == null) return;
            IBlockState state = world.getBlockState(this.destinationBlock);
            if (!(state.getBlock() instanceof BlockSweetBerryBush)) return;
            int age = state.getValue(BlockSweetBerryBush.AGE);
            int count = 1 + world.rand.nextInt(2) + (age == 3 ? 1 : 0);
            if (EntityFox.this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty()) {
                EntityFox.this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(PortiumItems.SWEET_BERRIES));
                --count;
            }
            if (count > 0) Block.spawnAsEntity(world, this.destinationBlock, new ItemStack(PortiumItems.SWEET_BERRIES, count));
            EntityFox.this.playSound(PortiumSounds.ITEM_SWEET_BERRIES_PICK_FROM_BUSH, 1.0F, 1.0F);
            world.setBlockState(this.destinationBlock, state.withProperty(BlockSweetBerryBush.AGE, 1), 2);
        }

        @Override
        public boolean shouldExecute() {
            return !EntityFox.this.isSleeping() && super.shouldExecute();
        }

        @Override
        public void startExecuting() {
            this.eatTicks = 0;
            EntityFox.this.setSitting(false);
            super.startExecuting();
        }
    }

    /** Wary mobs within 12 blocks (sight not needed). */
    private boolean alertableNearby() {
        return !this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(12.0D, 6.0D, 12.0D),
                entity -> entity != this && entity.isEntityAlive() && !(entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator())
                        && this.getDistanceSq(entity) <= 144.0D && this.isAlertable(entity)).isEmpty();
    }

    /** Out of the sky with a spot fit to rest on. */
    private boolean inShade() {
        BlockPos pos = new BlockPos(this.posX, this.getEntityBoundingBox().maxY, this.posZ);
        return !this.world.canSeeSky(pos) && this.getBlockPathWeight(pos) >= 0.0F;
    }

    /** Now and then, sit down and look about. */
    private class SitAndLookGoal extends EntityAIBase {
        private double lookX;
        private double lookZ;
        private int lookTime;
        private int looksLeft;

        SitAndLookGoal() {
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            return EntityFox.this.getRevengeTarget() == null && EntityFox.this.getRNG().nextFloat() < 0.02F && !EntityFox.this.isSleeping()
                    && EntityFox.this.getAttackTarget() == null && EntityFox.this.getNavigator().noPath() && !EntityFox.this.alertableNearby()
                    && !EntityFox.this.isPouncing() && !EntityFox.this.isCrouching();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.looksLeft > 0;
        }

        @Override
        public void startExecuting() {
            this.newLook();
            this.looksLeft = 2 + EntityFox.this.getRNG().nextInt(3);
            EntityFox.this.setSitting(true);
            EntityFox.this.getNavigator().clearPath();
        }

        @Override
        public void resetTask() {
            EntityFox.this.setSitting(false);
        }

        @Override
        public void updateTask() {
            --this.lookTime;
            if (this.lookTime <= 0) {
                --this.looksLeft;
                this.newLook();
            }
            EntityFox.this.getLookHelper().setLookPosition(EntityFox.this.posX + this.lookX, EntityFox.this.posY + EntityFox.this.getEyeHeight(),
                    EntityFox.this.posZ + this.lookZ, (float) EntityFox.this.getHorizontalFaceSpeed(), (float) EntityFox.this.getVerticalFaceSpeed());
        }

        private void newLook() {
            double angle = (Math.PI * 2.0D) * EntityFox.this.getRNG().nextDouble();
            this.lookX = Math.cos(angle);
            this.lookZ = Math.sin(angle);
            this.lookTime = 80 + EntityFox.this.getRNG().nextInt(20);
        }
    }

    /** By day, in the shade and with nothing wary about: lie down and sleep. */
    private class SleepGoal extends EntityAIBase {
        private int countdown;

        SleepGoal() {
            this.countdown = EntityFox.this.rand.nextInt(140);
            this.setMutexBits(7);
        }

        @Override
        public boolean shouldExecute() {
            if (EntityFox.this.moveStrafing != 0.0F || EntityFox.this.moveVertical != 0.0F || EntityFox.this.moveForward != 0.0F) return false;
            return this.canSleep() || EntityFox.this.isSleeping();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.canSleep();
        }

        private boolean canSleep() {
            if (this.countdown > 0) {
                --this.countdown;
                return false;
            }
            return EntityFox.this.world.isDaytime() && EntityFox.this.inShade() && !EntityFox.this.alertableNearby();
        }

        @Override
        public void resetTask() {
            this.countdown = EntityFox.this.rand.nextInt(140);
            EntityFox.this.clearStates();
        }

        @Override
        public void startExecuting() {
            EntityFox.this.setSitting(false);
            EntityFox.this.setCrouching(false);
            EntityFox.this.setInterested(false);
            EntityFox.this.setJumping(false);
            EntityFox.this.setSleeping(true);
            EntityFox.this.getNavigator().clearPath();
            EntityFox.this.getMoveHelper().setMoveTo(EntityFox.this.posX, EntityFox.this.posY, EntityFox.this.posZ, 0.0D);
        }
    }

    /** By day in the open (not in a village), or in a thunderstorm: go for cover. */
    private class FindShelterGoal extends EntityAIBase {
        private final double speed;
        private double shelterX;
        private double shelterY;
        private double shelterZ;
        private int cooldown = 100;

        FindShelterGoal(double speed) {
            this.speed = speed;
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (EntityFox.this.isSleeping() || EntityFox.this.getAttackTarget() != null) return false;
            if (EntityFox.this.world.isThundering()) return this.findShelter();
            if (this.cooldown > 0) {
                --this.cooldown;
                return false;
            }
            this.cooldown = 100;
            BlockPos pos = new BlockPos(EntityFox.this);
            return EntityFox.this.world.isDaytime() && EntityFox.this.world.canSeeSky(pos) && !isVillage(pos) && this.findShelter();
        }

        private boolean isVillage(BlockPos pos) {
            Village village = EntityFox.this.world.getVillageCollection().getNearestVillage(pos, 32);
            return village != null;
        }

        private boolean findShelter() {
            for (int i = 0; i < 10; ++i) {
                BlockPos pos = new BlockPos(EntityFox.this).add(EntityFox.this.rand.nextInt(20) - 10, EntityFox.this.rand.nextInt(6) - 3,
                        EntityFox.this.rand.nextInt(20) - 10);
                if (!EntityFox.this.world.canSeeSky(pos) && EntityFox.this.getBlockPathWeight(pos) < 0.0F) {
                    this.shelterX = pos.getX() + 0.5D;
                    this.shelterY = pos.getY();
                    this.shelterZ = pos.getZ() + 0.5D;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !EntityFox.this.getNavigator().noPath();
        }

        @Override
        public void startExecuting() {
            EntityFox.this.clearStates();
            EntityFox.this.getNavigator().tryMoveToXYZ(this.shelterX, this.shelterY, this.shelterZ, this.speed);
        }
    }

    /** When something hurts a player the fox trusts, it goes for it. */
    private class RevengeGoal extends EntityAINearestAttackableTarget<EntityLivingBase> {
        @Nullable
        private EntityLivingBase offender;
        private EntityLivingBase trusted;
        private int lastRevengeTimer;

        RevengeGoal(Predicate<EntityLivingBase> predicate) {
            super(EntityFox.this, EntityLivingBase.class, 10, false, false, predicate::test);
        }

        @Override
        public boolean shouldExecute() {
            if (this.taskOwner.getRNG().nextInt(10) != 0) return false;
            for (UUID uuid : EntityFox.this.getTrustedUUIDs()) {
                if (uuid == null || !(EntityFox.this.world instanceof WorldServer)) continue;
                Entity entity = ((WorldServer) EntityFox.this.world).getEntityFromUuid(uuid);
                if (entity instanceof EntityLivingBase) {
                    EntityLivingBase living = (EntityLivingBase) entity;
                    this.trusted = living;
                    this.offender = living.getRevengeTarget();
                    int timer = living.getRevengeTimer();
                    return timer != this.lastRevengeTimer && this.isSuitableTarget(this.offender, false);
                }
            }
            return false;
        }

        @Override
        public void startExecuting() {
            this.taskOwner.setAttackTarget(this.offender);
            this.targetEntity = this.offender;
            if (this.trusted != null) this.lastRevengeTimer = this.trusted.getRevengeTimer();
            EntityFox.this.playSound(PortiumSounds.ENTITY_FOX_AGGRO, 1.0F, 1.0F);
            EntityFox.this.setFoxAggroed(true);
            EntityFox.this.wakeUp();
            super.startExecuting();
        }
    }

    /** Cubs trust the players who fed their parents. */
    private class MateGoal extends EntityAIBreed116 {
        MateGoal(double speed) {
            super(EntityFox.this, speed);
        }

        @Override
        public void startExecuting() {
            ((EntityFox) this.animal).clearStates();
            ((EntityFox) this.targetMate).clearStates();
            super.startExecuting();
        }

        @Override
        protected void spawnBaby() {
            EntityFox baby = (EntityFox) this.animal.createChild(this.targetMate);
            if (baby == null) return;
            EntityPlayerMP first = this.animal.getLoveCause();
            EntityPlayerMP second = this.targetMate.getLoveCause();
            EntityPlayerMP credited = first;
            if (first != null) baby.addTrustedUUID(first.getUniqueID());
            else credited = second;
            if (second != null && first != second) baby.addTrustedUUID(second.getUniqueID());
            if (credited != null) {
                credited.addStat(net.minecraft.stats.StatList.ANIMALS_BRED);
                net.minecraft.advancements.CriteriaTriggers.BRED_ANIMALS.trigger(credited, this.animal, this.targetMate, baby);
            }
            this.finishBreeding(baby);
        }
    }

    private class BiteGoal extends EntityAIAttackMelee {
        BiteGoal(double speed, boolean useLongMemory) {
            super(EntityFox.this, speed, useLongMemory);
        }

        @Override
        protected void checkAndPerformAttack(EntityLivingBase enemy, double distanceSq) {
            if (distanceSq <= this.getAttackReachSqr(enemy) && this.attackTick <= 0) {
                this.attackTick = 20;
                this.attacker.attackEntityAsMob(enemy);
                EntityFox.this.playSound(PortiumSounds.ENTITY_FOX_BITE, 1.0F, 1.0F);
            }
        }

        @Override
        public void startExecuting() {
            EntityFox.this.setInterested(false);
            super.startExecuting();
        }

        @Override
        public boolean shouldExecute() {
            return !EntityFox.this.isSitting() && !EntityFox.this.isSleeping() && !EntityFox.this.isCrouching() && !EntityFox.this.isStuck()
                    && super.shouldExecute();
        }
    }

    /** Chases prey to within 6 blocks, then crouches and creeps (if the way is clear) ready to pounce. */
    private class FollowTargetGoal extends EntityAIBase {
        FollowTargetGoal() {
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            if (EntityFox.this.isSleeping()) return false;
            EntityLivingBase target = EntityFox.this.getAttackTarget();
            return target != null && target.isEntityAlive() && IS_PREY.test(target) && EntityFox.this.getDistanceSq(target) > 36.0D
                    && !EntityFox.this.isCrouching() && !EntityFox.this.isInterested() && !EntityFox.this.isJumping;
        }

        @Override
        public void startExecuting() {
            EntityFox.this.setSitting(false);
            EntityFox.this.setStuck(false);
        }

        @Override
        public void resetTask() {
            EntityLivingBase target = EntityFox.this.getAttackTarget();
            if (target != null && isPathClear(EntityFox.this, target)) {
                EntityFox.this.setInterested(true);
                EntityFox.this.setCrouching(true);
                EntityFox.this.getNavigator().clearPath();
                EntityFox.this.getLookHelper().setLookPositionWithEntity(target, (float) EntityFox.this.getHorizontalFaceSpeed(),
                        (float) EntityFox.this.getVerticalFaceSpeed());
            } else {
                EntityFox.this.setInterested(false);
                EntityFox.this.setCrouching(false);
            }
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = EntityFox.this.getAttackTarget();
            EntityFox.this.getLookHelper().setLookPositionWithEntity(target, (float) EntityFox.this.getHorizontalFaceSpeed(),
                    (float) EntityFox.this.getVerticalFaceSpeed());
            if (EntityFox.this.getDistanceSq(target) <= 36.0D) {
                EntityFox.this.setInterested(true);
                EntityFox.this.setCrouching(true);
                EntityFox.this.getNavigator().clearPath();
            } else {
                EntityFox.this.getNavigator().tryMoveToEntityLiving(target, 1.5D);
            }
        }
    }

    /** With an empty mouth, go for items lying within 8 blocks. */
    private class FindItemsGoal extends EntityAIBase {
        FindItemsGoal() {
            this.setMutexBits(1);
        }

        private List<EntityItem> items() {
            return EntityFox.this.world.getEntitiesWithinAABB(EntityItem.class, EntityFox.this.getEntityBoundingBox().grow(8.0D, 8.0D, 8.0D),
                    TRUSTED_TARGET_SELECTOR::test);
        }

        @Override
        public boolean shouldExecute() {
            if (!EntityFox.this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty()) return false;
            if (EntityFox.this.getAttackTarget() != null || EntityFox.this.getRevengeTarget() != null) return false;
            if (!EntityFox.this.canMove()) return false;
            if (EntityFox.this.getRNG().nextInt(10) != 0) return false;
            return !this.items().isEmpty() && EntityFox.this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty();
        }

        @Override
        public void updateTask() {
            List<EntityItem> items = this.items();
            if (EntityFox.this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty() && !items.isEmpty()) {
                EntityFox.this.getNavigator().tryMoveToEntityLiving(items.get(0), 1.2000000476837158D);
            }
        }

        @Override
        public void startExecuting() {
            List<EntityItem> items = this.items();
            if (!items.isEmpty()) EntityFox.this.getNavigator().tryMoveToEntityLiving(items.get(0), 1.2000000476837158D);
        }
    }
}
