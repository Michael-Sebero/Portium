package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.arguments.BlockPosArgument;
import com.michaelsebero.portium.command.arguments.BlockPredicateArgument;
import com.michaelsebero.portium.command.arguments.BlockStateArgument;
import com.michaelsebero.portium.command.arguments.BlockStateInput;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code setblock}, {@code fill} and {@code clone} (1.13). */
public final class BlockCommands {

    private static final int MAX_VOLUME = 32768;
    private static final CommandExceptionType.Simple SETBLOCK_FAILED = simple("commands.setblock.failed");
    private static final CommandExceptionType.Simple FILL_FAILED = simple("commands.fill.failed");
    private static final CommandExceptionType.Simple CLONE_FAILED = simple("commands.clone.failed");
    private static final CommandExceptionType.Simple CLONE_OVERLAP = simple("commands.clone.overlap");
    private static final CommandExceptionType.Dynamic2 FILL_TOO_BIG = new CommandExceptionType.Dynamic2((max, count) -> TextMessage.translate("commands.fill.toobig", max, count));
    private static final CommandExceptionType.Dynamic2 CLONE_TOO_BIG = new CommandExceptionType.Dynamic2((max, count) -> TextMessage.translate("commands.clone.toobig", max, count));

    private static CommandExceptionType.Simple simple(String key) {
        return new CommandExceptionType.Simple(TextMessage.translate(key));
    }

    private enum SetMode { REPLACE, DESTROY, KEEP }

    private enum FillMode { REPLACE, OUTLINE, HOLLOW, DESTROY, KEEP }

    private enum CloneMode { FORCE, MOVE, NORMAL }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("setblock").requires(source -> source.hasPermissionLevel(2)).then(argument("pos", BlockPosArgument.blockPos())
                .then(argument("block", BlockStateArgument.blockState())
                        .executes(context -> setBlock(context, SetMode.REPLACE))
                        .then(literal("destroy").executes(context -> setBlock(context, SetMode.DESTROY)))
                        .then(literal("keep").executes(context -> setBlock(context, SetMode.KEEP)))
                        .then(literal("replace").executes(context -> setBlock(context, SetMode.REPLACE))))));

        dispatcher.register(literal("fill").requires(source -> source.hasPermissionLevel(2)).then(argument("from", BlockPosArgument.blockPos())
                .then(argument("to", BlockPosArgument.blockPos()).then(argument("block", BlockStateArgument.blockState())
                        .executes(context -> fill(context, FillMode.REPLACE, null))
                        .then(literal("replace").executes(context -> fill(context, FillMode.REPLACE, null))
                                .then(argument("filter", BlockPredicateArgument.blockPredicate()).executes(context ->
                                        fill(context, FillMode.REPLACE, BlockPredicateArgument.getBlockPredicate(context, "filter")))))
                        .then(literal("keep").executes(context -> fill(context, FillMode.KEEP, null)))
                        .then(literal("outline").executes(context -> fill(context, FillMode.OUTLINE, null)))
                        .then(literal("hollow").executes(context -> fill(context, FillMode.HOLLOW, null)))
                        .then(literal("destroy").executes(context -> fill(context, FillMode.DESTROY, null)))))));

        dispatcher.register(literal("clone").requires(source -> source.hasPermissionLevel(2)).then(argument("begin", BlockPosArgument.blockPos())
                .then(argument("end", BlockPosArgument.blockPos()).then(argument("destination", BlockPosArgument.blockPos())
                        .executes(context -> clone(context, false, null, CloneMode.NORMAL))
                        .then(literal("replace").executes(context -> clone(context, false, null, CloneMode.NORMAL))
                                .then(literal("force").executes(context -> clone(context, false, null, CloneMode.FORCE)))
                                .then(literal("move").executes(context -> clone(context, false, null, CloneMode.MOVE)))
                                .then(literal("normal").executes(context -> clone(context, false, null, CloneMode.NORMAL))))
                        .then(literal("masked").executes(context -> clone(context, true, null, CloneMode.NORMAL))
                                .then(literal("force").executes(context -> clone(context, true, null, CloneMode.FORCE)))
                                .then(literal("move").executes(context -> clone(context, true, null, CloneMode.MOVE)))
                                .then(literal("normal").executes(context -> clone(context, true, null, CloneMode.NORMAL))))
                        .then(literal("filtered").then(argument("filter", BlockPredicateArgument.blockPredicate())
                                .executes(context -> clone(context, false, BlockPredicateArgument.getBlockPredicate(context, "filter"), CloneMode.NORMAL))
                                .then(literal("force").executes(context -> clone(context, false, BlockPredicateArgument.getBlockPredicate(context, "filter"), CloneMode.FORCE)))
                                .then(literal("move").executes(context -> clone(context, false, BlockPredicateArgument.getBlockPredicate(context, "filter"), CloneMode.MOVE)))
                                .then(literal("normal").executes(context -> clone(context, false, BlockPredicateArgument.getBlockPredicate(context, "filter"), CloneMode.NORMAL)))))))));
    }

    /** Empties a container before it is replaced, so its contents don't spill. */
    private static void clear(@Nullable TileEntity tile) {
        if (tile instanceof IInventory) ((IInventory) tile).clear();
    }

    private static int setBlock(CommandContext<CommandSource> context, SetMode mode) throws CommandSyntaxException {
        WorldServer world = context.getSource().getWorld();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        BlockStateInput block = BlockStateArgument.getBlockState(context, "block");
        if (mode == SetMode.KEEP && !world.isAirBlock(pos)) throw SETBLOCK_FAILED.create();
        if (mode == SetMode.DESTROY) world.destroyBlock(pos, true);
        clear(world.getTileEntity(pos));
        if (!block.place(world, pos, 2)) throw SETBLOCK_FAILED.create();
        world.notifyNeighborsRespectDebug(pos, block.getState().getBlock(), false);
        context.getSource().sendFeedback(new TextComponentTranslation("commands.setblock.success", pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }

    private static int fill(CommandContext<CommandSource> context, FillMode mode, @Nullable BlockPredicateArgument.Result filter) throws CommandSyntaxException {
        WorldServer world = context.getSource().getWorld();
        StructureBoundingBox box = new StructureBoundingBox(BlockPosArgument.getLoadedBlockPos(context, "from"), BlockPosArgument.getLoadedBlockPos(context, "to"));
        int volume = box.getXSize() * box.getYSize() * box.getZSize();
        if (volume > MAX_VOLUME) throw FILL_TOO_BIG.create(MAX_VOLUME, volume);
        BlockStateInput block = BlockStateArgument.getBlockState(context, "block");
        BlockStateInput air = new BlockStateInput(Blocks.AIR.getDefaultState(), null);
        List<BlockPos> changed = new ArrayList<>();
        for (BlockPos.MutableBlockPos mutable : BlockPos.getAllInBoxMutable(new BlockPos(box.minX, box.minY, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))) {
            BlockPos pos = mutable.toImmutable();
            if (filter != null && !filter.test(world, pos)) continue;
            boolean edge = pos.getX() == box.minX || pos.getX() == box.maxX || pos.getY() == box.minY || pos.getY() == box.maxY
                    || pos.getZ() == box.minZ || pos.getZ() == box.maxZ;
            BlockStateInput place = block;
            switch (mode) {
                case OUTLINE: if (!edge) continue; break;
                case HOLLOW: if (!edge) place = air; break;
                case KEEP: if (!world.isAirBlock(pos)) continue; break;
                case DESTROY: world.destroyBlock(pos, true); break;
                default: break;
            }
            clear(world.getTileEntity(pos));
            if (place.place(world, pos, 2)) changed.add(pos);
        }
        for (BlockPos pos : changed) world.notifyNeighborsRespectDebug(pos, world.getBlockState(pos).getBlock(), false);
        if (changed.isEmpty()) throw FILL_FAILED.create();
        context.getSource().sendFeedback(new TextComponentTranslation("commands.fill.success", changed.size()), true);
        return changed.size();
    }

    private static final class Copied {

        final BlockPos target;
        final IBlockState state;
        @Nullable
        final NBTTagCompound tile;
        final boolean waterlogged;

        Copied(BlockPos target, IBlockState state, @Nullable NBTTagCompound tile, boolean waterlogged) {
            this.target = target;
            this.state = state;
            this.tile = tile;
            this.waterlogged = waterlogged;
        }
    }

    private static int clone(CommandContext<CommandSource> context, boolean masked, @Nullable BlockPredicateArgument.Result filter, CloneMode mode) throws CommandSyntaxException {
        WorldServer world = context.getSource().getWorld();
        BlockPos begin = BlockPosArgument.getLoadedBlockPos(context, "begin");
        BlockPos end = BlockPosArgument.getLoadedBlockPos(context, "end");
        BlockPos destination = BlockPosArgument.getLoadedBlockPos(context, "destination");
        StructureBoundingBox source = new StructureBoundingBox(begin, end);
        BlockPos offset = new BlockPos(destination.getX() - source.minX, destination.getY() - source.minY, destination.getZ() - source.minZ);
        StructureBoundingBox target = new StructureBoundingBox(destination, destination.add(source.getXSize() - 1, source.getYSize() - 1, source.getZSize() - 1));
        int volume = source.getXSize() * source.getYSize() * source.getZSize();
        if (volume > MAX_VOLUME) throw CLONE_TOO_BIG.create(MAX_VOLUME, volume);
        if (mode != CloneMode.FORCE && mode != CloneMode.MOVE && source.intersectsWith(target)) throw CLONE_OVERLAP.create();
        if (!world.isAreaLoaded(new BlockPos(target.minX, target.minY, target.minZ), new BlockPos(target.maxX, target.maxY, target.maxZ))) {
            throw com.michaelsebero.portium.command.CommandExceptions.POS_UNLOADED.create();
        }
        // Solid blocks first, then blocks that hang off others (torches, ladders), then the block entities' data.
        List<Copied> solid = new ArrayList<>();
        List<Copied> attached = new ArrayList<>();
        List<BlockPos> moved = new ArrayList<>();
        for (int z = source.minZ; z <= source.maxZ; ++z) {
            for (int y = source.minY; y <= source.maxY; ++y) {
                for (int x = source.minX; x <= source.maxX; ++x) {
                    BlockPos pos = new BlockPos(x, y, z);
                    IBlockState state = world.getBlockState(pos);
                    if (masked && state.getBlock() == Blocks.AIR) continue;
                    if (filter != null && !filter.test(world, pos)) continue;
                    TileEntity tile = world.getTileEntity(pos);
                    NBTTagCompound data = tile == null ? null : tile.writeToNBT(new NBTTagCompound());
                    Copied copy = new Copied(pos.add(offset), state, data, Waterlogging.isWaterlogged(world, pos));
                    if (data == null && (state.isFullBlock() || state.isFullCube())) solid.add(copy);
                    else attached.add(copy);
                    moved.add(pos);
                }
            }
        }
        if (mode == CloneMode.MOVE) {
            for (BlockPos pos : moved) {
                clear(world.getTileEntity(pos));
                world.setBlockState(pos, Blocks.BARRIER.getDefaultState(), 2);
            }
            for (BlockPos pos : moved) world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }
        List<Copied> all = new ArrayList<>(solid);
        all.addAll(attached);
        for (int i = all.size() - 1; i >= 0; i--) {
            Copied copy = all.get(i);
            clear(world.getTileEntity(copy.target));
            world.setBlockState(copy.target, Blocks.BARRIER.getDefaultState(), 2);
        }
        int count = 0;
        for (Copied copy : all) {
            boolean placed = world.setBlockState(copy.target, copy.state, 2);
            if (Waterlogging.canWaterlog(copy.state) && Waterlogging.setWaterlogged(world, copy.target, copy.waterlogged) || placed) count++;
        }
        for (Copied copy : all) {
            if (copy.tile == null) continue;
            TileEntity tile = world.getTileEntity(copy.target);
            if (tile == null) continue;
            NBTTagCompound data = copy.tile.copy();
            data.setInteger("x", copy.target.getX());
            data.setInteger("y", copy.target.getY());
            data.setInteger("z", copy.target.getZ());
            tile.readFromNBT(data);
            tile.markDirty();
        }
        for (Copied copy : all) world.notifyNeighborsRespectDebug(copy.target, copy.state.getBlock(), false);
        if (count == 0) throw CLONE_FAILED.create();
        context.getSource().sendFeedback(new TextComponentTranslation("commands.clone.success", count), true);
        return count;
    }

    private BlockCommands() {}
}
