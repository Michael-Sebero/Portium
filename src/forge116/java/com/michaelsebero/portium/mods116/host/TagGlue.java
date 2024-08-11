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

    public static net.minecraft.tags.ITag.INamedTag<Object> entityWrapper(String id) {
        return new OptionalTag("entity_types", new ResourceLocation(id));
    }

    /** Forge 1.16's {@code DyeColor.getTag()}: the item tag {@code forge:dyes/<colour>}. */
    public static Tags.IOptionalNamedTag<Object> dyeTag(net.minecraft.item.DyeColor color) {
        return new OptionalTag("items", new ResourceLocation("forge", "dyes/" + color.func_176610_l()));
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
