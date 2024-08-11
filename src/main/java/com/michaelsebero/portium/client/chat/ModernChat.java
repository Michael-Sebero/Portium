package com.michaelsebero.portium.client.chat;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.network.CommandSuggestRequest;
import com.michaelsebero.portium.network.CommandSuggestResponse;
import com.michaelsebero.portium.network.PortiumNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 1.13's command help in the chat screen. While a command is typed its arguments are coloured in turn and anything
 * that doesn't parse turns red; above the input sits a list of suggestions (Tab applies and cycles them, the arrow
 * keys move through them), or the usage of what comes next, or why the command doesn't parse. Plain chat completes
 * player names on Tab.
 * <p>
 * A 1.13 client works all this out from a copy of the command tree. Here the server does it and answers each change
 * of the input (see {@code ChatSuggestions}); until an answer arrives the previous one stays on screen, extended
 * over what was typed since, so the screen doesn't flicker. Servers without Portium keep 1.12's tab completion.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class ModernChat {

    private static final TextFormatting[] ARGUMENT_COLOURS = {TextFormatting.AQUA, TextFormatting.YELLOW, TextFormatting.GREEN,
            TextFormatting.LIGHT_PURPLE, TextFormatting.GOLD};
    private static final int MAX_ROWS = 10;
    private static final int ROW_HEIGHT = 12;
    private static final int LIST_BACKGROUND = 0xD0000000;

    private static boolean serverSupport;
    private static int lastRequestId;
    @Nullable
    private static ModernChat open;

    private final GuiChat screen;
    private final GuiTextField input;
    private final IChatTextField field;
    private final FontRenderer font;
    private final String defaultText;

    /** Whether the text was changed from what the screen opened with; only then does the list open by itself. */
    private boolean hasEdits;
    private boolean applyingSuggestion;
    /** The latest answer, which colours the input. */
    @Nullable
    private CommandSuggestResponse answer;
    /** The request whose suggestions are awaited, or -1. */
    private int awaiting = -1;
    /** The finished suggestions for the current text, or null while they are awaited. */
    @Nullable
    private List<CommandSuggestResponse.Entry> pending;
    private int pendingStart;
    @Nullable
    private SuggestionList list;
    /** Whether the list and usage lines on screen are for an earlier text, with the answer for this one awaited. */
    private boolean stale;
    /** Tab was pressed on a stale list: apply the first suggestion as soon as the new list arrives. */
    private boolean tabQueued;
    private final List<String> usage = new ArrayList<>();
    private int usageX;
    private int usageWidth;

    private ModernChat(GuiChat screen, GuiTextField input, String defaultText) {
        this.screen = screen;
        this.input = input;
        this.field = (IChatTextField) input;
        this.font = Minecraft.getMinecraft().fontRenderer;
        this.defaultText = defaultText;
        this.field.portium$setFormatter(this::format);
        this.field.portium$setListener(this::onTextChanged);
        this.update();
    }

    public static void setServerSupport(boolean support) {
        serverSupport = support;
    }

    @SubscribeEvent
    public static void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        serverSupport = false;
    }

    /** Starts helping with a chat screen, unless the server can't answer or the help is turned off. */
    @Nullable
    public static ModernChat attach(GuiChat screen, GuiTextField input, String defaultText) {
        if (!serverSupport || !PortiumConfig.visuals.commandHelp) return null;
        open = new ModernChat(screen, input, defaultText);
        return open;
    }

    public void detach() {
        if (open == this) open = null;
        this.field.portium$setFormatter(null);
        this.field.portium$setSuggestion(null);
        this.field.portium$setListener(null);
    }

    public static void receive(CommandSuggestResponse response) {
        if (open != null) open.onAnswer(response);
    }

    // ---- Following the input ----

    private void onTextChanged() {
        this.hasEdits = !this.input.getText().equals(this.defaultText);
        this.update();
    }

    private void update() {
        String text = this.input.getText();
        if (!this.applyingSuggestion) {
            // Keep showing the old list until the new one arrives, its suggestion carried over if it still fits.
            this.stale = this.list != null || !this.usage.isEmpty();
            this.tabQueued = false;
            this.field.portium$setSuggestion(this.list == null ? null : this.list.suffixFor(text));
        }
        int cursor = this.input.getCursorPosition();
        if (text.startsWith("/")) {
            boolean wantSuggestions = cursor >= 1 && (this.list == null || !this.applyingSuggestion);
            int id = ++lastRequestId;
            if (wantSuggestions) {
                this.awaiting = id;
                this.pending = null;
            }
            PortiumNetwork.CHANNEL.sendToServer(new CommandSuggestRequest(id, text, cursor, wantSuggestions, targetedBlock(), targetedEntity()));
        } else {
            this.awaiting = -1;
            this.answer = null;
            this.pending = playerNames(text);
            this.pendingStart = lastWordStart(text);
            if (!this.applyingSuggestion) this.clearStale();
        }
    }

    private void onAnswer(CommandSuggestResponse response) {
        if (!this.input.getText().startsWith("/")) return;
        this.answer = response;
        if (response.id != this.awaiting) return;
        this.awaiting = -1;
        this.pending = response.suggestions;
        this.pendingStart = response.suggestionStart;
        this.clearStale();
        this.usageX = 0;
        this.usageWidth = this.screen.width;
        for (ITextComponent line : response.lines) this.usage.add(line.getFormattedText());
        if (response.usageStart >= 0 && !this.usage.isEmpty()) {
            int widest = 0;
            for (String line : this.usage) widest = Math.max(widest, this.font.getStringWidth(line));
            this.usageWidth = widest;
            this.usageX = MathHelper.clamp(this.xOf(response.usageStart), 0, this.screen.width - widest);
        }
        boolean tab = this.tabQueued;
        this.tabQueued = false;
        if (tab || this.hasEdits && PortiumConfig.visuals.autoSuggestCommands) this.showSuggestions();
        if (tab && this.list != null) this.list.keyPressed(Keyboard.KEY_TAB);
    }

    private void clearStale() {
        this.stale = false;
        this.list = null;
        this.usage.clear();
        if (!this.applyingSuggestion) this.field.portium$setSuggestion(null);
    }

    private void showSuggestions() {
        if (this.pending == null || this.pending.isEmpty()) return;
        int widest = 0;
        for (CommandSuggestResponse.Entry entry : this.pending) widest = Math.max(widest, this.font.getStringWidth(entry.text));
        int x = MathHelper.clamp(this.xOf(this.pendingStart), 0, this.screen.width - widest);
        this.list = new SuggestionList(x, this.screen.height - 12, widest, this.pending);
        this.stale = false;
    }

    /** The screen x of a character of the input, ignoring any scrolling, as 1.13 places its lists. */
    private int xOf(int index) {
        String text = this.input.getText();
        return index > text.length() ? this.input.x : this.input.x + this.font.getStringWidth(text.substring(0, index));
    }

    @Nullable
    private static BlockPos targetedBlock() {
        RayTraceResult target = Minecraft.getMinecraft().objectMouseOver;
        return target != null && target.typeOfHit == RayTraceResult.Type.BLOCK ? target.getBlockPos() : null;
    }

    @Nullable
    private static String targetedEntity() {
        RayTraceResult target = Minecraft.getMinecraft().objectMouseOver;
        return target != null && target.typeOfHit == RayTraceResult.Type.ENTITY && target.entityHit != null ? target.entityHit.getCachedUniqueIdString() : null;
    }

    /** After the last run of whitespace. */
    private static int lastWordStart(String text) {
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) start = i + 1;
        }
        return start;
    }

    /** The players in the tab list whose names start with the last word typed. */
    private static List<CommandSuggestResponse.Entry> playerNames(String text) {
        List<CommandSuggestResponse.Entry> names = new ArrayList<>();
        NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
        if (connection == null) return names;
        int start = lastWordStart(text);
        String word = text.substring(start);
        for (NetworkPlayerInfo player : connection.getPlayerInfoMap()) {
            String name = player.getGameProfile().getName();
            if (!name.equals(word) && name.toLowerCase(Locale.ROOT).startsWith(word.toLowerCase(Locale.ROOT))) {
                names.add(new CommandSuggestResponse.Entry(start, text.length(), name, null));
            }
        }
        names.sort((a, b) -> a.text.compareToIgnoreCase(b.text));
        return names;
    }

    // ---- Colouring ----

    /**
     * 1.13's colours: gray, with each argument of the last command in the next of five colours and the part that
     * doesn't parse in red. An answer for an earlier text colours what the two texts share; what was typed after it
     * continues an argument or error that ran to its end.
     */
    private String format(String visible, int offset) {
        String text = this.input.getText();
        CommandSuggestResponse answer = this.answer;
        if (!text.startsWith("/")) return visible;
        if (answer == null) return TextFormatting.GRAY + visible;
        int shared = 0;
        int limit = Math.min(answer.text.length(), text.length());
        while (shared < limit && answer.text.charAt(shared) == text.charAt(shared)) shared++;
        boolean appended = shared == answer.text.length() && text.indexOf(' ', shared) < 0;
        StringBuilder out = new StringBuilder(TextFormatting.GRAY.toString());
        int done = 0;
        for (int i = 0; i + 1 < answer.arguments.length; i += 2) {
            int start = answer.arguments[i];
            int end = this.extend(answer.arguments[i + 1], answer, shared, appended, text);
            if (start >= end) continue;
            int from = Math.max(start - offset, done);
            if (from >= visible.length()) break;
            int to = Math.min(end - offset, visible.length());
            if (to <= from) continue;
            out.append(visible, done, from).append(ARGUMENT_COLOURS[i / 2 % ARGUMENT_COLOURS.length]).append(visible, from, to).append(TextFormatting.GRAY);
            done = to;
        }
        if (answer.errorStart >= 0 && answer.errorStart < shared) {
            int from = Math.max(answer.errorStart - offset, done);
            int to = Math.min(this.extend(answer.errorEnd, answer, shared, appended, text) - offset, visible.length());
            if (from < to) {
                out.append(visible, done, from).append(TextFormatting.RED).append(visible, from, to);
                done = to;
            }
        }
        return out.append(visible.substring(done)).toString();
    }

    private int extend(int end, CommandSuggestResponse answer, int shared, boolean appended, String text) {
        return appended && end >= answer.text.length() ? text.length() : Math.min(end, shared);
    }

    // ---- Input from the screen ----

    /** A key typed into the chat screen; true if it was used here. */
    public boolean keyPressed(int keyCode) {
        if (this.list != null && !this.stale && this.list.keyPressed(keyCode)) return true;
        if (this.list != null && this.stale && keyCode == Keyboard.KEY_ESCAPE) {
            this.clearStale();
            return true;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            // Tab always belongs to the suggestions, never to 1.12's tab completion.
            this.hasEdits = true;
            if (this.pending == null && this.list != null) this.tabQueued = true;
            this.showSuggestions();
            return true;
        }
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        return this.list != null && !this.stale && this.list.mouseClicked(mouseX, mouseY);
    }

    public boolean mouseScrolled(int direction, int mouseX, int mouseY) {
        return this.list != null && this.list.mouseScrolled(direction, mouseX, mouseY);
    }

    public void render(int mouseX, int mouseY) {
        if (this.list != null) {
            this.list.render(mouseX, mouseY);
            return;
        }
        int bottom = this.screen.height;
        for (int i = 0; i < this.usage.size(); i++) {
            Gui.drawRect(this.usageX - 1, bottom - 14 - 13 - 12 * i, this.usageX + this.usageWidth + 1, bottom - 2 - 13 - 12 * i, 0xFF000000);
            this.font.drawStringWithShadow(this.usage.get(i), this.usageX, bottom - 14 - 13 + 2 - 12 * i, -1);
        }
    }

    // ---- The suggestion list ----

    private final class SuggestionList {

        private final int left;
        private final int top;
        private final int width;
        private final int height;
        private final List<CommandSuggestResponse.Entry> entries;
        /** The input the suggestions were made for; applying one always starts from it. */
        private final String original;
        private int scroll;
        private int selected;
        private int lastMouseX;
        private int lastMouseY;
        /** After a suggestion is applied, the next Tab moves on to the next one. */
        private boolean cycleOnTab;

        SuggestionList(int x, int y, int width, List<CommandSuggestResponse.Entry> entries) {
            int rows = Math.min(entries.size(), MAX_ROWS);
            this.left = x - 1;
            this.top = y - 3 - rows * ROW_HEIGHT;
            this.width = width + 1;
            this.height = rows * ROW_HEIGHT;
            this.entries = entries;
            this.original = ModernChat.this.input.getText();
            this.select(0);
        }

        void render(int mouseX, int mouseY) {
            int rows = Math.min(this.entries.size(), MAX_ROWS);
            boolean moreAbove = this.scroll > 0;
            boolean moreBelow = this.entries.size() > this.scroll + rows;
            boolean mouseMoved = this.lastMouseX != mouseX || this.lastMouseY != mouseY;
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            if (moreAbove || moreBelow) {
                Gui.drawRect(this.left, this.top - 1, this.left + this.width, this.top, LIST_BACKGROUND);
                Gui.drawRect(this.left, this.top + this.height, this.left + this.width, this.top + this.height + 1, LIST_BACKGROUND);
                for (int x = 0; x < this.width; x += 2) {
                    if (moreAbove) Gui.drawRect(this.left + x, this.top - 1, this.left + x + 1, this.top, 0xFFFFFFFF);
                    if (moreBelow) Gui.drawRect(this.left + x, this.top + this.height, this.left + x + 1, this.top + this.height + 1, 0xFFFFFFFF);
                }
            }
            boolean hovering = false;
            for (int row = 0; row < rows; row++) {
                int index = row + this.scroll;
                int rowTop = this.top + ROW_HEIGHT * row;
                Gui.drawRect(this.left, rowTop, this.left + this.width, rowTop + ROW_HEIGHT, LIST_BACKGROUND);
                if (mouseX > this.left && mouseX < this.left + this.width && mouseY > rowTop && mouseY < rowTop + ROW_HEIGHT) {
                    if (mouseMoved) this.select(index);
                    hovering = true;
                }
                ModernChat.this.font.drawStringWithShadow(this.entries.get(index).text, this.left + 1, rowTop + 2, index == this.selected ? 0xFFFFFF00 : 0xFFAAAAAA);
            }
            ITextComponent tooltip = this.entries.get(this.selected).tooltip;
            if (hovering && tooltip != null) ModernChat.this.screen.drawHoveringText(tooltip.getFormattedText(), mouseX, mouseY);
        }

        boolean keyPressed(int keyCode) {
            switch (keyCode) {
                case Keyboard.KEY_UP:
                    this.cycle(-1);
                    this.cycleOnTab = false;
                    return true;
                case Keyboard.KEY_DOWN:
                    this.cycle(1);
                    this.cycleOnTab = false;
                    return true;
                case Keyboard.KEY_TAB:
                    if (this.cycleOnTab) this.cycle(GuiScreen.isShiftKeyDown() ? -1 : 1);
                    this.apply();
                    return true;
                case Keyboard.KEY_ESCAPE:
                    ModernChat.this.list = null;
                    return true;
                default:
                    return false;
            }
        }

        boolean mouseClicked(int mouseX, int mouseY) {
            if (!this.contains(mouseX, mouseY)) return false;
            int index = (mouseY - this.top) / ROW_HEIGHT + this.scroll;
            if (index >= 0 && index < this.entries.size()) {
                this.select(index);
                this.apply();
            }
            return true;
        }

        boolean mouseScrolled(int direction, int mouseX, int mouseY) {
            if (!this.contains(mouseX, mouseY)) return false;
            this.scroll = MathHelper.clamp(this.scroll - direction, 0, Math.max(this.entries.size() - MAX_ROWS, 0));
            return true;
        }

        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= this.left && mouseX < this.left + this.width && mouseY >= this.top && mouseY < this.top + this.height;
        }

        private void cycle(int by) {
            this.select(this.selected + by);
            int lastShown = this.scroll + MAX_ROWS - 1;
            int maxScroll = Math.max(this.entries.size() - MAX_ROWS, 0);
            if (this.selected < this.scroll) {
                this.scroll = MathHelper.clamp(this.selected, 0, maxScroll);
            } else if (this.selected > lastShown) {
                this.scroll = MathHelper.clamp(this.selected + 1 - MAX_ROWS, 0, maxScroll);
            }
        }

        private void select(int index) {
            int size = this.entries.size();
            this.selected = ((index % size) + size) % size;
            ModernChat.this.field.portium$setSuggestion(this.suffixFor(ModernChat.this.input.getText()));
        }

        /** The rest of the selected suggestion after what's typed, if the suggestion continues it. */
        @Nullable
        String suffixFor(String text) {
            String applied = this.entries.get(this.selected).apply(this.original);
            return applied.startsWith(text) ? applied.substring(text.length()) : null;
        }

        private void apply() {
            CommandSuggestResponse.Entry entry = this.entries.get(this.selected);
            ModernChat.this.applyingSuggestion = true;
            ModernChat.this.input.setText(entry.apply(this.original));
            int cursor = entry.start + entry.text.length();
            ModernChat.this.input.setCursorPosition(cursor);
            ModernChat.this.input.setSelectionPos(cursor);
            this.select(this.selected);
            ModernChat.this.applyingSuggestion = false;
            this.cycleOnTab = true;
        }
    }
}
