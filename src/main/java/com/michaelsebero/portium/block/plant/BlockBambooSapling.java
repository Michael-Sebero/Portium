package com.michaelsebero.portium.block.plant;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSoundTypes;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/** The bamboo shoot (1.14) planted from a bamboo item on the ground; it grows into bamboo. */
public class BlockBambooSapling extends Block implements IGrowable {

    /** 1.14's Material.BAMBOO_SAPLING: no collision, flammable, broken by pistons. */
    public static final Material MATERIAL = new Material(MapColor.WOOD) {
        {
            this.setBurning();
            this.setNoPushMobility();
        }

        @Override
        public boolean blocksMovement() {
            return false;
        }

        @Override
        public boolean isSolid() {
            return false;
        }
    };

    private static final AxisAlignedBB SHAPE = BlockBamboo.box(4, 0, 4, 12, 12, 12);

    public BlockBambooSapling() {
        super(MATERIAL);
        this.setHardness(1.0F);
        this.setSoundType(PortiumSoundTypes.BAMBOO_SAPLING);
        this.setTickRandomly(true);
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE.offset(state.getOffset(world, pos));
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && BlockBamboo.isPlantableOn(world.getBlockState(pos.down()));
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!BlockBamboo.isPlantableOn(world.getBlockState(pos.down()))) {
            this.dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        } else if (fromPos.equals(pos.up()) && world.getBlockState(fromPos).getBlock() instanceof BlockBamboo) {
            world.setBlockState(pos, PortiumBlocks.BAMBOO.getDefaultState(), 2);
        }
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (random.nextInt(3) == 0 && world.isAirBlock(pos.up()) && world.getLightFromNeighbors(pos.up()) >= 9) this.growBamboo(world, pos);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {}

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return world.isAirBlock(pos.up());
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        this.growBamboo(world, pos);
    }

    private void growBamboo(World world, BlockPos pos) {
        world.setBlockState(pos.up(), PortiumBlocks.BAMBOO.getDefaultState().withProperty(BlockBamboo.LEAVES, BlockBamboo.Leaves.SMALL), 3);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(PortiumBlocks.BAMBOO);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(PortiumBlocks.BAMBOO);
    }

    @Override
    public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World world, BlockPos pos) {
        return player.getHeldItemMainhand().getItem() instanceof ItemSword ? 1.0F : super.getPlayerRelativeBlockHardness(state, player, world, pos);
    }
}
