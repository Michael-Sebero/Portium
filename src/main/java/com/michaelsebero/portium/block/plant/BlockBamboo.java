package com.michaelsebero.portium.block.plant;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSoundTypes;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Bamboo (1.14): a stalk that grows up to 16 high in light, thickening ({@code age}) and growing leaves near its top,
 * and stops for good once a stalk reaches its last stage. Swords cut it at once. Placing the item on the ground
 * plants a {@link BlockBambooSapling}.
 */
public class BlockBamboo extends Block implements IGrowable {

    public enum Leaves implements IStringSerializable {
        NONE, SMALL, LARGE;

        @Override
        public String getName() {
            return this.name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 1);
    public static final PropertyEnum<Leaves> LEAVES = PropertyEnum.create("leaves", Leaves.class);
    public static final PropertyInteger STAGE = PropertyInteger.create("stage", 0, 1);

    /** 1.14's Material.BAMBOO: solid, flammable, broken by pistons. */
    public static final Material MATERIAL = new Material(MapColor.FOLIAGE) {
        {
            this.setBurning();
            this.setNoPushMobility();
        }
    };

    private static final AxisAlignedBB SHAPE_NORMAL = box(5, 0, 5, 11, 16, 11);
    private static final AxisAlignedBB SHAPE_LARGE_LEAVES = box(3, 0, 3, 13, 16, 13);
    private static final AxisAlignedBB SHAPE_COLLISION = box(6.5, 0, 6.5, 9.5, 16, 9.5);

    public BlockBamboo() {
        super(MATERIAL, MapColor.FOLIAGE);
        this.setHardness(1.0F);
        this.setSoundType(PortiumSoundTypes.BAMBOO);
        this.setHarvestLevel("axe", 0);
        this.setTickRandomly(true);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(AGE, 0).withProperty(LEAVES, Leaves.NONE).withProperty(STAGE, 0));
    }

    static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE, LEAVES, STAGE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(AGE, meta & 1).withProperty(LEAVES, Leaves.values()[Math.min(2, meta >> 1 & 3)])
                .withProperty(STAGE, meta >> 3 & 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE) | state.getValue(LEAVES).ordinal() << 1 | state.getValue(STAGE) << 3;
    }

    // ---- Shape ----

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        Vec3d offset = state.getOffset(world, pos);
        return (state.getValue(LEAVES) == Leaves.LARGE ? SHAPE_LARGE_LEAVES : SHAPE_NORMAL).offset(offset);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
                                      @Nullable Entity entity, boolean isActualState) {
        addCollisionBoxToList(pos, entityBox, boxes, SHAPE_COLLISION.offset(state.getOffset(world, pos)));
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE_COLLISION.offset(state.getOffset(world, pos));
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

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    // ---- Placement and support ----

    /** 1.14's {@code bamboo_plantable_on}: sand, gravel, dirt, grass, mycelium, and bamboo itself. */
    public static boolean isPlantableOn(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.SAND || block == Blocks.GRAVEL || block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.MYCELIUM
                || block instanceof BlockBamboo || block instanceof BlockBambooSapling;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        IBlockState here = world.getBlockState(pos);
        return here.getBlock().isReplaceable(world, pos) && here.getMaterial() != Material.WATER && here.getMaterial() != Material.LAVA
                && isPlantableOn(world.getBlockState(pos.down()));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        IBlockState below = world.getBlockState(pos.down());
        if (below.getBlock() instanceof BlockBambooSapling) return this.getDefaultState().withProperty(AGE, 0);
        if (below.getBlock() instanceof BlockBamboo) return this.getDefaultState().withProperty(AGE, below.getValue(AGE) > 0 ? 1 : 0);
        IBlockState above = world.getBlockState(pos.up());
        if (above.getBlock() instanceof BlockBamboo) return this.getDefaultState().withProperty(AGE, above.getValue(AGE));
        if (above.getBlock() instanceof BlockBambooSapling || PortiumBlocks.BAMBOO_SAPLING == null) return this.getDefaultState();
        return PortiumBlocks.BAMBOO_SAPLING.getDefaultState();
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!isPlantableOn(world.getBlockState(pos.down()))) world.scheduleUpdate(pos, this, 1);
        if (fromPos.equals(pos.up())) {
            IBlockState above = world.getBlockState(fromPos);
            if (above.getBlock() instanceof BlockBamboo && above.getValue(AGE) > state.getValue(AGE)) world.setBlockState(pos, state.cycleProperty(AGE), 2);
        }
    }

    /** The scheduled check after losing its ground: it breaks. Growth comes from {@link #randomTick} only. */
    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (!isPlantableOn(world.getBlockState(pos.down()))) world.destroyBlock(pos, true);
    }

    // ---- Growth ----

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (state.getValue(STAGE) != 0) return;
        if (random.nextInt(3) == 0 && world.isAirBlock(pos.up()) && world.getLightFromNeighbors(pos.up()) >= 9) {
            int height = this.countBelow(world, pos) + 1;
            if (height < 16) this.growStalk(state, world, pos, random, height);
        }
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        int above = this.countAbove(world, pos);
        int below = this.countBelow(world, pos);
        IBlockState top = world.getBlockState(pos.up(above));
        return above + below + 1 < 16 && (!(top.getBlock() instanceof BlockBamboo) || top.getValue(STAGE) != 1);
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        int above = this.countAbove(world, pos);
        int below = this.countBelow(world, pos);
        int height = above + below + 1;
        int times = 1 + rand.nextInt(2);
        for (int i = 0; i < times; i++) {
            BlockPos top = pos.up(above);
            IBlockState topState = world.getBlockState(top);
            if (height >= 16 || !(topState.getBlock() instanceof BlockBamboo) || topState.getValue(STAGE) == 1 || !world.isAirBlock(top.up())) return;
            this.growStalk(topState, world, top, rand, height);
            above++;
            height++;
        }
    }

    /** Adds a stalk on top, moving the leaves up (1.14's {@code BambooBlock.grow}). */
    protected void growStalk(IBlockState state, World world, BlockPos pos, Random random, int height) {
        IBlockState below = world.getBlockState(pos.down());
        BlockPos twoBelow = pos.down(2);
        IBlockState twoBelowState = world.getBlockState(twoBelow);
        Leaves leaves = Leaves.NONE;
        if (height >= 1) {
            if (below.getBlock() instanceof BlockBamboo && below.getValue(LEAVES) != Leaves.NONE) {
                leaves = Leaves.LARGE;
                if (twoBelowState.getBlock() instanceof BlockBamboo) {
                    world.setBlockState(pos.down(), below.withProperty(LEAVES, Leaves.SMALL), 3);
                    world.setBlockState(twoBelow, twoBelowState.withProperty(LEAVES, Leaves.NONE), 3);
                }
            } else {
                leaves = Leaves.SMALL;
            }
        }
        int age = state.getValue(AGE) != 1 && !(twoBelowState.getBlock() instanceof BlockBamboo) ? 0 : 1;
        int stage = (height < 11 || random.nextFloat() >= 0.25F) && height != 15 ? 0 : 1;
        world.setBlockState(pos.up(), this.getDefaultState().withProperty(AGE, age).withProperty(LEAVES, leaves).withProperty(STAGE, stage), 3);
    }

    private int countAbove(IBlockAccess world, BlockPos pos) {
        int count = 0;
        while (count < 16 && world.getBlockState(pos.up(count + 1)).getBlock() instanceof BlockBamboo) count++;
        return count;
    }

    private int countBelow(IBlockAccess world, BlockPos pos) {
        int count = 0;
        while (count < 16 && world.getBlockState(pos.down(count + 1)).getBlock() instanceof BlockBamboo) count++;
        return count;
    }

    /** Swords cut bamboo at once (1.14). */
    @Override
    public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World world, BlockPos pos) {
        return player.getHeldItemMainhand().getItem() instanceof ItemSword ? 1.0F : super.getPlayerRelativeBlockHardness(state, player, world, pos);
    }
}
