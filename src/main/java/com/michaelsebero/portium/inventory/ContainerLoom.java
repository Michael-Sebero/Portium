package com.michaelsebero.portium.inventory;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.item.BannerPatterns116;
import com.michaelsebero.portium.item.ItemBannerPattern;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The loom's screen (1.14): a banner, a dye and (for some patterns) a banner pattern give the banner one more layer,
 * up to six. The chosen pattern is an index in 1.16's order ({@link BannerPatterns116}), chosen with the enchant-button
 * packet and sent back as a window property.
 */
public class ContainerLoom extends Container {

    private final World world;
    private final BlockPos pos;
    private final InventoryBasic input = new InventoryBasic("Loom", false, 3);
    private final InventoryBasic outputInventory = new InventoryBasic("Loom", false, 1);
    private final Slot bannerSlot;
    private final Slot dyeSlot;
    private final Slot patternSlot;
    private final Slot outputSlot;
    private int selected;
    private int sentSelected = -1;
    private long lastTakeTime;
    private Runnable updateListener = () -> {};

    public ContainerLoom(InventoryPlayer playerInventory, World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
        this.input.addInventoryChangeListener(inventory -> {
            this.onCraftMatrixChanged(inventory);
            this.updateListener.run();
        });
        this.outputInventory.addInventoryChangeListener(inventory -> this.updateListener.run());
        this.bannerSlot = this.addSlotToContainer(new Slot(this.input, 0, 13, 26) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() instanceof ItemBanner;
            }
        });
        this.dyeSlot = this.addSlotToContainer(new Slot(this.input, 1, 33, 26) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return BannerPatterns116.dyeColor(stack) != null;
            }
        });
        this.patternSlot = this.addSlotToContainer(new Slot(this.input, 2, 23, 45) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() instanceof ItemBannerPattern;
            }
        });
        this.outputSlot = this.addSlotToContainer(new Slot(this.outputInventory, 0, 143, 58) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            @Override
            public ItemStack onTake(EntityPlayer player, ItemStack stack) {
                ContainerLoom.this.bannerSlot.decrStackSize(1);
                ContainerLoom.this.dyeSlot.decrStackSize(1);
                if (!ContainerLoom.this.bannerSlot.getHasStack() || !ContainerLoom.this.dyeSlot.getHasStack()) ContainerLoom.this.selected = 0;
                if (!ContainerLoom.this.world.isRemote) {
                    long time = ContainerLoom.this.world.getTotalWorldTime();
                    if (ContainerLoom.this.lastTakeTime != time) {
                        ContainerLoom.this.world.playSound(null, ContainerLoom.this.pos, PortiumSounds.UI_LOOM_TAKE_RESULT, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        ContainerLoom.this.lastTakeTime = time;
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

    public int getSelectedPattern() {
        return this.selected;
    }

    public Slot getBannerSlot() {
        return this.bannerSlot;
    }

    public Slot getDyeSlot() {
        return this.dyeSlot;
    }

    public Slot getPatternSlot() {
        return this.patternSlot;
    }

    public Slot getOutputSlot() {
        return this.outputSlot;
    }

    @SideOnly(Side.CLIENT)
    public void setUpdateListener(Runnable listener) {
        this.updateListener = listener;
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        if (id > 0 && id <= BannerPatterns116.PATTERN_ITEM_INDEX) {
            this.selected = id;
            this.createOutput();
            return true;
        }
        return false;
    }

    /** How many layers a banner has. */
    public static int patternCount(ItemStack banner) {
        NBTTagCompound tag = banner.getSubCompound("BlockEntityTag");
        return tag != null && tag.hasKey("Patterns", 9) ? tag.getTagList("Patterns", 10).tagCount() : 0;
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        ItemStack banner = this.bannerSlot.getStack();
        ItemStack dye = this.dyeSlot.getStack();
        ItemStack pattern = this.patternSlot.getStack();
        ItemStack output = this.outputSlot.getStack();
        if (output.isEmpty() || !banner.isEmpty() && !dye.isEmpty() && this.selected > 0
                && (this.selected < BannerPatterns116.ORDER.length - BannerPatterns116.WITH_ITEMS || !pattern.isEmpty())) {
            if (!pattern.isEmpty() && pattern.getItem() instanceof ItemBannerPattern) {
                boolean full = !banner.isEmpty() && patternCount(banner) >= 6;
                this.selected = full ? 0 : ((ItemBannerPattern) pattern.getItem()).getPatternIndex();
            }
        } else {
            this.outputSlot.putStack(ItemStack.EMPTY);
            this.selected = 0;
        }
        this.createOutput();
        this.detectAndSendChanges();
    }

    private void createOutput() {
        if (this.selected <= 0) return;
        ItemStack banner = this.bannerSlot.getStack();
        ItemStack dye = this.dyeSlot.getStack();
        ItemStack result = ItemStack.EMPTY;
        EnumDyeColor color = BannerPatterns116.dyeColor(dye);
        if (!banner.isEmpty() && !dye.isEmpty() && color != null && BannerPatterns116.byIndex(this.selected) != null) {
            result = banner.copy();
            result.setCount(1);
            NBTTagCompound blockEntity = result.getOrCreateSubCompound("BlockEntityTag");
            NBTTagList patterns = blockEntity.hasKey("Patterns", 9) ? blockEntity.getTagList("Patterns", 10) : new NBTTagList();
            NBTTagCompound layer = new NBTTagCompound();
            layer.setString("Pattern", BannerPatterns116.ORDER[this.selected]);
            // 1.12 keeps banner colours as dye damage.
            layer.setInteger("Color", color.getDyeDamage());
            patterns.appendTag(layer);
            blockEntity.setTag("Patterns", patterns);
        }
        if (!ItemStack.areItemStacksEqual(result, this.outputSlot.getStack())) this.outputSlot.putStack(result);
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
        return PortiumBlocks.LOOM != null && this.world.getBlockState(this.pos).getBlock() == PortiumBlocks.LOOM
                && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return moved;
        ItemStack stack = slot.getStack();
        moved = stack.copy();
        if (index == this.outputSlot.slotNumber) {
            if (!this.mergeItemStack(stack, 4, 40, true)) return ItemStack.EMPTY;
            slot.onSlotChange(stack, moved);
        } else if (index != this.dyeSlot.slotNumber && index != this.bannerSlot.slotNumber && index != this.patternSlot.slotNumber) {
            if (stack.getItem() instanceof ItemBanner) {
                if (!this.mergeItemStack(stack, this.bannerSlot.slotNumber, this.bannerSlot.slotNumber + 1, false)) return ItemStack.EMPTY;
            } else if (BannerPatterns116.dyeColor(stack) != null) {
                if (!this.mergeItemStack(stack, this.dyeSlot.slotNumber, this.dyeSlot.slotNumber + 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemBannerPattern) {
                if (!this.mergeItemStack(stack, this.patternSlot.slotNumber, this.patternSlot.slotNumber + 1, false)) return ItemStack.EMPTY;
            } else if (index < 31) {
                if (!this.mergeItemStack(stack, 31, 40, false)) return ItemStack.EMPTY;
            } else if (index < 40 && !this.mergeItemStack(stack, 4, 31, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.mergeItemStack(stack, 4, 40, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        if (stack.getCount() == moved.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return moved;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!this.world.isRemote) this.clearContainer(player, this.world, this.input);
    }
}
