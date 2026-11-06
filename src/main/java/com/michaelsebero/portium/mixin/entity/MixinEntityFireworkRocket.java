package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.ICrossbowRocket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Firework rockets as 1.14 flies them: one shot from a crossbow flies straight (no speeding up and rising) and bursts
 * on the first mob or block it hits (a block only if it has stars); any rocket bursts on a mob in its path, keeps its
 * speed against walls, and its burst counts as its shooter's doing.
 */
@Mixin(EntityFireworkRocket.class)
public abstract class MixinEntityFireworkRocket extends Entity implements ICrossbowRocket {

    @Unique
    private static final DataParameter<Boolean> PORTIUM_SHOT_AT_ANGLE = EntityDataManager.createKey(EntityFireworkRocket.class, DataSerializers.BOOLEAN);

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private static DataParameter<net.minecraft.item.ItemStack> FIREWORK_ITEM;
    @Shadow
    private int fireworkAge;
    @Shadow
    private EntityLivingBase boostedEntity;

    @Shadow
    public abstract boolean isAttachedToEntity();

    @Shadow
    protected abstract void dealExplosionDamage();

    @Unique
    @Nullable
    private UUID portium$ownerId;
    @Unique
    @Nullable
    private Entity portium$owner;
    @Unique
    private boolean portium$leftOwner;
    @Unique
    private double portium$motionBeforeX;
    @Unique
    private double portium$motionBeforeY;
    @Unique
    private double portium$motionBeforeZ;

    protected MixinEntityFireworkRocket(World world) {
        super(world);
    }

    @Inject(method = "entityInit", at = @At("TAIL"))
    private void portium$registerAngle(CallbackInfo ci) {
        this.dataManager.register(PORTIUM_SHOT_AT_ANGLE, false);
    }

    @Override
    public void portium$shootFrom(EntityLivingBase shooter) {
        this.portium$owner = shooter;
        this.portium$ownerId = shooter.getUniqueID();
        this.dataManager.set(PORTIUM_SHOT_AT_ANGLE, true);
    }

    @Override
    public boolean portium$isShotAtAngle() {
        return this.dataManager.get(PORTIUM_SHOT_AT_ANGLE);
    }

    @Override
    public void portium$shoot(double x, double y, double z, float velocity, float inaccuracy) {
        Vec3d motion = new Vec3d(x, y, z).normalize().add(this.rand.nextGaussian() * 0.0075D * inaccuracy, this.rand.nextGaussian() * 0.0075D * inaccuracy,
                this.rand.nextGaussian() * 0.0075D * inaccuracy).scale(velocity);
        this.motionX = motion.x;
        this.motionY = motion.y;
        this.motionZ = motion.z;
        float horizontal = MathHelper.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.rotationYaw = (float) (MathHelper.atan2(motion.x, motion.z) * (180.0D / Math.PI));
        this.rotationPitch = (float) (MathHelper.atan2(motion.y, horizontal) * (180.0D / Math.PI));
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;
    }

    @Unique
    @Nullable
    private Entity portium$getOwner() {
        if (this.portium$owner == null && this.portium$ownerId != null && this.world instanceof WorldServer) {
            this.portium$owner = ((WorldServer) this.world).getEntityFromUuid(this.portium$ownerId);
        }
        return this.portium$owner;
    }

    /** Sideways: 1.0 when shot at an angle or against a wall, else 1.15. */
    @ModifyConstant(method = "onUpdate", constant = @Constant(doubleValue = 1.15D))
    private double portium$sidewaysBoost(double boost) {
        return this.portium$isShotAtAngle() || this.collidedHorizontally ? 1.0D : boost;
    }

    @ModifyConstant(method = "onUpdate", constant = @Constant(doubleValue = 0.04D))
    private double portium$rise(double rise) {
        return this.portium$isShotAtAngle() ? 0.0D : rise;
    }

    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EntityFireworkRocket;move(Lnet/minecraft/entity/MoverType;DDD)V"))
    private void portium$rememberMotion(CallbackInfo ci) {
        this.portium$motionBeforeX = this.motionX;
        this.portium$motionBeforeY = this.motionY;
        this.portium$motionBeforeZ = this.motionZ;
    }

    /** After moving: the speed kept (1.14 doesn't let a wall stop it), then what's in its path next. */
    @Inject(method = "onUpdate", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/entity/item/EntityFireworkRocket;move(Lnet/minecraft/entity/MoverType;DDD)V"))
    private void portium$collide(CallbackInfo ci) {
        this.motionX = this.portium$motionBeforeX;
        this.motionY = this.portium$motionBeforeY;
        this.motionZ = this.portium$motionBeforeZ;
        if (this.world.isRemote || this.isDead) return;
        Entity owner = this.portium$getOwner();
        if (!this.portium$leftOwner) {
            this.portium$leftOwner = owner == null || !owner.getEntityBoundingBox().grow(1.0D).intersects(this.getEntityBoundingBox());
        }
        Vec3d start = new Vec3d(this.posX, this.posY, this.posZ);
        Vec3d end = start.add(this.motionX, this.motionY, this.motionZ);
        RayTraceResult blockHit = this.world.rayTraceBlocks(start, end, false, true, false);
        if (blockHit != null && blockHit.typeOfHit != RayTraceResult.Type.MISS) end = blockHit.hitVec;
        Entity hitEntity = null;
        double nearest = Double.MAX_VALUE;
        AxisAlignedBB area = this.getEntityBoundingBox().expand(this.motionX, this.motionY, this.motionZ).grow(1.0D);
        for (Entity entity : this.world.getEntitiesWithinAABBExcludingEntity(this, area)) {
            if (entity instanceof net.minecraft.entity.player.EntityPlayer && ((net.minecraft.entity.player.EntityPlayer) entity).isSpectator()) continue;
            if (!entity.isEntityAlive() || !entity.canBeCollidedWith()) continue;
            if (entity == owner && !this.portium$leftOwner) continue;
            RayTraceResult intercept = entity.getEntityBoundingBox().grow(0.30000001192092896D).calculateIntercept(start, end);
            if (intercept == null) continue;
            double distance = start.squareDistanceTo(intercept.hitVec);
            if (distance < nearest) {
                nearest = distance;
                hitEntity = entity;
            }
        }
        if (hitEntity != null) {
            this.portium$explode();
        } else if (blockHit != null && blockHit.typeOfHit == RayTraceResult.Type.BLOCK && this.portium$hasExplosions()) {
            this.portium$explode();
        }
    }

    @Unique
    private boolean portium$hasExplosions() {
        net.minecraft.item.ItemStack stack = this.dataManager.get(FIREWORK_ITEM);
        NBTTagCompound fireworks = stack.isEmpty() ? null : stack.getSubCompound("Fireworks");
        NBTTagList explosions = fireworks != null ? fireworks.getTagList("Explosions", 10) : null;
        return explosions != null && !explosions.isEmpty();
    }

    @Unique
    private void portium$explode() {
        this.world.setEntityState(this, (byte) 17);
        this.dealExplosionDamage();
        this.setDead();
    }

    /** The burst hurts as its shooter's doing (1.14), so kills count for them. */
    @Redirect(method = "dealExplosionDamage", at = @At(value = "FIELD", target = "Lnet/minecraft/util/DamageSource;FIREWORKS:Lnet/minecraft/util/DamageSource;",
            opcode = Opcodes.GETSTATIC))
    private DamageSource portium$ownedBurst() {
        Entity owner = this.portium$getOwner();
        return owner == null ? DamageSource.FIREWORKS : new EntityDamageSourceIndirect("fireworks", this, owner).setExplosion();
    }

    @Inject(method = "writeEntityToNBT", at = @At("TAIL"))
    private void portium$writeAngle(NBTTagCompound compound, CallbackInfo ci) {
        compound.setBoolean("ShotAtAngle", this.portium$isShotAtAngle());
        if (this.portium$ownerId != null) compound.setUniqueId("Owner", this.portium$ownerId);
    }

    @Inject(method = "readEntityFromNBT", at = @At("TAIL"))
    private void portium$readAngle(NBTTagCompound compound, CallbackInfo ci) {
        this.dataManager.set(PORTIUM_SHOT_AT_ANGLE, compound.getBoolean("ShotAtAngle"));
        if (compound.hasUniqueId("Owner")) this.portium$ownerId = compound.getUniqueId("Owner");
    }
}
