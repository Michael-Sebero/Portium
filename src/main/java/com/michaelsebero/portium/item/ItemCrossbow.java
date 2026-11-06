package com.michaelsebero.portium.item;

import com.google.common.collect.Lists;
import com.michaelsebero.portium.entity.ICrossbowArrow;
import com.michaelsebero.portium.entity.ICrossbowRocket;
import com.michaelsebero.portium.entity.ICrossbowUser;
import com.michaelsebero.portium.init.PortiumEnchantments;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * The crossbow (1.14). Held down it loads (25 ticks, 5 fewer per Quick Charge level) and stays loaded until fired,
 * with an arrow or a firework rocket held in the other hand (else an arrow from the inventory); Multishot loads three,
 * fired 10° apart; Piercing arrows pass through mobs. Arrows fly at 3.15, rockets at 1.6.
 * <p>
 * The use lasts as long as it's held (1.12 would end it, so this never reports a finite duration: the charge is the
 * time held, as 1.14's {@code getUseDuration - itemInUseCount}).
 */
public class ItemCrossbow extends Item {

    public static final int USE_DURATION = 72000;
    private static final Random SOUND_RANDOM = new Random();

    private boolean isLoadingStart;
    private boolean isLoadingMiddle;

    public ItemCrossbow() {
        this.setMaxStackSize(1);
        this.setMaxDamage(326);
        this.setCreativeTab(CreativeTabs.COMBAT);
        this.addPropertyOverride(new ResourceLocation("pull"), (stack, world, entity) -> {
            if (entity == null || isCharged(stack)) return 0.0F;
            return entity.getActiveItemStack() == stack ? (float) (USE_DURATION - entity.getItemInUseCount()) / (float) getChargeTime(stack) : 0.0F;
        });
        this.addPropertyOverride(new ResourceLocation("pulling"),
                (stack, world, entity) -> entity != null && entity.isHandActive() && entity.getActiveItemStack() == stack && !isCharged(stack) ? 1.0F : 0.0F);
        this.addPropertyOverride(new ResourceLocation("charged"), (stack, world, entity) -> entity != null && isCharged(stack) ? 1.0F : 0.0F);
        this.addPropertyOverride(new ResourceLocation("firework"),
                (stack, world, entity) -> entity != null && isCharged(stack) && hasChargedProjectile(stack, Items.FIREWORKS) ? 1.0F : 0.0F);
    }

    /** What it loads from the other hand: arrows or firework rockets. */
    public static boolean isHeldAmmo(ItemStack stack) {
        return stack.getItem() instanceof ItemArrow || stack.getItem() == Items.FIREWORKS;
    }

    /** What it loads from the inventory: arrows only. */
    public static boolean isInventoryAmmo(ItemStack stack) {
        return stack.getItem() instanceof ItemArrow;
    }

    /** 1.14's {@code findAmmo}: the other hand (then this one), else the inventory; creative players always have an arrow. */
    public static ItemStack findAmmo(EntityLivingBase shooter) {
        if (isHeldAmmo(shooter.getHeldItem(EnumHand.OFF_HAND))) return shooter.getHeldItem(EnumHand.OFF_HAND);
        if (isHeldAmmo(shooter.getHeldItem(EnumHand.MAIN_HAND))) return shooter.getHeldItem(EnumHand.MAIN_HAND);
        if (!(shooter instanceof EntityPlayer)) return new ItemStack(Items.ARROW);
        EntityPlayer player = (EntityPlayer) shooter;
        for (int i = 0; i < player.inventory.getSizeInventory(); ++i) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (isInventoryAmmo(stack)) return stack;
        }
        return player.capabilities.isCreativeMode ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (isCharged(stack)) {
            fireProjectiles(world, player, hand, stack, velocity(stack), 1.0F);
            if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                com.michaelsebero.portium.advancements.ModernTriggers116.SHOT_CROSSBOW.trigger((net.minecraft.entity.player.EntityPlayerMP) player, stack,
                        null, null, null, 0.0F, null);
            }
            setCharged(stack, false);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (!findAmmo(player).isEmpty()) {
            this.isLoadingStart = false;
            this.isLoadingMiddle = false;
            player.setActiveHand(hand);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.FAIL, stack);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entity, int timeLeft) {
        float charge = getCharge(USE_DURATION - timeLeft, stack);
        if (charge >= 1.0F && !isCharged(stack) && loadAmmo(entity, stack)) {
            setCharged(stack, true);
            SoundCategory category = entity instanceof EntityPlayer ? SoundCategory.PLAYERS : SoundCategory.HOSTILE;
            world.playSound(null, entity.posX, entity.posY, entity.posZ, PortiumSounds.ITEM_CROSSBOW_LOADING_END, category, 1.0F,
                    1.0F / (itemRand.nextFloat() * 0.5F + 1.0F) + 0.2F);
        }
    }

    /** Takes the ammunition (one; Multishot copies it twice more) into the crossbow. */
    public static boolean loadAmmo(EntityLivingBase shooter, ItemStack crossbow) {
        int count = EnchantmentHelper.getEnchantmentLevel(PortiumEnchantments.MULTISHOT, crossbow) == 0 ? 1 : 3;
        boolean creative = shooter instanceof EntityPlayer && ((EntityPlayer) shooter).capabilities.isCreativeMode;
        ItemStack ammo = findAmmo(shooter);
        ItemStack copy = ammo.copy();
        for (int i = 0; i < count; ++i) {
            if (i > 0) ammo = copy.copy();
            if (ammo.isEmpty() && creative) {
                ammo = new ItemStack(Items.ARROW);
                copy = ammo.copy();
            }
            if (!loadProjectile(shooter, crossbow, ammo, i > 0, creative)) return false;
        }
        return true;
    }

    private static boolean loadProjectile(EntityLivingBase shooter, ItemStack crossbow, ItemStack ammo, boolean extra, boolean creative) {
        if (ammo.isEmpty()) return false;
        boolean infinite = creative && ammo.getItem() instanceof ItemArrow;
        ItemStack projectile;
        if (!infinite && !creative && !extra) {
            projectile = ammo.splitStack(1);
            if (ammo.isEmpty() && shooter instanceof EntityPlayer) ((EntityPlayer) shooter).inventory.deleteStack(ammo);
        } else {
            projectile = ammo.copy();
        }
        addChargedProjectile(crossbow, projectile);
        return true;
    }

    public static boolean isCharged(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getBoolean("Charged");
    }

    public static void setCharged(ItemStack stack, boolean charged) {
        if (stack.isEmpty()) return;
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setBoolean("Charged", charged);
    }

    private static void addChargedProjectile(ItemStack crossbow, ItemStack projectile) {
        if (!crossbow.hasTagCompound()) crossbow.setTagCompound(new NBTTagCompound());
        NBTTagCompound tag = crossbow.getTagCompound();
        NBTTagList list = tag.hasKey("ChargedProjectiles", 9) ? tag.getTagList("ChargedProjectiles", 10) : new NBTTagList();
        list.appendTag(projectile.writeToNBT(new NBTTagCompound()));
        tag.setTag("ChargedProjectiles", list);
    }

    private static List<ItemStack> getChargedProjectiles(ItemStack crossbow) {
        List<ItemStack> projectiles = Lists.newArrayList();
        NBTTagCompound tag = crossbow.getTagCompound();
        if (tag != null && tag.hasKey("ChargedProjectiles", 9)) {
            NBTTagList list = tag.getTagList("ChargedProjectiles", 10);
            for (int i = 0; i < list.tagCount(); ++i) projectiles.add(new ItemStack(list.getCompoundTagAt(i)));
        }
        return projectiles;
    }

    private static void clearProjectiles(ItemStack crossbow) {
        NBTTagCompound tag = crossbow.getTagCompound();
        if (tag != null) tag.setTag("ChargedProjectiles", new NBTTagList());
    }

    public static boolean hasChargedProjectile(ItemStack crossbow, Item item) {
        return getChargedProjectiles(crossbow).stream().anyMatch(stack -> stack.getItem() == item);
    }

    private static void fireProjectile(World world, EntityLivingBase shooter, EnumHand hand, ItemStack crossbow, ItemStack projectile, float pitch,
                                       boolean creative, float velocity, float inaccuracy, float angle) {
        if (world.isRemote) return;
        boolean firework = projectile.getItem() == Items.FIREWORKS;
        Entity entity;
        if (firework) {
            EntityFireworkRocket rocket = new EntityFireworkRocket(world, shooter.posX, shooter.posY + shooter.getEyeHeight() - 0.15000000596046448D, shooter.posZ,
                    projectile);
            ((ICrossbowRocket) rocket).portium$shootFrom(shooter);
            entity = rocket;
        } else {
            EntityArrow arrow = createArrow(world, shooter, crossbow, projectile);
            if (creative || angle != 0.0F) arrow.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
            entity = arrow;
        }
        if (shooter instanceof ICrossbowUser) {
            ICrossbowUser user = (ICrossbowUser) shooter;
            user.shootCrossbowProjectile(user.getCrossbowTarget(), crossbow, entity, angle);
        } else {
            Vec3d direction = CrossbowAim.rotate(shooter.getLook(1.0F), CrossbowAim.upVector(shooter), angle);
            shoot(entity, direction.x, direction.y, direction.z, velocity, inaccuracy);
        }
        crossbow.damageItem(firework ? 3 : 1, shooter);
        world.spawnEntity(entity);
        world.playSound(null, shooter.posX, shooter.posY, shooter.posZ, PortiumSounds.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1.0F, pitch);
    }

    /** {@code IProjectile.shoot} for arrows, and the same aim for rockets. */
    public static void shoot(Entity projectile, double x, double y, double z, float velocity, float inaccuracy) {
        if (projectile instanceof IProjectile) {
            ((IProjectile) projectile).shoot(x, y, z, velocity, inaccuracy);
        } else if (projectile instanceof ICrossbowRocket) {
            ((ICrossbowRocket) projectile).portium$shoot(x, y, z, velocity, inaccuracy);
        }
    }

    private static EntityArrow createArrow(World world, EntityLivingBase shooter, ItemStack crossbow, ItemStack ammo) {
        ItemArrow arrowItem = (ItemArrow) (ammo.getItem() instanceof ItemArrow ? ammo.getItem() : Items.ARROW);
        EntityArrow arrow = arrowItem.createArrow(world, ammo, shooter);
        if (shooter instanceof EntityPlayer) arrow.setIsCritical(true);
        ICrossbowArrow crossbowArrow = (ICrossbowArrow) arrow;
        crossbowArrow.portium$setHitSound(PortiumSounds.ITEM_CROSSBOW_HIT);
        crossbowArrow.portium$setShotFromCrossbow(true);
        int piercing = EnchantmentHelper.getEnchantmentLevel(PortiumEnchantments.PIERCING, crossbow);
        if (piercing > 0) crossbowArrow.portium$setPierceLevel((byte) piercing);
        return arrow;
    }

    /** Fires what's loaded: the first straight, the second 10° left, the third 10° right. */
    public static void fireProjectiles(World world, EntityLivingBase shooter, EnumHand hand, ItemStack crossbow, float velocity, float inaccuracy) {
        List<ItemStack> projectiles = getChargedProjectiles(crossbow);
        float[] pitches = randomSoundPitches(shooter.getRNG());
        boolean creative = shooter instanceof EntityPlayer && ((EntityPlayer) shooter).capabilities.isCreativeMode;
        for (int i = 0; i < projectiles.size() && i < 3; ++i) {
            ItemStack projectile = projectiles.get(i);
            if (projectile.isEmpty()) continue;
            float angle = i == 0 ? 0.0F : i == 1 ? -10.0F : 10.0F;
            fireProjectile(world, shooter, hand, crossbow, projectile, pitches[i], creative, velocity, inaccuracy, angle);
        }
        if (shooter instanceof EntityPlayerMP) ((EntityPlayerMP) shooter).addStat(StatList.getObjectUseStats(crossbow.getItem()));
        clearProjectiles(crossbow);
    }

    private static float[] randomSoundPitches(Random random) {
        boolean high = random.nextBoolean();
        return new float[] {1.0F, randomSoundPitch(high), randomSoundPitch(!high)};
    }

    private static float randomSoundPitch(boolean high) {
        float base = high ? 0.63F : 0.43F;
        return 1.0F / (SOUND_RANDOM.nextFloat() * 0.5F + 1.8F) + base;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase entity, int count) {
        World world = entity.world;
        if (world.isRemote) return;
        int quickCharge = EnchantmentHelper.getEnchantmentLevel(PortiumEnchantments.QUICK_CHARGE, stack);
        SoundEvent start = startSound(quickCharge);
        SoundEvent middle = quickCharge == 0 ? PortiumSounds.ITEM_CROSSBOW_LOADING_MIDDLE : null;
        float progress = (float) (USE_DURATION - count) / (float) getChargeTime(stack);
        if (progress < 0.2F) {
            this.isLoadingStart = false;
            this.isLoadingMiddle = false;
        }
        if (progress >= 0.2F && !this.isLoadingStart) {
            this.isLoadingStart = true;
            world.playSound(null, entity.posX, entity.posY, entity.posZ, start, SoundCategory.PLAYERS, 0.5F, 1.0F);
        }
        if (progress >= 0.5F && middle != null && !this.isLoadingMiddle) {
            this.isLoadingMiddle = true;
            world.playSound(null, entity.posX, entity.posY, entity.posZ, middle, SoundCategory.PLAYERS, 0.5F, 1.0F);
        }
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    public static int getChargeTime(ItemStack stack) {
        int quickCharge = EnchantmentHelper.getEnchantmentLevel(PortiumEnchantments.QUICK_CHARGE, stack);
        return quickCharge == 0 ? 25 : 25 - 5 * quickCharge;
    }

    /** No bow pose: Portium draws the crossbow's own (1.12 has no crossbow use action). */
    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.NONE;
    }

    private static SoundEvent startSound(int quickCharge) {
        switch (quickCharge) {
            case 1: return PortiumSounds.ITEM_CROSSBOW_QUICK_CHARGE_1;
            case 2: return PortiumSounds.ITEM_CROSSBOW_QUICK_CHARGE_2;
            case 3: return PortiumSounds.ITEM_CROSSBOW_QUICK_CHARGE_3;
            default: return PortiumSounds.ITEM_CROSSBOW_LOADING_START;
        }
    }

    private static float getCharge(int useTime, ItemStack stack) {
        return Math.min(1.0F, (float) useTime / (float) getChargeTime(stack));
    }

    /** 1.6 for a loaded rocket, 3.15 for arrows. */
    public static float velocity(ItemStack crossbow) {
        return hasChargedProjectile(crossbow, Items.FIREWORKS) ? 1.6F : 3.15F;
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        List<ItemStack> projectiles = getChargedProjectiles(stack);
        if (!isCharged(stack) || projectiles.isEmpty()) return;
        ItemStack first = projectiles.get(0);
        tooltip.add(I18n.translateToLocal("item.minecraft.crossbow.projectile") + " " + first.getDisplayName());
        if (flag.isAdvanced() && first.getItem() == Items.FIREWORKS) {
            List<String> lines = Lists.newArrayList();
            Items.FIREWORKS.addInformation(first, world, lines, flag);
            for (String line : lines) tooltip.add(TextFormatting.GRAY + "  " + line);
        }
    }

    /** Aim helpers: 1.14's quaternion turns, as rotations about an axis. */
    public static final class CrossbowAim {
        /** The direction "up" from an entity's view (its look turned 90° up). */
        public static Vec3d upVector(Entity entity) {
            float pitch = (entity.rotationPitch - 90.0F) * 0.017453292F;
            float yaw = -entity.rotationYaw * 0.017453292F;
            float yawCos = MathHelper.cos(yaw);
            float yawSin = MathHelper.sin(yaw);
            float pitchCos = MathHelper.cos(pitch);
            float pitchSin = MathHelper.sin(pitch);
            return new Vec3d(yawSin * pitchCos, -pitchSin, yawCos * pitchCos);
        }

        /** {@code vector} turned {@code degrees} about {@code axis} (Rodrigues' formula, as 1.14's quaternion transform). */
        public static Vec3d rotate(Vec3d vector, Vec3d axis, float degrees) {
            Vec3d k = axis.normalize();
            double angle = Math.toRadians(degrees);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3d cross = k.crossProduct(vector);
            double dot = k.dotProduct(vector);
            return vector.scale(cos).add(cross.scale(sin)).add(k.scale(dot * (1.0D - cos)));
        }

        private CrossbowAim() {}
    }
}
