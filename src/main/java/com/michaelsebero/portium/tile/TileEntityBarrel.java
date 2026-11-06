package com.michaelsebero.portium.tile;

import com.michaelsebero.portium.block.workstation.BlockBarrel;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The barrel's 27 slots (1.14): a chest that can't be blocked from opening. It shows open while players look in,
 * checked every 5 ticks as 1.14 does, with its own open and close sounds. Uses 1.12's chest screen.
 */
public class TileEntityBarrel extends TileEntityLockableLoot {

    private NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
    private int numPlayersUsing;

    @Override
    public int getSizeInventory() {
        return 27;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.contents) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.contents;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.customName : "container.barrel";
    }

    @Override
    public String getGuiID() {
        return "minecraft:chest";
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer player) {
        this.fillWithLoot(player);
        return new ContainerChest(playerInventory, this, player);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.contents = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
        if (!this.checkLootAndRead(compound)) ItemStackHelper.loadAllItems(compound, this.contents);
        if (compound.hasKey("CustomName", 8)) this.customName = compound.getString("CustomName");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (!this.checkLootAndWrite(compound)) ItemStackHelper.saveAllItems(compound, this.contents);
        if (this.hasCustomName()) compound.setString("CustomName", this.customName);
        return compound;
    }

    // ---- Open and closed ----

    @Override
    public void openInventory(EntityPlayer player) {
        if (player.isSpectator() || this.world.isRemote) return;
        if (this.numPlayersUsing < 0) this.numPlayersUsing = 0;
        this.numPlayersUsing++;
        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockBarrel)) return;
        if (!state.getValue(BlockBarrel.OPEN)) {
            this.playSound(state, PortiumSounds.BLOCK_BARREL_OPEN);
            this.world.setBlockState(this.pos, state.withProperty(BlockBarrel.OPEN, true), 3);
        }
        this.world.scheduleUpdate(this.pos, state.getBlock(), 5);
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        if (!player.isSpectator()) this.numPlayersUsing--;
    }

    /** The block's scheduled tick: counts who still looks in, and closes once nobody does. */
    public void barrelTick() {
        this.numPlayersUsing = countPlayersUsing(this.world, this, this.pos);
        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockBarrel)) {
            this.invalidate();
            return;
        }
        if (this.numPlayersUsing > 0) {
            this.world.scheduleUpdate(this.pos, state.getBlock(), 5);
        } else if (state.getValue(BlockBarrel.OPEN)) {
            this.playSound(state, PortiumSounds.BLOCK_BARREL_CLOSE);
            this.world.setBlockState(this.pos, state.withProperty(BlockBarrel.OPEN, false), 3);
        }
    }

    /** 1.14's {@code ChestTileEntity.calculatePlayersUsing}: players within 5 blocks with this barrel's screen open. */
    private static int countPlayersUsing(World world, IInventory inventory, BlockPos pos) {
        int count = 0;
        AxisAlignedBB around = new AxisAlignedBB(pos).grow(5.0D);
        for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class, around)) {
            if (player.openContainer instanceof ContainerChest && ((ContainerChest) player.openContainer).getLowerChestInventory() == inventory) count++;
        }
        return count;
    }

    private void playSound(IBlockState state, SoundEvent sound) {
        EnumFacing facing = state.getValue(BlockBarrel.FACING);
        double x = (double) this.pos.getX() + 0.5D + (double) facing.getXOffset() / 2.0D;
        double y = (double) this.pos.getY() + 0.5D + (double) facing.getYOffset() / 2.0D;
        double z = (double) this.pos.getZ() + 0.5D + (double) facing.getZOffset() / 2.0D;
        this.world.playSound(null, x, y, z, sound, SoundCategory.BLOCKS, 0.5F, this.world.rand.nextFloat() * 0.1F + 0.9F);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }
}
