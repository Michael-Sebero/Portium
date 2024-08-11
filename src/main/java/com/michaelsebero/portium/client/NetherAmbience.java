package com.michaelsebero.portium.client;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.client.particle.ParticleNetherAsh;
import com.michaelsebero.portium.client.particle.ParticleNetherSpore;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.world.biome.BiomeNether116;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.EnumHelperClient;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * 1.16's Nether ambience for the local player, by biome: fog colour blended over the cells around the camera, ash or
 * spores in the air, an ambient loop that fades between biomes, mood sounds in the dark, random additions, and music.
 * The Nether wastes (1.12's Hell) get theirs too.
 */
public final class NetherAmbience {

    /** What each Nether biome sounds and looks like in 1.16. */
    public enum Kind {
        NETHER_WASTES(3344392, null, 0.0F, PortiumSounds.AMBIENT_NETHER_WASTES_LOOP, PortiumSounds.AMBIENT_NETHER_WASTES_MOOD,
                PortiumSounds.AMBIENT_NETHER_WASTES_ADDITIONS, PortiumSounds.MUSIC_NETHER_NETHER_WASTES),
        SOUL_SAND_VALLEY(1787717, Particles.ASH, 0.00625F, PortiumSounds.AMBIENT_SOUL_SAND_VALLEY_LOOP, PortiumSounds.AMBIENT_SOUL_SAND_VALLEY_MOOD,
                PortiumSounds.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, PortiumSounds.MUSIC_NETHER_SOUL_SAND_VALLEY),
        CRIMSON_FOREST(3343107, Particles.CRIMSON_SPORE, 0.025F, PortiumSounds.AMBIENT_CRIMSON_FOREST_LOOP, PortiumSounds.AMBIENT_CRIMSON_FOREST_MOOD,
                PortiumSounds.AMBIENT_CRIMSON_FOREST_ADDITIONS, PortiumSounds.MUSIC_NETHER_CRIMSON_FOREST),
        WARPED_FOREST(1705242, Particles.WARPED_SPORE, 0.01428F, PortiumSounds.AMBIENT_WARPED_FOREST_LOOP, PortiumSounds.AMBIENT_WARPED_FOREST_MOOD,
                PortiumSounds.AMBIENT_WARPED_FOREST_ADDITIONS, PortiumSounds.MUSIC_NETHER_WARPED_FOREST),
        BASALT_DELTAS(6840176, Particles.WHITE_ASH, 0.118093334F, PortiumSounds.AMBIENT_BASALT_DELTAS_LOOP, PortiumSounds.AMBIENT_BASALT_DELTAS_MOOD,
                PortiumSounds.AMBIENT_BASALT_DELTAS_ADDITIONS, PortiumSounds.MUSIC_NETHER_BASALT_DELTAS);

        final int fogColor;
        @Nullable final Particles particle;
        final float particleChance;
        final SoundEvent loop;
        final SoundEvent mood;
        final SoundEvent additions;
        final SoundEvent music;
        MusicTicker.MusicType musicType;

        Kind(int fogColor, @Nullable Particles particle, float particleChance, SoundEvent loop, SoundEvent mood, SoundEvent additions, SoundEvent music) {
            this.fogColor = fogColor;
            this.particle = particle;
            this.particleChance = particleChance;
            this.loop = loop;
            this.mood = mood;
            this.additions = additions;
            this.music = music;
        }
    }

    enum Particles {
        ASH, WHITE_ASH, CRIMSON_SPORE, WARPED_SPORE
    }

    // 1.16's mood and additions settings, the same for every Nether biome.
    private static final int MOOD_DELAY = 6000;
    private static final int MOOD_RADIUS = 8;
    private static final double MOOD_OFFSET = 2.0D;
    private static final double ADDITIONS_CHANCE = 0.0111D;
    // 1.16's default music selector: 10 to 20 minutes between tracks, without cutting one short.
    private static final int MUSIC_MIN_DELAY = 12000;
    private static final int MUSIC_MAX_DELAY = 24000;
    /** 1.16's fog blending weights over six cells. */
    private static final double[] FOG_WEIGHTS = {0.0D, 1.0D, 4.0D, 6.0D, 4.0D, 1.0D, 0.0D};

    private static final Map<Kind, Loop> LOOPS = new EnumMap<>(Kind.class);
    @Nullable private static Kind current;
    private static float moodChance;

    /** The 1.16 ambience of a biome, if it's a Nether biome. */
    @Nullable
    public static Kind of(Biome biome) {
        if (biome == Biomes.HELL) return Kind.NETHER_WASTES;
        if (!(biome instanceof BiomeNether116)) return null;
        switch (((BiomeNether116) biome).getKind()) {
            case SOUL_SAND_VALLEY: return Kind.SOUL_SAND_VALLEY;
            case CRIMSON_FOREST: return Kind.CRIMSON_FOREST;
            case WARPED_FOREST: return Kind.WARPED_FOREST;
            default: return Kind.BASALT_DELTAS;
        }
    }

    /** Makes each biome's music a music type 1.12 can play. */
    public static void registerMusic() {
        for (Kind kind : Kind.values()) {
            kind.musicType = EnumHelperClient.addMusicType("PORTIUM_" + kind.name(), kind.music, MUSIC_MIN_DELAY, MUSIC_MAX_DELAY);
        }
    }

    // ---- Fog ----

    /**
     * 1.16's Nether fog colour at the camera: each biome's fog colour, weighted over the 6x6 cells around it. Null
     * where none of those biomes is a 1.16 one, leaving 1.12's fog.
     */
    @Nullable
    public static Vec3d fogColor(World world, float partialTicks) {
        if (!PortiumConfig.visuals.netherFog) return null;
        Entity camera = Minecraft.getMinecraft().getRenderViewEntity();
        if (camera == null) return null;
        double x = (camera.prevPosX + (camera.posX - camera.prevPosX) * partialTicks - 2.0D) * 0.25D;
        double z = (camera.prevPosZ + (camera.posZ - camera.prevPosZ) * partialTicks - 2.0D) * 0.25D;
        int cellX = MathHelper.floor(x);
        int cellZ = MathHelper.floor(z);
        double fx = x - cellX;
        double fz = z - cellZ;
        double total = 0.0D;
        double red = 0.0D;
        double green = 0.0D;
        double blue = 0.0D;
        boolean any116 = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // The Nether's biomes don't change with height, so 1.16's third axis drops out of the average.
        for (int i = 0; i < 6; i++) {
            double wx = fx * (FOG_WEIGHTS[i] - FOG_WEIGHTS[i + 1]) + FOG_WEIGHTS[i + 1];
            for (int j = 0; j < 6; j++) {
                double wz = fz * (FOG_WEIGHTS[j] - FOG_WEIGHTS[j + 1]) + FOG_WEIGHTS[j + 1];
                double weight = wx * wz;
                // A cell's biome as 1.12 keeps it: the column biome at the cell's middle.
                Biome biome = world.getBiome(pos.setPos(((cellX - 2 + i) << 2) + 2, 64, ((cellZ - 2 + j) << 2) + 2));
                Kind kind = of(biome);
                if (kind != null && kind != Kind.NETHER_WASTES) any116 = true;
                int color = kind == null ? Kind.NETHER_WASTES.fogColor : kind.fogColor;
                total += weight;
                red += weight * (double) (color >> 16 & 255) / 255.0D;
                green += weight * (double) (color >> 8 & 255) / 255.0D;
                blue += weight * (double) (color & 255) / 255.0D;
            }
        }
        if (!any116) return null;
        return new Vec3d(red / total, green / total, blue / total);
    }

    // ---- Particles ----

    /**
     * 1.16's ambient particles, tried at each spot 1.12 picks for random block effects around the player (two
     * thousand a tick): unless the block there is solid, its biome's particle may appear somewhere in it.
     */
    public static void ambientParticle(World world, BlockPos pos, IBlockState state) {
        if (!PortiumConfig.visuals.netherParticles || state.isFullCube()) return;
        Kind kind = of(world.getBiome(pos));
        if (kind == null || kind.particle == null || world.rand.nextFloat() > kind.particleChance) return;
        double x = (double) pos.getX() + world.rand.nextDouble();
        double y = (double) pos.getY() + world.rand.nextDouble();
        double z = (double) pos.getZ() + world.rand.nextDouble();
        Minecraft mc = Minecraft.getMinecraft();
        Entity camera = mc.getRenderViewEntity();
        if (camera == null || camera.getDistanceSq(x, y, z) > 1024.0D || !particleSettingAllows(mc, world)) return;
        Particle particle;
        switch (kind.particle) {
            case ASH: particle = ParticleNetherAsh.ash(world, x, y, z); break;
            case WHITE_ASH: particle = ParticleNetherAsh.whiteAsh(world, x, y, z); break;
            case CRIMSON_SPORE: particle = ParticleNetherSpore.crimson(world, x, y, z); break;
            default: particle = ParticleNetherSpore.warped(world, x, y, z); break;
        }
        mc.effectRenderer.addEffect(particle);
    }

    /** The Particles video setting, as 1.12 applies it to particles it spawns itself. */
    private static boolean particleSettingAllows(Minecraft mc, World world) {
        int setting = mc.gameSettings.particleSetting;
        if (setting == 1 && world.rand.nextInt(3) == 0) setting = 2;
        return setting <= 1;
    }

    // ---- Sounds ----

    /** Whether 1.12's cave sounds give way to the Nether biome's mood sounds where the player is. */
    public static boolean replacesCaveSounds(World world) {
        Minecraft mc = Minecraft.getMinecraft();
        return PortiumConfig.visuals.netherAmbientSounds && mc.player != null && of(world.getBiome(new BlockPos(mc.player))) != null;
    }

    public static void tick(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null || !PortiumConfig.visuals.netherAmbientSounds) {
            for (Loop loop : LOOPS.values()) loop.fadeOut();
            LOOPS.values().removeIf(Loop::isDonePlaying);
            current = null;
            moodChance = 0.0F;
            return;
        }
        LOOPS.values().removeIf(Loop::isDonePlaying);
        Kind here = of(mc.world.getBiome(new BlockPos(player.posX, player.posY, player.posZ)));
        if (here != current) {
            current = here;
            for (Loop loop : LOOPS.values()) loop.fadeOut();
            if (here != null) {
                Loop loop = LOOPS.get(here);
                if (loop == null) {
                    loop = new Loop(here.loop);
                    LOOPS.put(here, loop);
                    mc.getSoundHandler().playSound(loop);
                }
                loop.fadeIn();
            }
        }
        if (current == null) return;
        Random random = mc.world.rand;
        if (random.nextDouble() < ADDITIONS_CHANCE) {
            mc.getSoundHandler().playSound(new PositionedSoundRecord(current.additions.getSoundName(), SoundCategory.AMBIENT, 1.0F, 1.0F, false, 0,
                    ISound.AttenuationType.NONE, 0.0F, 0.0F, 0.0F));
        }
        mood(mc, player, random);
    }

    /** 1.16's mood sounds: darkness near the player builds up until one plays, from a dark spot nearby. */
    private static void mood(Minecraft mc, EntityPlayerSP player, Random random) {
        World world = mc.world;
        int size = MOOD_RADIUS * 2 + 1;
        BlockPos pos = new BlockPos(player.posX + (double) random.nextInt(size) - (double) MOOD_RADIUS,
                player.posY + (double) player.getEyeHeight() + (double) random.nextInt(size) - (double) MOOD_RADIUS,
                player.posZ + (double) random.nextInt(size) - (double) MOOD_RADIUS);
        int sky = world.getLightFor(EnumSkyBlock.SKY, pos);
        if (sky > 0) moodChance -= (float) sky / 15.0F * 0.001F;
        else moodChance -= (float) (world.getLightFor(EnumSkyBlock.BLOCK, pos) - 1) / (float) MOOD_DELAY;
        if (moodChance < 1.0F) {
            moodChance = Math.max(moodChance, 0.0F);
            return;
        }
        double dx = (double) pos.getX() + 0.5D - player.posX;
        double dy = (double) pos.getY() + 0.5D - (player.posY + (double) player.getEyeHeight());
        double dz = (double) pos.getZ() + 0.5D - player.posZ;
        double distance = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        double reach = distance + MOOD_OFFSET;
        mc.getSoundHandler().playSound(new PositionedSoundRecord(current.mood.getSoundName(), SoundCategory.AMBIENT, 1.0F, 1.0F, false, 0,
                ISound.AttenuationType.LINEAR, (float) (player.posX + dx / distance * reach),
                (float) (player.posY + (double) player.getEyeHeight() + dy / distance * reach), (float) (player.posZ + dz / distance * reach)));
        moodChance = 0.0F;
    }

    // ---- Music ----

    /** The Nether music for where the player is, in place of 1.12's; null to keep 1.12's. */
    @Nullable
    public static MusicTicker.MusicType music(Minecraft mc) {
        if (!PortiumConfig.visuals.netherMusic || mc.player == null || mc.world == null) return null;
        Kind kind = of(mc.world.getBiome(new BlockPos(mc.player)));
        return kind == null ? null : kind.musicType;
    }

    /** 1.16's biome music lets a playing track finish when the player crosses into another biome. */
    public static boolean keepsPlayingMusic(ResourceLocation next) {
        for (Kind kind : Kind.values()) {
            if (kind.music.getSoundName().equals(next)) return true;
        }
        return false;
    }

    /** A biome's ambient loop: fades in over two seconds, and out again once the player leaves the biome. */
    private static final class Loop extends MovingSound {

        private int fade;
        private int fadeSpeed;

        Loop(SoundEvent sound) {
            super(sound, SoundCategory.AMBIENT);
            this.repeat = true;
            this.repeatDelay = 0;
            // 1.12 skips a sound that starts silent; the first tick brings it down to where the fade is.
            this.volume = 1.0F;
            this.attenuationType = AttenuationType.NONE;
        }

        void fadeIn() {
            this.fade = Math.max(0, this.fade);
            this.fadeSpeed = 1;
        }

        void fadeOut() {
            this.fade = Math.min(this.fade, 40);
            this.fadeSpeed = -1;
        }

        @Override
        public void update() {
            if (this.fade < 0) {
                this.donePlaying = true;
                return;
            }
            this.fade += this.fadeSpeed;
            this.volume = MathHelper.clamp((float) this.fade / 40.0F, 0.0F, 1.0F);
        }
    }

    private NetherAmbience() {}
}
