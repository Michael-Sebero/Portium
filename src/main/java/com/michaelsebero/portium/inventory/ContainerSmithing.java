package com.michaelsebero.portium.inventory;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The smithing table's menu (1.16): a diamond tool or armour piece and a netherite ingot make the netherite piece,
 * keeping the original's enchantments, name, other data and damage. Slots as 1.16 lays them out (base at 27,47,
 * ingot at 76,47, result at 134,47).
 */
public class ContainerSmithing extends Container {

    private static final Map<Item, Item> UPGRADES = new IdentityHashMap<>();

    private final InventoryBasic inputs = new InventoryBasic("Smithing", false, 2) {
        @Override
        public void markDirty() {
            super.markDirty();
            onCraftMatrixChanged(this);
        }
    };
    private final InventoryCraftResult result = new InventoryCraftResult();
    private final World world;
    private final BlockPos pos;

    public ContainerSmithing(InventoryPlayer player, World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
        addSlotToContainer(new Slot(this.inputs, 0, 27, 47));
        addSlotToContainer(new Slot(this.inputs, 1, 76, 47));
        addSlotToContainer(new Slot(this.result, 2, 134, 47) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            @Override
            public ItemStack onTake(EntityPlayer taker, ItemStack stack) {
                inputs.decrStackSize(0, 1);
                inputs.decrStackSize(1, 1);
                if (!world.isRemote && PortiumSounds.BLOCK_SMITHING_TABLE_USE != null) {
                    world.playSound(null, ContainerSmithing.this.pos, PortiumSounds.BLOCK_SMITHING_TABLE_USE, SoundCategory.BLOCKS, 1.0F,
                            world.rand.nextFloat() * 0.1F + 0.9F);
                }
                return super.onTake(taker, stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) addSlotToContainer(new Slot(player, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlotToContainer(new Slot(player, column, 8 + column * 18, 142));
    }

    /** The netherite piece a diamond one becomes, or null. */
    @Nullable
    public static Item upgrade(Item item) {
        if (UPGRADES.isEmpty() && PortiumItems.NETHERITE_INGOT != null) {
            UPGRADES.put(Items.DIAMOND_SWORD, PortiumItems.NETHERITE_SWORD);
            UPGRADES.put(Items.DIAMOND_PICKAXE, PortiumItems.NETHERITE_PICKAXE);
            UPGRADES.put(Items.DIAMOND_AXE, PortiumItems.NETHERITE_AXE);
            UPGRADES.put(Items.DIAMOND_SHOVEL, PortiumItems.NETHERITE_SHOVEL);
            UPGRADES.put(Items.DIAMOND_HOE, PortiumItems.NETHERITE_HOE);
            UPGRADES.put(Items.DIAMOND_HELMET, PortiumItems.NETHERITE_HELMET);
            UPGRADES.put(Items.DIAMOND_CHESTPLATE, PortiumItems.NETHERITE_CHESTPLATE);
            UPGRADES.put(Items.DIAMOND_LEGGINGS, PortiumItems.NETHERITE_LEGGINGS);
            UPGRADES.put(Items.DIAMOND_BOOTS, PortiumItems.NETHERITE_BOOTS);
        }
        return UPGRADES.get(item);
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        super.onCraftMatrixChanged(inventory);
        if (inventory == this.inputs) this.result.setInventorySlotContents(0, resultFor(this.inputs.getStackInSlot(0), this.inputs.getStackInSlot(1)));
    }

    private static ItemStack resultFor(ItemStack base, ItemStack addition) {
        if (base.isEmpty() || addition.isEmpty() || addition.getItem() != PortiumItems.NETHERITE_INGOT) return ItemStack.EMPTY;
        Item upgraded = upgrade(base.getItem());
        if (upgraded == null) return ItemStack.EMPTY;
        ItemStack made = new ItemStack(upgraded);
        if (base.hasTagCompound()) made.setTagCompound(base.getTagCompound().copy());
        made.setItemDamage(base.getItemDamage());
        return made;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.world.getBlockState(this.pos).getBlock() == PortiumBlocks.SMITHING_TABLE
                && player.getDistanceSq(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!this.world.isRemote) clearContainer(player, this.world, this.inputs);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index == 2) {
            if (!mergeItemStack(stack, 3, 39, true)) return ItemStack.EMPTY;
            slot.onSlotChange(stack, original);
        } else if (index < 2) {
            if (!mergeItemStack(stack, 3, 39, false)) return ItemStack.EMPTY;
        } else {
            int target = stack.getItem() == PortiumItems.NETHERITE_INGOT ? 1 : upgrade(stack.getItem()) != null ? 0 : -1;
            if (target >= 0) {
                if (!mergeItemStack(stack, target, target + 1, false)) return ItemStack.EMPTY;
            } else if (index < 30) {
                if (!mergeItemStack(stack, 30, 39, false)) return ItemStack.EMPTY;
            } else if (!mergeItemStack(stack, 3, 30, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
