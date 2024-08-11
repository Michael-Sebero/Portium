package com.michaelsebero.portium.entity.fish;

import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.util.AquaticMobs;
import com.google.common.base.Predicate;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/**
 * The pufferfish (1.13) puffs up when a non-aquatic mob or a player in survival comes close, and stings anything that
 * touches it while puffed, poisoning it. It deflates again once left alone.
 */
public class EntityPufferfish extends EntityAbstractFish {

    private static final DataParameter<Integer> PUFF_STATE = EntityDataManager.createKey(EntityPufferfish.class, DataSerializers.VARINT);
    private static final Predicate<EntityLivingBase> ENEMY = entity -> {
        if (entity == null || AquaticMobs.isAquatic(entity)) return false;
        return !(entity instanceof EntityPlayer) || !((EntityPlayer) entity).isSpectator() && !((EntityPlayer) entity).isCreative();
    };

    private int puffTimer;
    private int deflateTimer;

    public EntityPufferfish(World world) {
        super(world);
        this.updateSize();
    }

    @Override
    protected void initEntityAI() {
        super.initEntityAI();
        this.tasks.addTask(1, new AIPuff(this));
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(PUFF_STATE, 0);
    }

    public int getPuffState() {
        return this.dataManager.get(PUFF_STATE);
    }

    public void setPuffState(int state) {
        this.dataManager.set(PUFF_STATE, state);
        this.updateSize();
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (PUFF_STATE.equals(key)) this.updateSize();
        super.notifyDataManagerChange(key);
    }

    private void updateSize() {
        float scale = getPuffScale(this.getPuffState());
        this.setSize(0.7F * scale, 0.7F * scale);
    }

    private static float getPuffScale(int state) {
        switch (state) {
            case 0: return 0.5F;
            case 1: return 0.7F;
            default: return 1.0F;
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("PuffState", this.getPuffState());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setPuffState(compound.getInteger("PuffState"));
    }

    @Override
    public void onUpdate() {
        if (!this.world.isRemote && this.isEntityAlive() && this.isServerWorld()) {
            int state = this.getPuffState();
            if (this.puffTimer > 0) {
                if (state == 0) {
                    this.playSound(PortiumSounds.ENTITY_PUFFER_FISH_BLOW_UP, this.getSoundVolume(), this.getSoundPitch());
                    this.setPuffState(1);
                } else if (this.puffTimer > 40 && state == 1) {
                    this.playSound(PortiumSounds.ENTITY_PUFFER_FISH_BLOW_UP, this.getSoundVolume(), this.getSoundPitch());
                    this.setPuffState(2);
                }
                ++this.puffTimer;
            } else if (state != 0) {
                if (this.deflateTimer > 60 && state == 2) {
                    this.playSound(PortiumSounds.ENTITY_PUFFER_FISH_BLOW_OUT, this.getSoundVolume(), this.getSoundPitch());
                    this.setPuffState(1);
                } else if (this.deflateTimer > 100 && state == 1) {
                    this.playSound(PortiumSounds.ENTITY_PUFFER_FISH_BLOW_OUT, this.getSoundVolume(), this.getSoundPitch());
                    this.setPuffState(0);
                }
                ++this.deflateTimer;
            }
        }
        super.onUpdate();
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.isEntityAlive() && this.getPuffState() > 0) {
            for (EntityLiving mob : this.world.getEntitiesWithinAABB(EntityLiving.class, this.getEntityBoundingBox().grow(0.3D), ENEMY)) {
                if (mob.isEntityAlive()) this.sting(mob);
            }
        }
    }

    private void sting(EntityLiving mob) {
        int state = this.getPuffState();
        if (mob.attackEntityFrom(DamageSource.causeMobDamage(this), 1 + state)) {
            mob.addPotionEffect(new PotionEffect(MobEffects.POISON, 60 * state, 0));
            this.playSound(PortiumSounds.ENTITY_PUFFER_FISH_STING, 1.0F, 1.0F);
        }
    }

    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        int state = this.getPuffState();
        if (player instanceof EntityPlayerMP && state > 0 && player.attackEntityFrom(DamageSource.causeMobDamage(this), 1 + state)) {
            // 1.13 tells the stung player's client to play the sting sound, wherever they are.
            ((EntityPlayerMP) player).connection.sendPacket(new SPacketSoundEffect(PortiumSounds.ENTITY_PUFFER_FISH_STING, SoundCategory.NEUTRAL,
                    player.posX, player.posY, player.posZ, 1.0F, 1.0F));
            player.addPotionEffect(new PotionEffect(MobEffects.POISON, 60 * state, 0));
        }
    }

    @Override
    protected ItemStack getBucket() {
        return new ItemStack(PortiumItems.PUFFERFISH_BUCKET);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return PortiumLootTables.PUFFERFISH;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_PUFFER_FISH_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_PUFFER_FISH_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return PortiumSounds.ENTITY_PUFFER_FISH_FLOP;
    }

    /** Puffs up while a threat is within two blocks. */
    static class AIPuff extends EntityAIBase {

        private final EntityPufferfish fish;

        AIPuff(EntityPufferfish fish) {
            this.fish = fish;
        }

        @Override
        public boolean shouldExecute() {
            return !this.fish.world.getEntitiesWithinAABB(EntityLivingBase.class, this.fish.getEntityBoundingBox().grow(2.0D), ENEMY).isEmpty();
        }

        @Override
        public void startExecuting() {
            this.fish.puffTimer = 1;
            this.fish.deflateTimer = 0;
        }

        @Override
        public void resetTask() {
            this.fish.puffTimer = 0;
        }
    }
}
