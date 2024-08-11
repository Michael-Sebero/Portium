package net.minecraftforge.common;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import com.michaelsebero.portium.mods116.shim.EntityShims116;

import java.lang.reflect.Method;

/**
 * Portium's replacement for Forge 1.16's own mod class, which registers Forge's content as a mod: its holders, for
 * mods that read them. Forge 1.12 is the running Forge, so there's no Forge 1.16 mod to construct. Raw types: the
 * Minecraft 1.16 this compiles against is unpatched, so its attributes and fluids aren't registry entries there.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ForgeMod {

    public static final String VERSION_CHECK_CAT = "version_checking";
    // Forge 1.16's own attributes: reach distance and swim speed are Forge 1.12's; 1.12 lacks the other two.
    public static final RegistryObject SWIM_SPEED = bound(EntityShims116.swimSpeed());
    public static final RegistryObject NAMETAG_DISTANCE = empty();
    public static final RegistryObject ENTITY_GRAVITY = empty();
    public static final RegistryObject REACH_DISTANCE = bound(EntityShims116.reachDistance());
    public static final RegistryObject MILK = RegistryObject.of(new ResourceLocation("milk"), ForgeRegistries.FLUIDS);
    public static final RegistryObject FLOWING_MILK = RegistryObject.of(new ResourceLocation("flowing_milk"), ForgeRegistries.FLUIDS);

    private static boolean milkFluid;
    private static ForgeMod instance;

    public static ForgeMod getInstance() {
        if (instance == null) instance = new ForgeMod();
        return instance;
    }

    /** Asks for Forge's milk fluid; not registered yet. */
    public static void enableMilkFluid() {
        milkFluid = true;
    }

    public String getModId() {
        return "forge";
    }

    /**
     * A holder of a value Portium already has, outside any registry. 1.12's attributes aren't registry entries, which
     * the holder's field is typed as; an interface type, so the JVM lets the value sit there, though reflection won't
     * put it there.
     */
    private static RegistryObject bound(Object value) {
        try {
            java.lang.reflect.Constructor<RegistryObject> made = RegistryObject.class.getDeclaredConstructor();
            made.setAccessible(true);
            RegistryObject holder = made.newInstance();
            java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            unsafe.putObject(holder, unsafe.objectFieldOffset(RegistryObject.class.getDeclaredField("value")), value);
            return holder;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T empty() {
        try {
            Method empty = RegistryObject.class.getDeclaredMethod("empty");
            empty.setAccessible(true);
            return (T) empty.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
