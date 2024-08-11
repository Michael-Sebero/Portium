package com.michaelsebero.portium.entity.passive;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.api.IPosedPlayer;
import com.michaelsebero.portium.entity.EntityWaterCreature;
import com.michaelsebero.portium.entity.ai.PathNavigateWater;
import com.michaelsebero.portium.init.PortiumDamageSources;
import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.init.PortiumPotions;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.mixin.accessor.EntityLivingAccessor;
import com.michaelsebero.portium.proxy.ParticleKind;
import com.michaelsebero.portium.world.gen.structure.OceanStructures;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import com.google.common.base.Predicate;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The dolphin (1.13). Dolphins breathe air and dry out on land, swim alongside players (giving them Dolphin's Grace),
 * chase boats, leap out of the water, toss items around, and after being fed fish lead the way to a shipwreck or
 * ocean ruin.
 */
public class EntityDolphin extends EntityWaterCreature {

    private static final DataParameter<BlockPos> TREASURE_POS = EntityDataManager.createKey(EntityDolphin.class, DataSerializers.BLOCK_POS);
    private static final DataParameter<Boolean> GOT_FISH = EntityDataManager.createKey(EntityDolphin.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> MOISTNESS = EntityDataManager.createKey(EntityDolphin.class, DataSerializers.VARINT);
    private static final Predicate<EntityItem> PLAYABLE_ITEM = item -> item != null && !item.cannotPickup() && item.isEntityAlive() && item.isInWater();
    public static final int MAX_AIR = 4800;
    private static final int MAX_MOISTNESS = 2400;

    public EntityDolphin(World world) {
        super(world);
        this.setSize(0.9F, 0.6F);
        this.moveHelper = new MoveHelper(this);
        ((EntityLivingAccessor) this).portium$setLookHelper(new LookHelper(this, 10));
        this.setCanPickUpLoot(true);
        this.setAir(MAX_AIR);
    }

    // ---- State ----

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(TREASURE_POS, BlockPos.ORIGIN);
        this.dataManager.register(GOT_FISH, false);
        this.dataManager.register(MOISTNESS, MAX_MOISTNESS);
    }

    public BlockPos getTreasurePos() {
        return this.dataManager.get(TREASURE_POS);
    }

    public void setTreasurePos(BlockPos pos) {
        this.dataManager.set(TREASURE_POS, pos);
    }

    public boolean hasGotFish() {
        return this.dataManager.get(GOT_FISH);
    }

    public void setGotFish(boolean gotFish) {
        this.dataManager.set(GOT_FISH, gotFish);
    }

    public int getMoistness() {
        return this.dataManager.get(MOISTNESS);
    }

    public void setMoistness(int moistness) {
        this.dataManager.set(MOISTNESS, moistness);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        BlockPos treasure = this.getTreasurePos();
        compound.setInteger("TreasurePosX", treasure.getX());
        compound.setInteger("TreasurePosY", treasure.getY());
        compound.setInteger("TreasurePosZ", treasure.getZ());
        compound.setBoolean("GotFish", this.hasGotFish());
        compound.setInteger("Moistness", this.getMoistness());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        this.setTreasurePos(new BlockPos(compound.getInteger("TreasurePosX"), compound.getInteger("TreasurePosY"), compound.getInteger("TreasurePosZ")));
        super.readEntityFromNBT(compound);
        this.setGotFish(compound.getBoolean("GotFish"));
        this.setMoistness(compound.getInteger("Moistness"));
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        this.setAir(MAX_AIR);
        this.rotationPitch = 0.0F;
        return super.onInitialSpawn(difficulty, data);
    }

    /** Dolphins spawn below sea level but above y=45. */
    @Override
    public boolean getCanSpawnHere() {
        return this.posY > 45.0D && this.posY < this.world.getSeaLevel() && super.getCanSpawnHere();
    }

    // ---- Breathing and drying out ----

    @Override
    public boolean canBreatheUnderwater() {
        return false;
    }

    /** Dolphins hold their breath for four minutes and refill it instantly at the surface. */
    @Override
    protected void updateAir(int air) {
        if (!this.isInsideOfMaterial(Material.WATER)) this.setAir(MAX_AIR);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.isAIDisabled()) {
            this.setAir(MAX_AIR);
            return;
        }
        if (this.isWet()) {
            this.setMoistness(MAX_MOISTNESS);
        } else {
            this.setMoistness(this.getMoistness() - 1);
            if (this.getMoistness() <= 0) this.attackEntityFrom(PortiumDamageSources.DRYOUT, 1.0F);
            if (this.onGround) {
                this.motionY += 0.5D;
                this.motionX += (this.rand.nextFloat() * 2.0F - 1.0F) * 0.2F;
                this.motionZ += (this.rand.nextFloat() * 2.0F - 1.0F) * 0.2F;
                this.rotationYaw = this.rand.nextFloat() * 360.0F;
                this.onGround = false;
                this.isAirBorne = true;
            }
        }
        if (this.world.isRemote && this.isInWater() && this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ > 0.03D) {
            Vec3d look = this.getLook(0.0F);
            float sideX = MathHelper.cos(this.rotationYaw * 0.017453292F) * 0.3F;
            float sideZ = MathHelper.sin(this.rotationYaw * 0.017453292F) * 0.3F;
            float back = 1.2F - this.rand.nextFloat() * 0.7F;
            for (int i = 0; i < 2; ++i) {
                Portium.proxy.spawnParticle(ParticleKind.DOLPHIN, this.world, this.posX - look.x * back + sideX, this.posY - look.y, this.posZ - look.z * back + sideZ, 0.0D, 0.0D, 0.0D);
                Portium.proxy.spawnParticle(ParticleKind.DOLPHIN, this.world, this.posX - look.x * back - sideX, this.posY - look.y, this.posZ - look.z * back - sideZ, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    // ---- AI and movement ----

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new AIBreatheAir(this));
        this.tasks.addTask(0, new AIFindWater(this));
        this.tasks.addTask(1, new AISwimToTreasure(this));
        this.tasks.addTask(2, new AISwimWithPlayer(this, 4.0D));
        this.tasks.addTask(4, new AISwim(this, 1.0D, 10));
        this.tasks.addTask(4, new EntityAILookIdle(this));
        this.tasks.addTask(5, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        this.tasks.addTask(5, new AIJump(this, 10));
        this.tasks.addTask(6, new EntityAIAttackMelee(this, 1.2D, true));
        this.tasks.addTask(8, new AIPlayWithItems(this));
        this.tasks.addTask(8, new AIFollowBoat(this));
        this.tasks.addTask(9, new EntityAIAvoidEntity<>(this, EntityGuardian.class, 8.0F, 1.0D, 1.0D));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, EntityGuardian.class));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(1.2D);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        return new PathNavigateWater(this, world);
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (this.isServerWorld() && this.isInWater()) {
            this.moveRelative(strafe, vertical, forward, this.getAIMoveSpeed());
            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.9D;
            this.motionY *= 0.9D;
            this.motionZ *= 0.9D;
            if (this.getAttackTarget() == null) this.motionY -= 0.005D;
        } else {
            super.travel(strafe, vertical, forward);
        }
    }

    @Override
    public float getEyeHeight() {
        return 0.3F;
    }

    @Override
    public int getVerticalFaceSpeed() {
        return 1;
    }

    @Override
    public int getHorizontalFaceSpeed() {
        return 1;
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return true;
    }

    @Override
    protected boolean canBeRidden(Entity entity) {
        return true;
    }

    private boolean closeToNavigationTarget() {
        net.minecraft.pathfinding.Path path = this.getNavigator().getPath();
        if (path == null || path.getFinalPathPoint() == null) return false;
        net.minecraft.pathfinding.PathPoint end = path.getFinalPathPoint();
        return this.getDistanceSq(end.x, end.y, end.z) < 144.0D;
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this), (int) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
        if (hit) {
            this.applyEnchantments(this, target);
            this.playSound(PortiumSounds.ENTITY_DOLPHIN_ATTACK, 1.0F, 1.0F);
        }
        return hit;
    }

    // ---- Items ----

    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty() || held.getItem() != Items.FISH && held.getItem() != Items.COOKED_FISH) return super.processInteract(player, hand);
        if (!this.world.isRemote) this.playSound(PortiumSounds.ENTITY_DOLPHIN_EAT, 1.0F, 1.0F);
        this.setGotFish(true);
        if (!player.capabilities.isCreativeMode) held.shrink(1);
        return true;
    }

    /** Dolphins only pick things up into their mouth (main hand). */
    @Override
    protected void updateEquipmentIfNeeded(EntityItem item) {
        if (!this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty()) return;
        ItemStack stack = item.getItem();
        this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, stack);
        this.setDropChance(EntityEquipmentSlot.MAINHAND, 2.0F);
        this.onItemPickup(item, stack.getCount());
        item.setDead();
    }

    private void throwItem(ItemStack stack) {
        if (stack.isEmpty()) return;
        EntityItem item = new EntityItem(this.world, this.posX, this.posY - 0.3D + this.getEyeHeight(), this.posZ, stack);
        item.setPickupDelay(40);
        item.setThrower(this.getCachedUniqueIdString());
        float angle = this.rand.nextFloat() * ((float) Math.PI * 2.0F);
        float spread = 0.02F * this.rand.nextFloat();
        item.motionX = 0.3F * -MathHelper.sin(this.rotationYaw * 0.017453292F) * MathHelper.cos(this.rotationPitch * 0.017453292F) + MathHelper.cos(angle) * spread;
        item.motionY = 0.3F * MathHelper.sin(this.rotationPitch * 0.017453292F) * 1.5F;
        item.motionZ = 0.3F * MathHelper.cos(this.rotationYaw * 0.017453292F) * MathHelper.cos(this.rotationPitch * 0.017453292F) + MathHelper.sin(angle) * spread;
        this.world.spawnEntity(item);
    }

    // ---- Effects and sounds ----

    @SideOnly(Side.CLIENT)
    @Override
    public void handleStatusUpdate(byte id) {
        if (id == 38) {
            for (int i = 0; i < 7; ++i) {
                this.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, this.posX + this.rand.nextFloat() * this.width * 2.0F - this.width,
                        this.posY + 0.2D + this.rand.nextFloat() * this.height, this.posZ + this.rand.nextFloat() * this.width * 2.0F - this.width,
                        this.rand.nextGaussian() * 0.01D, this.rand.nextGaussian() * 0.01D, this.rand.nextGaussian() * 0.01D);
            }
        } else {
            super.handleStatusUpdate(id);
        }
    }

    @Override
    protected ResourceLocation getLootTable() {
        return PortiumLootTables.DOLPHIN;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? PortiumSounds.ENTITY_DOLPHIN_AMBIENT_WATER : PortiumSounds.ENTITY_DOLPHIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_DOLPHIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_DOLPHIN_DEATH;
    }

    @Override
    protected SoundEvent getSplashSound() {
        return PortiumSounds.ENTITY_DOLPHIN_SPLASH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return PortiumSounds.ENTITY_DOLPHIN_SWIM;
    }

    // ---- Movement and look control ----

    /** Steers in three dimensions: pitch follows the path so the dolphin dives and surfaces smoothly. */
    static class MoveHelper extends EntityMoveHelper {

        private final EntityDolphin dolphin;

        MoveHelper(EntityDolphin dolphin) {
            super(dolphin);
            this.dolphin = dolphin;
        }

        @Override
        public void onUpdateMoveHelper() {
            if (this.dolphin.isInWater()) this.dolphin.motionY += 0.005D;
            if (this.action != Action.MOVE_TO || this.dolphin.getNavigator().noPath()) {
                this.dolphin.setAIMoveSpeed(0.0F);
                this.dolphin.setMoveStrafing(0.0F);
                this.dolphin.setMoveVertical(0.0F);
                this.dolphin.setMoveForward(0.0F);
                return;
            }
            double dx = this.posX - this.dolphin.posX;
            double dy = this.posY - this.dolphin.posY;
            double dz = this.posZ - this.dolphin.posZ;
            if (dx * dx + dy * dy + dz * dz < 2.5E-7D) {
                this.dolphin.setMoveForward(0.0F);
                return;
            }
            float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            this.dolphin.rotationYaw = this.limitAngle(this.dolphin.rotationYaw, yaw, 10.0F);
            this.dolphin.renderYawOffset = this.dolphin.rotationYaw;
            this.dolphin.rotationYawHead = this.dolphin.rotationYaw;
            float speed = (float) (this.speed * this.dolphin.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
            if (!this.dolphin.isInWater()) {
                this.dolphin.setAIMoveSpeed(speed * 0.1F);
                return;
            }
            this.dolphin.setAIMoveSpeed(speed * 0.02F);
            float pitch = -(float) (MathHelper.atan2(dy, MathHelper.sqrt(dx * dx + dz * dz)) * (180.0D / Math.PI));
            pitch = MathHelper.clamp(MathHelper.wrapDegrees(pitch), -85.0F, 85.0F);
            this.dolphin.rotationPitch = this.limitAngle(this.dolphin.rotationPitch, pitch, 5.0F);
            float cos = MathHelper.cos(this.dolphin.rotationPitch * 0.017453292F);
            float sin = MathHelper.sin(this.dolphin.rotationPitch * 0.017453292F);
            this.dolphin.moveForward = cos * speed;
            this.dolphin.moveVertical = -sin * speed;
        }
    }

    /** Turns the head towards its target but lets the body lag behind, and levels out when idle. */
    static class LookHelper extends EntityLookHelper {

        private final EntityLiving mob;
        private final int maxYawDifference;
        private float yawSpeed;
        private float pitchSpeed;
        private boolean looking;
        private double x;
        private double y;
        private double z;

        LookHelper(EntityLiving mob, int maxYawDifference) {
            super(mob);
            this.mob = mob;
            this.maxYawDifference = maxYawDifference;
        }

        @Override
        public void setLookPositionWithEntity(Entity entity, float yawSpeed, float pitchSpeed) {
            double eyes = entity instanceof net.minecraft.entity.EntityLivingBase ? entity.posY + entity.getEyeHeight()
                    : (entity.getEntityBoundingBox().minY + entity.getEntityBoundingBox().maxY) / 2.0D;
            this.setLookPosition(entity.posX, eyes, entity.posZ, yawSpeed, pitchSpeed);
        }

        @Override
        public void setLookPosition(double x, double y, double z, float yawSpeed, float pitchSpeed) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yawSpeed = yawSpeed;
            this.pitchSpeed = pitchSpeed;
            this.looking = true;
        }

        @Override
        public void onUpdateLook() {
            if (this.looking) {
                this.looking = false;
                double dx = this.x - this.mob.posX;
                double dy = this.y - (this.mob.posY + this.mob.getEyeHeight());
                double dz = this.z - this.mob.posZ;
                float targetYaw = (float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
                float targetPitch = (float) -(MathHelper.atan2(dy, MathHelper.sqrt(dx * dx + dz * dz)) * (180.0D / Math.PI));
                this.mob.rotationYawHead = approach(this.mob.rotationYawHead, targetYaw + 20.0F, this.yawSpeed);
                this.mob.rotationPitch = approach(this.mob.rotationPitch, targetPitch + 10.0F, this.pitchSpeed);
            } else {
                if (this.mob.getNavigator().noPath()) this.mob.rotationPitch = approach(this.mob.rotationPitch, 0.0F, 5.0F);
                this.mob.rotationYawHead = approach(this.mob.rotationYawHead, this.mob.renderYawOffset, this.yawSpeed);
            }
            float difference = MathHelper.wrapDegrees(this.mob.rotationYawHead - this.mob.renderYawOffset);
            if (difference < -this.maxYawDifference) this.mob.renderYawOffset -= 4.0F;
            else if (difference > this.maxYawDifference) this.mob.renderYawOffset += 4.0F;
        }

        private static float approach(float from, float to, float maxStep) {
            return from + MathHelper.clamp(MathHelper.wrapDegrees(to - from), -maxStep, maxStep);
        }

        @Override
        public boolean getIsLooking() {
            return this.looking;
        }

        @Override
        public double getLookPosX() {
            return this.x;
        }

        @Override
        public double getLookPosY() {
            return this.y;
        }

        @Override
        public double getLookPosZ() {
            return this.z;
        }
    }

    // ---- Goals ----

    /** Surfaces when running out of air (under 7 seconds left). */
    static class AIBreatheAir extends EntityAIBase {

        private final EntityDolphin dolphin;

        AIBreatheAir(EntityDolphin dolphin) {
            this.dolphin = dolphin;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            return this.dolphin.getAir() < 140;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.shouldExecute();
        }

        @Override
        public boolean isInterruptible() {
            return false;
        }

        @Override
        public void startExecuting() {
            this.navigateToSurface();
        }

        private void navigateToSurface() {
            BlockPos origin = new BlockPos(this.dolphin);
            BlockPos target = null;
            for (BlockPos.MutableBlockPos pos : BlockPos.getAllInBoxMutable(origin.add(-1, 0, -1), origin.add(1, 8, 1))) {
                if (canBreatheAt(this.dolphin.world, pos)) {
                    target = pos.toImmutable();
                    break;
                }
            }
            if (target == null) target = origin.up(8);
            this.dolphin.getNavigator().tryMoveToXYZ(target.getX(), target.getY() + 1, target.getZ(), 1.0D);
        }

        @Override
        public void updateTask() {
            this.navigateToSurface();
            this.dolphin.moveRelative(this.dolphin.moveStrafing, this.dolphin.moveVertical, this.dolphin.moveForward, 0.02F);
            this.dolphin.move(MoverType.SELF, this.dolphin.motionX, this.dolphin.motionY, this.dolphin.motionZ);
        }

        private static boolean canBreatheAt(World world, BlockPos pos) {
            IBlockState state = world.getBlockState(pos);
            return (!state.getMaterial().isLiquid() || state.getBlock() == com.michaelsebero.portium.init.PortiumBlocks.BUBBLE_COLUMN)
                    && !state.getMaterial().blocksMovement();
        }
    }

    /** Stranded on land, flops towards any water within two blocks. */
    static class AIFindWater extends EntityAIBase {

        private final EntityDolphin dolphin;

        AIFindWater(EntityDolphin dolphin) {
            this.dolphin = dolphin;
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return this.dolphin.onGround && !Waterlogging.hasWater(this.dolphin.world, new BlockPos(this.dolphin));
        }

        @Override
        public void startExecuting() {
            BlockPos origin = new BlockPos(this.dolphin);
            for (BlockPos.MutableBlockPos pos : BlockPos.getAllInBoxMutable(origin.add(-2, -2, -2), origin.add(2, 0, 2))) {
                if (Waterlogging.hasWater(this.dolphin.world, pos)) {
                    this.dolphin.getMoveHelper().setMoveTo(pos.getX(), pos.getY(), pos.getZ(), 1.0D);
                    return;
                }
            }
        }
    }

    /** Swims to random spots in the water. */
    static class AISwim extends net.minecraft.entity.ai.EntityAIWander {

        AISwim(EntityDolphin dolphin, double speed, int chance) {
            super(dolphin, speed, chance);
        }

        @Nullable
        @Override
        protected Vec3d getPosition() {
            for (int attempt = 0; attempt <= 10; attempt++) {
                Vec3d target = RandomPositionGenerator.findRandomTarget(this.entity, 10, 7);
                if (target == null) return null;
                if (Waterlogging.hasWater(this.entity.world, new BlockPos(target))) return target;
            }
            return null;
        }
    }

    /** Keeps pace with a nearby swimming player, granting Dolphin's Grace. */
    static class AISwimWithPlayer extends EntityAIBase {

        private final EntityDolphin dolphin;
        private final double speed;
        @Nullable
        private EntityPlayer player;

        AISwimWithPlayer(EntityDolphin dolphin, double speed) {
            this.dolphin = dolphin;
            this.speed = speed;
            this.setMutexBits(3);
        }

        private static boolean isSwimming(EntityPlayer player) {
            return IPosedPlayer.of(player).portium$isSwimming();
        }

        @Override
        public boolean shouldExecute() {
            this.player = this.dolphin.world.getClosestPlayerToEntity(this.dolphin, 10.0D);
            return this.player != null && !this.player.isSpectator() && isSwimming(this.player) && this.dolphin.getAttackTarget() != this.player;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.player != null && isSwimming(this.player) && this.dolphin.getDistanceSq(this.player) < 256.0D;
        }

        @Override
        public void startExecuting() {
            if (PortiumPotions.isRegistered(PortiumPotions.DOLPHINS_GRACE)) this.player.addPotionEffect(new PotionEffect(PortiumPotions.DOLPHINS_GRACE, 100));
        }

        @Override
        public void resetTask() {
            this.player = null;
            this.dolphin.getNavigator().clearPath();
        }

        @Override
        public void updateTask() {
            this.dolphin.getLookHelper().setLookPositionWithEntity(this.player, this.dolphin.getHorizontalFaceSpeed() + 20, this.dolphin.getVerticalFaceSpeed());
            if (this.dolphin.getDistanceSq(this.player) < 6.25D) this.dolphin.getNavigator().clearPath();
            else this.dolphin.getNavigator().tryMoveToEntityLiving(this.player, this.speed);
            if (isSwimming(this.player) && this.player.world.rand.nextInt(6) == 0 && PortiumPotions.isRegistered(PortiumPotions.DOLPHINS_GRACE)) {
                this.player.addPotionEffect(new PotionEffect(PortiumPotions.DOLPHINS_GRACE, 100));
            }
        }
    }

    /** After being fed, swims towards the nearest shipwreck or ocean ruin while the player follows. */
    static class AISwimToTreasure extends EntityAIBase {

        private final EntityDolphin dolphin;
        private boolean stuck;

        AISwimToTreasure(EntityDolphin dolphin) {
            this.dolphin = dolphin;
            this.setMutexBits(3);
        }

        @Override
        public boolean isInterruptible() {
            return false;
        }

        @Override
        public boolean shouldExecute() {
            return this.dolphin.hasGotFish() && this.dolphin.getAir() >= 100;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.reachedTreasure() && !this.stuck && this.dolphin.getAir() >= 100;
        }

        private boolean reachedTreasure() {
            BlockPos treasure = this.dolphin.getTreasurePos();
            double dx = treasure.getX() - this.dolphin.posX;
            double dz = treasure.getZ() - this.dolphin.posZ;
            return dx * dx + dz * dz < 16.0D;
        }

        @Override
        public void startExecuting() {
            this.stuck = false;
            this.dolphin.getNavigator().clearPath();
            World world = this.dolphin.world;
            BlockPos origin = new BlockPos(this.dolphin);
            boolean ruinFirst = world.rand.nextFloat() >= 0.5F;
            BlockPos target = OceanStructures.findNearest(world, ruinFirst ? OceanStructures.OCEAN_RUIN : OceanStructures.SHIPWRECK, origin, 50);
            if (target == null) target = OceanStructures.findNearest(world, ruinFirst ? OceanStructures.SHIPWRECK : OceanStructures.OCEAN_RUIN, origin, 50);
            if (target == null) {
                this.stuck = true;
                return;
            }
            this.dolphin.setTreasurePos(target);
            world.setEntityState(this.dolphin, (byte) 38);
        }

        @Override
        public void resetTask() {
            if (this.reachedTreasure() || this.stuck) this.dolphin.setGotFish(false);
        }

        @Override
        public void updateTask() {
            World world = this.dolphin.world;
            if (!this.dolphin.closeToNavigationTarget() && !this.dolphin.getNavigator().noPath()) return;
            Vec3d treasure = new Vec3d(this.dolphin.getTreasurePos());
            Vec3d target = RandomPositionGenerator.findRandomTargetBlockTowards(this.dolphin, 16, 1, treasure);
            if (target == null) target = RandomPositionGenerator.findRandomTargetBlockTowards(this.dolphin, 8, 4, treasure);
            if (target != null && !Waterlogging.hasWater(world, new BlockPos(target))) {
                target = RandomPositionGenerator.findRandomTargetBlockTowards(this.dolphin, 8, 5, treasure);
            }
            if (target == null) {
                this.stuck = true;
                return;
            }
            this.dolphin.getLookHelper().setLookPosition(target.x, target.y, target.z, this.dolphin.getHorizontalFaceSpeed() + 20, this.dolphin.getVerticalFaceSpeed());
            this.dolphin.getNavigator().tryMoveToXYZ(target.x, target.y, target.z, 1.3D);
            if (world.rand.nextInt(80) == 0) world.setEntityState(this.dolphin, (byte) 38);
        }
    }

    /** Leaps out of the water in an arc when the way ahead is open water with air above. */
    static class AIJump extends EntityAIBase {

        private static final int[] STEPS = {0, 1, 4, 5, 6, 7};
        private final EntityDolphin dolphin;
        private final int chance;
        private boolean breached;

        AIJump(EntityDolphin dolphin, int chance) {
            this.dolphin = dolphin;
            this.chance = chance;
            this.setMutexBits(5);
        }

        @Override
        public boolean shouldExecute() {
            if (this.dolphin.getRNG().nextInt(this.chance) != 0) return false;
            EnumFacing facing = this.dolphin.getAdjustedHorizontalFacing();
            int dx = facing.getXOffset();
            int dz = facing.getZOffset();
            BlockPos origin = new BlockPos(this.dolphin);
            for (int step : STEPS) {
                if (!this.isWater(origin, dx, dz, step) || !this.isAirAbove(origin, dx, dz, step)) return false;
            }
            return true;
        }

        private boolean isWater(BlockPos origin, int dx, int dz, int step) {
            IBlockState state = this.dolphin.world.getBlockState(origin.add(dx * step, 0, dz * step));
            return state.getMaterial() == Material.WATER && !state.getMaterial().blocksMovement();
        }

        private boolean isAirAbove(BlockPos origin, int dx, int dz, int step) {
            return this.dolphin.world.isAirBlock(origin.add(dx * step, 1, dz * step)) && this.dolphin.world.isAirBlock(origin.add(dx * step, 2, dz * step));
        }

        @Override
        public boolean shouldContinueExecuting() {
            double vertical = this.dolphin.motionY;
            return (vertical * vertical >= 0.03D || this.dolphin.rotationPitch == 0.0F || Math.abs(this.dolphin.rotationPitch) >= 10.0F || !this.dolphin.isInWater())
                    && !this.dolphin.onGround;
        }

        @Override
        public boolean isInterruptible() {
            return false;
        }

        @Override
        public void startExecuting() {
            EnumFacing facing = this.dolphin.getAdjustedHorizontalFacing();
            this.dolphin.motionX += facing.getXOffset() * 0.6D;
            this.dolphin.motionY += 0.7D;
            this.dolphin.motionZ += facing.getZOffset() * 0.6D;
            this.dolphin.getNavigator().clearPath();
        }

        @Override
        public void resetTask() {
            this.dolphin.rotationPitch = 0.0F;
        }

        @Override
        public void updateTask() {
            boolean wasInWater = this.breached;
            if (!wasInWater) this.breached = Waterlogging.hasWater(this.dolphin.world, new BlockPos(this.dolphin));
            if (this.breached && !wasInWater) this.dolphin.playSound(PortiumSounds.ENTITY_DOLPHIN_JUMP, 1.0F, 1.0F);
            double mx = this.dolphin.motionX;
            double my = this.dolphin.motionY;
            double mz = this.dolphin.motionZ;
            if (my * my < 0.03D && this.dolphin.rotationPitch != 0.0F) {
                this.dolphin.rotationPitch = this.dolphin.rotationPitch + MathHelper.wrapDegrees(0.0F - this.dolphin.rotationPitch) * 0.2F;
            } else {
                double horizontal = Math.sqrt(mx * mx + mz * mz);
                double total = Math.sqrt(mx * mx + my * my + mz * mz);
                if (total > 1.0E-7D) this.dolphin.rotationPitch = (float) (Math.signum(-my) * Math.acos(horizontal / total) * (180.0D / Math.PI));
            }
        }
    }

    /** Swims after items in the water, picks them up and flings them about. */
    static class AIPlayWithItems extends EntityAIBase {

        private final EntityDolphin dolphin;
        private int cooldownUntil;

        AIPlayWithItems(EntityDolphin dolphin) {
            this.dolphin = dolphin;
        }

        private List<EntityItem> nearbyItems() {
            return this.dolphin.world.getEntitiesWithinAABB(EntityItem.class, this.dolphin.getEntityBoundingBox().grow(8.0D), PLAYABLE_ITEM);
        }

        @Override
        public boolean shouldExecute() {
            if (this.cooldownUntil > this.dolphin.ticksExisted) return false;
            return !this.nearbyItems().isEmpty() || !this.dolphin.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty();
        }

        @Override
        public void startExecuting() {
            List<EntityItem> items = this.nearbyItems();
            if (!items.isEmpty()) {
                this.dolphin.getNavigator().tryMoveToEntityLiving(items.get(0), 1.2D);
                this.dolphin.playSound(PortiumSounds.ENTITY_DOLPHIN_PLAY, 1.0F, 1.0F);
            }
            this.cooldownUntil = 0;
        }

        @Override
        public void resetTask() {
            ItemStack held = this.dolphin.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
            if (!held.isEmpty()) {
                this.dolphin.throwItem(held);
                this.dolphin.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
                this.cooldownUntil = this.dolphin.ticksExisted + this.dolphin.rand.nextInt(100);
            }
        }

        @Override
        public void updateTask() {
            List<EntityItem> items = this.nearbyItems();
            ItemStack held = this.dolphin.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
            if (!held.isEmpty()) {
                this.dolphin.throwItem(held);
                this.dolphin.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
            } else if (!items.isEmpty()) {
                this.dolphin.getNavigator().tryMoveToEntityLiving(items.get(0), 1.2D);
            }
        }
    }

    /** Races alongside boats that are being rowed. */
    static class AIFollowBoat extends EntityAIBase {

        private final EntityDolphin dolphin;
        private int timer;
        @Nullable
        private EntityPlayer player;
        private boolean goingInBoatDirection;

        AIFollowBoat(EntityDolphin dolphin) {
            this.dolphin = dolphin;
        }

        private static boolean isRowing(EntityPlayer player) {
            return Math.abs(player.moveStrafing) > 0.0F || Math.abs(player.moveForward) > 0.0F;
        }

        @Override
        public boolean shouldExecute() {
            if (this.player != null && isRowing(this.player)) return true;
            for (EntityBoat boat : this.dolphin.world.getEntitiesWithinAABB(EntityBoat.class, this.dolphin.getEntityBoundingBox().grow(5.0D))) {
                Entity driver = boat.getControllingPassenger();
                if (driver instanceof EntityPlayer && isRowing((EntityPlayer) driver)) return true;
            }
            return false;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.player != null && this.player.isRiding() && isRowing(this.player);
        }

        @Override
        public void startExecuting() {
            for (EntityBoat boat : this.dolphin.world.getEntitiesWithinAABB(EntityBoat.class, this.dolphin.getEntityBoundingBox().grow(5.0D))) {
                if (boat.getControllingPassenger() instanceof EntityPlayer) {
                    this.player = (EntityPlayer) boat.getControllingPassenger();
                    break;
                }
            }
            this.timer = 0;
            this.goingInBoatDirection = false;
        }

        @Override
        public void resetTask() {
            this.player = null;
        }

        @Override
        public void updateTask() {
            float push = this.goingInBoatDirection ? (isRowing(this.player) ? 0.18F : 0.0F) : 0.135F;
            this.dolphin.moveRelative(this.dolphin.moveStrafing, this.dolphin.moveVertical, this.dolphin.moveForward, push);
            this.dolphin.move(MoverType.SELF, this.dolphin.motionX, this.dolphin.motionY, this.dolphin.motionZ);
            if (--this.timer > 0) return;
            this.timer = 10;
            if (!this.goingInBoatDirection) {
                BlockPos behind = new BlockPos(this.player).offset(this.player.getHorizontalFacing().getOpposite()).down();
                this.dolphin.getNavigator().tryMoveToXYZ(behind.getX(), behind.getY(), behind.getZ(), 1.0D);
                if (this.dolphin.getDistance(this.player) < 4.0F) {
                    this.timer = 0;
                    this.goingInBoatDirection = true;
                }
            } else {
                BlockPos ahead = new BlockPos(this.player).offset(this.player.getAdjustedHorizontalFacing(), 10);
                this.dolphin.getNavigator().tryMoveToXYZ(ahead.getX(), ahead.getY() - 1, ahead.getZ(), 1.0D);
                if (this.dolphin.getDistance(this.player) > 12.0F) {
                    this.timer = 0;
                    this.goingInBoatDirection = false;
                }
            }
        }
    }
}
