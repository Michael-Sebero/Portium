package com.michaelsebero.portium.block.workstation;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.init.PortiumStats;
import com.michaelsebero.portium.inventory.PortiumGuis;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The cartography table (1.14): copies, zooms out and locks maps. */
public class BlockCartographyTable extends Block {

    public BlockCartographyTable() {
        super(Material.WOOD);
        this.setHardness(2.5F);
        this.setSoundType(SoundType.WOOD);
        this.setHarvestLevel("axe", 0);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
                                    float hitY, float hitZ) {
        if (world.isRemote) return true;
        player.openGui(Portium.instance, PortiumGuis.CARTOGRAPHY_TABLE, world, pos.getX(), pos.getY(), pos.getZ());
        player.addStat(PortiumStats.INTERACT_WITH_CARTOGRAPHY_TABLE);
        return true;
    }
}
