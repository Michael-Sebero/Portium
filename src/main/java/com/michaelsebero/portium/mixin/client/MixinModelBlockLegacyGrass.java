package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.ModernPackSupport;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * {@code block/grass} is 1.12's grass block but 1.13's short grass plant. With 1.13+ models on (Portium's imported
 * 1.16.5 pack, or a modern pack), 1.12 mods' grass blocks built on it (Biomes O' Plenty's) turned into grass plants:
 * their models get 1.16's grass block model as their parent instead, which has the same texture slots. No 1.13+ model
 * is built on the plant.
 */
@Mixin(ModelBlock.class)
public abstract class MixinModelBlockLegacyGrass {

    @Unique
    private static final ResourceLocation PORTIUM_GRASS = new ResourceLocation("minecraft", "block/grass");
    @Unique
    private static final ResourceLocation PORTIUM_GRASS_BLOCK = new ResourceLocation("minecraft", "block/grass_block");

    @Shadow
    protected ResourceLocation parentLocation;

    @Inject(method = "<init>(Lnet/minecraft/util/ResourceLocation;Ljava/util/List;Ljava/util/Map;ZZLnet/minecraft/client/renderer/block/model/ItemCameraTransforms;Ljava/util/List;)V",
            at = @At("RETURN"))
    private void portium$legacyGrassParent(@Nullable ResourceLocation parent, List<BlockPart> elements, Map<String, String> textures, boolean ambientOcclusion,
                                           boolean gui3d, ItemCameraTransforms transforms, List<ItemOverride> overrides, CallbackInfo ci) {
        if (PORTIUM_GRASS.equals(this.parentLocation) && ModernPackSupport.modernModels()) this.parentLocation = PORTIUM_GRASS_BLOCK;
    }
}
