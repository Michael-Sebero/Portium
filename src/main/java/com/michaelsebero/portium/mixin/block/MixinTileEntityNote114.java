package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.block.NoteInstruments114;
import net.minecraft.block.Block;
import net.minecraft.block.BlockNote;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.14's note block instruments: the block below picks one of {@link NoteInstruments114} before 1.12's rules. */
@Mixin(TileEntityNote.class)
public abstract class MixinTileEntityNote114 {

    @Redirect(method = "triggerNote", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;addBlockEvent(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V"))
    private void portium$instrument114(World world, BlockPos pos, Block block, int id, int note) {
        int modern = NoteInstruments114.idFor(world.getBlockState(pos.down()).getBlock());
        world.addBlockEvent(pos, block, modern >= 0 ? modern : id, note);
    }
}
