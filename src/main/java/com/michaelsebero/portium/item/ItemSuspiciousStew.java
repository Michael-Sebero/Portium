package com.michaelsebero.portium.item;

import com.michaelsebero.portium.block.plant.BlockPortiumFlower;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.14's suspicious stew: a mushroom stew with a flower in it, giving that flower's effect when eaten (saved as
 * {@code Effects: [{EffectId, EffectDuration}]}, as 1.14 does).
 */
public class ItemSuspiciousStew extends ItemSoup {

    public ItemSuspiciousStew() {
        super(6);
        this.setCreativeTab(null);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("Effects", 9)) {
            NBTTagList effects = tag.getTagList("Effects", 10);
            for (int i = 0; i < effects.tagCount(); ++i) {
                NBTTagCompound effect = effects.getCompoundTagAt(i);
                int duration = effect.hasKey("EffectDuration", 3) ? effect.getInteger("EffectDuration") : 160;
                Potion potion = Potion.getPotionById(effect.getByte("EffectId"));
                if (potion != null && !world.isRemote) entity.addPotionEffect(new PotionEffect(potion, duration));
            }
        }
        return super.onItemUseFinish(stack, world, entity);
    }

    public static void addEffect(ItemStack stack, Potion potion, int duration) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        NBTTagList effects = tag.getTagList("Effects", 10);
        NBTTagCompound effect = new NBTTagCompound();
        effect.setByte("EffectId", (byte) Potion.getIdFromPotion(potion));
        effect.setInteger("EffectDuration", duration);
        effects.appendTag(effect);
        tag.setTag("Effects", effects);
        stack.setTagCompound(tag);
    }

    /** A flower's stew effect and duration in ticks, as 1.14's flowers carry them; null if it isn't a small flower. */
    @Nullable
    public static Object[] flowerEffect(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemBlock)) return null;
        Block block = ((ItemBlock) stack.getItem()).getBlock();
        if (block instanceof BlockPortiumFlower) {
            BlockPortiumFlower flower = (BlockPortiumFlower) block;
            return new Object[]{flower.getStewEffect(), flower.getStewDuration()};
        }
        if (block == Blocks.YELLOW_FLOWER) return new Object[]{MobEffects.SATURATION, 7};
        if (block == Blocks.RED_FLOWER) {
            BlockFlower.EnumFlowerType type = BlockFlower.EnumFlowerType.getType(BlockFlower.EnumFlowerColor.RED, stack.getMetadata());
            switch (type) {
                case POPPY: return new Object[]{MobEffects.NIGHT_VISION, 100};
                case BLUE_ORCHID: return new Object[]{MobEffects.SATURATION, 7};
                case ALLIUM: return new Object[]{MobEffects.FIRE_RESISTANCE, 80};
                case HOUSTONIA: return new Object[]{MobEffects.BLINDNESS, 160};
                case RED_TULIP: case ORANGE_TULIP: case WHITE_TULIP: case PINK_TULIP: return new Object[]{MobEffects.WEAKNESS, 180};
                case OXEYE_DAISY: return new Object[]{MobEffects.REGENERATION, 160};
                default: return null;
            }
        }
        return null;
    }
}
