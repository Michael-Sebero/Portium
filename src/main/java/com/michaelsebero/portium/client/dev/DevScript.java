package com.michaelsebero.portium.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.block.state.IBlockState;
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
        if (this.pondering > 0) {
            // A hovered item's tooltip, built each tick as a screen builds it while the mouse is over the item.
            net.minecraft.client.gui.GuiScreen screen = mc.currentScreen != null ? mc.currentScreen : new net.minecraft.client.gui.GuiScreen() {};
            if (mc.currentScreen == null) screen.setWorldAndResolution(mc, 400, 300);
            try {
                if (this.pondering % 10 == 1) run(mc, "static com.simibubi.create.foundation.ponder.PonderTooltipHandler deferTick holdWProgress");
                if (this.pondering % 10 == 1) run(mc, "static com.simibubi.create.foundation.utility.AnimationTickHolder ticks paused_ticks");
                List<String> lines = screen.getItemToolTip(this.ponderStack);
                if (--this.pondering == 0 || this.pondering % 10 == 0) {
                    LOGGER.info("  hovering {}: {} (W down for newer mods: {})", this.ponderStack, lines.subList(Math.max(0, lines.size() - 2), lines.size()),
                            com.michaelsebero.portium.modern.client.ModernKeys.isKeyDown(87));
                    run(mc, "static com.simibubi.create.foundation.ponder.PonderTooltipHandler deferTick subject holdWProgress hoveredStack trackingStack");
                }
            } catch (RuntimeException | LinkageError e) {
                LOGGER.error("  hovering {} failed", this.ponderStack, e);
                this.pondering = 0;
            }
            if (this.pondering == 0) com.michaelsebero.portium.modern.client.ModernKeys.hold(org.lwjgl.input.Keyboard.KEY_W, false);
        }
        if (this.held != null) {
            if (this.heldTicks-- > 0) {
                net.minecraft.client.settings.KeyBinding.setKeyBindState(this.held.getKeyCode(), true);
            } else {
                net.minecraft.client.settings.KeyBinding.setKeyBindState(this.held.getKeyCode(), false);
                this.held = null;
            }
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

    /** Ticks left of hovering an item with W held ({@code ponderhold}), and the item. */
    private int pondering;
    /** A Create icon (AllIcons field) drawn over every screen, and how often to log GL state while doing so. */
    private String iconProbe;
    private int iconProbeLogs;

    @SubscribeEvent
    public void onDrawScreen(net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post event) {
        if (this.iconProbe == null) return;
        try {
            ClassLoader loader = com.michaelsebero.portium.mods116.ModernHooks.loader();
            Class<?> icons = Class.forName("com.simibubi.create.foundation.gui.AllIcons", true, loader);
            Object icon = icons.getField(this.iconProbe).get(null);
            Class<?> stackType = Class.forName("com.mojang.blaze3d.matrix.MatrixStack", true, loader);
            Object stack = stackType.getConstructor().newInstance();
            if (this.iconProbeLogs-- > 0) {
                LOGGER.info("  iconprobe GL before: tex2d {}, depth {}, alpha {}, blend {}, active unit {}, bound {}, stencil {}",
                        org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_TEXTURE_2D), org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST),
                        org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_ALPHA_TEST), org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND),
                        org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE) - org.lwjgl.opengl.GL13.GL_TEXTURE0,
                        org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D), org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_STENCIL_TEST));
            }
            icons.getMethod("draw", stackType, int.class, int.class).invoke(icon, stack, 4, 4);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("  iconprobe {} failed", this.iconProbe, e);
            this.iconProbe = null;
        }
    }
    private net.minecraft.item.ItemStack ponderStack = net.minecraft.item.ItemStack.EMPTY;

    /** A key binding pressed by the last line, let go of before the next. */
    private net.minecraft.client.settings.KeyBinding release;
    /** A key binding held down for some ticks ({@code holdkey}), whatever the script does meanwhile. */
    private net.minecraft.client.settings.KeyBinding held;
    private int heldTicks;

    private void run(Minecraft mc, String line) {
        String[] words = line.split("\\s+");
        LOGGER.info("> {}", line);
        switch (words[0]) {
            case "cmd":
                mc.player.sendChatMessage(line.substring(4).trim());
                break;
            case "scmd": {
                // A command run on the integrated server as the player, past chat's 256-character limit.
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                String command = line.substring(5).trim();
                java.util.UUID player = mc.player.getUniqueID();
                server.addScheduledTask(() -> {
                    net.minecraft.entity.player.EntityPlayerMP sender = server.getPlayerList().getPlayerByUUID(player);
                    int result = server.getCommandManager().executeCommand(sender != null ? sender : server, command);
                    LOGGER.info("  scmd -> {}", result);
                });
                break;
            }
            case "look":
                mc.player.rotationYaw = Float.parseFloat(words[1]);
                mc.player.rotationPitch = Float.parseFloat(words[2]);
                break;
            case "craft": {
                // What a crafting grid makes: craft <width> <height> <item[:meta]|-> ... (row by row), as the server matches it.
                int width = Integer.parseInt(words[1]);
                int height = Integer.parseInt(words[2]);
                net.minecraft.inventory.InventoryCrafting grid = new net.minecraft.inventory.InventoryCrafting(new net.minecraft.inventory.Container() {
                    @Override
                    public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
                        return true;
                    }
                }, width, height);
                for (int i = 0; i < width * height && 3 + i < words.length; i++) {
                    String name = words[3 + i];
                    if (name.equals("-")) continue;
                    String[] parts = name.split("@");
                    net.minecraft.item.Item item = net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation(parts[0]));
                    grid.setInventorySlotContents(i, new net.minecraft.item.ItemStack(item, 1, parts.length > 1 ? Integer.parseInt(parts[1]) : 0));
                }
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                net.minecraft.world.World world = server == null ? mc.world : server.getWorld(mc.player.dimension);
                net.minecraft.item.crafting.IRecipe recipe = com.michaelsebero.portium.datapack.DataPackRecipes.findMatching(grid, world);
                if (recipe == null) recipe = net.minecraft.item.crafting.CraftingManager.findMatchingRecipe(grid, world);
                LOGGER.info("  craft {} -> {} ({})", line.substring(6), recipe == null ? "nothing" : recipe.getCraftingResult(grid),
                        recipe == null ? "-" : recipe.getRegistryName());
                break;
            }
            case "findbiome": {
                // Moves the player to the nearest spot of a biome (findbiome <id> <radius>), on the server's surface there.
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                net.minecraft.entity.player.EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
                net.minecraft.world.WorldServer world = server.getWorld(mc.player.dimension);
                net.minecraft.world.biome.Biome biome = net.minecraftforge.fml.common.registry.ForgeRegistries.BIOMES.getValue(new net.minecraft.util.ResourceLocation(words[1]));
                BlockPos found = biome == null ? null : world.getBiomeProvider().findBiomePosition((int) player.posX, (int) player.posZ,
                        Integer.parseInt(words[2]), java.util.Collections.singletonList(biome), new java.util.Random(world.getSeed()));
                if (found == null) {
                    LOGGER.info("  findbiome {}: none", words[1]);
                    break;
                }
                BlockPos top = world.getTopSolidOrLiquidBlock(found);
                player.setPositionAndUpdate(top.getX() + 0.5D, top.getY() + 2.0D, top.getZ() + 0.5D);
                LOGGER.info("  findbiome {}: {}", words[1], top);
                break;
            }
            case "genfeature": {
                // Runs one of Portium's world-gen features at a spot: genfeature berries|bamboo x y z.
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                net.minecraft.world.WorldServer world = server.getWorld(mc.player.dimension);
                BlockPos pos = at(mc, words[2], words[3], words[4]);
                java.util.Random random = new java.util.Random(pos.toLong());
                if (words[1].equals("berries")) com.michaelsebero.portium.world.gen.plant.Plants114.berryPatch(world, random, pos);
                else if (words[1].equals("bamboo")) com.michaelsebero.portium.world.gen.plant.Plants114.bamboo(world, random, pos);
                boolean grew = true;
                if (words[1].equals("megapine")) grew = new net.minecraft.world.gen.feature.WorldGenMegaPineTree(false, false).generate(world, random, pos);
                else if (words[1].equals("megaspruce")) grew = new net.minecraft.world.gen.feature.WorldGenMegaPineTree(false, true).generate(world, random, pos);
                LOGGER.info("  genfeature {} at {}: {} (on {})", words[1], pos, grew, world.getBlockState(pos.down()));
                if (words[1].startsWith("mega")) {
                    // The trunk's four columns: how high each reaches, and what tops the tallest.
                    StringBuilder tops = new StringBuilder();
                    for (BlockPos column : new BlockPos[] {pos, pos.east(), pos.south(), pos.south().east()}) {
                        BlockPos at = column;
                        while (world.getBlockState(at.up()).getBlock() instanceof net.minecraft.block.BlockLog) at = at.up();
                        tops.append(at.getY() - pos.getY()).append(' ').append(world.getBlockState(at.up()).getBlock().getRegistryName()).append("; ");
                    }
                    LOGGER.info("  trunk tops (height, above): {}", tops);
                }
                break;
            }
            case "plantscan": {
                // How many of Portium's new plants grow around the player, per biome (plantscan <radius>), in loaded chunks.
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                net.minecraft.world.WorldServer world = server.getWorld(mc.player.dimension);
                int radius = Integer.parseInt(words[1]);
                BlockPos center = new BlockPos(mc.player);
                Map<String, Integer> counts = new java.util.TreeMap<>();
                BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
                for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                    for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                        if (!world.isBlockLoaded(at.setPos(x, 0, z))) continue;
                        String biomeName = String.valueOf(world.getBiome(at).getRegistryName());
                        counts.merge("columns " + biomeName, 1, Integer::sum);
                        for (int y = 1; y < 128; y++) {
                            net.minecraft.block.Block block = world.getBlockState(at.setPos(x, y, z)).getBlock();
                            String name = String.valueOf(block.getRegistryName());
                            if (name.equals("portium:cornflower") || name.equals("portium:lily_of_the_valley") || name.equals("portium:sweet_berry_bush")
                                    || name.equals("portium:bamboo") || name.equals("minecraft:red_flower") || name.equals("minecraft:yellow_flower")) {
                                counts.merge(name + " in " + biomeName, 1, Integer::sum);
                            }
                        }
                    }
                }
                LOGGER.info("  plantscan {}: {}", radius, counts);
                break;
            }
            case "aim": {
                // Turns the player to look at a block's centre (or a face of it), for items used at what's looked at.
                BlockPos pos = at(mc, words[1], words[2], words[3]);
                // The middle of the block's outline (a campfire is under half a block tall), or of one of its faces.
                net.minecraft.util.math.AxisAlignedBB box = mc.world.getBlockState(pos).getBoundingBox(mc.world, pos).offset(pos);
                Vec3d target = box.getCenter();
                if (words.length > 4) {
                    EnumFacing side = EnumFacing.byName(words[4].toLowerCase(Locale.ROOT));
                    double half = side.getAxis() == EnumFacing.Axis.X ? (box.maxX - box.minX) / 2 : side.getAxis() == EnumFacing.Axis.Y
                            ? (box.maxY - box.minY) / 2 : (box.maxZ - box.minZ) / 2;
                    double reach = Math.max(0.0D, half - 0.01D);
                    target = target.add(side.getXOffset() * reach, side.getYOffset() * reach, side.getZOffset() * reach);
                }
                Vec3d eyes = mc.player.getPositionEyes(1.0F);
                double dx = target.x - eyes.x, dy = target.y - eyes.y, dz = target.z - eyes.z;
                mc.player.rotationYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
                mc.player.rotationPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
                mc.player.prevRotationYaw = mc.player.rotationYaw;
                mc.player.prevRotationPitch = mc.player.rotationPitch;
                break;
            }
            case "slot":
                mc.player.inventory.currentItem = Integer.parseInt(words[1]);
                break;
            case "hold": {
                // An item in the selected hotbar slot, as the creative inventory puts it there.
                net.minecraft.item.Item item = net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation(words[1]));
                if (item == null || item == net.minecraft.init.Items.AIR) {
                    LOGGER.info("  hold: no item {}", words[1]);
                    break;
                }
                net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item);
                mc.player.inventory.setInventorySlotContents(mc.player.inventory.currentItem, stack);
                mc.playerController.sendSlotPacket(stack, 36 + mc.player.inventory.currentItem);
                LOGGER.info("  holding {}", words[1]);
                break;
            }
            case "use": {
                BlockPos pos = at(mc, words[1], words[2], words[3]);
                EnumFacing side = EnumFacing.byName(words[4].toLowerCase(Locale.ROOT));
                Vec3d hit = new Vec3d(pos).add(0.5D + side.getXOffset() * 0.5D, 0.5D + side.getYOffset() * 0.5D, 0.5D + side.getZOffset() * 0.5D);
                try {
                    LOGGER.info("  use -> {}", mc.playerController.processRightClickBlock(mc.player, mc.world, pos, side, hit, EnumHand.MAIN_HAND));
                } catch (Throwable e) {
                    // Logged and carried on from, so one script finds every failing item.
                    LOGGER.error("  use failed", e);
                }
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
            case "fluid": {
                // The fluid state at a position on the server, as newer code sees it, and the way it flows.
                BlockPos pos = at(mc, words[1], words[2], words[3]);
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                net.minecraft.world.World world = server == null ? mc.world : server.getWorld(mc.player.dimension);
                try {
                    com.michaelsebero.portium.modern.mc.fluid.IFluidState fluid = com.michaelsebero.portium.modern.shim.FluidShims.getFluidState(world, pos);
                    LOGGER.info("  fluid {}: {} ({}), flow {}", pos, fluid, fluid.getFluid(), fluid.getFlow(world, pos));
                } catch (RuntimeException | LinkageError e) {
                    LOGGER.warn("  fluid {} failed", pos, e);
                }
                break;
            }
            case "guikey": {
                // A key typed into the open screen, as 1.12 hands it on (by LWJGL 2's key name: E, ESCAPE, W).
                if (mc.currentScreen == null) {
                    LOGGER.info("  guikey {}: no screen", words[1]);
                    break;
                }
                int code = org.lwjgl.input.Keyboard.getKeyIndex(words[1].toUpperCase(Locale.ROOT));
                try {
                    java.lang.reflect.Method typed = net.minecraft.client.gui.GuiScreen.class.getDeclaredMethod(
                            Boolean.TRUE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")) ? "keyTyped" : "func_73869_a",
                            char.class, int.class);
                    typed.setAccessible(true);
                    typed.invoke(mc.currentScreen, (char) 0, code);
                    LOGGER.info("  guikey {} ({}): screen now {}", words[1], code, mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getName());
                } catch (ReflectiveOperationException e) {
                    LOGGER.warn("  guikey {} failed", words[1], e);
                }
                break;
            }
            case "static": {
                // Static fields of a newer mod's class (loaded by Portium's 1.16.5 loader), for a mod's own state.
                try {
                    Class<?> type = Class.forName(words[1], false, com.michaelsebero.portium.mods116.ModernHooks.loader());
                    StringBuilder values = new StringBuilder();
                    for (int i = 2; i < words.length; i++) {
                        java.lang.reflect.Field field = type.getDeclaredField(words[i]);
                        field.setAccessible(true);
                        Object value = field.get(null);
                        // A LerpedFloat or the like: its value, by its own getValue().
                        if (value != null && !(value instanceof Number || value instanceof Boolean || value instanceof CharSequence)) {
                            try {
                                java.lang.reflect.Method getter = value.getClass().getMethod("getValue");
                                value = value + " = " + getter.invoke(value);
                            } catch (NoSuchMethodException e) {
                                // As it is.
                            }
                        }
                        values.append(words[i]).append('=').append(value).append(' ');
                    }
                    LOGGER.info("  static {}: {}", words[1], values.toString().trim());
                } catch (ReflectiveOperationException | LinkageError e) {
                    LOGGER.warn("  static {}: {}", words[1], e.toString());
                }
                break;
            }
            case "glowcheck": {
                // Glowstone near the player with air below it, and how many of those air blocks aren't lit by it.
                int radius = Integer.parseInt(words[1]);
                BlockPos center = new BlockPos(mc.player);
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                net.minecraft.world.World serverWorld = server == null ? null : server.getWorld(mc.player.dimension);
                int found = 0, serverDark = 0, clientDark = 0;
                BlockPos firstDark = null;
                for (BlockPos pos : BlockPos.getAllInBoxMutable(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
                    if (mc.world.getBlockState(pos).getBlock() != net.minecraft.init.Blocks.GLOWSTONE) continue;
                    BlockPos below = pos.down();
                    if (!mc.world.isAirBlock(below)) continue;
                    found++;
                    if (serverWorld != null && serverWorld.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, below) < 13) {
                        serverDark++;
                        if (firstDark == null) firstDark = below.toImmutable();
                    }
                    if (mc.world.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, below) < 13) clientDark++;
                }
                LOGGER.info("  glowcheck {}: {} glowstone over air, unlit below on the server {}, on the client {}, first {}", radius, found, serverDark,
                        clientDark, firstDark);
                break;
            }
            case "chunkconvert": {
                // A made-up 1.16 chunk (stone to y 59, water to y 63, air above) through Portium's conversion: the sky light down a column.
                net.minecraft.nbt.NBTTagCompound root = new net.minecraft.nbt.NBTTagCompound();
                root.setInteger("DataVersion", 2586);
                net.minecraft.nbt.NBTTagCompound level = new net.minecraft.nbt.NBTTagCompound();
                level.setString("Status", "full");
                net.minecraft.nbt.NBTTagList sections = new net.minecraft.nbt.NBTTagList();
                for (int sy = 0; sy < 4; sy++) {
                    net.minecraft.nbt.NBTTagCompound section = new net.minecraft.nbt.NBTTagCompound();
                    section.setByte("Y", (byte) sy);
                    net.minecraft.nbt.NBTTagList palette = new net.minecraft.nbt.NBTTagList();
                    for (String name : new String[] {"minecraft:air", "minecraft:stone", "minecraft:water"}) {
                        net.minecraft.nbt.NBTTagCompound entry = new net.minecraft.nbt.NBTTagCompound();
                        entry.setString("Name", name);
                        if (name.endsWith("water")) {
                            net.minecraft.nbt.NBTTagCompound properties = new net.minecraft.nbt.NBTTagCompound();
                            properties.setString("level", "0");
                            entry.setTag("Properties", properties);
                        }
                        palette.appendTag(entry);
                    }
                    section.setTag("Palette", palette);
                    long[] packed = new long[256];
                    for (int index = 0; index < 4096; index++) {
                        int y = (sy << 4) + (index >> 8);
                        long value = y < 58 ? 1 : 2;
                        packed[index / 16] |= value << (index % 16 * 4);
                    }
                    section.setTag("BlockStates", new net.minecraft.nbt.NBTTagLongArray(packed));
                    sections.appendTag(section);
                }
                level.setTag("Sections", sections);
                root.setTag("Level", level);
                net.minecraft.nbt.NBTTagCompound converted = com.michaelsebero.portium.compat.LegacyChunkNbt.convert(root).getCompoundTag("Level");
                StringBuilder column = new StringBuilder();
                net.minecraft.nbt.NBTTagList legacy = converted.getTagList("Sections", 10);
                for (int i = 0; i < legacy.tagCount(); i++) {
                    net.minecraft.nbt.NBTTagCompound section = legacy.getCompoundTagAt(i);
                    net.minecraft.world.chunk.NibbleArray sky = new net.minecraft.world.chunk.NibbleArray(section.getByteArray("SkyLight"));
                    for (int y = 15; y >= 0; y--) column.append((section.getByte("Y") << 4) + y).append('=').append(sky.get(3, y, 3)).append(' ');
                }
                LOGGER.info("  chunkconvert: LightPopulated {}, sky light at x3 z3 from the top: {}", converted.getBoolean("LightPopulated"), column);
                break;
            }
            case "iconprobe":
                this.iconProbe = words[1];
                this.iconProbeLogs = 3;
                break;
            case "ponderhold": {
                // Hovers an item (its tooltip each tick) with W held for newer mods, as a player asks Create to ponder.
                net.minecraft.item.Item item = net.minecraft.item.Item.getByNameOrId(words[1]);
                if (item == null) {
                    LOGGER.warn("  no item {}", words[1]);
                    break;
                }
                this.ponderStack = new net.minecraft.item.ItemStack(item);
                this.pondering = Integer.parseInt(words[2]);
                com.michaelsebero.portium.modern.client.ModernKeys.hold(org.lwjgl.input.Keyboard.KEY_W, true);
                break;
            }
            case "model": {
                // The model the chunk renderer draws for the client's block at a position, and its quads per side,
                // asked as chunk rendering asks (with the block's world and position) and without.
                BlockPos pos = at(mc, words[1], words[2], words[3]);
                IBlockState state = mc.world.getBlockState(pos);
                IBlockState drawn = state.getActualState(mc.world, pos);
                drawn = state.getBlock().getExtendedState(drawn, mc.world, pos);
                net.minecraft.client.renderer.block.model.IBakedModel model = mc.getBlockRendererDispatcher().getModelForState(drawn);
                StringBuilder sides = new StringBuilder();
                StringBuilder bare = new StringBuilder();
                com.michaelsebero.portium.modern.client.ModernRenderContext.begin(mc.world, pos);
                try {
                    for (net.minecraft.util.EnumFacing side : new net.minecraft.util.EnumFacing[] {null, net.minecraft.util.EnumFacing.UP,
                            net.minecraft.util.EnumFacing.NORTH}) {
                        sides.append(side).append('=').append(model.getQuads(drawn, side, 0L).size()).append(' ');
                    }
                } catch (RuntimeException | LinkageError e) {
                    sides.append("failed: ").append(e);
                } finally {
                    com.michaelsebero.portium.modern.client.ModernRenderContext.end();
                }
                try {
                    bare.append(model.getQuads(drawn, null, 0L).size());
                } catch (RuntimeException | LinkageError e) {
                    bare.append("failed: ").append(e);
                }
                LOGGER.info("  model {}: {} drawn as {} render {} model {} quads in world [{}] without context [{}]", pos, state, drawn, state.getRenderType(),
                        model.getClass().getName(), sides.toString().trim(), bare);
                LOGGER.info("    light: block {} sky {} combined {}, opacity {}, opaque cube {}, full cube {}, neighbour brightness {}",
                        mc.world.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, pos), mc.world.getLightFor(net.minecraft.world.EnumSkyBlock.SKY, pos),
                        Integer.toHexString(mc.world.getCombinedLight(pos, 0)), state.getLightOpacity(mc.world, pos), state.isOpaqueCube(), state.isFullCube(),
                        state.useNeighborBrightness());
                // How a Forge 1.16 model's model data methods look here, for ModelGlue's reflection.
                for (Class<?> c = model.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                    for (java.lang.reflect.Method method : c.getDeclaredMethods()) {
                        if (method.getName().equals("getModelData") || method.getName().equals("getQuads")) LOGGER.info("    {}", method);
                    }
                }
                break;
            }
            case "light": {
                // Block and sky light at a position, on the client and the integrated server.
                BlockPos pos = at(mc, words[1], words[2], words[3]);
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                net.minecraft.world.World serverWorld = server == null ? null : server.getWorld(mc.player.dimension);
                LOGGER.info("  light {}: client block {} sky {}, server block {} sky {}", pos,
                        mc.world.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, pos), mc.world.getLightFor(net.minecraft.world.EnumSkyBlock.SKY, pos),
                        serverWorld == null ? "?" : serverWorld.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, pos),
                        serverWorld == null ? "?" : serverWorld.getLightFor(net.minecraft.world.EnumSkyBlock.SKY, pos));
                break;
            }
            case "bell": {
                // A bell's swing as the client and the integrated server see it.
                BlockPos pos = at(mc, words[1], words[2], words[3]);
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                Object[] sides = {mc.world.getTileEntity(pos), server == null ? null : server.getWorld(mc.player.dimension).getTileEntity(pos)};
                StringBuilder out = new StringBuilder();
                for (int i = 0; i < 2; i++) {
                    out.append(i == 0 ? "client " : ", server ");
                    if (sides[i] instanceof com.michaelsebero.portium.tile.TileEntityBell) {
                        com.michaelsebero.portium.tile.TileEntityBell bell = (com.michaelsebero.portium.tile.TileEntityBell) sides[i];
                        out.append(bell.describe());
                    } else {
                        out.append("no bell");
                    }
                }
                LOGGER.info("  bell {}: {}", pos, out);
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
            case "tasks": {
                // The running AI tasks of the nearest server-side entity of a type: tasks <entity id>.
                net.minecraft.util.ResourceLocation type = new net.minecraft.util.ResourceLocation(words[1]);
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                net.minecraft.entity.EntityLiving nearest = null;
                for (net.minecraft.entity.Entity entity : server.getWorld(mc.player.dimension).loadedEntityList) {
                    if (!(entity instanceof net.minecraft.entity.EntityLiving) || !type.equals(net.minecraft.entity.EntityList.getKey(entity))) continue;
                    if (nearest == null || mc.player.getDistanceSq(entity) < mc.player.getDistanceSq(nearest)) nearest = (net.minecraft.entity.EntityLiving) entity;
                }
                if (nearest == null) {
                    LOGGER.info("  tasks {}: none", type);
                    break;
                }
                StringBuilder running = new StringBuilder();
                for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry entry : nearest.tasks.taskEntries) {
                    if (entry.using) running.append(entry.priority).append(':').append(entry.action.getClass().getSimpleName()).append(' ');
                }
                StringBuilder targets = new StringBuilder();
                for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry entry : nearest.targetTasks.taskEntries) {
                    if (entry.using) targets.append(entry.priority).append(':').append(entry.action.getClass().getSimpleName()).append(' ');
                }
                StringBuilder all = new StringBuilder();
                for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry entry : nearest.tasks.taskEntries) {
                    all.append(entry.priority).append(':').append(entry.action.getClass().getName().replaceAll(".*[.$]", "")).append(' ');
                }
                StringBuilder path = new StringBuilder();
                net.minecraft.pathfinding.Path current = nearest.getNavigator().getPath();
                if (current != null) {
                    for (int i = 0; i < current.getCurrentPathLength(); ++i) {
                        net.minecraft.pathfinding.PathPoint point = current.getPathPointFromIndex(i);
                        path.append(i == current.getCurrentPathIndex() ? "*" : "").append(point.x).append(',').append(point.y).append(',').append(point.z)
                                .append(':').append(point.nodeType).append(' ');
                    }
                }
                LOGGER.info("  tasks of {} at {}: [{}] targets [{}], path {} [{}], ai {}, all [{}]", type, nearest.getPositionVector(), running, targets,
                        nearest.getNavigator().noPath() ? "none" : "yes", path, !nearest.isAIDisabled(), all);
                break;
            }
            case "summondebug": {
                // Each step /summon takes for an entity id: the converted NBT and what 1.12 makes of it.
                net.minecraft.nbt.NBTTagCompound modern = new net.minecraft.nbt.NBTTagCompound();
                modern.setString("id", words[1]);
                net.minecraft.nbt.NBTTagCompound data = com.michaelsebero.portium.compat.LegacyEntityNbt.convert(modern);
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                net.minecraft.world.World world = server.getWorld(mc.player.dimension);
                Object made;
                try {
                    made = net.minecraft.entity.EntityList.createEntityFromNBT(data, world);
                } catch (Throwable e) {
                    made = e;
                    LOGGER.error("  summondebug failed", e);
                }
                net.minecraftforge.fml.common.registry.EntityEntry entry = net.minecraftforge.fml.common.registry.ForgeRegistries.ENTITIES
                        .getValue(new net.minecraft.util.ResourceLocation(data.getString("id")));
                LOGGER.info("  summondebug {} -> {} -> entry {} class {} -> {}", words[1], data, entry, entry == null ? null : entry.getEntityClass(), made);
                if (made instanceof net.minecraft.entity.Entity) {
                    net.minecraft.entity.Entity e = (net.minecraft.entity.Entity) made;
                    e.setLocationAndAngles(mc.player.posX, mc.player.posY, mc.player.posZ + 3, 0, 0);
                    net.minecraftforge.event.entity.EntityJoinWorldEvent join = new net.minecraftforge.event.entity.EntityJoinWorldEvent(e, world);
                    boolean canceled = net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(join);
                    LOGGER.info("  summondebug join event canceled {}, chunk loaded {}, spawnEntity {}", canceled,
                            world.isBlockLoaded(new BlockPos(e)), world.spawnEntity(e));
                }
                break;
            }
            case "raids": {
                // The raids of the player's world (server thread): raids.
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                int dimension = mc.player.dimension;
                server.addScheduledTask(() -> {
                    net.minecraft.world.WorldServer world = server.getWorld(dimension);
                    com.michaelsebero.portium.entity.raid.RaidManager116 manager = com.michaelsebero.portium.entity.raid.RaidManager116.get(world);
                    if (manager == null) {
                        LOGGER.info("  raids: none in this world");
                        return;
                    }
                    com.michaelsebero.portium.entity.raid.Raid116 raid = manager.findRaid(mc.player.getPosition(), 200.0D);
                    LOGGER.info("  raids: {}", raid == null ? "none near" : raid.describe().getText());
                    LOGGER.info("  raids: village here {}", com.michaelsebero.portium.world.Villages116.isVillage(world, new net.minecraft.util.math.BlockPos(mc.player)));
                });
                break;
            }
            case "villagers": {
                // 1.14 villagers within a radius: villagers <radius>. Data, activity, job site, home, sleep, offers. Read on the server thread.
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                double radius = Double.parseDouble(words[1]);
                net.minecraft.util.math.Vec3d at = mc.player.getPositionVector();
                int dimension = mc.player.dimension;
                server.addScheduledTask(() -> {
                    int found = 0;
                    for (net.minecraft.entity.Entity entity : server.getWorld(dimension).loadedEntityList) {
                        if (!(entity instanceof com.michaelsebero.portium.entity.villager.EntityVillager116) || entity.getPositionVector().distanceTo(at) > radius) continue;
                        com.michaelsebero.portium.entity.villager.EntityVillager116 villager = (com.michaelsebero.portium.entity.villager.EntityVillager116) entity;
                        com.michaelsebero.portium.entity.villager.VillagerData116 data = villager.getVillagerData();
                        com.michaelsebero.portium.entity.ai.brain.Brain<?> brain = villager.getBrain();
                        found++;
                        LOGGER.info("  villager {} at ({}, {}, {}): {} {} L{} xp {} child {} sleeping {} activities {} job {} potential {} home {} meet {} offers {} food {}",
                                villager.getEntityId(), String.format("%.1f", villager.posX), String.format("%.1f", villager.posY), String.format("%.1f", villager.posZ),
                                data.getType().getName(), data.getProfession().getName(), data.getLevel(), villager.getXp(), villager.isChild(), villager.isSleeping(),
                                brain.getActivities(), brain.getMemory(com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType.JOB_SITE).orElse(null),
                                brain.getMemory(com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType.POTENTIAL_JOB_SITE).orElse(null),
                                brain.getMemory(com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType.HOME).orElse(null),
                                brain.getMemory(com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType.MEETING_POINT).orElse(null),
                                villager.getOffers().size(), villager.getFoodLevel());
                    }
                    LOGGER.info("  villagers within {}: {} (of {} loaded entities)", radius, found, server.getWorld(dimension).loadedEntityList.size());
                });
                break;
            }
            case "brain": {
                // The activities and memories of the nearest server-side brain mob of a type: brain <entity id>.
                net.minecraft.util.ResourceLocation type = new net.minecraft.util.ResourceLocation(words[1]);
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                net.minecraft.entity.EntityLivingBase nearest = null;
                for (net.minecraft.entity.Entity entity : server.getWorld(mc.player.dimension).loadedEntityList) {
                    if (!(entity instanceof com.michaelsebero.portium.entity.ai.brain.IBrainHolder) || !type.equals(net.minecraft.entity.EntityList.getKey(entity))) continue;
                    if (nearest == null || mc.player.getDistanceSq(entity) < mc.player.getDistanceSq(nearest)) nearest = (net.minecraft.entity.EntityLivingBase) entity;
                }
                if (nearest == null) {
                    LOGGER.info("  brain {}: none", type);
                    break;
                }
                com.michaelsebero.portium.entity.ai.brain.Brain<?> brain = ((com.michaelsebero.portium.entity.ai.brain.IBrainHolder) nearest).getBrain();
                StringBuilder memories = new StringBuilder();
                brain.presentMemories().forEach((memory, value) -> {
                    String text = String.valueOf(value);
                    memories.append(memory.getName().replace("minecraft:", "")).append('=').append(text.length() > 60 ? text.substring(0, 60) : text).append("; ");
                });
                LOGGER.info("  brain of {} at {} hp {}: activities {} running {} memories [{}] main {} off {}", type, nearest.getPositionVector(), nearest.getHealth(),
                        brain.getActivities(), brain.getRunningTasks(), memories, nearest.getHeldItemMainhand(), nearest.getHeldItemOffhand());
                break;
            }
            case "interact": {
                // Right-click (main hand) the nearest entity of a type within 6 blocks: interact <entity id>.
                net.minecraft.util.ResourceLocation type = new net.minecraft.util.ResourceLocation(words[1]);
                net.minecraft.entity.Entity nearest = null;
                for (net.minecraft.entity.Entity entity : mc.world.loadedEntityList) {
                    if (!type.equals(net.minecraft.entity.EntityList.getKey(entity)) || mc.player.getDistance(entity) > 6.0F) continue;
                    if (nearest == null || mc.player.getDistance(entity) < mc.player.getDistance(nearest)) nearest = entity;
                }
                if (nearest == null) {
                    LOGGER.info("  interact {}: none within 6 blocks", type);
                    break;
                }
                LOGGER.info("  interact {} -> {}", nearest, mc.playerController.interactWithEntity(mc.player, nearest, net.minecraft.util.EnumHand.MAIN_HAND));
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
            case "state": {
                net.minecraft.entity.player.EntityPlayer serverPlayer = mc.getIntegratedServer() == null ? null
                        : mc.getIntegratedServer().getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
                double speed = serverPlayer == null ? -1.0D
                        : serverPlayer.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
                LOGGER.info("  sleeping {}, bed {}, at {} (y {}) in dimension {}, health {}, on ground {}, sneaking {}, speed {}, riding {} (server {})", mc.player.isPlayerSleeping(),
                        mc.player.bedLocation, mc.player.getPosition(), String.format("%.3f", mc.player.posY), mc.player.dimension, mc.player.getHealth(),
                        mc.player.onGround, mc.player.isSneaking(), String.format("%.4f", speed), mc.player.getRidingEntity(),
                        serverPlayer == null ? null : serverPlayer.getRidingEntity());
                break;
            }
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
            case "holdkey": {
                // A key binding held down for some ticks (holdkey <description> <ticks>), released on its own.
                for (net.minecraft.client.settings.KeyBinding binding : mc.gameSettings.keyBindings) {
                    if (binding.getKeyDescription().equals(words[1])) this.held = binding;
                }
                if (this.held == null) {
                    LOGGER.warn("  no key binding {}", words[1]);
                    break;
                }
                this.heldTicks = Integer.parseInt(words[2]);
                net.minecraft.client.settings.KeyBinding.setKeyBindState(this.held.getKeyCode(), true);
                LOGGER.info("  holding {} for {} ticks", words[1], this.heldTicks);
                break;
            }
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
            case "guiclick": {
                // A click at a spot of the open container screen, relative to its corner (guiclick <x> <y> [button]).
                if (!(mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer)) {
                    LOGGER.warn("  no container screen");
                    break;
                }
                net.minecraft.client.gui.inventory.GuiContainer screen = (net.minecraft.client.gui.inventory.GuiContainer) mc.currentScreen;
                click(screen.getGuiLeft() + Integer.parseInt(words[1]), screen.getGuiTop() + Integer.parseInt(words[2]), words.length > 3 ? Integer.parseInt(words[3]) : 0);
                break;
            }
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
                LOGGER.info("  {} ({} slots): {} cursor={} {}", container.getClass().getSimpleName(), container.inventorySlots.size(), stacks,
                        mc.player.inventory.getItemStack(), mc.player.inventory.getItemStack().getTagCompound());
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
            case "tooltip": {
                // The tooltip of the stack on the cursor (or else in hand), plain and advanced.
                net.minecraft.item.ItemStack stack = mc.player.inventory.getItemStack();
                if (stack.isEmpty()) stack = mc.player.getHeldItemMainhand();
                LOGGER.info("  tooltip: {}", stack.getTooltip(mc.player, net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL));
                LOGGER.info("  tooltip (advanced): {}", stack.getTooltip(mc.player, net.minecraft.client.util.ITooltipFlag.TooltipFlags.ADVANCED));
                break;
            }
            case "tooltips": {
                // Every tooltip of a mod's items, built as 1.12's screens build them (the creative inventory's hover).
                net.minecraft.client.gui.GuiScreen screen = new net.minecraft.client.gui.GuiScreen() {};
                screen.setWorldAndResolution(mc, 400, 300);
                int built = 0;
                List<String> failures = new java.util.ArrayList<>();
                for (net.minecraft.item.Item item : net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS) {
                    if (item.getRegistryName() == null || !item.getRegistryName().getNamespace().equals(words[1])) continue;
                    net.minecraft.util.NonNullList<net.minecraft.item.ItemStack> stacks = net.minecraft.util.NonNullList.create();
                    try {
                        item.getSubItems(net.minecraft.creativetab.CreativeTabs.SEARCH, stacks);
                    } catch (Throwable e) {
                        failures.add(item.getRegistryName() + " (sub-items): " + e);
                        continue;
                    }
                    for (net.minecraft.item.ItemStack stack : stacks) {
                        try {
                            for (String text : screen.getItemToolTip(stack)) text.length();
                            built++;
                        } catch (Throwable e) {
                            failures.add(item.getRegistryName() + ": " + e);
                        }
                    }
                }
                LOGGER.info("  tooltips of {}: {} built, {} failed", words[1], built, failures.size());
                for (String failure : failures.subList(0, Math.min(10, failures.size()))) LOGGER.info("    {}", failure);
                break;
            }
            case "items": {
                // Dropped items within a radius, on the server: what, where, and whether they burn.
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                net.minecraft.world.WorldServer world = server.getWorld(mc.player.dimension);
                double radius = Double.parseDouble(words[1]);
                StringBuilder found = new StringBuilder();
                for (net.minecraft.entity.item.EntityItem item : world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,
                        mc.player.getEntityBoundingBox().grow(radius))) {
                    found.append(item.getItem().getItem().getRegistryName()).append(" x").append(item.getItem().getCount())
                            .append(String.format(" at %.1f %.2f %.1f", item.posX, item.posY, item.posZ)).append(item.isBurning() ? " burning" : "")
                            .append(item.isInLava() ? " in lava" : "").append(item.isImmuneToFire() ? " fireproof" : "").append(item.isDead ? " dead" : "").append("; ");
                }
                LOGGER.info("  items within {}: {}", radius, found.length() == 0 ? "none" : found);
                break;
            }
            case "fly":
                // Creative flight on, so the player stays where a script puts them.
                mc.player.capabilities.isFlying = true;
                mc.player.sendPlayerAbilities();
                break;
            case "findopen": {
                // Moves the player to the nearest open air over lava at the Nether's lava sea (lava at 31, air 32 to 45).
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                net.minecraft.world.WorldServer world = server.getWorld(mc.player.dimension);
                BlockPos center = new BlockPos(mc.player);
                BlockPos found = null;
                for (int r = 0; r < 96 && found == null; r += 4) {
                    for (int dx = -r; dx <= r && found == null; dx += 4) {
                        for (int dz = -r; dz <= r && found == null; dz += 4) {
                            BlockPos column = new BlockPos(center.getX() + dx, 31, center.getZ() + dz);
                            if (world.getBlockState(column).getMaterial() != net.minecraft.block.material.Material.LAVA) continue;
                            boolean open = true;
                            for (int y = 32; y <= 45 && open; y++) open = world.isAirBlock(column.up(y - 31));
                            if (open) found = column.up(14);
                        }
                    }
                }
                if (found == null) {
                    LOGGER.info("  findopen: nothing open near {}", center);
                } else {
                    mc.player.sendChatMessage("/tp @s " + found.getX() + " " + found.getY() + " " + found.getZ());
                    LOGGER.info("  findopen: {}", found);
                }
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
            case "locatetp": {
                // To the nearest structure of a kind (locatetp <name> <dy> <dz> <yaw> <pitch>): offset from its start, at sea level + dy.
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                net.minecraft.world.WorldServer world = server.getWorld(mc.player.dimension);
                net.minecraft.entity.player.EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
                server.addScheduledTask(() -> {
                    BlockPos found = world.findNearestStructure(words[1], new BlockPos(player), false);
                    LOGGER.info("  locatetp {}: {}", words[1], found);
                    if (found == null) return;
                    player.connection.setPlayerLocation(found.getX() + 0.5D, world.getSeaLevel() + Double.parseDouble(words[2]), found.getZ() + 0.5D + Double.parseDouble(words[3]),
                            Float.parseFloat(words[4]), Float.parseFloat(words[5]));
                });
                break;
            }
            case "glinfo": {
                // The main framebuffer's stencil state (Create's ponder buttons draw their icons through the stencil).
                net.minecraft.client.shader.Framebuffer fb = mc.getFramebuffer();
                if (words.length > 2) {
                    // glinfo <class> <static field>: a mod's own framebuffer instead.
                    try {
                        java.lang.reflect.Field field = Class.forName(words[1], true, com.michaelsebero.portium.mods116.ModernHooks.loader()).getDeclaredField(words[2]);
                        field.setAccessible(true);
                        fb = (net.minecraft.client.shader.Framebuffer) field.get(null);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        LOGGER.info("  glinfo: no framebuffer {}.{}: {}", words[1], words[2], e.toString());
                        break;
                    }
                    if (fb == null) {
                        LOGGER.info("  glinfo: {}.{} is null", words[1], words[2]);
                        break;
                    }
                }
                fb.bindFramebuffer(false);
                LOGGER.info("  glinfo: stencil enabled {}, stencil bits {}, depth bits {}, fbo {}", fb.isStencilEnabled(),
                        org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_STENCIL_BITS), org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_DEPTH_BITS),
                        net.minecraft.client.renderer.OpenGlHelper.isFramebufferEnabled());
                mc.getFramebuffer().bindFramebuffer(true);
                break;
            }
            case "spawntrader": {
                // A wandering trader and its llamas near the player now, as the trader spawner places them (no timer or roll).
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                net.minecraft.world.WorldServer world = server.getWorld(mc.player.dimension);
                net.minecraft.entity.player.EntityPlayer player = world.getPlayerEntityByUUID(mc.player.getUniqueID());
                server.addScheduledTask(() -> LOGGER.info("  spawntrader: {}", com.michaelsebero.portium.world.spawn.WanderingTraderSpawner.spawnNear(world, player)));
                break;
            }
            case "offers": {
                // The open 1.14 trade screen's offers: price (after demand), second price, result, uses.
                if (!(mc.player.openContainer instanceof com.michaelsebero.portium.merchant.ContainerMerchant116)) {
                    LOGGER.info("  offers: no trade screen ({})", mc.player.openContainer.getClass().getSimpleName());
                    break;
                }
                com.michaelsebero.portium.merchant.ContainerMerchant116 container = (com.michaelsebero.portium.merchant.ContainerMerchant116) mc.player.openContainer;
                int index = 0;
                for (com.michaelsebero.portium.merchant.MerchantOffer offer : container.getOffers()) {
                    LOGGER.info("  offer {}: {} + {} -> {} ({}/{} uses, xp {})", index++, offer.getDiscountedBuyingStackFirst(), offer.getBuyingStackSecond(),
                            offer.getSellingStack(), offer.getUses(), offer.getMaxUses(), offer.getGivenExp());
                }
                LOGGER.info("  level {}, xp {}, bar {}", container.getMerchantLevel(), container.getXp(), container.showProgressBar());
                break;
            }
            case "spawnpatrol": {
                // A pillager patrol near the player now, as the patrol spawner makes one (no timer, day or roll).
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                net.minecraft.world.WorldServer world = server.getWorld(mc.player.dimension);
                net.minecraft.entity.player.EntityPlayer player = world.getPlayerEntityByUUID(mc.player.getUniqueID());
                server.addScheduledTask(() -> LOGGER.info("  spawnpatrol: {}", com.michaelsebero.portium.world.spawn.PatrolSpawner.spawnPatrolNear(world, player)));
                break;
            }
            case "attack": {
                // Left-click (attack) the nearest entity of a type within 6 blocks: attack <entity id>.
                net.minecraft.util.ResourceLocation type = new net.minecraft.util.ResourceLocation(words[1]);
                net.minecraft.entity.Entity nearest = null;
                for (net.minecraft.entity.Entity entity : mc.world.loadedEntityList) {
                    if (!type.equals(net.minecraft.entity.EntityList.getKey(entity)) || !entity.isEntityAlive() || mc.player.getDistance(entity) > 6.0F) continue;
                    if (entity instanceof net.minecraft.entity.EntityLivingBase && ((net.minecraft.entity.EntityLivingBase) entity).getHealth() <= 0.0F) continue;
                    if (nearest == null || mc.player.getDistance(entity) < mc.player.getDistance(nearest)) nearest = entity;
                }
                if (nearest == null) {
                    LOGGER.info("  attack {}: none within 6 blocks", type);
                    break;
                }
                mc.playerController.attackEntity(mc.player, nearest);
                LOGGER.info("  attacked {}", nearest);
                break;
            }
            case "view":
                // The camera: 0 first person, 1 behind, 2 in front.
                mc.gameSettings.thirdPersonView = Integer.parseInt(words[1]);
                break;
            case "entities": {
                // Server-side entities of a type within a radius of the player: entities <entity id> <radius>.
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                if (server == null) break;
                net.minecraft.util.ResourceLocation type = new net.minecraft.util.ResourceLocation(words[1]);
                double radius = Double.parseDouble(words[2]);
                int found = 0;
                for (net.minecraft.entity.Entity entity : server.getWorld(mc.player.dimension).loadedEntityList) {
                    if (!type.equals(net.minecraft.entity.EntityList.getKey(entity)) || mc.player.getDistance(entity) > radius) continue;
                    found++;
                    String extra = entity instanceof net.minecraft.entity.EntityLivingBase ? "health " + ((net.minecraft.entity.EntityLivingBase) entity).getHealth() : "";
                    if (entity instanceof com.michaelsebero.portium.entity.monster.EntityRavager) {
                        com.michaelsebero.portium.entity.monster.EntityRavager ravager = (com.michaelsebero.portium.entity.monster.EntityRavager) entity;
                        extra += ", attack " + ravager.getAttackTick() + ", stun " + ravager.getStunTick() + ", roar " + ravager.getRoarTick();
                    }
                    if (entity instanceof net.minecraft.entity.item.EntityItem) {
                        net.minecraft.item.ItemStack item = ((net.minecraft.entity.item.EntityItem) entity).getItem();
                        extra = item.getCount() + "x " + item.getItem().getRegistryName() + "@" + item.getMetadata();
                    }
                    if (entity instanceof net.minecraft.entity.projectile.EntityArrow) {
                        net.minecraft.nbt.NBTTagCompound tag = entity.writeToNBT(new net.minecraft.nbt.NBTTagCompound());
                        extra = "inGround " + tag.getByte("inGround") + ", pickup " + tag.getByte("pickup") + ", pierce " + tag.getByte("PierceLevel")
                                + ", crossbow " + tag.getBoolean("ShotFromCrossbow") + ", crit " + tag.getBoolean("crit");
                    }
                    LOGGER.info("  {} at ({}, {}, {}) motion ({}, {}, {}) {}", type, String.format("%.2f", entity.posX), String.format("%.2f", entity.posY),
                            String.format("%.2f", entity.posZ), String.format("%.2f", entity.motionX), String.format("%.2f", entity.motionY),
                            String.format("%.2f", entity.motionZ), extra);
                }
                LOGGER.info("  entities {} within {}: {}", type, radius, found);
                break;
            }
            case "held": {
                net.minecraft.item.ItemStack stack = mc.player.getHeldItemMainhand();
                LOGGER.info("  held: {} {}", stack, stack.getTagCompound());
                break;
            }
            case "jigsawsurvey": {
                net.minecraft.server.MinecraftServer server = mc.getIntegratedServer();
                int radius = Integer.parseInt(words[1]);
                if (server != null) server.addScheduledTask(() -> com.michaelsebero.portium.world.gen.jigsaw.JigsawStructures.survey(
                        server.getWorld(mc.player.dimension), radius));
                break;
            }
            case "audit116": {
                // Every block and item 1.16.5's language file names, checked against what 1.13 names resolve to here.
                java.io.File lang = new java.io.File("assetmover/assets/" + com.michaelsebero.portium.Tags.MOD_ID + "/lang_1_16/en_us.json");
                com.google.gson.JsonObject keys;
                try (java.io.Reader reader = new java.io.InputStreamReader(new java.io.FileInputStream(lang), java.nio.charset.StandardCharsets.UTF_8)) {
                    keys = new com.google.gson.JsonParser().parse(reader).getAsJsonObject();
                } catch (java.io.IOException e) {
                    LOGGER.warn("  audit116: can't read {}", lang);
                    break;
                }
                java.util.List<String> missingBlocks = new java.util.ArrayList<>();
                java.util.List<String> missingItems = new java.util.ArrayList<>();
                int blocks = 0;
                int items = 0;
                for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : keys.entrySet()) {
                    String key = entry.getKey();
                    String[] parts = key.split("\\.");
                    if (parts.length != 3 || !"minecraft".equals(parts[1])) continue;
                    String name = "minecraft:" + parts[2];
                    if ("block".equals(parts[0])) {
                        blocks++;
                        if (!com.michaelsebero.portium.compat.LegacyBlockStates.exists(name)) missingBlocks.add(parts[2]);
                    } else if ("item".equals(parts[0])) {
                        items++;
                        if (com.michaelsebero.portium.compat.LegacyItems.resolve(name) == null) missingItems.add(parts[2]);
                    }
                }
                LOGGER.info("  audit116 blocks {}/{} resolve; missing: {}", blocks - missingBlocks.size(), blocks, missingBlocks);
                LOGGER.info("  audit116 items {}/{} resolve; missing: {}", items - missingItems.size(), items, missingItems);
                java.util.Map<String, java.util.List<String>> missing = new java.util.LinkedHashMap<>();
                java.util.Map<String, Integer> totals = new java.util.LinkedHashMap<>();
                for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : keys.entrySet()) {
                    String[] parts = entry.getKey().split("\\.");
                    if (parts.length != 3 || !"minecraft".equals(parts[1])) continue;
                    net.minecraft.util.ResourceLocation id = new net.minecraft.util.ResourceLocation("minecraft", parts[2]);
                    boolean found;
                    switch (parts[0]) {
                        case "entity":
                            net.minecraft.util.ResourceLocation legacy = com.michaelsebero.portium.command.arguments.selector.EntityTypes.toLegacy(id);
                            found = net.minecraft.entity.EntityList.isRegistered(legacy) || net.minecraft.entity.EntityList.isRegistered(id)
                                    || "player".equals(parts[2]) || "lightning_bolt".equals(parts[2]) || "fishing_bobber".equals(parts[2]);
                            break;
                        case "biome":
                            found = net.minecraftforge.fml.common.registry.ForgeRegistries.BIOMES.containsKey(id)
                                    || net.minecraftforge.fml.common.registry.ForgeRegistries.BIOMES
                                    .containsKey(new net.minecraft.util.ResourceLocation(com.michaelsebero.portium.compat.LegacyBiomes.toLegacy(id.toString())));
                            break;
                        case "effect":
                            found = net.minecraftforge.fml.common.registry.ForgeRegistries.POTIONS.containsKey(id);
                            break;
                        case "enchantment":
                            found = net.minecraftforge.fml.common.registry.ForgeRegistries.ENCHANTMENTS.containsKey(id);
                            break;
                        default:
                            continue;
                    }
                    totals.merge(parts[0], 1, Integer::sum);
                    if (!found) missing.computeIfAbsent(parts[0], k -> new java.util.ArrayList<>()).add(parts[2]);
                }
                for (java.util.Map.Entry<String, Integer> total : totals.entrySet()) {
                    java.util.List<String> gone = missing.getOrDefault(total.getKey(), java.util.Collections.emptyList());
                    LOGGER.info("  audit116 {} {}/{} resolve; missing: {}", total.getKey(), total.getValue() - gone.size(), total.getValue(), gone);
                }
                break;
            }
            case "music":
                // The music the game would choose now (its type's sound).
                LOGGER.info("  music: {}", mc.getAmbientMusicType().getMusicLocation().getSoundName());
                break;
            case "modbegin":
                ModTest.begin(words[1]);
                break;
            case "modend":
                ModTest.end(words[1]);
                break;
            case "moditems":
                ModTest.items(mc, words[1]);
                break;
            case "modblocks":
                ModTest.blocks(mc, words[1]);
                break;
            case "modguis": {
                // Opens every block entity screen of the blocks `modblocks` placed: expands into script lines.
                List<String> expanded = ModTest.guis(mc);
                expanded.add("modguisummary " + words[1]);
                for (int i = expanded.size() - 1; i >= 0; i--) this.lines.addFirst(expanded.get(i));
                break;
            }
            case "modgui":
                ModTest.gui(mc, at(mc, words[1], words[2], words[3]));
                break;
            case "modguicheck":
                ModTest.guiCheck(mc, at(mc, words[1], words[2], words[3]));
                break;
            case "modguisummary":
                ModTest.guiSummary(words[1]);
                break;
            case "modentities":
                ModTest.entities(mc, words[1]);
                break;
            case "modentitycheck":
                ModTest.entityCheck(mc, words[1]);
                break;
            case "modparticles":
                ModTest.particles(mc, words[1]);
                break;
            case "modparticlecheck":
                ModTest.particleCheck(words[1]);
                break;
            case "mapdecor": {
                // The held map's markers as the client has them: key, type, icon, position, rotation, name.
                net.minecraft.item.ItemStack stack = mc.player.getHeldItemMainhand();
                if (!(stack.getItem() instanceof net.minecraft.item.ItemMap)) {
                    LOGGER.info("  mapdecor: not a map");
                    break;
                }
                net.minecraft.world.storage.MapData data = ((net.minecraft.item.ItemMap) stack.getItem()).getMapData(stack, mc.world);
                if (data == null) break;
                for (java.util.Map.Entry<String, net.minecraft.world.storage.MapDecoration> entry : data.mapDecorations.entrySet()) {
                    net.minecraft.world.storage.MapDecoration d = entry.getValue();
                    LOGGER.info("  mapdecor {} {} icon={} at {},{} rot={} name={}", entry.getKey(), d.getType(), d.getImage(), d.getX(), d.getY(), d.getRotation(),
                            ((com.michaelsebero.portium.item.IMapDecorationName) d).portium$getName());
                }
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
