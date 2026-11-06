package com.michaelsebero.portium.entity;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;

/**
 * 1.14's {@code MobEntity.attackEntityAsMob}, which any mob with attack damage uses (1.12 gave it only to monsters):
 * the attack damage with the held item's enchantments, fire aspect, knockback, an axe disabling a raised shield.
 */
public final class MobAttack {

    public static boolean attack(EntityLiving mob, Entity target) {
        return attack(mob, target, 0.0F);
    }

    /** {@code knockback}: 1.14's attack knockback attribute, which 1.12 doesn't have (ravagers and hoglins set it). */
    public static boolean attack(EntityLiving mob, Entity target, float knockback) {
        float damage = (float) mob.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        if (target instanceof EntityLivingBase) {
            damage += EnchantmentHelper.getModifierForCreature(mob.getHeldItemMainhand(), ((EntityLivingBase) target).getCreatureAttribute());
            knockback += (float) EnchantmentHelper.getKnockbackModifier(mob);
        }
        int fire = EnchantmentHelper.getFireAspectModifier(mob);
        if (fire > 0) target.setFire(fire * 4);
        boolean hurt = target.attackEntityFrom(DamageSource.causeMobDamage(mob), damage);
        if (hurt) {
            if (knockback > 0.0F && target instanceof EntityLivingBase) {
                ((EntityLivingBase) target).knockBack(mob, knockback * 0.5F, MathHelper.sin(mob.rotationYaw * 0.017453292F),
                        -MathHelper.cos(mob.rotationYaw * 0.017453292F));
                mob.motionX *= 0.6D;
                mob.motionZ *= 0.6D;
            }
            if (target instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) target;
                ItemStack weapon = mob.getHeldItemMainhand();
                ItemStack shield = player.isHandActive() ? player.getActiveItemStack() : ItemStack.EMPTY;
                if (!weapon.isEmpty() && !shield.isEmpty() && weapon.getItem().canDisableShield(weapon, shield, player, mob)
                        && shield.getItem().isShield(shield, player)) {
                    float chance = 0.25F + (float) EnchantmentHelper.getEfficiencyModifier(mob) * 0.05F;
                    if (mob.getRNG().nextFloat() < chance) {
                        player.getCooldownTracker().setCooldown(shield.getItem(), 100);
                        mob.world.setEntityState(player, (byte) 30);
                    }
                }
            }
            if (target instanceof EntityLivingBase) EnchantmentHelper.applyThornEnchantments((EntityLivingBase) target, mob);
            EnchantmentHelper.applyArthropodEnchantments(mob, target);
            mob.setLastAttackedEntity(target);
        }
        return hurt;
    }

    private MobAttack() {}
}
