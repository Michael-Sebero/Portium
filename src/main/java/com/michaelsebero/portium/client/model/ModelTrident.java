package com.michaelsebero.portium.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/** The 3D trident (1.13), used for thrown tridents and the held item. Texture is 32x32. */
public class ModelTrident extends ModelBase {

    private final ModelRenderer pole;

    public ModelTrident() {
        this.textureWidth = 32;
        this.textureHeight = 32;
        this.pole = new ModelRenderer(this, 0, 0);
        this.pole.addBox(-0.5F, -4.0F, -0.5F, 1, 31, 1, 0.0F);
        ModelRenderer base = new ModelRenderer(this, 4, 0);
        base.addBox(-1.5F, 0.0F, -0.5F, 3, 2, 1);
        this.pole.addChild(base);
        ModelRenderer leftSpike = new ModelRenderer(this, 4, 3);
        leftSpike.addBox(-2.5F, -3.0F, -0.5F, 1, 4, 1);
        this.pole.addChild(leftSpike);
        ModelRenderer rightSpike = new ModelRenderer(this, 4, 3);
        rightSpike.mirror = true;
        rightSpike.addBox(1.5F, -3.0F, -0.5F, 1, 4, 1);
        this.pole.addChild(rightSpike);
    }

    public void render() {
        this.pole.render(0.0625F);
    }
}
