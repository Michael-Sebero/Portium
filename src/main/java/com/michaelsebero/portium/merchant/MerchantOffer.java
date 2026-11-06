package com.michaelsebero.portium.merchant;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.MathHelper;

/**
 * 1.14's {@code MerchantOffer}: a trade with uses, an xp reward, and a price that moves with demand, a price multiplier
 * and a special price (gossip, Hero of the Village). Saved under 1.14's NBT names, a superset of 1.12's recipe tags.
 */
public class MerchantOffer {

    private final ItemStack buyingStackFirst;
    private final ItemStack buyingStackSecond;
    private final ItemStack sellingStack;
    private int uses;
    private final int maxUses;
    private boolean doesRewardExp = true;
    private int specialPrice;
    private int demand;
    private float priceMultiplier;
    private int givenExp = 1;

    public MerchantOffer(NBTTagCompound tag) {
        this.buyingStackFirst = new ItemStack(tag.getCompoundTag("buy"));
        this.buyingStackSecond = new ItemStack(tag.getCompoundTag("buyB"));
        this.sellingStack = new ItemStack(tag.getCompoundTag("sell"));
        this.uses = tag.getInteger("uses");
        this.maxUses = tag.hasKey("maxUses", 99) ? tag.getInteger("maxUses") : 4;
        if (tag.hasKey("rewardExp", 1)) this.doesRewardExp = tag.getBoolean("rewardExp");
        if (tag.hasKey("xp", 3)) this.givenExp = tag.getInteger("xp");
        if (tag.hasKey("priceMultiplier", 5)) this.priceMultiplier = tag.getFloat("priceMultiplier");
        this.specialPrice = tag.getInteger("specialPrice");
        this.demand = tag.getInteger("demand");
    }

    public MerchantOffer(ItemStack buying, ItemStack selling, int maxUses, int givenExp, float priceMultiplier) {
        this(buying, ItemStack.EMPTY, selling, maxUses, givenExp, priceMultiplier);
    }

    public MerchantOffer(ItemStack buyingFirst, ItemStack buyingSecond, ItemStack selling, int maxUses, int givenExp, float priceMultiplier) {
        this(buyingFirst, buyingSecond, selling, 0, maxUses, givenExp, priceMultiplier);
    }

    public MerchantOffer(ItemStack buyingFirst, ItemStack buyingSecond, ItemStack selling, int uses, int maxUses, int givenExp, float priceMultiplier) {
        this(buyingFirst, buyingSecond, selling, uses, maxUses, givenExp, priceMultiplier, 0);
    }

    public MerchantOffer(ItemStack buyingFirst, ItemStack buyingSecond, ItemStack selling, int uses, int maxUses, int givenExp, float priceMultiplier, int demand) {
        this.buyingStackFirst = buyingFirst;
        this.buyingStackSecond = buyingSecond;
        this.sellingStack = selling;
        this.uses = uses;
        this.maxUses = maxUses;
        this.givenExp = givenExp;
        this.priceMultiplier = priceMultiplier;
        this.demand = demand;
    }

    public ItemStack getBuyingStackFirst() {
        return this.buyingStackFirst;
    }

    /** The first price after demand and special price: never below 1 or above a stack. */
    public ItemStack getDiscountedBuyingStackFirst() {
        int count = this.buyingStackFirst.getCount();
        ItemStack stack = this.buyingStackFirst.copy();
        int demandBonus = Math.max(0, MathHelper.floor((float) (count * this.demand) * this.priceMultiplier));
        stack.setCount(MathHelper.clamp(count + demandBonus + this.specialPrice, 1, this.buyingStackFirst.getItem().getItemStackLimit(this.buyingStackFirst)));
        return stack;
    }

    public ItemStack getBuyingStackSecond() {
        return this.buyingStackSecond;
    }

    public ItemStack getSellingStack() {
        return this.sellingStack;
    }

    /** Restocking: more uses than left over raises demand (and the price), fewer lowers it. */
    public void calculateDemand() {
        this.demand = this.demand + this.uses - (this.maxUses - this.uses);
    }

    public ItemStack getCopyOfSellingStack() {
        return this.sellingStack.copy();
    }

    public int getUses() {
        return this.uses;
    }

    public void resetUses() {
        this.uses = 0;
    }

    public int getMaxUses() {
        return this.maxUses;
    }

    public void increaseUses() {
        ++this.uses;
    }

    public int getDemand() {
        return this.demand;
    }

    public void increaseSpecialPrice(int amount) {
        this.specialPrice += amount;
    }

    public void resetSpecialPrice() {
        this.specialPrice = 0;
    }

    public int getSpecialPrice() {
        return this.specialPrice;
    }

    public void setSpecialPrice(int specialPrice) {
        this.specialPrice = specialPrice;
    }

    public float getPriceMultiplier() {
        return this.priceMultiplier;
    }

    public int getGivenExp() {
        return this.givenExp;
    }

    public boolean hasNoUsesLeft() {
        return this.uses >= this.maxUses;
    }

    public void makeUnavailable() {
        this.uses = this.maxUses;
    }

    public boolean hasBeenUsed() {
        return this.uses > 0;
    }

    public boolean getDoesRewardExp() {
        return this.doesRewardExp;
    }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("buy", this.buyingStackFirst.writeToNBT(new NBTTagCompound()));
        tag.setTag("sell", this.sellingStack.writeToNBT(new NBTTagCompound()));
        tag.setTag("buyB", this.buyingStackSecond.writeToNBT(new NBTTagCompound()));
        tag.setInteger("uses", this.uses);
        tag.setInteger("maxUses", this.maxUses);
        tag.setBoolean("rewardExp", this.doesRewardExp);
        tag.setInteger("xp", this.givenExp);
        tag.setFloat("priceMultiplier", this.priceMultiplier);
        tag.setInteger("specialPrice", this.specialPrice);
        tag.setInteger("demand", this.demand);
        return tag;
    }

    public boolean matches(ItemStack first, ItemStack second) {
        ItemStack price = this.getDiscountedBuyingStackFirst();
        return equalIgnoringDamage(first, price) && first.getCount() >= price.getCount()
                && equalIgnoringDamage(second, this.buyingStackSecond) && second.getCount() >= this.buyingStackSecond.getCount();
    }

    /** Same item (any durability), and holding at least the wanted stack's tags. */
    private static boolean equalIgnoringDamage(ItemStack given, ItemStack wanted) {
        if (wanted.isEmpty() && given.isEmpty()) return true;
        if (given.isEmpty() || wanted.isEmpty() || !given.isItemEqualIgnoreDurability(wanted)) return false;
        return !wanted.hasTagCompound() || given.hasTagCompound() && NBTUtil.areNBTEquals(wanted.getTagCompound(), given.getTagCompound(), false);
    }

    /** Takes the price from the two payment stacks, if they pay it. */
    public boolean doTransaction(ItemStack first, ItemStack second) {
        if (!this.matches(first, second)) return false;
        first.shrink(this.getDiscountedBuyingStackFirst().getCount());
        if (!this.buyingStackSecond.isEmpty()) second.shrink(this.buyingStackSecond.getCount());
        return true;
    }
}
