package com.michaelsebero.portium.entity.merchant;

import com.michaelsebero.portium.entity.ai.EntityAIUseItem;
import com.michaelsebero.portium.entity.monster.EntityPillager;
import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.item.IDrinkSoundEntity;
import com.michaelsebero.portium.merchant.MerchantOffer;
import com.michaelsebero.portium.merchant.MerchantOffers;
import com.michaelsebero.portium.merchant.MerchantScreens;
import com.michaelsebero.portium.merchant.trade.ITrade;
import com.michaelsebero.portium.merchant.trade.WanderingTraderTrades;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityAIWatchClosest2;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityEvoker;
import net.minecraft.entity.monster.EntityIllusionIllager;
import net.minecraft.entity.monster.EntityVex;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.potion.PotionUtils;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.14's wandering trader: sells odds and ends from other biomes, comes with two trader llamas, drinks invisibility at
 * night and milk at dawn, heads for the village meeting point it was sent to, and leaves after its time is up.
 */
public class EntityWanderingTrader extends EntityAbstractVillager116 implements IDrinkSoundEntity {

    @Nullable
    private BlockPos wanderTarget;
    private int despawnDelay;

    public EntityWanderingTrader(World world) {
        super(world);
        this.setSize(0.6F, 1.95F);
        this.forceSpawn = true;
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(0, new EntityAIUseItem<>(this, PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.INVISIBILITY),
                PortiumSounds.ENTITY_WANDERING_TRADER_DISAPPEARED, trader -> !trader.world.isDaytime() && !trader.isInvisible()));
        this.tasks.addTask(0, new EntityAIUseItem<>(this, new ItemStack(Items.MILK_BUCKET), PortiumSounds.ENTITY_WANDERING_TRADER_REAPPEARED,
                trader -> trader.world.isDaytime() && trader.isInvisible()));
        this.tasks.addTask(1, new EntityAITradeWithPlayer116(this));
        this.tasks.addTask(1, new EntityAIAvoidEntity<>(this, EntityZombie.class, 8.0F, 0.5D, 0.5D));
        this.tasks.addTask(1, new EntityAIAvoidEntity<>(this, EntityEvoker.class, 12.0F, 0.5D, 0.5D));
        this.tasks.addTask(1, new EntityAIAvoidEntity<>(this, EntityVindicator.class, 8.0F, 0.5D, 0.5D));
        this.tasks.addTask(1, new EntityAIAvoidEntity<>(this, EntityVex.class, 8.0F, 0.5D, 0.5D));
        this.tasks.addTask(1, new EntityAIAvoidEntity<>(this, EntityPillager.class, 15.0F, 0.5D, 0.5D));
        this.tasks.addTask(1, new EntityAIAvoidEntity<>(this, EntityIllusionIllager.class, 12.0F, 0.5D, 0.5D));
        this.tasks.addTask(1, new EntityAIPanic(this, 0.5D));
        this.tasks.addTask(1, new EntityAILookAtCustomer116(this));
        this.tasks.addTask(2, new MoveToWanderTarget(2.0D, 0.35D));
        this.tasks.addTask(4, new EntityAIMoveTowardsRestriction(this, 0.35D));
        this.tasks.addTask(8, new EntityAIWanderAvoidWater(this, 0.35D));
        this.tasks.addTask(9, new EntityAIWatchClosest2(this, EntityPlayer.class, 3.0F, 1.0F));
        this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 8.0F));
    }

    @Nullable
    @Override
    public EntityAgeable createChild(EntityAgeable other) {
        return null;
    }

    @Override
    public boolean hasXPBar() {
        return false;
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.getItem() != Items.SPAWN_EGG && this.isEntityAlive() && !this.hasCustomer() && !this.isChild()) {
            if (hand == EnumHand.MAIN_HAND) player.addStat(StatList.TALKED_TO_VILLAGER);
            if (this.getOffers().isEmpty()) return true;
            if (!this.world.isRemote) {
                this.setCustomer(player);
                MerchantScreens.open(player, this, 1);
            }
            return true;
        }
        return super.processInteract(player, hand);
    }

    @Override
    protected void populateTradeData() {
        MerchantOffers offers = this.getOffers();
        this.addTrades(offers, WanderingTraderTrades.GENERIC, 5);
        ITrade rare = WanderingTraderTrades.RARE[this.rand.nextInt(WanderingTraderTrades.RARE.length)];
        MerchantOffer offer = rare.getOffer(this, this.rand);
        if (offer != null) offers.add(offer);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("DespawnDelay", this.despawnDelay);
        if (this.wanderTarget != null) tag.setTag("WanderTarget", NBTUtil.createPosTag(this.wanderTarget));
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("DespawnDelay", 99)) this.despawnDelay = tag.getInteger("DespawnDelay");
        if (tag.hasKey("WanderTarget", 10)) this.wanderTarget = NBTUtil.getPosFromTag(tag.getCompoundTag("WanderTarget"));
        this.setGrowingAge(Math.max(0, this.getGrowingAge()));
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    protected void onVillagerTrade(MerchantOffer offer) {
        if (offer.getDoesRewardExp()) {
            int xp = 3 + this.rand.nextInt(4);
            this.world.spawnEntity(new EntityXPOrb(this.world, this.posX, this.posY + 0.5D, this.posZ, xp));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.hasCustomer() ? PortiumSounds.ENTITY_WANDERING_TRADER_TRADE : PortiumSounds.ENTITY_WANDERING_TRADER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_WANDERING_TRADER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_WANDERING_TRADER_DEATH;
    }

    @Override
    public SoundEvent portium$getDrinkSound(ItemStack stack) {
        return stack.getItem() == Items.MILK_BUCKET ? PortiumSounds.ENTITY_WANDERING_TRADER_DRINK_MILK : PortiumSounds.ENTITY_WANDERING_TRADER_DRINK_POTION;
    }

    @Override
    protected SoundEvent getVillagerYesNoSound(boolean yes) {
        return yes ? PortiumSounds.ENTITY_WANDERING_TRADER_YES : PortiumSounds.ENTITY_WANDERING_TRADER_NO;
    }

    @Override
    public SoundEvent getYesSound() {
        return PortiumSounds.ENTITY_WANDERING_TRADER_YES;
    }

    public void setDespawnDelay(int delay) {
        this.despawnDelay = delay;
    }

    public int getDespawnDelay() {
        return this.despawnDelay;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote && this.despawnDelay > 0 && !this.hasCustomer() && --this.despawnDelay == 0) this.setDead();
    }

    public void setWanderTarget(@Nullable BlockPos target) {
        this.wanderTarget = target;
    }

    @Nullable
    private BlockPos getWanderTarget() {
        return this.wanderTarget;
    }

    @Override
    protected ResourceLocation getLootTable() {
        return PortiumLootTables.WANDERING_TRADER;
    }

    /** 1.14's {@code MoveToGoal}: heads for the wander target (in 10-block legs when far), until within 2 blocks. */
    private class MoveToWanderTarget extends EntityAIBase {

        private final double maxDistance;
        private final double speed;

        MoveToWanderTarget(double maxDistance, double speed) {
            this.maxDistance = maxDistance;
            this.speed = speed;
            this.setMutexBits(1);
        }

        @Override
        public void resetTask() {
            EntityWanderingTrader.this.setWanderTarget(null);
            EntityWanderingTrader.this.navigator.clearPath();
        }

        @Override
        public boolean shouldExecute() {
            BlockPos target = EntityWanderingTrader.this.getWanderTarget();
            return target != null && this.isFartherThan(target, this.maxDistance);
        }

        @Override
        public void updateTask() {
            BlockPos target = EntityWanderingTrader.this.getWanderTarget();
            EntityWanderingTrader trader = EntityWanderingTrader.this;
            if (target == null || !trader.navigator.noPath()) return;
            if (this.isFartherThan(target, 10.0D)) {
                Vec3d direction = new Vec3d(target.getX() - trader.posX, target.getY() - trader.posY, target.getZ() - trader.posZ).normalize();
                Vec3d step = direction.scale(10.0D).add(trader.posX, trader.posY, trader.posZ);
                trader.navigator.tryMoveToXYZ(step.x, step.y, step.z, this.speed);
            } else {
                trader.navigator.tryMoveToXYZ(target.getX(), target.getY(), target.getZ(), this.speed);
            }
        }

        /** 1.14's {@code !withinDistance}: the block's centre is at least that far. */
        private boolean isFartherThan(BlockPos target, double distance) {
            EntityWanderingTrader trader = EntityWanderingTrader.this;
            return target.distanceSqToCenter(trader.posX, trader.posY, trader.posZ) >= distance * distance;
        }
    }
}
