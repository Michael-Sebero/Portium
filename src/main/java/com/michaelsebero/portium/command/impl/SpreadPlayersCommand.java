package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.Vec2;
import com.michaelsebero.portium.command.arguments.EntityArgument;
import com.michaelsebero.portium.command.arguments.Vec2Argument;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.arguments.BoolArgumentType;
import com.michaelsebero.portium.command.brigadier.arguments.FloatArgumentType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/**
 * {@code spreadplayers} (1.13): scatters entities (or whole teams) over a square, pushing points apart until each
 * is at least {@code spreadDistance} from the others and stands on solid ground.
 */
public final class SpreadPlayersCommand {

    private static final int MAX_ITERATIONS = 10000;

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("spreadplayers").requires(source -> source.hasPermissionLevel(2))
                .then(argument("center", Vec2Argument.vec2())
                        .then(argument("spreadDistance", FloatArgumentType.floatArg(0.0F))
                                .then(argument("maxRange", FloatArgumentType.floatArg(1.0F))
                                        .then(argument("respectTeams", BoolArgumentType.bool())
                                                .then(argument("targets", EntityArgument.entities()).executes(context -> spread(context.getSource(),
                                                        Vec2Argument.getVec2(context, "center"), FloatArgumentType.getFloat(context, "spreadDistance"),
                                                        FloatArgumentType.getFloat(context, "maxRange"), BoolArgumentType.getBool(context, "respectTeams"),
                                                        EntityArgument.getEntities(context, "targets")))))))));
    }

    private static int spread(CommandSource source, Vec2 center, float spreadDistance, float maxRange, boolean respectTeams,
                              Collection<? extends Entity> targets) throws CommandSyntaxException {
        Random random = new Random();
        double minX = center.x - maxRange;
        double minZ = center.y - maxRange;
        double maxX = center.x + maxRange;
        double maxZ = center.y + maxRange;
        Point[] points = new Point[respectTeams ? teamCount(targets) : targets.size()];
        for (int i = 0; i < points.length; i++) {
            points[i] = new Point();
            points[i].randomize(random, minX, minZ, maxX, maxZ);
        }
        World world = source.getWorld();
        separate(center, spreadDistance, world, random, minX, minZ, maxX, maxZ, points, respectTeams);
        double averageDistance = place(targets, world, points, respectTeams);
        source.sendFeedback(new TextComponentTranslation("commands.spreadplayers.success." + (respectTeams ? "teams" : "entities"), points.length, center.x, center.y,
                String.format(Locale.ROOT, "%.2f", averageDistance)), true);
        return points.length;
    }

    private static int teamCount(Collection<? extends Entity> entities) {
        Set<Team> teams = new HashSet<>();
        for (Entity entity : entities) teams.add(entity instanceof EntityPlayer ? entity.getTeam() : null);
        return teams.size();
    }

    private static void separate(Vec2 center, double spreadDistance, World world, Random random, double minX, double minZ, double maxX, double maxZ,
                                 Point[] points, boolean respectTeams) throws CommandSyntaxException {
        boolean moved = true;
        double closest = Float.MAX_VALUE;
        int iteration;
        for (iteration = 0; iteration < MAX_ITERATIONS && moved; iteration++) {
            moved = false;
            closest = Float.MAX_VALUE;
            for (int i = 0; i < points.length; i++) {
                Point point = points[i];
                int crowding = 0;
                Point push = new Point();
                for (int j = 0; j < points.length; j++) {
                    if (i == j) continue;
                    Point other = points[j];
                    double distance = point.distanceTo(other);
                    closest = Math.min(distance, closest);
                    if (distance < spreadDistance) {
                        crowding++;
                        push.x += other.x - point.x;
                        push.z += other.z - point.z;
                    }
                }
                if (crowding > 0) {
                    push.x /= crowding;
                    push.z /= crowding;
                    if (push.length() > 0.0D) {
                        push.normalize();
                        point.x -= push.x;
                        point.z -= push.z;
                    } else {
                        point.randomize(random, minX, minZ, maxX, maxZ);
                    }
                    moved = true;
                }
                if (point.clamp(minX, minZ, maxX, maxZ)) moved = true;
            }
            if (!moved) {
                for (Point point : points) {
                    if (!point.isSafe(world)) {
                        point.randomize(random, minX, minZ, maxX, maxZ);
                        moved = true;
                    }
                }
            }
        }
        if (closest == Float.MAX_VALUE) closest = 0.0D;
        if (iteration >= MAX_ITERATIONS) {
            throw CommandExceptions.failure("commands.spreadplayers.failed." + (respectTeams ? "teams" : "entities"), points.length, center.x, center.y,
                    String.format(Locale.ROOT, "%.2f", closest));
        }
    }

    /** Moves each entity (or each team together) to its point, returning the average distance to the nearest other point. */
    private static double place(Collection<? extends Entity> targets, World world, Point[] points, boolean respectTeams) {
        double total = 0.0D;
        int next = 0;
        Map<Team, Point> teamPoints = new HashMap<>();
        for (Entity entity : targets) {
            Point point;
            if (respectTeams) {
                Team team = entity instanceof EntityPlayer ? entity.getTeam() : null;
                if (!teamPoints.containsKey(team)) teamPoints.put(team, points[next++]);
                point = teamPoints.get(team);
            } else {
                point = points[next++];
            }
            entity.setPositionAndUpdate(MathHelper.floor(point.x) + 0.5D, point.surfaceY(world), MathHelper.floor(point.z) + 0.5D);
            double nearest = Double.MAX_VALUE;
            for (Point other : points) {
                if (point != other) nearest = Math.min(point.distanceTo(other), nearest);
            }
            total += nearest;
        }
        return targets.size() < 2 ? 0.0D : total / targets.size();
    }

    private static final class Point {

        double x;
        double z;

        double distanceTo(Point other) {
            double dx = this.x - other.x;
            double dz = this.z - other.z;
            return Math.sqrt(dx * dx + dz * dz);
        }

        double length() {
            return Math.sqrt(this.x * this.x + this.z * this.z);
        }

        void normalize() {
            double length = this.length();
            this.x /= length;
            this.z /= length;
        }

        boolean clamp(double minX, double minZ, double maxX, double maxZ) {
            double x = MathHelper.clamp(this.x, minX, maxX);
            double z = MathHelper.clamp(this.z, minZ, maxZ);
            boolean changed = x != this.x || z != this.z;
            this.x = x;
            this.z = z;
            return changed;
        }

        void randomize(Random random, double minX, double minZ, double maxX, double maxZ) {
            this.x = MathHelper.nextDouble(random, minX, maxX);
            this.z = MathHelper.nextDouble(random, minZ, maxZ);
        }

        /** The height just above the highest non-air block in this column. */
        int surfaceY(World world) {
            for (BlockPos pos = new BlockPos(this.x, 256.0D, this.z); pos.getY() > 0; ) {
                pos = pos.down();
                if (!world.isAirBlock(pos)) return pos.getY() + 1;
            }
            return 257;
        }

        /** Whether the highest block in this column is something to stand on, not liquid or fire. */
        boolean isSafe(World world) {
            for (BlockPos pos = new BlockPos(this.x, 256.0D, this.z); pos.getY() > 0; ) {
                pos = pos.down();
                IBlockState state = world.getBlockState(pos);
                if (state.getMaterial() != Material.AIR) return !state.getMaterial().isLiquid() && state.getMaterial() != Material.FIRE;
            }
            return false;
        }
    }

    private SpreadPlayersCommand() {}
}
