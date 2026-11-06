package com.michaelsebero.portium.world;

import com.michaelsebero.portium.Tags;
import net.minecraft.world.GameRules;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** 1.14-1.16's game rules, added to a world's rules (and so its level.dat) when it loads without them. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PortiumGameRules {

    /** 1.15: pillager patrols spawn. */
    public static final String DO_PATROL_SPAWNING = "doPatrolSpawning";
    /** 1.15: wandering traders spawn. */
    public static final String DO_TRADER_SPAWNING = "doTraderSpawning";
    /** 1.14.3: no raids, and no Bad Omen. */
    public static final String DISABLE_RAIDS = "disableRaids";
    /** 1.16: mobs angered by a player stop being angry once that player dies. */
    public static final String FORGIVE_DEAD_PLAYERS = "forgiveDeadPlayers";
    /** 1.16: angered neutral mobs attack any nearby player, not just the one who angered them. */
    public static final String UNIVERSAL_ANGER = "universalAnger";
    /** 1.15: phantoms come for players who haven't slept. */
    public static final String DO_INSOMNIA = "doInsomnia";
    /** 1.15: players respawn at once, without the death screen. */
    public static final String DO_IMMEDIATE_RESPAWN = "doImmediateRespawn";
    /** 1.15: players take drowning, fall and fire damage. */
    public static final String DROWNING_DAMAGE = "drowningDamage";
    public static final String FALL_DAMAGE = "fallDamage";
    public static final String FIRE_DAMAGE = "fireDamage";

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld().isRemote) return;
        GameRules rules = event.getWorld().getGameRules();
        add(rules, DO_PATROL_SPAWNING, "true");
        add(rules, DISABLE_RAIDS, "false");
        add(rules, DO_TRADER_SPAWNING, "true");
        add(rules, FORGIVE_DEAD_PLAYERS, "true");
        add(rules, UNIVERSAL_ANGER, "false");
        add(rules, DO_INSOMNIA, "true");
        add(rules, DO_IMMEDIATE_RESPAWN, "false");
        add(rules, DROWNING_DAMAGE, "true");
        add(rules, FALL_DAMAGE, "true");
        add(rules, FIRE_DAMAGE, "true");
    }

    /** 1.15's damage rules: a player takes no drowning, falling or fire damage while its rule is off. */
    @SubscribeEvent
    public static void onAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof net.minecraft.entity.player.EntityPlayerMP)) return;
        net.minecraft.world.World world = event.getEntityLiving().world;
        net.minecraft.util.DamageSource source = event.getSource();
        if (source == net.minecraft.util.DamageSource.DROWN && !get(world, DROWNING_DAMAGE, true)) event.setCanceled(true);
        else if (source == net.minecraft.util.DamageSource.FALL && !get(world, FALL_DAMAGE, true)) event.setCanceled(true);
        else if (source.isFireDamage() && !get(world, FIRE_DAMAGE, true)) event.setCanceled(true);
    }

    /** 1.15's doImmediateRespawn: the dying player's client is told to respawn straight away (before its death packet). */
    @SubscribeEvent
    public static void onDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof net.minecraft.entity.player.EntityPlayerMP) || !get(event.getEntityLiving().world, DO_IMMEDIATE_RESPAWN, false)) return;
        com.michaelsebero.portium.network.PortiumNetwork.CHANNEL.sendTo(new com.michaelsebero.portium.network.ImmediateRespawnMessage(),
                (net.minecraft.entity.player.EntityPlayerMP) event.getEntityLiving());
    }

    /** A Portium rule's value, its 1.16 default when the world hasn't got it yet. */
    public static boolean get(net.minecraft.world.World world, String name, boolean fallback) {
        GameRules rules = world.getGameRules();
        return rules.hasRule(name) ? rules.getBoolean(name) : fallback;
    }

    private static void add(GameRules rules, String name, String value) {
        if (!rules.hasRule(name)) rules.addGameRule(name, value, GameRules.ValueType.BOOLEAN_VALUE);
    }

    private PortiumGameRules() {}
}
