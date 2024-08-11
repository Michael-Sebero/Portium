package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.modern.ModernMods;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/** Lists 1.13 mods in Forge's mod list, which only knows Forge 1.12's. */
@Mixin(value = GuiModList.class, remap = false)
public abstract class MixinGuiModList {

    @Shadow
    private ArrayList<ModContainer> mods;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void portium$addModernMods(GuiScreen parent, CallbackInfo ci) {
        for (Map<String, String> mod : ModernMods.descriptions()) {
            ModMetadata metadata = new ModMetadata();
            metadata.modId = mod.get("id");
            metadata.name = mod.get("name") + " (Forge 1.13.2)";
            metadata.version = mod.get("version");
            metadata.description = mod.get("description");
            metadata.url = mod.get("url");
            metadata.authorList = new ArrayList<>(Arrays.asList(mod.get("authors").split(",\\s*")));
            this.mods.add(new DummyModContainer(metadata));
        }
    }
}
