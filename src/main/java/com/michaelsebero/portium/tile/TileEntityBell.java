package com.michaelsebero.portium.tile;

import com.michaelsebero.portium.entity.HeardBell;
import com.michaelsebero.portium.entity.Raiders;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;

/**
 * A bell's swing (1.14), sent to clients as a block event: 50 ticks, away from the side it was struck on. Mobs within
 * 32 blocks hear it ({@link HeardBell}); if it's still swinging 5 ticks on with raiders within 32 blocks, it resonates,
 * and 2 seconds later raiders within 48 glow for 3 seconds, with a trail of particles pointing to each.
 */
public class TileEntityBell extends TileEntity implements ITickable {

    public static final int RING_EVENT = 1;

    private long ringTime;
    public int ringingTicks;
    public boolean isRinging;
    public EnumFacing ringDirection = EnumFacing.NORTH;
    private List<EntityLivingBase> entitiesAtRing;
    private boolean shouldReveal;
    private int revealWarmup;

    @Override
    public boolean receiveClientEvent(int id, int type) {
        if (id == RING_EVENT) {
            this.onRing();
            this.revealWarmup = 0;
            this.ringDirection = EnumFacing.byIndex(type);
            this.ringingTicks = 0;
            this.isRinging = true;
            return true;
        }
        return super.receiveClientEvent(id, type);
    }

    @Override
    public void update() {
        if (this.isRinging) ++this.ringingTicks;
        if (this.ringingTicks >= 50) {
            this.isRinging = false;
            this.ringingTicks = 0;
        }
        if (this.ringingTicks >= 5 && this.revealWarmup == 0 && this.hasRaidersNearby()) {
            this.shouldReveal = true;
            this.world.playSound(null, this.pos, PortiumSounds.BLOCK_BELL_RESONATE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        if (this.shouldReveal) {
            if (this.revealWarmup < 40) {
                ++this.revealWarmup;
            } else {
                this.glowRaiders();
                this.addRaiderParticles();
                this.shouldReveal = false;
            }
        }
    }

    /** Swings it (server): the swing and what it does play out on each side from the block event. */
    public void ring(EnumFacing direction) {
        this.ringDirection = direction;
        if (this.isRinging) this.ringingTicks = 0;
        else this.isRinging = true;
        this.world.addBlockEvent(this.pos, this.getBlockType(), RING_EVENT, direction.getIndex());
    }

    private void onRing() {
        BlockPos pos = this.pos;
        if (this.world.getTotalWorldTime() > this.ringTime + 60L || this.entitiesAtRing == null) {
            this.ringTime = this.world.getTotalWorldTime();
            this.entitiesAtRing = this.world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(pos).grow(48.0D));
        }
        if (!this.world.isRemote) {
            for (EntityLivingBase entity : this.entitiesAtRing) {
                if (entity.isEntityAlive() && within(pos, entity, 32.0D)) HeardBell.heard(entity, this.world.getTotalWorldTime());
            }
        }
    }

    private boolean hasRaidersNearby() {
        if (this.entitiesAtRing == null) return false;
        for (EntityLivingBase entity : this.entitiesAtRing) {
            if (entity.isEntityAlive() && within(this.pos, entity, 32.0D) && Raiders.isRaider(entity)) return true;
        }
        return false;
    }

    private boolean isNearbyRaider(EntityLivingBase entity) {
        return entity.isEntityAlive() && within(this.pos, entity, 48.0D) && Raiders.isRaider(entity);
    }

    private void glowRaiders() {
        if (this.world.isRemote) return;
        for (EntityLivingBase entity : this.entitiesAtRing) {
            if (this.isNearbyRaider(entity)) entity.addPotionEffect(new PotionEffect(MobEffects.GLOWING, 60));
        }
    }

    /** A line of spell particles from the bell toward each raider, more of them the fewer mobs are around. */
    private void addRaiderParticles() {
        if (!this.world.isRemote) return;
        World world = this.world;
        BlockPos pos = this.pos;
        int nearby = 0;
        for (EntityLivingBase entity : this.entitiesAtRing) {
            if (within(pos, entity, 48.0D)) nearby++;
        }
        int count = MathHelper.clamp((nearby - 21) / -2, 3, 15);
        int colour = 16700985;
        for (EntityLivingBase entity : this.entitiesAtRing) {
            if (!this.isNearbyRaider(entity)) continue;
            double dx = entity.posX - pos.getX();
            double dz = entity.posZ - pos.getZ();
            float distance = MathHelper.sqrt(dx * dx + dz * dz);
            double x = (double) ((float) pos.getX() + 0.5F) + (double) (1.0F / distance) * dx;
            double z = (double) ((float) pos.getZ() + 0.5F) + (double) (1.0F / distance) * dz;
            for (int i = 0; i < count; i++) {
                colour += 5;
                world.spawnParticle(EnumParticleTypes.SPELL_MOB, x, (double) ((float) pos.getY() + 0.5F), z,
                        (double) (colour >> 16 & 255) / 255.0D, (double) (colour >> 8 & 255) / 255.0D, (double) (colour & 255) / 255.0D);
            }
        }
    }

    /** 1.14's {@code BlockPos.withinDistance} to an entity's feet, from the block's middle. */
    private static boolean within(BlockPos pos, EntityLivingBase entity, double distance) {
        double dx = pos.getX() + 0.5D - entity.posX;
        double dy = pos.getY() + 0.5D - entity.posY;
        double dz = pos.getZ() + 0.5D - entity.posZ;
        return dx * dx + dy * dy + dz * dz < distance * distance;
    }

    /** Kept through the block's own state changes (Forge replaces a mod's tile entity on any state change). */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, net.minecraft.block.state.IBlockState oldState, net.minecraft.block.state.IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    /** For Portium's dev scripts. */
    public String describe() {
        int raiders = 0;
        if (this.entitiesAtRing != null) {
            for (EntityLivingBase entity : this.entitiesAtRing) if (Raiders.isRaider(entity)) raiders++;
        }
        return (this.isRinging ? "ringing " + this.ringingTicks + " toward " + this.ringDirection : "still") + ", heard by "
                + (this.entitiesAtRing == null ? "-" : this.entitiesAtRing.size()) + " (" + raiders + " raiders), reveal " + this.shouldReveal
                + " warmup " + this.revealWarmup;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(this.pos);
    }
}
