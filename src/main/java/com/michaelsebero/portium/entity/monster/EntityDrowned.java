package com.michaelsebero.portium.entity.monster;

import com.michaelsebero.portium.entity.EntityTrident;
import com.michaelsebero.portium.entity.ai.PathNavigateWater;
import com.michaelsebero.portium.entity.passive.EntityTurtle;
import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveToBlock;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityAIZombieAttack;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The drowned (1.13): an underwater zombie. Drowned swim after targets in the water, come ashore at night, and some
 * carry tridents they throw, or fishing rods. Zombies that stay under water for 30 seconds turn into drowned.
 */
public class EntityDrowned extends EntityZombie implements IRangedAttackMob {

    private static final DataParameter<Boolean> SWIMMING = EntityDataManager.createKey(EntityDrowned.class, DataSerializers.BOOLEAN);

    private final PathNavigate waterNavigator;
    private final PathNavigate groundNavigator;
    private boolean swimmingUp;
    private float swimAnimation;
    private float prevSwimAnimation;

    public EntityDrowned(World world) {
        super(world);
        this.stepHeight = 1.0F;
        this.moveHelper = new MoveHelper(this);
        this.setPathPriority(PathNodeType.WATER, 0.0F);
        this.waterNavigator = new PathNavigateWater(this, world);
        this.groundNavigator = new PathNavigateGround(this, world);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(SWIMMING, false);
    }

    /** Zombie goals that suit the water: no floating to the surface, no wandering that avoids water. */
    @Override
    protected void initEntityAI() {
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));
        this.applyEntityAI();
    }

    @Override
    protected void applyEntityAI() {
        this.tasks.addTask(1, new AIGoToWater(this, 1.0D));
        this.tasks.addTask(2, new AITridentAttack(this, 1.0D, 40, 10.0F));
        this.tasks.addTask(2, new AIAttack(this, 1.0D, false));
        this.tasks.addTask(5, new AIGoToBeach(this, 1.0D));
        this.tasks.addTask(6, new AISwimUp(this, 1.0D, this.world.getSeaLevel()));
        this.tasks.addTask(7, new EntityAIWander(this, 1.0D));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, EntityDrowned.class, EntityPigZombie.class));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, 10, true, false, this::shouldAttack));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<>(this, EntityVillager.class, false));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<>(this, EntityIronGolem.class, true));
        this.targetTasks.addTask(5, new EntityAINearestAttackableTarget<>(this, EntityTurtle.class, 10, true, false, EntityTurtle.TARGET_DRY_BABY));
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        data = super.onInitialSpawn(difficulty, data);
        if (this.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND).isEmpty() && this.rand.nextFloat() < 0.03F && PortiumItems.NAUTILUS_SHELL != null) {
            this.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(PortiumItems.NAUTILUS_SHELL));
            this.setDropChance(EntityEquipmentSlot.OFFHAND, 2.0F);
        }
        return data;
    }

    /** Drowned spawn rarely in oceans (at least five blocks down) and more often in rivers. */
    @Override
    public boolean getCanSpawnHere() {
        Biome biome = this.world.getBiome(new BlockPos(this.posX, this.getEntityBoundingBox().minY, this.posZ));
        if (biome == Biomes.RIVER || biome == Biomes.FROZEN_RIVER) return this.rand.nextInt(15) == 0 && super.getCanSpawnHere();
        return this.rand.nextInt(40) == 0 && this.getEntityBoundingBox().minY < this.world.getSeaLevel() - 5 && super.getCanSpawnHere();
    }

    @Override
    public boolean isNotColliding() {
        return this.world.checkNoEntityCollision(this.getEntityBoundingBox(), this);
    }

    @Override
    protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
        if (this.rand.nextFloat() > 0.9F) {
            boolean trident = this.rand.nextInt(16) < 10 && PortiumItems.TRIDENT != null;
            this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(trident ? PortiumItems.TRIDENT : Items.FISHING_ROD));
        }
    }

    /** Prefers a trident over anything else, and a less damaged trident over its own. */
    @Override
    protected void updateEquipmentIfNeeded(EntityItem itemEntity) {
        ItemStack candidate = itemEntity.getItem();
        ItemStack held = this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        boolean heldTrident = held.getItem() == PortiumItems.TRIDENT;
        if (candidate.getItem() != PortiumItems.TRIDENT || heldTrident && candidate.getItemDamage() >= held.getItemDamage()) {
            if (!heldTrident) super.updateEquipmentIfNeeded(itemEntity);
            return;
        }
        if (!held.isEmpty() && this.rand.nextFloat() - 0.1F < this.inventoryHandsDropChances[EntityEquipmentSlot.MAINHAND.getIndex()]) {
            this.entityDropItem(held, 0.0F);
        }
        this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, candidate);
        this.setDropChance(EntityEquipmentSlot.MAINHAND, 2.0F);
        this.enablePersistence();
        this.onItemPickup(itemEntity, candidate.getCount());
        itemEntity.setDead();
    }

    /** Drowned never break doors (1.13), and vanilla's door-breaking setup assumes a walking-only navigator. */
    @Override
    public void setBreakDoorsAItask(boolean enabled) {}

    @Override
    public boolean isPushedByWater() {
        return !this.isSwimming();
    }

    public boolean shouldAttack(@Nullable EntityLivingBase target) {
        return target != null && (!this.world.isDaytime() || target.isInWater());
    }

    // ---- Swimming ----

    public boolean isSwimming() {
        return this.dataManager.get(SWIMMING);
    }

    void setSwimmingUp(boolean swimmingUp) {
        this.swimmingUp = swimmingUp;
    }

    /** Whether the drowned should swim rather than walk: it is heading up, or chasing something in the water. */
    boolean wantsToSwim() {
        if (this.swimmingUp) return true;
        EntityLivingBase target = this.getAttackTarget();
        return target != null && target.isInWater();
    }

    boolean closeToPathTarget() {
        net.minecraft.pathfinding.Path path = this.getNavigator().getPath();
        if (path == null) return false;
        PathPoint end = path.getFinalPathPoint();
        return end != null && this.getDistanceSq(end.x, end.y, end.z) < 4.0D;
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (this.isServerWorld() && this.isInWater() && this.wantsToSwim()) {
            this.moveRelative(strafe, vertical, forward, 0.01F);
            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.9D;
            this.motionY *= 0.9D;
            this.motionZ *= 0.9D;
        } else {
            super.travel(strafe, vertical, forward);
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!this.world.isRemote) {
            boolean swim = this.isServerWorld() && this.isInWater() && this.wantsToSwim();
            this.navigator = swim ? this.waterNavigator : this.groundNavigator;
            this.dataManager.set(SWIMMING, swim);
        }
        this.prevSwimAnimation = this.swimAnimation;
        this.swimAnimation = this.isSwimming() ? Math.min(1.0F, this.swimAnimation + 0.09F) : Math.max(0.0F, this.swimAnimation - 0.09F);
    }

    @SideOnly(Side.CLIENT)
    public float getSwimAnimation(float partialTicks) {
        return this.prevSwimAnimation + (this.swimAnimation - this.prevSwimAnimation) * partialTicks;
    }

    // ---- Combat ----

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
        EntityTrident trident = new EntityTrident(this.world, this, new ItemStack(PortiumItems.TRIDENT));
        double dx = target.posX - this.posX;
        double dy = target.getEntityBoundingBox().minY + target.height / 3.0F - trident.posY;
        double dz = target.posZ - this.posZ;
        double horizontal = MathHelper.sqrt(dx * dx + dz * dz);
        trident.shoot(dx, dy + horizontal * 0.2D, dz, 1.6F, 14 - this.world.getDifficulty().getId() * 4);
        this.playSound(PortiumSounds.ENTITY_DROWNED_SHOOT, 1.0F, 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        this.world.spawnEntity(trident);
    }

    @Override
    public void setSwingingArms(boolean swingingArms) {
        this.setArmsRaised(swingingArms);
    }

    // ---- Drops and sounds ----

    @Override
    protected ResourceLocation getLootTable() {
        return PortiumLootTables.DROWNED;
    }

    @Override
    protected ItemStack getSkullDrop() {
        return ItemStack.EMPTY;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? PortiumSounds.ENTITY_DROWNED_AMBIENT_WATER : PortiumSounds.ENTITY_DROWNED_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isInWater() ? PortiumSounds.ENTITY_DROWNED_HURT_WATER : PortiumSounds.ENTITY_DROWNED_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isInWater() ? PortiumSounds.ENTITY_DROWNED_DEATH_WATER : PortiumSounds.ENTITY_DROWNED_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return PortiumSounds.ENTITY_DROWNED_STEP;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return PortiumSounds.ENTITY_DROWNED_SWIM;
    }

    // ---- Movement ----

    /** Swims towards its waypoint in water (drifting up towards a target above), and walks on land. */
    static class MoveHelper extends EntityMoveHelper {

        private final EntityDrowned drowned;

        MoveHelper(EntityDrowned drowned) {
            super(drowned);
            this.drowned = drowned;
        }

        @Override
        public void onUpdateMoveHelper() {
            EntityLivingBase target = this.drowned.getAttackTarget();
            if (!this.drowned.wantsToSwim() || !this.drowned.isInWater()) {
                if (!this.drowned.onGround) this.drowned.motionY -= 0.008D;
                super.onUpdateMoveHelper();
                return;
            }
            if (target != null && target.posY > this.drowned.posY || this.drowned.swimmingUp) this.drowned.motionY += 0.002D;
            if (this.action != Action.MOVE_TO || this.drowned.getNavigator().noPath()) {
                this.drowned.setAIMoveSpeed(0.0F);
                return;
            }
            double dx = this.posX - this.drowned.posX;
            double dy = this.posY - this.drowned.posY;
            double dz = this.posZ - this.drowned.posZ;
            double distance = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 1.0E-7D) return;
            dy /= distance;
            float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            this.drowned.rotationYaw = this.limitAngle(this.drowned.rotationYaw, yaw, 90.0F);
            this.drowned.renderYawOffset = this.drowned.rotationYaw;
            float speed = (float) (this.speed * this.drowned.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
            float moveSpeed = this.drowned.getAIMoveSpeed() + (speed - this.drowned.getAIMoveSpeed()) * 0.125F;
            this.drowned.setAIMoveSpeed(moveSpeed);
            this.drowned.motionX += moveSpeed * dx * 0.005D;
            this.drowned.motionY += moveSpeed * dy * 0.1D;
            this.drowned.motionZ += moveSpeed * dz * 0.005D;
        }
    }

    // ---- Goals ----

    /** By day, heads for nearby water. */
    static class AIGoToWater extends EntityAIBase {

        private final EntityDrowned drowned;
        private final double speed;
        private double x;
        private double y;
        private double z;

        AIGoToWater(EntityDrowned drowned, double speed) {
            this.drowned = drowned;
            this.speed = speed;
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (!this.drowned.world.isDaytime() || this.drowned.isInWater()) return false;
            Vec3d water = this.findWater();
            if (water == null) return false;
            this.x = water.x;
            this.y = water.y;
            this.z = water.z;
            return true;
        }

        @Nullable
        private Vec3d findWater() {
            BlockPos origin = new BlockPos(this.drowned.posX, this.drowned.getEntityBoundingBox().minY, this.drowned.posZ);
            for (int i = 0; i < 10; ++i) {
                BlockPos pos = origin.add(this.drowned.rand.nextInt(20) - 10, 2 - this.drowned.rand.nextInt(8), this.drowned.rand.nextInt(20) - 10);
                if (this.drowned.world.getBlockState(pos).getBlock() == Blocks.WATER) return new Vec3d(pos);
            }
            return null;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.drowned.getNavigator().noPath();
        }

        @Override
        public void startExecuting() {
            this.drowned.getNavigator().tryMoveToXYZ(this.x, this.y, this.z, this.speed);
        }
    }

    /** Throws its trident at targets in range. */
    static class AITridentAttack extends EntityAIAttackRanged {

        private final EntityDrowned drowned;

        AITridentAttack(EntityDrowned drowned, double speed, int interval, float maxDistance) {
            super(drowned, speed, interval, maxDistance);
            this.drowned = drowned;
        }

        @Override
        public boolean shouldExecute() {
            return super.shouldExecute() && this.drowned.getHeldItemMainhand().getItem() == PortiumItems.TRIDENT;
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            this.drowned.setArmsRaised(true);
        }

        @Override
        public void resetTask() {
            super.resetTask();
            this.drowned.setArmsRaised(false);
        }
    }

    /** Melee, but only at night or against targets in the water. */
    static class AIAttack extends EntityAIZombieAttack {

        private final EntityDrowned drowned;

        AIAttack(EntityDrowned drowned, double speed, boolean longMemory) {
            super(drowned, speed, longMemory);
            this.drowned = drowned;
        }

        @Override
        public boolean shouldExecute() {
            return super.shouldExecute() && this.drowned.shouldAttack(this.drowned.getAttackTarget());
        }

        @Override
        public boolean shouldContinueExecuting() {
            return super.shouldContinueExecuting() && this.drowned.shouldAttack(this.drowned.getAttackTarget());
        }
    }

    /** At night, climbs out onto the shore from near the surface. */
    static class AIGoToBeach extends EntityAIMoveToBlock {

        private final EntityDrowned drowned;

        AIGoToBeach(EntityDrowned drowned, double speed) {
            super(drowned, speed, 8);
            this.drowned = drowned;
        }

        @Override
        public boolean shouldExecute() {
            return super.shouldExecute() && !this.drowned.world.isDaytime() && this.drowned.isInWater() && this.drowned.posY >= this.drowned.world.getSeaLevel() - 3;
        }

        @Override
        protected boolean shouldMoveTo(World world, BlockPos pos) {
            BlockPos above = pos.up();
            return world.isAirBlock(above) && world.isAirBlock(above.up()) && world.getBlockState(pos).isTopSolid();
        }

        @Override
        public void startExecuting() {
            this.drowned.setSwimmingUp(false);
            this.drowned.navigator = this.drowned.groundNavigator;
            super.startExecuting();
        }
    }

    /** At night, swims up towards the surface. */
    static class AISwimUp extends EntityAIBase {

        private final EntityDrowned drowned;
        private final double speed;
        private final int targetY;
        private boolean obstructed;

        AISwimUp(EntityDrowned drowned, double speed, int targetY) {
            this.drowned = drowned;
            this.speed = speed;
            this.targetY = targetY;
        }

        @Override
        public boolean shouldExecute() {
            return !this.drowned.world.isDaytime() && this.drowned.isInWater() && this.drowned.posY < this.targetY - 2;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.shouldExecute() && !this.obstructed;
        }

        @Override
        public void updateTask() {
            if (this.drowned.posY >= this.targetY - 1 || !this.drowned.getNavigator().noPath() && !this.drowned.closeToPathTarget()) return;
            Vec3d target = RandomPositionGenerator.findRandomTargetBlockTowards(this.drowned, 4, 8, new Vec3d(this.drowned.posX, this.targetY - 1, this.drowned.posZ));
            if (target == null) {
                this.obstructed = true;
                return;
            }
            this.drowned.getNavigator().tryMoveToXYZ(target.x, target.y, target.z, this.speed);
        }

        @Override
        public void startExecuting() {
            this.drowned.setSwimmingUp(true);
            this.obstructed = false;
        }

        @Override
        public void resetTask() {
            this.drowned.setSwimmingUp(false);
        }
    }
}
