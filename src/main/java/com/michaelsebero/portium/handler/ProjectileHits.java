package com.michaelsebero.portium.handler;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.block.IProjectileHitBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/** 1.16's {@code onProjectileCollision} for Portium's blocks: what a projectile hitting them does (targets, bells). */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ProjectileHits {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onImpact(ProjectileImpactEvent event) {
        RayTraceResult hit = event.getRayTraceResult();
        Entity projectile = event.getEntity();
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || projectile.world.isRemote) return;
        IBlockState state = projectile.world.getBlockState(hit.getBlockPos());
        if (state.getBlock() instanceof IProjectileHitBlock) {
            ((IProjectileHitBlock) state.getBlock()).onProjectileHit(projectile.world, state, hit, projectile, shooter(projectile));
        }
    }

    @Nullable
    private static Entity shooter(Entity projectile) {
        if (projectile instanceof EntityArrow) return ((EntityArrow) projectile).shootingEntity;
        if (projectile instanceof EntityThrowable) return ((EntityThrowable) projectile).getThrower();
        if (projectile instanceof EntityFireball) return ((EntityFireball) projectile).shootingEntity;
        return null;
    }

    private ProjectileHits() {}
}
