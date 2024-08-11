package com.michaelsebero.portium.modern.flat;

import com.michaelsebero.portium.compat.LegacyBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A 1.13 block that 1.12 has only as a variant of a block (see {@link Flattening}): unregistered, standing for the
 * variant's state, and answering as that state does.
 */
public final class FlattenedBlock extends Block {

    private final ResourceLocation name;
    private final IBlockState state;
    private final LegacyBlock legacy;

    FlattenedBlock(String name, IBlockState state, LegacyBlock legacy) {
        super(state.getMaterial(), mapColor(state));
        this.name = new ResourceLocation(name);
        this.state = state;
        this.legacy = legacy;
        setRegistryName(this.name);
        // The variant's 1.12 state is the default: what 1.13 code places.
        setDefaultState(state);
        try {
            this.blockHardness = state.getBlock().getBlockHardness(state, null, BlockPos.ORIGIN);
        } catch (RuntimeException e) {
            this.blockHardness = 1.5F;
        }
        this.blockResistance = state.getBlock().getExplosionResistance(null) * 5.0F;
        this.blockSoundType = state.getBlock().getSoundType();
        this.lightValue = state.getLightValue();
    }

    @SuppressWarnings("deprecation")
    private static MapColor mapColor(IBlockState state) {
        try {
            return state.getMapColor(null, BlockPos.ORIGIN);
        } catch (RuntimeException e) {
            return state.getMaterial().getMaterialMapColor();
        }
    }

    /** The 1.12 block this is a variant of. */
    public Block base() {
        return this.state.getBlock();
    }

    /** Whether a state of the base block is this variant. */
    boolean matches(IBlockState state) {
        return state.getBlock() == base() && this.legacy.test(state);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this);
    }

    @Override
    public BlockStateContainer getBlockState() {
        return base().getBlockState();
    }

    /** 1.13's translation key, which Portium's 1.13 language files have. */
    @Override
    public String getTranslationKey() {
        return "block." + this.name.getNamespace() + "." + this.name.getPath();
    }

    @Override
    @SuppressWarnings("deprecation")
    public Material getMaterial(IBlockState state) {
        return base().getMaterial(this.state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return base().getMapColor(this.state, world, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getBlockHardness(IBlockState state, World world, BlockPos pos) {
        return base().getBlockHardness(this.state, world, pos);
    }

    @Override
    public SoundType getSoundType(IBlockState state, World world, BlockPos pos, Entity entity) {
        return base().getSoundType(this.state, world, pos, entity);
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return base().getItem(world, pos, this.state);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, net.minecraft.util.math.RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return base().getPickBlock(this.state, target, world, pos, player);
    }

    /** 1.13's {@code asItem}: this variant's item, the view if it has one. */
    public Item asItem() {
        return Flattening.item(this.name.toString(), Item.class);
    }

    @Override
    public String toString() {
        return "Block{" + this.name + "}";
    }
}
