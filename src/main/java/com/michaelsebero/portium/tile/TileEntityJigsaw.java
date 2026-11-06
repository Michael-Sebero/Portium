package com.michaelsebero.portium.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

/**
 * 1.16's jigsaw block entity: its name, the name it joins ({@code target}), the template pool it draws from, the block
 * it turns into once the structure is placed, and whether a vertical joint may turn ({@code rollable}) or not
 * ({@code aligned}); saved under 1.16's keys, so templates made either way read the same.
 */
public class TileEntityJigsaw extends TileEntity {

    public static final String EMPTY = "minecraft:empty";

    private String name = EMPTY;
    private String target = EMPTY;
    private String pool = EMPTY;
    private String finalState = "minecraft:air";
    private boolean rollable = true;

    public String getJigsawName() {
        return this.name;
    }

    public String getTarget() {
        return this.target;
    }

    public String getPool() {
        return this.pool;
    }

    public String getFinalState() {
        return this.finalState;
    }

    public boolean isRollable() {
        return this.rollable;
    }

    public void set(String name, String target, String pool, String finalState, boolean rollable) {
        this.name = name;
        this.target = target;
        this.pool = pool;
        this.finalState = finalState;
        this.rollable = rollable;
        this.markDirty();
        if (this.world != null) {
            net.minecraft.block.state.IBlockState state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 3);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("name", this.name);
        tag.setString("target", this.target);
        tag.setString("pool", this.pool);
        tag.setString("final_state", this.finalState);
        tag.setString("joint", this.rollable ? "rollable" : "aligned");
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.name = tag.hasKey("name", 8) ? tag.getString("name") : EMPTY;
        this.target = tag.hasKey("target", 8) ? tag.getString("target") : EMPTY;
        this.pool = tag.hasKey("pool", 8) ? tag.getString("pool") : EMPTY;
        this.finalState = tag.hasKey("final_state", 8) ? tag.getString("final_state") : "minecraft:air";
        this.rollable = !"aligned".equals(tag.getString("joint"));
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        this.readFromNBT(packet.getNbtCompound());
    }
}
