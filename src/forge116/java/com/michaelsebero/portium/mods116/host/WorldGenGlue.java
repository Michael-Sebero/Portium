package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.mods116.ModernFailures;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.common.world.MobSpawnInfoBuilder;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Forge 1.16's biome loading, for 1.12's biomes, and the features 1.16 mods add there placed in 1.12's chunks. Forge
 * posts {@code BiomeLoadingEvent} as it loads each biome, with builders of its generation settings and spawns that
 * mods add to; Portium posts it for each of 1.12's biomes with empty builders (1.12 generates the vanilla features
 * itself), keeps what mods added, and runs those features stage by stage as 1.16 decorates a chunk, each from a seed
 * of the world's, the chunk's and its place. 1.16's ambience isn't given (null), and the builders are made without
 * 1.16's defaults, whose classes would build 1.16's vanilla worldgen.
 */
final class WorldGenGlue {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");
    /** Per biome (1.12's name), per decoration stage, the features mods added. */
    @Nullable
    private static Map<String, List<List<Supplier<ConfiguredFeature<?, ?>>>>> features;
    private static final Set<Object> FAILED = Collections.synchronizedSet(new HashSet<>());
    private static final boolean DEBUG = Boolean.getBoolean("portium.dev.worldgen");
    private static final int[] RUN = new int[2];

    static void reset() {
        features = null;
    }

    static void generate(Object world, String biome, int chunkX, int chunkZ, long seed) {
        if (features == null) features = collect();
        List<List<Supplier<ConfiguredFeature<?, ?>>>> stages = features.get(biome);
        if (stages == null) return;
        Random random = new Random(seed);
        long decoration = (chunkX << 4) * (random.nextLong() | 1L) + (chunkZ << 4) * (random.nextLong() | 1L) ^ seed;
        BlockPos pos = new BlockPos((chunkX << 4) + 8, 0, (chunkZ << 4) + 8);
        for (int stage = 0; stage < stages.size(); stage++) {
            List<Supplier<ConfiguredFeature<?, ?>>> list = stages.get(stage);
            for (int i = 0; i < list.size(); i++) {
                ConfiguredFeature<?, ?> feature;
                try {
                    feature = list.get(i).get();
                } catch (RuntimeException | LinkageError e) {
                    continue;
                }
                if (feature == null || FAILED.contains(feature)) continue;
                random.setSeed(decoration + i + 10000L * stage);
                try {
                    boolean placed = feature.func_242765_a((ISeedReader) world, null, random, pos);
                    if (DEBUG) {
                        RUN[0]++;
                        if (placed) RUN[1]++;
                        if (RUN[0] % 500 == 0) LOGGER.info("1.16 world features: {} run, {} placed", RUN[0], RUN[1]);
                    }
                } catch (Throwable e) {
                    if (FAILED.add(feature)) ModernFailures.add("A 1.16 mod's world feature in " + biome + " was switched off", e);
                }
            }
        }
    }

    /** Asks the mods for each biome's features, as Forge 1.16's biome loading does. */
    private static Map<String, List<List<Supplier<ConfiguredFeature<?, ?>>>>> collect() {
        Map<String, List<List<Supplier<ConfiguredFeature<?, ?>>>>> collected = new HashMap<>();
        int count = 0;
        // The constructor taking the biome (Forge's event transformer gives every event one taking nothing too).
        Constructor<?> event = null;
        try {
            for (Constructor<?> constructor : BiomeLoadingEvent.class.getConstructors()) {
                if (event == null || constructor.getParameterCount() > event.getParameterCount()) event = constructor;
            }
        } catch (RuntimeException | LinkageError e) {
            ModernFailures.add("1.16 mods' world generation is off: Forge 1.16's biome loading couldn't be set up", e);
            return collected;
        }
        for (Object[] biome : com.michaelsebero.portium.mods116.ModernBiomes116.biomes()) {
            try {
                BiomeGenerationSettingsBuilder generation = generationBuilder();
                Object posted = event.newInstance(new ResourceLocation((String) biome[0]), climate(biome), Biome.Category.valueOf((String) biome[1]),
                        (Float) biome[5], (Float) biome[6], null, generation, spawnBuilder());
                MinecraftForge.EVENT_BUS.post((BiomeLoadingEvent) posted);
                List<List<Supplier<ConfiguredFeature<?, ?>>>> stages = new ArrayList<>();
                boolean any = false;
                for (GenerationStage.Decoration stage : GenerationStage.Decoration.values()) {
                    List<Supplier<ConfiguredFeature<?, ?>>> added = new ArrayList<>(((BiomeLoadingEvent) posted).getGeneration().getFeatures(stage));
                    any |= !added.isEmpty();
                    count += added.size();
                    stages.add(added);
                }
                if (any) collected.put((String) biome[0], stages);
            } catch (Throwable e) {
                // A failure for one biome is a failure for all: reported once, and the mods' features stay off.
                Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e;
                ModernFailures.add("1.16 mods' world generation is off: Forge 1.16's biome loading failed (for " + biome[0] + ")", cause);
                return new HashMap<>();
            }
        }
        LOGGER.info("1.16 mods add {} world features across {} biomes", count, collected.size());
        return collected;
    }

    /** 1.16's climate of a biome; its constructor is private. Null if it can't be made. */
    @Nullable
    private static Object climate(Object[] biome) {
        try {
            Class<?> climate = Class.forName("net.minecraft.world.biome.Biome$Climate", true, WorldGenGlue.class.getClassLoader());
            for (Constructor<?> constructor : climate.getDeclaredConstructors()) {
                if (constructor.getParameterCount() != 4) continue;
                constructor.setAccessible(true);
                return constructor.newInstance(Biome.RainType.valueOf((String) biome[2]), biome[3], Biome.TemperatureModifier.NONE, biome[4]);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.debug("Couldn't make 1.16's climate of {}", biome[0], e);
        }
        return null;
    }

    /** An empty generation settings builder, made without 1.16's default settings. */
    private static BiomeGenerationSettingsBuilder generationBuilder() throws ReflectiveOperationException {
        BiomeGenerationSettingsBuilder builder = allocate(BiomeGenerationSettingsBuilder.class);
        Class<?> base = net.minecraft.world.biome.BiomeGenerationSettings.Builder.class;
        set(base, builder, "field_242504_a", Optional.empty());
        set(base, builder, "field_242505_b", new LinkedHashMap<>());
        set(base, builder, "field_242506_c", new ArrayList<>());
        set(base, builder, "field_242507_d", new ArrayList<>());
        return builder;
    }

    /** An empty spawn builder, made without 1.16's empty spawns. */
    private static MobSpawnInfoBuilder spawnBuilder() throws ReflectiveOperationException {
        MobSpawnInfoBuilder builder = allocate(MobSpawnInfoBuilder.class);
        Class<?> base = net.minecraft.world.biome.MobSpawnInfo.Builder.class;
        Map<Object, List<Object>> spawners = new LinkedHashMap<>();
        for (Object classification : net.minecraft.entity.EntityClassification.values()) spawners.put(classification, new ArrayList<>());
        Map<Object, Object> costs = new LinkedHashMap<>();
        set(base, builder, "field_242567_a", spawners);
        set(base, builder, "field_242568_b", costs);
        set(base, builder, "field_242569_c", 0.1F);
        set(MobSpawnInfoBuilder.class, builder, "typesView", Collections.unmodifiableSet(spawners.keySet()));
        set(MobSpawnInfoBuilder.class, builder, "costView", Collections.unmodifiableSet(costs.keySet()));
        return builder;
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(Class<T> type) throws ReflectiveOperationException {
        Field unsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafe.setAccessible(true);
        return (T) ((sun.misc.Unsafe) unsafe.get(null)).allocateInstance(type);
    }

    private static void set(Class<?> owner, Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private WorldGenGlue() {}
}
