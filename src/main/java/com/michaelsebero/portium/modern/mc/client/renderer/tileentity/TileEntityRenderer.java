package com.michaelsebero.portium.modern.mc.client.renderer.tileentity;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

/**
 * 1.13's {@code TileEntityRenderer}, which is 1.12's {@link TileEntitySpecialRenderer} without the alpha argument to
 * {@code render}. 1.13 renderers override the 1.13 {@code render}, which 1.12's calls.
 */
public class TileEntityRenderer<T extends TileEntity> extends TileEntitySpecialRenderer<T> {

    @Srg("func_199341_a")
    public void render(T tileEntity, double x, double y, double z, float partialTicks, int destroyStage) {}

    @Override
    public void render(T tileEntity, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        render(tileEntity, x, y, z, partialTicks, destroyStage);
    }
}
