package com.michaelsebero.portium.block.plant;

import com.michaelsebero.portium.entity.IMotionMultiplier;
import com.michaelsebero.portium.init.PortiumDamageSources;
import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumSoundTypes;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The sweet berry bush (1.14): grows through four stages in light, gives berries when picked (two or three when ripe),
 * slows whatever walks through it and pricks what moves in it once it's grown (foxes and bees excepted).
 */
public class BlockSweetBerryBush extends BlockBush implements IGrowable {

    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 3);

    private static final AxisAlignedBB BUSHLING = new AxisAlignedBB(0.1875D, 0.0D, 0.1875D, 0.8125D, 0.5D, 0.8125D);
    private static final AxisAlignedBB GROWING = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 1.0D, 0.9375D);
    private static final Vec3d SLOWDOWN = new Vec3d(0.800000011920929D, 0.75D, 0.800000011920929D);

    public BlockSweetBerryBush() {
        super(Material.PLANTS);
        this.setSoundType(PortiumSoundTypes.SWEET_BERRY_BUSH);
        this.setTickRandomly(true);
        this.setDefaultState(this.blockState.getBaseState().withProperty(AGE, 0));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(AGE, meta & 3);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        int age = state.getValue(AGE);
        return age == 0 ? BUSHLING : age < 3 ? GROWING : FULL_BLOCK_AABB;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        super.updateTick(world, pos, state, rand);
        int age = state.getValue(AGE);
        if (age < 3 && rand.nextInt(5) == 0 && world.getLightFromNeighbors(pos.up()) >= 9) {
            world.setBlockState(pos, state.withProperty(AGE, age + 1), 2);
        }
    }

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!(entity instanceof EntityLivingBase) || isImmune(entity)) return;
        ((IMotionMultiplier) entity).portium$setMotionMultiplier(SLOWDOWN);
        // 1.14 compares with lastTickPos; Forge 1.12 resets that in every server-side move (chunk registration), so
        // the position at the start of the tick is prevPos here.
        if (!world.isRemote && state.getValue(AGE) > 0 && (entity.prevPosX != entity.posX || entity.prevPosZ != entity.posZ)) {
            double movedX = Math.abs(entity.posX - entity.prevPosX);
            double movedZ = Math.abs(entity.posZ - entity.prevPosZ);
            if (movedX >= 0.003000000026077032D || movedZ >= 0.003000000026077032D) entity.attackEntityFrom(PortiumDamageSources.SWEET_BERRY_BUSH, 1.0F);
        }
    }

    /** Foxes and bees go through berry bushes unharmed (1.14, 1.15). */
    private static boolean isImmune(Entity entity) {
        ResourceLocation id = EntityList.getKey(entity);
        return id != null && (com.michaelsebero.portium.Tags.MOD_ID.equals(id.getNamespace()) || "minecraft".equals(id.getNamespace())) && ("fox".equals(id.getPath()) || "bee".equals(id.getPath()));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
                                    float hitY, float hitZ) {
        int age = state.getValue(AGE);
        boolean ripe = age == 3;
        ItemStack held = player.getHeldItem(hand);
        // Bone meal on a bush that isn't ripe grows it instead.
        if (!ripe && held.getItem() == Items.DYE && held.getMetadata() == 15) return false;
        if (age <= 1) return false;
        int count = 1 + world.rand.nextInt(2);
        if (PortiumItems.SWEET_BERRIES != null) spawnAsEntity(world, pos, new ItemStack(PortiumItems.SWEET_BERRIES, count + (ripe ? 1 : 0)));
        world.playSound(null, pos, PortiumSounds.ITEM_SWEET_BERRIES_PICK_FROM_BUSH, SoundCategory.BLOCKS, 1.0F, 0.8F + world.rand.nextFloat() * 0.4F);
        world.setBlockState(pos, state.withProperty(AGE, 1), 2);
        return true;
    }

    // ---- Bone meal ----

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return state.getValue(AGE) < 3;
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        world.setBlockState(pos, state.withProperty(AGE, Math.min(3, state.getValue(AGE) + 1)), 2);
    }

    // ---- Drops ----

    /** Breaking it gives nothing but what's picked (1.14's loot: berries by age). */
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return PortiumItems.SWEET_BERRIES != null ? PortiumItems.SWEET_BERRIES : Items.AIR;
    }

    @Override
    public int quantityDropped(IBlockState state, int fortune, Random random) {
        int age = state.getValue(AGE);
        if (age < 2) return 0;
        int bonus = fortune > 0 ? random.nextInt(fortune + 1) : 0;
        return (age == 3 ? 2 + random.nextInt(2) : 1 + random.nextInt(2)) + bonus;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return PortiumItems.SWEET_BERRIES != null ? new ItemStack(PortiumItems.SWEET_BERRIES) : ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world, BlockPos pos) {
        return PathNodeType.DAMAGE_OTHER;
    }
}
