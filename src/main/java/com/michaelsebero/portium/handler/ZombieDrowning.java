package com.michaelsebero.portium.handler;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.api.IDrowningZombie;
import com.michaelsebero.portium.entity.monster.EntityDrowned;
import com.michaelsebero.portium.init.PortiumEntities;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.mixin.accessor.EntityLivingAccessor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.function.Function;

/**
 * Zombies drown (1.13): after 30 seconds with its head under water a zombie starts shaking and 15 seconds later it
 * becomes a drowned. Husks become zombies instead. Zombie pigmen and drowned are unaffected.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ZombieDrowning {

    private static final String IN_WATER_TIME = "InWaterTime";
    private static final String CONVERSION_TIME = "DrownedConversionTime";

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!(entity instanceof EntityZombie) || entity.world.isRemote || !entity.isEntityAlive() || !PortiumConfig.mechanics.zombieDrowning) return;
        if (entity instanceof EntityDrowned || entity instanceof EntityPigZombie) return;
        // Without drowned, only husks have something to turn into.
        if (!(entity instanceof EntityHusk) && !PortiumEntities.isRegistered(EntityDrowned.class)) return;
        NBTTagCompound data = entity.getEntityData();
        if (data.hasKey(CONVERSION_TIME)) {
            IDrowningZombie.of((EntityZombie) entity).portium$setDrowning(true);
            int conversion = data.getInteger(CONVERSION_TIME) - 1;
            if (conversion < 0) {
                convert((EntityZombie) entity);
                return;
            }
            data.setInteger(CONVERSION_TIME, conversion);
        } else if (entity.isInsideOfMaterial(Material.WATER)) {
            int inWater = data.getInteger(IN_WATER_TIME) + 1;
            data.setInteger(IN_WATER_TIME, inWater);
            if (inWater >= 600) {
                data.setInteger(CONVERSION_TIME, 300);
                data.removeTag(IN_WATER_TIME);
            }
        } else {
            data.removeTag(IN_WATER_TIME);
        }
    }

    private static void convert(EntityZombie zombie) {
        if (zombie instanceof EntityHusk) {
            replace(zombie, EntityZombie::new, PortiumSounds.ENTITY_HUSK_CONVERTED_TO_ZOMBIE);
        } else {
            replace(zombie, EntityDrowned::new, PortiumSounds.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED);
        }
    }

    private static void replace(EntityZombie old, Function<net.minecraft.world.World, ? extends EntityZombie> factory, SoundEvent sound) {
        EntityZombie replacement = factory.apply(old.world);
        replacement.copyLocationAndAnglesFrom(old);
        replacement.setCanPickUpLoot(old.canPickUpLoot());
        replacement.setBreakDoorsAItask(old.isBreakDoorsTaskSet());
        replacement.setChild(old.isChild());
        replacement.setNoAI(old.isAIDisabled());
        float[] oldHands = ((EntityLivingAccessor) old).portium$getHandDropChances();
        float[] oldArmor = ((EntityLivingAccessor) old).portium$getArmorDropChances();
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack stack = old.getItemStackFromSlot(slot);
            if (stack.isEmpty()) continue;
            replacement.setItemStackToSlot(slot, stack.copy());
            replacement.setDropChance(slot, slot.getSlotType() == EntityEquipmentSlot.Type.HAND ? oldHands[slot.getIndex()] : oldArmor[slot.getIndex()]);
            stack.setCount(0);
        }
        if (old.hasCustomName()) {
            replacement.setCustomNameTag(old.getCustomNameTag());
            replacement.setAlwaysRenderNameTag(old.getAlwaysRenderNameTag());
        }
        if (old.isNoDespawnRequired()) replacement.enablePersistence();
        replacement.setEntityInvulnerable(old.getIsInvulnerable());
        old.world.spawnEntity(replacement);
        old.setDead();
        old.world.playSound(null, new BlockPos(replacement), sound, SoundCategory.HOSTILE, 2.0F,
                (old.getRNG().nextFloat() - old.getRNG().nextFloat()) * 0.2F + 1.0F);
    }

    private ZombieDrowning() {}
}
