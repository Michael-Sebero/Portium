package com.michaelsebero.portium.modern.mc.entity.ai.attributes;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.entity.EntityType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.16's {@code GlobalEntityTypeAttributes}: the attributes of each living entity type. Only newer mods' types are
 * kept here (Forge's {@code put} and its attribute creation event fill it); 1.12's own mobs register theirs
 * themselves.
 */
public final class GlobalEntityTypeAttributes {

    private static final Map<EntityType<?>, AttributeModifierMap> BY_TYPE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, AttributeModifierMap> BY_CLASS = new ConcurrentHashMap<>();

    /** Forge 1.16's {@code GlobalEntityTypeAttributes.put(type, map)}. */
    @Nullable
    public static AttributeModifierMap put(EntityType<?> type, AttributeModifierMap map) {
        BY_CLASS.clear();
        return BY_TYPE.put(type, map);
    }

    /** For the glue, which has the two as objects. */
    public static void portium$put(Object type, Object map) {
        put((EntityType<?>) type, (AttributeModifierMap) map);
    }

    @Srg("func_233835_a_")
    public static AttributeModifierMap getSupplier(EntityType<?> type) {
        return BY_TYPE.get(type);
    }

    @Srg("func_233837_b_")
    public static boolean hasSupplier(EntityType<?> type) {
        return BY_TYPE.containsKey(type);
    }

    @Nullable
    private static AttributeModifierMap of(EntityLivingBase entity) {
        if (BY_TYPE.isEmpty()) return null;
        Class<?> entityClass = entity.getClass();
        AttributeModifierMap known = BY_CLASS.get(entityClass);
        if (known != null) return known;
        // The type whose factory is making this entity, or the one known for its class (never made here to find out).
        EntityType<?> type = EntityType.making();
        if (type == null || type.knownEntityClass() != null && type.knownEntityClass() != entityClass) type = com.michaelsebero.portium.mods116.ModernEntityTypes.typeOf(entity);
        AttributeModifierMap map = type == null ? null : BY_TYPE.get(type);
        if (map == null) {
            for (Map.Entry<EntityType<?>, AttributeModifierMap> entry : BY_TYPE.entrySet()) {
                if (entry.getKey().knownEntityClass() == entityClass) map = entry.getValue();
            }
        }
        if (map != null) BY_CLASS.put(entityClass, map);
        return map;
    }

    /**
     * A living entity has just registered 1.12's attributes: its 1.16 type's are added to them, with their base
     * values, before its health is set from them.
     */
    public static void apply(EntityLivingBase entity) {
        AttributeModifierMap map = of(entity);
        if (map == null) return;
        for (Map.Entry<IAttribute, Double> entry : map.portium$bases().entrySet()) {
            IAttributeInstance instance = entity.getAttributeMap().getAttributeInstance(entry.getKey());
            if (instance == null) instance = entity.getAttributeMap().registerAttribute(entry.getKey());
            instance.setBaseValue(entry.getValue());
        }
    }

    private GlobalEntityTypeAttributes() {}
}
