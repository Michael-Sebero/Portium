package com.michaelsebero.portium.tile;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.init.PortiumPotions;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.proxy.ParticleKind;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * The conduit's logic, following 1.13: it activates when surrounded by water and at least 16 prismarine frame blocks,
 * grants Conduit Power to wet players within {@code frame / 7 * 16} blocks, and with a full 42-block frame attacks one
 * wet hostile mob within 8 blocks every two seconds.
 */
public class TileEntityConduit extends TileEntity implements ITickable {

    private static final int FULL_FRAME = 42;
    private static final int MIN_FRAME = 16;

    public int ticksExisted;
    private float activeRotation;
    private boolean active;
    private boolean eyeOpen;
    private final List<BlockPos> frame = new ArrayList<>();
    @Nullable
    private EntityLivingBase target;
    @Nullable
    private UUID targetUuid;
    private long nextAmbientShortTime;

    public static boolean isFrameBlock(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.PRISMARINE || block == Blocks.SEA_LANTERN;
    }

    @Override
    public void update() {
        ++this.ticksExisted;
        long time = this.world.getTotalWorldTime();
        if (time % 40L == 0L) {
            this.setActive(this.checkStructure());
            if (!this.world.isRemote && this.active) {
                this.applyEffects();
                this.attackMobs();
            }
        }
        if (this.active) {
            if (time % 80L == 0L) this.playSound(PortiumSounds.BLOCK_CONDUIT_AMBIENT);
            if (time > this.nextAmbientShortTime) {
                this.nextAmbientShortTime = time + 60L + this.world.rand.nextInt(40);
                this.playSound(PortiumSounds.BLOCK_CONDUIT_AMBIENT_SHORT);
            }
        }
        if (this.world.isRemote) {
            this.updateClientTarget();
            this.spawnParticles();
            if (this.active) ++this.activeRotation;
        }
    }

    /**
     * The conduit needs water in the surrounding 3x3x3 (including its own waterlogged cell), and frame blocks on the
     * three 5x5 rings around it.
     */
    private boolean checkStructure() {
        this.frame.clear();
        for (int x = -1; x <= 1; ++x) {
            for (int y = -1; y <= 1; ++y) {
                for (int z = -1; z <= 1; ++z) {
                    if (!Waterlogging.hasWater(this.world, this.pos.add(x, y, z))) return false;
                }
            }
        }
        for (int x = -2; x <= 2; ++x) {
            for (int y = -2; y <= 2; ++y) {
                for (int z = -2; z <= 2; ++z) {
                    int ax = Math.abs(x);
                    int ay = Math.abs(y);
                    int az = Math.abs(z);
                    boolean outsideCore = ax > 1 || ay > 1 || az > 1;
                    boolean onRing = (x == 0 && (ay == 2 || az == 2)) || (y == 0 && (ax == 2 || az == 2)) || (z == 0 && (ax == 2 || ay == 2));
                    if (outsideCore && onRing) {
                        BlockPos framePos = this.pos.add(x, y, z);
                        if (isFrameBlock(this.world.getBlockState(framePos))) this.frame.add(framePos);
                    }
                }
            }
        }
        this.eyeOpen = this.frame.size() >= FULL_FRAME;
        return this.frame.size() >= MIN_FRAME;
    }

    private void applyEffects() {
        if (!PortiumPotions.isRegistered(PortiumPotions.CONDUIT_POWER)) return;
        int range = this.frame.size() / 7 * 16;
        AxisAlignedBB area = new AxisAlignedBB(this.pos).grow(range).expand(0.0D, this.world.getHeight(), 0.0D);
        for (EntityPlayer player : this.world.getEntitiesWithinAABB(EntityPlayer.class, area)) {
            if (this.pos.distanceSq(new BlockPos(player)) <= (double) (range * range) && player.isWet()) {
                player.addPotionEffect(new PotionEffect(PortiumPotions.CONDUIT_POWER, 260, 0, true, true));
            }
        }
    }

    private AxisAlignedBB attackArea() {
        return new AxisAlignedBB(this.pos).grow(8.0D);
    }

    private void attackMobs() {
        EntityLivingBase previous = this.target;
        if (this.frame.size() < FULL_FRAME) {
            this.target = null;
        } else if (this.target == null && this.targetUuid != null) {
            this.target = this.findTarget(this.targetUuid);
            this.targetUuid = null;
        } else if (this.target == null) {
            List<EntityLivingBase> mobs = this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.attackArea(), e -> e instanceof IMob && e.isWet());
            if (!mobs.isEmpty()) this.target = mobs.get(this.world.rand.nextInt(mobs.size()));
        } else if (!this.target.isEntityAlive() || this.pos.distanceSq(new BlockPos(this.target)) > 64.0D) {
            this.target = null;
        }

        if (this.target != null) {
            this.world.playSound(null, this.target.posX, this.target.posY, this.target.posZ, PortiumSounds.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.BLOCKS, 1.0F, 1.0F);
            this.target.attackEntityFrom(DamageSource.MAGIC, 4.0F);
        }
        if (previous != this.target) {
            IBlockState state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 2);
        }
    }

    @Nullable
    private EntityLivingBase findTarget(UUID uuid) {
        List<EntityLivingBase> found = this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.attackArea(), e -> e.getUniqueID().equals(uuid));
        return found.size() == 1 ? found.get(0) : null;
    }

    private void updateClientTarget() {
        if (this.targetUuid == null) {
            this.target = null;
        } else if (this.target == null || !this.target.getUniqueID().equals(this.targetUuid)) {
            this.target = this.findTarget(this.targetUuid);
            if (this.target == null) this.targetUuid = null;
        }
    }

    private void spawnParticles() {
        Random random = this.world.rand;
        float bob = MathHelper.sin((this.ticksExisted + 35) * 0.1F) / 2.0F + 0.5F;
        bob = (bob * bob + bob) * 0.3F;
        Vec3d center = new Vec3d(this.pos.getX() + 0.5, this.pos.getY() + 1.5 + bob, this.pos.getZ() + 0.5);
        for (BlockPos framePos : this.frame) {
            if (random.nextInt(50) == 0) {
                BlockPos offset = framePos.subtract(this.pos);
                double dx = -0.5 + random.nextFloat() + offset.getX();
                double dy = -2.0 + random.nextFloat() + offset.getY();
                double dz = -0.5 + random.nextFloat() + offset.getZ();
                Portium.proxy.spawnParticle(ParticleKind.NAUTILUS, this.world, center.x, center.y, center.z, dx, dy, dz);
            }
        }
        if (this.target != null) {
            double dx = (-0.5 + random.nextFloat()) * (3.0F + this.target.width);
            double dy = -1.0 + random.nextFloat() * this.target.height;
            double dz = (-0.5 + random.nextFloat()) * (3.0F + this.target.width);
            Portium.proxy.spawnParticle(ParticleKind.NAUTILUS, this.world, this.target.posX, this.target.posY + this.target.getEyeHeight(), this.target.posZ, dx, dy, dz);
        }
    }

    private void setActive(boolean active) {
        if (active != this.active) this.playSound(active ? PortiumSounds.BLOCK_CONDUIT_ACTIVATE : PortiumSounds.BLOCK_CONDUIT_DEACTIVATE);
        this.active = active;
    }

    private void playSound(SoundEvent sound) {
        this.world.playSound(null, this.pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    public boolean isActive() {
        return this.active;
    }

    public boolean isEyeOpen() {
        return this.eyeOpen;
    }

    @SideOnly(Side.CLIENT)
    public float getActiveRotation(float partialTicks) {
        return (this.activeRotation + partialTicks) * -0.0375F;
    }

    // ---- Saving and syncing the attack target ----

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.targetUuid = compound.hasUniqueId("target_uuid") ? compound.getUniqueId("target_uuid") : null;
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (this.target != null) compound.setUniqueId("target_uuid", this.target.getUniqueID());
        return compound;
    }

    @Nonnull
    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 5, this.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        this.readFromNBT(packet.getNbtCompound());
    }

    @SideOnly(Side.CLIENT)
    @Nonnull
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(this.pos).grow(1.0D);
    }
}
