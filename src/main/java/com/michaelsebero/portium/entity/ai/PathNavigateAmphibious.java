package com.michaelsebero.portium.entity.ai;

import com.michaelsebero.portium.entity.passive.EntityTurtle;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The turtle's navigator (1.13): swims and walks, keeping to open water while travelling. */
public class PathNavigateAmphibious extends PathNavigateWater {

    public PathNavigateAmphibious(EntityLiving entity, World world) {
        super(entity, world);
    }

    @Override
    protected PathFinder getPathFinder() {
        this.nodeProcessor = new NodeProcessorAmphibious();
        return new PathFinder(this.nodeProcessor);
    }

    @Override
    protected boolean canNavigate() {
        return true;
    }

    @Override
    public boolean canEntityStandOnPos(BlockPos pos) {
        if (this.entity instanceof EntityTurtle && ((EntityTurtle) this.entity).isTravelling()) {
            return this.world.getBlockState(pos).getBlock() == Blocks.WATER;
        }
        return !this.world.isAirBlock(pos.down());
    }
}
