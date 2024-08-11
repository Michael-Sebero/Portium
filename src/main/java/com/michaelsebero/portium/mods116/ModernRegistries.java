package com.michaelsebero.portium.mods116;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryManager;

import javax.annotation.Nullable;

/**
 * Forge 1.12's registries, for 1.13 mods. Both Forges name their registries alike ({@code minecraft:blocks} and so
 * on), and {@code IForgeRegistry} is shared between them, so 1.13 mods register straight into 1.12's registries.
 */
public final class ModernRegistries {

    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Portium/1.16.5");

    /** For the glue: 1.12's registry by name, or null if 1.12 has none such (1.13's tile entity types, say). */
    @Nullable
    public static IForgeRegistry<?> get(String name) {
        return RegistryManager.ACTIVE.getRegistry(new ResourceLocation(name));
    }

    /** For the glue's {@code ForgeRegistries}: the registry of what a class is (as loaded), or null if there's none. */
    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static IForgeRegistry<?> byType(Class<?> type) {
        return RegistryManager.ACTIVE.getRegistry((Class) type);
    }

    /**
     * Forge 1.16's registries that Forge 1.12 lacks, by 1.16 name and the 1.16 class of what they hold. Tile entity
     * and entity types have registries of their own (see {@link ModernTileEntityTypes}, {@link ModernEntityTypes}).
     */
    private static final String[][] MISSING = {
            {"minecraft:fluid", "net/minecraft/fluid/Fluid"},
            {"minecraft:particle_type", "net/minecraft/particles/ParticleType"},
            {"minecraft:menu", "net/minecraft/inventory/container/ContainerType"},
            {"minecraft:recipe_serializer", "net/minecraft/item/crafting/IRecipeSerializer"},
            {"minecraft:motive", "net/minecraft/entity/item/PaintingType"},
            {"minecraft:stat_type", "net/minecraft/stats/StatType"},
            {"minecraft:villager_profession", "net/minecraft/entity/merchant/villager/VillagerProfession"},
            {"minecraft:point_of_interest_type", "net/minecraft/village/PointOfInterestType"},
            {"minecraft:memory_module_type", "net/minecraft/entity/ai/brain/memory/MemoryModuleType"},
            {"minecraft:sensor_type", "net/minecraft/entity/ai/brain/sensor/SensorType"},
            {"minecraft:schedule", "net/minecraft/entity/ai/brain/schedule/Schedule"},
            {"minecraft:activity", "net/minecraft/entity/ai/brain/schedule/Activity"},
            {"minecraft:worldgen/carver", "net/minecraft/world/gen/carver/WorldCarver"},
            {"minecraft:worldgen/surface_builder", "net/minecraft/world/gen/surfacebuilders/SurfaceBuilder"},
            {"minecraft:worldgen/feature", "net/minecraft/world/gen/feature/Feature"},
            {"minecraft:worldgen/decorator", "net/minecraft/world/gen/placement/Placement"},
            {"minecraft:worldgen/structure_feature", "net/minecraft/world/gen/feature/structure/Structure"},
            {"minecraft:worldgen/block_state_provider_type", "net/minecraft/world/gen/blockstateprovider/BlockStateProviderType"},
            {"minecraft:worldgen/block_placer_type", "net/minecraft/world/gen/blockplacer/BlockPlacerType"},
            {"minecraft:worldgen/foliage_placer_type", "net/minecraft/world/gen/foliageplacer/FoliagePlacerType"},
            {"minecraft:worldgen/tree_decorator_type", "net/minecraft/world/gen/treedecorator/TreeDecoratorType"},
            {"forge:loot_modifier_serializers", "net/minecraftforge/common/loot/GlobalLootModifierSerializer"},
            {"forge:world_types", "net/minecraftforge/common/world/ForgeWorldType"},
    };

    /** Makes Forge 1.16's registries that 1.12 lacks, before the mods construct (they ask for them then). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void createMissing(ModernClassLoader loader) {
        ModernTileEntityTypes.create();
        ModernEntityTypes.create();
        for (String[] registry : MISSING) {
            ResourceLocation name = new ResourceLocation(registry[0]);
            if (RegistryManager.ACTIVE.getRegistry(name) != null) continue;
            Class<?> type;
            try {
                type = loader.modernClass(registry[1]);
            } catch (ClassNotFoundException | LinkageError e) {
                LOGGER.warn("Couldn't make 1.16's registry {}: {}", name, e.toString());
                continue;
            }
            if (!IForgeRegistryEntry.class.isAssignableFrom(type)) {
                LOGGER.warn("Couldn't make 1.16's registry {}: {} isn't a registry entry", name, type.getName());
                continue;
            }
            new RegistryBuilder().setName(name).setType((Class) type).setMaxID(Integer.MAX_VALUE - 1).disableSaving().create();
        }
    }

    private ModernRegistries() {}
}
