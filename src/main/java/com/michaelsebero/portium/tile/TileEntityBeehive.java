package com.michaelsebero.portium.tile;

import com.michaelsebero.portium.block.bee.BlockBeehive;
import com.michaelsebero.portium.datapack.DataPackManager;
import com.michaelsebero.portium.entity.passive.bee.EntityBee;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.BlockFire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 1.15's beehive and bee nest: holds up to three bees (as their saved data), lets them out by day in fair weather
 * through its front, a bee that brought nectar filling the honey level; fire beside it or a disturbed hive throws
 * them all out.
 */
public class TileEntityBeehive extends TileEntity implements ITickable {

    public static final int MAX_BEES = 3;
    private static final ResourceLocation INHABITORS = new ResourceLocation("beehive_inhabitors");

    private final List<Occupant> bees = new ArrayList<>();
    @Nullable
    private BlockPos flowerPos;

    public enum State {
        HONEY_DELIVERED,
        BEE_RELEASED,
        EMERGENCY
    }

    @Override
    public void markDirty() {
        if (this.world != null && this.isNearFire()) this.angerBees(null, this.world.getBlockState(this.pos), State.EMERGENCY);
        super.markDirty();
    }

    public boolean isNearFire() {
        if (this.world == null) return false;
        for (BlockPos at : BlockPos.getAllInBoxMutable(this.pos.add(-1, -1, -1), this.pos.add(1, 1, 1))) {
            if (this.world.getBlockState(at).getBlock() instanceof BlockFire) return true;
        }
        return false;
    }

    public boolean hasNoBees() {
        return this.bees.isEmpty();
    }

    public boolean isFullOfBees() {
        return this.bees.size() == MAX_BEES;
    }

    public int getBeeCount() {
        return this.bees.size();
    }

    /** Lets every bee out; those near the player go for them, unless a campfire smokes the hive (then they stay out a while). */
    public void angerBees(@Nullable EntityPlayer player, IBlockState state, State reason) {
        List<Entity> released = new ArrayList<>();
        this.bees.removeIf(bee -> this.releaseOccupant(state, bee, released, reason));
        if (player == null) return;
        for (Entity entity : released) {
            if (!(entity instanceof EntityBee)) continue;
            EntityBee bee = (EntityBee) entity;
            if (player.getDistanceSq(bee) <= 16.0D) {
                if (!this.isSmoked()) {
                    bee.setAttackTarget(player);
                } else {
                    bee.setStayOutOfHiveCountdown(400);
                }
            }
        }
    }

    public boolean isSmoked() {
        return this.world != null && BlockBeehive.isSmokingBlockAt(this.world, this.pos);
    }

    public void tryEnterHive(Entity entity, boolean hasNectar) {
        this.tryEnterHive(entity, hasNectar, 0);
    }

    public void tryEnterHive(Entity entity, boolean hasNectar, int ticksInHive) {
        if (this.bees.size() >= MAX_BEES) return;
        entity.dismountRidingEntity();
        entity.removePassengers();
        NBTTagCompound data = new NBTTagCompound();
        entity.writeToNBTOptional(data);
        this.bees.add(new Occupant(data, ticksInHive, hasNectar ? 2400 : 600));
        if (this.world != null) {
            if (entity instanceof EntityBee) {
                EntityBee bee = (EntityBee) entity;
                if (bee.hasFlower() && (this.flowerPos == null || this.world.rand.nextBoolean())) this.flowerPos = bee.getFlowerPos();
            }
            this.world.playSound(null, this.pos.getX(), this.pos.getY(), this.pos.getZ(), PortiumSounds.BLOCK_BEEHIVE_ENTER, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        entity.setDead();
        super.markDirty();
    }

    private boolean releaseOccupant(IBlockState state, Occupant occupant, @Nullable List<Entity> released, State reason) {
        if ((EntityBee.isNight(this.world) || this.world.isRaining()) && reason != State.EMERGENCY) return false;
        NBTTagCompound data = occupant.entityData;
        data.removeTag("Passengers");
        data.removeTag("Leash");
        data.removeTag("UUIDMost");
        data.removeTag("UUIDLeast");
        EnumFacing facing = state.getBlock() instanceof BlockBeehive ? state.getValue(BlockBeehive.FACING) : EnumFacing.SOUTH;
        BlockPos front = this.pos.offset(facing);
        boolean blocked = this.world.getBlockState(front).getCollisionBoundingBox(this.world, front) != null;
        if (blocked && reason != State.EMERGENCY) return false;
        Entity entity = EntityList.createEntityFromNBT(data, this.world);
        if (entity == null) return false;
        if (!isInhabitor(entity)) return false;
        if (entity instanceof EntityBee) {
            EntityBee bee = (EntityBee) entity;
            if (this.flowerPos != null && !bee.hasFlower() && this.world.rand.nextFloat() < 0.9F) bee.setFlowerPos(this.flowerPos);
            if (reason == State.HONEY_DELIVERED) {
                bee.onHoneyDelivered();
                if (state.getBlock() instanceof BlockBeehive) {
                    int honey = state.getValue(BlockBeehive.HONEY_LEVEL);
                    if (honey < 5) {
                        int added = this.world.rand.nextInt(100) == 0 ? 2 : 1;
                        if (honey + added > 5) --added;
                        this.world.setBlockState(this.pos, state.withProperty(BlockBeehive.HONEY_LEVEL, honey + added));
                    }
                }
            }
            ageInHive(occupant.ticksInHive, bee);
            if (released != null) released.add(bee);
        }
        double offset = blocked ? 0.0D : 0.55D + entity.width / 2.0F;
        double x = this.pos.getX() + 0.5D + offset * facing.getXOffset();
        double y = this.pos.getY() + 0.5D - entity.height / 2.0F;
        double z = this.pos.getZ() + 0.5D + offset * facing.getZOffset();
        entity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
        this.world.playSound(null, this.pos, PortiumSounds.BLOCK_BEEHIVE_EXIT, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return this.world.spawnEntity(entity);
    }

    private static boolean isInhabitor(Entity entity) {
        DataPackManager packs = DataPackManager.get();
        if (packs != null && packs.getTags().getEntityTypes().get(INHABITORS) != null) return DataPackManager.entityTypeTagContains(INHABITORS, entity);
        return entity instanceof EntityBee;
    }

    /** Time in the hive counts towards growing up and the breeding cooldown. */
    private static void ageInHive(int ticks, EntityBee bee) {
        int age = bee.getGrowingAge();
        if (age < 0) {
            bee.setGrowingAge(Math.min(0, age + ticks));
        } else if (age > 0) {
            bee.setGrowingAge(Math.max(0, age - ticks));
        }
        bee.shortenLove(ticks);
        bee.resetTicksWithoutNectar();
    }

    @Override
    public void update() {
        if (this.world.isRemote) return;
        IBlockState state = this.world.getBlockState(this.pos);
        for (Iterator<Occupant> it = this.bees.iterator(); it.hasNext(); ) {
            Occupant bee = it.next();
            if (bee.ticksInHive > bee.minOccupationTicks) {
                State reason = bee.entityData.getBoolean("HasNectar") ? State.HONEY_DELIVERED : State.BEE_RELEASED;
                if (this.releaseOccupant(state, bee, null, reason)) {
                    it.remove();
                    super.markDirty();
                    continue;
                }
            }
            ++bee.ticksInHive;
        }
        if (!this.bees.isEmpty() && this.world.rand.nextDouble() < 0.005D) {
            this.world.playSound(null, this.pos.getX() + 0.5D, this.pos.getY(), this.pos.getZ() + 0.5D, PortiumSounds.BLOCK_BEEHIVE_WORK,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.bees.clear();
        NBTTagList list = compound.getTagList("Bees", 10);
        for (int i = 0; i < list.tagCount(); ++i) {
            NBTTagCompound bee = list.getCompoundTagAt(i);
            this.bees.add(new Occupant(bee.getCompoundTag("EntityData"), bee.getInteger("TicksInHive"), bee.getInteger("MinOccupationTicks")));
        }
        this.flowerPos = compound.hasKey("FlowerPos", 10) ? NBTUtil.getPosFromTag(compound.getCompoundTag("FlowerPos")) : null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Bees", this.getBees());
        if (this.flowerPos != null) compound.setTag("FlowerPos", NBTUtil.createPosTag(this.flowerPos));
        return compound;
    }

    public NBTTagList getBees() {
        NBTTagList list = new NBTTagList();
        for (Occupant bee : this.bees) {
            bee.entityData.removeTag("UUIDMost");
            bee.entityData.removeTag("UUIDLeast");
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("EntityData", bee.entityData);
            tag.setInteger("TicksInHive", bee.ticksInHive);
            tag.setInteger("MinOccupationTicks", bee.minOccupationTicks);
            list.appendTag(tag);
        }
        return list;
    }

    @Override
    public boolean shouldRefresh(net.minecraft.world.World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    private static final class Occupant {
        private final NBTTagCompound entityData;
        private int ticksInHive;
        private final int minOccupationTicks;

        private Occupant(NBTTagCompound entityData, int ticksInHive, int minOccupationTicks) {
            entityData.removeTag("UUIDMost");
            entityData.removeTag("UUIDLeast");
            this.entityData = entityData;
            this.ticksInHive = ticksInHive;
            this.minOccupationTicks = minOccupationTicks;
        }
    }
}
