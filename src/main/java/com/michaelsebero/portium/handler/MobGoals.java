package com.michaelsebero.portium.handler;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.ai.EntityAIBreakTurtleEgg;
import com.michaelsebero.portium.entity.ai.EntityAISquidFlee;
import com.michaelsebero.portium.entity.monster.EntityDrowned;
import com.michaelsebero.portium.entity.passive.EntityTurtle;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITargetNonTamed;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.AbstractSkeleton;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.13's changes to vanilla mob AI: zombies sink in water (so they can drown) and trample turtle eggs; zombies,
 * skeletons, and wild wolves and ocelots hunt baby turtles on land; squids flee their attackers.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class MobGoals {

    @SubscribeEvent
    public static void onJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (event.getWorld().isRemote) return;
        boolean turtles = PortiumConfig.mechanics.turtleHunting && PortiumEntities.isRegistered(EntityTurtle.class);
        if (entity instanceof EntityZombie) {
            EntityZombie zombie = (EntityZombie) entity;
            if (PortiumConfig.mechanics.zombieDrowning) removeTask(zombie.tasks, EntityAISwimming.class);
            if (PortiumConfig.mechanics.turtleHunting && PortiumBlocks.TURTLE_EGG != null) zombie.tasks.addTask(4, new EntityAIBreakTurtleEgg(zombie, 1.0D));
            if (turtles && !(zombie instanceof EntityPigZombie) && !(zombie instanceof EntityDrowned)) {
                zombie.targetTasks.addTask(5, new EntityAINearestAttackableTarget<>(zombie, EntityTurtle.class, 10, true, false, EntityTurtle.TARGET_DRY_BABY));
            }
        } else if (entity instanceof EntitySquid) {
            if (PortiumConfig.mechanics.squidInk) ((EntitySquid) entity).tasks.addTask(1, new EntityAISquidFlee((EntitySquid) entity));
        } else if (!turtles) {
            return;
        } else if (entity instanceof AbstractSkeleton) {
            AbstractSkeleton skeleton = (AbstractSkeleton) entity;
            skeleton.targetTasks.addTask(3, new EntityAINearestAttackableTarget<>(skeleton, EntityTurtle.class, 10, true, false, EntityTurtle.TARGET_DRY_BABY));
        } else if (entity instanceof EntityWolf) {
            EntityWolf wolf = (EntityWolf) entity;
            wolf.targetTasks.addTask(5, new EntityAITargetNonTamed<>(wolf, EntityTurtle.class, false, EntityTurtle.TARGET_DRY_BABY));
        } else if (entity instanceof EntityOcelot) {
            EntityOcelot ocelot = (EntityOcelot) entity;
            ocelot.targetTasks.addTask(1, new EntityAITargetNonTamed<>(ocelot, EntityTurtle.class, false, EntityTurtle.TARGET_DRY_BABY));
        }
    }

    private static void removeTask(EntityAITasks tasks, Class<?> type) {
        tasks.taskEntries.removeIf(entry -> type.isInstance(entry.action));
    }

    private MobGoals() {}
}
