package com.michaelsebero.portium.datapack;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.api.IDataPackSettings;
import com.michaelsebero.portium.command.PortiumCommands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.io.File;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The running server's data packs: which are enabled and in what order (saved in {@code level.dat} as 1.13 does),
 * and the tags and functions loaded from them. {@code /reload} rescans the datapacks folder, enabling new packs.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class DataPackManager {

    @Nullable
    private static DataPackManager instance;

    private final MinecraftServer server;
    private final DataPackRepository repository;
    private final List<String> enabled = new ArrayList<>();
    private final List<String> disabled = new ArrayList<>();
    private final TagManager tags = new TagManager();
    private final FunctionManager functions;
    private List<DataPack> loaded = new ArrayList<>();

    private DataPackManager(MinecraftServer server, File folder) {
        this.server = server;
        this.repository = new DataPackRepository(folder);
        this.functions = new FunctionManager(server, PortiumCommands.getDispatcher());
    }

    @Nullable
    public static DataPackManager get() {
        return instance;
    }

    /** Called once the worlds have loaded: everything the packs hold (their tags again). */
    public static void start(MinecraftServer server) {
        if (instance == null || instance.server != server) create(server);
        instance.reload();
    }

    /**
     * With the overworld, before 1.12 generates the spawn area: only the packs' tags, which world generation reads
     * (1.16 mods' ores test the blocks they replace against tags), as 1.13 has its packs before generating anything.
     * Functions and recipes wait for {@link #start}, once mods have registered their commands.
     */
    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (!com.michaelsebero.portium.PortiumConfig.compatibility.dataPacks || event.getWorld().isRemote || event.getWorld().provider.getDimension() != 0) return;
        MinecraftServer server = event.getWorld().getMinecraftServer();
        if (server == null || instance != null && instance.server == server) return;
        create(server);
        instance.repository.scan();
        instance.enableNew(false);
        List<DataPack> packs = new ArrayList<>();
        for (String id : instance.enabled) packs.add(instance.repository.getPacks().get(id));
        instance.tags.load(packs);
    }

    private static void create(MinecraftServer server) {
        File folder = new File(server.getWorld(0).getSaveHandler().getWorldDirectory(), "datapacks");
        instance = new DataPackManager(server, folder);
        instance.readSettings();
    }

    public static void stop() {
        instance = null;
        DataPackRecipes.reset();
    }

    /** For {@code type=#tag} selectors. */
    public static boolean entityTypeTagContains(ResourceLocation tagId, Entity entity) {
        if (instance == null) return false;
        Tag<ResourceLocation> tag = instance.tags.getEntityTypes().get(tagId);
        if (tag == null) return false;
        ResourceLocation type = entity instanceof EntityPlayer ? new ResourceLocation("player") : EntityList.getKey(entity);
        return type != null && tag.getValues().contains(type);
    }

    public TagManager getTags() {
        return this.tags;
    }

    public FunctionManager getFunctions() {
        return this.functions;
    }

    public DataPackRepository getRepository() {
        return this.repository;
    }

    public List<String> getEnabled() {
        return this.enabled;
    }

    public Collection<String> getAvailable() {
        return this.repository.getPacks().keySet();
    }

    private WorldInfo worldInfo() {
        return this.server.getWorld(0).getWorldInfo();
    }

    private void readSettings() {
        NBTTagCompound settings = IDataPackSettings.of(this.worldInfo()).portium$getDataPacks();
        if (settings.isEmpty()) {
            this.enabled.add(DataPackRepository.VANILLA);
            return;
        }
        NBTTagList enabled = settings.getTagList("Enabled", 8);
        for (int i = 0; i < enabled.tagCount(); i++) this.enabled.add(enabled.getStringTagAt(i));
        NBTTagList disabled = settings.getTagList("Disabled", 8);
        for (int i = 0; i < disabled.tagCount(); i++) this.disabled.add(disabled.getStringTagAt(i));
    }

    private void saveSettings() {
        NBTTagCompound settings = new NBTTagCompound();
        NBTTagList enabled = new NBTTagList();
        for (String id : this.enabled) enabled.appendTag(new NBTTagString(id));
        NBTTagList disabled = new NBTTagList();
        for (String id : this.disabled) disabled.appendTag(new NBTTagString(id));
        settings.setTag("Enabled", enabled);
        settings.setTag("Disabled", disabled);
        IDataPackSettings.of(this.worldInfo()).portium$setDataPacks(settings);
    }

    /** Rescans for packs, enables any new ones, and loads everything again. */
    public void reload() {
        this.repository.scan();
        this.enableNew(true);
        this.saveSettings();
        this.load();
    }

    /** Drops packs that are gone and enables packs not seen before (vanilla first). */
    private void enableNew(boolean log) {
        this.enabled.removeIf(id -> !this.repository.getPacks().containsKey(id));
        for (String id : this.repository.getPacks().keySet()) {
            if (!this.enabled.contains(id) && !this.disabled.contains(id)) {
                if (log) Portium.LOGGER.info("Found new data pack {}, loading it automatically", id);
                if (DataPackRepository.VANILLA.equals(id)) this.enabled.add(0, id);
                else this.enabled.add(id);
            }
        }
    }

    /** Loads tags, functions and recipes, then has 1.12 reload loot tables, advancements and structures, which read the packs. */
    private void load() {
        List<DataPack> packs = new ArrayList<>();
        for (String id : this.enabled) packs.add(this.repository.getPacks().get(id));
        this.loaded = packs;
        this.tags.load(packs);
        this.functions.load(packs, this.tags);
        DataPackRecipes.load(this, this.tags);
        com.michaelsebero.portium.mods116.Mods116.dataReloaded();
        DataPackStructures.clearCaches(this.server);
        this.server.reload();
    }

    /** Every recipe file in the enabled packs, as text by id (the topmost pack's), for newer mods' recipe managers. */
    public static java.util.Map<String, String> recipeSources() {
        java.util.Map<String, String> sources = new java.util.LinkedHashMap<>();
        DataPackManager packs = instance;
        if (packs == null) return sources;
        for (ResourceLocation id : packs.list("recipes", ".json")) {
            try (InputStream stream = packs.open(id, "recipes", ".json")) {
                if (stream == null) continue;
                sources.put(id.toString(), new String(org.apache.commons.io.IOUtils.toByteArray(stream), java.nio.charset.StandardCharsets.UTF_8));
            } catch (IOException e) {
                Portium.LOGGER.warn("Couldn't read recipe {}", id, e);
            }
        }
        return sources;
    }

    /** Opens the topmost enabled pack's {@code data/<namespace>/<folder>/<path><extension>}, or returns null. */
    @Nullable
    public InputStream open(ResourceLocation id, String folder, String extension) throws IOException {
        String path = "data/" + id.getNamespace() + "/" + folder + "/" + id.getPath() + extension;
        for (int i = this.loaded.size() - 1; i >= 0; i--) {
            InputStream stream = this.loaded.get(i).open(path);
            if (stream != null) return stream;
        }
        return null;
    }

    /** Every file id in a folder across the enabled packs. */
    public Set<ResourceLocation> list(String folder, String extension) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (DataPack pack : this.loaded) ids.addAll(pack.list(folder, extension));
        return ids;
    }

    /** Enables a pack at the given position in the load order (0 loads first, so later packs override it). */
    public void enable(String id, int position) {
        this.disabled.remove(id);
        this.enabled.remove(id);
        this.enabled.add(Math.max(0, Math.min(position, this.enabled.size())), id);
        this.saveSettings();
        this.load();
    }

    public void disable(String id) {
        this.enabled.remove(id);
        if (!this.disabled.contains(id)) this.disabled.add(id);
        this.saveSettings();
        this.load();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && instance != null) instance.functions.tick();
    }
}
