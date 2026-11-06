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
        // 1.14's mobs, egg colours as 1.16.5's.
        register(registry, enabled.foxes, mob(com.michaelsebero.portium.entity.passive.EntityFox.class, "fox", 9, 14005919, 13396256));
        register(registry, enabled.pandas, mob(com.michaelsebero.portium.entity.passive.EntityPanda.class, "panda", 10, 15198183, 1776418));
        register(registry, enabled.cats, mob(com.michaelsebero.portium.entity.passive.EntityCat.class, "cat", 11, 15714446, 9794134));
        register(registry, enabled.pillagers, mob(com.michaelsebero.portium.entity.monster.EntityPillager.class, "pillager", 12, 5451574, 9804699));
        register(registry, enabled.ravagers, builder(com.michaelsebero.portium.entity.monster.EntityRavager.class, "ravager", 13)
                .tracker(160, 3, true).egg(7697520, 5984329).build());
        register(registry, enabled.wanderingTraders, builder(com.michaelsebero.portium.entity.merchant.EntityWanderingTrader.class, "wandering_trader", 14)
                .tracker(160, 3, true).egg(4547222, 15377456).build());
        register(registry, enabled.wanderingTraders, builder(com.michaelsebero.portium.entity.merchant.EntityTraderLlama.class, "trader_llama", 15)
                .tracker(160, 3, true).egg(15377456, 4547222).build());
        // 1.16's Nether mobs.
        register(registry, enabled.piglins, mob(com.michaelsebero.portium.entity.monster.piglin.EntityPiglin.class, "piglin", 16, 10051392, 16380836));
        register(registry, enabled.piglins, mob(com.michaelsebero.portium.entity.monster.piglin.EntityPiglinBrute.class, "piglin_brute", 19, 5843472, 16380836));
        register(registry, enabled.hoglins, mob(com.michaelsebero.portium.entity.monster.EntityHoglin.class, "hoglin", 17, 13004373, 6251620));
        register(registry, enabled.bees && PortiumConfig.blocks.beehives, mob(com.michaelsebero.portium.entity.passive.bee.EntityBee.class, "bee", 21, 15582019, 4400155));
        register(registry, enabled.zombifiedPiglins, mob(com.michaelsebero.portium.entity.monster.EntityZombifiedPiglin.class, "zombified_piglin", 22, 15373203, 5009705));
        register(registry, enabled.striders, mob(com.michaelsebero.portium.entity.passive.EntityStrider.class, "strider", 20, 10236982, 5065037));
        register(registry, enabled.hoglins, mob(com.michaelsebero.portium.entity.monster.EntityZoglin.class, "zoglin", 18, 13004373, 15132390));
        // 1.14 villagers: no egg of their own, as 1.12's villager egg makes one in 1.16.5 worlds (VillagerEvents116).
        register(registry, enabled.villagers, builder(com.michaelsebero.portium.entity.villager.EntityVillager116.class, "villager", 23)
                .tracker(80, 3, true).build());
    }

    private static void register(IForgeRegistry<EntityEntry> registry, boolean enabled, EntityEntry entry) {
        if (enabled) registry.register(entry);
    }

    private static <E extends Entity> EntityEntryBuilder<E> builder(Class<E> type, String name, int id) {
        return EntityEntryBuilder.<E>create()
                .entity(type)
                .id(PortiumNames.of(net.minecraftforge.fml.common.registry.ForgeRegistries.ENTITIES, name), id)
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
