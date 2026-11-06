package com.michaelsebero.portium.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.IBrainHolder;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.WorldServer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The nearest dropped item within 9 blocks that the mob wants and can see. */
public class WantedItemsSensor extends Sensor<EntityLiving> {

    @Override
    public Set<MemoryModuleType<?>> getUsedMemories() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
    }

    @Override
    protected void update(WorldServer world, EntityLiving entity) {
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, entity.getEntityBoundingBox().grow(8.0D, 4.0D, 8.0D));
        items.sort(Comparator.comparingDouble(entity::getDistanceSq));
        IBrainHolder holder = (IBrainHolder) entity;
        Optional<EntityItem> wanted = items.stream().filter(item -> holder.wantsToPickUp(item.getItem()))
                .filter(item -> item.getDistanceSq(entity) < 9.0D * 9.0D).filter(entity::canEntityBeSeen).findFirst();
        BrainUtil.brain(entity).setMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, wanted);
    }
}
