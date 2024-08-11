package com.michaelsebero.portium.modern.mc.particles;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.network.PacketBuffer;

/** 1.13's {@code net.minecraft.particles.BlockParticleData}: a block's crumbs or falling dust. */
public class BlockParticleData implements IParticleData {

    private final ParticleType<BlockParticleData> type;
    private final IBlockState state;

    public BlockParticleData(ParticleType<BlockParticleData> type, IBlockState state) {
        this.type = type;
        this.state = state;
    }

    @Override
    @Srg("func_197554_b")
    public ParticleType<BlockParticleData> getType() {
        return this.type;
    }

    @Srg("func_197584_c")
    public IBlockState getBlockState() {
        return this.state;
    }

    @Override
    @Srg("func_197553_a")
    public void write(PacketBuffer buffer) {
        buffer.writeVarInt(Block.getStateId(this.state));
    }

    @Override
    @Srg("func_197555_a")
    public String getParameters() {
        return this.type.getId() + " " + Block.REGISTRY.getNameForObject(this.state.getBlock());
    }
}
