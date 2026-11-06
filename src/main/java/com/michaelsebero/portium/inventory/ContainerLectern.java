package com.michaelsebero.portium.inventory;

import com.michaelsebero.portium.tile.TileEntityLectern;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Reading a lectern's book (1.14): one hidden slot with the book, and the page as window property 0. The screen's
 * buttons arrive as {@link #enchantItem} ids: 1 back a page, 2 on a page, 3 take the book, 100 + n go to page n.
 */
public class ContainerLectern extends Container {

    public static final int PREVIOUS_PAGE = 1;
    public static final int NEXT_PAGE = 2;
    public static final int TAKE_BOOK = 3;
    public static final int PAGE_OFFSET = 100;

    private final IInventory lectern;
    /** The lectern on the server; null in the client's copy, which only mirrors what the server sends. */
    @Nullable
    private final TileEntityLectern tile;
    private int page;
    private int sentPage = -1;
    @Nullable
    private Runnable onChange;

    public ContainerLectern(IInventory lectern, @Nullable TileEntityLectern tile) {
        this.lectern = lectern;
        this.tile = tile;
        this.addSlotToContainer(new Slot(lectern, 0, 0, 0) {
            @Override
            public void onSlotChanged() {
                super.onSlotChanged();
                ContainerLectern.this.onCraftMatrixChanged(this.inventory);
            }
        });
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        if (id >= PAGE_OFFSET) {
            this.setPage(id - PAGE_OFFSET);
            return true;
        }
        switch (id) {
            case PREVIOUS_PAGE:
                this.setPage(this.getPage() - 1);
                return true;
            case NEXT_PAGE:
                this.setPage(this.getPage() + 1);
                return true;
            case TAKE_BOOK:
                if (!player.isAllowEdit()) return false;
                ItemStack book = this.lectern.removeStackFromSlot(0);
                this.lectern.markDirty();
                if (!player.inventory.addItemStackToInventory(book)) player.dropItem(book, false);
                return true;
            default:
                return false;
        }
    }

    private void setPage(int page) {
        if (this.tile != null) this.tile.setPage(page);
        else this.page = page;
        this.changed();
    }

    public int getPage() {
        return this.tile != null ? this.tile.getPage() : this.page;
    }

    public ItemStack getBook() {
        return this.lectern.getStackInSlot(0);
    }

    // ---- Syncing the page ----

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendWindowProperty(this, 0, this.getPage());
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int page = this.getPage();
        if (page != this.sentPage) {
            for (IContainerListener listener : this.listeners) listener.sendWindowProperty(this, 0, page);
            this.sentPage = page;
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void updateProgressBar(int id, int data) {
        if (id == 0) {
            this.page = data;
            this.changed();
        }
    }

    // ---- What the client's screen follows ----

    /** The screen to tell when the book or page the server sent changes. */
    public void setOnChange(@Nullable Runnable onChange) {
        this.onChange = onChange;
    }

    private void changed() {
        if (this.onChange != null) this.onChange.run();
    }

    @Override
    public void putStackInSlot(int slotID, ItemStack stack) {
        super.putStackInSlot(slotID, stack);
        this.changed();
    }

    @Override
    public void setAll(List<ItemStack> stacks) {
        super.setAll(stacks);
        this.changed();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.lectern.isUsableByPlayer(player);
    }
}
