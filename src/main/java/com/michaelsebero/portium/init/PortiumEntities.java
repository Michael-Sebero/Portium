package com.michaelsebero.portium.init;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.EntityTrident;
import com.michaelsebero.portium.entity.fish.EntityCod;
import com.michaelsebero.portium.entity.fish.EntityPufferfish;
import com.michaelsebero.portium.entity.fish.EntitySalmon;
import com.michaelsebero.portium.entity.fish.EntityTropicalFish;
import com.michaelsebero.portium.entity.monster.EntityDrowned;
import com.michaelsebero.portium.entity.monster.EntityPhantom;
import com.michaelsebero.portium.entity.passive.EntityDolphin;
import com.michaelsebero.portium.entity.passive.EntityTurtle;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PortiumEntities {

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        IForgeRegistry<EntityEntry> registry = event.getRegistry();
        PortiumConfig.Mobs enabled = PortiumConfig.mobs;
        // Ids are fixed per entity, so switching one off doesn't renumber the others.
        register(registry, PortiumConfig.items.trident, builder(EntityTrident.class, "trident", 0).tracker(64, 20, true).build());
        // Spawn egg colours are 1.13's.
        register(registry, enabled.cod, mob(EntityCod.class, "cod", 1, 0xC1A76A, 0xE5C48B));
        register(registry, enabled.salmon, mob(EntitySalmon.class, "salmon", 2, 0xA00F10, 0x0E8474));
        register(registry, enabled.pufferfish, mob(EntityPufferfish.class, "pufferfish", 3, 0xF6B201, 0x37C3F2));
        register(registry, enabled.tropicalFish, mob(EntityTropicalFish.class, "tropical_fish", 4, 0xEF6915, 0xFFF9EF));
        register(registry, enabled.turtles, mob(EntityTurtle.class, "turtle", 5, 0xE7E7E7, 0x00AFAF));
        register(registry, enabled.dolphins, mob(EntityDolphin.class, "dolphin", 6, 0x223B4D, 0xF9F9F9));
        register(registry, enabled.drowned, mob(EntityDrowned.class, "drowned", 7, 0x8FF1D7, 0x799C65));
        register(registry, enabled.phantoms, mob(EntityPhantom.class, "phantom", 8, 0x43518A, 0x88FF00));
    }

    private static void register(IForgeRegistry<EntityEntry> registry, boolean enabled, EntityEntry entry) {
        if (enabled) registry.register(entry);
    }

    private static <E extends Entity> EntityEntryBuilder<E> builder(Class<E> type, String name, int id) {
        return EntityEntryBuilder.<E>create()
                .entity(type)
                .id(new ResourceLocation(Tags.MOD_ID, name), id)
                .name(Tags.MOD_ID + "." + name);
    }

    private static EntityEntry mob(Class<? extends Entity> type, String name, int id, int eggPrimary, int eggSecondary) {
        return builder(type, name, id).tracker(80, 3, true).egg(eggPrimary, eggSecondary).build();
    }

    /**
     * Network ids for newer mods' entity types, which register with 1.12 under Portium (the mod loading them): one
     * count for all of them, above Portium's own fixed ids, since 1.12 tells a mod's entities apart by id alone.
     */
    private static int nextModernId = 256;

    public static synchronized int nextModernNetworkId() {
        return nextModernId++;
    }

    /** Whether an entity of Portium's is registered, so it can be spawned. */
    public static boolean isRegistered(Class<? extends Entity> type) {
        return net.minecraftforge.fml.common.registry.EntityRegistry.getEntry(type) != null;
    }

    private PortiumEntities() {}
}
