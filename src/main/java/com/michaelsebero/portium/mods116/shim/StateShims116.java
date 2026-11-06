package com.michaelsebero.portium.mods116.shim;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;

import java.util.Collection;
import java.util.Optional;

/** 1.16's block state methods (on {@code StateHolder}) that 1.12's states have under other names, or lack. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class StateShims116 {

    /**
     * {@code with}: as 1.12's, but a property the state lacks leaves it as it is (Portium's stand-in for a block it
     * hasn't backported is air, which 1.16 code then sets its properties on).
     */
    public static Object with(IBlockState state, IProperty property, Comparable value) {
        return state.getPropertyKeys().contains(property) ? state.withProperty(property, value) : state;
    }

    /** {@code cycleValue}. */
    public static Object cycle(IBlockState state, IProperty property) {
        return state.cycleProperty(property);
    }

    /**
     * {@code hasProperty}: the property, or one of the same name and kind (1.16's {@code BlockStateProperties.LIT} on
     * Portium's blast furnace), or a furnace's lit-ness (1.12 has lit furnaces as blocks of their own).
     */
    public static boolean hasProperty(IBlockState state, IProperty property) {
        return state.getPropertyKeys().contains(property) || alike(state, property) != null || isFurnaceLit(state, property);
    }

    /** {@code getValue}: as {@link #hasProperty} finds it. */
    public static Comparable getValue(IBlockState state, IProperty property) {
        Comparable value = (Comparable) state.getProperties().get(property);
        if (value != null) return value;
        IProperty alike = alike(state, property);
        if (alike != null) return state.getValue(alike);
        if (isFurnaceLit(state, property)) return state.getBlock() == net.minecraft.init.Blocks.LIT_FURNACE;
        Comparable single = chestType(state, property);
        if (single != null) return single;
        return state.getValue(property);
    }

    /**
     * 1.16's {@code ChestBlock.TYPE} of one of 1.12's chests, which has none (1.12 pairs chests by their neighbours):
     * single, so 1.16 code taking a chest (Create's contraptions) leaves it as it is.
     */
    @javax.annotation.Nullable
    private static Comparable chestType(IBlockState state, IProperty property) {
        if (!(state.getBlock() instanceof net.minecraft.block.BlockChest) || !"type".equals(property.getName())) return null;
        for (Object value : property.getAllowedValues()) {
            if ("single".equals(property.getName((Comparable) value))) return (Comparable) value;
        }
        return null;
    }

    /**
     * The state's property that is the same as one it lacks (same name, value class and values: 1.16's {@code LIT}
     * beside Portium's own), or null. Same-named ones with other values (a horizontal axis beside a full one) aren't.
     */
    @javax.annotation.Nullable
    private static IProperty alike(IBlockState state, IProperty property) {
        for (IProperty own : state.getPropertyKeys()) {
            if (own.getName().equals(property.getName()) && own.getValueClass() == property.getValueClass()
                    && new java.util.HashSet<>(own.getAllowedValues()).equals(new java.util.HashSet<>(property.getAllowedValues()))) return own;
        }
        return null;
    }

    private static boolean isFurnaceLit(IBlockState state, IProperty property) {
        return state.getBlock() instanceof net.minecraft.block.BlockFurnace && "lit".equals(property.getName()) && property.getValueClass() == Boolean.class;
    }

    /** {@code getProperties}. */
    public static Collection getProperties(IBlockState state) {
        return state.getPropertyKeys();
    }

    /** {@code func_235903_d_}: the value, if the state has the property. */
    public static Optional getOptional(IBlockState state, IProperty property) {
        return hasProperty(state, property) ? Optional.of(state.getValue(property)) : Optional.empty();
    }

    /** {@code getValues}. */
    public static ImmutableMap getValues(IBlockState state) {
        return state.getProperties();
    }

    /**
     * 1.16's {@code new BlockState(block, values, codec)}: a state of its own (code makes one as a marker, compared by
     * identity), made as 1.12 makes its states.
     */
    public static IBlockState newState(net.minecraft.block.Block block, ImmutableMap values, Object codec) {
        return new MadeState(block, values == null ? ImmutableMap.of() : values);
    }

    private static final class MadeState extends net.minecraft.block.state.BlockStateContainer.StateImplementation {
        MadeState(net.minecraft.block.Block block, ImmutableMap values) {
            super(block, values);
        }
    }

    /** {@code getSelf}: the state itself (1.16's state classes are generic in their own type). */
    public static IBlockState self(IBlockState state) {
        return state;
    }

    /**
     * 1.16's {@code container.getBaseState()}, for block and fluid states alike: typed as objects, so a fluid's state
     * passes where 1.16 code takes its state container's states (1.12's state containers hold block states).
     */
    public static Object baseState(Object container) {
        if (container instanceof com.michaelsebero.portium.modern.mc.fluid.Fluid.States) {
            return ((com.michaelsebero.portium.modern.mc.fluid.Fluid.States) container).base();
        }
        return ((net.minecraft.block.state.BlockStateContainer) container).getBaseState();
    }

    private StateShims116() {}

    // 1.16's questions of a state that 1.16 answers from the state's cached fields, which 1.12's states don't have:
    // answered by 1.12's state (its block answers; 1.16 blocks through Portium's adapter, from their 1.16 shapes).

    /** 1.16's {@code isFaceSturdy(world, pos, face)}: the face fully covered, as for standing a block on it. */
    public static boolean isFaceSturdy(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos,
                                       net.minecraft.util.EnumFacing face) {
        return state.getBlockFaceShape(world, pos, face) == net.minecraft.block.state.BlockFaceShape.SOLID;
    }

    /**
     * With 1.16's support kind (a class of Minecraft 1.16.5's own, by name): {@code FULL} a whole face, {@code CENTER}
     * a post in the middle (torches and lanterns stand on fences, walls, panes), {@code RIGID} a sturdy edge.
     */
    public static boolean isFaceSturdy(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos,
                                       net.minecraft.util.EnumFacing face, Object kind) {
        net.minecraft.block.state.BlockFaceShape shape = state.getBlockFaceShape(world, pos, face);
        if (shape == net.minecraft.block.state.BlockFaceShape.SOLID) return true;
        if (!"CENTER".equals(kind instanceof Enum ? ((Enum<?>) kind).name() : String.valueOf(kind))) return false;
        switch (shape) {
            case CENTER:
            case CENTER_BIG:
            case CENTER_SMALL:
            case MIDDLE_POLE:
            case MIDDLE_POLE_THICK:
            case MIDDLE_POLE_THIN:
                return true;
            default:
                return false;
        }
    }

    /** 1.16's {@code isTopSolid(world, pos, entity, face)}: the face as the entity stands on it. */
    public static boolean isTopSolid(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos, Object entity,
                                     net.minecraft.util.EnumFacing face) {
        return isFaceSturdy(state, world, pos, face);
    }

    /** 1.16's {@code getFaceOcclusionShape}: a whole face if the state is opaque and covers it, else none. */
    public static com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape faceOcclusionShape(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos,
                                            net.minecraft.util.EnumFacing face) {
        return state.isOpaqueCube() || isFaceSturdy(state, world, pos, face) && state.getMaterial().isOpaque()
                ? com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShapes.fullCube()
                : com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShapes.empty();
    }

    /** 1.16's: whether the collision reaches above the block (fences, walls and gates are 1.5 tall). */
    public static boolean hasLargeCollisionShape(IBlockState state) {
        net.minecraft.block.Block block = state.getBlock();
        return block instanceof net.minecraft.block.BlockFence || block instanceof net.minecraft.block.BlockWall
                || block instanceof net.minecraft.block.BlockFenceGate;
    }

    /** 1.16's: whether light passes by the state's shape (slabs, stairs): 1.12's neighbour brightness. */
    public static boolean useShapeForLightOcclusion(IBlockState state) {
        return state.useNeighborBrightness();
    }

    /** 1.16's emissive rendering (magma, which 1.12 draws lit by its own light). */
    public static boolean emissiveRendering(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return false;
    }

    /** 1.16's: whether redstone power passes through (a whole, solid block). */
    public static boolean isRedstoneConductor(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return state.isNormalCube();
    }

    /** 1.16's hardness; 1.12 asks with a world, which most blocks don't read. */
    public static float destroySpeed(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        try {
            return state.getBlockHardness(world instanceof net.minecraft.world.World ? (net.minecraft.world.World) world : null, pos);
        } catch (RuntimeException e) {
            return 1.0F;
        }
    }

    public static boolean isSolidRender(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return state.isOpaqueCube();
    }

    public static boolean isSuffocating(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return state.causesSuffocation();
    }

    /** 1.16's: whether the state blocks the view from inside it (as suffocating blocks do, by default). */
    public static boolean isViewBlocking(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return state.causesSuffocation();
    }

    /** 1.16's post-processing after generation, which 1.12's generation doesn't do. */
    public static boolean hasPostProcess(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return false;
    }

    public static boolean isCollisionShapeFullBlock(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        net.minecraft.util.math.AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
        return net.minecraft.block.Block.FULL_BLOCK_AABB.equals(box);
    }

    /** 1.16's: whether the block drops only for the right tool (stone, ores): 1.12's material says so. */
    public static boolean requiresCorrectToolForDrops(IBlockState state) {
        return !state.getMaterial().isToolNotRequired();
    }
}
