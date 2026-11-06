package com.michaelsebero.portium.entity;

import net.minecraft.entity.EntityLivingBase;

/** 1.14's {@code constructKnockBackVector}: an attacker whose hit a shield blocked reacts its own way instead of being knocked back. */
public interface IShieldBlockReaction {

    void onBlockedByShield(EntityLivingBase blocker);
}
