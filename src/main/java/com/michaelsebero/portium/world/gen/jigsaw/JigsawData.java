package com.michaelsebero.portium.world.gen.jigsaw;

import com.cleanroommc.assetmover.AssetMoverAPI;
import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Element;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Feature;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.ListElement;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Single;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawTemplate.Jigsaw;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.16.5's jigsaw structures' data, from the official jar through AssetMover (nothing of it ships with Portium): the
 * pool and processor classes, read into {@link JigsawPools}, and every template those pools name.
 */
public final class JigsawData {

    private static final String VERSION = "1.16.5";
    private static final File ASSET_MOVER_DIR = new File("assetmover");
    private static final String CLASS_DIR = "assets/portium/modern_classes/";
    private static final String TEMPLATE_DIR = "assets/portium116/structures/";

    private static JigsawData instance;

    private final JigsawPools pools;
    private final Map<String, Optional<JigsawTemplate>> templates = new ConcurrentHashMap<>();

    private JigsawData(JigsawPools pools) {
        this.pools = pools;
    }

    /** The data, read on first use; null if the classes couldn't be fetched (no AssetMover). */
    @Nullable
    public static synchronized JigsawData get() {
        if (instance == null) {
            JigsawPools pools = JigsawPools.read(name -> open(new File(ASSET_MOVER_DIR, CLASS_DIR + name + ".class")));
            if (pools.getPools().isEmpty()) return null;
            instance = new JigsawData(pools);
        }
        return instance;
    }

    /** Fetches the pool classes, then the templates they name. Runs during mod construction (AssetMover's rule). */
    public static void fetch() {
        if (!Loader.isModLoaded("assetmover")) {
            Portium.LOGGER.warn("AssetMover is not installed, so 1.14+ villages, pillager outposts and bastions won't generate.");
            return;
        }
        Map<String, String> classes = new LinkedHashMap<>();
        List<String> names = new ArrayList<>();
        Collections.addAll(names, JigsawPools.POOL_CLASSES);
        Collections.addAll(names, JigsawPools.NAME_CLASSES);
        names.add(JigsawPools.PROCESSOR_CLASS);
        for (String name : names) classes.put(name + ".class", CLASS_DIR + name + ".class");
        AssetMoverAPI.fromMinecraft(VERSION, classes);
        JigsawData data = get();
        if (data == null) {
            // AssetMover finishes its downloads later in startup: on a first launch the templates come on the next one.
            Portium.LOGGER.info("1.16.5's jigsaw structure data is being fetched; villages, outposts and bastions generate after the next restart.");
            return;
        }
        Set<String> templates = new LinkedHashSet<>();
        for (JigsawPools.Pool pool : data.pools.getPools().values()) {
            for (Element element : pool.elements) collectTemplates(element, templates);
        }
        // Ruined portals and Nether fossils: single templates placed by code (TemplateFeatures116).
        for (int i = 1; i <= 10; i++) templates.add("ruined_portal/portal_" + i);
        for (int i = 1; i <= 3; i++) templates.add("ruined_portal/giant_portal_" + i);
        for (int i = 1; i <= 14; i++) templates.add("nether_fossils/fossil_" + i);
        Map<String, String> files = new LinkedHashMap<>();
        for (String template : templates) files.put("data/minecraft/structures/" + template + ".nbt", TEMPLATE_DIR + template + ".nbt");
        AssetMoverAPI.fromMinecraft(VERSION, files);
        Portium.LOGGER.info("Jigsaw structures: {} pools, {} templates from {}", data.pools.getPools().size(), templates.size(), VERSION);
    }

    private static void collectTemplates(Element element, Set<String> templates) {
        if (element instanceof Single) templates.add(((Single) element).template);
        if (element instanceof ListElement) {
            for (Element child : ((ListElement) element).elements) collectTemplates(child, templates);
        }
    }

    @Nullable
    private static InputStream open(File file) {
        try {
            return file.isFile() ? new FileInputStream(file) : null;
        } catch (IOException e) {
            return null;
        }
    }

    public JigsawPools pools() {
        return this.pools;
    }

    @Nullable
    public JigsawTemplate template(String name) {
        return this.templates.computeIfAbsent(name, key -> {
            File file = new File(ASSET_MOVER_DIR, TEMPLATE_DIR + key + ".nbt");
            try (InputStream stream = open(file)) {
                if (stream == null) {
                    Portium.LOGGER.warn("Jigsaw template {} is missing ({})", key, file);
                    return Optional.empty();
                }
                return Optional.of(JigsawTemplate.read(CompressedStreamTools.readCompressed(stream)));
            } catch (IOException | RuntimeException e) {
                Portium.LOGGER.error("Couldn't read jigsaw template {}", key, e);
                return Optional.empty();
            }
        }).orElse(null);
    }

    /** An element's box placed at an origin in a rotation; null if its template is missing. */
    @Nullable
    public StructureBoundingBox box(Element element, BlockPos origin, Rotation rotation) {
        if (element instanceof Single) {
            JigsawTemplate template = this.template(((Single) element).template);
            return template == null ? null : template.getBoundingBox(origin, rotation);
        }
        if (element instanceof ListElement) {
            StructureBoundingBox box = null;
            for (Element child : ((ListElement) element).elements) {
                StructureBoundingBox childBox = this.box(child, origin, rotation);
                if (childBox == null) continue;
                if (box == null) {
                    box = new StructureBoundingBox(childBox);
                } else {
                    box.expandTo(childBox);
                }
            }
            return box;
        }
        if (element instanceof Feature) return new StructureBoundingBox(origin, origin);
        return null;
    }

    /** An element's jigsaws in a rotation, shuffled. A list uses its first piece's; a feature has one facing down. */
    public List<Jigsaw> jigsaws(Element element, Rotation rotation, Random random) {
        if (element instanceof Single) {
            JigsawTemplate template = this.template(((Single) element).template);
            return template == null ? Collections.emptyList() : template.getJigsaws(rotation, random);
        }
        if (element instanceof ListElement) {
            List<Element> children = ((ListElement) element).elements;
            return children.isEmpty() ? Collections.emptyList() : this.jigsaws(children.get(0), rotation, random);
        }
        if (element instanceof Feature) return Collections.singletonList(FEATURE_JIGSAW);
        return Collections.emptyList();
    }

    private static final Jigsaw FEATURE_JIGSAW = JigsawTemplate.featureJigsaw();
}
