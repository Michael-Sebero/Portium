package com.michaelsebero.portium.entity.passive.bee;

import com.michaelsebero.portium.block.plant.BlockPortiumFlower;
import com.michaelsebero.portium.block.plant.BlockSweetBerryBush;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockStem;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.15's flower and bee-growable tags as 1.12 blocks: {@code small_flowers} (dandelions, 1.12's red flowers, 1.14's
 * flowers and wither roses), {@code tall_flowers} (sunflowers, lilacs, rose bushes, peonies) and {@code bee_growables}
 * (crops, melon and pumpkin stems, sweet berry bushes).
 */
public final class BeeFlowers {

    public static boolean isSmallFlower(Block block) {
        return block instanceof BlockFlower || block instanceof BlockPortiumFlower;
    }

    /** A tall flower's either half (the variant read from the lower half). */
    public static boolean isTallFlower(World world, BlockPos pos, IBlockState state) {
        if (!(state.getBlock() instanceof BlockDoublePlant)) return false;
        BlockDoublePlant.EnumPlantType type = state.getActualState(world, pos).getValue(BlockDoublePlant.VARIANT);
        return type != BlockDoublePlant.EnumPlantType.GRASS && type != BlockDoublePlant.EnumPlantType.FERN;
    }

    /** The {@code flowers} tag. */
    public static boolean isFlower(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return isSmallFlower(state.getBlock()) || isTallFlower(world, pos, state);
    }

    /** What a bee pollinates: any flower, but only a sunflower's top. */
    public static boolean canPollinate(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (isSmallFlower(state.getBlock())) return true;
        if (!isTallFlower(world, pos, state)) return false;
        IBlockState actual = state.getActualState(world, pos);
        return actual.getValue(BlockDoublePlant.VARIANT) != BlockDoublePlant.EnumPlantType.SUNFLOWER
                || actual.getValue(BlockDoublePlant.HALF) == BlockDoublePlant.EnumBlockHalf.UPPER;
    }

    /** The {@code flowers} item tag: what bees follow and breed on. */
    public static boolean isFlowerItem(ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof ItemBlock)) return false;
        Block block = ((ItemBlock) item).getBlock();
        if (isSmallFlower(block)) return true;
        if (block instanceof BlockDoublePlant) {
            int meta = stack.getMetadata();
            return meta != BlockDoublePlant.EnumPlantType.GRASS.getMeta() && meta != BlockDoublePlant.EnumPlantType.FERN.getMeta();
        }
        return false;
    }

    /** The age property a passing bee may grow by one, or null. */
    @Nullable
    public static PropertyInteger growableAge(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockCrops) {
            BlockCrops crops = (BlockCrops) block;
            if (crops.isMaxAge(state)) return null;
            for (net.minecraft.block.properties.IProperty<?> property : state.getPropertyKeys()) {
                if (property instanceof PropertyInteger && property.getName().equals("age")) return (PropertyInteger) property;
            }
            return null;
        }
        if (block instanceof BlockStem) return state.getValue(BlockStem.AGE) < 7 ? BlockStem.AGE : null;
        if (block instanceof BlockSweetBerryBush) return state.getValue(BlockSweetBerryBush.AGE) < 3 ? BlockSweetBerryBush.AGE : null;
        return null;
    }

    private BeeFlowers() {}
}
