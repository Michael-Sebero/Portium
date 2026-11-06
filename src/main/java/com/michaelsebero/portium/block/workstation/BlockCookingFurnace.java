package com.michaelsebero.portium.block.workstation;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.recipe.CookingRecipes;
import com.michaelsebero.portium.tile.TileEntityCookingFurnace;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/** The smoker and the blast furnace (1.14): furnaces for food and for ores and metal, cooking twice as fast. */
public class BlockCookingFurnace extends Block {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool LIT = PropertyBool.create("lit");

    private final CookingRecipes.Type type;
    private final int gui;
    private final StatBase stat;

    public BlockCookingFurnace(CookingRecipes.Type type, int gui, StatBase stat) {
        super(Material.ROCK);
        this.type = type;
        this.gui = gui;
        this.stat = stat;
        this.setHardness(3.5F);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(LIT, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, LIT);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(LIT, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(LIT) ? 4 : 0);
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (stack.hasDisplayName()) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityCookingFurnace) ((TileEntityCookingFurnace) tile).setCustomName(stack.getDisplayName());
        }
    }

    /** Switches the lit look and light, keeping the tile entity (1.12's furnace swaps blocks instead). */
    public static void setLit(World world, BlockPos pos, boolean lit) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockCookingFurnace && state.getValue(LIT) != lit) world.setBlockState(pos, state.withProperty(LIT, lit), 3);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
                                    float hitY, float hitZ) {
        if (world.isRemote) return true;
        if (world.getTileEntity(pos) instanceof TileEntityCookingFurnace) {
            player.openGui(Portium.instance, this.gui, world, pos.getX(), pos.getY(), pos.getZ());
            player.addStat(this.stat);
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof IInventory) {
            InventoryHelper.dropInventoryItems(world, pos, (IInventory) tile);
            world.updateComparatorOutputLevel(pos, this);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return this.type == CookingRecipes.Type.BLASTING ? new TileEntityCookingFurnace.BlastFurnace() : new TileEntityCookingFurnace.Smoker();
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return Container.calcRedstone(world.getTileEntity(pos));
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getLightValue(IBlockState state) {
        return state.getBlock() == this && state.getValue(LIT) ? 13 : 0;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.getLightValue(state);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (!state.getValue(LIT)) return;
        double x = (double) pos.getX() + 0.5D;
        double y = (double) pos.getY();
        double z = (double) pos.getZ() + 0.5D;
        if (this.type == CookingRecipes.Type.SMOKING) {
            // 1.14's smoker: a crackle now and then, smoke from its top.
            if (rand.nextDouble() < 0.1D) {
                world.playSound(x, y, z, PortiumSounds.BLOCK_SMOKER_SMOKE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
            }
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y + 1.1D, z, 0.0D, 0.0D, 0.0D);
        } else {
            // 1.14's blast furnace: smoke at its front.
            if (rand.nextDouble() < 0.1D) {
                world.playSound(x, y, z, PortiumSounds.BLOCK_BLASTFURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
            }
            EnumFacing facing = state.getValue(FACING);
            EnumFacing.Axis axis = facing.getAxis();
            double side = rand.nextDouble() * 0.6D - 0.3D;
            double dx = axis == EnumFacing.Axis.X ? (double) facing.getXOffset() * 0.52D : side;
            double dy = rand.nextDouble() * 9.0D / 16.0D;
            double dz = axis == EnumFacing.Axis.Z ? (double) facing.getZOffset() * 0.52D : side;
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + dx, y + dy, z + dz, 0.0D, 0.0D, 0.0D);
        }
    }
}
