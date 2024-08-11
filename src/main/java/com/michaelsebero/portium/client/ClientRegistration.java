package com.michaelsebero.portium.client;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.block.BlockBubbleColumn;
import com.michaelsebero.portium.block.building.BlockPortiumSlab;
import com.michaelsebero.portium.block.water.IWaterloggable;
import com.michaelsebero.portium.client.model.TridentItemModel;
import com.michaelsebero.portium.client.particle.ParticleBubblePop;
import com.michaelsebero.portium.client.particle.ParticleNautilus;
import com.michaelsebero.portium.client.render.PortiumItemStackRenderer;
import com.michaelsebero.portium.client.render.RenderConduit;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class ClientRegistration {

    private static final ModelResourceLocation TRIDENT_IN_HAND = new ModelResourceLocation(Tags.MOD_ID + ":trident", "inventory");
    private static final ModelResourceLocation TRIDENT_ICON = new ModelResourceLocation(Tags.MOD_ID + ":trident_icon", "inventory");

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (Item item : PortiumItems.all()) {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
        PortiumItemStackRenderer itemRenderer = new PortiumItemStackRenderer();
        if (PortiumItems.TRIDENT != null) {
            ModelLoader.registerItemVariants(PortiumItems.TRIDENT, new ResourceLocation(Tags.MOD_ID, "trident_icon"));
            PortiumItems.TRIDENT.setTileEntityItemStackRenderer(itemRenderer);
        }
        if (PortiumBlocks.CONDUIT != null) Item.getItemFromBlock(PortiumBlocks.CONDUIT).setTileEntityItemStackRenderer(itemRenderer);

        for (Block block : PortiumBlocks.all()) {
            IBlockState state = block.getDefaultState();
            if (block instanceof BlockBubbleColumn) {
                // Drawn by the fluid renderer; the model only supplies the breaking particle texture.
                ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
                    @Nonnull
                    @Override
                    protected ModelResourceLocation getModelResourceLocation(@Nonnull IBlockState state) {
                        return new ModelResourceLocation(new ResourceLocation(Tags.MOD_ID, "bubble_column"), "normal");
                    }
                });
            } else if (block instanceof BlockPortiumSlab) {
                ModelLoader.setCustomStateMapper(block, new StateMap.Builder().ignore(BlockPortiumSlab.VARIANT).build());
            } else if (block instanceof IWaterloggable && state.getPropertyKeys().contains(BlockLiquid.LEVEL)) {
                ModelLoader.setCustomStateMapper(block, new StateMap.Builder().ignore(BlockLiquid.LEVEL).build());
            }
        }
    }

    @SubscribeEvent
    public static void bakeModels(ModelBakeEvent event) {
        IBakedModel inHand = event.getModelRegistry().getObject(TRIDENT_IN_HAND);
        IBakedModel icon = event.getModelRegistry().getObject(TRIDENT_ICON);
        if (inHand != null && icon != null) {
            event.getModelRegistry().putObject(TRIDENT_IN_HAND, new TridentItemModel(inHand, icon));
        }
        RenderConduit.onResourceReload();
    }

    @SubscribeEvent
    public static void stitchTextures(TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(ParticleNautilus.SPRITE);
        for (ResourceLocation frame : ParticleBubblePop.FRAMES) event.getMap().registerSprite(frame);
        event.getMap().registerSprite(new ResourceLocation(Tags.MOD_ID, "blocks/water_still"));
        event.getMap().registerSprite(new ResourceLocation(Tags.MOD_ID, "blocks/water_flow"));
    }

    private ClientRegistration() {}
}
