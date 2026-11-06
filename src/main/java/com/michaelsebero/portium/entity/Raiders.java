package com.michaelsebero.portium.entity;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.datapack.DataPackManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.util.ResourceLocation;

/** 1.14's raiders: the {@code minecraft:raiders} entity type tag, as data packs define it. */
public final class Raiders {

    public static final ResourceLocation TAG = new ResourceLocation("raiders");
    private static final ResourceLocation PILLAGER = new ResourceLocation(Tags.MOD_ID, "pillager");
    private static final ResourceLocation RAVAGER = new ResourceLocation(Tags.MOD_ID, "ravager");

    public static boolean isRaider(Entity entity) {
        DataPackManager packs = DataPackManager.get();
        if (packs != null && packs.getTags().getEntityTypes().get(TAG) != null) return DataPackManager.entityTypeTagContains(TAG, entity);
        // Without data packs (a client of a server), the tag as 1.16.5 ships it: illagers (evokers, vindicators,
        // illusioners), pillagers, ravagers and witches.
        if (entity instanceof AbstractIllager || entity instanceof EntityWitch) return true;
        ResourceLocation id = EntityList.getKey(entity);
        return PILLAGER.equals(id) || RAVAGER.equals(id);
    }

    private Raiders() {}
}
