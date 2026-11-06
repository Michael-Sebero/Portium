package com.michaelsebero.portium.entity.ai.brain;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.EnumDifficulty;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/** 1.16's {@code EntityPredicate}: whether one entity may target another, by distance, sight, team and invisibility. */
public class TargetPredicate {

    public static final TargetPredicate DEFAULT = new TargetPredicate();

    private double distance = -1.0D;
    private boolean allowInvulnerable;
    private boolean friendlyFire;
    private boolean ignoresLineOfSight;
    private boolean skipAttackChecks;
    private boolean useVisibilityModifier = true;
    @Nullable
    private Predicate<EntityLivingBase> customPredicate;

    public TargetPredicate setDistance(double distance) {
        this.distance = distance;
        return this;
    }

    public TargetPredicate allowInvulnerable() {
        this.allowInvulnerable = true;
        return this;
    }

    public TargetPredicate allowFriendlyFire() {
        this.friendlyFire = true;
        return this;
    }

    public TargetPredicate setIgnoresLineOfSight() {
        this.ignoresLineOfSight = true;
        return this;
    }

    public TargetPredicate setSkipAttackChecks() {
        this.skipAttackChecks = true;
        return this;
    }

    public TargetPredicate setUseInvisibilityCheck() {
        this.useVisibilityModifier = false;
        return this;
    }

    public TargetPredicate setCustomPredicate(@Nullable Predicate<EntityLivingBase> predicate) {
        this.customPredicate = predicate;
        return this;
    }

    public boolean canTarget(@Nullable EntityLivingBase attacker, EntityLivingBase target) {
        if (attacker == target || isSpectator(target) || !target.isEntityAlive()) return false;
        if (!this.allowInvulnerable && isInvulnerable(target)) return false;
        if (this.customPredicate != null && !this.customPredicate.test(target)) return false;
        if (attacker != null) {
            if (!this.skipAttackChecks && attacker instanceof EntityLiving && !((EntityLiving) attacker).canAttackClass(target.getClass())) return false;
            if (!this.friendlyFire && attacker.isOnSameTeam(target)) return false;
            if (this.distance > 0.0D) {
                double visibility = this.useVisibilityModifier ? visibilityMultiplier(target, attacker) : 1.0D;
                double range = Math.max(this.distance * visibility, 2.0D);
                if (attacker.getDistanceSq(target.posX, target.posY, target.posZ) > range * range) return false;
            }
            if (!this.ignoresLineOfSight && attacker instanceof EntityLiving && !((EntityLiving) attacker).getEntitySenses().canSee(target)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSpectator(EntityLivingBase entity) {
        return entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator();
    }

    /** 1.16's {@code isInvulnerable}: creative players, and the invulnerable flag. */
    private static boolean isInvulnerable(EntityLivingBase entity) {
        return entity.getIsInvulnerable() || entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.disableDamage;
    }

    /** 1.16's {@code getVisibilityMultiplier}: sneaking, invisibility (less per armour piece worn), and mob heads. */
    public static double visibilityMultiplier(EntityLivingBase target, @Nullable EntityLivingBase viewer) {
        double multiplier = com.michaelsebero.portium.entity.raid.Raiders116.visibility(target);
        if (viewer != null) {
            net.minecraft.item.ItemStack head = target.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD);
            if (head.getItem() == net.minecraft.init.Items.SKULL) {
                int skull = head.getMetadata();
                if (viewer instanceof net.minecraft.entity.monster.AbstractSkeleton && skull == 0
                        || viewer instanceof net.minecraft.entity.monster.EntityZombie && skull == 2
                        || viewer instanceof net.minecraft.entity.monster.EntityCreeper && skull == 4) {
                    multiplier *= 0.5D;
                }
            }
        }
        return multiplier;
    }

    /** 1.16's {@code EntityPredicates.CAN_HOSTILE_AI_TARGET}. */
    public static boolean canHostileTarget(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return true;
        EntityPlayer player = (EntityPlayer) entity;
        return !player.isSpectator() && !player.isCreative() && entity.world.getDifficulty() != EnumDifficulty.PEACEFUL;
    }
}
