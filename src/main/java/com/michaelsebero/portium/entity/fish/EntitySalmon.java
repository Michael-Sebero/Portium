package com.michaelsebero.portium.entity.fish;

import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

public class EntitySalmon extends EntityAbstractGroupFish {

    public EntitySalmon(World world) {
        super(world);
        this.setSize(0.7F, 0.4F);
    }

    @Override
    public int getMaxGroupSize() {
        return 5;
    }

    @Override
    protected ItemStack getBucket() {
        return new ItemStack(PortiumItems.SALMON_BUCKET);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return PortiumLootTables.SALMON;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_SALMON_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_SALMON_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return PortiumSounds.ENTITY_SALMON_FLOP;
    }
}
