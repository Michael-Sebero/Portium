package com.michaelsebero.portium.datapack;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.compat.LegacyStructureNbt;
import com.michaelsebero.portium.mixin.accessor.TemplateManagerAccessor;
import com.michaelsebero.portium.world.gen.structure.StructureTemplates;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.Template;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

/** Structure templates from data packs, for structure blocks and the structures that generate from templates. */
public final class DataPackStructures {

    /** The template from the topmost enabled pack that has one, converted for 1.12, or null. */
    @Nullable
    public static Template load(ResourceLocation id) {
        DataPackManager packs = DataPackManager.get();
        if (packs == null) return null;
        try (InputStream stream = packs.open(id, "structures", ".nbt")) {
            if (stream == null) return null;
            Template template = new Template();
            template.read(LegacyStructureNbt.convert(CompressedStreamTools.readCompressed(stream)));
            return template;
        } catch (IOException | RuntimeException e) {
            Portium.LOGGER.error("Couldn't load structure {} from a data pack", id, e);
            return null;
        }
    }

    /** Forgets loaded templates, so the next use reads the packs again (1.13 does this on reload). */
    static void clearCaches(MinecraftServer server) {
        for (WorldServer world : server.worlds) ((TemplateManagerAccessor) world.getStructureTemplateManager()).portium$getTemplates().clear();
        StructureTemplates.clearCache();
    }

    private DataPackStructures() {}
}
