package com.michaelsebero.portium.modern.client;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The block being drawn on this thread, while 1.12 draws one. Forge 1.13's models ask the world for their model data
 * (a keypad's disguise) before their quads are asked for; 1.12 asks for quads alone, so Portium keeps where they're
 * for to find the data itself.
 */
@SideOnly(Side.CLIENT)
public final class ModernRenderContext {

    private static final ThreadLocal<Object[]> CURRENT = new ThreadLocal<>();

    public static void begin(IBlockAccess world, BlockPos pos) {
        CURRENT.set(new Object[] {world, pos});
    }

    public static void end() {
        CURRENT.remove();
    }

    /** The world and position of the block being drawn, or null when none is (items, entities). */
    @Nullable
    public static Object[] current() {
        return CURRENT.get();
    }

    private ModernRenderContext() {}
}
