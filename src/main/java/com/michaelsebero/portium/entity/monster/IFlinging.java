package com.michaelsebero.portium.entity.monster;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;

/** 1.16's {@code IFlinging}: hoglins and zoglins toss what they hit into the air. */
public interface IFlinging {

    /** Ticks left of the head-swing attack animation. */
    int getAttackAnimationTick();

    /** 1.16's attack knockback attribute (1.12 has none): hoglins and zoglins have 1. */
    double ATTACK_KNOCKBACK = 1.0D;

    /** Hits for half to one and a half times its attack damage (adults), then flings the target. */
    static boolean hurtAndThrowTarget(EntityLivingBase attacker, EntityLivingBase target) {
        float attack = (float) attacker.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        float damage = !attacker.isChild() && (int) attack > 0 ? attack / 2.0F + attacker.world.rand.nextInt((int) attack) : attack;
        boolean hurt = target.attackEntityFrom(DamageSource.causeMobDamage(attacker), damage);
        if (hurt) {
            EnchantmentHelper.applyThornEnchantments(target, attacker);
            EnchantmentHelper.applyArthropodEnchantments(attacker, target);
            if (!attacker.isChild()) throwTarget(attacker, target);
        }
        return hurt;
    }

    static void throwTarget(EntityLivingBase attacker, EntityLivingBase target) {
        double strength = ATTACK_KNOCKBACK - target.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue();
        if (strength <= 0.0D) return;
        double dx = target.posX - attacker.posX;
        double dz = target.posZ - attacker.posZ;
        float yaw = (float) (attacker.world.rand.nextInt(21) - 10);
        double horizontal = strength * (attacker.world.rand.nextFloat() * 0.5F + 0.2F);
        Vec3d push = new Vec3d(dx, 0.0D, dz).normalize().scale(horizontal).rotateYaw(yaw);
        double up = strength * attacker.world.rand.nextFloat() * 0.5D;
        target.addVelocity(push.x, up, push.z);
        target.velocityChanged = true;
    }
}
