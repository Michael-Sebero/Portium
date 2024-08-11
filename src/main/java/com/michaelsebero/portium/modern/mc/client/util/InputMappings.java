package com.michaelsebero.portium.modern.mc.client.util;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.client.ModernKeys;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 1.13's {@code net.minecraft.client.util.InputMappings}: keys and mouse buttons as GLFW numbers them. 1.12's key
 * bindings hold LWJGL 2's codes; {@link #toLegacy} and {@link #fromLegacy} turn one into the other.
 */
@SideOnly(Side.CLIENT)
public final class InputMappings {

    @Srg("field_197958_a")
    public static final Input INPUT_INVALID = Type.KEYSYM.getOrMakeInput(-1);

    private InputMappings() {}

    @Srg("func_197956_a")
    public static boolean isKeyDown(int key) {
        return ModernKeys.isKeyDown(key);
    }

    /** 1.14's: whether a key (GLFW's code) is down in the window (1.12 has the one). */
    @Srg("func_216506_a")
    public static boolean isKeyDown(long window, int key) {
        return isKeyDown(key);
    }

    @Srg("func_197954_a")
    public static Input getInputByCode(int key, int scanCode) {
        return key == -1 ? Type.SCANCODE.getOrMakeInput(scanCode) : Type.KEYSYM.getOrMakeInput(key);
    }

    @Srg("func_197955_a")
    public static Input getInputByName(String name) {
        Input known = Input.BY_NAME.get(name);
        if (known != null) return known;
        for (Type type : Type.values()) {
            if (name.startsWith(type.prefix)) return type.getOrMakeInput(Integer.parseInt(name.substring(type.prefix.length() + 1)));
        }
        throw new IllegalArgumentException("Unknown key name: " + name);
    }

    /** A key binding's LWJGL 2 code (mouse buttons are -100 and up, 0 is none) as 1.13's input. */
    public static Input fromLegacy(int code) {
        if (code == 0) return INPUT_INVALID;
        if (code < 0) return Type.MOUSE.getOrMakeInput(code + 100);
        return Type.KEYSYM.getOrMakeInput(ModernKeys.toGlfw(code));
    }

    /** 1.13's input as a key binding's LWJGL 2 code. */
    public static int toLegacy(Input input) {
        switch (input.type) {
            case MOUSE:
                return input.code - 100;
            case SCANCODE:
                return input.code;
            default:
                return input.code < 0 ? 0 : ModernKeys.toLwjgl(input.code);
        }
    }

    public static final class Input {

        static final Map<String, Input> BY_NAME = new HashMap<>();

        final String name;
        final Type type;
        final int code;

        Input(String name, Type type, int code) {
            this.name = name;
            this.type = type;
            this.code = code;
            BY_NAME.put(name, this);
        }

        /** The key's name to show. */
        @Srg("func_197936_a")
        public String getName() {
            if (this.type == Type.MOUSE) {
                String translated = I18n.format(this.name);
                return Objects.equals(translated, this.name) ? I18n.format(Type.MOUSE.prefix, this.code + 1) : translated;
            }
            int legacy = toLegacy(this);
            String name = legacy > 0 ? Keyboard.getKeyName(legacy) : null;
            return name == null ? I18n.format(this.name) : name;
        }

        @Srg("func_197938_b")
        public Type getType() {
            return this.type;
        }

        @Srg("func_197937_c")
        public int getKeyCode() {
            return this.code;
        }

        @Srg("func_197935_d")
        public String getTranslationKey() {
            return this.name;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof Input && ((Input) other).code == this.code && ((Input) other).type == this.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.type, this.code);
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public enum Type {
        KEYSYM("key.keyboard"),
        SCANCODE("scancode"),
        MOUSE("key.mouse");

        private static final String[] MOUSE_NAMES = {"left", "middle", "right"};

        final String prefix;
        private final Map<Integer, Input> inputs = new HashMap<>();

        Type(String prefix) {
            this.prefix = prefix;
        }

        @Srg("func_197944_a")
        public synchronized Input getOrMakeInput(int code) {
            Input known = this.inputs.get(code);
            if (known != null) return known;
            String suffix = this == MOUSE ? (code >= 0 && code <= 2 ? "." + MOUSE_NAMES[code] : "." + (code + 1))
                    : this == KEYSYM && code == -1 ? ".unknown" : "." + code;
            Input input = new Input(this.prefix + suffix, this, code);
            this.inputs.put(code, input);
            return input;
        }
    }
}
