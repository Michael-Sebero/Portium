package com.michaelsebero.portium.mixin.item;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.item.LodestoneCompass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemCompass;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;

/** The compass's 1.16 lodestone behaviour: locking onto a lodestone, its name and glint (see {@link LodestoneCompass}). */
@Mixin(ItemCompass.class)
public abstract class MixinItemCompass extends Item {

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (PortiumBlocks.LODESTONE == null || world.getBlockState(pos).getBlock() != PortiumBlocks.LODESTONE) {
            return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
        }
        return LodestoneCompass.onItemUse(player, world, pos, hand);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        LodestoneCompass.inventoryTick(stack, world);
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return LodestoneCompass.hasLodestone(stack) ? "item.portium.lodestone_compass" : super.getTranslationKey(stack);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean hasEffect(ItemStack stack) {
        return LodestoneCompass.hasLodestone(stack) || super.hasEffect(stack);
    }
}
