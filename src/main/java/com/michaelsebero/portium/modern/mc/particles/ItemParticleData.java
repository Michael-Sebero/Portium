package com.michaelsebero.portium.modern.mc.particles;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

/** 1.13's {@code net.minecraft.particles.ItemParticleData}: an item's crumbs. */
public class ItemParticleData implements IParticleData {

    private final ParticleType<ItemParticleData> type;
    private final ItemStack stack;

    public ItemParticleData(ParticleType<ItemParticleData> type, ItemStack stack) {
        this.type = type;
        this.stack = stack;
    }

    @Override
    @Srg("func_197554_b")
    public ParticleType<ItemParticleData> getType() {
        return this.type;
    }

    @Srg("func_197556_c")
    public ItemStack getItemStack() {
        return this.stack;
    }

    @Override
    @Srg("func_197553_a")
    public void write(PacketBuffer buffer) {
        buffer.writeItemStack(this.stack);
    }

    @Override
    @Srg("func_197555_a")
    public String getParameters() {
        return this.type.getId() + " " + Item.REGISTRY.getNameForObject(this.stack.getItem());
    }
}
