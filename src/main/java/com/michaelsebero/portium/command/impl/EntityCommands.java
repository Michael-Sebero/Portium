package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.EntityAnchor;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.Vec2;
import com.michaelsebero.portium.command.arguments.Coordinates;
import com.michaelsebero.portium.command.arguments.EntityArgument;
import com.michaelsebero.portium.command.arguments.MiscArguments;
import com.michaelsebero.portium.command.arguments.NbtArguments;
import com.michaelsebero.portium.command.arguments.RegistryArguments;
import com.michaelsebero.portium.command.arguments.RotationArgument;
import com.michaelsebero.portium.command.arguments.Vec3Argument;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.arguments.StringArgumentType;
import com.michaelsebero.portium.command.brigadier.tree.LiteralCommandNode;
import com.michaelsebero.portium.compat.LegacyEntityNbt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code kill}, {@code tag}, {@code summon} and {@code teleport}/{@code tp} (1.13). */
public final class EntityCommands {

    private static final CommandExceptionType.Simple TAG_ADD_FAILED = simple("commands.tag.add.failed");
    private static final CommandExceptionType.Simple TAG_REMOVE_FAILED = simple("commands.tag.remove.failed");
    private static final CommandExceptionType.Simple SUMMON_FAILED = simple("commands.summon.failed");

    private static CommandExceptionType.Simple simple(String key) {
        return new CommandExceptionType.Simple(TextMessage.translate(key));
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("kill").requires(source -> source.hasPermissionLevel(2))
                .executes(context -> kill(context.getSource(), Collections.singleton(context.getSource().assertIsEntity())))
                .then(argument("targets", EntityArgument.entities()).executes(context -> kill(context.getSource(), EntityArgument.getEntities(context, "targets")))));

        dispatcher.register(literal("tag").requires(source -> source.hasPermissionLevel(2)).then(argument("targets", EntityArgument.entities())
                .then(literal("add").then(argument("name", StringArgumentType.word()).executes(context ->
                        addTag(context.getSource(), EntityArgument.getEntities(context, "targets"), StringArgumentType.getString(context, "name")))))
                .then(literal("remove").then(argument("name", StringArgumentType.word()).executes(context ->
                        removeTag(context.getSource(), EntityArgument.getEntities(context, "targets"), StringArgumentType.getString(context, "name")))))
                .then(literal("list").executes(context -> listTags(context.getSource(), EntityArgument.getEntities(context, "targets"))))));

        dispatcher.register(literal("summon").requires(source -> source.hasPermissionLevel(2)).then(argument("entity", RegistryArguments.summonable())
                .executes(context -> summon(context.getSource(), context.getArgument("entity", ResourceLocation.class), context.getSource().getPos(), new NBTTagCompound(), true))
                .then(argument("pos", Vec3Argument.vec3()).executes(context -> summon(context.getSource(), context.getArgument("entity", ResourceLocation.class),
                        Vec3Argument.getVec3(context, "pos"), new NBTTagCompound(), true))
                        .then(argument("nbt", NbtArguments.compound()).executes(context -> summon(context.getSource(), context.getArgument("entity", ResourceLocation.class),
                                Vec3Argument.getVec3(context, "pos"), NbtArguments.getCompound(context, "nbt"), false))))));

        LiteralCommandNode<CommandSource> teleport = dispatcher.register(literal("teleport").requires(source -> source.hasPermissionLevel(2))
                .then(argument("targets", EntityArgument.entities())
                        .then(argument("location", Vec3Argument.vec3())
                                .executes(context -> toPos(context.getSource(), EntityArgument.getEntities(context, "targets"), context.getSource().getWorld(),
                                        Vec3Argument.getLocation(context, "location"), null, null))
                                .then(argument("rotation", RotationArgument.rotation()).executes(context -> toPos(context.getSource(), EntityArgument.getEntities(context, "targets"),
                                        context.getSource().getWorld(), Vec3Argument.getLocation(context, "location"), context.getArgument("rotation", Coordinates.class), null)))
                                .then(literal("facing")
                                        .then(literal("entity").then(argument("facingEntity", EntityArgument.entity())
                                                .executes(context -> toPos(context.getSource(), EntityArgument.getEntities(context, "targets"), context.getSource().getWorld(),
                                                        Vec3Argument.getLocation(context, "location"), null, new Facing(EntityArgument.getEntity(context, "facingEntity"), EntityAnchor.FEET)))
                                                .then(argument("facingAnchor", MiscArguments.anchor()).executes(context -> toPos(context.getSource(), EntityArgument.getEntities(context, "targets"),
                                                        context.getSource().getWorld(), Vec3Argument.getLocation(context, "location"), null,
                                                        new Facing(EntityArgument.getEntity(context, "facingEntity"), context.getArgument("facingAnchor", EntityAnchor.class)))))))
                                        .then(argument("facingLocation", Vec3Argument.vec3()).executes(context -> toPos(context.getSource(), EntityArgument.getEntities(context, "targets"),
                                                context.getSource().getWorld(), Vec3Argument.getLocation(context, "location"), null, new Facing(Vec3Argument.getVec3(context, "facingLocation")))))))
                        .then(argument("destination", EntityArgument.entity()).executes(context ->
                                toEntity(context.getSource(), EntityArgument.getEntities(context, "targets"), EntityArgument.getEntity(context, "destination")))))
                .then(argument("location", Vec3Argument.vec3()).executes(context -> toPos(context.getSource(), Collections.singleton(context.getSource().assertIsEntity()),
                        context.getSource().getWorld(), Vec3Argument.getLocation(context, "location"), com.michaelsebero.portium.command.arguments.WorldCoordinates.current(), null)))
                .then(argument("destination", EntityArgument.entity()).executes(context ->
                        toEntity(context.getSource(), Collections.singleton(context.getSource().assertIsEntity()), EntityArgument.getEntity(context, "destination")))));
        dispatcher.register(literal("tp").requires(source -> source.hasPermissionLevel(2)).redirect(teleport));
    }

    private static int kill(CommandSource source, Collection<? extends Entity> targets) {
        for (Entity entity : targets) entity.onKillCommand();
        if (targets.size() == 1) source.sendFeedback(new TextComponentTranslation("commands.kill.success.single", targets.iterator().next().getDisplayName()), true);
        else source.sendFeedback(new TextComponentTranslation("commands.kill.success.multiple", targets.size()), true);
        return targets.size();
    }

    private static int addTag(CommandSource source, Collection<? extends Entity> targets, String tag) throws CommandSyntaxException {
        int added = 0;
        for (Entity entity : targets) {
            if (entity.addTag(tag)) added++;
        }
        if (added == 0) throw TAG_ADD_FAILED.create();
        if (targets.size() == 1) source.sendFeedback(new TextComponentTranslation("commands.tag.add.success.single", tag, targets.iterator().next().getDisplayName()), true);
        else source.sendFeedback(new TextComponentTranslation("commands.tag.add.success.multiple", tag, targets.size()), true);
        return added;
    }

    private static int removeTag(CommandSource source, Collection<? extends Entity> targets, String tag) throws CommandSyntaxException {
        int removed = 0;
        for (Entity entity : targets) {
            if (entity.removeTag(tag)) removed++;
        }
        if (removed == 0) throw TAG_REMOVE_FAILED.create();
        if (targets.size() == 1) source.sendFeedback(new TextComponentTranslation("commands.tag.remove.success.single", tag, targets.iterator().next().getDisplayName()), true);
        else source.sendFeedback(new TextComponentTranslation("commands.tag.remove.success.multiple", tag, targets.size()), true);
        return removed;
    }

    private static int listTags(CommandSource source, Collection<? extends Entity> targets) {
        Set<String> tags = new java.util.TreeSet<>();
        for (Entity entity : targets) tags.addAll(entity.getTags());
        ITextComponent list = new TextComponentString(String.join(", ", tags));
        if (targets.size() == 1) {
            Entity entity = targets.iterator().next();
            if (tags.isEmpty()) source.sendFeedback(new TextComponentTranslation("commands.tag.list.single.empty", entity.getDisplayName()), false);
            else source.sendFeedback(new TextComponentTranslation("commands.tag.list.single.success", entity.getDisplayName(), tags.size(), list), false);
        } else if (tags.isEmpty()) {
            source.sendFeedback(new TextComponentTranslation("commands.tag.list.multiple.empty", targets.size()), false);
        } else {
            source.sendFeedback(new TextComponentTranslation("commands.tag.list.multiple.success", targets.size(), tags.size(), list), false);
        }
        return tags.size();
    }

    private static int summon(CommandSource source, ResourceLocation type, Vec3d pos, NBTTagCompound nbt, boolean randomize) throws CommandSyntaxException {
        WorldServer world = source.getWorld();
        if ("lightning_bolt".equals(type.getPath()) && "minecraft".equals(type.getNamespace())) {
            world.addWeatherEffect(new EntityLightningBolt(world, pos.x, pos.y, pos.z, false));
            source.sendFeedback(new TextComponentTranslation("commands.summon.success", new TextComponentTranslation("entity.LightningBolt.name")), true);
            return 1;
        }
        NBTTagCompound modern = nbt.copy();
        modern.setString("id", type.toString());
        NBTTagCompound data = LegacyEntityNbt.convert(modern);
        Entity entity = AnvilChunkLoader.readWorldEntityPos(data, world, pos.x, pos.y, pos.z, true);
        if (entity == null) throw SUMMON_FAILED.create();
        entity.setLocationAndAngles(pos.x, pos.y, pos.z, entity.rotationYaw, entity.rotationPitch);
        if (randomize && entity instanceof EntityLiving) ((EntityLiving) entity).onInitialSpawn(world.getDifficultyForLocation(entity.getPosition()), null);
        source.sendFeedback(new TextComponentTranslation("commands.summon.success", entity.getDisplayName()), true);
        return 1;
    }

    /** {@code facing} for teleports: an entity's anchor point or a position, applied after moving. */
    private static final class Facing {

        @Nullable
        private final Entity entity;
        private final EntityAnchor anchor;
        private final Vec3d pos;

        Facing(Entity entity, EntityAnchor anchor) {
            this.entity = entity;
            this.anchor = anchor;
            this.pos = null;
        }

        Facing(Vec3d pos) {
            this.entity = null;
            this.anchor = EntityAnchor.FEET;
            this.pos = pos;
        }

        void apply(CommandSource source, Entity target) {
            Vec3d look = this.entity != null ? this.anchor.apply(this.entity) : this.pos;
            CommandSource from = source.withEntity(target).withPos(target.getPositionVector()).withAnchor(EntityAnchor.EYES);
            Vec2 rotation = from.facing(look).getRotation();
            if (target instanceof EntityPlayerMP) {
                ((EntityPlayerMP) target).connection.setPlayerLocation(target.posX, target.posY, target.posZ, rotation.y, rotation.x);
            } else {
                target.rotationPitch = rotation.x;
                target.rotationYaw = rotation.y;
                target.setRotationYawHead(rotation.y);
            }
        }
    }

    private static int toEntity(CommandSource source, Collection<? extends Entity> targets, Entity destination) {
        for (Entity entity : targets) {
            teleport(source, entity, (WorldServer) destination.world, destination.posX, destination.posY, destination.posZ, EnumSet.noneOf(SPacketPlayerPosLook.EnumFlags.class),
                    destination.rotationYaw, destination.rotationPitch, null);
        }
        if (targets.size() == 1) {
            source.sendFeedback(new TextComponentTranslation("commands.teleport.success.entity.single", targets.iterator().next().getDisplayName(), destination.getDisplayName()), true);
        } else {
            source.sendFeedback(new TextComponentTranslation("commands.teleport.success.entity.multiple", targets.size(), destination.getDisplayName()), true);
        }
        return targets.size();
    }

    private static int toPos(CommandSource source, Collection<? extends Entity> targets, WorldServer world, Coordinates location, @Nullable Coordinates rotation,
                             @Nullable Facing facing) {
        Vec3d pos = location.getPosition(source);
        Vec2 angles = rotation == null ? null : rotation.getRotation(source);
        Set<SPacketPlayerPosLook.EnumFlags> relative = EnumSet.noneOf(SPacketPlayerPosLook.EnumFlags.class);
        if (location.isXRelative()) relative.add(SPacketPlayerPosLook.EnumFlags.X);
        if (location.isYRelative()) relative.add(SPacketPlayerPosLook.EnumFlags.Y);
        if (location.isZRelative()) relative.add(SPacketPlayerPosLook.EnumFlags.Z);
        if (rotation == null) {
            relative.add(SPacketPlayerPosLook.EnumFlags.X_ROT);
            relative.add(SPacketPlayerPosLook.EnumFlags.Y_ROT);
        } else {
            if (rotation.isXRelative()) relative.add(SPacketPlayerPosLook.EnumFlags.X_ROT);
            if (rotation.isYRelative()) relative.add(SPacketPlayerPosLook.EnumFlags.Y_ROT);
        }
        for (Entity entity : targets) {
            if (rotation == null) teleport(source, entity, world, pos.x, pos.y, pos.z, relative, entity.rotationYaw, entity.rotationPitch, facing);
            else teleport(source, entity, world, pos.x, pos.y, pos.z, relative, angles.y, angles.x, facing);
        }
        if (targets.size() == 1) {
            source.sendFeedback(new TextComponentTranslation("commands.teleport.success.location.single", targets.iterator().next().getDisplayName(), pos.x, pos.y, pos.z), true);
        } else {
            source.sendFeedback(new TextComponentTranslation("commands.teleport.success.location.multiple", targets.size(), pos.x, pos.y, pos.z), true);
        }
        return targets.size();
    }

    private static void teleport(CommandSource source, Entity entity, WorldServer world, double x, double y, double z, Set<SPacketPlayerPosLook.EnumFlags> relative,
                                 float yaw, float pitch, @Nullable Facing facing) {
        if (entity.world != world) {
            int dimension = world.provider.getDimension();
            ITeleporter teleporter = (targetWorld, moved, movedYaw) -> moved.moveToBlockPosAndAngles(new net.minecraft.util.math.BlockPos(x, y, z), yaw, pitch);
            if (entity instanceof EntityPlayerMP) {
                source.getServer().getPlayerList().transferPlayerToDimension((EntityPlayerMP) entity, dimension, teleporter);
            } else {
                entity = entity.changeDimension(dimension, teleporter);
                if (entity == null) return;
            }
        }
        if (entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) entity;
            player.dismountRidingEntity();
            if (player.isPlayerSleeping()) player.wakeUpPlayer(true, true, false);
            player.connection.setPlayerLocation(x, y, z, yaw, pitch, relative);
            player.setRotationYawHead(yaw);
        } else {
            float wrappedPitch = MathHelper.clamp(MathHelper.wrapDegrees(pitch), -90.0F, 90.0F);
            float wrappedYaw = MathHelper.wrapDegrees(yaw);
            entity.setLocationAndAngles(x, y, z, wrappedYaw, wrappedPitch);
            entity.setRotationYawHead(wrappedYaw);
        }
        if (facing != null) facing.apply(source, entity);
        if (!(entity instanceof EntityLivingBase) || !((EntityLivingBase) entity).isElytraFlying()) {
            entity.motionY = 0.0D;
            entity.onGround = true;
        }
    }

    private EntityCommands() {}
}
