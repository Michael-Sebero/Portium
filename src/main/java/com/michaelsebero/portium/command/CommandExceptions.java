package com.michaelsebero.portium.command;

import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;

/** Errors shared by the 1.13 commands, with 1.13's translation keys. */
public final class CommandExceptions {

    public static final CommandExceptionType.Simple REQUIRES_PLAYER = simple("permissions.requires.player");
    public static final CommandExceptionType.Simple REQUIRES_ENTITY = simple("permissions.requires.entity");
    public static final CommandExceptionType.Simple ENTITY_TOO_MANY = simple("argument.entity.toomany");
    public static final CommandExceptionType.Simple PLAYER_TOO_MANY = simple("argument.player.toomany");
    public static final CommandExceptionType.Simple PLAYER_ONLY = simple("argument.player.entities");
    public static final CommandExceptionType.Simple ENTITY_NOT_FOUND = simple("argument.entity.notfound.entity");
    public static final CommandExceptionType.Simple PLAYER_NOT_FOUND = simple("argument.entity.notfound.player");
    public static final CommandExceptionType.Simple PLAYER_UNKNOWN = simple("argument.player.unknown");
    public static final CommandExceptionType.Simple SELECTOR_NOT_ALLOWED = simple("argument.entity.selector.not_allowed");
    public static final CommandExceptionType.Simple POS_UNLOADED = simple("argument.pos.unloaded");
    public static final CommandExceptionType.Simple POS_OUT_OF_WORLD = simple("argument.pos.outofworld");
    public static final CommandExceptionType.Simple POS_INCOMPLETE = simple("argument.pos3d.incomplete");
    public static final CommandExceptionType.Simple POS2_INCOMPLETE = simple("argument.pos2d.incomplete");
    public static final CommandExceptionType.Simple POS_MIXED = simple("argument.pos.mixed");
    public static final CommandExceptionType.Simple ROTATION_INCOMPLETE = simple("argument.rotation.incomplete");
    public static final CommandExceptionType.Simple SWIZZLE_INVALID = simple("arguments.swizzle.invalid");
    public static final CommandExceptionType.Simple RANGE_EMPTY = simple("argument.range.empty");
    public static final CommandExceptionType.Simple RANGE_SWAPPED = simple("argument.range.swapped");
    public static final CommandExceptionType.Simple ID_INVALID = simple("argument.id.invalid");
    public static final CommandExceptionType.Simple TIME_INVALID_UNIT = simple("argument.time.invalid_unit");
    public static final CommandExceptionType.Simple OPERATION_INVALID = simple("arguments.operation.invalid");
    public static final CommandExceptionType.Simple OPERATION_DIVIDE_BY_ZERO = simple("arguments.operation.div0");
    public static final CommandExceptionType.Simple NBT_EXPECTED_KEY = simple("argument.nbt.expected.key");
    public static final CommandExceptionType.Simple NBT_EXPECTED_VALUE = simple("argument.nbt.expected.value");
    public static final CommandExceptionType.Simple NBT_TRAILING = simple("argument.nbt.trailing");
    public static final CommandExceptionType.Simple NBT_PATH_TOO_DEEP = simple("arguments.nbtpath.too_deep");
    public static final CommandExceptionType.Simple NBT_PATH_NODE_INVALID = simple("arguments.nbtpath.node.invalid");
    public static final CommandExceptionType.Simple SCORE_HOLDER_EMPTY = simple("argument.scoreHolder.empty");
    public static final CommandExceptionType.Simple ENTITY_INVALID_NAME = simple("argument.entity.invalid");
    public static final CommandExceptionType.Simple SELECTOR_MISSING = simple("argument.entity.selector.missing");
    public static final CommandExceptionType.Simple SELECTOR_OPTIONS_UNTERMINATED = simple("argument.entity.options.unterminated");
    public static final CommandExceptionType.Simple DISTANCE_NEGATIVE = simple("argument.entity.options.distance.negative");
    public static final CommandExceptionType.Simple LEVEL_NEGATIVE = simple("argument.entity.options.level.negative");
    public static final CommandExceptionType.Simple LIMIT_TOO_SMALL = simple("argument.entity.options.limit.toosmall");
    public static final CommandExceptionType.Simple COMPONENT_EXPECTED = simple("argument.component.invalid.empty");

    public static final CommandExceptionType.Dynamic UNKNOWN_SELECTOR = dynamic("argument.entity.selector.unknown");
    public static final CommandExceptionType.Dynamic UNKNOWN_OPTION = dynamic("argument.entity.options.unknown");
    public static final CommandExceptionType.Dynamic OPTION_INAPPLICABLE = dynamic("argument.entity.options.inapplicable");
    public static final CommandExceptionType.Dynamic OPTION_VALUELESS = dynamic("argument.entity.options.valueless");
    public static final CommandExceptionType.Dynamic SORT_IRREVERSIBLE = dynamic("argument.entity.options.sort.irreversible");
    public static final CommandExceptionType.Dynamic MODE_INVALID = dynamic("argument.entity.options.mode.invalid");
    public static final CommandExceptionType.Dynamic TYPE_INVALID = dynamic("argument.entity.options.type.invalid");
    public static final CommandExceptionType.Dynamic BLOCK_ID_INVALID = dynamic("argument.block.id.invalid");
    public static final CommandExceptionType.Dynamic BLOCK_TAG_UNKNOWN = dynamic("arguments.block.tag.unknown");
    public static final CommandExceptionType.Dynamic ITEM_ID_INVALID = dynamic("argument.item.id.invalid");
    public static final CommandExceptionType.Dynamic ITEM_TAG_UNKNOWN = dynamic("arguments.item.tag.unknown");
    public static final CommandExceptionType.Dynamic NBT_PATH_CHILD_INVALID = dynamic("arguments.nbtpath.child.invalid");
    public static final CommandExceptionType.Dynamic NBT_PATH_ELEMENT_INVALID = dynamic("arguments.nbtpath.element.invalid");
    public static final CommandExceptionType.Dynamic NBT_MIXED_LIST = dynamic("argument.nbt.list.mixed");
    public static final CommandExceptionType.Dynamic NBT_INVALID_ARRAY = dynamic("argument.nbt.array.invalid");
    public static final CommandExceptionType.Dynamic NBT_MIXED_ARRAY = dynamic("argument.nbt.array.mixed");
    public static final CommandExceptionType.Dynamic COMPONENT_INVALID = dynamic("argument.component.invalid");
    public static final CommandExceptionType.Dynamic OBJECTIVE_UNKNOWN = dynamic("arguments.objective.notFound");
    public static final CommandExceptionType.Dynamic OBJECTIVE_READ_ONLY = dynamic("arguments.objective.readonly");
    public static final CommandExceptionType.Dynamic CRITERIA_INVALID = dynamic("argument.criteria.invalid");
    public static final CommandExceptionType.Dynamic TEAM_UNKNOWN = dynamic("team.notFound");
    public static final CommandExceptionType.Dynamic COLOR_INVALID = dynamic("argument.color.invalid");
    public static final CommandExceptionType.Dynamic SLOT_UNKNOWN = dynamic("slot.unknown");
    public static final CommandExceptionType.Dynamic SCOREBOARD_SLOT_INVALID = dynamic("argument.scoreboardDisplaySlot.invalid");
    public static final CommandExceptionType.Dynamic ANCHOR_INVALID = dynamic("argument.anchor.invalid");
    public static final CommandExceptionType.Dynamic ENTITY_UNKNOWN = dynamic("entity.notFound");
    public static final CommandExceptionType.Dynamic EFFECT_UNKNOWN = dynamic("effect.effectNotFound");
    public static final CommandExceptionType.Dynamic ENCHANTMENT_UNKNOWN = dynamic("enchantment.unknown");
    public static final CommandExceptionType.Dynamic PARTICLE_UNKNOWN = dynamic("particle.notFound");
    public static final CommandExceptionType.Dynamic DIMENSION_INVALID = dynamic("argument.dimension.invalid");
    public static final CommandExceptionType.Dynamic FUNCTION_UNKNOWN = dynamic("arguments.function.unknown");
    public static final CommandExceptionType.Dynamic FUNCTION_TAG_UNKNOWN = dynamic("arguments.function.tag.unknown");
    public static final CommandExceptionType.Dynamic ADVANCEMENT_UNKNOWN = dynamic("advancement.advancementNotFound");
    public static final CommandExceptionType.Dynamic RECIPE_UNKNOWN = dynamic("recipe.notFound");
    public static final CommandExceptionType.Dynamic SUMMON_UNKNOWN = dynamic("entity.notFound");

    public static final CommandExceptionType.Dynamic2 BLOCK_PROPERTY_UNKNOWN = dynamic2("argument.block.property.unknown");
    public static final CommandExceptionType.Dynamic2 NBT_PATH_TYPE = dynamic2("arguments.nbtpath.child.invalid");

    private static final CommandExceptionType FAILURE = new CommandExceptionType() {};

    /** A translated failure, for command errors with more arguments than the typed exceptions take. */
    public static CommandSyntaxException failure(String key, Object... args) {
        return new CommandSyntaxException(FAILURE, TextMessage.translate(key, args));
    }

    private static CommandExceptionType.Simple simple(String key) {
        return new CommandExceptionType.Simple(TextMessage.translate(key));
    }

    private static CommandExceptionType.Dynamic dynamic(String key) {
        return new CommandExceptionType.Dynamic(arg -> TextMessage.translate(key, arg));
    }

    private static CommandExceptionType.Dynamic2 dynamic2(String key) {
        return new CommandExceptionType.Dynamic2((a, b) -> TextMessage.translate(key, a, b));
    }

    private CommandExceptions() {}
}
