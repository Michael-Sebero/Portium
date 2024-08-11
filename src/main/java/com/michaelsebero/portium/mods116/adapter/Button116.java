package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * What a 1.16 mod's buttons are, made or extended: a {@link Widget116} that runs its press action when clicked. The
 * action (1.16's {@code Button.IPressable}, a class of Minecraft 1.16.5's own) is called by name.
 */
@SideOnly(Side.CLIENT)
public class Button116 extends Widget116 {

    @Srg("field_230697_t_")
    protected final Object onPress;

    /** 1.16's {@code AbstractButton}'s: no action of its own ({@link #onPress} is overridden). */
    public Button116(int x, int y, int width, int height, ITextComponent title) {
        this(x, y, width, height, title, null);
    }

    public Button116(int x, int y, int width, int height, ITextComponent title, Object onPress) {
        super(x, y, width, height, title);
        this.onPress = onPress;
    }

    public Button116(int x, int y, int width, int height, ITextComponent title, Object onPress, Object tooltip) {
        this(x, y, width, height, title, onPress);
    }

    @Override
    @Srg("func_230982_a_")
    public void onClick(double mouseX, double mouseY) {
        onPress();
    }

    @Srg("func_230930_b_")
    public void onPress() {
        if (this.onPress == null) return;
        try {
            for (java.lang.reflect.Method method : this.onPress.getClass().getMethods()) {
                if (method.getParameterCount() == 1 && !method.isDefault() && method.getParameterTypes()[0].isInstance(this)
                        && method.getDeclaringClass() != Object.class) {
                    method.setAccessible(true);
                    method.invoke(this.onPress, this);
                    return;
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't press " + getMessage(), e);
        }
    }
}
