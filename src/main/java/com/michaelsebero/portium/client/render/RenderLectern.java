package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.block.village.BlockLectern;
import com.michaelsebero.portium.tile.TileEntityLectern;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.model.ModelBook;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;

/** The open book lying on a lectern (1.14's LecternTileEntityRenderer), drawn with the enchanting table's book. */
public class RenderLectern extends TileEntitySpecialRenderer<TileEntityLectern> {

    private static final ResourceLocation TEXTURE_BOOK = new ResourceLocation("textures/entity/enchanting_table_book.png");
    private final ModelBook book = new ModelBook();

    @Override
    public void render(TileEntityLectern lectern, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        IBlockState state = lectern.getWorld().getBlockState(lectern.getPos());
        if (!(state.getBlock() instanceof BlockLectern) || !state.getValue(BlockLectern.HAS_BOOK)) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y + 1.0625D, z + 0.5D);
        float angle = state.getValue(BlockLectern.FACING).rotateY().getHorizontalAngle();
        GlStateManager.rotate(-angle, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(67.5F, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(0.0D, -0.125D, 0.0D);
        this.bindTexture(TEXTURE_BOOK);
        GlStateManager.enableCull();
        this.book.render(null, 0.0F, 0.1F, 0.9F, 1.2F, 0.0F, 0.0625F);
        GlStateManager.popMatrix();
    }
}
