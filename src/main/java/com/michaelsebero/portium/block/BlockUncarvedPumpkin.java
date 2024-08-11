package com.michaelsebero.portium.block;

import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The 1.13 uncarved pumpkin. Shears carve it into vanilla's pumpkin block, which in 1.12 is the carved one,
 * and drop four seeds. Pumpkin stems and pumpkin patches produce this block (see the stem and world gen mixins).
 */
public class BlockUncarvedPumpkin extends Block {

    public BlockUncarvedPumpkin() {
        super(Material.GOURD, MapColor.ADOBE);
        this.setHardness(1.0F);
        this.setSoundType(SoundType.WOOD);
        this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    /** Endermen can carry blocks only once registered; call after registration. */
    public void allowEndermen() {
        EntityEnderman.setCarriable(this, true);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (!(held.getItem() instanceof ItemShears)) return false;
        if (!world.isRemote) {
            EnumFacing face = side.getAxis() == EnumFacing.Axis.Y ? player.getHorizontalFacing().getOpposite() : side;
            world.playSound(null, pos, PortiumSounds.BLOCK_PUMPKIN_CARVE, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.setBlockState(pos, Blocks.PUMPKIN.getDefaultState().withProperty(BlockHorizontal.FACING, face), 11);
            EntityItem seeds = new EntityItem(world, pos.getX() + 0.5 + face.getXOffset() * 0.65, pos.getY() + 0.1, pos.getZ() + 0.5 + face.getZOffset() * 0.65, new ItemStack(Items.PUMPKIN_SEEDS, 4));
            seeds.motionX = 0.05 * face.getXOffset() + world.rand.nextDouble() * 0.02;
            seeds.motionY = 0.05;
            seeds.motionZ = 0.05 * face.getZOffset() + world.rand.nextDouble() * 0.02;
            world.spawnEntity(seeds);
            held.damageItem(1, player);
        }
        return true;
    }
}
