package com.michaelsebero.portium.mixin.modern;

import com.michaelsebero.portium.modern.TileEntityFactories;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Saved tile entities of newer mods are remade by their type's factory (see {@link TileEntityFactories}). */
@Mixin(TileEntity.class)
public abstract class MixinTileEntityCreate {

    @Redirect(method = "create", at = @At(value = "INVOKE", target = "Ljava/lang/Class;newInstance()Ljava/lang/Object;"))
    private static Object portium$makeByType(Class<?> tileClass) throws InstantiationException, IllegalAccessException {
        return TileEntityFactories.make(tileClass);
    }
}
