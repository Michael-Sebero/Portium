package com.michaelsebero.portium.block.nether;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLogic;

/** 1.16's Nether materials, which unlike their Overworld kin never burn. */
public final class NetherMaterials {

    /** Crimson and warped stems, hyphae and planks: wood that lava and fire can't light. */
    public static final Material NETHER_WOOD = new Material(MapColor.WOOD);

    /** Roots and sprouts: like grass, replaceable and pushed aside, but not flammable. */
    public static final Material NETHER_PLANTS = new Plants();

    private static final class Plants extends MaterialLogic {

        Plants() {
            super(MapColor.FOLIAGE);
            this.setReplaceable();
            this.setNoPushMobility();
        }
    }

    private NetherMaterials() {}
}
