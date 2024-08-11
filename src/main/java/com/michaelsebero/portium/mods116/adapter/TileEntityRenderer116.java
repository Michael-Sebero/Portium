package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.client.TileEntityRendering116;
import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * What a 1.16 mod's tile entity renderers extend in place of 1.16's {@code TileEntityRenderer}: 1.12's special
 * renderer, whose drawing calls 1.16's {@code render} with a 1.16 matrix stack at the tile entity and render buffers
 * drawn by 1.12 ({@link TileEntityRendering116}). The stack and buffers are classes of Minecraft 1.16.5's own,
 * taken as objects; a renderer's own {@code render} is bridged to this one.
 */
@SideOnly(Side.CLIENT)
public class TileEntityRenderer116<T extends TileEntity> extends TileEntityRenderer<T> {

    @Srg("field_228858_b_")
    protected final TileEntityRendererDispatcher renderDispatcher;

    public TileEntityRenderer116(TileEntityRendererDispatcher dispatcher) {
        this.renderDispatcher = dispatcher;
    }

    /** 1.16's: draws the tile entity with the stack (at it) into the buffers, at that light and overlay. */
    @Srg("func_225616_a_")
    public void render(T tileEntity, float partialTicks, Object matrixStack, Object buffers, int light, int overlay) {}

    @Override
    public void render(T tileEntity, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        TileEntityRendering116.render(this, tileEntity, x, y, z, partialTicks);
    }
}
