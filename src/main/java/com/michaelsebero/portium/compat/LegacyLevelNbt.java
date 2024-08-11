package com.michaelsebero.portium.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Rewrites 1.13's level.dat data into 1.12's: generator settings go back from a compound to a string (Superflat's own
 * preset format, or JSON for Buffet worlds as Portium keeps them), and the singleplayer player is converted. 1.16
 * keeps the seed and each dimension's generator in {@code WorldGenSettings}; the Overworld's gives 1.12's world type.
 */
public final class LegacyLevelNbt {

    /** The flat world structures 1.12 knows, by 1.13's names for them. */
    private static final List<String> FLAT_STRUCTURES = Arrays.asList("village", "mineshaft", "stronghold", "biome_1", "dungeon", "decoration",
            "lake", "lava_lake", "oceanmonument");
    private static final Set<String> TEMPLES = new LinkedHashSet<>(Arrays.asList("desert_pyramid", "jungle_pyramid", "jungle_temple", "swamp_hut", "igloo"));

    public static NBTTagCompound convert(NBTTagCompound modern) {
        NBTTagCompound tag = modern.copy();
        if (tag.hasKey("generatorOptions", 10)) {
            NBTTagCompound options = tag.getCompoundTag("generatorOptions");
            boolean flat = "flat".equalsIgnoreCase(tag.getString("generatorName"));
            tag.setString("generatorOptions", flat ? flatToLegacy(options) : options.isEmpty() ? "" : toJson(options).toString());
        }
        if (tag.hasKey("WorldGenSettings", 10)) worldGenSettings(tag, tag.getCompoundTag("WorldGenSettings"));
        if (tag.hasKey("Player", 10)) tag.setTag("Player", LegacyEntityNbt.convert(tag.getCompoundTag("Player")));
        return tag;
    }

    /** 1.16's world generation settings as 1.12's seed, features flag, world type and generator options. */
    private static void worldGenSettings(NBTTagCompound tag, NBTTagCompound settings) {
        tag.setLong("RandomSeed", settings.getLong("seed"));
        if (settings.hasKey("generate_features", 99)) tag.setBoolean("MapFeatures", settings.getBoolean("generate_features"));
        NBTTagCompound generator = settings.getCompoundTag("dimensions").getCompoundTag("minecraft:overworld").getCompoundTag("generator");
        String type = generator.getString("type");
        String name = "default";
        String options = "";
        if (type.endsWith("flat")) {
            name = "flat";
            options = flatToLegacy(generator.getCompoundTag("settings"));
        } else if (type.endsWith("debug")) {
            name = "debug_all_block_states";
        } else if ("minecraft:amplified".equals(generator.getString("settings"))) {
            name = "amplified";
        } else if (generator.getCompoundTag("biome_source").getBoolean("large_biomes")) {
            name = "largeBiomes";
        }
        tag.setString("generatorName", name);
        tag.setInteger("generatorVersion", 1);
        tag.setString("generatorOptions", options);
    }

    /** 1.13's Superflat compound as 1.12's preset string, such as {@code 3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village}. */
    static String flatToLegacy(NBTTagCompound options) {
        // Without layers 1.13 uses its default preset (as its servers' {flat_world_options:...} settings get), which
        // is 1.12's default too.
        if (!options.hasKey("layers", 9)) return "";
        StringBuilder layers = new StringBuilder();
        NBTTagList list = options.getTagList("layers", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound layer = list.getCompoundTagAt(i);
            IBlockState state = LegacyBlockStates.convert(layer.getString("block"), Collections.emptyMap());
            int height = Math.max(1, layer.getInteger("height"));
            if (layers.length() > 0) layers.append(',');
            if (height > 1) layers.append(height).append('*');
            layers.append(Block.REGISTRY.getNameForObject(state.getBlock()));
            int meta = state.getBlock().getMetaFromState(state);
            if (meta > 0) layers.append(':').append(meta);
        }
        if (layers.length() == 0) layers.append(Block.REGISTRY.getNameForObject(Blocks.AIR));
        Biome biome = options.hasKey("biome", 8) ? ForgeRegistries.BIOMES.getValue(new ResourceLocation(LegacyBiomes.toLegacy(options.getString("biome")))) : null;
        StringBuilder result = new StringBuilder("3;").append(layers).append(';').append(biome == null ? 1 : Biome.getIdForBiome(biome));
        StringBuilder structures = new StringBuilder();
        NBTTagCompound modernStructures = options.getCompoundTag("structures");
        // 1.16 lists its structures one level down, by namespaced name, with spacings 1.12 doesn't have.
        if (modernStructures.hasKey("structures", 10)) {
            NBTTagCompound flattened = new NBTTagCompound();
            for (String key : modernStructures.getKeySet()) {
                if (!"structures".equals(key)) flattened.setTag(key, new NBTTagCompound());
            }
            for (String key : modernStructures.getCompoundTag("structures").getKeySet()) {
                flattened.setTag(key.substring(key.indexOf(':') + 1), new NBTTagCompound());
            }
            modernStructures = flattened;
        }
        Set<String> written = new LinkedHashSet<>();
        for (String key : modernStructures.getKeySet()) {
            String name = TEMPLES.contains(key) ? "biome_1" : "ocean_monument".equals(key) ? "oceanmonument" : key;
            if (!FLAT_STRUCTURES.contains(name) || !written.add(name)) continue;
            if (structures.length() > 0) structures.append(',');
            structures.append(name);
            NBTTagCompound settings = modernStructures.getCompoundTag(key);
            if (!settings.isEmpty()) {
                StringBuilder values = new StringBuilder();
                for (String setting : settings.getKeySet()) {
                    if (values.length() > 0) values.append(' ');
                    NBTBase value = settings.getTag(setting);
                    values.append(setting).append('=').append(value instanceof NBTTagString ? ((NBTTagString) value).getString() : value.toString());
                }
                structures.append('(').append(values).append(')');
            }
        }
        if (structures.length() > 0) result.append(';').append(structures);
        return result.toString();
    }

    /** NBT as the JSON 1.13 would write for it. */
    public static JsonElement toJson(NBTBase tag) {
        if (tag instanceof NBTTagCompound) {
            JsonObject object = new JsonObject();
            for (String key : ((NBTTagCompound) tag).getKeySet()) object.add(key, toJson(((NBTTagCompound) tag).getTag(key)));
            return object;
        }
        if (tag instanceof NBTTagList) {
            JsonArray array = new JsonArray();
            NBTTagList list = (NBTTagList) tag;
            for (int i = 0; i < list.tagCount(); i++) array.add(toJson(list.get(i)));
            return array;
        }
        if (tag instanceof NBTTagString) return new JsonPrimitive(((NBTTagString) tag).getString());
        if (tag instanceof NBTPrimitive) return new JsonPrimitive(((NBTPrimitive) tag).getDouble() % 1 == 0 ? (Number) ((NBTPrimitive) tag).getLong() : ((NBTPrimitive) tag).getDouble());
        JsonArray array = new JsonArray();
        if (tag instanceof NBTTagByteArray) for (byte b : ((NBTTagByteArray) tag).getByteArray()) array.add(b);
        if (tag instanceof NBTTagIntArray) for (int i : ((NBTTagIntArray) tag).getIntArray()) array.add(i);
        return array;
    }

    private LegacyLevelNbt() {}
}
