package com.michaelsebero.portium.entity.raid;

import com.michaelsebero.portium.init.PortiumPotions;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.PotionEffect;

import javax.annotation.Nullable;

/** 1.14's Hero of the Village effect: given for defending a village from a raid; villagers discount their trades. */
public final class HeroOfTheVillage {

    /** The effect on an entity, if it has it (and the effect is registered). */
    @Nullable
    public static PotionEffect effect(EntityLivingBase entity) {
        return PortiumPotions.HERO_OF_THE_VILLAGE.getRegistryName() == null ? null : entity.getActivePotionEffect(PortiumPotions.HERO_OF_THE_VILLAGE);
    }

    private HeroOfTheVillage() {}
}
