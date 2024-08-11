package com.michaelsebero.portium.entity.fish;

import com.michaelsebero.portium.advancements.PortiumTriggers;
import com.michaelsebero.portium.entity.EntityWaterCreature;
import com.michaelsebero.portium.entity.ai.PathNavigateWater;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.13 fish: they swim about, flee players, flop around on land, and can be scooped up with a water bucket. Fish that
 * came out of a bucket or have a name never despawn.
 */
public abstract class EntityAbstractFish extends EntityWaterCreature {

    private static final DataParameter<Boolean> FROM_BUCKET = EntityDataManager.createKey(EntityAbstractFish.class, DataSerializers.BOOLEAN);

    protected EntityAbstractFish(World world) {
        super(world);
        this.moveHelper = new MoveHelper(this);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAIPanic(this, 1.25D));
        this.tasks.addTask(2, new EntityAIAvoidEntity<>(this, EntityPlayer.class, EntitySelectors.NOT_SPECTATING, 8.0F, 1.6D, 1.4D));
        this.tasks.addTask(4, new AISwim(this));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(3.0D);
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        return new PathNavigateWater(this, world);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(FROM_BUCKET, false);
    }

    public boolean isFromBucket() {
        return this.dataManager.get(FROM_BUCKET);
    }

    public void setFromBucket(boolean fromBucket) {
        this.dataManager.set(FROM_BUCKET, fromBucket);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setBoolean("FromBucket", this.isFromBucket());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setFromBucket(compound.getBoolean("FromBucket"));
    }

    @Override
    public float getEyeHeight() {
        return this.height * 0.65F;
    }

    @Override
    protected boolean canDespawn() {
        return !this.isFromBucket() && !this.hasCustomName();
    }

    @Override
    public int getMaxSpawnedInChunk() {
        return 8;
    }

    /** Fish only spawn in open water, two blocks deep. */
    @Override
    public boolean getCanSpawnHere() {
        BlockPos pos = new BlockPos(this.posX, this.getEntityBoundingBox().minY, this.posZ);
        return isWaterBlock(this.world.getBlockState(pos)) && isWaterBlock(this.world.getBlockState(pos.up()));
    }

    private static boolean isWaterBlock(IBlockState state) {
        return state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER;
    }

    /** Whether the fish swims about on its own; schooling fish follow their leader instead. */
    protected boolean canRandomSwim() {
        return true;
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (this.isServerWorld() && this.isInWater()) {
            this.moveRelative(strafe, vertical, forward, 0.01F);
            this.move(net.minecraft.entity.MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.9D;
            this.motionY *= 0.9D;
            this.motionZ *= 0.9D;
            if (this.getAttackTarget() == null) this.motionY -= 0.005D;
        } else {
            super.travel(strafe, vertical, forward);
        }
    }

    @Override
    public void onLivingUpdate() {
        if (!this.isInWater() && this.onGround && this.collidedVertically) {
            this.motionY += 0.4D;
            this.motionX += (this.rand.nextFloat() * 2.0F - 1.0F) * 0.05F;
            this.motionZ += (this.rand.nextFloat() * 2.0F - 1.0F) * 0.05F;
            this.onGround = false;
            this.isAirBorne = true;
            this.playSound(this.getFlopSound(), this.getSoundVolume(), this.getSoundPitch());
        }
        super.onLivingUpdate();
    }

    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() != Items.WATER_BUCKET || !this.isEntityAlive()) return super.processInteract(player, hand);
        // Fish buckets can be switched off.
        if (this.getBucket().isEmpty()) return super.processInteract(player, hand);
        this.playSound(PortiumSounds.ITEM_BUCKET_FILL_FISH, 1.0F, 1.0F);
        held.shrink(1);
        ItemStack bucket = this.getBucket();
        this.setBucketData(bucket);
        if (player instanceof EntityPlayerMP) PortiumTriggers.FILLED_BUCKET.trigger((EntityPlayerMP) player, bucket);
        if (held.isEmpty()) {
            player.setHeldItem(hand, bucket);
        } else if (!player.inventory.addItemStackToInventory(bucket)) {
            player.dropItem(bucket, false);
        }
        this.setDead();
        return true;
    }

    /** Copies anything worth keeping onto the bucket the fish is scooped into. */
    protected void setBucketData(ItemStack bucket) {
        if (this.hasCustomName()) bucket.setStackDisplayName(this.getCustomNameTag());
    }

    protected abstract ItemStack getBucket();

    protected abstract SoundEvent getFlopSound();

    @Override
    protected SoundEvent getSwimSound() {
        return PortiumSounds.ENTITY_FISH_SWIM;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {}

    /** Swims towards the path target, adding a little buoyancy while submerged. */
    static class MoveHelper extends EntityMoveHelper {

        private final EntityAbstractFish fish;

        MoveHelper(EntityAbstractFish fish) {
            super(fish);
            this.fish = fish;
        }

        @Override
        public void onUpdateMoveHelper() {
            if (this.fish.isInsideOfMaterial(Material.WATER)) this.fish.motionY += 0.005D;
            if (this.action != Action.MOVE_TO || this.fish.getNavigator().noPath()) {
                this.fish.setAIMoveSpeed(0.0F);
                return;
            }
            double dx = this.posX - this.fish.posX;
            double dy = this.posY - this.fish.posY;
            double dz = this.posZ - this.fish.posZ;
            double distance = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 1.0E-7D) return;
            float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            this.fish.rotationYaw = this.limitAngle(this.fish.rotationYaw, yaw, 90.0F);
            this.fish.renderYawOffset = this.fish.rotationYaw;
            float speed = (float) (this.speed * this.fish.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
            this.fish.setAIMoveSpeed(this.fish.getAIMoveSpeed() + (speed - this.fish.getAIMoveSpeed()) * 0.125F);
            this.fish.motionY += this.fish.getAIMoveSpeed() * (dy / distance) * 0.1D;
        }
    }

    /** Wanders to random spots in the water. */
    static class AISwim extends EntityAIWander {

        private final EntityAbstractFish fish;

        AISwim(EntityAbstractFish fish) {
            super(fish, 1.0D, 40);
            this.fish = fish;
        }

        @Override
        public boolean shouldExecute() {
            return this.fish.canRandomSwim() && super.shouldExecute();
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
}
