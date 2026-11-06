package com.michaelsebero.portium.tile;

import com.michaelsebero.portium.block.light.BlockCampfire;
import com.michaelsebero.portium.recipe.CookingRecipes;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * A campfire's four cooking slots (1.14): food placed on it cooks while it burns (30 seconds for 1.16's recipes) and
 * drops off cooked; on a campfire that goes out it cools down twice as fast. Clients get the items to draw them.
 */
public class TileEntityCampfire extends TileEntity implements ITickable {

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(4, ItemStack.EMPTY);
    private final int[] cookingTimes = new int[4];
    private final int[] cookingTotalTimes = new int[4];

    @Override
    public void update() {
        IBlockState state = this.world.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockCampfire)) return;
        boolean lit = state.getValue(BlockCampfire.LIT);
        if (this.world.isRemote) {
            if (lit) this.addParticles(state);
        } else if (lit) {
            this.cookAndDrop();
        } else {
            for (int i = 0; i < this.inventory.size(); i++) {
                if (this.cookingTimes[i] > 0) this.cookingTimes[i] = MathHelper.clamp(this.cookingTimes[i] - 2, 0, this.cookingTotalTimes[i]);
            }
        }
    }

    private void cookAndDrop() {
        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack stack = this.inventory.get(i);
            if (stack.isEmpty()) continue;
            this.cookingTimes[i]++;
            if (this.cookingTimes[i] >= this.cookingTotalTimes[i]) {
                CookingRecipes.Recipe recipe = CookingRecipes.find(CookingRecipes.Type.CAMPFIRE_COOKING, stack);
                ItemStack result = recipe != null ? recipe.getResult() : stack;
                InventoryHelper.spawnItemStack(this.world, this.pos.getX(), this.pos.getY(), this.pos.getZ(), result);
                this.inventory.set(i, ItemStack.EMPTY);
                this.inventoryChanged();
            }
        }
    }

    private void addParticles(IBlockState state) {
        Random random = this.world.rand;
        if (random.nextFloat() < 0.11F) {
            for (int i = 0; i < random.nextInt(2) + 2; i++) BlockCampfire.spawnSmokeParticles(this.world, this.pos, state.getValue(BlockCampfire.SIGNAL_FIRE), false);
        }
        int facing = state.getValue(BlockCampfire.FACING).getHorizontalIndex();
        for (int i = 0; i < this.inventory.size(); i++) {
            if (this.inventory.get(i).isEmpty() || random.nextFloat() >= 0.2F) continue;
            EnumFacing side = EnumFacing.byHorizontalIndex(Math.floorMod(i + facing, 4));
            double x = (double) this.pos.getX() + 0.5D - (double) ((float) side.getXOffset() * 0.3125F) + (double) ((float) side.rotateY().getXOffset() * 0.3125F);
            double y = (double) this.pos.getY() + 0.5D;
            double z = (double) this.pos.getZ() + 0.5D - (double) ((float) side.getZOffset() * 0.3125F) + (double) ((float) side.rotateY().getZOffset() * 0.3125F);
            for (int k = 0; k < 4; k++) this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0.0D, 5.0E-4D, 0.0D);
        }
    }

    public NonNullList<ItemStack> getInventory() {
        return this.inventory;
    }

    /** Whether the stack can go on: a free slot and a campfire recipe for it. */
    @Nullable
    public CookingRecipes.Recipe findMatchingRecipe(ItemStack stack) {
        for (ItemStack slot : this.inventory) {
            if (slot.isEmpty()) return CookingRecipes.find(CookingRecipes.Type.CAMPFIRE_COOKING, stack);
        }
        return null;
    }

    /** Puts one of the stack in the first free slot. */
    public boolean addItem(ItemStack stack, int cookingTime) {
        for (int i = 0; i < this.inventory.size(); i++) {
            if (this.inventory.get(i).isEmpty()) {
                this.cookingTotalTimes[i] = cookingTime;
                this.cookingTimes[i] = 0;
                this.inventory.set(i, stack.splitStack(1));
                this.inventoryChanged();
                return true;
            }
        }
        return false;
    }

    /** Drops everything on it, when it goes out. */
    public void dropAllItems() {
        if (this.world == null) return;
        if (!this.world.isRemote) dropItems(this.world, this.pos, this.inventory);
        this.inventoryChanged();
    }

    public static void dropItems(net.minecraft.world.World world, BlockPos pos, NonNullList<ItemStack> items) {
        for (ItemStack stack : items) InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
    }

    private void inventoryChanged() {
        this.markDirty();
        IBlockState state = this.world.getBlockState(this.pos);
        this.world.notifyBlockUpdate(this.pos, state, state, 3);
    }

    // ---- Saving and syncing ----

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.inventory.clear();
        ItemStackHelper.loadAllItems(compound, this.inventory);
        if (compound.hasKey("CookingTimes", 11)) {
            int[] times = compound.getIntArray("CookingTimes");
            System.arraycopy(times, 0, this.cookingTimes, 0, Math.min(this.cookingTotalTimes.length, times.length));
        }
        if (compound.hasKey("CookingTotalTimes", 11)) {
            int[] times = compound.getIntArray("CookingTotalTimes");
            System.arraycopy(times, 0, this.cookingTotalTimes, 0, Math.min(this.cookingTotalTimes.length, times.length));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        this.writeItems(compound);
        compound.setIntArray("CookingTimes", this.cookingTimes);
        compound.setIntArray("CookingTotalTimes", this.cookingTotalTimes);
        return compound;
    }

    private NBTTagCompound writeItems(NBTTagCompound compound) {
        super.writeToNBT(compound);
        ItemStackHelper.saveAllItems(compound, this.inventory, true);
        return compound;
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 13, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeItems(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        this.readFromNBT(packet.getNbtCompound());
    }

    @Override
    public boolean shouldRefresh(net.minecraft.world.World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }
}
