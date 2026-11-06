package com.michaelsebero.portium.world.spawn;

import com.michaelsebero.portium.world.WorldGeneration;
import net.minecraft.world.World;

/**
 * Which worlds Portium's mobs spawn in by themselves: a world made with 1.12.2's generation gets none, one made with
 * 1.13.2's gets 1.13's (fish, turtles, dolphins, drowned, phantoms), and one made with 1.16.5's gets them all. Spawn
 * eggs, commands and spawners aren't affected.
 */
public final class MobVersions {

    private static final String PORTIUM_ENTITIES = "com.michaelsebero.portium.entity.";
    /** Portium's mobs from 1.14 on, which come with 1.16.5's generation. */
    private static final String[] FROM_114 = {"com.michaelsebero.portium.entity.passive.EntityFox", "com.michaelsebero.portium.entity.passive.EntityPanda",
            "com.michaelsebero.portium.entity.passive.EntityCat", "com.michaelsebero.portium.entity.monster.EntityPillager",
            "com.michaelsebero.portium.entity.monster.EntityRavager", "com.michaelsebero.portium.entity.merchant.",
            "com.michaelsebero.portium.entity.monster.piglin.", "com.michaelsebero.portium.entity.monster.EntityHoglin",
            "com.michaelsebero.portium.entity.monster.EntityZoglin", "com.michaelsebero.portium.entity.passive.EntityStrider",
            "com.michaelsebero.portium.entity.passive.bee."};

    /** Whether a mob of this class may spawn by itself (biome spawns, chunk generation, Portium's spawners) in the world. */
    public static boolean spawnsIn(World world, Class<?> type) {
        String name = type.getName();
        if (!name.startsWith(PORTIUM_ENTITIES)) return true;
        for (String later : FROM_114) {
            if (name.startsWith(later)) return WorldGeneration.is116(world);
        }
        return WorldGeneration.isModern(world);
    }

    private MobVersions() {}
}
