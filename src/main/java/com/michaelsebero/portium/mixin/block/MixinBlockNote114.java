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

/** 1.14's note block instruments ({@link NoteInstruments114}), played as 1.12 plays its own. */
@Mixin(BlockNote.class)
public abstract class MixinBlockNote114 {

    @Inject(method = "eventReceived", at = @At("HEAD"), cancellable = true)
    private void portium$play114(IBlockState state, World world, BlockPos pos, int id, int note, CallbackInfoReturnable<Boolean> cir) {
        SoundEvent sound = NoteInstruments114.sound(id);
        if (sound == null) return;
        float pitch = (float) Math.pow(2.0D, (note - 12) / 12.0D);
        world.playSound(null, pos, sound, SoundCategory.RECORDS, 3.0F, pitch);
        world.spawnParticle(EnumParticleTypes.NOTE, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, note / 24.0D, 0.0D, 0.0D);
        cir.setReturnValue(true);
    }
}
