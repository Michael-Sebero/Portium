package com.michaelsebero.portium.world.waterlog;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.mixin.block.BlockDynamicLiquidInvoker;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Water flowing out of waterlogged blocks. A waterlogged cell acts as a still source: it flows down if it can,
 * otherwise sideways the way 1.12's water finds its way, but only through faces of its block that aren't solid.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class WaterlogFlow {

    /** Water's tick rate. */
    private static final int DELAY = 5;
    private static final Map<World, Map<BlockPos, Long>> PENDING = new WeakHashMap<>();

    static void schedule(World world, BlockPos pos) {
        PENDING.computeIfAbsent(world, key -> new HashMap<>()).putIfAbsent(pos.toImmutable(), world.getTotalWorldTime() + DELAY);
    }

    /** A cell whose neighbours changed may now have somewhere to flow. */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        World world = event.getWorld();
        if (world.isRemote || !Waterlogging.isEnabled()) return;
        BlockPos pos = event.getPos();
        if (Waterlogging.isWaterlogged(world, pos)) schedule(world, pos);
        for (EnumFacing side : event.getNotifiedSides()) {
            BlockPos neighbor = pos.offset(side);
            if (Waterlogging.isWaterlogged(world, neighbor)) schedule(world, neighbor);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;
        Map<BlockPos, Long> pending = PENDING.get(event.world);
        if (pending == null || pending.isEmpty()) return;
        long now = event.world.getTotalWorldTime();
        List<BlockPos> due = new ArrayList<>();
        for (Iterator<Map.Entry<BlockPos, Long>> it = pending.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Long> entry = it.next();
            if (entry.getValue() > now) continue;
            due.add(entry.getKey());
            it.remove();
        }
        for (BlockPos pos : due) {
            if (event.world.isBlockLoaded(pos)) flow(event.world, pos);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        PENDING.remove(event.getWorld());
    }

    private static void flow(World world, BlockPos pos) {
        if (!Waterlogging.isWaterlogged(world, pos)) return;
        IBlockState state = world.getBlockState(pos);
        BlockDynamicLiquidInvoker water = (BlockDynamicLiquidInvoker) Blocks.FLOWING_WATER;
        BlockPos below = pos.down();
        IBlockState belowState = world.getBlockState(below);
        if (isOpen(world, pos, state, EnumFacing.DOWN) && water.portium$canFlowInto(world, below, belowState)) {
            water.portium$tryFlowInto(world, below, belowState, 8);
            return;
        }
        for (EnumFacing side : water.portium$getPossibleFlowDirections(world, pos)) {
            if (!isOpen(world, pos, state, side)) continue;
            BlockPos next = pos.offset(side);
            water.portium$tryFlowInto(world, next, world.getBlockState(next), 1);
        }
    }

    /** Water passes through any face of a block that isn't a full solid face. */
    static boolean isOpen(IBlockAccess world, BlockPos pos, IBlockState state, EnumFacing face) {
        return state.getBlockFaceShape(world, pos, face) != BlockFaceShape.SOLID;
    }

    private WaterlogFlow() {}
}
