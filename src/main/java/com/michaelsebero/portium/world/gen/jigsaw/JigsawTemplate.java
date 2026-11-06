package com.michaelsebero.portium.world.gen.jigsaw;

import com.michaelsebero.portium.compat.LegacyBlockStates;
import com.michaelsebero.portium.world.gen.structure.StructureTemplate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A 1.16 structure template as jigsaw structures use it: its blocks (each with its 1.16 name and properties, which
 * processors test, and the 1.12 state it becomes), its jigsaw blocks (where other pieces attach), and its entities.
 */
public final class JigsawTemplate {

    /** A block of the template: where, its 1.16 name and properties, its 1.12 state, and its block entity data. */
    public static final class Block {
        public final BlockPos pos;
        public final String name;
        public final Map<String, String> properties;
        public final IBlockState state;
        @Nullable
        public final NBTTagCompound nbt;

        Block(BlockPos pos, String name, Map<String, String> properties, IBlockState state, @Nullable NBTTagCompound nbt) {
            this.pos = pos;
            this.name = name;
            this.properties = properties;
            this.state = state;
            this.nbt = nbt;
        }
    }

    /** A jigsaw block: which way it faces (and its top, for aligned joints), and what it connects to. */
    public static final class Jigsaw {
        public final BlockPos pos;
        public final EnumFacing front;
        public final EnumFacing top;
        public final String pool;
        public final String name;
        public final String target;
        public final boolean rollable;
        public final String finalState;

        Jigsaw(BlockPos pos, EnumFacing front, EnumFacing top, String pool, String name, String target, boolean rollable, String finalState) {
            this.pos = pos;
            this.front = front;
            this.top = top;
            this.pool = pool;
            this.name = name;
            this.target = target;
            this.rollable = rollable;
            this.finalState = finalState;
        }

        /** This jigsaw as seen in a piece rotated so: its position and faces turned. */
        public Jigsaw rotated(Rotation rotation) {
            return new Jigsaw(StructureTemplate.transform(this.pos, Mirror.NONE, rotation, BlockPos.ORIGIN), rotate(this.front, rotation),
                    rotate(this.top, rotation), this.pool, this.name, this.target, this.rollable, this.finalState);
        }

        /** 1.16's {@code JigsawBlock.canAttach}: facing each other, names matching, tops lined up unless the joint rolls. */
        public boolean canAttach(Jigsaw other) {
            return this.front == other.front.getOpposite() && (this.rollable || this.top == other.top) && this.target.equals(other.name);
        }
    }

    /** An entity of the template, at its position within it. */
    public static final class EntityInfo {
        public final Vec3d pos;
        public final NBTTagCompound nbt;

        EntityInfo(Vec3d pos, NBTTagCompound nbt) {
            this.pos = pos;
            this.nbt = nbt;
        }
    }

    private final BlockPos size;
    private final List<Block> blocks;
    private final List<Jigsaw> jigsaws;
    private final List<EntityInfo> entities;

    private JigsawTemplate(BlockPos size, List<Block> blocks, List<Jigsaw> jigsaws, List<EntityInfo> entities) {
        this.size = size;
        this.blocks = blocks;
        this.jigsaws = jigsaws;
        this.entities = entities;
    }

    public BlockPos getSize() {
        return this.size;
    }

    public List<Block> getBlocks() {
        return this.blocks;
    }

    public List<EntityInfo> getEntities() {
        return this.entities;
    }

    /** The jigsaws in a rotation, shuffled (as 1.16 tries them in random order). */
    public List<Jigsaw> getJigsaws(Rotation rotation, Random random) {
        List<Jigsaw> list = new ArrayList<>(this.jigsaws.size());
        for (Jigsaw jigsaw : this.jigsaws) list.add(jigsaw.rotated(rotation));
        Collections.shuffle(list, random);
        return list;
    }

    /** The box covered when placed at an origin in a rotation (around the template's corner, as 1.16's pieces turn). */
    public StructureBoundingBox getBoundingBox(BlockPos origin, Rotation rotation) {
        BlockPos a = StructureTemplate.transform(BlockPos.ORIGIN, Mirror.NONE, rotation, BlockPos.ORIGIN).add(origin);
        BlockPos b = StructureTemplate.transform(this.size.add(-1, -1, -1), Mirror.NONE, rotation, BlockPos.ORIGIN).add(origin);
        return new StructureBoundingBox(a, b);
    }

    public static JigsawTemplate read(NBTTagCompound tag) {
        NBTTagList sizeTag = tag.getTagList("size", 3);
        BlockPos size = new BlockPos(sizeTag.getIntAt(0), sizeTag.getIntAt(1), sizeTag.getIntAt(2));
        NBTTagList paletteTag = tag.hasKey("palettes", 9) ? (NBTTagList) tag.getTagList("palettes", 9).get(0) : tag.getTagList("palette", 10);
        String[] names = new String[paletteTag.tagCount()];
        List<Map<String, String>> props = new ArrayList<>();
        IBlockState[] states = new IBlockState[names.length];
        for (int i = 0; i < names.length; i++) {
            NBTTagCompound entry = paletteTag.getCompoundTagAt(i);
            names[i] = entry.getString("Name");
            Map<String, String> properties = new HashMap<>();
            NBTTagCompound propertiesTag = entry.getCompoundTag("Properties");
            for (String key : propertiesTag.getKeySet()) properties.put(key, propertiesTag.getString(key));
            props.add(properties);
            states[i] = names[i].equals("minecraft:jigsaw") ? Blocks.AIR.getDefaultState() : LegacyBlockStates.convert(names[i], properties);
        }
        List<Block> blocks = new ArrayList<>();
        List<Jigsaw> jigsaws = new ArrayList<>();
        NBTTagList blocksTag = tag.getTagList("blocks", 10);
        for (int i = 0; i < blocksTag.tagCount(); i++) {
            NBTTagCompound entry = blocksTag.getCompoundTagAt(i);
            NBTTagList posTag = entry.getTagList("pos", 3);
            BlockPos pos = new BlockPos(posTag.getIntAt(0), posTag.getIntAt(1), posTag.getIntAt(2));
            int index = entry.getInteger("state");
            if (index >= names.length) continue;
            NBTTagCompound nbt = entry.hasKey("nbt", 10) ? entry.getCompoundTag("nbt") : null;
            if (names[index].equals("minecraft:jigsaw") && nbt != null) {
                String orientation = props.get(index).getOrDefault("orientation", "north_up");
                String[] faces = orientation.split("_");
                EnumFacing front = EnumFacing.byName(faces[0]);
                EnumFacing top = faces.length > 1 ? EnumFacing.byName(faces[1]) : EnumFacing.UP;
                if (front == null) front = EnumFacing.NORTH;
                // 1.16's default joint: aligned for sideways jigsaws, rollable for up and down ones.
                String joint = nbt.getString("joint");
                boolean rollable = joint.equals("rollable") || !joint.equals("aligned") && front.getAxis().isVertical();
                jigsaws.add(new Jigsaw(pos, front, top == null ? EnumFacing.UP : top, bare(nbt.getString("pool")), nbt.getString("name"),
                        nbt.getString("target"), rollable, nbt.hasKey("final_state") ? nbt.getString("final_state") : "minecraft:air"));
            }
            blocks.add(new Block(pos, names[index], props.get(index), states[index], nbt));
        }
        List<EntityInfo> entities = new ArrayList<>();
        NBTTagList entitiesTag = tag.getTagList("entities", 10);
        for (int i = 0; i < entitiesTag.tagCount(); i++) {
            NBTTagCompound entry = entitiesTag.getCompoundTagAt(i);
            NBTTagList posTag = entry.getTagList("pos", 6);
            entities.add(new EntityInfo(new Vec3d(posTag.getDoubleAt(0), posTag.getDoubleAt(1), posTag.getDoubleAt(2)), entry.getCompoundTag("nbt")));
        }
        return new JigsawTemplate(size, blocks, jigsaws, entities);
    }

    /** 1.16's feature piece's one jigsaw: at its block, facing down, named "bottom". */
    static Jigsaw featureJigsaw() {
        return new Jigsaw(BlockPos.ORIGIN, EnumFacing.DOWN, EnumFacing.SOUTH, "empty", "minecraft:bottom", "minecraft:empty", true, "minecraft:air");
    }

    static String bare(String name) {
        return name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name;
    }

    /** A direction turned by a rotation (up and down stay). */
    public static EnumFacing rotate(EnumFacing facing, Rotation rotation) {
        return facing.getAxis().isVertical() ? facing : rotation.rotate(facing);
    }
}
