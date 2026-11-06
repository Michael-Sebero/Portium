package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.client.ParticleRendering116;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

/**
 * The parts of 1.16 mods' particles that are classes of Minecraft 1.16.5's own: their factories, sprite sets and
 * render types, for {@code ParticleRendering116}, which keeps and draws them on 1.12. Client only.
 */
public final class ParticleGlue {

    private static IParticleRenderType open;

    /** A factory from 1.16's {@code sprites -> factory}, given the sprites of its type. */
    public static Object factory(Object metaFactory, Object type) throws ReflectiveOperationException {
        // The interface is the particle manager's own (package-private): found on the mod's lambda.
        for (Class<?> implemented : metaFactory.getClass().getInterfaces()) {
            for (Method method : implemented.getMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == IAnimatedSprite.class) {
                    method.setAccessible(true);
                    return method.invoke(metaFactory, new Sprites(type));
                }
            }
        }
        throw new NoSuchMethodException("No sprite factory method on " + metaFactory.getClass().getName());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object make(Object factory, Object data, Object world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        return ((IParticleFactory) factory).func_199234_a((IParticleData) data, (ClientWorld) world, x, y, z, motionX, motionY, motionZ);
    }

    /** A particle's data, read as its type's reader reads it from a packet. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object read(Object deserializer, Object type, Object buffer) {
        return ((IParticleData.IDeserializer) deserializer).func_197543_b((ParticleType) type, (PacketBuffer) buffer);
    }

    /** The vertex builder for a render type's particles, begun if the last particle's was another; null for particles that draw nothing. */
    public static Object begin(Object renderType) {
        IParticleRenderType type = (IParticleRenderType) renderType;
        if (type == IParticleRenderType.field_217606_f) return null;
        Tessellator tessellator = Tessellator.func_178181_a();
        if (type != open) {
            flush();
            type.func_217600_a(tessellator.func_178180_c(), Minecraft.func_71410_x().func_110434_K());
            open = type;
        }
        return tessellator.func_178180_c();
    }

    /** Draws the particles of the render type begun. */
    public static void flush() {
        IParticleRenderType type = open;
        if (type == null) return;
        open = null;
        type.func_217599_a(Tessellator.func_178181_a());
    }

    public static Object camera() {
        return ActiveRenderInfo116.game();
    }

    /** Forge 1.16's event for registering particle factories, to every mod. */
    static void postRegister(ModernHost host) {
        host.postToMods(new net.minecraftforge.client.event.ParticleFactoryRegisterEvent());
    }

    /** A particle type's sprites, looked up when first asked (1.16 fills them in when resources load). */
    private static final class Sprites implements IAnimatedSprite {

        private final Object type;

        Sprites(Object type) {
            this.type = type;
        }

        private List<TextureAtlasSprite> all() {
            return ParticleRendering116.sprites(this.type);
        }

        private static TextureAtlasSprite missing() {
            return ParticleRendering116.missingSprite();
        }

        /** The sprite for an age out of a lifetime. */
        @Override
        public TextureAtlasSprite func_217591_a(int age, int maxAge) {
            List<TextureAtlasSprite> sprites = all();
            if (sprites.isEmpty()) return missing();
            return sprites.get(Math.max(0, Math.min(sprites.size() - 1, age * (sprites.size() - 1) / Math.max(1, maxAge))));
        }

        @Override
        public TextureAtlasSprite func_217590_a(Random random) {
            List<TextureAtlasSprite> sprites = all();
            return sprites.isEmpty() ? missing() : sprites.get(random.nextInt(sprites.size()));
        }
    }

    private ParticleGlue() {}
}
