package com.michaelsebero.portium.block;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.inventory.PortiumGuis;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The smithing table (1.16): upgrades diamond gear to netherite with a netherite ingot (see ContainerSmithing). */
public class BlockSmithingTable extends Block {

    public BlockSmithingTable() {
        super(Material.WOOD, MapColor.WOOD);
        this.setHardness(2.5F);
        this.setSoundType(SoundType.WOOD);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY,
                                    float hitZ) {
        if (!world.isRemote) player.openGui(Portium.instance, PortiumGuis.SMITHING_TABLE, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }
}
