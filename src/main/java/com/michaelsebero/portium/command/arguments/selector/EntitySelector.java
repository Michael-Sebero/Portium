package com.michaelsebero.portium.command.arguments.selector;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.arguments.MinMaxBounds;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** A parsed target such as {@code @e[type=zombie,distance=..10]}, a player name or a UUID, resolved when the command runs. */
public class EntitySelector {

    private final int limit;
    private final boolean includeNonPlayers;
    private final boolean currentWorldOnly;
    private final Predicate<Entity> filter;
    private final MinMaxBounds distance;
    private final Function<Vec3d, Vec3d> positionGetter;
    @Nullable
    private final AxisAlignedBB box;
    private final BiConsumer<Vec3d, List<? extends Entity>> sorter;
    private final boolean self;
    @Nullable
    private final String username;
    @Nullable
    private final UUID uuid;
    private final Class<? extends Entity> type;
    private final boolean checkPermission;

    EntitySelector(int limit, boolean includeNonPlayers, boolean currentWorldOnly, Predicate<Entity> filter, MinMaxBounds distance,
                   Function<Vec3d, Vec3d> positionGetter, @Nullable AxisAlignedBB box, BiConsumer<Vec3d, List<? extends Entity>> sorter, boolean self,
                   @Nullable String username, @Nullable UUID uuid, Class<? extends Entity> type, boolean checkPermission) {
        this.limit = limit;
        this.includeNonPlayers = includeNonPlayers;
        this.currentWorldOnly = currentWorldOnly;
        this.filter = filter;
        this.distance = distance;
        this.positionGetter = positionGetter;
        this.box = box;
        this.sorter = sorter;
        this.self = self;
        this.username = username;
        this.uuid = uuid;
        this.type = type;
        this.checkPermission = checkPermission;
    }

    public int getLimit() {
        return this.limit;
    }

    public boolean includesEntities() {
        return this.includeNonPlayers;
    }

    public boolean isSelfSelector() {
        return this.self;
    }

    public boolean isWorldLimited() {
        return this.currentWorldOnly;
    }

    private void checkPermission(CommandSource source) throws CommandSyntaxException {
        if (this.checkPermission && !source.hasPermissionLevel(2)) throw CommandExceptions.SELECTOR_NOT_ALLOWED.create();
    }

    public Entity selectOne(CommandSource source) throws CommandSyntaxException {
        this.checkPermission(source);
        List<? extends Entity> entities = this.select(source);
        if (entities.isEmpty()) throw CommandExceptions.ENTITY_NOT_FOUND.create();
        if (entities.size() > 1) throw CommandExceptions.ENTITY_TOO_MANY.create();
        return entities.get(0);
    }

    public List<? extends Entity> select(CommandSource source) throws CommandSyntaxException {
        this.checkPermission(source);
        if (!this.includeNonPlayers) return this.selectPlayers(source);
        MinecraftServer server = source.getServer();
        if (this.username != null) {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(this.username);
            return player == null ? Collections.emptyList() : Collections.singletonList(player);
        }
        if (this.uuid != null) {
            for (WorldServer world : server.worlds) {
                Entity entity = world.getEntityFromUuid(this.uuid);
                if (entity != null) return Collections.singletonList(entity);
            }
            return Collections.emptyList();
        }
        Vec3d pos = this.positionGetter.apply(source.getPos());
        Predicate<Entity> predicate = this.withPosition(pos);
        if (this.self) {
            Entity entity = source.getEntity();
            return entity != null && predicate.test(entity) ? Collections.singletonList(entity) : Collections.emptyList();
        }
        List<Entity> entities = new ArrayList<>();
        if (this.currentWorldOnly) {
            this.collect(entities, source.getWorld(), pos, predicate);
        } else {
            for (WorldServer world : server.worlds) this.collect(entities, world, pos, predicate);
        }
        return this.sortAndLimit(pos, entities);
    }

    private void collect(List<Entity> out, WorldServer world, Vec3d pos, Predicate<Entity> predicate) {
        if (this.box != null) {
            out.addAll(world.getEntitiesWithinAABB(this.type, this.box.offset(pos), predicate::test));
        } else {
            out.addAll(world.getEntities(this.type, predicate::test));
        }
    }

    public EntityPlayerMP selectOnePlayer(CommandSource source) throws CommandSyntaxException {
        this.checkPermission(source);
        List<EntityPlayerMP> players = this.selectPlayers(source);
        if (players.size() != 1) throw CommandExceptions.PLAYER_NOT_FOUND.create();
        return players.get(0);
    }

    public List<EntityPlayerMP> selectPlayers(CommandSource source) throws CommandSyntaxException {
        this.checkPermission(source);
        MinecraftServer server = source.getServer();
        if (this.username != null) {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(this.username);
            return player == null ? Collections.emptyList() : Collections.singletonList(player);
        }
        if (this.uuid != null) {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(this.uuid);
            return player == null ? Collections.emptyList() : Collections.singletonList(player);
        }
        Vec3d pos = this.positionGetter.apply(source.getPos());
        Predicate<Entity> predicate = this.withPosition(pos);
        if (this.self) {
            Entity entity = source.getEntity();
            return entity instanceof EntityPlayerMP && predicate.test(entity) ? Collections.singletonList((EntityPlayerMP) entity) : Collections.emptyList();
        }
        List<EntityPlayerMP> players = new ArrayList<>();
        if (this.currentWorldOnly) {
            for (EntityPlayer player : source.getWorld().playerEntities) {
                if (player instanceof EntityPlayerMP && predicate.test(player)) players.add((EntityPlayerMP) player);
            }
        } else {
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                if (predicate.test(player)) players.add(player);
            }
        }
        return this.sortAndLimit(pos, players);
    }

    private Predicate<Entity> withPosition(Vec3d pos) {
        Predicate<Entity> predicate = this.filter;
        if (this.box != null) {
            AxisAlignedBB box = this.box.offset(pos);
            predicate = predicate.and(entity -> box.intersects(entity.getEntityBoundingBox()));
        }
        if (!this.distance.isUnbounded()) predicate = predicate.and(entity -> this.distance.testSquared(entity.getDistanceSq(pos.x, pos.y, pos.z)));
        return predicate;
    }

    private <T extends Entity> List<T> sortAndLimit(Vec3d pos, List<T> entities) {
        if (entities.size() > 1) this.sorter.accept(pos, entities);
        return entities.subList(0, Math.min(this.limit, entities.size()));
    }
}
