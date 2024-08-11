package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(TemplateManager.class)
public interface TemplateManagerAccessor {

    @Accessor("templates")
    Map<String, Template> portium$getTemplates();
}
