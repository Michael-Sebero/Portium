package com.michaelsebero.portium.entity.monster;

import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.mixin.accessor.EntityLivingAccessor;
import net.minecraft.entity.EntityBodyHelper;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The phantom (1.13): an undead flyer that haunts players who haven't slept for three days. It circles high above,
 * then swoops down to bite, and burns in daylight.
 */
public class EntityPhantom extends EntityFlying implements IMob {

    private static final DataParameter<Integer> SIZE = EntityDataManager.createKey(EntityPhantom.class, DataSerializers.VARINT);

    private Vec3d orbitOffset = Vec3d.ZERO;
    private BlockPos orbitPosition = BlockPos.ORIGIN;
    private AttackPhase attackPhase = AttackPhase.CIRCLE;

    private enum AttackPhase { CIRCLE, SWOOP }

    public EntityPhantom(World world) {
        super(world);
        this.setSize(0.9F, 0.5F);
        this.experienceValue = 5;
        this.moveHelper = new MoveHelper(this);
        ((EntityLivingAccessor) this).portium$setLookHelper(new LookHelper(this));
    }

    @Override
    protected EntityBodyHelper createBodyHelper() {
        return new BodyHelper(this);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(1, new AIPickAttack());
        this.tasks.addTask(2, new AISweepAttack());
        this.tasks.addTask(3, new AIOrbitPoint());
        this.targetTasks.addTask(1, new AIAttackPlayer());
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(SIZE, 0);
    }

    public int getPhantomSize() {
        return this.dataManager.get(SIZE);
    }

    public void setPhantomSize(int size) {
        this.dataManager.set(SIZE, MathHelper.clamp(size, 0, 64));
        this.updatePhantomSize();
    }

    private void updatePhantomSize() {
        int size = this.getPhantomSize();
        this.setSize(0.9F + 0.2F * size, 0.5F + 0.1F * size);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6 + size);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (SIZE.equals(key)) this.updatePhantomSize();
        super.notifyDataManagerChange(key);
    }

    @Override
    public float getEyeHeight() {
        return this.height * 0.35F;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) {
            float flap = MathHelper.cos((this.getEntityId() * 3 + this.ticksExisted) * 0.13F + (float) Math.PI);
            float nextFlap = MathHelper.cos((this.getEntityId() * 3 + this.ticksExisted + 1) * 0.13F + (float) Math.PI);
            if (flap > 0.0F && nextFlap <= 0.0F) {
                this.world.playSound(this.posX, this.posY, this.posZ, PortiumSounds.ENTITY_PHANTOM_FLAP, this.getSoundCategory(),
                        0.95F + this.rand.nextFloat() * 0.05F, 0.95F + this.rand.nextFloat() * 0.05F, false);
            }
            int size = this.getPhantomSize();
            float wingX = MathHelper.cos(this.rotationYaw * 0.017453292F) * (1.3F + 0.21F * size);
            float wingZ = MathHelper.sin(this.rotationYaw * 0.017453292F) * (1.3F + 0.21F * size);
            float wingY = (0.3F + flap * 0.45F) * (size * 0.2F + 1.0F);
            this.world.spawnParticle(EnumParticleTypes.TOWN_AURA, this.posX + wingX, this.posY + wingY, this.posZ + wingZ, 0.0D, 0.0D, 0.0D);
            this.world.spawnParticle(EnumParticleTypes.TOWN_AURA, this.posX - wingX, this.posY + wingY, this.posZ - wingZ, 0.0D, 0.0D, 0.0D);
        }
        if (!this.world.isRemote && this.world.getDifficulty() == EnumDifficulty.PEACEFUL) this.setDead();
    }

    @Override
    public void onLivingUpdate() {
        if (this.isEntityAlive() && this.isInDaylight()) this.setFire(8);
        super.onLivingUpdate();
    }

    private boolean isInDaylight() {
        if (!this.world.isDaytime() || this.world.isRemote) return false;
        float brightness = this.getBrightness();
        BlockPos pos = new BlockPos(this.posX, Math.round(this.posY), this.posZ);
        return brightness > 0.5F && this.rand.nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F && this.world.canSeeSky(pos);
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        this.orbitPosition = new BlockPos(this).up(5);
        this.setPhantomSize(0);
        return super.onInitialSpawn(difficulty, data);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey("AX")) this.orbitPosition = new BlockPos(compound.getInteger("AX"), compound.getInteger("AY"), compound.getInteger("AZ"));
        this.setPhantomSize(compound.getInteger("Size"));
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("AX", this.orbitPosition.getX());
        compound.setInteger("AY", this.orbitPosition.getY());
        compound.setInteger("AZ", this.orbitPosition.getZ());
        compound.setInteger("Size", this.getPhantomSize());
    }

    @Override
    public boolean attackEntityAsMob(net.minecraft.entity.Entity target) {
        float damage = (float) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
        if (hit) this.applyEnchantments(this, target);
        return hit;
    }

    private boolean canTarget(@Nullable EntityLivingBase target) {
        if (target == null || !target.isEntityAlive()) return false;
        return !(target instanceof EntityPlayer) || !((EntityPlayer) target).isSpectator() && !((EntityPlayer) target).isCreative();
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEAD;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return PortiumSounds.ENTITY_PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_PHANTOM_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 1.0F;
    }

    @Override
    protected ResourceLocation getLootTable() {
        return PortiumLootTables.PHANTOM;
    }

    // ---- Control ----

    private static float approach(float from, float to, float maxStep) {
        maxStep = Math.abs(maxStep);
        return from < to ? MathHelper.clamp(from + maxStep, from, to) : MathHelper.clamp(from - maxStep, to, from);
    }

    private static float approachDegrees(float from, float to, float maxStep) {
        return approach(from, from + MathHelper.wrapDegrees(to - from), maxStep);
    }

    /** Glides towards the orbit offset, picking up speed on straight runs and slowing in turns. */
    class MoveHelper extends EntityMoveHelper {

        private float speedFactor = 0.1F;

        MoveHelper(EntityLiving entity) {
            super(entity);
        }

        @Override
        public void onUpdateMoveHelper() {
            EntityPhantom phantom = EntityPhantom.this;
            if (phantom.collidedHorizontally) {
                phantom.rotationYaw += 180.0F;
                this.speedFactor = 0.1F;
            }
            float dx = (float) (phantom.orbitOffset.x - phantom.posX);
            float dy = (float) (phantom.orbitOffset.y - phantom.posY);
            float dz = (float) (phantom.orbitOffset.z - phantom.posZ);
            double horizontal = MathHelper.sqrt(dx * dx + dz * dz);
            double flatten = 1.0D - MathHelper.abs(dy * 0.7F) / horizontal;
            dx = (float) (dx * flatten);
            dz = (float) (dz * flatten);
            horizontal = MathHelper.sqrt(dx * dx + dz * dz);
            double total = MathHelper.sqrt(dx * dx + dz * dz + dy * dy);
            float oldYaw = phantom.rotationYaw;
            float heading = (float) MathHelper.atan2(dz, dx);
            float yaw = MathHelper.wrapDegrees(phantom.rotationYaw + 90.0F);
            float targetYaw = MathHelper.wrapDegrees(heading * (180.0F / (float) Math.PI));
            phantom.rotationYaw = approachDegrees(yaw, targetYaw, 4.0F) - 90.0F;
            phantom.renderYawOffset = phantom.rotationYaw;
            if (Math.abs(MathHelper.wrapDegrees(phantom.rotationYaw - oldYaw)) < 3.0F) {
                this.speedFactor = approach(this.speedFactor, 1.8F, 0.005F * (1.8F / this.speedFactor));
            } else {
                this.speedFactor = approach(this.speedFactor, 0.2F, 0.025F);
            }
            float pitch = (float) -(MathHelper.atan2(-dy, horizontal) * (180.0D / Math.PI));
            phantom.rotationPitch = pitch;
            float moveYaw = phantom.rotationYaw + 90.0F;
            double mx = this.speedFactor * MathHelper.cos(moveYaw * 0.017453292F) * Math.abs(dx / total);
            double mz = this.speedFactor * MathHelper.sin(moveYaw * 0.017453292F) * Math.abs(dz / total);
            double my = this.speedFactor * MathHelper.sin(pitch * 0.017453292F) * Math.abs(dy / total);
            phantom.motionX += (mx - phantom.motionX) * 0.2D;
            phantom.motionY += (my - phantom.motionY) * 0.2D;
            phantom.motionZ += (mz - phantom.motionZ) * 0.2D;
        }
    }

    /** Phantoms point where they fly; the head never turns on its own. */
    static class LookHelper extends EntityLookHelper {

        LookHelper(EntityLiving entity) {
            super(entity);
        }

        @Override
        public void onUpdateLook() {}
    }

    static class BodyHelper extends EntityBodyHelper {

        private final EntityLiving living;

        BodyHelper(EntityLiving living) {
            super(living);
            this.living = living;
        }

        @Override
        public void updateRenderAngles() {
            this.living.rotationYawHead = this.living.renderYawOffset;
            this.living.renderYawOffset = this.living.rotationYaw;
        }
    }

    // ---- Goals ----

    /** Every 3 seconds, targets the highest player within 64 blocks it can see. */
    class AIAttackPlayer extends EntityAIBase {

        private int tickDelay = 20;

        @Override
        public boolean shouldExecute() {
            if (this.tickDelay > 0) {
                --this.tickDelay;
                return false;
            }
            this.tickDelay = 60;
            EntityPhantom phantom = EntityPhantom.this;
            List<EntityPlayer> players = phantom.world.getEntitiesWithinAABB(EntityPlayer.class, phantom.getEntityBoundingBox().grow(16.0D, 64.0D, 16.0D));
            players.sort((a, b) -> a.posY > b.posY ? -1 : 1);
            for (EntityPlayer player : players) {
                if (phantom.getDistanceSq(player) <= 64.0D * 64.0D && phantom.canTarget(player)) {
                    phantom.setAttackTarget(player);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return EntityPhantom.this.canTarget(EntityPhantom.this.getAttackTarget());
        }
    }

    /** With a target, alternates between circling above it and swooping down. */
    class AIPickAttack extends EntityAIBase {

        private int tickDelay;

        @Override
        public boolean shouldExecute() {
            return EntityPhantom.this.canTarget(EntityPhantom.this.getAttackTarget());
        }

        @Override
        public void startExecuting() {
            this.tickDelay = 10;
            EntityPhantom.this.attackPhase = AttackPhase.CIRCLE;
            this.setAnchorAboveTarget();
        }

        @Override
        public void resetTask() {
            EntityPhantom phantom = EntityPhantom.this;
            phantom.orbitPosition = phantom.world.getHeight(phantom.orbitPosition).up(10 + phantom.rand.nextInt(20));
        }

        @Override
        public void updateTask() {
            EntityPhantom phantom = EntityPhantom.this;
            if (phantom.attackPhase != AttackPhase.CIRCLE) return;
            if (--this.tickDelay > 0) return;
            phantom.attackPhase = AttackPhase.SWOOP;
            this.setAnchorAboveTarget();
            this.tickDelay = (8 + phantom.rand.nextInt(4)) * 20;
            phantom.playSound(PortiumSounds.ENTITY_PHANTOM_SWOOP, 10.0F, 0.95F + phantom.rand.nextFloat() * 0.1F);
        }

        private void setAnchorAboveTarget() {
            EntityPhantom phantom = EntityPhantom.this;
            phantom.orbitPosition = new BlockPos(phantom.getAttackTarget()).up(20 + phantom.rand.nextInt(20));
            if (phantom.orbitPosition.getY() < phantom.world.getSeaLevel()) {
                phantom.orbitPosition = new BlockPos(phantom.orbitPosition.getX(), phantom.world.getSeaLevel() + 1, phantom.orbitPosition.getZ());
            }
        }
    }

    /** Dives at the target and bites, breaking off after hitting it, being hurt, or hitting a wall. */
    class AISweepAttack extends EntityAIBase {

        AISweepAttack() {
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return EntityPhantom.this.getAttackTarget() != null && EntityPhantom.this.attackPhase == AttackPhase.SWOOP;
        }

        @Override
        public boolean shouldContinueExecuting() {
            EntityLivingBase target = EntityPhantom.this.getAttackTarget();
            return EntityPhantom.this.canTarget(target) && this.shouldExecute();
        }

        @Override
        public void resetTask() {
            EntityPhantom.this.setAttackTarget(null);
            EntityPhantom.this.attackPhase = AttackPhase.CIRCLE;
        }

        @Override
        public void updateTask() {
            EntityPhantom phantom = EntityPhantom.this;
            EntityLivingBase target = phantom.getAttackTarget();
            phantom.orbitOffset = new Vec3d(target.posX, target.posY + target.height * 0.5D, target.posZ);
            if (phantom.getEntityBoundingBox().grow(0.2D).intersects(target.getEntityBoundingBox())) {
                phantom.attackEntityAsMob(target);
                phantom.attackPhase = AttackPhase.CIRCLE;
                phantom.world.playSound(null, new BlockPos(phantom), PortiumSounds.ENTITY_PHANTOM_BITE, SoundCategory.HOSTILE, 1.0F, 1.0F);
            } else if (phantom.collidedHorizontally || phantom.hurtTime > 0) {
                phantom.attackPhase = AttackPhase.CIRCLE;
            }
        }
    }

    /** Circles its anchor point, varying radius, height and direction now and then. */
    class AIOrbitPoint extends EntityAIBase {

        private float angle;
        private float distance;
        private float height;
        private float direction;

        AIOrbitPoint() {
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return EntityPhantom.this.getAttackTarget() == null || EntityPhantom.this.attackPhase == AttackPhase.CIRCLE;
        }

        @Override
        public void startExecuting() {
            java.util.Random rand = EntityPhantom.this.rand;
            this.distance = 5.0F + rand.nextFloat() * 10.0F;
            this.height = -4.0F + rand.nextFloat() * 9.0F;
            this.direction = rand.nextBoolean() ? 1.0F : -1.0F;
            this.selectNext();
        }

        @Override
        public void updateTask() {
            EntityPhantom phantom = EntityPhantom.this;
            java.util.Random rand = phantom.rand;
            if (rand.nextInt(350) == 0) this.height = -4.0F + rand.nextFloat() * 9.0F;
            if (rand.nextInt(250) == 0) {
                ++this.distance;
                if (this.distance > 15.0F) {
                    this.distance = 5.0F;
                    this.direction = -this.direction;
                }
            }
            if (rand.nextInt(450) == 0) {
                this.angle = rand.nextFloat() * 2.0F * (float) Math.PI;
                this.selectNext();
            }
            if (phantom.orbitOffset.squareDistanceTo(phantom.posX, phantom.posY, phantom.posZ) < 4.0D) this.selectNext();
            if (phantom.orbitOffset.y < phantom.posY && !phantom.world.isAirBlock(new BlockPos(phantom).down(1))) {
                this.height = Math.max(1.0F, this.height);
                this.selectNext();
            }
            if (phantom.orbitOffset.y > phantom.posY && !phantom.world.isAirBlock(new BlockPos(phantom).up(1))) {
                this.height = Math.min(-1.0F, this.height);
                this.selectNext();
            }
        }

        private void selectNext() {
            EntityPhantom phantom = EntityPhantom.this;
            if (BlockPos.ORIGIN.equals(phantom.orbitPosition)) phantom.orbitPosition = new BlockPos(phantom);
            this.angle += this.direction * 15.0F * 0.017453292F;
            phantom.orbitOffset = new Vec3d(phantom.orbitPosition).add(this.distance * MathHelper.cos(this.angle), -4.0F + this.height,
                    this.distance * MathHelper.sin(this.angle));
        }
    }
}
