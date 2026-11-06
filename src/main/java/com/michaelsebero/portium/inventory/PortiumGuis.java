package com.michaelsebero.portium.inventory;

import com.michaelsebero.portium.client.gui.GuiSmithing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

/** Portium's block screens, opened through Forge's GUI handler. */
public final class PortiumGuis implements IGuiHandler {

    public static final int SMITHING_TABLE = 0;
    public static final int SMOKER = 1;
    public static final int BLAST_FURNACE = 2;
    public static final int GRINDSTONE = 3;
    public static final int STONECUTTER = 4;
    public static final int LOOM = 5;
    public static final int CARTOGRAPHY_TABLE = 6;
    public static final int LECTERN = 7;
    /** 1.14's trade screen; x is the merchant's entity id. */
    public static final int MERCHANT = 8;

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == SMITHING_TABLE) return new ContainerSmithing(player.inventory, world, new BlockPos(x, y, z));
        if (id == SMOKER || id == BLAST_FURNACE) {
            net.minecraft.tileentity.TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (tile instanceof com.michaelsebero.portium.tile.TileEntityCookingFurnace) {
                return new ContainerCookingFurnace(player.inventory, (com.michaelsebero.portium.tile.TileEntityCookingFurnace) tile);
            }
        }
        if (id == GRINDSTONE) return new ContainerGrindstone(player.inventory, world, new BlockPos(x, y, z));
        if (id == STONECUTTER) return new ContainerStonecutter(player.inventory, world, new BlockPos(x, y, z));
        if (id == LOOM) return new ContainerLoom(player.inventory, world, new BlockPos(x, y, z));
        if (id == CARTOGRAPHY_TABLE) return new ContainerCartography(player.inventory, world, new BlockPos(x, y, z));
        if (id == MERCHANT) {
            net.minecraft.entity.Entity entity = world.getEntityByID(x);
            if (entity instanceof com.michaelsebero.portium.merchant.IMerchant116) {
                return new com.michaelsebero.portium.merchant.ContainerMerchant116(player.inventory, (com.michaelsebero.portium.merchant.IMerchant116) entity);
            }
        }
        if (id == LECTERN) {
            net.minecraft.tileentity.TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (tile instanceof com.michaelsebero.portium.tile.TileEntityLectern) {
                return new ContainerLectern(((com.michaelsebero.portium.tile.TileEntityLectern) tile).getInventory(), (com.michaelsebero.portium.tile.TileEntityLectern) tile);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == SMITHING_TABLE) return new GuiSmithing(new ContainerSmithing(player.inventory, world, new BlockPos(x, y, z)));
        if (id == SMOKER || id == BLAST_FURNACE) {
            net.minecraft.tileentity.TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
            if (tile instanceof com.michaelsebero.portium.tile.TileEntityCookingFurnace) {
                return new com.michaelsebero.portium.client.gui.GuiCookingFurnace(player.inventory, (com.michaelsebero.portium.tile.TileEntityCookingFurnace) tile);
            }
        }
        if (id == GRINDSTONE) {
            return new com.michaelsebero.portium.client.gui.GuiGrindstone(new ContainerGrindstone(player.inventory, world, new BlockPos(x, y, z)), player.inventory);
        }
        if (id == STONECUTTER) {
            return new com.michaelsebero.portium.client.gui.GuiStonecutter(new ContainerStonecutter(player.inventory, world, new BlockPos(x, y, z)), player.inventory);
        }
        if (id == LOOM) return new com.michaelsebero.portium.client.gui.GuiLoom(new ContainerLoom(player.inventory, world, new BlockPos(x, y, z)), player.inventory);
        if (id == CARTOGRAPHY_TABLE) {
            return new com.michaelsebero.portium.client.gui.GuiCartography(new ContainerCartography(player.inventory, world, new BlockPos(x, y, z)), player.inventory);
        }
        if (id == MERCHANT) {
            net.minecraft.entity.Entity entity = world.getEntityByID(x);
            String title = entity != null ? entity.getDisplayName().getFormattedText() : "";
            return new com.michaelsebero.portium.client.gui.GuiMerchant116(new com.michaelsebero.portium.merchant.ContainerMerchant116(player.inventory,
                    new com.michaelsebero.portium.merchant.ClientMerchant(player)), player.inventory, title);
        }
        if (id == LECTERN) return new com.michaelsebero.portium.client.gui.GuiLectern(new ContainerLectern(new net.minecraft.inventory.InventoryBasic("", false, 1), null));
        return null;
    }
}
