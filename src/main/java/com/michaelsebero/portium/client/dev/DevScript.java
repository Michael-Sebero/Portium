package com.michaelsebero.portium.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * A development aid for testing in the client without touching the desktop's mouse or keyboard. With
 * {@code -Dportium.dev.script=<file>} (and {@code -Dportium.dev.world=<save folder>}, and {@code -Dportium.dev.fresh=true}
 * to start it over as a new flat world, or a default one with {@code -Dportium.dev.seed=<seed>}), the client opens the world and
 * runs the file's lines once the player is in it, one per tick unless told to wait:
 * <pre>
 * cmd /time set day           a chat command, as the player
 * look 0 45                   face yaw and pitch
 * slot 2                      select a hotbar slot (0-8)
 * use 10 64 10 up             right-click a block's face, as the mouse would
 * wait 40                     wait some ticks
 * shot name                   save screenshots/name.png
 * gui mods / gui close         open Forge's mod list, or close the open screen
 * gui video                   open the video settings
 * gui title                   open the title screen
 * gui create                  open the Create World screen
 * scroll 100                  scroll the open screen's list
 * blend 2                     set the Biome Blend radius (0-7), as its slider does, and save the options
 * state                       log whether the player sleeps, and where
 * biome                       log the biome at the player, the block under them and the fog colour
 * translate key               log a translation key's text, as the client and the server have it
 * log text                    write to the log
 * quit                        close the game
 * </pre>
 */
public final class DevScript {

    private static final Logger LOGGER = LogManager.getLogger("Portium/DevScript");

    private final Deque<String> lines;
    private final String world;
    private boolean loading;
    private int waiting;

    private DevScript(List<String> lines, String world) {
        this.lines = new ArrayDeque<>(lines);
        this.world = world;
    }

    /** Starts the script named by the system property, if any. */
    public static void install() {
        String script = System.getProperty("portium.dev.script");
        String world = System.getProperty("portium.dev.world");
        if (script == null || world == null) return;
        try {
            MinecraftForge.EVENT_BUS.register(new DevScript(Files.readAllLines(new File(script).toPath(), StandardCharsets.UTF_8), world));
            LOGGER.info("Running {} in world {}", script, world);
        } catch (IOException e) {
            LOGGER.error("Couldn't read {}", script, e);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            String screen = mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getName();
            if (!screen.equals(this.lastScreen)) {
                LOGGER.info("  waiting on screen {}", screen);
                this.lastScreen = screen;
            }
            // Portium's notice of 1.13 mod problems: they're in the log already.
            if (mc.currentScreen instanceof com.michaelsebero.portium.modern.client.ModernFailureScreen) {
                ((com.michaelsebero.portium.modern.client.ModernFailureScreen) mc.currentScreen).proceed();
                return;
            }
            if (!this.loading && mc.currentScreen instanceof GuiMainMenu) {
                this.loading = true;
                // A fresh world: deleted, then made again flat, in creative, with cheats.
                WorldSettings settings = null;
                if (Boolean.getBoolean("portium.dev.fresh")) {
                    mc.getSaveLoader().deleteWorldDirectory(this.world);
                    String seed = System.getProperty("portium.dev.seed");
                    settings = seed == null ? new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands()
                            : new WorldSettings(Long.parseLong(seed), GameType.CREATIVE, true, false, WorldType.DEFAULT).enableCommands();
                }
                mc.launchIntegratedServer(this.world, this.world, settings);
            }
            return;
        }
        if (this.waiting > 0) {
            this.waiting--;
            return;
        }
        String line = this.lines.poll();
        if (line == null) return;
        line = line.trim();
        if (this.release != null) {
            net.minecraft.client.settings.KeyBinding.setKeyBindState(this.release.getKeyCode(), false);
            this.release = null;
        }
        if (line.isEmpty() || line.startsWith("#")) return;
        try {
            run(mc, line);
        } catch (RuntimeException e) {
            LOGGER.error("Script line failed: {}", line, e);
        }
    }

    /** Every message the player gets, action bar included, so a script's outcome shows in the log. */
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        LOGGER.info("  [{}] {}", event.getType(), event.getMessage().getUnformattedText());
    }

    private String lastScreen = "";

    /** A position from three coordinates, each absolute or relative to the player's block ({@code ~}, {@code ~2}). */
    private static BlockPos at(Minecraft mc, String x, String y, String z) {
        BlockPos player = new BlockPos(mc.player);
        return new BlockPos(coordinate(x, player.getX()), coordinate(y, player.getY()), coordinate(z, player.getZ()));
    }

    private static int coordinate(String word, int base) {
        if (!word.startsWith("~")) return Integer.parseInt(word);
        return word.length() == 1 ? base : base + Integer.parseInt(word.substring(1));
    }

    /** A key binding pressed by the last line, let go of before the next. */
    private net.minecraft.client.settings.KeyBinding release;

    private void run(Minecraft mc, String line) {
        String[] words = line.split("\\s+");
        LOGGER.info("> {}", line);
        switch (words[0]) {
            case "cmd":
                mc.player.sendChatMessage(line.substring(4).trim());
                break;
            case "look":
                mc.player.rotationYaw = Float.parseFloat(words[1]);
                mc.player.rotationPitch = Float.parseFloat(words[2]);
                break;
            case "slot":
                mc.player.inventory.currentItem = Integer.parseInt(words[1]);
                break;
            case "use": {
                BlockPos pos = at(mc, words[1], words[2], words[3]);
                EnumFacing side = EnumFacing.byName(words[4].toLowerCase(Locale.ROOT));
                Vec3d hit = new Vec3d(pos).add(0.5D + side.getXOffset() * 0.5D, 0.5D + side.getYOffset() * 0.5D, 0.5D + side.getZOffset() * 0.5D);
                LOGGER.info("  use -> {}", mc.playerController.processRightClickBlock(mc.player, mc.world, pos, side, hit, EnumHand.MAIN_HAND));
                break;
            }
            case "block": {
                // The state at a position on both sides, as the client and the integrated server see it.
                BlockPos pos = at(mc, words[1], words[2], words[3]);
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                Object serverState = server == null ? "?" : server.getWorld(mc.player.dimension).getBlockState(pos);
                LOGGER.info("  block {}: client {}, server {}", pos, mc.world.getBlockState(pos), serverState);
                break;
            }
            case "te": {
                // The server's tile entity at a position, and what it saves.
                BlockPos pos = at(mc, words[1], words[2], words[3]);
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                net.minecraft.tileentity.TileEntity tile = server == null ? null : server.getWorld(mc.player.dimension).getTileEntity(pos);
                if (tile == null) {
                    LOGGER.info("  te {}: none", pos);
                    break;
                }
                String saved;
                try {
                    saved = tile.writeToNBT(new net.minecraft.nbt.NBTTagCompound()).toString();
                } catch (RuntimeException | LinkageError e) {
                    saved = "(couldn't save: " + e + ")";
                }
                LOGGER.info("  te {}: {} {}", pos, tile.getClass().getName(), saved.length() > 600 ? saved.substring(0, 600) + "..." : saved);
                break;
            }
            case "hit": {
                net.minecraft.util.math.RayTraceResult over = mc.objectMouseOver;
                if (over == null || over.typeOfHit != net.minecraft.util.math.RayTraceResult.Type.BLOCK) {
                    LOGGER.info("  hit: {}", over == null ? "nothing" : over.typeOfHit);
                    break;
                }
                net.minecraft.block.state.IBlockState state = mc.world.getBlockState(over.getBlockPos());
                LOGGER.info("  hit {} {} side {} at {}, box {}", state, over.getBlockPos(), over.sideHit, over.hitVec,
                        state.getSelectedBoundingBox(mc.world, over.getBlockPos()));
                break;
            }
            case "wait":
                this.waiting = Integer.parseInt(words[1]);
                break;
            case "shot": {
                ITextComponent result = ScreenShotHelper.saveScreenshot(mc.gameDir, words[1] + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                LOGGER.info("  {}", result.getUnformattedText());
                break;
            }
            case "state":
                LOGGER.info("  sleeping {}, bed {}, at {}", mc.player.isPlayerSleeping(), mc.player.bedLocation, mc.player.getPosition());
                break;
            case "biome": {
                BlockPos at = mc.player.getPosition();
                net.minecraft.util.math.Vec3d fog = mc.world.provider.getFogColor(mc.world.getCelestialAngle(1.0F), 1.0F);
                LOGGER.info("  biome {} at {}, standing on {}, fog {} {} {}", mc.world.getBiome(at).getRegistryName(), at,
                        mc.world.getBlockState(at.down()).getBlock().getRegistryName(), String.format("%.3f", fog.x), String.format("%.3f", fog.y),
                        String.format("%.3f", fog.z));
                break;
            }
            case "translate":
                LOGGER.info("  {} -> client \"{}\", server \"{}\"", words[1], I18n.format(words[1]),
                        net.minecraft.util.text.translation.I18n.translateToLocal(words[1]));
                break;
            case "gui":
                if ("mods".equals(words[1])) {
                    GuiModList list = new GuiModList(mc.currentScreen);
                    mc.displayGuiScreen(list);
                    try {
                        java.lang.reflect.Field field = GuiModList.class.getDeclaredField("mods");
                        field.setAccessible(true);
                        StringBuilder names = new StringBuilder();
                        for (Object mod : (List<?>) field.get(list)) names.append(((net.minecraftforge.fml.common.ModContainer) mod).getName()).append("; ");
                        LOGGER.info("  mod list: {}", names);
                    } catch (ReflectiveOperationException e) {
                        LOGGER.warn("Couldn't read the mod list", e);
                    }
                }
                else if ("video".equals(words[1])) mc.displayGuiScreen(new net.minecraft.client.gui.GuiVideoSettings(mc.currentScreen, mc.gameSettings));
                else if ("title".equals(words[1])) mc.displayGuiScreen(new GuiMainMenu());
                else if ("create".equals(words[1])) mc.displayGuiScreen(new net.minecraft.client.gui.GuiCreateWorld(mc.currentScreen));
                else if ("close".equals(words[1])) mc.displayGuiScreen(null);
                break;
            case "key": {
                // A key binding pressed inside the game, by its description (e.g. key.inventory): no desktop input.
                net.minecraft.client.settings.KeyBinding found = null;
                for (net.minecraft.client.settings.KeyBinding binding : mc.gameSettings.keyBindings) {
                    if (binding.getKeyDescription().equals(words[1])) found = binding;
                }
                if (found == null) {
                    LOGGER.warn("  no key binding {}", words[1]);
                    break;
                }
                net.minecraft.client.settings.KeyBinding.setKeyBindState(found.getKeyCode(), true);
                net.minecraft.client.settings.KeyBinding.onTick(found.getKeyCode());
                this.release = found;
                LOGGER.info("  pressed {} ({})", words[1], found.getKeyCode());
                break;
            }
            case "scroll":
                if (mc.currentScreen != null) {
                    for (java.lang.reflect.Field field : mc.currentScreen.getClass().getDeclaredFields()) {
                        if (!net.minecraft.client.gui.GuiSlot.class.isAssignableFrom(field.getType())) continue;
                        try {
                            field.setAccessible(true);
                            ((net.minecraft.client.gui.GuiSlot) field.get(mc.currentScreen)).scrollBy(Integer.parseInt(words[1]));
                        } catch (ReflectiveOperationException e) {
                            LOGGER.warn("Couldn't scroll", e);
                        }
                    }
                }
                break;
            case "blend":
                mc.gameSettings.setOptionFloatValue(com.michaelsebero.portium.client.BiomeBlend.option(), Integer.parseInt(words[1]));
                LOGGER.info("  {}", mc.gameSettings.getKeyBinding(com.michaelsebero.portium.client.BiomeBlend.option()));
                mc.gameSettings.saveOptions();
                break;
            case "keys":
                for (net.minecraft.client.settings.KeyBinding binding : mc.gameSettings.keyBindings) LOGGER.info("  key {} = {}", binding.getKeyDescription(), binding.getKeyCode());
                break;
            case "respawn":
                mc.player.respawnPlayer();
                mc.displayGuiScreen(null);
                break;
            case "useitem": {
                // Right-click with the held item at nothing (an item that opens a screen, a bow).
                net.minecraft.util.EnumActionResult result = mc.playerController.processRightClick(mc.player, mc.world, net.minecraft.util.EnumHand.MAIN_HAND);
                LOGGER.info("  useitem -> {}", result);
                break;
            }
            case "close":
                // Closes the open screen, as Escape does.
                if (mc.currentScreen != null) mc.player.closeScreen();
                break;
            case "click":
                // A click on the open screen at scaled screen coordinates, through its own input calls.
                click(Integer.parseInt(words[1]), Integer.parseInt(words[2]), words.length > 3 ? Integer.parseInt(words[3]) : 0);
                break;
            case "clickslot": {
                // A click on a slot of the open container screen, at the slot's middle.
                if (!(mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer)) {
                    LOGGER.warn("  no container screen");
                    break;
                }
                net.minecraft.client.gui.inventory.GuiContainer screen = (net.minecraft.client.gui.inventory.GuiContainer) mc.currentScreen;
                net.minecraft.inventory.Slot slot = screen.inventorySlots.getSlot(Integer.parseInt(words[1]));
                click(screen.getGuiLeft() + slot.xPos + 8, screen.getGuiTop() + slot.yPos + 8, words.length > 2 ? Integer.parseInt(words[2]) : 0);
                break;
            }
            case "slots": {
                // The open container's stacks (and the one held by the cursor).
                net.minecraft.inventory.Container container = mc.player.openContainer;
                StringBuilder stacks = new StringBuilder();
                for (int i = 0; i < container.inventorySlots.size(); i++) {
                    net.minecraft.item.ItemStack stack = container.getSlot(i).getStack();
                    if (!stack.isEmpty()) stacks.append(i).append('=').append(stack.getCount()).append(' ').append(stack.getItem().getRegistryName()).append("; ");
                }
                LOGGER.info("  {} ({} slots): {} cursor={}", container.getClass().getSimpleName(), container.inventorySlots.size(), stacks,
                        mc.player.inventory.getItemStack());
                break;
            }
            case "count": {
                // Blocks whose name contains a text, in the server's world within a horizontal radius of the player (y 0 to 127).
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                net.minecraft.world.WorldServer world = server.getWorld(mc.player.dimension);
                int radius = Integer.parseInt(words[2]);
                BlockPos center = new BlockPos(mc.player);
                Map<String, Integer> counts = new java.util.TreeMap<>();
                BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
                for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                    for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                        if (!world.isBlockLoaded(at.setPos(x, 0, z))) continue;
                        for (int y = 0; y < 128; y++) {
                            net.minecraft.block.Block block = world.getBlockState(at.setPos(x, y, z)).getBlock();
                            String name = String.valueOf(block.getRegistryName());
                            if (name.contains(words[1])) counts.merge(name, 1, Integer::sum);
                        }
                    }
                }
                LOGGER.info("  count {} within {}: {}", words[1], radius, counts);
                break;
            }
            case "layers":
                // The player renderers' layers.
                for (Map.Entry<String, net.minecraft.client.renderer.entity.RenderPlayer> entry : mc.getRenderManager().getSkinMap().entrySet()) {
                    List<?> layers = net.minecraftforge.fml.common.ObfuscationReflectionHelper.getPrivateValue(
                            net.minecraft.client.renderer.entity.RenderLivingBase.class, entry.getValue(), "field_177097_h");
                    StringBuilder names = new StringBuilder();
                    for (Object layer : layers) names.append(layer.getClass().getSimpleName()).append(' ');
                    LOGGER.info("  {}: {}", entry.getKey(), names);
                }
                break;
            case "held": {
                net.minecraft.item.ItemStack stack = mc.player.getHeldItemMainhand();
                LOGGER.info("  held: {} {}", stack, stack.getTagCompound());
                break;
            }
            case "screen":
                LOGGER.info("  screen: {}", mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getName());
                break;
            case "log":
                LOGGER.info("  {}", line.substring(4));
                break;
            case "quit":
                mc.shutdown();
                break;
            default:
                LOGGER.warn("Unknown script line: {}", line);
        }
    }

    /** A press and release of a mouse button on the open screen, at scaled coordinates. */
    private static void click(int x, int y, int button) {
        net.minecraft.client.gui.GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen == null) {
            LOGGER.warn("  no screen to click");
            return;
        }
        try {
            java.lang.reflect.Method pressed = net.minecraftforge.fml.common.ObfuscationReflectionHelper.findMethod(net.minecraft.client.gui.GuiScreen.class,
                    "func_73864_a", void.class, int.class, int.class, int.class);
            java.lang.reflect.Method released = net.minecraftforge.fml.common.ObfuscationReflectionHelper.findMethod(net.minecraft.client.gui.GuiScreen.class,
                    "func_146286_b", void.class, int.class, int.class, int.class);
            pressed.invoke(screen, x, y, button);
            released.invoke(screen, x, y, button);
            LOGGER.info("  clicked {} {} with {}", x, y, button);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("  click failed", e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e);
        }
    }
}
