package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.entity.projectile.EntityArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityArrow.class)
public interface EntityArrowAccessor {

    @Accessor("xTile")
    void portium$setXTile(int x);

    @Accessor("yTile")
    void portium$setYTile(int y);

    @Accessor("zTile")
    void portium$setZTile(int z);
}
