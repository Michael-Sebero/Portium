package com.michaelsebero.portium.item;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;

/**
 * The turtle shell (1.13). While the wearer's head is out of water it keeps topping up ten seconds of
 * Water Breathing, so they get ten extra seconds of air after diving.
 */
public class ItemTurtleHelmet extends ItemArmor {

    public static final ArmorMaterial TURTLE = EnumHelper.addArmorMaterial("PORTIUM_TURTLE", Tags.MOD_ID + ":turtle", 25,
            new int[] {2, 5, 6, 2}, 9, PortiumSounds.ITEM_ARMOR_EQUIP_TURTLE, 0.0F);

    public ItemTurtleHelmet() {
        super(TURTLE, 0, EntityEquipmentSlot.HEAD);
        this.setCreativeTab(CreativeTabs.COMBAT);
    }

    @Override
    public void onArmorTick(World world, EntityPlayer player, ItemStack stack) {
        if (!world.isRemote && !player.isInsideOfMaterial(Material.WATER)) {
            player.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 200, 0, false, false));
        }
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        return Tags.MOD_ID + ":textures/models/armor/turtle_layer_1.png";
    }
}
