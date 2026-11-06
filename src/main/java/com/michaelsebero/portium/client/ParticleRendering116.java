package com.michaelsebero.portium.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.michaelsebero.portium.mods116.ModernHooks;
import com.michaelsebero.portium.mods116.Mods116;
import com.michaelsebero.portium.mods116.adapter.Particle116;
import com.michaelsebero.portium.modern.mc.particles.IParticleData;
import com.michaelsebero.portium.modern.mc.particles.ParticleType;
import com.michaelsebero.portium.network.ModParticleMessage;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 1.16 mods' particles on 1.12: their factories by particle type (1.16's {@code ParticleManager.registerFactory}),
 * their sprites (1.16's particle sheet; here stitched into 1.12's block atlas, named by the mods'
 * {@code particles/<type>.json} files), and their drawing, batched by 1.16 render type inside 1.12's pass for
 * particles that set their own drawing up. A factory or particle that fails is switched off, logged once. Client only.
 */
public final class ParticleRendering116 {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");
    /** 1.16's particle factories by type. */
    private static final Map<ParticleType<?>, Object> FACTORIES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<TextureAtlasSprite>> SPRITES = new ConcurrentHashMap<>();
    private static final Set<Object> OFF = Collections.synchronizedSet(new HashSet<>());

    private static MethodHandle factoryOf;
    private static MethodHandle make;
    private static MethodHandle begin;
    private static MethodHandle flush;
    private static MethodHandle camera;
    private static MethodHandle read;
    private static boolean drawing;
    private static final boolean DEBUG = Boolean.getBoolean("portium.debug.particles116");
    private static int debugMade;
    private static int debugDrawn;

    private static void link() throws ReflectiveOperationException {
        if (read != null) return;
        Class<?> glue = Class.forName("com.michaelsebero.portium.mods116.host.ParticleGlue", true, ModernHooks.loader());
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        factoryOf = lookup.findStatic(glue, "factory", MethodType.methodType(Object.class, Object.class, Object.class));
        make = lookup.findStatic(glue, "make", MethodType.methodType(Object.class, Object.class, Object.class, Object.class, double.class, double.class,
                double.class, double.class, double.class, double.class));
        begin = lookup.findStatic(glue, "begin", MethodType.methodType(Object.class, Object.class));
        flush = lookup.findStatic(glue, "flush", MethodType.methodType(void.class));
        camera = lookup.findStatic(glue, "camera", MethodType.methodType(Object.class));
        read = lookup.findStatic(glue, "read", MethodType.methodType(Object.class, Object.class, Object.class, Object.class));
    }

    // ---- Factories ----

    /** 1.16's {@code particleManager.registerFactory(type, factory)}. */
    public static void register(Object manager, ParticleType<?> type, Object factory) {
        FACTORIES.put(type, factory);
    }

    /** 1.16's {@code particleManager.registerFactory(type, sprites -> factory)}: the factory is given the type's sprites. */
    public static void registerSprites(Object manager, ParticleType<?> type, Object metaFactory) {
        try {
            link();
            FACTORIES.put(type, factoryOf.invoke(metaFactory, (Object) type));
        } catch (Throwable e) {
            LOGGER.error("1.16 mod problem: the particle factory for {} couldn't be made, so the particle doesn't show", type, e);
        }
    }

    public static java.util.Collection<ParticleType<?>> types() {
        return Collections.unmodifiableCollection(FACTORIES.keySet());
    }

    /** What failed and was switched off (factories, particle classes), for the test harness. */
    public static List<String> switchedOff() {
        List<String> names = new ArrayList<>();
        synchronized (OFF) {
            for (Object off : OFF) names.add(off instanceof Class ? ((Class<?>) off).getName() : off.getClass().getName());
        }
        return names;
    }

    /** A particle failed while ticking: it's dropped, and no more of its class are made. */
    public static void failed(Particle particle, Throwable error) {
        particle.setExpired();
        if (OFF.add(particle.getClass())) {
            LOGGER.error("1.16 mod problem: the particle {} was switched off", particle.getClass().getName(), error);
            com.michaelsebero.portium.diagnostics.ProblemReport.record("The particle " + particle.getClass().getName() + " was switched off", error);
        }
    }

    public static boolean has(ParticleType<?> type) {
        return FACTORIES.containsKey(type);
    }

    /** 1.16's {@code particleManager.makeParticle}: the particle, not yet added; null if its type has no factory or it makes none. */
    @Nullable
    public static Particle make(IParticleData data, World world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        Object factory = FACTORIES.get(data.getType());
        if (factory == null || OFF.contains(factory)) return null;
        try {
            link();
            Particle made = (Particle) make.invoke(factory, (Object) data, (Object) world, x, y, z, motionX, motionY, motionZ);
            if (made != null && OFF.contains(made.getClass())) return null;
            if (DEBUG && debugMade++ < 5) LOGGER.info("particle116 made {} -> {}", data.getType(), made == null ? null : made.getClass().getName() + " layer " + made.getFXLayer());
            return made;
        } catch (Throwable e) {
            if (OFF.add(factory)) LOGGER.error("1.16 mod problem: the particle {} was switched off", data.getType(), e);
            return null;
        }
    }

    /** Shows a particle of a 1.16 mod's type, as 1.16's world renderer does: by the Particles setting and the distance. */
    @Nullable
    public static Particle spawn(IParticleData data, World world, boolean force, boolean decreased, double x, double y, double z, double motionX, double motionY,
                                 double motionZ) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != world || mc.effectRenderer == null) return null;
        if (!force && !data.getType().getAlwaysShow()) {
            int setting = mc.gameSettings.particleSetting;
            if (decreased && setting == 2 && world.rand.nextInt(10) == 0) setting = 1;
            if (setting == 1 && world.rand.nextInt(3) == 0) setting = 2;
            Entity view = mc.getRenderViewEntity();
            if (setting == 2 || view == null || view.getDistanceSq(x, y, z) > 1024.0D) return null;
        }
        Particle particle = make(data, world, x, y, z, motionX, motionY, motionZ);
        if (particle != null) mc.effectRenderer.addEffect(particle);
        return particle;
    }

    /** 1.16's {@code particleManager.addParticle(data, ...)}. */
    @Nullable
    public static Particle add(Object manager, IParticleData data, double x, double y, double z, double motionX, double motionY, double motionZ) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return null;
        Particle particle = make(data, mc.world, x, y, z, motionX, motionY, motionZ);
        if (particle != null) mc.effectRenderer.addEffect(particle);
        return particle;
    }

    /** A server's particles of a 1.16 mod's type, spread as 1.12 spreads its own. */
    public static void receive(ModParticleMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) return;
            ParticleType<?> type = null;
            for (ParticleType<?> known : FACTORIES.keySet()) {
                if (message.type.equals(String.valueOf(known.getId()))) type = known;
            }
            if (type == null) return;
            IParticleData data;
            try {
                if (type.portium$deserializer == null) {
                    if (!(type instanceof IParticleData)) return;
                    data = (IParticleData) type;
                } else {
                    link();
                    data = (IParticleData) read.invoke(type.portium$deserializer, (Object) type, (Object) new PacketBuffer(Unpooled.wrappedBuffer(message.data)));
                }
            } catch (Throwable e) {
                if (OFF.add(type)) LOGGER.error("1.16 mod problem: the particle {} couldn't be read from the server's packet", type, e);
                return;
            }
            if (message.count == 0) {
                spawn(data, mc.world, message.force, false, message.x, message.y, message.z, message.speed * message.deltaX, message.speed * message.deltaY,
                        message.speed * message.deltaZ);
                return;
            }
            Random random = mc.world.rand;
            for (int i = 0; i < message.count; i++) {
                spawn(data, mc.world, message.force, false, message.x + random.nextGaussian() * message.deltaX, message.y + random.nextGaussian() * message.deltaY,
                        message.z + random.nextGaussian() * message.deltaZ, random.nextGaussian() * message.speed, random.nextGaussian() * message.speed,
                        random.nextGaussian() * message.speed);
            }
        });
    }

    // ---- Sprites ----

    /** A particle type's sprites, in the order its {@code particles/<type>.json} lists them. */
    public static List<TextureAtlasSprite> sprites(Object typeObject) {
        ResourceLocation id = ((ParticleType<?>) typeObject).getId();
        if (id == null) return Collections.emptyList();
        return SPRITES.computeIfAbsent(id, key -> {
            List<TextureAtlasSprite> sprites = new ArrayList<>();
            Minecraft mc = Minecraft.getMinecraft();
            try (IResource resource = mc.getResourceManager().getResource(new ResourceLocation(key.getNamespace(), "particles/" + key.getPath() + ".json"));
                 Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                for (ResourceLocation texture : textures(reader)) sprites.add(mc.getTextureMapBlocks().getAtlasSprite(texture.toString()));
            } catch (Exception e) {
                LOGGER.warn("1.16 mod problem: the particle {} has no readable list of sprites", key, e);
            }
            return sprites;
        });
    }

    public static TextureAtlasSprite missingSprite() {
        return Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
    }

    private static List<ResourceLocation> textures(Reader reader) {
        List<ResourceLocation> textures = new ArrayList<>();
        JsonElement root = new JsonParser().parse(reader);
        if (!root.isJsonObject()) return textures;
        JsonElement list = ((JsonObject) root).get("textures");
        if (list == null || !list.isJsonArray()) return textures;
        for (JsonElement texture : (JsonArray) list) {
            ResourceLocation name = new ResourceLocation(texture.getAsString());
            textures.add(new ResourceLocation(name.getNamespace(), "particle/" + name.getPath()));
        }
        return textures;
    }

    /** Every sprite the 1.16 mods' particle lists name goes into 1.12's block atlas (1.12 has no particle atlas to add to). */
    @SubscribeEvent
    public static void onStitch(TextureStitchEvent.Pre event) {
        if (event.getMap() != Minecraft.getMinecraft().getTextureMapBlocks()) return;
        SPRITES.clear();
        int count = 0;
        for (File jar : Mods116.dataPackJars().values()) {
            try (ZipFile zip = new ZipFile(jar)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (entry.isDirectory() || !name.startsWith("assets/") || !name.endsWith(".json")) continue;
                    int namespaceEnd = name.indexOf('/', 7);
                    if (namespaceEnd < 0 || !name.startsWith("/particles/", namespaceEnd)) continue;
                    try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                        for (ResourceLocation texture : textures(reader)) {
                            event.getMap().registerSprite(texture);
                            count++;
                        }
                    } catch (RuntimeException e) {
                        LOGGER.warn("1.16 mod problem: {} in {} isn't a readable list of particle sprites", name, jar.getName(), e);
                    }
                }
            } catch (java.io.IOException e) {
                LOGGER.warn("Couldn't read {} for its particles", jar, e);
            }
        }
        if (count > 0) LOGGER.info("1.16 mods' particles use {} sprites", count);
    }

    // ---- Drawing ----

    /**
     * One particle, from 1.12's pass over the particles that set their own drawing up. Particles of one render type
     * in a row share a batch, which {@link #flush} draws (1.16 draws all of a render type's at once).
     */
    public static void render(Particle116 particle, float partialTicks) {
        if (!OFF.isEmpty() && OFF.contains(particle.getClass())) return;
        try {
            link();
            Object type = particle.getRenderType();
            if (DEBUG && debugDrawn < 5) LOGGER.info("particle116 render {} type {}", particle.getClass().getName(), type);
            if (type == null) return;
            if (!drawing) {
                // 1.16's vertices are placed from the camera; 1.12's world is drawn from the viewer's feet.
                double[] view = RenderBridge116.camera();
                GlStateManager.pushMatrix();
                GlStateManager.translate(view[0] - Particle.interpPosX, view[1] - Particle.interpPosY, view[2] - Particle.interpPosZ);
                GlStateManager.disableLighting();
                drawing = true;
            }
            Object builder = begin.invoke(type);
            if (DEBUG && debugDrawn++ < 5) LOGGER.info("particle116 draw {} type {} builder {}", particle.getClass().getName(), type, builder);
            if (builder == null) return;
            particle.render(builder, camera.invoke(), partialTicks);
        } catch (Throwable e) {
            particle.setExpired();
            if (OFF.add(particle.getClass())) {
                LOGGER.error("1.16 mod problem: the particle {} was switched off while drawn", particle.getClass().getName(), e);
                com.michaelsebero.portium.diagnostics.ProblemReport.record("The particle " + particle.getClass().getName() + " was switched off while drawn", e);
            }
        }
    }

    /** Draws what's batched and puts 1.12's drawing state back; after 1.12's pass (MixinParticleManager116). */
    public static void flush() {
        if (!drawing) return;
        drawing = false;
        try {
            flush.invoke();
        } catch (Throwable e) {
            LOGGER.error("1.16 mod problem: 1.16 particles couldn't be drawn", e);
        }
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.alphaFunc(516, 0.1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE);
    }

    private ParticleRendering116() {}
}
