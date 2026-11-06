package com.michaelsebero.portium.entity.monster;

import com.michaelsebero.portium.entity.ICrossbowUser;
import com.michaelsebero.portium.entity.ai.EntityAICrossbowAttack;
import com.michaelsebero.portium.entity.ai.EntityAIPatrol;
import com.michaelsebero.portium.entity.ai.EntityAIRaiderFindTarget;
import com.michaelsebero.portium.entity.raid.IRaider;
import com.michaelsebero.portium.entity.raid.OminousBanner;
import com.michaelsebero.portium.entity.raid.Raiders116;
import com.michaelsebero.portium.init.PortiumEnchantments;
import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.item.ItemCrossbow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 1.14's pillager: an illager with a crossbow, found in patrols, outposts and raids. Its raider and patrol state comes
 * from {@link AbstractIllager} (Portium's raider mixin), as for 1.12's own illagers.
 */
public class EntityPillager extends AbstractIllager implements IRangedAttackMob, ICrossbowUser {

    private static final DataParameter<Boolean> CHARGING = EntityDataManager.createKey(EntityPillager.class, DataSerializers.BOOLEAN);

    private final InventoryBasic inventory = new InventoryBasic("Items", false, 5);

    public EntityPillager(World world) {
        super(world);
        this.setSize(0.6F, 1.95F);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(2, new EntityAIRaiderFindTarget(this, 10.0F));
        this.tasks.addTask(3, new EntityAICrossbowAttack<>(this, 1.0D, 8.0F));
        this.tasks.addTask(4, new EntityAIPatrol(this, 0.7D, 0.595D));
        this.tasks.addTask(8, new EntityAIWanderAvoidWater(this, 0.6D));
        this.tasks.addTask(9, new EntityAIWatchClosest(this, EntityPlayer.class, 15.0F, 1.0F));
        this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 15.0F));
        // 1.14 passes raiders as the classes not to strike back at; 1.12's list is of classes not to call for help.
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true) {
            @Override
            protected boolean isSuitableTarget(@Nullable EntityLivingBase target, boolean includeInvincibles) {
                return !(target instanceof IRaider) && super.isSuitableTarget(target, includeInvincibles);
            }
        });
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<>(this, EntityVillager.class, false));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<>(this, EntityIronGolem.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.35D);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(24.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(5.0D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(CHARGING, false);
    }

    public boolean isCharging() {
        return this.dataManager.get(CHARGING);
    }

    @Override
    public void setCharging(boolean charging) {
        this.dataManager.set(CHARGING, charging);
    }

    @Nullable
    @Override
    public EntityLivingBase getCrossbowTarget() {
        return this.getAttackTarget();
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.idleTime = 0;
    }

    /** The pose 1.14's illager model draws. */
    public IllagerArmPose116 getArmPose116() {
        if (this.isCharging()) return IllagerArmPose116.CROSSBOW_CHARGE;
        if (this.isHolding(ItemCrossbow.class)) return IllagerArmPose116.CROSSBOW_HOLD;
        if (((IRaider) this).portium$isCelebrating()) return IllagerArmPose116.CELEBRATING;
        return ((IRaider) this).portium$isAggressive() ? IllagerArmPose116.ATTACKING : IllagerArmPose116.NEUTRAL;
    }

    private boolean isHolding(Class<?> type) {
        return type.isInstance(this.getHeldItemMainhand().getItem()) || type.isInstance(this.getHeldItemOffhand().getItem());
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        NBTTagList items = new NBTTagList();
        for (int i = 0; i < this.inventory.getSizeInventory(); ++i) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) items.appendTag(stack.writeToNBT(new NBTTagCompound()));
        }
        tag.setTag("Inventory", items);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        NBTTagList items = tag.getTagList("Inventory", 10);
        for (int i = 0; i < items.tagCount(); ++i) {
            ItemStack stack = new ItemStack(items.getCompoundTagAt(i));
            if (!stack.isEmpty()) this.inventory.addItem(stack);
        }
        this.setCanPickUpLoot(true);
    }

    @Override
    public float getBlockPathWeight(BlockPos pos) {
        IBlockState below = this.world.getBlockState(pos.down());
        return below.getBlock() != Blocks.GRASS && below.getBlock() != Blocks.SAND ? 0.5F - this.world.getLightBrightness(pos) : 10.0F;
    }

    @Override
    public int getMaxSpawnedInChunk() {
        return 1;
    }

    @Override
    public double getYOffset() {
        return -0.45D;
    }

    /**
     * 1.16: pillagers spawning in an outpost need only little block light (no darkness: the outpost spawns them by
     * day), and solid ground; elsewhere they spawn as other monsters do.
     */
    @Override
    public boolean getCanSpawnHere() {
        BlockPos pos = new BlockPos(this.posX, this.getEntityBoundingBox().minY, this.posZ);
        if (!com.michaelsebero.portium.world.gen.jigsaw.JigsawStructures.isInside(this.world, com.michaelsebero.portium.world.gen.jigsaw.JigsawStructures.OUTPOST, pos)) {
            return super.getCanSpawnHere();
        }
        boolean can = this.world.getDifficulty() != net.minecraft.world.EnumDifficulty.PEACEFUL
                && this.world.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, pos) <= 8
                && this.world.getBlockState(pos.down()).canEntitySpawn(this);
        if (Boolean.getBoolean("portium.dev.jigsaw")) com.michaelsebero.portium.Portium.LOGGER.info("Outpost pillager at {}: {}", pos, can);
        return can;
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        this.setEquipmentBasedOnDifficulty(difficulty);
        this.setEnchantmentBasedOnDifficulty(difficulty);
        return super.onInitialSpawn(difficulty, data);
    }

    @Override
    protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
        if (PortiumItems.CROSSBOW != null) this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(PortiumItems.CROSSBOW));
    }

    /** 1.12's weapon and armour enchanting, with 1.14's 1-in-300 Piercing I on the crossbow between them. */
    @Override
    protected void setEnchantmentBasedOnDifficulty(DifficultyInstance difficulty) {
        float chance = difficulty.getClampedAdditionalDifficulty();
        if (!this.getHeldItemMainhand().isEmpty() && this.rand.nextFloat() < 0.25F * chance) {
            this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND,
                    EnchantmentHelper.addRandomEnchantment(this.rand, this.getHeldItemMainhand(), (int) (5.0F + chance * (float) this.rand.nextInt(18)), false));
        }
        if (this.rand.nextInt(300) == 0) {
            ItemStack stack = this.getHeldItemMainhand();
            if (stack.getItem() instanceof ItemCrossbow && PortiumEnchantments.PIERCING != null) {
                Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
                enchantments.putIfAbsent(PortiumEnchantments.PIERCING, 1);
                EnchantmentHelper.setEnchantments(enchantments, stack);
                this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, stack);
            }
        }
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (slot.getSlotType() != EntityEquipmentSlot.Type.ARMOR) continue;
            ItemStack stack = this.getItemStackFromSlot(slot);
            if (!stack.isEmpty() && this.rand.nextFloat() < 0.5F * chance) {
                this.setItemStackToSlot(slot, EnchantmentHelper.addRandomEnchantment(this.rand, stack, (int) (5.0F + chance * (float) this.rand.nextInt(18)), false));
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return PortiumSounds.ENTITY_PILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_PILLAGER_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_PILLAGER_HURT;
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
        ICrossbowUser.performCrossbowAttack(this, 1.6F);
    }

    @Override
    public void setSwingingArms(boolean swingingArms) {}

    @Override
    public void shootCrossbowProjectile(@Nullable EntityLivingBase target, ItemStack crossbow, Entity projectile, float angle) {
        if (target != null) ICrossbowUser.shootAt(this, target, projectile, angle, 1.6F);
    }

    /**
     * Only banners are picked up, the raider way (an ominous banner makes a raid's leader; others are worn like any
     * mob's gear). 1.14's pocketing of white banners in raids comes after its banner check, so never happens.
     */
    @Override
    protected void updateEquipmentIfNeeded(EntityItem itemEntity) {
        ItemStack stack = itemEntity.getItem();
        if (stack.getItem() instanceof ItemBanner && (!OminousBanner.is(stack) || !Raiders116.isRaidActive(this))) super.updateEquipmentIfNeeded(itemEntity);
    }

    @Override
    public boolean replaceItemInInventory(int slot, ItemStack stack) {
        if (super.replaceItemInInventory(slot, stack)) return true;
        int index = slot - 300;
        if (index >= 0 && index < this.inventory.getSizeInventory()) {
            this.inventory.setInventorySlotContents(index, stack);
            return true;
        }
        return false;
    }

    /** Overrides the raider mixin's version on {@link AbstractIllager} (so no {@code @Override} at compile time). */
    public SoundEvent portium$getCelebrationSound() {
        return PortiumSounds.ENTITY_PILLAGER_CELEBRATE;
    }

    @Override
    protected ResourceLocation getLootTable() {
        return PortiumLootTables.PILLAGER;
    }
}
