package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.15's sticky honey and water border path types: walking on a honey block, or beside water (a walkable spot with
 * water among the blocks around it, as 1.15's {@code checkNeighborBlocks} finds it), costs a mob 8, so it goes around
 * when it can. 1.12 has neither type, and adding to its enum risks the switches compiled over it; its "danger" type
 * costs the same (bees, which 1.15 has avoid water borders more, set it higher).
 */
@Mixin(WalkNodeProcessor.class)
public abstract class MixinWalkNodeProcessorHoney {

    @Inject(method = "checkNeighborBlocks", at = @At("RETURN"), cancellable = true)
    private void portium$waterBorder(IBlockAccess world, int x, int y, int z, PathNodeType type, CallbackInfoReturnable<PathNodeType> cir) {
        if (cir.getReturnValue() != PathNodeType.WALKABLE) return;
        BlockPos.PooledMutableBlockPos pos = BlockPos.PooledMutableBlockPos.retain();
        try {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) continue;
                        if (world.getBlockState(pos.setPos(x + dx, y + dy, z + dz)).getMaterial() == net.minecraft.block.material.Material.WATER) {
                            cir.setReturnValue(PathNodeType.DANGER_OTHER);
                            return;
                        }
                    }
                }
            }
        } finally {
            pos.release();
        }
    }

    @Inject(method = "getPathNodeType(Lnet/minecraft/world/IBlockAccess;III)Lnet/minecraft/pathfinding/PathNodeType;", at = @At("RETURN"),
            cancellable = true)
    private void portium$stickyHoney(IBlockAccess world, int x, int y, int z, CallbackInfoReturnable<PathNodeType> cir) {
        if (cir.getReturnValue() != PathNodeType.WALKABLE || PortiumBlocks.HONEY_BLOCK == null || y < 1) return;
        if (world.getBlockState(new BlockPos(x, y - 1, z)).getBlock() == PortiumBlocks.HONEY_BLOCK) cir.setReturnValue(PathNodeType.DANGER_OTHER);
    }
}
