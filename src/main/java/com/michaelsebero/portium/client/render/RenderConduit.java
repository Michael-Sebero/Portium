package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.tile.TileEntityConduit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * The conduit (1.13): a closed shell when inactive; when active a bobbing, rotating cage, an animated wind shell
 * and an eye that watches the viewer and opens once the frame is complete.
 */
public class RenderConduit extends TileEntitySpecialRenderer<TileEntityConduit> {

    private static final ResourceLocation BASE = new ResourceLocation(Tags.MOD_ID, "textures/entity/conduit/base.png");
    private static final ResourceLocation CAGE = new ResourceLocation(Tags.MOD_ID, "textures/entity/conduit/cage.png");
    private static final ResourceLocation WIND = new ResourceLocation(Tags.MOD_ID, "textures/entity/conduit/wind.png");
    private static final ResourceLocation WIND_VERTICAL = new ResourceLocation(Tags.MOD_ID, "textures/entity/conduit/wind_vertical.png");
    private static final ResourceLocation OPEN_EYE = new ResourceLocation(Tags.MOD_ID, "textures/entity/conduit/open_eye.png");
    private static final ResourceLocation CLOSED_EYE = new ResourceLocation(Tags.MOD_ID, "textures/entity/conduit/closed_eye.png");

    private static final BoxModel SHELL = new BoxModel(32, 16, 6, 0.0F);
    private static final BoxModel CAGE_MODEL = new BoxModel(32, 16, 8, 0.0F);
    private static final BoxModel EYE = new BoxModel(8, 8, 8, 0.01F, true);
    private static WindModel wind;

    public static void renderInactiveShell(double x, double y, double z) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(BASE);
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
        SHELL.render(0.0625F);
        GlStateManager.popMatrix();
    }

    @Override
    public void render(TileEntityConduit te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        float time = te.ticksExisted + partialTicks;
        if (!te.isActive()) {
            renderInactiveShell(x, y, z);
            return;
        }

        float rotation = te.getActiveRotation(partialTicks) * (180.0F / (float) Math.PI);
        float bob = MathHelper.sin(time * 0.1F) / 2.0F + 0.5F;
        bob = bob * bob + bob;

        this.bindTexture(CAGE);
        GlStateManager.disableCull();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 0.3F + bob * 0.2F, (float) z + 0.5F);
        GlStateManager.rotate(rotation, 0.5F, 1.0F, 0.5F);
        CAGE_MODEL.render(0.0625F);
        GlStateManager.popMatrix();

        this.renderWind(te, x, y, z);

        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        float pitch = viewer == null ? 0.0F : viewer.rotationPitch;
        float yaw = viewer == null ? 0.0F : viewer.rotationYaw;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 0.3F + bob * 0.2F, (float) z + 0.5F);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        this.bindTexture(te.isEyeOpen() ? OPEN_EYE : CLOSED_EYE);
        EYE.render(0.083333336F);
        GlStateManager.popMatrix();
        GlStateManager.enableCull();
    }

    private void renderWind(TileEntityConduit te, double x, double y, double z) {
        WindModel model = getWindModel();
        if (model == null) return;
        model.setFrame(te.ticksExisted / 3);
        int orientation = te.ticksExisted / 66 % 3;
        this.bindTexture(orientation == 1 ? WIND_VERTICAL : WIND);
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
        if (orientation == 1) GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
        else if (orientation == 2) GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        model.render(0.0625F);
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
        GlStateManager.scale(0.875F, 0.875F, 0.875F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        model.render(0.0625F);
        GlStateManager.popMatrix();
    }

    /**
     * The wind texture is a vertical strip of 64x32 frames; its length depends on the game version the assets came
     * from, so the frame count is read from the image itself.
     */
    private static WindModel getWindModel() {
        if (wind == null) {
            try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(WIND);
                 InputStream stream = resource.getInputStream()) {
                BufferedImage image = TextureUtil.readBufferedImage(stream);
                wind = new WindModel(image.getWidth(), image.getHeight());
            } catch (IOException e) {
                Portium.LOGGER.warn("Conduit wind texture is missing; is AssetMover installed?", e);
                wind = new WindModel(64, 32);
            }
        }
        return wind;
    }

    /** Forget the cached wind texture size, for resource reloads. */
    public static void onResourceReload() {
        wind = null;
    }

    /** A single centred cube, like 1.13's conduit shell, cage and eye models. */
    private static final class BoxModel extends ModelBase {

        private final ModelRenderer box;

        BoxModel(int textureWidth, int textureHeight, int size, float inflate) {
            this(textureWidth, textureHeight, size, inflate, false);
        }

        BoxModel(int textureWidth, int textureHeight, int size, float inflate, boolean flat) {
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.box = new ModelRenderer(this, 0, 0);
            float half = size / 2.0F;
            if (flat) this.box.addBox(-half, -half, 0.0F, size, size, 0, inflate);
            else this.box.addBox(-half, -half, -half, size, size, size, inflate);
        }

        void render(float scale) {
            this.box.render(scale);
        }
    }

    private static final class WindModel extends ModelBase {

        private final ModelRenderer[] frames;
        private int frame;

        WindModel(int width, int height) {
            this.textureWidth = width;
            this.textureHeight = height;
            int frameHeight = Math.max(1, width / 2);
            this.frames = new ModelRenderer[Math.max(1, height / frameHeight)];
            for (int i = 0; i < this.frames.length; i++) {
                this.frames[i] = new ModelRenderer(this, 0, frameHeight * i);
                this.frames[i].addBox(-8.0F, -8.0F, -8.0F, 16, 16, 16);
            }
        }

        void setFrame(int frame) {
            this.frame = Math.floorMod(frame, this.frames.length);
        }

        void render(float scale) {
            this.frames[this.frame].render(scale);
        }
    }
}
