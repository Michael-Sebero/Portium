package com.michaelsebero.portium.tile;

import com.michaelsebero.portium.block.workstation.BlockCookingFurnace;
import com.michaelsebero.portium.inventory.ContainerCookingFurnace;
import com.michaelsebero.portium.recipe.CookingRecipes;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.SlotFurnaceFuel;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nullable;

/**
 * The smoker and blast furnace (1.14): 1.12's furnace, cooking their own recipe types ({@link CookingRecipes}) in the
 * recipe's time (twice a furnace's speed for 1.14's) and burning fuel twice as fast. Hoppers use it as a furnace.
 */
public abstract class TileEntityCookingFurnace extends TileEntityLockable implements ITickable, ISidedInventory {

    private static final int[] SLOTS_TOP = {0};
    private static final int[] SLOTS_BOTTOM = {2, 1};
    private static final int[] SLOTS_SIDES = {1};

    private final CookingRecipes.Type type;
    private NonNullList<ItemStack> stacks = NonNullList.withSize(3, ItemStack.EMPTY);
    private int burnTime;
    private int currentItemBurnTime;
    private int cookTime;
    private int totalCookTime;
    private String customName;

    private final IItemHandler handlerTop = new SidedInvWrapper(this, EnumFacing.UP);
    private final IItemHandler handlerBottom = new SidedInvWrapper(this, EnumFacing.DOWN);
    private final IItemHandler handlerSide = new SidedInvWrapper(this, EnumFacing.WEST);

    protected TileEntityCookingFurnace(CookingRecipes.Type type) {
        this.type = type;
    }

    /** The smoker's (1.14), saved as {@code portium:smoker}. */
    public static class Smoker extends TileEntityCookingFurnace {
        public Smoker() {
            super(CookingRecipes.Type.SMOKING);
        }
    }

    /** The blast furnace's (1.14), saved as {@code portium:blast_furnace}. */
    public static class BlastFurnace extends TileEntityCookingFurnace {
        public BlastFurnace() {
            super(CookingRecipes.Type.BLASTING);
        }
    }

    public CookingRecipes.Type getType() {
        return this.type;
    }

    // ---- Inventory ----

    @Override
    public int getSizeInventory() {
        return this.stacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.stacks) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return this.stacks.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return ItemStackHelper.getAndSplit(this.stacks, index, count);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return ItemStackHelper.getAndRemove(this.stacks, index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        ItemStack previous = this.stacks.get(index);
        boolean same = !stack.isEmpty() && stack.isItemEqual(previous) && ItemStack.areItemStackTagsEqual(stack, previous);
        this.stacks.set(index, stack);
        if (stack.getCount() > this.getInventoryStackLimit()) stack.setCount(this.getInventoryStackLimit());
        if (index == 0 && !same) {
            this.totalCookTime = this.getCookTime(stack);
            this.cookTime = 0;
            this.markDirty();
        }
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.customName : this.type == CookingRecipes.Type.BLASTING ? "container.blast_furnace" : "container.smoker";
    }

    @Override
    public boolean hasCustomName() {
        return this.customName != null && !this.customName.isEmpty();
    }

    public void setCustomName(String name) {
        this.customName = name;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this
                && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == 2) return false;
        if (index != 1) return true;
        ItemStack fuel = this.stacks.get(1);
        return TileEntityFurnace.isItemFuel(stack) || SlotFurnaceFuel.isBucket(stack) && fuel.getItem() != Items.BUCKET;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return side == EnumFacing.DOWN ? SLOTS_BOTTOM : side == EnumFacing.UP ? SLOTS_TOP : SLOTS_SIDES;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, EnumFacing direction) {
        return this.isItemValidForSlot(index, stack);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        if (direction == EnumFacing.DOWN && index == 1) {
            Item item = stack.getItem();
            return item == Items.WATER_BUCKET || item == Items.BUCKET;
        }
        return true;
    }

    @Override
    public void clear() {
        this.stacks.clear();
    }

    @Override
    public int getField(int id) {
        switch (id) {
            case 0: return this.burnTime;
            case 1: return this.currentItemBurnTime;
            case 2: return this.cookTime;
            case 3: return this.totalCookTime;
            default: return 0;
        }
    }

    @Override
    public void setField(int id, int value) {
        switch (id) {
            case 0: this.burnTime = value; break;
            case 1: this.currentItemBurnTime = value; break;
            case 2: this.cookTime = value; break;
            case 3: this.totalCookTime = value; break;
            default: break;
        }
    }

    @Override
    public int getFieldCount() {
        return 4;
    }

    @Override
    public String getGuiID() {
        return this.type == CookingRecipes.Type.BLASTING ? "portium:blast_furnace" : "portium:smoker";
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer player) {
        return new ContainerCookingFurnace(playerInventory, this);
    }

    // ---- Cooking ----

    public boolean isBurning() {
        return this.burnTime > 0;
    }

    /** 1.14 burns fuel twice as fast here as in a furnace. */
    public static int getItemBurnTime(ItemStack stack) {
        return TileEntityFurnace.getItemBurnTime(stack) / 2;
    }

    public int getCookTime(ItemStack stack) {
        CookingRecipes.Recipe recipe = CookingRecipes.find(this.type, stack);
        return recipe != null ? recipe.cookingTime : CookingRecipes.DEFAULT_TIME;
    }

    @Override
    public void update() {
        boolean wasBurning = this.isBurning();
        boolean changed = false;
        if (this.isBurning()) this.burnTime--;
        if (!this.world.isRemote) {
            ItemStack fuel = this.stacks.get(1);
            if (this.isBurning() || !fuel.isEmpty() && !this.stacks.get(0).isEmpty()) {
                if (!this.isBurning() && this.canSmelt()) {
                    this.burnTime = getItemBurnTime(fuel);
                    this.currentItemBurnTime = this.burnTime;
                    if (this.isBurning()) {
                        changed = true;
                        if (!fuel.isEmpty()) {
                            Item item = fuel.getItem();
                            fuel.shrink(1);
                            if (fuel.isEmpty()) this.stacks.set(1, item.getContainerItem(fuel));
                        }
                    }
                }
                if (this.isBurning() && this.canSmelt()) {
                    this.cookTime++;
                    if (this.cookTime >= this.totalCookTime) {
                        this.cookTime = 0;
                        this.totalCookTime = this.getCookTime(this.stacks.get(0));
                        this.smeltItem();
                        changed = true;
                    }
                } else {
                    this.cookTime = 0;
                }
            } else if (!this.isBurning() && this.cookTime > 0) {
                this.cookTime = MathHelper.clamp(this.cookTime - 2, 0, this.totalCookTime);
            }
            if (wasBurning != this.isBurning()) {
                changed = true;
                BlockCookingFurnace.setLit(this.world, this.pos, this.isBurning());
            }
        }
        if (changed) this.markDirty();
    }

    private boolean canSmelt() {
        ItemStack input = this.stacks.get(0);
        if (input.isEmpty()) return false;
        CookingRecipes.Recipe recipe = CookingRecipes.find(this.type, input);
        if (recipe == null) return false;
        ItemStack result = recipe.getResult();
        ItemStack output = this.stacks.get(2);
        if (output.isEmpty()) return true;
        if (!output.isItemEqual(result) || !ItemStack.areItemStackTagsEqual(output, result)) return false;
        int count = output.getCount() + result.getCount();
        return count <= this.getInventoryStackLimit() && count <= output.getMaxStackSize();
    }

    private void smeltItem() {
        if (!this.canSmelt()) return;
        ItemStack input = this.stacks.get(0);
        ItemStack result = CookingRecipes.find(this.type, input).getResult();
        ItemStack output = this.stacks.get(2);
        if (output.isEmpty()) this.stacks.set(2, result.copy());
        else output.grow(result.getCount());
        input.shrink(1);
    }

    // ---- Saving ----

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.stacks = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(compound, this.stacks);
        this.burnTime = compound.getInteger("BurnTime");
        this.cookTime = compound.getInteger("CookTime");
        this.totalCookTime = compound.getInteger("CookTimeTotal");
        this.currentItemBurnTime = getItemBurnTime(this.stacks.get(1));
        if (compound.hasKey("CustomName", 8)) this.customName = compound.getString("CustomName");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("BurnTime", (short) this.burnTime);
        compound.setInteger("CookTime", (short) this.cookTime);
        compound.setInteger("CookTimeTotal", (short) this.totalCookTime);
        ItemStackHelper.saveAllItems(compound, this.stacks);
        if (this.hasCustomName()) compound.setString("CustomName", this.customName);
        return compound;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    // ---- Hoppers ----

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != null || super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != null) {
            if (facing == EnumFacing.DOWN) return (T) this.handlerBottom;
            if (facing == EnumFacing.UP) return (T) this.handlerTop;
            return (T) this.handlerSide;
        }
        return super.getCapability(capability, facing);
    }
}
