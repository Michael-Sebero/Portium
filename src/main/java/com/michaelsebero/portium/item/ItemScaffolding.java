package com.michaelsebero.portium.item;

import com.michaelsebero.portium.block.BlockScaffolding;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * Scaffolding's item (1.14's ScaffoldingItem). Used on scaffolding it builds onto the structure: upward to the top of
 * the column (or, aimed at the top face, sideways the way the player faces), or while sneaking out in the direction
 * aimed, up to 7 blocks sideways. Elsewhere it needs support within 6 pieces. Mobs in the way don't stop it.
 */
public class ItemScaffolding extends ItemBlock {

    public ItemScaffolding(Block block) {
        super(block);
    }

    /** Where a piece goes, and the face it's put against. */
    private static final class Placement {
        final BlockPos pos;
        final EnumFacing face;

        Placement(BlockPos pos, EnumFacing face) {
            this.pos = pos;
            this.face = face;
        }
    }

    @Nullable
    private Placement placement(World world, BlockPos clicked, EnumFacing face, EntityPlayer player) {
        IBlockState state = world.getBlockState(clicked);
        if (state.getBlock() != this.block) {
            BlockPos target = state.getBlock().isReplaceable(world, clicked) ? clicked : clicked.offset(face);
            return BlockScaffolding.getDistance(world, target) == BlockScaffolding.MAX_DISTANCE ? null : new Placement(target, face);
        }
        // Aimed from inside the piece (as when standing in a column), 1.14 takes the face as the one looked out of
        // toward the player.
        Vec3d eyes = player.getPositionEyes(1.0F);
        boolean inside = eyes.x >= clicked.getX() && eyes.x <= clicked.getX() + 1 && eyes.y >= clicked.getY() && eyes.y <= clicked.getY() + 1
                && eyes.z >= clicked.getZ() && eyes.z <= clicked.getZ() + 1;
        if (inside) {
            Vec3d look = player.getLookVec();
            face = EnumFacing.getFacingFromVector((float) look.x, (float) look.y, (float) look.z).getOpposite();
        }
        EnumFacing direction;
        if (player.isSneaking()) direction = inside ? face.getOpposite() : face;
        else direction = face == EnumFacing.UP ? player.getHorizontalFacing() : EnumFacing.UP;
        int steps = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(clicked).move(direction);
        while (steps < BlockScaffolding.MAX_DISTANCE) {
            if (!isValid(at)) {
                if (!world.isRemote && player instanceof EntityPlayerMP && at.getY() >= world.getHeight()) {
                    TextComponentTranslation message = new TextComponentTranslation("build.tooHigh", world.getHeight());
                    message.getStyle().setColor(TextFormatting.RED);
                    ((EntityPlayerMP) player).connection.sendPacket(new SPacketChat(message, ChatType.GAME_INFO));
                }
                break;
            }
            IBlockState there = world.getBlockState(at);
            if (there.getBlock() != this.block) {
                if (there.getBlock().isReplaceable(world, at)) return new Placement(at.toImmutable(), direction);
                break;
            }
            at.move(direction);
            if (direction.getAxis().isHorizontal()) steps++;
        }
        return null;
    }

    /** 1.14's {@code World.isValid}: inside build height and the 30-million-block world. */
    private static boolean isValid(BlockPos pos) {
        return pos.getY() >= 0 && pos.getY() < 256 && pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000
                && pos.getZ() < 30000000;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY,
                                      float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        Placement placement = this.placement(world, pos, facing, player);
        if (placement == null || stack.isEmpty() || !player.canPlayerEdit(placement.pos, placement.face, stack)
                || !world.mayPlace(this.block, placement.pos, true, placement.face, player)) {
            return EnumActionResult.FAIL;
        }
        IBlockState state = this.block.getStateForPlacement(world, placement.pos, placement.face, hitX, hitY, hitZ,
                this.getMetadata(stack.getMetadata()), player, hand);
        if (this.placeBlockAt(stack, player, world, placement.pos, placement.face, hitX, hitY, hitZ, state)) {
            IBlockState placed = world.getBlockState(placement.pos);
            SoundType sound = placed.getBlock().getSoundType(placed, world, placement.pos, player);
            world.playSound(player, placement.pos, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            stack.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }

    /** The client's check before it asks the server: whether there's anywhere for the piece to go. */
    @SideOnly(Side.CLIENT)
    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side, EntityPlayer player, ItemStack stack) {
        Placement placement = this.placement(world, pos, side, player);
        return placement != null && world.mayPlace(this.block, placement.pos, true, placement.face, null);
    }
}
