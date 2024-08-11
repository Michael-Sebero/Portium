package com.michaelsebero.portium.mixin.modern;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.modern.adapter.IItemPropertyHolder;
import com.michaelsebero.portium.modern.mc.item.Item$Properties;
import com.michaelsebero.portium.modern.mc.util.IItemProvider;
import com.michaelsebero.portium.modern.shim.ItemShims;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 1.13's item API on 1.12's items: items are item providers, and an item made by a 1.13 mod takes the properties it
 * was made with, rarity included.
 */
@Mixin(Item.class)
public abstract class MixinItem implements IItemProvider, IItemPropertyHolder {

    @Shadow
    protected int maxStackSize;

    /** A 1.13 item's rarity, before enchantment raises it; null for 1.12's items, which work theirs out. */
    @Unique
    @Nullable
    private EnumRarity portium$rarity;

    @Shadow
    public abstract Item setMaxDamage(int maxDamage);

    @Shadow
    public abstract Item setContainerItem(Item containerItem);

    @Shadow
    public abstract Item setCreativeTab(CreativeTabs tab);

    @Shadow(remap = false)
    public abstract Item setNoRepair();

    @Shadow(remap = false)
    public abstract void setHarvestLevel(String toolClass, int level);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void portium$applyModernProperties(CallbackInfo ci) {
        Item$Properties properties = ItemShims.takePending();
        if (properties != null) portium$applyItemProperties(properties);
    }

    @Override
    public void portium$applyItemProperties(Item$Properties properties) {
        this.maxStackSize = properties.getMaxStackSize();
        if (properties.getMaxDamage() > 0) setMaxDamage(properties.getMaxDamage());
        if (properties.getContainerItem() != null) setContainerItem(properties.getContainerItem());
        if (properties.getGroup() != null) setCreativeTab(properties.getGroup());
        this.portium$rarity = properties.getRarity();
        if (!properties.canRepair()) setNoRepair();
        for (Map.Entry<String, Integer> tool : properties.getToolLevels().entrySet()) setHarvestLevel(tool.getKey(), tool.getValue());
        if (properties.getRenderer() != null) Portium.proxy.setItemRenderer((Item) (Object) this, properties.getRenderer());
    }

    @Inject(method = "getRarity", at = @At("HEAD"), cancellable = true)
    private void portium$modernRarity(ItemStack stack, CallbackInfoReturnable<EnumRarity> cir) {
        EnumRarity rarity = this.portium$rarity;
        if (rarity == null) return;
        if (!stack.isItemEnchanted()) {
            cir.setReturnValue(rarity);
        } else if (rarity == EnumRarity.COMMON || rarity == EnumRarity.UNCOMMON) {
            cir.setReturnValue(EnumRarity.RARE);
        } else if (rarity == EnumRarity.RARE) {
            cir.setReturnValue(EnumRarity.EPIC);
        } else {
            cir.setReturnValue(rarity);
        }
    }

    @Override
    public Item asItem() {
        return (Item) (Object) this;
    }
}
