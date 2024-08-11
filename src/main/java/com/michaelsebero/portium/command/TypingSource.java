package com.michaelsebero.portium.command;

import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/** The player typing a command in chat, with what they are looking at, which 1.13 offers among the suggestions. */
final class TypingSource extends CommandSource {

    @Nullable
    private final BlockPos targetedBlock;
    @Nullable
    private final String targetedEntity;

    TypingSource(CommandSource player, @Nullable BlockPos targetedBlock, @Nullable String targetedEntity) {
        super(player);
        this.targetedBlock = targetedBlock;
        this.targetedEntity = targetedEntity;
    }

    @Nullable
    @Override
    public BlockPos getTargetedBlock() {
        return this.targetedBlock;
    }

    @Nullable
    @Override
    public String getTargetedEntity() {
        return this.targetedEntity;
    }
}
