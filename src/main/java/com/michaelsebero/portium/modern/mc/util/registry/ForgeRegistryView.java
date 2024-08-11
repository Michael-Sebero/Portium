package com.michaelsebero.portium.modern.mc.util.registry;

import com.michaelsebero.portium.modern.ModernTileEntityTypes;
import com.michaelsebero.portium.modern.flat.FlattenedBlock;
import com.michaelsebero.portium.modern.flat.FlattenedItemView;
import com.michaelsebero.portium.modern.flat.Flattening;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/**
 * One of 1.13's registries that 1.12 keeps in a Forge registry, read and written through that. Registering through
 * it works while Forge's registries take entries, as in 1.13.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ForgeRegistryView<T> implements IRegistry<T> {

    private final Supplier<IForgeRegistry> registry;
    @Nullable
    private final ResourceLocation defaultKey;

    public ForgeRegistryView(Supplier<IForgeRegistry> registry, @Nullable ResourceLocation defaultKey) {
        this.registry = registry;
        this.defaultKey = defaultKey;
    }

    /** The view of the Forge registry of a type ({@code GameData.getWrapper}). */
    @SuppressWarnings("unchecked")
    public static <T> ForgeRegistryView<T> of(Class<T> type) {
        if (type == Block.class) return (ForgeRegistryView<T>) blocks();
        if (type == Item.class) return (ForgeRegistryView<T>) items();
        IForgeRegistry found = net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry((Class) type);
        return new ForgeRegistryView<>(() -> net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistry((Class) type),
                found == null ? null : com.michaelsebero.portium.modern.shim.RegistryShims.getDefaultKey(found));
    }

    /** Blocks, by 1.13's names: flattened variants are their views. */
    static ForgeRegistryView<Block> blocks() {
        return new ForgeRegistryView<Block>(() -> ForgeRegistries.BLOCKS, new ResourceLocation("air")) {
            @Override
            public ResourceLocation getKey(Block value) {
                return value instanceof FlattenedBlock ? value.getRegistryName() : super.getKey(value);
            }

            @Override
            public Block getNullable(@Nullable ResourceLocation name) {
                Block found = super.getNullable(name);
                if (found != null || name == null || !"minecraft".equals(name.getNamespace())) return found;
                Block flattened = Flattening.block(name.toString(), Block.class);
                return flattened == net.minecraft.init.Blocks.AIR ? null : flattened;
            }
        };
    }

    /** Items, by 1.13's names. */
    static ForgeRegistryView<Item> items() {
        return new ForgeRegistryView<Item>(() -> ForgeRegistries.ITEMS, new ResourceLocation("air")) {
            @Override
            public ResourceLocation getKey(Item value) {
                return value instanceof FlattenedItemView ? value.getRegistryName() : super.getKey(value);
            }

            @Override
            public Item getNullable(@Nullable ResourceLocation name) {
                Item found = super.getNullable(name);
                if (found != null || name == null || !"minecraft".equals(name.getNamespace())) return found;
                Item flattened = Flattening.item(name.toString(), Item.class);
                return flattened == net.minecraft.init.Items.AIR ? null : flattened;
            }
        };
    }

    /** Tile entity types, Portium's Forge registry of them. */
    static ForgeRegistryView tileEntityTypes() {
        return new ForgeRegistryView(() -> RegistryManager.ACTIVE.getRegistry(ModernTileEntityTypes.NAME), null);
    }

    @Nullable
    private IForgeRegistry registry() {
        return this.registry.get();
    }

    @Nullable
    @Override
    public ResourceLocation getKey(T value) {
        IForgeRegistry registry = registry();
        ResourceLocation key = registry == null || value == null ? null : registry.getKey((IForgeRegistryEntry) value);
        return key != null ? key : this.defaultKey;
    }

    @Override
    public T get(@Nullable ResourceLocation name) {
        T value = getNullable(name);
        return value != null || this.defaultKey == null ? value : getNullable(this.defaultKey);
    }

    @Override
    public ResourceLocation getDefaultKey() {
        return this.defaultKey;
    }

    @Override
    public int getId(@Nullable T value) {
        IForgeRegistry registry = registry();
        return registry instanceof ForgeRegistry && value != null ? ((ForgeRegistry) registry).getID((IForgeRegistryEntry) value) : -1;
    }

    @Nullable
    @Override
    public T get(int id) {
        IForgeRegistry registry = registry();
        return registry instanceof ForgeRegistry ? (T) ((ForgeRegistry) registry).getValue(id) : null;
    }

    @Override
    public Iterator<T> iterator() {
        IForgeRegistry registry = registry();
        return registry == null ? new ArrayList<T>().iterator() : (Iterator<T>) registry.getValuesCollection().iterator();
    }

    @Nullable
    @Override
    public T getNullable(@Nullable ResourceLocation name) {
        IForgeRegistry registry = registry();
        if (registry == null || name == null || !registry.containsKey(name)) return null;
        return (T) registry.getValue(name);
    }

    @Override
    public void register(int id, ResourceLocation name, T value) {
        put(name, value);
    }

    @Override
    public void put(ResourceLocation name, T value) {
        IForgeRegistryEntry entry = (IForgeRegistryEntry) value;
        if (entry.getRegistryName() == null) entry.setRegistryName(name);
        registry().register(entry);
    }

    @Override
    public Set<ResourceLocation> keySet() {
        IForgeRegistry registry = registry();
        return registry == null ? java.util.Collections.emptySet() : registry.getKeys();
    }

    @Override
    public boolean isEmpty() {
        return keySet().isEmpty();
    }

    @Nullable
    @Override
    public T getRandom(Random random) {
        List<T> values = new ArrayList<>();
        iterator().forEachRemaining(values::add);
        return values.isEmpty() ? null : values.get(random.nextInt(values.size()));
    }

    @Override
    public boolean containsKey(ResourceLocation name) {
        return getNullable(name) != null;
    }
}
