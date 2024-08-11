package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.adapter.IBlockPropertyHolder;
import com.michaelsebero.portium.modern.adapter.ModernBlocks;
import com.michaelsebero.portium.modern.mc.block.Block$Properties;
import com.michaelsebero.portium.modern.mc.state.StateContainer$Builder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;

/**
 * What a 1.16 mod's rails extend instead of 1.12's {@code BlockRailBase}: 1.12's rail, so minecarts ride it, made
 * from 1.16's properties with 1.16's state container.
 */
public abstract class AbstractRailBlock116 extends BlockRailBase {

    public AbstractRailBlock116(boolean disableCorners, Block$Properties properties) {
        super(disableCorners);
        ((IBlockPropertyHolder) this).portium$applyBlockProperties(properties);
    }

    @Srg("func_206840_a")
    protected void fillStateContainer(StateContainer$Builder<Block, IBlockState> builder) {}

    @Override
    protected BlockStateContainer createBlockState() {
        StateContainer$Builder<Block, IBlockState> builder = new StateContainer$Builder<>(this);
        fillStateContainer(builder);
        return builder.build();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return ModernBlocks.metaFromState(this, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return ModernBlocks.stateFromMeta(this, meta);
    }

    @Override
    public String getTranslationKey() {
        return ModernBlocks.translationKey(this, super.getTranslationKey());
    }

    @Override
    public String getLocalizedName() {
        return ModernBlocks.localize(getTranslationKey());
    }
}
