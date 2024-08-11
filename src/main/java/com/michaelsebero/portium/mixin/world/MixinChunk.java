package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.world.waterlog.IWaterlogChunk;
import com.michaelsebero.portium.world.waterlog.WaterlogData;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chunks keep their waterlogged cells. A cell loses its water when its block is replaced by a different block, or
 * by one that can't hold water, as replacing a 1.13 block state replaces its water too.
 */
@Mixin(Chunk.class)
public abstract class MixinChunk implements IWaterlogChunk {

    @Shadow @Final private World world;

    @Unique
    private final WaterlogData portium$waterlog = new WaterlogData();

    @Shadow public abstract IBlockState getBlockState(BlockPos pos);

    @Override
    public WaterlogData portium$getWaterlog() {
        return this.portium$waterlog;
    }

    /**
     * The server decides; clients follow its updates, which may arrive before the block itself, so a client never
     * drains a cell on its own.
     */
    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void portium$drainReplacedBlock(BlockPos pos, IBlockState state, CallbackInfoReturnable<IBlockState> cir) {
        if (this.world.isRemote || !this.portium$waterlog.get(pos.getX(), pos.getY(), pos.getZ())) return;
        IBlockState old = this.getBlockState(pos);
        if (state.getBlock() != old.getBlock() || !Waterlogging.canWaterlog(state)) {
            this.portium$waterlog.set(pos.getX(), pos.getY(), pos.getZ(), false);
            Waterlogging.onChanged(this.world, pos, false);
        }
    }

    /**
     * A block that can hold water, put where a still water source was, keeps the water: by world generation, other
     * mods or anything else, as players already do (1.13 places such blocks waterlogged).
     */
    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void portium$keepReplacedWater(BlockPos pos, IBlockState state, CallbackInfoReturnable<IBlockState> cir) {
        if (this.world.isRemote || !Waterlogging.isEnabled() || this.world.provider.doesWaterVaporize()) return;
        IBlockState old = this.getBlockState(pos);
        if (old.getMaterial() != Material.WATER || !(old.getBlock() instanceof BlockLiquid) || old.getValue(BlockLiquid.LEVEL) != 0) return;
        if (!Waterlogging.canWaterlog(state) || !this.portium$waterlog.set(pos.getX(), pos.getY(), pos.getZ(), true)) return;
        Waterlogging.onChanged(this.world, pos, true);
    }
}
