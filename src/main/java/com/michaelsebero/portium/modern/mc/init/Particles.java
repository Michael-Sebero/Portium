package com.michaelsebero.portium.modern.mc.init;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.particles.BasicParticleType;
import com.michaelsebero.portium.modern.mc.particles.BlockParticleData;
import com.michaelsebero.portium.modern.mc.particles.IParticleData;
import com.michaelsebero.portium.modern.mc.particles.ItemParticleData;
import com.michaelsebero.portium.modern.mc.particles.ParticleType;
import com.michaelsebero.portium.modern.mc.particles.RedstoneParticleData;
import net.minecraft.util.ResourceLocation;

/** 1.13's {@code net.minecraft.init.Particles}. Which 1.12 or Portium particle each one shows is up to {@code ParticleShims}. */
public final class Particles {

    @Srg("field_197608_a")
    public static final BasicParticleType AMBIENT_ENTITY_EFFECT = basic("ambient_entity_effect", false);
    @Srg("field_197609_b")
    public static final BasicParticleType ANGRY_VILLAGER = basic("angry_villager", false);
    @Srg("field_197610_c")
    public static final BasicParticleType BARRIER = basic("barrier", false);
    @Srg("field_197611_d")
    public static final ParticleType<BlockParticleData> BLOCK = typed("block", false);
    @Srg("field_197612_e")
    public static final BasicParticleType BUBBLE = basic("bubble", false);
    @Srg("field_203220_f")
    public static final BasicParticleType BUBBLE_COLUMN_UP = basic("bubble_column_up", false);
    @Srg("field_197613_f")
    public static final BasicParticleType CLOUD = basic("cloud", false);
    @Srg("field_197614_g")
    public static final BasicParticleType CRIT = basic("crit", false);
    @Srg("field_197615_h")
    public static final BasicParticleType DAMAGE_INDICATOR = basic("damage_indicator", true);
    @Srg("field_197616_i")
    public static final BasicParticleType DRAGON_BREATH = basic("dragon_breath", false);
    @Srg("field_197617_j")
    public static final BasicParticleType DRIPPING_LAVA = basic("dripping_lava", false);
    @Srg("field_197618_k")
    public static final BasicParticleType DRIPPING_WATER = basic("dripping_water", false);
    @Srg("field_197619_l")
    public static final ParticleType<RedstoneParticleData> DUST = typed("dust", false);
    @Srg("field_197620_m")
    public static final BasicParticleType EFFECT = basic("effect", false);
    @Srg("field_197621_n")
    public static final BasicParticleType ELDER_GUARDIAN = basic("elder_guardian", true);
    @Srg("field_197622_o")
    public static final BasicParticleType ENCHANTED_HIT = basic("enchanted_hit", false);
    @Srg("field_197623_p")
    public static final BasicParticleType ENCHANT = basic("enchant", false);
    @Srg("field_197624_q")
    public static final BasicParticleType END_ROD = basic("end_rod", false);
    @Srg("field_197625_r")
    public static final BasicParticleType ENTITY_EFFECT = basic("entity_effect", false);
    @Srg("field_197626_s")
    public static final BasicParticleType EXPLOSION_EMITTER = basic("explosion_emitter", true);
    @Srg("field_197627_t")
    public static final BasicParticleType EXPLOSION = basic("explosion", true);
    @Srg("field_197628_u")
    public static final ParticleType<BlockParticleData> FALLING_DUST = typed("falling_dust", false);
    @Srg("field_197629_v")
    public static final BasicParticleType FIREWORK = basic("firework", false);
    @Srg("field_197630_w")
    public static final BasicParticleType FISHING = basic("fishing", false);
    @Srg("field_197631_x")
    public static final BasicParticleType FLAME = basic("flame", false);
    @Srg("field_197632_y")
    public static final BasicParticleType HAPPY_VILLAGER = basic("happy_villager", false);
    @Srg("field_197633_z")
    public static final BasicParticleType HEART = basic("heart", false);
    @Srg("field_197590_A")
    public static final BasicParticleType INSTANT_EFFECT = basic("instant_effect", false);
    @Srg("field_197591_B")
    public static final ParticleType<ItemParticleData> ITEM = typed("item", false);
    @Srg("field_197592_C")
    public static final BasicParticleType ITEM_SLIME = basic("item_slime", false);
    @Srg("field_197593_D")
    public static final BasicParticleType ITEM_SNOWBALL = basic("item_snowball", false);
    @Srg("field_197594_E")
    public static final BasicParticleType LARGE_SMOKE = basic("large_smoke", false);
    @Srg("field_197595_F")
    public static final BasicParticleType LAVA = basic("lava", false);
    @Srg("field_197596_G")
    public static final BasicParticleType MYCELIUM = basic("mycelium", false);
    @Srg("field_197597_H")
    public static final BasicParticleType NOTE = basic("note", false);
    @Srg("field_197598_I")
    public static final BasicParticleType POOF = basic("poof", true);
    @Srg("field_197599_J")
    public static final BasicParticleType PORTAL = basic("portal", false);
    @Srg("field_197600_K")
    public static final BasicParticleType RAIN = basic("rain", false);
    @Srg("field_197601_L")
    public static final BasicParticleType SMOKE = basic("smoke", false);
    @Srg("field_197602_M")
    public static final BasicParticleType SPIT = basic("spit", true);
    @Srg("field_197603_N")
    public static final BasicParticleType SWEEP_ATTACK = basic("sweep_attack", true);
    @Srg("field_197604_O")
    public static final BasicParticleType TOTEM_OF_UNDYING = basic("totem_of_undying", false);
    @Srg("field_197605_P")
    public static final BasicParticleType UNDERWATER = basic("underwater", false);
    @Srg("field_197606_Q")
    public static final BasicParticleType SPLASH = basic("splash", false);
    @Srg("field_197607_R")
    public static final BasicParticleType WITCH = basic("witch", false);
    @Srg("field_203217_T")
    public static final BasicParticleType BUBBLE_POP = basic("bubble_pop", false);
    @Srg("field_203218_U")
    public static final BasicParticleType CURRENT_DOWN = basic("current_down", false);
    @Srg("field_203219_V")
    public static final BasicParticleType SQUID_INK = basic("squid_ink", true);
    @Srg("field_205167_W")
    public static final BasicParticleType NAUTILUS = basic("nautilus", false);
    @Srg("field_206864_X")
    public static final BasicParticleType DOLPHIN = basic("dolphin", false);

    private static ResourceLocation id(String name) {
        return new ResourceLocation(name);
    }

    private static <T extends IParticleData> ParticleType<T> typed(String name, boolean alwaysShow) {
        return new ParticleType<T>(id(name), alwaysShow) {};
    }

    private static BasicParticleType basic(String name, boolean alwaysShow) {
        return new BasicParticleType(id(name), alwaysShow) {};
    }

    private Particles() {}
}
