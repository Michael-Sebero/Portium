package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.mixin.accessor.BlockStairsAccessor;
import com.michaelsebero.portium.modern.adapter.IBlockPropertyHolder;
import com.michaelsebero.portium.modern.adapter.ModernBlocks;
import com.michaelsebero.portium.modern.mc.block.Block$Properties;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * What a 1.16 mod's stairs are, made or extended: 1.12's stairs (with Portium's additions to them) made from 1.16's
 * properties. Forge 1.16 names the block stairs copy by a supplier, which often can't be asked until that block is
 * registered; until then the stairs copy a stand-in of the right material, then the block itself.
 */
public class StairsBlock116 extends BlockStairs {

    private static final List<StairsBlock116> PENDING = new CopyOnWriteArrayList<>();

    private final Supplier<?> model;

    public StairsBlock116(Supplier<?> model, Block$Properties properties) {
        super(standIn(properties));
        this.model = model;
        ((IBlockPropertyHolder) this).portium$applyBlockProperties(properties);
        if (!resolve()) PENDING.add(this);
    }

    /** 1.16's (deprecated) constructor naming the block's state itself (Botania's stairs). */
    public StairsBlock116(IBlockState model, Block$Properties properties) {
        this(() -> model, properties);
    }

    private static IBlockState standIn(Block$Properties properties) {
        return new Block(properties.getMaterial(), properties.getMapColor()).getDefaultState();
    }

    /** Copies the block the supplier names, once it can; true if it has. */
    private boolean resolve() {
        IBlockState state;
        try {
            state = (IBlockState) this.model.get();
        } catch (RuntimeException e) {
            return false;
        }
        if (state == null) return false;
        ((BlockStairsAccessor) this).portium$setModelBlock(state.getBlock());
        ((BlockStairsAccessor) this).portium$setModelState(state);
        return true;
    }

    /** Lets stairs waiting for their block copy it; called as each registry fills. */
    public static void resolvePending() {
        for (StairsBlock116 stairs : PENDING) {
            if (stairs.resolve()) PENDING.remove(stairs);
        }
    }

    @Override
    public String getTranslationKey() {
        return ModernBlocks.translationKey(this, super.getTranslationKey());
    }

    @Override
    public String getLocalizedName() {
        return ModernBlocks.localize(getTranslationKey());
    }

    /** Forge 1.13's tile entity factory, taking a block reader, which newer blocks override. */
    @javax.annotation.Nullable
    public net.minecraft.tileentity.TileEntity createTileEntity(IBlockState state, net.minecraft.world.IBlockAccess world) {
        return null;
    }

    @javax.annotation.Nullable
    @Override
    public net.minecraft.tileentity.TileEntity createTileEntity(net.minecraft.world.World world, IBlockState state) {
        return createTileEntity(state, (net.minecraft.world.IBlockAccess) world);
    }
}
