package com.michaelsebero.portium.mods116;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.13 methods that are 1.12 methods under another SRG id, with their parameters reordered, added or dropped: what SRG
 * ids can't tell {@link ModernLinker}. Calls to the 1.13 method reach 1.12's, and a mod's override of the 1.13 method
 * gets a bridge with 1.12's signature, in any class that has the method.
 */
final class ModernSignatures {

    /** A 1.12 argument with no 1.13 argument behind it: the type's zero, null or false. */
    static final int NONE = -1;
    /** The block state at the position among the arguments, read from the world among them. */
    static final int STATE_AT = -2;
    /** Air's state. */
    static final int AIR = -3;
    /** The fluid state at the position among the arguments, read from the world among them. */
    static final int FLUID_AT = -4;

    static final class Renamed {

        /** The 1.13 class declaring it, and its SRG id and descriptor, by 1.13's names. */
        final String owner;
        final String name;
        final String desc;
        /** 1.12's name (SRG id, or Forge's) and descriptor, by 1.12's names. */
        final String legacyName;
        final String legacyDesc;
        /** For each of 1.12's arguments, the index of 1.13's it comes from, or {@link #NONE} and the like. */
        final int[] legacyFrom;
        /** For each of 1.13's arguments, when bridging 1.12's call to it: 1.12's argument, or a stand-in. */
        final int[] modernFrom;
        /** Static fields ({@code owner.name:desc}, 1.13 names) to pass for 1.13 arguments 1.12 can't give, by index. */
        final Map<Integer, String> constants = new HashMap<>();

        Renamed(String owner, String name, String desc, String legacyName, String legacyDesc, int[] legacyFrom, int modernArguments) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.legacyName = legacyName;
            this.legacyDesc = legacyDesc;
            this.legacyFrom = legacyFrom;
            this.modernFrom = new int[modernArguments];
            for (int i = 0; i < modernArguments; i++) this.modernFrom[i] = STATE_AT;
            for (int i = 0; i < legacyFrom.length; i++) {
                if (legacyFrom[i] >= 0) this.modernFrom[legacyFrom[i]] = i;
            }
        }

        /** A value 1.12 can't give for one of 1.13's arguments when calling the bridge. */
        Renamed constant(int modernArgument, String field) {
            this.constants.put(modernArgument, field);
            return this;
        }

        /** Marks a 1.13 argument 1.12 doesn't give as its type's zero rather than a state read from the world. */
        Renamed none(int modernArgument) {
            this.modernFrom[modernArgument] = NONE;
            return this;
        }

        /** Marks a 1.13 argument 1.12 doesn't give as the fluid at the position. */
        Renamed fluid(int modernArgument) {
            this.modernFrom[modernArgument] = FLUID_AT;
            return this;
        }

        /** Whether only calls are linked to 1.12's method, overrides being left to {@code ModernCompletion}. */
        boolean callsOnly;

        Renamed callsOnly() {
            this.callsOnly = true;
            return this;
        }

        /** Marks a 1.13 argument 1.12 doesn't give as air. */
        Renamed air(int modernArgument) {
            this.modernFrom[modernArgument] = AIR;
            return this;
        }
    }

    private final Map<String, List<Renamed>> byMethod = new HashMap<>();

    ModernSignatures() {
        String block = "net/minecraft/block/Block";
        String state = "Lnet/minecraft/block/state/IBlockState;";
        String world = "Lnet/minecraft/world/World;";
        String pos = "Lnet/minecraft/util/math/BlockPos;";
        String random = "Ljava/util/Random;";
        String player = "Lnet/minecraft/entity/player/EntityPlayer;";
        String entity = "Lnet/minecraft/entity/Entity;";
        String reader = "Lnet/minecraft/world/IBlockReader;";
        String access = "Lnet/minecraft/world/IBlockAccess;";

        // 1.13 put the state first; 1.12 had it after the world and position.
        renamed(block, "func_196267_b", "(" + state + world + pos + random + ")V", "func_180650_b", "(" + world + pos + state + random + ")V", 1, 2, 0, 3);
        renamed(block, "func_196265_a", "(" + state + world + pos + random + ")V", "func_180645_a", "(" + world + pos + state + random + ")V", 1, 2, 0, 3);
        renamed(block, "func_196262_a", "(" + state + world + pos + entity + ")V", "func_180634_a", "(" + world + pos + state + entity + ")V", 1, 2, 0, 3);
        renamed(block, "func_196270_a", "(" + state + world + pos + player + ")V", "func_180649_a", "(" + world + pos + player + ")V", 1, 2, 3);
        renamed(block, "func_196255_a", "(" + state + world + pos + "FI)V", "func_180653_a", "(" + world + pos + state + "FI)V", 1, 2, 0, 3, 4);
        // onReplaced is breakBlock, once the block has changed; 1.12 calls it after setting the new state.
        renamed(block, "func_196243_a", "(" + state + world + pos + state + "Z)V", "func_180663_b", "(" + world + pos + state + ")V", 1, 2, 0).none(4);
        renamed(block, "func_196259_b", "(" + state + world + pos + state + ")V", "func_176213_c", "(" + world + pos + state + ")V", 1, 2, 0).air(3);
        renamed(block, "func_196264_a", "(" + state + random + ")I", "func_149745_a", "(" + random + ")I", 1);
        // Forge 1.12's state-aware ones.
        renamed(block, "func_196251_a", "(" + state + "I" + world + pos + random + ")I", "quantityDropped", "(" + state + "I" + random + ")I", 0, 1, 4).none(2).none(3);
        // These two and Forge's below keep their ids in 1.16, which names the state BlockState (1.12's names on the right).
        String state116 = "Lnet/minecraft/block/BlockState;";
        String player116 = "Lnet/minecraft/entity/player/PlayerEntity;";
        renamed(block, "func_200011_d", "(" + state116 + reader + pos + ")I", "getLightOpacity", "(" + state + access + pos + ")I", 0, 1, 2);
        renamed(block, "func_196266_a", "(" + state116 + reader + pos + "Lnet/minecraft/pathfinding/PathType;)Z", "func_176205_b", "(" + access + pos + ")Z", 1, 2)
                .constant(3, "net/minecraft/pathfinding/PathType.LAND:Lnet/minecraft/pathfinding/PathType;");
        // Forge 1.13 tells removedByPlayer the fluid left behind, and onNeighborChange the state.
        renamed(block, "removedByPlayer", "(" + state116 + world + pos + player116 + "ZLnet/minecraft/fluid/FluidState;)Z", "removedByPlayer",
                "(" + state + world + pos + player + "Z)Z", 0, 1, 2, 3, 4).fluid(5);
        renamed(block, "onNeighborChange", "(" + state116 + "Lnet/minecraft/world/IWorldReader;" + pos + pos + ")V", "onNeighborChange",
                "(" + access + pos + pos + ")V", 1, 2, 3);

        // Baked models: 1.12 asks for quads with a seed; 1.16 with a random (and Forge 1.16 with model data).
        String model = "net/minecraft/client/renderer/model/IBakedModel";
        String quads = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;J)Ljava/util/List;";
        // Calls only: a mod's model gets 1.12's method from ModernCompletion, which asks it with the block's model data.
        renamed(model, "func_200117_a", "(" + state116 + "Lnet/minecraft/util/Direction;Ljava/util/Random;)Ljava/util/List;", "func_188616_a", quads, 0, 1, 2)
                .callsOnly();
    }

    private Renamed renamed(String owner, String name, String desc, String legacyName, String legacyDesc, int... legacyFrom) {
        int modernArguments = org.objectweb.asm.Type.getArgumentTypes(desc).length;
        Renamed renamed = new Renamed(owner, name, desc, legacyName, legacyDesc, legacyFrom, modernArguments);
        this.byMethod.computeIfAbsent(name + desc, key -> new ArrayList<>()).add(renamed);
        return renamed;
    }

    /** The 1.12 method behind a 1.13 one reached through {@code owner}, or null; all by 1.13's names. */
    @Nullable
    Renamed find(String owner, String name, String desc, ModernRedirects.Hierarchy hierarchy) {
        List<Renamed> candidates = this.byMethod.get(name + desc);
        if (candidates == null) return null;
        for (Renamed renamed : candidates) {
            if (renamed.owner.equals(owner) || hierarchy.isSubtype(owner, renamed.owner)) return renamed;
        }
        return null;
    }
}
