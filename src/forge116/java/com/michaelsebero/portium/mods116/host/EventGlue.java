package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.mods116.ModernFailures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge 1.16's events that Forge 1.12 has under another class, or not at all, posted from the matching points of
 * 1.12 (see {@code ModernEvents} and {@code ModernClientEvents}, which call {@link #post} through the host). Each is
 * named by a short key; the arguments are 1.12's objects, which are 1.16's once translated.
 */
final class EventGlue {

    /** Events that failed to be made or posted: switched off, reported once. */
    private static final Set<String> OFF = ConcurrentHashMap.newKeySet();

    /**
     * @return what the caller needs of the event afterwards: whether it was cancelled (a Boolean) for cancellable
     * events, a changed value for those that carry one, else null
     */
    @Nullable
    static Object post(String name, Object[] a) {
        if (OFF.contains(name)) return null;
        try {
            return dispatch(name, a);
        } catch (Throwable e) {
            OFF.add(name);
            ModernFailures.add("Forge 1.16's event \"" + name + "\" couldn't be posted, so mods listening for it miss it", e);
            return null;
        }
    }

    private static RegistryKey<World> dimension(Object name) {
        return RegistryKey.func_240903_a_(net.minecraft.util.registry.Registry.field_239699_ae_, new ResourceLocation((String) name));
    }

    private static boolean fire(Event event) {
        return MinecraftForge.EVENT_BUS.post(event);
    }

    @Nullable
    private static Object dispatch(String name, Object[] a) {
        switch (name) {
            // ---- Players ----
            case "player_logged_in":
                fire(new PlayerEvent.PlayerLoggedInEvent((PlayerEntity) a[0]));
                return null;
            case "player_logged_out":
                fire(new PlayerEvent.PlayerLoggedOutEvent((PlayerEntity) a[0]));
                return null;
            case "player_respawn":
                fire(new PlayerEvent.PlayerRespawnEvent((PlayerEntity) a[0], (Boolean) a[1]));
                return null;
            case "player_changed_dimension":
                fire(new PlayerEvent.PlayerChangedDimensionEvent((PlayerEntity) a[0], dimension(a[1]), dimension(a[2])));
                return null;
            case "item_crafted":
                fire(new PlayerEvent.ItemCraftedEvent((PlayerEntity) a[0], (ItemStack) a[1], (IInventory) a[2]));
                return null;
            case "item_smelted":
                fire(new PlayerEvent.ItemSmeltedEvent((PlayerEntity) a[0], (ItemStack) a[1]));
                return null;
            case "item_pickup":
                fire(new PlayerEvent.ItemPickupEvent((PlayerEntity) a[0], (ItemEntity) a[1], (ItemStack) a[2]));
                return null;
            case "xp_pickup":
                return fire(new PlayerXpEvent.PickupXp((PlayerEntity) a[0], (ExperienceOrbEntity) a[1]));
            case "xp_change": {
                PlayerXpEvent.XpChange event = new PlayerXpEvent.XpChange((PlayerEntity) a[0], (Integer) a[1]);
                return fire(event) ? null : (Object) Integer.valueOf(event.getAmount());
            }
            case "xp_level": {
                PlayerXpEvent.LevelChange event = new PlayerXpEvent.LevelChange((PlayerEntity) a[0], (Integer) a[1]);
                return fire(event) ? null : (Object) Integer.valueOf(event.getLevels());
            }
            case "entity_leave_world":
                fire(new EntityLeaveWorldEvent((Entity) a[0], (World) a[1]));
                return null;
            case "sapling_grow": {
                net.minecraftforge.event.world.SaplingGrowTreeEvent event = new net.minecraftforge.event.world.SaplingGrowTreeEvent((World) a[0], (Random) a[1], (BlockPos) a[2]);
                fire(event);
                return event.getResult().name();
            }
            // ---- Data ----
            case "data_reload":
                ReloadGlue.reload(a[0], a[1]);
                return null;
            case "reload_listener":
                ReloadGlue.run(a[0], a[1], a[2]);
                return null;
            case "tags_updated": {
                net.minecraft.tags.ITagCollectionSupplier tags = net.minecraft.tags.TagCollectionManager.func_242178_a();
                fire(new net.minecraftforge.event.TagsUpdatedEvent.VanillaTagTypes(tags));
                fire(new net.minecraftforge.event.TagsUpdatedEvent.CustomTagTypes(tags));
                return null;
            }
            case "loot_type":
                LootGlue.registered((ResourceLocation) a[0], a[1]);
                return null;
            case "datapack_sync":
                fire(new net.minecraftforge.event.OnDatapackSyncEvent((net.minecraft.server.management.PlayerList) a[0],
                        (net.minecraft.entity.player.ServerPlayerEntity) a[1]));
                return null;
            default:
                return client(name, a);
        }
    }

    /** Client events, apart so a server never loads their classes. */
    @Nullable
    private static Object client(String name, Object[] a) {
        switch (name) {
            // ---- Input in the world ----
            case "key_input":
                fire(new InputEvent.KeyInputEvent((Integer) a[0], (Integer) a[1], (Integer) a[2], (Integer) a[3]));
                return null;
            case "mouse_input":
                fire(new InputEvent.MouseInputEvent((Integer) a[0], (Integer) a[1], (Integer) a[2]));
                return null;
            case "raw_mouse":
                return fire(new InputEvent.RawMouseEvent((Integer) a[0], (Integer) a[1], (Integer) a[2]));
            case "mouse_scroll":
                return fire(new InputEvent.MouseScrollEvent((Double) a[0], (Boolean) a[1], (Boolean) a[2], (Boolean) a[3], (Double) a[4], (Double) a[5]));
            case "click_input": {
                InputEvent.ClickInputEvent event = new InputEvent.ClickInputEvent((Integer) a[0], (net.minecraft.client.settings.KeyBinding) a[1],
                        (net.minecraft.util.Hand) a[2]);
                boolean cancelled = fire(event);
                return new boolean[] {cancelled, event.shouldSwingHand()};
            }
            // ---- The client's connection ----
            case "client_logged_in":
                fire(new ClientPlayerNetworkEvent.LoggedInEvent((net.minecraft.client.multiplayer.PlayerController) a[0],
                        (net.minecraft.client.entity.player.ClientPlayerEntity) a[1], (net.minecraft.network.NetworkManager) a[2]));
                return null;
            case "client_logged_out":
                fire(new ClientPlayerNetworkEvent.LoggedOutEvent((net.minecraft.client.multiplayer.PlayerController) a[0],
                        (net.minecraft.client.entity.player.ClientPlayerEntity) a[1], (net.minecraft.network.NetworkManager) a[2]));
                return null;
            case "client_respawn":
                fire(new ClientPlayerNetworkEvent.RespawnEvent((net.minecraft.client.multiplayer.PlayerController) a[0],
                        (net.minecraft.client.entity.player.ClientPlayerEntity) a[1], (net.minecraft.client.entity.player.ClientPlayerEntity) a[2],
                        (net.minecraft.network.NetworkManager) a[3]));
                return null;
            case "recipes_updated":
                fire(new net.minecraftforge.client.event.RecipesUpdatedEvent(RecipeManager116.game()));
                return null;
            // ---- Input on a screen ----
            case "screen_mouse_clicked_pre":
                return fire(new GuiScreenEvent.MouseClickedEvent.Pre((Screen) a[0], (Double) a[1], (Double) a[2], (Integer) a[3]));
            case "screen_mouse_clicked_post":
                return fire(new GuiScreenEvent.MouseClickedEvent.Post((Screen) a[0], (Double) a[1], (Double) a[2], (Integer) a[3]));
            case "screen_mouse_released_pre":
                return fire(new GuiScreenEvent.MouseReleasedEvent.Pre((Screen) a[0], (Double) a[1], (Double) a[2], (Integer) a[3]));
            case "screen_mouse_released_post":
                return fire(new GuiScreenEvent.MouseReleasedEvent.Post((Screen) a[0], (Double) a[1], (Double) a[2], (Integer) a[3]));
            case "screen_mouse_drag_pre":
                return fire(new GuiScreenEvent.MouseDragEvent.Pre((Screen) a[0], (Double) a[1], (Double) a[2], (Integer) a[3], (Double) a[4], (Double) a[5]));
            case "screen_mouse_drag_post":
                return fire(new GuiScreenEvent.MouseDragEvent.Post((Screen) a[0], (Double) a[1], (Double) a[2], (Integer) a[3], (Double) a[4], (Double) a[5]));
            case "screen_mouse_scroll_pre":
                return fire(new GuiScreenEvent.MouseScrollEvent.Pre((Screen) a[0], (Double) a[1], (Double) a[2], (Double) a[3]));
            case "screen_mouse_scroll_post":
                return fire(new GuiScreenEvent.MouseScrollEvent.Post((Screen) a[0], (Double) a[1], (Double) a[2], (Double) a[3]));
            case "screen_key_pressed_pre":
                return fire(new GuiScreenEvent.KeyboardKeyPressedEvent.Pre((Screen) a[0], (Integer) a[1], (Integer) a[2], (Integer) a[3]));
            case "screen_key_pressed_post":
                return fire(new GuiScreenEvent.KeyboardKeyPressedEvent.Post((Screen) a[0], (Integer) a[1], (Integer) a[2], (Integer) a[3]));
            case "screen_key_released_pre":
                return fire(new GuiScreenEvent.KeyboardKeyReleasedEvent.Pre((Screen) a[0], (Integer) a[1], (Integer) a[2], (Integer) a[3]));
            case "screen_key_released_post":
                return fire(new GuiScreenEvent.KeyboardKeyReleasedEvent.Post((Screen) a[0], (Integer) a[1], (Integer) a[2], (Integer) a[3]));
            case "screen_char_typed_pre":
                return fire(new GuiScreenEvent.KeyboardCharTypedEvent.Pre((Screen) a[0], (Character) a[1], (Integer) a[2]));
            case "screen_char_typed_post":
                return fire(new GuiScreenEvent.KeyboardCharTypedEvent.Post((Screen) a[0], (Character) a[1], (Integer) a[2]));
            default:
                throw new IllegalArgumentException("No such event: " + name);
        }
    }

    private EventGlue() {}
}
