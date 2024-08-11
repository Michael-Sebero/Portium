package com.michaelsebero.portium.modern.mc.block;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;

import javax.annotation.Nullable;

/**
 * 1.13's {@code net.minecraft.block.Block.Properties}: what a block is made with. Named with the {@code $} of a nested
 * class so it can stand in for one while 1.12's {@code Block} stays the real thing. Applied to 1.12's block fields as
 * the block's constructor finishes (see {@code BlockShims}).
 */
public class Block$Properties {

    final Material material;
    final MapColor mapColor;
    boolean blocksMovement = true;
    SoundType soundType = SoundType.STONE;
    int lightValue;
    float resistance;
    float hardness;
    boolean needsRandomTick;
    float slipperiness = 0.6F;
    boolean variableOpacity;
    // 1.16's additions.
    boolean solid = true;
    @Nullable
    java.util.function.ToIntFunction<IBlockState> lightLevel;
    boolean requiresTool;
    boolean noDrops;
    boolean air;
    @Nullable
    String harvestTool;
    int harvestLevel = -1;

    private Block$Properties(Material material, MapColor mapColor) {
        this.material = material;
        this.mapColor = mapColor;
    }

    @Srg("func_200945_a")
    public static Block$Properties create(Material material) {
        return new Block$Properties(material, material.getMaterialMapColor());
    }

    @Srg("func_200952_a")
    public static Block$Properties create(Material material, EnumDyeColor color) {
        return new Block$Properties(material, MapColor.getBlockColor(color));
    }

    @Srg("func_200949_a")
    public static Block$Properties create(Material material, MapColor mapColor) {
        return new Block$Properties(material, mapColor);
    }

    /** A copy of an existing block's properties. */
    @Srg("func_200950_a")
    public static Block$Properties from(Block block) {
        Block$Properties properties = new Block$Properties(block.getDefaultState().getMaterial(), block.getDefaultState().getMaterial().getMaterialMapColor());
        properties.soundType = block.getSoundType();
        properties.lightValue = block.getDefaultState().getLightValue();
        properties.hardness = block.getBlockHardness(block.getDefaultState(), null, null);
        properties.resistance = block.getExplosionResistance(null);
        properties.needsRandomTick = block.getTickRandomly();
        properties.slipperiness = block.slipperiness;
        return properties;
    }

    @Srg("func_200942_a")
    public Block$Properties doesNotBlockMovement() {
        this.blocksMovement = false;
        return this;
    }

    @Srg("func_200941_a")
    public Block$Properties slipperiness(float slipperiness) {
        this.slipperiness = slipperiness;
        return this;
    }

    @Srg("func_200947_a")
    public Block$Properties sound(SoundType soundType) {
        this.soundType = soundType;
        return this;
    }

    @Srg("func_200951_a")
    public Block$Properties lightValue(int lightValue) {
        this.lightValue = lightValue;
        return this;
    }

    @Srg("func_200948_a")
    public Block$Properties hardnessAndResistance(float hardness, float resistance) {
        this.hardness = hardness;
        this.resistance = Math.max(0.0F, resistance);
        return this;
    }

    @Srg("func_200946_b")
    public Block$Properties zeroHardnessAndResistance() {
        return hardnessAndResistance(0.0F);
    }

    @Srg("func_200943_b")
    public Block$Properties hardnessAndResistance(float hardnessAndResistance) {
        return hardnessAndResistance(hardnessAndResistance, hardnessAndResistance);
    }

    @Srg("func_200944_c")
    public Block$Properties needsRandomTick() {
        this.needsRandomTick = true;
        return this;
    }

    @Srg("func_208770_d")
    public Block$Properties variableOpacity() {
        this.variableOpacity = true;
        return this;
    }

    // ---- 1.16's builder methods ----

    /** 1.16's: made of a material whose map colour depends on the state; 1.12 colours a block by its material. */
    @Srg("func_235836_a_")
    public static Block$Properties create(Material material, java.util.function.Function<IBlockState, MapColor> color) {
        return new Block$Properties(material, material.getMaterialMapColor());
    }

    /** 1.16's: not a solid block for rendering and light (glass, leaves); 1.12 then doesn't treat it as opaque. */
    @Srg("func_226896_b_")
    public Block$Properties notSolid() {
        this.solid = false;
        return this;
    }

    @Srg("func_226897_b_")
    public Block$Properties speedFactor(float factor) {
        return this;
    }

    @Srg("func_226898_c_")
    public Block$Properties jumpFactor(float factor) {
        return this;
    }

    /** 1.16's light by state. */
    @Srg("func_235838_a_")
    public Block$Properties setLightLevel(java.util.function.ToIntFunction<IBlockState> level) {
        this.lightLevel = level;
        return this;
    }

    @Srg("func_222380_e")
    public Block$Properties noDrops() {
        this.noDrops = true;
        return this;
    }

    @Srg("func_222379_b")
    public Block$Properties lootFrom(Block block) {
        return this;
    }

    @Srg("func_235859_g_")
    public Block$Properties setAir() {
        this.air = true;
        return this;
    }

    /** 1.16's: only drops when broken with the right tool. */
    @Srg("func_235861_h_")
    public Block$Properties setRequiresTool() {
        this.requiresTool = true;
        return this;
    }

    // 1.16's per-position predicates (taking its own predicate types, which only 1.16 code can name). 1.12 works these
    // out from the block's material and shape.

    @Srg("func_235827_a_")
    public Block$Properties setAllowsSpawn(Object predicate) {
        return this;
    }

    @Srg("func_235828_a_")
    public Block$Properties setOpaque(Object predicate) {
        return this;
    }

    @Srg("func_235842_b_")
    public Block$Properties setSuffocates(Object predicate) {
        return this;
    }

    @Srg("func_235847_c_")
    public Block$Properties setBlocksVision(Object predicate) {
        return this;
    }

    @Srg("func_235852_d_")
    public Block$Properties setNeedsPostProcessing(Object predicate) {
        return this;
    }

    @Srg("func_235856_e_")
    public Block$Properties setEmmisiveRendering(Object predicate) {
        return this;
    }

    /** Forge 1.16's harvest level (its tool type comes through the glue, by name). */
    public Block$Properties harvestLevel(int level) {
        this.harvestLevel = level;
        return this;
    }

    /** For the glue: Forge 1.16's harvest tool, by 1.12's name for it. */
    public Block$Properties portium$harvestTool(String tool) {
        this.harvestTool = tool;
        return this;
    }

    public boolean isSolid() {
        return this.solid;
    }

    @Nullable
    public java.util.function.ToIntFunction<IBlockState> getLightLevel() {
        return this.lightLevel;
    }

    @Nullable
    public String getHarvestTool() {
        return this.harvestTool;
    }

    public int getHarvestLevel() {
        return this.harvestLevel;
    }

    public Material getMaterial() {
        return this.material;
    }

    public MapColor getMapColor() {
        return this.mapColor;
    }

    public boolean blocksMovement() {
        return this.blocksMovement;
    }

    public SoundType getSoundType() {
        return this.soundType;
    }

    public int getLightValue() {
        return this.lightValue;
    }

    /** In 1.13's units, a fifth of 1.12's stored resistance. */
    public float getResistance() {
        return this.resistance;
    }

    public float getHardness() {
        return this.hardness;
    }

    public boolean getNeedsRandomTick() {
        return this.needsRandomTick;
    }

    public float getSlipperiness() {
        return this.slipperiness;
    }
}
