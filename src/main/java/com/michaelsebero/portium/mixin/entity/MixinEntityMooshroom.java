package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.entity.IMooshroomType;
import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.item.ItemSuspiciousStew;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.Potion;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

/**
 * 1.14's brown mooshrooms: lightning turns a red mooshroom brown and back; a brown one fed a small flower gives
 * suspicious stew (with that flower's effect) when milked with a bowl; brown ones shear into brown mushrooms; two
 * mooshrooms of a kind rarely (1 in 1024) have a calf of the other.
 */
@Mixin(EntityMooshroom.class)
public abstract class MixinEntityMooshroom extends EntityCow implements IMooshroomType {

    @Unique
    private static final DataParameter<Boolean> PORTIUM_BROWN = EntityDataManager.createKey(EntityMooshroom.class, DataSerializers.BOOLEAN);

    @Unique
    private UUID portium$lastLightning;
    @Unique
    private Potion portium$stewEffect;
    @Unique
    private int portium$stewDuration;

    private MixinEntityMooshroom(World world) {
        super(world);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(PORTIUM_BROWN, false);
    }

    @Override
    public boolean portium$isBrown() {
        return this.dataManager.get(PORTIUM_BROWN);
    }

    @Override
    public void portium$setBrown(boolean brown) {
        this.dataManager.set(PORTIUM_BROWN, brown);
    }

    @Override
    public void onStruckByLightning(EntityLightningBolt bolt) {
        if (!PortiumConfig.mobs.brownMooshrooms) {
            super.onStruckByLightning(bolt);
            return;
        }
        UUID id = bolt.getUniqueID();
        if (!id.equals(this.portium$lastLightning)) {
            this.portium$setBrown(!this.portium$isBrown());
            this.portium$lastLightning = id;
            this.playSound(PortiumSounds.ENTITY_MOOSHROOM_CONVERT, 2.0F, 1.0F);
        }
    }

    @Inject(method = "processInteract", at = @At("HEAD"), cancellable = true)
    private void portium$brownInteract(EntityPlayer player, EnumHand hand, CallbackInfoReturnable<Boolean> cir) {
        if (!PortiumConfig.mobs.brownMooshrooms || !this.portium$isBrown() || this.isChild()) return;
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() == Items.BOWL && this.portium$stewEffect != null && PortiumItems.SUSPICIOUS_STEW != null) {
            ItemStack stew = new ItemStack(PortiumItems.SUSPICIOUS_STEW);
            ItemSuspiciousStew.addEffect(stew, this.portium$stewEffect, this.portium$stewDuration);
            this.portium$stewEffect = null;
            held.shrink(1);
            if (held.isEmpty()) {
                player.setHeldItem(hand, stew);
            } else if (!player.inventory.addItemStackToInventory(stew)) {
                player.dropItem(stew, false);
            }
            this.playSound(PortiumSounds.ENTITY_MOOSHROOM_SUSPICIOUS_MILK, 1.0F, 1.0F);
            cir.setReturnValue(true);
            return;
        }
        Object[] effect = ItemSuspiciousStew.flowerEffect(held);
        if (effect != null && effect[0] instanceof Potion) {
            if (this.portium$stewEffect != null) {
                this.portium$particles(EnumParticleTypes.SMOKE_NORMAL);
            } else {
                this.portium$stewEffect = (Potion) effect[0];
                this.portium$stewDuration = (Integer) effect[1];
                if (!player.capabilities.isCreativeMode) held.shrink(1);
                this.portium$particles(EnumParticleTypes.SPELL_MOB);
                this.playSound(PortiumSounds.ENTITY_MOOSHROOM_EAT, 2.0F, 1.0F);
            }
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void portium$particles(EnumParticleTypes type) {
        if (this.world instanceof WorldServer) {
            ((WorldServer) this.world).spawnParticle(type, this.posX, this.posY + this.height / 2.0F, this.posZ, 4, this.width / 4.0D, this.height / 4.0D,
                    this.width / 4.0D, 0.01D);
        }
    }

    /** Brown mooshrooms shear into brown mushrooms. */
    @Inject(method = "onSheared", at = @At("RETURN"), remap = false)
    private void portium$brownMushrooms(ItemStack item, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos, int fortune,
                                        CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!this.portium$isBrown()) return;
        Item red = Item.getItemFromBlock(Blocks.RED_MUSHROOM);
        for (int i = 0; i < cir.getReturnValue().size(); i++) {
            ItemStack stack = cir.getReturnValue().get(i);
            if (stack.getItem() == red) cir.getReturnValue().set(i, new ItemStack(Blocks.BROWN_MUSHROOM, stack.getCount()));
        }
    }

    @Inject(method = "createChild", at = @At("RETURN"))
    private void portium$calfType(EntityAgeable mate, CallbackInfoReturnable<EntityMooshroom> cir) {
        EntityMooshroom calf = cir.getReturnValue();
        if (calf == null || !(mate instanceof IMooshroomType)) return;
        boolean mine = this.portium$isBrown();
        boolean theirs = ((IMooshroomType) mate).portium$isBrown();
        boolean brown = mine == theirs && this.rand.nextInt(1024) == 0 ? !mine : this.rand.nextBoolean() ? mine : theirs;
        ((IMooshroomType) calf).portium$setBrown(brown);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setString("Type", this.portium$isBrown() ? "brown" : "red");
        if (this.portium$stewEffect != null) {
            tag.setByte("EffectId", (byte) Potion.getIdFromPotion(this.portium$stewEffect));
            tag.setInteger("EffectDuration", this.portium$stewDuration);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.portium$setBrown("brown".equals(tag.getString("Type")));
        if (tag.hasKey("EffectId", 1)) {
            this.portium$stewEffect = Potion.getPotionById(tag.getByte("EffectId"));
            this.portium$stewDuration = tag.getInteger("EffectDuration");
        }
    }
}
