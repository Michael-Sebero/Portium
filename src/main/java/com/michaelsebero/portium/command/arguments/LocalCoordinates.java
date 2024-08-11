package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.Vec2;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** {@code ^left ^up ^forwards}: offsets along the source's own facing, from its anchor point (1.13). */
public class LocalCoordinates implements Coordinates {

    private final double left;
    private final double up;
    private final double forwards;

    public LocalCoordinates(double left, double up, double forwards) {
        this.left = left;
        this.up = up;
        this.forwards = forwards;
    }

    @Override
    public Vec3d getPosition(CommandSource source) {
        Vec2 rotation = source.getRotation();
        Entity entity = source.getEntity();
        Vec3d origin = entity == null ? source.getPos() : source.getEntityAnchor().apply(source.getPos(), entity);
        float toRadians = (float) Math.PI / 180.0F;
        float yawCos = MathHelper.cos((rotation.y + 90.0F) * toRadians);
        float yawSin = MathHelper.sin((rotation.y + 90.0F) * toRadians);
        float pitchCos = MathHelper.cos(-rotation.x * toRadians);
        float pitchSin = MathHelper.sin(-rotation.x * toRadians);
        float upCos = MathHelper.cos((-rotation.x + 90.0F) * toRadians);
        float upSin = MathHelper.sin((-rotation.x + 90.0F) * toRadians);
        Vec3d forwardAxis = new Vec3d(yawCos * pitchCos, pitchSin, yawSin * pitchCos);
        Vec3d upAxis = new Vec3d(yawCos * upCos, upSin, yawSin * upCos);
        Vec3d leftAxis = forwardAxis.crossProduct(upAxis).scale(-1.0D);
        double dx = forwardAxis.x * this.forwards + upAxis.x * this.up + leftAxis.x * this.left;
        double dy = forwardAxis.y * this.forwards + upAxis.y * this.up + leftAxis.y * this.left;
        double dz = forwardAxis.z * this.forwards + upAxis.z * this.up + leftAxis.z * this.left;
        return new Vec3d(origin.x + dx, origin.y + dy, origin.z + dz);
    }

    @Override
    public Vec2 getRotation(CommandSource source) {
        return Vec2.ZERO;
    }

    @Override
    public boolean isXRelative() {
        return true;
    }

    @Override
    public boolean isYRelative() {
        return true;
    }

    @Override
    public boolean isZRelative() {
        return true;
    }

    public static LocalCoordinates parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        double left = readPart(reader, start);
        if (!reader.canRead() || reader.peek() != ' ') throw incomplete(reader, start);
        reader.skip();
        double up = readPart(reader, start);
        if (!reader.canRead() || reader.peek() != ' ') throw incomplete(reader, start);
        reader.skip();
        return new LocalCoordinates(left, up, readPart(reader, start));
    }

    private static double readPart(StringReader reader, int start) throws CommandSyntaxException {
        if (!reader.canRead()) throw incomplete(reader, start);
        if (reader.peek() != '^') {
            reader.setCursor(start);
            throw CommandExceptions.POS_MIXED.createWithContext(reader);
        }
        reader.skip();
        return reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0D;
    }

    private static CommandSyntaxException incomplete(StringReader reader, int start) {
        reader.setCursor(start);
        return CommandExceptions.POS_INCOMPLETE.createWithContext(reader);
    }
}
