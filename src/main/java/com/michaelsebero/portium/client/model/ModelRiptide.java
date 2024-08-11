package com.michaelsebero.portium.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/** The water vortex around a player using Riptide (1.13). Texture is 64x64. */
public class ModelRiptide extends ModelBase {

    private final ModelRenderer box;

    public ModelRiptide() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.box = new ModelRenderer(this, 0, 0);
        this.box.addBox(-8.0F, -16.0F, -8.0F, 16, 32, 16);
    }

    public void render(float scale) {
        this.box.render(scale);
    }
}
