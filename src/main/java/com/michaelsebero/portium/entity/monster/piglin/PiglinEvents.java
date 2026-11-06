package com.michaelsebero.portium.entity.monster.piglin;

import com.michaelsebero.portium.Tags;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.16: piglins guard gold and containers. Opening a chest, barrel, ender chest or shulker box they can see, or
 * breaking anything in the {@code guarded_by_piglins} tag near them, angers them.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PiglinEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOpen(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote || event.isCanceled() || player.isSneaking() && !player.getHeldItemMainhand().isEmpty()) return;
        IBlockState state = player.world.getBlockState(event.getPos());
        if (!PiglinTags.isGuardedByPiglins(state)) return;
        TileEntity tile = player.world.getTileEntity(event.getPos());
        if (tile instanceof IInventory || tile instanceof TileEntityEnderChest) PiglinTasks.angerNearbyPiglins(player, true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || event.getWorld().isRemote || event.getPlayer() == null) return;
        if (PiglinTags.isGuardedByPiglins(event.getState())) PiglinTasks.angerNearbyPiglins(event.getPlayer(), false);
    }

    private PiglinEvents() {}
}
