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
    /** The world of the entity the method is called on (a bridge's own {@code this}). */
    static final int THIS_WORLD = -5;
    /** The x, y or z of the vector among 1.13's arguments, as a float. */
    static final int VECTOR_X = -10;
    static final int VECTOR_Y = -11;
    static final int VECTOR_Z = -12;
    /** A vector of the three floats among 1.12's arguments. */
    static final int VECTOR = -13;

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

        /** Marks a 1.13 argument 1.12 doesn't give as the world of the entity called. */
        Renamed thisWorld(int modernArgument) {
            this.modernFrom[modernArgument] = THIS_WORLD;
            return this;
        }

        /** Marks a 1.13 argument as the vector of 1.12's three floats. */
        Renamed vector(int modernArgument) {
            this.modernFrom[modernArgument] = VECTOR;
            return this;
        }

        /** Static methods ({@code owner.name(desc)}, taking the object called) that make 1.13 arguments 1.12 can't give, by index. */
        final Map<Integer, String> providers = new HashMap<>();

        Renamed fromThis(int modernArgument, String method) {
            this.providers.put(modernArgument, method);
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

        entities();

        // Baked models: 1.12 asks for quads with a seed; 1.16 with a random (and Forge 1.16 with model data).
        String model = "net/minecraft/client/renderer/model/IBakedModel";
        String quads = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;J)Ljava/util/List;";
        // Calls only: a mod's model gets 1.12's method from ModernCompletion, which asks it with the block's model data.
        renamed(model, "func_200117_a", "(" + state116 + "Lnet/minecraft/util/Direction;Ljava/util/Random;)Ljava/util/List;", "func_188616_a", quads, 0, 1, 2)
                .callsOnly();
    }

    /**
     * 1.16's entity methods that are 1.12's under another id, with the world, the spawn reason and the like added
     * (1.12's entities know their world), so that a 1.16 mob's overrides are what 1.12 calls and its calls up reach
     * 1.12's methods.
     */
    private void entities() {
        String entity = "net/minecraft/entity/Entity";
        String living = "net/minecraft/entity/LivingEntity";
        String mob = "net/minecraft/entity/MobEntity";
        String nbt116 = "Lnet/minecraft/nbt/CompoundNBT;";
        String nbt = "Lnet/minecraft/nbt/NBTTagCompound;";
        String reason = "Lnet/minecraft/entity/SpawnReason;";
        String natural = "net/minecraft/entity/SpawnReason.NATURAL:" + reason;
        String pos = "Lnet/minecraft/util/math/BlockPos;";
        // Saving: 1.14 renamed it.
        renamed(entity, "func_213281_b", "(" + nbt116 + ")V", "func_70014_b", "(" + nbt + ")V", 0);
        // A mob's first spawn; 1.12 gives the difficulty and the group's data.
        renamed(mob, "func_213386_a", "(Lnet/minecraft/world/IServerWorld;Lnet/minecraft/world/DifficultyInstance;" + reason
                        + "Lnet/minecraft/entity/ILivingEntityData;" + nbt116 + ")Lnet/minecraft/entity/ILivingEntityData;",
                "func_180482_a", "(Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/entity/IEntityLivingData;)Lnet/minecraft/entity/IEntityLivingData;", 1, 3)
                .thisWorld(0).constant(2, natural).none(4);
        // A player using an item on a mob: 1.16 answers with a result, 1.12 with whether anything happened.
        renamed(mob, "func_230254_b_", "(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResultType;",
                "func_184645_a", "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/EnumHand;)Z", 0, 1);
        // Whether a mob may spawn where it is.
        renamed(mob, "func_213380_a", "(Lnet/minecraft/world/IWorld;" + reason + ")Z", "func_70601_bi", "()Z").thisWorld(0).constant(1, natural);
        renamed(mob, "func_205019_a", "(Lnet/minecraft/world/IWorldReader;)Z", "func_70058_J", "()Z").thisWorld(0);
        renamed(mob, "func_213397_c", "(D)Z", "func_70692_ba", "()Z").none(0);
        // Moving by input: 1.16 takes the three as a vector.
        renamed(living, "func_213352_e", "(Lnet/minecraft/util/math/vector/Vector3d;)V", "func_191986_a", "(FFF)V", VECTOR_X, VECTOR_Y, VECTOR_Z).vector(0);
        // Breeding, kills, path weights: 1.16 passes the world along.
        renamed("net/minecraft/entity/AgeableEntity", "func_241840_a",
                "(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/entity/AgeableEntity;)Lnet/minecraft/entity/AgeableEntity;",
                "func_90011_a", "(Lnet/minecraft/entity/EntityAgeable;)Lnet/minecraft/entity/EntityAgeable;", 1).thisWorld(0);
        renamed(entity, "func_241847_a", "(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/entity/LivingEntity;)V",
                "func_70074_a", "(Lnet/minecraft/entity/EntityLivingBase;)V", 1).thisWorld(0);
        renamed("net/minecraft/entity/CreatureEntity", "func_205022_a", "(" + pos + "Lnet/minecraft/world/IWorldReader;)F", "func_180484_a", "(" + pos + ")F", 0)
                .thisWorld(1);
        // A navigator's path finder, made with how many nodes it may visit (1.12's knows its own limit).
        renamed("net/minecraft/pathfinding/PathNavigator", "func_179679_a", "(I)Lnet/minecraft/pathfinding/PathFinder;", "func_179679_a",
                "()Lnet/minecraft/pathfinding/PathFinder;").none(0);
        // Moving by input relative to facing: 1.16 takes the input as a vector.
        renamed(entity, "func_213309_a", "(FLnet/minecraft/util/math/vector/Vector3d;)V", "func_191958_b", "(FFFF)V", VECTOR_X, VECTOR_Y, VECTOR_Z, 0).vector(1);
        // Drops beyond the loot table: 1.12 doesn't say what killed.
        renamed(living, "func_213333_a", "(Lnet/minecraft/util/DamageSource;IZ)V", "func_82160_b", "(ZI)V", 2, 1)
                .constant(0, "net/minecraft/util/DamageSource.field_76377_j:Lnet/minecraft/util/DamageSource;");
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
