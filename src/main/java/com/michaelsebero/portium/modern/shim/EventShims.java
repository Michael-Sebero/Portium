package com.michaelsebero.portium.modern.shim;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.DimensionType;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/** Forge 1.13's getters on events whose Forge 1.12 versions have public fields. */
public final class EventShims {

    public static EntityPlayer getPlayer(PlayerEvent event) {
        return event.player;
    }

    public static ItemStack getCrafting(PlayerEvent.ItemCraftedEvent event) {
        return event.crafting;
    }

    public static IInventory getInventory(PlayerEvent.ItemCraftedEvent event) {
        return event.craftMatrix;
    }

    public static ItemStack getSmelting(PlayerEvent.ItemSmeltedEvent event) {
        return event.smelting;
    }

    public static DimensionType getFrom(PlayerEvent.PlayerChangedDimensionEvent event) {
        return MiscShims.dimensionType(event.fromDim);
    }

    public static DimensionType getTo(PlayerEvent.PlayerChangedDimensionEvent event) {
        return MiscShims.dimensionType(event.toDim);
    }

    private EventShims() {}
}
