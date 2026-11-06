package com.michaelsebero.portium.inventory;

import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityXPOrb;
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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The grindstone's screen (1.14): one item (or two of the same, repaired together with 1.14's 5% bonus) comes out
 * without its enchantments, curses kept, and the player gets back some of the experience they cost.
 */
public class ContainerGrindstone extends Container {

    private final IInventory output = new InventoryCraftResult();
    private final InventoryBasic input = new InventoryBasic("Grindstone", false, 2);
    private final World world;
    private final BlockPos pos;

    public ContainerGrindstone(InventoryPlayer playerInventory, World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
        this.input.addInventoryChangeListener(inventory -> this.onCraftMatrixChanged(inventory));
        this.addSlotToContainer(new InputSlot(0, 49, 19));
        this.addSlotToContainer(new InputSlot(1, 49, 40));
        this.addSlotToContainer(new Slot(this.output, 2, 129, 34) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            @Override
            public ItemStack onTake(EntityPlayer player, ItemStack stack) {
                if (!ContainerGrindstone.this.world.isRemote) {
                    int experience = ContainerGrindstone.this.experienceFromInputs();
                    while (experience > 0) {
                        int orb = EntityXPOrb.getXPSplit(experience);
                        experience -= orb;
                        ContainerGrindstone.this.world.spawnEntity(new EntityXPOrb(ContainerGrindstone.this.world, (double) ContainerGrindstone.this.pos.getX(),
                                (double) ContainerGrindstone.this.pos.getY() + 0.5D, (double) ContainerGrindstone.this.pos.getZ() + 0.5D, orb));
                    }
                    ContainerGrindstone.this.world.playEvent(GRIND_EVENT, ContainerGrindstone.this.pos, 0);
                }
                ContainerGrindstone.this.input.setInventorySlotContents(0, ItemStack.EMPTY);
                ContainerGrindstone.this.input.setInventorySlotContents(1, ItemStack.EMPTY);
                return stack;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) this.addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        }
        for (int column = 0; column < 9; column++) this.addSlotToContainer(new Slot(playerInventory, column, 8 + column * 18, 142));
    }

    /** 1.14's world event 1042: the grindstone's sound (WorldEvents116). */
    public static final int GRIND_EVENT = 1042;

    private final class InputSlot extends Slot {
        InputSlot(int index, int x, int y) {
            super(ContainerGrindstone.this.input, index, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack.isItemStackDamageable() || stack.getItem() == Items.ENCHANTED_BOOK || stack.isItemEnchanted();
        }
    }

    private int experienceFromInputs() {
        int experience = enchantmentExperience(this.input.getStackInSlot(0)) + enchantmentExperience(this.input.getStackInSlot(1));
        if (experience <= 0) return 0;
        int half = (int) Math.ceil((double) experience / 2.0D);
        return half + this.world.rand.nextInt(half);
    }

    private static int enchantmentExperience(ItemStack stack) {
        int experience = 0;
        for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            if (!entry.getKey().isCurse()) experience += entry.getKey().getMinEnchantability(entry.getValue());
        }
        return experience;
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        super.onCraftMatrixChanged(inventory);
        if (inventory == this.input) this.updateOutput();
    }

    private void updateOutput() {
        ItemStack first = this.input.getStackInSlot(0);
        ItemStack second = this.input.getStackInSlot(1);
        boolean any = !first.isEmpty() || !second.isEmpty();
        boolean both = !first.isEmpty() && !second.isEmpty();
        if (!any) {
            this.output.setInventorySlotContents(0, ItemStack.EMPTY);
            this.detectAndSendChanges();
            return;
        }
        boolean plain = !first.isEmpty() && first.getItem() != Items.ENCHANTED_BOOK && !first.isItemEnchanted()
                || !second.isEmpty() && second.getItem() != Items.ENCHANTED_BOOK && !second.isItemEnchanted();
        if (first.getCount() > 1 || second.getCount() > 1 || !both && plain) {
            this.output.setInventorySlotContents(0, ItemStack.EMPTY);
            this.detectAndSendChanges();
            return;
        }
        int count = 1;
        int damage;
        ItemStack base;
        if (both) {
            if (first.getItem() != second.getItem()) {
                this.output.setInventorySlotContents(0, ItemStack.EMPTY);
                this.detectAndSendChanges();
                return;
            }
            Item item = first.getItem();
            int max = item.getMaxDamage(first);
            int durability = max - first.getItemDamage() + (max - second.getItemDamage()) + max * 5 / 100;
            damage = Math.max(max - durability, 0);
            base = copyEnchantments(first, second);
            if (!base.isItemStackDamageable()) {
                if (!ItemStack.areItemStacksEqual(first, second)) {
                    this.output.setInventorySlotContents(0, ItemStack.EMPTY);
                    this.detectAndSendChanges();
                    return;
                }
                count = 2;
            }
        } else {
            boolean firstOnly = !first.isEmpty();
            damage = firstOnly ? first.getItemDamage() : second.getItemDamage();
            base = firstOnly ? first : second;
        }
        this.output.setInventorySlotContents(0, removeEnchantments(base, damage, count));
        this.detectAndSendChanges();
    }

    /** The second item's enchantments added to the first's (a curse the first has isn't doubled). */
    private static ItemStack copyEnchantments(ItemStack first, ItemStack second) {
        ItemStack copy = first.copy();
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(copy);
        for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(second).entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment.isCurse() && enchantments.containsKey(enchantment)) continue;
            copy.addEnchantment(enchantment, entry.getValue());
        }
        return copy;
    }

    private static ItemStack removeEnchantments(ItemStack stack, int damage, int count) {
        ItemStack result = stack.copy();
        NBTTagCompound tag = result.getTagCompound();
        if (tag != null) {
            tag.removeTag("ench");
            tag.removeTag("StoredEnchantments");
        }
        if (damage > 0) result.setItemDamage(damage);
        else if (result.isItemStackDamageable()) result.setItemDamage(0);
        result.setCount(count);
        Map<Enchantment, Integer> curses = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            if (entry.getKey().isCurse()) curses.put(entry.getKey(), entry.getValue());
        }
        EnchantmentHelper.setEnchantments(curses, result);
        result.setRepairCost(0);
        if (result.getItem() == Items.ENCHANTED_BOOK && curses.isEmpty()) {
            result = new ItemStack(Items.BOOK);
            if (stack.hasDisplayName()) result.setStackDisplayName(stack.getDisplayName());
        }
        for (int i = 0; i < curses.size(); i++) result.setRepairCost(result.getRepairCost() * 2 + 1);
        if (result.hasTagCompound() && result.getTagCompound().isEmpty()) result.setTagCompound(null);
        return result;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!this.world.isRemote) this.clearContainer(player, this.world, this.input);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return PortiumBlocks.GRINDSTONE != null && this.world.getBlockState(this.pos).getBlock() == PortiumBlocks.GRINDSTONE
                && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return moved;
        ItemStack stack = slot.getStack();
        moved = stack.copy();
        ItemStack first = this.input.getStackInSlot(0);
        ItemStack second = this.input.getStackInSlot(1);
        if (index == 2) {
            if (!this.mergeItemStack(stack, 3, 39, true)) return ItemStack.EMPTY;
            slot.onSlotChange(stack, moved);
        } else if (index != 0 && index != 1) {
            if (!first.isEmpty() && !second.isEmpty()) {
                if (index < 30) {
                    if (!this.mergeItemStack(stack, 30, 39, false)) return ItemStack.EMPTY;
                } else if (index < 39 && !this.mergeItemStack(stack, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(stack, 0, 2, false)) {
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
}
