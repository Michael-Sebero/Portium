package com.michaelsebero.portium.inventory;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.item.LockedMaps;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;

/**
 * The cartography table's screen (1.14): a map and paper zooms it out, a map and an empty map copy it, a map and a
 * glass pane lock it. The map item does the zooming and locking when taken ({@code ItemMap.onCreated}).
 */
public class ContainerCartography extends Container {

    private final World world;
    private final BlockPos pos;
    private final InventoryBasic table = new InventoryBasic("Cartography", false, 2);
    private final InventoryCraftResult result = new InventoryCraftResult();
    private long lastTakeTime;

    public ContainerCartography(InventoryPlayer playerInventory, World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
        this.table.addInventoryChangeListener(this::onCraftMatrixChanged);
        this.addSlotToContainer(new Slot(this.table, 0, 15, 15) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() == Items.FILLED_MAP;
            }
        });
        this.addSlotToContainer(new Slot(this.table, 1, 15, 52) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return isSecondItem(stack.getItem());
            }
        });
        this.addSlotToContainer(new Slot(this.result, 2, 145, 39) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            @Override
            public ItemStack onTake(EntityPlayer player, ItemStack stack) {
                ContainerCartography.this.inventorySlots.get(0).decrStackSize(1);
                ContainerCartography.this.inventorySlots.get(1).decrStackSize(1);
                stack.getItem().onCreated(stack, player.world, player);
                if (!ContainerCartography.this.world.isRemote) {
                    long time = ContainerCartography.this.world.getTotalWorldTime();
                    if (ContainerCartography.this.lastTakeTime != time) {
                        ContainerCartography.this.world.playSound(null, ContainerCartography.this.pos, PortiumSounds.UI_CARTOGRAPHY_TABLE_TAKE_RESULT,
                                SoundCategory.BLOCKS, 1.0F, 1.0F);
                        ContainerCartography.this.lastTakeTime = time;
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

    private static boolean isSecondItem(Item item) {
        return item == Items.PAPER || item == Items.MAP || item == Item.getItemFromBlock(Blocks.GLASS_PANE);
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        ItemStack map = this.table.getStackInSlot(0);
        ItemStack second = this.table.getStackInSlot(1);
        ItemStack output = this.result.getStackInSlot(0);
        if (!map.isEmpty() && !second.isEmpty()) {
            this.updateResult(map, second, output);
        } else if (!output.isEmpty()) {
            this.result.removeStackFromSlot(0);
        }
    }

    private void updateResult(ItemStack map, ItemStack second, ItemStack previous) {
        MapData data = Items.FILLED_MAP.getMapData(map, this.world);
        if (data == null) return;
        Item item = second.getItem();
        boolean locked = LockedMaps.isLocked(data);
        ItemStack made;
        if (item == Items.PAPER && !locked && data.scale < 4) {
            made = map.copy();
            made.setCount(1);
            tag(made).setInteger("map_scale_direction", 1);
        } else if (item == Item.getItemFromBlock(Blocks.GLASS_PANE) && !locked) {
            made = map.copy();
            made.setCount(1);
            tag(made).setBoolean("map_to_lock", true);
        } else if (item == Items.MAP) {
            made = map.copy();
            made.setCount(2);
        } else {
            this.result.removeStackFromSlot(0);
            this.detectAndSendChanges();
            return;
        }
        if (!ItemStack.areItemStacksEqual(made, previous)) this.result.setInventorySlotContents(0, made);
        this.detectAndSendChanges();
    }

    private static NBTTagCompound tag(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return PortiumBlocks.CARTOGRAPHY_TABLE != null && this.world.getBlockState(this.pos).getBlock() == PortiumBlocks.CARTOGRAPHY_TABLE
                && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return slot.inventory != this.result && super.canMergeSlot(stack, slot);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return moved;
        ItemStack stack = slot.getStack();
        Item item = stack.getItem();
        moved = stack.copy();
        if (index == 2) {
            item.onCreated(stack, player.world, player);
            if (!this.mergeItemStack(stack, 3, 39, true)) return ItemStack.EMPTY;
            slot.onSlotChange(stack, moved);
        } else if (index != 1 && index != 0) {
            if (item == Items.FILLED_MAP) {
                if (!this.mergeItemStack(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (isSecondItem(item)) {
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
        slot.onSlotChanged();
        if (stack.getCount() == moved.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        this.detectAndSendChanges();
        return moved;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        this.result.removeStackFromSlot(0);
        if (!this.world.isRemote) this.clearContainer(player, this.world, this.table);
    }
}
