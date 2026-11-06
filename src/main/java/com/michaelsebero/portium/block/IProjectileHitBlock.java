package com.michaelsebero.portium.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** 1.16's {@code Block.onProjectileCollision}: a block that reacts to a projectile hitting it (server side). */
public interface IProjectileHitBlock {

    void onProjectileHit(World world, IBlockState state, RayTraceResult hit, Entity projectile, @Nullable Entity shooter);
}
