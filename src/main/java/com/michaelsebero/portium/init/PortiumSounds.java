package com.michaelsebero.portium.init;

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
    public static final SoundEvent ITEM_AXE_STRIP = create("item.axe.strip");

    public static final SoundEvent ITEM_TRIDENT_HIT = create("item.trident.hit");
    public static final SoundEvent ITEM_TRIDENT_HIT_GROUND = create("item.trident.hit_ground");
    public static final SoundEvent ITEM_TRIDENT_RETURN = create("item.trident.return");
    public static final SoundEvent ITEM_TRIDENT_RIPTIDE_1 = create("item.trident.riptide_1");
    public static final SoundEvent ITEM_TRIDENT_RIPTIDE_2 = create("item.trident.riptide_2");
    public static final SoundEvent ITEM_TRIDENT_RIPTIDE_3 = create("item.trident.riptide_3");
    public static final SoundEvent ITEM_TRIDENT_THROW = create("item.trident.throw");
    public static final SoundEvent ITEM_TRIDENT_THUNDER = create("item.trident.thunder");

    static SoundEvent create(String name) {
        ResourceLocation location = new ResourceLocation(Tags.MOD_ID, name);
        SoundEvent sound = new SoundEvent(location).setRegistryName(location);
        SOUNDS.add(sound);
        return sound;
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        // The block sound types make their events as they load; they must exist before these are registered.
        PortiumSoundTypes.init();
        for (SoundEvent sound : SOUNDS) event.getRegistry().register(sound);
    }

    private PortiumSounds() {}
}
