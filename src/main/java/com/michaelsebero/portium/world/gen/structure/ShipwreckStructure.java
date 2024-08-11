package com.michaelsebero.portium.world.gen.structure;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.init.PortiumLootTables;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Shipwrecks (1.13): sunken ships of many shapes on the ocean floor, and half-buried ones on beaches. */
public class ShipwreckStructure extends OceanStructure {

    private static final BlockPos PIVOT = new BlockPos(4, 0, 15);
    private static final String[] BEACHED = {"with_mast", "sideways_full", "sideways_fronthalf", "sideways_backhalf", "rightsideup_full",
            "rightsideup_fronthalf", "rightsideup_backhalf", "with_mast_degraded", "rightsideup_full_degraded", "rightsideup_fronthalf_degraded",
            "rightsideup_backhalf_degraded"};
    private static final String[] OCEAN = {"with_mast", "upsidedown_full", "upsidedown_fronthalf", "upsidedown_backhalf", "sideways_full",
            "sideways_fronthalf", "sideways_backhalf", "rightsideup_full", "rightsideup_fronthalf", "rightsideup_backhalf", "with_mast_degraded",
            "upsidedown_full_degraded", "upsidedown_fronthalf_degraded", "upsidedown_backhalf_degraded", "sideways_full_degraded",
            "sideways_fronthalf_degraded", "sideways_backhalf_degraded", "rightsideup_full_degraded", "rightsideup_fronthalf_degraded",
            "rightsideup_backhalf_degraded"};

    /** Every shipwreck template (the beached ones are a subset of the ocean ones). */
    static final String[] TEMPLATES = OCEAN;

    ShipwreckStructure() {
        super("Shipwreck", 16, 8, 165745295);
    }

    @Override
    protected boolean canSpawnIn(Biome biome) {
        return OceanBiomes.isOcean(biome) || OceanBiomes.isBeach(biome);
    }

    @Override
    protected List<StructurePiece> createPieces(World world, Random random, int chunkX, int chunkZ, Biome biome) {
        Rotation rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
        boolean beached = OceanBiomes.isBeach(biome);
        String name = beached ? BEACHED[random.nextInt(BEACHED.length)] : OCEAN[random.nextInt(OCEAN.length)];
        StructureTemplate template = StructureTemplates.get(new ResourceLocation(Tags.MOD_ID, "shipwreck/" + name));
        if (template == null) return Collections.emptyList();
        return Collections.singletonList(new Piece(template, new BlockPos(chunkX << 4, 90, chunkZ << 4), rotation, beached));
    }

    static class Piece extends StructurePiece {

        private final StructureTemplate template;
        private final BlockPos origin;
        private final Rotation rotation;
        private final boolean beached;

        Piece(StructureTemplate template, BlockPos origin, Rotation rotation, boolean beached) {
            this.template = template;
            this.origin = origin;
            this.rotation = rotation;
            this.beached = beached;
        }

        private StructureTemplate.Settings settings(@Nullable StructureBoundingBox clip) {
            return new StructureTemplate.Settings().rotation(this.rotation).pivot(PIVOT).ignoreAir().clip(clip);
        }

        @Override
        public StructureBoundingBox getHorizontalBounds() {
            return this.template.getBoundingBox(this.origin, this.settings(null));
        }

        /** Rests on the average ocean floor under the ship, or sinks half into the beach. */
        @Override
        public int resolveY(World world, Random random) {
            BlockPos size = this.template.getSize();
            int min = 256;
            int total = 0;
            int columns = 0;
            for (int dx = 0; dx < size.getX(); dx++) {
                for (int dz = 0; dz < size.getZ(); dz++) {
                    int x = this.origin.getX() + dx;
                    int z = this.origin.getZ() + dz;
                    int height = this.beached ? TerrainHeights.surface(world, x, z) : TerrainHeights.oceanFloor(world, x, z);
                    if (height < 0) continue;
                    min = Math.min(min, height);
                    total += height;
                    columns++;
                }
            }
            if (columns == 0) return world.getSeaLevel() - 10;
            return this.beached ? min - size.getY() / 2 - random.nextInt(3) : total / columns;
        }

        @Override
        public void place(World world, int y, StructureBoundingBox clip, Random random) {
            this.template.place(world, new BlockPos(this.origin.getX(), y, this.origin.getZ()), this.settings(clip), (pos, marker) -> {
                ResourceLocation loot;
                switch (marker) {
                    case "map_chest": loot = PortiumLootTables.SHIPWRECK_MAP; break;
                    case "treasure_chest": loot = PortiumLootTables.SHIPWRECK_TREASURE; break;
                    case "supply_chest": loot = PortiumLootTables.SHIPWRECK_SUPPLY; break;
                    default: return;
                }
                TileEntity chest = world.getTileEntity(pos.down());
                if (chest instanceof TileEntityLockableLoot) ((TileEntityLockableLoot) chest).setLootTable(loot, random.nextLong());
            });
        }
    }
}
