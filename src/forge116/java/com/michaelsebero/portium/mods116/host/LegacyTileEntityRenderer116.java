package com.michaelsebero.portium.mods116.host;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;

/**
 * A 1.12 tile entity renderer (vanilla's chest, or an older mod's) as 1.16 code asks for one, as Create does for the
 * tile entities on a contraption: 1.12's draws under the matrix stack's pose.
 */
public final class LegacyTileEntityRenderer116 extends TileEntityRenderer<TileEntity> {

    private final Object legacy;

    public LegacyTileEntityRenderer116(TileEntityRendererDispatcher dispatcher, Object legacy) {
        super(dispatcher);
        this.legacy = legacy;
    }

    @Override
    public void func_225616_a_(TileEntity tile, float partialTicks, MatrixStack matrices, IRenderTypeBuffer buffers, int light, int overlay) {
        com.michaelsebero.portium.client.TileEntityRendering116.renderLegacy(this.legacy, tile, partialTicks, RenderGlue.pose(matrices), light);
    }
}
