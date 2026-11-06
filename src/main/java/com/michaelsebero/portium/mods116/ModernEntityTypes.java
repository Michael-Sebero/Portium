package com.michaelsebero.portium.mods116;

import com.michaelsebero.portium.modern.mc.entity.EntityType;
import com.michaelsebero.portium.modern.mc.util.registry.IRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistryInternal;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.13's entity types ({@code minecraft:entitytypes}, so 1.13 mods' registry events for them fire with the rest).
 * Each type registered is registered with Forge 1.12's entities too: its class, factory and tracking settings, under
 * its name, so 1.12 spawns, saves, tracks and sends it.
 */
public final class ModernEntityTypes {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");
    public static final ResourceLocation NAME = new ResourceLocation("minecraft", "entitytypes");
    private static final Map<Class<?>, EntityType<?>> BY_CLASS = new ConcurrentHashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void create() {
        if (RegistryManager.ACTIVE.getRegistry(NAME) != null) return;
        RegistryBuilder builder = new RegistryBuilder().setName(NAME).setType(EntityType.class).setMaxID(Short.MAX_VALUE).disableSaving();
        builder.add(new IForgeRegistry.AddCallback() {
            @Override
            public void onAdd(IForgeRegistryInternal owner, RegistryManager stage, int id, IForgeRegistryEntry entry, IForgeRegistryEntry old) {
                if (stage == RegistryManager.ACTIVE) added((EntityType<?>) entry);
            }
        });
        builder.create();
    }

    /** For {@code EntityType.register}: into Portium's registry, which registers it with 1.12. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register(EntityType<?> type) {
        IForgeRegistry registry = RegistryManager.ACTIVE.getRegistry(NAME);
        if (registry != null && !registry.containsKey(type.getRegistryName())) {
            registry.register(type);
        } else if (registry == null) {
            added(type);
        }
    }

    private static synchronized void added(EntityType<?> type) {
        ResourceLocation name = type.getRegistryName();
        Class<? extends Entity> entityClass = type.getEntityClass();
        if (name == null || entityClass == null) return;
        IRegistry.ENTITY_TYPE.put(name, type);
        BY_CLASS.put(entityClass, type);
        if (ForgeRegistries.ENTITIES.containsKey(name)) return;
        int networkId = com.michaelsebero.portium.init.PortiumEntities.nextModernNetworkId();
        EntityEntry entry = EntityEntryBuilder.create().entity(entityClass).id(name, networkId).name(name.getNamespace() + "." + name.getPath())
                .tracker(type.getTrackingRange(), type.getUpdateFrequency(), type.shouldSendVelocityUpdates()).factory(type::create).build();
        try {
            ForgeRegistries.ENTITIES.register(entry);
        } catch (RuntimeException e) {
            LOGGER.error("Couldn't register entity type {} with 1.12", name, e);
        }
    }

    /** 1.13's {@code entity.getType()}: the type registered for its class, or a vanilla type by its 1.12 name. */
    @Nullable
    public static EntityType<?> typeOf(Entity entity) {
        EntityType<?> type = BY_CLASS.get(entity.getClass());
        if (type != null) return type;
        ResourceLocation id = net.minecraft.entity.EntityList.getKey(entity);
        return id == null ? null : EntityType.getById("minecraft:" + id.getPath());
    }

    private static final java.util.Set<String> SPAWNS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** A 1.16 mod's spawn in a biome (Forge 1.16's biome loading), as 1.12's biome spawn list entry. */
    @SuppressWarnings("unchecked")
    public static void addSpawn(String biomeName, Object type, Object classification, Object weighted, int min, int max) {
        int weight;
        try {
            weight = net.minecraftforge.fml.common.ObfuscationReflectionHelper.getPrivateValue(net.minecraft.util.WeightedRandom.Item.class,
                    (net.minecraft.util.WeightedRandom.Item) weighted, "field_76292_a");
        } catch (RuntimeException e) {
            return;
        }
        if (!(type instanceof EntityType) || !(classification instanceof net.minecraft.entity.EnumCreatureType) || weight <= 0) return;
        net.minecraft.world.biome.Biome biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(biomeName));
        Class<? extends Entity> entityClass = ((EntityType<?>) type).getEntityClass();
        if (biome == null || entityClass == null || !net.minecraft.entity.EntityLiving.class.isAssignableFrom(entityClass)) return;
        if (!SPAWNS.add(biomeName + "|" + entityClass.getName() + "|" + classification)) return;
        biome.getSpawnableList((net.minecraft.entity.EnumCreatureType) classification).add(new net.minecraft.world.biome.Biome.SpawnListEntry(
                (Class<? extends net.minecraft.entity.EntityLiving>) entityClass, weight, Math.max(1, min), Math.max(1, max)));
    }

    private ModernEntityTypes() {}
}
