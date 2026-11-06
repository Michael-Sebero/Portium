package com.michaelsebero.portium.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.DefaultStateMapper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Forge 1.16's model loading on 1.12's: extra models a mod asks for by name ({@code ModelLoader.addSpecialModel},
 * which Flywheel's partial models use), whose textures 1.12 stitches and whose baked models it keeps under that
 * plain name, where 1.16 keeps them; and 1.16's model name for a block state. Client only.
 */
public final class ModelShims116 {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");
    private static final DefaultStateMapper STATE_NAMES = new DefaultStateMapper();
    private static final Set<ResourceLocation> SPECIAL = Collections.synchronizedSet(new LinkedHashSet<>());

    /** {@code ModelLoader.addSpecialModel(location)}. */
    public static void addSpecialModel(ResourceLocation location) {
        SPECIAL.add(location);
    }

    /** {@code BlockModelShapes.getModelLocation(state)}: the block's name and the state's properties, as 1.12 names them too. */
    public static ModelResourceLocation modelLocation(IBlockState state) {
        net.minecraft.util.ResourceLocation id = state.getBlock().getRegistryName();
        // Portium's own blocks keep their models in Portium's folder, whatever namespace their ids took.
        if (id != null && com.michaelsebero.portium.init.PortiumBlocks.isOwn(state.getBlock())) id = new net.minecraft.util.ResourceLocation(
                com.michaelsebero.portium.Tags.MOD_ID, id.getPath());
        return new ModelResourceLocation(id, STATE_NAMES.getPropertyString(state.getProperties()));
    }

    private static Set<ResourceLocation> builtinTextures;

    /** 1.12's {@code ModelBakery.LOCATIONS_BUILTIN_TEXTURES}: textures the blocks' atlas always has. */
    @SuppressWarnings("unchecked")
    public static synchronized Set<ResourceLocation> builtinTextures() {
        if (builtinTextures == null) {
            for (String name : new String[] {"LOCATIONS_BUILTIN_TEXTURES", "field_177602_b"}) {
                try {
                    java.lang.reflect.Field field = net.minecraft.client.renderer.block.model.ModelBakery.class.getDeclaredField(name);
                    field.setAccessible(true);
                    builtinTextures = (Set<ResourceLocation>) field.get(null);
                    break;
                } catch (ReflectiveOperationException e) {
                    // The other name.
                }
            }
            if (builtinTextures == null) builtinTextures = new LinkedHashSet<>();
        }
        return builtinTextures;
    }

    /** Asks the blocks' atlas for the extra models' textures. */
    public static void stitch(TextureMap map) {
        for (ResourceLocation location : snapshot()) {
            try {
                for (ResourceLocation texture : ModelLoaderRegistry.getModelOrMissing(location).getTextures()) map.registerSprite(texture);
            } catch (RuntimeException e) {
                LOGGER.warn("Couldn't load model {}", location, e);
            }
        }
    }

    /** Bakes the extra models into the registry, under their plain names (1.12's keys are model names with variants). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void bake(IRegistry registry) {
        for (ResourceLocation location : snapshot()) {
            try {
                IModel model = ModelLoaderRegistry.getModelOrMissing(location);
                IBakedModel baked = model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
                registry.putObject(location, baked);
            } catch (RuntimeException e) {
                LOGGER.warn("Couldn't bake model {}", location, e);
            }
        }
    }

    /** Forge 1.16's {@code modelBakery.getBakedModel(name, transform, sprites)}: with 1.12's own sprites. */
    public static IBakedModel bake(Object bakery, ResourceLocation location, Object transform, Object sprites) {
        return bake(bakery, location, transform);
    }

    /** 1.16's {@code modelBakery.bake(name, transform)}: the model by name, baked in the transform if it's one of 1.12's. */
    public static IBakedModel bake(Object bakery, ResourceLocation location, Object transform) {
        IModel model = ModelLoaderRegistry.getModelOrMissing(location);
        net.minecraftforge.common.model.IModelState state = transform instanceof net.minecraftforge.common.model.IModelState
                ? (net.minecraftforge.common.model.IModelState) transform : model.getDefaultState();
        return model.bake(state, DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
    }

    private static ResourceLocation[] snapshot() {
        synchronized (SPECIAL) {
            return SPECIAL.toArray(new ResourceLocation[0]);
        }
    }

    private ModelShims116() {}
}
