package com.michaelsebero.portium.entity.ai.brain;

import com.michaelsebero.portium.entity.ai.brain.memory.BlockPosWrapper;
import com.michaelsebero.portium.entity.ai.brain.memory.EntityPosWrapper;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.memory.WalkTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 1.14's {@code BrainUtil}, plus {@link #brain} to reach an entity's brain. */
public final class BrainUtil {

    /** The brain of an entity run by one; anything else is a programming error. */
    public static Brain<?> brain(EntityLivingBase entity) {
        return ((IBrainHolder) entity).getBrain();
    }

    public static void lookApproachEachOther(EntityLivingBase first, EntityLivingBase second, float speed) {
        lookAt(first, second);
        lookAt(second, first);
        setTargetEntity(first, second, speed, 2);
        setTargetEntity(second, first, speed, 2);
    }

    public static boolean canSee(Brain<?> brain, EntityLivingBase target) {
        return brain.getMemory(MemoryModuleType.VISIBLE_MOBS).filter(mobs -> mobs.contains(target)).isPresent();
    }

    public static void lookAt(EntityLivingBase entity, EntityLivingBase target) {
        brain(entity).setMemory(MemoryModuleType.LOOK_TARGET, new EntityPosWrapper(target, true));
    }

    public static void setTargetEntity(EntityLivingBase entity, Entity target, float speed, int distance) {
        WalkTarget walk = new WalkTarget(new EntityPosWrapper(target, false), speed, distance);
        brain(entity).setMemory(MemoryModuleType.LOOK_TARGET, new EntityPosWrapper(target, true));
        brain(entity).setMemory(MemoryModuleType.WALK_TARGET, walk);
    }

    public static void setTargetPosition(EntityLivingBase entity, BlockPos pos, float speed, int distance) {
        brain(entity).setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosWrapper(pos));
        brain(entity).setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPosWrapper(pos), speed, distance));
    }

    /** Throws an item from the entity's chest height towards a point. */
    public static void spawnItemNearEntity(EntityLivingBase entity, ItemStack stack, Vec3d towards) {
        double y = entity.posY + entity.getEyeHeight() - 0.3D;
        EntityItem item = new EntityItem(entity.world, entity.posX, y, entity.posZ, stack);
        Vec3d motion = towards.subtract(entity.getPositionVector()).normalize().scale(0.3D);
        item.motionX = motion.x;
        item.motionY = motion.y;
        item.motionZ = motion.z;
        item.setDefaultPickupDelay();
        entity.world.spawnEntity(item);
    }

    /**
     * 1.16's {@code canFireAtTarget}: in range of a held crossbow (8 blocks; the only launcher brain mobs fire), or in
     * melee reach.
     */
    public static boolean canFireAtTarget(EntityLiving entity, EntityLivingBase target, int rangeReduction) {
        ItemStack held = entity.getHeldItemMainhand();
        if (isCrossbow(held)) {
            int range = 8 - rangeReduction;
            return entity.getDistanceSq(target) < (double) (range * range);
        }
        return canAttackTarget(entity, target);
    }

    public static boolean isCrossbow(ItemStack stack) {
        return stack.getItem() instanceof com.michaelsebero.portium.item.ItemCrossbow;
    }

    /** 1.16's melee reach: within twice the attacker's width, plus the target's. */
    public static boolean canAttackTarget(EntityLivingBase entity, EntityLivingBase target) {
        double distance = entity.getDistanceSq(target.posX, target.posY, target.posZ);
        double reach = entity.width * 2.0F * entity.width * 2.0F + target.width;
        return distance <= reach;
    }

    /** Whether {@code other} is farther than the current attack target by more than {@code margin}. */
    public static boolean isTargetWithinDistance(EntityLivingBase entity, EntityLivingBase other, double margin) {
        Optional<EntityLivingBase> target = brain(entity).getMemory(MemoryModuleType.ATTACK_TARGET);
        if (!target.isPresent()) return false;
        double current = entity.getDistanceSq(target.get());
        double candidate = entity.getDistanceSq(other);
        return candidate > current + margin * margin;
    }

    public static boolean isMobVisible(EntityLivingBase entity, EntityLivingBase target) {
        Optional<List<EntityLivingBase>> visible = brain(entity).getMemory(MemoryModuleType.VISIBLE_MOBS);
        return visible.isPresent() && visible.get().contains(target);
    }

    public static EntityLivingBase getNearestEntity(EntityLivingBase entity, Optional<EntityLivingBase> current, EntityLivingBase other) {
        return !current.isPresent() ? other : getNearestEntity(entity, current.get(), other);
    }

    public static EntityLivingBase getNearestEntity(EntityLivingBase entity, EntityLivingBase first, EntityLivingBase second) {
        return entity.getDistanceSq(first) < entity.getDistanceSq(second) ? first : second;
    }

    /** The living entity whose UUID the memory holds, if it's loaded in this world. */
    public static Optional<EntityLivingBase> getTargetFromMemory(EntityLivingBase entity, MemoryModuleType<UUID> type) {
        return brain(entity).getMemory(type).map(uuid -> {
            Entity found = ((WorldServer) entity.world).getEntityFromUuid(uuid);
            return found instanceof EntityLivingBase ? (EntityLivingBase) found : null;
        });
    }

    private BrainUtil() {}
}
