package com.michaelsebero.portium.entity.monster;

import com.michaelsebero.portium.entity.ai.EntityAIPatrol;
import com.michaelsebero.portium.entity.raid.IRaider;
import com.michaelsebero.portium.entity.raid.RaiderState;
import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.14's ravager: a raid beast that bites hard, tramples through leaves, and when its bite is blocked by a shield is
 * either stunned for two seconds (then roars, flinging everything near) or flings the shield holder.
 */
public class EntityRavager extends EntityMob implements IRaider, com.michaelsebero.portium.entity.IShieldBlockReaction {

    private static final DataParameter<Boolean> CELEBRATING = EntityDataManager.createKey(EntityRavager.class, DataSerializers.BOOLEAN);
    /** 1.16's attack knockback attribute (1.12 has none): added to enchantment knockback when it bites. */
    private static final float ATTACK_KNOCKBACK = 1.5F;

    private final RaiderState raider = new RaiderState();
    private boolean aggressive;
    private int attackTick;
    private int stunTick;
    private int roarTick;

    public EntityRavager(World world) {
        super(world);
        this.setSize(1.95F, 2.2F);
        this.stepHeight = 1.0F;
        this.experienceValue = 20;
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(4, new EntityAIPatrol(this, 0.7D, 0.595D));
        this.tasks.addTask(4, new EntityAIAttackMelee(this, 1.0D, true) {
            @Override
            protected double getAttackReachSqr(EntityLivingBase target) {
                float reach = EntityRavager.this.width - 0.1F;
                return reach * 2.0F * reach * 2.0F + target.width;
            }
        });
        this.tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.4D));
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 8.0F));
        this.targetTasks.addTask(2, new EntityAIHurtByTarget(this, true) {
            @Override
            protected boolean isSuitableTarget(@Nullable EntityLivingBase target, boolean includeInvincibles) {
                return !(target instanceof IRaider) && super.isSuitableTarget(target, includeInvincibles);
            }
        });
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
        this.targetTasks.addTask(4, new EntityAINearestAttackableTarget<>(this, EntityVillager.class, true));
        this.targetTasks.addTask(4, new EntityAINearestAttackableTarget<>(this, EntityIronGolem.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(100.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.75D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(12.0D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(CELEBRATING, false);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("AttackTick", this.attackTick);
        tag.setInteger("StunTick", this.stunTick);
        tag.setInteger("RoarTick", this.roarTick);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.attackTick = tag.getInteger("AttackTick");
        this.stunTick = tag.getInteger("StunTick");
        this.roarTick = tag.getInteger("RoarTick");
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        return new Navigator(this, world);
    }

    @Override
    public int getHorizontalFaceSpeed() {
        return 45;
    }

    @Override
    public double getMountedYOffset() {
        return 2.1D;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.isEntityAlive()) return;
        if (this.isMovementBlocked()) {
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        } else {
            double target = this.getAttackTarget() != null ? 0.35D : 0.3D;
            double speed = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue();
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(speed + 0.1D * (target - speed));
        }
        if (this.collidedHorizontally && ForgeEventFactory.getMobGriefingEvent(this.world, this)) {
            boolean broke = false;
            AxisAlignedBB box = this.getEntityBoundingBox().grow(0.2D);
            for (BlockPos pos : BlockPos.getAllInBoxMutable(new BlockPos(MathHelper.floor(box.minX), MathHelper.floor(box.minY), MathHelper.floor(box.minZ)),
                    new BlockPos(MathHelper.floor(box.maxX), MathHelper.floor(box.maxY), MathHelper.floor(box.maxZ)))) {
                IBlockState state = this.world.getBlockState(pos);
                Block block = state.getBlock();
                if (block instanceof BlockLeaves) broke = this.world.destroyBlock(pos, true) || broke;
            }
            if (!broke && this.onGround) this.jump();
        }
        if (this.roarTick > 0) {
            --this.roarTick;
            if (this.roarTick == 10) this.roar();
        }
        if (this.attackTick > 0) --this.attackTick;
        if (this.stunTick > 0) {
            --this.stunTick;
            this.stunParticles();
            if (this.stunTick == 0) {
                this.playSound(PortiumSounds.ENTITY_RAVAGER_ROAR, 1.0F, 1.0F);
                this.roarTick = 20;
            }
        }
    }

    private void stunParticles() {
        if (this.rand.nextInt(6) != 0) return;
        double x = this.posX - this.width * Math.sin(this.renderYawOffset * 0.017453292F) + (this.rand.nextDouble() * 0.6D - 0.3D);
        double y = this.posY + this.height - 0.3D;
        double z = this.posZ + this.width * Math.cos(this.renderYawOffset * 0.017453292F) + (this.rand.nextDouble() * 0.6D - 0.3D);
        this.world.spawnParticle(EnumParticleTypes.SPELL_MOB, x, y, z, 0.4980392156862745D, 0.5137254901960784D, 0.5725490196078431D);
    }

    @Override
    protected boolean isMovementBlocked() {
        return super.isMovementBlocked() || this.attackTick > 0 || this.stunTick > 0 || this.roarTick > 0;
    }

    @Override
    public boolean canEntityBeSeen(Entity entity) {
        return this.stunTick <= 0 && this.roarTick <= 0 && super.canEntityBeSeen(entity);
    }

    /** A bite blocked by a shield (1.14's {@code constructKnockBackVector}): stunned half the time, else it flings the holder. */
    public void onBlockedByShield(EntityLivingBase blocker) {
        if (this.roarTick != 0) return;
        if (this.rand.nextDouble() < 0.5D) {
            this.stunTick = 40;
            this.playSound(PortiumSounds.ENTITY_RAVAGER_STUNNED, 1.0F, 1.0F);
            this.world.setEntityState(this, (byte) 39);
            blocker.applyEntityCollision(this);
        } else {
            this.launch(blocker);
        }
        blocker.velocityChanged = true;
    }

    private void roar() {
        if (!this.isEntityAlive()) return;
        List<EntityLivingBase> near = this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.getEntityBoundingBox().grow(4.0D),
                entity -> entity.isEntityAlive() && !(entity instanceof EntityRavager));
        for (EntityLivingBase entity : near) {
            if (!(entity instanceof AbstractIllager)) entity.attackEntityFrom(DamageSource.causeMobDamage(this), 6.0F);
            this.launch(entity);
        }
        AxisAlignedBB box = this.getEntityBoundingBox();
        Vec3d center = new Vec3d((box.minX + box.maxX) / 2.0D, (box.minY + box.maxY) / 2.0D, (box.minZ + box.maxZ) / 2.0D);
        for (int i = 0; i < 40; ++i) {
            this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, center.x, center.y, center.z, this.rand.nextGaussian() * 0.2D,
                    this.rand.nextGaussian() * 0.2D, this.rand.nextGaussian() * 0.2D);
        }
    }

    private void launch(Entity entity) {
        double dx = entity.posX - this.posX;
        double dz = entity.posZ - this.posZ;
        double distanceSq = Math.max(dx * dx + dz * dz, 0.001D);
        entity.addVelocity(dx / distanceSq * 4.0D, 0.2D, dz / distanceSq * 4.0D);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleStatusUpdate(byte id) {
        if (id == 4) {
            this.attackTick = 10;
            this.playSound(PortiumSounds.ENTITY_RAVAGER_ATTACK, 1.0F, 1.0F);
        } else if (id == 39) {
            this.stunTick = 40;
        }
        super.handleStatusUpdate(id);
    }

    public int getAttackTick() {
        return this.attackTick;
    }

    public int getStunTick() {
        return this.stunTick;
    }

    public int getRoarTick() {
        return this.roarTick;
    }

    /** 1.16's mob attack, with the ravager's attack knockback. */
    @Override
    public boolean attackEntityAsMob(Entity target) {
        this.attackTick = 10;
        this.world.setEntityState(this, (byte) 4);
        this.playSound(PortiumSounds.ENTITY_RAVAGER_ATTACK, 1.0F, 1.0F);
        float damage = (float) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        float knockback = ATTACK_KNOCKBACK;
        if (target instanceof EntityLivingBase) {
            damage += EnchantmentHelper.getModifierForCreature(this.getHeldItemMainhand(), ((EntityLivingBase) target).getCreatureAttribute());
            knockback += EnchantmentHelper.getKnockbackModifier(this);
        }
        int fire = EnchantmentHelper.getFireAspectModifier(this);
        if (fire > 0) target.setFire(fire * 4);
        boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
        if (hit) {
            if (knockback > 0.0F && target instanceof EntityLivingBase) {
                ((EntityLivingBase) target).knockBack(this, knockback * 0.5F, MathHelper.sin(this.rotationYaw * 0.017453292F), -MathHelper.cos(this.rotationYaw * 0.017453292F));
                this.motionX *= 0.6D;
                this.motionZ *= 0.6D;
            }
            if (target instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) target;
                ItemStack weapon = this.getHeldItemMainhand();
                ItemStack active = player.isHandActive() ? player.getActiveItemStack() : ItemStack.EMPTY;
                if (!weapon.isEmpty() && !active.isEmpty() && weapon.getItem().canDisableShield(weapon, active, player, this)
                        && active.getItem().isShield(active, player)) {
                    float chance = 0.25F + (float) EnchantmentHelper.getEfficiencyModifier(this) * 0.05F;
                    if (this.rand.nextFloat() < chance) {
                        player.getCooldownTracker().setCooldown(active.getItem(), 100);
                        this.world.setEntityState(player, (byte) 30);
                    }
                }
            }
            this.applyEnchantments(this, target);
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return PortiumSounds.ENTITY_RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        this.playSound(PortiumSounds.ENTITY_RAVAGER_STEP, 0.15F, 1.0F);
    }

    @Override
    public boolean isNotColliding() {
        return !this.world.containsAnyLiquid(this.getEntityBoundingBox());
    }

    @Override
    protected ResourceLocation getLootTable() {
        return PortiumLootTables.RAVAGER;
    }

    @Override
    public RaiderState portium$raider() {
        return this.raider;
    }

    @Override
    public boolean portium$isCelebrating() {
        return this.dataManager.get(CELEBRATING);
    }

    @Override
    public void portium$setCelebrating(boolean celebrating) {
        this.dataManager.set(CELEBRATING, celebrating);
    }

    @Override
    public boolean portium$isAggressive() {
        return this.aggressive;
    }

    @Override
    public void portium$setAggressive(boolean aggressive) {
        this.aggressive = aggressive;
    }

    @Override
    public SoundEvent portium$getCelebrationSound() {
        return PortiumSounds.ENTITY_RAVAGER_CELEBRATE;
    }

    @Override
    public boolean portium$canBeLeader() {
        return false;
    }

    /** Walks through leaves (it tramples them). 1.14 maps its LEAVES node type to open; 1.12 has none, so leaf blocks are checked. */
    static class Navigator extends PathNavigateGround {

        Navigator(EntityLiving entity, World world) {
            super(entity, world);
        }

        @Override
        protected PathFinder getPathFinder() {
            this.nodeProcessor = new WalkNodeProcessor() {
                @Override
                public PathNodeType getPathNodeType(IBlockAccess world, int x, int y, int z) {
                    if (world.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof BlockLeaves) return PathNodeType.OPEN;
                    return super.getPathNodeType(world, x, y, z);
                }
            };
            this.nodeProcessor.setCanEnterDoors(true);
            return new PathFinder(this.nodeProcessor);
        }
    }
}
