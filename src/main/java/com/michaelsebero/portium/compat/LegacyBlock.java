package com.michaelsebero.portium.compat;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;

import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

/**
 * What a 1.13 block name means in 1.12: a block, plus the variant properties that tell it apart from the other 1.13
 * blocks sharing that 1.12 block ({@code minecraft:mossy_stone_bricks} is {@code stonebrick} with
 * {@code variant=mossy_stonebrick}). Matches any state with that block and those variant values.
 */
public final class LegacyBlock implements Predicate<IBlockState> {

    private final Block block;
    private final Map<IProperty<?>, Comparable<?>> variant;

    public LegacyBlock(Block block, Map<IProperty<?>, Comparable<?>> variant) {
        this.block = block;
        this.variant = variant;
    }

    public static LegacyBlock of(Block block) {
        return new LegacyBlock(block, Collections.emptyMap());
    }

    public Block getBlock() {
        return this.block;
    }

    public Map<IProperty<?>, Comparable<?>> getVariant() {
        return this.variant;
    }

    @Override
    public boolean test(IBlockState state) {
        if (state.getBlock() != this.block) return false;
        for (Map.Entry<IProperty<?>, Comparable<?>> entry : this.variant.entrySet()) {
            if (!state.getValue(entry.getKey()).equals(entry.getValue())) return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LegacyBlock && ((LegacyBlock) o).block == this.block && ((LegacyBlock) o).variant.equals(this.variant);
    }

    @Override
    public int hashCode() {
        return this.block.hashCode() * 31 + this.variant.hashCode();
    }
}
