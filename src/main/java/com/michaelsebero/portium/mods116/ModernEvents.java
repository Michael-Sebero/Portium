package com.michaelsebero.portium.mods116;

import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Passes Forge 1.12's loading events on to 1.13 mods' own buses. Game events need no passing on: 1.13 mods listen on
 * 1.12's main bus.
 */
public final class ModernEvents {

    private final IModernHost host;

    ModernEvents(IModernHost host) {
        this.host = host;
    }

    /** Each world tells Portium of entities leaving it, for Forge 1.16's capability invalidation listeners. */
    // Forge 1.12's tick events, as Forge 1.16's (another class in 1.16, which only this posts).

    @SubscribeEvent
    public void clientTick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        this.host.tick(IModernHost.TICK_CLIENT, event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START, null, 0.0F);
    }

    @SubscribeEvent
    public void serverTick(net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent event) {
        this.host.tick(IModernHost.TICK_SERVER, event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START, null, 0.0F);
    }

    @SubscribeEvent
    public void worldTick(net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent event) {
        this.host.tick(IModernHost.TICK_WORLD, event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START, event.world, 0.0F);
    }

    @SubscribeEvent
    public void playerTick(net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent event) {
        this.host.tick(IModernHost.TICK_PLAYER, event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START, event.player, 0.0F);
    }

    @SubscribeEvent
    public void renderTick(net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent event) {
        this.host.tick(IModernHost.TICK_RENDER, event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START, null, event.renderTickTime);
    }

    // Forge 1.12's player events of FML's package, as Forge 1.16's (nested in its PlayerEvent, which only this posts).

    @SubscribeEvent
    public void loggedIn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        this.host.postEvent("player_logged_in", event.player);
        if (event.player instanceof net.minecraft.entity.player.EntityPlayerMP && event.player.getServer() != null) {
            this.host.postEvent("datapack_sync", event.player.getServer().getPlayerList(), event.player);
        }
    }

    @SubscribeEvent
    public void loggedOut(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        this.host.postEvent("player_logged_out", event.player);
    }

    @SubscribeEvent
    public void respawned(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        this.host.postEvent("player_respawn", event.player, event.isEndConquered());
    }

    @SubscribeEvent
    public void changedDimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        this.host.postEvent("player_changed_dimension", event.player, dimensionName(event.fromDim), dimensionName(event.toDim));
    }

    private static String dimensionName(int id) {
        try {
            return net.minecraft.world.DimensionType.getById(id).getName();
        } catch (IllegalArgumentException e) {
            return "overworld";
        }
    }

    @SubscribeEvent
    public void crafted(net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent event) {
        this.host.postEvent("item_crafted", event.player, event.crafting, event.craftMatrix);
    }

    @SubscribeEvent
    public void smelted(net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemSmeltedEvent event) {
        this.host.postEvent("item_smelted", event.player, event.smelting);
    }

    @SubscribeEvent
    public void pickedUp(net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemPickupEvent event) {
        this.host.postEvent("item_pickup", event.player, event.getOriginalEntity(), event.getStack());
    }

    /** 1.12's experience pickup event, as 1.16's (which can cancel it too). */
    @SubscribeEvent
    public void pickedUpXp(net.minecraftforge.event.entity.player.PlayerPickupXpEvent event) {
        if (Boolean.TRUE.equals(this.host.postEvent("xp_pickup", event.getEntityPlayer(), event.getOrb()))) event.setCanceled(true);
    }

    @SubscribeEvent
    public void worldLoad(net.minecraftforge.event.world.WorldEvent.Load event) {
        com.michaelsebero.portium.mods116.shim.CapabilityShims116.watch(event.getWorld());
    }

    @SubscribeEvent
    public void newRegistry(RegistryEvent.NewRegistry event) {
        ModernTileEntityTypes.create();
        ModernEntityTypes.create();
        Mods116.createRegistries();
    }

    /** Every registry's event: the raw parameter type makes Forge 1.12 pass all of them, not just one type's. */
    @SubscribeEvent
    @SuppressWarnings("rawtypes")
    public void register(RegistryEvent.Register event) {
        this.host.register(event.getName(), event.getRegistry());
    }
}
