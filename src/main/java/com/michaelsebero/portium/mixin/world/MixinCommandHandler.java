package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.command.PortiumCommands;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Sends vanilla commands to Portium's 1.13 dispatcher on the server; anything it doesn't know (other mods' commands)
 * carries on to 1.12's command system. Client-side command handlers are left alone.
 */
@Mixin(CommandHandler.class)
public abstract class MixinCommandHandler {

    private boolean portium$isServer() {
        return (Object) this instanceof ServerCommandManager && PortiumConfig.mechanics.modernCommands;
    }

    @Inject(method = "executeCommand", at = @At("HEAD"), cancellable = true)
    private void portium$executeModern(ICommandSender sender, String command, CallbackInfoReturnable<Integer> cir) {
        if (this.portium$isServer() && PortiumCommands.handles(command)) cir.setReturnValue(PortiumCommands.execute(sender, command));
    }

    @Inject(method = "getTabCompletions", at = @At("HEAD"), cancellable = true)
    private void portium$completeModern(ICommandSender sender, String input, @Nullable BlockPos pos, CallbackInfoReturnable<List<String>> cir) {
        if (this.portium$isServer() && input.contains(" ") && PortiumCommands.handles(input)) cir.setReturnValue(PortiumCommands.getTabCompletions(sender, input));
    }

    @Inject(method = "getTabCompletions", at = @At("RETURN"), cancellable = true)
    private void portium$addModernNames(ICommandSender sender, String input, @Nullable BlockPos pos, CallbackInfoReturnable<List<String>> cir) {
        if (!this.portium$isServer() || input.contains(" ")) return;
        List<String> names = new ArrayList<>(cir.getReturnValue());
        for (String name : PortiumCommands.getRootNames(sender, input)) {
            if (!names.contains(name)) names.add(name);
        }
        names.sort(String::compareTo);
        cir.setReturnValue(names);
    }
}
