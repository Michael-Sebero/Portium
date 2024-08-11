package com.michaelsebero.portium.entity.passive;

import com.michaelsebero.portium.block.BlockTurtleEgg;
import com.michaelsebero.portium.entity.ai.PathNavigateAmphibious;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import com.google.common.base.Predicate;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIMoveToBlock;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The sea turtle (1.13). Turtles remember the beach they hatched on. After breeding, the turtle carrying eggs swims
 * home, digs into the sand and lays 1-4 eggs. Babies drop a scute when they grow up. Turtles roam far out to sea and
 * can be lured with seagrass.
 */
public class EntityTurtle extends EntityAnimal {

    private static final DataParameter<BlockPos> HOME_POS = EntityDataManager.createKey(EntityTurtle.class, DataSerializers.BLOCK_POS);
    private static final DataParameter<Boolean> HAS_EGG = EntityDataManager.createKey(EntityTurtle.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> IS_DIGGING = EntityDataManager.createKey(EntityTurtle.class, DataSerializers.BOOLEAN);
    private static final DataParameter<BlockPos> TRAVEL_POS = EntityDataManager.createKey(EntityTurtle.class, DataSerializers.BLOCK_POS);
    private static final DataParameter<Boolean> GOING_HOME = EntityDataManager.createKey(EntityTurtle.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> TRAVELLING = EntityDataManager.createKey(EntityTurtle.class, DataSerializers.BOOLEAN);

    /** Baby turtles out of the water, which zombies, skeletons and wild wolves and ocelots hunt. */
    public static final Predicate<Entity> TARGET_DRY_BABY = entity -> entity instanceof EntityTurtle && ((EntityTurtle) entity).isChild() && !entity.isInWater();

    private int digTicks;

    public EntityTurtle(World world) {
        super(world);
        this.setSize(1.2F, 0.4F);
        this.moveHelper = new MoveHelper(this);
        this.spawnableBlock = Blocks.SAND;
        this.stepHeight = 1.0F;
        this.setPathPriority(PathNodeType.WATER, 0.0F);
    }

    // ---- State ----

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(HOME_POS, BlockPos.ORIGIN);
        this.dataManager.register(HAS_EGG, false);
        this.dataManager.register(TRAVEL_POS, BlockPos.ORIGIN);
        this.dataManager.register(GOING_HOME, false);
        this.dataManager.register(TRAVELLING, false);
        this.dataManager.register(IS_DIGGING, false);
    }

    public void setHome(BlockPos pos) {
        this.dataManager.set(HOME_POS, pos);
    }

    public BlockPos getHome() {
        return this.dataManager.get(HOME_POS);
    }

    private void setTravelPos(BlockPos pos) {
        this.dataManager.set(TRAVEL_POS, pos);
    }

    private BlockPos getTravelPos() {
        return this.dataManager.get(TRAVEL_POS);
    }

    public boolean hasEgg() {
        return this.dataManager.get(HAS_EGG);
    }

    private void setHasEgg(boolean hasEgg) {
        this.dataManager.set(HAS_EGG, hasEgg);
    }

    public boolean isDigging() {
        return this.dataManager.get(IS_DIGGING);
    }

    private void setDigging(boolean digging) {
        this.digTicks = digging ? 1 : 0;
        this.dataManager.set(IS_DIGGING, digging);
    }

    private boolean isGoingHome() {
        return this.dataManager.get(GOING_HOME);
    }

    private void setGoingHome(boolean goingHome) {
        this.dataManager.set(GOING_HOME, goingHome);
    }

    public boolean isTravelling() {
        return this.dataManager.get(TRAVELLING);
    }

    private void setTravelling(boolean travelling) {
        this.dataManager.set(TRAVELLING, travelling);
    }

    private boolean isNear(BlockPos pos, double distance) {
        return pos.distanceSq(this.posX, this.posY, this.posZ) < distance * distance;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        BlockPos home = this.getHome();
        compound.setInteger("HomePosX", home.getX());
        compound.setInteger("HomePosY", home.getY());
        compound.setInteger("HomePosZ", home.getZ());
        compound.setBoolean("HasEgg", this.hasEgg());
        BlockPos travel = this.getTravelPos();
        compound.setInteger("TravelPosX", travel.getX());
        compound.setInteger("TravelPosY", travel.getY());
        compound.setInteger("TravelPosZ", travel.getZ());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        this.setHome(new BlockPos(compound.getInteger("HomePosX"), compound.getInteger("HomePosY"), compound.getInteger("HomePosZ")));
        super.readEntityFromNBT(compound);
        this.setHasEgg(compound.getBoolean("HasEgg"));
        this.setTravelPos(new BlockPos(compound.getInteger("TravelPosX"), compound.getInteger("TravelPosY"), compound.getInteger("TravelPosZ")));
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        this.setHome(new BlockPos(this.posX, this.posY, this.posZ));
        this.setTravelPos(BlockPos.ORIGIN);
        return super.onInitialSpawn(difficulty, data);
    }

    /** Turtles spawn on beach sand no more than a few blocks above sea level. */
    @Override
    public boolean getCanSpawnHere() {
        return this.posY < this.world.getSeaLevel() + 4 && super.getCanSpawnHere();
    }

    // ---- AI and movement ----

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new AIPanic(this, 1.2D));
        this.tasks.addTask(1, new AIMate(this, 1.0D));
        this.tasks.addTask(1, new AILayEgg(this, 1.0D));
        // Seagrass tempts turtles; without it nothing does.
        if (PortiumBlocks.SEAGRASS != null) this.tasks.addTask(2, new AITempt(this, 1.1D, Item.getItemFromBlock(PortiumBlocks.SEAGRASS)));
        this.tasks.addTask(3, new AIGoToWater(this, 1.0D));
        this.tasks.addTask(4, new AIGoHome(this, 1.0D));
        this.tasks.addTask(7, new AITravel(this, 1.0D));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(9, new AIWander(this, 1.0D, 100));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        return new PathNavigateAmphibious(this, world);
    }

    @Override
    public float getBlockPathWeight(BlockPos pos) {
        if (!this.isGoingHome() && Waterlogging.hasWater(this.world, pos)) return 10.0F;
        return this.world.getBlockState(pos.down()).getBlock() == Blocks.SAND ? 10.0F : this.world.getLightBrightness(pos) - 0.5F;
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (this.isServerWorld() && this.isInWater()) {
            this.moveRelative(strafe, vertical, forward, 0.1F);
            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.9D;
            this.motionY *= 0.9D;
            this.motionZ *= 0.9D;
            if (this.getAttackTarget() == null && (!this.isGoingHome() || !this.isNear(this.getHome(), 20.0D))) this.motionY -= 0.005D;
        } else {
            super.travel(strafe, vertical, forward);
        }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.isEntityAlive() && this.isDigging() && this.digTicks >= 1 && this.digTicks % 5 == 0) {
            BlockPos below = new BlockPos(this).down();
            if (this.world.getBlockState(below).getBlock() == Blocks.SAND) {
                this.world.playEvent(2001, below, Block.getStateId(Blocks.SAND.getDefaultState()));
            }
        }
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return false;
    }

    @Override
    public int getTalkInterval() {
        return 200;
    }

    // ---- Growing up and breeding ----

    @Override
    public void setScaleForAge(boolean child) {
        this.setScale(child ? 0.3F : 1.0F);
    }

    @Override
    protected void onGrowingAdult() {
        super.onGrowingAdult();
        if (!this.isChild() && this.world.getGameRules().getBoolean("doMobLoot") && PortiumItems.SCUTE != null) {
            this.entityDropItem(new ItemStack(PortiumItems.SCUTE), 0.0F);
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return PortiumBlocks.SEAGRASS != null && !stack.isEmpty() && stack.getItem() == Item.getItemFromBlock(PortiumBlocks.SEAGRASS);
    }

    @Override
    public boolean canMateWith(EntityAnimal other) {
        return super.canMateWith(other) && !this.hasEgg() && !((EntityTurtle) other).hasEgg();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return new EntityTurtle(this.world);
    }

    // ---- Death ----

    @Override
    protected ResourceLocation getLootTable() {
        return PortiumLootTables.TURTLE;
    }

    @Override
    public void onStruckByLightning(EntityLightningBolt lightning) {
        this.attackEntityFrom(DamageSource.LIGHTNING_BOLT, Float.MAX_VALUE);
    }

    /** A turtle killed by lightning leaves its shell behind as a bowl. */
    @Override
    protected void dropLoot(boolean wasRecentlyHit, int lootingModifier, DamageSource source) {
        super.dropLoot(wasRecentlyHit, lootingModifier, source);
        if (source == DamageSource.LIGHTNING_BOLT) this.dropItem(Items.BOWL, 1);
    }

    // ---- Sounds ----

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return !this.isInWater() && this.onGround && !this.isChild() ? PortiumSounds.ENTITY_TURTLE_AMBIENT_LAND : super.getAmbientSound();
    }

    /** Turtles swim noisily. */
    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound, sound == PortiumSounds.ENTITY_TURTLE_SWIM ? volume * 1.5F : volume, pitch);
    }

    @Override
    protected SoundEvent getSwimSound() {
        return PortiumSounds.ENTITY_TURTLE_SWIM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isChild() ? PortiumSounds.ENTITY_TURTLE_HURT_BABY : PortiumSounds.ENTITY_TURTLE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isChild() ? PortiumSounds.ENTITY_TURTLE_DEATH_BABY : PortiumSounds.ENTITY_TURTLE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        this.playSound(this.isChild() ? PortiumSounds.ENTITY_TURTLE_SHAMBLE_BABY : PortiumSounds.ENTITY_TURTLE_SHAMBLE, 0.15F, 1.0F);
    }

    // ---- Movement controller ----

    /** Slow on land, faster in open water, slower again far from home and for babies. */
    static class MoveHelper extends EntityMoveHelper {

        private final EntityTurtle turtle;

        MoveHelper(EntityTurtle turtle) {
            super(turtle);
            this.turtle = turtle;
        }

        private void updateSpeed() {
            if (this.turtle.isInWater()) {
                this.turtle.motionY += 0.005D;
                if (!this.turtle.isNear(this.turtle.getHome(), 16.0D)) this.turtle.setAIMoveSpeed(Math.max(this.turtle.getAIMoveSpeed() / 2.0F, 0.08F));
                if (this.turtle.isChild()) this.turtle.setAIMoveSpeed(Math.max(this.turtle.getAIMoveSpeed() / 3.0F, 0.06F));
            } else if (this.turtle.onGround) {
                this.turtle.setAIMoveSpeed(Math.max(this.turtle.getAIMoveSpeed() / 2.0F, 0.06F));
            }
        }

        @Override
        public void onUpdateMoveHelper() {
            this.updateSpeed();
            if (this.action != Action.MOVE_TO || this.turtle.getNavigator().noPath()) {
                this.turtle.setAIMoveSpeed(0.0F);
                return;
            }
            double dx = this.posX - this.turtle.posX;
            double dy = this.posY - this.turtle.posY;
            double dz = this.posZ - this.turtle.posZ;
            double distance = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 1.0E-7D) return;
            float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            this.turtle.rotationYaw = this.limitAngle(this.turtle.rotationYaw, yaw, 90.0F);
            this.turtle.renderYawOffset = this.turtle.rotationYaw;
            float speed = (float) (this.speed * this.turtle.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
            this.turtle.setAIMoveSpeed(this.turtle.getAIMoveSpeed() + (speed - this.turtle.getAIMoveSpeed()) * 0.125F);
            this.turtle.motionY += this.turtle.getAIMoveSpeed() * (dy / distance) * 0.1D;
        }
    }

    // ---- Goals ----

    /** Flees towards the nearest water when hurt or burning. */
    static class AIPanic extends EntityAIPanic {

        AIPanic(EntityTurtle turtle, double speed) {
            super(turtle, speed);
        }

        @Override
        public boolean shouldExecute() {
            if (this.creature.getRevengeTarget() == null && !this.creature.isBurning()) return false;
            BlockPos water = this.nearestWater(7, 4);
            if (water == null) return this.findRandomPosition();
            this.randPosX = water.getX();
            this.randPosY = water.getY();
            this.randPosZ = water.getZ();
            return true;
        }

        @Nullable
        private BlockPos nearestWater(int horizontal, int vertical) {
            BlockPos origin = new BlockPos(this.creature);
            BlockPos best = null;
            float bestDistance = horizontal * horizontal * vertical * 2;
            for (BlockPos.MutableBlockPos pos : BlockPos.getAllInBoxMutable(origin.add(-horizontal, -vertical, -horizontal), origin.add(horizontal, vertical, horizontal))) {
                if (!Waterlogging.hasWater(this.creature.world, pos)) continue;
                float distance = (float) pos.distanceSq(origin);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = pos.toImmutable();
                }
            }
            return best;
        }
    }

    /** Breeding gives one of the pair eggs to carry home instead of a baby. */
    static class AIMate extends EntityAIBase {

        private final EntityTurtle turtle;
        private final double speed;
        @Nullable
        private EntityTurtle mate;
        private int breedTicks;

        AIMate(EntityTurtle turtle, double speed) {
            this.turtle = turtle;
            this.speed = speed;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            if (!this.turtle.isInLove() || this.turtle.hasEgg()) return false;
            this.mate = this.findMate();
            return this.mate != null;
        }

        @Nullable
        private EntityTurtle findMate() {
            List<EntityTurtle> nearby = this.turtle.world.getEntitiesWithinAABB(EntityTurtle.class, this.turtle.getEntityBoundingBox().grow(8.0D));
            EntityTurtle best = null;
            double bestDistance = Double.MAX_VALUE;
            for (EntityTurtle other : nearby) {
                double distance = this.turtle.getDistanceSq(other);
                if (this.turtle.canMateWith(other) && distance < bestDistance) {
                    best = other;
                    bestDistance = distance;
                }
            }
            return best;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.mate != null && this.mate.isEntityAlive() && this.mate.isInLove() && this.breedTicks < 60;
        }

        @Override
        public void resetTask() {
            this.mate = null;
            this.breedTicks = 0;
        }

        @Override
        public void updateTask() {
            this.turtle.getLookHelper().setLookPositionWithEntity(this.mate, 10.0F, this.turtle.getVerticalFaceSpeed());
            this.turtle.getNavigator().tryMoveToEntityLiving(this.mate, this.speed);
            if (++this.breedTicks >= 60 && this.turtle.getDistanceSq(this.mate) < 9.0D) this.breed();
        }

        private void breed() {
            EntityPlayerMP player = this.turtle.getLoveCause();
            if (player == null) player = this.mate.getLoveCause();
            if (player != null) {
                player.addStat(StatList.ANIMALS_BRED);
                CriteriaTriggers.BRED_ANIMALS.trigger(player, this.turtle, this.mate, null);
            }
            // Without turtle eggs there's nothing to lay.
            if (PortiumBlocks.TURTLE_EGG != null) this.turtle.setHasEgg(true);
            this.turtle.resetInLove();
            this.mate.resetInLove();
            World world = this.turtle.world;
            if (world.getGameRules().getBoolean("doMobLoot")) {
                world.spawnEntity(new EntityXPOrb(world, this.turtle.posX, this.turtle.posY, this.turtle.posZ, this.turtle.getRNG().nextInt(7) + 1));
            }
        }
    }

    /** Back home with eggs, digs into the sand for ten seconds and lays them. */
    static class AILayEgg extends EntityAIMoveToBlock {

        private final EntityTurtle turtle;

        AILayEgg(EntityTurtle turtle, double speed) {
            super(turtle, speed, 16);
            this.turtle = turtle;
        }

        @Override
        public boolean shouldExecute() {
            return this.turtle.hasEgg() && this.turtle.isNear(this.turtle.getHome(), 9.0D) && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return super.shouldContinueExecuting() && this.turtle.hasEgg() && this.turtle.isNear(this.turtle.getHome(), 9.0D);
        }

        @Override
        public void updateTask() {
            super.updateTask();
            BlockPos pos = new BlockPos(this.turtle);
            if (!this.turtle.isInWater() && this.getIsAboveDestination()) {
                if (this.turtle.digTicks < 1) {
                    this.turtle.setDigging(true);
                } else if (this.turtle.digTicks > 200) {
                    World world = this.turtle.world;
                    world.playSound(null, pos, PortiumSounds.ENTITY_TURTLE_LAY_EGG, SoundCategory.BLOCKS, 0.3F, 0.9F + world.rand.nextFloat() * 0.2F);
                    world.setBlockState(this.destinationBlock.up(), PortiumBlocks.TURTLE_EGG.getDefaultState()
                            .withProperty(BlockTurtleEgg.EGGS, this.turtle.rand.nextInt(4) + 1), 3);
                    this.turtle.setHasEgg(false);
                    this.turtle.setDigging(false);
                    this.turtle.setInLove(null);
                }
                if (this.turtle.isDigging()) this.turtle.digTicks++;
            }
        }

        @Override
        protected boolean shouldMoveTo(World world, BlockPos pos) {
            return world.isAirBlock(pos.up()) && world.getBlockState(pos).getBlock() == Blocks.SAND;
        }
    }

    /** Follows a player holding seagrass. */
    static class AITempt extends EntityAIBase {

        private final EntityTurtle turtle;
        private final double speed;
        private final Item item;
        @Nullable
        private EntityPlayer tempter;
        private int cooldown;

        AITempt(EntityTurtle turtle, double speed, Item item) {
            this.turtle = turtle;
            this.speed = speed;
            this.item = item;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            if (this.cooldown > 0) {
                --this.cooldown;
                return false;
            }
            this.tempter = this.turtle.world.getClosestPlayerToEntity(this.turtle, 10.0D);
            if (this.tempter == null) return false;
            return this.tempter.getHeldItemMainhand().getItem() == this.item || this.tempter.getHeldItemOffhand().getItem() == this.item;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.shouldExecute();
        }

        @Override
        public void resetTask() {
            this.tempter = null;
            this.turtle.getNavigator().clearPath();
            this.cooldown = 100;
        }

        @Override
        public void updateTask() {
            this.turtle.getLookHelper().setLookPositionWithEntity(this.tempter, this.turtle.getHorizontalFaceSpeed() + 20, this.turtle.getVerticalFaceSpeed());
            if (this.turtle.getDistanceSq(this.tempter) < 6.25D) this.turtle.getNavigator().clearPath();
            else this.turtle.getNavigator().tryMoveToEntityLiving(this.tempter, this.speed);
        }
    }

    /** Heads back into the water when stranded on land (babies always, adults unless going home or carrying eggs). */
    static class AIGoToWater extends EntityAIMoveToBlock {

        private final EntityTurtle turtle;

        AIGoToWater(EntityTurtle turtle, double speed) {
            super(turtle, speed, 24);
            this.turtle = turtle;
        }

        @Override
        public boolean shouldExecute() {
            if (this.turtle.isChild() && !this.turtle.isInWater()) return super.shouldExecute();
            return !this.turtle.isGoingHome() && !this.turtle.isInWater() && !this.turtle.hasEgg() && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.turtle.isInWater() && super.shouldContinueExecuting();
        }

        @Override
        protected boolean shouldMoveTo(World world, BlockPos pos) {
            return world.getBlockState(pos).getBlock() == Blocks.WATER;
        }
    }

    /** Swims back to the home beach to lay eggs, or now and then when it has strayed far. */
    static class AIGoHome extends EntityAIBase {

        private final EntityTurtle turtle;
        private final double speed;
        private boolean noPath;
        private int closeTicks;

        AIGoHome(EntityTurtle turtle, double speed) {
            this.turtle = turtle;
            this.speed = speed;
        }

        @Override
        public boolean shouldExecute() {
            if (this.turtle.isChild()) return false;
            if (this.turtle.hasEgg()) return true;
            return this.turtle.getRNG().nextInt(700) == 0 && !this.turtle.isNear(this.turtle.getHome(), 64.0D);
        }

        @Override
        public void startExecuting() {
            this.turtle.setGoingHome(true);
            this.noPath = false;
            this.closeTicks = 0;
        }

        @Override
        public void resetTask() {
            this.turtle.setGoingHome(false);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.turtle.isNear(this.turtle.getHome(), 7.0D) && !this.noPath && this.closeTicks <= 600;
        }

        @Override
        public void updateTask() {
            BlockPos home = this.turtle.getHome();
            boolean close = this.turtle.isNear(home, 16.0D);
            if (close) ++this.closeTicks;
            if (!this.turtle.getNavigator().noPath()) return;
            Vec3d towards = new Vec3d(home);
            Vec3d target = RandomPositionGenerator.findRandomTargetBlockTowards(this.turtle, 16, 3, towards);
            if (target == null) target = RandomPositionGenerator.findRandomTargetBlockTowards(this.turtle, 8, 7, towards);
            if (target != null && !close && this.turtle.world.getBlockState(new BlockPos(target)).getBlock() != Blocks.WATER) {
                target = RandomPositionGenerator.findRandomTargetBlockTowards(this.turtle, 16, 5, towards);
            }
            if (target == null) {
                this.noPath = true;
                return;
            }
            this.turtle.getNavigator().tryMoveToXYZ(target.x, target.y, target.z, this.speed);
        }
    }

    /** Picks a far-off point at sea (up to 512 blocks away) and swims towards it. */
    static class AITravel extends EntityAIBase {

        private final EntityTurtle turtle;
        private final double speed;
        private boolean stuck;

        AITravel(EntityTurtle turtle, double speed) {
            this.turtle = turtle;
            this.speed = speed;
        }

        @Override
        public boolean shouldExecute() {
            return !this.turtle.isGoingHome() && !this.turtle.hasEgg() && this.turtle.isInWater();
        }

        @Override
        public void startExecuting() {
            java.util.Random random = this.turtle.rand;
            int dx = random.nextInt(1025) - 512;
            int dy = random.nextInt(9) - 4;
            int dz = random.nextInt(1025) - 512;
            if (dy + this.turtle.posY > this.turtle.world.getSeaLevel() - 1) dy = 0;
            this.turtle.setTravelPos(new BlockPos(dx + this.turtle.posX, dy + this.turtle.posY, dz + this.turtle.posZ));
            this.turtle.setTravelling(true);
            this.stuck = false;
        }

        @Override
        public void updateTask() {
            if (!this.turtle.getNavigator().noPath()) return;
            Vec3d towards = new Vec3d(this.turtle.getTravelPos());
            Vec3d target = RandomPositionGenerator.findRandomTargetBlockTowards(this.turtle, 16, 3, towards);
            if (target == null) target = RandomPositionGenerator.findRandomTargetBlockTowards(this.turtle, 8, 7, towards);
            if (target != null) {
                int x = MathHelper.floor(target.x);
                int z = MathHelper.floor(target.z);
                if (!this.turtle.world.isAreaLoaded(new BlockPos(x - 34, 0, z - 34), new BlockPos(x + 34, 0, z + 34))) target = null;
            }
            if (target == null) {
                this.stuck = true;
                return;
            }
            this.turtle.getNavigator().tryMoveToXYZ(target.x, target.y, target.z, this.speed);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.turtle.getNavigator().noPath() && !this.stuck && !this.turtle.isGoingHome() && !this.turtle.isInLove() && !this.turtle.hasEgg();
        }

        @Override
        public void resetTask() {
            this.turtle.setTravelling(false);
        }
    }

    /** Wanders on land when it has nothing better to do. */
    static class AIWander extends EntityAIWander {

        private final EntityTurtle turtle;

        AIWander(EntityTurtle turtle, double speed, int chance) {
            super(turtle, speed, chance);
            this.turtle = turtle;
        }

        @Override
        public boolean shouldExecute() {
            return !this.turtle.isInWater() && !this.turtle.isGoingHome() && !this.turtle.hasEgg() && super.shouldExecute();
        }
    }
}
