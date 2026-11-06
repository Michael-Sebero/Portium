package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.mods116.Mods116;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.EnumHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge 1.16's {@code InputEvent.ClickInputEvent} for 1.16 mods: posted before the attack, use and pick-block clicks,
 * any of which a mod can cancel (and say whether the hand still swings).
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftClickInput116 {

    @Shadow
    public GameSettings gameSettings;
    @Shadow
    public EntityPlayerSP player;

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    private void portium$attackClick(CallbackInfo ci) {
        if (this.portium$cancelled(0, this.gameSettings.keyBindAttack)) ci.cancel();
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"), cancellable = true)
    private void portium$useClick(CallbackInfo ci) {
        if (this.portium$cancelled(1, this.gameSettings.keyBindUseItem)) ci.cancel();
    }

    @Inject(method = "middleClickMouse", at = @At("HEAD"), cancellable = true)
    private void portium$pickClick(CallbackInfo ci) {
        if (this.portium$cancelled(2, this.gameSettings.keyBindPickBlock)) ci.cancel();
    }

    private boolean portium$cancelled(int button, Object keyBinding) {
        if (!Mods116.active() || this.player == null) return false;
        Object result = Mods116.postEvent("click_input", button, keyBinding, EnumHand.MAIN_HAND);
        if (!(result instanceof boolean[])) return false;
        boolean[] outcome = (boolean[]) result;
        if (outcome[0] && outcome[1]) this.player.swingArm(EnumHand.MAIN_HAND);
        return outcome[0];
    }
}
