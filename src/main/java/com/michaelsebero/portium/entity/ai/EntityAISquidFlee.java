package com.michaelsebero.portium.entity.ai;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Squids swim away from whatever hurt them while it is within 10 blocks (1.13). */
public class EntityAISquidFlee extends EntityAIBase {

    private final EntitySquid squid;

    public EntityAISquidFlee(EntitySquid squid) {
        this.squid = squid;
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase attacker = this.squid.getRevengeTarget();
        return this.squid.isInWater() && attacker != null && this.squid.getDistanceSq(attacker) < 100.0D;
    }

    @Override
    public void updateTask() {
        EntityLivingBase attacker = this.squid.getRevengeTarget();
        if (attacker == null) return;
        Vec3d away = new Vec3d(this.squid.posX - attacker.posX, this.squid.posY - attacker.posY, this.squid.posZ - attacker.posZ);
        BlockPos aheadPos = new BlockPos(this.squid.posX + away.x, this.squid.posY + away.y, this.squid.posZ + away.z);
        boolean air = this.squid.world.getBlockState(aheadPos).getMaterial() == Material.AIR;
        if (!Waterlogging.hasWater(this.squid.world, aheadPos) && !air) return;
        double distance = away.length();
        // 1.13 means to normalise first but discards the result, so the scale applies to the full offset.
        if (distance > 0.0D) {
            float speed = 3.0F;
            if (distance > 5.0D) speed -= (float) ((distance - 5.0D) / 5.0D);
            if (speed > 0.0F) away = away.scale(speed);
        }
        if (air) away = away.subtract(0.0D, away.y, 0.0D);
        this.squid.setMovementVector((float) away.x / 20.0F, (float) away.y / 20.0F, (float) away.z / 20.0F);
    }
}
