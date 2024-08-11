package com.michaelsebero.portium.modern.mc.entity;

import com.michaelsebero.portium.modern.ModernEntityTypes;
import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.util.registry.IRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * 1.13's {@code net.minecraft.entity.EntityType}: a kind of entity, made by its factory. 1.13 mods' types are
 * registered with Forge 1.12's entities too ({@code ModernEntityTypes}); vanilla's stand for 1.12's (or Portium's)
 * entity of the same name.
 */
public class EntityType<T extends Entity> extends IForgeRegistryEntry.Impl<EntityType<?>> {

    @Srg("field_200788_b")
    public static final EntityType<Entity> AREA_EFFECT_CLOUD = vanilla("area_effect_cloud");
    @Srg("field_200789_c")
    public static final EntityType<Entity> ARMOR_STAND = vanilla("armor_stand");
    @Srg("field_200790_d")
    public static final EntityType<Entity> ARROW = vanilla("arrow");
    @Srg("field_200791_e")
    public static final EntityType<Entity> BAT = vanilla("bat");
    @Srg("field_200792_f")
    public static final EntityType<Entity> BLAZE = vanilla("blaze");
    @Srg("field_200793_g")
    public static final EntityType<Entity> BOAT = vanilla("boat");
    @Srg("field_200794_h")
    public static final EntityType<Entity> CAVE_SPIDER = vanilla("cave_spider");
    @Srg("field_200795_i")
    public static final EntityType<Entity> CHICKEN = vanilla("chicken");
    @Srg("field_203780_j")
    public static final EntityType<Entity> COD = vanilla("cod");
    @Srg("field_200796_j")
    public static final EntityType<Entity> COW = vanilla("cow");
    @Srg("field_200797_k")
    public static final EntityType<Entity> CREEPER = vanilla("creeper");
    @Srg("field_200798_l")
    public static final EntityType<Entity> DONKEY = vanilla("donkey");
    @Srg("field_205137_n")
    public static final EntityType<Entity> DOLPHIN = vanilla("dolphin");
    @Srg("field_200799_m")
    public static final EntityType<Entity> DRAGON_FIREBALL = vanilla("dragon_fireball");
    @Srg("field_204724_o")
    public static final EntityType<Entity> DROWNED = vanilla("drowned");
    @Srg("field_200800_n")
    public static final EntityType<Entity> ELDER_GUARDIAN = vanilla("elder_guardian");
    @Srg("field_200801_o")
    public static final EntityType<Entity> END_CRYSTAL = vanilla("end_crystal");
    @Srg("field_200802_p")
    public static final EntityType<Entity> ENDER_DRAGON = vanilla("ender_dragon");
    @Srg("field_200803_q")
    public static final EntityType<Entity> ENDERMAN = vanilla("enderman");
    @Srg("field_200804_r")
    public static final EntityType<Entity> ENDERMITE = vanilla("endermite");
    @Srg("field_200805_s")
    public static final EntityType<Entity> EVOKER_FANGS = vanilla("evoker_fangs");
    @Srg("field_200806_t")
    public static final EntityType<Entity> EVOKER = vanilla("evoker");
    @Srg("field_200807_u")
    public static final EntityType<Entity> EXPERIENCE_ORB = vanilla("experience_orb");
    @Srg("field_200808_v")
    public static final EntityType<Entity> EYE_OF_ENDER = vanilla("eye_of_ender");
    @Srg("field_200809_w")
    public static final EntityType<Entity> FALLING_BLOCK = vanilla("falling_block");
    @Srg("field_200810_x")
    public static final EntityType<Entity> FIREWORK_ROCKET = vanilla("firework_rocket");
    @Srg("field_200811_y")
    public static final EntityType<Entity> GHAST = vanilla("ghast");
    @Srg("field_200812_z")
    public static final EntityType<Entity> GIANT = vanilla("giant");
    @Srg("field_200761_A")
    public static final EntityType<Entity> GUARDIAN = vanilla("guardian");
    @Srg("field_200762_B")
    public static final EntityType<Entity> HORSE = vanilla("horse");
    @Srg("field_200763_C")
    public static final EntityType<Entity> HUSK = vanilla("husk");
    @Srg("field_200764_D")
    public static final EntityType<Entity> ILLUSIONER = vanilla("illusioner");
    @Srg("field_200765_E")
    public static final EntityType<Entity> ITEM = vanilla("item");
    @Srg("field_200766_F")
    public static final EntityType<Entity> ITEM_FRAME = vanilla("item_frame");
    @Srg("field_200767_G")
    public static final EntityType<Entity> FIREBALL = vanilla("fireball");
    @Srg("field_200768_H")
    public static final EntityType<Entity> LEASH_KNOT = vanilla("leash_knot");
    @Srg("field_200769_I")
    public static final EntityType<Entity> LLAMA = vanilla("llama");
    @Srg("field_200770_J")
    public static final EntityType<Entity> LLAMA_SPIT = vanilla("llama_spit");
    @Srg("field_200771_K")
    public static final EntityType<Entity> MAGMA_CUBE = vanilla("magma_cube");
    @Srg("field_200772_L")
    public static final EntityType<Entity> MINECART = vanilla("minecart");
    @Srg("field_200773_M")
    public static final EntityType<Entity> CHEST_MINECART = vanilla("chest_minecart");
    @Srg("field_200774_N")
    public static final EntityType<Entity> COMMAND_BLOCK_MINECART = vanilla("command_block_minecart");
    @Srg("field_200775_O")
    public static final EntityType<Entity> FURNACE_MINECART = vanilla("furnace_minecart");
    @Srg("field_200776_P")
    public static final EntityType<Entity> HOPPER_MINECART = vanilla("hopper_minecart");
    @Srg("field_200777_Q")
    public static final EntityType<Entity> SPAWNER_MINECART = vanilla("spawner_minecart");
    @Srg("field_200778_R")
    public static final EntityType<Entity> TNT_MINECART = vanilla("tnt_minecart");
    @Srg("field_200779_S")
    public static final EntityType<Entity> MULE = vanilla("mule");
    @Srg("field_200780_T")
    public static final EntityType<Entity> MOOSHROOM = vanilla("mooshroom");
    @Srg("field_200781_U")
    public static final EntityType<Entity> OCELOT = vanilla("ocelot");
    @Srg("field_200782_V")
    public static final EntityType<Entity> PAINTING = vanilla("painting");
    @Srg("field_200783_W")
    public static final EntityType<Entity> PARROT = vanilla("parrot");
    @Srg("field_200784_X")
    public static final EntityType<Entity> PIG = vanilla("pig");
    @Srg("field_203779_Z")
    public static final EntityType<Entity> PUFFERFISH = vanilla("pufferfish");
    @Srg("field_200785_Y")
    public static final EntityType<Entity> ZOMBIE_PIGMAN = vanilla("zombie_pigman");
    @Srg("field_200786_Z")
    public static final EntityType<Entity> POLAR_BEAR = vanilla("polar_bear");
    @Srg("field_200735_aa")
    public static final EntityType<Entity> TNT = vanilla("tnt");
    @Srg("field_200736_ab")
    public static final EntityType<Entity> RABBIT = vanilla("rabbit");
    @Srg("field_203778_ae")
    public static final EntityType<Entity> SALMON = vanilla("salmon");
    @Srg("field_200737_ac")
    public static final EntityType<Entity> SHEEP = vanilla("sheep");
    @Srg("field_200738_ad")
    public static final EntityType<Entity> SHULKER = vanilla("shulker");
    @Srg("field_200739_ae")
    public static final EntityType<Entity> SHULKER_BULLET = vanilla("shulker_bullet");
    @Srg("field_200740_af")
    public static final EntityType<Entity> SILVERFISH = vanilla("silverfish");
    @Srg("field_200741_ag")
    public static final EntityType<Entity> SKELETON = vanilla("skeleton");
    @Srg("field_200742_ah")
    public static final EntityType<Entity> SKELETON_HORSE = vanilla("skeleton_horse");
    @Srg("field_200743_ai")
    public static final EntityType<Entity> SLIME = vanilla("slime");
    @Srg("field_200744_aj")
    public static final EntityType<Entity> SMALL_FIREBALL = vanilla("small_fireball");
    @Srg("field_200745_ak")
    public static final EntityType<Entity> SNOW_GOLEM = vanilla("snow_golem");
    @Srg("field_200746_al")
    public static final EntityType<Entity> SNOWBALL = vanilla("snowball");
    @Srg("field_200747_am")
    public static final EntityType<Entity> SPECTRAL_ARROW = vanilla("spectral_arrow");
    @Srg("field_200748_an")
    public static final EntityType<Entity> SPIDER = vanilla("spider");
    @Srg("field_200749_ao")
    public static final EntityType<Entity> SQUID = vanilla("squid");
    @Srg("field_200750_ap")
    public static final EntityType<Entity> STRAY = vanilla("stray");
    @Srg("field_204262_at")
    public static final EntityType<Entity> TROPICAL_FISH = vanilla("tropical_fish");
    @Srg("field_203099_aq")
    public static final EntityType<Entity> TURTLE = vanilla("turtle");
    @Srg("field_200751_aq")
    public static final EntityType<Entity> EGG = vanilla("egg");
    @Srg("field_200752_ar")
    public static final EntityType<Entity> ENDER_PEARL = vanilla("ender_pearl");
    @Srg("field_200753_as")
    public static final EntityType<Entity> EXPERIENCE_BOTTLE = vanilla("experience_bottle");
    @Srg("field_200754_at")
    public static final EntityType<Entity> POTION = vanilla("potion");
    @Srg("field_200755_au")
    public static final EntityType<Entity> VEX = vanilla("vex");
    @Srg("field_200756_av")
    public static final EntityType<Entity> VILLAGER = vanilla("villager");
    @Srg("field_200757_aw")
    public static final EntityType<Entity> IRON_GOLEM = vanilla("iron_golem");
    @Srg("field_200758_ax")
    public static final EntityType<Entity> VINDICATOR = vanilla("vindicator");
    @Srg("field_200759_ay")
    public static final EntityType<Entity> WITCH = vanilla("witch");
    @Srg("field_200760_az")
    public static final EntityType<Entity> WITHER = vanilla("wither");
    @Srg("field_200722_aA")
    public static final EntityType<Entity> WITHER_SKELETON = vanilla("wither_skeleton");
    @Srg("field_200723_aB")
    public static final EntityType<Entity> WITHER_SKULL = vanilla("wither_skull");
    @Srg("field_200724_aC")
    public static final EntityType<Entity> WOLF = vanilla("wolf");
    @Srg("field_200725_aD")
    public static final EntityType<Entity> ZOMBIE = vanilla("zombie");
    @Srg("field_200726_aE")
    public static final EntityType<Entity> ZOMBIE_HORSE = vanilla("zombie_horse");
    @Srg("field_200727_aF")
    public static final EntityType<Entity> ZOMBIE_VILLAGER = vanilla("zombie_villager");
    @Srg("field_203097_aH")
    public static final EntityType<Entity> PHANTOM = vanilla("phantom");
    @Srg("field_200728_aG")
    public static final EntityType<Entity> LIGHTNING_BOLT = vanilla("lightning_bolt");
    @Srg("field_200729_aH")
    public static final EntityType<Entity> PLAYER = vanilla("player");
    @Srg("field_200730_aI")
    public static final EntityType<Entity> FISHING_BOBBER = vanilla("fishing_bobber");
    @Srg("field_203098_aL")
    public static final EntityType<Entity> TRIDENT = vanilla("trident");

    @Nullable
    private final Class<? extends T> entityClass;
    @Nullable
    private final Function<? super World, ? extends T> factory;
    private final boolean serializable;
    private final boolean summonable;
    private final boolean velocityUpdates;
    private final int trackingRange;
    private final int updateFrequency;
    /** For vanilla's: the 1.13 name, by which 1.12's entity is found. */
    @Nullable
    private final String vanilla;
    /** 1.16's factory, which takes the type too; null for 1.13's. */
    @Nullable
    private java.util.function.BiFunction<EntityType<T>, World, T> factory116;
    /** The class 1.16's factory makes, learned by making one. */
    @Nullable
    private Class<? extends T> madeClass;
    private float width = 0.6F;
    private float height = 1.8F;
    private boolean immuneToFire;
    private String translationKey;

    public EntityType(Class<? extends T> entityClass, Function<? super World, ? extends T> factory, boolean serializable, boolean summonable,
                      @Nullable Object dataFixerType) {
        this(entityClass, factory, serializable, summonable, true, 64, 3, null);
    }

    private EntityType(@Nullable Class<? extends T> entityClass, @Nullable Function<? super World, ? extends T> factory, boolean serializable,
                       boolean summonable, boolean velocityUpdates, int trackingRange, int updateFrequency, @Nullable String vanilla) {
        this.entityClass = entityClass;
        this.factory = factory;
        this.serializable = serializable;
        this.summonable = summonable;
        this.velocityUpdates = velocityUpdates;
        this.trackingRange = trackingRange;
        this.updateFrequency = updateFrequency;
        this.vanilla = vanilla;
    }

    private static EntityType<Entity> vanilla(String id) {
        EntityType<Entity> type = new EntityType<>(null, null, true, true, true, 80, 3, id);
        type.setRegistryName(new ResourceLocation(id));
        IRegistry.ENTITY_TYPE.put(type.getRegistryName(), type);
        return type;
    }

    /** 1.12's (or Portium's) entity of a vanilla type. */
    @Nullable
    private EntityEntry legacy() {
        if (this.vanilla == null) return null;
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation("minecraft", this.vanilla));
        if (entry == null) entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation("portium", this.vanilla));
        return entry;
    }

    /** 1.13's way of making and registering a type at once (Forge 1.13's registry event is the other). */
    @Srg("func_200712_a")
    public static <T extends Entity> EntityType<T> register(String id, Builder<T> builder) {
        EntityType<T> type = builder.build(id);
        type.setRegistryName(new ResourceLocation(id));
        ModernEntityTypes.register(type);
        return type;
    }

    @Nullable
    @Srg("func_200718_a")
    public static ResourceLocation getId(EntityType<?> type) {
        return type.getRegistryName();
    }

    @Nullable
    @Srg("func_200713_a")
    public static EntityType<?> getById(String id) {
        try {
            return (EntityType<?>) IRegistry.ENTITY_TYPE.getNullable(new ResourceLocation(id));
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Srg("func_200715_a")
    public boolean isSerializable() {
        return this.serializable;
    }

    @Srg("func_200720_b")
    public boolean isSummonable() {
        return this.summonable;
    }

    @Nullable
    @Srg("func_201760_c")
    @SuppressWarnings("unchecked")
    public Class<? extends T> getEntityClass() {
        if (this.entityClass != null) return this.entityClass;
        if (this.factory116 != null) {
            if (this.madeClass == null) {
                try {
                    // Without a world, as 1.12 makes entities for their registry entries' sake too.
                    T made = this.factory116.apply(this, null);
                    if (made != null) this.madeClass = (Class<? extends T>) made.getClass();
                } catch (RuntimeException | LinkageError e) {
                    org.apache.logging.log4j.LogManager.getLogger("Portium").warn("Couldn't make an entity of type {} to learn its class", getRegistryName(), e);
                }
            }
            return this.madeClass;
        }
        EntityEntry entry = legacy();
        return entry == null ? null : (Class<? extends T>) entry.getEntityClass();
    }

    @Srg("func_210760_d")
    public String getTranslationKey() {
        if (this.translationKey == null) {
            ResourceLocation id = getRegistryName();
            this.translationKey = id == null ? "entity.unknown" : "entity." + id.getNamespace() + "." + id.getPath();
        }
        return this.translationKey;
    }

    @Srg("func_212546_e")
    public ITextComponent getName() {
        return new TextComponentTranslation(getTranslationKey());
    }

    @Nullable
    @Srg("func_200721_a")
    @SuppressWarnings("unchecked")
    public T create(World world) {
        if (this.factory116 != null) return this.factory116.apply(this, world);
        if (this.factory != null) return this.factory.apply(world);
        EntityEntry entry = legacy();
        return entry == null ? null : (T) entry.newInstance(world);
    }

    @Nullable
    @Srg("func_200714_a")
    public static Entity create(World world, ResourceLocation id) {
        return EntityList.createEntityByIDFromName(id, world);
    }

    @Nullable
    @Srg("func_200716_a")
    public static Entity create(NBTTagCompound nbt, World world) {
        return EntityList.createEntityFromNBT(nbt, world);
    }

    // 1.16's size and fire immunity.

    public float portium$getWidth() {
        return this.width;
    }

    public float portium$getHeight() {
        return this.height;
    }

    @Srg("func_220338_c")
    public boolean isImmuneToFire() {
        return this.immuneToFire;
    }

    // Forge 1.13's tracking settings.

    public int getTrackingRange() {
        return this.trackingRange;
    }

    public int getUpdateFrequency() {
        return this.updateFrequency;
    }

    public boolean shouldSendVelocityUpdates() {
        return this.velocityUpdates;
    }

    @Override
    public String toString() {
        return "EntityType{" + getRegistryName() + "}";
    }

    /** 1.13's {@code EntityType.Builder}, with Forge 1.13's tracking settings. */
    public static class Builder<T extends Entity> {

        private final Class<? extends T> entityClass;
        @Nullable
        private final Function<? super World, ? extends T> factory;
        private boolean serializable = true;
        private boolean summonable = true;
        private boolean velocityUpdates = true;
        private int trackingRange = 64;
        private int updateFrequency = 3;

        private Builder(Class<? extends T> entityClass, @Nullable Function<? super World, ? extends T> factory) {
            this.entityClass = entityClass;
            this.factory = factory;
        }

        @Nullable
        private java.util.function.BiFunction<EntityType<T>, World, T> factory116;
        private float width = 0.6F;
        private float height = 1.8F;
        private boolean immuneToFire;

        /** 1.16's {@code create(factory, classification)}, for the glue (1.16's factory takes the type). */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static Builder portium$create(@Nullable java.util.function.BiFunction factory, Object classification) {
            Builder builder = new Builder(null, null);
            builder.factory116 = factory;
            if (factory == null) builder.summonable = false;
            return builder;
        }

        /** 1.16's: a type that can't be made from its name (spawned by code only). */
        @Srg("func_220319_a")
        public static <T extends Entity> Builder<T> create(net.minecraft.entity.EnumCreatureType classification) {
            return portium$create(null, classification);
        }

        @Srg("func_220321_a")
        public Builder<T> size(float width, float height) {
            this.width = width;
            this.height = height;
            return this;
        }

        @Srg("func_220320_c")
        public Builder<T> immuneToFire() {
            this.immuneToFire = true;
            return this;
        }

        /** 1.16's blocks the entity doesn't take damage from (berry bushes, wither roses); 1.12 entities take none from them. */
        @Srg("func_233607_a_")
        public Builder<T> immuneTo(net.minecraft.block.Block... blocks) {
            return this;
        }

        /** 1.16's spawning far from players; 1.12 has no such rule for mod entities. */
        @Srg("func_225435_d")
        public Builder<T> spawnsFarFromPlayers() {
            return this;
        }

        /** 1.16's tracking range is in chunks; 1.12's in blocks. */
        @Srg("func_233606_a_")
        public Builder<T> trackingRange(int chunks) {
            this.trackingRange = chunks * 16;
            return this;
        }

        @Srg("func_233608_b_")
        public Builder<T> updateInterval(int ticks) {
            this.updateFrequency = ticks;
            return this;
        }

        // Forge 1.16's tracking settings.

        public Builder<T> setTrackingRange(int chunks) {
            return trackingRange(chunks);
        }

        public Builder<T> setUpdateInterval(int ticks) {
            return updateInterval(ticks);
        }

        public Builder<T> setShouldReceiveVelocityUpdates(boolean velocityUpdates) {
            this.velocityUpdates = velocityUpdates;
            return this;
        }

        /** Forge 1.16's client-side factory for spawn messages; Forge 1.12 spawns mod entities itself. */
        public Builder<T> setCustomClientFactory(java.util.function.BiFunction<?, ?, ?> factory) {
            return this;
        }

        @Srg("func_201757_a")
        public static <T extends Entity> Builder<T> create(Class<? extends T> entityClass, Function<? super World, ? extends T> factory) {
            return new Builder<>(entityClass, factory);
        }

        @Srg("func_201758_a")
        public static <T extends Entity> Builder<T> create(Class<? extends T> entityClass) {
            return new Builder<>(entityClass, null);
        }

        @Srg("func_200706_c")
        public Builder<T> disableSummoning() {
            this.summonable = false;
            return this;
        }

        @Srg("func_200705_b")
        public Builder<T> disableSerialization() {
            this.serializable = false;
            return this;
        }

        public Builder<T> tracker(int range, int updateFrequency, boolean sendVelocityUpdates) {
            this.trackingRange = range;
            this.updateFrequency = updateFrequency;
            this.velocityUpdates = sendVelocityUpdates;
            return this;
        }

        /** Forge 1.13's client-side factory for spawn messages; Forge 1.12 spawns mod entities itself. */
        public Builder<T> customClientFactory(Object factory) {
            return this;
        }

        @Srg("func_206830_a")
        public EntityType<T> build(String id) {
            EntityType<T> type = new EntityType<>(this.entityClass, this.factory, this.serializable, this.summonable, this.velocityUpdates,
                    this.trackingRange, this.updateFrequency, null);
            type.factory116 = this.factory116;
            type.width = this.width;
            type.height = this.height;
            type.immuneToFire = this.immuneToFire;
            return type;
        }
    }
}
