package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.client.model.ModelTrident;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Draws the 3D trident and the conduit shell for their held and inventory item models. */
public class PortiumItemStackRenderer extends TileEntityItemStackRenderer {

    private final ModelTrident trident = new ModelTrident();

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        Item item = stack.getItem();
        if (item == PortiumItems.TRIDENT) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(RenderTrident.TEXTURE);
            GlStateManager.pushMatrix();
            GlStateManager.scale(1.0F, -1.0F, -1.0F);
            this.trident.render();
            if (stack.hasEffect()) GlintRenderer.render(this.trident::render);
            GlStateManager.popMatrix();
        } else if (item == Item.getItemFromBlock(PortiumBlocks.CONDUIT)) {
            RenderConduit.renderInactiveShell(0.0, 0.0, 0.0);
        }
    }
}
