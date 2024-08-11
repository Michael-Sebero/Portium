package com.michaelsebero.portium.modern.mc.particles;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.util.ResourceLocation;

/** 1.13's {@code net.minecraft.particles.ParticleType}. */
public class ParticleType<T extends IParticleData> extends com.michaelsebero.portium.modern.RegistryEntryStandIn<ParticleType<?>> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final Class<ParticleType<?>> getRegistryType() {
        return (Class) ParticleType.class;
    }


    private final ResourceLocation id;
    private final boolean alwaysShow;

    /** 1.16's reader of the particle's data from commands and packets (a class of Minecraft 1.16.5's own); null for 1.13's. */
    @javax.annotation.Nullable
    public final Object portium$deserializer;

    protected ParticleType(ResourceLocation id, boolean alwaysShow) {
        this.id = id;
        this.alwaysShow = alwaysShow;
        this.portium$deserializer = null;
    }

    /** 1.16's: named by its registry name, with a reader of its data. */
    protected ParticleType(boolean alwaysShow, Object deserializer) {
        this.id = null;
        this.alwaysShow = alwaysShow;
        this.portium$deserializer = deserializer;
    }

    @Srg("func_197570_d")
    public ResourceLocation getId() {
        return this.id != null ? this.id : getRegistryName();
    }

    /** Whether the particle shows however far away it is and whatever the particle setting. */
    @Srg("func_197575_f")
    public boolean getAlwaysShow() {
        return this.alwaysShow;
    }

    @Override
    public String toString() {
        return String.valueOf(getId());
    }
}
