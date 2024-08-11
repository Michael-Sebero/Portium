package com.michaelsebero.portium.modern;

import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 1.13 methods that 1.12 lacks or has with other parameters, and the static methods in {@code modern.shim} that
 * stand in for them. A call to one becomes a static call that takes the receiver as its first argument. Names and
 * descriptors here are 1.13's; the translator renames the classes in them afterwards.
 */
final class ModernRedirects {

    /** What's known about classes at runtime. */
    interface Hierarchy {

        /** Whether one 1.13 class extends or implements another. */
        boolean isSubtype(String type, String supertype);

        /** Whether a class of the parent class loader's (Minecraft's, say) exists. */
        boolean exists(String type);

        /** Whether one class extends or implements another, both by their translated names. */
        boolean isTranslatedSubtype(String type, String supertype);

        /** Whether a class, by translated name, is an interface; null if it can't be found. */
        @Nullable
        Boolean isInterface(String type);

        /** Whether a class, by translated name, is one of Minecraft 1.13.2's own (see {@code ModernNatives}). */
        boolean isNative(String type);

        /** A 1.13 class's superclass by 1.13's names, for classes this loader defines; null otherwise. */
        @Nullable
        String modernSuperName(String type);

        /** Whether Minecraft 1.13.2's class (1.13 name) itself declares a method or field. */
        boolean nativeDeclares(String type, String name, String desc, boolean field);

        /** Whether the parent has an enum or interface by this name. */
        boolean isLegacyValueType(String type);

        /** Whether Minecraft 1.13.2 has a class, by its 1.13 name, whether or not 1.12 has one so named. */
        boolean inMinecraft113(String type);

        /** The superclass (first; null for none) and interfaces of a class, all by translated names; empty if unknown. */
        String[] supertypes(String type);

        /**
         * Whether a 1.13 class this loader defines (a mod's, or Minecraft 1.13.2's), or one of those above it before the
         * first of the parent's, declares a method; by 1.13's names.
         */
        boolean declaresModern(String type, String name, String desc);

        /**
         * As {@link #declaresModern}, but only classes that run as 1.13 code count (a mod's, those 1.12 lacks, block
         * and item classes copied from 1.13.2): the walk stops at a class Portium adapts ({@code Block}, {@code
         * Item}...) or one 1.12 has.
         */
        boolean declaresBelowAdapters(String type, String name, String desc);

        /** The methods (name and descriptor, as translated) that a class this loader defines declares, by translated name. */
        Set<String> translatedMethods(String type);
    }

    static final class Redirect {

        /** The 1.13 class that declares the method. */
        final String declarer;
        final String owner;
        final String name;

        Redirect(String declarer, String owner, String name) {
            this.declarer = declarer;
            this.owner = owner;
            this.name = name;
        }

        /** The stand-in's descriptor for a call with this opcode and the original descriptor. */
        String desc(int opcode, String desc) {
            return opcode == Opcodes.INVOKESTATIC ? desc : "(L" + this.declarer + ";" + desc.substring(1);
        }
    }

    /**
     * A 1.13 constructor that 1.12 has with other parameters: 1.12's arguments are made from 1.13's where they lie on
     * the stack, and 1.12's constructor called instead.
     */
    static final class Constructor {

        /** 1.12's descriptor, by 1.12's names. */
        final String desc;
        final Argument[] arguments;

        Constructor(String desc, Argument[] arguments) {
            this.desc = desc;
            this.arguments = arguments;
        }
    }

    /**
     * One of 1.12's constructor arguments: 1.13's argument {@code from}, passed through a static method given as
     * {@code owner.name desc} if any. A method returning void only takes the argument in, for 1.12 to pick up later,
     * and makes no argument of its own.
     */
    static final class Argument {

        final int from;
        @Nullable
        final String converter;

        Argument(int from, @Nullable String converter) {
            this.from = from;
            this.converter = converter;
        }

        boolean producesArgument() {
            return this.converter == null || !this.converter.endsWith(")V");
        }
    }

    private static final String SHIMS = "com/michaelsebero/portium/modern/shim/";
    private static final String PARTICLES = SHIMS + "ParticleShims";
    private static final String BLOCKS = SHIMS + "BlockShims";
    private static final String ITEMS = SHIMS + "ItemShims";
    private static final String REGISTRIES = SHIMS + "RegistryShims";
    private static final String AS_ITEM = SHIMS + "ItemShims.asItem(Lnet/minecraft/util/IItemProvider;)Lnet/minecraft/item/Item;";
    private static final String ITEM_PROPERTIES = "Lnet/minecraft/item/Item$Properties;";

    private static final String ADAPTERS = "com/michaelsebero/portium/modern/adapter/";
    private static final String GLUE = "com/michaelsebero/portium/modern/host/";

    /** 1.12 classes 1.13 code gets Portium's adapters of when it extends or makes them. */
    private static final Map<String, String> ADAPTED = new HashMap<>();

    static {
        ADAPTED.put("net/minecraft/block/Block", ADAPTERS + "ModernBlock");
        ADAPTED.put("net/minecraft/block/BlockBed", ADAPTERS + "ModernBlockBed");
        ADAPTED.put("net/minecraft/item/Item", ADAPTERS + "ModernItem");
        ADAPTED.put("net/minecraft/item/ItemBlock", ADAPTERS + "ModernItemBlock");
        ADAPTED.put("net/minecraft/item/ItemBed", ADAPTERS + "ModernItemBlock");
        ADAPTED.put("net/minecraft/tileentity/TileEntity", ADAPTERS + "ModernTileEntity");
        // 1.13's screens, buttons and text fields, with 1.13's input.
        ADAPTED.put("net/minecraft/client/gui/GuiScreen", ADAPTERS + "ModernGuiScreen");
        ADAPTED.put("net/minecraft/client/gui/inventory/GuiContainer", ADAPTERS + "ModernGuiContainer");
        ADAPTED.put("net/minecraft/client/gui/GuiButton", ADAPTERS + "ModernGuiButton");
        ADAPTED.put("net/minecraft/client/gui/GuiTextField", ADAPTERS + "ModernGuiTextField");
    }

    private final Map<String, List<Redirect>> methods = new HashMap<>();
    private final Map<String, Constructor> constructors = new HashMap<>();
    /** A 1.13 field 1.12 lacks or has in another type: static methods read and write it. */
    private static final class FieldRedirect {

        final String declarer;
        final String getter;
        @Nullable
        final String setter;

        FieldRedirect(String declarer, String getter, @Nullable String setter) {
            this.declarer = declarer;
            this.getter = getter;
            this.setter = setter;
        }
    }

    private final Map<String, List<FieldRedirect>> fields = new HashMap<>();

    ModernRedirects() {
        String particle = "Lnet/minecraft/particles/IParticleData;";
        method("net/minecraft/world/World", "func_195594_a", "(" + particle + "DDDDDD)V", PARTICLES, "addParticle");
        method("net/minecraft/world/IWorld", "func_195594_a", "(" + particle + "DDDDDD)V", PARTICLES, "addParticle");
        method("net/minecraft/world/World", "func_195590_a", "(" + particle + "ZDDDDDD)V", PARTICLES, "addParticle");
        method("net/minecraft/world/World", "func_195589_b", "(" + particle + "DDDDDD)V", PARTICLES, "addOptionalParticle");
        method("net/minecraft/world/WorldServer", "func_195598_a", "(" + particle + "DDDIDDDD)I", PARTICLES, "spawnParticle");
        method("net/minecraft/world/WorldServer", "func_195600_a", "(Lnet/minecraft/entity/player/EntityPlayerMP;" + particle + "ZDDDIDDDD)Z",
                PARTICLES, "spawnParticle");

        method("net/minecraft/block/Block", "getPickBlock", "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/RayTraceResult;"
                + "Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;",
                BLOCKS, "getPickBlock");

        // 1.12's registries lack a few of 1.13's methods; getValues returns a list there.
        String registry = "net/minecraftforge/registries/IForgeRegistry";
        method(registry, "getRegistryName", "()Lnet/minecraft/util/ResourceLocation;", REGISTRIES, "getRegistryName");
        method(registry, "isEmpty", "()Z", REGISTRIES, "isEmpty");
        method(registry, "getDefaultKey", "()Lnet/minecraft/util/ResourceLocation;", REGISTRIES, "getDefaultKey");
        method(registry, "getValues", "()Ljava/util/Collection;", REGISTRIES, "getValues");
        method("net/minecraftforge/registries/GameData", "getWrapper", "(Ljava/lang/Class;)Lnet/minecraft/util/registry/RegistryNamespaced;", REGISTRIES, "wrapper");
        method("net/minecraftforge/registries/GameData", "getWrapperDefaulted", "(Ljava/lang/Class;)Lnet/minecraft/util/registry/RegistryNamespacedDefaultedByKey;",
                REGISTRIES, "wrapperDefaulted");
        // 1.13's flattened names find what they are here.
        method(registry, "getValue", "(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraftforge/registries/IForgeRegistryEntry;", REGISTRIES, "getValue");
        method(registry, "containsKey", "(Lnet/minecraft/util/ResourceLocation;)Z", REGISTRIES, "containsKey");

        String state = "Lnet/minecraft/block/state/IBlockState;";
        String pos = "Lnet/minecraft/util/math/BlockPos;";
        String world = "Lnet/minecraft/world/World;";
        method("net/minecraft/block/Block", "func_196246_j", "(" + state + ")I", BLOCKS, "getStateId");
        method("net/minecraft/block/Block", "func_199769_a", "(" + state + world + pos + "I)Lnet/minecraft/util/IItemProvider;", BLOCKS, "getItemDropped");
        method("net/minecraft/block/Block", "func_208617_a", "(DDDDDD)Lnet/minecraft/util/math/shapes/VoxelShape;", BLOCKS, "makeCuboidShape");
        method("net/minecraft/block/Block", "func_196244_b", "(" + state + "Lnet/minecraft/world/IBlockReader;" + pos + ")Lnet/minecraft/util/math/shapes/VoxelShape;",
                BLOCKS, "getShape");
        method("net/minecraft/block/Block", "func_185473_a", "(Lnet/minecraft/world/IBlockReader;" + pos + state + ")Lnet/minecraft/item/ItemStack;", BLOCKS, "getItem");
        method("net/minecraft/block/BlockBed", "func_196350_d", "()Lnet/minecraft/item/EnumDyeColor;", BLOCKS, "getBedColor");
        method("net/minecraft/block/state/IBlockState", "func_196949_c", "(" + world + pos + "I)V", BLOCKS, "dropBlockAsItem");
        method("net/minecraft/block/state/IBlockState", "func_196953_a", "(Lnet/minecraft/item/BlockItemUseContext;)Z", BLOCKS, "isReplaceable");
        method("net/minecraft/block/state/IBlockState", "func_196955_c", "(Lnet/minecraft/world/IWorldReaderBase;" + pos + ")Z", BLOCKS, "isValidPosition");
        method("net/minecraft/block/state/IBlockState", "func_206870_a", "(Lnet/minecraft/state/IProperty;Ljava/lang/Comparable;)Ljava/lang/Object;",
                BLOCKS, "with");
        method("net/minecraft/item/EnumDyeColor", "func_196059_a", "()I", SHIMS + "MiscShims", "dyeId");
        // 1.13 names dyes alike everywhere (light_gray, light_blue); 1.12 has silver, and camel case in its keys.
        method("net/minecraft/item/EnumDyeColor", "func_176762_d", "()Ljava/lang/String;", SHIMS + "MiscShims", "dyeName");
        method("net/minecraft/item/EnumDyeColor", "func_176610_l", "()Ljava/lang/String;", SHIMS + "MiscShims", "dyeName");
        method("net/minecraft/entity/EntityLivingBase", "func_195064_c", "(Lnet/minecraft/potion/PotionEffect;)Z", SHIMS + "MiscShims", "addPotionEffect");
        method("net/minecraft/server/MinecraftServer", "func_71218_a", "(Lnet/minecraft/world/dimension/DimensionType;)Lnet/minecraft/world/WorldServer;",
                SHIMS + "MiscShims", "getWorld");
        method("net/minecraft/util/DamageSource", "func_199683_a", "()Lnet/minecraft/util/DamageSource;", SHIMS + "MiscShims", "netherBedExplosion");
        method("net/minecraft/world/World", "func_211529_a", "(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/DamageSource;DDDFZZ)Lnet/minecraft/world/Explosion;",
                SHIMS + "MiscShims", "createExplosion");
        method("net/minecraft/world/IWorld", "func_201670_d", "()Z", SHIMS + "MiscShims", "isRemote");
        field("net/minecraft/stats/StatList", "field_188065_ae", "Lnet/minecraft/stats/StatType;", SHIMS + "MiscShims.blockMined()Lnet/minecraft/stats/StatType;");

        // Fluids: 1.12's liquids, Portium's waterlogging and 1.13 blocks, read as 1.13 fluid states.
        String fluids = SHIMS + "FluidShims";
        String fluidState = "Lnet/minecraft/fluid/IFluidState;";
        method("net/minecraft/world/IBlockReader", "func_204610_c", "(" + pos + ")" + fluidState, fluids, "getFluidState");
        method("net/minecraft/block/state/IBlockState", "func_204520_s", "()" + fluidState, fluids, "getFluidState");
        method("net/minecraft/block/Block", "func_204507_t", "(" + state + ")" + fluidState, fluids, "getFluidState");
        method("net/minecraft/world/IWorld", "func_205219_F_", "()Lnet/minecraft/world/ITickList;", fluids, "getPendingFluidTicks");
        method("net/minecraft/world/IWorld", "func_205220_G_", "()Lnet/minecraft/world/ITickList;", fluids, "getPendingBlockTicks");

        String reflection = "net/minecraftforge/fml/common/ObfuscationReflectionHelper";
        method(reflection, "findMethod", "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", SHIMS + "ReflectionShims", "findMethod");
        method(reflection, "findField", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Field;", SHIMS + "ReflectionShims", "findField");
        method(reflection, "getPrivateValue", "(Ljava/lang/Class;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", SHIMS + "ReflectionShims", "getPrivateValue");
        method(reflection, "setPrivateValue", "(Ljava/lang/Class;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V", SHIMS + "ReflectionShims", "setPrivateValue");

        method("net/minecraftforge/registries/ForgeRegistryEntry", "setRegistryName", "(Ljava/lang/String;)Lnet/minecraftforge/registries/IForgeRegistryEntry;",
                GLUE + "RegistryGlue", "setRegistryName");

        // Capabilities are 1.12's; LazyOptional is Forge 1.13's, so these go to the glue.
        String provider = "net/minecraftforge/common/capabilities/ICapabilityProvider";
        String capability = "Lnet/minecraftforge/common/capabilities/Capability;";
        String optional = "Lnet/minecraftforge/common/util/LazyOptional;";
        method(provider, "getCapability", "(" + capability + "Lnet/minecraft/util/EnumFacing;)" + optional, GLUE + "CapabilityGlue", "getCapability");
        method(provider, "getCapability", "(" + capability + ")" + optional, GLUE + "CapabilityGlue", "getCapability");
        method("net/minecraftforge/common/capabilities/Capability", "orEmpty", "(" + capability + optional + ")" + optional, GLUE + "CapabilityGlue", "orEmpty");

        // Forge 1.13's getters on events Forge 1.12 gives public fields.
        String events = SHIMS + "EventShims";
        String playerEvent = "net/minecraftforge/fml/common/gameevent/PlayerEvent";
        String dimensionType = "Lnet/minecraft/world/dimension/DimensionType;";
        method(playerEvent, "getPlayer", "()Lnet/minecraft/entity/player/EntityPlayer;", events, "getPlayer");
        method(playerEvent + "$ItemCraftedEvent", "getCrafting", "()Lnet/minecraft/item/ItemStack;", events, "getCrafting");
        method(playerEvent + "$ItemCraftedEvent", "getInventory", "()Lnet/minecraft/inventory/IInventory;", events, "getInventory");
        method(playerEvent + "$ItemSmeltedEvent", "getSmelting", "()Lnet/minecraft/item/ItemStack;", events, "getSmelting");
        method(playerEvent + "$PlayerChangedDimensionEvent", "getFrom", "()" + dimensionType, events, "getFrom");
        method(playerEvent + "$PlayerChangedDimensionEvent", "getTo", "()" + dimensionType, events, "getTo");

        // Dimensions are types in 1.13 (registry entries in Forge 1.13), ids in 1.12.
        String misc = SHIMS + "MiscShims";
        method("net/minecraft/world/dimension/DimensionType", "getRegistryName", "()Lnet/minecraft/util/ResourceLocation;", misc, "dimensionName");
        method("net/minecraft/world/dimension/DimensionType", "func_212678_a", "(" + dimensionType + ")Lnet/minecraft/util/ResourceLocation;", misc, "dimensionName");
        method("net/minecraftforge/common/DimensionManager", "getWorld", "(Lnet/minecraft/server/MinecraftServer;" + dimensionType + "ZZ)Lnet/minecraft/world/WorldServer;",
                misc, "getWorld");
        field("net/minecraft/entity/Entity", "field_71093_bK", dimensionType, misc + ".dimension(Lnet/minecraft/entity/Entity;)" + dimensionType,
                misc + ".setDimension(Lnet/minecraft/entity/Entity;" + dimensionType + ")V");

        // What 1.13 asks of its world interfaces.
        method("net/minecraft/world/World", "func_201674_k", "()Ljava/util/Random;", misc, "getRandom");
        method("net/minecraft/world/IWorld", "func_201674_k", "()Ljava/util/Random;", misc, "getRandom");
        method("net/minecraft/world/IWorld", "func_201672_e", "()Lnet/minecraft/world/World;", misc, "getWorld");
        method("net/minecraft/world/IWorldReaderBase", "func_201675_m", "()Lnet/minecraft/world/dimension/Dimension;", misc, "getDimension");
        method("net/minecraft/world/IWorldReaderBase", "func_201670_d", "()Z", misc, "isRemote");

        // Entities: names as text, the shooter of an arrow by UUID.
        method("net/minecraft/entity/Entity", "func_200200_C_", "()Lnet/minecraft/util/text/ITextComponent;", misc, "getName");
        method("net/minecraft/entity/Entity", "func_200201_e", "()Lnet/minecraft/util/text/ITextComponent;", misc, "getCustomName");
        method("net/minecraft/entity/Entity", "func_200203_b", "(Lnet/minecraft/util/text/ITextComponent;)V", misc, "setCustomName");
        method("net/minecraft/util/INameable", "func_200200_C_", "()Lnet/minecraft/util/text/ITextComponent;", misc, "nameOf");
        method("net/minecraft/util/INameable", "func_200201_e", "()Lnet/minecraft/util/text/ITextComponent;", misc, "customNameOf");
        field("net/minecraft/entity/projectile/EntityArrow", "field_70250_c", "Ljava/util/UUID;",
                misc + ".shootingEntity(Lnet/minecraft/entity/projectile/EntityArrow;)Ljava/util/UUID;");

        method("net/minecraft/entity/projectile/EntityArrow", "func_212360_k", "()Lnet/minecraft/entity/Entity;", misc, "getShooter");
        field("net/minecraft/entity/ai/EntityAINearestAttackableTarget", "field_82643_g", "Ljava/util/function/Predicate;",
                misc + ".targetSelector(Lnet/minecraft/entity/ai/EntityAINearestAttackableTarget;)Ljava/util/function/Predicate;",
                misc + ".setTargetSelector(Lnet/minecraft/entity/ai/EntityAINearestAttackableTarget;Ljava/util/function/Predicate;)V");
        method("net/minecraft/inventory/Container", "func_195929_a", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z", misc, "areItemsAndTagsEqual");
        method("net/minecraft/block/ITileEntityProvider", "func_196283_a_", "(Lnet/minecraft/world/IBlockReader;)Lnet/minecraft/tileentity/TileEntity;",
                BLOCKS, "createNewTileEntity");

        // Chunks: heights by kind, fluids and biomes by position.
        String chunk = "net/minecraft/world/chunk/Chunk";
        method(chunk, "func_201576_a", "(Lnet/minecraft/world/gen/Heightmap$Type;II)I", GLUE + "ChunkGlue", "getTopBlockY");
        method(chunk, "func_204610_c", "(" + pos + ")Lnet/minecraft/fluid/IFluidState;", SHIMS + "ChunkShims", "getFluidState");
        method(chunk, "func_201600_k", "(" + pos + ")Lnet/minecraft/world/biome/Biome;", SHIMS + "ChunkShims", "getBiome");

        // Command sources of entities and the server.
        String commandSource = "com/michaelsebero/portium/command/CommandSource";
        method("net/minecraft/entity/Entity", "func_195051_bN", "()Lnet/minecraft/command/CommandSource;", commandSource, "ofEntity");
        method("net/minecraft/server/MinecraftServer", "func_195573_aM", "()Lnet/minecraft/command/CommandSource;", commandSource, "ofServer");

        // Saved data stays in 1.12's format: 1.13's fixers aren't run on it.
        String nbt = "Lnet/minecraft/nbt/NBTTagCompound;";
        String fixer = "(Lcom/mojang/datafixers/DataFixer;Lcom/mojang/datafixers/DSL$TypeReference;" + nbt;
        method("net/minecraft/nbt/NBTUtil", "func_210822_a", fixer + "I)" + nbt, GLUE + "DataFixGlue", "update");
        method("net/minecraft/nbt/NBTUtil", "func_210821_a", fixer + "II)" + nbt, GLUE + "DataFixGlue", "update");

        // Recipes are 1.12's: the recipe manager's answers come from its furnace recipes and crafting manager.
        String recipes = GLUE + "RecipeGlue";
        String manager = "net/minecraft/item/crafting/RecipeManager";
        String inventory = "Lnet/minecraft/inventory/IInventory;";
        String recipeType = "Lnet/minecraftforge/common/crafting/RecipeType;";
        method("net/minecraft/world/World", "func_199532_z", "()L" + manager + ";", recipes, "manager");
        method(manager, "getResult", "(" + inventory + world + recipeType + ")Lnet/minecraft/item/ItemStack;", recipes, "getResult");
        method(manager, "func_199514_a", "(" + inventory + world + ")Lnet/minecraft/item/ItemStack;", recipes, "getCraftingResult");
        method(manager, "getRecipe", "(" + inventory + world + recipeType + ")Lnet/minecraft/item/crafting/IRecipe;", recipes, "getRecipe");
        method(manager, "func_199515_b", "(" + inventory + world + ")Lnet/minecraft/item/crafting/IRecipe;", recipes, "getCraftingRecipe");
        method(manager, "getRecipes", "(" + recipeType + ")Ljava/util/List;", recipes, "getRecipes");

        // Fuel.
        method("net/minecraft/item/ItemStack", "getBurnTime", "()I", misc, "getBurnTime");
        method("net/minecraftforge/event/ForgeEventFactory", "getItemBurnTime", "(Lnet/minecraft/item/ItemStack;I)I", misc, "getItemBurnTime");

        // Worlds give 1.13 code whole states, as 1.13's worlds keep them.
        method("net/minecraft/world/IBlockReader", "func_180495_p", "(" + pos + ")" + state, BLOCKS, "getActualBlockState");

        // Forge 1.13's block state methods.
        String stateType = "net/minecraft/block/state/IBlockState";
        String reader = "Lnet/minecraft/world/IBlockReader;";
        method(stateType, "func_196958_f", "()Z", BLOCKS, "isAir");
        method(stateType, "isAir", "(" + reader + pos + ")Z", BLOCKS, "isAir");
        method(stateType, "canBeConnectedTo", "(" + reader + pos + "Lnet/minecraft/util/EnumFacing;)Z", BLOCKS, "canBeConnectedTo");
        method(stateType, "hasTileEntity", "()Z", BLOCKS, "hasTileEntity");
        method(stateType, "func_196959_b", "(Lnet/minecraft/state/IProperty;)Z", BLOCKS, "has");
        method(stateType, "func_200016_a", "(" + reader + pos + ")I", BLOCKS, "getOpacity");
        method(stateType, "func_200131_a", "(" + reader + pos + ")Z", BLOCKS, "propagatesSkylightDown");
        method(stateType, "func_200132_m", "()Z", BLOCKS, "isSolid");
        method(stateType, "func_196952_d", "(" + reader + pos + ")Lnet/minecraft/util/math/shapes/VoxelShape;", BLOCKS, "getCollisionShape");
        method(stateType, "func_206871_b", "()Lcom/google/common/collect/ImmutableMap;", BLOCKS, "getValues");
        method("net/minecraft/block/Block", "func_208619_r", "()Z", BLOCKS, "isVariableOpacity");
        method("net/minecraft/block/Block", "func_200124_e", "(" + state + ")Z", BLOCKS, "isSolid");
        method("net/minecraft/block/Block", "func_196268_f", "(" + state + reader + pos + ")Lnet/minecraft/util/math/shapes/VoxelShape;", BLOCKS, "getCollisionShape");

        // 1.13 tile entities built on 1.12's vanilla ones.
        String tileEntities = SHIMS + "TileEntityShims";
        method("net/minecraft/tileentity/TileEntity", "func_195044_w", "()" + state, tileEntities, "getBlockState");
        method("net/minecraft/tileentity/TileEntityChest", "func_195480_a", "(F)F", tileEntities, "getLidAngle");
        method("net/minecraft/tileentity/IChestLid", "func_195480_a", "(F)F", tileEntities, "getLidAngle");
        method("net/minecraft/tileentity/TileEntitySign", "func_212366_a", "(I)Lnet/minecraft/util/text/ITextComponent;", tileEntities, "getText");
        method("net/minecraft/tileentity/TileEntitySign", "func_212365_a", "(ILnet/minecraft/util/text/ITextComponent;)V", tileEntities, "setText");
        method("net/minecraft/tileentity/TileEntitySign", "func_212364_a", "(ILjava/util/function/Function;)Ljava/lang/String;", tileEntities, "getRenderText");

        // 1.13's window and mouse are read from 1.12's Minecraft; colour handlers for blocks and items go to Forge 1.12's.
        String client = SHIMS + "client/ClientShims";
        field("net/minecraft/client/Minecraft", "field_195558_d", "Lnet/minecraft/client/MainWindow;",
                client + ".mainWindow(Lnet/minecraft/client/Minecraft;)Lnet/minecraft/client/MainWindow;");
        field("net/minecraft/client/Minecraft", "field_71417_B", "Lnet/minecraft/client/MouseHelper;",
                client + ".mouseHelper(Lnet/minecraft/client/Minecraft;)Lnet/minecraft/client/MouseHelper;");
        method("net/minecraft/client/renderer/texture/TextureAtlasSprite", "func_195668_m", "()Lnet/minecraft/util/ResourceLocation;", client, "getSpriteName");
        method("net/minecraft/client/renderer/texture/TextureMap", "func_195424_a",
                "(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;", client, "getSprite");
        method("net/minecraft/client/renderer/GameRenderer", "func_195458_a", "(FJZ)V", client, "updateCameraAndRender");
        String bakedModel = "net/minecraft/client/renderer/model/IBakedModel";
        method(bakedModel, "func_200117_a", "(" + state + "Lnet/minecraft/util/EnumFacing;Ljava/util/Random;)Ljava/util/List;", client, "getQuads");
        method(bakedModel, "getQuads", "(" + state + "Lnet/minecraft/util/EnumFacing;Ljava/util/Random;Lnet/minecraftforge/client/model/data/IModelData;)Ljava/util/List;",
                GLUE + "ModelGlue", "getQuads");
        method("net/minecraft/client/Minecraft", "func_195551_G", "()Lnet/minecraft/resources/IResourceManager;", client, "getResourceManager");
        field("net/minecraft/client/Minecraft", "field_195559_v", "Lnet/minecraft/client/KeyboardListener;",
                client + ".keyboardListener(Lnet/minecraft/client/Minecraft;)Lnet/minecraft/client/KeyboardListener;");
        // Key bindings: 1.13's inputs are GLFW's, 1.12's codes LWJGL 2's.
        String binding = "net/minecraft/client/settings/KeyBinding";
        String input = "Lnet/minecraft/client/util/InputMappings$Input;";
        method(binding, "getKey", "()" + input, client, "getKey");
        method(binding, "func_197977_i", "()" + input, client, "getDefault");
        method(binding, "func_197979_b", "(" + input + ")V", client, "bind");
        method(binding, "func_197980_a", "(" + input + "Z)V", client, "setKeyBindState");
        method(binding, "func_197981_a", "(" + input + ")V", client, "onTick");
        method(binding, "func_197983_b", "(Lnet/minecraft/client/settings/KeyBinding;)Z", client, "conflicts");
        method(binding, "func_197986_j", "()Z", client, "isInvalid");
        method(binding, "func_197976_a", "(II)Z", client, "matchesKey");
        method(binding, "func_197984_a", "(I)Z", client, "matchesMouseKey");
        method(binding, "func_197978_k", "()Ljava/lang/String;", client, "getLocalizedName");
        method(binding, "func_197985_l", "()Z", client, "isDefault");
        method(binding, "func_197982_m", "()Ljava/lang/String;", client, "getTranslationKey");
        method("net/minecraft/client/renderer/color/ItemColors", "func_199877_a",
                "(Lnet/minecraft/client/renderer/color/IItemColor;[Lnet/minecraft/util/IItemProvider;)V", client, "registerItemColor");
        method("net/minecraftforge/fml/client/registry/ClientRegistry", "bindTileEntitySpecialRenderer",
                "(Ljava/lang/Class;Lnet/minecraft/client/renderer/tileentity/TileEntityRenderer;)V", SHIMS + "client/ClientShims", "bindTileEntitySpecialRenderer");

        // Forge 1.13's item properties: tool types are Forge 1.13's class, so the glue reads them.
        method("net/minecraft/item/Item$Properties", "addToolType", "(Lnet/minecraftforge/common/ToolType;I)" + ITEM_PROPERTIES, GLUE + "ItemGlue", "addToolType");

        // 1.13's flattened blocks and items are 1.12 variants: stacks made of them get the item and metadata.
        String flat = "com/michaelsebero/portium/modern/flat/Flattening";
        String stackItem = flat + ".stackItem(Lnet/minecraft/util/IItemProvider;)Lnet/minecraft/item/Item;";
        String stackMeta = flat + ".stackMeta(Lnet/minecraft/util/IItemProvider;)I";
        constructor("net/minecraft/item/ItemStack", "(Lnet/minecraft/util/IItemProvider;)V", "(Lnet/minecraft/item/Item;II)V",
                arg(0, stackItem), arg(0, flat + ".one(Lnet/minecraft/util/IItemProvider;)I"), arg(0, stackMeta));
        constructor("net/minecraft/item/ItemStack", "(Lnet/minecraft/util/IItemProvider;I)V", "(Lnet/minecraft/item/Item;II)V", arg(0, stackItem), arg(1), arg(0, stackMeta));
        constructor("net/minecraft/item/ItemStack", "(Lnet/minecraft/util/IItemProvider;ILnet/minecraft/nbt/NBTTagCompound;)V",
                "(Lnet/minecraft/item/Item;IILnet/minecraft/nbt/NBTTagCompound;)V", arg(0, stackItem), arg(1), arg(0, stackMeta), arg(2));
        method("net/minecraft/block/state/IBlockState", "func_177230_c", "()Lnet/minecraft/block/Block;", flat, "getBlock");
        method("net/minecraft/item/ItemStack", "func_77973_b", "()Lnet/minecraft/item/Item;", flat, "getItem");
        method("net/minecraft/item/Item", "func_150898_a", "(Lnet/minecraft/block/Block;)Lnet/minecraft/item/Item;", flat, "itemFromBlock");
        method("net/minecraft/block/Block", "func_149634_a", "(Lnet/minecraft/item/Item;)Lnet/minecraft/block/Block;", flat, "blockFromItem");
        method("net/minecraft/entity/Entity", "func_200600_R", "()Lnet/minecraft/entity/EntityType;", "com/michaelsebero/portium/modern/ModernEntityTypes", "typeOf");
        // LWJGL 3's capabilities are LWJGL 2's context capabilities.
        if (!com.michaelsebero.portium.compat.Cleanroom.LWJGL3) {
            method("org/lwjgl/opengl/GL", "getCapabilities", "()Lorg/lwjgl/opengl/GLCapabilities;", "org/lwjgl/opengl/GLContext", "getCapabilities");
        }
        // 1.13's text components are Brigadier messages; 1.12's aren't, so what Brigadier asks of a message comes here.
        method("com/mojang/brigadier/Message", "getString", "()Ljava/lang/String;", GLUE + "ModernCommands", "getString");
        // 1.12 makes a basic inventory from a text component only on the client.
        String conversions = SHIMS + "Conversions.";
        constructor("net/minecraft/inventory/InventoryBasic", "(Lnet/minecraft/util/text/ITextComponent;I)V", "(Ljava/lang/String;ZI)V",
                arg(0, conversions + "text(Lnet/minecraft/util/text/ITextComponent;)Ljava/lang/String;"), arg(0, conversions + "named(Ljava/lang/Object;)Z"), arg(1));
        // 1.13's key codes are GLFW's; 1.12's are LWJGL 2's.
        String keys = "com/michaelsebero/portium/modern/client/ModernKeys.toLwjgl(I)I";
        constructor("net/minecraft/client/settings/KeyBinding", "(Ljava/lang/String;ILjava/lang/String;)V", "(Ljava/lang/String;ILjava/lang/String;)V",
                arg(0), arg(1, keys), arg(2));
        constructor("net/minecraft/client/settings/KeyBinding", "(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;ILjava/lang/String;)V",
                "(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;ILjava/lang/String;)V", arg(0), arg(1), arg(2, keys), arg(3));
        // Forge 1.13 takes index -1 as the next free tab; 1.12 has a constructor without an index for that.
        constructor("net/minecraft/item/ItemGroup", "(ILjava/lang/String;)V", "(ILjava/lang/String;)V", arg(0, ITEMS + ".groupIndex(I)I"), arg(1));
    }

    private static Argument arg(int from) {
        return new Argument(from, null);
    }

    private static Argument arg(int from, String converter) {
        return new Argument(from, converter);
    }

    private void method(String declarer, String name, String desc, String owner, String target) {
        this.methods.computeIfAbsent(name + desc, key -> new ArrayList<>(1)).add(new Redirect(declarer, owner, target));
    }

    /** @param desc 1.12's descriptor, by 1.12's names */
    private void constructor(String owner, String modernDesc, String desc, Argument... arguments) {
        this.constructors.put(owner + modernDesc, new Constructor(desc, arguments));
    }

    private void field(String owner, String name, String desc, String getter) {
        field(owner, name, desc, getter, null);
    }

    private void field(String owner, String name, String desc, String getter, @Nullable String setter) {
        this.fields.computeIfAbsent(name + " " + desc, key -> new ArrayList<>(1)).add(new FieldRedirect(owner, getter, setter));
    }

    /** {@link #adapter}, for those without the redirects at hand. */
    @Nullable
    static String adapterOf(String type) {
        return ADAPTED.get(type);
    }

    /** The adapter 1.13 code extends or makes in place of a 1.12 class, or null if it has none. */
    @Nullable
    String adapter(String type) {
        return ADAPTED.get(type);
    }

    /**
     * A static method ({@code owner.name desc}) standing in for reading a 1.13 field 1.12 lacks, or null; for an
     * instance field it takes the object the field is read from.
     */
    @Nullable
    String findField(String owner, String name, String desc, Hierarchy hierarchy) {
        FieldRedirect redirect = findFieldRedirect(owner, name, desc, hierarchy);
        return redirect == null ? null : redirect.getter;
    }

    /** A static method standing in for writing such a field, taking the object (for an instance field) and the value; or null. */
    @Nullable
    String findFieldSetter(String owner, String name, String desc, Hierarchy hierarchy) {
        FieldRedirect redirect = findFieldRedirect(owner, name, desc, hierarchy);
        return redirect == null ? null : redirect.setter;
    }

    @Nullable
    private FieldRedirect findFieldRedirect(String owner, String name, String desc, Hierarchy hierarchy) {
        List<FieldRedirect> candidates = this.fields.get(name + " " + desc);
        if (candidates == null) return null;
        for (FieldRedirect redirect : candidates) {
            if (redirect.declarer.equals(owner)) return redirect;
        }
        for (FieldRedirect redirect : candidates) {
            if (hierarchy.isSubtype(owner, redirect.declarer)) return redirect;
        }
        return null;
    }

    /** How to call 1.13's constructor {@code owner.<init> desc} on 1.12, or null if 1.12 has it. */
    @Nullable
    Constructor findConstructor(String owner, String desc) {
        return this.constructors.get(owner + desc);
    }

    /**
     * The stand-in for a call to {@code owner.name desc}, or null if 1.12 has the method: calls through the declarer
     * or its subtypes. (An SRG id is shared by every override, so it alone doesn't tell the class.)
     */
    @Nullable
    Redirect find(String owner, String name, String desc, Hierarchy hierarchy) {
        List<Redirect> candidates = this.methods.get(name + desc);
        if (candidates == null) return null;
        for (Redirect redirect : candidates) {
            if (redirect.declarer.equals(owner)) return redirect;
        }
        for (Redirect redirect : candidates) {
            if (hierarchy.isSubtype(owner, redirect.declarer)) return redirect;
        }
        return null;
    }
}
