package com.michaelsebero.portium.mods116;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How names in 1.13 code become names that exist on 1.12.
 * <ul>
 * <li>Classes 1.12 has under another name are renamed to it: a few listed here, the rest in
 * {@code classes-1.13.2.txt}, which {@code tools/compat113.py} generates by matching the SRG ids classes share.</li>
 * <li>Classes 1.12 lacks are renamed to Portium's stand-ins in {@code modern.mc}, which mirror 1.13's packages below
 * {@code net.minecraft}.</li>
 * <li>Members keep their SRG names, which 1.12 shares for everything that survived into 1.13. In a development
 * environment, where Minecraft runs with MCP names, they're renamed through Forge's SRG-to-MCP table.</li>
 * <li>Members of stand-ins are renamed by their {@link Srg} annotations.</li>
 * <li>Forge 1.13's classes that 1.12 has too, by the same name, are 1.12's: its events, capabilities, registries and
 * so on, which 1.13 mods then use directly. A few are 1.12's under another name ({@link #SHARED}): 1.13's event bus
 * API is 1.12's. The rest, Forge 1.13's own loader among them, are moved under {@link #FORGE}, so they never share a
 * name with a different class of Forge 1.12's: a class's name in a method signature has to mean the same class to
 * the caller and to the class that declares the method. Some classes 1.12 also has are kept as Forge 1.13's
 * ({@link #OWN}) because 1.13's differ too much.</li>
 * </ul>
 * SRG names are unique per method or field (overrides share theirs), so members are renamed by name alone, whatever
 * class the reference goes through.
 */
final class ModernMappings {

    static final String SHIMS = "com/michaelsebero/portium/modern/mc/";
    /** Where classes with {@link Srg} names live: the stand-ins and the adapters. */
    private static final String[] ANNOTATED = {SHIMS, "com/michaelsebero/portium/modern/adapter/", "com/michaelsebero/portium/command/",
            "com/michaelsebero/portium/mods116/adapter/", "com/michaelsebero/portium/client/GlStateManager116"};
    /** Where Forge 1.13's classes live once loaded: {@code forge113/net/minecraftforge/...}. */
    static final String FORGE = "forge116/";
    /**
     * Where Minecraft 1.13.2's own classes live once loaded if 1.12 has a different class by the same name that 1.13
     * code shouldn't get ({@link #NATIVE_CLASSES}); 1.13's classes 1.12 lacks keep their names.
     */
    static final String NATIVE = "mc116/";
    private static final String MINECRAFT = "net/minecraft/";
    private static final String MINECRAFT_FORGE = "net/minecraftforge/";
    private static final Pattern FORGE_NAMES = Pattern.compile("net([/.])minecraftforge[/.][\\w$/.]*");
    private static final String SRG_ANNOTATION = "Lcom/michaelsebero/portium/modern/Srg;";

    /**
     * Marks a class name in code the translator writes before renaming as one of 1.12's already, which renaming must
     * leave alone: 1.12 and 1.13 share some names for different classes (both have an {@code IRegistry}).
     */
    static final String LEGACY = "portium$legacy/";

    /** A 1.12 class name, marked to pass renaming as it is. */
    static String legacy(String internalName) {
        return internalName.startsWith("[") ? legacyDesc(internalName) : LEGACY + internalName;
    }

    /** A descriptor by 1.12's names, its classes marked to pass renaming as they are. */
    static String legacyDesc(String desc) {
        StringBuilder out = new StringBuilder(desc.length() + 32);
        for (int i = 0; i < desc.length(); i++) {
            char c = desc.charAt(i);
            out.append(c);
            if (c == 'L') {
                int end = desc.indexOf(';', i);
                out.append(LEGACY).append(desc, i + 1, end + 1);
                i = end;
            }
        }
        return out.toString();
    }

    /** 1.13 classes that 1.12 has under another name, beyond the generated table or overriding it. */
    private static final Map<String, String> RENAMED = new HashMap<>();

    static {
        // Minecraft.getDataFixer and friends keep their SRG names but return 1.12's own data fixer.
        RENAMED.put("com/mojang/datafixers/DataFixer", "net/minecraft/util/datafix/DataFixer");
        RENAMED.put("net/minecraft/item/ItemBed", "net/minecraft/item/ItemBlock");
        // 1.13 runs on LWJGL 3, 1.12 on LWJGL 2: the same capability flags under another class.
        // (Under Cleanroom, LWJGL 3 is there itself: no mapping.)
        if (!com.michaelsebero.portium.compat.Cleanroom.LWJGL3) RENAMED.put("org/lwjgl/opengl/GLCapabilities", "org/lwjgl/opengl/ContextCapabilities");
        // Portium's 1.13 commands have 1.13's command source.
        RENAMED.put("net/minecraft/command/CommandSource", "com/michaelsebero/portium/command/CommandSource");
        // What 1.13 asks of a world while reading it; 1.12 asks an IBlockAccess (the table would say World).
        RENAMED.put("net/minecraft/world/IWorldReaderBase", "net/minecraft/world/IBlockAccess");
        // Names 1.12 uses for another class: 1.13's item renderer is 1.12's RenderItem (1.12's ItemRenderer is 1.13's
        // FirstPersonRenderer), 1.13's snow block 1.12's BlockSnowBlock (1.12's BlockSnow is 1.13's BlockSnowLayer).
        RENAMED.put("net/minecraft/client/renderer/ItemRenderer", "net/minecraft/client/renderer/RenderItem");
        // 1.16's player renderer, which the generated table misses.
        RENAMED.put("net/minecraft/client/renderer/entity/PlayerRenderer", "net/minecraft/client/renderer/entity/RenderPlayer");
        // 1.16's entity model (a living renderer's): 1.12's model base.
        RENAMED.put("net/minecraft/client/renderer/entity/model/EntityModel", "net/minecraft/client/model/ModelBase");
        RENAMED.put("net/minecraft/block/BlockSnow", "net/minecraft/block/BlockSnowBlock");
        // 1.13's namespaced registries are its IRegistry; 1.12's classes of these names aren't (GameData.getWrapper).
        RENAMED.put("net/minecraft/util/registry/RegistryNamespaced", "com/michaelsebero/portium/modern/mc/util/registry/IRegistry");
        RENAMED.put("net/minecraft/util/registry/RegistryNamespacedDefaultedByKey", "com/michaelsebero/portium/modern/mc/util/registry/IRegistry");
        // The marker for animals (and golems, mobs, villagers) is IAnimals in 1.12.
        RENAMED.put("net/minecraft/entity/passive/IAnimal", "net/minecraft/entity/passive/IAnimals");
        // Data fixer types only describe data to 1.13's fixers, which 1.12 doesn't have.
        RENAMED.put("com/mojang/datafixers/types/Type", "java/lang/Object");

        // 1.16: a block state is a class (with its methods on AbstractBlockState and StateHolder); 1.12's is IBlockState.
        RENAMED.put("net/minecraft/block/BlockState", "net/minecraft/block/state/IBlockState");
        RENAMED.put("net/minecraft/block/AbstractBlock$AbstractBlockState", "net/minecraft/block/state/IBlockState");
        RENAMED.put("net/minecraft/state/StateHolder", "net/minecraft/block/state/IBlockState");
        // 1.16 names for classes Portium stands in for under their 1.13 names.
        RENAMED.put("net/minecraft/block/AbstractBlock$Properties", SHIMS + "block/Block$Properties");
        RENAMED.put("net/minecraft/particles/ParticleTypes", SHIMS + "init/Particles");
        RENAMED.put("net/minecraft/util/registry/Registry", SHIMS + "util/registry/IRegistry");
        // Locked containers' base, which the SRG ids don't tie together (1.16 rewrote it around menus and names).
        RENAMED.put("net/minecraft/tileentity/LockableTileEntity", "net/minecraft/tileentity/TileEntityLockable");
        // 1.16's fluid states are Portium's (1.13's) fluid state interface, whose methods they share; its fluids, Portium's holder.
        RENAMED.put("net/minecraft/fluid/FluidState", SHIMS + "fluid/IFluidState");
        RENAMED.put("net/minecraft/fluid/Fluids", SHIMS + "init/Fluids");
        // 1.16's screens and widgets are 1.12's (mods extending them get Portium's adapters, as in 1.13).
        RENAMED.put("net/minecraft/client/gui/screen/Screen", "net/minecraft/client/gui/GuiScreen");
        RENAMED.put("net/minecraft/client/gui/screen/inventory/ContainerScreen", "net/minecraft/client/gui/inventory/GuiContainer");
        // 1.16's widgets are Portium's 1.16 widgets (1.12's buttons and text fields share no widget class).
        String adapters116 = "com/michaelsebero/portium/mods116/adapter/";
        RENAMED.put("net/minecraft/client/gui/widget/Widget", adapters116 + "Widget116");
        RENAMED.put("net/minecraft/client/gui/widget/button/AbstractButton", adapters116 + "Button116");
        RENAMED.put("net/minecraft/client/gui/widget/button/Button", adapters116 + "Button116");
        RENAMED.put("net/minecraft/client/gui/widget/TextFieldWidget", "net/minecraft/client/gui/GuiTextField");
        // 1.16's GUI base classes: 1.12's (1.16's widgets include text fields, which aren't 1.12 buttons).
        RENAMED.put("net/minecraft/client/gui/AbstractGui", "net/minecraft/client/gui/Gui");
        // Mojang's language interface (its bridge library, which 1.12 lacks) is 1.12's language class to 1.16 code.
        RENAMED.put("com/mojang/bridge/game/Language", "net/minecraft/client/resources/Language");
        // 1.16's formattable text is 1.12's text (whose styles change in place).
        RENAMED.put("net/minecraft/util/text/IFormattableTextComponent", "net/minecraft/util/text/ITextComponent");
        // 1.16's crafting recipe interface adds only a default type to the recipe interface; 1.12's crafting recipes,
        // which 1.16 mods are given as 1.16's (and cast to it), have 1.12's.
        RENAMED.put("net/minecraft/item/crafting/ICraftingRecipe", "net/minecraft/item/crafting/IRecipe");
        // LWJGL 3's memory helpers, which 1.12's LWJGL 2 lacks (render libraries allocate buffers with them).
        RENAMED.put("org/lwjgl/system/MemoryUtil", "com/michaelsebero/portium/mods116/lwjgl/MemoryUtil116");
        RENAMED.put("org/lwjgl/system/MemoryStack", "com/michaelsebero/portium/mods116/lwjgl/MemoryStack116");
        // 1.16's world interfaces, which 1.12's world class covers: like IWorld (World by the generated table), so 1.16 code
        // handing one as another (a generation reader as a world writer) still type-checks.
        for (String world : new String[] {"ICollisionReader", "IBiomeReader", "IDayTimeReader", "IServerWorld", "gen/IWorldGenerationReader",
                "gen/IWorldGenerationBaseReader"}) {
            RENAMED.put("net/minecraft/world/" + world, "net/minecraft/world/World");
        }
        RENAMED.put("net/minecraft/world/IBlockDisplayReader", "net/minecraft/world/IBlockAccess");
        // 1.16's registry classes are all Portium's registry stand-in to 1.16 code.
        for (String registry : new String[] {"DefaultedRegistry", "SimpleRegistry", "MutableRegistry"}) {
            RENAMED.put("net/minecraft/util/registry/" + registry, SHIMS + "util/registry/IRegistry");
        }
        // 1.16's potion is 1.12's potion type (1.12's potion is 1.16's effect).
        RENAMED.put("net/minecraft/potion/Potion", "net/minecraft/potion/PotionType");
        // 1.16's single-value tags, made by factories the SRG ids don't tie to 1.12's constructors.
        for (String[] tag : new String[][] {{"StringNBT", "NBTTagString"}, {"ByteNBT", "NBTTagByte"}, {"ShortNBT", "NBTTagShort"}, {"IntNBT", "NBTTagInt"},
                {"LongNBT", "NBTTagLong"}, {"FloatNBT", "NBTTagFloat"}, {"DoubleNBT", "NBTTagDouble"}, {"EndNBT", "NBTTagEnd"}}) {
            RENAMED.put("net/minecraft/nbt/" + tag[0], "net/minecraft/nbt/" + tag[1]);
        }
        // 1.16's fluids are Minecraft 1.16.5's own; their blocks are Portium's, not 1.12's water and lava.
        RENAMED.put("net/minecraft/block/FlowingFluidBlock", "com/michaelsebero/portium/mods116/adapter/FlowingFluidBlock116");
        // 1.16's GL state manager can't start here (it needs LWJGL 3); Portium's shares 1.12's state cache. Its enums
        // are 1.12's.
        RENAMED.put("com/mojang/blaze3d/platform/GlStateManager", "com/michaelsebero/portium/client/GlStateManager116");
        for (String inner : new String[] {"SourceFactor", "DestFactor", "FogMode", "LogicOp", "TexGen", "CullFace"}) {
            RENAMED.put("com/mojang/blaze3d/platform/GlStateManager$" + inner, "net/minecraft/client/renderer/GlStateManager$" + inner);
        }
        // 1.16's camera, which 1.12 has as a static helper of another shape: Portium's, in the 1.16 glue.
        RENAMED.put("net/minecraft/client/renderer/ActiveRenderInfo", "com/michaelsebero/portium/mods116/host/ActiveRenderInfo116");
        // Forge 1.16's hand rendering event is Forge 1.12's event for one hand.
        RENAMED.put("net/minecraftforge/client/event/RenderHandEvent", "net/minecraftforge/client/event/RenderSpecificHandEvent");
    }

    /**
     * Minecraft classes 1.12 has, but whose 1.13 versions 1.13 code gets instead: Minecraft 1.13.2's own, moved under
     * {@link #NATIVE} (see {@link ModernNatives}).
     */
    private static final Set<String> NATIVE_CLASSES = new HashSet<>();

    static {
        // 1.16's vertex buffers, run as 1.16's own: they write vertices in 1.16's layouts, which the 1.16 glue hands to
        // 1.12 to draw (RenderBridge116). 1.12's buffer builder isn't a 1.16 vertex builder.
        NATIVE_CLASSES.add("net/minecraft/client/renderer/BufferBuilder");
        NATIVE_CLASSES.add("net/minecraft/client/renderer/Tessellator");
        NATIVE_CLASSES.add("net/minecraft/client/renderer/WorldVertexBufferUploader");
        // A buffer per block render type (mods build their own render buffers on it).
        NATIVE_CLASSES.add("net/minecraft/client/renderer/RegionRenderCacheBuilder");
        // 1.16's palettes, which mods use on their own (Create saves a contraption's blocks with one); 1.12's chunk
        // palettes, which the generated table pairs them with, are built another way.
        NATIVE_CLASSES.add("net/minecraft/util/palette/IPalette");
        NATIVE_CLASSES.add("net/minecraft/util/palette/HashMapPalette");
        NATIVE_CLASSES.add("net/minecraft/util/palette/ArrayPalette");
    }

    /** Forge 1.13 classes that are Forge 1.12's under another name. */
    private static final Map<String, String> SHARED = new HashMap<>();
    /** Forge 1.13 classes, or prefixes of their names, kept as 1.13's although 1.12 has classes so named. */
    private static final String[] OWN = {"net/minecraftforge/fml/common/Mod", "net/minecraftforge/common/MinecraftForge",
            // Forge 1.13's recipe conditions and serializers, which data pack recipes go through.
            "net/minecraftforge/common/crafting/CraftingHelper"};

    static {
        SHARED.put("net/minecraftforge/registries/ForgeRegistryEntry", "net/minecraftforge/registries/IForgeRegistryEntry$Impl");
        String bus = "net/minecraftforge/eventbus/api/";
        String legacyBus = "net/minecraftforge/fml/common/eventhandler/";
        for (String name : new String[] {"Event", "Event$Result", "Event$HasResult", "Cancelable", "EventPriority", "GenericEvent", "IGenericEvent"}) {
            SHARED.put(bus + name, legacyBus + name);
        }
        SHARED.put("net/minecraftforge/fml/LogicalSide", "net/minecraftforge/fml/relauncher/Side");
        SHARED.put("net/minecraftforge/common/extensions/IForgeDimension$SleepResult", "net/minecraft/world/WorldProvider$WorldSleepResult");
    }

    /** {@code classes-1.13.2.txt}: 1.13 classes 1.12 has under another name, found by the SRG ids they share. */
    private final Map<String, String> generated = new HashMap<>();
    private final Set<String> shims = new HashSet<>();
    /** SRG names to runtime names: Forge's SRG-to-MCP table in a development environment, else empty. */
    private final Map<String, String> members = new HashMap<>();
    /** Per stand-in or adapter class, its members' {@link Srg} names to their Java names. */
    private final Map<String, Map<String, String>> annotated = new HashMap<>();
    /** The SRG names of stand-ins' and adapters' methods, by owner, Java name and descriptor. */
    private final Map<String, String> annotatedSrg = new HashMap<>();
    private final Map<String, String> ownedNames = new java.util.concurrent.ConcurrentHashMap<>();
    /** A class's supertypes by translated names, superclass first; set by the class loader. */
    private java.util.function.Function<String, String[]> supertypes = type -> new String[0];
    /** In a development environment, the SRG names of Minecraft's methods and fields by owner, MCP name and descriptor. */
    @Nullable
    private final Map<String, String> srgNames;
    private final Map<String, String> classes = new HashMap<>();
    /** Whether Forge 1.12 has a class, by internal name. */
    private Predicate<String> legacy = name -> false;

    /** @param srgMcp Forge's SRG-to-MCP table, in a development environment; null in production */
    ModernMappings(@Nullable File srgMcp) throws IOException {
        indexShims();
        readGenerated();
        this.srgNames = srgMcp == null ? null : new HashMap<>();
        if (srgMcp != null) readSrgMcp(srgMcp);
    }

    void setLegacyClasses(Predicate<String> legacy) {
        this.legacy = legacy;
    }

    void setSupertypes(java.util.function.Function<String, String[]> supertypes) {
        this.supertypes = supertypes;
    }

    /** The class 1.12 code knows a 1.13 class as. */
    synchronized String mapClass(String name) {
        String mapped = this.classes.get(name);
        if (mapped == null) {
            mapped = RENAMED.get(name);
            if (mapped == null && name.startsWith(MINECRAFT_FORGE)) {
                mapped = SHARED.get(name);
                if (mapped == null) mapped = !isOwn(name) && this.legacy.test(name) ? name : FORGE + name;
            } else if (mapped == null && name.startsWith(MINECRAFT)) {
                String shim = SHIMS + name.substring(MINECRAFT.length());
                if (this.shims.contains(shim)) {
                    mapped = shim;
                } else if (NATIVE_CLASSES.contains(name) || NATIVE_CLASSES.contains(outermost(name))) {
                    mapped = NATIVE + name;
                } else {
                    mapped = this.generated.get(name);
                }
            }
            if (mapped == null) mapped = name;
            this.classes.put(name, mapped);
        }
        return mapped;
    }

    /**
     * Renames the Forge 1.13 class names inside a string constant, internal ({@code net/minecraftforge/...}), as a
     * descriptor or dotted. Forge 1.13 names some classes by string, such as the event bus's listener list.
     */
    String mapString(String text) {
        if (!text.contains("minecraftforge")) return text;
        Matcher matcher = FORGE_NAMES.matcher(text);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            boolean dotted = ".".equals(matcher.group(1));
            String name = dotted ? matcher.group().replace('.', '/') : matcher.group();
            String mapped = mapClass(name);
            matcher.appendReplacement(out, Matcher.quoteReplacement(dotted ? mapped.replace('/', '.') : mapped));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String outermost(String name) {
        int dollar = name.indexOf('$');
        return dollar < 0 ? name : name.substring(0, dollar);
    }

    /**
     * Forge 1.16 packages kept as 1.16's whole: its fluid stacks, attributes and handlers are another API from 1.12's
     * classes of the same names.
     */
    private static final String[] OWN_PACKAGES = {"net/minecraftforge/fluids/"};

    private static boolean isOwn(String name) {
        for (String own : OWN) {
            if (name.equals(own) || name.startsWith(own + "$")) return true;
        }
        for (String own : OWN_PACKAGES) {
            if (name.startsWith(own)) return true;
        }
        return false;
    }

    /**
     * The name a method or field of 1.12's goes by at runtime, given its SRG name: the MCP name in a development
     * environment. Not for members of Portium's stand-ins, which {@link #mapMember(String, String)} finds.
     */
    String mapMember(String name) {
        if (!name.startsWith("func_") && !name.startsWith("field_")) return name;
        String mapped = this.members.get(name);
        return mapped == null ? name : mapped;
    }

    /**
     * The name a method or field goes by at runtime, reached through a class (by 1.13's name): the Java name a
     * stand-in or adapter on the way up gives the SRG name, else {@link #mapMember(String)}. An SRG id is shared by
     * the overrides of a method, so a stand-in's own name only goes for its own class and those below it.
     */
    String mapMember(String owner, String name) {
        if (FORGE_MEMBER_NAMES.contains(name) && owner != null && !owner.startsWith("[")) {
            String renamed = forgeRenamed(mapClass(owner), name, new HashSet<>());
            if (renamed != null) return renamed.startsWith("func_") ? mapMember(renamed) : renamed;
        }
        if (!name.startsWith("func_") && !name.startsWith("field_")) return name;
        if (owner == null || owner.startsWith("[")) return mapMember(name);
        String key = owner + "." + name;
        String known = this.ownedNames.get(key);
        if (known != null) return known;
        String found = annotatedName(mapClass(owner), name, new HashSet<>());
        String mapped = found != null ? found : mapMember(name);
        this.ownedNames.put(key, mapped);
        return mapped;
    }

    /**
     * Members 1.13 names otherwise than 1.12, by class (1.12 name) and 1.13 name: Forge's additions under another name,
     * and methods Minecraft 1.13.2 left unobfuscated (lambdas implement them) that 1.12 has under an SRG id.
     */
    private static final Map<String, String> FORGE_MEMBERS = new HashMap<>();
    private static final Set<String> FORGE_MEMBER_NAMES = new HashSet<>();

    static {
        FORGE_MEMBERS.put("net/minecraft/client/gui/GuiButton.packedFGColor", "packedFGColour");
        FORGE_MEMBERS.put("net/minecraft/client/renderer/color/IItemColor.getColor", "func_186726_a");
        FORGE_MEMBERS.put("net/minecraft/client/renderer/color/IBlockColor.getColor", "func_186720_a");
        FORGE_MEMBERS.put("net/minecraft/dispenser/IBehaviorDispenseItem.dispense", "func_82482_a");
        FORGE_MEMBERS.put("net/minecraft/client/gui/GuiYesNoCallback.confirmResult", "func_73878_a");
        FORGE_MEMBERS.put("net/minecraft/client/resources/SkinManager$SkinAvailableCallback.onSkinTextureAvailable", "func_180521_a");
        FORGE_MEMBERS.put("net/minecraft/util/text/ITextComponent.getString", "func_150260_c");
        for (String key : FORGE_MEMBERS.keySet()) FORGE_MEMBER_NAMES.add(key.substring(key.indexOf('.') + 1));
    }

    @Nullable
    private String forgeRenamed(@Nullable String type, String name, Set<String> seen) {
        if (type == null || !seen.add(type) || type.startsWith("java/")) return null;
        String renamed = FORGE_MEMBERS.get(type + "." + name);
        if (renamed != null) return renamed;
        for (String parent : this.supertypes.apply(type)) {
            String found = forgeRenamed(parent, name, seen);
            if (found != null) return found;
        }
        return null;
    }

    @Nullable
    private String annotatedName(@Nullable String type, String name, Set<String> seen) {
        if (type == null || !seen.add(type) || type.startsWith("java/")) return null;
        Map<String, String> names = this.annotated.get(type);
        if (names != null) {
            String javaName = names.get(name);
            if (javaName != null) return javaName;
        }
        for (String parent : this.supertypes.apply(type)) {
            String found = annotatedName(parent, name, seen);
            if (found != null) return found;
        }
        return null;
    }

    private void readGenerated() throws IOException {
        try (InputStream in = ModernMappings.class.getResourceAsStream("classes-1.16.5.txt")) {
            if (in == null) throw new IOException("classes-1.16.5.txt is missing from Portium");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                int space = line.indexOf(' ');
                this.generated.put(line.substring(0, space), line.substring(space + 1));
            }
        }
    }

    /**
     * The SRG name of one of 1.12's methods, given its declaring class, runtime name and descriptor: the runtime name
     * itself unless Minecraft runs with MCP names; null if it has none (a method Forge or a mod added).
     */
    @Nullable
    /** Whether Portium's class declares this method as a newer one ({@code @Srg}), rather than overriding 1.12's. */
    boolean isAnnotated(String owner, String name, String desc) {
        return this.annotatedSrg.containsKey(owner + "/" + name + desc);
    }

    String srgName(String owner, String name, String desc) {
        String annotation = this.annotatedSrg.get(owner + "/" + name + desc);
        if (annotation != null) return annotation;
        if (this.srgNames == null) return name;
        String srg = this.srgNames.get(owner + "/" + name + desc);
        return srg != null ? srg : name.startsWith("func_") ? name : null;
    }

    /** Reads the stand-ins' {@link Srg} names from Portium's own classes. */
    private void indexShims() throws IOException {
        URL self = ModernMappings.class.getResource("ModernMappings.class");
        if (self == null) throw new IOException("Can't find Portium's classes");
        if ("jar".equals(self.getProtocol())) {
            JarFile jar = ((JarURLConnection) self.openConnection()).getJarFile();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (annotated(entry.getName()) && entry.getName().endsWith(".class")) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        indexShim(in);
                    }
                }
            }
        } else {
            File root;
            try {
                // .../com/michaelsebero/portium/modern/ModernMappings.class -> the classes directory
                root = new File(self.toURI()).getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
            for (String folder : ANNOTATED) {
                File dir = new File(root, folder);
                File single = new File(root, folder + ".class");
                if (dir.isDirectory()) {
                    indexShims(dir);
                } else if (single.isFile()) {
                    try (InputStream in = new FileInputStream(single)) {
                        indexShim(in);
                    }
                }
            }
        }
    }

    private static boolean annotated(String path) {
        for (String folder : ANNOTATED) {
            if (path.startsWith(folder)) return true;
        }
        return false;
    }

    private void indexShims(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                indexShims(file);
            } else if (file.getName().endsWith(".class")) {
                try (InputStream in = new FileInputStream(file)) {
                    indexShim(in);
                }
            }
        }
    }

    private void indexShim(InputStream in) throws IOException {
        new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM5) {

            private String owner;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.owner = name;
                if (name.startsWith(SHIMS)) ModernMappings.this.shims.add(name);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                return new FieldVisitor(Opcodes.ASM5) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        return srg(annotation, owner, name, null);
                    }
                };
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM5) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        return srg(annotation, owner, name, desc);
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    @Nullable
    private AnnotationVisitor srg(String annotation, String owner, String member, @Nullable String desc) {
        if (!SRG_ANNOTATION.equals(annotation)) return null;
        return new AnnotationVisitor(Opcodes.ASM5) {
            @Override
            public void visit(String key, Object value) {
                if (!"value".equals(key)) return;
                ModernMappings.this.annotated.computeIfAbsent(owner, type -> new HashMap<>()).put((String) value, member);
                if (desc != null) ModernMappings.this.annotatedSrg.put(owner + "/" + member + desc, (String) value);
            }
        };
    }

    /** Forge's {@code srg-mcp.srg}: lines like {@code MD: owner/func_1_a (desc) owner/name (desc)}. */
    private void readSrgMcp(File file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                boolean method = line.startsWith("MD: ");
                if (!method && !line.startsWith("FD: ")) continue;
                String[] parts = line.split(" ");
                String srg = parts[1];
                String mcp = parts[method ? 3 : 2];
                String srgName = srg.substring(srg.lastIndexOf('/') + 1);
                this.members.putIfAbsent(srgName, mcp.substring(mcp.lastIndexOf('/') + 1));
                if (method) this.srgNames.put(mcp + parts[4], srgName);
            }
        }
    }
}
