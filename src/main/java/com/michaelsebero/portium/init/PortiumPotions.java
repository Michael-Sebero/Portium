package com.michaelsebero.portium.init;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.potion.PotionPortium;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Mob effects and brewable potion types from 1.13. Brewing recipes are in {@link PortiumRecipes}. An effect that
 * {@link PortiumConfig#effects} has off is never registered: nothing may give it ({@link #isRegistered}), and the
 * potion types made from it stay null.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PortiumPotions {

    public static final Potion CONDUIT_POWER = new PotionPortium("conduit_power", false, 0x1DC2D1);
    public static final Potion DOLPHINS_GRACE = new PotionPortium("dolphins_grace", false, 0x88A3BE);
    public static final Potion SLOW_FALLING = new PotionPortium("slow_falling", false, 0xFFEFD1);

    public static PotionType SLOW_FALLING_TYPE;
    public static PotionType LONG_SLOW_FALLING_TYPE;
    public static PotionType TURTLE_MASTER_TYPE;
    public static PotionType LONG_TURTLE_MASTER_TYPE;
    public static PotionType STRONG_TURTLE_MASTER_TYPE;

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        IForgeRegistry<Potion> registry = event.getRegistry();
        PortiumConfig.Effects enabled = PortiumConfig.effects;
        if (enabled.conduitPower) registry.register(CONDUIT_POWER.setRegistryName(new ResourceLocation(Tags.MOD_ID, "conduit_power")));
        if (enabled.dolphinsGrace) registry.register(DOLPHINS_GRACE.setRegistryName(new ResourceLocation(Tags.MOD_ID, "dolphins_grace")));
        if (enabled.slowFalling) registry.register(SLOW_FALLING.setRegistryName(new ResourceLocation(Tags.MOD_ID, "slow_falling")));
    }

    @SubscribeEvent
    public static void registerPotionTypes(RegistryEvent.Register<PotionType> event) {
        IForgeRegistry<PotionType> registry = event.getRegistry();
        if (isRegistered(SLOW_FALLING)) {
            SLOW_FALLING_TYPE = register(registry, "slow_falling", "slow_falling", new PotionEffect(SLOW_FALLING, 1800));
            LONG_SLOW_FALLING_TYPE = register(registry, "long_slow_falling", "slow_falling", new PotionEffect(SLOW_FALLING, 4800));
        }
        if (!PortiumConfig.effects.turtleMaster) return;
        TURTLE_MASTER_TYPE = register(registry, "turtle_master", "turtle_master",
                new PotionEffect(MobEffects.SLOWNESS, 400, 3), new PotionEffect(MobEffects.RESISTANCE, 400, 2));
        LONG_TURTLE_MASTER_TYPE = register(registry, "long_turtle_master", "turtle_master",
                new PotionEffect(MobEffects.SLOWNESS, 800, 3), new PotionEffect(MobEffects.RESISTANCE, 800, 2));
        STRONG_TURTLE_MASTER_TYPE = register(registry, "strong_turtle_master", "turtle_master",
                new PotionEffect(MobEffects.SLOWNESS, 400, 5), new PotionEffect(MobEffects.RESISTANCE, 400, 3));
    }

    /** Whether an effect is registered, and so may be given to an entity. */
    public static boolean isRegistered(Potion potion) {
        return potion.getRegistryName() != null && ForgeRegistries.POTIONS.containsValue(potion);
    }

    /** @param baseName the shared translation name, so long and strong variants reuse the base potion's name. */
    private static PotionType register(IForgeRegistry<PotionType> registry, String name, String baseName, PotionEffect... effects) {
        PotionType type = new PotionType(Tags.MOD_ID + "." + baseName, effects);
        type.setRegistryName(new ResourceLocation(Tags.MOD_ID, name));
        registry.register(type);
        return type;
    }

    private PortiumPotions() {}
}
