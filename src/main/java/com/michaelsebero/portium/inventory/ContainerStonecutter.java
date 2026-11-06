package com.michaelsebero.portium.inventory;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.recipe.StonecuttingRecipes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * The stonecutter's screen (1.14): one input, the recipes it can be cut into, and the chosen one's result. The choice
 * goes to the server as an enchant-button packet, as 1.14 does, and comes back as a window property.
 */
public class ContainerStonecutter extends Container {

    private final World world;
    private final BlockPos pos;
    private final InventoryBasic input = new InventoryBasic("Stonecutter", false, 1);
    private final InventoryCraftResult output = new InventoryCraftResult();
    private final Slot inputSlot;
    private final Slot outputSlot;
    private List<StonecuttingRecipes.Recipe> recipes = new ArrayList<>();
    private ItemStack lastInput = ItemStack.EMPTY;
    private int selected = -1;
    private int sentSelected = -2;
    private long lastTakeTime;
    private Runnable updateListener = () -> {};

    public ContainerStonecutter(InventoryPlayer playerInventory, World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
        this.input.addInventoryChangeListener(inventory -> {
            this.onCraftMatrixChanged(inventory);
            this.updateListener.run();
        });
        this.inputSlot = this.addSlotToContainer(new Slot(this.input, 0, 20, 33));
        this.outputSlot = this.addSlotToContainer(new Slot(this.output, 1, 143, 33) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            @Override
            public ItemStack onTake(EntityPlayer player, ItemStack stack) {
                stack.onCrafting(player.world, player, stack.getCount());
                ItemStack used = ContainerStonecutter.this.inputSlot.decrStackSize(1);
                if (!used.isEmpty()) ContainerStonecutter.this.updateResult();
                if (!ContainerStonecutter.this.world.isRemote) {
                    long time = ContainerStonecutter.this.world.getTotalWorldTime();
                    if (ContainerStonecutter.this.lastTakeTime != time) {
                        ContainerStonecutter.this.world.playSound(null, ContainerStonecutter.this.pos, PortiumSounds.UI_STONECUTTER_TAKE_RESULT, SoundCategory.BLOCKS,
                                1.0F, 1.0F);
                        ContainerStonecutter.this.lastTakeTime = time;
                    }
                }
                return super.onTake(player, stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) this.addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        }
        for (int column = 0; column < 9; column++) this.addSlotToContainer(new Slot(playerInventory, column, 8 + column * 18, 142));
    }

    public int getSelectedRecipe() {
        return this.selected;
    }

    public List<StonecuttingRecipes.Recipe> getRecipeList() {
        return this.recipes;
    }

    public boolean hasItemsInInputSlot() {
        return this.inputSlot.getHasStack() && !this.recipes.isEmpty();
    }

    @SideOnly(Side.CLIENT)
    public void setUpdateListener(Runnable listener) {
        this.updateListener = listener;
    }

    /** The button packet: choose a recipe. */
    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        if (id >= 0 && id < this.recipes.size()) {
            this.selected = id;
            this.updateResult();
        }
        return true;
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        ItemStack stack = this.inputSlot.getStack();
        if (stack.getItem() != this.lastInput.getItem() || stack.getMetadata() != this.lastInput.getMetadata()) {
            this.lastInput = stack.copy();
            this.recipes = new ArrayList<>();
            this.selected = -1;
            this.outputSlot.putStack(ItemStack.EMPTY);
            if (!stack.isEmpty()) this.recipes = StonecuttingRecipes.find(stack);
        }
    }

    private void updateResult() {
        if (!this.recipes.isEmpty() && this.selected >= 0 && this.selected < this.recipes.size()) {
            this.outputSlot.putStack(this.recipes.get(this.selected).getResult());
        } else {
            this.outputSlot.putStack(ItemStack.EMPTY);
        }
        this.detectAndSendChanges();
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (this.sentSelected != this.selected) {
            for (IContainerListener listener : this.listeners) listener.sendWindowProperty(this, 0, this.selected);
            this.sentSelected = this.selected;
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void updateProgressBar(int id, int data) {
        if (id == 0) this.selected = data;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return PortiumBlocks.STONECUTTER != null && this.world.getBlockState(this.pos).getBlock() == PortiumBlocks.STONECUTTER
                && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return slot.inventory != this.output && super.canMergeSlot(stack, slot);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return moved;
        ItemStack stack = slot.getStack();
        moved = stack.copy();
        if (index == 1) {
            stack.getItem().onCreated(stack, player.world, player);
            if (!this.mergeItemStack(stack, 2, 38, true)) return ItemStack.EMPTY;
            slot.onSlotChange(stack, moved);
        } else if (index == 0) {
            if (!this.mergeItemStack(stack, 2, 38, false)) return ItemStack.EMPTY;
        } else if (StonecuttingRecipes.hasRecipe(stack)) {
            if (!this.mergeItemStack(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 29) {
            if (!this.mergeItemStack(stack, 29, 38, false)) return ItemStack.EMPTY;
        } else if (index < 38 && !this.mergeItemStack(stack, 2, 29, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        slot.onSlotChanged();
        if (stack.getCount() == moved.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        this.detectAndSendChanges();
        return moved;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        this.output.removeStackFromSlot(0);
        if (!this.world.isRemote) this.clearContainer(player, this.world, this.input);
    }
}
