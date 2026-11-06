package com.michaelsebero.portium.entity;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** 1.15's iron golem cracks: the golem cracks as it's hurt (with a sound at each stage) and an iron ingot mends 25 health. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class GolemCracks {

    public enum Cracks {
        NONE, LOW, MEDIUM, HIGH;

        public static Cracks of(float healthRatio) {
            if (healthRatio < 0.25F) return HIGH;
            if (healthRatio < 0.5F) return MEDIUM;
            if (healthRatio < 0.75F) return LOW;
            return NONE;
        }
    }

    public static Cracks of(EntityIronGolem golem) {
        return Cracks.of(golem.getHealth() / golem.getMaxHealth());
    }

    @SubscribeEvent
    public static void onRepair(PlayerInteractEvent.EntityInteract event) {
        if (!PortiumConfig.mechanics.golemCracks || !(event.getTarget() instanceof EntityIronGolem)) return;
        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = player.getHeldItem(event.getHand());
        if (stack.getItem() != Items.IRON_INGOT) return;
        EntityIronGolem golem = (EntityIronGolem) event.getTarget();
        float before = golem.getHealth();
        golem.heal(25.0F);
        if (golem.getHealth() == before) return;
        float pitch = 1.0F + (golem.getRNG().nextFloat() - golem.getRNG().nextFloat()) * 0.2F;
        golem.playSound(PortiumSounds.ENTITY_IRON_GOLEM_REPAIR, 1.0F, pitch);
        if (!player.capabilities.isCreativeMode) stack.shrink(1);
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!PortiumConfig.mechanics.golemCracks || !(event.getEntityLiving() instanceof EntityIronGolem)) return;
        EntityIronGolem golem = (EntityIronGolem) event.getEntityLiving();
        Cracks before = of(golem);
        Cracks after = Cracks.of(Math.max(0.0F, golem.getHealth() - event.getAmount()) / golem.getMaxHealth());
        if (after != before) golem.playSound(PortiumSounds.ENTITY_IRON_GOLEM_DAMAGE, 1.0F, 1.0F);
    }

    private GolemCracks() {}
}
