package com.michaelsebero.portium.modern.host;

import com.michaelsebero.portium.modern.shim.ItemShims;
import net.minecraft.item.Item;
import net.minecraftforge.common.ToolType;

/** Forge 1.13's item property additions that take Forge 1.13's classes. The translator points the calls here. */
public final class ItemGlue {

    /** {@code properties.addToolType(tool, level)}: 1.12 knows tools by name. */
    public static Item.Properties addToolType(Item.Properties properties, ToolType tool, int level) {
        return (Item.Properties) ItemShims.addToolType(properties, tool.getName(), level);
    }

    private ItemGlue() {}
}
