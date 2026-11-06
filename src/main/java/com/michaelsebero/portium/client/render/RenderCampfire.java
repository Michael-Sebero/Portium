package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.block.light.BlockCampfire;
import com.michaelsebero.portium.tile.TileEntityCampfire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;

/** The food cooking on a campfire, lying flat on its logs, one item per side (1.14's CampfireTileEntityRenderer). */
public class RenderCampfire extends TileEntitySpecialRenderer<TileEntityCampfire> {

    @Override
    public void render(TileEntityCampfire campfire, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        IBlockState state = campfire.getWorld().getBlockState(campfire.getPos());
        if (!(state.getBlock() instanceof BlockCampfire)) return;
        EnumFacing facing = state.getValue(BlockCampfire.FACING);
        NonNullList<ItemStack> items = campfire.getInventory();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5D, y + 0.44921875D, z + 0.5D);
            EnumFacing side = EnumFacing.byHorizontalIndex((i + facing.getHorizontalIndex()) % 4);
            GlStateManager.rotate(-side.getHorizontalAngle(), 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.translate(-0.3125D, -0.3125D, 0.0D);
            GlStateManager.scale(0.375F, 0.375F, 0.375F);
            Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
            GlStateManager.popMatrix();
        }
    }
}
