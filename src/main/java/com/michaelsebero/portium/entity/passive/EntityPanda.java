package com.michaelsebero.portium.entity.passive;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.entity.MobAttack;
import com.michaelsebero.portium.entity.ai.EntityAIBreed116;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.proxy.ParticleKind;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * The panda (1.14), in jungles. Two genes (main and hidden) make its personality: lazy ones lie on their backs, worried
 * ones hide from players and monsters and cower in storms, playful ones roll about, weak ones sneeze a lot, aggressive
 * ones fight back (and join fights), brown and weak need both genes. Pandas pick up and sit to eat bamboo and cake,
 * breed only with bamboo growing nearby (else they sulk), and their cubs sneeze, making adults jump.
 */
public class EntityPanda extends EntityAnimal {

    private static final DataParameter<Integer> UNHAPPY_COUNTER = EntityDataManager.createKey(EntityPanda.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> SNEEZE_COUNTER = EntityDataManager.createKey(EntityPanda.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> EAT_COUNTER = EntityDataManager.createKey(EntityPanda.class, DataSerializers.VARINT);
    private static final DataParameter<Byte> MAIN_GENE = EntityDataManager.createKey(EntityPanda.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> HIDDEN_GENE = EntityDataManager.createKey(EntityPanda.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> PANDA_FLAGS = EntityDataManager.createKey(EntityPanda.class, DataSerializers.BYTE);

    private static final int FLAG_SNEEZING = 2;
    private static final int FLAG_ROLLING = 4;
    private static final int FLAG_SITTING = 8;
    private static final int FLAG_ON_BACK = 16;

    private static final ResourceLocation LOOT = new ResourceLocation(com.michaelsebero.portium.Tags.MOD_ID, "entities/panda");
    private static final Predicate<EntityItem> PANDA_ITEMS = item -> isPandaFood(item.getItem()) && item.isEntityAlive() && !item.cannotPickup();

    private boolean gotBamboo;
    private boolean didBite;
    public int rollCounter;
    private Vec3d rollDelta = Vec3d.ZERO;
    private float sitAmount;
    private float sitAmountO;
    private float onBackAmount;
    private float onBackAmountO;
    private float rollAmount;
    private float rollAmountO;
    private WatchGoal watchGoal;

    public EntityPanda(World world) {
        super(world);
        this.setSize(1.3F, 1.25F);
        this.moveHelper = new PandaMoveHelper();
        if (!this.isChild()) this.setCanPickUpLoot(true);
    }

    @Nullable
    private static Item bamboo() {
        return PortiumBlocks.BAMBOO == null ? null : Item.getItemFromBlock(PortiumBlocks.BAMBOO);
    }

    private static boolean isPandaFood(ItemStack stack) {
        Item item = stack.getItem();
        return item != Items.AIR && (item == bamboo() || item == Items.CAKE);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(UNHAPPY_COUNTER, 0);
        this.dataManager.register(SNEEZE_COUNTER, 0);
        this.dataManager.register(MAIN_GENE, (byte) 0);
        this.dataManager.register(HIDDEN_GENE, (byte) 0);
        this.dataManager.register(PANDA_FLAGS, (byte) 0);
        this.dataManager.register(EAT_COUNTER, 0);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(2, new PanicGoal(2.0D));
        this.tasks.addTask(2, new MateGoal(1.0D));
        this.tasks.addTask(3, new AttackGoal(1.2000000476837158D, true));
        Item bamboo = bamboo();
        if (bamboo != null) this.tasks.addTask(4, new EntityAITempt(this, 1.0D, bamboo, false));
        this.tasks.addTask(6, new AvoidGoal<>(EntityPlayer.class, 8.0F, 2.0D, 2.0D));
        this.tasks.addTask(6, new AvoidGoal<>(EntityMob.class, 4.0F, 2.0D, 2.0D));
        this.tasks.addTask(7, new SitGoal());
        this.tasks.addTask(8, new LieBackGoal());
        this.tasks.addTask(8, new ChildPlayGoal());
        this.watchGoal = new WatchGoal(EntityPlayer.class, 6.0F);
        this.tasks.addTask(9, this.watchGoal);
        this.tasks.addTask(10, new EntityAILookIdle(this));
        this.tasks.addTask(12, new RollGoal());
        this.tasks.addTask(13, new EntityAIFollowParent(this, 1.25D));
        this.tasks.addTask(14, new EntityAIWanderAvoidWater(this, 1.0D));
        this.targetTasks.addTask(1, new RevengeGoal());
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.15000000596046448D);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0D);
    }

    // ---- State ----

    public int getUnhappyCounter() {
        return this.dataManager.get(UNHAPPY_COUNTER);
    }

    public void setUnhappyCounter(int counter) {
        this.dataManager.set(UNHAPPY_COUNTER, counter);
    }

    public boolean isSneezing() {
        return this.getPandaFlag(FLAG_SNEEZING);
    }

    public boolean isSitting() {
        return this.getPandaFlag(FLAG_SITTING);
    }

    public void setSitting(boolean sitting) {
        this.setPandaFlag(FLAG_SITTING, sitting);
    }

    public boolean isOnBack() {
        return this.getPandaFlag(FLAG_ON_BACK);
    }

    public void setOnBack(boolean onBack) {
        this.setPandaFlag(FLAG_ON_BACK, onBack);
    }

    public boolean isEating() {
        return this.dataManager.get(EAT_COUNTER) > 0;
    }

    public void setEating(boolean eating) {
        this.dataManager.set(EAT_COUNTER, eating ? 1 : 0);
    }

    private int getEatCounter() {
        return this.dataManager.get(EAT_COUNTER);
    }

    private void setEatCounter(int counter) {
        this.dataManager.set(EAT_COUNTER, counter);
    }

    public void setSneezing(boolean sneezing) {
        this.setPandaFlag(FLAG_SNEEZING, sneezing);
        if (!sneezing) this.setSneezeCounter(0);
    }

    public int getSneezeCounter() {
        return this.dataManager.get(SNEEZE_COUNTER);
    }

    public void setSneezeCounter(int counter) {
        this.dataManager.set(SNEEZE_COUNTER, counter);
    }

    public Gene getMainGene() {
        return Gene.byIndex(this.dataManager.get(MAIN_GENE));
    }

    public void setMainGene(Gene gene) {
        if (gene.index > 6) gene = Gene.getRandomType(this.rand);
        this.dataManager.set(MAIN_GENE, (byte) gene.index);
    }

    public Gene getHiddenGene() {
        return Gene.byIndex(this.dataManager.get(HIDDEN_GENE));
    }

    public void setHiddenGene(Gene gene) {
        if (gene.index > 6) gene = Gene.getRandomType(this.rand);
        this.dataManager.set(HIDDEN_GENE, (byte) gene.index);
    }

    public boolean isRolling() {
        return this.getPandaFlag(FLAG_ROLLING);
    }

    public void setRolling(boolean rolling) {
        this.setPandaFlag(FLAG_ROLLING, rolling);
    }

    private boolean getPandaFlag(int flag) {
        return (this.dataManager.get(PANDA_FLAGS) & flag) != 0;
    }

    private void setPandaFlag(int flag, boolean on) {
        byte flags = this.dataManager.get(PANDA_FLAGS);
        this.dataManager.set(PANDA_FLAGS, (byte) (on ? flags | flag : flags & ~flag));
    }

    /** The gene that shows: a recessive one (brown, weak) only when both genes are it. */
    public Gene getVisibleGene() {
        return Gene.visible(this.getMainGene(), this.getHiddenGene());
    }

    public boolean isLazy() {
        return this.getVisibleGene() == Gene.LAZY;
    }

    public boolean isWorried() {
        return this.getVisibleGene() == Gene.WORRIED;
    }

    public boolean isPlayful() {
        return this.getVisibleGene() == Gene.PLAYFUL;
    }

    public boolean isWeak() {
        return this.getVisibleGene() == Gene.WEAK;
    }

    public boolean isAggressive() {
        return this.getVisibleGene() == Gene.AGGRESSIVE;
    }

    /** A worried panda in a thunderstorm, cowering. */
    public boolean isScared() {
        return this.isWorried() && this.world.isThundering();
    }

    /** Free to do something: not lying back, cowering, eating, rolling or sitting. */
    public boolean canPerformAction() {
        return !this.isOnBack() && !this.isScared() && !this.isEating() && !this.isRolling() && !this.isSitting();
    }

    public float getSitAmount(float partialTicks) {
        return this.sitAmountO + (this.sitAmount - this.sitAmountO) * partialTicks;
    }

    public float getOnBackAmount(float partialTicks) {
        return this.onBackAmountO + (this.onBackAmount - this.onBackAmountO) * partialTicks;
    }

    public float getRollAmount(float partialTicks) {
        return this.rollAmountO + (this.rollAmount - this.rollAmountO) * partialTicks;
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return false;
    }

    // ---- Saving ----

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setString("MainGene", this.getMainGene().name);
        compound.setString("HiddenGene", this.getHiddenGene().name);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setMainGene(Gene.byName(compound.getString("MainGene")));
        this.setHiddenGene(Gene.byName(compound.getString("HiddenGene")));
    }

    // ---- Breeding and spawning ----

    @Nullable
    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        EntityPanda baby = new EntityPanda(this.world);
        if (mate instanceof EntityPanda) baby.inheritGenes(this, (EntityPanda) mate);
        baby.setAttributes();
        return baby;
    }

    /** One gene from each parent at random (or a random one for a lone parent), each mutating 1 time in 32. */
    public void inheritGenes(EntityPanda parent, @Nullable EntityPanda other) {
        if (other == null) {
            if (this.rand.nextBoolean()) {
                this.setMainGene(parent.randomGene());
                this.setHiddenGene(Gene.getRandomType(this.rand));
            } else {
                this.setMainGene(Gene.getRandomType(this.rand));
                this.setHiddenGene(parent.randomGene());
            }
        } else if (this.rand.nextBoolean()) {
            this.setMainGene(parent.randomGene());
            this.setHiddenGene(other.randomGene());
        } else {
            this.setMainGene(other.randomGene());
            this.setHiddenGene(parent.randomGene());
        }
        if (this.rand.nextInt(32) == 0) this.setMainGene(Gene.getRandomType(this.rand));
        if (this.rand.nextInt(32) == 0) this.setHiddenGene(Gene.getRandomType(this.rand));
    }

    private Gene randomGene() {
        return this.rand.nextBoolean() ? this.getMainGene() : this.getHiddenGene();
    }

    /** Weak pandas have 10 health, lazy ones crawl. */
    public void setAttributes() {
        if (this.isWeak()) this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        if (this.isLazy()) this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.07000000029802322D);
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        this.setMainGene(Gene.getRandomType(this.rand));
        this.setHiddenGene(Gene.getRandomType(this.rand));
        this.setAttributes();
        if (!(data instanceof GroupData)) data = new GroupData();
        GroupData group = (GroupData) data;
        if (group.index > 0 && this.rand.nextFloat() <= 0.2F) this.setGrowingAge(-24000);
        group.index++;
        this.setHealth(this.getMaxHealth());
        return super.onInitialSpawn(difficulty, data);
    }

    /** Pandas spawning together: from the second on, each a cub one time in five. */
    private static class GroupData implements IEntityLivingData {
        int index;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        Item bamboo = bamboo();
        return bamboo != null && stack.getItem() == bamboo;
    }

    private boolean isBreedingItemOrCake(ItemStack stack) {
        return this.isBreedingItem(stack) || stack.getItem() == Items.CAKE;
    }

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    // ---- Ticking ----

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.isWorried()) {
            if (this.world.isThundering() && !this.isInWater()) {
                this.setSitting(true);
                this.setEating(false);
            } else if (!this.isEating()) {
                this.setSitting(false);
            }
        }
        if (this.getAttackTarget() == null) {
            this.gotBamboo = false;
            this.didBite = false;
        }
        if (this.getUnhappyCounter() > 0) {
            if (this.getAttackTarget() != null) this.faceEntity(this.getAttackTarget(), 90.0F, 90.0F);
            if (this.getUnhappyCounter() == 29 || this.getUnhappyCounter() == 14) this.playSound(PortiumSounds.ENTITY_PANDA_CANT_BREED, 1.0F, 1.0F);
            this.setUnhappyCounter(this.getUnhappyCounter() - 1);
        }
        if (this.isSneezing()) {
            this.setSneezeCounter(this.getSneezeCounter() + 1);
            if (this.getSneezeCounter() > 20) {
                this.setSneezing(false);
                this.sneeze();
            } else if (this.getSneezeCounter() == 1) {
                this.playSound(PortiumSounds.ENTITY_PANDA_PRE_SNEEZE, 1.0F, 1.0F);
            }
        }
        if (this.isRolling()) this.handleRoll();
        else this.rollCounter = 0;
        if (this.isSitting()) this.rotationPitch = 0.0F;
        this.updateSitAmount();
        this.handleEating();
        this.updateOnBackAmount();
        this.updateRollAmount();
    }

    private void handleEating() {
        ItemStack held = this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        if (!this.isEating() && this.isSitting() && !this.isScared() && !held.isEmpty() && this.rand.nextInt(80) == 1) {
            this.setEating(true);
        } else if (held.isEmpty() || !this.isSitting()) {
            this.setEating(false);
        }
        if (this.isEating()) {
            this.eatEffects();
            if (!this.world.isRemote && this.getEatCounter() > 80 && this.rand.nextInt(20) == 1) {
                if (this.getEatCounter() > 100 && this.isBreedingItemOrCake(this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND))) {
                    this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    this.setSitting(false);
                }
                this.setEating(false);
                return;
            }
            this.setEatCounter(this.getEatCounter() + 1);
        }
    }

    private void eatEffects() {
        if (this.getEatCounter() % 5 != 0) return;
        this.playSound(PortiumSounds.ENTITY_PANDA_EAT, 0.5F + 0.5F * (float) this.rand.nextInt(2), (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
        ItemStack held = this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        for (int i = 0; i < 6; ++i) {
            Vec3d speed = new Vec3d((this.rand.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, (this.rand.nextFloat() - 0.5D) * 0.1D)
                    .rotatePitch(-this.rotationPitch * 0.017453292F).rotateYaw(-this.rotationYaw * 0.017453292F);
            double y = (double) (-this.rand.nextFloat()) * 0.6D - 0.3D;
            Vec3d at = new Vec3d((this.rand.nextFloat() - 0.5D) * 0.8D, y, 1.0D + (this.rand.nextFloat() - 0.5D) * 0.4D)
                    .rotateYaw(-this.renderYawOffset * 0.017453292F).add(this.posX, this.posY + this.getEyeHeight() + 1.0D, this.posZ);
            this.world.spawnParticle(EnumParticleTypes.ITEM_CRACK, at.x, at.y, at.z, speed.x, speed.y + 0.05D, speed.z, Item.getIdFromItem(held.getItem()),
                    held.getMetadata());
        }
    }

    private void updateSitAmount() {
        this.sitAmountO = this.sitAmount;
        this.sitAmount = this.isSitting() ? Math.min(1.0F, this.sitAmount + 0.15F) : Math.max(0.0F, this.sitAmount - 0.19F);
    }

    private void updateOnBackAmount() {
        this.onBackAmountO = this.onBackAmount;
        this.onBackAmount = this.isOnBack() ? Math.min(1.0F, this.onBackAmount + 0.15F) : Math.max(0.0F, this.onBackAmount - 0.19F);
    }

    private void updateRollAmount() {
        this.rollAmountO = this.rollAmount;
        this.rollAmount = this.isRolling() ? Math.min(1.0F, this.rollAmount + 0.15F) : Math.max(0.0F, this.rollAmount - 0.19F);
    }

    /** A somersault: a hop forward, then bounces on ticks 7, 15 and 23, for 32 ticks. */
    private void handleRoll() {
        ++this.rollCounter;
        if (this.rollCounter > 32) {
            this.setRolling(false);
            return;
        }
        if (this.world.isRemote) return;
        if (this.rollCounter == 1) {
            float yaw = this.rotationYaw * 0.017453292F;
            float speed = this.isChild() ? 0.1F : 0.2F;
            this.rollDelta = new Vec3d(this.motionX + (double) (-MathHelper.sin(yaw) * speed), 0.0D, this.motionZ + (double) (MathHelper.cos(yaw) * speed));
            this.motionX = this.rollDelta.x;
            this.motionY = this.rollDelta.y + 0.27D;
            this.motionZ = this.rollDelta.z;
        } else if (this.rollCounter != 7 && this.rollCounter != 15 && this.rollCounter != 23) {
            this.motionX = this.rollDelta.x;
            this.motionZ = this.rollDelta.z;
        } else {
            this.motionX = 0.0D;
            if (this.onGround) this.motionY = 0.27D;
            this.motionZ = 0.0D;
        }
    }

    /** A sneeze: a green puff, nearby grown pandas on the ground jump, and now and then a slimeball. */
    private void sneeze() {
        if (this.world.isRemote) {
            Portium.proxy.spawnParticle(ParticleKind.SNEEZE, this.world,
                    this.posX - (double) (this.width + 1.0F) * 0.5D * (double) MathHelper.sin(this.renderYawOffset * 0.017453292F),
                    this.posY + this.getEyeHeight() - 0.10000000149011612D,
                    this.posZ + (double) (this.width + 1.0F) * 0.5D * (double) MathHelper.cos(this.renderYawOffset * 0.017453292F), this.motionX, 0.0D,
                    this.motionZ);
        }
        this.playSound(PortiumSounds.ENTITY_PANDA_SNEEZE, 1.0F, 1.0F);
        for (EntityPanda panda : this.world.getEntitiesWithinAABB(EntityPanda.class, this.getEntityBoundingBox().grow(10.0D))) {
            if (!panda.isChild() && panda.onGround && !panda.isInWater() && panda.canPerformAction()) panda.jump();
        }
        if (!this.world.isRemote && this.rand.nextInt(700) == 0 && this.world.getGameRules().getBoolean("doMobLoot")) {
            this.dropItem(Items.SLIME_BALL, 1);
        }
    }

    // ---- Items, interaction, damage ----

    @Override
    protected void updateEquipmentIfNeeded(EntityItem itemEntity) {
        if (!this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty() || !PANDA_ITEMS.test(itemEntity)) return;
        ItemStack stack = itemEntity.getItem();
        this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, stack);
        this.inventoryHandsDropChances[EntityEquipmentSlot.MAINHAND.getIndex()] = 2.0F;
        this.onItemPickup(itemEntity, stack.getCount());
        itemEntity.setDead();
    }

    private void tryToSit() {
        if (this.isInWater()) return;
        this.setMoveForward(0.0F);
        this.getNavigator().clearPath();
        this.setSitting(true);
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (this.isScared()) return false;
        if (this.isOnBack()) {
            this.setOnBack(false);
            return true;
        }
        if (!this.isBreedingItem(stack)) return false;
        if (this.getAttackTarget() != null) this.gotBamboo = true;
        if (this.isChild()) {
            this.consumeItemFromStack(player, stack);
            this.ageUp((int) ((float) (-this.getGrowingAge() / 20) * 0.1F), true);
        } else if (!this.world.isRemote && this.getGrowingAge() == 0 && !this.isInLove()) {
            this.consumeItemFromStack(player, stack);
            this.setInLove(player);
        } else {
            if (this.world.isRemote || this.isSitting() || this.isInWater()) return false;
            this.tryToSit();
            this.setEating(true);
            ItemStack held = this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
            if (!held.isEmpty() && !player.capabilities.isCreativeMode) this.entityDropItem(held, 0.0F);
            this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(stack.getItem(), 1));
            this.consumeItemFromStack(player, stack);
        }
        return true;
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        this.playSound(PortiumSounds.ENTITY_PANDA_BITE, 1.0F, 1.0F);
        if (!this.isAggressive()) this.didBite = true;
        return MobAttack.attack(this, target);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        this.setSitting(false);
        return super.attackEntityFrom(source, amount);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isAggressive()) return PortiumSounds.ENTITY_PANDA_AGGRESSIVE_AMBIENT;
        return this.isWorried() ? PortiumSounds.ENTITY_PANDA_WORRIED_AMBIENT : PortiumSounds.ENTITY_PANDA_AMBIENT;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        this.playSound(PortiumSounds.ENTITY_PANDA_STEP, 0.15F, 1.0F);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_PANDA_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_PANDA_HURT;
    }

    // ---- Genes ----

    public enum Gene {
        NORMAL(0, "normal", false),
        LAZY(1, "lazy", false),
        WORRIED(2, "worried", false),
        PLAYFUL(3, "playful", false),
        BROWN(4, "brown", true),
        WEAK(5, "weak", true),
        AGGRESSIVE(6, "aggressive", false);

        private static final Gene[] BY_INDEX = values();
        private final int index;
        private final String name;
        private final boolean recessive;

        Gene(int index, String name, boolean recessive) {
            this.index = index;
            this.name = name;
            this.recessive = recessive;
        }

        public String getName() {
            return this.name;
        }

        static Gene visible(Gene main, Gene hidden) {
            if (main.recessive) return main == hidden ? main : NORMAL;
            return main;
        }

        public static Gene byIndex(int index) {
            return index < 0 || index >= BY_INDEX.length ? NORMAL : BY_INDEX[index];
        }

        public static Gene byName(String name) {
            for (Gene gene : BY_INDEX) {
                if (gene.name.equals(name)) return gene;
            }
            return NORMAL;
        }

        /** 1.14's weights out of 16: lazy, worried, playful, aggressive 1 each, weak 5, brown 2, normal 5. */
        public static Gene getRandomType(Random random) {
            int roll = random.nextInt(16);
            if (roll == 0) return LAZY;
            if (roll == 1) return WORRIED;
            if (roll == 2) return PLAYFUL;
            if (roll == 4) return AGGRESSIVE;
            if (roll < 9) return WEAK;
            return roll < 11 ? BROWN : NORMAL;
        }
    }

    // ---- Goals ----

    private class PandaMoveHelper extends EntityMoveHelper {
        PandaMoveHelper() {
            super(EntityPanda.this);
        }

        @Override
        public void onUpdateMoveHelper() {
            if (EntityPanda.this.canPerformAction()) super.onUpdateMoveHelper();
        }
    }

    /** Only while burning: run for water, or anywhere. */
    private class PanicGoal extends EntityAIBase {
        private final double speed;
        private double x;
        private double y;
        private double z;

        PanicGoal(double speed) {
            this.speed = speed;
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (!EntityPanda.this.isBurning()) return false;
            BlockPos water = this.findWater(5, 4);
            if (water != null) {
                this.x = water.getX();
                this.y = water.getY();
                this.z = water.getZ();
                return true;
            }
            Vec3d target = RandomPositionGenerator.findRandomTarget(EntityPanda.this, 5, 4);
            if (target == null) return false;
            this.x = target.x;
            this.y = target.y;
            this.z = target.z;
            return true;
        }

        @Nullable
        private BlockPos findWater(int horizontal, int vertical) {
            BlockPos origin = new BlockPos(EntityPanda.this);
            BlockPos best = null;
            double nearest = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.getAllInBoxMutable(origin.add(-horizontal, -1, -horizontal), origin.add(horizontal, vertical, horizontal))) {
                if (EntityPanda.this.world.getBlockState(pos).getMaterial() != net.minecraft.block.material.Material.WATER) continue;
                double distance = pos.distanceSq(origin);
                if (distance < nearest) {
                    nearest = distance;
                    best = pos.toImmutable();
                }
            }
            return best;
        }

        @Override
        public void startExecuting() {
            EntityPanda.this.getNavigator().tryMoveToXYZ(this.x, this.y, this.z, this.speed);
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (EntityPanda.this.isSitting()) {
                EntityPanda.this.getNavigator().clearPath();
                return false;
            }
            return !EntityPanda.this.getNavigator().noPath();
        }
    }

    /** A panda attacked fights back; only aggressive ones join in for others, and the rest give up after one bite. */
    private class RevengeGoal extends EntityAIHurtByTarget {
        RevengeGoal() {
            super(EntityPanda.this, true);
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (!EntityPanda.this.gotBamboo && !EntityPanda.this.didBite) return super.shouldContinueExecuting();
            EntityPanda.this.setAttackTarget(null);
            return false;
        }

        @Override
        protected void setEntityAttackTarget(EntityCreature creature, EntityLivingBase target) {
            if (creature instanceof EntityPanda && ((EntityPanda) creature).isAggressive()) creature.setAttackTarget(target);
        }
    }

    /** Lazy pandas roll onto their backs now and then. */
    private class LieBackGoal extends EntityAIBase {
        private int cooldownUntil;

        @Override
        public boolean shouldExecute() {
            return this.cooldownUntil < EntityPanda.this.ticksExisted && EntityPanda.this.isLazy() && EntityPanda.this.canPerformAction()
                    && EntityPanda.this.rand.nextInt(400) == 1;
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (!EntityPanda.this.isInWater() && (EntityPanda.this.isLazy() || EntityPanda.this.rand.nextInt(600) != 1)) {
                return EntityPanda.this.rand.nextInt(2000) != 1;
            }
            return false;
        }

        @Override
        public void startExecuting() {
            EntityPanda.this.setOnBack(true);
            this.cooldownUntil = 0;
        }

        @Override
        public void resetTask() {
            EntityPanda.this.setOnBack(false);
            this.cooldownUntil = EntityPanda.this.ticksExisted + 200;
        }
    }

    /** Bamboo or cake nearby, or in its paw: go and sit down to eat. Afterwards a rest of 10 seconds to a few minutes. */
    private class SitGoal extends EntityAIBase {
        private int cooldownUntil;

        SitGoal() {
            this.setMutexBits(1);
        }

        private List<EntityItem> food(double range) {
            return EntityPanda.this.world.getEntitiesWithinAABB(EntityItem.class, EntityPanda.this.getEntityBoundingBox().grow(range, range, range), PANDA_ITEMS::test);
        }

        @Override
        public boolean shouldExecute() {
            if (this.cooldownUntil > EntityPanda.this.ticksExisted || EntityPanda.this.isChild() || EntityPanda.this.isInWater()
                    || !EntityPanda.this.canPerformAction() || EntityPanda.this.getUnhappyCounter() > 0) {
                return false;
            }
            return !this.food(6.0D).isEmpty() || !EntityPanda.this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty();
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (!EntityPanda.this.isInWater() && (EntityPanda.this.isLazy() || EntityPanda.this.rand.nextInt(600) != 1)) {
                return EntityPanda.this.rand.nextInt(2000) != 1;
            }
            return false;
        }

        @Override
        public void updateTask() {
            if (!EntityPanda.this.isSitting() && !EntityPanda.this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty()) EntityPanda.this.tryToSit();
        }

        @Override
        public void startExecuting() {
            List<EntityItem> food = this.food(8.0D);
            if (!food.isEmpty() && EntityPanda.this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty()) {
                EntityPanda.this.getNavigator().tryMoveToEntityLiving(food.get(0), 1.2000000476837158D);
            } else if (!EntityPanda.this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty()) {
                EntityPanda.this.tryToSit();
            }
            this.cooldownUntil = 0;
        }

        @Override
        public void resetTask() {
            ItemStack held = EntityPanda.this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
            if (!held.isEmpty()) {
                EntityPanda.this.entityDropItem(held, 0.0F);
                EntityPanda.this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
                int seconds = EntityPanda.this.isLazy() ? EntityPanda.this.rand.nextInt(50) + 10 : EntityPanda.this.rand.nextInt(150) + 10;
                this.cooldownUntil = EntityPanda.this.ticksExisted + seconds * 20;
            }
            EntityPanda.this.setSitting(false);
        }
    }

    /** Worried pandas keep away from players and monsters. */
    private class AvoidGoal<T extends EntityLivingBase> extends EntityAIAvoidEntity<T> {
        AvoidGoal(Class<T> type, float distance, double farSpeed, double nearSpeed) {
            super(EntityPanda.this, type, entity -> !(entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator()), distance, farSpeed, nearSpeed);
        }

        @Override
        public boolean shouldExecute() {
            return EntityPanda.this.isWorried() && EntityPanda.this.canPerformAction() && super.shouldExecute();
        }
    }

    /** Pandas breed only with bamboo growing within 8 blocks; otherwise they sulk at the nearest player. */
    private class MateGoal extends EntityAIBreed116 {
        private int nextSulk;

        MateGoal(double speed) {
            super(EntityPanda.this, speed);
        }

        @Override
        public boolean shouldExecute() {
            if (!super.shouldExecute() || EntityPanda.this.getUnhappyCounter() != 0) return false;
            if (this.bambooNearby()) return true;
            if (this.nextSulk <= EntityPanda.this.ticksExisted) {
                EntityPanda.this.setUnhappyCounter(32);
                this.nextSulk = EntityPanda.this.ticksExisted + 600;
                if (EntityPanda.this.isServerWorld()) {
                    EntityPlayer player = this.world.getClosestPlayerToEntity(EntityPanda.this, 8.0D);
                    EntityPanda.this.watchGoal.setTarget(player);
                }
            }
            return false;
        }

        private boolean bambooNearby() {
            if (PortiumBlocks.BAMBOO == null) return false;
            BlockPos origin = new BlockPos(EntityPanda.this);
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int y = 0; y < 3; ++y) {
                for (int ring = 0; ring < 8; ++ring) {
                    for (int x = 0; x <= ring; x = x > 0 ? -x : 1 - x) {
                        for (int z = x < ring && x > -ring ? ring : 0; z <= ring; z = z > 0 ? -z : 1 - z) {
                            at.setPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                            if (this.world.getBlockState(at).getBlock() == PortiumBlocks.BAMBOO) return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    /** Cubs sneeze now and then (weak ones often). */
    private class ChildPlayGoal extends EntityAIBase {
        @Override
        public boolean shouldExecute() {
            if (!EntityPanda.this.isChild() || !EntityPanda.this.canPerformAction()) return false;
            if (EntityPanda.this.isWeak() && EntityPanda.this.rand.nextInt(500) == 1) return true;
            return EntityPanda.this.rand.nextInt(6000) == 1;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return false;
        }

        @Override
        public void startExecuting() {
            EntityPanda.this.setSneezing(true);
        }
    }

    /** Cubs and playful pandas somersault, at ledges especially. Not interrupted. */
    private class RollGoal extends EntityAIBase {
        RollGoal() {
            this.setMutexBits(7);
        }

        @Override
        public boolean shouldExecute() {
            if (!(EntityPanda.this.isChild() || EntityPanda.this.isPlayful()) || !EntityPanda.this.onGround) return false;
            if (!EntityPanda.this.canPerformAction()) return false;
            float yaw = EntityPanda.this.rotationYaw * 0.017453292F;
            int dx = 0;
            int dz = 0;
            float x = -MathHelper.sin(yaw);
            float z = MathHelper.cos(yaw);
            if ((double) Math.abs(x) > 0.5D) dx = (int) ((float) dx + x / Math.abs(x));
            if ((double) Math.abs(z) > 0.5D) dz = (int) ((float) dz + z / Math.abs(z));
            IBlockState ahead = EntityPanda.this.world.getBlockState(new BlockPos(EntityPanda.this).add(dx, -1, dz));
            if (ahead.getBlock().isAir(ahead, EntityPanda.this.world, new BlockPos(EntityPanda.this).add(dx, -1, dz))) return true;
            if (EntityPanda.this.isPlayful() && EntityPanda.this.rand.nextInt(60) == 1) return true;
            return EntityPanda.this.rand.nextInt(500) == 1;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return false;
        }

        @Override
        public void startExecuting() {
            EntityPanda.this.setRolling(true);
        }

        @Override
        public boolean isInterruptible() {
            return false;
        }
    }

    /** Watching a player, kept on the one it sulked at. */
    private class WatchGoal extends EntityAIWatchClosest {
        WatchGoal(Class<? extends Entity> type, float distance) {
            super(EntityPanda.this, type, distance);
        }

        void setTarget(@Nullable Entity target) {
            this.closestEntity = target;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.closestEntity != null && super.shouldContinueExecuting();
        }

        @Override
        public boolean shouldExecute() {
            // 1.14's LookAtGoal chance, 0.02 as 1.12's.
            if (this.entity.getRNG().nextFloat() >= 0.02F) return false;
            if (this.closestEntity == null) {
                if (this.watchedClass == EntityPlayer.class) {
                    this.closestEntity = this.entity.world.getClosestPlayer(this.entity.posX, this.entity.posY + this.entity.getEyeHeight(), this.entity.posZ,
                            this.maxDistance, player -> player != null && !((EntityPlayer) player).isSpectator());
                } else {
                    this.closestEntity = this.entity.world.findNearestEntityWithinAABB(this.watchedClass,
                            this.entity.getEntityBoundingBox().grow(this.maxDistance, 3.0D, this.maxDistance), this.entity);
                }
            }
            return EntityPanda.this.canPerformAction() && this.closestEntity != null;
        }

        @Override
        public void updateTask() {
            if (this.closestEntity != null) super.updateTask();
        }
    }

    private class AttackGoal extends EntityAIAttackMelee {
        AttackGoal(double speed, boolean useLongMemory) {
            super(EntityPanda.this, speed, useLongMemory);
        }

        @Override
        public boolean shouldExecute() {
            return EntityPanda.this.canPerformAction() && super.shouldExecute();
        }
    }
}
