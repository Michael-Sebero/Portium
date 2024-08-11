package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * What a 1.16 mod's widgets extend instead of 1.16's {@code Widget}: 1.12's {@code Gui} with 1.16's widget fields,
 * input methods and drawing. 1.16's matrix stack (a class of Minecraft 1.16.5's own) is taken as an object; overrides
 * taking it are bridged to these methods.
 */
@SideOnly(Side.CLIENT)
public class Widget116 extends Gui {

    @Srg("field_230687_i_")
    public static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/widgets.png");

    @Srg("field_230688_j_")
    protected int width;
    @Srg("field_230689_k_")
    protected int height;
    @Srg("field_230690_l_")
    public int x;
    @Srg("field_230691_m_")
    public int y;
    @Srg("field_230684_a_")
    private ITextComponent message;
    @Srg("field_230685_b_")
    protected boolean wasHovered;
    @Srg("field_230692_n_")
    protected boolean isHovered;
    @Srg("field_230693_o_")
    public boolean active = true;
    @Srg("field_230694_p_")
    public boolean visible = true;
    @Srg("field_230695_q_")
    protected float alpha = 1.0F;
    @Srg("field_230686_c_")
    private boolean focused;

    public Widget116(int x, int y, int width, int height, ITextComponent message) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.message = message;
    }

    @Srg("func_230430_a_")
    public void render(Object matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;
        this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        renderWidget(matrixStack, mouseX, mouseY, partialTicks);
        this.wasHovered = this.isHovered;
    }

    /** 1.16's button look, drawn as 1.12 draws its buttons. */
    @Srg("func_230431_b_")
    public void renderWidget(Object matrixStack, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.getTextureManager().bindTexture(WIDGETS_LOCATION);
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, this.alpha);
        int v = 46 + getYImage(isHovered()) * 20;
        drawTexturedModalRect(this.x, this.y, 0, v, this.width / 2, this.height);
        drawTexturedModalRect(this.x + this.width / 2, this.y, 200 - this.width / 2, v, this.width / 2, this.height);
        renderBg(matrixStack, minecraft, mouseX, mouseY);
        int colour = this.active ? 0xFFFFFF : 0xA0A0A0;
        String text = this.message == null ? "" : this.message.getFormattedText();
        drawCenteredString(minecraft.fontRenderer, text, this.x + this.width / 2, this.y + (this.height - 8) / 2, colour);
    }

    @Srg("func_230441_a_")
    protected void renderBg(Object matrixStack, Minecraft minecraft, int mouseX, int mouseY) {}

    @Srg("func_230989_a_")
    protected int getYImage(boolean hovered) {
        return !this.active ? 0 : hovered ? 2 : 1;
    }

    @Srg("func_230982_a_")
    public void onClick(double mouseX, double mouseY) {}

    @Srg("func_231000_a__")
    public void onRelease(double mouseX, double mouseY) {}

    @Srg("func_230983_a_")
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {}

    @Srg("func_231044_a_")
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || !isValidClickButton(button) || !clicked(mouseX, mouseY)) return false;
        playDownSound(Minecraft.getMinecraft().getSoundHandler());
        onClick(mouseX, mouseY);
        return true;
    }

    @Srg("func_231048_c_")
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!isValidClickButton(button)) return false;
        onRelease(mouseX, mouseY);
        return true;
    }

    @Srg("func_231045_a_")
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!isValidClickButton(button)) return false;
        onDrag(mouseX, mouseY, dragX, dragY);
        return true;
    }

    @Srg("func_231043_a_")
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    @Srg("func_231046_a_")
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        return false;
    }

    @Srg("func_223281_a_")
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        return false;
    }

    @Srg("func_231042_a_")
    public boolean charTyped(char character, int modifiers) {
        return false;
    }

    @Srg("func_230987_a_")
    protected boolean isValidClickButton(int button) {
        return button == 0;
    }

    @Srg("func_230992_c_")
    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible && isMouseOver(mouseX, mouseY);
    }

    @Srg("func_231047_b_")
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active && this.visible && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
    }

    @Srg("func_230449_g_")
    public boolean isHovered() {
        return this.isHovered || this.focused;
    }

    @Srg("func_231049_c__")
    public boolean changeFocus(boolean forward) {
        if (!this.active || !this.visible) return false;
        this.focused = !this.focused;
        onFocusedChanged(this.focused);
        return this.focused;
    }

    @Srg("func_230995_c_")
    protected void onFocusedChanged(boolean focused) {}

    @Srg("func_230443_a_")
    public void renderToolTip(Object matrixStack, int mouseX, int mouseY) {}

    @Srg("func_230988_a_")
    public void playDownSound(SoundHandler sounds) {
        sounds.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Srg("func_230998_h_")
    public int getWidth() {
        return this.width;
    }

    @Srg("func_238483_d_")
    public int getHeightRealms() {
        return this.height;
    }

    @Srg("func_230991_b_")
    public void setWidth(int width) {
        this.width = width;
    }

    @Srg("func_230986_a_")
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    @Srg("func_238482_a_")
    public void setMessage(ITextComponent message) {
        this.message = message;
    }

    @Srg("func_230458_i_")
    public ITextComponent getMessage() {
        return this.message;
    }

    @Srg("func_230999_j_")
    public boolean isFocused() {
        return this.focused;
    }

    @Srg("func_230996_d_")
    protected void setFocused(boolean focused) {
        this.focused = focused;
    }
}
