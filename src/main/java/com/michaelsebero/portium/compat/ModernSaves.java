package com.michaelsebero.portium.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.michaelsebero.portium.Portium;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.scoreboard.IScoreCriteria;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.IFixType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Lets 1.12 load what 1.13 to 1.16 saved. Every save file 1.12 reads goes through its data fixer, tagged with the
 * version that wrote it; data from 1.13.0 to 1.16.5 is rewritten into 1.12's layout there, before 1.12 reads it, and
 * marked as 1.12.2 data. 1.12 then saves it in its own format, so a world converts piece by piece as it is played.
 */
public final class ModernSaves {

    /** 1.12.2's data version, which converted data is given. */
    public static final int LEGACY_VERSION = 1343;
    /** The first 1.13 snapshot with flattened blocks (17w47a); earlier snapshots still saved as 1.12 does. */
    public static final int FIRST_FLATTENED = 1451;
    /** 1.16.5's data version, the last this converts. */
    public static final int LAST_SUPPORTED = 2586;
    /** 20w17a (1.16): a section's block indices no longer run on across longs; each long holds whole ones. */
    public static final int FIRST_PADDED_STATES = 2527;

    public static boolean isModern(int version) {
        return version >= FIRST_FLATTENED && version <= LAST_SUPPORTED;
    }

    /** The data in 1.12's layout if 1.13 wrote it, or as it is. */
    public static NBTTagCompound downgrade(IFixType type, NBTTagCompound tag) {
        int version = tag.hasKey("DataVersion", 99) ? tag.getInteger("DataVersion") : -1;
        if (!isModern(version) || !(type instanceof FixTypes)) return tag;
        NBTTagCompound converted;
        try {
            switch ((FixTypes) type) {
                case CHUNK:
                    converted = LegacyChunkNbt.convert(tag);
                    break;
                case LEVEL:
                    converted = LegacyLevelNbt.convert(tag);
                    break;
                case PLAYER:
                case ENTITY:
                    converted = LegacyEntityNbt.convert(tag);
                    break;
                case BLOCK_ENTITY:
                    converted = LegacyBlockEntityNbt.convert(tag, null, new java.util.HashMap<>());
                    break;
                case ITEM_INSTANCE:
                    converted = LegacyItemNbt.convertStack(tag);
                    break;
                case STRUCTURE:
                    converted = LegacyStructureNbt.convert(tag);
                    break;
                default:
                    return tag;
            }
        } catch (RuntimeException e) {
            Portium.LOGGER.error("Couldn't convert {} data saved by version {}; 1.12 will read it as it is", type, version, e);
            return tag;
        }
        converted.setInteger("DataVersion", LEGACY_VERSION);
        return converted;
    }

    /** 1.13 adds its data version to a player's advancements file, which 1.12 would reject the whole file for. */
    public static String withoutDataVersion(String json) {
        if (!json.contains("\"DataVersion\"")) return json;
        try {
            JsonElement parsed = new JsonParser().parse(json);
            if (!parsed.isJsonObject()) return json;
            parsed.getAsJsonObject().remove("DataVersion");
            return parsed.toString();
        } catch (RuntimeException e) {
            return json;
        }
    }

    /**
     * 1.13 groups a player's statistics by type ({@code {"stats":{"minecraft:mined":{"minecraft:stone":5}}}}); 1.12
     * keeps one flat list ({@code {"stat.mineBlock.minecraft.stone":5}}). Statistics for things 1.12 merges, such as
     * the colours of wool, are added together; ones 1.12 has no statistic for are dropped.
     */
    public static String legacyStats(String json) {
        try {
            JsonElement parsed = new JsonParser().parse(json);
            if (!parsed.isJsonObject() || !parsed.getAsJsonObject().has("stats") || !parsed.getAsJsonObject().get("stats").isJsonObject()) return json;
            JsonObject legacy = new JsonObject();
            for (Map.Entry<String, JsonElement> type : parsed.getAsJsonObject().getAsJsonObject("stats").entrySet()) {
                if (!type.getValue().isJsonObject()) continue;
                for (Map.Entry<String, JsonElement> stat : type.getValue().getAsJsonObject().entrySet()) {
                    IScoreCriteria criteria = LegacyCriteria.convert(type.getKey().replace(':', '.') + ":" + stat.getKey().replace(':', '.'));
                    if (criteria == null || !stat.getValue().isJsonPrimitive()) continue;
                    String name = criteria.getName();
                    int previous = legacy.has(name) ? legacy.get(name).getAsInt() : 0;
                    legacy.addProperty(name, previous + stat.getValue().getAsInt());
                }
            }
            return legacy.toString();
        } catch (RuntimeException e) {
            Portium.LOGGER.error("Couldn't convert 1.13 statistics", e);
            return json;
        }
    }

    /**
     * 1.13 scoreboards keep display names, team prefixes and suffixes as JSON text and use its own criteria names;
     * 1.12 keeps formatted text and its own names. Converted in place as the scoreboard loads.
     */
    public static void convertScoreboard(NBTTagCompound data) {
        NBTTagList objectives = data.getTagList("Objectives", 10);
        for (int i = 0; i < objectives.tagCount(); i++) {
            NBTTagCompound objective = objectives.getCompoundTagAt(i);
            String criteria = objective.getString("CriteriaName");
            if (!IScoreCriteria.INSTANCES.containsKey(criteria)) {
                IScoreCriteria legacy = LegacyCriteria.convert(criteria);
                if (legacy != null) objective.setString("CriteriaName", legacy.getName());
            }
            objective.setString("DisplayName", LegacyItemNbt.textToLegacy(objective.getString("DisplayName")));
        }
        NBTTagList teams = data.getTagList("Teams", 10);
        for (int i = 0; i < teams.tagCount(); i++) {
            NBTTagCompound team = teams.getCompoundTagAt(i);
            team.setString("DisplayName", LegacyItemNbt.textToLegacy(team.getString("DisplayName")));
            if (team.hasKey("MemberNamePrefix", 8)) team.setString("Prefix", LegacyItemNbt.textToLegacy(team.getString("MemberNamePrefix")));
            if (team.hasKey("MemberNameSuffix", 8)) team.setString("Suffix", LegacyItemNbt.textToLegacy(team.getString("MemberNameSuffix")));
        }
    }

    /**
     * Zips a world folder into {@code backups/} beside the saves folder, named by date and folder as 1.13 names its
     * backups. Returns the zip.
     */
    public static File backup(File saves, String folder) throws IOException {
        File world = new File(saves, folder);
        File backups = new File(saves.getParentFile(), "backups");
        Files.createDirectories(backups.toPath());
        String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(new Date());
        File zip = new File(backups, date + "_" + folder + ".zip");
        Path root = world.toPath();
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip)); Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                if (!Files.isRegularFile(file) || file.getFileName().toString().equals("session.lock")) continue;
                out.putNextEntry(new ZipEntry(folder + "/" + root.relativize(file).toString().replace(File.separatorChar, '/')));
                Files.copy(file, out);
                out.closeEntry();
            }
        }
        return zip;
    }

    private ModernSaves() {}
}
