package com.michaelsebero.portium.world.gen.jigsaw;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.block.jigsaw.BlockJigsaw;
import com.michaelsebero.portium.tile.TileEntityJigsaw;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.List;
import java.util.Random;

/**
 * The jigsaw screen's Generate button (1.16): lays out a structure from the block's pool, as many levels deep as
 * asked, starting in front of the block, and places it, keeping its jigsaw blocks if asked. Pools are 1.16.5's own
 * (Portium reads no data pack pools).
 */
public final class JigsawGenerate {

    public static boolean generate(WorldServer world, BlockPos pos, int levels, boolean keepJigsaws) {
        if (!(world.getTileEntity(pos) instanceof TileEntityJigsaw)) return false;
        TileEntityJigsaw jigsaw = (TileEntityJigsaw) world.getTileEntity(pos);
        JigsawData data = JigsawData.get();
        if (data == null || data.pools().getPool(jigsaw.getPool()) == null) {
            Portium.LOGGER.info("Jigsaw at {}: no pool {} to generate from", pos, jigsaw.getPool());
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        BlockPos start = state.getBlock() instanceof BlockJigsaw ? pos.offset(state.getValue(BlockJigsaw.ORIENTATION).front) : pos.up();
        List<JigsawAssembler.Placed> layout = JigsawAssembler.assemble(data, new HeightSampler(world), jigsaw.getPool(), start, Math.max(0, levels), false,
                true, new Random(world.rand.nextLong()));
        if (layout.isEmpty()) return false;
        StructureBoundingBox all = StructureBoundingBox.getNewBoundingBox();
        for (JigsawAssembler.Placed piece : layout) all.expandTo(piece.box);
        HeightSampler heights = new HeightSampler(world);
        JigsawPlacer.keepJigsaws = keepJigsaws;
        try {
            for (JigsawAssembler.Placed piece : layout) JigsawPlacer.place(data, piece, world, all, heights);
        } finally {
            JigsawPlacer.keepJigsaws = false;
        }
        Portium.LOGGER.info("Jigsaw at {}: generated {} pieces from {}", pos, layout.size(), jigsaw.getPool());
        return true;
    }

    private JigsawGenerate() {}
}
