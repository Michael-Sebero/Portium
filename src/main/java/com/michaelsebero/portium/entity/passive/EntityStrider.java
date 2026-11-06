package com.michaelsebero.portium.entity.passive;

import com.google.common.collect.Sets;
import com.michaelsebero.portium.entity.ILavaWalker;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAIMoveToBlock;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * 1.16's strider: walks on lava, shivers (slow, purple) out of it, hurt by water; tempted and bred with warped fungus;
 * saddled and steered with a warped fungus on a stick. Some spawn ridden by a zombified piglin (1.12's zombie pigman)
 * or carrying a baby.
 */
public class EntityStrider extends EntityAnimal implements ILavaWalker {

    private static final DataParameter<Integer> BOOST_TIME = EntityDataManager.createKey(EntityStrider.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> COLD = EntityDataManager.createKey(EntityStrider.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SADDLED = EntityDataManager.createKey(EntityStrider.class, DataSerializers.BOOLEAN);

    private boolean boosting;
    private int boostTime;
    private int totalBoostTime;
    private EntityAITempt temptGoal;
    private EntityAIPanic panicGoal;

    public EntityStrider(World world) {
        super(world);
        this.setSize(0.9F, 1.7F);
        this.preventEntitySpawning = true;
        this.isImmuneToFire = true;
        this.setPathPriority(PathNodeType.WATER, -1.0F);
        this.setPathPriority(PathNodeType.LAVA, 0.0F);
        this.setPathPriority(PathNodeType.DANGER_FIRE, 0.0F);
        this.setPathPriority(PathNodeType.DAMAGE_FIRE, 0.0F);
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        return new PathNavigateGround(this, world) {
            /** Lava isn't safe ground to 1.12's straight-line check, so always follow path nodes. */
            @Override
            protected boolean isDirectPathBetweenPoints(Vec3d from, Vec3d to, int sizeX, int sizeY, int sizeZ) {
                return false;
            }

            @Override
            protected boolean canNavigate() {
                return true;
            }

            /** A target in the air over lava is the lava's surface: 1.12 paths over lava at the lava block's height. */
            @Override
            public net.minecraft.pathfinding.Path getPathToPos(BlockPos pos) {
                BlockPos target = pos;
                while (target.getY() > 0 && this.world.isAirBlock(target) && !this.world.isAirBlock(target.down())
                        && this.world.getBlockState(target.down()).getMaterial() == Material.LAVA) {
                    target = target.down();
                    break;
                }
                return super.getPathToPos(target);
            }
        };
    }

    @Override
    protected void initEntityAI() {
        this.panicGoal = new EntityAIPanic(this, 1.65D);
        this.tasks.addTask(1, this.panicGoal);
        this.tasks.addTask(2, new EntityAIMate(this, 1.0D));
        this.temptGoal = new TemptGoal(this);
        this.tasks.addTask(3, this.temptGoal);
        this.tasks.addTask(4, new MoveToLavaGoal(this));
        this.tasks.addTask(5, new EntityAIFollowParent(this, 1.1D));
        this.tasks.addTask(7, new EntityAIWander(this, 1.0D, 60));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));
        this.tasks.addTask(9, new EntityAIWatchClosest(this, EntityStrider.class, 8.0F));
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(BOOST_TIME, 0);
        this.dataManager.register(COLD, false);
        this.dataManager.register(SADDLED, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.175D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(16.0D);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (BOOST_TIME.equals(key) && this.world.isRemote) {
            this.boosting = true;
            this.boostTime = 0;
            this.totalBoostTime = this.dataManager.get(BOOST_TIME);
        }
        super.notifyDataManagerChange(key);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setBoolean("Saddle", this.isSaddled());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.dataManager.set(SADDLED, compound.getBoolean("Saddle"));
    }

    // ---- Saddle and riding ----

    public boolean isSaddled() {
        return this.dataManager.get(SADDLED);
    }

    public boolean canBeSaddled() {
        return this.isEntityAlive() && !this.isChild();
    }

    public void saddle(boolean playSound) {
        this.dataManager.set(SADDLED, true);
        if (playSound) this.world.playSound(null, this.posX, this.posY, this.posZ, PortiumSounds.ENTITY_STRIDER_SADDLE, SoundCategory.NEUTRAL, 0.5F, 1.0F);
    }

    /** Starts a speed boost; false if one is already going. */
    public boolean boost() {
        if (this.boosting) return false;
        this.boosting = true;
        this.boostTime = 0;
        this.totalBoostTime = this.getRNG().nextInt(841) + 140;
        this.dataManager.set(BOOST_TIME, this.totalBoostTime);
        return true;
    }

    /** Shivering: off a warm block (lava) and out of lava; a strider riding another is as cold as the one below. */
    public boolean isCold() {
        Entity mount = this.getRidingEntity();
        return mount instanceof EntityStrider ? ((EntityStrider) mount).isCold() : this.dataManager.get(COLD);
    }

    @Override
    public boolean canBeSteered() {
        Entity rider = this.getControllingPassenger();
        if (!(rider instanceof EntityPlayer)) return false;
        EntityPlayer player = (EntityPlayer) rider;
        return isFungusStick(player.getHeldItemMainhand()) || isFungusStick(player.getHeldItemOffhand());
    }

    private static boolean isFungusStick(ItemStack stack) {
        return PortiumItems.WARPED_FUNGUS_ON_A_STICK != null && stack.getItem() == PortiumItems.WARPED_FUNGUS_ON_A_STICK;
    }

    @Nullable
    @Override
    public Entity getControllingPassenger() {
        return this.getPassengers().isEmpty() ? null : this.getPassengers().get(0);
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && !this.isInsideOfMaterial(Material.LAVA);
    }

    @Override
    public double getMountedYOffset() {
        float swing = Math.min(0.25F, this.limbSwingAmount);
        return this.height - 0.19D + 0.12F * MathHelper.cos(this.limbSwing * 1.5F) * 2.0F * swing;
    }

    private float walkSpeed() {
        return (float) this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue() * (this.isCold() ? 0.66F : 1.0F);
    }

    private float mountedSpeed() {
        return (float) this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue() * (this.isCold() ? 0.23F : 0.55F);
    }

    /** 1.16's {@code IRideable.ride}: a rider holding the stick steers; the boost swells and fades over its length. */
    @Override
    public void travel(float strafe, float vertical, float forward) {
        this.setAIMoveSpeed(this.walkSpeed());
        Entity rider = this.getControllingPassenger();
        if (this.isEntityAlive() && this.isBeingRidden() && this.canBeSteered() && rider instanceof EntityPlayer) {
            this.rotationYaw = rider.rotationYaw;
            this.prevRotationYaw = this.rotationYaw;
            this.rotationPitch = rider.rotationPitch * 0.5F;
            this.setRotation(this.rotationYaw, this.rotationPitch);
            this.renderYawOffset = this.rotationYaw;
            this.rotationYawHead = this.rotationYaw;
            this.stepHeight = 1.0F;
            this.jumpMovementFactor = this.getAIMoveSpeed() * 0.1F;
            if (this.boosting && this.boostTime++ > this.totalBoostTime) this.boosting = false;
            if (this.canPassengerSteer()) {
                float speed = this.mountedSpeed();
                if (this.boosting) speed += speed * 1.15F * MathHelper.sin((float) this.boostTime / (float) this.totalBoostTime * (float) Math.PI);
                this.setAIMoveSpeed(speed);
                super.travel(0.0F, 0.0F, 1.0F);
                this.newPosRotationIncrements = 0;
            } else {
                this.motionX = 0.0D;
                this.motionY = 0.0D;
                this.motionZ = 0.0D;
            }
            this.prevLimbSwingAmount = this.limbSwingAmount;
            double dx = this.posX - this.prevPosX;
            double dz = this.posZ - this.prevPosZ;
            float moved = Math.min(MathHelper.sqrt(dx * dx + dz * dz) * 4.0F, 1.0F);
            this.limbSwingAmount += (moved - this.limbSwingAmount) * 0.4F;
            this.limbSwing += this.limbSwingAmount;
        } else {
            this.stepHeight = 0.5F;
            this.jumpMovementFactor = 0.02F;
            super.travel(strafe, vertical, forward);
        }
    }

    // ---- Lava ----

    /** 1.16 doesn't swim a strider through lava: it walks (and floats up onto the surface). */
    @Override
    public boolean isInLava() {
        return false;
    }

    public boolean isInLavaBlock() {
        return this.world.isMaterialInBB(this.getEntityBoundingBox().grow(-0.1D, -0.4D, -0.1D), Material.LAVA);
    }

    @Override
    public void onUpdate() {
        if (this.temptGoal != null && isRunning(this.temptGoal) && this.rand.nextInt(140) == 0) {
            this.playSound(PortiumSounds.ENTITY_STRIDER_HAPPY, 1.0F, this.getSoundPitch());
        } else if (this.panicGoal != null && isRunning(this.panicGoal) && this.rand.nextInt(60) == 0) {
            this.playSound(PortiumSounds.ENTITY_STRIDER_RETREAT, 1.0F, this.getSoundPitch());
        }
        IBlockState in = this.world.getBlockState(new BlockPos(this));
        IBlockState below = this.world.getBlockState(new BlockPos(this.posX, this.getEntityBoundingBox().minY - 0.5000001D, this.posZ));
        boolean warm = in.getMaterial() == Material.LAVA || below.getMaterial() == Material.LAVA || this.isInLavaBlock();
        if (!this.world.isRemote) this.dataManager.set(COLD, !warm);
        super.onUpdate();
        this.floatStrider();
    }

    private boolean isRunning(net.minecraft.entity.ai.EntityAIBase goal) {
        for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry entry : this.tasks.taskEntries) {
            if (entry.action == goal) return entry.using;
        }
        return false;
    }

    /** In lava: stands on the surface when above it (and nothing but air above), else rises. */
    private void floatStrider() {
        if (!this.isInLavaBlock()) return;
        BlockPos pos = new BlockPos(this);
        boolean onSurface = this.getEntityBoundingBox().minY > pos.getY() + 0.5D - 1.0E-5D && this.world.getBlockState(pos.up()).getMaterial() != Material.LAVA;
        if (onSurface) {
            this.onGround = true;
        } else {
            this.motionX *= 0.5D;
            this.motionY = this.motionY * 0.5D + 0.05D;
            this.motionZ *= 0.5D;
        }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote && this.isWet()) this.attackEntityFrom(DamageSource.DROWN, 1.0F);
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
        if (!this.isInLavaBlock()) super.fall(distance, damageMultiplier);
    }

    @Override
    protected void updateFallState(double y, boolean onGround, IBlockState state, BlockPos pos) {
        if (this.isInLavaBlock()) {
            this.fallDistance = 0.0F;
        } else {
            super.updateFallState(y, onGround, state, pos);
        }
    }

    @Override
    public boolean isBurning() {
        return false;
    }

    @Override
    public float getBlockPathWeight(BlockPos pos) {
        if (this.world.getBlockState(pos).getMaterial() == Material.LAVA) return 10.0F;
        return this.isInLavaBlock() ? Float.NEGATIVE_INFINITY : 0.0F;
    }

    /** 1.16's spawn rule: in lava, with air above the lava's top. */
    @Override
    public boolean getCanSpawnHere() {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(new BlockPos(this));
        do {
            pos.move(net.minecraft.util.EnumFacing.UP);
        } while (this.world.getBlockState(pos).getMaterial() == Material.LAVA && pos.getY() < this.world.getHeight());
        return this.world.isAirBlock(pos);
    }

    @Override
    public boolean isNotColliding() {
        return this.world.checkNoEntityCollision(this.getEntityBoundingBox(), this);
    }

    // ---- Breeding and interaction ----

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return PortiumBlocks.WARPED_FUNGUS != null && stack.getItem() == Item.getItemFromBlock(PortiumBlocks.WARPED_FUNGUS);
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        return new EntityStrider(this.world);
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        boolean food = this.isBreedingItem(stack);
        if (!food && this.isSaddled() && !this.isBeingRidden() && !player.isSneaking()) {
            if (!this.world.isRemote) player.startRiding(this);
            return true;
        }
        if (super.processInteract(player, hand)) {
            if (food && !this.isSilent()) {
                this.world.playSound(null, this.posX, this.posY, this.posZ, PortiumSounds.ENTITY_STRIDER_EAT, this.getSoundCategory(), 1.0F,
                        1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
            }
            return true;
        }
        if (stack.getItem() == Items.SADDLE && this.canBeSaddled() && !this.isSaddled()) {
            if (!this.world.isRemote) {
                this.saddle(true);
                if (!player.capabilities.isCreativeMode) stack.shrink(1);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier) {
        super.dropEquipment(wasRecentlyHit, lootingModifier);
        if (this.isSaddled()) this.dropItem(Items.SADDLE, 1);
    }

    /** One in thirty spawns with a zombified piglin riding it, holding a warped fungus on a stick; one in ten with a baby on top. */
    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        if (this.isChild()) return super.onInitialSpawn(difficulty, data);
        if (this.rand.nextInt(30) == 0) {
            EntityPigZombie rider = new EntityPigZombie(this.world);
            this.spawnRider(rider, difficulty);
            if (PortiumItems.WARPED_FUNGUS_ON_A_STICK != null) {
                rider.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(PortiumItems.WARPED_FUNGUS_ON_A_STICK));
            }
            this.saddle(false);
        } else if (this.rand.nextInt(10) == 0) {
            EntityStrider baby = new EntityStrider(this.world);
            baby.setGrowingAge(-24000);
            this.spawnRider(baby, difficulty);
        }
        return super.onInitialSpawn(difficulty, data);
    }

    private void spawnRider(EntityLiving rider, DifficultyInstance difficulty) {
        rider.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, 0.0F);
        rider.onInitialSpawn(difficulty, null);
        if (rider.isChild() && rider instanceof EntityPigZombie) ((EntityPigZombie) rider).setChild(false);
        this.world.spawnEntity(rider);
        rider.startRiding(this, true);
    }

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return PortiumLootTables.STRIDER;
    }

    // ---- Sounds ----

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        boolean busy = this.temptGoal != null && isRunning(this.temptGoal) || this.panicGoal != null && isRunning(this.panicGoal);
        return busy ? null : PortiumSounds.ENTITY_STRIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_STRIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_STRIDER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        this.playSound(this.isInLavaBlock() ? PortiumSounds.ENTITY_STRIDER_STEP_LAVA : PortiumSounds.ENTITY_STRIDER_STEP, 1.0F, 1.0F);
    }

    // ---- Goals ----

    private static final class TemptGoal extends EntityAITempt {
        TemptGoal(EntityStrider strider) {
            super(strider, 1.4D, false, temptItems());
        }

        private static Set<Item> temptItems() {
            Set<Item> items = Sets.newHashSet();
            if (PortiumBlocks.WARPED_FUNGUS != null) items.add(Item.getItemFromBlock(PortiumBlocks.WARPED_FUNGUS));
            if (PortiumItems.WARPED_FUNGUS_ON_A_STICK != null) items.add(PortiumItems.WARPED_FUNGUS_ON_A_STICK);
            return items;
        }
    }

    /** Out of lava: heads for lava with room above it within 8 blocks. */
    private static final class MoveToLavaGoal extends EntityAIMoveToBlock {
        private final EntityStrider strider;

        MoveToLavaGoal(EntityStrider strider) {
            super(strider, 1.5D, 8);
            this.strider = strider;
        }

        @Override
        public boolean shouldExecute() {
            return !this.strider.isInLavaBlock() && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.strider.isInLavaBlock() && super.shouldContinueExecuting();
        }

        @Override
        protected boolean shouldMoveTo(World world, BlockPos pos) {
            return world.getBlockState(pos).getMaterial() == Material.LAVA && !world.getBlockState(pos.up()).getMaterial().blocksMovement()
                    && world.getBlockState(pos.up()).getMaterial() != Material.LAVA;
        }
    }
}
