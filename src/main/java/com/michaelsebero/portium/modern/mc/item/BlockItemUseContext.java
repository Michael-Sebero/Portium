package com.michaelsebero.portium.modern.mc.item;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.util.math.BlockRayTraceResult;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 1.13's {@code net.minecraft.item.BlockItemUseContext}: a block being placed. Its position is where the block goes,
 * the clicked block itself if that can be replaced.
 */
public class BlockItemUseContext extends ItemUseContext {

    private final BlockPos placePos;
    protected boolean replaceClicked;

    public BlockItemUseContext(ItemUseContext context) {
        this(context.world, context.player, context.hand, context.item, context.pos, context.face, context.hitX, context.hitY, context.hitZ, context.inside);
    }

    /** 1.16's: placing what the player holds where the ray trace hit. */
    public BlockItemUseContext(EntityPlayer player, EnumHand hand, ItemStack item, BlockRayTraceResult hit) {
        this(player.world, player, hand, item, hit);
    }

    /** 1.16's (protected there). */
    public BlockItemUseContext(World world, @Nullable EntityPlayer player, EnumHand hand, ItemStack item, BlockRayTraceResult hit) {
        this(world, player, hand, item, hit.getBlockPos(), hit.sideHit, (float) (hit.hitVec.x - hit.getBlockPos().getX()),
                (float) (hit.hitVec.y - hit.getBlockPos().getY()), (float) (hit.hitVec.z - hit.getBlockPos().getZ()), hit.isInside());
    }

    private BlockItemUseContext(World world, @Nullable EntityPlayer player, EnumHand hand, ItemStack item, BlockPos pos, EnumFacing face, float hitX,
                                float hitY, float hitZ, boolean inside) {
        super(world, player, hand, item, pos, face, hitX, hitY, hitZ, inside);
        this.replaceClicked = world.getBlockState(pos).getBlock().isReplaceable(world, pos);
        this.placePos = this.replaceClicked ? pos : pos.offset(face);
    }

    /** 1.16's: the same placement, aimed at the middle of a block's face instead. */
    @Srg("func_221536_a")
    public static BlockItemUseContext atFace(BlockItemUseContext context, BlockPos pos, EnumFacing face) {
        Vec3d hit = new Vec3d(pos.getX() + 0.5D + face.getXOffset() * 0.5D, pos.getY() + 0.5D + face.getYOffset() * 0.5D,
                pos.getZ() + 0.5D + face.getZOffset() * 0.5D);
        return new BlockItemUseContext(context.world, context.player, context.hand, context.item, new BlockRayTraceResult(hit, face, pos, false));
    }

    public BlockItemUseContext(World world, @Nullable EntityPlayer player, ItemStack item, BlockPos pos, EnumFacing face, float hitX, float hitY,
                               float hitZ) {
        super(world, player, item, pos, face, hitX, hitY, hitZ);
        this.replaceClicked = world.getBlockState(pos).getBlock().isReplaceable(world, pos);
        this.placePos = this.replaceClicked ? pos : pos.offset(face);
    }

    /** A context whose position is already where the block goes, as 1.12's placement code has worked it out. */
    public static BlockItemUseContext at(World world, @Nullable EntityPlayer player, ItemStack item, BlockPos placePos, EnumFacing face, float hitX,
                                         float hitY, float hitZ) {
        return at(world, player, EnumHand.MAIN_HAND, item, placePos, face, hitX, hitY, hitZ);
    }

    /** The same, with the hand holding the item. */
    public static BlockItemUseContext at(World world, @Nullable EntityPlayer player, EnumHand hand, ItemStack item, BlockPos placePos, EnumFacing face,
                                         float hitX, float hitY, float hitZ) {
        return new BlockItemUseContext(world, player, hand, item, placePos.offset(face.getOpposite()), face, hitX, hitY, hitZ, placePos);
    }

    private BlockItemUseContext(World world, @Nullable EntityPlayer player, EnumHand hand, ItemStack item, BlockPos clicked, EnumFacing face, float hitX,
                                float hitY, float hitZ, BlockPos placePos) {
        super(world, player, hand, item, clicked, face, hitX, hitY, hitZ, false);
        this.placePos = placePos;
        this.replaceClicked = placePos.equals(clicked);
    }

    @Override
    @Srg("func_195995_a")
    public BlockPos getPos() {
        return this.placePos;
    }

    @Srg("func_196011_b")
    public boolean canPlace() {
        return this.replaceClicked || this.world.getBlockState(this.placePos).getBlock().isReplaceable(this.world, this.placePos);
    }

    @Srg("func_196012_c")
    public boolean replacingClickedOnBlock() {
        return this.replaceClicked;
    }

    /** The direction the placer looks most nearly towards. */
    @Srg("func_196010_d")
    public EnumFacing getNearestLookingDirection() {
        return getNearestLookingDirections()[0];
    }

    /** All six directions, nearest to the placer's look first. */
    @Srg("func_196009_e")
    public EnumFacing[] getNearestLookingDirections() {
        if (this.player == null) return EnumFacing.values();
        Vec3d look = this.player.getLook(1.0F);
        EnumFacing[] facings = EnumFacing.values().clone();
        Arrays.sort(facings, Comparator.comparingDouble(facing -> -(facing.getXOffset() * look.x + facing.getYOffset() * look.y + facing.getZOffset() * look.z)));
        return facings;
    }
}
