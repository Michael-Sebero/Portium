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
        // 1.16 builds a material from its properties.
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
        // 1.16's state methods, on its state classes (1.12's block state, by Portium's mapping).
        String states116 = SHIMS116 + "StateShims116";
        String property = "Lnet/minecraft/state/Property;";
        for (String holder : new String[] {"net/minecraft/state/StateHolder", "net/minecraft/block/AbstractBlock$AbstractBlockState", "net/minecraft/block/BlockState"}) {
            method(holder, "func_206870_a", "(" + property + "Ljava/lang/Comparable;)Ljava/lang/Object;", states116, "with");
            method(holder, "func_235896_a_", "(" + property + ")Ljava/lang/Object;", states116, "cycle");
            method(holder, "func_235901_b_", "(" + property + ")Z", states116, "hasProperty");
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
        // Forge 1.16's entity renderers by type, and widgets of screens being set up (client).
        method("net/minecraftforge/fml/client/registry/RenderingRegistry", "registerEntityRenderingHandler",
                "(Lnet/minecraft/entity/EntityType;Lnet/minecraftforge/fml/client/registry/IRenderFactory;)V", SHIMS116 + "ClientShims116", "registerEntityRenderer");
        method("net/minecraftforge/client/event/GuiScreenEvent$InitGuiEvent", "getWidgetList", "()Ljava/util/List;", SHIMS116 + "ClientShims116", "widgets");
        method("net/minecraftforge/client/event/GuiScreenEvent$InitGuiEvent", "addWidget", "(Lnet/minecraft/client/gui/widget/Widget;)V", SHIMS116 + "ClientShims116",
                "addWidget");
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
        method("net/minecraft/client/renderer/entity/LivingRenderer", "func_217764_d", "()Lnet/minecraft/client/renderer/entity/model/EntityModel;", entityDrawing, "mainModel");
        String arm = "(" + stack116 + "Lnet/minecraft/client/renderer/IRenderTypeBuffer;ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;)V";
        String armTarget = "(Lnet/minecraft/client/renderer/entity/PlayerRenderer;Ljava/lang/Object;Ljava/lang/Object;ILnet/minecraft/client/entity/player/AbstractClientPlayerEntity;)V";
        method("net/minecraft/client/renderer/entity/PlayerRenderer", "func_229144_a_", arm, entityDrawing, "renderRightArm", armTarget);
        method("net/minecraft/client/renderer/entity/PlayerRenderer", "func_229146_b_", arm, entityDrawing, "renderLeftArm", armTarget);
        method("net/minecraft/client/renderer/model/ModelRenderer", "func_228307_a_", "(" + stack116 + ")V", entityDrawing, "translateRotate",
                "(Lnet/minecraft/client/renderer/model/ModelRenderer;Ljava/lang/Object;)V");
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
        // 1.16's servers and clients are task executors; 1.12's schedule tasks.
        // 1.16 shoots fireworks as projectiles.
        method("net/minecraft/entity/projectile/FireworkRocketEntity", "func_70186_c", "(DDDFF)V", SHIMS116 + "MiscShims116", "shoot");
        // Forge 1.16's shaped recipe size limit.
        method("net/minecraft/item/crafting/ShapedRecipe", "setCraftingSize", "(II)V", SHIMS116 + "MiscShims116", "setCraftingSize");
        method("net/minecraft/util/ObjectIntIdentityMap", "func_195867_b", "(Ljava/lang/Object;)V", SHIMS116 + "MiscShims116", "addIdentity");
        // 1.14's food.
        method("net/minecraft/item/Item$Properties", "func_221540_a", "(Lnet/minecraft/item/Food;)Lnet/minecraft/item/Item$Properties;", GLUE + "ItemGlue", "food");
        method("net/minecraft/item/Item", "func_219971_r", "()Z", SHIMS116 + "ItemShims116", "isFood");
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
                {"func_217344_a", "(IIIIII)Z", "isAreaLoaded"},
                {"func_241211_a_", "(" + bp + "Lnet/minecraft/block/BlockState;II)Z", "setBlockState"},
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
                {"func_217405_b", "(I)Z", "isYOutOfBounds"},
                {"func_217384_a", "(" + player + ent + "Lnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V", "playMovingSound"},
        };
        String[] worldTypes = {"net/minecraft/world/World", "net/minecraft/world/IWorld", "net/minecraft/world/IWorldReader", "net/minecraft/world/IWorldWriter",
                "net/minecraft/world/IEntityReader", "net/minecraft/world/ICollisionReader", "net/minecraft/world/IBiomeReader",
                "net/minecraft/world/IDayTimeReader", "net/minecraft/world/IServerWorld", "net/minecraft/world/ISeedReader", "net/minecraft/world/server/ServerWorld"};
        for (String worldType : worldTypes) {
            for (String[] each : worldMethods) method(worldType, each[0], each[1], worlds116, each[2]);
            // Typed loosely where 1.16's argument is a class of its own (heightmap types, explosion modes).
            method(worldType, "func_201676_a", "(Lnet/minecraft/world/gen/Heightmap$Type;II)I", worlds116, "getHeight", "(Lnet/minecraft/world/World;Ljava/lang/Object;II)I");
            method(worldType, "func_205770_a", "(Lnet/minecraft/world/gen/Heightmap$Type;" + bp + ")" + bp, worlds116, "getHeight",
                    "(Lnet/minecraft/world/World;Ljava/lang/Object;" + bp + ")" + bp);
            method(worldType, "func_217385_a", "(" + ent + "DDDFLnet/minecraft/world/Explosion$Mode;)Lnet/minecraft/world/Explosion;", worlds116, "createExplosion",
                    "(Lnet/minecraft/world/World;" + ent + "DDDFLjava/lang/Object;)Lnet/minecraft/world/Explosion;");
            method(worldType, "func_217398_a", "(" + ent + "DDDFZLnet/minecraft/world/Explosion$Mode;)Lnet/minecraft/world/Explosion;", worlds116, "createExplosion",
                    "(Lnet/minecraft/world/World;" + ent + "DDDFZLjava/lang/Object;)Lnet/minecraft/world/Explosion;");
            // 1.13's, which 1.16 declares on more interfaces.
            method(worldType, "func_201670_d", "()Z", SHIMS + "MiscShims", "isRemote");
            method(worldType, "func_201674_k", "()Ljava/util/Random;", SHIMS + "MiscShims", "getRandom");
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
        method("net/minecraft/item/DyeColor", "getTag", "()" + optionalTag, GLUE + "TagGlue", "dyeTag");
        // 1.16's named tags, read from Portium's tags (see TagGlue); vanilla's tag classes make theirs this way too.
        String namedTag = "Lnet/minecraft/tags/ITag$INamedTag;";
        method("net/minecraft/tags/ItemTags", "func_199901_a", "(Ljava/lang/String;)" + namedTag, GLUE + "TagGlue", "itemWrapper");
        method("net/minecraft/tags/BlockTags", "func_199894_a", "(Ljava/lang/String;)" + namedTag, GLUE + "TagGlue", "blockWrapper");
        method("net/minecraft/tags/FluidTags", "func_206956_a", "(Ljava/lang/String;)" + namedTag, GLUE + "TagGlue", "fluidWrapper");
        method("net/minecraft/tags/EntityTypeTags", "func_232896_a_", "(Ljava/lang/String;)" + namedTag, GLUE + "TagGlue", "entityWrapper");
        // Forge 1.16's bucket fluid.
        method("net/minecraft/item/BucketItem", "getFluid", "()Lnet/minecraft/fluid/Fluid;", GLUE + "FluidGlue", "getFluid");
        method("net/minecraft/block/FlowingFluidBlock", "getFluid", "()Lnet/minecraft/fluid/FlowingFluid;", GLUE + "FluidGlue", "getFluid");
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

        // Fluids: 1.12's liquids, Portium's waterlogging and 1.13 blocks, read as 1.13 fluid states.
        String fluids = SHIMS + "FluidShims";
        String fluidState = "Lnet/minecraft/fluid/FluidState;";
        method("net/minecraft/world/IBlockReader", "func_204610_c", "(" + pos + ")" + fluidState, fluids, "getFluidState");
        method("net/minecraft/block/BlockState", "func_204520_s", "()" + fluidState, fluids, "getFluidState");
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
        method(provider, "getCapability", "(" + capability + "Lnet/minecraft/util/Direction;)" + optional, GLUE + "CapabilityGlue", "getCapability");
        method(provider, "getCapability", "(" + capability + ")" + optional, GLUE + "CapabilityGlue", "getCapability");
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
            method(worldType, "func_234923_W_", "()Lnet/minecraft/util/RegistryKey;", GLUE + "WorldGlue", "dimensionKey");
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
        field("net/minecraft/entity/ai/goal/NearestAttackableTargetGoal", "field_82643_g", "Ljava/util/function/Predicate;",
                misc + ".targetSelector(Lnet/minecraft/entity/ai/goal/NearestAttackableTargetGoal;)Ljava/util/function/Predicate;",
                misc + ".setTargetSelector(Lnet/minecraft/entity/ai/goal/NearestAttackableTargetGoal;Ljava/util/function/Predicate;)V");
        method("net/minecraft/inventory/container/Container", "func_195929_a", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z", misc, "areItemsAndTagsEqual");
        method("net/minecraft/block/ITileEntityProvider", "func_196283_a_", "(Lnet/minecraft/world/IBlockReader;)Lnet/minecraft/tileentity/TileEntity;",
                BLOCKS, "createNewTileEntity");

        // Chunks: heights by kind, fluids and biomes by position.
        String chunk = "net/minecraft/world/chunk/Chunk";
        method(chunk, "func_201576_a", "(Lnet/minecraft/world/gen/Heightmap$Type;II)I", GLUE + "ChunkGlue", "getTopBlockY");
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
        method("net/minecraft/server/MinecraftServer", "func_199529_aN", "()L" + manager + ";", GLUE + "RecipeManager116", "of");
        method(manager, "func_215377_a", "(Lnet/minecraft/util/ResourceLocation;Lcom/google/gson/JsonObject;)Lnet/minecraft/item/crafting/IRecipe;", GLUE + "RecipeManager116",
                "deserialize");

        // Fuel.
        method("net/minecraft/item/ItemStack", "getBurnTime", "()I", misc, "getBurnTime");
        forgeItemStack();
        method("net/minecraftforge/event/ForgeEventFactory", "getItemBurnTime", "(Lnet/minecraft/item/ItemStack;I)I", misc, "getItemBurnTime");

        // Worlds give 1.13 code whole states, as 1.13's worlds keep them.
        method("net/minecraft/world/IBlockReader", "func_180495_p", "(" + pos + ")" + state, BLOCKS, "getActualBlockState");

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
        method("net/minecraft/tileentity/ChestTileEntity", "func_195480_a", "(F)F", tileEntities, "getLidAngle");
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
        method(bakedModel, "func_200117_a", "(" + state + "Lnet/minecraft/util/Direction;Ljava/util/Random;)Ljava/util/List;", client, "getQuads");
        method(bakedModel, "getQuads", "(" + state + "Lnet/minecraft/util/Direction;Ljava/util/Random;Lnet/minecraftforge/client/model/data/IModelData;)Ljava/util/List;",
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
        // 1.16 makes a chunk position from its packed long.
        String math116 = SHIMS116 + "MathShims116.";
        constructor("net/minecraft/util/math/ChunkPos", "(J)V", "(II)V", arg(0, math116 + "chunkX(J)I"), arg(0, math116 + "chunkZ(J)I"));
        // 1.16's hover events hold any value; 1.12's hold text.
        constructor("net/minecraft/util/text/event/HoverEvent", "(Lnet/minecraft/util/text/event/HoverEvent$Action;Ljava/lang/Object;)V",
                "(Lnet/minecraft/util/text/event/HoverEvent$Action;Lnet/minecraft/util/text/ITextComponent;)V",
                arg(0), arg(1, misc116Class() + ".hoverValue(Ljava/lang/Object;)Lnet/minecraft/util/text/ITextComponent;"));
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
