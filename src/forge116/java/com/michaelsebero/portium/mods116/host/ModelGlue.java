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

    /** Dev: logs casings' quads as the world asks for them (-Dportium.dev.quads=true). */
    private static final boolean DEBUG_QUADS = Boolean.getBoolean("portium.dev.quads");

    /**
     * {@code model.getQuads(state, side, random, data)}, Forge 1.16's call: the model's own version if it has one (unless
     * this is that version calling super), else as Forge 1.16's {@code BakedModelWrapper} answers for 1.12's wrappers
     * (the wrapped model, with the data), else 1.12's quads, without the data.
     */
    public static List<BakedQuad> getQuads(IBakedModel model, @Nullable BlockState state, @Nullable Direction side, Random random, IModelData data) {
        Method own = withData(model);
        if (own != null && !inside(model)) {
            List<BakedQuad> quads = ask(own, model, state, side, random, data);
            if (quads != null) return quads;
        }
        IBakedModel wrapped = wrapped(model);
        if (wrapped != null) return getQuads(wrapped, state, side, random, data);
        if (own != null) return java.util.Collections.emptyList();
        return model.func_200117_a(state, side, random);
    }

    /** Models in their own Forge 1.16 {@code getQuads} on this thread: one asked again is calling super. */
    private static final ThreadLocal<java.util.IdentityHashMap<Object, Integer>> INSIDE = ThreadLocal.withInitial(java.util.IdentityHashMap::new);

    private static boolean inside(Object model) {
        return INSIDE.get().containsKey(model);
    }

    /** Model classes whose Forge 1.16 quads failed, reported once each. */
    private static final java.util.Set<Class<?>> FAILED = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * The model's own Forge 1.16 quads; null if they failed (the caller falls back to 1.12's), as chunks are drawn
     * on worker threads, where a mod's error would otherwise take the game down.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    private static List<BakedQuad> ask(Method own, Object model, @Nullable BlockState state, @Nullable Direction side, Random random, IModelData data) {
        java.util.IdentityHashMap<Object, Integer> inside = INSIDE.get();
        inside.merge(model, 1, Integer::sum);
        try {
            List<BakedQuad> quads = (List<BakedQuad>) call(own, model, state, side, random, data == null ? EmptyModelData.INSTANCE : data);
            return quads == null ? java.util.Collections.emptyList() : quads;
        } catch (RuntimeException | LinkageError e) {
            if (FAILED.add(model.getClass())) {
                com.michaelsebero.portium.mods116.ModernFailures.add("Model " + model.getClass().getName() + "'s quads failed; it's drawn as 1.12 would", e);
            }
            return null;
        } finally {
            inside.computeIfPresent(model, (key, depth) -> depth > 1 ? depth - 1 : null);
        }
    }

    /** A model's Forge 1.16 {@code getQuads} with model data, if it has one. */
    @Nullable
    private static Method withData(Object model) {
        return method(model.getClass(), "getQuads", BlockState.class, Direction.class, Random.class, IModelData.class);
    }

    @Nullable
    private static java.lang.reflect.Field original;

    /** The model a Forge 1.12 {@code BakedModelWrapper} wraps (Create's bracketed shafts and cogs are ones). */
    @Nullable
    private static IBakedModel wrapped(Object model) {
        if (!(model instanceof net.minecraftforge.client.model.BakedModelWrapper)) return null;
        try {
            if (original == null) {
                java.lang.reflect.Field field = net.minecraftforge.client.model.BakedModelWrapper.class.getDeclaredField("originalModel");
                field.setAccessible(true);
                original = field;
            }
            return (IBakedModel) original.get(model);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /**
     * 1.12 asking a Forge 1.16 model for its quads: as Forge 1.16 would, with the model data the model (and its block's
     * tile entity) gives for the block being drawn, if one is (see {@link ModernRenderContext}).
     */
    public static List<BakedQuad> quads(Object model, @Nullable BlockState state, @Nullable Direction side, long seed) {
        List<BakedQuad> quads = quadsOrSuper(model, state, side, seed);
        return quads != null ? quads : java.util.Collections.emptyList();
    }

    /**
     * As {@link #quads}, for a Forge 1.16 model under one of 1.12's classes that answers 1.12 itself (Forge 1.12's
     * {@code BakedModelWrapper}): null while the model is in its own 1.16 {@code getQuads}, which then asked for its
     * 1.12 quads through super, so the 1.12 class above answers.
     */
    @Nullable
    public static List<BakedQuad> quadsOrSuper(Object model, @Nullable BlockState state, @Nullable Direction side, long seed) {
        if (inside(model)) return null;
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
        Method own = withData(model);
        if (own == null) return java.util.Collections.emptyList();
        List<BakedQuad> quads = ask(own, model, state, side, new Random(seed), data);
        if (DEBUG_QUADS && quads != null && String.valueOf(state).contains("casing")) {
            for (Object quad : quads) {
                float[] v = com.michaelsebero.portium.client.RenderBridge116.vertices(quad, state);
                org.apache.logging.log4j.LogManager.getLogger("Portium/quads").info("  world {} {} data {} uv {},{}", state, side, data, v[3], v[4]);
            }
        }
        return quads;
    }

    /**
     * Forge 1.16's first step in {@code BlockModelRenderer.renderModel}: {@code model.getModelData(world, pos, state,
     * data)}, which lets the model add its own (Create's kinetic models mark "brackets only" here, without which the
     * whole cog is drawn under the turning one).
     */
    public static IModelData modelData(@Nullable Object model, IBlockDisplayReader world, BlockPos pos, BlockState state, @Nullable IModelData data) {
        IModelData given = data == null ? EmptyModelData.INSTANCE : data;
        if (model == null) return given;
        Method modelMethod = method(model.getClass(), "getModelData", IBlockDisplayReader.class, BlockPos.class, BlockState.class, IModelData.class);
        if (modelMethod == null) return given;
        // A model's own asking its interface's default (super.getModelData) comes back here: the default keeps the data.
        java.util.Set<Object> asking = MODEL_DATA.get();
        if (!asking.add(model)) return given;
        try {
            IModelData refined = (IModelData) call(modelMethod, model, world, pos, state, given);
            return refined == null ? given : refined;
        } finally {
            asking.remove(model);
        }
    }

    private static final ThreadLocal<java.util.Set<Object>> MODEL_DATA = ThreadLocal.withInitial(
            () -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));

    /** Forge 1.16's {@code tileEntity.getModelData()}: the tile entity's own, if its class has one; else none. */
    public static IModelData tileModelData(TileEntity tileEntity) {
        Method own = method(tileEntity.getClass(), "getModelData");
        IModelData data = own == null ? null : (IModelData) call(own, tileEntity);
        return data == null ? EmptyModelData.INSTANCE : data;
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
