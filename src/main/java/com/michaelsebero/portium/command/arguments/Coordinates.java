package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.Vec2;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** A position (or rotation) read from a command, resolved against the source when the command runs. */
public interface Coordinates {

    Vec3d getPosition(CommandSource source);

    Vec2 getRotation(CommandSource source);

    default BlockPos getBlockPos(CommandSource source) {
        return new BlockPos(this.getPosition(source));
    }

    boolean isXRelative();

    boolean isYRelative();

    boolean isZRelative();
}
