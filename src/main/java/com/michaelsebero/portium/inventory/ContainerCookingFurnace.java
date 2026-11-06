package com.michaelsebero.portium.inventory;

import com.michaelsebero.portium.recipe.CookingRecipes;
import com.michaelsebero.portium.tile.TileEntityCookingFurnace;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotFurnaceFuel;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The smoker's and blast furnace's screen (1.14): 1.12's furnace container, cooking their own recipes. */
public class ContainerCookingFurnace extends Container {

    private final IInventory furnace;
    private final CookingRecipes.Type type;
    private int cookTime;
    private int totalCookTime;
    private int burnTime;
    private int currentItemBurnTime;

    public ContainerCookingFurnace(InventoryPlayer playerInventory, IInventory furnace, CookingRecipes.Type type) {
        this.furnace = furnace;
        this.type = type;
        this.addSlotToContainer(new Slot(furnace, 0, 56, 17));
        this.addSlotToContainer(new SlotFurnaceFuel(furnace, 1, 56, 53));
        this.addSlotToContainer(new OutputSlot(playerInventory.player, furnace, 2, 116, 35));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) this.addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        }
        for (int column = 0; column < 9; column++) this.addSlotToContainer(new Slot(playerInventory, column, 8 + column * 18, 142));
    }

    public ContainerCookingFurnace(InventoryPlayer playerInventory, TileEntityCookingFurnace furnace) {
        this(playerInventory, furnace, furnace.getType());
    }

    public CookingRecipes.Type getType() {
        return this.type;
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendAllWindowProperties(this, this.furnace);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (IContainerListener listener : this.listeners) {
            if (this.cookTime != this.furnace.getField(2)) listener.sendWindowProperty(this, 2, this.furnace.getField(2));
            if (this.burnTime != this.furnace.getField(0)) listener.sendWindowProperty(this, 0, this.furnace.getField(0));
            if (this.currentItemBurnTime != this.furnace.getField(1)) listener.sendWindowProperty(this, 1, this.furnace.getField(1));
            if (this.totalCookTime != this.furnace.getField(3)) listener.sendWindowProperty(this, 3, this.furnace.getField(3));
        }
        this.cookTime = this.furnace.getField(2);
        this.burnTime = this.furnace.getField(0);
        this.currentItemBurnTime = this.furnace.getField(1);
        this.totalCookTime = this.furnace.getField(3);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void updateProgressBar(int id, int data) {
        this.furnace.setField(id, data);
    }

    public IInventory getFurnace() {
        return this.furnace;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.furnace.isUsableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return moved;
        ItemStack stack = slot.getStack();
        moved = stack.copy();
        if (index == 2) {
            if (!this.mergeItemStack(stack, 3, 39, true)) return ItemStack.EMPTY;
            slot.onSlotChange(stack, moved);
        } else if (index != 1 && index != 0) {
            if (CookingRecipes.find(this.type, stack) != null) {
                if (!this.mergeItemStack(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (TileEntityFurnace.isItemFuel(stack)) {
                if (!this.mergeItemStack(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else if (index < 30) {
                if (!this.mergeItemStack(stack, 30, 39, false)) return ItemStack.EMPTY;
            } else if (index < 39 && !this.mergeItemStack(stack, 3, 30, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.mergeItemStack(stack, 3, 39, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        if (stack.getCount() == moved.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return moved;
    }

    /** The result slot: taking cooked items gives their recipe's experience, as 1.12's furnace output does. */
    private final class OutputSlot extends Slot {

        private final EntityPlayer player;
        private int removeCount;

        OutputSlot(EntityPlayer player, IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            this.player = player;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public ItemStack decrStackSize(int amount) {
            if (this.getHasStack()) this.removeCount += Math.min(amount, this.getStack().getCount());
            return super.decrStackSize(amount);
        }

        @Override
        public ItemStack onTake(EntityPlayer player, ItemStack stack) {
            this.onCrafting(stack);
            super.onTake(player, stack);
            return stack;
        }

        @Override
        protected void onCrafting(ItemStack stack, int amount) {
            this.removeCount += amount;
            this.onCrafting(stack);
        }

        @Override
        protected void onCrafting(ItemStack stack) {
            stack.onCrafting(this.player.world, this.player, this.removeCount);
            if (!this.player.world.isRemote) {
                // 1.14's splitAndSpawnExperience: the whole part, and the fraction by chance.
                float total = (float) this.removeCount * CookingRecipes.experienceFor(ContainerCookingFurnace.this.type, stack);
                int count = MathHelper.floor(total);
                float fraction = total - (float) count;
                if (fraction != 0.0F && Math.random() < (double) fraction) count++;
                while (count > 0) {
                    int orb = EntityXPOrb.getXPSplit(count);
                    count -= orb;
                    this.player.world.spawnEntity(new EntityXPOrb(this.player.world, this.player.posX, this.player.posY + 0.5D, this.player.posZ + 0.5D, orb));
                }
            }
            this.removeCount = 0;
        }
    }
}
