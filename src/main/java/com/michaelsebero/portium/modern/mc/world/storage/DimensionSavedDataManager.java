package com.michaelsebero.portium.modern.mc.world.storage;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * 1.14's {@code DimensionSavedDataManager}: a world's saved data by name, made by a supplier (1.14's data classes
 * have no-argument constructors, which 1.12's {@code MapStorage} can't make). Kept in 1.12's per-world storage, so
 * it saves with the world as 1.12's data does.
 */
public class DimensionSavedDataManager {

    private static final Map<World, DimensionSavedDataManager> BY_WORLD = new WeakHashMap<>();

    private final World world;

    private DimensionSavedDataManager(World world) {
        this.world = world;
    }

    /** 1.16's {@code serverWorld.getSavedData()}. */
    public static synchronized DimensionSavedDataManager of(World world) {
        return BY_WORLD.computeIfAbsent(world, DimensionSavedDataManager::new);
    }

    private MapStorage storage() {
        return this.world.getPerWorldStorage();
    }

    /** {@code getOrCreate(supplier, name)}: the data saved under the name, or a new one (marked to be saved). */
    @Srg("func_215752_a")
    public <T extends WorldSavedData> T getOrCreate(Supplier<T> supplier, String name) {
        T data = get(supplier, name);
        if (data == null) {
            data = supplier.get();
            set(data);
        }
        return data;
    }

    /** {@code get(supplier, name)}: the data saved under the name, read from its file if not loaded yet; null if none. */
    @Srg("func_215753_b")
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends WorldSavedData> T get(Supplier<T> supplier, String name) {
        MapStorage storage = storage();
        WorldSavedData loaded = loaded(storage, name);
        if (loaded != null) return (T) loaded;
        File file = this.world.getSaveHandler().getMapFileFromName(name);
        if (file == null || !file.isFile()) return null;
        try (InputStream in = new FileInputStream(file)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(in);
            T data = supplier.get();
            data.readFromNBT(root.getCompoundTag("data"));
            storage.setData(name, data);
            return data;
        } catch (IOException | RuntimeException e) {
            LogManager.getLogger("Portium/1.16.5").error("Couldn't read saved data {}", name, e);
            return null;
        }
    }

    /** {@code set(data)}: kept under its name and saved with the world. */
    @Srg("func_215757_a")
    public void set(WorldSavedData data) {
        storage().setData(data.mapName, data);
    }

    @Nullable
    private static WorldSavedData loaded(MapStorage storage, String name) {
        try {
            java.lang.reflect.Field field = net.minecraftforge.fml.common.ObfuscationReflectionHelper.findField(MapStorage.class, "field_75749_b");
            Object map = field.get(storage);
            return map instanceof Map ? (WorldSavedData) ((Map<?, ?>) map).get(name) : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
