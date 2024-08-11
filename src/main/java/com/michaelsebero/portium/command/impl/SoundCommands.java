package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.arguments.EntityArgument;
import com.michaelsebero.portium.command.arguments.IdSuggestions;
import com.michaelsebero.portium.command.arguments.ResourceLocationArgument;
import com.michaelsebero.portium.command.arguments.Vec3Argument;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.arguments.FloatArgumentType;
import com.michaelsebero.portium.command.brigadier.builder.LiteralArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.builder.RequiredArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionProvider;
import com.michaelsebero.portium.compat.LegacySounds;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.network.play.server.SPacketCustomSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.Collection;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code playsound} and {@code stopsound} (1.13), taking 1.13's sound ids. */
public final class SoundCommands {

    private static final CommandExceptionType.Simple PLAY_FAILED = new CommandExceptionType.Simple(TextMessage.translate("commands.playsound.failed"));
    private static final SuggestionProvider<CommandSource> SOUNDS = (context, builder) -> IdSuggestions.suggest(LegacySounds.modernIds(), builder);

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        RequiredArgumentBuilder<CommandSource, ResourceLocation> sound = argument("sound", ResourceLocationArgument.resourceLocation()).suggests(SOUNDS);
        for (SoundCategory category : SoundCategory.values()) sound.then(playSource(category));
        dispatcher.register(literal("playsound").requires(source -> source.hasPermissionLevel(2)).then(sound));

        RequiredArgumentBuilder<CommandSource, ?> targets = argument("targets", EntityArgument.players())
                .executes(context -> stopSound(context.getSource(), EntityArgument.getPlayers(context, "targets"), null, null));
        for (SoundCategory category : SoundCategory.values()) {
            targets.then(literal(category.getName())
                    .executes(context -> stopSound(context.getSource(), EntityArgument.getPlayers(context, "targets"), category, null))
                    .then(argument("sound", ResourceLocationArgument.resourceLocation()).suggests(SOUNDS).executes(context ->
                            stopSound(context.getSource(), EntityArgument.getPlayers(context, "targets"), category, ResourceLocationArgument.getResourceLocation(context, "sound")))));
        }
        targets.then(literal("*")
                .executes(context -> stopSound(context.getSource(), EntityArgument.getPlayers(context, "targets"), null, null))
                .then(argument("sound", ResourceLocationArgument.resourceLocation()).suggests(SOUNDS).executes(context ->
                        stopSound(context.getSource(), EntityArgument.getPlayers(context, "targets"), null, ResourceLocationArgument.getResourceLocation(context, "sound")))));
        dispatcher.register(literal("stopsound").requires(source -> source.hasPermissionLevel(2)).then(targets));
    }

    private static LiteralArgumentBuilder<CommandSource> playSource(SoundCategory category) {
        return literal(category.getName()).then(argument("targets", EntityArgument.players())
                .executes(context -> playSound(context, category, context.getSource().getPos(), 1.0F, 1.0F, 0.0F))
                .then(argument("pos", Vec3Argument.vec3())
                        .executes(context -> playSound(context, category, Vec3Argument.getVec3(context, "pos"), 1.0F, 1.0F, 0.0F))
                        .then(argument("volume", FloatArgumentType.floatArg(0.0F))
                                .executes(context -> playSound(context, category, Vec3Argument.getVec3(context, "pos"), FloatArgumentType.getFloat(context, "volume"), 1.0F, 0.0F))
                                .then(argument("pitch", FloatArgumentType.floatArg(0.0F, 2.0F))
                                        .executes(context -> playSound(context, category, Vec3Argument.getVec3(context, "pos"), FloatArgumentType.getFloat(context, "volume"),
                                                FloatArgumentType.getFloat(context, "pitch"), 0.0F))
                                        .then(argument("minVolume", FloatArgumentType.floatArg(0.0F, 1.0F)).executes(context -> playSound(context, category,
                                                Vec3Argument.getVec3(context, "pos"), FloatArgumentType.getFloat(context, "volume"), FloatArgumentType.getFloat(context, "pitch"),
                                                FloatArgumentType.getFloat(context, "minVolume"))))))));
    }

    /** Players out of earshot hear the sound at {@code minVolume} from two blocks away in its direction, or not at all. */
    private static int playSound(CommandContext<CommandSource> context, SoundCategory category, Vec3d pos, float volume, float pitch, float minVolume)
            throws CommandSyntaxException {
        CommandSource source = context.getSource();
        Collection<EntityPlayerMP> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation id = ResourceLocationArgument.getResourceLocation(context, "sound");
        String name = LegacySounds.toLegacy(id).toString();
        double range = Math.pow(volume > 1.0F ? volume * 16.0D : 16.0D, 2.0D);
        int played = 0;
        for (EntityPlayerMP player : targets) {
            double dx = pos.x - player.posX;
            double dy = pos.y - player.posY;
            double dz = pos.z - player.posZ;
            double distance = dx * dx + dy * dy + dz * dz;
            Vec3d at = pos;
            float playVolume = volume;
            if (distance > range) {
                if (minVolume <= 0.0F) continue;
                double length = MathHelper.sqrt(distance);
                at = new Vec3d(player.posX + dx / length * 2.0D, player.posY + dy / length * 2.0D, player.posZ + dz / length * 2.0D);
                playVolume = minVolume;
            }
            player.connection.sendPacket(new SPacketCustomSound(name, category, at.x, at.y, at.z, playVolume, pitch));
            played++;
        }
        if (played == 0) throw PLAY_FAILED.create();
        if (targets.size() == 1) source.sendFeedback(new TextComponentTranslation("commands.playsound.success.single", id, targets.iterator().next().getDisplayName()), true);
        else source.sendFeedback(new TextComponentTranslation("commands.playsound.success.multiple", id, targets.size()), true);
        return played;
    }

    private static int stopSound(CommandSource source, Collection<EntityPlayerMP> targets, @Nullable SoundCategory category, @Nullable ResourceLocation id) {
        // 1.12 stops sounds through a plugin message: the category name, then the sound, each empty for "any".
        for (EntityPlayerMP player : targets) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeString(category == null ? "" : category.getName());
            buffer.writeString(id == null ? "" : LegacySounds.toLegacy(id).toString());
            player.connection.sendPacket(new SPacketCustomPayload("MC|StopSound", buffer));
        }
        if (category != null) {
            if (id != null) source.sendFeedback(new TextComponentTranslation("commands.stopsound.success.source.sound", id, category.getName()), true);
            else source.sendFeedback(new TextComponentTranslation("commands.stopsound.success.source.any", category.getName()), true);
        } else if (id != null) {
            source.sendFeedback(new TextComponentTranslation("commands.stopsound.success.sourceless.sound", id), true);
        } else {
            source.sendFeedback(new TextComponentTranslation("commands.stopsound.success.sourceless.any"), true);
        }
        return targets.size();
    }

    private SoundCommands() {}
}
