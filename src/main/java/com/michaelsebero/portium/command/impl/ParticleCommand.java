package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.arguments.EntityArgument;
import com.michaelsebero.portium.command.arguments.ParticleArgument;
import com.michaelsebero.portium.command.arguments.Vec3Argument;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.arguments.FloatArgumentType;
import com.michaelsebero.portium.command.brigadier.arguments.IntegerArgumentType;
import com.michaelsebero.portium.command.brigadier.builder.LiteralArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.network.PortiumParticles;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

import java.util.Collection;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code particle} (1.13). */
public final class ParticleCommand {

    private static final CommandExceptionType.Simple FAILED = new CommandExceptionType.Simple(TextMessage.translate("commands.particle.failed"));

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("particle").requires(source -> source.hasPermissionLevel(2))
                .then(argument("name", ParticleArgument.particle())
                        .executes(context -> spawn(context, context.getSource().getPos(), Vec3d.ZERO, 0.0F, 0, false, allPlayers(context)))
                        .then(argument("pos", Vec3Argument.vec3())
                                .executes(context -> spawn(context, Vec3Argument.getVec3(context, "pos"), Vec3d.ZERO, 0.0F, 0, false, allPlayers(context)))
                                .then(argument("delta", Vec3Argument.vec3(false)).then(argument("speed", FloatArgumentType.floatArg(0.0F))
                                        .then(argument("count", IntegerArgumentType.integer(0))
                                                .executes(context -> spawn(context, false, allPlayers(context)))
                                                .then(mode("force", true))
                                                .then(mode("normal", false))))))));
    }

    private static LiteralArgumentBuilder<CommandSource> mode(String name, boolean force) {
        return literal(name).executes(context -> spawn(context, force, allPlayers(context)))
                .then(argument("viewers", EntityArgument.players()).executes(context -> spawn(context, force, EntityArgument.getPlayers(context, "viewers"))));
    }

    private static Collection<EntityPlayerMP> allPlayers(CommandContext<CommandSource> context) {
        return context.getSource().getServer().getPlayerList().getPlayers();
    }

    private static int spawn(CommandContext<CommandSource> context, boolean force, Collection<EntityPlayerMP> viewers) throws CommandSyntaxException {
        return spawn(context, Vec3Argument.getVec3(context, "pos"), Vec3Argument.getVec3(context, "delta"), FloatArgumentType.getFloat(context, "speed"),
                IntegerArgumentType.getInteger(context, "count"), force, viewers);
    }

    private static int spawn(CommandContext<CommandSource> context, Vec3d pos, Vec3d delta, float speed, int count, boolean force,
                             Collection<EntityPlayerMP> viewers) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        ParticleArgument.Particle particle = ParticleArgument.getParticle(context, "name");
        WorldServer world = source.getWorld();
        int shown = 0;
        for (EntityPlayerMP player : viewers) {
            if (player.getServerWorld() != world || !PortiumParticles.canSee(player, pos.x, pos.y, pos.z, force)) continue;
            PortiumParticles.send(player, particle, pos.x, pos.y, pos.z, delta.x, delta.y, delta.z, speed, count, force);
            shown++;
        }
        if (shown == 0) throw FAILED.create();
        source.sendFeedback(new TextComponentTranslation("commands.particle.success", particle.text), true);
        return shown;
    }

    private ParticleCommand() {}
}
