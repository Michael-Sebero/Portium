package com.michaelsebero.portium.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Runs commands through 1.12's command system on behalf of a 1.13 command source, so other mods' commands work in
 * data pack functions and under {@code execute}.
 */
public final class LegacyCommands {

    public static boolean isLegacyCommand(MinecraftServer server, String name) {
        return server.getCommandManager().getCommands().containsKey(name);
    }

    public static int execute(CommandSource source, String command) {
        return source.getServer().getCommandManager().executeCommand(sender(source), command);
    }

    public static ICommandSender sender(CommandSource source) {
        return new Sender(source);
    }

    /** A 1.12 command sender that reports the 1.13 source's position, entity, world and permission level. */
    private static final class Sender implements ICommandSender {

        private final CommandSource source;

        Sender(CommandSource source) {
            this.source = source;
        }

        @Override
        public String getName() {
            return this.source.getName();
        }

        @Override
        public ITextComponent getDisplayName() {
            return this.source.getDisplayName();
        }

        @Override
        public void sendMessage(ITextComponent component) {
            this.source.sendFeedback(component, false);
        }

        @Override
        public boolean canUseCommand(int permLevel, String commandName) {
            return this.source.hasPermissionLevel(permLevel);
        }

        @Override
        public BlockPos getPosition() {
            return new BlockPos(this.source.getPos());
        }

        @Override
        public Vec3d getPositionVector() {
            return this.source.getPos();
        }

        @Override
        public World getEntityWorld() {
            return this.source.getWorld();
        }

        @Nullable
        @Override
        public Entity getCommandSenderEntity() {
            return this.source.getEntity();
        }

        @Override
        public boolean sendCommandFeedback() {
            return !this.source.isFeedbackDisabled() && this.source.getSender().sendCommandFeedback();
        }

        @Override
        public MinecraftServer getServer() {
            return this.source.getServer();
        }
    }

    private LegacyCommands() {}
}
