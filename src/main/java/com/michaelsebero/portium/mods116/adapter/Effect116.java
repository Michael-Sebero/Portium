package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.potion.Potion;

/**
 * What a 1.16 mod's potion effects extend in place of 1.16's {@code Effect}: 1.12's potion, made from 1.16's effect
 * kind (beneficial, harmful, neutral: 1.16's own enum, taken as an object) and colour, with 1.16's attribute modifier
 * builder onto 1.12's.
 */
public class Effect116 extends Potion {

    private final Object type;

    public Effect116(Object type, int color) {
        super(type instanceof Enum && "HARMFUL".equals(((Enum<?>) type).name()), color);
        this.type = type;
        if (type instanceof Enum && "BENEFICIAL".equals(((Enum<?>) type).name())) setBeneficial();
    }

    /** 1.16's: beneficial, harmful or neutral. */
    @Srg("func_220303_e")
    public Object getEffectType() {
        return this.type;
    }

    /** 1.16's {@code addAttributesModifier(attribute, uuid, amount, operation)}: 1.12's by the operation's index. */
    @Srg("func_220304_a")
    public Effect116 addAttributesModifier(IAttribute attribute, String uuid, double amount, Object operation) {
        registerPotionAttributeModifier(attribute, uuid, amount, operation instanceof Enum ? ((Enum<?>) operation).ordinal() : 0);
        return this;
    }
}
