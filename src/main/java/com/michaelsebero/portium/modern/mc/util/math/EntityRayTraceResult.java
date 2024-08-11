package com.michaelsebero.portium.modern.mc.util.math;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/** 1.16's {@code net.minecraft.util.math.EntityRayTraceResult}: an entity hit, as 1.12's ray trace result of an entity. */
public class EntityRayTraceResult extends RayTraceResult {

    public EntityRayTraceResult(Entity entity) {
        super(entity);
    }

    public EntityRayTraceResult(Entity entity, Vec3d hit) {
        super(entity, hit);
    }

    @Srg("func_216348_a")
    public Entity getEntity() {
        return this.entityHit;
    }

    @Srg("func_216346_c")
    public Type getType() {
        return this.typeOfHit;
    }

    @Srg("func_216347_e")
    public Vec3d getHitVec() {
        return this.hitVec;
    }
}
