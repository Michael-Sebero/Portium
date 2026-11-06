package com.michaelsebero.portium.block.light;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.proxy.ParticleKind;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * The soul torch (1.16), on a floor or a wall like 1.12's torch (1.16 splits the wall one off as soul_wall_torch):
 * light 10 and blue flames.
 */
public class BlockSoulTorch extends BlockTorch {

    public BlockSoulTorch() {
        this.setHardness(0.0F);
        this.lightValue = 10;
        this.setSoundType(SoundType.WOOD);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        EnumFacing facing = state.getValue(FACING);
        double x = (double) pos.getX() + 0.5D;
        double y = (double) pos.getY() + 0.7D;
        double z = (double) pos.getZ() + 0.5D;
        if (facing.getAxis().isHorizontal()) {
            EnumFacing back = facing.getOpposite();
            x += 0.27D * (double) back.getXOffset();
            y += 0.22D;
            z += 0.27D * (double) back.getZOffset();
        }
        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0.0D, 0.0D, 0.0D);
        Portium.proxy.spawnParticle(ParticleKind.SOUL_FIRE_FLAME, world, x, y, z, 0.0D, 0.0D, 0.0D);
    }
}
