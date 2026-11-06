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

    /** Whether the entity's eyes are in water (or lava). */
    public static boolean eyesIn(net.minecraft.entity.Entity entity, boolean lava) {
        return entity.isInsideOfMaterial(lava ? net.minecraft.block.material.Material.LAVA : net.minecraft.block.material.Material.WATER);
    }

    /** 1.16's {@code projectile.getShooter()}: a 1.16 mod's projectile's own, or 1.12's projectiles' shooters. */
    @javax.annotation.Nullable
    public static net.minecraft.entity.Entity projectileOwner(net.minecraft.entity.Entity projectile) {
        if (projectile instanceof net.minecraft.entity.projectile.EntityArrow) return ((net.minecraft.entity.projectile.EntityArrow) projectile).shootingEntity;
        if (projectile instanceof net.minecraft.entity.projectile.EntityThrowable) return ((net.minecraft.entity.projectile.EntityThrowable) projectile).getThrower();
        if (projectile instanceof net.minecraft.entity.projectile.EntityFireball) return ((net.minecraft.entity.projectile.EntityFireball) projectile).shootingEntity;
        if (projectile instanceof net.minecraft.entity.projectile.EntityLlamaSpit) return ((net.minecraft.entity.projectile.EntityLlamaSpit) projectile).owner;
        java.lang.reflect.Method own = ownMethod(projectile.getClass(), "func_234616_v_", 0);
        try {
            return own == null ? null : (net.minecraft.entity.Entity) own.invoke(projectile);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /** 1.16's {@code projectile.setShooter(entity)}. */
    public static void setProjectileOwner(net.minecraft.entity.Entity projectile, @javax.annotation.Nullable net.minecraft.entity.Entity owner) {
        if (projectile instanceof net.minecraft.entity.projectile.EntityArrow) {
            ((net.minecraft.entity.projectile.EntityArrow) projectile).shootingEntity = owner;
            return;
        }
        if (projectile instanceof net.minecraft.entity.projectile.EntityFireball && owner instanceof net.minecraft.entity.EntityLivingBase) {
            ((net.minecraft.entity.projectile.EntityFireball) projectile).shootingEntity = (net.minecraft.entity.EntityLivingBase) owner;
            return;
        }
        java.lang.reflect.Method own = ownMethod(projectile.getClass(), "func_212361_a", 1);
        try {
            if (own != null) own.invoke(projectile, owner);
        } catch (ReflectiveOperationException e) {
            // A projectile without a shooter to set.
        }
    }

    @javax.annotation.Nullable
    private static java.lang.reflect.Method ownMethod(Class<?> type, String name, int parameters) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (java.lang.reflect.Method method : c.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameters) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        return null;
    }

    public static net.minecraft.network.datasync.DataSerializer<?> serializerOPTIONAL_UUID() {
        return com.michaelsebero.portium.network.ModernSerializers116.OPTIONAL_UUID;
    }

    public static net.minecraft.network.datasync.DataSerializer<?> serializerOPTIONAL_BLOCK_POS() {
        return com.michaelsebero.portium.network.ModernSerializers116.OPTIONAL_BLOCK_POS;
    }

    public static net.minecraft.network.datasync.DataSerializer<?> serializerOPTIONAL_BLOCK_STATE() {
        return com.michaelsebero.portium.network.ModernSerializers116.OPTIONAL_BLOCK_STATE;
    }

    public static net.minecraft.network.datasync.DataSerializer<?> serializerOPTIONAL_TEXT() {
        return com.michaelsebero.portium.network.ModernSerializers116.OPTIONAL_TEXT;
    }

    /** 1.13's {@code isInWaterOrBubbleColumn}: in water (a bubble column is water to 1.12). */
    public static boolean inWaterOrBubbleColumn(net.minecraft.entity.Entity entity) {
        return entity.isInWater();
    }

    /** 1.15's {@code getPositionUnderneath}: the block just below the entity's feet. */
    public static net.minecraft.util.math.BlockPos positionUnderneath(net.minecraft.entity.Entity entity) {
        return new net.minecraft.util.math.BlockPos(entity.posX, entity.getEntityBoundingBox().minY - 0.5000001D, entity.posZ);
    }

    private static java.lang.reflect.Method setSize;

    /** 1.12's protected {@code Entity.setSize}, by its name in dev or production. */
    private static java.lang.reflect.Method sizeSetter() {
        if (setSize == null) {
            setSize = net.minecraftforge.fml.common.ObfuscationReflectionHelper.findMethod(net.minecraft.entity.Entity.class, "func_70105_a", void.class,
                    float.class, float.class);
        }
        return setSize;
    }

    /** 1.16's {@code Entity(type, world)}: the size its type gives (1.12's constructor has no type; it's dropped). */
    public static void typeSize(net.minecraft.entity.Entity entity, Object type) {
        if (!(type instanceof com.michaelsebero.portium.modern.mc.entity.EntityType)) return;
        com.michaelsebero.portium.modern.mc.entity.EntityType<?> modern = (com.michaelsebero.portium.modern.mc.entity.EntityType<?>) type;
        float width = modern.portium$getWidth();
        float height = modern.portium$getHeight();
        if (width == entity.width && height == entity.height) return;
        try {
            sizeSetter().invoke(entity, width, height);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Left at 1.12's default size.
        }
    }

    /** 1.14's {@code recalculateSize}: the size its 1.16 {@code getSize(pose)} gives, if it has one. */
    public static void recalculateSize(net.minecraft.entity.Entity entity) {
        try {
            java.lang.reflect.Method pose = null;
            java.lang.reflect.Method size = null;
            for (Class<?> c = entity.getClass(); c != null && (pose == null || size == null); c = c.getSuperclass()) {
                for (java.lang.reflect.Method method : c.getDeclaredMethods()) {
                    if (size == null && "func_213305_a".equals(method.getName()) && method.getParameterCount() == 1) size = method;
                }
            }
            if (size == null) return;
            size.setAccessible(true);
            Class<?> poses = size.getParameterTypes()[0];
            Object standing = poses.isEnum() ? Enum.valueOf((Class) poses, poseName(entity)) : null;
            Object made = size.invoke(entity, standing);
            if (made == null) return;
            float width = made.getClass().getField("field_220315_a").getFloat(made);
            float height = made.getClass().getField("field_220316_b").getFloat(made);
            sizeSetter().invoke(entity, width, height);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Left at the size it has.
        }
    }

    public static boolean withinHome(net.minecraft.entity.EntityLiving mob, net.minecraft.util.math.BlockPos pos) {
        return !(mob instanceof net.minecraft.entity.EntityCreature) || ((net.minecraft.entity.EntityCreature) mob).isWithinHomeDistanceFromPosition(pos);
    }

    public static void setHome(net.minecraft.entity.EntityLiving mob, net.minecraft.util.math.BlockPos pos, int distance) {
        if (mob instanceof net.minecraft.entity.EntityCreature) ((net.minecraft.entity.EntityCreature) mob).setHomePosAndDistance(pos, distance);
    }

    public static net.minecraft.util.math.BlockPos home(net.minecraft.entity.EntityLiving mob) {
        return mob instanceof net.minecraft.entity.EntityCreature ? ((net.minecraft.entity.EntityCreature) mob).getHomePosition() : net.minecraft.util.math.BlockPos.ORIGIN;
    }

    public static float homeDistanceOf(net.minecraft.entity.EntityLiving mob) {
        return mob instanceof net.minecraft.entity.EntityCreature ? ((net.minecraft.entity.EntityCreature) mob).getMaximumHomeDistance() : -1.0F;
    }

    public static boolean hasHome(net.minecraft.entity.EntityLiving mob) {
        return mob instanceof net.minecraft.entity.EntityCreature && ((net.minecraft.entity.EntityCreature) mob).hasHome();
    }

    public static boolean wet(net.minecraft.entity.Entity entity) {
        return entity.isWet();
    }

    public static void setSitting(net.minecraft.entity.passive.EntityTameable tameable, boolean sitting) {
        tameable.setSitting(sitting);
    }

    public static boolean shouldBeDead(net.minecraft.entity.EntityLivingBase entity) {
        return entity.getHealth() <= 0.0F;
    }

    /** 1.16's limb animation from how far the entity moved (with its height change, if asked). */
    public static void updateLimbs(net.minecraft.entity.EntityLivingBase self, net.minecraft.entity.EntityLivingBase entity, boolean withY) {
        entity.prevLimbSwingAmount = entity.limbSwingAmount;
        double dx = entity.posX - entity.prevPosX;
        double dy = withY ? entity.posY - entity.prevPosY : 0.0D;
        double dz = entity.posZ - entity.prevPosZ;
        float speed = net.minecraft.util.math.MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 4.0F;
        if (speed > 1.0F) speed = 1.0F;
        entity.limbSwingAmount += (speed - entity.limbSwingAmount) * 0.4F;
        entity.limbSwing += entity.limbSwingAmount;
    }

    public static double randomX(net.minecraft.entity.Entity entity, double scale) {
        return entity.posX + entity.width * (2.0D * entity.world.rand.nextDouble() - 1.0D) * scale;
    }

    public static double randomZ(net.minecraft.entity.Entity entity, double scale) {
        return entity.posZ + entity.width * (2.0D * entity.world.rand.nextDouble() - 1.0D) * scale;
    }

    public static double randomY(net.minecraft.entity.Entity entity) {
        return entity.posY + entity.height * entity.world.rand.nextDouble();
    }

    public static net.minecraft.entity.EnumCreatureAttribute creatureAttributeUNDEFINED() {
        return net.minecraft.entity.EnumCreatureAttribute.UNDEFINED;
    }

    public static net.minecraft.entity.EnumCreatureAttribute creatureAttributeUNDEAD() {
        return net.minecraft.entity.EnumCreatureAttribute.UNDEAD;
    }

    public static net.minecraft.entity.EnumCreatureAttribute creatureAttributeARTHROPOD() {
        return net.minecraft.entity.EnumCreatureAttribute.ARTHROPOD;
    }

    public static net.minecraft.entity.EnumCreatureAttribute creatureAttributeILLAGER() {
        return net.minecraft.entity.EnumCreatureAttribute.ILLAGER;
    }

    /** 1.13's water creatures (impaling hurts them): 1.12 has no such kind; a named one if a mod added it, else none. */
    public static net.minecraft.entity.EnumCreatureAttribute creatureAttributeWATER() {
        for (net.minecraft.entity.EnumCreatureAttribute kind : net.minecraft.entity.EnumCreatureAttribute.values()) {
            if (kind.name().equals("WATER")) return kind;
        }
        return net.minecraft.entity.EnumCreatureAttribute.UNDEFINED;
    }

    /** 1.16's {@code entityType.isContained(tag)}: by the tag's name, in the data packs' entity type tags. */
    public static boolean typeInTag(com.michaelsebero.portium.modern.mc.entity.EntityType<?> type, Object tag) {
        try {
            net.minecraft.util.ResourceLocation name = (net.minecraft.util.ResourceLocation) tag.getClass().getMethod("func_230234_a_").invoke(tag);
            net.minecraft.util.ResourceLocation id = type.getRegistryName();
            return id != null && TagShims116.contains("entity_types", name, type);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** 1.16's {@code path.getTarget()}: where the path ends. */
    public static net.minecraft.util.math.BlockPos pathTarget(net.minecraft.pathfinding.Path path) {
        net.minecraft.pathfinding.PathPoint end = path.getFinalPathPoint();
        return end == null ? net.minecraft.util.math.BlockPos.ORIGIN : new net.minecraft.util.math.BlockPos(end.x, end.y, end.z);
    }

    // 1.16's path methods onto 1.12's path.

    @javax.annotation.Nullable
    public static net.minecraft.pathfinding.PathPoint pathCurrent(net.minecraft.pathfinding.Path path) {
        int index = path.getCurrentPathIndex();
        return index >= 0 && index < path.getCurrentPathLength() ? path.getPathPointFromIndex(index) : null;
    }

    @javax.annotation.Nullable
    public static net.minecraft.pathfinding.PathPoint pathPrevious(net.minecraft.pathfinding.Path path) {
        int index = path.getCurrentPathIndex();
        return index > 0 && index <= path.getCurrentPathLength() ? path.getPathPointFromIndex(index - 1) : null;
    }

    public static boolean pathNotStarted(net.minecraft.pathfinding.Path path) {
        return path.getCurrentPathIndex() <= 0;
    }

    public static net.minecraft.util.math.BlockPos pathPos(net.minecraft.pathfinding.Path path, int index) {
        net.minecraft.pathfinding.PathPoint point = path.getPathPointFromIndex(index);
        return new net.minecraft.util.math.BlockPos(point.x, point.y, point.z);
    }

    public static void pathSetLength(net.minecraft.pathfinding.Path path, int length) {
        path.setCurrentPathLength(length);
    }

    /** 1.16's {@code reachesTarget}: 1.12's paths end where they can; taken as reaching it. */
    public static boolean pathReaches(net.minecraft.pathfinding.Path path) {
        return true;
    }

    public static float pathDistance(net.minecraft.pathfinding.Path path) {
        return 0.0F;
    }

    /** 1.16's {@code vector.isWithinDistanceOf(position, distance)}. */
    public static boolean withinDistance(net.minecraft.util.math.Vec3d vector, Object position, double distance) {
        double x;
        double y;
        double z;
        if (position instanceof net.minecraft.util.math.Vec3d) {
            net.minecraft.util.math.Vec3d other = (net.minecraft.util.math.Vec3d) position;
            x = other.x;
            y = other.y;
            z = other.z;
        } else if (position instanceof net.minecraft.dispenser.IPosition) {
            net.minecraft.dispenser.IPosition other = (net.minecraft.dispenser.IPosition) position;
            x = other.getX();
            y = other.getY();
            z = other.getZ();
        } else {
            return false;
        }
        return vector.squareDistanceTo(x, y, z) < distance * distance;
    }

    public static boolean preventDespawn(net.minecraft.entity.EntityLiving mob) {
        return false;
    }

    @javax.annotation.Nullable
    private static java.lang.reflect.Field stuckNode;

    private static java.lang.reflect.Field stuckNodeField() {
        if (stuckNode == null) stuckNode = net.minecraftforge.fml.common.ObfuscationReflectionHelper.findField(net.minecraft.pathfinding.PathNavigate.class, "field_188557_k");
        return stuckNode;
    }

    public static net.minecraft.util.math.Vec3i stuckNode(net.minecraft.pathfinding.PathNavigate navigator) {
        try {
            net.minecraft.util.math.Vec3d node = (net.minecraft.util.math.Vec3d) stuckNodeField().get(navigator);
            return new net.minecraft.util.math.Vec3i(node.x, node.y, node.z);
        } catch (IllegalAccessException | RuntimeException e) {
            return net.minecraft.util.math.Vec3i.NULL_VECTOR;
        }
    }

    public static void setStuckNode(net.minecraft.pathfinding.PathNavigate navigator, net.minecraft.util.math.Vec3i node) {
        try {
            stuckNodeField().set(navigator, new net.minecraft.util.math.Vec3d(node.getX(), node.getY(), node.getZ()));
        } catch (IllegalAccessException | RuntimeException e) {
            // Left as it was.
        }
    }

    public static int maxAir(net.minecraft.entity.Entity entity) {
        return 300;
    }

    public static float homeDistance(net.minecraft.entity.EntityCreature creature) {
        return creature.getMaximumHomeDistance();
    }

    public static void setHomeDistance(net.minecraft.entity.EntityCreature creature, float distance) {
        creature.setHomePosAndDistance(creature.getHomePosition(), (int) distance);
    }

    // 1.16's attribute access, onto 1.12's.

    public static double attributeValue(net.minecraft.entity.EntityLivingBase entity, net.minecraft.entity.ai.attributes.IAttribute attribute) {
        net.minecraft.entity.ai.attributes.IAttributeInstance instance = entity.getEntityAttribute(attribute);
        return instance != null ? instance.getAttributeValue() : attribute.getDefaultValue();
    }

    public static double attributeBase(net.minecraft.entity.EntityLivingBase entity, net.minecraft.entity.ai.attributes.IAttribute attribute) {
        net.minecraft.entity.ai.attributes.IAttributeInstance instance = entity.getEntityAttribute(attribute);
        return instance != null ? instance.getBaseValue() : attribute.getDefaultValue();
    }

    public static net.minecraft.entity.ai.attributes.AbstractAttributeMap attributeMap(net.minecraft.entity.EntityLivingBase entity) {
        return entity.getAttributeMap();
    }

    /** 1.16's {@code applyNonPersistentModifier}/{@code applyPersistentModifier}: 1.12's, replacing one with the same id. */
    public static void applyModifier(net.minecraft.entity.ai.attributes.IAttributeInstance instance, net.minecraft.entity.ai.attributes.AttributeModifier modifier) {
        if (instance.getModifier(modifier.getID()) != null) instance.removeModifier(modifier.getID());
        instance.applyModifier(modifier);
    }

    public static boolean removeModifier(net.minecraft.entity.ai.attributes.IAttributeInstance instance, java.util.UUID id) {
        boolean had = instance.getModifier(id) != null;
        instance.removeModifier(id);
        return had;
    }

    /** 1.16's {@code createInstanceIfAbsent}-style lookup: 1.12's instance, registered if missing. */
    public static net.minecraft.entity.ai.attributes.IAttributeInstance attributeInstance(net.minecraft.entity.ai.attributes.AbstractAttributeMap map,
                                                                                       net.minecraft.entity.ai.attributes.IAttribute attribute) {
        net.minecraft.entity.ai.attributes.IAttributeInstance instance = map.getAttributeInstance(attribute);
        return instance != null ? instance : map.registerAttribute(attribute);
    }

    public static double mapValue(net.minecraft.entity.ai.attributes.AbstractAttributeMap map, net.minecraft.entity.ai.attributes.IAttribute attribute) {
        net.minecraft.entity.ai.attributes.IAttributeInstance instance = map.getAttributeInstance(attribute);
        return instance != null ? instance.getAttributeValue() : attribute.getDefaultValue();
    }

    public static boolean mapHas(net.minecraft.entity.ai.attributes.AbstractAttributeMap map, net.minecraft.entity.ai.attributes.IAttribute attribute) {
        return map.getAttributeInstance(attribute) != null;
    }

    private static final java.util.Map<net.minecraft.entity.ai.EntityAIBase, java.util.EnumSet<?>> GOAL_FLAGS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /**
     * 1.14's {@code goal.setMutexFlags(flags)}: 1.12's mutex bits, MOVE 1, LOOK 2, JUMP 4 (1.12's swimming); TARGET
     * goals run in 1.12's separate target task list, where a target task holds bit 1 as 1.12's do.
     */
    public static void setGoalFlags(net.minecraft.entity.ai.EntityAIBase goal, java.util.EnumSet<?> flags) {
        GOAL_FLAGS.put(goal, flags.clone());
        int bits = 0;
        for (Object flag : flags) {
            switch (((Enum<?>) flag).name()) {
                case "MOVE":
                case "TARGET":
                    bits |= 1;
                    break;
                case "LOOK":
                    bits |= 2;
                    break;
                case "JUMP":
                    bits |= 4;
                    break;
                default:
                    break;
            }
        }
        goal.setMutexBits(bits);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static java.util.EnumSet goalFlags(net.minecraft.entity.ai.EntityAIBase goal) {
        java.util.EnumSet<?> flags = GOAL_FLAGS.get(goal);
        return flags != null ? flags.clone() : null;
    }

    /** 1.14's {@code isPreemptible}: 1.12's {@code isInterruptible}. */
    public static boolean goalPreemptible(net.minecraft.entity.ai.EntityAIBase goal) {
        return goal.isInterruptible();
    }

    public static boolean isGlowing(net.minecraft.entity.Entity entity) {
        return entity.isGlowing();
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

    /** 1.12's AI flags (no AI, left-handed); 1.16 adds 4, aggressive. */
    @javax.annotation.Nullable
    private static net.minecraft.network.datasync.DataParameter<Byte> aiFlags;

    @SuppressWarnings("unchecked")
    private static net.minecraft.network.datasync.DataParameter<Byte> aiFlags() {
        if (aiFlags == null) {
            aiFlags = (net.minecraft.network.datasync.DataParameter<Byte>) net.minecraftforge.fml.common.ObfuscationReflectionHelper
                    .getPrivateValue(net.minecraft.entity.EntityLiving.class, null, "field_184654_a");
        }
        return aiFlags;
    }

    /** 1.16's {@code MobEntity.setAggroed(aggressive)}. */
    public static void setAggroed(net.minecraft.entity.EntityLiving entity, boolean aggressive) {
        byte flags = entity.getDataManager().get(aiFlags());
        entity.getDataManager().set(aiFlags(), (byte) (aggressive ? flags | 4 : flags & ~4));
    }

    /** 1.16's {@code MobEntity.isAggressive()}. */
    public static boolean isAggressive(net.minecraft.entity.EntityLiving entity) {
        return (entity.getDataManager().get(aiFlags()) & 4) != 0;
    }

    /** Forge 1.16's {@code IForgeBlockState.getAiPathNodeType(world, pos)}: 1.12 Forge's, on the block. */
    @javax.annotation.Nullable
    public static net.minecraft.pathfinding.PathNodeType pathNodeType(net.minecraft.block.state.IBlockState state, net.minecraft.world.IBlockAccess world,
            net.minecraft.util.math.BlockPos pos) {
        return state.getBlock().getAiPathNodeType(state, world, pos);
    }

    @javax.annotation.Nullable
    public static net.minecraft.pathfinding.PathNodeType pathNodeType(net.minecraft.block.state.IBlockState state, net.minecraft.world.IBlockAccess world,
            net.minecraft.util.math.BlockPos pos, @javax.annotation.Nullable net.minecraft.entity.EntityLiving entity) {
        return state.getBlock().getAiPathNodeType(state, world, pos);
    }

    /** 1.16's {@code entity.setMotionMultiplier(state, multiplier)}: 1.12 slows an entity only as cobwebs do. */
    public static void setMotionMultiplier(net.minecraft.entity.Entity entity, Object state, net.minecraft.util.math.Vec3d multiplier) {
        if (multiplier.x < 1.0D || multiplier.y < 1.0D || multiplier.z < 1.0D) entity.setInWeb();
    }

    /** 1.16's {@code attributeMap.reapplyModifiers(modifiers)}: keyed by attribute; 1.12's map takes them by name. */
    public static void reapplyModifiers(net.minecraft.entity.ai.attributes.AbstractAttributeMap map,
            com.google.common.collect.Multimap<?, net.minecraft.entity.ai.attributes.AttributeModifier> modifiers) {
        map.applyAttributeModifiers(byName(modifiers));
    }

    public static void removeModifiers(net.minecraft.entity.ai.attributes.AbstractAttributeMap map,
            com.google.common.collect.Multimap<?, net.minecraft.entity.ai.attributes.AttributeModifier> modifiers) {
        map.removeAttributeModifiers(byName(modifiers));
    }

    private static com.google.common.collect.Multimap<String, net.minecraft.entity.ai.attributes.AttributeModifier> byName(
            com.google.common.collect.Multimap<?, net.minecraft.entity.ai.attributes.AttributeModifier> modifiers) {
        com.google.common.collect.Multimap<String, net.minecraft.entity.ai.attributes.AttributeModifier> named = com.google.common.collect.HashMultimap.create();
        for (java.util.Map.Entry<?, net.minecraft.entity.ai.attributes.AttributeModifier> entry : modifiers.entries()) {
            Object key = entry.getKey();
            String name = key instanceof net.minecraft.entity.ai.attributes.IAttribute ? ((net.minecraft.entity.ai.attributes.IAttribute) key).getName() : String.valueOf(key);
            named.put(name, entry.getValue());
        }
        return named;
    }
}
