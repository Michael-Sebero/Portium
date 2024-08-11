package com.michaelsebero.portium.world.waterlog;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDaylightDetector;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockEnchantmentTable;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneTorch;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Which blocks can hold water, besides Portium's own aquatic blocks and blocks that say so themselves.
 * <ul>
 * <li>Vanilla: 1.13's (stairs, slabs, fences, panes and bars, chests, ladders, signs, trapdoors, walls), and also
 * enchanting tables, anvils, mob heads, redstone torches, dust, repeaters and comparators, hoppers, doors, fence
 * gates, levers, daylight sensors, buttons, pressure plates, leaves, shulker boxes and pistons.</li>
 * <li>Other mods' blocks: subclasses of those, and any block that isn't a full cube but blocks movement, unless it's
 * a plant or a fluid ({@code mechanics.waterlogModBlocks}).</li>
 * <li>The config's own lists of block ids, which win over the rules above.</li>
 * </ul>
 */
final class WaterlogRules {

    private static volatile String[] listedIds;
    private static volatile String[] excludedIds;
    private static volatile Set<Block> listed = Collections.emptySet();
    private static volatile Set<Block> excluded = Collections.emptySet();

    static boolean canWaterlog(IBlockState state) {
        Block block = state.getBlock();
        refreshLists();
        if (excluded.contains(block)) return false;
        if (listed.contains(block)) return true;
        if (isWaterloggableKind(block)) return true;
        return PortiumConfig.mechanics.waterlogModBlocks && isModBlockShape(block, state);
    }

    private static boolean isWaterloggableKind(Block block) {
        // 1.13's own.
        if (block instanceof BlockStairs || block instanceof BlockSlab && !((BlockSlab) block).isDouble() || block instanceof BlockFence
                || block instanceof BlockPane || block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockLadder
                || block instanceof BlockSign || block instanceof BlockTrapDoor || block instanceof BlockWall) {
            return true;
        }
        // Portium's additions.
        return block instanceof BlockEnchantmentTable || block instanceof BlockAnvil || block instanceof BlockSkull
                || block instanceof BlockRedstoneTorch || block instanceof BlockRedstoneWire || block instanceof BlockRedstoneDiode
                || block instanceof BlockHopper || block instanceof BlockDoor || block instanceof BlockFenceGate || block instanceof BlockLever
                || block instanceof BlockDaylightDetector || block instanceof BlockButton || block instanceof BlockBasePressurePlate
                || block instanceof BlockLeaves || block instanceof BlockShulkerBox || block instanceof BlockPistonBase
                || block instanceof BlockPistonExtension;
    }

    /** Another mod's block shaped like something that could stand in water: solid, but not a whole cube. */
    private static boolean isModBlockShape(Block block, IBlockState state) {
        ResourceLocation id = block.getRegistryName();
        if (id == null || "minecraft".equals(id.getNamespace()) || Tags.MOD_ID.equals(id.getNamespace())) return false;
        if (block instanceof BlockLiquid || block instanceof IFluidBlock || block instanceof IPlantable || block instanceof BlockPistonMoving) return false;
        Material material = state.getMaterial();
        return material.blocksMovement() && !material.isLiquid() && !state.isFullCube();
    }

    /** The config's lists, looked up again whenever they change. */
    private static void refreshLists() {
        String[] wanted = PortiumConfig.mechanics.waterloggableBlocks;
        String[] unwanted = PortiumConfig.mechanics.neverWaterloggableBlocks;
        if (wanted == listedIds && unwanted == excludedIds) return;
        listed = resolve(wanted);
        excluded = resolve(unwanted);
        // Until every mod has registered its blocks, some ids can't be found yet; look again next time.
        if (Loader.instance().hasReachedState(LoaderState.AVAILABLE)) {
            listedIds = wanted;
            excludedIds = unwanted;
        }
    }

    private static Set<Block> resolve(String[] ids) {
        if (ids == null || ids.length == 0) return Collections.emptySet();
        Set<Block> blocks = new HashSet<>();
        for (String id : ids) {
            if (id == null || id.trim().isEmpty()) continue;
            ResourceLocation name = new ResourceLocation(id.trim());
            if (ForgeRegistries.BLOCKS.containsKey(name)) blocks.add(ForgeRegistries.BLOCKS.getValue(name));
        }
        return blocks;
    }

    private WaterlogRules() {}
}
