package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import com.michaelsebero.portium.compat.LegacyBlockStates;
import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.proxy.ParticleKind;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.EnumParticleTypes;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * A particle in 1.13's naming ({@code minecraft:dust 1 0 0 1}, {@code minecraft:block minecraft:stone}). The
 * particles 1.13 added that 1.12 lacks (bubble columns, nautilus, dolphin) are Portium's own.
 */
public class ParticleArgument implements ArgumentType<ParticleArgument.Particle> {

    /** A resolved particle: either a 1.12 particle with its arguments, or one of Portium's. */
    public static final class Particle {

        @Nullable
        public final EnumParticleTypes vanilla;
        public final int[] arguments;
        @Nullable
        public final ParticleKind portium;
        /** For {@code dust}: red, green, blue. */
        @Nullable
        public final float[] color;
        /** The particle as it was typed, for command feedback. */
        public final String text;

        Particle(@Nullable EnumParticleTypes vanilla, int[] arguments, @Nullable ParticleKind portium, @Nullable float[] color, String text) {
            this.vanilla = vanilla;
            this.arguments = arguments;
            this.portium = portium;
            this.color = color;
            this.text = text;
        }
    }

    private static final Map<String, EnumParticleTypes> VANILLA = new HashMap<>();
    private static final Map<String, ParticleKind> PORTIUM = new HashMap<>();

    static {
        VANILLA.put("ambient_entity_effect", EnumParticleTypes.SPELL_MOB_AMBIENT);
        VANILLA.put("angry_villager", EnumParticleTypes.VILLAGER_ANGRY);
        VANILLA.put("barrier", EnumParticleTypes.BARRIER);
        VANILLA.put("bubble", EnumParticleTypes.WATER_BUBBLE);
        VANILLA.put("cloud", EnumParticleTypes.CLOUD);
        VANILLA.put("crit", EnumParticleTypes.CRIT);
        VANILLA.put("damage_indicator", EnumParticleTypes.DAMAGE_INDICATOR);
        VANILLA.put("dragon_breath", EnumParticleTypes.DRAGON_BREATH);
        VANILLA.put("dripping_lava", EnumParticleTypes.DRIP_LAVA);
        VANILLA.put("dripping_water", EnumParticleTypes.DRIP_WATER);
        VANILLA.put("effect", EnumParticleTypes.SPELL);
        VANILLA.put("elder_guardian", EnumParticleTypes.MOB_APPEARANCE);
        VANILLA.put("enchanted_hit", EnumParticleTypes.CRIT_MAGIC);
        VANILLA.put("enchant", EnumParticleTypes.ENCHANTMENT_TABLE);
        VANILLA.put("end_rod", EnumParticleTypes.END_ROD);
        VANILLA.put("entity_effect", EnumParticleTypes.SPELL_MOB);
        VANILLA.put("explosion_emitter", EnumParticleTypes.EXPLOSION_HUGE);
        VANILLA.put("explosion", EnumParticleTypes.EXPLOSION_LARGE);
        VANILLA.put("firework", EnumParticleTypes.FIREWORKS_SPARK);
        VANILLA.put("fishing", EnumParticleTypes.WATER_WAKE);
        VANILLA.put("flame", EnumParticleTypes.FLAME);
        VANILLA.put("happy_villager", EnumParticleTypes.VILLAGER_HAPPY);
        VANILLA.put("heart", EnumParticleTypes.HEART);
        VANILLA.put("instant_effect", EnumParticleTypes.SPELL_INSTANT);
        VANILLA.put("item_slime", EnumParticleTypes.SLIME);
        VANILLA.put("item_snowball", EnumParticleTypes.SNOWBALL);
        VANILLA.put("large_smoke", EnumParticleTypes.SMOKE_LARGE);
        VANILLA.put("lava", EnumParticleTypes.LAVA);
        VANILLA.put("mycelium", EnumParticleTypes.TOWN_AURA);
        VANILLA.put("note", EnumParticleTypes.NOTE);
        VANILLA.put("poof", EnumParticleTypes.EXPLOSION_NORMAL);
        VANILLA.put("portal", EnumParticleTypes.PORTAL);
        VANILLA.put("rain", EnumParticleTypes.WATER_DROP);
        VANILLA.put("smoke", EnumParticleTypes.SMOKE_NORMAL);
        VANILLA.put("spit", EnumParticleTypes.SPIT);
        VANILLA.put("sweep_attack", EnumParticleTypes.SWEEP_ATTACK);
        VANILLA.put("totem_of_undying", EnumParticleTypes.TOTEM);
        VANILLA.put("underwater", EnumParticleTypes.SUSPENDED);
        VANILLA.put("splash", EnumParticleTypes.WATER_SPLASH);
        VANILLA.put("witch", EnumParticleTypes.SPELL_WITCH);
        VANILLA.put("dust", EnumParticleTypes.REDSTONE);
        VANILLA.put("block", EnumParticleTypes.BLOCK_CRACK);
        VANILLA.put("falling_dust", EnumParticleTypes.FALLING_DUST);
        VANILLA.put("item", EnumParticleTypes.ITEM_CRACK);
        PORTIUM.put("bubble_column_up", ParticleKind.BUBBLE_COLUMN_UP);
        PORTIUM.put("current_down", ParticleKind.CURRENT_DOWN);
        PORTIUM.put("nautilus", ParticleKind.NAUTILUS);
        PORTIUM.put("dolphin", ParticleKind.DOLPHIN);
        PORTIUM.put("squid_ink", ParticleKind.SQUID_INK);
    }

    public static ParticleArgument particle() {
        return new ParticleArgument();
    }

    public static Particle getParticle(CommandContext<?> context, String name) {
        return context.getArgument(name, Particle.class);
    }

    /**
     * A particle by its 1.13 name (without namespace), or null if neither 1.12 nor Portium has it. {@code arguments}
     * are 1.12's (block state id; item id and data), {@code color} is dust's.
     */
    @Nullable
    public static Particle byName(String id, int[] arguments, @Nullable float[] color, String text) {
        ParticleKind portium = PORTIUM.get(id);
        EnumParticleTypes type = portium == null ? VANILLA.get(id) : null;
        return portium == null && type == null ? null : new Particle(type, arguments, portium, color, text);
    }

    @Override
    public Particle parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        String id = ResourceLocationArgument.read(reader).getPath();
        ParticleKind portium = PORTIUM.get(id);
        EnumParticleTypes type = portium == null ? VANILLA.get(id) : null;
        if (portium == null && type == null) {
            reader.setCursor(start);
            throw CommandExceptions.PARTICLE_UNKNOWN.createWithContext(reader, id);
        }
        int[] arguments = new int[0];
        float[] color = null;
        switch (portium != null ? "" : id) {
            case "dust": {
                reader.expect(' ');
                float red = reader.readFloat();
                reader.expect(' ');
                float green = reader.readFloat();
                reader.expect(' ');
                float blue = reader.readFloat();
                reader.expect(' ');
                reader.readFloat();
                color = new float[] {red, green, blue};
                break;
            }
            case "block":
            case "falling_dust": {
                reader.expect(' ');
                BlockStateParser parser = new BlockStateParser(reader, false).parse(false);
                arguments = new int[] {Block.getStateId(LegacyBlockStates.convert(parser.getId().toString(), parser.getProperties()))};
                break;
            }
            case "item": {
                reader.expect(' ');
                LegacyItem item = new ItemArgument().parse(reader).getItem();
                arguments = new int[] {Item.getIdFromItem(item.getItem()), Math.max(0, item.getMeta())};
                break;
            }
            default:
                break;
        }
        return new Particle(type, arguments, portium, color, reader.getString().substring(start, reader.getCursor()));
    }

    @Override
    public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        TreeSet<String> names = new TreeSet<>();
        for (String name : VANILLA.keySet()) names.add("minecraft:" + name);
        for (String name : PORTIUM.keySet()) names.add("minecraft:" + name);
        return IdSuggestions.suggest(names, builder);
    }
}
