package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Waterlogged cells count as water through Forge's per-block liquid checks, which entity swimming, drowning and
 * liquid detection all ask; they resist explosions like water; and a waterlogged block that is broken leaves its
 * water behind (1.13).
 */
@Mixin(Block.class)
public abstract class MixinBlockWaterlog {

    /** What 1.12's water block resists explosions with: its hardness of 100. */
    private static final float WATER_EXPLOSION_RESISTANCE = 100.0F;

    /** A waterlogged cell is a still source: entities are in it below its surface, their eyes anywhere in the cell. */
    @Inject(method = "isEntityInsideMaterial", at = @At("HEAD"), cancellable = true, remap = false)
    private void portium$entityInWaterloggedCell(IBlockAccess world, BlockPos pos, IBlockState state, Entity entity, double yToTest, Material material,
                                                 boolean testingHead, CallbackInfoReturnable<Boolean> cir) {
        if (material != Material.WATER || !Waterlogging.isWaterlogged(world, pos)) return;
        cir.setReturnValue(testingHead ? yToTest < pos.getY() + 1.0D : yToTest >= pos.getY() + Waterlogging.SURFACE);
    }

    /** A waterlogged cell's water stands like a source: 8/9 of a block, or the full block under more water. Boats float on it. */
    @Inject(method = "getBlockLiquidHeight", at = @At("HEAD"), cancellable = true, remap = false)
    private void portium$waterloggedHeight(World world, BlockPos pos, IBlockState state, Material material, CallbackInfoReturnable<Float> cir) {
        if (material != Material.WATER || !Waterlogging.isWaterlogged(world, pos)) return;
        cir.setReturnValue(Waterlogging.hasWater(world, pos.up()) ? 1.0F : (float) Waterlogging.SURFACE);
    }

    @Inject(method = "isAABBInsideMaterial", at = @At("HEAD"), cancellable = true, remap = false)
    private void portium$boxInWaterloggedCell(World world, BlockPos pos, AxisAlignedBB box, Material material, CallbackInfoReturnable<Boolean> cir) {
        if (material == Material.WATER && Waterlogging.isWaterlogged(world, pos)) cir.setReturnValue(true);
    }

    @Inject(method = "isAABBInsideLiquid", at = @At("HEAD"), cancellable = true, remap = false)
    private void portium$liquidInWaterloggedCell(World world, BlockPos pos, AxisAlignedBB box, CallbackInfoReturnable<Boolean> cir) {
        if (Waterlogging.isWaterlogged(world, pos)) cir.setReturnValue(true);
    }

    /**
     * Mobs path through a waterlogged cell as water, since 1.13's path finding checks the fluid. Trapdoors, fences and
     * walls keep their own kinds, which 1.13 checks first.
     */
    @Inject(method = "getAiPathNodeType(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/pathfinding/PathNodeType;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void portium$waterloggedPath(IBlockState state, IBlockAccess world, BlockPos pos, CallbackInfoReturnable<PathNodeType> cir) {
        if (!Waterlogging.isWaterlogged(world, pos)) return;
        Block block = state.getBlock();
        if (block instanceof BlockTrapDoor) {
            cir.setReturnValue(PathNodeType.TRAPDOOR);
        } else if (!(block instanceof BlockFence) && !(block instanceof BlockWall)) {
            cir.setReturnValue(PathNodeType.WATER);
        }
    }

    /** Explosions meet the stronger of the block and its water, so waterlogged blocks resist them like water (1.13). */
    @Inject(method = "getExplosionResistance(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;Lnet/minecraft/world/Explosion;)F",
            at = @At("RETURN"), cancellable = true, remap = false)
    private void portium$waterResistance(World world, BlockPos pos, Entity exploder, Explosion explosion, CallbackInfoReturnable<Float> cir) {
        if (Waterlogging.isWaterlogged(world, pos)) cir.setReturnValue(Math.max(cir.getReturnValueF(), WATER_EXPLOSION_RESISTANCE));
    }

    @Redirect(method = "removedByPlayer", remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z", remap = true))
    private boolean portium$leaveWaterWhenBroken(World world, BlockPos pos, IBlockState air, int flags, IBlockState state, World sameWorld, BlockPos samePos,
                                                 EntityPlayer player, boolean willHarvest) {
        return world.setBlockState(pos, Waterlogging.leftBehind(world, pos, air), flags);
    }

    @Redirect(method = "onBlockExploded", remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockToAir(Lnet/minecraft/util/math/BlockPos;)Z", remap = true))
    private boolean portium$leaveWaterWhenExploded(World world, BlockPos pos, World sameWorld, BlockPos samePos, Explosion explosion) {
        IBlockState left = Waterlogging.leftBehind(world, pos, null);
        return left == null ? world.setBlockToAir(pos) : world.setBlockState(pos, left, 3);
    }
}
