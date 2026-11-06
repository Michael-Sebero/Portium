package com.michaelsebero.portium.handler;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.BlockBush;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** A mob the Wither kills leaves a wither rose where it died, or drops one if it can't grow there (1.14). */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class WitherRoses {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        EntityLivingBase dead = event.getEntityLiving();
        World world = dead.world;
        if (world.isRemote || PortiumBlocks.WITHER_ROSE == null || !(dead.getAttackingEntity() instanceof EntityWither)) return;
        boolean placed = false;
        if (world.getGameRules().getBoolean("mobGriefing")) {
            BlockPos pos = new BlockPos(dead);
            if (world.isAirBlock(pos) && ((BlockBush) PortiumBlocks.WITHER_ROSE).canBlockStay(world, pos, PortiumBlocks.WITHER_ROSE.getDefaultState())) {
                world.setBlockState(pos, PortiumBlocks.WITHER_ROSE.getDefaultState(), 3);
                placed = true;
            }
        }
        if (!placed) world.spawnEntity(new EntityItem(world, dead.posX, dead.posY, dead.posZ, new ItemStack(PortiumBlocks.WITHER_ROSE)));
    }

    private WitherRoses() {}
}
