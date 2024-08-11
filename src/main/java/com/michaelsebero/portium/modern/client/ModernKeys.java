package com.michaelsebero.portium.modern.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * Key codes: 1.13 runs on GLFW, whose codes are mostly characters ({@code 'A'} is 65, escape 256); 1.12 on LWJGL 2's
 * keyboard, whose codes are scan-code-like ({@code A} is 30, escape 1). 1.13 code's key codes are converted both
 * ways, and its modifier bits made from LWJGL's keyboard state.
 */
@SideOnly(Side.CLIENT)
public final class ModernKeys {

    private static final int[] TO_GLFW = new int[256];
    private static final int[] TO_LWJGL = new int[350];

    static {
        java.util.Arrays.fill(TO_GLFW, -1);
        java.util.Arrays.fill(TO_LWJGL, Keyboard.KEY_NONE);
        map(Keyboard.KEY_SPACE, 32);
        map(Keyboard.KEY_APOSTROPHE, 39);
        map(Keyboard.KEY_COMMA, 44);
        map(Keyboard.KEY_MINUS, 45);
        map(Keyboard.KEY_PERIOD, 46);
        map(Keyboard.KEY_SLASH, 47);
        int[] digits = {Keyboard.KEY_0, Keyboard.KEY_1, Keyboard.KEY_2, Keyboard.KEY_3, Keyboard.KEY_4, Keyboard.KEY_5, Keyboard.KEY_6, Keyboard.KEY_7,
                Keyboard.KEY_8, Keyboard.KEY_9};
        for (int i = 0; i < 10; i++) map(digits[i], 48 + i);
        map(Keyboard.KEY_SEMICOLON, 59);
        map(Keyboard.KEY_EQUALS, 61);
        int[] letters = {Keyboard.KEY_A, Keyboard.KEY_B, Keyboard.KEY_C, Keyboard.KEY_D, Keyboard.KEY_E, Keyboard.KEY_F, Keyboard.KEY_G, Keyboard.KEY_H,
                Keyboard.KEY_I, Keyboard.KEY_J, Keyboard.KEY_K, Keyboard.KEY_L, Keyboard.KEY_M, Keyboard.KEY_N, Keyboard.KEY_O, Keyboard.KEY_P, Keyboard.KEY_Q,
                Keyboard.KEY_R, Keyboard.KEY_S, Keyboard.KEY_T, Keyboard.KEY_U, Keyboard.KEY_V, Keyboard.KEY_W, Keyboard.KEY_X, Keyboard.KEY_Y, Keyboard.KEY_Z};
        for (int i = 0; i < 26; i++) map(letters[i], 65 + i);
        map(Keyboard.KEY_LBRACKET, 91);
        map(Keyboard.KEY_BACKSLASH, 92);
        map(Keyboard.KEY_RBRACKET, 93);
        map(Keyboard.KEY_GRAVE, 96);
        map(Keyboard.KEY_ESCAPE, 256);
        map(Keyboard.KEY_RETURN, 257);
        map(Keyboard.KEY_TAB, 258);
        map(Keyboard.KEY_BACK, 259);
        map(Keyboard.KEY_INSERT, 260);
        map(Keyboard.KEY_DELETE, 261);
        map(Keyboard.KEY_RIGHT, 262);
        map(Keyboard.KEY_LEFT, 263);
        map(Keyboard.KEY_DOWN, 264);
        map(Keyboard.KEY_UP, 265);
        map(Keyboard.KEY_PRIOR, 266);
        map(Keyboard.KEY_NEXT, 267);
        map(Keyboard.KEY_HOME, 268);
        map(Keyboard.KEY_END, 269);
        map(Keyboard.KEY_CAPITAL, 280);
        map(Keyboard.KEY_SCROLL, 281);
        map(Keyboard.KEY_NUMLOCK, 282);
        map(Keyboard.KEY_SYSRQ, 283);
        map(Keyboard.KEY_PAUSE, 284);
        int[] functions = {Keyboard.KEY_F1, Keyboard.KEY_F2, Keyboard.KEY_F3, Keyboard.KEY_F4, Keyboard.KEY_F5, Keyboard.KEY_F6, Keyboard.KEY_F7,
                Keyboard.KEY_F8, Keyboard.KEY_F9, Keyboard.KEY_F10, Keyboard.KEY_F11, Keyboard.KEY_F12, Keyboard.KEY_F13, Keyboard.KEY_F14, Keyboard.KEY_F15,
                Keyboard.KEY_F16, Keyboard.KEY_F17, Keyboard.KEY_F18, Keyboard.KEY_F19};
        for (int i = 0; i < functions.length; i++) map(functions[i], 290 + i);
        int[] pad = {Keyboard.KEY_NUMPAD0, Keyboard.KEY_NUMPAD1, Keyboard.KEY_NUMPAD2, Keyboard.KEY_NUMPAD3, Keyboard.KEY_NUMPAD4, Keyboard.KEY_NUMPAD5,
                Keyboard.KEY_NUMPAD6, Keyboard.KEY_NUMPAD7, Keyboard.KEY_NUMPAD8, Keyboard.KEY_NUMPAD9};
        for (int i = 0; i < 10; i++) map(pad[i], 320 + i);
        map(Keyboard.KEY_DECIMAL, 330);
        map(Keyboard.KEY_DIVIDE, 331);
        map(Keyboard.KEY_MULTIPLY, 332);
        map(Keyboard.KEY_SUBTRACT, 333);
        map(Keyboard.KEY_ADD, 334);
        map(Keyboard.KEY_NUMPADENTER, 335);
        map(Keyboard.KEY_NUMPADEQUALS, 336);
        map(Keyboard.KEY_LSHIFT, 340);
        map(Keyboard.KEY_LCONTROL, 341);
        map(Keyboard.KEY_LMENU, 342);
        map(Keyboard.KEY_LMETA, 343);
        map(Keyboard.KEY_RSHIFT, 344);
        map(Keyboard.KEY_RCONTROL, 345);
        map(Keyboard.KEY_RMENU, 346);
        map(Keyboard.KEY_RMETA, 347);
        map(Keyboard.KEY_APPS, 348);
    }

    private static void map(int lwjgl, int glfw) {
        TO_GLFW[lwjgl] = glfw;
        TO_LWJGL[glfw] = lwjgl;
    }

    /** GLFW's code for an LWJGL key, or -1 (GLFW's unknown key). */
    public static int toGlfw(int lwjgl) {
        return lwjgl >= 0 && lwjgl < TO_GLFW.length ? TO_GLFW[lwjgl] : -1;
    }

    /** LWJGL's code for a GLFW key, or 0 (LWJGL's none). Codes below zero (1.12's mouse buttons) pass through. */
    public static int toLwjgl(int glfw) {
        if (glfw < 0) return glfw;
        return glfw < TO_LWJGL.length ? TO_LWJGL[glfw] : Keyboard.KEY_NONE;
    }

    /** GLFW's modifier bits (shift 1, control 2, alt 4, super 8) for the keys held now. */
    public static int modifiers() {
        int mods = 0;
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) mods |= 1;
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) mods |= 2;
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) mods |= 4;
        if (Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA)) mods |= 8;
        return mods;
    }

    /** 1.13's {@code InputMappings.isKeyDown(window, key)}: by GLFW code. */
    public static boolean isKeyDown(int glfw) {
        int lwjgl = toLwjgl(glfw);
        return lwjgl != Keyboard.KEY_NONE && Keyboard.isKeyDown(lwjgl);
    }

    private ModernKeys() {}
}
