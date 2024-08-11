package com.michaelsebero.portium.entity.ai;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Zombies seek out turtle eggs within 24 blocks and jump on them until they break (1.13's "attack turtle egg" goal).
 */
public class EntityAIBreakTurtleEgg extends EntityAIBase {

    private static final int SEARCH_RANGE = 24;
    private static final int SEARCH_HEIGHT = 3;

    private final EntityCreature mob;
    private final double speed;
    private final Block block = PortiumBlocks.TURTLE_EGG;
    private int runDelay;
    private int timeoutCounter;
    private int breakingTime;
    private boolean aboveDestination;
    private BlockPos destination = BlockPos.ORIGIN;

    public EntityAIBreakTurtleEgg(EntityCreature mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        if (!this.mob.world.getGameRules().getBoolean("mobGriefing")) return false;
        if (this.runDelay > 0) {
            --this.runDelay;
            return false;
        }
        if (this.searchForDestination()) {
            this.runDelay = 20;
            return true;
        }
        this.runDelay = 200 + this.mob.getRNG().nextInt(200);
        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.timeoutCounter >= -1200 && this.timeoutCounter <= 1200 && this.isEgg(this.mob.world, this.destination);
    }

    @Override
    public void startExecuting() {
        this.moveToDestination();
        this.timeoutCounter = 0;
        this.breakingTime = 0;
    }

    @Override
    public void resetTask() {
        this.mob.fallDistance = 1.0F;
    }

    private void moveToDestination() {
        this.mob.getNavigator().tryMoveToXYZ(this.destination.getX() + 0.5D, this.destination.getY() + 1, this.destination.getZ() + 0.5D, this.speed);
    }

    @Override
    public void updateTask() {
        if (this.mob.getDistanceSqToCenter(this.destination.up()) > 1.0D) {
            this.aboveDestination = false;
            ++this.timeoutCounter;
            if (this.timeoutCounter % 40 == 0) this.moveToDestination();
        } else {
            this.aboveDestination = true;
            --this.timeoutCounter;
        }

        World world = this.mob.world;
        BlockPos egg = this.findEggUnderfoot(new BlockPos(this.mob), world);
        Random rand = this.mob.getRNG();
        if (!this.aboveDestination || egg == null) return;
        if (this.breakingTime > 0) {
            this.mob.motionY = 0.3D;
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.ITEM_CRACK, egg.getX() + 0.5D, egg.getY() + 0.7D, egg.getZ() + 0.5D, 3,
                        (rand.nextFloat() - 0.5D) * 0.08D, (rand.nextFloat() - 0.5D) * 0.08D, (rand.nextFloat() - 0.5D) * 0.08D, 0.15D, Item.getIdFromItem(Items.EGG), 0);
            }
        }
        if (this.breakingTime % 2 == 0) {
            this.mob.motionY = -0.3D;
            if (this.breakingTime % 6 == 0) {
                world.playSound(null, this.destination, PortiumSounds.ENTITY_ZOMBIE_DESTROY_EGG, SoundCategory.HOSTILE, 0.5F, 0.9F + rand.nextFloat() * 0.2F);
            }
        }
        if (this.breakingTime > 60) {
            world.setBlockToAir(egg);
            if (world instanceof WorldServer) {
                for (int i = 0; i < 20; ++i) {
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, egg.getX() + 0.5D, egg.getY(), egg.getZ() + 0.5D, 1,
                            rand.nextGaussian() * 0.02D, rand.nextGaussian() * 0.02D, rand.nextGaussian() * 0.02D, 0.15D);
                }
            }
            world.playSound(null, egg, PortiumSounds.ENTITY_TURTLE_EGG_BREAK, SoundCategory.BLOCKS, 0.7F, 0.9F + rand.nextFloat() * 0.2F);
        }
        ++this.breakingTime;
    }

    @Nullable
    private BlockPos findEggUnderfoot(BlockPos pos, World world) {
        if (world.getBlockState(pos).getBlock() == this.block) return pos;
        for (BlockPos near : new BlockPos[] {pos.down(), pos.west(), pos.east(), pos.north(), pos.south(), pos.down().down()}) {
            if (world.getBlockState(near).getBlock() == this.block) return near;
        }
        return null;
    }

    private boolean searchForDestination() {
        BlockPos origin = new BlockPos(this.mob);
        World world = this.mob.world;
        for (int dy = 0; dy <= SEARCH_HEIGHT; dy = dy > 0 ? -dy : 1 - dy) {
            for (int ring = 0; ring < SEARCH_RANGE; ++ring) {
                for (int dx = 0; dx <= ring; dx = dx > 0 ? -dx : 1 - dx) {
                    for (int dz = dx < ring && dx > -ring ? ring : 0; dz <= ring; dz = dz > 0 ? -dz : 1 - dz) {
                        BlockPos pos = origin.add(dx, dy - 1, dz);
                        if (this.mob.isWithinHomeDistanceFromPosition(pos) && this.isEgg(world, pos)) {
                            this.destination = pos;
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** An egg with two blocks of air above it, in a loaded chunk. */
    private boolean isEgg(World world, BlockPos pos) {
        if (!world.isBlockLoaded(pos)) return false;
        Chunk chunk = world.getChunk(pos);
        return chunk.getBlockState(pos).getBlock() == this.block && chunk.getBlockState(pos.up()).getMaterial() == net.minecraft.block.material.Material.AIR
                && chunk.getBlockState(pos.up(2)).getMaterial() == net.minecraft.block.material.Material.AIR;
    }
}
