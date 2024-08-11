package com.michaelsebero.portium.modern.mc.item;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import com.michaelsebero.portium.modern.mc.util.math.BlockRayTraceResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.13's {@code net.minecraft.item.ItemUseContext}: an item used on a block, which 1.12 passes as separate arguments.
 * Also 1.16's, which adds the hand and keeps the hit as a block ray trace result.
 */
public class ItemUseContext {

    protected final World world;
    @Nullable
    protected final EntityPlayer player;
    protected final ItemStack item;
    protected final BlockPos pos;
    protected final EnumFacing face;
    protected final float hitX;
    protected final float hitY;
    protected final float hitZ;
    protected final EnumHand hand;
    protected final boolean inside;

    public ItemUseContext(World world, @Nullable EntityPlayer player, ItemStack item, BlockPos pos, EnumFacing face, float hitX, float hitY, float hitZ) {
        this(world, player, EnumHand.MAIN_HAND, item, pos, face, hitX, hitY, hitZ, false);
    }

    /** 1.12's use of an item on a block, with the hand that holds it. */
    public ItemUseContext(World world, @Nullable EntityPlayer player, EnumHand hand, ItemStack item, BlockPos pos, EnumFacing face, float hitX, float hitY,
                          float hitZ, boolean inside) {
        this.world = world;
        this.hand = hand;
        this.inside = inside;
        this.player = player;
        this.item = item;
        this.pos = pos;
        this.face = face;
        this.hitX = hitX;
        this.hitY = hitY;
        this.hitZ = hitZ;
    }

    public ItemUseContext(EntityPlayer player, ItemStack item, BlockPos pos, EnumFacing face, float hitX, float hitY, float hitZ) {
        this(player.world, player, item, pos, face, hitX, hitY, hitZ);
    }

    /** 1.16's: what the player holds in that hand, used where the ray trace hit. */
    public ItemUseContext(EntityPlayer player, EnumHand hand, BlockRayTraceResult hit) {
        this(player.world, player, hand, player.getHeldItem(hand), hit);
    }

    /** 1.16's (protected there). */
    public ItemUseContext(World world, @Nullable EntityPlayer player, EnumHand hand, ItemStack item, BlockRayTraceResult hit) {
        this(world, player, hand, item, hit.getBlockPos(), hit.sideHit, (float) (hit.hitVec.x - hit.getBlockPos().getX()),
                (float) (hit.hitVec.y - hit.getBlockPos().getY()), (float) (hit.hitVec.z - hit.getBlockPos().getZ()), hit.isInside());
    }

    /** 1.16's hit, as a ray trace result (protected there). */
    @Srg("func_242401_i")
    public BlockRayTraceResult getRayTraceResult() {
        return new BlockRayTraceResult(getHitVec(), this.face, this.pos, this.inside);
    }

    /** Where the block was hit, in world coordinates. */
    @Srg("func_221532_j")
    public Vec3d getHitVec() {
        return new Vec3d(this.pos.getX() + this.hitX, this.pos.getY() + this.hitY, this.pos.getZ() + this.hitZ);
    }

    @Srg("func_221533_k")
    public boolean isInside() {
        return this.inside;
    }

    @Srg("func_221531_n")
    public EnumHand getHand() {
        return this.hand;
    }

    /** 1.16's sneak check (secondary use: sneaking, for players). */
    @Srg("func_225518_g_")
    public boolean hasSecondaryUseForPlayer() {
        return this.player != null && this.player.isSneaking();
    }

    @Srg("func_195995_a")
    public BlockPos getPos() {
        return this.pos;
    }

    @Srg("func_196000_l")
    public EnumFacing getFace() {
        return this.face;
    }

    @Srg("func_195996_i")
    public ItemStack getItem() {
        return this.item;
    }

    @Nullable
    @Srg("func_195999_j")
    public EntityPlayer getPlayer() {
        return this.player;
    }

    @Srg("func_195991_k")
    public World getWorld() {
        return this.world;
    }

    @Srg("func_195990_h")
    public float getPlacementYaw() {
        return this.player == null ? 0.0F : this.player.rotationYaw;
    }

    @Srg("func_195992_f")
    public EnumFacing getPlacementHorizontalFacing() {
        return this.player == null ? EnumFacing.NORTH : this.player.getHorizontalFacing();
    }

    @Srg("func_195998_g")
    public boolean isPlacerSneaking() {
        return this.player != null && this.player.isSneaking();
    }

    @Srg("func_195997_m")
    public float getHitX() {
        return this.hitX;
    }

    @Srg("func_195993_n")
    public float getHitY() {
        return this.hitY;
    }

    @Srg("func_195994_o")
    public float getHitZ() {
        return this.hitZ;
    }
}
