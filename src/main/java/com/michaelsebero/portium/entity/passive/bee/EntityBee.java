package com.michaelsebero.portium.entity.passive.bee;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.block.bee.BlockBeehive;
import com.michaelsebero.portium.entity.ai.LookHelper116;
import com.michaelsebero.portium.entity.ai.brain.TargetPredicate;
import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.mixin.accessor.EntityAnimalAccessor;
import com.michaelsebero.portium.mixin.accessor.EntityLivingAccessor;
import com.michaelsebero.portium.proxy.ParticleKind;
import com.michaelsebero.portium.tile.TileEntityBeehive;
import com.michaelsebero.portium.world.PortiumGameRules;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateFlying;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 1.15's bee: flies between flowers and its hive, carrying nectar home (where it becomes honey) and growing crops it
 * passes; goes in at night and in the rain; angered (with every bee about) when hit or its hive is disturbed, it
 * stings once, poisoning, and soon dies.
 */
public class EntityBee extends EntityAnimal {

    private static final DataParameter<Byte> FLAGS = EntityDataManager.createKey(EntityBee.class, DataSerializers.BYTE);
    private static final DataParameter<Integer> ANGER_TIME = EntityDataManager.createKey(EntityBee.class, DataSerializers.VARINT);
    private static final int FLAG_ROLL = 2;
    private static final int FLAG_HAS_STUNG = 4;
    private static final int FLAG_HAS_NECTAR = 8;

    @Nullable
    private UUID angerTarget;
    private float rollAmount;
    private float prevRollAmount;
    private int timeSinceSting;
    private int ticksWithoutNectarSinceExitingHive;
    private int stayOutOfHiveCountdown;
    private int numCropsGrownSincePollination;
    private int remainingCooldownBeforeLocatingNewHive;
    private int remainingCooldownBeforeLocatingNewFlower;
    @Nullable
    private BlockPos savedFlowerPos;
    @Nullable
    private BlockPos hivePos;
    private PollinateGoal pollinateGoal;
    private FindBeehiveGoal findBeehiveGoal;
    private int underWaterTicks;

    public EntityBee(World world) {
        super(world);
        this.setSize(0.7F, 0.6F);
        this.moveHelper = new FlyHelper116(this, 20, true);
        ((EntityLivingAccessor) this).portium$setLookHelper(new LookHelper116(this) {
            @Override
            public void onUpdateLook() {
                if (!EntityBee.this.isAngry()) super.onUpdateLook();
            }

            @Override
            protected boolean shouldResetPitch() {
                return EntityBee.this.pollinateGoal == null || !EntityBee.this.pollinateGoal.isRunning();
            }
        });
        this.setPathPriority(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathPriority(PathNodeType.WATER, -1.0F);
        // 1.15's WATER_BORDER (16 for bees), which Portium marks as 1.12's DANGER_OTHER.
        this.setPathPriority(PathNodeType.DANGER_OTHER, 16.0F);
        this.setPathPriority(PathNodeType.FENCE, -1.0F);
    }

    /** Night where there's a day (not in the Nether or the End), as 1.15's {@code isNightTime}. */
    public static boolean isNight(World world) {
        return world.provider.isSurfaceWorld() && !world.isDaytime();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(FLAGS, (byte) 0);
        this.dataManager.register(ANGER_TIME, 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.FLYING_SPEED).setBaseValue(0.6D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(2.0D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(48.0D);
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        PathNavigateFlying navigator = new PathNavigateFlying(this, world) {
            @Override
            public boolean canEntityStandOnPos(BlockPos pos) {
                return !this.world.isAirBlock(pos.down());
            }

            @Override
            public void onUpdateNavigation() {
                if (EntityBee.this.pollinateGoal == null || !EntityBee.this.pollinateGoal.isRunning()) super.onUpdateNavigation();
            }
        };
        navigator.setCanOpenDoors(false);
        navigator.setCanFloat(false);
        navigator.setCanEnterDoors(true);
        return navigator;
    }

    @Override
    public float getBlockPathWeight(BlockPos pos) {
        return this.world.isAirBlock(pos) ? 10.0F : 0.0F;
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new StingGoal(this));
        this.tasks.addTask(1, new EnterBeehiveGoal());
        this.tasks.addTask(2, new EntityAIMate(this, 1.0D));
        this.tasks.addTask(3, new TemptGoal(this, 1.25D));
        this.pollinateGoal = new PollinateGoal();
        this.tasks.addTask(4, this.pollinateGoal);
        this.tasks.addTask(5, new EntityAIFollowParent(this, 1.25D));
        this.tasks.addTask(5, new UpdateBeehiveGoal());
        this.findBeehiveGoal = new FindBeehiveGoal();
        this.tasks.addTask(5, this.findBeehiveGoal);
        this.tasks.addTask(6, new FindFlowerGoal());
        this.tasks.addTask(7, new FindPollinationTargetGoal());
        this.tasks.addTask(8, new WanderGoal());
        this.tasks.addTask(9, new EntityAISwimming(this));
        this.targetTasks.addTask(1, new AngerGoal(this));
        this.targetTasks.addTask(2, new AttackPlayerGoal(this));
    }

    // ---- Saving ----

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (this.hivePos != null) compound.setTag("HivePos", NBTUtil.createPosTag(this.hivePos));
        if (this.savedFlowerPos != null) compound.setTag("FlowerPos", NBTUtil.createPosTag(this.savedFlowerPos));
        compound.setBoolean("HasNectar", this.hasNectar());
        compound.setBoolean("HasStung", this.hasStung());
        compound.setInteger("TicksSincePollination", this.ticksWithoutNectarSinceExitingHive);
        compound.setInteger("CannotEnterHiveTicks", this.stayOutOfHiveCountdown);
        compound.setInteger("CropsGrownSincePollination", this.numCropsGrownSincePollination);
        compound.setInteger("AngerTime", this.getAngerTime());
        if (this.angerTarget != null) compound.setUniqueId("AngryAt", this.angerTarget);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        this.hivePos = compound.hasKey("HivePos", 10) ? NBTUtil.getPosFromTag(compound.getCompoundTag("HivePos")) : null;
        this.savedFlowerPos = compound.hasKey("FlowerPos", 10) ? NBTUtil.getPosFromTag(compound.getCompoundTag("FlowerPos")) : null;
        super.readEntityFromNBT(compound);
        this.setHasNectar(compound.getBoolean("HasNectar"));
        this.setHasStung(compound.getBoolean("HasStung"));
        this.ticksWithoutNectarSinceExitingHive = compound.getInteger("TicksSincePollination");
        this.stayOutOfHiveCountdown = compound.getInteger("CannotEnterHiveTicks");
        this.numCropsGrownSincePollination = compound.getInteger("CropsGrownSincePollination");
        this.setAngerTime(compound.getInteger("AngerTime"));
        this.angerTarget = compound.hasUniqueId("AngryAt") ? compound.getUniqueId("AngryAt") : null;
        if (this.angerTarget != null && this.world instanceof WorldServer) {
            Entity target = ((WorldServer) this.world).getEntityFromUuid(this.angerTarget);
            if (target instanceof EntityLivingBase) this.setRevengeTarget((EntityLivingBase) target);
        }
    }

    // ---- Stinging and anger (1.16's IAngerable) ----

    @Override
    public boolean attackEntityAsMob(Entity target) {
        DamageSource sting = new EntityDamageSource("sting", this);
        boolean hurt = target.attackEntityFrom(sting, (float) (int) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
        if (hurt) {
            this.applyEnchantments(this, target);
            if (target instanceof EntityLivingBase) {
                int seconds = this.world.getDifficulty() == EnumDifficulty.NORMAL ? 10 : this.world.getDifficulty() == EnumDifficulty.HARD ? 18 : 0;
                if (seconds > 0) ((EntityLivingBase) target).addPotionEffect(new PotionEffect(MobEffects.POISON, seconds * 20, 0));
                // 1.15: the stinger stays in what it stung.
                if (target instanceof com.michaelsebero.portium.entity.IBeeStingers) {
                    com.michaelsebero.portium.entity.IBeeStingers stung = (com.michaelsebero.portium.entity.IBeeStingers) target;
                    stung.portium$setBeeStingers(stung.portium$getBeeStingers() + 1);
                }
            }
            this.setHasStung(true);
            this.resetAnger();
            this.playSound(PortiumSounds.ENTITY_BEE_STING, 1.0F, 1.0F);
        }
        return hurt;
    }

    public boolean isAngry() {
        return this.getAngerTime() > 0;
    }

    public int getAngerTime() {
        return this.dataManager.get(ANGER_TIME);
    }

    public void setAngerTime(int ticks) {
        this.dataManager.set(ANGER_TIME, ticks);
    }

    private void setRandomAngerTime() {
        this.setAngerTime(400 + this.rand.nextInt(380));
    }

    private void resetAnger() {
        this.setRevengeTarget(null);
        this.angerTarget = null;
        this.setAttackTarget(null);
        this.setAngerTime(0);
    }

    /** 1.16's {@code IAngerable.func_241359_a_}: anger follows the attack target, and wears off. */
    private void updateAnger() {
        EntityLivingBase target = this.getAttackTarget();
        if ((target == null || !target.isEntityAlive()) && this.angerTarget != null
                && ((WorldServer) this.world).getEntityFromUuid(this.angerTarget) instanceof net.minecraft.entity.EntityLiving) {
            this.resetAnger();
            return;
        }
        if (target != null && !target.getUniqueID().equals(this.angerTarget)) {
            this.angerTarget = target.getUniqueID();
            this.setRandomAngerTime();
        }
        if (this.getAngerTime() > 0) {
            this.setAngerTime(this.getAngerTime() - 1);
            if (this.getAngerTime() == 0) this.resetAnger();
        }
        if (target instanceof EntityPlayer && !target.isEntityAlive() && PortiumGameRules.get(this.world, PortiumGameRules.FORGIVE_DEAD_PLAYERS, true)) {
            this.resetAnger();
        }
    }

    /** 1.16's {@code func_233680_b_}: angry at this one (or, under universal anger, any player). */
    boolean isAngryAt(EntityLivingBase entity) {
        if (!TargetPredicate.canHostileTarget(entity)) return false;
        if (entity instanceof EntityPlayer && PortiumGameRules.get(this.world, PortiumGameRules.UNIVERSAL_ANGER, false) && this.isAngry()) return true;
        return entity.getUniqueID().equals(this.angerTarget);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.isEntityInvulnerable(source)) return false;
        if (!this.world.isRemote && this.pollinateGoal != null) this.pollinateGoal.cancel();
        return super.attackEntityFrom(source, amount);
    }

    // ---- Ticking ----

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote && this.hasNectar() && this.numCropsGrownSincePollination < 10 && this.rand.nextFloat() < 0.05F) {
            for (int i = 0; i < this.rand.nextInt(2) + 1; ++i) {
                Portium.proxy.spawnParticle(ParticleKind.FALLING_NECTAR, this.world, this.posX + (this.rand.nextDouble() - 0.5D) * 0.6D,
                        this.posY + this.height * 0.5D, this.posZ + (this.rand.nextDouble() - 0.5D) * 0.6D, 0.0D, 0.0D, 0.0D);
            }
        }
        this.updateBodyPitch();
    }

    private void updateBodyPitch() {
        this.prevRollAmount = this.rollAmount;
        this.rollAmount = this.isNearTarget() ? Math.min(1.0F, this.rollAmount + 0.2F) : Math.max(0.0F, this.rollAmount - 0.24F);
    }

    public float getBodyPitch(float partialTicks) {
        return this.prevRollAmount + (this.rollAmount - this.prevRollAmount) * partialTicks;
    }

    @Override
    protected void updateAITasks() {
        if (this.isInWater()) {
            ++this.underWaterTicks;
        } else {
            this.underWaterTicks = 0;
        }
        if (this.underWaterTicks > 20) this.attackEntityFrom(DamageSource.DROWN, 1.0F);
        if (this.hasStung()) {
            ++this.timeSinceSting;
            if (this.timeSinceSting % 5 == 0 && this.rand.nextInt(MathHelper.clamp(1200 - this.timeSinceSting, 1, 1200)) == 0) {
                this.attackEntityFrom(DamageSource.GENERIC, this.getHealth());
            }
        }
        if (!this.hasNectar()) ++this.ticksWithoutNectarSinceExitingHive;
        this.updateAnger();
        super.updateAITasks();
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) return;
        if (this.stayOutOfHiveCountdown > 0) --this.stayOutOfHiveCountdown;
        if (this.remainingCooldownBeforeLocatingNewHive > 0) --this.remainingCooldownBeforeLocatingNewHive;
        if (this.remainingCooldownBeforeLocatingNewFlower > 0) --this.remainingCooldownBeforeLocatingNewFlower;
        EntityLivingBase target = this.getAttackTarget();
        this.setNearTarget(this.isAngry() && !this.hasStung() && target != null && target.getDistanceSq(this) < 4.0D);
        if (this.ticksExisted % 20 == 0 && !this.isHiveValid()) this.hivePos = null;
    }

    // ---- Hive and flower ----

    private void startMovingTo(BlockPos pos) {
        Vec3d target = new Vec3d(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        BlockPos at = new BlockPos(this);
        int distance = Math.abs(at.getX() - pos.getX()) + Math.abs(at.getY() - pos.getY()) + Math.abs(at.getZ() - pos.getZ());
        int horizontal = 6;
        int vertical = 8;
        if (distance < 15) {
            horizontal = distance / 2;
            vertical = distance / 2;
        }
        Vec3d towards = RandomPositionGenerator.findRandomTargetBlockTowards(this, Math.max(1, horizontal), Math.max(1, vertical), target);
        if (towards != null) this.navigator.tryMoveToXYZ(towards.x, towards.y, towards.z, 1.0D);
    }

    @Nullable
    public BlockPos getFlowerPos() {
        return this.savedFlowerPos;
    }

    public boolean hasFlower() {
        return this.savedFlowerPos != null;
    }

    public void setFlowerPos(BlockPos pos) {
        this.savedFlowerPos = pos;
    }

    @Nullable
    public BlockPos getHivePos() {
        return this.hivePos;
    }

    public boolean hasHive() {
        return this.hivePos != null;
    }

    private boolean failedPollinatingTooLong() {
        return this.ticksWithoutNectarSinceExitingHive > 3600;
    }

    private boolean canEnterHive() {
        if (this.stayOutOfHiveCountdown > 0 || this.pollinateGoal.isRunning() || this.hasStung() || this.getAttackTarget() != null) return false;
        boolean wantsIn = this.failedPollinatingTooLong() || this.world.isRaining() || isNight(this.world) || this.hasNectar();
        return wantsIn && !this.isHiveNearFire();
    }

    public void setStayOutOfHiveCountdown(int ticks) {
        this.stayOutOfHiveCountdown = ticks;
    }

    public void resetTicksWithoutNectar() {
        this.ticksWithoutNectarSinceExitingHive = 0;
    }

    /** Time in the hive cuts the love time short, as 1.15 does. */
    public void shortenLove(int ticks) {
        EntityAnimalAccessor accessor = (EntityAnimalAccessor) this;
        accessor.portium$setInLove(Math.max(0, accessor.portium$getInLove() - ticks));
    }

    private boolean isHiveNearFire() {
        if (this.hivePos == null) return false;
        TileEntity tile = this.world.getTileEntity(this.hivePos);
        return tile instanceof TileEntityBeehive && ((TileEntityBeehive) tile).isNearFire();
    }

    private boolean doesHiveHaveSpace(BlockPos pos) {
        TileEntity tile = this.world.getTileEntity(pos);
        return tile instanceof TileEntityBeehive && !((TileEntityBeehive) tile).isFullOfBees();
    }

    private boolean isHiveValid() {
        return this.hivePos != null && this.world.isBlockLoaded(this.hivePos) && this.world.getTileEntity(this.hivePos) instanceof TileEntityBeehive;
    }

    public void onHoneyDelivered() {
        this.setHasNectar(false);
        this.numCropsGrownSincePollination = 0;
    }

    public boolean hasNectar() {
        return this.getBeeFlag(FLAG_HAS_NECTAR);
    }

    private void setHasNectar(boolean nectar) {
        if (nectar) this.resetTicksWithoutNectar();
        this.setBeeFlag(FLAG_HAS_NECTAR, nectar);
    }

    public boolean hasStung() {
        return this.getBeeFlag(FLAG_HAS_STUNG);
    }

    private void setHasStung(boolean stung) {
        this.setBeeFlag(FLAG_HAS_STUNG, stung);
    }

    private boolean isNearTarget() {
        return this.getBeeFlag(FLAG_ROLL);
    }

    private void setNearTarget(boolean near) {
        this.setBeeFlag(FLAG_ROLL, near);
    }

    private boolean isTooFar(BlockPos pos) {
        return !this.isWithinDistance(pos, 32);
    }

    private boolean isWithinDistance(BlockPos pos, int distance) {
        return pos.distanceSqToCenter(this.posX, this.posY, this.posZ) < (double) (distance * distance);
    }

    private void setBeeFlag(int flag, boolean on) {
        byte flags = this.dataManager.get(FLAGS);
        this.dataManager.set(FLAGS, (byte) (on ? flags | flag : flags & ~flag));
    }

    private boolean getBeeFlag(int flag) {
        return (this.dataManager.get(FLAGS) & flag) != 0;
    }

    // ---- Animal ----

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return BeeFlowers.isFlowerItem(stack);
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        return new EntityBee(this.world);
    }

    @Override
    public float getEyeHeight() {
        return this.height * 0.5F;
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    protected void updateFallState(double y, boolean onGround, IBlockState state, BlockPos pos) {
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.ARTHROPOD;
    }

    @Override
    protected void handleJumpWater() {
        this.motionY += 0.01D;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_BEE_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return PortiumLootTables.BEE;
    }

    /** Bees don't spawn by themselves: they come out of nests. */
    @Override
    public boolean getCanSpawnHere() {
        return false;
    }

    // ---- Goals ----

    /** A goal that pauses while the bee is angry. */
    abstract class PassiveGoal extends EntityAIBase {
        abstract boolean canBeeStart();

        abstract boolean canBeeContinue();

        @Override
        public boolean shouldExecute() {
            return this.canBeeStart() && !EntityBee.this.isAngry();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.canBeeContinue() && !EntityBee.this.isAngry();
        }
    }

    final class EnterBeehiveGoal extends PassiveGoal {
        @Override
        boolean canBeeStart() {
            if (EntityBee.this.hasHive() && EntityBee.this.canEnterHive() && EntityBee.this.isWithinDistance(EntityBee.this.hivePos, 2)) {
                TileEntity tile = EntityBee.this.world.getTileEntity(EntityBee.this.hivePos);
                if (tile instanceof TileEntityBeehive) {
                    if (!((TileEntityBeehive) tile).isFullOfBees()) return true;
                    EntityBee.this.hivePos = null;
                }
            }
            return false;
        }

        @Override
        boolean canBeeContinue() {
            return false;
        }

        @Override
        public void startExecuting() {
            TileEntity tile = EntityBee.this.world.getTileEntity(EntityBee.this.hivePos);
            if (tile instanceof TileEntityBeehive) ((TileEntityBeehive) tile).tryEnterHive(EntityBee.this, EntityBee.this.hasNectar());
        }
    }

    static final class StingGoal extends EntityAIAttackMelee {
        private final EntityBee bee;

        StingGoal(EntityBee bee) {
            super(bee, 1.4D, true);
            this.bee = bee;
        }

        @Override
        public boolean shouldExecute() {
            return super.shouldExecute() && this.bee.isAngry() && !this.bee.hasStung();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return super.shouldContinueExecuting() && this.bee.isAngry() && !this.bee.hasStung();
        }
    }

    /** With nectar, grows a crop below now and then (up to ten per trip). */
    final class FindPollinationTargetGoal extends PassiveGoal {
        @Override
        boolean canBeeStart() {
            if (EntityBee.this.numCropsGrownSincePollination >= 10 || EntityBee.this.rand.nextFloat() < 0.3F) return false;
            return EntityBee.this.hasNectar() && EntityBee.this.isHiveValid();
        }

        @Override
        boolean canBeeContinue() {
            return this.canBeeStart();
        }

        @Override
        public void updateTask() {
            if (EntityBee.this.rand.nextInt(30) != 0) return;
            for (int down = 1; down <= 2; ++down) {
                BlockPos pos = new BlockPos(EntityBee.this).down(down);
                IBlockState state = EntityBee.this.world.getBlockState(pos);
                PropertyInteger age = BeeFlowers.growableAge(state);
                if (age != null) {
                    EntityBee.this.world.playEvent(2005, pos, 0);
                    EntityBee.this.world.setBlockState(pos, state.withProperty(age, state.getValue(age) + 1));
                    ++EntityBee.this.numCropsGrownSincePollination;
                }
            }
        }
    }

    /** Picks the nearest hive with room within 20 blocks (not one it recently gave up on). */
    final class UpdateBeehiveGoal extends PassiveGoal {
        @Override
        boolean canBeeStart() {
            return EntityBee.this.remainingCooldownBeforeLocatingNewHive == 0 && !EntityBee.this.hasHive() && EntityBee.this.canEnterHive();
        }

        @Override
        boolean canBeeContinue() {
            return false;
        }

        @Override
        public void startExecuting() {
            EntityBee.this.remainingCooldownBeforeLocatingNewHive = 200;
            List<BlockPos> hives = this.getNearbyFreeHives();
            if (hives.isEmpty()) return;
            for (BlockPos hive : hives) {
                if (!EntityBee.this.findBeehiveGoal.isPossibleHive(hive)) {
                    EntityBee.this.hivePos = hive;
                    return;
                }
            }
            EntityBee.this.findBeehiveGoal.clearPossibleHives();
            EntityBee.this.hivePos = hives.get(0);
        }

        private List<BlockPos> getNearbyFreeHives() {
            BlockPos at = new BlockPos(EntityBee.this);
            List<BlockPos> hives = new ArrayList<>();
            for (TileEntity tile : EntityBee.this.world.loadedTileEntityList) {
                if (tile instanceof TileEntityBeehive && !tile.isInvalid() && tile.getPos().distanceSq(at) <= 400.0D) hives.add(tile.getPos());
            }
            return hives.stream().filter(EntityBee.this::doesHiveHaveSpace).sorted(Comparator.comparingDouble(pos -> pos.distanceSq(at)))
                    .collect(Collectors.toList());
        }
    }

    /** Hovers over a flower nearby for about twenty seconds, gathering nectar. */
    final class PollinateGoal extends PassiveGoal {
        private int pollinationTicks;
        private int lastPollinationTick;
        private boolean running;
        @Nullable
        private Vec3d nextTarget;
        private int ticks;

        PollinateGoal() {
            this.setMutexBits(1);
        }

        @Override
        boolean canBeeStart() {
            if (EntityBee.this.remainingCooldownBeforeLocatingNewFlower > 0 || EntityBee.this.hasNectar() || EntityBee.this.world.isRaining()) return false;
            if (EntityBee.this.rand.nextFloat() < 0.7F) return false;
            Optional<BlockPos> flower = this.findFlower(5.0D);
            if (!flower.isPresent()) return false;
            EntityBee.this.savedFlowerPos = flower.get();
            EntityBee.this.navigator.tryMoveToXYZ(flower.get().getX() + 0.5D, flower.get().getY() + 0.5D, flower.get().getZ() + 0.5D, 1.2D);
            return true;
        }

        @Override
        boolean canBeeContinue() {
            if (!this.running || !EntityBee.this.hasFlower() || EntityBee.this.world.isRaining()) return false;
            if (this.completedPollination()) return EntityBee.this.rand.nextFloat() < 0.2F;
            if (EntityBee.this.ticksExisted % 20 == 0 && !BeeFlowers.isFlower(EntityBee.this.world, EntityBee.this.savedFlowerPos)) {
                EntityBee.this.savedFlowerPos = null;
                return false;
            }
            return true;
        }

        private boolean completedPollination() {
            return this.pollinationTicks > 400;
        }

        boolean isRunning() {
            return this.running;
        }

        void cancel() {
            this.running = false;
        }

        @Override
        public void startExecuting() {
            this.pollinationTicks = 0;
            this.ticks = 0;
            this.lastPollinationTick = 0;
            this.running = true;
            EntityBee.this.resetTicksWithoutNectar();
        }

        @Override
        public void resetTask() {
            if (this.completedPollination()) EntityBee.this.setHasNectar(true);
            this.running = false;
            EntityBee.this.navigator.clearPath();
            EntityBee.this.remainingCooldownBeforeLocatingNewFlower = 200;
        }

        @Override
        public void updateTask() {
            ++this.ticks;
            if (this.ticks > 600 || EntityBee.this.savedFlowerPos == null) {
                EntityBee.this.savedFlowerPos = null;
                return;
            }
            BlockPos flower = EntityBee.this.savedFlowerPos;
            Vec3d above = new Vec3d(flower.getX() + 0.5D, flower.getY() + 0.6D, flower.getZ() + 0.5D);
            Vec3d at = EntityBee.this.getPositionVector();
            if (above.distanceTo(at) > 1.0D) {
                this.nextTarget = above;
                this.moveToNextTarget();
                return;
            }
            if (this.nextTarget == null) this.nextTarget = above;
            boolean arrived = at.distanceTo(this.nextTarget) <= 0.1D;
            boolean move = true;
            if (!arrived && this.ticks > 600) {
                EntityBee.this.savedFlowerPos = null;
                return;
            }
            if (arrived) {
                if (EntityBee.this.rand.nextInt(25) == 0) {
                    this.nextTarget = new Vec3d(above.x + this.randomOffset(), above.y, above.z + this.randomOffset());
                    EntityBee.this.navigator.clearPath();
                } else {
                    move = false;
                }
                EntityBee.this.getLookHelper().setLookPosition(above.x, above.y, above.z, EntityBee.this.getHorizontalFaceSpeed(),
                        EntityBee.this.getVerticalFaceSpeed());
            }
            if (move) this.moveToNextTarget();
            ++this.pollinationTicks;
            if (EntityBee.this.rand.nextFloat() < 0.05F && this.pollinationTicks > this.lastPollinationTick + 60) {
                this.lastPollinationTick = this.pollinationTicks;
                EntityBee.this.playSound(PortiumSounds.ENTITY_BEE_POLLINATE, 1.0F, 1.0F);
            }
        }

        private void moveToNextTarget() {
            EntityBee.this.getMoveHelper().setMoveTo(this.nextTarget.x, this.nextTarget.y, this.nextTarget.z, 0.35D);
        }

        private float randomOffset() {
            return (EntityBee.this.rand.nextFloat() * 2.0F - 1.0F) * 0.33333334F;
        }

        /** 1.15's search: outward in rings, one block below the bee up to five above, within five blocks. */
        private Optional<BlockPos> findFlower(double range) {
            BlockPos origin = new BlockPos(EntityBee.this);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int dy = 0; dy <= range; dy = dy > 0 ? -dy : 1 - dy) {
                for (int ring = 0; ring < range; ++ring) {
                    for (int dx = 0; dx <= ring; dx = dx > 0 ? -dx : 1 - dx) {
                        for (int dz = dx < ring && dx > -ring ? ring : 0; dz <= ring; dz = dz > 0 ? -dz : 1 - dz) {
                            pos.setPos(origin.getX() + dx, origin.getY() + dy - 1, origin.getZ() + dz);
                            if (origin.distanceSq(pos) < range * range && BeeFlowers.canPollinate(EntityBee.this.world, pos)) {
                                return Optional.of(pos.toImmutable());
                            }
                        }
                    }
                }
            }
            return Optional.empty();
        }
    }

    /** After two minutes without nectar, flies back to a remembered flower. */
    final class FindFlowerGoal extends PassiveGoal {
        private int ticks;

        FindFlowerGoal() {
            this.ticks = EntityBee.this.world.rand.nextInt(10);
            this.setMutexBits(1);
        }

        @Override
        boolean canBeeStart() {
            BlockPos flower = EntityBee.this.savedFlowerPos;
            return flower != null && !EntityBee.this.hasHome() && EntityBee.this.ticksWithoutNectarSinceExitingHive > 2400
                    && BeeFlowers.isFlower(EntityBee.this.world, flower) && !EntityBee.this.isWithinDistance(flower, 2);
        }

        @Override
        boolean canBeeContinue() {
            return this.canBeeStart();
        }

        @Override
        public void startExecuting() {
            this.ticks = 0;
        }

        @Override
        public void resetTask() {
            this.ticks = 0;
            EntityBee.this.navigator.clearPath();
        }

        @Override
        public void updateTask() {
            if (EntityBee.this.savedFlowerPos == null) return;
            if (++this.ticks > 600) {
                EntityBee.this.savedFlowerPos = null;
            } else if (EntityBee.this.navigator.noPath()) {
                if (EntityBee.this.isTooFar(EntityBee.this.savedFlowerPos)) {
                    EntityBee.this.savedFlowerPos = null;
                } else {
                    EntityBee.this.startMovingTo(EntityBee.this.savedFlowerPos);
                }
            }
        }
    }

    /** Flies home when it wants to go in; gives up on a hive it can't reach. */
    final class FindBeehiveGoal extends PassiveGoal {
        private int ticks;
        private final List<BlockPos> possibleHives = new ArrayList<>();
        @Nullable
        private Path path;
        private int samePathTicks;

        FindBeehiveGoal() {
            this.ticks = EntityBee.this.world.rand.nextInt(10);
            this.setMutexBits(1);
        }

        @Override
        boolean canBeeStart() {
            return EntityBee.this.hivePos != null && !EntityBee.this.hasHome() && EntityBee.this.canEnterHive() && !this.isCloseEnough(EntityBee.this.hivePos)
                    && EntityBee.this.world.getBlockState(EntityBee.this.hivePos).getBlock() instanceof BlockBeehive;
        }

        @Override
        boolean canBeeContinue() {
            return this.canBeeStart();
        }

        @Override
        public void startExecuting() {
            this.ticks = 0;
            this.samePathTicks = 0;
        }

        @Override
        public void resetTask() {
            this.ticks = 0;
            this.samePathTicks = 0;
            EntityBee.this.navigator.clearPath();
        }

        @Override
        public void updateTask() {
            BlockPos hive = EntityBee.this.hivePos;
            if (hive == null) return;
            if (++this.ticks > 600) {
                this.makeChosenHivePossibleHive();
            } else if (EntityBee.this.navigator.noPath()) {
                if (!EntityBee.this.isWithinDistance(hive, 16)) {
                    if (EntityBee.this.isTooFar(hive)) {
                        this.reset();
                    } else {
                        EntityBee.this.startMovingTo(hive);
                    }
                } else if (!this.startMovingToFar(hive)) {
                    this.makeChosenHivePossibleHive();
                } else if (this.path != null && EntityBee.this.navigator.getPath() != null && EntityBee.this.navigator.getPath().isSamePath(this.path)) {
                    if (++this.samePathTicks > 60) {
                        this.reset();
                        this.samePathTicks = 0;
                    }
                } else {
                    this.path = EntityBee.this.navigator.getPath();
                }
            }
        }

        private boolean startMovingToFar(BlockPos hive) {
            EntityBee.this.navigator.tryMoveToXYZ(hive.getX(), hive.getY(), hive.getZ(), 1.0D);
            Path current = EntityBee.this.navigator.getPath();
            return current != null && current.getFinalPathPoint() != null
                    && current.getFinalPathPoint().distanceToSquared(new net.minecraft.pathfinding.PathPoint(hive.getX(), hive.getY(), hive.getZ())) <= 4.0F;
        }

        boolean isPossibleHive(BlockPos pos) {
            return this.possibleHives.contains(pos);
        }

        private void addPossibleHive(BlockPos pos) {
            this.possibleHives.add(pos);
            while (this.possibleHives.size() > 3) this.possibleHives.remove(0);
        }

        void clearPossibleHives() {
            this.possibleHives.clear();
        }

        private void makeChosenHivePossibleHive() {
            if (EntityBee.this.hivePos != null) this.addPossibleHive(EntityBee.this.hivePos);
            this.reset();
        }

        private void reset() {
            EntityBee.this.hivePos = null;
            EntityBee.this.remainingCooldownBeforeLocatingNewHive = 200;
        }

        private boolean isCloseEnough(BlockPos pos) {
            return EntityBee.this.isWithinDistance(pos, 2);
        }
    }

    /** Wanders in the air, back towards the hive when more than 22 blocks from it. */
    final class WanderGoal extends EntityAIBase {
        WanderGoal() {
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return EntityBee.this.navigator.noPath() && EntityBee.this.rand.nextInt(10) == 0;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !EntityBee.this.navigator.noPath();
        }

        @Override
        public void startExecuting() {
            Vec3d target = this.getRandomLocation();
            if (target != null) EntityBee.this.navigator.setPath(EntityBee.this.navigator.getPathToPos(new BlockPos(target)), 1.0D);
        }

        @Nullable
        private Vec3d getRandomLocation() {
            Vec3d direction;
            if (EntityBee.this.isHiveValid() && !EntityBee.this.isWithinDistance(EntityBee.this.hivePos, 22)) {
                BlockPos hive = EntityBee.this.hivePos;
                direction = new Vec3d(hive.getX() + 0.5D - EntityBee.this.posX, hive.getY() + 0.5D - EntityBee.this.posY, hive.getZ() + 0.5D - EntityBee.this.posZ).normalize();
            } else {
                direction = EntityBee.this.getLook(0.0F);
            }
            Vec3d ahead = EntityBee.this.getPositionVector().add(direction.x * 8.0D, direction.y * 3.0D, direction.z * 8.0D);
            Vec3d air = RandomPositionGenerator.findRandomTargetBlockTowards(EntityBee.this, 8, 7, ahead);
            if (air != null && !EntityBee.this.world.isAirBlock(new BlockPos(air).down(3))) return air;
            return air != null ? air.add(0.0D, -Math.min(2.0D, air.y - EntityBee.this.posY), 0.0D) : RandomPositionGenerator.findRandomTarget(EntityBee.this, 8, 4);
        }
    }

    /** 1.12's tempt goal without its ground-navigator check: follows a player holding a flower within ten blocks. */
    static final class TemptGoal extends EntityAIBase {
        private final EntityBee bee;
        private final double speed;
        @Nullable
        private EntityPlayer player;
        private int delay;

        TemptGoal(EntityBee bee, double speed) {
            this.bee = bee;
            this.speed = speed;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            if (this.delay > 0) {
                --this.delay;
                return false;
            }
            this.player = this.bee.world.getClosestPlayerToEntity(this.bee, 10.0D);
            return this.player != null && !this.player.isSpectator()
                    && (BeeFlowers.isFlowerItem(this.player.getHeldItemMainhand()) || BeeFlowers.isFlowerItem(this.player.getHeldItemOffhand()));
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.shouldExecute();
        }

        @Override
        public void resetTask() {
            this.player = null;
            this.bee.getNavigator().clearPath();
            this.delay = 100;
        }

        @Override
        public void updateTask() {
            this.bee.getLookHelper().setLookPositionWithEntity(this.player, this.bee.getHorizontalFaceSpeed() + 20, this.bee.getVerticalFaceSpeed());
            if (this.bee.getDistanceSq(this.player) < 6.25D) {
                this.bee.getNavigator().clearPath();
            } else {
                this.bee.getNavigator().tryMoveToEntityLiving(this.player, this.speed);
            }
        }

        boolean isRunning() {
            return this.player != null;
        }
    }

    /** Hit: angry at the attacker, and every bee nearby that sees it too. */
    static final class AngerGoal extends EntityAIHurtByTarget {
        AngerGoal(EntityBee bee) {
            super(bee, true);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return ((EntityBee) this.taskOwner).isAngry() && super.shouldContinueExecuting();
        }

        @Override
        protected void setEntityAttackTarget(EntityCreature creature, EntityLivingBase target) {
            if (creature instanceof EntityBee && this.taskOwner.canEntityBeSeen(target)) creature.setAttackTarget(target);
        }
    }

    /** An angry bee that hasn't stung goes for the player it's angry at. */
    static final class AttackPlayerGoal extends EntityAINearestAttackableTarget<EntityPlayer> {
        AttackPlayerGoal(EntityBee bee) {
            super(bee, EntityPlayer.class, 10, true, false, player -> player != null && bee.isAngryAt(player));
        }

        @Override
        public boolean shouldExecute() {
            return this.canSting() && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (this.canSting() && this.taskOwner.getAttackTarget() != null) return super.shouldContinueExecuting();
            this.targetEntity = null;
            return false;
        }

        private boolean canSting() {
            EntityBee bee = (EntityBee) this.taskOwner;
            return bee.isAngry() && !bee.hasStung();
        }
    }
}
