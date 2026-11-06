package com.michaelsebero.portium.block.plant;

import com.michaelsebero.portium.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** Portium's plants in 1.12's flower pot: an empty pot given one becomes its {@link BlockPortiumFlowerPot}. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class FlowerPots {

    private static final Map<Block, Block> POTTED = new HashMap<>();

    /** Called as Portium registers a potted plant. */
    public static void register(Block plant, BlockPortiumFlowerPot potted) {
        POTTED.put(plant, potted);
    }

    @Nullable
    public static Block pottedOf(ItemStack stack) {
        return stack.isEmpty() ? null : POTTED.get(Block.getBlockFromItem(stack.getItem()));
    }

    /** Whether the item goes in a flower pot: one of Portium's plants, or one 1.12's pot takes. */
    public static boolean isPottable(ItemStack stack) {
        if (pottedOf(stack) != null) return true;
        Block block = Block.getBlockFromItem(stack.getItem());
        return block == Blocks.YELLOW_FLOWER || block == Blocks.RED_FLOWER || block == Blocks.CACTUS || block == Blocks.BROWN_MUSHROOM
                || block == Blocks.RED_MUSHROOM || block == Blocks.SAPLING || block == Blocks.DEADBUSH
                || block == Blocks.TALLGRASS && stack.getMetadata() == BlockTallGrass.EnumType.FERN.getMeta();
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Block potted = pottedOf(event.getItemStack());
        if (potted == null) return;
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != Blocks.FLOWER_POT) return;
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityFlowerPot && !((TileEntityFlowerPot) tile).getFlowerItemStack().isEmpty()) return;
        EntityPlayer player = event.getEntityPlayer();
        world.setBlockState(pos, potted.getDefaultState(), 3);
        player.addStat(StatList.FLOWER_POTTED);
        if (!player.capabilities.isCreativeMode) event.getItemStack().shrink(1);
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    private FlowerPots() {}
}
