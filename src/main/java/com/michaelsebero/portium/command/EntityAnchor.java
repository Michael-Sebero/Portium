package com.michaelsebero.portium.command;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/** Which point of an entity {@code ^} coordinates and {@code facing} are measured from (1.13). */
public enum EntityAnchor {
    FEET("feet"),
    EYES("eyes");

    private final String name;

    EntityAnchor(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Vec3d apply(Vec3d pos, Entity entity) {
        return this == EYES ? new Vec3d(pos.x, pos.y + entity.getEyeHeight(), pos.z) : pos;
    }

    public Vec3d apply(Entity entity) {
        return this.apply(new Vec3d(entity.posX, entity.posY, entity.posZ), entity);
    }

    public static EntityAnchor byName(String name) {
        for (EntityAnchor anchor : values()) {
            if (anchor.name.equals(name)) return anchor;
        }
        return null;
    }
}
