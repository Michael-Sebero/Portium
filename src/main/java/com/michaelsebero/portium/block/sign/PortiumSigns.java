package com.michaelsebero.portium.block.sign;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.tile.TileEntityPortiumSign;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Random;

/**
 * Signs of newer woods, standing and on walls: 1.12's sign blocks with Portium's sign tile entity (drawn with the
 * wood's texture, {@code textures/entity/signs/<wood>.png}) and their own item, named {@code <wood>_sign}.
 */
public final class PortiumSigns {

    public static final class Standing extends BlockStandingSign implements IPortiumSign {

        private final String wood;

        public Standing(String wood) {
            this.wood = wood;
            this.setHardness(1.0F);
            this.setSoundType(SoundType.WOOD);
            this.disableStats();
        }

        @Override
        public ResourceLocation getSignTexture() {
            return texture(this.wood);
        }

        @Override
        public Item getSignItem() {
            return item(this.wood);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileEntityPortiumSign();
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return getSignItem();
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(getSignItem());
        }
    }

    public static final class Wall extends BlockWallSign implements IPortiumSign {

        private final String wood;

        public Wall(String wood) {
            this.wood = wood;
            this.setHardness(1.0F);
            this.setSoundType(SoundType.WOOD);
            this.disableStats();
        }

        @Override
        public ResourceLocation getSignTexture() {
            return texture(this.wood);
        }

        @Override
        public Item getSignItem() {
            return item(this.wood);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileEntityPortiumSign();
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return getSignItem();
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(getSignItem());
        }
    }

    static ResourceLocation texture(String wood) {
        return new ResourceLocation(Tags.MOD_ID, "textures/entity/signs/" + wood + ".png");
    }

    static Item item(String wood) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(Tags.MOD_ID, wood + "_sign"));
        return item == null ? Items.SIGN : item;
    }

    private PortiumSigns() {}
}
