package com.michaelsebero.portium.modern.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A 1.13 mod's jar as one of the game's built-in resource packs, as Forge 1.13 makes every mod. Vanilla-format
 * blockstate files are given 1.12's variant names on the way: 1.13 matches variants by any of a state's properties
 * (and names the one state of a block without properties ""), where 1.12 looks variants up by every property, in
 * order, or "normal". Forge-format ones ({@code forge_marker}) read the same in both.
 */
public class ModernResourcePack extends FileResourcePack {

    private static final Pattern BLOCKSTATE = Pattern.compile("assets/([^/]+)/blockstates/(.+)\\.json");

    private final String name;

    public ModernResourcePack(File jar) {
        super(jar);
        this.name = jar.getName();
    }

    @Override
    protected InputStream getInputStreamByName(String path) throws IOException {
        InputStream in = super.getInputStreamByName(path);
        Matcher blockstate = BLOCKSTATE.matcher(path);
        if (!blockstate.matches()) return in;
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockstate.group(1), blockstate.group(2)));
        if (block == null) return in;
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            return new ByteArrayInputStream(convert(json, block).toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 1.12's blockstate file for a 1.13 one: each state gets the first 1.13 variant that matches it, and models are
     * named from {@code models/block}, as 1.12 looks them up, rather than from {@code models}.
     */
    static JsonObject convert(JsonObject json, Block block) {
        // Forge's own format reads the same in Forge 1.12.
        if (json.has("forge_marker")) return json;
        if (json.has("multipart")) {
            for (JsonElement part : json.getAsJsonArray("multipart")) {
                if (part.getAsJsonObject().has("apply")) relativizeModels(part.getAsJsonObject().get("apply"));
            }
        }
        if (!json.has("variants")) return json;
        for (Map.Entry<String, JsonElement> variant : json.getAsJsonObject("variants").entrySet()) relativizeModels(variant.getValue());
        JsonObject variants = json.getAsJsonObject("variants");
        JsonObject legacy = new JsonObject();
        for (IBlockState state : block.getBlockState().getValidStates()) {
            String key = key(state);
            for (Map.Entry<String, JsonElement> variant : variants.entrySet()) {
                if (matches(variant.getKey(), state)) {
                    legacy.add(key, variant.getValue());
                    break;
                }
            }
        }
        JsonObject converted = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) converted.add(entry.getKey(), entry.getValue());
        converted.add("variants", legacy);
        return converted;
    }

    /** {@code namespace:block/name} becomes {@code namespace:name}, which 1.12 reads from {@code models/block}. */
    private static void relativizeModels(JsonElement models) {
        if (models.isJsonArray()) {
            for (JsonElement model : models.getAsJsonArray()) relativizeModels(model);
            return;
        }
        JsonObject model = models.getAsJsonObject();
        if (!model.has("model")) return;
        String name = model.get("model").getAsString();
        int colon = name.indexOf(':');
        String path = name.substring(colon + 1);
        if (path.startsWith("block/")) model.addProperty("model", name.substring(0, colon + 1) + path.substring("block/".length()));
    }

    /** 1.12's name for a state's variant: its properties by name, or "normal". */
    private static String key(IBlockState state) {
        StringBuilder key = new StringBuilder();
        for (Map.Entry<IProperty<?>, Comparable<?>> property : state.getProperties().entrySet()) {
            if (key.length() > 0) key.append(',');
            key.append(property.getKey().getName()).append('=').append(valueName(property.getKey(), property.getValue()));
        }
        return key.length() == 0 ? "normal" : key.toString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String valueName(IProperty property, Comparable value) {
        return property.getName(value);
    }

    /** Whether a 1.13 variant name, a list of property=value pairs that may leave properties out, fits a state. */
    private static boolean matches(String variant, IBlockState state) {
        if (variant.isEmpty() || variant.equals("normal")) return true;
        for (String condition : variant.split(",")) {
            int equals = condition.indexOf('=');
            if (equals < 0) return false;
            IProperty<?> property = state.getBlock().getBlockState().getProperty(condition.substring(0, equals));
            if (property == null || !valueName(property, state.getValue(property)).equals(condition.substring(equals + 1))) return false;
        }
        return true;
    }

    @Nullable
    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) {
        try {
            return super.getPackMetadata(serializer, section);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    @Override
    public String getPackName() {
        return this.name;
    }
}
