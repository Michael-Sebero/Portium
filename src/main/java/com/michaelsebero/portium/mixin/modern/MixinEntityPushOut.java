package com.michaelsebero.portium.mixin.modern;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

/**
 * Whether an entity is stuck in blocks, before 1.12 pushes it out, asked with the entity, as 1.16 asks
 * ({@code noCollision(entity)}): blocks whose collision depends on what touches them (Create's crushing wheel
 * controller lets items fall through) otherwise count as solid and push items back up.
 */
@Mixin(Entity.class)
public abstract class MixinEntityPushOut {

    @Redirect(method = "pushOutOfBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;collidesWithAnyBlock(Lnet/minecraft/util/math/AxisAlignedBB;)Z"))
    private boolean portium$collidesAsEntity(World world, AxisAlignedBB box) {
        Entity self = (Entity) (Object) this;
        List<AxisAlignedBB> boxes = new ArrayList<>();
        BlockPos.PooledMutableBlockPos pos = BlockPos.PooledMutableBlockPos.retain();
        try {
            int x0 = MathHelper.floor(box.minX) - 1, x1 = MathHelper.ceil(box.maxX) + 1;
            int y0 = MathHelper.floor(box.minY) - 1, y1 = MathHelper.ceil(box.maxY) + 1;
            int z0 = MathHelper.floor(box.minZ) - 1, z1 = MathHelper.ceil(box.maxZ) + 1;
            for (int x = x0; x < x1; x++) {
                for (int z = z0; z < z1; z++) {
                    for (int y = y0; y < y1; y++) {
                        pos.setPos(x, y, z);
                        if (!world.isBlockLoaded(pos, false)) continue;
                        IBlockState state = world.getBlockState(pos);
                        state.addCollisionBoxToList(world, pos, box, boxes, self, false);
                        if (!boxes.isEmpty()) return true;
                    }
                }
            }
            return false;
        } finally {
            pos.release();
        }
    }
}
