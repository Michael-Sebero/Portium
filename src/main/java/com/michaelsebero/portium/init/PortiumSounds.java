package com.michaelsebero.portium.init;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Sound events. They are created up front because blocks and items, which are registered before sounds,
 * reference them in their sound types and armour material.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PortiumSounds {

    private static final List<SoundEvent> SOUNDS = new ArrayList<>();
    /** Sound names kept under portium: because 1.12 has a sound by that name (1.12's cat sounds, for instance). */
    private static final java.util.Set<String> KEPT = new java.util.HashSet<>();

    public static final SoundEvent AMBIENT_UNDERWATER_ENTER = create("ambient.underwater.enter");
    public static final SoundEvent AMBIENT_UNDERWATER_EXIT = create("ambient.underwater.exit");
    public static final SoundEvent AMBIENT_UNDERWATER_LOOP = create("ambient.underwater.loop");
    public static final SoundEvent AMBIENT_UNDERWATER_LOOP_ADDITIONS = create("ambient.underwater.loop.additions");
    public static final SoundEvent AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE = create("ambient.underwater.loop.additions.rare");
    public static final SoundEvent AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE = create("ambient.underwater.loop.additions.ultra_rare");

    // 1.16's Nether biomes: an ambient loop, mood sounds and random additions each, and their music.
    public static final SoundEvent AMBIENT_BASALT_DELTAS_LOOP = create("ambient.basalt_deltas.loop");
    public static final SoundEvent AMBIENT_BASALT_DELTAS_MOOD = create("ambient.basalt_deltas.mood");
    public static final SoundEvent AMBIENT_BASALT_DELTAS_ADDITIONS = create("ambient.basalt_deltas.additions");
    public static final SoundEvent AMBIENT_CRIMSON_FOREST_LOOP = create("ambient.crimson_forest.loop");
    public static final SoundEvent AMBIENT_CRIMSON_FOREST_MOOD = create("ambient.crimson_forest.mood");
    public static final SoundEvent AMBIENT_CRIMSON_FOREST_ADDITIONS = create("ambient.crimson_forest.additions");
    public static final SoundEvent AMBIENT_NETHER_WASTES_LOOP = create("ambient.nether_wastes.loop");
    public static final SoundEvent AMBIENT_NETHER_WASTES_MOOD = create("ambient.nether_wastes.mood");
    public static final SoundEvent AMBIENT_NETHER_WASTES_ADDITIONS = create("ambient.nether_wastes.additions");
    public static final SoundEvent AMBIENT_SOUL_SAND_VALLEY_LOOP = create("ambient.soul_sand_valley.loop");
    public static final SoundEvent AMBIENT_SOUL_SAND_VALLEY_MOOD = create("ambient.soul_sand_valley.mood");
    public static final SoundEvent AMBIENT_SOUL_SAND_VALLEY_ADDITIONS = create("ambient.soul_sand_valley.additions");
    public static final SoundEvent AMBIENT_WARPED_FOREST_LOOP = create("ambient.warped_forest.loop");
    public static final SoundEvent AMBIENT_WARPED_FOREST_MOOD = create("ambient.warped_forest.mood");
    public static final SoundEvent AMBIENT_WARPED_FOREST_ADDITIONS = create("ambient.warped_forest.additions");

    public static final SoundEvent MUSIC_NETHER_BASALT_DELTAS = create("music.nether.basalt_deltas");
    public static final SoundEvent MUSIC_NETHER_CRIMSON_FOREST = create("music.nether.crimson_forest");
    public static final SoundEvent MUSIC_NETHER_NETHER_WASTES = create("music.nether.nether_wastes");
    public static final SoundEvent MUSIC_NETHER_SOUL_SAND_VALLEY = create("music.nether.soul_sand_valley");
    public static final SoundEvent MUSIC_NETHER_WARPED_FOREST = create("music.nether.warped_forest");

    public static final SoundEvent BLOCK_BUBBLE_COLUMN_BUBBLE_POP = create("block.bubble_column.bubble_pop");
    public static final SoundEvent BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT = create("block.bubble_column.upwards_ambient");
    public static final SoundEvent BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE = create("block.bubble_column.upwards_inside");
    public static final SoundEvent BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT = create("block.bubble_column.whirlpool_ambient");
    public static final SoundEvent BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE = create("block.bubble_column.whirlpool_inside");

    public static final SoundEvent BLOCK_CONDUIT_ACTIVATE = create("block.conduit.activate");
    public static final SoundEvent BLOCK_CONDUIT_AMBIENT = create("block.conduit.ambient");
    public static final SoundEvent BLOCK_CONDUIT_AMBIENT_SHORT = create("block.conduit.ambient.short");
    public static final SoundEvent BLOCK_CONDUIT_ATTACK_TARGET = create("block.conduit.attack.target");
    public static final SoundEvent BLOCK_CONDUIT_DEACTIVATE = create("block.conduit.deactivate");

    public static final SoundEvent BLOCK_CORAL_BLOCK_BREAK = create("block.coral_block.break");
    public static final SoundEvent BLOCK_CORAL_BLOCK_FALL = create("block.coral_block.fall");
    public static final SoundEvent BLOCK_CORAL_BLOCK_HIT = create("block.coral_block.hit");
    public static final SoundEvent BLOCK_CORAL_BLOCK_PLACE = create("block.coral_block.place");
    public static final SoundEvent BLOCK_CORAL_BLOCK_STEP = create("block.coral_block.step");

    public static final SoundEvent BLOCK_WET_GRASS_BREAK = create("block.wet_grass.break");
    public static final SoundEvent BLOCK_WET_GRASS_FALL = create("block.wet_grass.fall");
    public static final SoundEvent BLOCK_WET_GRASS_HIT = create("block.wet_grass.hit");
    public static final SoundEvent BLOCK_WET_GRASS_PLACE = create("block.wet_grass.place");
    public static final SoundEvent BLOCK_WET_GRASS_STEP = create("block.wet_grass.step");

    public static final SoundEvent BLOCK_PUMPKIN_CARVE = create("block.pumpkin.carve");

    public static final SoundEvent ENTITY_TURTLE_EGG_BREAK = create("entity.turtle.egg_break");
    public static final SoundEvent ENTITY_TURTLE_EGG_CRACK = create("entity.turtle.egg_crack");
    public static final SoundEvent ENTITY_TURTLE_EGG_HATCH = create("entity.turtle.egg_hatch");

    public static final SoundEvent ENTITY_SQUID_SQUIRT = create("entity.squid.squirt");
    public static final SoundEvent ENTITY_HUSK_CONVERTED_TO_ZOMBIE = create("entity.husk.converted_to_zombie");

    public static final SoundEvent ENTITY_COD_DEATH = create("entity.cod.death");
    public static final SoundEvent ENTITY_COD_FLOP = create("entity.cod.flop");
    public static final SoundEvent ENTITY_COD_HURT = create("entity.cod.hurt");
    public static final SoundEvent ENTITY_SALMON_DEATH = create("entity.salmon.death");
    public static final SoundEvent ENTITY_SALMON_FLOP = create("entity.salmon.flop");
    public static final SoundEvent ENTITY_SALMON_HURT = create("entity.salmon.hurt");
    public static final SoundEvent ENTITY_TROPICAL_FISH_DEATH = create("entity.tropical_fish.death");
    public static final SoundEvent ENTITY_TROPICAL_FISH_FLOP = create("entity.tropical_fish.flop");
    public static final SoundEvent ENTITY_TROPICAL_FISH_HURT = create("entity.tropical_fish.hurt");
    public static final SoundEvent ENTITY_PUFFER_FISH_BLOW_OUT = create("entity.puffer_fish.blow_out");
    public static final SoundEvent ENTITY_PUFFER_FISH_BLOW_UP = create("entity.puffer_fish.blow_up");
    public static final SoundEvent ENTITY_PUFFER_FISH_DEATH = create("entity.puffer_fish.death");
    public static final SoundEvent ENTITY_PUFFER_FISH_FLOP = create("entity.puffer_fish.flop");
    public static final SoundEvent ENTITY_PUFFER_FISH_HURT = create("entity.puffer_fish.hurt");
    public static final SoundEvent ENTITY_PUFFER_FISH_STING = create("entity.puffer_fish.sting");
    public static final SoundEvent ENTITY_FISH_SWIM = create("entity.fish.swim");

    public static final SoundEvent ENTITY_DOLPHIN_AMBIENT = create("entity.dolphin.ambient");
    public static final SoundEvent ENTITY_DOLPHIN_AMBIENT_WATER = create("entity.dolphin.ambient_water");
    public static final SoundEvent ENTITY_DOLPHIN_ATTACK = create("entity.dolphin.attack");
    public static final SoundEvent ENTITY_DOLPHIN_DEATH = create("entity.dolphin.death");
    public static final SoundEvent ENTITY_DOLPHIN_EAT = create("entity.dolphin.eat");
    public static final SoundEvent ENTITY_DOLPHIN_HURT = create("entity.dolphin.hurt");
    public static final SoundEvent ENTITY_DOLPHIN_JUMP = create("entity.dolphin.jump");
    public static final SoundEvent ENTITY_DOLPHIN_PLAY = create("entity.dolphin.play");
    public static final SoundEvent ENTITY_DOLPHIN_SPLASH = create("entity.dolphin.splash");
    public static final SoundEvent ENTITY_DOLPHIN_SWIM = create("entity.dolphin.swim");

    public static final SoundEvent ENTITY_TURTLE_AMBIENT_LAND = create("entity.turtle.ambient_land");
    public static final SoundEvent ENTITY_TURTLE_DEATH = create("entity.turtle.death");
    public static final SoundEvent ENTITY_TURTLE_DEATH_BABY = create("entity.turtle.death_baby");
    public static final SoundEvent ENTITY_TURTLE_HURT = create("entity.turtle.hurt");
    public static final SoundEvent ENTITY_TURTLE_HURT_BABY = create("entity.turtle.hurt_baby");
    public static final SoundEvent ENTITY_TURTLE_LAY_EGG = create("entity.turtle.lay_egg");
    public static final SoundEvent ENTITY_TURTLE_SHAMBLE = create("entity.turtle.shamble");
    public static final SoundEvent ENTITY_TURTLE_SHAMBLE_BABY = create("entity.turtle.shamble_baby");
    public static final SoundEvent ENTITY_TURTLE_SWIM = create("entity.turtle.swim");

    public static final SoundEvent ENTITY_DROWNED_AMBIENT = create("entity.drowned.ambient");
    public static final SoundEvent ENTITY_DROWNED_AMBIENT_WATER = create("entity.drowned.ambient_water");
    public static final SoundEvent ENTITY_DROWNED_DEATH = create("entity.drowned.death");
    public static final SoundEvent ENTITY_DROWNED_DEATH_WATER = create("entity.drowned.death_water");
    public static final SoundEvent ENTITY_DROWNED_HURT = create("entity.drowned.hurt");
    public static final SoundEvent ENTITY_DROWNED_HURT_WATER = create("entity.drowned.hurt_water");
    public static final SoundEvent ENTITY_DROWNED_SHOOT = create("entity.drowned.shoot");
    public static final SoundEvent ENTITY_DROWNED_STEP = create("entity.drowned.step");
    public static final SoundEvent ENTITY_DROWNED_SWIM = create("entity.drowned.swim");
    public static final SoundEvent ENTITY_ZOMBIE_CONVERTED_TO_DROWNED = create("entity.zombie.converted_to_drowned");
    public static final SoundEvent ENTITY_ZOMBIE_DESTROY_EGG = create("entity.zombie.destroy_egg");

    public static final SoundEvent ENTITY_PHANTOM_AMBIENT = create("entity.phantom.ambient");
    public static final SoundEvent ENTITY_PARROT_IMITATE_DROWNED = create("entity.parrot.imitate.drowned");
    public static final SoundEvent ENTITY_PARROT_IMITATE_PHANTOM = create("entity.parrot.imitate.phantom");
    public static final SoundEvent ENTITY_PHANTOM_BITE = create("entity.phantom.bite");
    public static final SoundEvent ENTITY_PHANTOM_DEATH = create("entity.phantom.death");
    public static final SoundEvent ENTITY_PHANTOM_FLAP = create("entity.phantom.flap");
    public static final SoundEvent ENTITY_PHANTOM_HURT = create("entity.phantom.hurt");
    public static final SoundEvent ENTITY_PHANTOM_SWOOP = create("entity.phantom.swoop");

    public static final SoundEvent ITEM_BUCKET_EMPTY_FISH = create("item.bucket.empty_fish");
    public static final SoundEvent ITEM_BUCKET_FILL_FISH = create("item.bucket.fill_fish");

    public static final SoundEvent ITEM_ARMOR_EQUIP_TURTLE = create("item.armor.equip_turtle");
    public static final SoundEvent ITEM_ARMOR_EQUIP_NETHERITE = create("item.armor.equip_netherite");
    public static final SoundEvent BLOCK_SMITHING_TABLE_USE = create("block.smithing_table.use");
    public static final SoundEvent ITEM_AXE_STRIP = create("item.axe.strip");
    public static final SoundEvent BLOCK_CAMPFIRE_CRACKLE = create("block.campfire.crackle");
    public static final SoundEvent BLOCK_RESPAWN_ANCHOR_AMBIENT = create("block.respawn_anchor.ambient");
    public static final SoundEvent BLOCK_RESPAWN_ANCHOR_CHARGE = create("block.respawn_anchor.charge");
    public static final SoundEvent BLOCK_RESPAWN_ANCHOR_DEPLETE = create("block.respawn_anchor.deplete");
    public static final SoundEvent BLOCK_RESPAWN_ANCHOR_SET_SPAWN = create("block.respawn_anchor.set_spawn");
    public static final SoundEvent ITEM_LODESTONE_COMPASS_LOCK = create("item.lodestone_compass.lock");
    public static final SoundEvent ITEM_SWEET_BERRIES_PICK_FROM_BUSH = create("item.sweet_berries.pick_from_bush");
    public static final SoundEvent BLOCK_BARREL_OPEN = create("block.barrel.open");
    public static final SoundEvent BLOCK_BARREL_CLOSE = create("block.barrel.close");
    public static final SoundEvent BLOCK_SMOKER_SMOKE = create("block.smoker.smoke");
    public static final SoundEvent BLOCK_BLASTFURNACE_FIRE_CRACKLE = create("block.blastfurnace.fire_crackle");
    public static final SoundEvent BLOCK_COMPOSTER_FILL = create("block.composter.fill");
    public static final SoundEvent BLOCK_COMPOSTER_FILL_SUCCESS = create("block.composter.fill_success");
    public static final SoundEvent BLOCK_COMPOSTER_EMPTY = create("block.composter.empty");
    public static final SoundEvent BLOCK_COMPOSTER_READY = create("block.composter.ready");
    public static final SoundEvent BLOCK_GRINDSTONE_USE = create("block.grindstone.use");
    public static final SoundEvent UI_STONECUTTER_TAKE_RESULT = create("ui.stonecutter.take_result");
    public static final SoundEvent UI_STONECUTTER_SELECT_RECIPE = create("ui.stonecutter.select_recipe");
    public static final SoundEvent UI_LOOM_SELECT_PATTERN = create("ui.loom.select_pattern");
    public static final SoundEvent UI_LOOM_TAKE_RESULT = create("ui.loom.take_result");
    public static final SoundEvent UI_CARTOGRAPHY_TABLE_TAKE_RESULT = create("ui.cartography_table.take_result");
    public static final SoundEvent ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH = create("entity.player.hurt_sweet_berry_bush");
    public static final SoundEvent BLOCK_BELL_USE = create("block.bell.use");
    public static final SoundEvent BLOCK_BELL_RESONATE = create("block.bell.resonate");
    public static final SoundEvent ITEM_BOOK_PAGE_TURN = create("item.book.page_turn");
    public static final SoundEvent ITEM_BOOK_PUT = create("item.book.put");
    public static final SoundEvent BLOCK_HONEY_BLOCK_SLIDE = create("block.honey_block.slide");
    public static final SoundEvent ITEM_HONEY_BOTTLE_DRINK = create("item.honey_bottle.drink");
    public static final SoundEvent ENTITY_PANDA_PRE_SNEEZE = create("entity.panda.pre_sneeze");
    public static final SoundEvent ENTITY_PANDA_SNEEZE = create("entity.panda.sneeze");
    public static final SoundEvent ENTITY_PANDA_AMBIENT = create("entity.panda.ambient");
    public static final SoundEvent ENTITY_PANDA_BITE = create("entity.panda.bite");
    public static final SoundEvent ENTITY_PANDA_CANT_BREED = create("entity.panda.cant_breed");
    public static final SoundEvent ENTITY_PANDA_DEATH = create("entity.panda.death");
    public static final SoundEvent ENTITY_PANDA_EAT = create("entity.panda.eat");
    public static final SoundEvent ENTITY_PANDA_STEP = create("entity.panda.step");
    public static final SoundEvent ENTITY_PANDA_AGGRESSIVE_AMBIENT = create("entity.panda.aggressive_ambient");
    public static final SoundEvent ENTITY_PANDA_WORRIED_AMBIENT = create("entity.panda.worried_ambient");
    public static final SoundEvent ENTITY_PANDA_HURT = create("entity.panda.hurt");
    public static final SoundEvent ENTITY_OCELOT_AMBIENT = create("entity.ocelot.ambient");
    public static final SoundEvent ENTITY_OCELOT_HURT = create("entity.ocelot.hurt");
    public static final SoundEvent ENTITY_OCELOT_DEATH = create("entity.ocelot.death");
    public static final SoundEvent ENTITY_CAT_AMBIENT = create("entity.cat.ambient");
    public static final SoundEvent ENTITY_CAT_STRAY_AMBIENT = create("entity.cat.stray_ambient");
    public static final SoundEvent ENTITY_CAT_PURR = create("entity.cat.purr");
    public static final SoundEvent ENTITY_CAT_PURREOW = create("entity.cat.purreow");
    public static final SoundEvent ENTITY_CAT_HISS = create("entity.cat.hiss");
    public static final SoundEvent ENTITY_CAT_HURT = create("entity.cat.hurt");
    public static final SoundEvent ENTITY_PILLAGER_AMBIENT = create("entity.pillager.ambient");
    public static final SoundEvent ENTITY_PILLAGER_HURT = create("entity.pillager.hurt");
    public static final SoundEvent ENTITY_PILLAGER_DEATH = create("entity.pillager.death");
    public static final SoundEvent ENTITY_PILLAGER_CELEBRATE = create("entity.pillager.celebrate");
    public static final SoundEvent ENTITY_VINDICATOR_CELEBRATE = create("entity.vindicator.celebrate");
    public static final SoundEvent ENTITY_EVOKER_CELEBRATE = create("entity.evoker.celebrate");
    public static final SoundEvent ENTITY_WITCH_CELEBRATE = create("entity.witch.celebrate");
    public static final SoundEvent ENTITY_RAVAGER_AMBIENT = create("entity.ravager.ambient");
    public static final SoundEvent ENTITY_RAVAGER_ATTACK = create("entity.ravager.attack");
    public static final SoundEvent ENTITY_RAVAGER_CELEBRATE = create("entity.ravager.celebrate");
    public static final SoundEvent ENTITY_RAVAGER_DEATH = create("entity.ravager.death");
    public static final SoundEvent ENTITY_RAVAGER_HURT = create("entity.ravager.hurt");
    public static final SoundEvent ENTITY_RAVAGER_STEP = create("entity.ravager.step");

    public static final SoundEvent ENTITY_BEE_DEATH = create("entity.bee.death");
    public static final SoundEvent ENTITY_BEE_HURT = create("entity.bee.hurt");
    public static final SoundEvent ENTITY_BEE_LOOP = create("entity.bee.loop");
    public static final SoundEvent ENTITY_BEE_LOOP_AGGRESSIVE = create("entity.bee.loop_aggressive");
    public static final SoundEvent ENTITY_BEE_POLLINATE = create("entity.bee.pollinate");
    public static final SoundEvent ENTITY_BEE_STING = create("entity.bee.sting");
    public static final SoundEvent BLOCK_BEEHIVE_DRIP = create("block.beehive.drip");
    public static final SoundEvent BLOCK_BEEHIVE_ENTER = create("block.beehive.enter");
    public static final SoundEvent BLOCK_BEEHIVE_EXIT = create("block.beehive.exit");
    public static final SoundEvent BLOCK_BEEHIVE_SHEAR = create("block.beehive.shear");
    public static final SoundEvent BLOCK_BEEHIVE_WORK = create("block.beehive.work");
    public static final SoundEvent ENTITY_IRON_GOLEM_DAMAGE = create("entity.iron_golem.damage");
    public static final SoundEvent ENTITY_IRON_GOLEM_REPAIR = create("entity.iron_golem.repair");
    public static final SoundEvent MUSIC_DISC_PIGSTEP = create("music_disc.pigstep");
    public static final SoundEvent ENTITY_ZOMBIFIED_PIGLIN_AMBIENT = create("entity.zombified_piglin.ambient");
    public static final SoundEvent ENTITY_ZOMBIFIED_PIGLIN_ANGRY = create("entity.zombified_piglin.angry");
    public static final SoundEvent ENTITY_ZOMBIFIED_PIGLIN_DEATH = create("entity.zombified_piglin.death");
    public static final SoundEvent ENTITY_ZOMBIFIED_PIGLIN_HURT = create("entity.zombified_piglin.hurt");
    public static final SoundEvent PARTICLE_SOUL_ESCAPE = create("particle.soul_escape");
    // 1.16 Nether mobs.
    public static final SoundEvent ENTITY_PIGLIN_ADMIRING_ITEM = create("entity.piglin.admiring_item");
    public static final SoundEvent ENTITY_PIGLIN_AMBIENT = create("entity.piglin.ambient");
    public static final SoundEvent ENTITY_PIGLIN_ANGRY = create("entity.piglin.angry");
    public static final SoundEvent ENTITY_PIGLIN_CELEBRATE = create("entity.piglin.celebrate");
    public static final SoundEvent ENTITY_PIGLIN_DEATH = create("entity.piglin.death");
    public static final SoundEvent ENTITY_PIGLIN_JEALOUS = create("entity.piglin.jealous");
    public static final SoundEvent ENTITY_PIGLIN_HURT = create("entity.piglin.hurt");
    public static final SoundEvent ENTITY_PIGLIN_RETREAT = create("entity.piglin.retreat");
    public static final SoundEvent ENTITY_PIGLIN_STEP = create("entity.piglin.step");
    public static final SoundEvent ENTITY_PIGLIN_CONVERTED_TO_ZOMBIFIED = create("entity.piglin.converted_to_zombified");
    public static final SoundEvent ENTITY_PIGLIN_BRUTE_AMBIENT = create("entity.piglin_brute.ambient");
    public static final SoundEvent ENTITY_PIGLIN_BRUTE_ANGRY = create("entity.piglin_brute.angry");
    public static final SoundEvent ENTITY_PIGLIN_BRUTE_CONVERTED_TO_ZOMBIFIED = create("entity.piglin_brute.converted_to_zombified");
    public static final SoundEvent ENTITY_PIGLIN_BRUTE_DEATH = create("entity.piglin_brute.death");
    public static final SoundEvent ENTITY_PIGLIN_BRUTE_HURT = create("entity.piglin_brute.hurt");
    public static final SoundEvent ENTITY_PIGLIN_BRUTE_STEP = create("entity.piglin_brute.step");
    public static final SoundEvent ENTITY_STRIDER_AMBIENT = create("entity.strider.ambient");
    public static final SoundEvent ENTITY_STRIDER_DEATH = create("entity.strider.death");
    public static final SoundEvent ENTITY_STRIDER_EAT = create("entity.strider.eat");
    public static final SoundEvent ENTITY_STRIDER_HAPPY = create("entity.strider.happy");
    public static final SoundEvent ENTITY_STRIDER_HURT = create("entity.strider.hurt");
    public static final SoundEvent ENTITY_STRIDER_RETREAT = create("entity.strider.retreat");
    public static final SoundEvent ENTITY_STRIDER_SADDLE = create("entity.strider.saddle");
    public static final SoundEvent ENTITY_STRIDER_STEP = create("entity.strider.step");
    public static final SoundEvent ENTITY_STRIDER_STEP_LAVA = create("entity.strider.step_lava");
    public static final SoundEvent ENTITY_HOGLIN_AMBIENT = create("entity.hoglin.ambient");
    public static final SoundEvent ENTITY_HOGLIN_ANGRY = create("entity.hoglin.angry");
    public static final SoundEvent ENTITY_HOGLIN_ATTACK = create("entity.hoglin.attack");
    public static final SoundEvent ENTITY_HOGLIN_CONVERTED_TO_ZOMBIFIED = create("entity.hoglin.converted_to_zombified");
    public static final SoundEvent ENTITY_HOGLIN_DEATH = create("entity.hoglin.death");
    public static final SoundEvent ENTITY_HOGLIN_HURT = create("entity.hoglin.hurt");
    public static final SoundEvent ENTITY_HOGLIN_RETREAT = create("entity.hoglin.retreat");
    public static final SoundEvent ENTITY_HOGLIN_STEP = create("entity.hoglin.step");
    public static final SoundEvent ENTITY_ZOGLIN_AMBIENT = create("entity.zoglin.ambient");
    public static final SoundEvent ENTITY_ZOGLIN_ANGRY = create("entity.zoglin.angry");
    public static final SoundEvent ENTITY_ZOGLIN_ATTACK = create("entity.zoglin.attack");
    public static final SoundEvent ENTITY_ZOGLIN_DEATH = create("entity.zoglin.death");
    public static final SoundEvent ENTITY_ZOGLIN_HURT = create("entity.zoglin.hurt");
    public static final SoundEvent ENTITY_ZOGLIN_STEP = create("entity.zoglin.step");
    public static final SoundEvent ENTITY_RAVAGER_STUNNED = create("entity.ravager.stunned");
    public static final SoundEvent ENTITY_RAVAGER_ROAR = create("entity.ravager.roar");
    public static final SoundEvent ENTITY_WANDERING_TRADER_AMBIENT = create("entity.wandering_trader.ambient");
    public static final SoundEvent ENTITY_WANDERING_TRADER_DEATH = create("entity.wandering_trader.death");
    public static final SoundEvent ENTITY_WANDERING_TRADER_DISAPPEARED = create("entity.wandering_trader.disappeared");
    public static final SoundEvent ENTITY_WANDERING_TRADER_DRINK_MILK = create("entity.wandering_trader.drink_milk");
    public static final SoundEvent ENTITY_WANDERING_TRADER_DRINK_POTION = create("entity.wandering_trader.drink_potion");
    public static final SoundEvent ENTITY_WANDERING_TRADER_HURT = create("entity.wandering_trader.hurt");
    public static final SoundEvent ENTITY_WANDERING_TRADER_NO = create("entity.wandering_trader.no");
    public static final SoundEvent ENTITY_WANDERING_TRADER_REAPPEARED = create("entity.wandering_trader.reappeared");
    public static final SoundEvent ENTITY_WANDERING_TRADER_TRADE = create("entity.wandering_trader.trade");
    public static final SoundEvent ENTITY_WANDERING_TRADER_YES = create("entity.wandering_trader.yes");
    public static final SoundEvent ENTITY_CAT_DEATH = create("entity.cat.death");
    public static final SoundEvent ENTITY_CAT_EAT = create("entity.cat.eat");
    public static final SoundEvent ENTITY_CAT_BEG_FOR_FOOD = create("entity.cat.beg_for_food");
    public static final SoundEvent ITEM_CROSSBOW_QUICK_CHARGE_1 = create("item.crossbow.quick_charge_1");
    public static final SoundEvent ITEM_CROSSBOW_QUICK_CHARGE_2 = create("item.crossbow.quick_charge_2");
    public static final SoundEvent ITEM_CROSSBOW_QUICK_CHARGE_3 = create("item.crossbow.quick_charge_3");
    public static final SoundEvent ITEM_CROSSBOW_LOADING_START = create("item.crossbow.loading_start");
    public static final SoundEvent ITEM_CROSSBOW_LOADING_MIDDLE = create("item.crossbow.loading_middle");
    public static final SoundEvent ITEM_CROSSBOW_LOADING_END = create("item.crossbow.loading_end");
    public static final SoundEvent ITEM_CROSSBOW_SHOOT = create("item.crossbow.shoot");
    public static final SoundEvent ITEM_CROSSBOW_HIT = create("item.crossbow.hit");
    public static final SoundEvent ENTITY_FOX_AMBIENT = create("entity.fox.ambient");
    public static final SoundEvent ENTITY_FOX_HURT = create("entity.fox.hurt");
    public static final SoundEvent ENTITY_FOX_DEATH = create("entity.fox.death");
    public static final SoundEvent ENTITY_FOX_AGGRO = create("entity.fox.aggro");
    public static final SoundEvent ENTITY_FOX_SNIFF = create("entity.fox.sniff");
    public static final SoundEvent ENTITY_FOX_BITE = create("entity.fox.bite");
    public static final SoundEvent ENTITY_FOX_EAT = create("entity.fox.eat");
    public static final SoundEvent ENTITY_FOX_SCREECH = create("entity.fox.screech");
    public static final SoundEvent ENTITY_FOX_SLEEP = create("entity.fox.sleep");
    public static final SoundEvent ENTITY_FOX_SPIT = create("entity.fox.spit");
    public static final SoundEvent ENTITY_FOX_TELEPORT = create("entity.fox.teleport");

    public static final SoundEvent ITEM_TRIDENT_HIT = create("item.trident.hit");
    public static final SoundEvent ITEM_TRIDENT_HIT_GROUND = create("item.trident.hit_ground");
    public static final SoundEvent ITEM_TRIDENT_RETURN = create("item.trident.return");
    public static final SoundEvent ITEM_TRIDENT_RIPTIDE_1 = create("item.trident.riptide_1");
    public static final SoundEvent ITEM_TRIDENT_RIPTIDE_2 = create("item.trident.riptide_2");
    public static final SoundEvent ITEM_TRIDENT_RIPTIDE_3 = create("item.trident.riptide_3");
    public static final SoundEvent ITEM_TRIDENT_THROW = create("item.trident.throw");
    public static final SoundEvent ITEM_TRIDENT_THUNDER = create("item.trident.thunder");

    // Villagers (1.14): work sounds by profession, celebrating, and zombie villagers being cured.
    public static final SoundEvent ENTITY_VILLAGER_WORK_ARMORER = create("entity.villager.work_armorer");
    public static final SoundEvent ENTITY_VILLAGER_WORK_BUTCHER = create("entity.villager.work_butcher");
    public static final SoundEvent ENTITY_VILLAGER_WORK_CARTOGRAPHER = create("entity.villager.work_cartographer");
    public static final SoundEvent ENTITY_VILLAGER_WORK_CLERIC = create("entity.villager.work_cleric");
    public static final SoundEvent ENTITY_VILLAGER_WORK_FARMER = create("entity.villager.work_farmer");
    public static final SoundEvent ENTITY_VILLAGER_WORK_FISHERMAN = create("entity.villager.work_fisherman");
    public static final SoundEvent ENTITY_VILLAGER_WORK_FLETCHER = create("entity.villager.work_fletcher");
    public static final SoundEvent ENTITY_VILLAGER_WORK_LEATHERWORKER = create("entity.villager.work_leatherworker");
    public static final SoundEvent ENTITY_VILLAGER_WORK_LIBRARIAN = create("entity.villager.work_librarian");
    public static final SoundEvent ENTITY_VILLAGER_WORK_MASON = create("entity.villager.work_mason");
    public static final SoundEvent ENTITY_VILLAGER_WORK_SHEPHERD = create("entity.villager.work_shepherd");
    public static final SoundEvent ENTITY_VILLAGER_WORK_TOOLSMITH = create("entity.villager.work_toolsmith");
    public static final SoundEvent ENTITY_VILLAGER_WORK_WEAPONSMITH = create("entity.villager.work_weaponsmith");
    public static final SoundEvent ENTITY_VILLAGER_CELEBRATE = create("entity.villager.celebrate");
    public static final SoundEvent EVENT_RAID_HORN = create("event.raid.horn");
    /** 1.13's underwater music (client/UnderwaterAmbience). */
    public static final SoundEvent MUSIC_UNDER_WATER = create("music.under_water");
    // 1.14's note block instruments, by the block under the note block (TileEntityNote/BlockNote mixins).
    public static final SoundEvent BLOCK_NOTE_BLOCK_IRON_XYLOPHONE = create("block.note_block.iron_xylophone");
    public static final SoundEvent BLOCK_NOTE_BLOCK_COW_BELL = create("block.note_block.cow_bell");
    public static final SoundEvent BLOCK_NOTE_BLOCK_DIDGERIDOO = create("block.note_block.didgeridoo");
    public static final SoundEvent BLOCK_NOTE_BLOCK_BIT = create("block.note_block.bit");
    public static final SoundEvent BLOCK_NOTE_BLOCK_BANJO = create("block.note_block.banjo");
    public static final SoundEvent BLOCK_NOTE_BLOCK_PLING = create("block.note_block.pling");
    public static final SoundEvent ENTITY_MOOSHROOM_CONVERT = create("entity.mooshroom.convert");
    public static final SoundEvent ENTITY_MOOSHROOM_EAT = create("entity.mooshroom.eat");
    public static final SoundEvent ENTITY_MOOSHROOM_MILK = create("entity.mooshroom.milk");
    public static final SoundEvent ENTITY_MOOSHROOM_SUSPICIOUS_MILK = create("entity.mooshroom.suspicious_milk");
    public static final SoundEvent ENTITY_ZOMBIE_VILLAGER_CURE = create("entity.zombie_villager.cure");
    public static final SoundEvent ENTITY_ZOMBIE_VILLAGER_CONVERTED = create("entity.zombie_villager.converted");

    /**
     * A sound event named as 1.13 to 1.16 name it, {@code minecraft:<name>} (as Portium's blocks and items are),
     * unless 1.12 already has that sound; then {@code portium:<name>}. Its definition stays in Portium's sounds.json,
     * which the sound handler files under the event's name ({@link #definitionName}).
     */
    static SoundEvent create(String name) {
        ResourceLocation vanilla = new ResourceLocation(name);
        boolean kept = !PortiumConfig.mechanics.minecraftIds || vanilla112().contains(vanilla);
        if (kept) {
            KEPT.add(name);
        } else if (SoundEvent.REGISTRY.containsKey(vanilla)) {
            // Made already by 1.16 code Portium runs (1.16's own sound events): the same event, defined by Portium.
            PortiumNames.moved(SoundEvent.class, name);
            return SoundEvent.REGISTRY.getObject(vanilla);
        }
        ResourceLocation location = kept ? new ResourceLocation(Tags.MOD_ID, name) : vanilla;
        if (!kept) PortiumNames.moved(SoundEvent.class, name);
        SoundEvent sound = new SoundEvent(location).setRegistryName(location);
        SOUNDS.add(sound);
        // Registered now, not only in the registry event: 1.16 code Portium runs registers 1.16's sound events as its
        // classes load, and finding this one there it takes it rather than registering a second under the name.
        try {
            net.minecraftforge.fml.common.registry.ForgeRegistries.SOUND_EVENTS.register(sound);
        } catch (RuntimeException e) {
            // Too early or too late for the registry: the registry event below registers it.
        }
        return sound;
    }

    private static java.util.Set<ResourceLocation> vanilla112;

    /** 1.12's own sound events, by the fields of its SoundEvents (other minecraft: events may be 1.16 code's). */
    private static java.util.Set<ResourceLocation> vanilla112() {
        if (vanilla112 != null) return vanilla112;
        java.util.Set<ResourceLocation> names = new java.util.HashSet<>();
        for (java.lang.reflect.Field field : net.minecraft.init.SoundEvents.class.getFields()) {
            if (field.getType() != SoundEvent.class || !java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            try {
                SoundEvent event = (SoundEvent) field.get(null);
                if (event != null && event.getRegistryName() != null) names.add(event.getRegistryName());
            } catch (IllegalAccessException ignored) {
                // Public fields; not reached.
            }
        }
        vanilla112 = names;
        return names;
    }

    /** The event a definition in Portium's sounds.json belongs to: {@code portium:<name>} or {@code minecraft:<name>}. */
    public static ResourceLocation definitionName(ResourceLocation location) {
        if (!Tags.MOD_ID.equals(location.getNamespace()) || KEPT.contains(location.getPath()) || !PortiumConfig.mechanics.minecraftIds) return location;
        return new ResourceLocation(location.getPath());
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        // The block sound types make their events as they load; they must exist before these are registered.
        PortiumSoundTypes.init();
        for (SoundEvent sound : SOUNDS) {
            if (event.getRegistry().getValue(sound.getRegistryName()) != sound) event.getRegistry().register(sound);
            PortiumNames.alias(event.getRegistry(), sound.getRegistryName());
        }
    }

    private PortiumSounds() {}
}
