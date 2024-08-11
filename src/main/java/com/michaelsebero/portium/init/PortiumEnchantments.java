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
 * The trident's enchantments. One that {@link PortiumConfig#enchantments} has off is never registered, so no item can
 * carry it and {@code EnchantmentHelper} always finds it at level 0.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PortiumEnchantments {

    public static final Enchantment IMPALING = EnchantmentPortium.impaling();
    public static final Enchantment LOYALTY = EnchantmentPortium.loyalty();
    public static final Enchantment RIPTIDE = EnchantmentPortium.riptide();
    public static final Enchantment CHANNELING = EnchantmentPortium.channeling();

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        PortiumConfig.Enchantments enabled = PortiumConfig.enchantments;
        if (enabled.impaling) event.getRegistry().register(IMPALING.setRegistryName(new ResourceLocation(Tags.MOD_ID, "impaling")));
        if (enabled.loyalty) event.getRegistry().register(LOYALTY.setRegistryName(new ResourceLocation(Tags.MOD_ID, "loyalty")));
        if (enabled.riptide) event.getRegistry().register(RIPTIDE.setRegistryName(new ResourceLocation(Tags.MOD_ID, "riptide")));
        if (enabled.channeling) event.getRegistry().register(CHANNELING.setRegistryName(new ResourceLocation(Tags.MOD_ID, "channeling")));
    }

    private PortiumEnchantments() {}
}
