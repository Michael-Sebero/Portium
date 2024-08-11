package com.michaelsebero.portium.mixin.item;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** The enchanted golden apple gets a name of its own, as in 1.13; 1.12 calls both apples "Golden Apple". */
@Mixin(ItemAppleGold.class)
public abstract class MixinItemAppleGold extends Item {

    @Override
    public String getTranslationKey(ItemStack stack) {
        return stack.getMetadata() > 0 && PortiumConfig.visuals.modernNames ? "item." + Tags.MOD_ID + ".enchanted_golden_apple" : this.getTranslationKey();
    }
}
