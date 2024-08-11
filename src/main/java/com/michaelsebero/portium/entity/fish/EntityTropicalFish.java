package com.michaelsebero.portium.entity.fish;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Tropical fish (1.13) come in two body shapes, six patterns per shape and any pair of body and pattern colours.
 * The variant is packed as {@code shape | pattern << 8 | bodyColour << 16 | patternColour << 24}. Most natural
 * spawns use one of 22 named real-world species, and a school always shares one look.
 */
public class EntityTropicalFish extends EntityAbstractGroupFish {

    public static final String BUCKET_VARIANT_TAG = "BucketVariantTag";
    private static final DataParameter<Integer> VARIANT = EntityDataManager.createKey(EntityTropicalFish.class, DataSerializers.VARINT);

    public enum Pattern {
        KOB(0, 0), SUNSTREAK(0, 1), SNOOPER(0, 2), DASHER(0, 3), BRINELY(0, 4), SPOTTY(0, 5),
        FLOPPER(1, 0), STRIPEY(1, 1), GLITTER(1, 2), BLOCKFISH(1, 3), BETTY(1, 4), CLAYFISH(1, 5);

        private final int shape;
        private final int index;

        Pattern(int shape, int index) {
            this.shape = shape;
            this.index = index;
        }

        public static Pattern of(int shape, int index) {
            for (Pattern pattern : values()) {
                if (pattern.shape == shape && pattern.index == index) return pattern;
            }
            return KOB;
        }

        public String translationKey() {
            return "entity." + Tags.MOD_ID + ".tropical_fish.type." + this.name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** The named species, in 1.13's order (their names are {@code entity.portium.tropical_fish.predefined.<index>}). */
    public static final int[] PREDEFINED = {
            pack(Pattern.STRIPEY, EnumDyeColor.ORANGE, EnumDyeColor.GRAY),
            pack(Pattern.FLOPPER, EnumDyeColor.GRAY, EnumDyeColor.GRAY),
            pack(Pattern.FLOPPER, EnumDyeColor.GRAY, EnumDyeColor.BLUE),
            pack(Pattern.CLAYFISH, EnumDyeColor.WHITE, EnumDyeColor.GRAY),
            pack(Pattern.SUNSTREAK, EnumDyeColor.BLUE, EnumDyeColor.GRAY),
            pack(Pattern.KOB, EnumDyeColor.ORANGE, EnumDyeColor.WHITE),
            pack(Pattern.SPOTTY, EnumDyeColor.PINK, EnumDyeColor.LIGHT_BLUE),
            pack(Pattern.BLOCKFISH, EnumDyeColor.PURPLE, EnumDyeColor.YELLOW),
            pack(Pattern.CLAYFISH, EnumDyeColor.WHITE, EnumDyeColor.RED),
            pack(Pattern.SPOTTY, EnumDyeColor.WHITE, EnumDyeColor.YELLOW),
            pack(Pattern.GLITTER, EnumDyeColor.WHITE, EnumDyeColor.GRAY),
            pack(Pattern.CLAYFISH, EnumDyeColor.WHITE, EnumDyeColor.ORANGE),
            pack(Pattern.DASHER, EnumDyeColor.CYAN, EnumDyeColor.PINK),
            pack(Pattern.BRINELY, EnumDyeColor.LIME, EnumDyeColor.LIGHT_BLUE),
            pack(Pattern.BETTY, EnumDyeColor.RED, EnumDyeColor.WHITE),
            pack(Pattern.SNOOPER, EnumDyeColor.GRAY, EnumDyeColor.RED),
            pack(Pattern.BLOCKFISH, EnumDyeColor.RED, EnumDyeColor.WHITE),
            pack(Pattern.FLOPPER, EnumDyeColor.WHITE, EnumDyeColor.YELLOW),
            pack(Pattern.KOB, EnumDyeColor.RED, EnumDyeColor.WHITE),
            pack(Pattern.SUNSTREAK, EnumDyeColor.GRAY, EnumDyeColor.WHITE),
            pack(Pattern.DASHER, EnumDyeColor.CYAN, EnumDyeColor.YELLOW),
            pack(Pattern.FLOPPER, EnumDyeColor.YELLOW, EnumDyeColor.YELLOW)};

    public EntityTropicalFish(World world) {
        super(world);
        this.setSize(0.5F, 0.4F);
    }

    private static int pack(Pattern pattern, EnumDyeColor body, EnumDyeColor patternColor) {
        return pattern.shape | pattern.index << 8 | body.getMetadata() << 16 | patternColor.getMetadata() << 24;
    }

    public static int shape(int variant) {
        return Math.min(variant & 255, 1);
    }

    public static int patternIndex(int variant) {
        return Math.min(variant >> 8 & 255, 5);
    }

    public static EnumDyeColor bodyColor(int variant) {
        return EnumDyeColor.byMetadata(variant >> 16 & 255);
    }

    public static EnumDyeColor patternColor(int variant) {
        return EnumDyeColor.byMetadata(variant >> 24 & 255);
    }

    public static Pattern pattern(int variant) {
        return Pattern.of(shape(variant), patternIndex(variant));
    }

    /** The species index of a named variant, or -1. */
    public static int predefinedIndex(int variant) {
        for (int i = 0; i < PREDEFINED.length; i++) {
            if (PREDEFINED[i] == variant) return i;
        }
        return -1;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(VARIANT, 0);
    }

    public int getVariant() {
        return this.dataManager.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataManager.set(VARIANT, variant);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("Variant", this.getVariant());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setVariant(compound.getInteger("Variant"));
    }

    @Override
    protected void setBucketData(ItemStack bucket) {
        super.setBucketData(bucket);
        if (!bucket.hasTagCompound()) bucket.setTagCompound(new NBTTagCompound());
        bucket.getTagCompound().setInteger(BUCKET_VARIANT_TAG, this.getVariant());
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        data = super.onInitialSpawn(difficulty, data);
        if (data instanceof SchoolData) {
            this.setVariant(((SchoolData) data).variant);
        } else if (this.rand.nextFloat() < 0.9F) {
            int variant = PREDEFINED[this.rand.nextInt(PREDEFINED.length)];
            this.setVariant(variant);
            data = new SchoolData(((GroupData) data).leader, variant);
        } else {
            this.setVariant(this.rand.nextInt(2) | this.rand.nextInt(6) << 8 | this.rand.nextInt(15) << 16 | this.rand.nextInt(15) << 24);
        }
        return data;
    }

    /** A school that shares one look. */
    static class SchoolData extends GroupData {

        final int variant;

        SchoolData(EntityAbstractGroupFish leader, int variant) {
            super(leader);
            this.variant = variant;
        }
    }

    @Override
    protected ItemStack getBucket() {
        return new ItemStack(PortiumItems.TROPICAL_FISH_BUCKET);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return PortiumLootTables.TROPICAL_FISH;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_TROPICAL_FISH_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_TROPICAL_FISH_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return PortiumSounds.ENTITY_TROPICAL_FISH_FLOP;
    }
}
