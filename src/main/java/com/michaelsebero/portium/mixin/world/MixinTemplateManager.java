package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.datapack.DataPackStructures;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Structures saved in the world still come first, as in 1.13, including ones 1.13 saved in its own folder; after them,
 * data pack structures take precedence over the game's own.
 */
@Mixin(TemplateManager.class)
public abstract class MixinTemplateManager {

    @Shadow @Final private Map<String, Template> templates;

    @Shadow @Final private String baseFolder;

    @Shadow
    protected abstract void readTemplateFromStream(String id, InputStream stream) throws IOException;

    /** 1.13 saves a world's structures under {@code generated/<namespace>/structures}; 1.12 under {@code structures}. */
    @Inject(method = "readTemplateFromJar", at = @At("HEAD"), cancellable = true)
    private void portium$readModernWorldStructure(ResourceLocation id, CallbackInfoReturnable<Boolean> cir) {
        File file = new File(new File(this.baseFolder).getParentFile(), "generated/" + id.getNamespace() + "/structures/" + id.getPath() + ".nbt");
        if (!file.isFile()) return;
        try (InputStream stream = new FileInputStream(file)) {
            this.readTemplateFromStream(id.getPath(), stream);
            cir.setReturnValue(true);
        } catch (IOException | RuntimeException e) {
            Portium.LOGGER.error("Couldn't read the structure {} saved by 1.13", id, e);
        }
    }

    @Inject(method = "readTemplateFromJar", at = @At("HEAD"), cancellable = true)
    private void portium$readFromDataPacks(ResourceLocation id, CallbackInfoReturnable<Boolean> cir) {
        Template template = DataPackStructures.load(id);
        if (template == null) return;
        this.templates.put(id.getPath(), template);
        cir.setReturnValue(true);
    }
}
