package com.michaelsebero.portium.entity.monster.piglin;

import com.michaelsebero.portium.entity.Conversions116;
import com.michaelsebero.portium.entity.MobAttack;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.IBrainHolder;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;

/**
 * 1.16's {@code AbstractPiglinEntity}: piglins and piglin brutes. Run by a brain; open doors; turn into zombified
 * piglins (1.12's zombie pigmen) after 15 seconds outside the Nether unless immune. A monster that, as in 1.16, isn't
 * removed in peaceful (so it's an {@link EntityCreature} marked {@link IMob}, not an {@code EntityMob}).
 */
public abstract class EntityAbstractPiglin extends EntityCreature implements IMob, IBrainHolder {

    protected static final DataParameter<Boolean> IMMUNE_TO_ZOMBIFICATION = EntityDataManager.createKey(EntityAbstractPiglin.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> AGGRESSIVE = EntityDataManager.createKey(EntityAbstractPiglin.class, DataSerializers.BOOLEAN);

    protected int timeInOverworld;

    public EntityAbstractPiglin(World world) {
        super(world);
        this.setCanPickUpLoot(true);
        this.experienceValue = 5;
        if (this.getNavigator() instanceof PathNavigateGround) {
            ((PathNavigateGround) this.getNavigator()).setBreakDoors(true);
            ((PathNavigateGround) this.getNavigator()).setEnterDoors(true);
        }
        this.setPathPriority(PathNodeType.DANGER_FIRE, 16.0F);
        this.setPathPriority(PathNodeType.DAMAGE_FIRE, -1.0F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(IMMUNE_TO_ZOMBIFICATION, false);
        this.dataManager.register(AGGRESSIVE, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
    }

    @Override
    public abstract Brain<? extends EntityAbstractPiglin> getBrain();

    /** Whether it joins hunts for hoglins. */
    public abstract boolean canHunt();

    public void setImmuneToZombification(boolean immune) {
        this.dataManager.set(IMMUNE_TO_ZOMBIFICATION, immune);
    }

    protected boolean isImmuneToZombification() {
        return this.dataManager.get(IMMUNE_TO_ZOMBIFICATION);
    }

    public void setAggressive(boolean aggressive) {
        this.dataManager.set(AGGRESSIVE, aggressive);
    }

    public boolean isAggressive() {
        return this.dataManager.get(AGGRESSIVE);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (this.isImmuneToZombification()) compound.setBoolean("IsImmuneToZombification", true);
        compound.setInteger("TimeInOverworld", this.timeInOverworld);
        compound.setTag("Brain", this.getBrain().write());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setImmuneToZombification(compound.getBoolean("IsImmuneToZombification"));
        this.timeInOverworld = compound.getInteger("TimeInOverworld");
        if (compound.hasKey("Brain", 10)) this.getBrain().read(compound.getCompoundTag("Brain"));
    }

    @Override
    public double getYOffset() {
        return this.isChild() ? -0.05D : -0.45D;
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (this.isConverting()) {
            ++this.timeInOverworld;
        } else {
            this.timeInOverworld = 0;
        }
        if (this.timeInOverworld > 300) {
            this.playConvertedSound();
            this.zombify((WorldServer) this.world);
        }
    }

    /** Outside a piglin-safe dimension (the Nether), not immune, and with AI: shaking, about to zombify. */
    public boolean isConverting() {
        return !isPiglinSafe(this.world) && !this.isImmuneToZombification() && !this.isAIDisabled();
    }

    public static boolean isPiglinSafe(World world) {
        return world.provider.getDimensionType() == DimensionType.NETHER || world.provider.isNether();
    }

    protected void zombify(WorldServer world) {
        EntityPigZombie zombified = Conversions116.convertTo(this, EntityPigZombie::new, true);
        if (zombified != null) zombified.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 200, 0));
    }

    public boolean isAdult() {
        return !this.isChild();
    }

    public abstract PiglinAction getAction();

    @Nullable
    @Override
    public EntityLivingBase getAttackTarget() {
        return this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
    }

    /** Holding a sword or tool (1.16's {@code TieredItem}). */
    protected boolean holdsMeleeWeapon() {
        net.minecraft.item.Item item = this.getHeldItemMainhand().getItem();
        return item instanceof ItemSword || item instanceof ItemTool || item instanceof ItemHoe;
    }

    @Override
    public void playLivingSound() {
        if (PiglinTasks.isIdle(this)) super.playLivingSound();
    }

    protected abstract void playConvertedSound();

    @Override
    public boolean attackEntityAsMob(Entity target) {
        return MobAttack.attack(this, target);
    }

    @Override
    public void onLivingUpdate() {
        this.updateArmSwingProgress();
        super.onLivingUpdate();
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.ENTITY_HOSTILE_SWIM;
    }

    @Override
    protected SoundEvent getSplashSound() {
        return SoundEvents.ENTITY_HOSTILE_SPLASH;
    }

    @Override
    protected boolean canDropLoot() {
        return true;
    }
}
