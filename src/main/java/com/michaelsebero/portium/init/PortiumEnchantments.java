package com.michaelsebero.portium.init;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.enchantment.EnchantmentPortium;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The trident's and crossbow's enchantments, and Soul Speed. One that {@link PortiumConfig#enchantments} has off is never registered, so no item can
 * carry it and {@code EnchantmentHelper} always finds it at level 0.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PortiumEnchantments {

    public static final Enchantment IMPALING = EnchantmentPortium.impaling();
    public static final Enchantment LOYALTY = EnchantmentPortium.loyalty();
    public static final Enchantment RIPTIDE = EnchantmentPortium.riptide();
    public static final Enchantment CHANNELING = EnchantmentPortium.channeling();
    public static final Enchantment MULTISHOT = EnchantmentPortium.multishot();
    public static final Enchantment QUICK_CHARGE = EnchantmentPortium.quickCharge();
    public static final Enchantment PIERCING = EnchantmentPortium.piercing();
    public static final Enchantment SOUL_SPEED = new com.michaelsebero.portium.enchantment.EnchantmentSoulSpeed();

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        PortiumConfig.Enchantments enabled = PortiumConfig.enchantments;
        if (enabled.impaling) event.getRegistry().register(IMPALING.setRegistryName(PortiumNames.of(event.getRegistry(), "impaling")));
        if (enabled.loyalty) event.getRegistry().register(LOYALTY.setRegistryName(PortiumNames.of(event.getRegistry(), "loyalty")));
        if (enabled.riptide) event.getRegistry().register(RIPTIDE.setRegistryName(PortiumNames.of(event.getRegistry(), "riptide")));
        if (enabled.channeling) event.getRegistry().register(CHANNELING.setRegistryName(PortiumNames.of(event.getRegistry(), "channeling")));
        if (PortiumConfig.items.crossbow) {
            event.getRegistry().register(MULTISHOT.setRegistryName(PortiumNames.of(event.getRegistry(), "multishot")));
            event.getRegistry().register(QUICK_CHARGE.setRegistryName(PortiumNames.of(event.getRegistry(), "quick_charge")));
            event.getRegistry().register(PIERCING.setRegistryName(PortiumNames.of(event.getRegistry(), "piercing")));
        }
        if (enabled.soulSpeed) event.getRegistry().register(SOUL_SPEED.setRegistryName(PortiumNames.of(event.getRegistry(), "soul_speed")));
    }

    private PortiumEnchantments() {}
}
