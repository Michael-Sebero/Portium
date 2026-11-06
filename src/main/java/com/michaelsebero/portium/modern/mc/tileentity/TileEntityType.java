package com.michaelsebero.portium.modern.mc.tileentity;

import com.michaelsebero.portium.modern.ModernTileEntityTypes;
import com.michaelsebero.portium.modern.Srg;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.function.Supplier;

/**
 * 1.13's {@code net.minecraft.tileentity.TileEntityType}: a kind of tile entity, made by its factory. Registered in
 * Portium's tile entity type registry ({@code minecraft:tileentities}), which also registers the class with 1.12's
 * tile entity ids so saved tile entities load (see {@code ModernTileEntityTypes}). 1.13's data fixer types are
 * ignored.
 */
public class TileEntityType<T extends TileEntity> extends IForgeRegistryEntry.Impl<TileEntityType<?>> {

    private final Supplier<? extends T> factory;
    /** 1.16's: the blocks the type's tile entities belong to. */
    private java.util.Set<net.minecraft.block.Block> validBlocks = java.util.Collections.emptySet();

    public TileEntityType(Supplier<? extends T> factory, Object dataFixerType) {
        this.factory = factory;
    }

    /** 1.16's, as its builder makes them (some mods make their types directly): with the blocks they belong to. */
    public TileEntityType(Supplier<? extends T> factory, java.util.Set<net.minecraft.block.Block> validBlocks, Object dataFixerType) {
        this.factory = factory;
        if (validBlocks != null) this.validBlocks = validBlocks;
    }

    @Srg("func_223045_a")
    public boolean isValidBlock(net.minecraft.block.Block block) {
        return this.validBlocks.contains(block);
    }

    /** 1.15's {@code getIfExists(world, pos)}: the block entity there, if it's of this type. */
    @Srg("func_226986_a_")
    @javax.annotation.Nullable
    @SuppressWarnings("unchecked")
    public T getIfExists(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
        return tile instanceof com.michaelsebero.portium.modern.adapter.ModernTileEntity && ((com.michaelsebero.portium.modern.adapter.ModernTileEntity) tile).getType() == this ? (T) tile : null;
    }

    @Srg("func_200968_a")
    public T create() {
        return this.factory.get();
    }

    /** 1.13's way of making and registering a type at once (Forge 1.13's registry event is the other). */
    @Srg("func_200966_a")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends TileEntity> TileEntityType<T> register(String id, Builder<T> builder) {
        TileEntityType<T> type = builder.build(null);
        type.setRegistryName(new ResourceLocation(id));
        IForgeRegistry registry = RegistryManager.ACTIVE.getRegistry(ModernTileEntityTypes.NAME);
        if (registry == null) return type;
        // Vanilla's types are also made when newer code first reads TileEntityType.SIGN and the like, long after
        // registration: the registered one if there is one, else this one unregistered (1.12 keeps vanilla's).
        Object existing = registry.containsKey(type.getRegistryName()) ? registry.getValue(type.getRegistryName()) : null;
        if (existing instanceof TileEntityType) return (TileEntityType<T>) existing;
        if (registry instanceof net.minecraftforge.registries.ForgeRegistry && ((net.minecraftforge.registries.ForgeRegistry) registry).isLocked()) {
            // One per id, so a type read from TileEntityType.CHEST is the one a chest's getType() gives.
            return (TileEntityType<T>) UNREGISTERED.computeIfAbsent(type.getRegistryName(), key -> type);
        }
        registry.register(type);
        return type;
    }

    private static final java.util.Map<ResourceLocation, TileEntityType<?>> UNREGISTERED = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 1.16's {@code getType()} of any tile entity: a 1.13 or 1.16 mod's its own; one of 1.12's (vanilla's chest, or
     * an older mod's) the type of its 1.12 id, the registered one or else one made for it.
     */
    @javax.annotation.Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TileEntityType<?> of(TileEntity tile) {
        if (tile instanceof com.michaelsebero.portium.modern.adapter.ModernTileEntity) return ((com.michaelsebero.portium.modern.adapter.ModernTileEntity) tile).getType();
        ResourceLocation id = TileEntity.getKey(tile.getClass());
        if (id == null) return null;
        IForgeRegistry registry = RegistryManager.ACTIVE.getRegistry(ModernTileEntityTypes.NAME);
        if (registry != null && registry.containsKey(id)) return (TileEntityType<?>) registry.getValue(id);
        TileEntityType<?> known = UNREGISTERED.get(id);
        if (known != null) return known;
        Class<? extends TileEntity> kind = tile.getClass();
        return register(id.toString(), Builder.create(() -> {
            try {
                return kind.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Can't make a " + kind.getName(), e);
            }
        }));
    }

    @Srg("func_200969_a")
    public static ResourceLocation getId(TileEntityType<?> type) {
        return type.getRegistryName();
    }

    /** 1.13's {@code TileEntityType.Builder}. */
    public static final class Builder<T extends TileEntity> {

        private final Supplier<? extends T> factory;
        private java.util.Set<net.minecraft.block.Block> validBlocks = java.util.Collections.emptySet();

        private Builder(Supplier<? extends T> factory) {
            this.factory = factory;
        }

        /** 1.16's, with the blocks the tile entities belong to. */
        @Srg("func_223042_a")
        public static <T extends TileEntity> Builder<T> create(Supplier<? extends T> factory, net.minecraft.block.Block... validBlocks) {
            Builder<T> builder = new Builder<>(factory);
            builder.validBlocks = com.google.common.collect.ImmutableSet.copyOf(validBlocks);
            return builder;
        }

        @Srg("func_200963_a")
        public static <T extends TileEntity> Builder<T> create(Supplier<? extends T> factory) {
            return new Builder<>(factory);
        }

        @Srg("func_206865_a")
        public TileEntityType<T> build(Object dataFixerType) {
            TileEntityType<T> type = new TileEntityType<>(this.factory, dataFixerType);
            type.validBlocks = this.validBlocks;
            return type;
        }
    }
}
