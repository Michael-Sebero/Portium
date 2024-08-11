package com.michaelsebero.portium.advancements;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.michaelsebero.portium.Tags;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collection;
import java.util.Collections;

/**
 * {@code minecraft:fishing_rod_hooked} (1.13): the player reeled in a hooked entity or a catch. Checks the rod, the
 * hooked entity and, if given, an item among the catch (or the hooked item entity).
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class FishingRodHookedTrigger extends PortiumTrigger<FishingRodHookedTrigger.Instance> {

    public FishingRodHookedTrigger() {
        super(new ResourceLocation("fishing_rod_hooked"));
    }

    @Override
    public Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
        return new Instance(this.getId(), ItemPredicate.deserialize(json.get("rod")), EntityPredicate.deserialize(json.get("entity")),
                ItemPredicate.deserialize(json.get("item")));
    }

    public void trigger(EntityPlayerMP player, EntityFishHook hook, Collection<ItemStack> caught) {
        ItemStack rod = rod(player);
        this.trigger(player, instance -> instance.test(player, rod, hook, caught));
    }

    /** Reeling in hooked entities is triggered from the hook; catches from Forge's event, before they fly out. */
    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        EntityPlayer angler = event.getHookEntity().getAngler();
        if (angler instanceof EntityPlayerMP) PortiumTriggers.FISHING_ROD_HOOKED.trigger((EntityPlayerMP) angler, event.getHookEntity(), event.getDrops());
    }

    public static void onEntityReeled(EntityFishHook hook) {
        if (hook.getAngler() instanceof EntityPlayerMP) PortiumTriggers.FISHING_ROD_HOOKED.trigger((EntityPlayerMP) hook.getAngler(), hook, Collections.emptyList());
    }

    private static ItemStack rod(EntityPlayer player) {
        ItemStack main = player.getHeldItemMainhand();
        return main.getItem() == Items.FISHING_ROD ? main : player.getHeldItemOffhand();
    }

    public static class Instance extends AbstractCriterionInstance {

        private final ItemPredicate rod;
        private final EntityPredicate entity;
        private final ItemPredicate item;

        Instance(ResourceLocation id, ItemPredicate rod, EntityPredicate entity, ItemPredicate item) {
            super(id);
            this.rod = rod;
            this.entity = entity;
            this.item = item;
        }

        boolean test(EntityPlayerMP player, ItemStack rodStack, EntityFishHook hook, Collection<ItemStack> caught) {
            if (!this.rod.test(rodStack) || !this.entity.test(player, hook.caughtEntity)) return false;
            if (this.item == ItemPredicate.ANY) return true;
            if (hook.caughtEntity instanceof EntityItem && this.item.test(((EntityItem) hook.caughtEntity).getItem())) return true;
            for (ItemStack stack : caught) {
                if (this.item.test(stack)) return true;
            }
            return false;
        }
    }
}
