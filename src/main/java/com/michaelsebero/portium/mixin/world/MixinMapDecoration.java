package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.item.IMapDecorationName;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.storage.MapDecoration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

/** A map marker's name (1.13: a named banner's), drawn under it. */
@Mixin(MapDecoration.class)
public abstract class MixinMapDecoration implements IMapDecorationName {

    @Unique
    @Nullable
    private ITextComponent portium$name;

    @Override
    @Nullable
    public ITextComponent portium$getName() {
        return this.portium$name;
    }

    @Override
    public void portium$setName(@Nullable ITextComponent name) {
        this.portium$name = name;
    }
}
