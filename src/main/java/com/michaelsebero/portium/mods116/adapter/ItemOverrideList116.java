package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Collections;

/**
 * What a 1.16 mod's item override lists extend in place of 1.16's: 1.12's, whose bare constructor is private (Patchouli's
 * book model makes one, so a book shows its own model). 1.12 asks it for the model through Forge 1.12's
 * {@code handleItemState}, which answers with 1.16's {@code getOverrideModel}.
 */
@SideOnly(Side.CLIENT)
public class ItemOverrideList116 extends ItemOverrideList {

    public ItemOverrideList116() {
        super(Collections.emptyList());
    }

    /** 1.16's: the model to draw for the stack; the model as it is unless a mod's list says otherwise. */
    @Srg("func_239290_a_")
    @Nullable
    public IBakedModel getOverrideModel(IBakedModel model, ItemStack stack, @Nullable WorldClient world, @Nullable EntityLivingBase entity) {
        return model;
    }

    @Override
    public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
        IBakedModel model = getOverrideModel(originalModel, stack, world instanceof WorldClient ? (WorldClient) world : null, entity);
        return model != null ? model : originalModel;
    }
}
