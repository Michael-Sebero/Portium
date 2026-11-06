package com.michaelsebero.portium.mods116;

import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client events Forge 1.16 posts on each mod's own bus, which Forge 1.12 posts on its main bus: handed on to the 1.16
 * mods' buses. Model baking is where mods swap in their own models (Create hides a turning shaft's static model so
 * only its renderer's turning one shows), texture stitching where they add sprites, colour handlers where they tint.
 */
@SideOnly(Side.CLIENT)
final class ModernClientEvents {

    private final IModernHost host;

    ModernClientEvents(IModernHost host) {
        this.host = host;
    }

    // ---- Input: Forge 1.12's raw mouse and keyboard events, as Forge 1.16's finer ones ----

    private static int modifiers() {
        return com.michaelsebero.portium.modern.client.ModernKeys.modifiers();
    }

    /** A key pressed, repeated or released in the world (1.16 posts this on screens too; those get the screen events below). */
    @SubscribeEvent
    public void keyInput(net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent event) {
        int key = org.lwjgl.input.Keyboard.getEventKey();
        if (key == 0) return;
        int action = org.lwjgl.input.Keyboard.getEventKeyState() ? (org.lwjgl.input.Keyboard.isRepeatEvent() ? 2 : 1) : 0;
        this.host.postEvent("key_input", com.michaelsebero.portium.modern.client.ModernKeys.toGlfw(key), key, action, modifiers());
    }

    @SubscribeEvent
    public void mouseInput(net.minecraftforge.fml.common.gameevent.InputEvent.MouseInputEvent event) {
        int button = org.lwjgl.input.Mouse.getEventButton();
        if (button >= 0) this.host.postEvent("mouse_input", button, org.lwjgl.input.Mouse.getEventButtonState() ? 1 : 0, modifiers());
    }

    /** 1.12's in-world mouse event: 1.16's raw button event and its scroll event, either of which can cancel it. */
    @SubscribeEvent
    public void rawMouse(net.minecraftforge.client.event.MouseEvent event) {
        if (event.getButton() >= 0 && Boolean.TRUE.equals(this.host.postEvent("raw_mouse", event.getButton(), event.isButtonstate() ? 1 : 0, modifiers()))) {
            event.setCanceled(true);
        }
        if (event.getDwheel() != 0 && Boolean.TRUE.equals(this.host.postEvent("mouse_scroll", event.getDwheel() / 120.0D, org.lwjgl.input.Mouse.isButtonDown(0),
                org.lwjgl.input.Mouse.isButtonDown(2), org.lwjgl.input.Mouse.isButtonDown(1), (double) event.getX(), (double) event.getY()))) {
            event.setCanceled(true);
        }
    }

    private double lastMouseX;
    private double lastMouseY;
    private int heldButton = -1;

    @SubscribeEvent
    public void screenMousePre(net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Pre event) {
        if (screenMouse(event.getGui(), "pre")) event.setCanceled(true);
    }

    @SubscribeEvent
    public void screenMousePost(net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Post event) {
        screenMouse(event.getGui(), "post");
    }

    /** One raw mouse event on a screen as 1.16's click, release, drag or scroll event; true if a mod cancelled it. */
    private boolean screenMouse(net.minecraft.client.gui.GuiScreen gui, String phase) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        double x = org.lwjgl.input.Mouse.getEventX() * (double) gui.width / mc.displayWidth;
        double y = gui.height - org.lwjgl.input.Mouse.getEventY() * (double) gui.height / mc.displayHeight - 1.0D;
        int button = org.lwjgl.input.Mouse.getEventButton();
        boolean down = org.lwjgl.input.Mouse.getEventButtonState();
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        boolean post = phase.equals("post");
        Object cancelled = null;
        if (wheel != 0) {
            cancelled = this.host.postEvent("screen_mouse_scroll_" + phase, gui, x, y, wheel / 120.0D);
        } else if (button >= 0 && down) {
            cancelled = this.host.postEvent("screen_mouse_clicked_" + phase, gui, x, y, button);
            if (post) this.heldButton = button;
        } else if (button >= 0) {
            cancelled = this.host.postEvent("screen_mouse_released_" + phase, gui, x, y, button);
            if (post && this.heldButton == button) this.heldButton = -1;
        } else if (this.heldButton >= 0 && org.lwjgl.input.Mouse.isButtonDown(this.heldButton)) {
            cancelled = this.host.postEvent("screen_mouse_drag_" + phase, gui, x, y, this.heldButton, x - this.lastMouseX, y - this.lastMouseY);
        }
        if (post) {
            this.lastMouseX = x;
            this.lastMouseY = y;
        }
        return Boolean.TRUE.equals(cancelled);
    }

    @SubscribeEvent
    public void screenKeyPre(net.minecraftforge.client.event.GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (screenKey(event.getGui(), "pre")) event.setCanceled(true);
    }

    @SubscribeEvent
    public void screenKeyPost(net.minecraftforge.client.event.GuiScreenEvent.KeyboardInputEvent.Post event) {
        screenKey(event.getGui(), "post");
    }

    /** One raw keyboard event on a screen as 1.16's key press (and typed character) or release; true if cancelled. */
    private boolean screenKey(net.minecraft.client.gui.GuiScreen gui, String phase) {
        int key = org.lwjgl.input.Keyboard.getEventKey();
        char typed = org.lwjgl.input.Keyboard.getEventCharacter();
        boolean down = org.lwjgl.input.Keyboard.getEventKeyState();
        int glfw = com.michaelsebero.portium.modern.client.ModernKeys.toGlfw(key);
        boolean cancelled = false;
        if (down || (key == 0 && typed >= ' ')) {
            if (key != 0) cancelled = Boolean.TRUE.equals(this.host.postEvent("screen_key_pressed_" + phase, gui, glfw, key, modifiers()));
            if (!cancelled && typed >= ' ' && typed != 127) {
                cancelled = Boolean.TRUE.equals(this.host.postEvent("screen_char_typed_" + phase, gui, typed, modifiers()));
            }
        } else if (key != 0) {
            cancelled = Boolean.TRUE.equals(this.host.postEvent("screen_key_released_" + phase, gui, glfw, key, modifiers()));
        }
        return cancelled;
    }

    // ---- The client's connection: 1.16's logged in, respawned and logged out events ----

    private net.minecraft.client.entity.EntityPlayerSP lastPlayer;

    @SubscribeEvent
    public void clientPlayerJoined(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (!event.getWorld().isRemote || !(event.getEntity() instanceof net.minecraft.client.entity.EntityPlayerSP) || event.getEntity() != mc.player) return;
        net.minecraft.client.entity.EntityPlayerSP player = mc.player;
        Object network = mc.getConnection() == null ? null : mc.getConnection().getNetworkManager();
        if (this.lastPlayer == null) {
            this.host.postEvent("client_logged_in", mc.playerController, player, network);
            // 1.16 sends a joining client the server's tags and recipes, and tells mods when each has arrived (JEI starts then).
            this.host.postEvent("tags_updated");
            this.host.postEvent("recipes_updated");
        } else if (this.lastPlayer != player) {
            this.host.postEvent("client_respawn", mc.playerController, this.lastPlayer, player, network);
        }
        this.lastPlayer = player;
    }

    @SubscribeEvent
    public void clientDisconnected(net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        Object network = event.getManager();
        mc.addScheduledTask(() -> {
            if (this.lastPlayer == null) return;
            this.host.postEvent("client_logged_out", mc.playerController, this.lastPlayer, network);
            this.lastPlayer = null;
        });
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void modelRegistry(ModelRegistryEvent event) {
        this.host.postToMods(event);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void modelBake(ModelBakeEvent event) {
        com.michaelsebero.portium.client.ModelShims116.bake(event.getModelRegistry());
        this.host.postToMods(event);
        if (Boolean.getBoolean("portium.dev.models")) {
            for (Object key : event.getModelRegistry().getKeys()) {
                String name = String.valueOf(key);
                if (name.startsWith("create:cogwheel#") || name.startsWith("create:shaft#")) {
                    org.apache.logging.log4j.LogManager.getLogger("Portium/1.16.5").info("Model {} is {}", name, event.getModelRegistry().getObject((net.minecraft.client.renderer.block.model.ModelResourceLocation) key).getClass().getName());
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void textureStitchPre(TextureStitchEvent.Pre event) {
        if (event.getMap() == net.minecraft.client.Minecraft.getMinecraft().getTextureMapBlocks()) com.michaelsebero.portium.client.ModelShims116.stitch(event.getMap());
        postAsEachAtlas(event);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void textureStitchPost(TextureStitchEvent.Post event) {
        postAsEachAtlas(event);
    }

    /** The block atlas's stitch event, to mods as the block atlas's and then as each other 1.16 atlas's. */
    private void postAsEachAtlas(TextureStitchEvent event) {
        this.host.postToMods(event);
        if (event.getMap() != net.minecraft.client.Minecraft.getMinecraft().getTextureMapBlocks()) return;
        try {
            for (String atlas : com.michaelsebero.portium.mods116.shim.ClientShims116.ATLASES_116) {
                com.michaelsebero.portium.mods116.shim.ClientShims116.announcedAtlas = new net.minecraft.util.ResourceLocation(atlas);
                this.host.postToMods(event);
            }
        } finally {
            com.michaelsebero.portium.mods116.shim.ClientShims116.announcedAtlas = null;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void blockColors(ColorHandlerEvent.Block event) {
        this.host.postToMods(event);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void itemColors(ColorHandlerEvent.Item event) {
        this.host.postToMods(event);
    }

    // 1.16 mods draw into the shared render buffers during these events; what they left is drawn once they're done.

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterWorld(net.minecraftforge.client.event.RenderWorldLastEvent event) {
        finishShared();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterOverlay(net.minecraftforge.client.event.RenderGameOverlayEvent.Post event) {
        finishShared();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterHand(net.minecraftforge.client.event.RenderSpecificHandEvent event) {
        finishShared();
    }

    private static java.lang.invoke.MethodHandle finishShared;

    private static void finishShared() {
        try {
            if (finishShared == null) {
                Class<?> glue = Class.forName("com.michaelsebero.portium.mods116.host.RenderGlue", true, ModernHooks.loader());
                finishShared = java.lang.invoke.MethodHandles.publicLookup().findStatic(glue, "finishShared", java.lang.invoke.MethodType.methodType(void.class));
            }
            finishShared.invoke();
        } catch (Throwable e) {
            org.apache.logging.log4j.LogManager.getLogger("Portium/1.16.5").warn("Couldn't draw 1.16 mods' buffers", e);
        }
    }
}
