package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.block.sign.IDyedSign;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A sign's text colour (1.14), saved as 1.14 saves it ({@code Color}: the dye's name) and sent with the sign. */
@Mixin(TileEntitySign.class)
public abstract class MixinTileEntitySignColor implements IDyedSign {

    @Unique
    private EnumDyeColor portium$color;

    @Override
    public EnumDyeColor portium$getColor() {
        // Read lazily: a mixin's field initializer isn't run for every way a block entity is made.
        return this.portium$color == null ? EnumDyeColor.BLACK : this.portium$color;
    }

    @Override
    public boolean portium$setColor(EnumDyeColor color) {
        if (color == this.portium$getColor()) return false;
        this.portium$color = color;
        return true;
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void portium$writeColor(NBTTagCompound compound, CallbackInfoReturnable<NBTTagCompound> cir) {
        cir.getReturnValue().setString("Color", this.portium$getColor().getName());
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void portium$readColor(NBTTagCompound compound, CallbackInfo ci) {
        EnumDyeColor color = EnumDyeColor.BLACK;
        for (EnumDyeColor candidate : EnumDyeColor.values()) {
            if (candidate.getName().equals(compound.getString("Color"))) color = candidate;
        }
        this.portium$color = color;
    }
}
