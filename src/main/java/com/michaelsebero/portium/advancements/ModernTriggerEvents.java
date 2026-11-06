package com.michaelsebero.portium.advancements;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.ICrossbowArrow;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** The 1.14+ advancement triggers that follow Forge's events: items used on blocks, mobs interacted with, crossbow kills. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModernTriggerEvents {

    /** Who each crossbow arrow has killed so far (a piercing arrow can kill several). */
    private static final Map<EntityArrow, List<Entity>> KILLS = new WeakHashMap<>();

    /** As 1.14: after the use, so a block's new state counts (a respawn anchor's fourth charge). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || !(event.getEntityPlayer() instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        ItemStack used = event.getItemStack().copy();
        BlockPos pos = event.getPos();
        player.getServer().addScheduledTask(() -> ModernTriggers116.ITEM_USED_ON_BLOCK.trigger(player, used, null, pos, null, 0.0F, null));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || !(event.getEntityPlayer() instanceof EntityPlayerMP)) return;
        ModernTriggers116.INTERACTED_WITH_ENTITY.trigger((EntityPlayerMP) event.getEntityPlayer(), event.getItemStack().copy(), event.getTarget(), null,
                null, 0.0F, null);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        Entity source = event.getSource().getImmediateSource();
        if (!(source instanceof EntityArrow) || !(source instanceof ICrossbowArrow) || !((ICrossbowArrow) source).portium$isShotFromCrossbow()) return;
        Entity shooter = ((EntityArrow) source).shootingEntity;
        if (!(shooter instanceof EntityPlayerMP)) return;
        List<Entity> killed = KILLS.computeIfAbsent((EntityArrow) source, key -> new ArrayList<>());
        killed.add(event.getEntityLiving());
        ModernTriggers116.KILLED_BY_CROSSBOW.trigger((EntityPlayerMP) shooter, new ArrayList<>(killed));
    }

    private ModernTriggerEvents() {}
}
