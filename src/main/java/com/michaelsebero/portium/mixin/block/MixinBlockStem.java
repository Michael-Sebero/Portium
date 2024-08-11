package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStem;
import net.minecraft.init.Blocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Pumpkin stems grow, and connect to, the uncarved pumpkin (1.13). The seed lookup still sees vanilla's pumpkin. */
@Mixin(BlockStem.class)
public abstract class MixinBlockStem {

    @Shadow
    @Final
    private Block crop;

    @Redirect(method = {"getActualState", "updateTick"}, at = @At(value = "FIELD", target = "Lnet/minecraft/block/BlockStem;crop:Lnet/minecraft/block/Block;"))
    private Block portium$growUncarvedPumpkin(BlockStem stem) {
        return this.crop == Blocks.PUMPKIN && PortiumConfig.worldGen.uncarvedPumpkins && PortiumBlocks.PUMPKIN != null ? PortiumBlocks.PUMPKIN : this.crop;
    }
}
