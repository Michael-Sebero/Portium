package com.michaelsebero.portium.block.nether;

import com.michaelsebero.portium.block.building.BlockSimple;
import com.michaelsebero.portium.init.PortiumSoundTypes;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** The block of netherite (1.16): diamond-pickaxe hard, blast-proof as obsidian, and a beacon base. */
public class BlockNetherite extends BlockSimple {

    public BlockNetherite() {
        super(Material.IRON, MapColor.BLACK, PortiumSoundTypes.NETHERITE);
        this.setHardness(50.0F);
        this.setResistance(2000.0F);
        this.setHarvestLevel("pickaxe", 3);
    }

    @Override
    public boolean isBeaconBase(IBlockAccess world, BlockPos pos, BlockPos beacon) {
        return true;
    }
}
