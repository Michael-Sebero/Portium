package com.michaelsebero.portium.tile;

import com.michaelsebero.portium.block.village.BlockLectern;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWrittenBook;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;

/**
 * A lectern's book and the page it's open at (1.14). A written book's text is resolved (selectors, scores) when it's
 * put down, as by whoever put it there, else by "Lectern" with operator rights.
 */
public class TileEntityLectern extends TileEntity {

    private ItemStack book = ItemStack.EMPTY;
    private int page;
    private int pages;

    /** The book as a one-slot inventory, for the reading screen: taking it out leaves the lectern empty. */
    private final IInventory inventory = new IInventory() {
        @Override
        public int getSizeInventory() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return TileEntityLectern.this.book.isEmpty();
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return index == 0 ? TileEntityLectern.this.book : ItemStack.EMPTY;
        }

        @Override
        public ItemStack decrStackSize(int index, int count) {
            if (index != 0) return ItemStack.EMPTY;
            ItemStack taken = TileEntityLectern.this.book.splitStack(count);
            if (TileEntityLectern.this.book.isEmpty()) TileEntityLectern.this.bookRemoved();
            return taken;
        }

        @Override
        public ItemStack removeStackFromSlot(int index) {
            if (index != 0) return ItemStack.EMPTY;
            ItemStack taken = TileEntityLectern.this.book;
            TileEntityLectern.this.book = ItemStack.EMPTY;
            TileEntityLectern.this.bookRemoved();
            return taken;
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {}

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {
            TileEntityLectern.this.markDirty();
        }

        @Override
        public boolean isUsableByPlayer(EntityPlayer player) {
            if (TileEntityLectern.this.world.getTileEntity(TileEntityLectern.this.pos) != TileEntityLectern.this) return false;
            BlockPos pos = TileEntityLectern.this.pos;
            return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D && TileEntityLectern.this.hasBook();
        }

        @Override
        public void openInventory(EntityPlayer player) {}

        @Override
        public void closeInventory(EntityPlayer player) {}

        @Override
        public boolean isItemValidForSlot(int index, ItemStack stack) {
            return false;
        }

        @Override
        public int getField(int id) {
            return 0;
        }

        @Override
        public void setField(int id, int value) {}

        @Override
        public int getFieldCount() {
            return 0;
        }

        @Override
        public void clear() {}

        @Override
        public String getName() {
            return "container.lectern";
        }

        @Override
        public boolean hasCustomName() {
            return false;
        }

        @Override
        public ITextComponent getDisplayName() {
            return new TextComponentTranslation("container.lectern");
        }
    };

    public IInventory getInventory() {
        return this.inventory;
    }

    public ItemStack getBook() {
        return this.book;
    }

    public boolean hasBook() {
        return this.book.getItem() == Items.WRITABLE_BOOK || this.book.getItem() == Items.WRITTEN_BOOK;
    }

    public void setBook(ItemStack stack) {
        this.setBook(stack, null);
    }

    public void setBook(ItemStack stack, @Nullable EntityPlayer player) {
        this.book = this.ensureResolved(stack, player);
        this.page = 0;
        this.pages = pageCount(this.book);
        this.markDirty();
    }

    private void bookRemoved() {
        this.page = 0;
        this.pages = 0;
        BlockLectern.setHasBook(this.world, this.pos, this.world.getBlockState(this.pos), false);
    }

    public int getPage() {
        return this.page;
    }

    /** Turns to a page (clamped as 1.14 clamps it), pulsing redstone if it changed. */
    public void setPage(int page) {
        int clamped = MathHelper.clamp(page, 0, this.pages - 1);
        if (clamped != this.page) {
            this.page = clamped;
            this.markDirty();
            BlockLectern.pulse(this.world, this.pos, this.world.getBlockState(this.pos));
        }
    }

    public int getComparatorSignalLevel() {
        float progress = this.pages > 1 ? (float) this.getPage() / ((float) this.pages - 1.0F) : 1.0F;
        return MathHelper.floor(progress * 14.0F) + (this.hasBook() ? 1 : 0);
    }

    public static int pageCount(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null ? tag.getTagList("pages", 8).tagCount() : 0;
    }

    public void clear() {
        this.setBook(ItemStack.EMPTY);
    }

    // ---- Written books' text ----

    private ItemStack ensureResolved(ItemStack stack, @Nullable EntityPlayer player) {
        if (this.world instanceof WorldServer && stack.getItem() == Items.WRITTEN_BOOK) resolveContents(stack, this.commandSender(player), player);
        return stack;
    }

    /** 1.14's {@code WrittenBookItem.resolveContents}: each page's text with its selectors and scores filled in, once. */
    private static void resolveContents(ItemStack stack, ICommandSender sender, @Nullable Entity entity) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || tag.getBoolean("resolved")) return;
        tag.setBoolean("resolved", true);
        if (!ItemWrittenBook.validBookTagContents(tag)) return;
        NBTTagList pages = tag.getTagList("pages", 8);
        for (int i = 0; i < pages.tagCount(); i++) {
            String text = pages.getStringTagAt(i);
            ITextComponent component;
            try {
                component = ITextComponent.Serializer.fromJsonLenient(text);
                component = TextComponentUtils.processComponent(sender, component, entity);
            } catch (CommandException | RuntimeException e) {
                component = new TextComponentString(text);
            }
            pages.set(i, new NBTTagString(ITextComponent.Serializer.componentToJson(component)));
        }
        tag.setTag("pages", pages);
    }

    /** Who resolves the book: the player who put it down, or "Lectern", either at the lectern with operator rights. */
    private ICommandSender commandSender(@Nullable EntityPlayer player) {
        World world = this.world;
        BlockPos pos = this.pos;
        String name = player == null ? "Lectern" : player.getName();
        ITextComponent displayName = player == null ? new TextComponentString("Lectern") : player.getDisplayName();
        return new ICommandSender() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public ITextComponent getDisplayName() {
                return displayName;
            }

            @Override
            public boolean canUseCommand(int permLevel, String commandName) {
                return permLevel <= 2;
            }

            @Override
            public World getEntityWorld() {
                return world;
            }

            @Override
            public BlockPos getPosition() {
                return pos;
            }

            @Override
            public Vec3d getPositionVector() {
                return new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            }

            @Nullable
            @Override
            public Entity getCommandSenderEntity() {
                return player;
            }

            @Nullable
            @Override
            public MinecraftServer getServer() {
                return world.getMinecraftServer();
            }
        };
    }

    /** Kept through the block's own state changes (Forge replaces a mod's tile entity on any state change). */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, net.minecraft.block.state.IBlockState oldState, net.minecraft.block.state.IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    // ---- Saving ----

    @Override
    public boolean onlyOpsCanSetNbt() {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.book = compound.hasKey("Book", 10) ? new ItemStack(compound.getCompoundTag("Book")) : ItemStack.EMPTY;
        if (this.world != null) this.book = this.ensureResolved(this.book, null);
        this.pages = pageCount(this.book);
        this.page = MathHelper.clamp(compound.getInteger("Page"), 0, this.pages - 1);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (!this.book.isEmpty()) {
            compound.setTag("Book", this.book.writeToNBT(new NBTTagCompound()));
            compound.setInteger("Page", this.page);
        }
        return compound;
    }
}
