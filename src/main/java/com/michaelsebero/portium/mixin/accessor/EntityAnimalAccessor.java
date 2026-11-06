package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.entity.passive.EntityAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityAnimal.class)
public interface EntityAnimalAccessor {

    @Accessor("inLove")
    int portium$getInLove();

    @Accessor("inLove")
    void portium$setInLove(int inLove);
}
