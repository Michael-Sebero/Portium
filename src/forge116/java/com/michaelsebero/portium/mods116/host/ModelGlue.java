package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.modern.client.ModernRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/** Forge 1.16's model data, which Forge 1.12 doesn't have: 1.12 asks for quads without it, and gets them with it here. */
public final class ModelGlue {

    /** {@code model.getQuads(state, side, random, data)} on a model of 1.12's: without the data. */
    public static List<BakedQuad> getQuads(IBakedModel model, @Nullable BlockState state, @Nullable Direction side, Random random, IModelData data) {
        return model.func_200117_a(state, side, random);
    }

    /**
     * 1.12 asking a Forge 1.16 model for its quads: as Forge 1.16 would, with the model data the model (and its block's
     * tile entity) gives for the block being drawn, if one is (see {@link ModernRenderContext}).
     */
    @SuppressWarnings("unchecked")
    public static List<BakedQuad> quads(Object model, @Nullable BlockState state, @Nullable Direction side, long seed) {
        IModelData data = EmptyModelData.INSTANCE;
        Object[] context = state == null ? null : ModernRenderContext.current();
        if (context != null) {
            IBlockReader world = (IBlockReader) context[0];
            BlockPos pos = (BlockPos) context[1];
            IModelData tileData = EmptyModelData.INSTANCE;
            TileEntity tileEntity = world.func_175625_s(pos);
            Method tileMethod = tileEntity == null ? null : method(tileEntity.getClass(), "getModelData");
            if (tileMethod != null) tileData = (IModelData) call(tileMethod, tileEntity);
            Method modelMethod = method(model.getClass(), "getModelData", IBlockDisplayReader.class, BlockPos.class, BlockState.class, IModelData.class);
            if (modelMethod != null) data = (IModelData) call(modelMethod, model, world, pos, state, tileData == null ? EmptyModelData.INSTANCE : tileData);
        }
        Method quads = method(model.getClass(), "getQuads", BlockState.class, Direction.class, Random.class, IModelData.class);
        if (quads == null) return java.util.Collections.emptyList();
        return (List<BakedQuad>) call(quads, model, state, side, new Random(seed), data == null ? EmptyModelData.INSTANCE : data);
    }

    private static final Map<String, Optional<Method>> METHODS = new ConcurrentHashMap<>();

    @Nullable
    private static Method method(Class<?> type, String name, Class<?>... parameters) {
        return METHODS.computeIfAbsent(type.getName() + "." + name + parameters.length, key -> {
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                try {
                    Method found = c.getDeclaredMethod(name, parameters);
                    found.setAccessible(true);
                    return Optional.of(found);
                } catch (NoSuchMethodException e) {
                    // Further up.
                }
            }
            for (Class<?> itf : type.getInterfaces()) {
                try {
                    return Optional.of(itf.getMethod(name, parameters));
                } catch (NoSuchMethodException e) {
                    // Not this one.
                }
            }
            return Optional.empty();
        }).orElse(null);
    }

    private static Object call(Method method, Object target, Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IllegalStateException(cause);
        }
    }

    private ModelGlue() {}
}
