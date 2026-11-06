package com.michaelsebero.portium.entity.raid;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.init.PortiumPotions;
import com.michaelsebero.portium.world.PortiumGameRules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** 1.14's {@code AbstractRaiderEntity.onDeath}: the player who kills a patrol's banner-carrying leader gets Bad Omen. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class RaiderEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntityLiving() instanceof IRaider) || !(event.getEntityLiving().world instanceof WorldServer)) return;
        EntityLiving raider = (EntityLiving) event.getEntityLiving();
        if (!IRaider.state(raider).isLeader() || Raiders116.hasRaid(raider) || !PortiumPotions.isRegistered(PortiumPotions.BAD_OMEN)) return;
        EntityPlayer player = killer(event.getSource().getTrueSource());
        if (player == null || !OminousBanner.is(raider.getItemStackFromSlot(EntityEquipmentSlot.HEAD))) return;
        PotionEffect current = player.getActivePotionEffect(PortiumPotions.BAD_OMEN);
        int amplifier = 0;
        if (current != null) {
            amplifier = 1 + current.getAmplifier();
            player.removeActivePotionEffect(PortiumPotions.BAD_OMEN);
        }
        amplifier = MathHelper.clamp(amplifier, 0, 4);
        if (!raider.world.getGameRules().getBoolean(PortiumGameRules.DISABLE_RAIDS)) {
            player.addPotionEffect(new PotionEffect(PortiumPotions.BAD_OMEN, 120000, amplifier, false, false));
        }
    }

    /** The player behind a kill: the killer itself, or a tamed wolf's owner. */
    private static EntityPlayer killer(Entity source) {
        if (source instanceof EntityPlayer) return (EntityPlayer) source;
        if (source instanceof EntityWolf && ((EntityWolf) source).isTamed()) {
            EntityLivingBase owner = ((EntityWolf) source).getOwner();
            if (owner instanceof EntityPlayer) return (EntityPlayer) owner;
        }
        return null;
    }

    private RaiderEvents() {}
}
