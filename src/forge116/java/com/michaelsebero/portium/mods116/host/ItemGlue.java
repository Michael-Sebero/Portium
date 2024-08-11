package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.modern.shim.ItemShims;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.Item;
import net.minecraftforge.common.ToolType;

/** Forge 1.16's item and block property additions that take its tool types. The translator points the calls here. */
public final class ItemGlue {

    /** {@code properties.addToolType(tool, level)}: 1.12 knows tools by name. */
    public static Item.Properties addToolType(Item.Properties properties, ToolType tool, int level) {
        return (Item.Properties) ItemShims.addToolType(properties, tool.getName(), level);
    }

    /** {@code properties.harvestTool(tool)}, by the tool's name. */
    public static AbstractBlock.Properties harvestTool(AbstractBlock.Properties properties, ToolType tool) {
        return (AbstractBlock.Properties) (Object) ((com.michaelsebero.portium.modern.mc.block.Block$Properties) (Object) properties).portium$harvestTool(tool.getName());
    }

    /** {@code properties.food(food)}: Portium's items eat by the food's values. */
    public static Item.Properties food(Item.Properties properties, net.minecraft.item.Food food) {
        java.util.List<Object> effects = new java.util.ArrayList<>();
        java.util.List<Float> chances = new java.util.ArrayList<>();
        for (com.mojang.datafixers.util.Pair<net.minecraft.potion.EffectInstance, Float> effect : food.func_221464_f()) {
            effects.add(effect.getFirst());
            chances.add(effect.getSecond());
        }
        com.michaelsebero.portium.modern.adapter.FoodValues values = new com.michaelsebero.portium.modern.adapter.FoodValues(food.func_221466_a(),
                food.func_221469_b(), food.func_221467_c(), food.func_221468_d(), food.func_221465_e(), effects, chances);
        return (Item.Properties) (Object) ((com.michaelsebero.portium.modern.mc.item.Item$Properties) (Object) properties).portium$food(values);
    }

    private ItemGlue() {}
}
