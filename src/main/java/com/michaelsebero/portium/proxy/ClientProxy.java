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
import net.minecraft.entity.Entity;
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
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ClientProxy.class);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(com.michaelsebero.portium.client.ParticleRendering116.class);
        super.preInit(event);
        // Runs after the language manager, which replaces the language map on every resource reload.
        // Screens translate through the client's own table, which 1.12 keeps apart from the one text components use.
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(manager -> {
            Map<String, String> added = ModernLang.inject(Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode());
            ((LocaleAccessor) I18nAccessor.portium$getLocale()).portium$getProperties().putAll(added);
        });
        ModernClientResources.registerLanguageReload();
        ModernFailureScreen.register();
        com.michaelsebero.portium.client.model.TemplateOverrideStubs.register();
        RenderingRegistry.registerEntityRenderingHandler(EntityTrident.class, RenderTrident::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityCod.class, RenderFish::cod);
        RenderingRegistry.registerEntityRenderingHandler(EntitySalmon.class, RenderFish::salmon);
        RenderingRegistry.registerEntityRenderingHandler(EntityPufferfish.class, RenderPufferfish::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityTropicalFish.class, RenderTropicalFish::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityTurtle.class, RenderTurtle::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityDolphin.class, RenderDolphin::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityDrowned.class, RenderDrowned::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityPhantom.class, RenderPhantom::new);
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.monster.EntityPillager.class,
                com.michaelsebero.portium.client.render.RenderPillager::new);
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.monster.EntityRavager.class,
                com.michaelsebero.portium.client.render.RenderRavager::new);
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.merchant.EntityWanderingTrader.class,
                com.michaelsebero.portium.client.render.RenderWanderingTrader::new);
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.merchant.EntityTraderLlama.class,
                com.michaelsebero.portium.client.render.RenderTraderLlama::new);
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.monster.piglin.EntityPiglin.class,
                manager -> new com.michaelsebero.portium.client.render.RenderPiglin<>(manager, "piglin/piglin"));
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.monster.piglin.EntityPiglinBrute.class,
                manager -> new com.michaelsebero.portium.client.render.RenderPiglin<>(manager, "piglin/piglin_brute"));
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.monster.EntityZombifiedPiglin.class,
                com.michaelsebero.portium.client.render.RenderZombifiedPiglin::new);
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.villager.EntityVillager116.class,
                com.michaelsebero.portium.client.render.RenderVillager116::new);
        if (com.michaelsebero.portium.PortiumConfig.mobs.villagers) {
            RenderingRegistry.registerEntityRenderingHandler(net.minecraft.entity.monster.EntityZombieVillager.class,
                    com.michaelsebero.portium.client.render.RenderZombieVillager116::new);
        }
        // 1.14's vindicators and evokers: 1.16's illager model, celebrating, and raid captains' banners on their heads.
        RenderingRegistry.registerEntityRenderingHandler(net.minecraft.entity.monster.EntityVindicator.class,
                com.michaelsebero.portium.client.render.RenderIllager116::vindicator);
        RenderingRegistry.registerEntityRenderingHandler(net.minecraft.entity.monster.EntityEvoker.class,
                com.michaelsebero.portium.client.render.RenderIllager116::evoker);
        // 1.15's horse model and 64x64 textures.
        RenderingRegistry.registerEntityRenderingHandler(net.minecraft.entity.passive.EntityHorse.class,
                com.michaelsebero.portium.client.render.RenderHorse116::horse);
        RenderingRegistry.registerEntityRenderingHandler(net.minecraft.entity.passive.EntityDonkey.class,
                com.michaelsebero.portium.client.render.RenderHorse116::donkey);
        RenderingRegistry.registerEntityRenderingHandler(net.minecraft.entity.passive.EntityMule.class,
                com.michaelsebero.portium.client.render.RenderHorse116::mule);
        RenderingRegistry.registerEntityRenderingHandler(net.minecraft.entity.passive.EntityZombieHorse.class,
                com.michaelsebero.portium.client.render.RenderHorse116::undead);
        RenderingRegistry.registerEntityRenderingHandler(net.minecraft.entity.passive.EntitySkeletonHorse.class,
                com.michaelsebero.portium.client.render.RenderHorse116::undead);
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.passive.bee.EntityBee.class,
                com.michaelsebero.portium.client.render.RenderBee::new);
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.passive.EntityStrider.class,
                com.michaelsebero.portium.client.render.RenderStrider::new);
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.monster.EntityHoglin.class,
                manager -> new com.michaelsebero.portium.client.render.RenderBoar<>(manager, "hoglin/hoglin"));
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.monster.EntityZoglin.class,
                manager -> new com.michaelsebero.portium.client.render.RenderBoar<>(manager, "hoglin/zoglin"));
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.passive.EntityFox.class,
                com.michaelsebero.portium.client.render.RenderFox::new);
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.passive.EntityPanda.class,
                com.michaelsebero.portium.client.render.RenderPanda::new);
        RenderingRegistry.registerEntityRenderingHandler(com.michaelsebero.portium.entity.passive.EntityCat.class,
                com.michaelsebero.portium.client.render.RenderCat::new);
        RenderingRegistry.registerEntityRenderingHandler(net.minecraft.entity.item.EntityFireworkRocket.class,
                com.michaelsebero.portium.client.render.RenderFireworkRocket116::new);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityConduit.class, new RenderConduit());
        ClientRegistry.bindTileEntitySpecialRenderer(com.michaelsebero.portium.tile.TileEntityPortiumSign.class,
                new com.michaelsebero.portium.client.render.TileEntityPortiumSignRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(com.michaelsebero.portium.tile.TileEntityCampfire.class,
                new com.michaelsebero.portium.client.render.RenderCampfire());
        ClientRegistry.bindTileEntitySpecialRenderer(com.michaelsebero.portium.tile.TileEntityBell.class,
                new com.michaelsebero.portium.client.render.RenderBell());
        ClientRegistry.bindTileEntitySpecialRenderer(com.michaelsebero.portium.tile.TileEntityLectern.class,
                new com.michaelsebero.portium.client.render.RenderLectern());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        DevScript.install();
        NetherAmbience.registerMusic();
        if (PortiumBlocks.LODESTONE != null) com.michaelsebero.portium.client.LodestoneCompassAngle.install();
        Minecraft mc = Minecraft.getMinecraft();
        BlockColors blockColors = mc.getBlockColors();
        if (PortiumBlocks.BUBBLE_COLUMN != null) {
            blockColors.registerBlockColorHandler((state, world, pos, tint) ->
                    world != null && pos != null ? BiomeColorHelper.getWaterColorAtPos(world, pos) : -1, PortiumBlocks.BUBBLE_COLUMN);
        }
        if (com.michaelsebero.portium.init.PortiumItems.LEATHER_HORSE_ARMOR != null) {
            mc.getItemColors().registerItemColorHandler((stack, tint) -> tint == 0 ? com.michaelsebero.portium.item.ItemLeatherHorseArmor.getColor(stack) : -1,
                    com.michaelsebero.portium.init.PortiumItems.LEATHER_HORSE_ARMOR);
        }
        // 1.16 Forge gives the main framebuffer stencil bits when mods ask (Create's Ponder masks its button icons with
        // them); 1.12's has none unless enabled.
        if (com.michaelsebero.portium.mods116.Mods116.active() && !mc.getFramebuffer().isStencilEnabled()) mc.getFramebuffer().enableStencil();
        for (RenderPlayer renderer : mc.getRenderManager().getSkinMap().values()) {
            renderer.addLayer(new LayerRiptide(renderer));
            renderer.addLayer(new com.michaelsebero.portium.client.render.LayerBeeStingers(renderer));
        }
        if (com.michaelsebero.portium.PortiumConfig.mechanics.golemCracks) {
            net.minecraft.client.renderer.entity.Render<?> golem = mc.getRenderManager().entityRenderMap.get(net.minecraft.entity.monster.EntityIronGolem.class);
            if (golem instanceof net.minecraft.client.renderer.entity.RenderIronGolem) {
                ((net.minecraft.client.renderer.entity.RenderIronGolem) golem).addLayer(
                        new com.michaelsebero.portium.client.render.LayerGolemCracks((net.minecraft.client.renderer.entity.RenderIronGolem) golem));
            }
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
    public net.minecraft.entity.player.EntityPlayer clientPlayer() {
        return Minecraft.getMinecraft().player;
    }

    @Override
    public void receiveMerchantOffers(com.michaelsebero.portium.network.MerchantOffersMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.player == null) return;
            net.minecraft.inventory.Container container = mc.player.openContainer;
            if (message.getWindowId() != container.windowId || !(container instanceof com.michaelsebero.portium.merchant.ContainerMerchant116)) return;
            com.michaelsebero.portium.merchant.ContainerMerchant116 merchant = (com.michaelsebero.portium.merchant.ContainerMerchant116) container;
            merchant.setClientSideOffers(message.getOffers());
            merchant.setXp(message.getXp());
            merchant.setMerchantLevel(message.getLevel());
            merchant.setShowProgressBar(message.showProgress());
            merchant.setCanRestock(message.canRestock());
        });
    }

    @Override
    public void receiveMapLock(com.michaelsebero.portium.network.MapLockMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) return;
            // Before the map's own packet, the client has no copy yet: made here as 1.12's packet handler makes it.
            net.minecraft.world.storage.MapData data = net.minecraft.item.ItemMap.loadMapData(message.mapId, mc.world);
            if (data == null) {
                data = new net.minecraft.world.storage.MapData("map_" + message.mapId);
                mc.world.setData(data.mapName, data);
            }
            ((com.michaelsebero.portium.item.ILockableMap) data).portium$setLocked(message.locked);
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
    /** Set by the server's doImmediateRespawn message, which arrives before the death packet that opens the death screen. */
    private static boolean respawnImmediately;

    @Override
    public void respawnImmediately() {
        Minecraft.getMinecraft().addScheduledTask(() -> respawnImmediately = true);
    }

    /** The death screen 1.15 skips with doImmediateRespawn: the player respawns instead of seeing it. */
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public static void onGuiOpen(net.minecraftforge.client.event.GuiOpenEvent event) {
        if (!respawnImmediately || !(event.getGui() instanceof net.minecraft.client.gui.GuiGameOver)) return;
        respawnImmediately = false;
        event.setCanceled(true);
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player != null) minecraft.player.respawnPlayer();
    }

    @Override
    public void openJigsaw(com.michaelsebero.portium.tile.TileEntityJigsaw jigsaw) {
        Minecraft.getMinecraft().displayGuiScreen(new com.michaelsebero.portium.client.gui.GuiJigsaw116(jigsaw));
    }

    @Override
    public void receiveRecipes(RecipeSyncMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (!mc.isIntegratedServerRunning()) DataPackRecipes.receive(message);
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
        if (mc.world != world) return;
        if (kind == ParticleKind.CAMPFIRE_COSY_SMOKE || kind == ParticleKind.CAMPFIRE_SIGNAL_SMOKE) {
            // 1.14 spawns campfire smoke whatever the distance and the Particles setting.
        } else if (kind.limited()) {
            Entity camera = mc.getRenderViewEntity();
            if (camera == null || camera.getDistanceSq(x, y, z) > 1024.0D || !particleLevelAllows(mc, world)) return;
        } else if (mc.gameSettings.particleSetting == 2) {
            return;
        }
        Particle particle;
        switch (kind) {
            case BUBBLE_COLUMN_UP: particle = new ParticleBubbleColumnUp(world, x, y, z, dx, dy, dz); break;
            case CURRENT_DOWN: particle = new ParticleCurrentDown(world, x, y, z); break;
            case NAUTILUS: particle = new ParticleNautilus(world, x, y, z, dx, dy, dz); break;
            case DOLPHIN: particle = new ParticleDolphin(world, x, y, z); break;
            case SQUID_INK: particle = new ParticleSquidInk(world, x, y, z, dx, dy, dz); break;
            case SOUL_FIRE_FLAME: particle = new com.michaelsebero.portium.client.particle.ParticleSoulFireFlame(world, x, y, z, dx, dy, dz); break;
            case CAMPFIRE_COSY_SMOKE: particle = new com.michaelsebero.portium.client.particle.ParticleCampfireSmoke(world, x, y, z, dx, dy, dz, false); break;
            case CAMPFIRE_SIGNAL_SMOKE: particle = new com.michaelsebero.portium.client.particle.ParticleCampfireSmoke(world, x, y, z, dx, dy, dz, true); break;
            case REVERSE_PORTAL: particle = new com.michaelsebero.portium.client.particle.ParticleReversePortal(world, x, y, z, dx, dy, dz); break;
            case COMPOSTER: particle = new com.michaelsebero.portium.client.particle.ParticleComposter(world, x, y, z, dx, dy, dz); break;
            case DRIPPING_HONEY: particle = new com.michaelsebero.portium.client.particle.ParticleHoneyDrip(world, x, y, z, false); break;
            case FALLING_NECTAR: particle = new com.michaelsebero.portium.client.particle.ParticleHoneyDrip(world, x, y, z, true); break;
            case SOUL: particle = new com.michaelsebero.portium.client.particle.ParticleSoul(world, x, y, z, dx, dy, dz); break;
            case SNEEZE:
                // 1.14 colours its cloud (200, 50, 120) as bytes that wrap round, which shows green.
                particle = new net.minecraft.client.particle.ParticleCloud.Factory().createParticle(0, world, x, y, z, dx, dy, dz);
                if (particle == null) return;
                particle.setRBGColorF(56.0F / 255.0F, 206.0F / 255.0F, 136.0F / 255.0F);
                particle.setAlphaF(0.4F);
                break;
            default: return;
        }
        mc.effectRenderer.addEffect(particle);
    }

    /** RenderGlobal's particle level for block effects: Decreased drops a third of them, Minimal all. */
    private static boolean particleLevelAllows(Minecraft mc, World world) {
        int level = mc.gameSettings.particleSetting;
        if (level == 1 && world.rand.nextInt(3) == 0) level = 2;
        return level <= 1;
    }
}
