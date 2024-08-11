package com.michaelsebero.portium.modern.mc.entity.ai.attributes;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.passive.AbstractHorse;

/**
 * 1.16's {@code net.minecraft.entity.ai.attributes.Attributes}: the shared attributes, which are 1.12's own where 1.12
 * has them, so 1.16 code's modifiers act on 1.12's entities. 1.16's attack knockback is new; zombie reinforcements
 * are 1.12's own attribute of that name in zombies, which 1.12 keeps out of reach.
 */
public final class Attributes {

    @Srg("field_233818_a_")
    public static final IAttribute MAX_HEALTH = SharedMonsterAttributes.MAX_HEALTH;
    @Srg("field_233819_b_")
    public static final IAttribute FOLLOW_RANGE = SharedMonsterAttributes.FOLLOW_RANGE;
    @Srg("field_233820_c_")
    public static final IAttribute KNOCKBACK_RESISTANCE = SharedMonsterAttributes.KNOCKBACK_RESISTANCE;
    @Srg("field_233821_d_")
    public static final IAttribute MOVEMENT_SPEED = SharedMonsterAttributes.MOVEMENT_SPEED;
    @Srg("field_233822_e_")
    public static final IAttribute FLYING_SPEED = SharedMonsterAttributes.FLYING_SPEED;
    @Srg("field_233823_f_")
    public static final IAttribute ATTACK_DAMAGE = SharedMonsterAttributes.ATTACK_DAMAGE;
    @Srg("field_233824_g_")
    public static final IAttribute ATTACK_KNOCKBACK = new RangedAttribute(null, "generic.attackKnockback", 0.0D, 0.0D, 5.0D);
    @Srg("field_233825_h_")
    public static final IAttribute ATTACK_SPEED = SharedMonsterAttributes.ATTACK_SPEED;
    @Srg("field_233826_i_")
    public static final IAttribute ARMOR = SharedMonsterAttributes.ARMOR;
    @Srg("field_233827_j_")
    public static final IAttribute ARMOR_TOUGHNESS = SharedMonsterAttributes.ARMOR_TOUGHNESS;
    @Srg("field_233828_k_")
    public static final IAttribute LUCK = SharedMonsterAttributes.LUCK;
    @Srg("field_233829_l_")
    public static final IAttribute ZOMBIE_SPAWN_REINFORCEMENTS = hidden(net.minecraft.entity.monster.EntityZombie.class, "SPAWN_REINFORCEMENTS_CHANCE",
            "field_110186_bp", "zombie.spawnReinforcements", 0.0D, 1.0D);
    @Srg("field_233830_m_")
    public static final IAttribute HORSE_JUMP_STRENGTH = hidden(AbstractHorse.class, "JUMP_STRENGTH", "field_110271_bv", "horse.jumpStrength", 0.7D, 2.0D);

    /** One of 1.12's attributes it keeps protected in its entity class (by MCP name, else SRG name), or a like one. */
    private static IAttribute hidden(Class<?> owner, String name, String srgName, String fallback, double value, double max) {
        for (String field : new String[] {name, srgName}) {
            try {
                java.lang.reflect.Field found = owner.getDeclaredField(field);
                found.setAccessible(true);
                return (IAttribute) found.get(null);
            } catch (ReflectiveOperationException e) {
                // Try the other name.
            }
        }
        return new RangedAttribute(null, fallback, value, 0.0D, max);
    }

    private Attributes() {}
}
