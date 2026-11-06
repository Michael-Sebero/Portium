package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelHorse116;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityMule;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * 1.15's horse renderers, on {@link ModelHorse116} and 1.16.5's 64x64 textures: horses with their coat, markings and
 * armour layers, donkeys and mules (with chests), and zombie and skeleton horses.
 */
public class RenderHorse116 extends RenderLiving<AbstractHorse> {

    private static final String[] COATS = {"white", "creamy", "chestnut", "brown", "black", "gray", "darkbrown"};
    private static final String[] MARKINGS = {null, "white", "whitefield", "whitedots", "blackdots"};
    private static final ResourceLocation[] COAT_TEXTURES = new ResourceLocation[COATS.length];
    private static final ResourceLocation[] MARKING_TEXTURES = new ResourceLocation[MARKINGS.length];
    private static final ResourceLocation DONKEY = texture("donkey");
    private static final ResourceLocation MULE = texture("mule");
    private static final ResourceLocation ZOMBIE = texture("horse_zombie");
    private static final ResourceLocation SKELETON = texture("horse_skeleton");
    private static final ResourceLocation LEATHER = texture("armor/horse_armor_leather");
    private static final String[] ARMORS = {"iron", "gold", "diamond"};
    private static final ResourceLocation[] ARMOR_TEXTURES = new ResourceLocation[ARMORS.length];

    static {
        for (int i = 0; i < COATS.length; ++i) COAT_TEXTURES[i] = texture("horse_" + COATS[i]);
        for (int i = 1; i < MARKINGS.length; ++i) MARKING_TEXTURES[i] = texture("horse_markings_" + MARKINGS[i]);
        for (int i = 0; i < ARMORS.length; ++i) ARMOR_TEXTURES[i] = texture("armor/horse_armor_" + ARMORS[i]);
    }

    private final float scale;

    private RenderHorse116(RenderManager manager, boolean chested, float scale) {
        super(manager, new ModelHorse116(0.0F, chested), 0.75F);
        this.scale = scale;
    }

    public static RenderHorse116 horse(RenderManager manager) {
        RenderHorse116 render = new RenderHorse116(manager, false, 1.1F);
        render.addLayer(render.new LayerMarkings());
        render.addLayer(render.new LayerArmor());
        return render;
    }

    public static RenderHorse116 donkey(RenderManager manager) {
        return new RenderHorse116(manager, true, 0.87F);
    }

    public static RenderHorse116 mule(RenderManager manager) {
        return new RenderHorse116(manager, true, 0.92F);
    }

    public static RenderHorse116 undead(RenderManager manager) {
        return new RenderHorse116(manager, false, 1.0F);
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Tags.MOD_ID, "textures/entity/horse/" + name + ".png");
    }

    @Override
    protected void preRenderCallback(AbstractHorse horse, float partialTicks) {
        GlStateManager.scale(this.scale, this.scale, this.scale);
        this.shadowSize = horse.isChild() ? 0.375F : 0.75F;
    }

    @Override
    protected ResourceLocation getEntityTexture(AbstractHorse horse) {
        if (horse instanceof EntityHorse) {
            int coat = ((EntityHorse) horse).getHorseVariant() & 0xFF;
            return COAT_TEXTURES[coat < COAT_TEXTURES.length ? coat : 0];
        }
        if (horse instanceof EntityZombieHorse) return ZOMBIE;
        if (horse instanceof EntitySkeletonHorse) return SKELETON;
        return horse instanceof EntityMule ? MULE : DONKEY;
    }

    /** A horse's white or black markings, over its coat. */
    private final class LayerMarkings implements LayerRenderer<AbstractHorse> {

        @Override
        public void doRenderLayer(AbstractHorse horse, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            if (!(horse instanceof EntityHorse) || horse.isInvisible()) return;
            int marking = (((EntityHorse) horse).getHorseVariant() >> 8) & 0xFF;
            if (marking <= 0 || marking >= MARKING_TEXTURES.length) return;
            RenderHorse116.this.bindTexture(MARKING_TEXTURES[marking]);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderHorse116.this.getMainModel().render(horse, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            GlStateManager.disableBlend();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }

    /** Horse armour, on a slightly larger copy of the model; leather is dyed. */
    private final class LayerArmor implements LayerRenderer<AbstractHorse> {

        private final ModelHorse116 model = new ModelHorse116(0.1F, false);

        @Override
        public void doRenderLayer(AbstractHorse horse, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            if (!(horse instanceof EntityHorse)) return;
            String armor = armorTexture((EntityHorse) horse);
            if (armor == null) return;
            int tint = armor.indexOf("|tint=");
            ResourceLocation texture = tint >= 0 ? LEATHER : vanillaArmor(armor);
            if (texture == null) return;
            float red = 1.0F;
            float green = 1.0F;
            float blue = 1.0F;
            if (tint >= 0) {
                int color = Integer.parseInt(armor.substring(tint + 6), 16);
                red = (color >> 16 & 255) / 255.0F;
                green = (color >> 8 & 255) / 255.0F;
                blue = (color & 255) / 255.0F;
            }
            RenderHorse116.this.bindTexture(texture);
            this.model.setModelAttributes(RenderHorse116.this.getMainModel());
            this.model.setLivingAnimations(horse, limbSwing, limbSwingAmount, partialTicks);
            GlStateManager.color(red, green, blue, 1.0F);
            this.model.render(horse, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }

    /** The armour texture 1.12 chose for the horse (its item's, or the armour type's), if it wears any. */
    @Nullable
    private static String armorTexture(EntityHorse horse) {
        horse.getHorseTexture();
        String[] paths = horse.getVariantTexturePaths();
        return paths.length > 2 ? paths[2] : null;
    }

    /** 1.16.5's texture for vanilla iron, gold and diamond armour; other mods' armour is drawn for 1.12's model, so not here. */
    @Nullable
    private static ResourceLocation vanillaArmor(String path) {
        ResourceLocation location = new ResourceLocation(path);
        if (!"minecraft".equals(location.getNamespace())) return null;
        for (int i = 0; i < ARMORS.length; ++i) {
            if (location.getPath().endsWith("horse_armor_" + ARMORS[i] + ".png")) return ARMOR_TEXTURES[i];
        }
        return null;
    }
}
