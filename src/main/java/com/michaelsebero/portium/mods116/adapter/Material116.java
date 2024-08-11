package com.michaelsebero.portium.mods116.adapter;

import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

/**
 * What {@code new Material(...)} in 1.16 code makes: 1.12's material, with each property 1.16 gives its constructor.
 * 1.12 sets those through protected setters and derives some (opacity from blocking movement); here they're as given.
 */
public class Material116 extends Material {

    private final boolean liquid;
    private final boolean solid;
    private final boolean blocksMovement;
    private final boolean opaque;
    private final boolean flammable;
    private final boolean replaceable;
    private final EnumPushReaction pushReaction;

    public Material116(MapColor color, boolean liquid, boolean solid, boolean blocksMovement, boolean opaque, boolean flammable, boolean replaceable,
                       EnumPushReaction pushReaction) {
        super(color);
        this.liquid = liquid;
        this.solid = solid;
        this.blocksMovement = blocksMovement;
        this.opaque = opaque;
        this.flammable = flammable;
        this.replaceable = replaceable;
        this.pushReaction = pushReaction;
    }

    @Override
    public boolean isLiquid() {
        return this.liquid;
    }

    @Override
    public boolean isSolid() {
        return this.solid;
    }

    @Override
    public boolean blocksMovement() {
        return this.blocksMovement;
    }

    @Override
    public boolean isOpaque() {
        return this.opaque;
    }

    @Override
    public boolean getCanBurn() {
        return this.flammable;
    }

    @Override
    public boolean isReplaceable() {
        return this.replaceable;
    }

    @Override
    public EnumPushReaction getPushReaction() {
        return this.pushReaction;
    }
}
