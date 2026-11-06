package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.mods116.shim.TagShims116;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Forge 1.16's optional named tags ({@code BlockTags.createOptional} and the like, which Forge adds to Minecraft's
 * classes and Minecraft 1.16.5's own lack): their contents come from the data packs of the world Portium runs.
 */
public final class TagGlue {

    public static Tags.IOptionalNamedTag<Object> blocks(ResourceLocation id) {
        return new OptionalTag("blocks", id);
    }

    public static Tags.IOptionalNamedTag<Object> blocks(ResourceLocation id, @Nullable Set<Supplier<Object>> defaults) {
        return new OptionalTag("blocks", id);
    }

    public static Tags.IOptionalNamedTag<Object> items(ResourceLocation id) {
        return new OptionalTag("items", id);
    }

    public static Tags.IOptionalNamedTag<Object> items(ResourceLocation id, @Nullable Set<Supplier<Object>> defaults) {
        return new OptionalTag("items", id);
    }

    public static Tags.IOptionalNamedTag<Object> fluids(ResourceLocation id) {
        return new OptionalTag("fluids", id);
    }

    public static Tags.IOptionalNamedTag<Object> fluids(ResourceLocation id, @Nullable Set<Supplier<Object>> defaults) {
        return new OptionalTag("fluids", id);
    }

    // 1.16's named tags ({@code ItemTags.makeWrapperTag("wool")}, and vanilla's own tag constants made the same way):
    // 1.16 binds them as tags load, which its own tag registries do and Portium's don't; these read Portium's tags live.

    public static net.minecraft.tags.ITag.INamedTag<Object> itemWrapper(String id) {
        return new OptionalTag("items", new ResourceLocation(id));
    }

    public static net.minecraft.tags.ITag.INamedTag<Object> blockWrapper(String id) {
        return new OptionalTag("blocks", new ResourceLocation(id));
    }

    public static net.minecraft.tags.ITag.INamedTag<Object> fluidWrapper(String id) {
        return new OptionalTag("fluids", new ResourceLocation(id));
    }

    /**
     * 1.16's {@code FluidTags.getAllTags()}: every named fluid tag. 1.16 lists the ones made by name; Portium's tags
     * come from the data packs, so all of theirs (Create looks its recipes' fluid tags up here: forge:honey...).
     */
    public static List<net.minecraft.tags.ITag.INamedTag<Object>> allFluidTags() {
        List<net.minecraft.tags.ITag.INamedTag<Object>> tags = new java.util.ArrayList<>();
        for (ResourceLocation id : TagShims116.ids("fluids")) tags.add(new OptionalTag("fluids", id));
        return tags;
    }

    public static net.minecraft.tags.ITag.INamedTag<Object> entityWrapper(String id) {
        return new OptionalTag("entity_types", new ResourceLocation(id));
    }

    /** Forge 1.16's {@code DyeColor.getTag()}: the item tag {@code forge:dyes/<colour>}. */
    public static Tags.IOptionalNamedTag<Object> dyeTag(net.minecraft.item.DyeColor color) {
        return new OptionalTag("items", new ResourceLocation("forge", "dyes/" + color.func_176610_l()));
    }

    // 1.16's tag collections (BlockTags.getCollection(), TagCollectionManager.getManager()...): views of Portium's tags.

    public static net.minecraft.tags.ITagCollection<Object> blockCollection() {
        return new LiveCollection("blocks");
    }

    public static net.minecraft.tags.ITagCollection<Object> itemCollection() {
        return new LiveCollection("items");
    }

    public static net.minecraft.tags.ITagCollection<Object> fluidCollection() {
        return new LiveCollection("fluids");
    }

    public static net.minecraft.tags.ITagCollection<Object> entityCollection() {
        return new LiveCollection("entity_types");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.minecraft.tags.ITagCollectionSupplier collections() {
        return new net.minecraft.tags.ITagCollectionSupplier() {
            @Override
            public net.minecraft.tags.ITagCollection func_241835_a() {
                return blockCollection();
            }

            @Override
            public net.minecraft.tags.ITagCollection func_241836_b() {
                return itemCollection();
            }

            @Override
            public net.minecraft.tags.ITagCollection func_241837_c() {
                return fluidCollection();
            }

            @Override
            public net.minecraft.tags.ITagCollection func_241838_d() {
                return entityCollection();
            }
        };
    }

    /** 1.16's {@code DataPackRegistries.getTags()}: the same live collections. */
    public static net.minecraft.tags.ITagCollectionSupplier collections(Object registries) {
        return collections();
    }

    private static final class LiveCollection implements net.minecraft.tags.ITagCollection<Object> {

        private final String kind;

        LiveCollection(String kind) {
            this.kind = kind;
        }

        @Override
        public java.util.Map<ResourceLocation, net.minecraft.tags.ITag<Object>> func_241833_a() {
            java.util.Map<ResourceLocation, net.minecraft.tags.ITag<Object>> tags = new java.util.LinkedHashMap<>();
            for (ResourceLocation id : TagShims116.ids(this.kind)) tags.put(id, new OptionalTag(this.kind, id));
            return tags;
        }

        @Override
        @Nullable
        public net.minecraft.tags.ITag<Object> func_199910_a(ResourceLocation id) {
            return TagShims116.ids(this.kind).contains(id) ? new OptionalTag(this.kind, id) : null;
        }

        @Override
        public net.minecraft.tags.ITag<Object> func_241834_b(ResourceLocation id) {
            return new OptionalTag(this.kind, id);
        }

        @Override
        @Nullable
        public ResourceLocation func_232973_a_(net.minecraft.tags.ITag<Object> tag) {
            return tag instanceof OptionalTag ? ((OptionalTag) tag).id : null;
        }

        @Override
        public java.util.Collection<ResourceLocation> func_199908_a() {
            return TagShims116.ids(this.kind);
        }

        @Override
        public java.util.Collection<ResourceLocation> func_199913_a(Object value) {
            List<ResourceLocation> ids = new java.util.ArrayList<>();
            for (ResourceLocation id : TagShims116.ids(this.kind)) if (TagShims116.contains(this.kind, id, value)) ids.add(id);
            return ids;
        }
    }

    private static final class OptionalTag implements Tags.IOptionalNamedTag<Object> {

        private final String kind;
        private final ResourceLocation id;

        OptionalTag(String kind, ResourceLocation id) {
            this.kind = kind;
            this.id = id;
        }

        @Override
        public ResourceLocation func_230234_a_() {
            return this.id;
        }

        @Override
        public boolean func_230235_a_(Object value) {
            return TagShims116.contains(this.kind, this.id, value);
        }

        @Override
        public List<Object> func_230236_b_() {
            return TagShims116.values(this.kind, this.id);
        }

        @Override
        public boolean isDefaulted() {
            return false;
        }

        @Override
        public String toString() {
            return "Tag[" + this.kind + " " + this.id + "]";
        }
    }

    private TagGlue() {}
}
