package com.michaelsebero.portium.world.poi;

import com.michaelsebero.portium.entity.villager.VillagerProfession116;
import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;

import javax.annotation.Nullable;

/**
 * 1.14's points of interest that villagers use: a workstation per profession (one villager each), bed heads (one
 * sleeper each) and bells (any number meet there).
 */
public final class PoiType116 {

    public static final PoiType116 HOME = new PoiType116("home", null, 1);
    public static final PoiType116 MEETING = new PoiType116("meeting", null, 32);

    private static final PoiType116[] WORKSTATIONS = new PoiType116[VillagerProfession116.values().length];

    static {
        for (VillagerProfession116 profession : VillagerProfession116.values()) {
            if (profession.hasWorkstation()) WORKSTATIONS[profession.ordinal()] = new PoiType116(profession.getName(), profession, 1);
        }
    }

    private final String name;
    @Nullable
    private final VillagerProfession116 profession;
    private final int maxTickets;

    private PoiType116(String name, @Nullable VillagerProfession116 profession, int maxTickets) {
        this.name = name;
        this.profession = profession;
        this.maxTickets = maxTickets;
    }

    public String getName() {
        return this.name;
    }

    /** The profession this workstation gives; null for beds and bells. */
    @Nullable
    public VillagerProfession116 getProfession() {
        return this.profession;
    }

    public int getMaxTickets() {
        return this.maxTickets;
    }

    public boolean isWorkstation() {
        return this.profession != null;
    }

    /** The workstation type of a profession, if it has one. */
    @Nullable
    public static PoiType116 workstation(VillagerProfession116 profession) {
        return WORKSTATIONS[profession.ordinal()];
    }

    /** Blocks that are points of interest (beds are told apart by their part), made once blocks are registered. */
    @Nullable
    private static java.util.Map<net.minecraft.block.Block, PoiType116> byBlock;

    /** What point of interest a block state is, if any. */
    @Nullable
    public static PoiType116 of(IBlockState state) {
        if (state.getBlock() instanceof BlockBed) return state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD ? HOME : null;
        java.util.Map<net.minecraft.block.Block, PoiType116> blocks = byBlock;
        if (blocks == null) {
            blocks = new java.util.IdentityHashMap<>();
            if (PortiumBlocks.BELL != null) blocks.put(PortiumBlocks.BELL, MEETING);
            for (VillagerProfession116 profession : VillagerProfession116.values()) {
                net.minecraft.block.Block workstation = profession.hasWorkstation() ? profession.getWorkstation() : null;
                if (workstation != null) blocks.put(workstation, WORKSTATIONS[profession.ordinal()]);
            }
            byBlock = blocks;
        }
        return blocks.get(state.getBlock());
    }

    @Override
    public String toString() {
        return this.name;
    }
}
