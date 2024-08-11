package com.michaelsebero.portium.world.gen.structure;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.Random;

/**
 * One part of a structure. Pieces are recreated from the world seed whenever needed, so the only state worth saving
 * is the height a piece settled at, which depends on the terrain when it was first placed.
 */
public abstract class StructurePiece {

    /** Where the piece may place blocks, horizontally. The vertical extent isn't known until the piece is placed. */
    public abstract StructureBoundingBox getHorizontalBounds();

    /** Works out the piece's height from the terrain, looking only at loaded chunks. */
    public abstract int resolveY(World world, Random random);

    /** Places the part of the piece inside {@code clip}, with its origin at height {@code y}. */
    public abstract void place(World world, int y, StructureBoundingBox clip, Random random);
}
