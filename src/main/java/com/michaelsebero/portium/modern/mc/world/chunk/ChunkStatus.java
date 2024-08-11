package com.michaelsebero.portium.modern.mc.world.chunk;

import com.michaelsebero.portium.modern.Srg;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 1.13's {@code net.minecraft.world.chunk.ChunkStatus}: how far a chunk has been generated, by 1.13's stages. 1.12
 * generates chunks its own way, so only the names and order are here; an unknown name (as in a 1.12 chunk's data,
 * which has none) is {@link #EMPTY} rather than null.
 */
public enum ChunkStatus {
    EMPTY("empty", -1, false, Type.PROTOCHUNK),
    BASE("base", 0, false, Type.PROTOCHUNK),
    CARVED("carved", 0, false, Type.PROTOCHUNK),
    LIQUID_CARVED("liquid_carved", 1, false, Type.PROTOCHUNK),
    DECORATED("decorated", 1, true, Type.PROTOCHUNK),
    LIGHTED("lighted", 1, true, Type.PROTOCHUNK),
    MOBS_SPAWNED("mobs_spawned", 0, true, Type.PROTOCHUNK),
    FINALIZED("finalized", 0, true, Type.PROTOCHUNK),
    FULLCHUNK("fullchunk", 0, true, Type.LEVELCHUNK),
    POSTPROCESSED("postprocessed", 0, true, Type.LEVELCHUNK);

    private static final Map<String, ChunkStatus> BY_NAME = new HashMap<>();

    // 1.16's statuses, which are 1.13's nearest: 1.12 generates chunks whole either way. Not final, as mods' access
    // transformers make 1.16's, to replace them.
    @com.michaelsebero.portium.modern.Srg("field_223226_a_")
    public static ChunkStatus EMPTY_116 = EMPTY;
    @com.michaelsebero.portium.modern.Srg("field_222606_b")
    public static ChunkStatus STRUCTURE_STARTS_116 = BASE;
    @com.michaelsebero.portium.modern.Srg("field_222607_c")
    public static ChunkStatus STRUCTURE_REFERENCES_116 = BASE;
    @com.michaelsebero.portium.modern.Srg("field_222608_d")
    public static ChunkStatus BIOMES_116 = BASE;
    @com.michaelsebero.portium.modern.Srg("field_222609_e")
    public static ChunkStatus NOISE_116 = BASE;
    @com.michaelsebero.portium.modern.Srg("field_222610_f")
    public static ChunkStatus SURFACE_116 = BASE;
    @com.michaelsebero.portium.modern.Srg("field_222611_g")
    public static ChunkStatus CARVERS_116 = CARVED;
    @com.michaelsebero.portium.modern.Srg("field_222612_h")
    public static ChunkStatus LIQUID_CARVERS_116 = LIQUID_CARVED;
    @com.michaelsebero.portium.modern.Srg("field_222613_i")
    public static ChunkStatus FEATURES_116 = DECORATED;
    @com.michaelsebero.portium.modern.Srg("field_222614_j")
    public static ChunkStatus LIGHT_116 = LIGHTED;
    @com.michaelsebero.portium.modern.Srg("field_222615_k")
    public static ChunkStatus SPAWN_116 = MOBS_SPAWNED;
    @com.michaelsebero.portium.modern.Srg("field_222616_l")
    public static ChunkStatus HEIGHTMAPS_116 = FINALIZED;
    @com.michaelsebero.portium.modern.Srg("field_222617_m")
    public static ChunkStatus FULL_116 = FULLCHUNK;

    static {
        for (ChunkStatus status : values()) BY_NAME.put(status.name, status);
    }

    private final String name;
    private final int taskRange;
    private final boolean heightmaps;
    private final Type type;

    ChunkStatus(String name, int taskRange, boolean heightmaps, Type type) {
        this.name = name;
        this.taskRange = taskRange;
        this.heightmaps = heightmaps;
        this.type = type;
    }

    @Srg("func_202127_a")
    public static ChunkStatus byName(String name) {
        return BY_NAME.getOrDefault(name, EMPTY);
    }

    @Srg("func_202125_b")
    public String getName() {
        return this.name;
    }

    @Srg("func_202128_c")
    public int getTaskRange() {
        return this.taskRange;
    }

    @Srg("func_202129_d")
    public Type getType() {
        return this.type;
    }

    @Nullable
    @Srg("func_201497_a_")
    public ChunkStatus getParent() {
        return ordinal() == 0 ? null : values()[ordinal() - 1];
    }

    @Srg("func_207794_f")
    public boolean shouldGenerateHeightmaps() {
        return this.heightmaps;
    }

    @Srg("func_209003_a")
    public boolean isAtLeast(ChunkStatus status) {
        return ordinal() >= status.ordinal();
    }

    public enum Type {
        PROTOCHUNK,
        LEVELCHUNK
    }
}
