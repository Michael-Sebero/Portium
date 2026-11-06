package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.entity.ILavaWalker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.16's {@code LAVA_COLLISION_SHAPE}: to an entity that can stand on lava, a still lava block with no lava above is
 * solid up to half a block, once the entity is above that.
 */
@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquidLavaWalk extends Block {

    private static final AxisAlignedBB PORTIUM_LAVA_SURFACE = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);

    private MixinBlockLiquidLavaWalk(Material material) {
        super(material);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
                                      @Nullable Entity entity, boolean isActualState) {
        if (entity instanceof ILavaWalker && this.material == Material.LAVA && state.getValue(BlockLiquid.LEVEL) == 0
                && world.getBlockState(pos.up()).getMaterial() != Material.LAVA && entity.getEntityBoundingBox().minY > pos.getY() + 0.5D - 1.0E-5D) {
            addCollisionBoxToList(pos, entityBox, boxes, PORTIUM_LAVA_SURFACE);
            return;
        }
        super.addCollisionBoxToList(state, world, pos, entityBox, boxes, entity, isActualState);
    }
}
