package com.michaelsebero.portium.entity.ai;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** 1.14's {@code MoveToBlockGoal}: walk to a block found nearby, stay a while within reach of it. */
public abstract class EntityAIMoveToBlock116 extends EntityAIBase {

    protected final EntityCreature creature;
    public final double movementSpeed;
    protected int runDelay;
    protected int timeoutCounter;
    private int maxStayTicks;
    protected BlockPos destinationBlock = BlockPos.ORIGIN;
    private boolean isAboveDestination;
    private final int searchLength;
    private final int searchHeight;
    protected int searchStartY;

    public EntityAIMoveToBlock116(EntityCreature creature, double speed, int searchLength, int searchHeight) {
        this.creature = creature;
        this.movementSpeed = speed;
        this.searchLength = searchLength;
        this.searchStartY = 0;
        this.searchHeight = searchHeight;
        this.setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        if (this.runDelay > 0) {
            --this.runDelay;
            return false;
        }
        this.runDelay = this.getRunDelay(this.creature);
        return this.searchForDestination();
    }

    protected int getRunDelay(EntityCreature creature) {
        return 200 + creature.getRNG().nextInt(200);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.timeoutCounter >= -this.maxStayTicks && this.timeoutCounter <= 1200 && this.shouldMoveTo(this.creature.world, this.destinationBlock);
    }

    @Override
    public void startExecuting() {
        this.attemptToMove();
        this.timeoutCounter = 0;
        this.maxStayTicks = this.creature.getRNG().nextInt(this.creature.getRNG().nextInt(1200) + 1200) + 1200;
    }

    protected void attemptToMove() {
        this.creature.getNavigator().tryMoveToXYZ(this.destinationBlock.getX() + 0.5D, this.destinationBlock.getY() + 1, this.destinationBlock.getZ() + 0.5D,
                this.movementSpeed);
    }

    public double getTargetDistanceSq() {
        return 1.0D;
    }

    protected BlockPos target() {
        return this.destinationBlock.up();
    }

    @Override
    public void updateTask() {
        BlockPos target = this.target();
        // 1.14 measures from the block's middle, and squares the "distance squared" once more.
        double dx = target.getX() + 0.5D - this.creature.posX;
        double dy = target.getY() + 0.5D - this.creature.posY;
        double dz = target.getZ() + 0.5D - this.creature.posZ;
        if (dx * dx + dy * dy + dz * dz >= this.getTargetDistanceSq() * this.getTargetDistanceSq()) {
            this.isAboveDestination = false;
            ++this.timeoutCounter;
            if (this.shouldMove()) this.creature.getNavigator().tryMoveToXYZ(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, this.movementSpeed);
        } else {
            this.isAboveDestination = true;
            --this.timeoutCounter;
        }
    }

    public boolean shouldMove() {
        return this.timeoutCounter % 40 == 0;
    }

    protected boolean getIsAboveDestination() {
        return this.isAboveDestination;
    }

    protected boolean searchForDestination() {
        BlockPos origin = new BlockPos(this.creature);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int y = this.searchStartY; y <= this.searchHeight; y = y > 0 ? -y : 1 - y) {
            for (int ring = 0; ring < this.searchLength; ++ring) {
                for (int x = 0; x <= ring; x = x > 0 ? -x : 1 - x) {
                    for (int z = x < ring && x > -ring ? ring : 0; z <= ring; z = z > 0 ? -z : 1 - z) {
                        at.setPos(origin.getX() + x, origin.getY() + y - 1, origin.getZ() + z);
                        if (this.creature.isWithinHomeDistanceFromPosition(at) && this.shouldMoveTo(this.creature.world, at)) {
                            this.destinationBlock = at.toImmutable();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected abstract boolean shouldMoveTo(World world, BlockPos pos);
}
