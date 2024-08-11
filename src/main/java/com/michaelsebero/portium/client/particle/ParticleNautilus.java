package com.michaelsebero.portium.client.particle;

import com.michaelsebero.portium.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEnchantmentTable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * The conduit particle (1.13): moves like an enchanting table glyph, from {@code start + offset} into {@code start},
 * drawn with the nautilus sprite.
 */
public class ParticleNautilus extends ParticleEnchantmentTable {

    public static final ResourceLocation SPRITE = new ResourceLocation(Tags.MOD_ID, "particle/nautilus");

    /** False while the vanilla constructor runs, which assigns a glyph from the particle sheet (layer 0). */
    private boolean useSprite;

    public ParticleNautilus(World world, double x, double y, double z, double dx, double dy, double dz) {
        super(world, x, y, z, dx, dy, dz);
        this.useSprite = true;
        this.setParticleTexture(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(SPRITE.toString()));
    }

    @Override
    public int getFXLayer() {
        return this.useSprite ? 1 : 0;
    }
}
