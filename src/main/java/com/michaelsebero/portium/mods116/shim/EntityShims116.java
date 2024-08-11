package com.michaelsebero.portium.mods116.shim;

import com.michaelsebero.portium.modern.mc.entity.EntityType;

import java.util.function.BiFunction;

/**
 * 1.16's entities on 1.12's: entity type building onto Portium's entity types (typed loosely for the glue, which
 * compiles against 1.16), and the position, motion and other getters 1.16 code calls, which read fields 1.12 lacks.
 */
public final class EntityShims116 {

    /** A builder of a 1.16 factory, which takes the type and the world. */
    @SuppressWarnings("rawtypes")
    public static Object builder(BiFunction factory, Object classification) {
        return EntityType.Builder.portium$create(factory, classification);
    }

    /**
     * The name of 1.16's pose for an entity (1.16's {@code Pose} is its own class here; the glue turns the name into
     * it): a player's from Portium's poses, else from 1.12's state.
     */
    public static String poseName(Object entity) {
        net.minecraft.entity.Entity on = (net.minecraft.entity.Entity) entity;
        if (on instanceof net.minecraft.entity.EntityLivingBase) {
            net.minecraft.entity.EntityLivingBase living = (net.minecraft.entity.EntityLivingBase) on;
            if (living.getHealth() <= 0.0F) return "DYING";
            if (living.isPlayerSleeping()) return "SLEEPING";
            if (living.isElytraFlying()) return "FALL_FLYING";
        }
        if (on instanceof net.minecraft.entity.player.EntityPlayer) {
            String pose = com.michaelsebero.portium.api.IPosedPlayer.of((net.minecraft.entity.player.EntityPlayer) on).portium$getPose().name();
            if ("SWIMMING".equals(pose) || "CROUCHING".equals(pose) || "STANDING".equals(pose)) return pose;
        }
        return on.isSneaking() ? "CROUCHING" : "STANDING";
    }

    private EntityShims116() {}

    /** Forge 1.16's reach distance attribute: Forge 1.12's, as the glue can't name 1.12's classes. */
    public static Object reachDistance() {
        return net.minecraft.entity.player.EntityPlayer.REACH_DISTANCE;
    }

    /** Forge 1.16's swim speed attribute: Forge 1.12's. */
    public static Object swimSpeed() {
        return net.minecraft.entity.EntityLivingBase.SWIM_SPEED;
    }

    // 1.16 keeps an entity's position and motion as vectors; 1.12 as fields. Its getters read fields 1.12 lacks.

    public static net.minecraft.util.math.Vec3d positionVec(net.minecraft.entity.Entity entity) {
        return new net.minecraft.util.math.Vec3d(entity.posX, entity.posY, entity.posZ);
    }

    /** The block the entity is in (1.12's own getPosition is half a block higher). */
    public static net.minecraft.util.math.BlockPos blockPos(net.minecraft.entity.Entity entity) {
        return new net.minecraft.util.math.BlockPos(entity.posX, entity.posY, entity.posZ);
    }

    public static double posX(net.minecraft.entity.Entity entity) {
        return entity.posX;
    }

    public static double posY(net.minecraft.entity.Entity entity) {
        return entity.posY;
    }

    public static double posZ(net.minecraft.entity.Entity entity) {
        return entity.posZ;
    }

    public static double posYEye(net.minecraft.entity.Entity entity) {
        return entity.posY + entity.getEyeHeight();
    }

    public static double posYHeight(net.minecraft.entity.Entity entity, double scale) {
        return entity.posY + entity.height * scale;
    }

    public static net.minecraft.util.math.Vec3d motion(net.minecraft.entity.Entity entity) {
        return new net.minecraft.util.math.Vec3d(entity.motionX, entity.motionY, entity.motionZ);
    }

    public static void setMotion(net.minecraft.entity.Entity entity, net.minecraft.util.math.Vec3d motion) {
        setMotion(entity, motion.x, motion.y, motion.z);
    }

    public static void setMotion(net.minecraft.entity.Entity entity, double x, double y, double z) {
        entity.motionX = x;
        entity.motionY = y;
        entity.motionZ = z;
    }

    public static void setRawPosition(net.minecraft.entity.Entity entity, double x, double y, double z) {
        entity.posX = x;
        entity.posY = y;
        entity.posZ = z;
    }

    public static void move(net.minecraft.entity.Entity entity, net.minecraft.entity.MoverType type, net.minecraft.util.math.Vec3d by) {
        entity.move(type, by.x, by.y, by.z);
    }

    public static boolean isSneaking(net.minecraft.entity.Entity entity) {
        return entity.isSneaking();
    }

    /** 1.16's isSuppressingBounce and isSteppingCarefully: sneaking, in 1.12's terms. */
    public static boolean sneakingCarefully(net.minecraft.entity.Entity entity) {
        return entity.isSneaking();
    }

    public static boolean isOnGround(net.minecraft.entity.Entity entity) {
        return entity.onGround;
    }

    public static void setOnGround(net.minecraft.entity.Entity entity, boolean onGround) {
        entity.onGround = onGround;
    }

    public static float width(net.minecraft.entity.Entity entity) {
        return entity.width;
    }

    public static float height(net.minecraft.entity.Entity entity) {
        return entity.height;
    }

    /** 1.16's static {@code horizontalMag}: a vector's squared length across. */
    public static double horizontalMag(net.minecraft.util.math.Vec3d vector) {
        return vector.x * vector.x + vector.z * vector.z;
    }

    public static double distanceSq(net.minecraft.entity.Entity entity, net.minecraft.util.math.Vec3d to) {
        return entity.getDistanceSq(to.x, to.y, to.z);
    }

    /** 1.16's onLivingFall: 1.12's fall; 1.16 reports whether the fall hurt, which 1.12 doesn't tell. */
    public static boolean onLivingFall(net.minecraft.entity.Entity entity, float distance, float damageMultiplier) {
        entity.fall(distance, damageMultiplier);
        return false;
    }

    public static boolean attemptTeleport(net.minecraft.entity.EntityLivingBase entity, double x, double y, double z, boolean particles) {
        return entity.attemptTeleport(x, y, z);
    }

    /** 1.16's sendBreakAnimation: the item in that hand breaking, shown. */
    public static void sendBreakAnimation(net.minecraft.entity.EntityLivingBase entity, net.minecraft.util.EnumHand hand) {
        entity.renderBrokenItemStack(entity.getHeldItem(hand));
    }
}
