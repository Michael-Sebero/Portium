package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.client.EntityRendering116;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 1.16's models of 1.12's renderers. A mod asking a vanilla renderer for its model (to add a layer to the player, say)
 * gets 1.16's biped model standing for 1.12's: its parts follow 1.12's model's pose, copied before each of the mod's
 * layers draws. Mods' own renderers are 1.16's and answer with their own models.
 */
public final class ModelProxyGlue {

    private static final Map<Object, BipedModel<?>> PROXIES = new WeakHashMap<>();

    /** {@code livingRenderer.getEntityModel()}. */
    @Nullable
    public static EntityModel<?> entityModel(Object renderer) {
        Object legacy = EntityRendering116.legacyMainModel(renderer);
        if (legacy != null) return proxy(legacy);
        return (EntityModel<?>) ownModel(renderer);
    }

    /** The stand-in for a 1.12 model, made once; null if it's no biped (1.16 code then sees no model it knows). */
    @Nullable
    public static synchronized BipedModel<?> proxy(Object legacy) {
        if (EntityRendering116.bipedPose(legacy) == null) return null;
        return PROXIES.computeIfAbsent(legacy, key -> new BipedModel<>(0.0F));
    }

    /** Copies the 1.12 model's pose into its stand-in, if it has one. */
    public static synchronized void sync(Object legacy) {
        BipedModel<?> proxy = PROXIES.get(legacy);
        float[] pose = proxy == null ? null : EntityRendering116.bipedPose(legacy);
        if (pose == null) return;
        ModelRenderer[] parts = {proxy.field_78116_c, proxy.field_78115_e, proxy.field_178723_h, proxy.field_178724_i, proxy.field_178721_j, proxy.field_178722_k};
        for (int i = 0; i < parts.length; i++) {
            ModelRenderer part = parts[i];
            int at = i * 7;
            part.field_78795_f = pose[at];
            part.field_78796_g = pose[at + 1];
            part.field_78808_h = pose[at + 2];
            part.field_78800_c = pose[at + 3];
            part.field_78797_d = pose[at + 4];
            part.field_78798_e = pose[at + 5];
            part.field_78806_j = pose[at + 6] != 0.0F;
        }
        proxy.field_178720_f.func_217177_a(proxy.field_78116_c);
        proxy.field_228270_o_ = pose[42] != 0.0F;
        proxy.field_217114_e = pose[43] != 0.0F;
    }

    @Nullable
    private static Object ownModel(Object renderer) {
        for (Class<?> c = renderer.getClass(); c != null; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && "func_217764_d".equals(method.getName())) {
                    try {
                        method.setAccessible(true);
                        return method.invoke(renderer);
                    } catch (ReflectiveOperationException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private ModelProxyGlue() {}
}
