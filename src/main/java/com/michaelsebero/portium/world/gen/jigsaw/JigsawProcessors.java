package com.michaelsebero.portium.world.gen.jigsaw;

import com.michaelsebero.portium.compat.LegacyBlockStates;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Named;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Processor;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.StateValue;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 1.16's structure processors as read from its processor lists ({@link JigsawPools}), run on template blocks by
 * their 1.16 names: rule processors (random block swaps, by block, state, tag or what's already in the world),
 * block rot (integrity), and the position test the bastion's high ramparts use.
 */
public final class JigsawProcessors {

    /** A block as processors see it: its 1.16 name and properties. */
    public static final class Candidate {
        public String name;
        public Map<String, String> properties;
        public boolean changed;

        Candidate(String name, Map<String, String> properties) {
            this.name = name;
            this.properties = properties;
        }
    }

    /**
     * Runs a processor list on a block placed at {@code pos} (local position {@code local} in a piece at {@code origin});
     * false if a processor removed it.
     */
    public static boolean process(List<Processor> processors, Candidate block, World world, BlockPos pos, BlockPos origin, Random pieceRandom) {
        for (Processor processor : processors) {
            switch (processor.kind) {
                case "cst": // RuleProcessor: the first rule that matches replaces the block.
                    if (!processor.args.isEmpty() && processor.args.get(0) instanceof List) applyRules((List<?>) processor.args.get(0), block, world, pos, origin);
                    break;
                case "csg": // BlockRotProcessor: keeps a block by its integrity.
                    if (!processor.args.isEmpty() && processor.args.get(0) instanceof Float && pieceRandom.nextFloat() > (Float) processor.args.get(0)) return false;
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    private static void applyRules(List<?> rules, Candidate block, World world, BlockPos pos, BlockPos origin) {
        Random random = new Random(MathHelper.getPositionRandom(pos));
        IBlockState existing = world.getBlockState(pos);
        for (Object item : rules) {
            if (!(item instanceof Processor) || !((Processor) item).kind.equals("csq")) continue;
            List<Object> args = ((Processor) item).args;
            if (args.size() < 3) continue;
            Object output = args.get(args.size() - 1);
            Object posTest = args.size() >= 4 ? args.get(2) : null;
            if (test(args.get(0), block.name, block.properties, random) && locationTest(args.get(1), existing, random)
                    && (posTest == null || posTest(posTest, pos, origin, random))) {
                if (output instanceof StateValue) {
                    block.name = full(((StateValue) output).block);
                    block.properties = new HashMap<>(((StateValue) output).properties);
                    block.changed = true;
                }
                return;
            }
        }
    }

    /** A rule's input test, on the template block's 1.16 name and properties. */
    private static boolean test(Object test, String name, Map<String, String> properties, Random random) {
        if (!(test instanceof Processor)) return false;
        Processor rule = (Processor) test;
        List<Object> args = rule.args;
        switch (rule.kind) {
            case "csa": // AlwaysTrueTest
                return true;
            case "csf": // BlockMatchTest
                return matchesBlock(args.get(0), name);
            case "csr": // RandomBlockMatchTest
                return matchesBlock(args.get(0), name) && random.nextFloat() < floatOf(args.get(1));
            case "csh": // BlockStateMatchTest
                return matchesState(args.get(0), name, properties);
            case "css": // RandomBlockStateMatchTest
                return matchesState(args.get(0), name, properties) && random.nextFloat() < floatOf(args.get(1));
            case "ctc": // TagMatchTest
                return args.get(0) instanceof Named && inTag(((Named) args.get(0)).name, name);
            default:
                return false;
        }
    }

    /** A rule's location test, on the block already in the world there (water, mostly). */
    private static boolean locationTest(Object test, IBlockState existing, Random random) {
        if (!(test instanceof Processor)) return false;
        Processor rule = (Processor) test;
        switch (rule.kind) {
            case "csa":
                return true;
            case "csf":
            case "csr": {
                if (!(rule.args.get(0) instanceof Named)) return false;
                String wanted = ((Named) rule.args.get(0)).name;
                boolean matches = wanted.equals("water") ? existing.getMaterial() == Material.WATER
                        : wanted.equals("lava") ? existing.getMaterial() == Material.LAVA : existing.getBlock() == block(wanted);
                return matches && (rule.kind.equals("csf") || random.nextFloat() < floatOf(rule.args.get(1)));
            }
            default:
                return false;
        }
    }

    /** 1.16's AxisAlignedLinearPosTest: a chance growing from one to the other with distance from the piece along an axis. */
    private static boolean posTest(Object test, BlockPos pos, BlockPos origin, Random random) {
        if (!(test instanceof Processor) || !((Processor) test).kind.equals("csb")) return true;
        List<Object> args = ((Processor) test).args;
        float minChance = floatOf(args.get(0));
        float maxChance = floatOf(args.get(1));
        int minDist = args.get(2) instanceof Integer ? (Integer) args.get(2) : 0;
        int maxDist = args.get(3) instanceof Integer ? (Integer) args.get(3) : 1;
        int distance = Math.abs(pos.getY() - origin.getY());
        float t = MathHelper.clamp((float) (distance - minDist) / (float) Math.max(1, maxDist - minDist), 0.0F, 1.0F);
        return random.nextFloat() <= minChance + (maxChance - minChance) * t;
    }

    private static boolean matchesBlock(Object block, String name) {
        return block instanceof Named && full(((Named) block).name).equals(name);
    }

    private static boolean matchesState(Object state, String name, Map<String, String> properties) {
        if (!(state instanceof StateValue) || !full(((StateValue) state).block).equals(name)) return false;
        for (Map.Entry<String, String> entry : ((StateValue) state).properties.entrySet()) {
            if (!entry.getValue().equalsIgnoreCase(properties.getOrDefault(entry.getKey(), ""))) return false;
        }
        return true;
    }

    /** The block tags 1.16's village and bastion processors test. */
    private static boolean inTag(String tag, String name) {
        switch (tag) {
            case "doors":
            case "wooden_doors":
                return name.endsWith("_door");
            case "planks":
                return name.endsWith("_planks");
            case "logs":
                return name.endsWith("_log") || name.endsWith("_wood");
            default:
                return false;
        }
    }

    private static float floatOf(Object value) {
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    static String full(String name) {
        return name.indexOf(':') >= 0 ? name : "minecraft:" + name;
    }

    private static final Map<String, Block> BLOCKS = new HashMap<>();

    @Nullable
    private static Block block(String name) {
        return BLOCKS.computeIfAbsent(name, key -> LegacyBlockStates.convert(full(key), new HashMap<>()).getBlock());
    }

    private JigsawProcessors() {}
}
