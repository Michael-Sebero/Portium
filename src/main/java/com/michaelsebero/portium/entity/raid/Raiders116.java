package com.michaelsebero.portium.entity.raid;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/** Lookups 1.14's raider goals share. */
public final class Raiders116 {

    /** 1.14's {@code notInRaid}: not part of an active raid. */
    public static boolean notInRaid(EntityLiving entity) {
        return !isRaidActive(entity);
    }

    /** Part of a raid that is still going. Raids come with the 1.14 villages; until then nothing is in one. */
    public static boolean isRaidActive(EntityLiving entity) {
        return IRaider.state(entity).getRaidId() >= 0 && RaidHooks.isActive(entity.world, IRaider.state(entity).getRaidId());
    }

    /** Whether it belongs to a raid at all (active or over). */
    public static boolean hasRaid(EntityLiving entity) {
        return IRaider.state(entity).getRaidId() >= 0 && RaidHooks.exists(entity.world, IRaider.state(entity).getRaidId());
    }

    /** Raiders (other than {@code self}) in a box, the way 1.14's raider target predicate sees them. */
    public static List<EntityLiving> raidersNear(EntityLiving self, AxisAlignedBB box, double distance) {
        return self.world.getEntitiesWithinAABB(EntityLiving.class, box, other -> other != self && other instanceof IRaider && other.isEntityAlive()
                && self.getDistanceSq(other) <= sq(Math.max(distance * visibility(other), 2.0D)));
    }

    /** 1.14's {@code getVisibilityMultiplier}, without the mob-head part: sneaking and invisibility hide. */
    public static double visibility(EntityLivingBase entity) {
        double visibility = 1.0D;
        if (entity.isSneaking()) visibility *= 0.8D;
        if (entity.isInvisible()) visibility *= 0.7D * Math.max(armorCover(entity), 0.1F);
        return visibility;
    }

    /** 1.14's {@code getArmorCoverPercentage}: the share of armour slots filled (1.12 only has it for players). */
    private static float armorCover(EntityLivingBase entity) {
        int slots = 0;
        int worn = 0;
        for (ItemStack stack : entity.getArmorInventoryList()) {
            ++slots;
            if (!stack.isEmpty()) ++worn;
        }
        return slots > 0 ? (float) worn / (float) slots : 0.0F;
    }

    /** 1.14's {@code getHeight(MOTION_BLOCKING_NO_LEAVES)}: above the top block that stops movement, leaves aside. */
    public static BlockPos surface(World world, BlockPos pos) {
        return world.getTopSolidOrLiquidBlock(pos);
    }

    private static double sq(double value) {
        return value * value;
    }

    private Raiders116() {}
}
