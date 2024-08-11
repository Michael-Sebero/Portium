package com.michaelsebero.portium.world.waterlog;

import com.michaelsebero.portium.Tags;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** How players waterlog blocks, as in 1.13: with buckets, or by placing a block into a water source. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class WaterlogEvents {

    /** A water bucket fills the block it's used on; an empty bucket takes the water back out. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFillBucket(FillBucketEvent event) {
        RayTraceResult target = event.getTarget();
        if (!Waterlogging.isEnabled() || event.getResult() != Event.Result.DEFAULT || target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) return;
        World world = event.getWorld();
        BlockPos pos = target.getBlockPos();
        ItemStack held = event.getEmptyBucket();
        if (held.getItem() == Items.BUCKET && Waterlogging.isWaterlogged(world, pos)) {
            Waterlogging.setWaterlogged(world, pos, false);
            event.getEntityPlayer().playSound(SoundEvents.ITEM_BUCKET_FILL, 1.0F, 1.0F);
            event.setFilledBucket(new ItemStack(Items.WATER_BUCKET));
            event.setResult(Event.Result.ALLOW);
        } else if (held.getItem() == Items.WATER_BUCKET && Waterlogging.setWaterlogged(world, pos, true)) {
            event.getEntityPlayer().playSound(SoundEvents.ITEM_BUCKET_EMPTY, 1.0F, 1.0F);
            event.setFilledBucket(new ItemStack(Items.BUCKET));
            event.setResult(Event.Result.ALLOW);
        }
    }

    /** A block placed into a still water source keeps the water. */
    @SubscribeEvent
    public static void onPlace(BlockEvent.PlaceEvent event) {
        IBlockState replaced = event.getBlockSnapshot().getReplacedBlock();
        if (replaced.getMaterial() == Material.WATER && replaced.getBlock() instanceof BlockLiquid && replaced.getValue(BlockLiquid.LEVEL) == 0) {
            Waterlogging.setWaterlogged(event.getWorld(), event.getPos(), true);
        }
    }

    private WaterlogEvents() {}
}
