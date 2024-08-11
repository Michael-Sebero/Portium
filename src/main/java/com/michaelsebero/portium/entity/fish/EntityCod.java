package com.michaelsebero.portium.entity.fish;

import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

public class EntityCod extends EntityAbstractGroupFish {

    public EntityCod(World world) {
        super(world);
        this.setSize(0.5F, 0.3F);
    }

    @Override
    protected ItemStack getBucket() {
        return new ItemStack(PortiumItems.COD_BUCKET);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return PortiumLootTables.COD;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_COD_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_COD_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return PortiumSounds.ENTITY_COD_FLOP;
    }
}
