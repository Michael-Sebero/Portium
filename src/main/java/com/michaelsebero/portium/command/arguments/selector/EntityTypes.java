package com.michaelsebero.portium.command.arguments.selector;

import com.michaelsebero.portium.Tags;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity type ids as 1.13 writes them. 1.13 renamed twelve of 1.12's ({@code minecraft:evoker} was
 * {@code evocation_illager}); the mobs 1.13 added are registered under Portium's namespace, so {@code minecraft:cod}
 * means {@code portium:cod}.
 */
public final class EntityTypes {

    public static final ResourceLocation PLAYER = new ResourceLocation("player");

    /** 1.13 names and the 1.12 names they replaced. */
    private static final Map<String, String> RENAMED = new HashMap<>();
    private static final Map<String, String> MODERN = new HashMap<>();

    static {
        String[] pairs = {"evoker", "evocation_illager", "vindicator", "vindication_illager", "illusioner", "illusion_illager",
                "iron_golem", "villager_golem", "snow_golem", "snowman", "experience_orb", "xp_orb", "experience_bottle", "xp_bottle",
                "eye_of_ender", "eye_of_ender_signal", "end_crystal", "ender_crystal", "firework_rocket", "fireworks_rocket",
                "command_block_minecart", "commandblock_minecart", "evoker_fangs", "evocation_fangs"};
        for (int i = 0; i < pairs.length; i += 2) {
            RENAMED.put("minecraft:" + pairs[i], "minecraft:" + pairs[i + 1]);
            MODERN.put("minecraft:" + pairs[i + 1], "minecraft:" + pairs[i]);
        }
        // Later renames, read from newer saves only (1.13's names stay the ones shown).
        RENAMED.put("minecraft:zombified_piglin", "minecraft:zombie_pigman");
    }

    @Nullable
    public static ResourceLocation toLegacy(ResourceLocation id) {
        if (PLAYER.equals(id)) return PLAYER;
        String renamed = RENAMED.get(id.toString());
        if (renamed != null) return new ResourceLocation(renamed);
        if (EntityList.isRegistered(id)) return id;
        if ("minecraft".equals(id.getNamespace())) {
            ResourceLocation portium = new ResourceLocation(Tags.MOD_ID, id.getPath());
            if (EntityList.isRegistered(portium)) return portium;
        }
        return null;
    }

    /** The id 1.13 would show for an entity type. */
    public static ResourceLocation toModern(ResourceLocation legacy) {
        String modern = MODERN.get(legacy.toString());
        if (modern != null) return new ResourceLocation(modern);
        return Tags.MOD_ID.equals(legacy.getNamespace()) ? new ResourceLocation(legacy.getPath()) : legacy;
    }

    public static List<ResourceLocation> modernIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        ids.add(PLAYER);
        for (ResourceLocation id : EntityList.getEntityNameList()) ids.add(toModern(id));
        return ids;
    }

    private EntityTypes() {}
}
