package com.michaelsebero.portium.entity.ai;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/** 1.14's {@code UseItemGoal}: when the condition holds, takes the item in hand and uses it up (wandering traders' potions). */
public class EntityAIUseItem<T extends EntityLiving> extends EntityAIBase {

    private final T user;
    private final ItemStack stack;
    private final Predicate<? super T> condition;
    @Nullable
    private final SoundEvent finishSound;

    public EntityAIUseItem(T user, ItemStack stack, @Nullable SoundEvent finishSound, Predicate<? super T> condition) {
        this.user = user;
        this.stack = stack;
        this.finishSound = finishSound;
        this.condition = condition;
    }

    @Override
    public boolean shouldExecute() {
        return this.condition.test(this.user);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.user.isHandActive();
    }

    @Override
    public void startExecuting() {
        this.user.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, this.stack.copy());
        this.user.setActiveHand(EnumHand.MAIN_HAND);
    }

    @Override
    public void resetTask() {
        this.user.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
        if (this.finishSound != null) this.user.playSound(this.finishSound, 1.0F, this.user.getRNG().nextFloat() * 0.2F + 0.9F);
    }
}
