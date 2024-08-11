package com.michaelsebero.portium.modern.shim;

import com.michaelsebero.portium.modern.mc.stats.StatType;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.world.DimensionType;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/** 1.13 methods and fields that 1.12 has in another shape. */
public final class MiscShims {

    private static final StatType<Block> BLOCK_MINED = new StatType<>(StatList::getBlockStats);

    /** 1.13's {@code EnumDyeColor.getId}, 1.12's wool metadata. */
    public static int dyeId(EnumDyeColor color) {
        return color.getMetadata();
    }

    /** 1.13's name for a dye colour, which its keys use too: 1.12's, but light_gray for 1.12's silver. */
    public static String dyeName(EnumDyeColor color) {
        return color == EnumDyeColor.SILVER ? "light_gray" : color.getName();
    }

    /** 1.13's returns whether the effect took; 1.12's always applies it. */
    public static boolean addPotionEffect(EntityLivingBase entity, PotionEffect effect) {
        if (!entity.isPotionApplicable(effect)) return false;
        entity.addPotionEffect(effect);
        return true;
    }

    public static WorldServer getWorld(MinecraftServer server, DimensionType type) {
        return server.getWorld(type.getId());
    }

    public static DamageSource netherBedExplosion() {
        return new DamageSource("netherBedExplosion").setDifficultyScaled().setExplosion();
    }

    /** 1.13's explosion with a damage source; 1.12's explosions all hurt as explosions. */
    public static Explosion createExplosion(World world, Entity entity, DamageSource source, double x, double y, double z, float strength, boolean fire,
                                            boolean damagesTerrain) {
        return world.newExplosion(entity, x, y, z, strength, fire, damagesTerrain);
    }

    /** 1.13's {@code IWorld.isRemote()}, 1.12's field. */
    public static boolean isRemote(World world) {
        return world.isRemote;
    }

    /** 1.13's {@code StatList.BLOCK_MINED}. */
    public static StatType<Block> blockMined() {
        return BLOCK_MINED;
    }

    private MiscShims() {}

    // Dimensions: 1.13 passes dimension types where 1.12 passes ids.

    /** The type of a dimension id; the overworld's for one 1.12 doesn't know. */
    public static DimensionType dimensionType(int id) {
        try {
            return DimensionType.getById(id);
        } catch (IllegalArgumentException e) {
            return DimensionType.OVERWORLD;
        }
    }

    /** 1.13's {@code Entity.dimension}. */
    public static DimensionType dimension(Entity entity) {
        return dimensionType(entity.dimension);
    }

    public static void setDimension(Entity entity, DimensionType type) {
        entity.dimension = type.getId();
    }

    /** Forge 1.13's dimension type registry names: {@code minecraft:overworld}, {@code minecraft:the_nether} and so on. */
    public static ResourceLocation dimensionName(DimensionType type) {
        return new ResourceLocation(type.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_"));
    }

    public static WorldServer getWorld(MinecraftServer server, DimensionType type, boolean resetUnloadDelay, boolean forceLoad) {
        WorldServer world = DimensionManager.getWorld(type.getId());
        if (world == null && forceLoad) {
            DimensionManager.initDimension(type.getId());
            world = DimensionManager.getWorld(type.getId());
        }
        return world;
    }

    // Worlds: what 1.13 asks of its world interfaces.

    public static Random getRandom(World world) {
        return world.rand;
    }

    public static World getWorld(World world) {
        return world;
    }

    @Nullable
    public static WorldProvider getDimension(IBlockAccess world) {
        return world instanceof World ? ((World) world).provider : null;
    }

    public static boolean isRemote(IBlockAccess world) {
        return world instanceof World && ((World) world).isRemote;
    }

    // Entities: 1.13 names them with text components and keeps who shot an arrow by UUID.

    public static ITextComponent getName(Entity entity) {
        return new TextComponentString(entity.getName());
    }

    @Nullable
    public static ITextComponent getCustomName(Entity entity) {
        return entity.hasCustomName() ? new TextComponentString(entity.getCustomNameTag()) : null;
    }

    public static void setCustomName(Entity entity, @Nullable ITextComponent name) {
        entity.setCustomNameTag(name == null ? "" : name.getFormattedText());
    }

    @Nullable
    public static UUID shootingEntity(EntityArrow arrow) {
        return arrow.shootingEntity == null ? null : arrow.shootingEntity.getUniqueID();
    }

    // Fuel: Forge 1.13 asks the stack, then the event with that time.

    public static int getBurnTime(ItemStack stack) {
        return stack.getItem().getItemBurnTime(stack);
    }

    public static int getItemBurnTime(ItemStack stack, int burnTime) {
        int fromEvent = ForgeEventFactory.getItemBurnTime(stack);
        return fromEvent >= 0 ? fromEvent : burnTime;
    }

    @Nullable
    public static Entity getShooter(EntityArrow arrow) {
        return arrow.shootingEntity;
    }

    /** 1.13's {@code Container.areItemsAndTagsEqual}. */
    public static boolean areItemsAndTagsEqual(ItemStack first, ItemStack second) {
        return ItemStack.areItemsEqual(first, second) && ItemStack.areItemStackTagsEqual(first, second);
    }

    // A target task's entity filter: Java's predicate in 1.13, Guava's in 1.12.

    private static final java.lang.reflect.Field TARGET_SELECTOR = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(
            net.minecraft.entity.ai.EntityAINearestAttackableTarget.class, "targetEntitySelector", "field_82643_g");

    @Nullable
    @SuppressWarnings("unchecked")
    public static java.util.function.Predicate<Object> targetSelector(net.minecraft.entity.ai.EntityAINearestAttackableTarget<?> task) {
        try {
            com.google.common.base.Predicate<Object> selector = (com.google.common.base.Predicate<Object>) TARGET_SELECTOR.get(task);
            return selector == null ? null : selector::apply;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void setTargetSelector(net.minecraft.entity.ai.EntityAINearestAttackableTarget<?> task, @Nullable java.util.function.Predicate<Object> selector) {
        try {
            TARGET_SELECTOR.set(task, selector == null ? null : (com.google.common.base.Predicate<Object>) selector::test);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    // Names of nameable things (inventories, tile entities) as 1.13's text components.

    public static ITextComponent nameOf(net.minecraft.world.IWorldNameable nameable) {
        return new TextComponentString(nameable.getName());
    }

    @Nullable
    public static ITextComponent customNameOf(net.minecraft.world.IWorldNameable nameable) {
        return nameable.hasCustomName() ? new TextComponentString(nameable.getName()) : null;
    }
}
