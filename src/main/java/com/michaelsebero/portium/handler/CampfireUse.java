package com.michaelsebero.portium.handler;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.block.light.BlockCampfire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * What items do to campfires (1.14), as 1.14's items do it in their own use code, so also while sneaking: flint and
 * steel and fire charges light an unlit campfire (instead of setting fire beside it), shovels put a lit one out.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class CampfireUse {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockCampfire)) return;
        EntityPlayer player = event.getEntityPlayer();
        EnumFacing face = event.getFace() != null ? event.getFace() : EnumFacing.UP;
        if (!player.canPlayerEdit(pos, face, stack)) return;
        boolean used;
        if (stack.getItem() == Items.FLINT_AND_STEEL) {
            used = BlockCampfire.canBeLit(world, pos, state);
            if (used) {
                world.playSound(player, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, world.rand.nextFloat() * 0.4F + 0.8F);
                world.setBlockState(pos, state.withProperty(BlockCampfire.LIT, true), 11);
                stack.damageItem(1, player);
            }
        } else if (stack.getItem() == Items.FIRE_CHARGE) {
            used = BlockCampfire.canBeLit(world, pos, state);
            if (used) {
                world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0F,
                        (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F + 1.0F);
                world.setBlockState(pos, state.withProperty(BlockCampfire.LIT, true), 3);
                if (!player.capabilities.isCreativeMode) stack.shrink(1);
            }
        } else if (stack.getItem().getToolClasses(stack).contains("shovel")) {
            used = face != EnumFacing.DOWN && state.getValue(BlockCampfire.LIT);
            if (used) {
                if (!world.isRemote) world.playEvent(null, 1009, pos, 0);
                BlockCampfire.extinguish(world, pos, state);
                if (!world.isRemote) {
                    world.setBlockState(pos, state.withProperty(BlockCampfire.LIT, false), 11);
                    stack.damageItem(1, player);
                }
            }
        } else {
            return;
        }
        if (used) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }

    private CampfireUse() {}
}
