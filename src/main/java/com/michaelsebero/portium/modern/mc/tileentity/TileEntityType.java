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

    @Srg("func_223045_a")
    public boolean isValidBlock(net.minecraft.block.Block block) {
        return this.validBlocks.contains(block);
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
        if (registry != null) registry.register(type);
        return type;
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
