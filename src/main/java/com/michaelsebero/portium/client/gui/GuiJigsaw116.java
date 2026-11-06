package com.michaelsebero.portium.client.gui;

import com.michaelsebero.portium.block.jigsaw.BlockJigsaw;
import com.michaelsebero.portium.network.JigsawUpdateMessage;
import com.michaelsebero.portium.network.PortiumNetwork;
import com.michaelsebero.portium.tile.TileEntityJigsaw;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * 1.16's jigsaw screen: the target pool, name, target name and the block it turns into; the joint type for a block
 * facing up or down; and Generate, with how many levels deep and whether to keep jigsaw blocks. Laid out as 1.16's.
 */
public class GuiJigsaw116 extends GuiScreen {

    private static final int DONE = 0;
    private static final int CANCEL = 1;
    private static final int JOINT = 2;
    private static final int LEVELS = 3;
    private static final int KEEP = 4;
    private static final int GENERATE = 5;

    private final TileEntityJigsaw jigsaw;
    private GuiTextField pool;
    private GuiTextField name;
    private GuiTextField target;
    private GuiTextField finalState;
    private GuiButton jointButton;
    private GuiSlider levels;
    private GuiButton keepButton;
    private boolean rollable;
    private boolean keepJigsaws = true;
    private int jointLabelWidth;

    public GuiJigsaw116(TileEntityJigsaw jigsaw) {
        this.jigsaw = jigsaw;
        this.rollable = jigsaw.isRollable();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        int left = this.width / 2 - 152;
        this.pool = this.field(1, left, 20, 128, this.jigsaw.getPool());
        this.name = this.field(2, left, 55, 128, this.jigsaw.getJigsawName());
        this.target = this.field(3, left, 90, 128, this.jigsaw.getTarget());
        this.finalState = this.field(4, left, 125, 256, this.jigsaw.getFinalState());
        this.pool.setFocused(true);
        this.jointLabelWidth = this.fontRenderer.getStringWidth(I18n.format("jigsaw_block.joint_label")) + 10;
        this.buttonList.clear();
        this.jointButton = this.addButton(new GuiButton(JOINT, left + this.jointLabelWidth, 150, 300 - this.jointLabelWidth, 20, ""));
        this.jointButton.enabled = this.isVertical();
        this.levels = this.addButton(new GuiSlider(LEVELS, this.width / 2 - 154, 180, 100, 20, I18n.format("jigsaw_block.levels", "").trim() + " ", "", 0, 7,
                0, false, true));
        this.keepButton = this.addButton(new GuiButton(KEEP, this.width / 2 - 50, 180, 100, 20, ""));
        this.addButton(new GuiButton(GENERATE, this.width / 2 + 54, 180, 100, 20, I18n.format("jigsaw_block.generate")));
        this.addButton(new GuiButton(DONE, this.width / 2 - 4 - 150, 210, 150, 20, I18n.format("gui.done")));
        this.addButton(new GuiButton(CANCEL, this.width / 2 + 4, 210, 150, 20, I18n.format("gui.cancel")));
        this.updateLabels();
    }

    private GuiTextField field(int id, int x, int y, int length, String text) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, x, y, 300, 20);
        field.setMaxStringLength(length);
        field.setText(text);
        return field;
    }

    private boolean isVertical() {
        IBlockState state = this.jigsaw.getWorld().getBlockState(this.jigsaw.getPos());
        return state.getBlock() instanceof BlockJigsaw && state.getValue(BlockJigsaw.ORIENTATION).front.getAxis() == net.minecraft.util.EnumFacing.Axis.Y;
    }

    private void updateLabels() {
        this.jointButton.displayString = I18n.format("jigsaw_block.joint." + (this.rollable ? "rollable" : "aligned"));
        this.keepButton.displayString = I18n.format("jigsaw_block.keep_jigsaws") + ": " + I18n.format(this.keepJigsaws ? "options.on" : "options.off");
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case DONE:
                this.send(false);
                this.mc.displayGuiScreen(null);
                break;
            case CANCEL:
                this.mc.displayGuiScreen(null);
                break;
            case JOINT:
                this.rollable = !this.rollable;
                this.updateLabels();
                break;
            case KEEP:
                this.keepJigsaws = !this.keepJigsaws;
                this.updateLabels();
                break;
            case GENERATE:
                this.send(true);
                this.mc.displayGuiScreen(null);
                break;
            default:
                break;
        }
    }

    private void send(boolean generate) {
        PortiumNetwork.CHANNEL.sendToServer(new JigsawUpdateMessage(this.jigsaw.getPos(), this.name.getText(), this.target.getText(), this.pool.getText(),
                this.finalState.getText(), this.rollable, generate, this.levels.getValueInt(), this.keepJigsaws));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            this.actionPerformed(this.buttonList.get(this.buttonList.size() - 2));
            return;
        }
        GuiTextField[] fields = {this.pool, this.name, this.target, this.finalState};
        if (keyCode == Keyboard.KEY_TAB) {
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].isFocused()) {
                    fields[i].setFocused(false);
                    fields[(i + (isShiftKeyDown() ? fields.length - 1 : 1)) % fields.length].setFocused(true);
                    return;
                }
            }
        }
        for (GuiTextField field : fields) field.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : new GuiTextField[] {this.pool, this.name, this.target, this.finalState}) field.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        for (GuiTextField field : new GuiTextField[] {this.pool, this.name, this.target, this.finalState}) field.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int left = this.width / 2 - 153;
        this.drawString(this.fontRenderer, I18n.format("jigsaw_block.pool"), left, 10, 0xA0A0A0);
        this.pool.drawTextBox();
        this.drawString(this.fontRenderer, I18n.format("jigsaw_block.name"), left, 45, 0xA0A0A0);
        this.name.drawTextBox();
        this.drawString(this.fontRenderer, I18n.format("jigsaw_block.target"), left, 80, 0xA0A0A0);
        this.target.drawTextBox();
        this.drawString(this.fontRenderer, I18n.format("jigsaw_block.final_state"), left, 115, 0xA0A0A0);
        this.finalState.drawTextBox();
        if (this.isVertical()) this.drawString(this.fontRenderer, I18n.format("jigsaw_block.joint_label"), left, 156, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
