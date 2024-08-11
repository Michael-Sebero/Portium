package com.michaelsebero.portium.modern.mc.util.registry;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 1.13's {@code net.minecraft.util.registry.IRegistry}: Minecraft's own registries. Those 1.12 keeps in Forge's
 * registries (blocks, items, sounds and so on) are views of Forge's; blocks and items speak 1.13's flattened names
 * ({@code modern.flat}). The rest hold what 1.13 code registers in them, for 1.13 code to find.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface IRegistry<T> extends Iterable<T> {

    @Srg("field_212617_f")
    IRegistry REGISTRY = new SimpleRegistry(null);
    @Srg("field_212618_g")
    IRegistry BLOCK = named("block", ForgeRegistryView.blocks());
    @Srg("field_212619_h")
    IRegistry FLUID = named("fluid", new SimpleRegistry(new ResourceLocation("empty")));
    @Srg("field_212620_i")
    IRegistry MOTIVE = named("motive", new SimpleRegistry(new ResourceLocation("kebab")));
    @Srg("field_212621_j")
    IRegistry POTION = named("potion", new ForgeRegistryView(() -> ForgeRegistries.POTION_TYPES, new ResourceLocation("empty")));
    @Srg("field_212622_k")
    IRegistry DIMENSION_TYPE = named("dimension_type", new SimpleRegistry(null));
    @Srg("field_212623_l")
    IRegistry CUSTOM_STAT = named("custom_stat", new SimpleRegistry(null));
    @Srg("field_212624_m")
    IRegistry BIOME = named("biome", new ForgeRegistryView(() -> ForgeRegistries.BIOMES, null));
    @Srg("field_212625_n")
    IRegistry BIOME_SOURCE_TYPE = named("biome_source_type", new SimpleRegistry(null));
    @Srg("field_212626_o")
    IRegistry BLOCK_ENTITY_TYPE = named("block_entity_type", ForgeRegistryView.tileEntityTypes());
    @Srg("field_212627_p")
    IRegistry CHUNK_GENERATOR_TYPE = named("chunk_generator_type", new SimpleRegistry(null));
    @Srg("field_212628_q")
    IRegistry ENCHANTMENT = named("enchantment", new ForgeRegistryView(() -> ForgeRegistries.ENCHANTMENTS, null));
    @Srg("field_212629_r")
    IRegistry ENTITY_TYPE = named("entity_type", new SimpleRegistry(null));
    @Srg("field_212630_s")
    IRegistry ITEM = named("item", ForgeRegistryView.items());
    @Srg("field_212631_t")
    IRegistry MOB_EFFECT = named("mob_effect", new ForgeRegistryView(() -> ForgeRegistries.POTIONS, null));
    @Srg("field_212632_u")
    IRegistry PARTICLE_TYPE = named("particle_type", new SimpleRegistry(null));
    @Srg("field_212633_v")
    IRegistry SOUND_EVENT = named("sound_event", new ForgeRegistryView(() -> ForgeRegistries.SOUND_EVENTS, null));
    @Srg("field_212634_w")
    IRegistry STATS = named("stats", new SimpleRegistry(null));

    // ---- 1.16's registries; those Forge 1.16 has are views of the Forge registries Portium makes for them ----

    @Srg("field_218360_A")
    IRegistry CHUNK_STATUS = named("chunk_status", new SimpleRegistry(new ResourceLocation("empty")));
    @Srg("field_218363_D")
    IRegistry RULE_TEST = named("rule_test", new SimpleRegistry(null));
    @Srg("field_239691_aJ_")
    IRegistry POS_RULE_TEST = named("pos_rule_test", new SimpleRegistry(null));
    @Srg("field_218366_G")
    IRegistry MENU = named("menu", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:menu")), null));
    @Srg("field_218367_H")
    IRegistry RECIPE_TYPE = named("recipe_type", new SimpleRegistry(null));
    @Srg("field_218368_I")
    IRegistry RECIPE_SERIALIZER = named("recipe_serializer", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:recipe_serializer")), null));
    @Srg("field_239692_aP_")
    IRegistry ATTRIBUTE = named("attribute", new SimpleRegistry(null));
    @Srg("field_218369_K")
    IRegistry VILLAGER_TYPE = named("villager_type", new SimpleRegistry(new ResourceLocation("plains")));
    @Srg("field_218370_L")
    IRegistry VILLAGER_PROFESSION = named("villager_profession", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:villager_profession")), new ResourceLocation("none")));
    @Srg("field_218371_M")
    IRegistry POINT_OF_INTEREST_TYPE = named("point_of_interest_type", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:point_of_interest_type")), new ResourceLocation("unemployed")));
    @Srg("field_218372_N")
    IRegistry MEMORY_MODULE_TYPE = named("memory_module_type", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:memory_module_type")), new ResourceLocation("dummy")));
    @Srg("field_218373_O")
    IRegistry SENSOR_TYPE = named("sensor_type", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:sensor_type")), new ResourceLocation("dummy")));
    @Srg("field_218374_P")
    IRegistry SCHEDULE = named("schedule", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:schedule")), null));
    @Srg("field_218375_Q")
    IRegistry ACTIVITY = named("activity", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:activity")), null));
    @Srg("field_239693_aY_")
    IRegistry LOOT_POOL_ENTRY_TYPE = named("loot_pool_entry_type", new SimpleRegistry(null));
    @Srg("field_239694_aZ_")
    IRegistry LOOT_FUNCTION_TYPE = named("loot_function_type", new SimpleRegistry(null));
    @Srg("field_239704_ba_")
    IRegistry LOOT_CONDITION_TYPE = named("loot_condition_type", new SimpleRegistry(null));
    @Srg("field_218378_p")
    IRegistry SURFACE_BUILDER = named("worldgen/surface_builder", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:worldgen/surface_builder")), null));
    @Srg("field_218377_o")
    IRegistry CARVER = named("worldgen/carver", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:worldgen/carver")), null));
    @Srg("field_218379_q")
    IRegistry FEATURE = named("worldgen/feature", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:worldgen/feature")), null));
    @Srg("field_218361_B")
    IRegistry STRUCTURE_FEATURE = named("worldgen/structure_feature", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:worldgen/structure_feature")), null));
    @Srg("field_218362_C")
    IRegistry STRUCTURE_PIECE = named("worldgen/structure_piece", new SimpleRegistry(null));
    @Srg("field_218380_r")
    IRegistry DECORATOR = named("worldgen/decorator", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:worldgen/decorator")), null));
    @Srg("field_229387_t_")
    IRegistry BLOCK_STATE_PROVIDER_TYPE = named("worldgen/block_state_provider_type", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:worldgen/block_state_provider_type")), null));
    @Srg("field_229388_u_")
    IRegistry BLOCK_PLACER_TYPE = named("worldgen/block_placer_type", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:worldgen/block_placer_type")), null));
    @Srg("field_229389_v_")
    IRegistry FOLIAGE_PLACER_TYPE = named("worldgen/foliage_placer_type", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:worldgen/foliage_placer_type")), null));
    @Srg("field_239701_aw_")
    IRegistry TRUNK_REPLACER = named("worldgen/trunk_placer_type", new SimpleRegistry(null));
    @Srg("field_229390_w_")
    IRegistry TREE_DECORATOR_TYPE = named("worldgen/tree_decorator_type", new ForgeRegistryView(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry(new ResourceLocation("minecraft:worldgen/tree_decorator_type")), null));
    @Srg("field_239702_ay_")
    IRegistry FEATURE_SIZE_TYPE = named("worldgen/feature_size_type", new SimpleRegistry(null));
    @Srg("field_218364_E")
    IRegistry STRUCTURE_PROCESSOR = named("worldgen/structure_processor", new SimpleRegistry(null));
    @Srg("field_218365_F")
    IRegistry STRUCTURE_POOL_ELEMENT = named("worldgen/structure_pool_element", new SimpleRegistry(null));

    @Srg("func_212610_a")
    static <T> IRegistry<T> named(String name, IRegistry<T> registry) {
        REGISTRY.put(new ResourceLocation(name), registry);
        return registry;
    }

    @Nullable
    @Srg("func_177774_c")
    ResourceLocation getKey(T value);

    @Srg("func_82594_a")
    T get(@Nullable ResourceLocation name);

    @Srg("func_212609_b")
    ResourceLocation getDefaultKey();

    @Srg("func_148757_b")
    int getId(@Nullable T value);

    @Nullable
    @Srg("func_148754_a")
    T get(int id);

    @Override
    Iterator<T> iterator();

    /** The value by that name, or null (where {@link #get(ResourceLocation)} may give the default). */
    @Nullable
    @Srg("func_212608_b")
    T getNullable(@Nullable ResourceLocation name);

    @Srg("func_177775_a")
    void register(int id, ResourceLocation name, T value);

    @Srg("func_82595_a")
    void put(ResourceLocation name, T value);

    @Srg("func_148742_b")
    Set<ResourceLocation> keySet();

    @Srg("func_195866_d")
    boolean isEmpty();

    @Nullable
    @Srg("func_186801_a")
    T getRandom(Random random);

    @Srg("func_201756_e")
    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Srg("func_212607_c")
    boolean containsKey(ResourceLocation name);

    // ---- 1.16's additions ----

    /** 1.16's: the value by that name, if any. */
    @Srg("func_241873_b")
    default java.util.Optional<T> getOptional(@Nullable ResourceLocation name) {
        return java.util.Optional.ofNullable(getNullable(name));
    }

    /**
     * 1.16's way to register: puts the value under the name and returns it. One of Minecraft 1.16.5's own objects
     * that 1.12 hasn't (a newer sound, say) can come here from its class's static initialiser, outside 1.12's
     * registry events: Forge 1.12's registries may refuse it then, and it stays unregistered.
     */
    @Srg("func_218322_a")
    static <V, U extends V> U register(IRegistry<V> registry, ResourceLocation name, U value) {
        try {
            registry.put(name, value);
        } catch (RuntimeException e) {
            org.apache.logging.log4j.LogManager.getLogger("Portium").debug("Couldn't register {} now", name, e);
        }
        return value;
    }

    @Srg("func_218325_a")
    static <V, U extends V> U register(IRegistry<V> registry, String name, U value) {
        return register(registry, new ResourceLocation(name), value);
    }

    @Srg("func_218343_a")
    static <V, U extends V> U register(IRegistry<V> registry, int id, String name, U value) {
        try {
            registry.register(id, new ResourceLocation(name), value);
        } catch (RuntimeException e) {
            org.apache.logging.log4j.LogManager.getLogger("Portium").debug("Couldn't register {} now", name, e);
        }
        return value;
    }
}
