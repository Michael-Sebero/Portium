package com.michaelsebero.portium.entity.villager;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.SoundEvent;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 1.14's villager professions: each but the jobless and nitwits is taken at a workstation, which the villager claims
 * (its job site) and works at, with that profession's sound. Farmers also pick up seeds and crops and till farmland.
 */
public enum VillagerProfession116 {
    NONE(() -> null, null),
    ARMORER(() -> PortiumBlocks.BLAST_FURNACE, PortiumSounds.ENTITY_VILLAGER_WORK_ARMORER),
    BUTCHER(() -> PortiumBlocks.SMOKER, PortiumSounds.ENTITY_VILLAGER_WORK_BUTCHER),
    CARTOGRAPHER(() -> PortiumBlocks.CARTOGRAPHY_TABLE, PortiumSounds.ENTITY_VILLAGER_WORK_CARTOGRAPHER),
    CLERIC(() -> Blocks.BREWING_STAND, PortiumSounds.ENTITY_VILLAGER_WORK_CLERIC),
    FARMER(() -> PortiumBlocks.COMPOSTER, PortiumSounds.ENTITY_VILLAGER_WORK_FARMER),
    FISHERMAN(() -> PortiumBlocks.BARREL, PortiumSounds.ENTITY_VILLAGER_WORK_FISHERMAN),
    FLETCHER(() -> PortiumBlocks.FLETCHING_TABLE, PortiumSounds.ENTITY_VILLAGER_WORK_FLETCHER),
    LEATHERWORKER(() -> Blocks.CAULDRON, PortiumSounds.ENTITY_VILLAGER_WORK_LEATHERWORKER),
    LIBRARIAN(() -> PortiumBlocks.LECTERN, PortiumSounds.ENTITY_VILLAGER_WORK_LIBRARIAN),
    MASON(() -> PortiumBlocks.STONECUTTER, PortiumSounds.ENTITY_VILLAGER_WORK_MASON),
    NITWIT(() -> null, null),
    SHEPHERD(() -> PortiumBlocks.LOOM, PortiumSounds.ENTITY_VILLAGER_WORK_SHEPHERD),
    TOOLSMITH(() -> PortiumBlocks.SMITHING_TABLE, PortiumSounds.ENTITY_VILLAGER_WORK_TOOLSMITH),
    WEAPONSMITH(() -> PortiumBlocks.GRINDSTONE, PortiumSounds.ENTITY_VILLAGER_WORK_WEAPONSMITH);

    private final Supplier<Block> workstation;
    @Nullable
    private final SoundEvent sound;

    VillagerProfession116(Supplier<Block> workstation, @Nullable SoundEvent sound) {
        this.workstation = workstation;
        this.sound = sound;
    }

    /** The name 1.14 saves and textures use. */
    public String getName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /** The workstation block, if this profession has one (and that block is enabled). */
    @Nullable
    public Block getWorkstation() {
        return this.workstation.get();
    }

    /** Whether a block state is this profession's workstation. */
    public boolean isWorkstation(IBlockState state) {
        Block block = getWorkstation();
        return block != null && state.getBlock() == block;
    }

    @Nullable
    public SoundEvent getSound() {
        return this.sound;
    }

    /** The items this profession picks up off the ground, beyond food (farmers: crops and seeds). */
    public Set<Item> getSpecificItems() {
        if (this != FARMER) return Collections.emptySet();
        Set<Item> items = new HashSet<>();
        Collections.addAll(items, Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS);
        return items;
    }

    /** Whether this profession takes a job site; the jobless look for any, nitwits never. */
    public boolean hasWorkstation() {
        return this != NONE && this != NITWIT;
    }

    public static VillagerProfession116 byName(String name) {
        String bare = name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name;
        for (VillagerProfession116 profession : values()) {
            if (profession.getName().equals(bare)) return profession;
        }
        return NONE;
    }

    /** The profession whose workstation this block is, if any. */
    @Nullable
    public static VillagerProfession116 byWorkstation(IBlockState state) {
        for (VillagerProfession116 profession : values()) {
            if (profession.isWorkstation(state)) return profession;
        }
        return null;
    }
}
