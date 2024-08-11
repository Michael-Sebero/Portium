package com.michaelsebero.portium.compat;

import com.michaelsebero.portium.Tags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Sound event ids across the flattening. 1.13 renamed many sound events, and the ones it added are Portium's own
 * ({@code portium:entity.dolphin.ambient} for {@code minecraft:entity.dolphin.ambient}).
 */
public final class LegacySounds {

    /** Whole 1.13 ids whose 1.12 names differ in more than a prefix. */
    private static final Map<String, String> EXACT = new LinkedHashMap<>();
    /** 1.13 id prefixes and the 1.12 prefixes they replaced. */
    private static final Map<String, String> PREFIXES = new LinkedHashMap<>();

    static {
        for (String sound : new String[] {"death", "hurt", "jump", "squish"}) EXACT.put("entity.slime." + sound + "_small", "entity.small_slime." + sound);
        for (String sound : new String[] {"death", "hurt", "squish"}) EXACT.put("entity.magma_cube." + sound + "_small", "entity.small_magmacube." + sound);
        EXACT.put("entity.polar_bear.ambient_baby", "entity.polar_bear.baby_ambient");
        EXACT.put("entity.zombie.attack_wooden_door", "entity.zombie.attack_door_wood");
        EXACT.put("entity.zombie.break_wooden_door", "entity.zombie.break_door_wood");
        EXACT.put("entity.parrot.imitate.ender_dragon", "entity.parrot.imitate.enderdragon");
        EXACT.put("entity.parrot.imitate.evoker", "entity.parrot.imitate.evocation_illager");
        EXACT.put("entity.parrot.imitate.illusioner", "entity.parrot.imitate.illusion_illager");
        EXACT.put("entity.parrot.imitate.vindicator", "entity.parrot.imitate.vindication_illager");
        EXACT.put("entity.parrot.imitate.magma_cube", "entity.parrot.imitate.magmacube");
        EXACT.put("entity.parrot.imitate.zombie_pigman", "entity.parrot.imitate.zombie_pig");

        PREFIXES.put("block.wool.", "block.cloth.");
        PREFIXES.put("block.ender_chest.", "block.enderchest.");
        PREFIXES.put("block.metal_pressure_plate.", "block.metal_pressureplate.");
        PREFIXES.put("block.stone_pressure_plate.", "block.stone_pressureplate.");
        PREFIXES.put("block.wooden_pressure_plate.", "block.wood_pressureplate.");
        PREFIXES.put("block.wooden_button.", "block.wood_button.");
        PREFIXES.put("block.note_block.", "block.note.");
        PREFIXES.put("block.slime_block.", "block.slime.");
        PREFIXES.put("block.lily_pad.", "block.waterlily.");
        PREFIXES.put("entity.armor_stand.", "entity.armorstand.");
        PREFIXES.put("entity.fishing_bobber.", "entity.bobber.");
        PREFIXES.put("entity.ender_dragon.", "entity.enderdragon.");
        PREFIXES.put("entity.dragon_fireball.", "entity.enderdragon_fireball.");
        PREFIXES.put("entity.ender_eye.", "entity.endereye.");
        PREFIXES.put("entity.enderman.", "entity.endermen.");
        PREFIXES.put("entity.ender_pearl.", "entity.enderpearl.");
        PREFIXES.put("entity.evoker_fangs.", "entity.evocation_fangs.");
        PREFIXES.put("entity.evoker.", "entity.evocation_illager.");
        PREFIXES.put("entity.firework_rocket.", "entity.firework.");
        PREFIXES.put("entity.illusioner.", "entity.illusion_illager.");
        PREFIXES.put("entity.iron_golem.", "entity.irongolem.");
        PREFIXES.put("entity.item_frame.", "entity.itemframe.");
        PREFIXES.put("entity.leash_knot.", "entity.leashknot.");
        PREFIXES.put("entity.lightning_bolt.", "entity.lightning.");
        PREFIXES.put("entity.lingering_potion.", "entity.lingeringpotion.");
        PREFIXES.put("entity.magma_cube.", "entity.magmacube.");
        PREFIXES.put("entity.snow_golem.", "entity.snowman.");
        PREFIXES.put("entity.vindicator.", "entity.vindication_illager.");
        PREFIXES.put("entity.zombie_pigman.", "entity.zombie_pig.");
        PREFIXES.put("music_disc.", "record.");
    }

    /** The sound event to send for a 1.13 id; unknown ids pass through, as resource packs may define them. */
    public static ResourceLocation toLegacy(ResourceLocation id) {
        if (!id.getNamespace().equals("minecraft")) return id;
        ResourceLocation ours = new ResourceLocation(Tags.MOD_ID, id.getPath());
        if (SoundEvent.REGISTRY.containsKey(ours)) return ours;
        String path = id.getPath();
        String legacy = EXACT.get(path);
        if (legacy == null) {
            for (Map.Entry<String, String> prefix : PREFIXES.entrySet()) {
                if (path.startsWith(prefix.getKey())) {
                    legacy = prefix.getValue() + path.substring(prefix.getKey().length());
                    break;
                }
            }
        }
        if (legacy == null) return id;
        ResourceLocation old = new ResourceLocation("minecraft", legacy);
        return SoundEvent.REGISTRY.containsKey(old) ? old : id;
    }

    /** Every registered sound event under its 1.13 id. */
    public static TreeSet<String> modernIds() {
        TreeSet<String> ids = new TreeSet<>();
        for (ResourceLocation key : SoundEvent.REGISTRY.getKeys()) {
            if (key.getNamespace().equals(Tags.MOD_ID)) ids.add("minecraft:" + key.getPath());
            else if (key.getNamespace().equals("minecraft")) ids.add("minecraft:" + modernPath(key.getPath()));
            else ids.add(key.toString());
        }
        return ids;
    }

    private static String modernPath(String path) {
        for (Map.Entry<String, String> exact : EXACT.entrySet()) {
            if (exact.getValue().equals(path)) return exact.getKey();
        }
        for (Map.Entry<String, String> prefix : PREFIXES.entrySet()) {
            if (path.startsWith(prefix.getValue())) return prefix.getKey() + path.substring(prefix.getValue().length());
        }
        return path;
    }

    private LegacySounds() {}
}
