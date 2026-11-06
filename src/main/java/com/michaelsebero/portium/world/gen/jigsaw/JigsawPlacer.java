package com.michaelsebero.portium.world.gen.jigsaw;

import com.michaelsebero.portium.compat.LegacyBlockEntityNbt;
import com.michaelsebero.portium.compat.LegacyBlockStates;
import com.michaelsebero.portium.compat.LegacyEntityNbt;
import com.michaelsebero.portium.entity.villager.EntityVillager116;
import com.michaelsebero.portium.entity.villager.VillagerEvents116;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawAssembler.Placed;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Element;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Feature;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.ListElement;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Projection;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Single;
import com.michaelsebero.portium.world.gen.structure.StructureTemplate;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Places a laid-out jigsaw piece, the part inside a clip box (the chunk being populated): its template's blocks after
 * its processors (jigsaws become their final state), block entities and entities; terrain-matching pieces follow the
 * ground block by block. Rigid pieces get a foundation down to the ground, standing in for 1.16's terrain smoothing.
 */
public final class JigsawPlacer {

    public static void place(JigsawData data, Placed piece, World world, StructureBoundingBox clip, HeightSampler heights) {
        place(data, piece.element, piece.origin, piece.rotation, world, clip, heights);
    }

    private static void place(JigsawData data, Element element, BlockPos origin, Rotation rotation, World world, StructureBoundingBox clip,
                              HeightSampler heights) {
        if (element instanceof ListElement) {
            for (Element child : ((ListElement) element).elements) place(data, child, origin, rotation, world, clip, heights);
        } else if (element instanceof Single) {
            placeSingle(data, (Single) element, origin, rotation, world, clip, heights);
        } else if (element instanceof Feature && clip.isVecInside(origin)) {
            placeFeature(((Feature) element).feature, world, origin);
        }
    }

    /** Set while the jigsaw screen's Generate places a structure with "keep jigsaws" on (server thread only). */
    static boolean keepJigsaws;

    private static void placeSingle(JigsawData data, Single single, BlockPos origin, Rotation rotation, World world, StructureBoundingBox clip,
                                    HeightSampler heights) {
        JigsawTemplate template = data.template(single.template);
        if (template == null) return;
        List<JigsawPools.Processor> processors = data.pools().getProcessors(single.processors);
        boolean terrain = single.projection == Projection.TERRAIN_MATCHING;
        Random pieceRandom = new Random(MathHelper.getPositionRandom(origin));
        for (JigsawTemplate.Block block : template.getBlocks()) {
            BlockPos local = StructureTemplate.transform(block.pos, Mirror.NONE, rotation, BlockPos.ORIGIN);
            BlockPos pos = local.add(origin);
            // Terrain-matching pieces (streets): each block at the ground's height less one, plus its height in the template.
            if (terrain) pos = new BlockPos(pos.getX(), heights.surface(pos.getX(), pos.getZ()) - 1 + block.pos.getY(), pos.getZ());
            if (!clip.isVecInside(pos)) continue;
            String name = block.name;
            Map<String, String> properties = block.properties;
            IBlockState state = block.state;
            if (name.equals("minecraft:structure_void") || name.equals("minecraft:structure_block")) continue;
            boolean jigsaw = name.equals("minecraft:jigsaw") && !keepJigsaws;
            if (jigsaw) {
                String finalState = jigsawFinalState(template, block);
                int bracket = finalState.indexOf('[');
                name = bracket >= 0 ? finalState.substring(0, bracket) : finalState;
                properties = bracket >= 0 ? parseProperties(finalState.substring(bracket + 1, finalState.length() - 1)) : new HashMap<>();
                if (name.equals("minecraft:structure_void")) continue;
            }
            JigsawProcessors.Candidate candidate = new JigsawProcessors.Candidate(name, properties);
            if (!JigsawProcessors.process(processors, candidate, world, pos, origin, pieceRandom)) continue;
            // 1.16's legacy pieces (villages, outposts) ignore air, last of their processors: a street's template is air
            // around its paths, which would otherwise dig the grass out beside them down to the dirt.
            if (single.legacy && candidate.name.equals("minecraft:air")) continue;
            if (candidate.changed || jigsaw || candidate.name.equals("minecraft:jigsaw")) state = LegacyBlockStates.convert(candidate.name, candidate.properties);
            state = state.withRotation(rotation);
            if (block.nbt != null && !jigsaw) {
                TileEntity existing = world.getTileEntity(pos);
                if (existing instanceof IInventory) ((IInventory) existing).clear();
            }
            world.setBlockState(pos, state, 2);
            if ("true".equals(candidate.properties.get("waterlogged")) && Waterlogging.canWaterlog(state)) Waterlogging.setWaterloggedQuietly(world, pos);
            if (block.nbt != null && !jigsaw) {
                TileEntity tile = world.getTileEntity(pos);
                if (tile != null) {
                    NBTTagCompound tag = LegacyBlockEntityNbt.convert(block.nbt.copy(), candidate.name, candidate.properties);
                    tag.setInteger("x", pos.getX());
                    tag.setInteger("y", pos.getY());
                    tag.setInteger("z", pos.getZ());
                    tile.readFromNBT(tag);
                    tile.rotate(rotation);
                }
            }
        }
        if (!terrain) foundation(template, origin, rotation, world, clip);
        for (JigsawTemplate.EntityInfo info : template.getEntities()) {
            Vec3d at = rotate(info.pos, rotation).add(origin.getX(), origin.getY(), origin.getZ());
            if (!clip.isVecInside(new BlockPos(at))) continue;
            spawn(world, info.nbt, at, rotation);
        }
    }

    /** A jigsaw block's final state, from the template's jigsaw list (where its block entity data was read). */
    private static String jigsawFinalState(JigsawTemplate template, JigsawTemplate.Block block) {
        return block.nbt != null && block.nbt.hasKey("final_state") ? block.nbt.getString("final_state") : "minecraft:air";
    }

    private static Map<String, String> parseProperties(String text) {
        Map<String, String> properties = new HashMap<>();
        for (String pair : text.split(",")) {
            int equals = pair.indexOf('=');
            if (equals > 0) properties.put(pair.substring(0, equals).trim(), pair.substring(equals + 1).trim());
        }
        return properties;
    }

    /**
     * Under each solid block of the piece's bottom layer, the ground's filler down to solid ground (at most a dozen
     * blocks), so pieces on slopes stand on earth rather than air.
     */
    private static void foundation(JigsawTemplate template, BlockPos origin, Rotation rotation, World world, StructureBoundingBox clip) {
        for (JigsawTemplate.Block block : template.getBlocks()) {
            if (block.pos.getY() != 0 || !block.state.getMaterial().isSolid()) continue;
            BlockPos pos = StructureTemplate.transform(block.pos, Mirror.NONE, rotation, BlockPos.ORIGIN).add(origin);
            if (!clip.isVecInside(pos)) continue;
            Biome biome = world.getBiome(pos);
            IBlockState filler = biome.fillerBlock.getMaterial() == Material.SAND ? Blocks.SANDSTONE.getDefaultState() : biome.fillerBlock;
            BlockPos below = pos.down();
            for (int i = 0; i < 12 && below.getY() > 0; i++, below = below.down()) {
                IBlockState state = world.getBlockState(below);
                if (state.getMaterial().isSolid() && !state.getMaterial().isReplaceable()) break;
                world.setBlockState(below, filler, 2);
            }
        }
    }

    /** A template entity: 1.16.5's data made 1.12's; villagers in 1.16.5 worlds as 1.14 villagers with their data. */
    private static void spawn(World world, NBTTagCompound modern, Vec3d at, Rotation rotation) {
        String id = modern.getString("id");
        Entity entity;
        if (id.equals("minecraft:villager") && VillagerEvents116.isActive(world)) {
            entity = new EntityVillager116(world);
            NBTTagCompound tag = modern.copy();
            tag.removeTag("UUID");
            tag.removeTag("UUIDMost");
            tag.removeTag("UUIDLeast");
            tag.removeTag("Pos");
            entity.readFromNBT(withPosition(tag, at));
        } else {
            NBTTagCompound legacy = LegacyEntityNbt.convert(modern.copy());
            legacy.removeTag("UUIDMost");
            legacy.removeTag("UUIDLeast");
            entity = EntityList.createEntityFromNBT(withPosition(legacy, at), world);
        }
        if (entity == null) return;
        entity.setLocationAndAngles(at.x, at.y, at.z, entity.getRotatedYaw(rotation), entity.rotationPitch);
        if (entity instanceof EntityLiving) ((EntityLiving) entity).enablePersistence();
        world.spawnEntity(entity);
    }

    private static NBTTagCompound withPosition(NBTTagCompound tag, Vec3d at) {
        net.minecraft.nbt.NBTTagList pos = new net.minecraft.nbt.NBTTagList();
        pos.appendTag(new net.minecraft.nbt.NBTTagDouble(at.x));
        pos.appendTag(new net.minecraft.nbt.NBTTagDouble(at.y));
        pos.appendTag(new net.minecraft.nbt.NBTTagDouble(at.z));
        tag.setTag("Pos", pos);
        return tag;
    }

    /** A position within a template turned as its blocks turn (around the template's corner). */
    private static Vec3d rotate(Vec3d pos, Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90:
                return new Vec3d(-pos.z, pos.y, pos.x);
            case CLOCKWISE_180:
                return new Vec3d(-pos.x, pos.y, -pos.z);
            case COUNTERCLOCKWISE_90:
                return new Vec3d(pos.z, pos.y, -pos.x);
            default:
                return pos;
        }
    }

    /** 1.16's configured features that village pools place (trees, piles, patches), done with 1.12's means. */
    private static void placeFeature(String feature, World world, BlockPos pos) {
        Random random = new Random(MathHelper.getPositionRandom(pos));
        switch (feature) {
            case "oak":
                tree(new net.minecraft.world.gen.feature.WorldGenTrees(false), world, random, pos);
                break;
            case "spruce":
                tree(new net.minecraft.world.gen.feature.WorldGenTaiga2(false), world, random, pos);
                break;
            case "pine":
                tree(new net.minecraft.world.gen.feature.WorldGenTaiga1(), world, random, pos);
                break;
            case "acacia":
                tree(new net.minecraft.world.gen.feature.WorldGenSavannaTree(false), world, random, pos);
                break;
            case "pile_hay":
                pile(world, random, pos, Blocks.HAY_BLOCK.getDefaultState());
                break;
            case "pile_melon":
                pile(world, random, pos, Blocks.MELON_BLOCK.getDefaultState());
                break;
            case "pile_pumpkin":
                pile(world, random, pos, Blocks.PUMPKIN.getDefaultState());
                break;
            case "pile_snow":
                pile(world, random, pos, Blocks.SNOW.getDefaultState());
                break;
            case "pile_ice":
                pile(world, random, pos, random.nextInt(5) == 0 ? Blocks.ICE.getDefaultState() : Blocks.PACKED_ICE.getDefaultState());
                break;
            case "flower_plain":
                patch(world, random, pos, Blocks.RED_FLOWER.getDefaultState(), 16);
                break;
            case "patch_grass_savanna":
            case "patch_taiga_grass":
            case "patch_grass_plain":
                patch(world, random, pos, Blocks.TALLGRASS.getStateFromMeta(1), 24);
                break;
            case "patch_cactus":
                if (Blocks.CACTUS.canPlaceBlockAt(world, pos)) world.setBlockState(pos, Blocks.CACTUS.getDefaultState(), 2);
                break;
            case "patch_berry_bush":
            case "patch_berry_sparse":
                if (com.michaelsebero.portium.init.PortiumBlocks.SWEET_BERRY_BUSH != null) {
                    patch(world, random, pos, com.michaelsebero.portium.init.PortiumBlocks.SWEET_BERRY_BUSH.getDefaultState(), 8);
                }
                break;
            default:
                break;
        }
    }

    private static void tree(WorldGenAbstractTree tree, World world, Random random, BlockPos pos) {
        tree.generate(world, random, pos);
    }

    /** 1.16's block piles: a rough heap a few blocks across. */
    private static void pile(World world, Random random, BlockPos center, IBlockState state) {
        int radius = 2 + random.nextInt(2);
        for (BlockPos pos : BlockPos.getAllInBoxMutable(center.add(-radius, 0, -radius), center.add(radius, 1, radius))) {
            int dx = pos.getX() - center.getX(), dz = pos.getZ() - center.getZ();
            if (dx * dx + dz * dz > radius * radius + random.nextInt(2) || pos.getY() > center.getY() && random.nextBoolean()) continue;
            BlockPos below = pos.down();
            if (world.isAirBlock(pos) && world.getBlockState(below).isSideSolid(world, below, net.minecraft.util.EnumFacing.UP) && random.nextFloat() < 0.75F) {
                world.setBlockState(pos.toImmutable(), state, 2);
            }
        }
    }

    /** A patch of plants scattered on grass around a spot. */
    private static void patch(World world, Random random, BlockPos center, IBlockState plant, int tries) {
        for (int i = 0; i < tries; i++) {
            BlockPos pos = center.add(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
            if (world.isAirBlock(pos) && plant.getBlock().canPlaceBlockAt(world, pos)) world.setBlockState(pos, plant, 2);
        }
    }

    private JigsawPlacer() {}
}
