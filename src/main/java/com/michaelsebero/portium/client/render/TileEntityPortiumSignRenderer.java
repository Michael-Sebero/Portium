package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.block.sign.IPortiumSign;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/** 1.12's sign renderer, binding the sign's wood texture where 1.12 binds its one sign texture. */
@SideOnly(Side.CLIENT)
public class TileEntityPortiumSignRenderer extends TileEntitySignRenderer {

    private static final ResourceLocation VANILLA = new ResourceLocation("textures/entity/sign.png");
    @Nullable
    private ResourceLocation texture;

    @Override
    public void render(TileEntitySign sign, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        Block block = sign.getBlockType();
        this.texture = block instanceof IPortiumSign ? ((IPortiumSign) block).getSignTexture() : null;
        try {
            super.render(sign, x, y, z, partialTicks, destroyStage, alpha);
        } finally {
            this.texture = null;
        }
    }

    @Override
    protected void bindTexture(ResourceLocation location) {
        super.bindTexture(this.texture != null && VANILLA.equals(location) ? this.texture : location);
    }
}
