package com.michaelsebero.portium.modern.mc.entity.ai.attributes;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.16's {@code net.minecraft.entity.ai.attributes.AttributeModifierMap}: the attributes a kind of mob has and their
 * base values, which 1.16 keeps per entity type. 1.12's mobs register theirs as they're made; a 1.16 mob's are added
 * to those then ({@link GlobalEntityTypeAttributes#apply}).
 */
public class AttributeModifierMap {

    private final Map<IAttribute, Double> bases;

    public AttributeModifierMap(Map<IAttribute, Double> bases) {
        this.bases = bases;
    }

    public Map<IAttribute, Double> portium$bases() {
        return Collections.unmodifiableMap(this.bases);
    }

    @Srg("func_233803_a_")
    public static MutableAttribute builder() {
        return new MutableAttribute();
    }

    @Srg("func_233804_a_")
    public double getValue(IAttribute attribute) {
        return getBaseValue(attribute);
    }

    @Srg("func_233807_b_")
    public double getBaseValue(IAttribute attribute) {
        Double base = this.bases.get(attribute);
        if (base == null) throw new IllegalArgumentException("Can't find attribute " + attribute.getName());
        return base;
    }

    @Srg("func_233809_c_")
    public boolean hasAttribute(IAttribute attribute) {
        return this.bases.containsKey(attribute);
    }

    /** 1.16's {@code LivingEntity.createLivingAttributes()}. */
    public static MutableAttribute living() {
        return builder().add(Attributes.MAX_HEALTH).add(Attributes.KNOCKBACK_RESISTANCE).add(Attributes.MOVEMENT_SPEED).add(Attributes.ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS);
    }

    /** 1.16's {@code MobEntity.createMobAttributes()}. */
    public static MutableAttribute mob() {
        return living().add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.ATTACK_KNOCKBACK);
    }

    /** 1.16's {@code MonsterEntity.createMonsterAttributes()}. */
    public static MutableAttribute monster() {
        return mob().add(SharedMonsterAttributes.ATTACK_DAMAGE);
    }

    /** 1.16's {@code AttributeModifierMap.MutableAttribute}: the builder. */
    public static class MutableAttribute {

        private final Map<IAttribute, Double> bases = new LinkedHashMap<>();

        @Srg("func_233814_a_")
        public MutableAttribute add(IAttribute attribute) {
            this.bases.put(attribute, attribute.getDefaultValue());
            return this;
        }

        @Srg("func_233815_a_")
        public MutableAttribute add(IAttribute attribute, double base) {
            this.bases.put(attribute, base);
            return this;
        }

        @Srg("func_233813_a_")
        public AttributeModifierMap build() {
            return new AttributeModifierMap(new LinkedHashMap<>(this.bases));
        }
    }
}
