package com.michaelsebero.portium.world.gen.carver;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.block.BlockBubbleColumn;
import com.michaelsebero.portium.world.WorldGeneration;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Adds 1.13's underwater caves and canyons after the normal ones, and starts bubble columns over the magma they leave
 * on their floors (1.13 schedules a tick on that magma; 1.12's chunk generation can't, so it's done when the chunk
 * populates).
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class UnderwaterCarvers {

    /** The map generator events fire on the terrain bus. */
    public static void registerTerrainEvents() {
        MinecraftForge.TERRAIN_GEN_BUS.register(new TerrainBus());
    }

    public static final class TerrainBus {

        @SubscribeEvent
        public void onInitMapGen(InitMapGenEvent event) {
            if (!PortiumConfig.worldGen.underwaterCaves) return;
            if (event.getType() == InitMapGenEvent.EventType.CAVE) event.setNewGen(new Combined(event.getNewGen(), new MapGenUnderwaterCaves()));
            else if (event.getType() == InitMapGenEvent.EventType.RAVINE) event.setNewGen(new Combined(event.getNewGen(), new MapGenUnderwaterRavine()));
        }
    }

    /** Runs the existing carver, then the underwater one. */
    private static final class Combined extends MapGenBase {

        private final MapGenBase first;
        private final MapGenBase second;

        Combined(MapGenBase first, MapGenBase second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public void generate(World world, int x, int z, ChunkPrimer primer) {
            this.first.generate(world, x, z, primer);
            if (WorldGeneration.isModern(world)) this.second.generate(world, x, z, primer);
        }
    }

    @SubscribeEvent
    public static void onPopulate(PopulateChunkEvent.Post event) {
        if (!PortiumConfig.worldGen.underwaterCaves || event.getWorld().isRemote || !WorldGeneration.isModern(event.getWorld())) return;
        World world = event.getWorld();
        // The area this population owns, as 1.12's decorators do: offset by 8, so every change (and the neighbour
        // updates it sends) stays inside chunks that are already loaded. These areas cover each column once.
        int baseX = (event.getChunkX() << 4) + 8;
        int baseZ = (event.getChunkZ() << 4) + 8;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                pos.setPos(baseX + dx, UnderwaterCarving.FLOOR_Y, baseZ + dz);
                if (world.getBlockState(pos).getBlock() == Blocks.MAGMA) BlockBubbleColumn.placeBubbleColumn(world, pos.up(), true);
            }
        }
    }

    private UnderwaterCarvers() {}
}
