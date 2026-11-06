package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.block.sign.IDyedSign;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A dyed sign's text in 1.14's colour (the dye's text colour at 40%); and any standing sign drawn standing (1.12 asks
 * whether the block is its oak standing sign, so Portium's other woods' standing signs were drawn as wall signs).
 */
@Mixin(TileEntitySignRenderer.class)
public abstract class MixinTileEntitySignRendererColor {

    @Unique
    private int portium$textColor;
    @Unique
    private net.minecraft.block.Block portium$block;

    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntitySign;DDDFIF)V", at = @At("HEAD"))
    private void portium$readColor(TileEntitySign sign, double x, double y, double z, float partialTicks, int destroyStage, float alpha,
                                   CallbackInfo ci) {
        this.portium$textColor = sign instanceof IDyedSign ? IDyedSign.renderColor(((IDyedSign) sign).portium$getColor()) : 0;
        this.portium$block = sign.getBlockType();
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "render(Lnet/minecraft/tileentity/TileEntitySign;DDDFIF)V", at = @At(value = "FIELD",
            target = "Lnet/minecraft/init/Blocks;STANDING_SIGN:Lnet/minecraft/block/Block;", opcode = org.objectweb.asm.Opcodes.GETSTATIC))
    private net.minecraft.block.Block portium$standing() {
        return this.portium$block instanceof net.minecraft.block.BlockStandingSign ? this.portium$block : net.minecraft.init.Blocks.STANDING_SIGN;
    }

    @ModifyArg(method = "render(Lnet/minecraft/tileentity/TileEntitySign;DDDFIF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"), index = 3)
    private int portium$color(int color) {
        return color == 0 ? this.portium$textColor : color;
    }
}
