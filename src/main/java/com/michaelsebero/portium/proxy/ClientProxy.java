package com.michaelsebero.portium.proxy;

import com.michaelsebero.portium.client.PortiumAssets;
import com.michaelsebero.portium.client.chat.ModernChat;
import com.michaelsebero.portium.client.particle.ParticleBubbleColumnUp;
import com.michaelsebero.portium.client.particle.ParticleCurrentDown;
import com.michaelsebero.portium.client.particle.ParticleDolphin;
import com.michaelsebero.portium.client.particle.ParticleNautilus;
import com.michaelsebero.portium.client.particle.ParticleSquidInk;
import com.michaelsebero.portium.client.render.LayerRiptide;
import com.michaelsebero.portium.client.render.RenderConduit;
import com.michaelsebero.portium.client.render.RenderDolphin;
import com.michaelsebero.portium.client.render.RenderDrowned;
import com.michaelsebero.portium.client.render.RenderFish;
import com.michaelsebero.portium.client.render.RenderPhantom;
import com.michaelsebero.portium.client.render.RenderPufferfish;
import com.michaelsebero.portium.client.render.RenderTrident;
import com.michaelsebero.portium.client.render.RenderTropicalFish;
import com.michaelsebero.portium.client.render.RenderTurtle;
import com.michaelsebero.portium.compat.ModernLang;
import com.michaelsebero.portium.datapack.DataPackRecipes;
import com.michaelsebero.portium.entity.EntityTrident;
import com.michaelsebero.portium.entity.fish.EntityCod;
import com.michaelsebero.portium.entity.fish.EntityPufferfish;
import com.michaelsebero.portium.entity.fish.EntitySalmon;
import com.michaelsebero.portium.entity.fish.EntityTropicalFish;
import com.michaelsebero.portium.entity.monster.EntityDrowned;
import com.michaelsebero.portium.entity.monster.EntityPhantom;
import com.michaelsebero.portium.entity.passive.EntityDolphin;
import com.michaelsebero.portium.entity.passive.EntityTurtle;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.mixin.client.I18nAccessor;
import com.michaelsebero.portium.mixin.client.LocaleAccessor;
import com.michaelsebero.portium.network.CommandSuggestResponse;
import com.michaelsebero.portium.network.ParticleMessage;
import com.michaelsebero.portium.network.RecipeSyncMessage;
import com.michaelsebero.portium.network.ServerFeaturesMessage;
import com.michaelsebero.portium.network.WaterlogChunkMessage;
import com.michaelsebero.portium.network.ModernStateChunkMessage;
import com.michaelsebero.portium.network.ModernStateUpdateMessage;
import com.michaelsebero.portium.network.WaterlogUpdateMessage;
import com.michaelsebero.portium.tile.TileEntityConduit;
import com.michaelsebero.portium.world.waterlog.IWaterlogChunk;
import com.michaelsebero.portium.modern.client.ModernClientResources;
import com.michaelsebero.portium.modern.client.ModernFailureScreen;
import com.michaelsebero.portium.modern.state.IModernStateChunk;
import com.michaelsebero.portium.client.dev.DevScript;
import com.michaelsebero.portium.client.NetherAmbience;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.item.Item;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.io.File;
import java.util.function.Supplier;

public class ClientProxy extends CommonProxy {

    @Override
    public void construct(FMLConstructionEvent event) {
        super.construct(event);
        PortiumAssets.fetch();
        // Before the resource load that follows construction: Minecraft 1.16.5's look as a pack, and support for modern packs.
        com.michaelsebero.portium.client.ModernTextureImport.run(Minecraft.getMinecraft());
        com.michaelsebero.portium.client.ModernPackSupport.install(Minecraft.getMinecraft());
    }

    @Override
    protected Collection<String> languages() {
        return Arrays.asList("en_us", Minecraft.getMinecraft().gameSettings.language);
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        // Runs after the language manager, which replaces the language map on every resource reload.
        // Screens translate through the client's own table, which 1.12 keeps apart from the one text components use.
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(manager -> {
            Map<String, String> added = ModernLang.inject(Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode());
            ((LocaleAccessor) I18nAccessor.portium$getLocale()).portium$getProperties().putAll(added);
        });
        ModernClientResources.registerLanguageReload();
        ModernFailureScreen.register();
        RenderingRegistry.registerEntityRenderingHandler(EntityTrident.class, RenderTrident::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityCod.class, RenderFish::cod);
        RenderingRegistry.registerEntityRenderingHandler(EntitySalmon.class, RenderFish::salmon);
        RenderingRegistry.registerEntityRenderingHandler(EntityPufferfish.class, RenderPufferfish::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityTropicalFish.class, RenderTropicalFish::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityTurtle.class, RenderTurtle::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityDolphin.class, RenderDolphin::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityDrowned.class, RenderDrowned::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityPhantom.class, RenderPhantom::new);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityConduit.class, new RenderConduit());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        DevScript.install();
        NetherAmbience.registerMusic();
        Minecraft mc = Minecraft.getMinecraft();
        BlockColors blockColors = mc.getBlockColors();
        if (PortiumBlocks.BUBBLE_COLUMN != null) {
            blockColors.registerBlockColorHandler((state, world, pos, tint) ->
                    world != null && pos != null ? BiomeColorHelper.getWaterColorAtPos(world, pos) : -1, PortiumBlocks.BUBBLE_COLUMN);
        }
        for (RenderPlayer renderer : mc.getRenderManager().getSkinMap().values()) {
            renderer.addLayer(new LayerRiptide(renderer));
        }
    }

    @Override
    public void receiveWaterlogChunk(WaterlogChunkMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) return;
            Chunk chunk = mc.world.getChunkProvider().getLoadedChunk(message.chunkX, message.chunkZ);
            if (chunk == null) return;
            ((IWaterlogChunk) chunk).portium$getWaterlog().copyFrom(message.data);
            int x = message.chunkX << 4;
            int z = message.chunkZ << 4;
            mc.world.markBlockRangeForRenderUpdate(x - 1, 0, z - 1, x + 16, 255, z + 16);
        });
    }

    @Override
    public void receiveWaterlogUpdate(WaterlogUpdateMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) return;
            Chunk chunk = mc.world.getChunkProvider().getLoadedChunk(message.pos.getX() >> 4, message.pos.getZ() >> 4);
            if (chunk == null) return;
            ((IWaterlogChunk) chunk).portium$getWaterlog().set(message.pos.getX(), message.pos.getY(), message.pos.getZ(), message.waterlogged);
            mc.world.markBlockRangeForRenderUpdate(message.pos.add(-1, -1, -1), message.pos.add(1, 1, 1));
        });
    }

    @Override
    public void receiveModernStates(ModernStateChunkMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) return;
            Chunk chunk = mc.world.getChunkProvider().getLoadedChunk(message.chunkX, message.chunkZ);
            if (chunk == null) return;
            ((IModernStateChunk) chunk).portium$getModernStates().copyFrom(message.data);
            int x = message.chunkX << 4;
            int z = message.chunkZ << 4;
            mc.world.markBlockRangeForRenderUpdate(x - 1, 0, z - 1, x + 16, 255, z + 16);
        });
    }

    @Override
    public void receiveModernState(ModernStateUpdateMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) return;
            Chunk chunk = mc.world.getChunkProvider().getLoadedChunk(message.pos.getX() >> 4, message.pos.getZ() >> 4);
            if (chunk == null) return;
            ((IModernStateChunk) chunk).portium$getModernStates().put(message.pos.getX(), message.pos.getY(), message.pos.getZ(), message.index);
            mc.world.markBlockRangeForRenderUpdate(message.pos.add(-1, -1, -1), message.pos.add(1, 1, 1));
        });
    }

    /** An integrated server shares its recipes with the client already. */
    @Override
    public void receiveRecipes(RecipeSyncMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (!mc.isIntegratedServerRunning()) DataPackRecipes.receive(message.recipes, message.replaced);
        });
    }

    @Override
    public void receiveServerFeatures(ServerFeaturesMessage message) {
        Minecraft.getMinecraft().addScheduledTask(() -> ModernChat.setServerSupport(message.commandHelp));
    }

    @Override
    public void receiveCommandSuggestions(CommandSuggestResponse message) {
        Minecraft.getMinecraft().addScheduledTask(() -> ModernChat.receive(message));
    }

    @Override
    public void addModernResources(List<File> jars, @javax.annotation.Nullable File minecraftJar) {
        ModernClientResources.install(jars, minecraftJar);
    }

    @Override
    /** Forge 1.13 gives a supplier of a callable making the renderer (both kept lazy for servers). */
    public void setItemRenderer(Item item, Supplier<?> renderer) {
        Object made = renderer.get();
        if (made instanceof java.util.concurrent.Callable) {
            try {
                made = ((java.util.concurrent.Callable<?>) made).call();
            } catch (Exception e) {
                throw new IllegalStateException("Couldn't make the item renderer of " + item.getRegistryName(), e);
            }
        }
        item.setTileEntityItemStackRenderer((TileEntityItemStackRenderer) made);
    }

    @Override
    public void spawnParticles(ParticleMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) return;
            if (message.count == 0) {
                this.spawnParticle(message.kind, mc.world, message.x, message.y, message.z,
                        message.speed * message.deltaX, message.speed * message.deltaY, message.speed * message.deltaZ);
                return;
            }
            Random random = mc.world.rand;
            for (int i = 0; i < message.count; i++) {
                this.spawnParticle(message.kind, mc.world, message.x + random.nextGaussian() * message.deltaX, message.y + random.nextGaussian() * message.deltaY,
                        message.z + random.nextGaussian() * message.deltaZ, random.nextGaussian() * message.speed, random.nextGaussian() * message.speed,
                        random.nextGaussian() * message.speed);
            }
        });
    }

    @Override
    public void spawnParticle(ParticleKind kind, World world, double x, double y, double z, double dx, double dy, double dz) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != world || mc.gameSettings.particleSetting == 2) return;
        Particle particle;
        switch (kind) {
            case BUBBLE_COLUMN_UP: particle = new ParticleBubbleColumnUp(world, x, y, z, dx, dy, dz); break;
            case CURRENT_DOWN: particle = new ParticleCurrentDown(world, x, y, z); break;
            case NAUTILUS: particle = new ParticleNautilus(world, x, y, z, dx, dy, dz); break;
            case DOLPHIN: particle = new ParticleDolphin(world, x, y, z); break;
            case SQUID_INK: particle = new ParticleSquidInk(world, x, y, z, dx, dy, dz); break;
            default: return;
        }
        mc.effectRenderer.addEffect(particle);
    }
}
