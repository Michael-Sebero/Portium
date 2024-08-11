package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.Vec2;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import net.minecraft.util.math.Vec3d;

/** World coordinates: each axis absolute or {@code ~} relative. */
public class WorldCoordinates implements Coordinates {

    private final LocationPart x;
    private final LocationPart y;
    private final LocationPart z;

    public WorldCoordinates(LocationPart x, LocationPart y, LocationPart z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Vec3d getPosition(CommandSource source) {
        Vec3d pos = source.getPos();
        return new Vec3d(this.x.get(pos.x), this.y.get(pos.y), this.z.get(pos.z));
    }

    @Override
    public Vec2 getRotation(CommandSource source) {
        Vec2 rotation = source.getRotation();
        return new Vec2((float) this.x.get(rotation.x), (float) this.y.get(rotation.y));
    }

    @Override
    public boolean isXRelative() {
        return this.x.isRelative();
    }

    @Override
    public boolean isYRelative() {
        return this.y.isRelative();
    }

    @Override
    public boolean isZRelative() {
        return this.z.isRelative();
    }

    public static WorldCoordinates parseInt(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        LocationPart x = LocationPart.parseInt(reader);
        if (!reader.canRead() || reader.peek() != ' ') throw incomplete(reader, start);
        reader.skip();
        LocationPart y = LocationPart.parseInt(reader);
        if (!reader.canRead() || reader.peek() != ' ') throw incomplete(reader, start);
        reader.skip();
        return new WorldCoordinates(x, y, LocationPart.parseInt(reader));
    }

    public static WorldCoordinates parseDouble(StringReader reader, boolean centerIntegers) throws CommandSyntaxException {
        int start = reader.getCursor();
        LocationPart x = LocationPart.parseDouble(reader, centerIntegers);
        if (!reader.canRead() || reader.peek() != ' ') throw incomplete(reader, start);
        reader.skip();
        LocationPart y = LocationPart.parseDouble(reader, false);
        if (!reader.canRead() || reader.peek() != ' ') throw incomplete(reader, start);
        reader.skip();
        return new WorldCoordinates(x, y, LocationPart.parseDouble(reader, centerIntegers));
    }

    private static CommandSyntaxException incomplete(StringReader reader, int start) {
        reader.setCursor(start);
        return CommandExceptions.POS_INCOMPLETE.createWithContext(reader);
    }

    /** {@code ~ ~ ~}: the source's own position. */
    public static WorldCoordinates current() {
        return new WorldCoordinates(new LocationPart(true, 0.0D), new LocationPart(true, 0.0D), new LocationPart(true, 0.0D));
    }
}
