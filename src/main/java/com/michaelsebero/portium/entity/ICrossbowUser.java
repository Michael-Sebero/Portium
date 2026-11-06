package com.michaelsebero.portium.entity;

import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.item.ItemCrossbow;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

/** A mob that uses a crossbow (1.14's {@code ICrossbowUser}): pillagers, piglins. */
public interface ICrossbowUser {

    void setCharging(boolean charging);

    @Nullable
    EntityLivingBase getCrossbowTarget();

    /** After firing (pillagers reset their attack timer). */
    void onCrossbowAttackPerformed();

    /** Aims what was loaded at the target. */
    void shootCrossbowProjectile(@Nullable EntityLivingBase target, ItemStack crossbow, Entity projectile, float angle);

    /** Fires the loaded crossbow in whichever hand holds it (inaccuracy by difficulty: 10, 6 or 2). */
    static void performCrossbowAttack(EntityLivingBase user, float velocity) {
        EnumHand hand = user.getHeldItemMainhand().getItem() instanceof ItemCrossbow ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
        ItemStack stack = user.getHeldItem(hand);
        if (stack.getItem() instanceof ItemCrossbow) {
            ItemCrossbow.fireProjectiles(user.world, user, hand, stack, velocity, (float) (14 - user.world.getDifficulty().getId() * 4));
        }
        if (user instanceof ICrossbowUser) ((ICrossbowUser) user).onCrossbowAttackPerformed();
    }

    /** 1.14's aim: at a third of the target's height, raised by a fifth of the distance, turned by {@code angle}. */
    static void shootAt(EntityLivingBase user, EntityLivingBase target, Entity projectile, float angle, float velocity) {
        double dx = target.posX - user.posX;
        double dz = target.posZ - user.posZ;
        double distance = MathHelper.sqrt(dx * dx + dz * dz);
        double dy = target.posY + target.height * 0.3333333333333333D - projectile.posY + distance * 0.2F;
        Vec3d direction = aim(user, new Vec3d(dx, dy, dz), angle);
        ItemCrossbow.shoot(projectile, direction.x, direction.y, direction.z, velocity, (float) (14 - user.world.getDifficulty().getId() * 4));
        user.playSound(PortiumSounds.ITEM_CROSSBOW_SHOOT, 1.0F, 1.0F / (user.getRNG().nextFloat() * 0.4F + 0.8F));
    }

    /** The direction turned {@code angle} degrees about the perpendicular pointing up from it. */
    static Vec3d aim(EntityLivingBase user, Vec3d direction, float angle) {
        Vec3d forward = direction.normalize();
        Vec3d side = forward.crossProduct(new Vec3d(0.0D, 1.0D, 0.0D));
        if (side.lengthSquared() <= 1.0E-7D) side = forward.crossProduct(ItemCrossbow.CrossbowAim.upVector(user));
        Vec3d up = ItemCrossbow.CrossbowAim.rotate(forward, side, 90.0F);
        return ItemCrossbow.CrossbowAim.rotate(forward, up, angle);
    }
}
