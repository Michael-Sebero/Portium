package com.michaelsebero.portium.entity.passive;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.ai.EntityAIBreed116;
import com.michaelsebero.portium.entity.ai.EntityAIMoveToBlock116;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.item.Dyes114;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFollowOwner;
import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.ai.EntityAIOcelotAttack;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITargetNonTamed;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * The cat (1.14), split from the ocelot: strays in villages and swamp huts (black), tamed with raw cod or salmon. Eleven
 * coats; a dyeable collar; tame cats sit when told, follow, lie on beds and sit on chests, lit furnaces and beds, curl
 * up by a sleeping owner and may leave a gift by the bed in the morning. Creepers and phantoms keep away from cats.
 */
public class EntityCat extends EntityTameable {

    private static final DataParameter<Integer> CAT_TYPE = EntityDataManager.createKey(EntityCat.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> LYING = EntityDataManager.createKey(EntityCat.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> RELAXED = EntityDataManager.createKey(EntityCat.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> COLLAR_COLOR = EntityDataManager.createKey(EntityCat.class, DataSerializers.VARINT);

    public static final ResourceLocation MORNING_GIFT = new ResourceLocation(Tags.MOD_ID, "gameplay/cat_morning_gift");
    private static final ResourceLocation LOOT = new ResourceLocation(Tags.MOD_ID, "entities/cat");
    public static final int ALL_BLACK = 10;

    private AvoidPlayerGoal avoidPlayerGoal;
    private TemptGoal temptGoal;
    /** Told to sit (1.14's "queued to sit"), as against sitting where it is. */
    private boolean orderedToSit;
    private float lieDownAmount;
    private float lieDownAmountO;
    private float lieDownAmountTail;
    private float lieDownAmountOTail;
    private float relaxStateOneAmount;
    private float relaxStateOneAmountO;

    public EntityCat(World world) {
        super(world);
        this.setSize(0.6F, 0.7F);
    }

    private static boolean isBreedingFood(ItemStack stack) {
        return stack.getItem() == Items.FISH && (stack.getMetadata() == 0 || stack.getMetadata() == 1);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(CAT_TYPE, 1);
        this.dataManager.register(LYING, false);
        this.dataManager.register(RELAXED, false);
        this.dataManager.register(COLLAR_COLOR, EnumDyeColor.RED.getMetadata());
    }

    @Override
    protected void initEntityAI() {
        this.temptGoal = new TemptGoal(0.6D);
        this.tasks.addTask(1, new EntityAISwimming(this));
        this.tasks.addTask(1, new SitGoal());
        this.tasks.addTask(2, new MorningGiftGoal());
        this.tasks.addTask(3, this.temptGoal);
        this.tasks.addTask(5, new LieOnBedGoal(1.1D, 8));
        this.tasks.addTask(6, new EntityAIFollowOwner(this, 1.0D, 10.0F, 5.0F));
        this.tasks.addTask(7, new SitOnBlockGoal(0.8D));
        this.tasks.addTask(8, new EntityAILeapAtTarget(this, 0.3F));
        this.tasks.addTask(9, new EntityAIOcelotAttack(this));
        this.tasks.addTask(10, new EntityAIBreed116(this, 0.8D));
        this.tasks.addTask(11, new EntityAIWanderAvoidWater(this, 0.8D, 1.0000001E-5F));
        this.tasks.addTask(12, new EntityAIWatchClosest(this, EntityPlayer.class, 10.0F));
        this.targetTasks.addTask(1, new EntityAITargetNonTamed<>(this, EntityRabbit.class, false, null));
        this.targetTasks.addTask(1, new EntityAITargetNonTamed<>(this, EntityTurtle.class, false, EntityTurtle.TARGET_DRY_BABY::test));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.30000001192092896D);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
    }

    // ---- State ----

    public ResourceLocation getCatTexture() {
        return new ResourceLocation(Tags.MOD_ID, "textures/entity/cat/" + TYPE_NAMES[this.getCatType()] + ".png");
    }

    private static final String[] TYPE_NAMES = {"tabby", "black", "red", "siamese", "british_shorthair", "calico", "persian", "ragdoll", "white", "jellie",
            "all_black"};

    public int getCatType() {
        int type = this.dataManager.get(CAT_TYPE);
        return type < 0 || type >= TYPE_NAMES.length ? 0 : type;
    }

    public void setCatType(int type) {
        if (type < 0 || type >= 11) type = this.rand.nextInt(10);
        this.dataManager.set(CAT_TYPE, type);
    }

    public void setLying(boolean lying) {
        this.dataManager.set(LYING, lying);
    }

    public boolean isLying() {
        return this.dataManager.get(LYING);
    }

    public void setRelaxed(boolean relaxed) {
        this.dataManager.set(RELAXED, relaxed);
    }

    public boolean isRelaxed() {
        return this.dataManager.get(RELAXED);
    }

    public EnumDyeColor getCollarColor() {
        return EnumDyeColor.byMetadata(this.dataManager.get(COLLAR_COLOR));
    }

    public void setCollarColor(EnumDyeColor color) {
        this.dataManager.set(COLLAR_COLOR, color.getMetadata());
    }

    public boolean isOrderedToSit() {
        return this.orderedToSit;
    }

    public void setOrderedToSit(boolean ordered) {
        this.orderedToSit = ordered;
    }

    public float getLieDownAmount(float partialTicks) {
        return this.lieDownAmountO + (this.lieDownAmount - this.lieDownAmountO) * partialTicks;
    }

    public float getLieDownAmountTail(float partialTicks) {
        return this.lieDownAmountOTail + (this.lieDownAmountTail - this.lieDownAmountOTail) * partialTicks;
    }

    public float getRelaxStateOneAmount(float partialTicks) {
        return this.relaxStateOneAmountO + (this.relaxStateOneAmount - this.relaxStateOneAmountO) * partialTicks;
    }

    // ---- Saving ----

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("CatType", this.getCatType());
        compound.setByte("CollarColor", (byte) this.getCollarColor().getMetadata());
        compound.setBoolean("Sitting", this.orderedToSit);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setCatType(compound.getInteger("CatType"));
        if (compound.hasKey("CollarColor", 99)) this.setCollarColor(EnumDyeColor.byMetadata(compound.getInteger("CollarColor")));
        this.orderedToSit = compound.getBoolean("Sitting");
    }

    // ---- Behaviour ----

    @Override
    public void updateAITasks() {
        if (this.getMoveHelper().isUpdating()) {
            double speed = this.getMoveHelper().getSpeed();
            this.setSneaking(speed == 0.6D);
            this.setSprinting(speed == 1.33D);
        } else {
            this.setSneaking(false);
            this.setSprinting(false);
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.temptGoal != null && this.temptGoal.isRunning() && !this.isTamed() && this.ticksExisted % 100 == 0) {
            this.playSound(PortiumSounds.ENTITY_CAT_BEG_FOR_FOOD, 1.0F, 1.0F);
        }
        if ((this.isLying() || this.isRelaxed()) && this.ticksExisted % 5 == 0) {
            this.playSound(PortiumSounds.ENTITY_CAT_PURR, 0.6F + 0.4F * (this.rand.nextFloat() - this.rand.nextFloat()), 1.0F);
        }
        this.lieDownAmountO = this.lieDownAmount;
        this.lieDownAmountOTail = this.lieDownAmountTail;
        if (this.isLying()) {
            this.lieDownAmount = Math.min(1.0F, this.lieDownAmount + 0.15F);
            this.lieDownAmountTail = Math.min(1.0F, this.lieDownAmountTail + 0.08F);
        } else {
            this.lieDownAmount = Math.max(0.0F, this.lieDownAmount - 0.22F);
            this.lieDownAmountTail = Math.max(0.0F, this.lieDownAmountTail - 0.13F);
        }
        this.relaxStateOneAmountO = this.relaxStateOneAmount;
        if (this.isRelaxed()) this.relaxStateOneAmount = Math.min(1.0F, this.relaxStateOneAmount + 0.1F);
        else this.relaxStateOneAmount = Math.max(0.0F, this.relaxStateOneAmount - 0.13F);
    }

    /** A hiss, for phantoms it scares off. */
    public void hiss() {
        this.playSound(PortiumSounds.ENTITY_CAT_HISS, this.getSoundVolume(), this.getSoundPitch());
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        return target.attackEntityFrom(DamageSource.causeMobDamage(this),
                (float) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        EntityCat kitten = new EntityCat(this.world);
        if (mate instanceof EntityCat) {
            kitten.setCatType(this.rand.nextBoolean() ? this.getCatType() : ((EntityCat) mate).getCatType());
            if (this.isTamed()) {
                kitten.setOwnerId(this.getOwnerId());
                kitten.setTamed(true);
                kitten.setCollarColor(this.rand.nextBoolean() ? this.getCollarColor() : ((EntityCat) mate).getCollarColor());
            }
        }
        return kitten;
    }

    @Override
    public boolean canMateWith(EntityAnimal other) {
        if (!this.isTamed() || !(other instanceof EntityCat)) return false;
        EntityCat cat = (EntityCat) other;
        return cat.isTamed() && other != this && this.isInLove() && other.isInLove();
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        data = super.onInitialSpawn(difficulty, data);
        this.setCatType(this.world.getCurrentMoonPhaseFactor() > 0.9F ? this.rand.nextInt(11) : this.rand.nextInt(10));
        if (isInSwampHut(this.world, new BlockPos(this))) {
            this.setCatType(ALL_BLACK);
            this.enablePersistence();
        }
        return data;
    }

    /** Whether a spot is in a witch hut (where 1.14 places a black cat). */
    public static boolean isInSwampHut(World world, BlockPos pos) {
        if (!(world instanceof WorldServer)) return false;
        net.minecraft.world.gen.IChunkGenerator generator = ((WorldServer) world).getChunkProvider().chunkGenerator;
        return generator.isInsideStructure(world, "Temple", pos) && world.getBiome(pos) instanceof net.minecraft.world.biome.BiomeSwamp;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return isBreedingFood(stack);
    }

    @Override
    protected void consumeItemFromStack(EntityPlayer player, ItemStack stack) {
        if (this.isBreedingItem(stack)) this.playSound(PortiumSounds.ENTITY_CAT_EAT, 1.0F, 1.0F);
        super.consumeItemFromStack(player, stack);
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        Item item = stack.getItem();
        if (this.world.isRemote) {
            if (this.isTamed() && this.isOwner(player)) return true;
            return this.isBreedingItem(stack) && !(this.getHealth() >= this.getMaxHealth() && this.isTamed());
        }
        if (this.isTamed()) {
            if (this.isOwner(player)) {
                EnumDyeColor dye = Dyes114.colorOf(stack);
                if (dye == null) {
                    if (item instanceof ItemFood && this.isBreedingItem(stack) && this.getHealth() < this.getMaxHealth()) {
                        this.consumeItemFromStack(player, stack);
                        this.heal((float) ((ItemFood) item).getHealAmount(stack));
                        return true;
                    }
                    boolean used = super.processInteract(player, hand);
                    if (!used || this.isChild()) this.setOrderedToSit(!this.isOrderedToSit());
                    return used;
                }
                if (dye != this.getCollarColor()) {
                    this.setCollarColor(dye);
                    if (!player.capabilities.isCreativeMode) stack.shrink(1);
                    this.enablePersistence();
                    return true;
                }
            }
        } else if (this.isBreedingItem(stack)) {
            this.consumeItemFromStack(player, stack);
            if (this.rand.nextInt(3) == 0) {
                this.setTamedBy(player);
                this.setOrderedToSit(true);
                this.world.setEntityState(this, (byte) 7);
            } else {
                this.world.setEntityState(this, (byte) 6);
            }
            this.enablePersistence();
            return true;
        }
        boolean used = super.processInteract(player, hand);
        if (used) this.enablePersistence();
        return used;
    }

    @Override
    public float getEyeHeight() {
        return this.height * 0.5F;
    }

    @Override
    protected boolean canDespawn() {
        return !this.isTamed() && this.ticksExisted > 2400;
    }

    @Override
    protected void setupTamedAI() {
        if (this.avoidPlayerGoal == null) this.avoidPlayerGoal = new AvoidPlayerGoal(16.0F, 0.8D, 1.33D);
        this.tasks.removeTask(this.avoidPlayerGoal);
        if (!this.isTamed()) this.tasks.addTask(4, this.avoidPlayerGoal);
    }

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    // ---- Sounds ----

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (!this.isTamed()) return PortiumSounds.ENTITY_CAT_STRAY_AMBIENT;
        if (this.isInLove()) return PortiumSounds.ENTITY_CAT_PURR;
        return this.rand.nextInt(4) == 0 ? PortiumSounds.ENTITY_CAT_PURREOW : PortiumSounds.ENTITY_CAT_AMBIENT;
    }

    @Override
    public int getTalkInterval() {
        return 120;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_CAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_CAT_DEATH;
    }

    // ---- Goals ----

    /** Told to sit: stays put (unless its owner is under attack close by). */
    private class SitGoal extends EntityAIBase {
        SitGoal() {
            this.setMutexBits(5);
        }

        @Override
        public boolean shouldExecute() {
            if (!EntityCat.this.isTamed() || EntityCat.this.isInWater() || !EntityCat.this.onGround) return false;
            EntityLivingBase owner = EntityCat.this.getOwner();
            if (owner == null) return true;
            return (EntityCat.this.getDistanceSq(owner) >= 144.0D || owner.getRevengeTarget() == null) && EntityCat.this.isOrderedToSit();
        }

        @Override
        public void startExecuting() {
            EntityCat.this.getNavigator().clearPath();
            EntityCat.this.setSitting(true);
        }

        @Override
        public void resetTask() {
            EntityCat.this.setSitting(false);
        }
    }

    /** Strays follow raw fish (1.12's tempt task can't spare a favourite player from its startle, as 1.14's does). */
    private class TemptGoal extends EntityAITempt {

        TemptGoal(double speed) {
            super(EntityCat.this, speed, true, new java.util.HashSet<>(java.util.Collections.singletonList(Items.FISH)));
        }

        @Override
        protected boolean isTempting(ItemStack stack) {
            return isBreedingFood(stack);
        }

        @Override
        public boolean shouldExecute() {
            return super.shouldExecute() && !EntityCat.this.isTamed();
        }

    }

    /** Strays keep away from players. */
    private class AvoidPlayerGoal extends EntityAIAvoidEntity<EntityPlayer> {
        AvoidPlayerGoal(float distance, double farSpeed, double nearSpeed) {
            super(EntityCat.this, EntityPlayer.class, EntitySelectors.CAN_AI_TARGET::apply, distance, farSpeed, nearSpeed);
        }

        @Override
        public boolean shouldExecute() {
            return !EntityCat.this.isTamed() && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !EntityCat.this.isTamed() && super.shouldContinueExecuting();
        }
    }

    /** A tame cat goes to its sleeping owner's bed, curls up there, and may leave a gift come morning. */
    private class MorningGiftGoal extends EntityAIBase {
        private EntityPlayer owner;
        private BlockPos bedPos;
        private int tickCounter;

        @Override
        public boolean shouldExecute() {
            if (!EntityCat.this.isTamed() || EntityCat.this.isOrderedToSit()) return false;
            EntityLivingBase living = EntityCat.this.getOwner();
            if (!(living instanceof EntityPlayer)) return false;
            this.owner = (EntityPlayer) living;
            if (!this.owner.isPlayerSleeping() || EntityCat.this.getDistanceSq(this.owner) > 100.0D) return false;
            BlockPos pos = new BlockPos(this.owner);
            IBlockState state = EntityCat.this.world.getBlockState(pos);
            if (!(state.getBlock() instanceof BlockBed)) return false;
            this.bedPos = pos.offset(state.getValue(BlockBed.FACING).getOpposite());
            return !this.anotherCatOnBed();
        }

        private boolean anotherCatOnBed() {
            for (EntityCat cat : EntityCat.this.world.getEntitiesWithinAABB(EntityCat.class, new AxisAlignedBB(this.bedPos).grow(2.0D))) {
                if (cat != EntityCat.this && (cat.isLying() || cat.isRelaxed())) return true;
            }
            return false;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return EntityCat.this.isTamed() && !EntityCat.this.isOrderedToSit() && this.owner != null && this.owner.isPlayerSleeping()
                    && this.bedPos != null && !this.anotherCatOnBed();
        }

        @Override
        public void startExecuting() {
            if (this.bedPos == null) return;
            EntityCat.this.setSitting(false);
            EntityCat.this.getNavigator().tryMoveToXYZ(this.bedPos.getX(), this.bedPos.getY(), this.bedPos.getZ(), 1.100000023841858D);
        }

        @Override
        public void resetTask() {
            EntityCat.this.setLying(false);
            float celestial = EntityCat.this.world.getCelestialAngle(1.0F);
            if (this.owner.getSleepTimer() >= 100 && celestial > 0.77D && celestial < 0.8D && EntityCat.this.world.rand.nextFloat() < 0.7D) {
                this.giveMorningGift();
            }
            this.tickCounter = 0;
            EntityCat.this.setRelaxed(false);
            EntityCat.this.getNavigator().clearPath();
        }

        private void giveMorningGift() {
            Random random = EntityCat.this.getRNG();
            BlockPos here = new BlockPos(EntityCat.this);
            EntityCat.this.attemptTeleport(here.getX() + random.nextInt(11) - 5, here.getY() + random.nextInt(5) - 2, here.getZ() + random.nextInt(11) - 5);
            here = new BlockPos(EntityCat.this);
            WorldServer world = (WorldServer) EntityCat.this.world;
            LootTable table = world.getLootTableManager().getLootTableFromLocation(MORNING_GIFT);
            List<ItemStack> gifts = table.generateLootForPools(random, new LootContext.Builder(world).withLootedEntity(EntityCat.this).build());
            for (ItemStack gift : gifts) {
                world.spawnEntity(new EntityItem(world, here.getX() - MathHelper.sin(EntityCat.this.renderYawOffset * 0.017453292F), here.getY(),
                        here.getZ() + MathHelper.cos(EntityCat.this.renderYawOffset * 0.017453292F), gift));
            }
        }

        @Override
        public void updateTask() {
            if (this.owner == null || this.bedPos == null) return;
            EntityCat.this.setSitting(false);
            EntityCat.this.getNavigator().tryMoveToXYZ(this.bedPos.getX(), this.bedPos.getY(), this.bedPos.getZ(), 1.100000023841858D);
            if (EntityCat.this.getDistanceSq(this.owner) < 2.5D) {
                ++this.tickCounter;
                if (this.tickCounter > 16) {
                    EntityCat.this.setLying(true);
                    EntityCat.this.setRelaxed(false);
                } else {
                    EntityCat.this.faceEntity(this.owner, 45.0F, 45.0F);
                    EntityCat.this.setRelaxed(true);
                }
            } else {
                EntityCat.this.setLying(false);
            }
        }
    }

    /** A tame cat goes and lies on a bed nearby. */
    private class LieOnBedGoal extends EntityAIMoveToBlock116 {
        LieOnBedGoal(double speed, int length) {
            super(EntityCat.this, speed, length, 6);
            this.searchStartY = -2;
            this.setMutexBits(5);
        }

        @Override
        public boolean shouldExecute() {
            return EntityCat.this.isTamed() && !EntityCat.this.isOrderedToSit() && !EntityCat.this.isLying() && super.shouldExecute();
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            EntityCat.this.setSitting(false);
        }

        @Override
        protected int getRunDelay(net.minecraft.entity.EntityCreature creature) {
            return 40;
        }

        @Override
        public void resetTask() {
            super.resetTask();
            EntityCat.this.setLying(false);
        }

        @Override
        public void updateTask() {
            super.updateTask();
            EntityCat.this.setSitting(false);
            if (!this.getIsAboveDestination()) EntityCat.this.setLying(false);
            else if (!EntityCat.this.isLying()) EntityCat.this.setLying(true);
        }

        @Override
        protected boolean shouldMoveTo(World world, BlockPos pos) {
            return world.isAirBlock(pos.up()) && world.getBlockState(pos).getBlock() instanceof BlockBed;
        }
    }

    /** A tame cat sits on a chest nobody has open, a lit furnace or the foot of a bed. */
    private class SitOnBlockGoal extends EntityAIMoveToBlock116 {
        SitOnBlockGoal(double speed) {
            super(EntityCat.this, speed, 8, 1);
        }

        @Override
        public boolean shouldExecute() {
            return EntityCat.this.isTamed() && !EntityCat.this.isOrderedToSit() && super.shouldExecute();
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            EntityCat.this.setSitting(false);
        }

        @Override
        public void resetTask() {
            super.resetTask();
            EntityCat.this.setSitting(false);
        }

        @Override
        public void updateTask() {
            super.updateTask();
            EntityCat.this.setSitting(this.getIsAboveDestination());
        }

        @Override
        protected boolean shouldMoveTo(World world, BlockPos pos) {
            if (!world.isAirBlock(pos.up())) return false;
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() == Blocks.CHEST) {
                TileEntity tile = world.getTileEntity(pos);
                return !(tile instanceof TileEntityChest) || ((TileEntityChest) tile).numPlayersUsing < 1;
            }
            if (state.getBlock() == Blocks.LIT_FURNACE) return true;
            return state.getBlock() instanceof BlockBed && state.getValue(BlockBed.PART) != BlockBed.EnumPartType.HEAD;
        }
    }
}
