package com.michaelsebero.portium.client;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * 1.16's {@code com.mojang.blaze3d.platform.GlStateManager}, which 1.16's render system and mods call: onto 1.12's
 * state manager, so both share one cache of GL state (a blend 1.16 code turns on is on as 1.12 sees it), and plain
 * GL (LWJGL 2) for the rest. 1.16's own can't run here: it starts up on LWJGL 3's memory utilities. Calls that pass
 * LWJGL 3 memory addresses for pixels do nothing; 1.16's light, overlay and outline set-ups are 1.12's nearest.
 * Client only.
 */
public final class GlStateManager116 {

    // Attributes

    @Srg("func_227630_a_")
    public static void pushLightingAttributes() {
        GlStateManager.pushAttrib();
    }

    @Srg("func_227669_b_")
    public static void pushTextureAttributes() {
        GL11.glPushAttrib(270336);
    }

    @Srg("func_227686_c_")
    public static void popAttributes() {
        GlStateManager.popAttrib();
    }

    // Alpha test, lighting and colour material

    @Srg("func_227700_d_")
    public static void disableAlphaTest() {
        GlStateManager.disableAlpha();
    }

    @Srg("func_227709_e_")
    public static void enableAlphaTest() {
        GlStateManager.enableAlpha();
    }

    @Srg("func_227639_a_")
    public static void alphaFunc(int func, float ref) {
        GlStateManager.alphaFunc(func, ref);
    }

    @Srg("func_227716_f_")
    public static void enableLighting() {
        GlStateManager.enableLighting();
    }

    @Srg("func_227722_g_")
    public static void disableLighting() {
        GlStateManager.disableLighting();
    }

    @Srg("func_227638_a_")
    public static void enableLight(int light) {
        GlStateManager.enableLight(light);
    }

    @Srg("func_227725_h_")
    public static void enableColorMaterial() {
        GlStateManager.enableColorMaterial();
    }

    @Srg("func_227728_i_")
    public static void disableColorMaterial() {
        GlStateManager.disableColorMaterial();
    }

    @Srg("func_227641_a_")
    public static void colorMaterial(int face, int mode) {
        GlStateManager.colorMaterial(face, mode);
    }

    @Srg("func_227653_a_")
    public static void light(int light, int name, FloatBuffer params) {
        GlStateManager.glLight(light, name, params);
    }

    @Srg("func_227656_a_")
    public static void lightModel(int name, FloatBuffer params) {
        GlStateManager.glLightModel(name, params);
    }

    @Srg("func_227636_a_")
    public static void normal3f(float x, float y, float z) {
        GlStateManager.glNormal3f(x, y, z);
    }

    // Scissor

    @Srg("func_244593_j")
    public static void disableScissorTest() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Srg("func_244594_k")
    public static void enableScissorTest() {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    @Srg("func_244592_a")
    public static void scissorBox(int x, int y, int width, int height) {
        GL11.glScissor(x, y, width, height);
    }

    // Depth and blending

    @Srg("func_227731_j_")
    public static void disableDepthTest() {
        GlStateManager.disableDepth();
    }

    @Srg("func_227734_k_")
    public static void enableDepthTest() {
        GlStateManager.enableDepth();
    }

    @Srg("func_227674_b_")
    public static void depthFunc(int func) {
        GlStateManager.depthFunc(func);
    }

    @Srg("func_227667_a_")
    public static void depthMask(boolean mask) {
        GlStateManager.depthMask(mask);
    }

    @Srg("func_227737_l_")
    public static void disableBlend() {
        GlStateManager.disableBlend();
    }

    @Srg("func_227740_m_")
    public static void enableBlend() {
        GlStateManager.enableBlend();
    }

    @Srg("func_227676_b_")
    public static void blendFunc(int source, int destination) {
        GlStateManager.blendFunc(source, destination);
    }

    @Srg("func_227644_a_")
    public static void blendFuncSeparate(int source, int destination, int sourceAlpha, int destinationAlpha) {
        GlStateManager.tryBlendFuncSeparate(source, destination, sourceAlpha, destinationAlpha);
    }

    @Srg("func_227706_d_")
    public static void glBlendFuncSeparate(int source, int destination, int sourceAlpha, int destinationAlpha) {
        GlStateManager.tryBlendFuncSeparate(source, destination, sourceAlpha, destinationAlpha);
    }

    @Srg("func_227637_a_")
    public static void blendColor(float red, float green, float blue, float alpha) {
        GL14.glBlendColor(red, green, blue, alpha);
    }

    @Srg("func_227690_c_")
    public static void blendEquation(int mode) {
        GlStateManager.glBlendEquation(mode);
    }

    /** 1.16's start-up with LWJGL 3's capabilities: 1.12 started up already. */
    @Srg("func_227666_a_")
    public static String init(Object capabilities) {
        return "Portium (1.12's GL state)";
    }

    // Shaders

    @Srg("func_227691_c_")
    public static int getProgram(int program, int name) {
        return GL20.glGetProgrami(program, name);
    }

    @Srg("func_227704_d_")
    public static void attachShader(int program, int shader) {
        GL20.glAttachShader(program, shader);
    }

    @Srg("func_227703_d_")
    public static void deleteShader(int shader) {
        GL20.glDeleteShader(shader);
    }

    @Srg("func_227711_e_")
    public static int createShader(int type) {
        return GL20.glCreateShader(type);
    }

    @Srg("func_227654_a_")
    public static void shaderSource(int shader, CharSequence source) {
        GL20.glShaderSource(shader, source);
    }

    @Srg("func_227717_f_")
    public static void compileShader(int shader) {
        GL20.glCompileShader(shader);
    }

    @Srg("func_227712_e_")
    public static int getShader(int shader, int name) {
        return GL20.glGetShaderi(shader, name);
    }

    @Srg("func_227723_g_")
    public static void useProgram(int program) {
        OpenGlHelper.glUseProgram(program);
    }

    @Srg("func_227743_n_")
    public static int createProgram() {
        return GL20.glCreateProgram();
    }

    @Srg("func_227726_h_")
    public static void deleteProgram(int program) {
        GL20.glDeleteProgram(program);
    }

    @Srg("func_227729_i_")
    public static void linkProgram(int program) {
        GL20.glLinkProgram(program);
    }

    @Srg("func_227680_b_")
    public static int getUniformLocation(int program, CharSequence name) {
        return GL20.glGetUniformLocation(program, name);
    }

    @Srg("func_227657_a_")
    public static void uniform1i(int location, IntBuffer values) {
        GL20.glUniform1(location, values);
    }

    @Srg("func_227718_f_")
    public static void uniform1i(int location, int value) {
        GL20.glUniform1i(location, value);
    }

    @Srg("func_227681_b_")
    public static void uniform1f(int location, FloatBuffer values) {
        GL20.glUniform1(location, values);
    }

    @Srg("func_227682_b_")
    public static void uniform2i(int location, IntBuffer values) {
        GL20.glUniform2(location, values);
    }

    @Srg("func_227696_c_")
    public static void uniform2f(int location, FloatBuffer values) {
        GL20.glUniform2(location, values);
    }

    @Srg("func_227697_c_")
    public static void uniform3i(int location, IntBuffer values) {
        GL20.glUniform3(location, values);
    }

    @Srg("func_227707_d_")
    public static void uniform3f(int location, FloatBuffer values) {
        GL20.glUniform3(location, values);
    }

    @Srg("func_227708_d_")
    public static void uniform4i(int location, IntBuffer values) {
        GL20.glUniform4(location, values);
    }

    @Srg("func_227715_e_")
    public static void uniform4f(int location, FloatBuffer values) {
        GL20.glUniform4(location, values);
    }

    @Srg("func_227659_a_")
    public static void uniformMatrix2f(int location, boolean transpose, FloatBuffer values) {
        GL20.glUniformMatrix2(location, transpose, values);
    }

    @Srg("func_227683_b_")
    public static void uniformMatrix3f(int location, boolean transpose, FloatBuffer values) {
        GL20.glUniformMatrix3(location, transpose, values);
    }

    @Srg("func_227698_c_")
    public static void uniformMatrix4f(int location, boolean transpose, FloatBuffer values) {
        GL20.glUniformMatrix4(location, transpose, values);
    }

    @Srg("func_227695_c_")
    public static int getAttribLocation(int program, CharSequence name) {
        return GL20.glGetAttribLocation(program, name);
    }

    @Srg("func_227733_j_")
    public static String getShaderInfoLog(int shader, int length) {
        return GL20.glGetShaderInfoLog(shader, length);
    }

    @Srg("func_227736_k_")
    public static String getProgramInfoLog(int program, int length) {
        return GL20.glGetProgramInfoLog(program, length);
    }

    // Buffers and framebuffers

    @Srg("func_227746_o_")
    public static int genBuffers() {
        return GL15.glGenBuffers();
    }

    @Srg("func_227724_g_")
    public static void bindBuffer(int target, int buffer) {
        GL15.glBindBuffer(target, buffer);
    }

    @Srg("func_227655_a_")
    public static void bufferData(int target, ByteBuffer data, int usage) {
        GL15.glBufferData(target, data, usage);
    }

    @Srg("func_227732_j_")
    public static void deleteBuffers(int buffer) {
        GL15.glDeleteBuffers(buffer);
    }

    @Srg("func_237509_a_")
    public static void copySubImage(int target, int level, int xOffset, int yOffset, int x, int y, int width, int height) {
        GL11.glCopyTexSubImage2D(target, level, xOffset, yOffset, x, y, width, height);
    }

    @Srg("func_227727_h_")
    public static void bindFramebuffer(int target, int framebuffer) {
        OpenGlHelper.glBindFramebuffer(target, framebuffer);
    }

    @Srg("func_237514_p_")
    public static int getFrameBufferAttachmentParam() {
        return GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
    }

    @Srg("func_237510_a_")
    public static void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GL30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    @Srg("func_227738_l_")
    public static void deleteFramebuffers(int framebuffer) {
        OpenGlHelper.glDeleteFramebuffers(framebuffer);
    }

    @Srg("func_227749_p_")
    public static int genFramebuffers() {
        return OpenGlHelper.glGenFramebuffers();
    }

    @Srg("func_227741_m_")
    public static int checkFramebufferStatus(int target) {
        return OpenGlHelper.glCheckFramebufferStatus(target);
    }

    @Srg("func_227645_a_")
    public static void framebufferTexture2D(int target, int attachment, int textureTarget, int texture, int level) {
        OpenGlHelper.glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
    }

    // Texture units

    @Srg("func_237515_r_")
    public static int getActiveTextureId() {
        return GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    }

    @Srg("func_227744_n_")
    public static void glActiveTexture(int unit) {
        GlStateManager.setActiveTexture(unit);
    }

    @Srg("func_227756_r_")
    public static void activeTexture(int unit) {
        GlStateManager.setActiveTexture(unit);
    }

    @Srg("func_227747_o_")
    public static void clientActiveTexture(int unit) {
        OpenGlHelper.setClientActiveTexture(unit);
    }

    @Srg("func_227640_a_")
    public static void multiTexCoord2f(int unit, float s, float t) {
        OpenGlHelper.setLightmapTextureCoords(unit, s, t);
    }

    // 1.16's fixed set-ups: 1.12's nearest

    @Srg("func_227755_r_")
    public static void setupOutline() {}

    @Srg("func_227757_s_")
    public static void teardownOutline() {}

    /** The hurt and flash overlay, which 1.12 draws otherwise. */
    @Srg("func_227739_l_")
    public static void setupOverlayColor(int texture, int size) {}

    @Srg("func_227759_t_")
    public static void teardownOverlayColor() {}

    @Srg("func_237512_a_")
    public static void setupLighting(Object light0, Object light1, Object matrix) {
        RenderHelper.enableStandardItemLighting();
    }

    @Srg("func_237511_a_")
    public static void setupScaledLighting(Object light0, Object light1) {
        RenderHelper.enableGUIStandardItemLighting();
    }

    @Srg("func_237513_b_")
    public static void setupGui3DMatrix(Object light0, Object light1) {
        RenderHelper.enableGUIStandardItemLighting();
    }

    @Srg("func_227761_u_")
    public static void setupEndPortalTexGen() {}

    @Srg("func_227763_v_")
    public static void clearTexGen() {
        GlStateManager.disableTexGenCoord(GlStateManager.TexGen.S);
        GlStateManager.disableTexGenCoord(GlStateManager.TexGen.T);
        GlStateManager.disableTexGenCoord(GlStateManager.TexGen.R);
        GlStateManager.disableTexGenCoord(GlStateManager.TexGen.Q);
    }

    @Srg("func_227765_w_")
    public static void mulTextureByProjModelView() {}

    // Fog

    @Srg("func_227767_x_")
    public static void enableFog() {
        GlStateManager.enableFog();
    }

    @Srg("func_227769_y_")
    public static void disableFog() {
        GlStateManager.disableFog();
    }

    @Srg("func_227750_p_")
    public static void fogMode(int mode) {
        for (GlStateManager.FogMode fog : GlStateManager.FogMode.values()) {
            if (fog.capabilityId == mode) {
                GlStateManager.setFog(fog);
                return;
            }
        }
        GlStateManager.glFogi(GL11.GL_FOG_MODE, mode);
    }

    @Srg("func_227634_a_")
    public static void fogDensity(float density) {
        GlStateManager.setFogDensity(density);
    }

    @Srg("func_227671_b_")
    public static void fogStart(float start) {
        GlStateManager.setFogStart(start);
    }

    @Srg("func_227687_c_")
    public static void fogEnd(float end) {
        GlStateManager.setFogEnd(end);
    }

    @Srg("func_227660_a_")
    public static void fog(int name, float[] params) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(Math.max(4, params.length));
        buffer.put(params).flip();
        GlStateManager.glFog(name, buffer);
    }

    @Srg("func_227742_m_")
    public static void fogi(int name, int param) {
        GlStateManager.glFogi(name, param);
    }

    // Culling, polygons, logic ops and texture coordinates

    @Srg("func_227771_z_")
    public static void enableCull() {
        GlStateManager.enableCull();
    }

    @Srg("func_227605_A_")
    public static void disableCull() {
        GlStateManager.disableCull();
    }

    @Srg("func_227745_n_")
    public static void polygonMode(int face, int mode) {
        GlStateManager.glPolygonMode(face, mode);
    }

    @Srg("func_227607_B_")
    public static void enablePolygonOffset() {
        GlStateManager.enablePolygonOffset();
    }

    @Srg("func_227609_C_")
    public static void disablePolygonOffset() {
        GlStateManager.disablePolygonOffset();
    }

    @Srg("func_227611_D_")
    public static void enableLineOffset() {
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
    }

    @Srg("func_227613_E_")
    public static void disableLineOffset() {
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_LINE);
    }

    @Srg("func_227635_a_")
    public static void polygonOffset(float factor, float units) {
        GlStateManager.doPolygonOffset(factor, units);
    }

    @Srg("func_227615_F_")
    public static void enableColorLogicOp() {
        GlStateManager.enableColorLogic();
    }

    @Srg("func_227617_G_")
    public static void disableColorLogicOp() {
        GlStateManager.disableColorLogic();
    }

    @Srg("func_227753_q_")
    public static void logicOp(int opcode) {
        GlStateManager.colorLogicOp(opcode);
    }

    @Srg("func_227662_a_")
    public static void enableTexGen(GlStateManager.TexGen coordinate) {
        GlStateManager.enableTexGenCoord(coordinate);
    }

    @Srg("func_227685_b_")
    public static void disableTexGen(GlStateManager.TexGen coordinate) {
        GlStateManager.disableTexGenCoord(coordinate);
    }

    @Srg("func_227663_a_")
    public static void texGenMode(GlStateManager.TexGen coordinate, int mode) {
        GlStateManager.texGen(coordinate, mode);
    }

    @Srg("func_227664_a_")
    public static void texGenParam(GlStateManager.TexGen coordinate, int name, FloatBuffer params) {
        GlStateManager.texGen(coordinate, name, params);
    }

    // Textures

    @Srg("func_227619_H_")
    public static void enableTexture() {
        GlStateManager.enableTexture2D();
    }

    @Srg("func_227621_I_")
    public static void disableTexture() {
        GlStateManager.disableTexture2D();
    }

    @Srg("func_227643_a_")
    public static void texEnv(int target, int name, int param) {
        GlStateManager.glTexEnvi(target, name, param);
    }

    @Srg("func_227642_a_")
    public static void texParameter(int target, int name, float param) {
        GlStateManager.glTexParameterf(target, name, param);
    }

    @Srg("func_227677_b_")
    public static void texParameter(int target, int name, int param) {
        GlStateManager.glTexParameteri(target, name, param);
    }

    @Srg("func_227692_c_")
    public static int getTexLevelParameter(int target, int level, int name) {
        return GlStateManager.glGetTexLevelParameteri(target, level, name);
    }

    @Srg("func_227622_J_")
    public static int genTexture() {
        return GlStateManager.generateTexture();
    }

    @Srg("func_242998_a")
    public static void genTextures(int[] textures) {
        for (int i = 0; i < textures.length; i++) textures[i] = GlStateManager.generateTexture();
    }

    @Srg("func_227758_s_")
    public static void deleteTexture(int texture) {
        GlStateManager.deleteTexture(texture);
    }

    @Srg("func_242999_b")
    public static void deleteTextures(int[] textures) {
        for (int texture : textures) GlStateManager.deleteTexture(texture);
    }

    @Srg("func_227760_t_")
    public static void bindTexture(int texture) {
        GlStateManager.bindTexture(texture);
    }

    @Srg("func_227647_a_")
    public static void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, IntBuffer pixels) {
        GlStateManager.glTexImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
    }

    /** Pixels at an LWJGL 3 address, which LWJGL 2 can't take: nothing is uploaded. */
    @Srg("func_227646_a_")
    public static void texSubImage2D(int target, int level, int x, int y, int width, int height, int format, int type, long pixels) {}

    @Srg("func_227649_a_")
    public static void getTexImage(int target, int level, int format, int type, long pixels) {}

    // Shading, viewport, masks and clearing

    @Srg("func_227762_u_")
    public static void shadeModel(int mode) {
        GlStateManager.shadeModel(mode);
    }

    @Srg("func_227623_K_")
    public static void enableRescaleNormal() {
        GlStateManager.enableRescaleNormal();
    }

    @Srg("func_227624_L_")
    public static void disableRescaleNormal() {
        GlStateManager.disableRescaleNormal();
    }

    @Srg("func_227714_e_")
    public static void viewport(int x, int y, int width, int height) {
        GlStateManager.viewport(x, y, width, height);
    }

    @Srg("func_227668_a_")
    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GlStateManager.colorMask(red, green, blue, alpha);
    }

    @Srg("func_227705_d_")
    public static void stencilFunc(int func, int ref, int mask) {
        GL11.glStencilFunc(func, ref, mask);
    }

    @Srg("func_227764_v_")
    public static void stencilMask(int mask) {
        GL11.glStencilMask(mask);
    }

    @Srg("func_227713_e_")
    public static void stencilOp(int fail, int depthFail, int pass) {
        GL11.glStencilOp(fail, depthFail, pass);
    }

    @Srg("func_227631_a_")
    public static void clearDepth(double depth) {
        GlStateManager.clearDepth(depth);
    }

    @Srg("func_227673_b_")
    public static void clearColor(float red, float green, float blue, float alpha) {
        GlStateManager.clearColor(red, green, blue, alpha);
    }

    @Srg("func_227766_w_")
    public static void clearStencil(int stencil) {
        GL11.glClearStencil(stencil);
    }

    @Srg("func_227658_a_")
    public static void clear(int mask, boolean checkError) {
        GlStateManager.clear(mask);
    }

    // Matrices and colour

    @Srg("func_227768_x_")
    public static void matrixMode(int mode) {
        GlStateManager.matrixMode(mode);
    }

    @Srg("func_227625_M_")
    public static void loadIdentity() {
        GlStateManager.loadIdentity();
    }

    @Srg("func_227626_N_")
    public static void pushMatrix() {
        GlStateManager.pushMatrix();
    }

    @Srg("func_227627_O_")
    public static void popMatrix() {
        GlStateManager.popMatrix();
    }

    @Srg("func_227721_f_")
    public static void getMatrix(int name, FloatBuffer matrix) {
        GlStateManager.getFloat(name, matrix);
    }

    @Srg("func_227633_a_")
    public static void ortho(double left, double right, double bottom, double top, double near, double far) {
        GlStateManager.ortho(left, right, bottom, top, near, far);
    }

    @Srg("func_227689_c_")
    public static void rotatef(float angle, float x, float y, float z) {
        GlStateManager.rotate(angle, x, y, z);
    }

    @Srg("func_227672_b_")
    public static void scalef(float x, float y, float z) {
        GlStateManager.scale(x, y, z);
    }

    @Srg("func_227632_a_")
    public static void scaled(double x, double y, double z) {
        GlStateManager.scale(x, y, z);
    }

    @Srg("func_227688_c_")
    public static void translatef(float x, float y, float z) {
        GlStateManager.translate(x, y, z);
    }

    @Srg("func_227670_b_")
    public static void translated(double x, double y, double z) {
        GlStateManager.translate(x, y, z);
    }

    @Srg("func_227665_a_")
    public static void multMatrix(FloatBuffer matrix) {
        GlStateManager.multMatrix(matrix);
    }

    private static Method writeMatrix;

    /** A 1.16 matrix (a class of Minecraft 1.16.5's own), written out and multiplied in. */
    @Srg("func_227699_c_")
    public static void multMatrix(Object matrix) {
        try {
            if (writeMatrix == null) writeMatrix = matrix.getClass().getMethod("func_195879_b", FloatBuffer.class);
            FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
            writeMatrix.invoke(matrix, buffer);
            buffer.rewind();
            GlStateManager.multMatrix(buffer);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't read a 1.16 matrix", e);
        }
    }

    @Srg("func_227702_d_")
    public static void color4f(float red, float green, float blue, float alpha) {
        GlStateManager.color(red, green, blue, alpha);
    }

    @Srg("func_227628_P_")
    public static void clearCurrentColor() {
        GlStateManager.resetColor();
    }

    // Vertex arrays (offsets into the bound buffer, as LWJGL 2 takes them)

    @Srg("func_227652_a_")
    public static void normalPointer(int type, int stride, long offset) {
        GL11.glNormalPointer(type, stride, offset);
    }

    @Srg("func_227650_a_")
    public static void texCoordPointer(int size, int type, int stride, long offset) {
        GL11.glTexCoordPointer(size, type, stride, offset);
    }

    @Srg("func_227679_b_")
    public static void vertexPointer(int size, int type, int stride, long offset) {
        GL11.glVertexPointer(size, type, stride, offset);
    }

    @Srg("func_227694_c_")
    public static void colorPointer(int size, int type, int stride, long offset) {
        GL11.glColorPointer(size, type, stride, offset);
    }

    @Srg("func_227651_a_")
    public static void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset) {
        GL20.glVertexAttribPointer(index, size, type, normalized, stride, offset);
    }

    @Srg("func_227770_y_")
    public static void enableClientState(int state) {
        GlStateManager.glEnableClientState(state);
    }

    @Srg("func_227772_z_")
    public static void disableClientState(int state) {
        GlStateManager.glDisableClientState(state);
    }

    @Srg("func_227606_A_")
    public static void enableVertexAttribArray(int index) {
        GL20.glEnableVertexAttribArray(index);
    }

    @Srg("func_227608_B_")
    public static void disableVertexAttribArray(int index) {
        GL20.glDisableVertexAttribArray(index);
    }

    @Srg("func_227719_f_")
    public static void drawArrays(int mode, int first, int count) {
        GlStateManager.glDrawArrays(mode, first, count);
    }

    @Srg("func_227701_d_")
    public static void lineWidth(float width) {
        GlStateManager.glLineWidth(width);
    }

    @Srg("func_227748_o_")
    public static void pixelStore(int name, int param) {
        GlStateManager.glPixelStorei(name, param);
    }

    @Srg("func_227675_b_")
    public static void pixelTransfer(int name, float param) {
        GL11.glPixelTransferf(name, param);
    }

    @Srg("func_227648_a_")
    public static void readPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
        GL11.glReadPixels(x, y, width, height, format, type, pixels);
    }

    @Srg("func_227629_Q_")
    public static int getError() {
        return GL11.glGetError();
    }

    @Srg("func_227610_C_")
    public static String getString(int name) {
        return GL11.glGetString(name);
    }

    @Srg("func_227612_D_")
    public static int getInteger(int name) {
        return GL11.glGetInteger(name);
    }

    /** 1.16's "fabulous" graphics, which 1.12 doesn't have. */
    @Srg("func_237508_S_")
    public static boolean isFabulous() {
        return false;
    }

    // Blend factor enums: 1.16 reads their GL values by its own field names (RenderSystem).

    public static int factor(GlStateManager.SourceFactor factor) {
        return factor.factor;
    }

    public static int factor(GlStateManager.DestFactor factor) {
        return factor.factor;
    }

    private GlStateManager116() {}
}
