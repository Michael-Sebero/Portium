package com.michaelsebero.portium.entity.ai.brain;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.16's {@code RandomPositionGenerator.getLandPos}: a random spot within reach, moved up out of solid ground (and here
 * down onto it), not in water. 1.12's keeps only spots at a height a mob can already stand on, which on most ground
 * is a few of its ten tries; mobs wandering by it often stand still.
 */
public final class LandPos116 {

    @Nullable
    public static Vec3d find(EntityCreature entity, int maxXZ, int maxY) {
        World world = entity.world;
        Random random = entity.getRNG();
        boolean nearHome = entity.hasHome() && entity.getHomePosition().distanceSq(entity.posX, entity.posY, entity.posZ) + 4.0D
                < Math.pow(entity.getMaximumHomeDistance() + maxXZ, 2);
        BlockPos best = null;
        float bestWeight = -99999.0F;
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos pos = new BlockPos(entity.posX + random.nextInt(2 * maxXZ + 1) - maxXZ, entity.posY + random.nextInt(2 * maxY + 1) - maxY,
                    entity.posZ + random.nextInt(2 * maxXZ + 1) - maxXZ);
            if (pos.getY() < 1 || pos.getY() > world.getHeight() - 2) continue;
            if (nearHome && !entity.isWithinHomeDistanceFromPosition(pos)) continue;
            if (!world.isBlockLoaded(pos)) continue;
            int climbed = 0;
            while (pos.getY() < world.getHeight() - 2 && world.getBlockState(pos).getMaterial().isSolid() && climbed++ <= maxY * 2) pos = pos.up();
            int dropped = 0;
            while (pos.getY() > 1 && !standsOn(world.getBlockState(pos.down())) && dropped++ <= maxY * 2) pos = pos.down();
            if (!standsOn(world.getBlockState(pos.down()))) continue;
            IBlockState at = world.getBlockState(pos);
            if (at.getMaterial().isLiquid() || at.getMaterial().blocksMovement() || world.getBlockState(pos.up()).getMaterial().blocksMovement()) continue;
            float weight = entity.getBlockPathWeight(pos);
            if (weight > bestWeight) {
                bestWeight = weight;
                best = pos;
            }
        }
        return best == null ? null : new Vec3d(best.getX() + 0.5D, best.getY(), best.getZ() + 0.5D);
    }

    private static boolean standsOn(IBlockState state) {
        Material material = state.getMaterial();
        return material.isSolid() && material != Material.LAVA && !(material == Material.CACTUS);
    }

    private LandPos116() {}
}
