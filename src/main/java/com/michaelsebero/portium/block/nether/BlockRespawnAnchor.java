package com.michaelsebero.portium.block.nether;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.proxy.ParticleKind;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The respawn anchor (1.16): charged with glowstone (four charges), it sets the player's respawn point in the Nether,
 * and each respawn there uses a charge. Anywhere else, using a charged anchor blows it up.
 * <p>
 * 1.12 keeps a respawn point per dimension; Portium's anchor sets the Nether's and the player's respawn dimension,
 * and answers as a bed while charged ({@link #isBed}, {@link #getBedSpawnPosition}). {@code MixinPlayerList} sends a
 * player whose anchor works to the Nether wherever they died, as 1.16 does.
 */
public class BlockRespawnAnchor extends Block {

    public static final PropertyInteger CHARGES = PropertyInteger.create("charges", 0, 4);

    /** Where around the anchor a player may respawn, in 1.16's order: beside it, then below, then above. */
    private static final List<Vec3i> RESPAWN_OFFSETS;

    static {
        List<Vec3i> beside = new ArrayList<>();
        int[][] offsets = {{0, -1}, {-1, 0}, {0, 1}, {1, 0}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
        for (int[] offset : offsets) beside.add(new Vec3i(offset[0], 0, offset[1]));
        List<Vec3i> all = new ArrayList<>(beside);
        for (Vec3i offset : beside) all.add(new Vec3i(offset.getX(), -1, offset.getZ()));
        for (Vec3i offset : beside) all.add(new Vec3i(offset.getX(), 1, offset.getZ()));
        all.add(new Vec3i(0, 1, 0));
        RESPAWN_OFFSETS = Collections.unmodifiableList(all);
    }

    public BlockRespawnAnchor() {
        super(Material.ROCK, MapColor.BLACK);
        this.setHardness(50.0F);
        // 1.16's blast resistance 1200.
        this.setResistance(2000.0F);
        this.setHarvestLevel("pickaxe", 3);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(CHARGES, 0));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, CHARGES);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(CHARGES, Math.min(meta & 7, 4));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(CHARGES);
    }

    // ---- Use ----

    private static boolean isFuel(ItemStack stack) {
        return stack.getItem() == Item.getItemFromBlock(Blocks.GLOWSTONE);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
                                    float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        // Glowstone in the other hand charges it instead.
        if (hand == EnumHand.MAIN_HAND && !isFuel(stack) && isFuel(player.getHeldItem(EnumHand.OFF_HAND))) return false;
        if (isFuel(stack) && state.getValue(CHARGES) < 4) {
            charge(world, pos, state);
            if (!player.capabilities.isCreativeMode) stack.shrink(1);
            return true;
        }
        if (state.getValue(CHARGES) == 0) return false;
        if (!works(world)) {
            if (!world.isRemote) this.explode(world, pos);
            return true;
        }
        if (!world.isRemote) {
            EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
            int dimension = world.provider.getDimension();
            if (serverPlayer.getSpawnDimension() != dimension || !pos.equals(serverPlayer.getBedLocation(dimension))) {
                setRespawnPoint(serverPlayer, dimension, pos);
                serverPlayer.sendMessage(new TextComponentTranslation("block.minecraft.set_spawn"));
                world.playSound(null, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D,
                        PortiumSounds.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }
        return true;
    }

    /**
     * 1.16 has one respawn point: an anchor's replaces the bed's. 1.12 keeps one per dimension, so the others are
     * cleared, and the respawn dimension becomes the anchor's.
     */
    public static void setRespawnPoint(EntityPlayerMP player, int dimension, BlockPos pos) {
        player.setSpawnChunk(null, false, 0);
        player.setSpawnChunk(pos, false, dimension);
        player.setSpawnDimension(dimension);
        com.michaelsebero.portium.handler.RespawnAnchors.remember(player, dimension, pos);
    }

    /** Whether anchors work in this world: only where beds don't (1.16: the Nether). */
    public static boolean works(World world) {
        return world.provider.getDimensionType() == DimensionType.NETHER;
    }

    public static void charge(World world, BlockPos pos, IBlockState state) {
        world.setBlockState(pos, state.withProperty(CHARGES, state.getValue(CHARGES) + 1), 3);
        world.playSound(null, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, PortiumSounds.BLOCK_RESPAWN_ANCHOR_CHARGE,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    /**
     * Blows up like a bed in the Nether (power 5, with fire), unless water touches it: then it hurts what's around but
     * breaks nothing, as 1.16's water-resistant explosion does.
     */
    private void explode(World world, BlockPos pos) {
        world.setBlockToAir(pos);
        boolean water = Waterlogging.hasWater(world, pos.up());
        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) water |= isNearWater(world, pos.offset(side));
        // Its deaths are credited to [Intentional Game Design], as a bed's are (1.16's bad respawn point explosion).
        com.michaelsebero.portium.world.BedExplosions.begin();
        try {
            world.newExplosion(null, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, 5.0F, !water, !water);
        } finally {
            com.michaelsebero.portium.world.BedExplosions.end();
        }
    }

    /** 1.16: a water source (or waterlogged block), or flowing water at least two high that has no water under it. */
    private static boolean isNearWater(World world, BlockPos pos) {
        if (Waterlogging.isWaterlogged(world, pos)) return true;
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial() != Material.WATER) return false;
        if (!(state.getBlock() instanceof BlockLiquid)) return true;
        int level = state.getValue(BlockLiquid.LEVEL);
        if (level == 0) return true;
        // 1.12 counts flowing water down from a source (1-7); falling water (8 and up) is 1.16's level 8.
        int modernLevel = level >= 8 ? 8 : 8 - level;
        if (modernLevel < 2) return false;
        return !Waterlogging.hasWater(world, pos.down());
    }

    // ---- Respawning (as 1.12's bed hooks) ----

    @Override
    public boolean isBed(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable Entity player) {
        return state.getValue(CHARGES) > 0 && world instanceof World && works((World) world);
    }

    /** Where the player respawns, using up a charge; null if no spot around is safe. */
    @Nullable
    @Override
    public BlockPos getBedSpawnPosition(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable EntityPlayer player) {
        if (!(world instanceof World)) return null;
        BlockPos spot = findRespawnPoint((World) world, pos);
        if (spot != null) ((World) world).setBlockState(pos, state.withProperty(CHARGES, state.getValue(CHARGES) - 1), 3);
        return spot;
    }

    /** Whether a player whose respawn point is here would respawn: charged, in the Nether, with room around. */
    public static boolean canRespawnAt(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof BlockRespawnAnchor && state.getValue(CHARGES) > 0 && works(world) && findRespawnPoint(world, pos) != null;
    }

    /** 1.16's {@code findRespawnPoint}: the first spot around where a player fits, first avoiding danger. */
    @Nullable
    public static BlockPos findRespawnPoint(World world, BlockPos anchor) {
        for (boolean avoidDanger : new boolean[] {true, false}) {
            for (Vec3i offset : RESPAWN_OFFSETS) {
                BlockPos spot = anchor.add(offset);
                if (canStandAt(world, spot, avoidDanger)) return spot;
            }
        }
        return null;
    }

    /**
     * 1.16's {@code TransportationHelper.func_242379_a} for a player: standing on the block's collision (or the
     * block below's), within a block of its base, with room for the player's box.
     */
    private static boolean canStandAt(World world, BlockPos pos, boolean avoidDanger) {
        IBlockState state = world.getBlockState(pos);
        if (avoidDanger && isDangerous(state)) return false;
        double floor = floorHeight(world, pos, state);
        if (Double.isInfinite(floor) || floor >= 1.0D) return false;
        if (avoidDanger && floor <= 0.0D && isDangerous(world.getBlockState(pos.down()))) return false;
        double y = (double) pos.getY() + floor;
        AxisAlignedBB box = new AxisAlignedBB((double) pos.getX() + 0.2D, y, (double) pos.getZ() + 0.2D, (double) pos.getX() + 0.8D, y + 1.8D,
                (double) pos.getZ() + 0.8D);
        return world.getCollisionBoxes(null, box).isEmpty();
    }

    /** The height above the block's base a player would stand at: its collision top, or the block below's (minus 1). */
    private static double floorHeight(World world, BlockPos pos, IBlockState state) {
        AxisAlignedBB top = collisionTop(world, pos, state);
        if (top != null) return top.maxY;
        BlockPos below = pos.down();
        AxisAlignedBB under = collisionTop(world, below, world.getBlockState(below));
        return under != null && under.maxY >= 1.0D ? under.maxY - 1.0D : Double.NEGATIVE_INFINITY;
    }

    @Nullable
    private static AxisAlignedBB collisionTop(World world, BlockPos pos, IBlockState state) {
        // Ladders, vines and open trapdoors don't hold anyone up (1.16 leaves climbable blocks out).
        if (state.getBlock().isLadder(state, world, pos, null)) return null;
        AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
        return box == null || box == NULL_AABB ? null : box;
    }

    /** What 1.16 keeps players from spawning in or on: fire, lava, magma, lit campfires, cactus (and 1.14's plants that hurt). */
    private static boolean isDangerous(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.FIRE || block instanceof BlockSoulFire || block == Blocks.MAGMA || state.getMaterial() == Material.LAVA
                || com.michaelsebero.portium.block.light.BlockCampfire.isLit(state) || block == Blocks.CACTUS;
    }

    // ---- Light, comparators, effects ----

    public static int chargeScale(IBlockState state, int max) {
        return MathHelper.floor((float) state.getValue(CHARGES) / 4.0F * (float) max);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getLightValue(IBlockState state) {
        return state.getBlock() == this ? chargeScale(state, 15) : 0;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.getLightValue(state);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return chargeScale(state, 15);
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (state.getValue(CHARGES) == 0) return;
        if (rand.nextInt(100) == 0) {
            world.playSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, PortiumSounds.BLOCK_RESPAWN_ANCHOR_AMBIENT,
                    SoundCategory.BLOCKS, 1.0F, 1.0F, false);
        }
        double x = (double) pos.getX() + 0.5D + (0.5D - rand.nextDouble());
        double y = (double) pos.getY() + 1.0D;
        double z = (double) pos.getZ() + 0.5D + (0.5D - rand.nextDouble());
        double speed = (double) rand.nextFloat() * 0.04D;
        Portium.proxy.spawnParticle(ParticleKind.REVERSE_PORTAL, world, x, y, z, 0.0D, speed, 0.0D);
    }
}
