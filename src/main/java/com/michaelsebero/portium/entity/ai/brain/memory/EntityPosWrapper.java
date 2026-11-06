package com.michaelsebero.portium.entity.ai.brain.memory;

import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;

/** An entity as a look target (at its eyes) or a walk target (at its feet). */
public class EntityPosWrapper implements PosWrapper {

    private final Entity entity;
    private final boolean eyeHeight;

    public EntityPosWrapper(Entity entity, boolean eyeHeight) {
        this.entity = entity;
        this.eyeHeight = eyeHeight;
    }

    @Override
    public Vec3d getPos() {
        return this.eyeHeight ? new Vec3d(this.entity.posX, this.entity.posY + this.entity.getEyeHeight(), this.entity.posZ)
                : new Vec3d(this.entity.posX, this.entity.posY, this.entity.posZ);
    }

    @Override
    public BlockPos getBlockPos() {
        return new BlockPos(this.entity);
    }

    /** A living entity stays a target while alive and seen; anything else always. */
    @Override
    public boolean isVisibleTo(EntityLivingBase viewer) {
        if (!(this.entity instanceof EntityLivingBase)) return true;
        if (!this.entity.isEntityAlive()) return false;
        Optional<List<EntityLivingBase>> visible = BrainUtil.brain(viewer).getMemory(MemoryModuleType.VISIBLE_MOBS);
        return visible.isPresent() && visible.get().contains(this.entity);
    }

    public Entity getEntity() {
        return this.entity;
    }

    @Override
    public String toString() {
        return "EntityTracker for " + this.entity;
    }
}
