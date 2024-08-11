package com.michaelsebero.portium.mods116;

import com.michaelsebero.portium.modern.mc.tileentity.TileEntityType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistryInternal;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 1.13's tile entity type registry ({@code minecraft:tileentities}), which 1.12 lacks, made as a Forge 1.12 registry
 * so 1.13 mods' registry events for it fire with the rest. Each type registered also has its tile entity class
 * registered under its name with 1.12's tile entity ids, which saved tile entities are found by.
 */
public final class ModernTileEntityTypes {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");
    public static final ResourceLocation NAME = new ResourceLocation("minecraft", "tileentities");

    /** Raw types: a generic type of its own, {@code TileEntityType<?>}, is beyond Forge 1.12's registry bounds. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void create() {
        if (RegistryManager.ACTIVE.getRegistry(NAME) != null) return;
        RegistryBuilder builder = new RegistryBuilder().setName(NAME).setType(TileEntityType.class).setMaxID(Short.MAX_VALUE).disableSaving();
        builder.add(new IForgeRegistry.AddCallback() {
            @Override
            public void onAdd(IForgeRegistryInternal owner, RegistryManager stage, int id, IForgeRegistryEntry entry, IForgeRegistryEntry old) {
                if (stage == RegistryManager.ACTIVE) added((TileEntityType<?>) entry);
            }
        });
        builder.create();
    }

    /** Types whose tile entities couldn't be made yet (1.16 factories often look their own type up once registered). */
    private static final java.util.List<TileEntityType<?>> PENDING = new java.util.concurrent.CopyOnWriteArrayList<>();

    private static void added(TileEntityType<?> type) {
        if (!learn(type, false)) PENDING.add(type);
    }

    /** Registers the tile entity class of each type still waiting, now that registry objects are filled. */
    public static void resolvePending() {
        for (TileEntityType<?> type : PENDING) {
            if (learn(type, true)) PENDING.remove(type);
        }
    }

    /** Registers the type's tile entity class with 1.12 under its name, making one to learn it; false if it can't yet. */
    private static boolean learn(TileEntityType<?> type, boolean report) {
        ResourceLocation name = type.getRegistryName();
        if (name == null) return true;
        Class<? extends TileEntity> tileClass;
        try {
            tileClass = type.create().getClass();
        } catch (RuntimeException | LinkageError e) {
            if (report) LOGGER.error("Couldn't make a tile entity of type {} to learn its class", name, e);
            return report;
        }
        if (TileEntity.getKey(tileClass) == null) GameRegistry.registerTileEntity(tileClass, name);
        com.michaelsebero.portium.modern.TileEntityFactories.register(tileClass, type::create);
        return true;
    }

    private ModernTileEntityTypes() {}
}
