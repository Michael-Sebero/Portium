package com.michaelsebero.portium.modern.host;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import sun.misc.Unsafe;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fills 1.13 mods' {@code @ObjectHolder} fields from 1.12's registries after each registry event, as Forge 1.13 does:
 * an annotated class names a namespace for all its public static final fields (named after them, lowercased), and an
 * annotated field names its object, qualified by its class's holder namespace or mod id when it has none of its own.
 */
final class ModernObjectHolders {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.13.2");
    private static final Type OBJECT_HOLDER = Type.getType(ObjectHolder.class);
    private static final Type MOD = Type.getType(Mod.class);
    private static final Unsafe UNSAFE = unsafe();

    private static final class Holder {

        final Field field;
        final ResourceLocation name;

        Holder(Field field, ResourceLocation name) {
            this.field = field;
            this.name = name;
        }
    }

    private final List<Holder> holders = new ArrayList<>();

    /** Finds the object holders in a mod jar's scan. */
    void scan(ModFileScanData scan, ClassLoader loader) {
        Map<Type, String> namespaces = new HashMap<>();
        for (ModFileScanData.AnnotationData annotation : scan.getAnnotations()) {
            if (annotation.getTargetType() != ElementType.TYPE) continue;
            if (OBJECT_HOLDER.equals(annotation.getAnnotationType()) || MOD.equals(annotation.getAnnotationType())) {
                namespaces.putIfAbsent(annotation.getClassType(), (String) annotation.getAnnotationData().get("value"));
            }
        }
        for (ModFileScanData.AnnotationData annotation : scan.getAnnotations()) {
            if (!OBJECT_HOLDER.equals(annotation.getAnnotationType())) continue;
            String value = (String) annotation.getAnnotationData().get("value");
            try {
                Class<?> owner = Class.forName(annotation.getClassType().getClassName(), true, loader);
                if (annotation.getTargetType() == ElementType.TYPE) {
                    for (Field field : owner.getFields()) {
                        int modifiers = field.getModifiers();
                        if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && !field.isAnnotationPresent(ObjectHolder.class)) {
                            this.holders.add(new Holder(field, new ResourceLocation(value, field.getName())));
                        }
                    }
                } else if (annotation.getTargetType() == ElementType.FIELD) {
                    String name = value;
                    if (name.indexOf(':') < 0) {
                        String namespace = namespaces.get(annotation.getClassType());
                        if (namespace == null) {
                            LOGGER.warn("Ignoring @ObjectHolder(\"{}\") on {}.{}: it names no mod", value, owner.getName(), annotation.getMemberName());
                            continue;
                        }
                        name = namespace + ":" + name;
                    }
                    this.holders.add(new Holder(owner.getDeclaredField(annotation.getMemberName()), new ResourceLocation(name)));
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't read @ObjectHolder on " + annotation.getClassType().getClassName(), e);
            }
        }
    }

    /** Fills the holders whose type the registry holds, from what's in it now. */
    void apply(IForgeRegistry<?> registry) {
        Class<?> type = registry.getRegistrySuperType();
        for (Holder holder : this.holders) {
            if (!type.isAssignableFrom(holder.field.getType())) continue;
            if (!registry.containsKey(holder.name)) continue;
            Object value = registry.getValue(holder.name);
            if (!holder.field.getType().isInstance(value)) {
                LOGGER.warn("@ObjectHolder {} on {}.{} is a {}, which doesn't fit", holder.name, holder.field.getDeclaringClass().getName(),
                        holder.field.getName(), value.getClass().getName());
                continue;
            }
            // Static final fields: written past the final, as Forge does.
            UNSAFE.putObjectVolatile(UNSAFE.staticFieldBase(holder.field), UNSAFE.staticFieldOffset(holder.field), value);
        }
    }

    private static Unsafe unsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
