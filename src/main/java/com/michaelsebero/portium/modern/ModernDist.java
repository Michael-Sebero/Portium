package com.michaelsebero.portium.modern;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.util.List;

/**
 * What Forge 1.13's runtime dist cleaner does to classes as they load: members marked {@code @OnlyIn} another side
 * than this one are removed, as are interfaces marked so through {@code _interface}, and a class marked so can't load.
 * 1.13 mods count on it as 1.12 mods count on {@code @SideOnly}: a client-only method referring to client classes,
 * say, never reaches a dedicated server.
 */
final class ModernDist {

    private static final String ONLY_IN = "Lnet/minecraftforge/api/distmarker/OnlyIn;";
    private static final String ONLY_INS = "Lnet/minecraftforge/api/distmarker/OnlyIns;";

    static void clean(ClassNode node, String dist) {
        String classDist = dist(node.visibleAnnotations);
        if (classDist == null) classDist = dist(node.invisibleAnnotations);
        if (classDist != null && !classDist.equals(dist)) {
            throw new RuntimeException("Attempted to load class " + node.name + " for invalid dist " + dist);
        }
        cleanInterfaces(node, node.visibleAnnotations, dist);
        cleanInterfaces(node, node.invisibleAnnotations, dist);
        node.fields.removeIf(field -> wrongDist(field.visibleAnnotations, field.invisibleAnnotations, dist));
        node.methods.removeIf(method -> wrongDist(method.visibleAnnotations, method.invisibleAnnotations, dist));
    }

    private static boolean wrongDist(@Nullable List<AnnotationNode> visible, @Nullable List<AnnotationNode> invisible, String dist) {
        String marked = dist(visible);
        if (marked == null) marked = dist(invisible);
        return marked != null && !marked.equals(dist);
    }

    /** The side an {@code @OnlyIn} among these marks for the member itself, or null if none does. */
    @Nullable
    private static String dist(@Nullable List<AnnotationNode> annotations) {
        if (annotations == null) return null;
        for (AnnotationNode annotation : annotations) {
            if (ONLY_IN.equals(annotation.desc) && interfaceOf(annotation) == null) return value(annotation);
        }
        return null;
    }

    private static void cleanInterfaces(ClassNode node, @Nullable List<AnnotationNode> annotations, String dist) {
        if (annotations == null) return;
        for (AnnotationNode annotation : annotations) {
            if (ONLY_IN.equals(annotation.desc)) {
                removeInterface(node, annotation, dist);
            } else if (ONLY_INS.equals(annotation.desc) && annotation.values != null) {
                for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                    if (!"value".equals(annotation.values.get(i))) continue;
                    for (Object each : (List<?>) annotation.values.get(i + 1)) removeInterface(node, (AnnotationNode) each, dist);
                }
            }
        }
    }

    private static void removeInterface(ClassNode node, AnnotationNode annotation, String dist) {
        Type type = interfaceOf(annotation);
        String marked = value(annotation);
        if (type != null && marked != null && !marked.equals(dist)) node.interfaces.remove(type.getInternalName());
    }

    @Nullable
    private static Type interfaceOf(AnnotationNode annotation) {
        if (annotation.values == null) return null;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if ("_interface".equals(annotation.values.get(i))) return (Type) annotation.values.get(i + 1);
        }
        return null;
    }

    @Nullable
    private static String value(AnnotationNode annotation) {
        if (annotation.values == null) return null;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if ("value".equals(annotation.values.get(i))) return ((String[]) annotation.values.get(i + 1))[1];
        }
        return null;
    }

    private ModernDist() {}
}
