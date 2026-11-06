package com.michaelsebero.portium.entity.villager;

import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.schedule.Activity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Predicate;

/**
 * 1.14's iron golem summoning by villagers: five villagers that slept recently and haven't seen a golem summon one
 * while gossiping; three panicking villagers summon one when scared (1.16). Not more than once every five seconds.
 */
public final class VillagerGolems116 {

    static void trySummonFromGossip(EntityVillager116 villager, long gameTime) {
        trySummon(villager, gameTime, 5, other -> other.wantsGolem(gameTime));
    }

    static void trySummonFromPanic(EntityVillager116 villager, long gameTime) {
        trySummon(villager, gameTime, 3, other -> other.getBrain().hasActivity(Activity.PANIC)
                && !other.getBrain().hasMemory(MemoryModuleType.GOLEM_DETECTED_RECENTLY));
    }

    private static void trySummon(EntityVillager116 villager, long gameTime, int needed, Predicate<EntityVillager116> wants) {
        if (gameTime < villager.getLastGolemCheck() + 100L || !wants.test(villager)) return;
        villager.setLastGolemCheck(gameTime);
        World world = villager.world;
        AxisAlignedBB around = villager.getEntityBoundingBox().grow(10.0D, 10.0D, 10.0D);
        List<EntityVillager116> willing = world.getEntitiesWithinAABB(EntityVillager116.class, around, other -> other.isEntityAlive() && wants.test(other));
        if (willing.size() < needed) return;
        if (!world.getEntitiesWithinAABB(EntityIronGolem.class, villager.getEntityBoundingBox().grow(16.0D)).isEmpty()) return;
        EntityIronGolem golem = spawnGolem(villager);
        if (golem == null) return;
        for (EntityVillager116 other : willing) other.getBrain().replaceMemory(MemoryModuleType.GOLEM_DETECTED_RECENTLY, true, 600L);
    }

    /** Ten tries at a spot within 8 blocks with ground below and room for a golem, as 1.14 picks one. */
    private static EntityIronGolem spawnGolem(EntityVillager116 villager) {
        World world = villager.world;
        BlockPos origin = new BlockPos(villager);
        for (int attempt = 0; attempt < 10; attempt++) {
            double dx = world.rand.nextInt(16) - 8;
            double dz = world.rand.nextInt(16) - 8;
            BlockPos pos = findGround(world, origin.add(dx, 6, dz));
            if (pos == null) continue;
            EntityIronGolem golem = new EntityIronGolem(world);
            golem.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, world.rand.nextFloat() * 360.0F, 0.0F);
            if (!world.getCollisionBoxes(golem, golem.getEntityBoundingBox()).isEmpty() || world.containsAnyLiquid(golem.getEntityBoundingBox())) continue;
            golem.onInitialSpawn(world.getDifficultyForLocation(pos), null);
            world.spawnEntity(golem);
            return golem;
        }
        return null;
    }

    /** Down from a position to the first block with a solid top under air. */
    private static BlockPos findGround(World world, BlockPos start) {
        BlockPos pos = start;
        for (int i = 0; i < 12; i++, pos = pos.down()) {
            IBlockState below = world.getBlockState(pos.down());
            if (world.isAirBlock(pos) && below.isSideSolid(world, pos.down(), net.minecraft.util.EnumFacing.UP)) return pos;
        }
        return null;
    }

    private VillagerGolems116() {}
}
