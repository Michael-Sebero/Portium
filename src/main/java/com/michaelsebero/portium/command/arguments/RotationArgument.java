package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;

/** {@code yaw pitch}, each absolute or {@code ~} relative to the source's own rotation. */
public class RotationArgument implements ArgumentType<Coordinates> {

    public static RotationArgument rotation() {
        return new RotationArgument();
    }

    @Override
    public Coordinates parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        if (!reader.canRead()) throw CommandExceptions.ROTATION_INCOMPLETE.createWithContext(reader);
        LocationPart yaw = LocationPart.parseDouble(reader, false);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw CommandExceptions.ROTATION_INCOMPLETE.createWithContext(reader);
        }
        reader.skip();
        LocationPart pitch = LocationPart.parseDouble(reader, false);
        return new WorldCoordinates(pitch, yaw, new LocationPart(true, 0.0D));
    }
}
