package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.server.management.UserListEntryBan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(UserListEntryBan.class)
public interface UserListEntryBanAccessor {

    @Accessor("bannedBy")
    String portium$getBannedBy();
}
