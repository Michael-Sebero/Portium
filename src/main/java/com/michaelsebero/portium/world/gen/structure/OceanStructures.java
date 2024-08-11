package com.michaelsebero.portium.world.gen.structure;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.world.WorldGeneration;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 1.13's ocean structures: shipwrecks, ocean ruins and buried treasure. They are laid out from the world seed and
 * placed piece by piece as chunks populate, each population only touching its own 16x16 area so no neighbouring chunk
 * is ever forced to generate. They can be found with {@code /locate} and explorer maps.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class OceanStructures {

    public static final OceanStructure SHIPWRECK = new ShipwreckStructure();
    public static final OceanStructure OCEAN_RUIN = new OceanRuinStructure();
    public static final OceanStructure BURIED_TREASURE = new BuriedTreasureStructure();
    private static final List<OceanStructure> ALL = Arrays.asList(SHIPWRECK, OCEAN_RUIN, BURIED_TREASURE);

    /** How many chunks from its start a structure may reach. */
    private static final int REACH = 4;

    private static boolean generatesIn(World world) {
        if (!PortiumConfig.worldGen.oceanStructures || !world.getWorldInfo().isMapFeaturesEnabled() || !WorldGeneration.isModern(world)) return false;
        WorldType type = world.getWorldType();
        return world.provider.getDimensionType() == DimensionType.OVERWORLD && type != WorldType.FLAT && type != WorldType.DEBUG_ALL_BLOCK_STATES;
    }

    /** Before decoration, so kelp, seagrass and coral grow around the structures as in 1.13. */
    @SubscribeEvent
    public static void onPopulate(PopulateChunkEvent.Pre event) {
        World world = event.getWorld();
        if (world.isRemote || !generatesIn(world)) return;
        int chunkX = event.getChunkX();
        int chunkZ = event.getChunkZ();
        StructureBoundingBox area = new StructureBoundingBox((chunkX << 4) + 8, 0, (chunkZ << 4) + 8, (chunkX << 4) + 23, 255, (chunkZ << 4) + 23);
        StructureData data = StructureData.get(world);
        Random random = event.getRand();
        for (OceanStructure structure : ALL) {
            int reach = structure == BURIED_TREASURE ? 0 : REACH;
            for (int regionX = structure.regionOf(chunkX - reach); regionX <= structure.regionOf(chunkX + 1 + reach); regionX++) {
                for (int regionZ = structure.regionOf(chunkZ - reach); regionZ <= structure.regionOf(chunkZ + 1 + reach); regionZ++) {
                    ChunkPos start = structure.getStartInRegion(world.getSeed(), regionX, regionZ);
                    if (Math.abs(start.x - chunkX) > reach + 1 || Math.abs(start.z - chunkZ) > reach + 1) continue;
                    if (!structure.hasStartAt(world, start.x, start.z)) continue;
                    placePieces(world, structure, start, area, data, random);
                }
            }
        }
    }

    private static void placePieces(World world, OceanStructure structure, ChunkPos start, StructureBoundingBox area, StructureData data, Random random) {
        List<StructurePiece> pieces = structure.getPieces(world, start.x, start.z);
        for (int i = 0; i < pieces.size(); i++) {
            StructurePiece piece = pieces.get(i);
            if (!piece.getHorizontalBounds().intersectsWith(area.minX, area.minZ, area.maxX, area.maxZ)) continue;
            int y;
            if (data.hasHeight(structure, start.x, start.z, i)) {
                y = data.getHeight(structure, start.x, start.z, i);
            } else {
                y = piece.resolveY(world, OceanStructure.startRandom(world.getSeed(), start.x, start.z));
                data.setHeight(structure, start.x, start.z, i, y);
            }
            piece.place(world, y, area, random);
        }
    }

    /** The nearest structure of the given {@code /locate} name within 1.13's locate radius, or null. */
    @Nullable
    public static BlockPos findNearest(World world, String name, BlockPos pos) {
        if (!generatesIn(world)) return null;
        for (OceanStructure structure : ALL) {
            if (structure.getName().equals(name)) return structure.findNearest(world, pos, 100);
        }
        return null;
    }

    public static boolean isOurs(String name) {
        for (OceanStructure structure : ALL) {
            if (structure.getName().equals(name)) return true;
        }
        return false;
    }

    /** For dolphins: the nearest start within {@code radius} regions. */
    @Nullable
    public static BlockPos findNearest(World world, OceanStructure structure, BlockPos pos, int radius) {
        return generatesIn(world) ? structure.findNearest(world, pos, radius) : null;
    }

    private OceanStructures() {}
}
