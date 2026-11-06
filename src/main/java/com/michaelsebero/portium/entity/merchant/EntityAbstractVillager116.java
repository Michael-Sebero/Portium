package com.michaelsebero.portium.entity.merchant;

import com.michaelsebero.portium.merchant.IMerchant116;
import com.michaelsebero.portium.merchant.MerchantOffer;
import com.michaelsebero.portium.merchant.MerchantOffers;
import com.michaelsebero.portium.merchant.trade.ITrade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.INpc;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** 1.14's {@code AbstractVillagerEntity}: a merchant mob with a customer, offers, a small inventory and head shaking. */
public abstract class EntityAbstractVillager116 extends EntityAgeable implements INpc, IMerchant116 {

    private static final DataParameter<Integer> SHAKE_HEAD_TICKS = EntityDataManager.createKey(EntityAbstractVillager116.class, DataSerializers.VARINT);

    @Nullable
    private EntityPlayer customer;
    @Nullable
    protected MerchantOffers offers;
    private final InventoryBasic villagerInventory = new InventoryBasic("Items", false, 8);

    public EntityAbstractVillager116(World world) {
        super(world);
        this.setPathPriority(PathNodeType.DANGER_FIRE, 16.0F);
        this.setPathPriority(PathNodeType.DAMAGE_FIRE, -1.0F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(SHAKE_HEAD_TICKS, 0);
    }

    public int getShakeHeadTicks() {
        return this.dataManager.get(SHAKE_HEAD_TICKS);
    }

    public void setShakeHeadTicks(int ticks) {
        this.dataManager.set(SHAKE_HEAD_TICKS, ticks);
    }

    @Override
    public int getXp() {
        return 0;
    }

    @Override
    public float getEyeHeight() {
        return this.isChild() ? 0.81F : 1.62F;
    }

    @Override
    public void setCustomer(@Nullable EntityPlayer player) {
        this.customer = player;
    }

    @Nullable
    @Override
    public EntityPlayer getCustomer() {
        return this.customer;
    }

    public boolean hasCustomer() {
        return this.customer != null;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = new MerchantOffers();
            this.populateTradeData();
        }
        return this.offers;
    }

    @Override
    public void setClientSideOffers(@Nullable MerchantOffers offers) {}

    @Override
    public void setXP(int xp) {}

    @Override
    public void onTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.livingSoundTime = -this.getTalkInterval();
        this.onVillagerTrade(offer);
    }

    protected abstract void onVillagerTrade(MerchantOffer offer);

    @Override
    public boolean hasXPBar() {
        return true;
    }

    @Override
    public void verifySellingItem(ItemStack stack) {
        if (!this.world.isRemote && this.livingSoundTime > -this.getTalkInterval() + 20) {
            this.livingSoundTime = -this.getTalkInterval();
            this.playSound(this.getVillagerYesNoSound(!stack.isEmpty()), this.getSoundVolume(), this.getSoundPitch());
        }
    }

    @Override
    public SoundEvent getYesSound() {
        return SoundEvents.ENTITY_VILLAGER_YES;
    }

    protected SoundEvent getVillagerYesNoSound(boolean yes) {
        return yes ? SoundEvents.ENTITY_VILLAGER_YES : SoundEvents.ENTITY_VILLAGER_NO;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        MerchantOffers offers = this.getOffers();
        if (!offers.isEmpty()) tag.setTag("Offers", offers.write());
        NBTTagList items = new NBTTagList();
        for (int i = 0; i < this.villagerInventory.getSizeInventory(); ++i) {
            ItemStack stack = this.villagerInventory.getStackInSlot(i);
            if (!stack.isEmpty()) items.appendTag(stack.writeToNBT(new NBTTagCompound()));
        }
        tag.setTag("Inventory", items);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("Offers", 10)) this.offers = new MerchantOffers(tag.getCompoundTag("Offers"));
        NBTTagList items = tag.getTagList("Inventory", 10);
        for (int i = 0; i < items.tagCount(); ++i) {
            ItemStack stack = new ItemStack(items.getCompoundTagAt(i));
            if (!stack.isEmpty()) this.villagerInventory.addItem(stack);
        }
    }

    @Nullable
    @Override
    public Entity changeDimension(int dimension, net.minecraftforge.common.util.ITeleporter teleporter) {
        this.setCustomer(null);
        return super.changeDimension(dimension, teleporter);
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        this.setCustomer(null);
    }

    protected void spawnParticles(EnumParticleTypes particle) {
        for (int i = 0; i < 5; ++i) {
            this.world.spawnParticle(particle, this.posX + (this.rand.nextDouble() * 2.0D - 1.0D) * this.width, this.posY + 1.0D + this.rand.nextDouble() * this.height,
                    this.posZ + (this.rand.nextDouble() * 2.0D - 1.0D) * this.width, this.rand.nextGaussian() * 0.02D, this.rand.nextGaussian() * 0.02D,
                    this.rand.nextGaussian() * 0.02D);
        }
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return false;
    }

    public InventoryBasic getVillagerInventory() {
        return this.villagerInventory;
    }

    @Override
    public boolean replaceItemInInventory(int slot, ItemStack stack) {
        if (super.replaceItemInInventory(slot, stack)) return true;
        int index = slot - 300;
        if (index >= 0 && index < this.villagerInventory.getSizeInventory()) {
            this.villagerInventory.setInventorySlotContents(index, stack);
            return true;
        }
        return false;
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    protected abstract void populateTradeData();

    /** Adds {@code count} different trades picked at random (all of them if there are no more). */
    protected void addTrades(MerchantOffers offers, ITrade[] trades, int count) {
        Set<Integer> picked = new HashSet<>();
        if (trades.length > count) {
            while (picked.size() < count) picked.add(this.rand.nextInt(trades.length));
        } else {
            for (int i = 0; i < trades.length; ++i) picked.add(i);
        }
        for (Integer index : picked) {
            MerchantOffer offer = trades[index].getOffer(this, this.rand);
            if (offer != null) offers.add(offer);
        }
    }

    protected Random random() {
        return this.rand;
    }
}
