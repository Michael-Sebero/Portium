package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.client.ItemRendering116;
import com.michaelsebero.portium.modern.Srg;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * What a 1.16 mod's item stack renderers extend in place of 1.16's {@code ItemStackTileEntityRenderer}: 1.12's item
 * renderer, whose drawing calls 1.16's {@code render} with the view it's drawn for (as Forge 1.12 last applied it), a
 * 1.16 matrix stack and render buffers drawn by 1.12 ({@link ItemRendering116}). The stack and buffers are classes of
 * Minecraft 1.16.5's own, taken as objects; a renderer's own {@code render} is bridged to this one.
 */
@SideOnly(Side.CLIENT)
public class ItemStackTileEntityRenderer116 extends TileEntityItemStackRenderer {

    /** 1.16's: draws the stack for that view with the stack of matrices into the buffers. */
    @Srg("func_239207_a_")
    public void render(ItemStack stack, ItemCameraTransforms.TransformType view, Object matrixStack, Object buffers, int light, int overlay) {}

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        ItemRendering116.render(this, stack);
    }
}
