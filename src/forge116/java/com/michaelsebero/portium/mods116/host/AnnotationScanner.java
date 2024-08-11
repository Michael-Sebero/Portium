package com.michaelsebero.portium.mods116.host;

import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads a class's annotations into Forge 1.16's scan data, as its own mod scanner does (enums as its enum holders,
 * arrays as lists). Forge's scanner asks ASM for a newer API level than 1.12's ASM has; this one asks for 1.12's.
 */
final class AnnotationScanner extends ClassVisitor {

    private Type type;
    private Type parent;
    private final Set<Type> interfaces = new LinkedHashSet<>();
    private final List<ModAnnotation> annotations = new ArrayList<>();

    AnnotationScanner() {
        super(Opcodes.ASM5);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.type = Type.getObjectType(name);
        this.parent = superName == null ? null : Type.getObjectType(superName);
        if (interfaces != null) for (String itf : interfaces) this.interfaces.add(Type.getObjectType(itf));
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return annotation(new ModAnnotation(ElementType.TYPE, Type.getType(desc), this.type.getClassName()));
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return new FieldVisitor(Opcodes.ASM5) {
            @Override
            public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                return AnnotationScanner.this.annotation(new ModAnnotation(ElementType.FIELD, Type.getType(annotation), name));
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new MethodVisitor(Opcodes.ASM5) {
            @Override
            public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                return AnnotationScanner.this.annotation(new ModAnnotation(ElementType.METHOD, Type.getType(annotation), name + desc));
            }
        };
    }

    private AnnotationVisitor annotation(ModAnnotation annotation) {
        this.annotations.add(annotation);
        return new Values(annotation);
    }

    /** Adds what was read to the scan, as Forge's scanner does. */
    void addTo(ModFileScanData scan) {
        scan.getClasses().add(new ModFileScanData.ClassData(this.type, this.parent, this.interfaces));
        for (ModAnnotation annotation : this.annotations) scan.getAnnotations().add(ModAnnotation.fromModAnnotation(this.type, annotation));
    }

    /** An annotation's values, nested annotations and arrays included. */
    private static final class Values extends AnnotationVisitor {

        private final ModAnnotation annotation;
        private final boolean array;

        Values(ModAnnotation annotation) {
            this(annotation, false);
        }

        private Values(ModAnnotation annotation, boolean array) {
            super(Opcodes.ASM5);
            this.annotation = annotation;
            this.array = array;
        }

        @Override
        public void visit(String name, Object value) {
            this.annotation.addProperty(name, value);
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            this.annotation.addEnumProperty(name, desc, value);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            this.annotation.addArray(name);
            return new Values(this.annotation, true);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc) {
            return new Values(this.annotation.addChildAnnotation(name, desc));
        }

        @Override
        public void visitEnd() {
            if (this.array) this.annotation.endArray();
        }
    }
}
