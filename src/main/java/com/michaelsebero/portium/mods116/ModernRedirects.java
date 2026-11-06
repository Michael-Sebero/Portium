package com.michaelsebero.portium.mods116;

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

        /** The names, as written (untranslated), of the fields a class this loader defines declares itself; empty for any other. */
        default java.util.Set<String> declaredFields(String type) {
            return java.util.Collections.emptySet();
        }

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

        /** As {@link #declaresBelowAdapters}, for a field. */
        boolean declaresFieldBelowAdapters(String type, String name, String desc);

        /** The methods (name and descriptor, as translated) that a class this loader defines declares, by translated name. */
        Set<String> translatedMethods(String type);
    }

    static final class Redirect {

        /** The 1.13 class that declares the method. */
        final String declarer;
        final String owner;
        final String name;

        /** The stand-in's own descriptor, where it isn't made from the call's. */
        @Nullable
        final String targetDesc;

        Redirect(String declarer, String owner, String name) {
            this(declarer, owner, name, null);
        }

        Redirect(String declarer, String owner, String name, @Nullable String targetDesc) {
            this.declarer = declarer;
            this.owner = owner;
            this.name = name;
            this.targetDesc = targetDesc;
        }

        /** The stand-in's descriptor for a call with this opcode and the original descriptor. */
        String desc(int opcode, String desc) {
            if (this.targetDesc != null) return this.targetDesc;
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
    /** Shims for what 1.16 added beyond 1.13. */
    private static final String SHIMS116 = "com/michaelsebero/portium/mods116/shim/";
    private static final String PARTICLES = SHIMS + "ParticleShims";
    private static final String BLOCKS = SHIMS + "BlockShims";
    private static final String ITEMS = SHIMS + "ItemShims";
    private static final String REGISTRIES = SHIMS + "RegistryShims";
    private static final String AS_ITEM = SHIMS + "ItemShims.asItem(Lnet/minecraft/util/IItemProvider;)Lnet/minecraft/item/Item;";
    private static final String ITEM_PROPERTIES = "Lnet/minecraft/item/Item$Properties;";

    private static final String ADAPTERS = "com/michaelsebero/portium/modern/adapter/";
    private static final String GLUE = "com/michaelsebero/portium/mods116/host/";

    /** 1.12 classes 1.13 code gets Portium's adapters of when it extends or makes them. */
    private static final Map<String, String> ADAPTED = new HashMap<>();

    static {
        // 1.16's block: Portium's 1.13 one with 1.16's shapes, clicks, ticks and updates.
        ADAPTED.put("net/minecraft/block/Block", "com/michaelsebero/portium/mods116/adapter/Block116");
        ADAPTED.put("net/minecraft/block/BlockBed", ADAPTERS + "ModernBlockBed");
        ADAPTED.put("net/minecraft/item/Item", ADAPTERS + "ModernItem");
        ADAPTED.put("net/minecraft/item/ItemBlock", ADAPTERS + "ModernItemBlock");
        ADAPTED.put("net/minecraft/item/ItemBed", ADAPTERS + "ModernItemBlock");
        // 1.16's tile entity: Portium's 1.13 one, loading with its block's state.
        ADAPTED.put("net/minecraft/tileentity/TileEntity", "com/michaelsebero/portium/mods116/adapter/TileEntity116");
        // 1.13's screens, buttons and text fields, with 1.13's input.
        ADAPTED.put("net/minecraft/client/gui/GuiScreen", ADAPTERS + "ModernGuiScreen");
        ADAPTED.put("net/minecraft/client/gui/inventory/GuiContainer", ADAPTERS + "ModernGuiContainer");
        ADAPTED.put("net/minecraft/client/gui/GuiButton", ADAPTERS + "ModernGuiButton");
        ADAPTED.put("net/minecraft/client/gui/GuiTextField", ADAPTERS + "ModernGuiTextField");
        // The same classes by their 1.16 names.
        ADAPTED.put("net/minecraft/item/BlockItem", ADAPTERS + "ModernItemBlock");
        ADAPTED.put("net/minecraft/item/BedItem", ADAPTERS + "ModernItemBlock");
        ADAPTED.put("net/minecraft/block/BedBlock", ADAPTERS + "ModernBlockBed");
        ADAPTED.put("net/minecraft/client/gui/screen/Screen", "com/michaelsebero/portium/mods116/adapter/Screen116");
        ADAPTED.put("net/minecraft/client/gui/screen/inventory/ContainerScreen", "com/michaelsebero/portium/mods116/adapter/ContainerScreen116");
        ADAPTED.put("net/minecraft/client/gui/widget/TextFieldWidget", ADAPTERS + "ModernGuiTextField");
        // 1.16's crafting recipes are 1.12's, made with an id (and extended so: Create's mechanical crafting).
        ADAPTED.put("net/minecraft/item/crafting/ShapedRecipe", "com/michaelsebero/portium/mods116/adapter/ShapedRecipe116");
        ADAPTED.put("net/minecraft/item/crafting/ShapelessRecipe", "com/michaelsebero/portium/mods116/adapter/ShapelessRecipe116");
        // 1.16's containers (menus): 1.12's, made with a menu type (taken loosely) and a window id.
        ADAPTED.put("net/minecraft/inventory/container/Container", "com/michaelsebero/portium/mods116/adapter/Container116");
        // A mod's own worlds (Create's contraption worlds): 1.12 worlds holding nothing, made from 1.16's arguments.
        ADAPTED.put("net/minecraft/world/World", "com/michaelsebero/portium/mods116/adapter/World116");
        // Rails stay 1.12's, for minecarts.
        ADAPTED.put("net/minecraft/block/AbstractRailBlock", "com/michaelsebero/portium/mods116/adapter/AbstractRailBlock116");
        // Stairs stay 1.12's, with Portium's additions.
        ADAPTED.put("net/minecraft/block/StairsBlock", "com/michaelsebero/portium/mods116/adapter/StairsBlock116");
        // Buckets of 1.16 fluids.
        ADAPTED.put("net/minecraft/item/BucketItem", "com/michaelsebero/portium/mods116/adapter/BucketItem116");
        // 1.16's item stack renderers likewise (client).
        ADAPTED.put("net/minecraft/client/renderer/tileentity/ItemStackTileEntityRenderer", "com/michaelsebero/portium/mods116/adapter/ItemStackTileEntityRenderer116");
        // 1.16's tile entity renderers draw with a matrix stack into render buffers (client).
        ADAPTED.put("net/minecraft/client/renderer/tileentity/TileEntityRenderer", "com/michaelsebero/portium/mods116/adapter/TileEntityRenderer116");
        // 1.16's entity renderers likewise: a mod's renderer draws through 1.16's render with 1.16's stack and buffers.
        ADAPTED.put("net/minecraft/client/renderer/entity/EntityRenderer", "com/michaelsebero/portium/mods116/adapter/EntityRenderer116");
        // 1.16's entity layers are a class (1.12's an interface): a mod's layer draws through 1.16's render likewise.
        ADAPTED.put("net/minecraft/client/renderer/entity/layers/LayerRenderer", "com/michaelsebero/portium/mods116/adapter/LayerRenderer116");
        // 1.16's armor: 1.12's, so it's worn, drawn and counted as armor, with 1.16's material.
        ADAPTED.put("net/minecraft/item/ArmorItem", "com/michaelsebero/portium/mods116/adapter/ArmorItem116");
        // 1.16's item override lists can be made bare (1.12's bare constructor is private), and answer 1.16's question.
        ADAPTED.put("net/minecraft/client/renderer/model/ItemOverrideList", "com/michaelsebero/portium/mods116/adapter/ItemOverrideList116");
        // 1.16 builds a material from its properties.
        // 1.16's particles: 1.12's, drawn by their 1.16 render methods (Minecraft 1.16.5's own particle classes build on it too).
        ADAPTED.put("net/minecraft/client/particle/Particle", "com/michaelsebero/portium/mods116/adapter/Particle116");
        // 1.16's potion effects: 1.12's potions, made from an effect kind and a colour.
        ADAPTED.put("net/minecraft/potion/Effect", "com/michaelsebero/portium/mods116/adapter/Effect116");
        ADAPTED.put("net/minecraft/block/material/Material", "com/michaelsebero/portium/mods116/adapter/Material116");
    }

    /**
     * Casts 1.16 code makes to classes that 1.12's objects of the same thing aren't (1.12 makes one ray trace result
     * class for blocks and entities alike): by class, a static method converting an object, and one testing it.
     */
    private static final Map<String, String[]> CASTS = new HashMap<>();

    static {
        String math = SHIMS116 + "MathShims116";
        CASTS.put("net/minecraft/util/math/BlockRayTraceResult", new String[] {math, "asBlockHit", "isBlockHit"});
        CASTS.put("net/minecraft/util/math/EntityRayTraceResult", new String[] {math, "asEntityHit", "isEntityHit"});
        // Furnaces: 1.12's and Portium's blast furnace and smoker aren't 1.16's AbstractFurnaceBlock (only instanceof).
        CASTS.put("net/minecraft/block/AbstractFurnaceBlock", new String[] {SHIMS116 + "BlockShims116", null, "isFurnace"});
        // 1.12's storage block entities as 1.16's (whose classes are 1.16's own here): only instanceof.
        String tiles = SHIMS116 + "TileEntityShims116";
        CASTS.put("net/minecraft/tileentity/ChestTileEntity", new String[] {tiles, null, "isChest"});
        CASTS.put("net/minecraft/tileentity/TrappedChestTileEntity", new String[] {tiles, null, "isTrappedChest"});
        CASTS.put("net/minecraft/tileentity/ShulkerBoxTileEntity", new String[] {tiles, null, "isShulkerBox"});
        CASTS.put("net/minecraft/tileentity/BarrelTileEntity", new String[] {tiles, null, "isBarrel"});
    }

    /** For a cast to {@code type} (1.16 name): the shim's class, its converting method and its testing method; or null. */
    @Nullable
    String[] cast(String type) {
        return CASTS.get(type);
    }

    private final Map<String, List<Redirect>> methods = new HashMap<>();
    /** Static methods making what 1.16 code makes with {@code new}, by 1.16 class and constructor descriptor. */
    private final Map<String, String> factories = new HashMap<>();
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
        method("net/minecraft/world/server/ServerWorld", "func_195598_a", "(" + particle + "DDDIDDDD)I", PARTICLES, "spawnParticle");
        method("net/minecraft/world/server/ServerWorld", "func_195600_a", "(Lnet/minecraft/entity/player/ServerPlayerEntity;" + particle + "ZDDDIDDDD)Z",
                PARTICLES, "spawnParticle");

        method("net/minecraft/block/Block", "getPickBlock", "(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/RayTraceResult;"
                + "Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/item/ItemStack;",
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

        String state = "Lnet/minecraft/block/BlockState;";
        String pos = "Lnet/minecraft/util/math/BlockPos;";
        String world = "Lnet/minecraft/world/World;";
        method("net/minecraft/block/Block", "func_196246_j", "(" + state + ")I", BLOCKS, "getStateId");
        method("net/minecraft/block/Block", "func_199769_a", "(" + state + world + pos + "I)Lnet/minecraft/util/IItemProvider;", BLOCKS, "getItemDropped");
        method("net/minecraft/block/Block", "func_208617_a", "(DDDDDD)Lnet/minecraft/util/math/shapes/VoxelShape;", BLOCKS, "makeCuboidShape");
        method("net/minecraft/block/Block", "func_196244_b", "(" + state + "Lnet/minecraft/world/IBlockReader;" + pos + ")Lnet/minecraft/util/math/shapes/VoxelShape;",
                BLOCKS, "getShape");
        method("net/minecraft/block/Block", "func_185473_a", "(Lnet/minecraft/world/IBlockReader;" + pos + state + ")Lnet/minecraft/item/ItemStack;", BLOCKS, "getItem");
        method("net/minecraft/block/BedBlock", "func_196350_d", "()Lnet/minecraft/item/DyeColor;", BLOCKS, "getBedColor");
        method("net/minecraft/block/BlockState", "func_196949_c", "(" + world + pos + "I)V", BLOCKS, "dropBlockAsItem");
        method("net/minecraft/block/BlockState", "func_196953_a", "(Lnet/minecraft/item/BlockItemUseContext;)Z", BLOCKS, "isReplaceable");
        // Forge 1.16's rail hooks (state first) on any rail: 1.12's (Create's minecart contraptions follow vanilla rails).
        String rail116 = "net/minecraft/block/AbstractRailBlock";
        String railState = "Lnet/minecraft/block/BlockState;";
        String railReader = "Lnet/minecraft/world/IBlockReader;";
        String railPos = "Lnet/minecraft/util/math/BlockPos;";
        String railCart = "Lnet/minecraft/entity/item/minecart/AbstractMinecartEntity;";
        String rails = SHIMS116 + "RailShims116";
        method(rail116, "getRailDirection", "(" + railState + railReader + railPos + railCart + ")Lnet/minecraft/state/properties/RailShape;", rails, "getRailDirection");
        method(rail116, "getRailMaxSpeed", "(" + railState + "Lnet/minecraft/world/World;" + railPos + railCart + ")F", rails, "getRailMaxSpeed");
        method(rail116, "isFlexibleRail", "(" + railState + railReader + railPos + ")Z", rails, "isFlexibleRail");
        method(rail116, "canMakeSlopes", "(" + railState + railReader + railPos + ")Z", rails, "canMakeSlopes");
        method(rail116, "onMinecartPass", "(" + railState + "Lnet/minecraft/world/World;" + railPos + railCart + ")V", rails, "onMinecartPass");
        // Forge 1.16's fluid stacks over the network (IForgePacketBuffer's): Forge 1.16's own FluidStack writes and reads.
        method("net/minecraft/network/PacketBuffer", "writeFluidStack", "(Lnet/minecraftforge/fluids/FluidStack;)V", GLUE + "FluidGlue", "writeFluidStack",
                "(Lnet/minecraft/network/PacketBuffer;Lnet/minecraftforge/fluids/FluidStack;)V");
        method("net/minecraft/network/PacketBuffer", "readFluidStack", "()Lnet/minecraftforge/fluids/FluidStack;", GLUE + "FluidGlue", "readFluidStack",
                "(Lnet/minecraft/network/PacketBuffer;)Lnet/minecraftforge/fluids/FluidStack;");
        // ModelBakery.LOCATIONS_BUILTIN_TEXTURES: 1.16's holds materials, 1.12's (same SRG id) texture names.
        field("net/minecraft/client/renderer/model/ModelBakery", "field_177602_b", "Ljava/util/Set;", GLUE + "RenderGlue.builtinMaterials()Ljava/util/Set;", null);
        // RecipeBookContainer's grid methods on 1.12's crafting containers.
        String recipeBook = "net/minecraft/inventory/container/RecipeBookContainer";
        String containers = SHIMS116 + "ContainerShims116";
        method(recipeBook, "func_201771_a", "(Lnet/minecraft/item/crafting/RecipeItemHelper;)V", containers, "fillStackedContents");
        method(recipeBook, "func_201768_e", "()V", containers, "clearCraftingContent");
        method(recipeBook, "func_201769_a", "(Lnet/minecraft/item/crafting/IRecipe;)Z", containers, "recipeMatches");
        method(recipeBook, "func_201767_f", "()I", containers, "resultSlot");
        method(recipeBook, "func_201770_g", "()I", containers, "gridWidth");
        method(recipeBook, "func_201772_h", "()I", containers, "gridHeight");
        method(recipeBook, "func_203721_h", "()I", containers, "size");
        // Forge 1.16's GameData privates that mods reflect on, and its key-based registry wrapper.
        method("java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", SHIMS116 + "ReflectShims116", "getDeclaredMethod");
        method("net/minecraftforge/registries/GameData", "getWrapper",
                "(Lnet/minecraft/util/RegistryKey;Lcom/mojang/serialization/Lifecycle;Ljava/lang/String;)Lnet/minecraft/util/registry/DefaultedRegistry;",
                GLUE + "RegistryGlue", "getWrapper",
                "(Lnet/minecraft/util/RegistryKey;Lcom/mojang/serialization/Lifecycle;Ljava/lang/String;)Lnet/minecraft/util/registry/DefaultedRegistry;");
        // Effect.getCategory on any effect (1.12's potions have bad/beneficial flags instead).
        method("net/minecraft/potion/Effect", "func_220303_e", "()Lnet/minecraft/potion/EffectType;", GLUE + "EntityGlue", "effectType",
                "(Lnet/minecraft/potion/Effect;)Lnet/minecraft/potion/EffectType;");
        method("net/minecraft/entity/ai/attributes/Attribute", "func_233754_c_", "()Ljava/lang/String;", misc116Class(), "attributeDescription");
        // AttributeModifier.getOperation: 1.12 keeps a number.
        method("net/minecraft/entity/ai/attributes/AttributeModifier", "func_220375_c", "()Lnet/minecraft/entity/ai/attributes/AttributeModifier$Operation;",
                GLUE + "EntityGlue", "modifierOperation");
        // Forge 1.16's enum extensions on 1.12's enums.
        method("net/minecraft/item/Rarity", "create", "(Ljava/lang/String;Lnet/minecraft/util/text/TextFormatting;)Lnet/minecraft/item/Rarity;",
                SHIMS116 + "ItemShims116", "createRarity");
        // Direction.Axis.CODEC (1.12's axis enum has none; Quark's jigsaw processors read it).
        field("net/minecraft/util/Direction$Axis", "field_239632_d_", "Lcom/mojang/serialization/Codec;", GLUE + "CodecGlue.axis()Lcom/mojang/serialization/Codec;", null);
        // ItemStack.useOn: 1.12's onItemUse, which reaches 1.12's items too (1.16's body only knows 1.16 items).
        method("net/minecraft/item/ItemStack", "func_196084_a", "(Lnet/minecraft/item/ItemUseContext;)Lnet/minecraft/util/ActionResultType;",
                SHIMS116 + "ItemStackShims116", "useOn");
        // 1.16's state methods, on its state classes (1.12's block state, by Portium's mapping).
        String states116 = SHIMS116 + "StateShims116";
        String property = "Lnet/minecraft/state/Property;";
        for (String holder : new String[] {"net/minecraft/state/StateHolder", "net/minecraft/block/AbstractBlock$AbstractBlockState", "net/minecraft/block/BlockState"}) {
            method(holder, "func_206870_a", "(" + property + "Ljava/lang/Comparable;)Ljava/lang/Object;", states116, "with");
            method(holder, "func_235896_a_", "(" + property + ")Ljava/lang/Object;", states116, "cycle");
            method(holder, "func_235901_b_", "(" + property + ")Z", states116, "hasProperty");
            method(holder, "func_177229_b", "(" + property + ")Ljava/lang/Comparable;", states116, "getValue");
            method(holder, "func_235904_r_", "()Ljava/util/Collection;", states116, "getProperties");
            method(holder, "func_235903_d_", "(" + property + ")Ljava/util/Optional;", states116, "getOptional");
            method(holder, "func_206871_b", "()Lcom/google/common/collect/ImmutableMap;", states116, "getValues");
            method(holder, "func_230340_p_", "()Lnet/minecraft/block/BlockState;", states116, "self");
        }
        // 1.16's block methods on any block, and its state methods that call them: 1.16 blocks answer, 1.12's blocks
        // through their own methods (BlockShims116).
        String blocks116 = SHIMS116 + "BlockShims116";
        String state116 = "Lnet/minecraft/block/BlockState;";
        String reader116 = "Lnet/minecraft/world/IBlockReader;";
        String pos116 = "Lnet/minecraft/util/math/BlockPos;";
        String shape116 = "Lnet/minecraft/util/math/shapes/VoxelShape;";
        String context116 = "Lnet/minecraft/util/math/shapes/ISelectionContext;";
        String player116 = "Lnet/minecraft/entity/player/PlayerEntity;";
        String hit116 = "Lnet/minecraft/util/math/BlockRayTraceResult;";
        String result116 = "Lnet/minecraft/util/ActionResultType;";
        for (String block116 : new String[] {"net/minecraft/block/AbstractBlock", "net/minecraft/block/Block"}) {
            method(block116, "func_220053_a", "(" + state116 + reader116 + pos116 + context116 + ")" + shape116, blocks116, "getShape");
            method(block116, "func_220071_b", "(" + state116 + reader116 + pos116 + context116 + ")" + shape116, blocks116, "getCollisionShape");
            method(block116, "func_225533_a_", "(" + state116 + "Lnet/minecraft/world/World;" + pos116 + player116 + "Lnet/minecraft/util/Hand;" + hit116 + ")"
                    + result116, blocks116, "onBlockActivated");
        }
        for (String holder : new String[] {"net/minecraft/block/AbstractBlock$AbstractBlockState", "net/minecraft/block/BlockState"}) {
            method(holder, "func_196954_c", "(" + reader116 + pos116 + ")" + shape116, blocks116, "getShape");
            method(holder, "func_215700_a", "(" + reader116 + pos116 + context116 + ")" + shape116, blocks116, "getShape");
            method(holder, "func_196952_d", "(" + reader116 + pos116 + ")" + shape116, blocks116, "getCollisionShape");
            method(holder, "func_196951_e", "(" + reader116 + pos116 + ")" + shape116, blocks116, "getCollisionShape");
            method(holder, "func_215685_b", "(" + reader116 + pos116 + context116 + ")" + shape116, blocks116, "getCollisionShape");
            method(holder, "func_235754_c_", "(" + reader116 + pos116 + ")" + shape116, blocks116, "getRenderShape");
            method(holder, "func_227031_a_", "(Lnet/minecraft/world/World;" + player116 + "Lnet/minecraft/util/Hand;" + hit116 + ")" + result116, blocks116,
                    "onBlockActivated");
            method(holder, "func_196942_a", "(Lnet/minecraft/world/World;" + pos116 + player116 + ")V", blocks116, "onBlockClicked");
            method(holder, "func_196956_a", "(Lnet/minecraft/util/Direction;" + state116 + "Lnet/minecraft/world/IWorld;" + pos116 + pos116 + ")" + state116, blocks116,
                    "updatePostPlacement");
            method(holder, "func_215697_a", "(Lnet/minecraft/world/World;" + pos116 + "Lnet/minecraft/block/Block;" + pos116 + "Z)V", blocks116, "neighborChanged");
            method(holder, "func_196955_c", "(Lnet/minecraft/world/IWorldReader;" + pos116 + ")Z", blocks116, "isValidPosition");
            method(holder, "getPickBlock", "(Lnet/minecraft/util/math/RayTraceResult;" + reader116 + pos116 + player116 + ")Lnet/minecraft/item/ItemStack;", blocks116,
                    "getPickBlock");
        }
        // 1.16's state questions that 1.16 answers from fields 1.12's states lack (see StateShims116), on both classes
        // 1.16 calls them through; and the 1.13 ones that 1.16's own code calls through AbstractBlockState.
        String dir116 = "Lnet/minecraft/util/Direction;";
        String at = "(" + state116 + reader116 + pos116;
        for (String holder : new String[] {"net/minecraft/block/AbstractBlock$AbstractBlockState", "net/minecraft/block/BlockState"}) {
            method(holder, "func_224755_d", "(" + reader116 + pos116 + dir116 + ")Z", states116, "isFaceSturdy", at + dir116 + ")Z");
            method(holder, "func_242698_a", "(" + reader116 + pos116 + dir116 + "Lnet/minecraft/util/BlockVoxelShape;)Z", states116, "isFaceSturdy",
                    at + dir116 + "Ljava/lang/Object;)Z");
            method(holder, "func_215682_a", "(" + reader116 + pos116 + "Lnet/minecraft/entity/Entity;" + dir116 + ")Z", states116, "isTopSolid",
                    at + "Ljava/lang/Object;" + dir116 + ")Z");
            method(holder, "func_215702_a", "(" + reader116 + pos116 + dir116 + ")" + shape116, states116, "faceOcclusionShape", at + dir116 + ")" + shape116);
            method(holder, "func_215704_f", "()Z", states116, "hasLargeCollisionShape", "(" + state116 + ")Z");
            method(holder, "func_215691_g", "()Z", states116, "useShapeForLightOcclusion", "(" + state116 + ")Z");
            method(holder, "func_235783_q_", "()Z", states116, "requiresCorrectToolForDrops", "(" + state116 + ")Z");
            String[][] questions = {{"func_227035_k_", "emissiveRendering"}, {"func_215686_e", "isRedstoneConductor"}, {"func_200015_d", "isSolidRender"},
                    {"func_229980_m_", "isSuffocating"}, {"func_215696_m", "isViewBlocking"}, {"func_202065_c", "hasPostProcess"},
                    {"func_235785_r_", "isCollisionShapeFullBlock"}};
            for (String[] question : questions) method(holder, question[0], "(" + reader116 + pos116 + ")Z", states116, question[1], at + ")Z");
            method(holder, "func_185887_b", "(" + reader116 + pos116 + ")F", states116, "destroySpeed", at + ")F");
        }
        String abstractState = "net/minecraft/block/AbstractBlock$AbstractBlockState";
        // Forge 1.16's block state methods (IForgeBlockState defaults), which 1.12's state lacks.
        String forgeShims = SHIMS116 + "BlockShims116";
        String forgeEntity = "Lnet/minecraft/entity/Entity;";
        String player116s = "Lnet/minecraft/entity/player/PlayerEntity;";
        for (String holder : new String[] {"net/minecraft/block/BlockState", abstractState, "net/minecraftforge/common/extensions/IForgeBlockState"}) {
            method(holder, "getSoundType", "(Lnet/minecraft/world/IWorldReader;" + pos116 + forgeEntity + ")Lnet/minecraft/block/SoundType;", forgeShims, "soundType");
            method(holder, "getSlipperiness", "(Lnet/minecraft/world/IWorldReader;" + pos116 + forgeEntity + ")F", forgeShims, "slipperiness");
            method(holder, "rotate", "(Lnet/minecraft/world/IWorld;" + pos116 + "Lnet/minecraft/util/Rotation;)" + state116, forgeShims, "rotate");
            method(holder, "createTileEntity", "(" + reader116 + ")Lnet/minecraft/tileentity/TileEntity;", forgeShims, "createTileEntity");
            method(holder, "removedByPlayer", "(Lnet/minecraft/world/World;" + pos116 + player116s + "ZLnet/minecraft/fluid/FluidState;)Z", forgeShims, "removedByPlayer",
                    "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/EntityPlayer;ZLjava/lang/Object;)Z");
            method(holder, "canSustainPlant", "(" + reader116 + pos116 + "Lnet/minecraft/util/Direction;Lnet/minecraftforge/common/IPlantable;)Z", forgeShims, "canSustainPlant");
            method(holder, "canStickTo", "(" + state116 + ")Z", forgeShims, "canStickTo");
            method(holder, "canHarvestBlock", "(" + reader116 + pos116 + player116s + ")Z", forgeShims, "canHarvestBlock");
            method(holder, "addRunningEffects", "(Lnet/minecraft/world/World;" + pos116 + forgeEntity + ")Z", forgeShims, "addRunningEffects");
        }
        // 1.16 made the build height check static; 1.12's of the same id is the world's own.
        method("net/minecraft/world/World", "func_189509_E", "(" + pos116 + ")Z", SHIMS116 + "WorldShims116", "isOutsideBuildHeight", "(" + pos116 + ")Z");
        method("net/minecraft/world/spawner/WorldEntitySpawner", "func_209382_a",
                "(Lnet/minecraft/entity/EntitySpawnPlacementRegistry$PlacementType;Lnet/minecraft/world/IWorldReader;" + pos116 + "Lnet/minecraft/entity/EntityType;)Z",
                SHIMS116 + "WorldShims116", "canSpawnAt", "(Ljava/lang/Object;Lnet/minecraft/world/World;" + pos116 + "Ljava/lang/Object;)Z");
        // 1.16's template placing (Create's ponder scenes build their worlds from structure templates).
        String template116 = "net/minecraft/world/gen/feature/template/Template";
        String settings116 = "Lnet/minecraft/world/gen/feature/template/PlacementSettings;";
        method(template116, "func_186256_b", "(Lnet/minecraft/nbt/CompoundNBT;)V", SHIMS116 + "WorldShims116", "readTemplate");
        method(template116, "func_237152_b_", "(Lnet/minecraft/world/IServerWorld;" + pos116 + settings116 + "Ljava/util/Random;)V", SHIMS116 + "WorldShims116", "placeTemplate");
        method(template116, "func_237144_a_", "(Lnet/minecraft/world/IServerWorld;" + pos116 + settings116 + "Ljava/util/Random;)V", SHIMS116 + "WorldShims116",
                "placeTemplateInChunk");
        String settings116Owner = "net/minecraft/world/gen/feature/template/PlacementSettings";
        method(settings116Owner, "func_215222_a", "(Lnet/minecraft/world/gen/feature/template/StructureProcessor;)" + settings116, SHIMS116 + "WorldShims116",
                "addProcessor", "(" + settings116 + "Ljava/lang/Object;)" + settings116);
        method(settings116Owner, "func_215219_b", "()" + settings116, SHIMS116 + "WorldShims116", "clearProcessors");
        method(settings116Owner, "func_207664_d", "()" + pos116, SHIMS116 + "WorldShims116", "rotationPivot");
        method(settings116Owner, "func_207665_a", "(" + pos116 + ")" + settings116, SHIMS116 + "WorldShims116", "setRotationPivot");
        method(settings116Owner, "func_215220_b", "(Lnet/minecraft/world/gen/feature/template/StructureProcessor;)" + settings116, SHIMS116 + "WorldShims116",
                "removeProcessor", "(" + settings116 + "Ljava/lang/Object;)" + settings116);
        method(settings116Owner, "func_215221_j", "()Ljava/util/List;", SHIMS116 + "WorldShims116", "processors");
        method(template116, "func_237146_a_", "(Lnet/minecraft/world/IServerWorld;" + pos116 + pos116 + settings116 + "Ljava/util/Random;I)Z", SHIMS116 + "WorldShims116",
                "placeTemplate");
        method(abstractState, "func_196958_f", "()Z", BLOCKS, "isAir", "(" + state116 + ")Z");
        method(abstractState, "func_200016_a", "(" + reader116 + pos116 + ")I", BLOCKS, "getOpacity", at + ")I");
        method(abstractState, "func_200131_a", "(" + reader116 + pos116 + ")Z", BLOCKS, "propagatesSkylightDown", at + ")Z");
        method(abstractState, "func_200132_m", "()Z", BLOCKS, "isSolid", "(" + state116 + ")Z");
        // Forge 1.16's entity renderers by type, and widgets of screens being set up (client).
        method("net/minecraftforge/fml/client/registry/RenderingRegistry", "registerEntityRenderingHandler",
                "(Lnet/minecraft/entity/EntityType;Lnet/minecraftforge/fml/client/registry/IRenderFactory;)V", SHIMS116 + "ClientShims116", "registerEntityRenderer");
        method("net/minecraftforge/client/event/GuiScreenEvent$InitGuiEvent", "getWidgetList", "()Ljava/util/List;", SHIMS116 + "ClientShims116", "widgets");
        method("net/minecraftforge/client/event/GuiScreenEvent$InitGuiEvent", "addWidget", "(Lnet/minecraft/client/gui/widget/Widget;)V", SHIMS116 + "ClientShims116",
                "addWidget");
        // A tile entity's renderer: 1.12's wrapped as 1.16's (Create draws a contraption's tile entities with them).
        method("net/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher", "func_147547_b",
                "(Lnet/minecraft/tileentity/TileEntity;)Lnet/minecraft/client/renderer/tileentity/TileEntityRenderer;",
                "com/michaelsebero/portium/client/TileEntityRendering116", "rendererFor");
        // Any tile entity's type: 1.12's (a chest on a contraption) have none of their own.
        method("net/minecraft/tileentity/TileEntity", "func_200662_C", "()Lnet/minecraft/tileentity/TileEntityType;",
                "com/michaelsebero/portium/modern/mc/tileentity/TileEntityType", "of");
        // 1.16's entity position and motion, kept in fields 1.12 lacks (EntityShims116).
        String entities116 = SHIMS116 + "EntityShims116";
        String entity116 = "net/minecraft/entity/Entity";
        String vector = "Lnet/minecraft/util/math/vector/Vector3d;";
        method(entity116, "func_213303_ch", "()" + vector, entities116, "positionVec");
        method(entity116, "func_233580_cy_", "()Lnet/minecraft/util/math/BlockPos;", entities116, "blockPos");
        method(entity116, "func_226277_ct_", "()D", entities116, "posX");
        method(entity116, "func_226278_cu_", "()D", entities116, "posY");
        method(entity116, "func_226281_cx_", "()D", entities116, "posZ");
        method(entity116, "func_226280_cw_", "()D", entities116, "posYEye");
        method(entity116, "func_226283_e_", "(D)D", entities116, "posYHeight");
        method(entity116, "func_213322_ci", "()" + vector, entities116, "motion");
        method(entity116, "func_213317_d", "(" + vector + ")V", entities116, "setMotion");
        method(entity116, "func_213293_j", "(DDD)V", entities116, "setMotion");
        method(entity116, "func_226288_n_", "(DDD)V", entities116, "setRawPosition");
        method(entity116, "func_213315_a", "(Lnet/minecraft/entity/MoverType;" + vector + ")V", entities116, "move");
        method(entity116, "func_225608_bj_", "()Z", entities116, "isSneaking");
        method(entity116, "func_226272_bl_", "()Z", entities116, "sneakingCarefully");
        method(entity116, "func_226271_bk_", "()Z", entities116, "sneakingCarefully");
        method(entity116, "func_233570_aj_", "()Z", entities116, "isOnGround");
        method(entity116, "func_230245_c_", "(Z)V", entities116, "setOnGround");
        method(entity116, "func_213311_cf", "()F", entities116, "width");
        method(entity116, "func_213302_cg", "()F", entities116, "height");
        method(entity116, "func_213296_b", "(" + vector + ")D", entities116, "horizontalMag");
        method(entity116, "func_195048_a", "(" + vector + ")D", entities116, "distanceSq");
        method(entity116, "func_225503_b_", "(FF)Z", entities116, "onLivingFall");
        method("net/minecraft/entity/LivingEntity", "func_213373_a", "(DDDZ)Z", entities116, "attemptTeleport");
        method("net/minecraft/entity/LivingEntity", "func_213334_d", "(Lnet/minecraft/util/Hand;)V", entities116, "sendBreakAnimation");
        // 1.16's global sounds are 1.12's without attenuation (client).
        String sound116 = "Lnet/minecraft/client/audio/LocatableSound;";
        field("net/minecraft/client/audio/LocatableSound", "field_217862_m", "Z", SHIMS116 + "ClientShims116.isGlobal(" + sound116 + ")Z",
                SHIMS116 + "ClientShims116.setGlobal(" + sound116 + "Z)V");
        // 1.16's drawing (client): vertices from 1.16's own buffers, drawn by 1.12 with each render type's state set up by
        // Portium (RenderGlue, RenderBridge116), and tile entity renderers bound by type.
        String buffer116 = "Lnet/minecraft/client/renderer/BufferBuilder;";
        String renderType116 = "net/minecraft/client/renderer/RenderType";
        String impl = "Lnet/minecraft/client/renderer/IRenderTypeBuffer$Impl;";
        method("net/minecraft/client/renderer/WorldVertexBufferUploader", "func_181679_a", "(" + buffer116 + ")V", GLUE + "RenderGlue", "draw");
        method(renderType116, "func_228547_a_", "()V", GLUE + "RenderGlue", "setupRenderState");
        method(renderType116, "func_228549_b_", "()V", GLUE + "RenderGlue", "clearRenderState");
        method(renderType116, "func_228631_a_", "(" + buffer116 + "III)V", GLUE + "RenderGlue", "finish");
        method("net/minecraft/client/renderer/IRenderTypeBuffer", "func_228455_a_", "(" + buffer116 + ")" + impl, GLUE + "RenderGlue", "getImpl");
        method("net/minecraft/client/renderer/IRenderTypeBuffer", "func_228456_a_", "(Ljava/util/Map;" + buffer116 + ")" + impl, GLUE + "RenderGlue", "getImpl");
        method("net/minecraft/client/Minecraft", "func_228019_au_", "()Lnet/minecraft/client/renderer/RenderTypeBuffers;", GLUE + "RenderGlue", "getRenderTypeBuffers");
        method("net/minecraft/client/renderer/RenderTypeBuffers", "func_228487_b_", "()" + impl, GLUE + "RenderGlue", "getBufferSource");
        method("net/minecraft/client/renderer/RenderTypeBuffers", "func_228489_c_", "()" + impl, GLUE + "RenderGlue", "getBufferSource");
        method("net/minecraftforge/fml/client/registry/ClientRegistry", "bindTileEntityRenderer",
                "(Lnet/minecraft/tileentity/TileEntityType;Ljava/util/function/Function;)V", SHIMS116 + "ClientShims116", "bindTileEntityRenderer");
        String stack116 = "Lcom/mojang/blaze3d/matrix/MatrixStack;";
        String builder116 = "Lcom/mojang/blaze3d/vertex/IVertexBuilder;";
        String modelData = "Lnet/minecraftforge/client/model/data/IModelData;";
        String state116r = "Lnet/minecraft/block/BlockState;";
        method("net/minecraft/client/renderer/RenderTypeLookup", "canRenderInLayer", "(" + state116r + "Lnet/minecraft/client/renderer/RenderType;)Z", GLUE + "RenderGlue",
                "canRenderInLayer");
        method("net/minecraft/client/renderer/BlockModelRenderer", "renderModel", "(Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/client/renderer/model/IBakedModel;"
                + state116r + "Lnet/minecraft/util/math/BlockPos;" + stack116 + builder116 + "ZLjava/util/Random;JI" + modelData + ")Z", GLUE + "RenderGlue", "renderModel");
        method("net/minecraft/client/renderer/BlockRendererDispatcher", "renderModel", "(" + state116r + "Lnet/minecraft/util/math/BlockPos;"
                + "Lnet/minecraft/world/IBlockDisplayReader;" + stack116 + builder116 + "ZLjava/util/Random;" + modelData + ")Z", GLUE + "RenderGlue", "renderModel");
        method("net/minecraft/client/renderer/BlockRendererDispatcher", "renderBlock", "(" + state116r + stack116 + "Lnet/minecraft/client/renderer/IRenderTypeBuffer;II"
                + modelData + ")V", GLUE + "RenderGlue", "renderBlock");
        // Forge 1.16's extra models by name, and 1.16's model name for a state.
        method("net/minecraftforge/client/model/ModelLoader", "addSpecialModel", "(Lnet/minecraft/util/ResourceLocation;)V",
                "com/michaelsebero/portium/client/ModelShims116", "addSpecialModel");
        method("net/minecraft/client/renderer/BlockModelShapes", "func_209554_c", "(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/renderer/model/ModelResourceLocation;",
                "com/michaelsebero/portium/client/ModelShims116", "modelLocation");
        // 1.16's light texture: 1.12's renderer has the lightmap itself.
        method("net/minecraft/client/renderer/GameRenderer", "func_228384_l_", "()Lnet/minecraft/client/renderer/LightTexture;", GLUE + "RenderGlue", "lightTexture");
        method("net/minecraft/client/renderer/LightTexture", "func_205109_c", "()V", SHIMS116 + "ClientShims116", "enableLightmap", "(Ljava/lang/Object;)V");
        method("net/minecraft/client/renderer/LightTexture", "func_205108_b", "()V", SHIMS116 + "ClientShims116", "disableLightmap", "(Ljava/lang/Object;)V");
        // Forge 1.16's model data of a tile entity, which 1.12's tile entity lacks.
        method("net/minecraft/tileentity/TileEntity", "getModelData", "()Lnet/minecraftforge/client/model/data/IModelData;", GLUE + "ModelGlue", "tileModelData");
        // Forge 1.16's model.getModelData(world, pos, state, data) on any model: its own if it has one, else the data
        // as given (Botania's delegating models ask the 1.12 models they wrap).
        String modelDataDesc = "(Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;"
                + "Lnet/minecraftforge/client/model/data/IModelData;)Lnet/minecraftforge/client/model/data/IModelData;";
        for (String model : new String[] {"net/minecraft/client/renderer/model/IBakedModel", "net/minecraftforge/client/extensions/IForgeBakedModel"}) {
            method(model, "getModelData", modelDataDesc, GLUE + "ModelGlue", "modelData", "(Ljava/lang/Object;" + modelDataDesc.substring(1));
        }
        // 1.16's baked quads: their shading flag, and made with one.
        method("net/minecraft/client/renderer/model/BakedQuad", "func_239287_f_", "()Z", SHIMS116 + "RenderShims116", "shade");
        factory("net/minecraft/client/renderer/model/BakedQuad", "([IILnet/minecraft/util/Direction;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Z)V",
                SHIMS116 + "RenderShims116.bakedQuad([IILnet/minecraft/util/EnumFacing;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Z)"
                + "Lnet/minecraft/client/renderer/block/model/BakedQuad;");
        // Forge 1.16's vertex builder lighting (IForgeVertexBuilder), which 1.16's own builder lacks.
        for (String builder : new String[] {"com/mojang/blaze3d/vertex/IVertexBuilder", "net/minecraftforge/client/extensions/IForgeVertexBuilder"}) {
            method(builder, "applyBakedLighting", "(ILjava/nio/ByteBuffer;)I", GLUE + "RenderGlue", "applyBakedLighting",
                    "(Ljava/lang/Object;ILjava/nio/ByteBuffer;)I");
            method(builder, "applyBakedNormals", "(Lnet/minecraft/util/math/vector/Vector3f;Ljava/nio/ByteBuffer;Lnet/minecraft/util/math/vector/Matrix3f;)V",
                    GLUE + "RenderGlue", "applyBakedNormals",
                    "(Ljava/lang/Object;Lnet/minecraft/util/math/vector/Vector3f;Ljava/nio/ByteBuffer;Lnet/minecraft/util/math/vector/Matrix3f;)V");
        }
        // 1.16's sounds that tick end themselves; Forge 1.16's overlay events give the window.
        method("net/minecraft/client/audio/TickableSound", "func_239509_o_", "()V", SHIMS116 + "ClientShims116", "finishPlaying");
        for (String overlay : new String[] {"net/minecraftforge/client/event/RenderGameOverlayEvent", "net/minecraftforge/client/event/RenderGameOverlayEvent$Pre",
                "net/minecraftforge/client/event/RenderGameOverlayEvent$Post", "net/minecraftforge/client/event/RenderGameOverlayEvent$Text"}) {
            method(overlay, "getWindow", "()Lnet/minecraft/client/MainWindow;", SHIMS116 + "ClientShims116", "overlayWindow",
                    "(Lnet/minecraftforge/client/event/RenderGameOverlayEvent;)Lnet/minecraft/client/MainWindow;");
        }
        // A tile entity renderer's 1.16 render, on whichever renderer the dispatcher returned.
        method("net/minecraft/client/renderer/tileentity/TileEntityRenderer", "func_225616_a_",
                "(Lnet/minecraft/tileentity/TileEntity;FLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;II)V",
                SHIMS116 + "RenderShims116", "renderTileEntity", "(Ljava/lang/Object;Lnet/minecraft/tileentity/TileEntity;FLjava/lang/Object;Ljava/lang/Object;II)V");
        // 1.16's framebuffer clearing and resizing take whether the game runs on macOS.
        method("net/minecraft/client/shader/Framebuffer", "func_216493_b", "(Z)V", SHIMS116 + "ClientShims116", "clearFramebuffer");
        method("net/minecraft/client/shader/Framebuffer", "func_216491_a", "(IIZ)V", SHIMS116 + "ClientShims116", "resizeFramebuffer");
        // A screen's init on 1.12's screens too.
        method("net/minecraft/client/gui/screen/Screen", "func_231158_b_", "(Lnet/minecraft/client/Minecraft;II)V", SHIMS116 + "ScreenShims116", "init");
        method("net/minecraft/client/gui/screen/Screen", "func_230430_a_", "(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V", SHIMS116 + "ScreenShims116", "render",
                "(Lnet/minecraft/client/gui/GuiScreen;Ljava/lang/Object;IIF)V");
        // 1.16's camera from where 1.16 code gets it, and Forge 1.16's render events' stacks and buffers (client).
        String camera = "Lnet/minecraft/client/renderer/ActiveRenderInfo;";
        String cameraGlue = GLUE + "ActiveRenderInfo116";
        method("net/minecraft/client/renderer/GameRenderer", "func_215316_n", "()" + camera, cameraGlue, "of");
        method("net/minecraftforge/client/event/EntityViewRenderEvent", "getInfo", "()" + camera, cameraGlue, "of");
        field("net/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher", "field_217666_g", camera,
                cameraGlue + ".of(Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;)" + camera);
        field("net/minecraft/client/renderer/entity/EntityRendererManager", "field_217783_c", camera,
                cameraGlue + ".of(Lnet/minecraft/client/renderer/entity/EntityRendererManager;)" + camera);
        method("net/minecraftforge/client/event/RenderWorldLastEvent", "getMatrixStack", "()" + stack116, GLUE + "RenderGlue", "matrixStack");
        method("net/minecraftforge/client/event/RenderGameOverlayEvent", "getMatrixStack", "()" + stack116, GLUE + "RenderGlue", "matrixStack");
        method("net/minecraftforge/client/event/RenderHandEvent", "getMatrixStack", "()" + stack116, GLUE + "RenderGlue", "matrixStack");
        method("net/minecraftforge/client/event/RenderHandEvent", "getBuffers", "()Lnet/minecraft/client/renderer/IRenderTypeBuffer;", GLUE + "RenderGlue", "buffers");
        method("net/minecraftforge/client/event/RenderHandEvent", "getLight", "()I", GLUE + "RenderGlue", "light");
        // 1.16 reads blend factors' GL values by its own field names (RenderSystem); 1.12's enums name them otherwise.
        field("com/mojang/blaze3d/platform/GlStateManager$SourceFactor", "field_225655_p_", "I",
                "com/michaelsebero/portium/client/GlStateManager116.factor(Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;)I");
        field("com/mojang/blaze3d/platform/GlStateManager$DestFactor", "field_225654_o_", "I",
                "com/michaelsebero/portium/client/GlStateManager116.factor(Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)I");
        // 1.16's texture atlases and Forge 1.16's stitching (client).
        String atlas = "net/minecraft/client/renderer/texture/AtlasTexture";
        method(atlas, "func_229223_g_", "()Lnet/minecraft/util/ResourceLocation;", SHIMS116 + "ClientShims116", "atlasLocation");
        method(atlas, "func_195424_a", "(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;", SHIMS116 + "ClientShims116",
                "sprite");
        method("net/minecraftforge/client/event/TextureStitchEvent$Pre", "addSprite", "(Lnet/minecraft/util/ResourceLocation;)Z", SHIMS116 + "ClientShims116", "addSprite");
        // 1.16's item drawing (client): 1.12's item renderer under the matrix stack, and a model's quads into a builder.
        String itemRenderer = "net/minecraft/client/renderer/ItemRenderer";
        String view = "Lnet/minecraft/client/renderer/model/ItemCameraTransforms$TransformType;";
        String buffers116 = "Lnet/minecraft/client/renderer/IRenderTypeBuffer;";
        method(itemRenderer, "func_229110_a_", "(Lnet/minecraft/item/ItemStack;" + view + "II" + stack116 + buffers116 + ")V", GLUE + "RenderGlue", "renderItem");
        method(itemRenderer, "func_229111_a_", "(Lnet/minecraft/item/ItemStack;" + view + "Z" + stack116 + buffers116 + "IILnet/minecraft/client/renderer/model/IBakedModel;)V",
                GLUE + "RenderGlue", "renderItemModel");
        // 1.16's living renderers' models (1.12's main model) and model parts moving a matrix stack.
        String entityDrawing = "com/michaelsebero/portium/client/EntityRendering116";
        // A living renderer's model: 1.12's renderers answer with a 1.16 stand-in (ModelProxyGlue).
        method("net/minecraft/client/renderer/entity/LivingRenderer", "func_217764_d", "()Lnet/minecraft/client/renderer/entity/model/EntityModel;",
                GLUE + "ModelProxyGlue", "entityModel", "(Ljava/lang/Object;)Lnet/minecraft/client/renderer/entity/model/EntityModel;");
        // Writes to a living renderer's model (reads stay 1.12's field, or the mod renderer's own).
        field("net/minecraft/client/renderer/entity/LivingRenderer", "field_77045_g", "Lnet/minecraft/client/renderer/entity/model/EntityModel;", null,
                entityDrawing + ".setMainModel(Ljava/lang/Object;Ljava/lang/Object;)V");
        // 1.16 renderers (Minecraft 1.16.5's own, under mods' mobs) drawing their layers: mods' layers are 1.12's
        // layers adapted (LayerRenderer116); 1.12's own layers are drawn by 1.12.
        method("net/minecraft/client/renderer/entity/layers/LayerRenderer", "func_225628_a_",
                "(" + stack116 + "Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/entity/Entity;FFFFFF)V", entityDrawing, "renderLayer116",
                "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ILnet/minecraft/entity/Entity;FFFFFF)V");
        String arm = "(" + stack116 + "Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;)V";
        String armTarget = "(Lnet/minecraft/client/renderer/entity/PlayerRenderer;Ljava/lang/Object;Ljava/lang/Object;ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;)V";
        method("net/minecraft/client/renderer/entity/PlayerRenderer", "func_229144_a_", arm, entityDrawing, "renderRightArm", armTarget);
        method("net/minecraft/client/renderer/entity/PlayerRenderer", "func_229146_b_", arm, entityDrawing, "renderLeftArm", armTarget);
        String items = "com/michaelsebero/portium/client/ItemRendering116";
        method("net/minecraft/client/renderer/model/IBakedModel", "func_230044_c_", "()Z", items, "sideLit");
        // 1.16's light setups: directions only (1.12's lighting sets its own); its standard one is 1.12's.
        String lighting = "net/minecraft/client/renderer/RenderHelper";
        method(lighting, "func_227783_c_", "()V", items, "guiLightDirections");
        method(lighting, "func_227784_d_", "()V", items, "guiLightDirections");
        method(lighting, "func_237533_a_", "(Lnet/minecraft/util/math/vector/Matrix4f;)V", items, "worldLightDirections", "(Ljava/lang/Object;)V");
        method(lighting, "func_227781_a_", "(Lnet/minecraft/util/math/vector/Matrix4f;)V", items, "worldLightDirections", "(Ljava/lang/Object;)V");
        method(lighting, "func_227780_a_", "()V", items, "standardLighting");
        method(itemRenderer, "func_229112_a_", "(" + stack116 + builder116 + "Ljava/util/List;Lnet/minecraft/item/ItemStack;II)V", GLUE + "RenderGlue", "renderQuads");
        // Forge 1.16's model baking by name (a mod's extra item models): 1.12's model loader. 1.16's model transform
        // is taken loosely: 1.12 has rotations but not that interface.
        String bakeDesc = "(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/model/IModelTransform;)Lnet/minecraft/client/renderer/model/IBakedModel;";
        String bakeTarget = "(Ljava/lang/Object;Lnet/minecraft/util/ResourceLocation;Ljava/lang/Object;)Lnet/minecraft/client/renderer/model/IBakedModel;";
        method("net/minecraft/client/renderer/model/ModelBakery", "func_217845_a", bakeDesc, "com/michaelsebero/portium/client/ModelShims116", "bake", bakeTarget);
        method("net/minecraftforge/client/model/ModelLoader", "func_217845_a", bakeDesc, "com/michaelsebero/portium/client/ModelShims116", "bake", bakeTarget);
        String bakeWith = "(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/model/IModelTransform;Ljava/util/function/Function;)"
                + "Lnet/minecraft/client/renderer/model/IBakedModel;";
        String bakeWithTarget = "(Ljava/lang/Object;Lnet/minecraft/util/ResourceLocation;Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/client/renderer/model/IBakedModel;";
        method("net/minecraft/client/renderer/model/ModelBakery", "getBakedModel", bakeWith, "com/michaelsebero/portium/client/ModelShims116", "bake", bakeWithTarget);
        method("net/minecraftforge/client/model/ModelLoader", "getBakedModel", bakeWith, "com/michaelsebero/portium/client/ModelShims116", "bake", bakeWithTarget);
        // 1.16's block and particle formats: 1.12's fields of the same ids have other layouts.
        String vertexFormat = "Lnet/minecraft/client/renderer/vertex/VertexFormat;";
        field("net/minecraft/client/renderer/vertex/DefaultVertexFormats", "field_176600_a", vertexFormat, SHIMS116 + "RenderShims116.block116()" + vertexFormat);
        field("net/minecraft/client/renderer/vertex/DefaultVertexFormats", "field_181704_d", vertexFormat, SHIMS116 + "RenderShims116.particle116()" + vertexFormat);
        // 1.16 makes a tile entity from a tag with the state it's in.
        method("net/minecraft/tileentity/TileEntity", "func_235657_b_", "(Lnet/minecraft/block/BlockState;Lnet/minecraft/nbt/CompoundNBT;)Lnet/minecraft/tileentity/TileEntity;",
                SHIMS116 + "TileEntityShims116", "readTileEntity");
        // 1.16's ray trace results have their kind and hit as methods; 1.12's as fields.
        String rayTrace = "net/minecraft/util/math/RayTraceResult";
        method(rayTrace, "func_216346_c", "()Lnet/minecraft/util/math/RayTraceResult$Type;", SHIMS116 + "MathShims116", "getType");
        method(rayTrace, "func_216347_e", "()Lnet/minecraft/util/math/vector/Vector3d;", SHIMS116 + "MathShims116", "getHitVec");
        // A state container's base state, which for a fluid is a fluid state (1.12's containers hold block states).
        method("net/minecraft/state/StateContainer", "func_177621_b", "()Lnet/minecraft/state/StateHolder;", states116, "baseState",
                "(Ljava/lang/Object;)Ljava/lang/Object;");
        // 1.16's state codec, for block and fluid states.
        for (String holder : new String[] {"net/minecraft/state/StateHolder", "net/minecraft/block/AbstractBlock$AbstractBlockState", "net/minecraft/block/BlockState",
                "net/minecraft/fluid/FluidState"}) {
            method(holder, "func_235897_a_", "(Lcom/mojang/serialization/Codec;Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;", GLUE + "StateGlue", "codec");
        }
        // 1.12's potion entity isn't one of 1.16's item projectiles.
        method("net/minecraft/entity/projectile/PotionEntity", "func_213884_b", "(Lnet/minecraft/item/ItemStack;)V", SHIMS116 + "MiscShims116", "setPotionItem");
        // 1.16's tag factories.
        String nbt116 = SHIMS116 + "NbtShims116";
        method("net/minecraft/nbt/StringNBT", "func_229705_a_", "(Ljava/lang/String;)Lnet/minecraft/nbt/StringNBT;", nbt116, "string");
        method("net/minecraft/nbt/ByteNBT", "func_229671_a_", "(B)Lnet/minecraft/nbt/ByteNBT;", nbt116, "ofByte");
        method("net/minecraft/nbt/ByteNBT", "func_229672_a_", "(Z)Lnet/minecraft/nbt/ByteNBT;", nbt116, "ofBoolean");
        method("net/minecraft/nbt/ShortNBT", "func_229701_a_", "(S)Lnet/minecraft/nbt/ShortNBT;", nbt116, "ofShort");
        method("net/minecraft/nbt/IntNBT", "func_229692_a_", "(I)Lnet/minecraft/nbt/IntNBT;", nbt116, "ofInt");
        method("net/minecraft/nbt/LongNBT", "func_229698_a_", "(J)Lnet/minecraft/nbt/LongNBT;", nbt116, "ofLong");
        method("net/minecraft/nbt/FloatNBT", "func_229689_a_", "(F)Lnet/minecraft/nbt/FloatNBT;", nbt116, "ofFloat");
        method("net/minecraft/nbt/DoubleNBT", "func_229684_a_", "(D)Lnet/minecraft/nbt/DoubleNBT;", nbt116, "ofDouble");
        method("net/minecraft/nbt/CompoundNBT", "isEmpty", "()Z", nbt116, "isEmpty");
        // A block state saved by 1.16 (Create's ponder scenes, schematics): Minecraft's own blocks by their flattened names.
        method("net/minecraft/nbt/NBTUtil", "func_190008_d", "(Lnet/minecraft/nbt/CompoundNBT;)Lnet/minecraft/block/BlockState;", nbt116, "readBlockState");
        // 1.16's region files are closeable (close() unobfuscated); 1.12's close by an SRG id.
        method("net/minecraft/world/chunk/storage/RegionFile", "close", "()V", SHIMS116 + "WorldShims116", "closeRegion");
        // Forge 1.16's block snapshots; Forge 1.12 makes them from the world alone.
        String snapshot = "Lnet/minecraftforge/common/util/BlockSnapshot;";
        method("net/minecraftforge/common/util/BlockSnapshot", "create", "(Lnet/minecraft/util/RegistryKey;Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;)"
                + snapshot, SHIMS116 + "MiscShims116", "blockSnapshot", "(Ljava/lang/Object;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)" + snapshot);
        method("net/minecraftforge/common/util/BlockSnapshot", "create", "(Lnet/minecraft/util/RegistryKey;Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;I)"
                + snapshot, SHIMS116 + "MiscShims116", "blockSnapshot", "(Ljava/lang/Object;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;I)" + snapshot);
        // 1.16's servers and clients are task executors; 1.12's schedule tasks.
        // 1.16 shoots fireworks as projectiles.
        method("net/minecraft/entity/projectile/FireworkRocketEntity", "func_70186_c", "(DDDFF)V", SHIMS116 + "MiscShims116", "shoot",
                "(Lnet/minecraft/entity/Entity;DDDFF)V");
        // Forge 1.16's shaped recipe size limit.
        method("net/minecraft/item/crafting/ShapedRecipe", "setCraftingSize", "(II)V", SHIMS116 + "MiscShims116", "setCraftingSize");
        method("net/minecraft/util/ObjectIntIdentityMap", "func_195867_b", "(Ljava/lang/Object;)V", SHIMS116 + "MiscShims116", "addIdentity");
        // 1.14's food.
        method("net/minecraft/item/Item$Properties", "func_221540_a", "(Lnet/minecraft/item/Food;)Lnet/minecraft/item/Item$Properties;", GLUE + "ItemGlue", "food");
        method("net/minecraft/item/Item", "func_219971_r", "()Z", SHIMS116 + "ItemShims116", "isFood");
        method("net/minecraft/item/Item", "func_219967_s", "()Lnet/minecraft/item/Food;", GLUE + "ItemGlue", "foodOf");
        field("net/minecraft/item/Item", "field_219974_q", "Lnet/minecraft/item/Food;", GLUE + "ItemGlue.foodOf(Lnet/minecraft/item/Item;)Lnet/minecraft/item/Food;");
        method("net/minecraftforge/event/RegistryEvent$MissingMappings", "getMappings", "(Ljava/lang/String;)Lcom/google/common/collect/ImmutableList;",
                SHIMS116 + "RegistryShims116", "getMappings");
        // 1.16's entity types are made from a factory taking the type.
        method("net/minecraft/entity/EntityType$Builder", "func_220322_a",
                "(Lnet/minecraft/entity/EntityType$IFactory;Lnet/minecraft/entity/EntityClassification;)Lnet/minecraft/entity/EntityType$Builder;",
                GLUE + "EntityGlue", "create");
        // 1.16's dynamic registries and dimension types, onto Portium's registries and 1.12's dimension types.
        String worlds116 = SHIMS116 + "WorldShims116";
        for (String worldType : new String[] {"net/minecraft/world/World", "net/minecraft/world/IWorld", "net/minecraft/world/IWorldReader"}) {
            method(worldType, "func_241828_r", "()Lnet/minecraft/util/registry/DynamicRegistries;", worlds116, "registries");
            method(worldType, "func_230315_m_", "()Lnet/minecraft/world/DimensionType;", worlds116, "dimensionType");
        }
        method("net/minecraft/util/registry/DynamicRegistries", "func_243612_b", "(Lnet/minecraft/util/RegistryKey;)Lnet/minecraft/util/registry/MutableRegistry;",
                GLUE + "WorldGlue", "registry", "(Ljava/lang/Object;Lnet/minecraft/util/RegistryKey;)Lnet/minecraft/util/registry/MutableRegistry;");
        method("net/minecraft/util/registry/DynamicRegistries", "func_230521_a_", "(Lnet/minecraft/util/RegistryKey;)Ljava/util/Optional;",
                GLUE + "WorldGlue", "optionalRegistry", "(Ljava/lang/Object;Lnet/minecraft/util/RegistryKey;)Ljava/util/Optional;");
        // 1.16's world methods on every world interface (all 1.12's World to Portium), and its block reader's on 1.12's block access.
        String bp = "Lnet/minecraft/util/math/BlockPos;";
        String ent = "Lnet/minecraft/entity/Entity;";
        String player = "Lnet/minecraft/entity/player/PlayerEntity;";
        String aabb = "Lnet/minecraft/util/math/AxisAlignedBB;";
        String[][] worldMethods = {
                {"func_195588_v", "(" + bp + ")Z", "isBlockPresent"},
                {"func_217354_b", "(II)Z", "chunkExists"},
                // Forge 1.16's, by the name 1.12 has only in development (see ModernMappings.productionName).
                {"isAreaLoaded", "(" + bp + "I)Z", "isAreaLoaded"},
                {"func_217344_a", "(IIIIII)Z", "isAreaLoaded"},
                {"func_241211_a_", "(" + bp + "Lnet/minecraft/block/BlockState;II)Z", "setBlockState"},
                // 1.12 has it too; Portium's keeps world features in the chunks being populated.
                {"func_180501_a", "(" + bp + "Lnet/minecraft/block/BlockState;I)Z", "setBlockState"},
                {"func_217377_a", "(" + bp + "Z)Z", "removeBlock"},
                {"func_241212_a_", "(" + bp + "Z" + ent + "I)Z", "destroyBlock"},
                {"func_225521_a_", "(" + bp + "Z" + ent + ")Z", "destroyBlock"},
                {"func_217376_c", "(" + ent + ")Z", "addEntity"},
                {"func_217378_a", "(" + player + "I" + bp + "I)V", "playEvent"},
                {"func_217379_c", "(I" + bp + "I)V", "playEvent"},
                {"func_195593_d", "(" + bp + "Lnet/minecraft/block/Block;)V", "notifyNeighbors"},
                {"func_217369_A", "()Ljava/util/List;", "getPlayers"},
                {"func_217371_b", "(Ljava/util/UUID;)" + player, "getPlayerByUuid"},
                {"func_217357_a", "(Ljava/lang/Class;" + aabb + ")Ljava/util/List;", "getEntities"},
                {"func_225317_b", "(Ljava/lang/Class;" + aabb + ")Ljava/util/List;", "getEntities"},
                {"func_225316_b", "(Ljava/lang/Class;" + aabb + "Ljava/util/function/Predicate;)Ljava/util/List;", "getEntities"},
                {"func_217362_a", "(" + ent + "D)" + player, "getClosestPlayer"},
                {"func_217366_a", "(DDDDZ)" + player, "getClosestPlayer"},
                {"func_217358_a", "(DDDD)Z", "isPlayerWithin"},
                {"func_201696_r", "(" + bp + ")I", "getLight"},
                {"func_226660_f_", "(" + bp + ")Z", "canSeeSky"},
                {"func_226691_t_", "(" + bp + ")Lnet/minecraft/world/biome/Biome;", "getBiome"},
                {"func_226690_K_", "()Z", "isNightTime"},
                {"func_242414_af", "()I", "getMoonPhase"},
                {"func_242413_ae", "()F", "getMoonFactor"},
                {"func_242415_f", "(F)F", "getCelestialAngle"},
                {"func_234925_Z_", "()Z", "isDebug"},
                {"func_226664_a_", "(" + aabb + ")Z", "hasNoCollisions"},
                {"func_226669_j_", "(" + ent + ")Z", "hasNoCollisions"},
                {"func_226665_a__", "(" + ent + aabb + ")Z", "hasNoCollisions"},
                {"func_226668_i_", "(" + ent + ")Z", "checkNoEntityCollision"},
                {"func_217400_a", "(" + bp + ent + ")Z", "isTopSolid"},
                {"func_217381_Z", "()Lnet/minecraft/profiler/IProfiler;", "getProfiler"},
                {"func_225522_c_", "(II)Lnet/minecraft/world/IBlockReader;", "getBlockReader"},
                {"func_217375_a", "(" + bp + "Ljava/util/function/Predicate;)Z", "hasBlockState"},
                {"func_225319_b", "(" + bp + "Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;)V", "markForRender"},
                {"func_217384_a", "(" + player + ent + "Lnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V", "playMovingSound"},
        };
        String[] worldTypes = {"net/minecraft/world/World", "net/minecraft/world/IWorld", "net/minecraft/world/IWorldReader", "net/minecraft/world/IWorldWriter",
                "net/minecraft/world/IEntityReader", "net/minecraft/world/ICollisionReader", "net/minecraft/world/IBiomeReader",
                "net/minecraft/world/IDayTimeReader", "net/minecraft/world/IServerWorld", "net/minecraft/world/ISeedReader", "net/minecraft/world/server/ServerWorld"};
        for (String worldType : worldTypes) {
            // The stand-ins take a World: named so, since a server or client world may be the class called on.
            for (String[] each : worldMethods) method(worldType, each[0], each[1], worlds116, each[2], "(Lnet/minecraft/world/World;" + each[1].substring(1));
            // Typed loosely where 1.16's argument is a class of its own (heightmap types, explosion modes).
            method(worldType, "func_201676_a", "(Lnet/minecraft/world/gen/Heightmap$Type;II)I", worlds116, "getHeight", "(Lnet/minecraft/world/World;Ljava/lang/Object;II)I");
            method(worldType, "func_205770_a", "(Lnet/minecraft/world/gen/Heightmap$Type;" + bp + ")" + bp, worlds116, "getHeight",
                    "(Lnet/minecraft/world/World;Ljava/lang/Object;" + bp + ")" + bp);
            method(worldType, "func_217385_a", "(" + ent + "DDDFLnet/minecraft/world/Explosion$Mode;)Lnet/minecraft/world/Explosion;", worlds116, "createExplosion",
                    "(Lnet/minecraft/world/World;" + ent + "DDDFLjava/lang/Object;)Lnet/minecraft/world/Explosion;");
            method(worldType, "func_217398_a", "(" + ent + "DDDFZLnet/minecraft/world/Explosion$Mode;)Lnet/minecraft/world/Explosion;", worlds116, "createExplosion",
                    "(Lnet/minecraft/world/World;" + ent + "DDDFZLjava/lang/Object;)Lnet/minecraft/world/Explosion;");
            method(worldType, "func_217353_a", "(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/IChunk;", worlds116, "getChunk",
                    "(Lnet/minecraft/world/World;IILjava/lang/Object;Z)Lnet/minecraft/world/chunk/Chunk;");
            // Static in 1.16 (so no world on the stack).
            method(worldType, "func_217405_b", "(I)Z", worlds116, "isYOutOfBounds", "(I)Z");
            // 1.13's, which 1.16 declares on more interfaces.
            method(worldType, "func_201670_d", "()Z", SHIMS + "MiscShims", "isRemote", "(Lnet/minecraft/world/World;)Z");
            method(worldType, "func_201674_k", "()Ljava/util/Random;", SHIMS + "MiscShims", "getRandom", "(Lnet/minecraft/world/World;)Ljava/util/Random;");
        }
        // 1.16's block ray trace, a default of its block reader: on every world and reader (Create's Ponder picks blocks with it).
        String[] rayTracers = new String[worldTypes.length + 2];
        System.arraycopy(worldTypes, 0, rayTracers, 0, worldTypes.length);
        rayTracers[worldTypes.length] = "net/minecraft/world/IBlockReader";
        rayTracers[worldTypes.length + 1] = "net/minecraft/world/IBlockDisplayReader";
        for (String reader : rayTracers) {
            method(reader, "func_217299_a", "(Lnet/minecraft/util/math/RayTraceContext;)Lnet/minecraft/util/math/BlockRayTraceResult;", GLUE + "RayTraceGlue",
                    "rayTraceBlocks", "(Ljava/lang/Object;Lnet/minecraft/util/math/RayTraceContext;)Lnet/minecraft/util/math/BlockRayTraceResult;");
        }
        for (String reader : new String[] {"net/minecraft/world/IBlockReader", "net/minecraft/world/IBlockDisplayReader"}) {
            method(reader, "func_201572_C", "()I", worlds116, "getMaxLightLevel");
            method(reader, "func_217301_I", "()I", worlds116, "getBuildHeight");
            method(reader, "func_217298_h", "(" + bp + ")I", worlds116, "getLightValue");
            method(reader, "func_226658_a_", "(Lnet/minecraft/world/LightType;" + bp + ")I", worlds116, "getLightFor");
        }
        method("net/minecraft/util/Direction$Axis", "func_196052_a", "(III)I", SHIMS116 + "MathShims116", "coordinate");
        method("net/minecraft/util/Direction$Axis", "func_196051_a", "(DDD)D", SHIMS116 + "MathShims116", "coordinate");
        // Client: 1.16's reload listeners and render layers.
        method("net/minecraft/resources/IReloadableResourceManager", "func_219534_a", "(Lnet/minecraft/resources/IFutureReloadListener;)V",
                SHIMS116 + "ResourceShims116", "addReloadListener", "(Ljava/lang/Object;Ljava/lang/Object;)V");
        String renderType = "Lnet/minecraft/client/renderer/RenderType;";
        method("net/minecraft/client/renderer/RenderTypeLookup", "setRenderLayer", "(Lnet/minecraft/block/Block;" + renderType + ")V", GLUE + "RenderGlue", "setRenderLayer");
        // 1.16's GUI drawing (AbstractGui is 1.12's Gui here), with the matrix stack taken loosely.
        guiDrawing();
        fontDrawing();
        // Forge 1.16's listener list of an event (1.12's events have their own kind): an empty one of 1.16's.
        method("net/minecraftforge/eventbus/api/Event", "getListenerList", "()Lnet/minecraftforge/eventbus/ListenerList;", GLUE + "ModernEventBus",
                "listenerList");
        // Forge 1.16 says which render type is drawn; 1.12 Forge which block layer.
        method("net/minecraftforge/client/ForgeHooksClient", "setRenderLayer", "(" + renderType + ")V", GLUE + "RenderGlue", "setForgeRenderLayer");
        method("net/minecraftforge/client/MinecraftForgeClient", "getRenderLayer", "()" + renderType, GLUE + "RenderGlue", "getForgeRenderLayer");
        method("net/minecraft/client/renderer/RenderTypeLookup", "setRenderLayer", "(Lnet/minecraft/block/Block;Ljava/util/function/Predicate;)V", GLUE + "RenderGlue",
                "setRenderLayer");
        method("net/minecraft/client/renderer/RenderTypeLookup", "setRenderLayer", "(Lnet/minecraft/fluid/Fluid;" + renderType + ")V", GLUE + "RenderGlue",
                "setRenderLayer", "(Ljava/lang/Object;" + renderType + ")V");
        // 1.16's task executors: the client, the server, and their base classes.
        for (String executor : new String[] {"net/minecraft/util/concurrent/ThreadTaskExecutor", "net/minecraft/util/concurrent/RecursiveEventLoop",
                "net/minecraft/client/Minecraft", "net/minecraft/server/MinecraftServer"}) {
            method(executor, "func_212871_a_", "(Ljava/lang/Runnable;)V", worlds116, "enqueue", "(Ljava/lang/Object;Ljava/lang/Runnable;)V");
            method(executor, "func_213165_a", "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;", worlds116, "deferTask",
                    "(Ljava/lang/Object;Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;");
            method(executor, "func_222817_e", "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;", worlds116, "deferTask",
                    "(Ljava/lang/Object;Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;");
            method(executor, "func_213169_a", "(Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;", worlds116, "supplyAsync",
                    "(Ljava/lang/Object;Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;");
            method(executor, "func_213162_bc", "()Z", worlds116, "isOnExecutionThread", "(Ljava/lang/Object;)Z");
            method(executor, "execute", "(Ljava/lang/Runnable;)V", worlds116, "enqueue", "(Ljava/lang/Object;Ljava/lang/Runnable;)V");
            method(executor, "func_213142_bd", "()Ljava/lang/String;", worlds116, "executorName", "(Ljava/lang/Object;)Ljava/lang/String;");
        }
        // 1.16's screen fields that 1.12's screen has too.
        String screen = "net/minecraft/client/gui/screen/Screen";
        String screens = SHIMS116 + "ScreenShims116.";
        String[][] screenFields = {{"field_230706_i_", "Lnet/minecraft/client/Minecraft;", "minecraft"}, {"field_230708_k_", "I", "width"},
                {"field_230709_l_", "I", "height"}, {"field_230712_o_", "Lnet/minecraft/client/gui/FontRenderer;", "font"},
                {"field_230707_j_", "Lnet/minecraft/client/renderer/ItemRenderer;", "itemRenderer"}};
        for (String[] each : screenFields) {
            field(screen, each[0], each[1], screens + each[2] + "(L" + screen + ";)" + each[1], screens + each[2] + "(L" + screen + ";" + each[1] + ")V");
        }
        // Forge 1.16's player events name their player getPlayer.
        method("net/minecraftforge/event/entity/player/PlayerEvent", "getPlayer", "()Lnet/minecraft/entity/player/PlayerEntity;", SHIMS116 + "MiscShims116", "eventPlayer");
        // Forge 1.16's tooltip lines are text components; 1.12's event holds text.
        method("net/minecraftforge/event/entity/player/ItemTooltipEvent", "getToolTip", "()Ljava/util/List;", SHIMS + "TooltipList", "lines");
        method("net/minecraftforge/event/entity/player/ItemTooltipEvent", "getPlayer", "()Lnet/minecraft/entity/player/PlayerEntity;", SHIMS116 + "MiscShims116",
                "tooltipPlayer");
        // Mojang's language interface, as 1.12's language.
        method("com/mojang/bridge/game/Language", "getCode", "()Ljava/lang/String;", SHIMS116 + "ClientShims116", "languageCode");
        method("com/mojang/bridge/game/Language", "getName", "()Ljava/lang/String;", SHIMS116 + "ClientShims116", "languageName");
        // Forge 1.16's name for an item's special renderer.
        method("net/minecraft/item/Item", "getItemStackTileEntityRenderer", "()Lnet/minecraft/client/renderer/tileentity/ItemStackTileEntityRenderer;",
                SHIMS116 + "ClientShims116", "itemRenderer");
        // 1.16 screens' static key helpers, called through Screen (1.12's screen, to Portium).
        String screenStatics = "net/minecraft/client/gui/screen/Screen";
        method(screenStatics, "func_231173_s_", "()Z", SHIMS116 + "ClientShims116", "shiftDown");
        method(screenStatics, "func_231172_r_", "()Z", SHIMS116 + "ClientShims116", "controlDown");
        method(screenStatics, "func_231174_t_", "()Z", SHIMS116 + "ClientShims116", "altDown");
        // 1.16's formattable text, on every text class.
        String text = "Lnet/minecraft/util/text/IFormattableTextComponent;";
        String texts = SHIMS116 + "TextShims116";
        // (Declared on the interfaces: calls through the text classes reach them as subtypes, with the receiver as text.)
        for (String textClass : new String[] {"net/minecraft/util/text/IFormattableTextComponent", "net/minecraft/util/text/ITextComponent"}) {
            method(textClass, "func_230530_a_", "(Lnet/minecraft/util/text/Style;)" + text, texts, "setStyle");
            method(textClass, "func_240702_b_", "(Ljava/lang/String;)" + text, texts, "appendString");
            method(textClass, "func_230529_a_", "(Lnet/minecraft/util/text/ITextComponent;)" + text, texts, "appendSibling");
            method(textClass, "func_240700_a_", "(Ljava/util/function/UnaryOperator;)" + text, texts, "modifyStyle");
            method(textClass, "func_240703_c_", "(Lnet/minecraft/util/text/Style;)" + text, texts, "mergeStyle");
            method(textClass, "func_240701_a_", "([Lnet/minecraft/util/text/TextFormatting;)" + text, texts, "mergeStyle");
            method(textClass, "func_240699_a_", "(Lnet/minecraft/util/text/TextFormatting;)" + text, texts, "mergeStyle");
            method(textClass, "func_230531_f_", "()" + text, texts, "copy");
            method(textClass, "func_230532_e_", "()" + text, texts, "copy");
            method(textClass, "func_212636_a", "(I)Ljava/lang/String;", texts, "truncated");
        }
        // Forge 1.16's capability invalidation listeners (see CapabilityShims116).
        method("net/minecraftforge/event/AttachCapabilitiesEvent", "addListener", "(Ljava/lang/Runnable;)V", SHIMS116 + "CapabilityShims116", "addListener");
        // 1.16's entity poses, from 1.12's state.
        method("net/minecraft/entity/Entity", "func_213283_Z", "()Lnet/minecraft/entity/Pose;", GLUE + "EntityGlue", "pose", "(Ljava/lang/Object;)Lnet/minecraft/entity/Pose;");
        // Forge 1.16's entity data.
        method("net/minecraft/entity/Entity", "getPersistentData", "()Lnet/minecraft/nbt/CompoundNBT;", worlds116, "persistentData");
        // 1.16's command manager, for its dispatcher (see the glue's ModernCommands).
        method("net/minecraft/server/MinecraftServer", "func_195571_aL", "()Lnet/minecraft/command/Commands;", GLUE + "ModernCommands", "commandManager",
                "(Lnet/minecraft/server/MinecraftServer;)Ljava/lang/Object;");
        method("net/minecraft/command/Commands", "func_197054_a", "()Lcom/mojang/brigadier/CommandDispatcher;", GLUE + "ModernCommands", "dispatcher",
                "(Ljava/lang/Object;)Lcom/mojang/brigadier/CommandDispatcher;");
        method("net/minecraft/command/Commands", "func_197059_a", "(Lnet/minecraft/command/CommandSource;Ljava/lang/String;)I", SHIMS116 + "MiscShims116",
                "handleCommand", "(Ljava/lang/Object;Lnet/minecraft/command/CommandSource;Ljava/lang/String;)I");
        // Forge 1.16's registry objects listen for object holder refreshes.
        method("net/minecraftforge/registries/ObjectHolderRegistry", "addHandler", "(Ljava/util/function/Consumer;)V", SHIMS116 + "RegistryShims116", "addHandler");
        // 1.16 enum constants and helpers 1.12's enums lack.
        String misc116 = SHIMS116 + "MiscShims116.";
        String result = "Lnet/minecraft/util/ActionResultType;";
        field("net/minecraft/util/ActionResultType", "CONSUME", result, misc116 + "consume()" + result);
        method("net/minecraft/util/ActionResultType", "func_233537_a_", "(Z)" + result, SHIMS116 + "MiscShims116", "sidedSuccess");
        method("net/minecraft/util/ActionResultType", "func_226246_a_", "()Z", SHIMS116 + "MiscShims116", "isSuccessOrConsume");
        method("net/minecraft/util/ActionResultType", "func_226247_b_", "()Z", SHIMS116 + "MiscShims116", "isSuccess");
        String use = "Lnet/minecraft/item/UseAction;";
        field("net/minecraft/item/UseAction", "SPEAR", use, misc116 + "spear()" + use);
        field("net/minecraft/item/UseAction", "CROSSBOW", use, misc116 + "crossbow()" + use);
        // Forge 1.16's block harvest tool, by its tool type.
        method("net/minecraft/block/AbstractBlock$Properties", "harvestTool", "(Lnet/minecraftforge/common/ToolType;)Lnet/minecraft/block/AbstractBlock$Properties;",
                GLUE + "ItemGlue", "harvestTool");
        // Forge 1.16's optional tags, which it adds to Minecraft's tag classes.
        String optionalTag = "Lnet/minecraftforge/common/Tags$IOptionalNamedTag;";
        for (String[] kind : new String[][] {{"BlockTags", "blocks"}, {"ItemTags", "items"}, {"FluidTags", "fluids"}}) {
            method("net/minecraft/tags/" + kind[0], "createOptional", "(Lnet/minecraft/util/ResourceLocation;)" + optionalTag, GLUE + "TagGlue", kind[1]);
            method("net/minecraft/tags/" + kind[0], "createOptional", "(Lnet/minecraft/util/ResourceLocation;Ljava/util/Set;)" + optionalTag, GLUE + "TagGlue", kind[1]);
        }
        // Forge 1.16's colour of a dye item, by its class or its dye tag.
        method("net/minecraft/item/DyeColor", "getColor", "(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/DyeColor;",
                "com/michaelsebero/portium/item/BannerPatterns116", "dyeColor", "(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/DyeColor;");
        method("net/minecraft/item/DyeColor", "getTag", "()" + optionalTag, GLUE + "TagGlue", "dyeTag");
        // 1.16's named tags, read from Portium's tags (see TagGlue); vanilla's tag classes make theirs this way too.
        String namedTag = "Lnet/minecraft/tags/ITag$INamedTag;";
        method("net/minecraft/tags/ItemTags", "func_199901_a", "(Ljava/lang/String;)" + namedTag, GLUE + "TagGlue", "itemWrapper");
        method("net/minecraft/tags/BlockTags", "func_199894_a", "(Ljava/lang/String;)" + namedTag, GLUE + "TagGlue", "blockWrapper");
        method("net/minecraft/tags/FluidTags", "func_206956_a", "(Ljava/lang/String;)" + namedTag, GLUE + "TagGlue", "fluidWrapper");
        method("net/minecraft/tags/FluidTags", "func_241280_c_", "()Ljava/util/List;", GLUE + "TagGlue", "allFluidTags");
        String tagCollection = "()Lnet/minecraft/tags/ITagCollection;";
        method("net/minecraft/tags/BlockTags", "func_199896_a", tagCollection, GLUE + "TagGlue", "blockCollection");
        method("net/minecraft/tags/ItemTags", "func_199903_a", tagCollection, GLUE + "TagGlue", "itemCollection");
        method("net/minecraft/tags/EntityTypeTags", "func_219762_a", tagCollection, GLUE + "TagGlue", "entityCollection");
        method("net/minecraft/resources/DataPackRegistries", "func_244358_d", "()Lnet/minecraft/tags/ITagCollectionSupplier;", GLUE + "TagGlue", "collections",
                "(Ljava/lang/Object;)Lnet/minecraft/tags/ITagCollectionSupplier;");
        method("net/minecraft/tags/TagCollectionManager", "func_242178_a", "()Lnet/minecraft/tags/ITagCollectionSupplier;", GLUE + "TagGlue", "collections");
        method("net/minecraft/tags/EntityTypeTags", "func_232896_a_", "(Ljava/lang/String;)" + namedTag, GLUE + "TagGlue", "entityWrapper");
        // Forge 1.16's bucket fluid.
        method("net/minecraft/item/BucketItem", "getFluid", "()Lnet/minecraft/fluid/Fluid;", GLUE + "FluidGlue", "getFluid");
        method("net/minecraft/block/FlowingFluidBlock", "getFluid", "()Lnet/minecraft/fluid/FlowingFluid;", GLUE + "FluidGlue", "getFluid");
        // Forge 1.16's fluid attributes (Create's tanks read a fluid's).
        for (String fluidClass : new String[] {"net/minecraft/fluid/Fluid", "net/minecraft/fluid/FlowingFluid"}) {
            method(fluidClass, "getAttributes", "()Lnet/minecraftforge/fluids/FluidAttributes;", GLUE + "FluidGlue", "getAttributes",
                    "(Lnet/minecraft/fluid/Fluid;)Lnet/minecraftforge/fluids/FluidAttributes;");
        }
        // Forge 1.16's creative tab index helper.
        method("net/minecraft/item/ItemGroup", "getGroupCountSafe", "()I", SHIMS116 + "ItemShims116", "groupCount");
        method("net/minecraft/item/DyeColor", "func_196059_a", "()I", SHIMS + "MiscShims", "dyeId");
        // 1.13 names dyes alike everywhere (light_gray, light_blue); 1.12 has silver, and camel case in its keys.
        method("net/minecraft/item/DyeColor", "func_176762_d", "()Ljava/lang/String;", SHIMS + "MiscShims", "dyeName");
        method("net/minecraft/item/DyeColor", "func_176610_l", "()Ljava/lang/String;", SHIMS + "MiscShims", "dyeName");
        method("net/minecraft/entity/LivingEntity", "func_195064_c", "(Lnet/minecraft/potion/EffectInstance;)Z", SHIMS + "MiscShims", "addPotionEffect");
        method("net/minecraft/server/MinecraftServer", "func_71218_a", "(Lnet/minecraft/world/dimension/DimensionType;)Lnet/minecraft/world/server/ServerWorld;",
                SHIMS + "MiscShims", "getWorld");
        method("net/minecraft/util/DamageSource", "func_199683_a", "()Lnet/minecraft/util/DamageSource;", SHIMS + "MiscShims", "netherBedExplosion");
        method("net/minecraft/world/World", "func_211529_a", "(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/DamageSource;DDDFZZ)Lnet/minecraft/world/Explosion;",
                SHIMS + "MiscShims", "createExplosion");
        method("net/minecraft/world/IWorld", "func_201670_d", "()Z", SHIMS + "MiscShims", "isRemote");
        field("net/minecraft/stats/Stats", "field_188065_ae", "Lnet/minecraft/stats/StatType;", SHIMS + "MiscShims.blockMined()Lnet/minecraft/stats/StatType;");
        // ITEM_USED shares its id with 1.12's array of use stats; 1.16's own Stats class can't start here.
        field("net/minecraft/stats/Stats", "field_75929_E", "Lnet/minecraft/stats/StatType;", SHIMS + "MiscShims.itemUsed()Lnet/minecraft/stats/StatType;");

        // Fluids: 1.12's liquids, Portium's waterlogging and 1.13 blocks, read as 1.13 fluid states.
        String fluids = SHIMS + "FluidShims";
        String fluidState = "Lnet/minecraft/fluid/FluidState;";
        method("net/minecraft/world/IBlockReader", "func_204610_c", "(" + pos + ")" + fluidState, fluids, "getFluidState");
        method("net/minecraft/block/BlockState", "func_204520_s", "()" + fluidState, fluids, "getFluidState");
        method("net/minecraft/block/Block", "func_204507_t", "(" + state + ")" + fluidState, fluids, "getFluidState");
        method("net/minecraft/world/IWorld", "func_205219_F_", "()Lnet/minecraft/world/ITickList;", fluids, "getPendingFluidTicks");
        method("net/minecraft/world/IWorld", "func_205220_G_", "()Lnet/minecraft/world/ITickList;", fluids, "getPendingBlockTicks");
        for (String ticking : new String[] {"net/minecraft/world/World", "net/minecraft/world/server/ServerWorld"}) {
            method(ticking, "func_205219_F_", "()Lnet/minecraft/world/ITickList;", fluids, "getPendingFluidTicks", "(Lnet/minecraft/world/World;)Lnet/minecraft/world/ITickList;");
            method(ticking, "func_205220_G_", "()Lnet/minecraft/world/ITickList;", fluids, "getPendingBlockTicks", "(Lnet/minecraft/world/World;)Lnet/minecraft/world/ITickList;");
            method(ticking, "func_205219_F_", "()Lnet/minecraft/world/server/ServerTickList;", fluids, "getPendingFluidTicks", "(Lnet/minecraft/world/World;)Lnet/minecraft/world/ITickList;");
            method(ticking, "func_205220_G_", "()Lnet/minecraft/world/server/ServerTickList;", fluids, "getPendingBlockTicks", "(Lnet/minecraft/world/World;)Lnet/minecraft/world/ITickList;");
        }

        String reflection = "net/minecraftforge/fml/common/ObfuscationReflectionHelper";
        method(reflection, "remapName", "(Lcpw/mods/modlauncher/api/INameMappingService$Domain;Ljava/lang/String;)Ljava/lang/String;",
                SHIMS + "ReflectionShims", "remapName", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;");
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
        method(provider, "getCapability", "(" + capability + "Lnet/minecraft/util/Direction;)" + optional, GLUE + "CapabilityGlue", "getCapability");
        method(provider, "getCapability", "(" + capability + ")" + optional, GLUE + "CapabilityGlue", "getCapability");
        method("net/minecraftforge/items/wrapper/SidedInvWrapper", "create", "(Lnet/minecraft/inventory/ISidedInventory;[Lnet/minecraft/util/Direction;)[" + optional,
                GLUE + "CapabilityGlue", "sidedWrappers");
        method("net/minecraftforge/common/capabilities/Capability", "orEmpty", "(" + capability + optional + ")" + optional, GLUE + "CapabilityGlue", "orEmpty");

        // Forge 1.13's getters on events Forge 1.12 gives public fields.
        String events = SHIMS + "EventShims";
        String playerEvent = "net/minecraftforge/fml/common/gameevent/PlayerEvent";
        String dimensionType = "Lnet/minecraft/world/dimension/DimensionType;";
        method(playerEvent, "getPlayer", "()Lnet/minecraft/entity/player/PlayerEntity;", events, "getPlayer");
        method(playerEvent + "$ItemCraftedEvent", "getCrafting", "()Lnet/minecraft/item/ItemStack;", events, "getCrafting");
        method(playerEvent + "$ItemCraftedEvent", "getInventory", "()Lnet/minecraft/inventory/IInventory;", events, "getInventory");
        method(playerEvent + "$ItemSmeltedEvent", "getSmelting", "()Lnet/minecraft/item/ItemStack;", events, "getSmelting");
        method(playerEvent + "$PlayerChangedDimensionEvent", "getFrom", "()" + dimensionType, events, "getFrom");
        method(playerEvent + "$PlayerChangedDimensionEvent", "getTo", "()" + dimensionType, events, "getTo");

        // Dimensions are types in 1.13 (registry entries in Forge 1.13), ids in 1.12.
        String misc = SHIMS + "MiscShims";
        method("net/minecraft/world/dimension/DimensionType", "getRegistryName", "()Lnet/minecraft/util/ResourceLocation;", misc, "dimensionName");
        method("net/minecraft/world/dimension/DimensionType", "func_212678_a", "(" + dimensionType + ")Lnet/minecraft/util/ResourceLocation;", misc, "dimensionName");
        method("net/minecraftforge/common/DimensionManager", "getWorld", "(Lnet/minecraft/server/MinecraftServer;" + dimensionType + "ZZ)Lnet/minecraft/world/server/ServerWorld;",
                misc, "getWorld");
        field("net/minecraft/entity/Entity", "field_71093_bK", dimensionType, misc + ".dimension(Lnet/minecraft/entity/Entity;)" + dimensionType,
                misc + ".setDimension(Lnet/minecraft/entity/Entity;" + dimensionType + ")V");

        // What 1.13 asks of its world interfaces.
        method("net/minecraft/world/World", "func_201674_k", "()Ljava/util/Random;", misc, "getRandom");
        method("net/minecraft/world/IWorld", "func_201674_k", "()Ljava/util/Random;", misc, "getRandom");
        method("net/minecraft/world/IWorld", "func_201672_e", "()Lnet/minecraft/world/World;", misc, "getWorld");
        // A world's dimension key, which 1.16 keeps in the field 1.12's world keeps its provider in.
        for (String worldType : new String[] {"net/minecraft/world/World", "net/minecraft/world/server/ServerWorld", "net/minecraft/client/world/ClientWorld"}) {
            method(worldType, "func_234923_W_", "()Lnet/minecraft/util/RegistryKey;", GLUE + "WorldGlue", "dimensionKey",
                    "(Lnet/minecraft/world/World;)Lnet/minecraft/util/RegistryKey;");
            field(worldType, "field_73011_w", "Lnet/minecraft/util/RegistryKey;", GLUE + "WorldGlue.dimensionKey(Lnet/minecraft/world/World;)Lnet/minecraft/util/RegistryKey;");
        }
        // 1.16's world constructor, for a mod's world (World116 takes the dimension key loosely).
        constructor("net/minecraft/world/World", "(Lnet/minecraft/world/storage/ISpawnWorldInfo;Lnet/minecraft/util/RegistryKey;Lnet/minecraft/world/DimensionType;"
                + "Ljava/util/function/Supplier;ZZJ)V", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/util/function/Supplier;ZZJ)V",
                arg(0), arg(1), arg(2), arg(3), arg(4), arg(5), arg(6));
        constructor("net/minecraft/inventory/container/Container", "(Lnet/minecraft/inventory/container/ContainerType;I)V", "(Ljava/lang/Object;I)V",
                arg(0), arg(1));
        // 1.16's armor material is its own class here (see ArmorItem116).
        constructor("net/minecraft/item/ArmorItem", "(Lnet/minecraft/item/IArmorMaterial;Lnet/minecraft/inventory/EquipmentSlotType;Lnet/minecraft/item/Item$Properties;)V",
                "(Ljava/lang/Object;Lnet/minecraft/inventory/EntityEquipmentSlot;Lcom/michaelsebero/portium/modern/mc/item/Item$Properties;)V", arg(0), arg(1), arg(2));
        method("net/minecraft/inventory/container/Container", "func_216957_a", "()Lnet/minecraft/inventory/container/ContainerType;", GLUE + "ContainerGlue",
                "getType");
        // 1.16's villagers' points of interest (moving a block clears its one); 1.12 has none.
        method("net/minecraft/village/PointOfInterestType", "func_221047_b", "(Lnet/minecraft/block/BlockState;)Ljava/util/Optional;",
                SHIMS116 + "WorldShims116", "pointOfInterest");
        method("net/minecraft/world/World", "markAndNotifyBlock", "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/chunk/Chunk;"
                + "Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;II)V", SHIMS116 + "WorldShims116", "markAndNotifyBlock");
        // 1.16's server-world interfaces give the server world (which they are here).
        for (String serverWorld : new String[] {"net/minecraft/world/IServerWorld", "net/minecraft/world/ISeedReader"}) {
            method(serverWorld, "func_201672_e", "()Lnet/minecraft/world/server/ServerWorld;", SHIMS116 + "WorldShims116", "serverWorld");
        }
        method("net/minecraft/world/IWorldReader", "func_201675_m", "()Lnet/minecraft/world/dimension/Dimension;", misc, "getDimension");
        method("net/minecraft/world/IWorldReader", "func_201670_d", "()Z", misc, "isRemote");

        // Entities: names as text, the shooter of an arrow by UUID.
        method("net/minecraft/entity/Entity", "func_200200_C_", "()Lnet/minecraft/util/text/ITextComponent;", misc, "getName");
        method("net/minecraft/entity/Entity", "func_200201_e", "()Lnet/minecraft/util/text/ITextComponent;", misc, "getCustomName");
        method("net/minecraft/entity/Entity", "func_200203_b", "(Lnet/minecraft/util/text/ITextComponent;)V", misc, "setCustomName");
        method("net/minecraft/util/INameable", "func_200200_C_", "()Lnet/minecraft/util/text/ITextComponent;", misc, "nameOf");
        method("net/minecraft/util/INameable", "func_200201_e", "()Lnet/minecraft/util/text/ITextComponent;", misc, "customNameOf");
        field("net/minecraft/entity/projectile/AbstractArrowEntity", "field_70250_c", "Ljava/util/UUID;",
                misc + ".shootingEntity(Lnet/minecraft/entity/projectile/AbstractArrowEntity;)Ljava/util/UUID;");

        method("net/minecraft/entity/projectile/AbstractArrowEntity", "func_212360_k", "()Lnet/minecraft/entity/Entity;", misc, "getShooter");
        method("net/minecraft/inventory/container/Container", "func_195929_a", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z", misc, "areItemsAndTagsEqual");
        method("net/minecraft/block/ITileEntityProvider", "func_196283_a_", "(Lnet/minecraft/world/IBlockReader;)Lnet/minecraft/tileentity/TileEntity;",
                BLOCKS, "createNewTileEntity");

        method("net/minecraft/entity/Entity", "func_199702_a", "(Lnet/minecraft/util/IItemProvider;I)Lnet/minecraft/entity/item/ItemEntity;", misc116Class(), "dropItem");
        // Forge 1.16's extensions' "this" (also as method references, as Quark's block::getBlock suppliers).
        method("net/minecraftforge/common/extensions/IForgeBlock", "getBlock", "()Lnet/minecraft/block/Block;", misc116Class(), "selfBlock",
                "(Ljava/lang/Object;)Lnet/minecraft/block/Block;");
        method("net/minecraftforge/common/extensions/IForgeItem", "getItem", "()Lnet/minecraft/item/Item;", misc116Class(), "selfItem",
                "(Ljava/lang/Object;)Lnet/minecraft/item/Item;");
        // 1.16's profiler sections, dye colours by id and name, and the camera's point of view.
        for (String profiler : new String[] {"net/minecraft/profiler/IProfiler", "net/minecraft/profiler/Profiler"}) {
            method(profiler, "func_219895_b", "(Ljava/lang/String;)V", misc116Class(), "endStartSection", "(Lnet/minecraft/profiler/Profiler;Ljava/lang/String;)V");
            method(profiler, "func_230035_c_", "(Ljava/util/function/Supplier;)V", misc116Class(), "endStartSection",
                    "(Lnet/minecraft/profiler/Profiler;Ljava/util/function/Supplier;)V");
        }
        method("net/minecraft/item/DyeColor", "func_196056_a", "(I)Lnet/minecraft/item/DyeColor;", misc116Class(), "dyeById");
        method("net/minecraft/item/DyeColor", "func_204271_a", "(Ljava/lang/String;Lnet/minecraft/item/DyeColor;)Lnet/minecraft/item/DyeColor;", misc116Class(), "dyeByName");
        method("net/minecraft/client/GameSettings", "func_243230_g", "()Lnet/minecraft/client/settings/PointOfView;", GLUE + "RenderGlue", "pointOfView",
                "(Ljava/lang/Object;)Lnet/minecraft/client/settings/PointOfView;");
        method("net/minecraft/client/GameSettings", "func_243229_a", "(Lnet/minecraft/client/settings/PointOfView;)V", GLUE + "RenderGlue", "setPointOfView",
                "(Ljava/lang/Object;Lnet/minecraft/client/settings/PointOfView;)V");
        // 1.16's server chunk provider is 1.12's (ChunkProviderServer): its 1.16 calls (ChunkProviderShims116). Calls on
        // AbstractChunkProvider stay as they are: mods (Create) make their own.
        String providers = SHIMS116 + "ChunkProviderShims116";
        String chunkProvider = "net/minecraft/world/server/ServerChunkProvider";
        method(chunkProvider, "func_217205_a", "(IIZ)Lnet/minecraft/world/chunk/Chunk;", providers, "getChunk", "(Ljava/lang/Object;IIZ)Lnet/minecraft/world/chunk/Chunk;");
        method(chunkProvider, "func_225313_a", "(II)Lnet/minecraft/world/chunk/Chunk;", providers, "getChunkNow", "(Ljava/lang/Object;II)Lnet/minecraft/world/chunk/Chunk;");
        method(chunkProvider, "func_73149_a", "(II)Z", providers, "chunkExists", "(Ljava/lang/Object;II)Z");
        method(chunkProvider, "func_217204_a", "(Lnet/minecraft/entity/Entity;)Z", providers, "entityChunkLoaded", "(Ljava/lang/Object;Lnet/minecraft/entity/Entity;)Z");
        method(chunkProvider, "func_222865_a", "(Lnet/minecraft/util/math/ChunkPos;)Z", providers, "chunkLoaded", "(Ljava/lang/Object;Lnet/minecraft/util/math/ChunkPos;)Z");
        method(chunkProvider, "func_222866_a", "(Lnet/minecraft/util/math/BlockPos;)Z", providers, "canTick", "(Ljava/lang/Object;Lnet/minecraft/util/math/BlockPos;)Z");
        field(chunkProvider, "field_217237_a", "Lnet/minecraft/world/server/ChunkManager;",
                GLUE + "WorldGlue.chunkManager(Ljava/lang/Object;)Lnet/minecraft/world/server/ChunkManager;");
        method("net/minecraft/world/server/ChunkManager", "func_219097_a", "(Lnet/minecraft/util/math/ChunkPos;Z)Ljava/util/stream/Stream;", providers,
                "trackingPlayers", "(Ljava/lang/Object;Lnet/minecraft/util/math/ChunkPos;Z)Ljava/util/stream/Stream;");
        // A world's chunk generator: a 1.16 stand-in answering from the 1.12 world (see the glue's LegacyChunkGenerator116).
        method("net/minecraft/world/server/ServerChunkProvider", "func_201711_g", "()Lnet/minecraft/world/gen/ChunkGenerator;",
                GLUE + "LegacyChunkGenerator116", "ofProvider", "(Ljava/lang/Object;)Lnet/minecraft/world/gen/ChunkGenerator;");
        // Chunks: heights by kind, fluids and biomes by position.
        String chunk = "net/minecraft/world/chunk/Chunk";
        method(chunk, "func_201576_a", "(Lnet/minecraft/world/gen/Heightmap$Type;II)I", SHIMS + "ChunkShims", "getTopBlockY", "(Lnet/minecraft/world/chunk/Chunk;Ljava/lang/Object;II)I");
        method(chunk, "func_204610_c", "(" + pos + ")Lnet/minecraft/fluid/FluidState;", SHIMS + "ChunkShims", "getFluidState");
        method(chunk, "func_201600_k", "(" + pos + ")Lnet/minecraft/world/biome/Biome;", SHIMS + "ChunkShims", "getBiome");

        // Command sources of entities and the server.
        String commandSource = "com/michaelsebero/portium/command/CommandSource";
        method("net/minecraft/entity/Entity", "func_195051_bN", "()Lnet/minecraft/command/CommandSource;", commandSource, "ofEntity");
        method("net/minecraft/server/MinecraftServer", "func_195573_aM", "()Lnet/minecraft/command/CommandSource;", commandSource, "ofServer");

        // Saved data stays in 1.12's format: 1.13's fixers aren't run on it.
        String nbt = "Lnet/minecraft/nbt/CompoundNBT;";
        String fixer = "(Lcom/mojang/datafixers/DataFixer;Lcom/mojang/datafixers/DSL$TypeReference;" + nbt;
        method("net/minecraft/nbt/NBTUtil", "func_210822_a", fixer + "I)" + nbt, GLUE + "DataFixGlue", "update");
        method("net/minecraft/nbt/NBTUtil", "func_210821_a", fixer + "II)" + nbt, GLUE + "DataFixGlue", "update");

        // 1.16's recipe manager: the 1.16 glue's, reading every recipe in the data packs by its type's serializer.
        String manager = "net/minecraft/item/crafting/RecipeManager";
        for (String worldType : new String[] {"net/minecraft/world/World", "net/minecraft/world/server/ServerWorld"}) {
            method(worldType, "func_199532_z", "()L" + manager + ";", GLUE + "RecipeManager116", "of");
        }
        method("net/minecraft/server/MinecraftServer", "func_71218_a", "(Lnet/minecraft/util/RegistryKey;)Lnet/minecraft/world/server/ServerWorld;",
                GLUE + "WorldGlue", "world");
        // 1.16's mob attributes by entity type: the usual starting sets, as Portium's stand-in makes them.
        String attributeMap = "com/michaelsebero/portium/modern/mc/entity/ai/attributes/AttributeModifierMap";
        String attributeBuilder = "()Lnet/minecraft/entity/ai/attributes/AttributeModifierMap$MutableAttribute;";
        for (String owner : new String[] {"LivingEntity", "MobEntity", "CreatureEntity", "AgeableEntity", "FlyingEntity", "passive/AnimalEntity",
                "passive/TameableEntity", "passive/WaterMobEntity", "passive/GolemEntity", "passive/AmbientEntity", "monster/MonsterEntity"}) {
            method("net/minecraft/entity/" + owner, "func_233639_cI_", attributeBuilder, attributeMap, "living", attributeBuilder);
            if (!owner.equals("LivingEntity")) method("net/minecraft/entity/" + owner, "func_233666_p_", attributeBuilder, attributeMap, "mob", attributeBuilder);
        }
        method("net/minecraft/entity/monster/MonsterEntity", "func_234295_eP_", attributeBuilder, attributeMap, "monster", attributeBuilder);
        // 1.16's dye colours: the map colour and the firework and text colours, which 1.12 keeps elsewhere.
        method("net/minecraft/item/DyeColor", "func_196055_e", "()Lnet/minecraft/block/material/MaterialColor;", SHIMS116 + "MiscShims116", "dyeMapColor");
        method("net/minecraft/item/DyeColor", "func_196060_f", "()I", SHIMS116 + "MiscShims116", "dyeFireworkColor");
        method("net/minecraft/item/DyeColor", "func_218388_g", "()I", SHIMS116 + "MiscShims116", "dyeTextColor");
        // 1.14's saved data by world (Portium's stand-in over 1.12's per-world storage).
        String savedData = "com/michaelsebero/portium/modern/mc/world/storage/DimensionSavedDataManager";
        for (String worldType : new String[] {"net/minecraft/world/server/ServerWorld", "net/minecraft/world/World"}) {
            method(worldType, "func_217481_x", "()Lnet/minecraft/world/storage/DimensionSavedDataManager;", savedData, "of",
                    "(Lnet/minecraft/world/World;)Lcom/michaelsebero/portium/modern/mc/world/storage/DimensionSavedDataManager;");
        }
        // 1.16's colour of a text formatting (an RGB integer; none for the styles).
        method("net/minecraft/util/text/TextFormatting", "func_211163_e", "()Ljava/lang/Integer;", SHIMS116 + "TextShims116", "formattingColor");
        // 1.16's "eyes in a fluid of this tag" (water or lava): 1.12's material check at the eyes.
        method("net/minecraft/entity/Entity", "func_208600_a", "(Lnet/minecraft/tags/ITag;)Z", GLUE + "EntityGlue", "eyesInFluid",
                "(Lnet/minecraft/entity/Entity;Lnet/minecraft/tags/ITag;)Z");
        // 1.14's way to open a menu for a player (vanilla-style blocks; Forge's NetworkHooks.openGui is the glue's too).
        method("net/minecraft/entity/player/PlayerEntity", "func_213829_a", "(Lnet/minecraft/inventory/container/INamedContainerProvider;)Ljava/util/OptionalInt;",
                GLUE + "MenuGlue", "openContainer");
        // 1.15's sprite lookup by atlas: all in 1.12's block atlas.
        method("net/minecraft/client/Minecraft", "func_228015_a_", "(Lnet/minecraft/util/ResourceLocation;)Ljava/util/function/Function;",
                SHIMS116 + "ClientShims116", "atlasSprites");
        // A projectile's shooter, for 1.12's projectiles as well as 1.16 mods' own.
        method("net/minecraft/entity/projectile/ProjectileEntity", "func_234616_v_", "()Lnet/minecraft/entity/Entity;", SHIMS116 + "EntityShims116", "projectileOwner",
                "(Lnet/minecraft/entity/Entity;)Lnet/minecraft/entity/Entity;");
        method("net/minecraft/entity/projectile/ProjectileEntity", "func_212361_a", "(Lnet/minecraft/entity/Entity;)V", SHIMS116 + "EntityShims116", "setProjectileOwner",
                "(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;)V");
        // Forge 1.16's biome types by registry key: 1.12's by the biome of that name.
        method("net/minecraftforge/common/BiomeDictionary", "getTypes", "(Lnet/minecraft/util/RegistryKey;)Ljava/util/Set;", SHIMS116 + "WorldShims116",
                "biomeTypes", "(Ljava/lang/Object;)Ljava/util/Set;");
        method("net/minecraftforge/common/BiomeDictionary", "hasType", "(Lnet/minecraft/util/RegistryKey;Lnet/minecraftforge/common/BiomeDictionary$Type;)Z",
                SHIMS116 + "WorldShims116", "biomeHasType", "(Ljava/lang/Object;Lnet/minecraftforge/common/BiomeDictionary$Type;)Z");
        // Forge 1.16's new banner patterns: 1.12's, added to its pattern enum.
        method("net/minecraft/tileentity/BannerPattern", "create", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)Lnet/minecraft/tileentity/BannerPattern;",
                "com/michaelsebero/portium/item/BannerPatterns116", "create");
        // 1.16's biome category (1.12 sorts biomes by their dictionary types).
        method("net/minecraft/world/biome/Biome", "func_201856_r", "()Lnet/minecraft/world/biome/Biome$Category;", GLUE + "WorldGlue", "category",
                "(Lnet/minecraft/world/biome/Biome;)Lnet/minecraft/world/biome/Biome$Category;");
        // 1.16's new enchantment categories: Forge 1.12's, added to its enum.
        method("net/minecraft/enchantment/EnchantmentType", "create", "(Ljava/lang/String;Ljava/util/function/Predicate;)Lnet/minecraft/enchantment/EnchantmentType;",
                SHIMS116 + "ItemShims116", "enchantmentType");
        // 1.16's development-mode pause before an error: none outside development.
        method("net/minecraft/util/Util", "func_229757_c_", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;", SHIMS116 + "MiscShims116", "pauseDevMode");
        // 1.16's attribute reads and modifier changes, onto 1.12's attribute instances.
        String attrShims = SHIMS116 + "EntityShims116";
        String attribute = "Lnet/minecraft/entity/ai/attributes/Attribute;";
        String livingTarget = "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/entity/ai/attributes/IAttribute;)D";
        method("net/minecraft/entity/LivingEntity", "func_233637_b_", "(" + attribute + ")D", attrShims, "attributeValue", livingTarget);
        method("net/minecraft/entity/LivingEntity", "func_233638_c_", "(" + attribute + ")D", attrShims, "attributeBase", livingTarget);
        method("net/minecraft/entity/LivingEntity", "func_233645_dx_", "()Lnet/minecraft/entity/ai/attributes/AttributeModifierManager;", attrShims, "attributeMap",
                "(Lnet/minecraft/entity/EntityLivingBase;)Lnet/minecraft/entity/ai/attributes/AbstractAttributeMap;");
        String instance = "net/minecraft/entity/ai/attributes/ModifiableAttributeInstance";
        String attrModifier = "Lnet/minecraft/entity/ai/attributes/AttributeModifier;";
        String instanceTarget = "(Lnet/minecraft/entity/ai/attributes/IAttributeInstance;" + attrModifier + ")V";
        method(instance, "func_233767_b_", "(" + attrModifier + ")V", attrShims, "applyModifier", instanceTarget);
        method(instance, "func_233769_c_", "(" + attrModifier + ")V", attrShims, "applyModifier", instanceTarget);
        method(instance, "func_233770_c_", "(Ljava/util/UUID;)Z", attrShims, "removeModifier", "(Lnet/minecraft/entity/ai/attributes/IAttributeInstance;Ljava/util/UUID;)Z");
        String attrManager = "net/minecraft/entity/ai/attributes/AttributeModifierManager";
        method(attrManager, "func_233779_a_", "(" + attribute + ")L" + instance + ";", attrShims, "attributeInstance",
                "(Lnet/minecraft/entity/ai/attributes/AbstractAttributeMap;Lnet/minecraft/entity/ai/attributes/IAttribute;)Lnet/minecraft/entity/ai/attributes/IAttributeInstance;");
        method(attrManager, "func_233795_c_", "(" + attribute + ")D", attrShims, "mapValue",
                "(Lnet/minecraft/entity/ai/attributes/AbstractAttributeMap;Lnet/minecraft/entity/ai/attributes/IAttribute;)D");
        method(attrManager, "func_233790_b_", "(" + attribute + ")Z", attrShims, "mapHas",
                "(Lnet/minecraft/entity/ai/attributes/AbstractAttributeMap;Lnet/minecraft/entity/ai/attributes/IAttribute;)Z");
        // 1.16's whole-map modifier changes (a held item's modifiers, as Create's deployer applies them): by name on 1.12's map.
        method(attrManager, "func_233793_b_", "(Lcom/google/common/collect/Multimap;)V", attrShims, "reapplyModifiers",
                "(Lnet/minecraft/entity/ai/attributes/AbstractAttributeMap;Lcom/google/common/collect/Multimap;)V");
        method(attrManager, "func_233785_a_", "(Lcom/google/common/collect/Multimap;)V", attrShims, "removeModifiers",
                "(Lnet/minecraft/entity/ai/attributes/AbstractAttributeMap;Lcom/google/common/collect/Multimap;)V");
        // 1.16's static block drops (a drill's or saw's broken blocks): 1.12's own drops.
        String blockShims = SHIMS116 + "BlockShims116";
        String dropState = "Lnet/minecraft/block/BlockState;";
        String legacyDropArgs = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;";
        method("net/minecraft/block/Block", "func_220070_a", "(" + dropState + "Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;)Ljava/util/List;",
                blockShims, "drops", legacyDropArgs + ")Ljava/util/List;");
        method("net/minecraft/block/Block", "func_220077_a", "(" + dropState + "Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;"
                + "Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;", blockShims, "drops",
                legacyDropArgs + "Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;");
        method("net/minecraft/block/Block", "func_220075_c", "(" + dropState + "Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V", blockShims, "spawnDrops",
                "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V");
        method("net/minecraft/block/Block", "func_220059_a", "(" + dropState + "Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;)V",
                blockShims, "spawnDrops", legacyDropArgs + ")V");
        method("net/minecraft/block/Block", "func_220054_a", "(" + dropState + "Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;"
                + "Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V", blockShims, "spawnDrops",
                legacyDropArgs + "Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V");
        // Forge 1.16's right click event of a block hit (Create's deployer): 1.12's, by face and hit vector.
        method("net/minecraftforge/common/ForgeHooks", "onRightClickBlock", "(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;"
                + "Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockRayTraceResult;)Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock;",
                blockShims, "rightClickBlock", "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/EnumHand;Lnet/minecraft/util/math/BlockPos;"
                        + "Lnet/minecraft/util/math/RayTraceResult;)Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock;");
        // Forge 1.16's burn time of a fuel: 1.12's furnace's.
        method("net/minecraftforge/common/ForgeHooks", "getBurnTime", "(Lnet/minecraft/item/ItemStack;)I", blockShims, "burnTime", "(Lnet/minecraft/item/ItemStack;)I");
        method("net/minecraftforge/common/ForgeHooks", "getBurnTime", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/crafting/IRecipeType;)I", blockShims, "burnTime",
                "(Lnet/minecraft/item/ItemStack;Ljava/lang/Object;)I");
        // 1.16's day time (the world's time of day counting days, as 1.12's world time does).
        for (String dayTimeOwner : new String[] {"net/minecraft/world/World", "net/minecraft/world/IDayTimeReader", "net/minecraft/world/IWorld"}) {
            method(dayTimeOwner, "func_241851_ab", "()J", SHIMS116 + "WorldShims116", "dayTime", "(Lnet/minecraft/world/World;)J");
        }
        // Forge 1.16's brewing recipes by ingredients.
        method("net/minecraftforge/common/brewing/BrewingRecipeRegistry", "addRecipe",
                "(Lnet/minecraft/item/crafting/Ingredient;Lnet/minecraft/item/crafting/Ingredient;Lnet/minecraft/item/ItemStack;)Z", SHIMS116 + "ItemShims116", "addBrewing");
        // 1.14's optional entity data holds Java's Optional; 1.12's serializers hold Guava's.
        String serializer = "Lnet/minecraft/network/datasync/IDataSerializer;";
        String serializers116 = "com/michaelsebero/portium/network/ModernSerializers116";
        for (String[] optionalField : new String[][] {{"field_187203_m", "OPTIONAL_UUID"}, {"field_187201_k", "OPTIONAL_BLOCK_POS"},
                {"field_187197_g", "OPTIONAL_BLOCK_STATE"}, {"field_200544_f", "OPTIONAL_TEXT"}}) {
            field("net/minecraft/network/datasync/DataSerializers", optionalField[0], serializer, SHIMS116 + "EntityShims116.serializer" + optionalField[1]
                    + "()Lnet/minecraft/network/datasync/DataSerializer;");
        }
        // More of 1.16's entity methods 1.12 names otherwise or lacks.
        String entityShims = SHIMS116 + "EntityShims116";
        method("net/minecraft/entity/Entity", "func_203005_aq", "()Z", entityShims, "inWaterOrBubbleColumn", "(Lnet/minecraft/entity/Entity;)Z");
        method("net/minecraft/entity/Entity", "func_226270_aj_", "()Lnet/minecraft/util/math/BlockPos;", entityShims, "positionUnderneath",
                "(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/BlockPos;");
        method("net/minecraft/entity/Entity", "func_213323_x_", "()V", entityShims, "recalculateSize", "(Lnet/minecraft/entity/Entity;)V");
        method("net/minecraft/entity/Entity", "func_205010_bg", "()I", entityShims, "maxAir", "(Lnet/minecraft/entity/Entity;)I");
        for (String creature : new String[] {"net/minecraft/entity/CreatureEntity", "net/minecraft/entity/MobEntity"}) {
            field(creature, "field_213402_bL", "F", entityShims + ".homeDistance(Lnet/minecraft/entity/EntityCreature;)F",
                    entityShims + ".setHomeDistance(Lnet/minecraft/entity/EntityCreature;F)V");
        }
        // 1.16 keeps a mob's home on every mob; 1.12 only on creatures.
        String mob = "net/minecraft/entity/MobEntity";
        String livingArg = "(Lnet/minecraft/entity/EntityLiving;";
        method(mob, "func_213389_a", "(Lnet/minecraft/util/math/BlockPos;)Z", entityShims, "withinHome", livingArg + "Lnet/minecraft/util/math/BlockPos;)Z");
        method(mob, "func_213390_a", "(Lnet/minecraft/util/math/BlockPos;I)V", entityShims, "setHome", livingArg + "Lnet/minecraft/util/math/BlockPos;I)V");
        method(mob, "func_213384_dI", "()Lnet/minecraft/util/math/BlockPos;", entityShims, "home", livingArg + ")Lnet/minecraft/util/math/BlockPos;");
        method(mob, "func_213391_dJ", "()F", entityShims, "homeDistanceOf", livingArg + ")F");
        method(mob, "func_213394_dL", "()Z", entityShims, "hasHome", livingArg + ")Z");
        // 1.16's path finder takes how many nodes it may visit; 1.12's has its own limit.
        constructor("net/minecraft/pathfinding/PathFinder", "(Lnet/minecraft/pathfinding/NodeProcessor;I)V", "(Lnet/minecraft/pathfinding/NodeProcessor;)V", arg(0));
        // 1.16's static node type helpers (mods' node processors call them).
        String walk = "net/minecraft/pathfinding/WalkNodeProcessor";
        String pathShims = SHIMS116 + "PathShims116";
        String legacyNodeArgs = "(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;";
        String nodeType = "Lnet/minecraft/pathfinding/PathNodeType;";
        method(walk, "func_237238_b_", "(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;)" + nodeType, pathShims, "blockNodeType", legacyNodeArgs + ")" + nodeType);
        method(walk, "func_237231_a_", "(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos$Mutable;)" + nodeType, pathShims, "floorNodeType", legacyNodeArgs + ")" + nodeType);
        method(walk, "func_237232_a_", "(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos$Mutable;" + nodeType + ")" + nodeType, pathShims, "surroundingDanger",
                legacyNodeArgs + nodeType + ")" + nodeType);
        method(walk, "func_197682_a", "(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;)D", pathShims, "groundY", legacyNodeArgs + ")D");
        // 1.16's swimmers may breach the surface (an argument 1.12's swim processor lacks); 1.12's, which stays under water.
        constructor("net/minecraft/pathfinding/SwimNodeProcessor", "(Z)V", "()V");
        // More entity methods: wetness, sitting, death, limb animation, random spots.
        method("net/minecraft/entity/Entity", "func_203008_ap", "()Z", entityShims, "wet", "(Lnet/minecraft/entity/Entity;)Z");
        method("net/minecraft/entity/passive/TameableEntity", "func_233687_w_", "(Z)V", entityShims, "setSitting", "(Lnet/minecraft/entity/passive/EntityTameable;Z)V");
        method("net/minecraft/entity/LivingEntity", "func_233643_dh_", "()Z", entityShims, "shouldBeDead", "(Lnet/minecraft/entity/EntityLivingBase;)Z");
        method("net/minecraft/entity/LivingEntity", "func_233629_a_", "(Lnet/minecraft/entity/LivingEntity;Z)V", entityShims, "updateLimbs",
                "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/entity/EntityLivingBase;Z)V");
        method("net/minecraft/entity/Entity", "func_226282_d_", "(D)D", entityShims, "randomX", "(Lnet/minecraft/entity/Entity;D)D");
        method("net/minecraft/entity/Entity", "func_226287_g_", "(D)D", entityShims, "randomZ", "(Lnet/minecraft/entity/Entity;D)D");
        method("net/minecraft/entity/Entity", "func_226279_cv_", "()D", entityShims, "randomY", "(Lnet/minecraft/entity/Entity;)D");
        // 1.16's creature attributes are constants of a class; 1.12's an enum.
        for (String[] kind : new String[][] {{"field_223222_a_", "UNDEFINED"}, {"field_223223_b_", "UNDEAD"}, {"field_223224_c_", "ARTHROPOD"},
                {"field_223225_d_", "ILLAGER"}, {"field_203100_e", "WATER"}}) {
            field("net/minecraft/entity/CreatureAttribute", kind[0], "Lnet/minecraft/entity/CreatureAttribute;",
                    entityShims + ".creatureAttribute" + kind[1] + "()Lnet/minecraft/entity/EnumCreatureAttribute;");
        }
        // 1.16's entity type tags.
        // 1.16's motion multiplier (cobwebs, berry bushes, Create's crushing wheels): 1.12's web slowdown.
        method("net/minecraft/entity/Entity", "func_213295_a", "(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/vector/Vector3d;)V", entityShims,
                "setMotionMultiplier", "(Lnet/minecraft/entity/Entity;Ljava/lang/Object;Lnet/minecraft/util/math/Vec3d;)V");
        // 1.16's aggressive flag (an AI flag bit 1.12's mobs don't have; pillagers' and mods' arm poses read it).
        method("net/minecraft/entity/MobEntity", "func_213395_q", "(Z)V", entityShims, "setAggroed", "(Lnet/minecraft/entity/EntityLiving;Z)V");
        method("net/minecraft/entity/MobEntity", "func_213398_dR", "()Z", entityShims, "isAggressive", "(Lnet/minecraft/entity/EntityLiving;)Z");
        // Forge 1.16's path node type of a state: 1.12 Forge asks the block.
        for (String stateType : new String[] {"net/minecraft/block/BlockState", "net/minecraft/block/AbstractBlock$AbstractBlockState",
                "net/minecraftforge/common/extensions/IForgeBlockState"}) {
            method(stateType, "getAiPathNodeType", "(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/pathfinding/PathNodeType;",
                    entityShims, "pathNodeType", "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/pathfinding/PathNodeType;");
            method(stateType, "getAiPathNodeType", "(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/MobEntity;)Lnet/minecraft/pathfinding/PathNodeType;",
                    entityShims, "pathNodeType", "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/EntityLiving;)Lnet/minecraft/pathfinding/PathNodeType;");
        }
        method("net/minecraft/entity/EntityType", "func_220341_a", "(Lnet/minecraft/tags/ITag;)Z", entityShims, "typeInTag",
                "(Lcom/michaelsebero/portium/modern/mc/entity/EntityType;Ljava/lang/Object;)Z");
        // 1.16's paths: points in a list, and where they lead.
        method("net/minecraft/pathfinding/Path", "func_242948_g", "()Lnet/minecraft/util/math/BlockPos;", entityShims, "pathTarget",
                "(Lnet/minecraft/pathfinding/Path;)Lnet/minecraft/util/math/BlockPos;");
        String path = "net/minecraft/pathfinding/Path";
        String pathArg = "(Lnet/minecraft/pathfinding/Path;";
        String point = "Lnet/minecraft/pathfinding/PathPoint;";
        method(path, "func_237225_h_", "()" + point, entityShims, "pathCurrent", pathArg + ")" + point);
        method(path, "func_242950_i", "()" + point, entityShims, "pathPrevious", pathArg + ")" + point);
        method(path, "func_242945_b", "()Z", entityShims, "pathNotStarted", pathArg + ")Z");
        method(path, "func_242947_d", "(I)Lnet/minecraft/util/math/BlockPos;", entityShims, "pathPos", pathArg + "I)Lnet/minecraft/util/math/BlockPos;");
        method(path, "func_215747_b", "(I)V", entityShims, "pathSetLength", pathArg + "I)V");
        method(path, "func_224771_h", "()Z", entityShims, "pathReaches", pathArg + ")Z");
        method(path, "func_224770_k", "()Lnet/minecraft/util/math/BlockPos;", entityShims, "pathTarget", pathArg + ")Lnet/minecraft/util/math/BlockPos;");
        method(path, "func_224769_l", "()F", entityShims, "pathDistance", pathArg + ")F");
        method("net/minecraft/util/math/vector/Vector3d", "func_237488_a_", "(Lnet/minecraft/dispenser/IPosition;D)Z", entityShims, "withinDistance",
                "(Lnet/minecraft/util/math/Vec3d;Ljava/lang/Object;D)Z");
        method("net/minecraft/entity/MobEntity", "func_213392_I", "()Z", entityShims, "preventDespawn", "(Lnet/minecraft/entity/EntityLiving;)Z");
        // 1.16's point-of-interest manager for mods, answered from the world's blocks (PoiGlue).
        String poi = "net/minecraft/village/PointOfInterestManager";
        String poiGlue = GLUE + "PoiGlue";
        String types = "Ljava/util/function/Predicate;";
        String poiStatus = "Lnet/minecraft/village/PointOfInterestManager$Status;";
        String bpos = "Lnet/minecraft/util/math/BlockPos;";
        method("net/minecraft/world/server/ServerWorld", "func_217443_B", "()L" + poi + ";", poiGlue, "manager");
        method(poi, "func_225399_a", "(" + types + types + bpos + "I" + poiStatus + ")Ljava/util/stream/Stream;", poiGlue, "findAll",
                "(Ljava/lang/Object;" + types + types + bpos + "ILjava/lang/Object;)Ljava/util/stream/Stream;");
        method(poi, "func_242324_b", "(" + types + types + bpos + "I" + poiStatus + ")Ljava/util/stream/Stream;", poiGlue, "findAll",
                "(Ljava/lang/Object;" + types + types + bpos + "ILjava/lang/Object;)Ljava/util/stream/Stream;");
        method(poi, "func_219127_a", "(" + types + types + bpos + "I" + poiStatus + ")Ljava/util/Optional;", poiGlue, "find",
                "(Ljava/lang/Object;" + types + types + bpos + "ILjava/lang/Object;)Ljava/util/Optional;");
        method(poi, "func_234148_d_", "(" + types + bpos + "I" + poiStatus + ")Ljava/util/Optional;", poiGlue, "findClosest",
                "(Ljava/lang/Object;" + types + bpos + "ILjava/lang/Object;)Ljava/util/Optional;");
        method(poi, "func_219157_a", "(" + types + types + bpos + "I)Ljava/util/Optional;", poiGlue, "take",
                "(Ljava/lang/Object;" + types + types + bpos + "I)Ljava/util/Optional;");
        method(poi, "func_219163_a", "(" + types + types + poiStatus + bpos + "ILjava/util/Random;)Ljava/util/Optional;", poiGlue, "random",
                "(Ljava/lang/Object;" + types + types + "Ljava/lang/Object;" + bpos + "ILjava/util/Random;)Ljava/util/Optional;");
        method(poi, "func_219145_a", "(" + types + bpos + "I" + poiStatus + ")J", poiGlue, "count", "(Ljava/lang/Object;" + types + bpos + "ILjava/lang/Object;)J");
        for (String recordQuery : new String[] {"func_226353_b_", "func_219146_b"}) {
            method(poi, recordQuery, "(" + types + bpos + "I" + poiStatus + ")Ljava/util/stream/Stream;", poiGlue, "records",
                    "(Ljava/lang/Object;" + types + bpos + "ILjava/lang/Object;)Ljava/util/stream/Stream;");
        }
        method(poi, "func_219138_a", "(" + bpos + types + ")Z", poiGlue, "hasTypeAt", "(Ljava/lang/Object;" + bpos + types + ")Z");
        method(poi, "func_219148_c", "(" + bpos + ")Ljava/util/Optional;", poiGlue, "typeAt", "(Ljava/lang/Object;" + bpos + ")Ljava/util/Optional;");
        method(poi, "func_219142_b", "(" + bpos + ")Z", poiGlue, "release", "(Ljava/lang/Object;" + bpos + ")Z");
        // 1.16's navigator stuck-check node is a block position; 1.12's a vector.
        field("net/minecraft/pathfinding/PathNavigator", "field_188557_k", "Lnet/minecraft/util/math/vector/Vector3i;",
                entityShims + ".stuckNode(Lnet/minecraft/pathfinding/PathNavigate;)Lnet/minecraft/util/math/Vec3i;",
                entityShims + ".setStuckNode(Lnet/minecraft/pathfinding/PathNavigate;Lnet/minecraft/util/math/Vec3i;)V");
        // Forge 1.16's brewing recipe by ingredients: Forge 1.12's by stacks (the ingredients' first).
        factory("net/minecraftforge/common/brewing/BrewingRecipe", "(Lnet/minecraft/item/crafting/Ingredient;Lnet/minecraft/item/crafting/Ingredient;Lnet/minecraft/item/ItemStack;)V",
                SHIMS116 + "ItemShims116.brewingRecipe(Lnet/minecraft/item/crafting/Ingredient;Lnet/minecraft/item/crafting/Ingredient;Lnet/minecraft/item/ItemStack;)"
                        + "Lnet/minecraftforge/common/brewing/BrewingRecipe;");
        // ... and the same for recipes that extend it (Alex's Mobs' ProperBrewingRecipe calls this super constructor).
        String firstStack = SHIMS116 + "ItemShims116.firstStack(Lnet/minecraft/item/crafting/Ingredient;)Lnet/minecraft/item/ItemStack;";
        constructor("net/minecraftforge/common/brewing/BrewingRecipe", "(Lnet/minecraft/item/crafting/Ingredient;Lnet/minecraft/item/crafting/Ingredient;Lnet/minecraft/item/ItemStack;)V",
                "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)V", arg(0, firstStack), arg(1, firstStack), arg(2));
        // Forge 1.16's registry builder options 1.12's lacks (tags of custom registries): accepted, without effect.
        String registryBuilder = "net/minecraftforge/registries/RegistryBuilder";
        method(registryBuilder, "tagFolder", "(Ljava/lang/String;)L" + registryBuilder + ";", SHIMS116 + "RegistryShims116", "tagFolder",
                "(L" + registryBuilder + ";Ljava/lang/String;)L" + registryBuilder + ";");
        method(registryBuilder, "hasTags", "()L" + registryBuilder + ";", SHIMS116 + "RegistryShims116", "hasTags", "(L" + registryBuilder + ";)L" + registryBuilder + ";");
        // Forge 1.16's registry entries in packets.
        String packets = SHIMS116 + "PacketShims116";
        String packetBuffer = "net/minecraft/network/PacketBuffer";
        String registryEntry = "Lnet/minecraftforge/registries/IForgeRegistryEntry;";
        String forgeRegistry = "Lnet/minecraftforge/registries/IForgeRegistry;";
        String legacyBuffer = "(Lnet/minecraft/network/PacketBuffer;";
        method(packetBuffer, "writeRegistryId", "(" + registryEntry + ")V", packets, "writeRegistryId", legacyBuffer + registryEntry + ")V");
        method(packetBuffer, "readRegistryId", "()" + registryEntry, packets, "readRegistryId", legacyBuffer + ")" + registryEntry);
        method(packetBuffer, "readRegistryIdSafe", "(Ljava/lang/Class;)" + registryEntry, packets, "readRegistryIdSafe", legacyBuffer + "Ljava/lang/Class;)" + registryEntry);
        method(packetBuffer, "writeRegistryIdUnsafe", "(" + forgeRegistry + registryEntry + ")V", packets, "writeRegistryIdUnsafe", legacyBuffer + forgeRegistry + registryEntry + ")V");
        method(packetBuffer, "writeRegistryIdUnsafe", "(" + forgeRegistry + "Lnet/minecraft/util/ResourceLocation;)V", packets, "writeRegistryIdUnsafe",
                legacyBuffer + forgeRegistry + "Lnet/minecraft/util/ResourceLocation;)V");
        method(packetBuffer, "readRegistryIdUnsafe", "(" + forgeRegistry + ")" + registryEntry, packets, "readRegistryIdUnsafe", legacyBuffer + forgeRegistry + ")" + registryEntry);
        // 1.16's do-nothing dispense behaviour: 1.12's.
        field("net/minecraft/dispenser/IDispenseItemBehavior", "field_223216_a_", "Lnet/minecraft/dispenser/IDispenseItemBehavior;",
                SHIMS116 + "BlockShims116.noDispense()Lnet/minecraft/dispenser/IBehaviorDispenseItem;");
        // 1.16's block map colour.
        for (String blockType : new String[] {"net/minecraft/block/AbstractBlock", "net/minecraft/block/Block"}) {
            method(blockType, "func_235697_s_", "()Lnet/minecraft/block/material/MaterialColor;", SHIMS116 + "BlockShims116", "materialColor",
                    "(Lnet/minecraft/block/Block;)Lnet/minecraft/block/material/MapColor;");
        }
        // Widget members on 1.16's text field (1.12's text field isn't a widget).
        String textField = "net/minecraft/client/gui/widget/TextFieldWidget";
        String textShims = SHIMS116 + "TextFieldShims116";
        String legacyField = "(Lnet/minecraft/client/gui/GuiTextField;";
        method(textField, "func_230999_j_", "()Z", textShims, "isFocused", legacyField + ")Z");
        method(textField, "func_230996_d_", "(Z)V", textShims, "setFocused", legacyField + "Z)V");
        method(textField, "func_230430_a_", "(" + stack116 + "IIF)V", textShims, "render", legacyField + "Ljava/lang/Object;IIF)V");
        method(textField, "func_231044_a_", "(DDI)Z", textShims, "mouseClicked", legacyField + "DDI)Z");
        method(textField, "func_231046_a_", "(III)Z", textShims, "keyPressed", legacyField + "III)Z");
        method(textField, "func_231042_a_", "(CI)Z", textShims, "charTyped", legacyField + "CI)Z");
        for (String[] each : new String[][] {{"field_230690_l_", "x", "setX"}, {"field_230691_m_", "y", "setY"}, {"field_230688_j_", "width", "setWidth"},
                {"field_230689_k_", "height", "setHeight"}}) {
            field(textField, each[0], "I", textShims + "." + each[1] + legacyField + ")I", textShims + "." + each[2] + legacyField + "I)V");
        }
        field(textField, "field_230694_p_", "Z", textShims + ".visible" + legacyField + ")Z", textShims + ".setVisible" + legacyField + "Z)V");
        // 1.16's static shader helpers (Botania's own shader programs).
        String shaderShims = SHIMS116 + "ShaderShims116";
        String linkHelper = "net/minecraft/client/shader/ShaderLinkHelper";
        method(linkHelper, "func_148078_c", "()I", shaderShims, "createProgram", "()I");
        // LWJGL 3 calls LWJGL 2 has in another shape (Flywheel's).
        String lwjglShims = "com/michaelsebero/portium/mods116/lwjgl/LwjglShims116";
        for (String glClass : new String[] {"org/lwjgl/opengl/GL20", "org/lwjgl/opengl/GL20C", "org/lwjgl/opengl/GL21", "org/lwjgl/opengl/GL30",
                "org/lwjgl/opengl/GL31", "org/lwjgl/opengl/GL32", "org/lwjgl/opengl/GL33"}) {
            method(glClass, "glUniform4fv", "(I[F)V", lwjglShims, "glUniform4fv");
            method(glClass, "glUniformMatrix4fv", "(IZ[F)V", lwjglShims, "glUniformMatrix4fv");
            method(glClass, "glGetShaderInfoLog", "(I)Ljava/lang/String;", lwjglShims, "glGetShaderInfoLog");
            method(glClass, "glGetProgramInfoLog", "(I)Ljava/lang/String;", lwjglShims, "glGetProgramInfoLog");
        }
        for (String glClass : new String[] {"org/lwjgl/opengl/GL15", "org/lwjgl/opengl/GL20", "org/lwjgl/opengl/GL30"}) {
            method(glClass, "glMapBuffer", "(II)Ljava/nio/ByteBuffer;", lwjglShims, "glMapBuffer");
        }
        method("org/lwjgl/glfw/GLFW", "glfwGetKey", "(JI)I", lwjglShims, "glfwGetKey");
        method("org/lwjgl/glfw/GLFW", "glfwGetMouseButton", "(JI)I", lwjglShims, "glfwGetMouseButton");
        method("com/jozufozu/flywheel/backend/gl/versioned/GlCompat", "safeShaderSource", "(ILjava/lang/CharSequence;)V", shaderShims, "shaderSource");
        method(linkHelper, "func_227804_a_", "(I)V", shaderShims, "useProgram", "(I)V");
        method(linkHelper, "func_148075_b", "(Lnet/minecraft/client/shader/IShaderManager;)V", GLUE + "ShaderGlue", "linkProgram");
        method("net/minecraft/client/shader/ShaderLoader", "func_216534_a",
                "(Lnet/minecraft/client/shader/ShaderLoader$ShaderType;Ljava/lang/String;Ljava/io/InputStream;Ljava/lang/String;)Lnet/minecraft/client/shader/ShaderLoader;",
                shaderShims, "compile");
        // 1.16's loot builders and loot context (LootGlue; a mod adding pools in LootTableLoadEvent).
        String lootGlue = GLUE + "LootGlue";
        String poolBuilder = "net/minecraft/loot/LootPool$Builder";
        method(poolBuilder, "name", "(Ljava/lang/String;)L" + poolBuilder + ";", lootGlue, "poolName");
        method(poolBuilder, "bonusRolls", "(FF)L" + poolBuilder + ";", lootGlue, "bonusRolls");
        method(poolBuilder, "func_216044_b", "()Lnet/minecraft/loot/LootPool;", lootGlue, "buildPool");
        String lootContext = "net/minecraft/loot/LootContext";
        method(lootContext, "func_216031_c", "(Lnet/minecraft/loot/LootParameter;)Ljava/lang/Object;", lootGlue, "param");
        method(lootContext, "func_216033_a", "(Lnet/minecraft/loot/LootParameter;)Z", lootGlue, "hasParam");
        method(lootContext, "func_216032_b", "()Ljava/util/Random;", lootGlue, "random");
        method(lootContext, "func_202879_g", "()Lnet/minecraft/world/server/ServerWorld;", lootGlue, "world");
        method(lootContext, "func_227502_a_", "(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/loot/LootTable;", lootGlue, "lootTable");
        method("net/minecraft/loot/LootTable", "func_216114_a", "(L" + lootContext + ";Ljava/util/function/Consumer;)V", lootGlue, "generate");
        method("net/minecraft/loot/LootTable", "func_216113_a", "(L" + lootContext + ";)Ljava/util/List;", lootGlue, "generate");
        method("net/minecraft/loot/functions/LootFunctionManager", "func_216241_a", "([Ljava/util/function/BiFunction;)Ljava/util/function/BiFunction;", lootGlue, "compose");
        field("net/minecraft/loot/functions/LootFunctionManager", "field_216242_a", "Ljava/util/function/BiFunction;", lootGlue + ".identity()Ljava/util/function/BiFunction;");
        // A mod's renderer posting Forge's living render events itself (1.12's take a 1.12 renderer and a position).
        String livingEvent = "net/minecraftforge/client/event/RenderLivingEvent";
        String renderShims = "com/michaelsebero/portium/client/EntityRendering116";
        String livingEventArgs = "(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/LivingRenderer;F" + stack116
                + "Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V";
        factory(livingEvent + "$Pre", livingEventArgs, renderShims + ".livingPre(Ljava/lang/Object;Ljava/lang/Object;FLjava/lang/Object;Ljava/lang/Object;I)L" + livingEvent + "$Pre;");
        factory(livingEvent + "$Post", livingEventArgs, renderShims + ".livingPost(Ljava/lang/Object;Ljava/lang/Object;FLjava/lang/Object;Ljava/lang/Object;I)L" + livingEvent + "$Post;");
        for (String each : new String[] {livingEvent, livingEvent + "$Pre", livingEvent + "$Post"}) {
            method(each, "getMatrixStack", "()" + stack116, GLUE + "RenderGlue", "matrixStack");
            method(each, "getBuffers", "()Lnet/minecraft/client/renderer/IRenderTypeBuffer;", GLUE + "RenderGlue", "buffers");
            method(each, "getLight", "()I", GLUE + "RenderGlue", "light");
        }
        String lootRange = "Lnet/minecraft/loot/IRandomRange;";
        String functionBuilder = "Lnet/minecraft/loot/LootFunction$Builder;";
        method("net/minecraft/loot/functions/SetCount", "func_215932_a", "(" + lootRange + ")" + functionBuilder, lootGlue, "setCount");
        method("net/minecraft/loot/functions/SetNBT", "func_215952_a", "(Lnet/minecraft/nbt/CompoundNBT;)" + functionBuilder, lootGlue, "setNbt");
        method("net/minecraft/loot/functions/EnchantRandomly", "func_215900_c", "()" + functionBuilder, lootGlue, "enchantRandomly");
        method("net/minecraft/loot/functions/SetDamage", "func_215931_a", "(Lnet/minecraft/loot/RandomValueRange;)" + functionBuilder, lootGlue, "setDamage");
        method("net/minecraft/loot/functions/Smelt", "func_215953_b", "()" + functionBuilder, lootGlue, "smelt");
        method("net/minecraft/loot/functions/LootingEnchantBonus", "func_215915_a", "(Lnet/minecraft/loot/RandomValueRange;)Lnet/minecraft/loot/functions/LootingEnchantBonus$Builder;",
                lootGlue, "lootingBonus");
        method("net/minecraft/loot/functions/EnchantWithLevels", "func_215895_a", "(" + lootRange + ")Lnet/minecraft/loot/functions/EnchantWithLevels$Builder;",
                lootGlue, "enchantWithLevels");
        String conditionBuilder = "Lnet/minecraft/loot/conditions/ILootCondition$IBuilder;";
        method("net/minecraft/loot/conditions/RandomChance", "func_216004_a", "(F)" + conditionBuilder, lootGlue, "randomChance");
        method("net/minecraft/loot/conditions/RandomChanceWithLooting", "func_216003_a", "(FF)" + conditionBuilder, lootGlue, "randomChanceWithLooting");
        String lootTypes = "com/michaelsebero/portium/loot/ModernLootTypes";
        String legacyRange = "Lnet/minecraft/world/storage/loot/RandomValueRange;";
        method("net/minecraft/loot/ConstantRange", "func_215835_a", "(I)Lnet/minecraft/loot/ConstantRange;", lootTypes, "constant", "(I)" + legacyRange);
        method("net/minecraft/loot/BinomialRange", "func_215838_a", "(IF)Lnet/minecraft/loot/BinomialRange;", lootTypes, "binomial", "(IF)" + legacyRange);
        factory("net/minecraft/loot/ConstantRange", "(I)V", lootTypes + ".constant(I)" + legacyRange);
        factory("net/minecraft/loot/BinomialRange", "(IF)V", lootTypes + ".binomial(IF)" + legacyRange);
        // 1.16's light brightness at a position.
        for (String reader : new String[] {"net/minecraft/world/IWorldReader", "net/minecraft/world/World"}) {
            method(reader, "func_205052_D", "(Lnet/minecraft/util/math/BlockPos;)F", SHIMS116 + "WorldShims116", "brightness",
                    "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)F");
        }
        // 1.16's game rules by key: 1.12's by the key's name.
        String ruleKey = "Lnet/minecraft/world/GameRules$RuleKey;";
        method("net/minecraft/world/GameRules", "func_223586_b", "(" + ruleKey + ")Z", SHIMS116 + "WorldShims116", "ruleBoolean",
                "(Lnet/minecraft/world/GameRules;Ljava/lang/Object;)Z");
        method("net/minecraft/world/GameRules", "func_223592_c", "(" + ruleKey + ")I", SHIMS116 + "WorldShims116", "ruleInt",
                "(Lnet/minecraft/world/GameRules;Ljava/lang/Object;)I");
        // 1.16's dimension ambient light.
        field("net/minecraft/world/DimensionType", "field_236018_y_", "F", SHIMS116 + "WorldShims116.ambientLight(Lnet/minecraft/world/DimensionType;)F");
        String dimension116 = "net/minecraft/world/DimensionType";
        String worlds = SHIMS116 + "WorldShims116";
        method(dimension116, "func_218272_d", "()Z", worlds, "hasSkyLight");
        method(dimension116, "func_236037_d_", "()Z", worlds, "hasCeiling");
        method(dimension116, "func_236040_e_", "()Z", worlds, "ultrawarm");
        method(dimension116, "func_236043_f_", "()Z", worlds, "natural");
        method(dimension116, "func_242724_f", "()D", worlds, "coordinateScale");
        method(dimension116, "func_241509_i_", "()Z", worlds, "piglinSafe");
        method(dimension116, "func_241510_j_", "()Z", worlds, "bedWorks");
        method(dimension116, "func_241511_k_", "()Z", worlds, "respawnAnchorWorks");
        method(dimension116, "func_241512_l_", "()Z", worlds, "hasRaids");
        method(dimension116, "func_241513_m_", "()I", worlds, "logicalHeight");
        method(dimension116, "func_236046_h_", "()Z", worlds, "hasDragonFight");
        method(dimension116, "func_241514_p_", "()Z", worlds, "hasFixedTime");
        // 1.14's goal flags: 1.12's task mutex bits (Goal is 1.12's task base; 1.16's goals themselves run as 1.16's).
        method("net/minecraft/entity/ai/goal/Goal", "func_220684_a", "(Ljava/util/EnumSet;)V", SHIMS116 + "EntityShims116", "setGoalFlags",
                "(Lnet/minecraft/entity/ai/EntityAIBase;Ljava/util/EnumSet;)V");
        method("net/minecraft/entity/ai/goal/Goal", "func_220686_i", "()Ljava/util/EnumSet;", SHIMS116 + "EntityShims116", "goalFlags",
                "(Lnet/minecraft/entity/ai/EntityAIBase;)Ljava/util/EnumSet;");
        method("net/minecraft/entity/ai/goal/Goal", "func_220685_C_", "()Z", SHIMS116 + "EntityShims116", "goalPreemptible",
                "(Lnet/minecraft/entity/ai/EntityAIBase;)Z");
        // 1.15's glowing check (1.12 names it otherwise).
        method("net/minecraft/entity/Entity", "func_225510_bt_", "()Z", SHIMS116 + "EntityShims116", "isGlowing");
        // Forge 1.16's key binding self (IForgeKeybinding.getKeyBinding).
        method("net/minecraft/client/settings/KeyBinding", "getKeyBinding", "()Lnet/minecraft/client/settings/KeyBinding;", SHIMS116 + "ClientShims116", "self");
        // 1.15's loading overlay: none once the game is running.
        field("net/minecraft/client/Minecraft", "field_213279_p", "Lnet/minecraft/client/gui/LoadingGui;",
                GLUE + "RenderGlue.loadingGui(Lnet/minecraft/client/Minecraft;)Lnet/minecraft/client/gui/LoadingGui;");
        // 1.16's statistic of a stat type with a formatter: 1.12's statistic for the object.
        method("net/minecraft/stats/StatType", "func_199077_a", "(Ljava/lang/Object;Lnet/minecraft/stats/IStatFormatter;)Lnet/minecraft/stats/Stat;",
                "com/michaelsebero/portium/modern/mc/stats/StatType", "withFormatter",
                "(Lcom/michaelsebero/portium/modern/mc/stats/StatType;Ljava/lang/Object;Ljava/lang/Object;)Lnet/minecraft/stats/StatBase;");
        method("net/minecraft/server/MinecraftServer", "func_212370_w", "()Ljava/lang/Iterable;", SHIMS116 + "WorldShims116", "allWorlds");
        // Forge 1.12's spawn events keep their place as floats.
        for (String spawnEvent : new String[] {"", "$CheckSpawn", "$SpecialSpawn", "$AllowDespawn"}) {
            for (String axis : new String[] {"X", "Y", "Z"}) {
                method("net/minecraftforge/event/entity/living/LivingSpawnEvent" + spawnEvent, "get" + axis, "()D", SHIMS116 + "MiscShims116", "spawn" + axis,
                        "(Lnet/minecraftforge/event/entity/living/LivingSpawnEvent;)D");
            }
        }
        method("net/minecraft/server/MinecraftServer", "func_240794_aZ_", "()Lnet/minecraft/resources/DataPackRegistries;", GLUE + "ReloadGlue", "registries",
                "(Ljava/lang/Object;)Lnet/minecraft/resources/DataPackRegistries;");
        method("net/minecraft/server/MinecraftServer", "func_199529_aN", "()L" + manager + ";", GLUE + "RecipeManager116", "of");
        method(manager, "func_215377_a", "(Lnet/minecraft/util/ResourceLocation;Lcom/google/gson/JsonObject;)Lnet/minecraft/item/crafting/IRecipe;", GLUE + "RecipeManager116",
                "deserialize");

        // Fuel.
        method("net/minecraft/item/ItemStack", "getBurnTime", "()I", misc, "getBurnTime");
        forgeItemStack();
        method("net/minecraftforge/event/ForgeEventFactory", "getItemBurnTime", "(Lnet/minecraft/item/ItemStack;I)I", misc, "getItemBurnTime");

        // Worlds give 1.13 code whole states, as 1.13's worlds keep them (and world features only the chunks being populated).
        method("net/minecraft/world/IBlockReader", "func_180495_p", "(" + pos + ")" + state, SHIMS116 + "WorldShims116", "getBlockState");

        // Forge 1.13's block state methods.
        String stateType = "net/minecraft/block/BlockState";
        String reader = "Lnet/minecraft/world/IBlockReader;";
        method(stateType, "func_196958_f", "()Z", BLOCKS, "isAir");
        method(stateType, "isAir", "(" + reader + pos + ")Z", BLOCKS, "isAir");
        method(stateType, "canBeConnectedTo", "(" + reader + pos + "Lnet/minecraft/util/Direction;)Z", BLOCKS, "canBeConnectedTo");
        method(stateType, "hasTileEntity", "()Z", BLOCKS, "hasTileEntity");
        method(stateType, "func_196959_b", "(Lnet/minecraft/state/Property;)Z", BLOCKS, "has");
        method(stateType, "func_200016_a", "(" + reader + pos + ")I", BLOCKS, "getOpacity");
        method(stateType, "func_200131_a", "(" + reader + pos + ")Z", BLOCKS, "propagatesSkylightDown");
        method(stateType, "func_200132_m", "()Z", BLOCKS, "isSolid");
        method("net/minecraft/block/Block", "func_208619_r", "()Z", BLOCKS, "isVariableOpacity");
        method("net/minecraft/block/Block", "func_200124_e", "(" + state + ")Z", BLOCKS, "isSolid");
        method("net/minecraft/block/Block", "func_196268_f", "(" + state + reader + pos + ")Lnet/minecraft/util/math/shapes/VoxelShape;", BLOCKS, "getCollisionShape");

        // 1.13 tile entities built on 1.12's vanilla ones.
        String tileEntities = SHIMS + "TileEntityShims";
        method("net/minecraft/tileentity/TileEntity", "func_195044_w", "()" + state, tileEntities, "getBlockState");
        method("net/minecraft/tileentity/IChestLid", "func_195480_a", "(F)F", tileEntities, "getLidAngle");
        method("net/minecraft/tileentity/SignTileEntity", "func_212366_a", "(I)Lnet/minecraft/util/text/ITextComponent;", tileEntities, "getText");
        method("net/minecraft/tileentity/SignTileEntity", "func_212365_a", "(ILnet/minecraft/util/text/ITextComponent;)V", tileEntities, "setText");
        method("net/minecraft/tileentity/SignTileEntity", "func_212364_a", "(ILjava/util/function/Function;)Ljava/lang/String;", tileEntities, "getRenderText");

        // 1.13's window and mouse are read from 1.12's Minecraft; colour handlers for blocks and items go to Forge 1.12's.
        String client = SHIMS + "client/ClientShims";
        field("net/minecraft/client/Minecraft", "field_195558_d", "Lnet/minecraft/client/MainWindow;",
                client + ".mainWindow(Lnet/minecraft/client/Minecraft;)Lnet/minecraft/client/MainWindow;");
        field("net/minecraft/client/Minecraft", "field_71417_B", "Lnet/minecraft/client/MouseHelper;",
                client + ".mouseHelper(Lnet/minecraft/client/Minecraft;)Lnet/minecraft/client/MouseHelper;");
        method("net/minecraft/client/renderer/texture/TextureAtlasSprite", "func_195668_m", "()Lnet/minecraft/util/ResourceLocation;", client, "getSpriteName");
        method("net/minecraft/client/renderer/GameRenderer", "func_195458_a", "(FJZ)V", client, "updateCameraAndRender");
        String bakedModel = "net/minecraft/client/renderer/model/IBakedModel";
        // In 1.16's vertex layout, which 1.16 code reads (see RenderShims116.getQuads116).
        method(bakedModel, "func_200117_a", "(" + state + "Lnet/minecraft/util/Direction;Ljava/util/Random;)Ljava/util/List;", SHIMS116 + "RenderShims116", "getQuads116");
        method(bakedModel, "getQuads", "(" + state + "Lnet/minecraft/util/Direction;Ljava/util/Random;Lnet/minecraftforge/client/model/data/IModelData;)Ljava/util/List;",
                GLUE + "ModelGlue", "getQuads");
        method("net/minecraft/client/Minecraft", "func_195551_G", "()Lnet/minecraft/resources/IResourceManager;", client, "getResourceManager");
        // 1.16's air has an empty model; 1.12's lookup answers invisible blocks with the missing model.
        String modelOf = "(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/renderer/model/IBakedModel;";
        method("net/minecraft/client/renderer/BlockRendererDispatcher", "func_184389_a", modelOf, SHIMS116 + "ClientShims116", "modelForState");
        method("net/minecraft/client/renderer/BlockModelShapes", "func_178125_b", modelOf, SHIMS116 + "ClientShims116", "modelFor");
        field("net/minecraft/client/Minecraft", "field_195559_v", "Lnet/minecraft/client/KeyboardListener;",
                client + ".keyboardListener(Lnet/minecraft/client/Minecraft;)Lnet/minecraft/client/KeyboardListener;");
        // Key bindings: 1.13's inputs are GLFW's, 1.12's codes LWJGL 2's.
        String binding = "net/minecraft/client/settings/KeyBinding";
        String input = "Lnet/minecraft/client/util/InputMappings$Input;";
        method(binding, "func_238171_j_", "()Lnet/minecraft/util/text/ITextComponent;", SHIMS116 + "ClientShims116", "keyMessage");
        method(binding, "getKey", "()" + input, client, "getKey");
        method(binding, "isActiveAndMatches", "(" + input + ")Z", client, "isActiveAndMatches");
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
        method("net/minecraft/client/renderer/color/BlockColors", "func_186722_a",
                "(Lnet/minecraft/client/renderer/color/IBlockColor;[Lnet/minecraft/block/Block;)V", client, "registerBlockColor");
        // 1.16's graphics setting (fast, fancy, fabulous): 1.12's fancy graphics switch.
        field("net/minecraft/client/GameSettings", "field_238330_f_", "Lnet/minecraft/client/settings/GraphicsFanciness;",
                GLUE + "RenderGlue.graphics(Lnet/minecraft/client/GameSettings;)Lnet/minecraft/client/settings/GraphicsFanciness;");
        // 1.16's colour of a block's tint index; 1.12 names it otherwise (and Forge keeps its colours by block, not id).
        method("net/minecraft/client/renderer/color/BlockColors", "func_228054_a_",
                "(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/util/math/BlockPos;I)I", SHIMS116 + "ClientShims116", "blockColor");
        // The language in use: 1.12 has none until its language list has loaded.
        method("net/minecraft/client/resources/LanguageManager", "func_135041_c", "()Lnet/minecraft/client/resources/Language;", SHIMS116 + "ClientShims116", "currentLanguage");
        // 1.16's biome colours (blended per position): 1.12's.
        String biomeColors = "(Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/util/math/BlockPos;)I";
        method("net/minecraft/world/biome/BiomeColors", "func_228358_a_", biomeColors, SHIMS116 + "ClientShims116", "grassColor", biomeColors);
        method("net/minecraft/world/biome/BiomeColors", "func_228361_b_", biomeColors, SHIMS116 + "ClientShims116", "foliageColor", biomeColors);
        method("net/minecraft/world/biome/BiomeColors", "func_228363_c_", biomeColors, SHIMS116 + "ClientShims116", "waterColor", biomeColors);
        // 1.16's redstone colour by power (its table isn't built here): 1.12's, the same formula.
        method("net/minecraft/block/RedstoneWireBlock", "func_235550_b_", "(I)I", SHIMS116 + "BlockShims116", "redstoneColor");
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
        constructor("net/minecraft/item/ItemStack", "(Lnet/minecraft/util/IItemProvider;ILnet/minecraft/nbt/CompoundNBT;)V",
                "(Lnet/minecraft/item/Item;IILnet/minecraft/nbt/NBTTagCompound;)V", arg(0, stackItem), arg(1), arg(0, stackMeta), arg(2));
        // The same as factories, for constructor references.
        String stacks = SHIMS116 + "ItemShims116.stack(Lnet/minecraft/util/IItemProvider;";
        factory("net/minecraft/item/ItemStack", "(Lnet/minecraft/util/IItemProvider;)V", stacks + ")Lnet/minecraft/item/ItemStack;");
        factory("net/minecraft/item/ItemStack", "(Lnet/minecraft/util/IItemProvider;I)V", stacks + "I)Lnet/minecraft/item/ItemStack;");
        factory("net/minecraft/item/ItemStack", "(Lnet/minecraft/util/IItemProvider;ILnet/minecraft/nbt/CompoundNBT;)V",
                stacks + "ILnet/minecraft/nbt/CompoundNBT;)Lnet/minecraft/item/ItemStack;");
        method("net/minecraft/block/BlockState", "func_177230_c", "()Lnet/minecraft/block/Block;", flat, "getBlock");
        method("net/minecraft/item/ItemStack", "func_77973_b", "()Lnet/minecraft/item/Item;", flat, "getItem");
        method("net/minecraft/item/Item", "func_150898_a", "(Lnet/minecraft/block/Block;)Lnet/minecraft/item/Item;", flat, "itemFromBlock");
        method("net/minecraft/block/Block", "func_149634_a", "(Lnet/minecraft/item/Item;)Lnet/minecraft/block/Block;", flat, "blockFromItem");
        method("net/minecraft/entity/Entity", "func_200600_R", "()Lnet/minecraft/entity/EntityType;", "com/michaelsebero/portium/modern/ModernEntityTypes", "typeOf");
        // LWJGL 3's capabilities are LWJGL 2's on Forge; under Cleanroom they're LWJGL 3's own.
        if (!com.michaelsebero.portium.compat.Cleanroom.LWJGL3) {
            method("org/lwjgl/opengl/GL", "getCapabilities", "()Lorg/lwjgl/opengl/GLCapabilities;", "org/lwjgl/opengl/GLContext", "getCapabilities");
            method("org/lwjgl/opengl/GL", "createCapabilities", "()Lorg/lwjgl/opengl/GLCapabilities;", "org/lwjgl/opengl/GLContext", "getCapabilities");
        }
        // Instanced drawing is told unavailable: 1.16 mods draw instances through 1.16's render pipeline, which Portium
        // doesn't have, so they take their plain path instead (Create's tile entity renderers rather than Flywheel's).
        String instancing = SHIMS116 + "RenderShims116.noInstancing(Ljava/lang/Object;)Z";
        field("org/lwjgl/opengl/GLCapabilities", "OpenGL33", "Z", instancing);
        field("org/lwjgl/opengl/GLCapabilities", "GL_ARB_instanced_arrays", "Z", instancing);
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
        // Forge 1.16's other key binding constructors, which take an input (or its kind and code) and a modifier.
        String keyShims = SHIMS + "client/ClientShims.keyBinding";
        String keyBinding = "Lnet/minecraft/client/settings/KeyBinding;";
        String string = "Ljava/lang/String;";
        String conflict = "Lnet/minecraftforge/client/settings/IKeyConflictContext;";
        String modifier = "Lnet/minecraftforge/client/settings/KeyModifier;";
        String keyInput = "Lnet/minecraft/client/util/InputMappings$Input;";
        String keyType = "Lnet/minecraft/client/util/InputMappings$Type;";
        for (String arguments : new String[] {string + conflict + modifier + keyInput + string, string + conflict + keyInput + string,
                string + conflict + modifier + keyType + "I" + string, string + conflict + keyType + "I" + string, string + keyType + "I" + string}) {
            factory("net/minecraft/client/settings/KeyBinding", "(" + arguments + ")V", keyShims + "(" + arguments + ")" + keyBinding);
        }
        // 1.16's plain inventory of a size (1.12's takes a name and whether it's a custom one as well).
        constructor("net/minecraft/inventory/Inventory", "(I)V", "(Ljava/lang/String;ZI)V",
                arg(0, misc116Class() + ".inventoryName(I)Ljava/lang/String;"), arg(0, misc116Class() + ".notCustom(I)Z"), arg(0));
        // 1.16's effect kind and colour, which Portium's effect adapter takes (the kind loosely: 1.12 has no such class).
        constructor("net/minecraft/potion/Effect", "(Lnet/minecraft/potion/EffectType;I)V", "(Ljava/lang/Object;I)V", arg(0), arg(1));
        // 1.16's effect instance with whether its icon shows (1.12 always shows it).
        constructor("net/minecraft/potion/EffectInstance", "(Lnet/minecraft/potion/Effect;IIZZZ)V", "(Lnet/minecraft/potion/Potion;IIZZ)V",
                arg(0), arg(1), arg(2), arg(3), arg(4));
        // 1.16 makes a chunk position from its packed long.
        String math116 = SHIMS116 + "MathShims116.";
        constructor("net/minecraft/util/math/ChunkPos", "(J)V", "(II)V", arg(0, math116 + "chunkX(J)I"), arg(0, math116 + "chunkZ(J)I"));
        // 1.16's hover events hold any value; 1.12's hold text.
        constructor("net/minecraft/util/text/event/HoverEvent", "(Lnet/minecraft/util/text/event/HoverEvent$Action;Ljava/lang/Object;)V",
                "(Lnet/minecraft/util/text/event/HoverEvent$Action;Lnet/minecraft/util/text/ITextComponent;)V",
                arg(0), arg(1, misc116Class() + ".hoverValue(Ljava/lang/Object;)Lnet/minecraft/util/text/ITextComponent;"));
        // 1.16's particle factories, by type; 1.12's particle manager has its own by number.
        String particles116 = "com/michaelsebero/portium/client/ParticleRendering116";
        method("net/minecraft/client/particle/ParticleManager", "func_199283_a", "(Lnet/minecraft/particles/ParticleType;Lnet/minecraft/client/particle/IParticleFactory;)V",
                particles116, "register", "(Ljava/lang/Object;Lnet/minecraft/particles/ParticleType;Ljava/lang/Object;)V");
        method("net/minecraft/client/particle/ParticleManager", "func_215234_a",
                "(Lnet/minecraft/particles/ParticleType;Lnet/minecraft/client/particle/ParticleManager$IParticleMetaFactory;)V",
                particles116, "registerSprites", "(Ljava/lang/Object;Lnet/minecraft/particles/ParticleType;Ljava/lang/Object;)V");
        method("net/minecraft/client/particle/ParticleManager", "func_199280_a", "(Lnet/minecraft/particles/IParticleData;DDDDDD)Lnet/minecraft/client/particle/Particle;",
                particles116, "add", "(Ljava/lang/Object;Lnet/minecraft/particles/IParticleData;DDDDDD)Lnet/minecraft/client/particle/Particle;");
        // 1.16's particle sprites are in 1.12's block atlas (ParticleRendering116).
        field("net/minecraft/client/renderer/texture/AtlasTexture", "field_215262_g", "Lnet/minecraft/util/ResourceLocation;",
                SHIMS116 + "ClientShims116.particleAtlas()Lnet/minecraft/util/ResourceLocation;");
        // 1.16's particle types are named by their registry names, and read their data with 1.16's reader.
        constructor("net/minecraft/particles/ParticleType", "(ZLnet/minecraft/particles/IParticleData$IDeserializer;)V", "(ZLjava/lang/Object;)V",
                arg(0), arg(1));
        // Block states made directly (as markers): 1.12 states are an interface's.
        factory("net/minecraft/block/BlockState", "(Lnet/minecraft/block/Block;Lcom/google/common/collect/ImmutableMap;Lcom/mojang/serialization/MapCodec;)V",
                SHIMS116 + "StateShims116.newState(Lnet/minecraft/block/Block;Lcom/google/common/collect/ImmutableMap;Ljava/lang/Object;)Lnet/minecraft/block/BlockState;");
        // A box between two points, which 1.12 makes on clients only.
        factory("net/minecraft/util/math/AxisAlignedBB", "(Lnet/minecraft/util/math/vector/Vector3d;Lnet/minecraft/util/math/vector/Vector3d;)V",
                misc116Class() + ".box(Lnet/minecraft/util/math/vector/Vector3d;Lnet/minecraft/util/math/vector/Vector3d;)Lnet/minecraft/util/math/AxisAlignedBB;");
        // 1.16's ingredients keep item lists and tags, read when asked: 1.12 ingredients that do the same.
        ingredients();
        recipes();
        nbtLists();
        forgeSelves();
        // A mod's own chunk status (1.12 has no chunk steps).
        String status = "Lnet/minecraft/world/chunk/ChunkStatus;";
        String statusArguments = "(Ljava/lang/String;" + status + "ILjava/util/EnumSet;Lnet/minecraft/world/chunk/ChunkStatus$Type;"
                + "Lnet/minecraft/world/chunk/ChunkStatus$IGenerationWorker;Lnet/minecraft/world/chunk/ChunkStatus$ILoadingWorker;)";
        factory("net/minecraft/world/chunk/ChunkStatus", statusArguments + "V", GLUE + "ChunkGlue.status(Ljava/lang/String;" + status
                + "ILjava/util/EnumSet;Lnet/minecraft/world/chunk/ChunkStatus$Type;Ljava/lang/Object;Ljava/lang/Object;)" + status);
        // 1.16's vertex formats are made whole; 1.12's an element at a time.
        factory("net/minecraft/client/renderer/vertex/VertexFormat", "(Lcom/google/common/collect/ImmutableList;)V",
                SHIMS116 + "RenderShims116.vertexFormat(Lcom/google/common/collect/ImmutableList;)Lnet/minecraft/client/renderer/vertex/VertexFormat;");
        method("net/minecraft/client/renderer/vertex/VertexFormat", "func_227894_c_", "()Lcom/google/common/collect/ImmutableList;", SHIMS116 + "RenderShims116",
                "elements");
        method("net/minecraft/client/renderer/vertex/VertexFormat", "getOffset", "(I)I", SHIMS116 + "RenderShims116", "offset");
        // 1.16's frame buffers take whether to clear on macOS; 1.12's don't.
        constructor("net/minecraft/client/shader/Framebuffer", "(IIZZ)V", "(IIZ)V", arg(0), arg(1), arg(2));
        // 1.16's buttons take a press action (a class of Minecraft 1.16.5's own), taken as an object.
        String button = "net/minecraft/client/gui/widget/button/Button";
        String buttonStart = "(IIIILnet/minecraft/util/text/ITextComponent;";
        constructor(button, buttonStart + "L" + button + "$IPressable;)V", buttonStart + "Ljava/lang/Object;)V", arg(0), arg(1), arg(2), arg(3), arg(4), arg(5));
        constructor(button, buttonStart + "L" + button + "$IPressable;L" + button + "$ITooltip;)V", buttonStart + "Ljava/lang/Object;Ljava/lang/Object;)V",
                arg(0), arg(1), arg(2), arg(3), arg(4), arg(5), arg(6));
        // 1.16's tessellator of the default size.
        // 1.16's tessellator is its own (ModernMappings' native classes), so it's made as 1.16 makes it.
        // 1.16's translated text without format arguments.
        constructor("net/minecraft/util/text/TranslationTextComponent", "(Ljava/lang/String;)V", "(Ljava/lang/String;[Ljava/lang/Object;)V",
                arg(0), arg(0, misc116Class() + ".noArgs(Ljava/lang/String;)[Ljava/lang/Object;"));
        // 1.16's ranged attributes have no parent.
        constructor("net/minecraft/entity/ai/attributes/RangedAttribute", "(Ljava/lang/String;DDD)V",
                "(Lnet/minecraft/entity/ai/attributes/IAttribute;Ljava/lang/String;DDD)V",
                arg(0, misc116Class() + ".noParent(Ljava/lang/String;)Lnet/minecraft/entity/ai/attributes/IAttribute;"), arg(0), arg(1), arg(2), arg(3));
        // 1.16's attribute modifiers take an operation enum; 1.12's take its number.
        String operation = misc116Class() + ".operation(Ljava/lang/Object;)I";
        String modifierOperation = "Lnet/minecraft/entity/ai/attributes/AttributeModifier$Operation;";
        constructor("net/minecraft/entity/ai/attributes/AttributeModifier", "(Ljava/util/UUID;Ljava/lang/String;D" + modifierOperation + ")V",
                "(Ljava/util/UUID;Ljava/lang/String;DI)V", arg(0), arg(1), arg(2), arg(3, operation));
        constructor("net/minecraft/entity/ai/attributes/AttributeModifier", "(Ljava/lang/String;D" + modifierOperation + ")V",
                "(Ljava/lang/String;DI)V", arg(0), arg(1), arg(2, operation));
        // 1.16's dynamic textures may skip clearing their pixels; 1.12's always start clear.
        constructor("net/minecraft/client/renderer/texture/DynamicTexture", "(IIZ)V", "(II)V", arg(0), arg(1));
        // Forge 1.13 takes index -1 as the next free tab; 1.12 has a constructor without an index for that.
        constructor("net/minecraft/item/ItemGroup", "(ILjava/lang/String;)V", "(ILjava/lang/String;)V", arg(0, ITEMS + ".groupIndex(I)I"), arg(1));
    }

    private static String misc116Class() {
        return SHIMS116 + "MiscShims116";
    }

    private static Argument arg(int from) {
        return new Argument(from, null);
    }

    private static Argument arg(int from, String converter) {
        return new Argument(from, converter);
    }

    /**
     * 1.16's recipe methods that 1.12's recipe interface (the one both have) lacks or declares for crafting grids only,
     * by 1.16 names: to the recipe's own class, or 1.12's crafting recipes.
     */
    private void recipes() {
        String recipe = "net/minecraft/item/crafting/IRecipe";
        String glue = GLUE + "RecipeGlue";
        String inventory = "Lnet/minecraft/inventory/IInventory;";
        method(recipe, "func_222127_g", "()Lnet/minecraft/item/crafting/IRecipeType;", glue, "getType");
        method(recipe, "func_199560_c", "()Lnet/minecraft/util/ResourceLocation;", glue, "getId");
        method(recipe, "func_199559_b", "()Lnet/minecraft/item/crafting/IRecipeSerializer;", glue, "getSerializer");
        method(recipe, "func_222128_h", "()Lnet/minecraft/item/ItemStack;", glue, "getIcon");
        method(recipe, "func_77569_a", "(" + inventory + "Lnet/minecraft/world/World;)Z", glue, "matches");
        method(recipe, "func_77572_b", "(" + inventory + ")Lnet/minecraft/item/ItemStack;", glue, "getCraftingResult");
        method(recipe, "func_179532_b", "(" + inventory + ")Lnet/minecraft/util/NonNullList;", glue, "getRemainingItems");
    }

    /**
     * A development aid, run with {@code -Dportium.dev.redirects=<file>}: writes every method and field redirect as
     * {@code kind declarer name desc}, by 1.16 names, so a script can check each against Minecraft and Forge 1.16.5
     * (a redirect naming a class or descriptor 1.16 lacks never applies).
     */
    void dump(java.io.File file) throws java.io.IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, List<Redirect>> entry : this.methods.entrySet()) {
            int paren = entry.getKey().indexOf('(');
            for (Redirect redirect : entry.getValue()) {
                lines.add("method " + redirect.declarer + " " + entry.getKey().substring(0, paren) + " " + entry.getKey().substring(paren));
            }
        }
        for (Map.Entry<String, List<FieldRedirect>> entry : this.fields.entrySet()) {
            for (FieldRedirect redirect : entry.getValue()) lines.add("field " + redirect.declarer + " " + entry.getKey());
        }
        java.util.Collections.sort(lines);
        java.nio.file.Files.write(file.toPath(), lines, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Forge 1.16's {@code IForge*} self accessors ({@code block.getBlock()}), and 1.16's light lookups for rendering. */
    private void forgeSelves() {
        String misc = SHIMS116 + "MiscShims116";
        method("net/minecraft/block/Block", "getBlock", "()Lnet/minecraft/block/Block;", misc, "self");
        method("net/minecraft/block/BlockState", "getBlockState", "()Lnet/minecraft/block/BlockState;", misc, "self");
        method("net/minecraft/item/Item", "getItem", "()Lnet/minecraft/item/Item;", misc, "self");
        method("net/minecraft/item/ItemStack", "getStack", "()Lnet/minecraft/item/ItemStack;", misc, "self");
        method("net/minecraft/tileentity/TileEntity", "getTileEntity", "()Lnet/minecraft/tileentity/TileEntity;", misc, "self");
        method("net/minecraft/entity/Entity", "getEntity", "()Lnet/minecraft/entity/Entity;", misc, "self");
        // Forge 1.16's tags of a block or item, by name.
        method("net/minecraft/block/Block", "getTags", "()Ljava/util/Set;", SHIMS116 + "TagShims116", "blockTags");
        method("net/minecraft/item/Item", "getTags", "()Ljava/util/Set;", SHIMS116 + "TagShims116", "itemTags");
        String renderer = "net/minecraft/client/renderer/WorldRenderer";
        String reader = "Lnet/minecraft/world/IBlockDisplayReader;";
        String pos = "Lnet/minecraft/util/math/BlockPos;";
        // Flywheel (the render library under Create): its GPU backend is never available here, so its mods draw without.
        String flywheel = "com/jozufozu/flywheel/backend/Backend";
        for (String check : new String[] {"available", "canUseVBOs", "canUseInstancing"}) {
            method(flywheel, check, "()Z", SHIMS116 + "RenderShims116", "flywheelBackend", "(Ljava/lang/Object;)Z");
        }
        method(flywheel, "canUseInstancing", "(Lnet/minecraft/world/World;)Z", SHIMS116 + "RenderShims116", "flywheelBackend",
                "(Ljava/lang/Object;Ljava/lang/Object;)Z");
        // LWJGL 3 calls whose LWJGL 2 namesakes take other arguments (render libraries make textures and map buffers).
        for (String gl : new String[] {"org/lwjgl/opengl/GL12", "org/lwjgl/opengl/GL13", "org/lwjgl/opengl/GL14", "org/lwjgl/opengl/GL15",
                "org/lwjgl/opengl/GL20", "org/lwjgl/opengl/GL21", "org/lwjgl/opengl/GL30", "org/lwjgl/opengl/GL31", "org/lwjgl/opengl/GL32",
                "org/lwjgl/opengl/GL33"}) {
            method(gl, "glTexImage3D", "(IIIIIIIIIJ)V", SHIMS116 + "RenderShims116", "texImage3D");
        }
        for (String gl : new String[] {"org/lwjgl/opengl/GL30", "org/lwjgl/opengl/GL31", "org/lwjgl/opengl/GL32", "org/lwjgl/opengl/GL33",
                "org/lwjgl/opengl/ARBMapBufferRange"}) {
            method(gl, "glMapBufferRange", "(IJJI)Ljava/nio/ByteBuffer;", SHIMS116 + "RenderShims116", "mapBufferRange");
        }
        method("net/minecraft/client/renderer/culling/ClippingHelper", "func_228957_a_", "(Lnet/minecraft/util/math/AxisAlignedBB;)Z",
                SHIMS116 + "RenderShims116", "isVisible");
        method(renderer, "func_228421_a_", "(" + reader + pos + ")I", SHIMS116 + "RenderShims116", "combinedLight");
        method(renderer, "func_228420_a_", "(" + reader + "Lnet/minecraft/block/BlockState;" + pos + ")I", SHIMS116 + "RenderShims116", "packedLight");
    }

    /** 1.16's {@code AbstractGui} drawing methods and blit offset, by 1.16 names: Portium's drawing on 1.12's tessellator. */
    private void guiDrawing() {
        String gui = "net/minecraft/client/gui/AbstractGui";
        String self = "L" + gui + ";";
        String stack = "Lcom/mojang/blaze3d/matrix/MatrixStack;";
        String font = "Lnet/minecraft/client/gui/FontRenderer;";
        String text = "Lnet/minecraft/util/text/ITextComponent;";
        String shims = "com/michaelsebero/portium/mods116/adapter/GuiDrawing116";
        String loose = "Ljava/lang/Object;";
        method(gui, "func_230927_p_", "()I", shims, "getBlitOffset");
        method(gui, "func_230926_e_", "(I)V", shims, "setBlitOffset");
        field(gui, "field_230662_a_", "I", shims + ".getBlitOffset(" + self + ")I", shims + ".setBlitOffset(" + self + "I)V");
        method(gui, "func_238467_a_", "(" + stack + "IIIII)V", shims, "fill", "(" + loose + "IIIII)V");
        method(gui, "func_238465_a_", "(" + stack + "IIII)V", shims, "hLine", "(" + self + loose + "IIII)V");
        method(gui, "func_238473_b_", "(" + stack + "IIII)V", shims, "vLine", "(" + self + loose + "IIII)V");
        method(gui, "func_238468_a_", "(" + stack + "IIIIII)V", shims, "fillGradient", "(" + self + loose + "IIIIII)V");
        method(gui, "func_238471_a_", "(" + stack + font + "Ljava/lang/String;III)V", shims, "drawCenteredString", "(" + loose + font + "Ljava/lang/String;III)V");
        method(gui, "func_238472_a_", "(" + stack + font + text + "III)V", shims, "drawCenteredString", "(" + loose + font + text + "III)V");
        method(gui, "func_238476_c_", "(" + stack + font + "Ljava/lang/String;III)V", shims, "drawString", "(" + loose + font + "Ljava/lang/String;III)V");
        method(gui, "func_238475_b_", "(" + stack + font + text + "III)V", shims, "drawString", "(" + loose + font + text + "III)V");
        method(gui, "func_238474_b_", "(" + stack + "IIIIII)V", shims, "blit", "(" + self + loose + "IIIIII)V");
        method(gui, "func_238464_a_", "(" + stack + "IIIFFIIII)V", shims, "blit", "(" + loose + "IIIFFIIII)V");
        method(gui, "func_238466_a_", "(" + stack + "IIIIFFIIII)V", shims, "blit", "(" + loose + "IIIIFFIIII)V");
        method(gui, "func_238463_a_", "(" + stack + "IIFFIIII)V", shims, "blit", "(" + loose + "IIFFIIII)V");
        method(gui, "func_238470_a_", "(" + stack + "IIIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V", shims, "blit",
                "(" + loose + "IIIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V");
    }

    /**
     * 1.16's font, character manager and text properties, and Forge 1.16's GUI utilities: Portium's text drawing on
     * 1.12's font, where every 1.16 text (properties, reordered sequences) is a 1.12 text component.
     */
    private void fontDrawing() {
        String font = "net/minecraft/client/gui/FontRenderer";
        String self = "L" + font + ";";
        String stack = "Lcom/mojang/blaze3d/matrix/MatrixStack;";
        String component = "Lnet/minecraft/util/text/ITextComponent;";
        String properties = "Lnet/minecraft/util/text/ITextProperties;";
        String reordering = "Lnet/minecraft/util/IReorderingProcessor;";
        String style = "Lnet/minecraft/util/text/Style;";
        String world = "Lnet/minecraft/util/math/vector/Matrix4f;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ZII)I";
        String loose = "Ljava/lang/Object;";
        String string = "Ljava/lang/String;";
        String shims = "com/michaelsebero/portium/mods116/adapter/FontShims116";
        method(font, "func_238421_b_", "(" + stack + string + "FFI)I", shims, "draw", "(" + self + loose + string + "FFI)I");
        method(font, "func_238405_a_", "(" + stack + string + "FFI)I", shims, "drawShadow", "(" + self + loose + string + "FFI)I");
        method(font, "func_238406_a_", "(" + stack + string + "FFIZ)I", shims, "drawShadow", "(" + self + loose + string + "FFIZ)I");
        for (String text : new String[] {component, reordering}) {
            String name = text.equals(component) ? "func_243248_b" : "func_238422_b_";
            String shadowName = text.equals(component) ? "func_243246_a" : "func_238407_a_";
            method(font, name, "(" + stack + text + "FFI)I", shims, "draw", "(" + self + loose + loose + "FFI)I");
            method(font, shadowName, "(" + stack + text + "FFI)I", shims, "drawShadow", "(" + self + loose + loose + "FFI)I");
        }
        method(font, "func_228079_a_", "(" + string + "FFIZ" + world, shims, "drawInWorld", "(" + self + string + "FFIZ" + loose + loose + "ZII)I");
        method(font, "func_243247_a", "(" + component + "FFIZ" + world, shims, "drawInWorld", "(" + self + loose + "FFIZ" + loose + loose + "ZII)I");
        method(font, "func_238416_a_", "(" + reordering + "FFIZ" + world, shims, "drawInWorld", "(" + self + loose + "FFIZ" + loose + loose + "ZII)I");
        method(font, "func_238414_a_", "(" + properties + ")I", shims, "width", "(" + self + loose + ")I");
        method(font, "func_243245_a", "(" + reordering + ")I", shims, "width", "(" + self + loose + ")I");
        method(font, "func_238417_a_", "(" + properties + "I)" + properties, shims, "trim", "(" + self + loose + "I)" + loose);
        method(font, "func_238412_a_", "(" + string + "I)" + string, shims, "trim");
        method(font, "func_238413_a_", "(" + string + "IZ)" + string, shims, "trim");
        method(font, "func_238425_b_", "(" + properties + "I)Ljava/util/List;", shims, "wrap", "(" + self + loose + "I)Ljava/util/List;");
        method(font, "func_238418_a_", "(" + properties + "IIII)V", shims, "drawWrapped", "(" + self + loose + "IIII)V");
        method(font, "func_238420_b_", "()Lnet/minecraft/util/text/CharacterManager;", shims, "characterManager", "(" + self + ")" + loose);
        method(font, "func_147647_b", "(" + string + ")" + string, shims, "bidiReorder");
        // Font sets and their glyphs: 1.12 draws 1.16 text itself, so a mod's flush of the white glyph's layer after
        // drawing (Create's nixie tubes) gets stand-ins and flushes the solid layer.
        String fonts = "net/minecraft/client/gui/fonts/";
        method(font, "func_238419_a_", "(Lnet/minecraft/util/ResourceLocation;)L" + fonts + "Font;", GLUE + "RenderGlue", "fontSet",
                "(L" + font + ";Lnet/minecraft/util/ResourceLocation;)L" + fonts + "Font;");
        method(fonts + "Font", "func_228157_b_", "()L" + fonts + "TexturedGlyph;", GLUE + "RenderGlue", "whiteGlyph",
                "(L" + fonts + "Font;)L" + fonts + "TexturedGlyph;");
        method(fonts + "TexturedGlyph", "func_228163_a_", "(Z)Lnet/minecraft/client/renderer/RenderType;", GLUE + "RenderGlue", "glyphType",
                "(L" + fonts + "TexturedGlyph;Z)Lnet/minecraft/client/renderer/RenderType;");
        // The character manager is the font (see FontShims116.characterManager), taken loosely.
        String manager = "net/minecraft/util/text/CharacterManager";
        method(manager, "func_238350_a_", "(" + string + ")F", shims, "managerWidth", "(" + loose + loose + ")F");
        method(manager, "func_238356_a_", "(" + properties + ")F", shims, "managerWidth", "(" + loose + loose + ")F");
        method(manager, "func_243238_a", "(" + reordering + ")F", shims, "managerWidth", "(" + loose + loose + ")F");
        method(manager, "func_238352_a_", "(" + string + "I" + style + ")I", shims, "managerFitting", "(" + loose + string + "I" + loose + ")I");
        method(manager, "func_238361_b_", "(" + string + "I" + style + ")" + string, shims, "managerTrim", "(" + loose + string + "I" + loose + ")" + string);
        method(manager, "func_238364_c_", "(" + string + "I" + style + ")" + string, shims, "managerTrimEnd", "(" + loose + string + "I" + loose + ")" + string);
        method(manager, "func_238358_a_", "(" + properties + "I" + style + ")" + properties, shims, "managerTrim", "(" + loose + loose + "I" + loose + ")" + loose);
        method(manager, "func_238365_g_", "(" + string + "I" + style + ")Ljava/util/List;", shims, "wrapLines", "(" + loose + string + "I" + loose + ")Ljava/util/List;");
        method(manager, "func_238362_b_", "(" + properties + "I" + style + ")Ljava/util/List;", shims, "wrapLines", "(" + loose + loose + "I" + loose + ")Ljava/util/List;");
        // Text properties and reordered sequences are 1.12's components.
        String texts = SHIMS116 + "TextShims116";
        method("net/minecraft/util/text/ITextProperties", "getString", "()" + string, texts, "string", "(" + loose + ")" + string);
        method("net/minecraft/util/text/ITextProperties", "func_240652_a_", "(" + string + ")" + properties, texts, "plain", "(" + string + ")" + loose);
        method("net/minecraft/util/text/ITextComponent", "func_241878_f", "()" + reordering, texts, "reordering", "(" + component + ")" + loose);
        field("net/minecraft/util/text/Style", "field_240709_b_", style, texts + ".emptyStyle()" + style);
        // 1.16's fonts by name: 1.12 has the one.
        method("net/minecraft/util/text/Style", "func_240719_a_", "(Lnet/minecraft/util/ResourceLocation;)" + style, texts, "withFont");
        // 1.16's immutable style changes, on copies of 1.12's.
        String style116 = "net/minecraft/util/text/Style";
        String formatting116 = "Lnet/minecraft/util/text/TextFormatting;";
        method(style116, "func_240718_a_", "(Lnet/minecraft/util/text/Color;)" + style, texts, "withColor", "(" + style + "Ljava/lang/Object;)" + style);
        method(style116, "func_240712_a_", "(" + formatting116 + ")" + style, texts, "withFormattingColor");
        method(style116, "func_240713_a_", "(Ljava/lang/Boolean;)" + style, texts, "withBold");
        method(style116, "func_240722_b_", "(Ljava/lang/Boolean;)" + style, texts, "withItalic");
        method(style116, "func_244282_c", "(Ljava/lang/Boolean;)" + style, texts, "withUnderlined");
        method(style116, "func_240715_a_", "(Lnet/minecraft/util/text/event/ClickEvent;)" + style, texts, "withClickEvent");
        method(style116, "func_240714_a_", "(Ljava/lang/String;)" + style, texts, "withInsertion");
        method(style116, "func_240721_b_", "(" + formatting116 + ")" + style, texts, "withFormatting");
        method(style116, "func_240723_c_", "(" + formatting116 + ")" + style, texts, "withFormatting");
        method(style116, "func_240720_a_", "([" + formatting116 + ")" + style, texts, "withFormattings");
        method(style116, "func_240717_a_", "(" + style + ")" + style, texts, "withParent");
        // Forge 1.16's GUI utilities, on Forge 1.12's.
        String utils = "net/minecraftforge/fml/client/gui/GuiUtils";
        String drawing = "com/michaelsebero/portium/mods116/adapter/GuiDrawing116";
        method(utils, "drawGradientRect", "(Lnet/minecraft/util/math/vector/Matrix4f;IIIIIII)V", drawing, "gradientRect", "(" + loose + "IIIIIII)V");
        method(utils, "drawHoveringText", "(" + stack + "Ljava/util/List;IIIII" + self + ")V", drawing, "hoveringText", "(" + loose + "Ljava/util/List;IIIII" + self + ")V");
        method(utils, "drawHoveringText", "(" + stack + "Ljava/util/List;IIIIIIII" + self + ")V", drawing, "hoveringText",
                "(" + loose + "Ljava/util/List;IIIIIIII" + self + ")V");
    }

    /** 1.16's list tag is a Java list; 1.12's has its own methods for the same. By 1.16 names. */
    private void nbtLists() {
        String list = "net/minecraft/nbt/ListNBT";
        String shims = SHIMS116 + "NbtShims116";
        String tag = "Lnet/minecraft/nbt/INBT;";
        method(list, "add", "(Ljava/lang/Object;)Z", shims, "add");
        method(list, "add", "(I" + tag + ")V", shims, "add", "(Lnet/minecraft/nbt/ListNBT;ILjava/lang/Object;)V");
        method(list, "add", "(ILjava/lang/Object;)V", shims, "add", "(Lnet/minecraft/nbt/ListNBT;ILjava/lang/Object;)V");
        method(list, "get", "(I)" + tag, shims, "get");
        method(list, "get", "(I)Ljava/lang/Object;", shims, "get", "(Lnet/minecraft/nbt/ListNBT;I)" + tag);
        method(list, "set", "(I" + tag + ")" + tag, shims, "set");
        method(list, "remove", "(I)" + tag, shims, "remove");
        method(list, "remove", "(I)Ljava/lang/Object;", shims, "remove", "(Lnet/minecraft/nbt/ListNBT;I)" + tag);
        method(list, "size", "()I", shims, "size");
        method(list, "isEmpty", "()Z", shims, "isEmpty");
        method(list, "clear", "()V", shims, "clear");
        method(list, "stream", "()Ljava/util/stream/Stream;", shims, "stream");
        method(list, "contains", "(Ljava/lang/Object;)Z", shims, "contains");
        method(list, "indexOf", "(Ljava/lang/Object;)I", shims, "indexOf");
    }

    /** 1.16's ingredient methods that 1.12's lack (or that read 1.16's fields), by 1.16 names. */
    private void ingredients() {
        String ingredient = "net/minecraft/item/crafting/Ingredient";
        String type = "L" + ingredient + ";";
        String shims = SHIMS116 + "IngredientShims116";
        factory(ingredient, "(Ljava/util/stream/Stream;)V", shims + ".lists(Ljava/util/stream/Stream;)" + type);
        method(ingredient, "func_209357_a", "(Ljava/util/stream/Stream;)" + type, shims, "fromLists");
        method(ingredient, "func_234819_a_", "(Ljava/util/stream/Stream;)" + type, shims, "fromStacks");
        method(ingredient, "func_199804_a", "([Lnet/minecraft/util/IItemProvider;)" + type, shims, "fromItems");
        method(ingredient, "func_199805_a", "(Lnet/minecraft/tags/ITag;)" + type, GLUE + "IngredientGlue", "fromTag");
        method(ingredient, "func_199566_b", "(Lnet/minecraft/network/PacketBuffer;)" + type, shims, "read");
        method(ingredient, "func_199802_a", "(Lcom/google/gson/JsonElement;)" + type, shims, "deserialize");
        method(ingredient, "func_199564_a", "(Lnet/minecraft/network/PacketBuffer;)V", shims, "write");
        method(ingredient, "func_200304_c", "()Lcom/google/gson/JsonElement;", shims, "serialize");
        method(ingredient, "func_203189_d", "()Z", shims, "hasNoMatchingItems");
        method(ingredient, "test", "(Lnet/minecraft/item/ItemStack;)Z", shims, "test");
        // 1.16's recipe readers' keys and results, which 1.12's same-named methods read without tags or 1.16 names.
        method("net/minecraft/item/crafting/ShapedRecipe", "func_192408_a", "(Lcom/google/gson/JsonObject;)Ljava/util/Map;", shims, "shapedKey");
        method("net/minecraft/item/crafting/ShapedRecipe", "func_192407_a", "(Lcom/google/gson/JsonArray;)[Ljava/lang/String;", shims, "pattern");
        method("net/minecraft/item/crafting/ShapedRecipe", "func_199798_a", "(Lcom/google/gson/JsonObject;)Lnet/minecraft/item/ItemStack;", shims, "resultStack");
    }

    /** Forge 1.16's {@code IForgeItemStack} defaults, which 1.12's item stacks lack, by 1.16 names. */
    private void forgeItemStack() {
        String stack = "net/minecraft/item/ItemStack";
        String shims = SHIMS116 + "ItemStackShims116";
        String living = "Lnet/minecraft/entity/LivingEntity;";
        String player = "Lnet/minecraft/entity/player/PlayerEntity;";
        String world = "Lnet/minecraft/world/World;";
        String[][] defaults = {
                {"equals", "(Lnet/minecraft/item/ItemStack;Z)Z"},
                {"areShareTagsEqual", "(Lnet/minecraft/item/ItemStack;)Z"},
                {"getShareTag", "()Lnet/minecraft/nbt/CompoundNBT;"},
                {"readShareTag", "(Lnet/minecraft/nbt/CompoundNBT;)V"},
                {"getContainerItem", "()Lnet/minecraft/item/ItemStack;"},
                {"hasContainerItem", "()Z"},
                {"onItemUseFirst", "(Lnet/minecraft/item/ItemUseContext;)Lnet/minecraft/util/ActionResultType;"},
                {"onBlockStartBreak", "(Lnet/minecraft/util/math/BlockPos;" + player + ")Z"},
                {"shouldCauseBlockBreakReset", "(Lnet/minecraft/item/ItemStack;)Z"},
                {"doesSneakBypassUse", "(Lnet/minecraft/world/IWorldReader;Lnet/minecraft/util/math/BlockPos;" + player + ")Z"},
                {"onEntitySwing", "(" + living + ")Z"},
                {"onUsingTick", "(" + living + "I)V"},
                {"canApplyAtEnchantingTable", "(Lnet/minecraft/enchantment/Enchantment;)Z"},
                {"getItemEnchantability", "()I"},
                {"isBookEnchantable", "(Lnet/minecraft/item/ItemStack;)Z"},
                {"getXpRepairRatio", "()F"},
                {"isRepairable", "()Z"},
                {"getEquipmentSlot", "()Lnet/minecraft/inventory/EquipmentSlotType;"},
                {"canEquip", "(Lnet/minecraft/inventory/EquipmentSlotType;Lnet/minecraft/entity/Entity;)Z"},
                {"canDisableShield", "(Lnet/minecraft/item/ItemStack;" + living + living + ")Z"},
                {"isShield", "(" + living + ")Z"},
                {"onArmorTick", "(" + world + player + ")V"},
                {"onHorseArmorTick", "(" + world + "Lnet/minecraft/entity/MobEntity;)V"},
                {"canElytraFly", "(" + living + ")Z"},
                {"elytraFlightTick", "(" + living + "I)Z"},
                {"isPiglinCurrency", "()Z"},
                {"makesPiglinsNeutral", "(" + living + ")Z"},
                {"isEnderMask", "(" + player + "Lnet/minecraft/entity/monster/EndermanEntity;)Z"},
                {"getEntityLifespan", "(" + world + ")I"},
                {"onEntityItemUpdate", "(Lnet/minecraft/entity/item/ItemEntity;)Z"},
                {"onDroppedByPlayer", "(" + player + ")Z"},
                {"getHighlightTip", "(Lnet/minecraft/util/text/ITextComponent;)Lnet/minecraft/util/text/ITextComponent;"},
        };
        for (String[] method : defaults) method(stack, method[0], method[1], shims, method[0]);
        // By recipe type, which is 1.16's own class here.
        method(stack, "getBurnTime", "(Lnet/minecraft/item/crafting/IRecipeType;)I", shims, "getBurnTime", "(Lnet/minecraft/item/ItemStack;Ljava/lang/Object;)I");
    }

    private void method(String declarer, String name, String desc, String owner, String target) {
        this.methods.computeIfAbsent(name + desc, key -> new ArrayList<>(1)).add(new Redirect(declarer, owner, target));
    }

    /** As {@link #method}, to a stand-in with a descriptor of its own (which must still fit the stack the call leaves). */
    private void method(String declarer, String name, String desc, String owner, String target, String targetDesc) {
        this.methods.computeIfAbsent(name + desc, key -> new ArrayList<>(1)).add(new Redirect(declarer, owner, target, targetDesc));
    }

    /** @param target {@code owner.name desc} of a static method taking the constructor's arguments, by 1.16 names */
    private void factory(String owner, String desc, String target) {
        this.factories.put(owner + desc, target);
    }

    /** The static method that makes what {@code new owner(desc)} does in 1.16 code, or null. */
    @Nullable
    String findFactory(String owner, String desc) {
        return this.factories.get(owner + desc);
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
