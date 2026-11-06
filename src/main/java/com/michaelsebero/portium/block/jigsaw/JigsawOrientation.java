package com.michaelsebero.portium.block.jigsaw;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;

/** 1.16's {@code JigsawOrientation}: which way a jigsaw block's connection faces, and which way is its top. */
public enum JigsawOrientation implements IStringSerializable {
    DOWN_EAST("down_east", EnumFacing.DOWN, EnumFacing.EAST),
    DOWN_NORTH("down_north", EnumFacing.DOWN, EnumFacing.NORTH),
    DOWN_SOUTH("down_south", EnumFacing.DOWN, EnumFacing.SOUTH),
    DOWN_WEST("down_west", EnumFacing.DOWN, EnumFacing.WEST),
    UP_EAST("up_east", EnumFacing.UP, EnumFacing.EAST),
    UP_NORTH("up_north", EnumFacing.UP, EnumFacing.NORTH),
    UP_SOUTH("up_south", EnumFacing.UP, EnumFacing.SOUTH),
    UP_WEST("up_west", EnumFacing.UP, EnumFacing.WEST),
    WEST_UP("west_up", EnumFacing.WEST, EnumFacing.UP),
    EAST_UP("east_up", EnumFacing.EAST, EnumFacing.UP),
    NORTH_UP("north_up", EnumFacing.NORTH, EnumFacing.UP),
    SOUTH_UP("south_up", EnumFacing.SOUTH, EnumFacing.UP);

    private final String name;
    public final EnumFacing front;
    public final EnumFacing top;

    JigsawOrientation(String name, EnumFacing front, EnumFacing top) {
        this.name = name;
        this.front = front;
        this.top = top;
    }

    @Override
    public String getName() {
        return this.name;
    }

    /** The orientation with this front and top (top only matters when the front is up or down). */
    public static JigsawOrientation of(EnumFacing front, EnumFacing top) {
        for (JigsawOrientation orientation : values()) {
            if (orientation.front == front && orientation.top == top) return orientation;
        }
        return NORTH_UP;
    }
}
