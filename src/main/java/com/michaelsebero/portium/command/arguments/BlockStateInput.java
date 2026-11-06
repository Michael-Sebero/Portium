package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.compat.LegacyBlockEntityNbt;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/** A block state to place, with optional block entity data, from {@code setblock}/{@code fill}/{@code clone}. */
public class BlockStateInput {

    private final IBlockState state;
    @Nullable
    private final NBTTagCompound tag;
    /** 1.13's {@code waterlogged} property, kept beside the 1.12 state. */
    private final boolean waterlogged;
    /** The block as the command named it in 1.13's terms, which the block entity data is written for. */
    @Nullable
    private final String modernName;
    private final Map<String, String> modernProperties;

    public BlockStateInput(IBlockState state, @Nullable NBTTagCompound tag) {
        this(state, tag, false, null, Collections.emptyMap());
    }

    public BlockStateInput(IBlockState state, @Nullable NBTTagCompound tag, boolean waterlogged, @Nullable String modernName,
                           Map<String, String> modernProperties) {
        this.state = state;
        this.tag = tag;
        this.waterlogged = waterlogged;
        this.modernName = modernName;
        this.modernProperties = modernProperties;
    }

    public IBlockState getState() {
        return this.state;
    }

    /** Places the block (and its data), as 1.13's {@code BlockStateInput.place}. Changing only the water is a change too. */
    public boolean place(World world, BlockPos pos, int flags) {
        boolean placed = world.setBlockState(pos, this.state, flags);
        boolean watered = Waterlogging.canWaterlog(this.state) && Waterlogging.setWaterlogged(world, pos, this.waterlogged);
        if (!placed && !watered) return false;
        TileEntity tile = world.getTileEntity(pos);
        if (this.tag != null) {
            if (tile != null) {
                NBTTagCompound data = this.legacyTag(tile);
                data.setInteger("x", pos.getX());
                data.setInteger("y", pos.getY());
                data.setInteger("z", pos.getZ());
                tile.readFromNBT(data);
            }
        } else if (tile != null && this.modernName != null) {
            // No data given, but 1.13's block name says what 1.12 keeps in the block entity: a banner's or bed's
            // colour, a head's kind. Those are put on the block entity as it is.
            NBTTagCompound implied = this.legacyTag(tile, new NBTTagCompound());
            implied.removeTag("id");
            if (!implied.isEmpty()) {
                NBTTagCompound data = tile.writeToNBT(new NBTTagCompound());
                data.merge(implied);
                tile.readFromNBT(data);
                tile.markDirty();
            }
        }
        return true;
    }

    /**
     * The command's block entity data in 1.12's layout: written for 1.13, where a banner's colours count like wool's
     * and its base colour is the block's, for instance ({@link LegacyBlockEntityNbt}).
     */
    private NBTTagCompound legacyTag(TileEntity tile) {
        return this.legacyTag(tile, this.tag.copy());
    }

    private NBTTagCompound legacyTag(TileEntity tile, NBTTagCompound modern) {
        if (this.modernName == null) return modern;
        boolean named = modern.hasKey("id", 8);
        if (!named) {
            net.minecraft.util.ResourceLocation id = TileEntity.getKey(tile.getClass());
            if (id == null) return modern;
            modern.setString("id", id.toString());
        }
        NBTTagCompound legacy = LegacyBlockEntityNbt.convert(modern, this.modernName, this.modernProperties);
        if (!named) legacy.removeTag("id");
        return legacy;
    }
}
