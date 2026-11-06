package com.michaelsebero.portium.client.model;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;

import java.util.Set;
import java.util.function.Function;

/**
 * Stand-ins for the models that 1.16.5's item models, fetched as templates, name in their overrides (the crossbow's
 * pulling, arrow and firework models). Portium's own models list their own overrides, so a template's are loaded but
 * never used. Without these Forge looks for 1.16's own files, and if they were there it would loop through their parent.
 */
public final class TemplateOverrideStubs implements ICustomModelLoader {

    private static final Set<ResourceLocation> STUBS;

    static {
        ImmutableSet.Builder<ResourceLocation> stubs = ImmutableSet.builder();
        for (String model : new String[] {"crossbow_pulling_0", "crossbow_pulling_1", "crossbow_pulling_2", "crossbow_arrow", "crossbow_firework"}) {
            stubs.add(new ResourceLocation("minecraft", "models/item/" + model));
        }
        STUBS = stubs.build();
    }

    private static final IModel STUB = new IModel() {
        @Override
        public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textures) {
            return ModelLoaderRegistry.getMissingModel().bake(state, format, textures);
        }
    };

    public static void register() {
        ModelLoaderRegistry.registerLoader(new TemplateOverrideStubs());
    }

    @Override
    public boolean accepts(ResourceLocation location) {
        return STUBS.contains(location);
    }

    @Override
    public IModel loadModel(ResourceLocation location) {
        return STUB;
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {}

    private TemplateOverrideStubs() {}
}
