package com.michaelsebero.portium.modern.mc.state;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 1.13's {@code net.minecraft.state.StateContainer.Builder}: the properties a block's or fluid's states have, as 1.13
 * lists them in {@code fillStateContainer}. A block's become 1.12's {@link BlockStateContainer} (1.13's state
 * container); a fluid makes its own states (see {@code Fluid}).
 */
public class StateContainer$Builder<O, S> {

    private final O owner;
    private final List<IProperty<?>> properties = new ArrayList<>();

    public StateContainer$Builder(O owner) {
        this.owner = owner;
    }

    @Srg("func_206894_a")
    public StateContainer$Builder<O, S> add(IProperty<?>... properties) {
        this.properties.addAll(Arrays.asList(properties));
        return this;
    }

    public List<IProperty<?>> properties() {
        return this.properties;
    }

    /** 1.12's container for a block's states. */
    public BlockStateContainer build() {
        return new BlockStateContainer((Block) this.owner, this.properties.toArray(new IProperty<?>[0]));
    }
}
