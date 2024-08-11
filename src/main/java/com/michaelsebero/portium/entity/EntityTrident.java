package com.michaelsebero.portium.entity;

import com.michaelsebero.portium.advancements.PortiumTriggers;
import com.michaelsebero.portium.init.PortiumEnchantments;
import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.mixin.accessor.EntityArrowAccessor;
import com.michaelsebero.portium.util.AquaticMobs;
import io.netty.buffer.ByteBuf;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A thrown trident (1.13). It deals 8 damage (plus Impaling against aquatic mobs) and drops after hitting a mob
 * instead of disappearing. With Loyalty it flies back to its thrower; with Channeling it calls lightning during storms.
 */
public class EntityTrident extends EntityArrow implements IEntityAdditionalSpawnData {

    private static final DataParameter<Byte> LOYALTY = EntityDataManager.createKey(EntityTrident.class, DataSerializers.BYTE);
    private static final DataParameter<Boolean> FOIL = EntityDataManager.createKey(EntityTrident.class, DataSerializers.BOOLEAN);

    private ItemStack thrownStack = new ItemStack(PortiumItems.TRIDENT);
    private boolean dealtDamage;
    private boolean returning;
    private int returningTicks;

    public EntityTrident(World world) {
        super(world);
    }

    public EntityTrident(World world, EntityLivingBase thrower, ItemStack stack) {
        super(world, thrower);
        this.setStack(stack.copy());
    }

    private void setStack(ItemStack stack) {
        this.thrownStack = stack;
        this.dataManager.set(LOYALTY, (byte) EnchantmentHelper.getEnchantmentLevel(PortiumEnchantments.LOYALTY, stack));
        this.dataManager.set(FOIL, stack.isItemEnchanted());
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(LOYALTY, (byte) 0);
        this.dataManager.register(FOIL, false);
    }

    public boolean hasFoil() {
        return this.dataManager.get(FOIL);
    }

    public boolean isReturning() {
        return this.returning;
    }

    @Override
    public void onUpdate() {
        if (this.timeInGround > 4) this.dealtDamage = true;
        Entity owner = this.shootingEntity;
        int loyalty = this.dataManager.get(LOYALTY);
        if ((this.dealtDamage || this.returning) && owner != null && loyalty > 0) {
            if (!this.shouldReturnTo(owner)) {
                if (!this.world.isRemote && this.pickupStatus == PickupStatus.ALLOWED) this.entityDropItem(this.getArrowStack(), 0.1F);
                this.setDead();
                return;
            }
            if (!this.returning) {
                // Forget the block it was stuck in, or the arrow logic would re-stick it while flying back.
                EntityArrowAccessor arrow = (EntityArrowAccessor) this;
                arrow.portium$setXTile(-1);
                arrow.portium$setYTile(-1);
                arrow.portium$setZTile(-1);
                this.returning = true;
            }
            this.inGround = false;
            Vec3d toOwner = new Vec3d(owner.posX - this.posX, owner.posY + owner.getEyeHeight() - this.posY, owner.posZ - this.posZ);
            this.posY += toOwner.y * 0.015D * loyalty;
            if (this.world.isRemote) this.lastTickPosY = this.posY;
            Vec3d direction = toOwner.normalize();
            double speed = 0.05D * loyalty;
            this.motionX = this.motionX * 0.95D + direction.x * speed;
            this.motionY = this.motionY * 0.95D + direction.y * speed;
            this.motionZ = this.motionZ * 0.95D + direction.z * speed;
            if (this.returningTicks == 0) this.playSound(PortiumSounds.ITEM_TRIDENT_RETURN, 10.0F, 1.0F);
            ++this.returningTicks;
        }
        super.onUpdate();
    }

    private boolean shouldReturnTo(Entity owner) {
        if (!owner.isEntityAlive()) return false;
        return !(owner instanceof EntityPlayerMP) || !((EntityPlayerMP) owner).isSpectator();
    }

    @Nullable
    @Override
    protected Entity findEntityOnPath(Vec3d start, Vec3d end) {
        return this.dealtDamage ? null : super.findEntityOnPath(start, end);
    }

    @Override
    protected void onHit(RayTraceResult result) {
        if (this.returning) return;
        Entity entity = result.entityHit;
        if (entity == null) {
            super.onHit(result);
            return;
        }

        float damage = 8.0F;
        if (entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            damage += EnchantmentHelper.getModifierForCreature(this.thrownStack, living.getCreatureAttribute());
            damage += AquaticMobs.impalingBonus(this.thrownStack, entity);
        }
        Entity owner = this.shootingEntity;
        DamageSource source = new EntityDamageSourceIndirect("trident", this, owner == null ? this : owner).setProjectile();
        this.dealtDamage = true;
        SoundEvent sound = PortiumSounds.ITEM_TRIDENT_HIT;
        if (entity.attackEntityFrom(source, damage) && entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            if (owner instanceof EntityLivingBase) {
                EnchantmentHelper.applyThornEnchantments(living, owner);
                EnchantmentHelper.applyArthropodEnchantments((EntityLivingBase) owner, living);
            }
            this.arrowHit(living);
        }
        this.motionX *= -0.01D;
        this.motionY *= -0.1D;
        this.motionZ *= -0.01D;

        float volume = 1.0F;
        if (this.world.isThundering() && EnchantmentHelper.getEnchantmentLevel(PortiumEnchantments.CHANNELING, this.thrownStack) > 0) {
            BlockPos pos = new BlockPos(entity);
            if (this.world.canSeeSky(pos)) {
                EntityLightningBolt bolt = new EntityLightningBolt(this.world, pos.getX(), pos.getY(), pos.getZ(), false);
                this.world.addWeatherEffect(bolt);
                if (this.shootingEntity instanceof EntityPlayerMP) {
                    // The entities the bolt strikes on its first tick.
                    PortiumTriggers.CHANNELED_LIGHTNING.trigger((EntityPlayerMP) this.shootingEntity, this.world.getEntitiesWithinAABBExcludingEntity(bolt,
                            new AxisAlignedBB(bolt.posX - 3.0D, bolt.posY - 3.0D, bolt.posZ - 3.0D, bolt.posX + 3.0D, bolt.posY + 9.0D, bolt.posZ + 3.0D)));
                }
                sound = PortiumSounds.ITEM_TRIDENT_THUNDER;
                volume = 5.0F;
            }
        }
        this.playSound(sound, volume, 1.0F);
    }

    /** {@link EntityArrow} hard-codes the arrow hit sound for blocks; tridents sound different. */
    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound == SoundEvents.ENTITY_ARROW_HIT ? PortiumSounds.ITEM_TRIDENT_HIT_GROUND : sound, volume, pitch);
    }

    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        Entity owner = this.shootingEntity;
        if (owner != null && !owner.getUniqueID().equals(player.getUniqueID())) return;
        if (this.world.isRemote || !(this.inGround || this.returning) || this.arrowShake > 0) return;

        boolean pickUp = this.pickupStatus == PickupStatus.ALLOWED || (this.pickupStatus == PickupStatus.CREATIVE_ONLY && player.capabilities.isCreativeMode);
        if (this.pickupStatus == PickupStatus.ALLOWED && !player.inventory.addItemStackToInventory(this.getArrowStack())) pickUp = false;
        if (pickUp) {
            player.onItemPickup(this, 1);
            this.setDead();
        }
    }

    @Nonnull
    @Override
    protected ItemStack getArrowStack() {
        return this.thrownStack.copy();
    }

    @Override
    public boolean getIsCritical() {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }

    // ---- Saving and spawn data ----

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setTag("Trident", this.thrownStack.writeToNBT(new NBTTagCompound()));
        compound.setBoolean("DealtDamage", this.dealtDamage);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey("Trident", 10)) this.setStack(new ItemStack(compound.getCompoundTag("Trident")));
        this.dealtDamage = compound.getBoolean("DealtDamage");
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeInt(this.shootingEntity == null ? -1 : this.shootingEntity.getEntityId());
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        int ownerId = buffer.readInt();
        this.shootingEntity = ownerId == -1 ? null : this.world.getEntityByID(ownerId);
    }
}
