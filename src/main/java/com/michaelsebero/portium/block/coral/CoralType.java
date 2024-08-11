package com.michaelsebero.portium.block.coral;

import net.minecraft.block.material.MapColor;

public enum CoralType {

    TUBE("tube", MapColor.BLUE),
    BRAIN("brain", MapColor.PINK),
    BUBBLE("bubble", MapColor.PURPLE),
    FIRE("fire", MapColor.RED),
    HORN("horn", MapColor.YELLOW);

    private final String name;
    private final MapColor mapColor;

    CoralType(String name, MapColor mapColor) {
        this.name = name;
        this.mapColor = mapColor;
    }

    public String getName() {
        return this.name;
    }

    public MapColor getMapColor() {
        return this.mapColor;
    }
}
