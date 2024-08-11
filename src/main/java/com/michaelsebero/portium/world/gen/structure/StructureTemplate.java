package com.michaelsebero.portium.world.gen.structure;

import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.compat.LegacyBlockStates;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * A structure template in 1.13's {@code .nbt} format, with its palette translated to 1.12 block states. Placement
 * follows 1.13: rotation around a pivot, optional skipping of air, per-block integrity, clipping to a box, and data
 * markers (structure blocks in data mode) reported back instead of placed.
 *
 * <p>A template may have several palettes ({@code palettes}, as shipwrecks do for their wood types) instead of one
 * ({@code palette}). 1.13.2 picks one from the template's position, and decides integrity from the same position, so
 * every chunk a template crosses agrees on both.
 */
public final class StructureTemplate {

    private final BlockPos size;
    /** The blocks, once for each palette. */
    private final List<List<BlockInfo>> palettes;

    private StructureTemplate(BlockPos size, List<List<BlockInfo>> palettes) {
        this.size = size;
        this.palettes = palettes;
    }

    public BlockPos getSize() {
        return this.size;
    }

    public static StructureTemplate read(NBTTagCompound tag) {
        NBTTagList sizeTag = tag.getTagList("size", 3);
        BlockPos size = new BlockPos(sizeTag.getIntAt(0), sizeTag.getIntAt(1), sizeTag.getIntAt(2));
        NBTTagList blocksTag = tag.getTagList("blocks", 10);
        List<List<BlockInfo>> palettes = new ArrayList<>();
        if (tag.hasKey("palettes", 9)) {
            NBTTagList palettesTag = tag.getTagList("palettes", 9);
            for (int i = 0; i < palettesTag.tagCount(); i++) palettes.add(readBlocks((NBTTagList) palettesTag.get(i), blocksTag));
        } else {
            palettes.add(readBlocks(tag.getTagList("palette", 10), blocksTag));
        }
        return new StructureTemplate(size, Collections.unmodifiableList(palettes));
    }

    private static List<BlockInfo> readBlocks(NBTTagList paletteTag, NBTTagList blocksTag) {
        IBlockState[] palette = new IBlockState[paletteTag.tagCount()];
        boolean[] waterlogged = new boolean[palette.length];
        for (int i = 0; i < palette.length; i++) {
            NBTTagCompound entry = paletteTag.getCompoundTagAt(i);
            Map<String, String> properties = new HashMap<>();
            NBTTagCompound propertiesTag = entry.getCompoundTag("Properties");
            for (String key : propertiesTag.getKeySet()) properties.put(key, propertiesTag.getString(key));
            palette[i] = LegacyBlockStates.convert(entry.getString("Name"), properties);
            waterlogged[i] = "true".equals(properties.get("waterlogged"));
        }
        List<BlockInfo> blocks = new ArrayList<>(blocksTag.tagCount());
        for (int i = 0; i < blocksTag.tagCount(); i++) {
            NBTTagCompound entry = blocksTag.getCompoundTagAt(i);
            NBTTagList posTag = entry.getTagList("pos", 3);
            BlockPos pos = new BlockPos(posTag.getIntAt(0), posTag.getIntAt(1), posTag.getIntAt(2));
            int stateIndex = entry.getInteger("state");
            IBlockState state = stateIndex < palette.length ? palette[stateIndex] : Blocks.AIR.getDefaultState();
            blocks.add(new BlockInfo(pos, state, entry.hasKey("nbt", 10) ? entry.getCompoundTag("nbt") : null,
                    stateIndex < palette.length && waterlogged[stateIndex]));
        }
        return Collections.unmodifiableList(blocks);
    }

    /** 1.13.2's random for a template placed at {@code origin}, from its x and z alone. */
    private static Random positionRandom(BlockPos origin) {
        int x = origin.getX();
        int z = origin.getZ();
        return new Random((long) (x * x * 4987142) + (long) (x * 5947611) + (long) (z * z) * 4392871L + (long) (z * 389711) ^ 987234911L);
    }

    /** 1.13's template transform: mirror, then rotate around the pivot. */
    public static BlockPos transform(BlockPos pos, Mirror mirror, Rotation rotation, BlockPos pivot) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        boolean mirrored = true;
        switch (mirror) {
            case LEFT_RIGHT: z = -z; break;
            case FRONT_BACK: x = -x; break;
            default: mirrored = false;
        }
        int px = pivot.getX();
        int pz = pivot.getZ();
        switch (rotation) {
            case COUNTERCLOCKWISE_90: return new BlockPos(px - pz + z, y, px + pz - x);
            case CLOCKWISE_90: return new BlockPos(px + pz - z, y, pz - px + x);
            case CLOCKWISE_180: return new BlockPos(px + px - x, y, pz + pz - z);
            default: return mirrored ? new BlockPos(x, y, z) : pos;
        }
    }

    /** The box this template covers when placed at {@code origin}. */
    public StructureBoundingBox getBoundingBox(BlockPos origin, Settings settings) {
        BlockPos a = transform(BlockPos.ORIGIN, settings.mirror, settings.rotation, settings.pivot).add(origin);
        BlockPos b = transform(this.size.add(-1, -1, -1), settings.mirror, settings.rotation, settings.pivot).add(origin);
        return new StructureBoundingBox(a, b);
    }

    /**
     * Places the template, calling {@code dataMarkers} with the world position and metadata of each data-mode
     * structure block (which is not itself placed).
     */
    public void place(World world, BlockPos origin, Settings settings, @Nullable BiConsumer<BlockPos, String> dataMarkers) {
        if (this.palettes.isEmpty()) return;
        List<BlockInfo> blocks = this.palettes.get(positionRandom(origin).nextInt(this.palettes.size()));
        // Every block draws its integrity roll, in template order, whether or not it ends up placed.
        Random integrity = positionRandom(origin);
        for (BlockInfo info : blocks) {
            boolean intact = settings.integrity >= 1.0F || integrity.nextFloat() <= settings.integrity;
            BlockPos pos = transform(info.pos, settings.mirror, settings.rotation, settings.pivot).add(origin);
            if (settings.clip != null && !settings.clip.isVecInside(pos)) continue;
            IBlockState state = info.state;
            if (state.getBlock() == Blocks.STRUCTURE_BLOCK) {
                if (dataMarkers != null && info.nbt != null && "DATA".equals(info.nbt.getString("mode"))) {
                    dataMarkers.accept(pos, info.nbt.getString("metadata"));
                }
                continue;
            }
            if (settings.ignoreAir && state.getBlock() == Blocks.AIR) continue;
            if (state.getBlock() == Blocks.STRUCTURE_VOID) continue;
            if (!intact) continue;
            state = state.withMirror(settings.mirror).withRotation(settings.rotation);
            // Blocks that can hold water keep it when placed into a water source, as 1.13's templates do, or next to one.
            boolean intoWater = WaterUtil.isWaterSource(world.getBlockState(pos)) || Waterlogging.isOpenToWater(world, pos);
            if (info.nbt != null) {
                // Like 1.12's templates: clear out an existing block entity so it doesn't spill its contents.
                TileEntity existing = world.getTileEntity(pos);
                if (existing != null) {
                    if (existing instanceof IInventory) ((IInventory) existing).clear();
                    world.setBlockState(pos, Blocks.BARRIER.getDefaultState(), 4);
                }
            }
            boolean placed = world.setBlockState(pos, state, 2);
            if ((info.waterlogged || intoWater) && Waterlogging.canWaterlog(state)) Waterlogging.setWaterloggedQuietly(world, pos);
            if (!placed || info.nbt == null) continue;
            TileEntity tile = world.getTileEntity(pos);
            if (tile != null) {
                NBTTagCompound data = info.nbt.copy();
                data.setInteger("x", pos.getX());
                data.setInteger("y", pos.getY());
                data.setInteger("z", pos.getZ());
                tile.readFromNBT(data);
                tile.mirror(settings.mirror);
                tile.rotate(settings.rotation);
            }
        }
    }

    public static final class Settings {

        Rotation rotation = Rotation.NONE;
        Mirror mirror = Mirror.NONE;
        BlockPos pivot = BlockPos.ORIGIN;
        float integrity = 1.0F;
        boolean ignoreAir;
        @Nullable
        StructureBoundingBox clip;

        public Settings rotation(Rotation rotation) {
            this.rotation = rotation;
            return this;
        }

        public Settings pivot(BlockPos pivot) {
            this.pivot = pivot;
            return this;
        }

        public Settings integrity(float integrity) {
            this.integrity = integrity;
            return this;
        }

        public Settings ignoreAir() {
            this.ignoreAir = true;
            return this;
        }

        public Settings clip(@Nullable StructureBoundingBox clip) {
            this.clip = clip;
            return this;
        }
    }

    private static final class BlockInfo {

        final BlockPos pos;
        final IBlockState state;
        @Nullable
        final NBTTagCompound nbt;
        final boolean waterlogged;

        BlockInfo(BlockPos pos, IBlockState state, @Nullable NBTTagCompound nbt, boolean waterlogged) {
            this.pos = pos;
            this.state = state;
            this.nbt = nbt;
            this.waterlogged = waterlogged;
        }
    }
}
