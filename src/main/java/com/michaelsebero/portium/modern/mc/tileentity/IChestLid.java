package com.michaelsebero.portium.modern.mc.tileentity;

import com.michaelsebero.portium.modern.Srg;

/**
 * 1.13's {@code net.minecraft.tileentity.IChestLid}: how far open a chest's lid is. 1.13's chests have it, 1.12's
 * don't, so calls to it go to {@code TileEntityShims.getLidAngle}, which reads a 1.12 chest's lid as well.
 */
public interface IChestLid {

    @Srg("func_195480_a")
    float getLidAngle(float partialTicks);
}
