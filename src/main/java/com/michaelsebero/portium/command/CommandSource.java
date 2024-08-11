package com.michaelsebero.portium.command;

import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.ResultConsumer;
import com.michaelsebero.portium.modern.Srg;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.CommandBlockBaseLogic;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;

/**
 * Who is running a 1.13 command and from where: the sender that receives feedback, plus the position, rotation,
 * world, entity, anchor and permission level that {@code execute} can change. Immutable; every change makes a copy.
 */
public class CommandSource {

    private final ICommandSender sender;
    private final Vec3d pos;
    private final Vec2 rotation;
    private final WorldServer world;
    private final int permissionLevel;
    private final String name;
    private final ITextComponent displayName;
    private final MinecraftServer server;
    private final boolean feedbackDisabled;
    @Nullable
    private final Entity entity;
    private final ResultConsumer<CommandSource> resultConsumer;
    private final EntityAnchor anchor;

    public CommandSource(ICommandSender sender, Vec3d pos, Vec2 rotation, WorldServer world, int permissionLevel, String name, ITextComponent displayName,
                         MinecraftServer server, @Nullable Entity entity, boolean feedbackDisabled, ResultConsumer<CommandSource> resultConsumer, EntityAnchor anchor) {
        this.sender = sender;
        this.pos = pos;
        this.rotation = rotation;
        this.world = world;
        this.permissionLevel = permissionLevel;
        this.name = name;
        this.displayName = displayName;
        this.server = server;
        this.entity = entity;
        this.feedbackDisabled = feedbackDisabled;
        this.resultConsumer = resultConsumer;
        this.anchor = anchor;
    }

    /** A copy of another source, for subclasses that add to it. */
    protected CommandSource(CommandSource other) {
        this(other.sender, other.pos, other.rotation, other.world, other.permissionLevel, other.name, other.displayName, other.server, other.entity,
                other.feedbackDisabled, other.resultConsumer, other.anchor);
    }

    /** The source for a 1.12 command sender: a player, command block, the console, an rcon client, a mod's sender. */
    public static CommandSource of(ICommandSender sender) {
        MinecraftServer server = sender.getServer();
        Entity entity = sender.getCommandSenderEntity();
        Vec2 rotation = entity == null ? Vec2.ZERO : new Vec2(entity.rotationPitch, entity.rotationYaw);
        WorldServer world = sender.getEntityWorld() instanceof WorldServer ? (WorldServer) sender.getEntityWorld() : server.getWorld(0);
        return new CommandSource(sender, sender.getPositionVector(), rotation, world, permissionLevelOf(sender), sender.getName(),
                sender.getDisplayName(), server, entity, false, (context, success, result) -> {}, EntityAnchor.FEET);
    }

    /** 1.13's {@code Entity.getCommandSource()}. */
    public static CommandSource ofEntity(Entity entity) {
        return of(entity);
    }

    /** 1.13's {@code MinecraftServer.getCommandSource()}. */
    public static CommandSource ofServer(MinecraftServer server) {
        return of(server);
    }

    /** 1.12 senders only answer "may you use level N?", so find the highest level they may use. */
    private static int permissionLevelOf(ICommandSender sender) {
        for (int level = 4; level > 0; level--) {
            if (sender.canUseCommand(level, "")) return level;
        }
        return 0;
    }

    @Srg("func_197024_a")
    public CommandSource withEntity(Entity entity) {
        if (this.entity == entity) return this;
        return new CommandSource(this.sender, this.pos, this.rotation, this.world, this.permissionLevel, entity.getName(), entity.getDisplayName(), this.server,
                entity, this.feedbackDisabled, this.resultConsumer, this.anchor);
    }

    @Srg("func_201009_a")
    public CommandSource withPos(Vec3d pos) {
        if (this.pos.equals(pos)) return this;
        return new CommandSource(this.sender, pos, this.rotation, this.world, this.permissionLevel, this.name, this.displayName, this.server, this.entity,
                this.feedbackDisabled, this.resultConsumer, this.anchor);
    }

    public CommandSource withRotation(Vec2 rotation) {
        return new CommandSource(this.sender, this.pos, rotation, this.world, this.permissionLevel, this.name, this.displayName, this.server, this.entity,
                this.feedbackDisabled, this.resultConsumer, this.anchor);
    }

    public CommandSource withResultConsumer(ResultConsumer<CommandSource> consumer) {
        return new CommandSource(this.sender, this.pos, this.rotation, this.world, this.permissionLevel, this.name, this.displayName, this.server, this.entity,
                this.feedbackDisabled, consumer, this.anchor);
    }

    /** Adds a consumer that runs after the current one. */
    public CommandSource withResultConsumer(ResultConsumer<CommandSource> consumer, java.util.function.BinaryOperator<ResultConsumer<CommandSource>> combiner) {
        return this.withResultConsumer(combiner.apply(this.resultConsumer, consumer));
    }

    @Srg("func_197031_a")
    public CommandSource withFeedbackDisabled() {
        if (this.feedbackDisabled) return this;
        return new CommandSource(this.sender, this.pos, this.rotation, this.world, this.permissionLevel, this.name, this.displayName, this.server, this.entity,
                true, this.resultConsumer, this.anchor);
    }

    @Srg("func_197033_a")
    public CommandSource withPermissionLevel(int level) {
        if (level == this.permissionLevel) return this;
        return new CommandSource(this.sender, this.pos, this.rotation, this.world, level, this.name, this.displayName, this.server, this.entity,
                this.feedbackDisabled, this.resultConsumer, this.anchor);
    }

    @Srg("func_197026_b")
    public CommandSource withMinPermissionLevel(int level) {
        return level > this.permissionLevel ? this.withPermissionLevel(level) : this;
    }

    public CommandSource withAnchor(EntityAnchor anchor) {
        if (anchor == this.anchor) return this;
        return new CommandSource(this.sender, this.pos, this.rotation, this.world, this.permissionLevel, this.name, this.displayName, this.server, this.entity,
                this.feedbackDisabled, this.resultConsumer, anchor);
    }

    @Srg("func_201003_a")
    public CommandSource withWorld(WorldServer world) {
        if (world == this.world) return this;
        return new CommandSource(this.sender, this.pos, this.rotation, world, this.permissionLevel, this.name, this.displayName, this.server, this.entity,
                this.feedbackDisabled, this.resultConsumer, this.anchor);
    }

    /** Turns to face an entity's anchor point. */
    public CommandSource facing(Entity target, EntityAnchor anchor) {
        return this.facing(anchor.apply(target));
    }

    /** Turns to face a point, as seen from this source's anchor. */
    @Srg("func_201005_b")
    public CommandSource facing(Vec3d target) {
        Vec3d from = this.entity == null ? this.pos : this.anchor.apply(this.pos, this.entity);
        double dx = target.x - from.x;
        double dy = target.y - from.y;
        double dz = target.z - from.z;
        double horizontal = MathHelper.sqrt(dx * dx + dz * dz);
        float pitch = MathHelper.wrapDegrees((float) -(MathHelper.atan2(dy, horizontal) * (180.0D / Math.PI)));
        float yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F);
        return this.withRotation(new Vec2(pitch, yaw));
    }

    /** The block the player typing a command looks at, which 1.13 offers as coordinates. Only known while suggesting. */
    @Nullable
    public BlockPos getTargetedBlock() {
        return null;
    }

    /** The UUID of the entity the player typing a command looks at, which 1.13 offers as a target. Only known while suggesting. */
    @Nullable
    public String getTargetedEntity() {
        return null;
    }

    public ICommandSender getSender() {
        return this.sender;
    }

    @Srg("func_197019_b")
    public ITextComponent getDisplayName() {
        return this.displayName;
    }

    @Srg("func_197037_c")
    public String getName() {
        return this.name;
    }

    @Srg("func_197034_c")
    public boolean hasPermissionLevel(int level) {
        return this.permissionLevel >= level;
    }

    public int getPermissionLevel() {
        return this.permissionLevel;
    }

    @Srg("func_197036_d")
    public Vec3d getPos() {
        return this.pos;
    }

    @Srg("func_197023_e")
    public WorldServer getWorld() {
        return this.world;
    }

    @Nullable
    @Srg("func_197022_f")
    public Entity getEntity() {
        return this.entity;
    }

    @Srg("func_197027_g")
    public Entity assertIsEntity() throws CommandSyntaxException {
        if (this.entity == null) throw CommandExceptions.REQUIRES_ENTITY.create();
        return this.entity;
    }

    @Srg("func_197035_h")
    public EntityPlayerMP asPlayer() throws CommandSyntaxException {
        if (!(this.entity instanceof EntityPlayerMP)) throw CommandExceptions.REQUIRES_PLAYER.create();
        return (EntityPlayerMP) this.entity;
    }

    public Vec2 getRotation() {
        return this.rotation;
    }

    @Srg("func_197028_i")
    public MinecraftServer getServer() {
        return this.server;
    }

    public EntityAnchor getEntityAnchor() {
        return this.anchor;
    }

    public ResultConsumer<CommandSource> getResultConsumer() {
        return this.resultConsumer;
    }

    public boolean isFeedbackDisabled() {
        return this.feedbackDisabled;
    }

    /** Tells the sender, and (when {@code allowLogging}) the server's operators and log, as 1.13 does. */
    @Srg("func_197030_a")
    public void sendFeedback(ITextComponent message, boolean allowLogging) {
        if (this.feedbackDisabled) return;
        if (this.sender.sendCommandFeedback()) this.sender.sendMessage(message);
        if (allowLogging && this.allowsLogging()) this.logFeedback(message);
    }

    private boolean allowsLogging() {
        if (this.sender instanceof CommandBlockBaseLogic) return this.server.worlds[0].getGameRules().getBoolean("commandBlockOutput");
        return this.sender instanceof EntityPlayer || this.sender == this.server || this.sender instanceof net.minecraft.network.rcon.RConConsoleSource;
    }

    private void logFeedback(ITextComponent message) {
        ITextComponent admin = new TextComponentTranslation("chat.type.admin", this.getDisplayName(), message);
        admin.getStyle().setColor(TextFormatting.GRAY).setItalic(true);
        if (this.server.worlds[0].getGameRules().getBoolean("sendCommandFeedback")) {
            for (EntityPlayerMP player : this.server.getPlayerList().getPlayers()) {
                if (player != this.sender && this.server.getPlayerList().canSendCommands(player.getGameProfile())) player.sendMessage(admin);
            }
        }
        if (this.sender != this.server && this.server.worlds[0].getGameRules().getBoolean("logAdminCommands")) this.server.sendMessage(admin);
    }

    @Srg("func_197021_a")
    public void sendErrorMessage(ITextComponent message) {
        if (this.feedbackDisabled) return;
        ITextComponent error = new TextComponentString("").appendSibling(message);
        error.getStyle().setColor(TextFormatting.RED);
        this.sender.sendMessage(error);
    }

    public void onCommandComplete(com.michaelsebero.portium.command.brigadier.context.CommandContext<CommandSource> context, boolean success, int result) {
        if (this.resultConsumer != null) this.resultConsumer.onCommandComplete(context, success, result);
    }
}
