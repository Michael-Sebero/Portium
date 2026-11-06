package com.michaelsebero.portium.client;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.nio.FloatBuffer;

/**
 * The GL state a pass of 1.12's world renderer is drawing in, read from GL and put back through 1.12's state
 * manager (so its cache follows): what 1.16 code drawing in the middle of the pass changes (shading, lighting, blend,
 * alpha test, depth, culling, the bound textures and the light map's unit). Client only.
 */
final class GlStateSnapshot {

    private static final FloatBuffer COLOR = BufferUtils.createFloatBuffer(16);

    private int shadeModel;
    private boolean lighting;
    private boolean depthTest;
    private boolean depthMask;
    private int depthFunc;
    private boolean blend;
    private int blendSrcRgb;
    private int blendDstRgb;
    private int blendSrcAlpha;
    private int blendDstAlpha;
    private boolean alphaTest;
    private int alphaFunc;
    private float alphaRef;
    private boolean cull;
    private boolean texture;
    private int textureBinding;
    private boolean lightmap;
    private int lightmapBinding;
    private final float[] color = new float[4];

    static GlStateSnapshot take() {
        GlStateSnapshot state = new GlStateSnapshot();
        state.shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        state.lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        state.depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        state.depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        state.depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        state.blend = GL11.glIsEnabled(GL11.GL_BLEND);
        state.blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        state.blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        state.blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        state.blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        state.alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        state.alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        state.alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        state.cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        state.lightmap = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        state.lightmapBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        state.texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        state.textureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        COLOR.clear();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, COLOR);
        for (int i = 0; i < 4; i++) state.color[i] = COLOR.get(i);
        return state;
    }

    void restore() {
        GlStateManager.shadeModel(this.shadeModel);
        if (this.lighting) RenderHelper.enableStandardItemLighting();
        else RenderHelper.disableStandardItemLighting();
        if (this.depthTest) GlStateManager.enableDepth();
        else GlStateManager.disableDepth();
        GlStateManager.depthMask(this.depthMask);
        GlStateManager.depthFunc(this.depthFunc);
        if (this.blend) GlStateManager.enableBlend();
        else GlStateManager.disableBlend();
        GlStateManager.tryBlendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcAlpha, this.blendDstAlpha);
        if (this.alphaTest) GlStateManager.enableAlpha();
        else GlStateManager.disableAlpha();
        GlStateManager.alphaFunc(this.alphaFunc, this.alphaRef);
        if (this.cull) GlStateManager.enableCull();
        else GlStateManager.disableCull();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        if (this.lightmap) GlStateManager.enableTexture2D();
        else GlStateManager.disableTexture2D();
        GlStateManager.bindTexture(this.lightmapBinding);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        if (this.texture) GlStateManager.enableTexture2D();
        else GlStateManager.disableTexture2D();
        GlStateManager.bindTexture(this.textureBinding);
        GlStateManager.color(this.color[0], this.color[1], this.color[2], this.color[3]);
    }
}
