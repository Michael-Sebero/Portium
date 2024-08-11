package com.michaelsebero.portium;

import com.michaelsebero.portium.command.impl.BossBarCommand;
import com.michaelsebero.portium.compat.ModernLang;
import com.michaelsebero.portium.datapack.DataPackManager;
import com.michaelsebero.portium.init.PortiumRecipes;
import com.michaelsebero.portium.modern.ModernMods;
import com.michaelsebero.portium.mods116.Mods116;
import com.michaelsebero.portium.proxy.CommonProxy;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderException;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.MOD_VERSION,
        dependencies = "required-after:mixinbooter@[11.0,);required-client-after:assetmover@[2.5,);after:assetmover@[2.5,)")
public final class Portium {

    public static final String MODID = Tags.MOD_ID;
    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @Mod.Instance(Tags.MOD_ID)
    public static Portium instance;

    @SidedProxy(clientSide = "com.michaelsebero.portium.proxy.ClientProxy", serverSide = "com.michaelsebero.portium.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void construct(FMLConstructionEvent event) {
        // Both mods rework the same water behaviour; running them together would break both.
        if (Loader.isModLoaded("fluidlogged_api")) {
            throw new LoaderException("Portium has its own 1.13 waterlogging and can't run alongside Fluidlogged API. Remove one of the two mods.");
        }
        proxy.construct(event);
        ModernMods.construct();
        Mods116.construct();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
        ModernMods.preInit();
        Mods116.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        PortiumRecipes.registerFurnaceAndBrewing();
        proxy.init(event);
        ModernMods.init();
        Mods116.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
        ModernMods.postInit();
        Mods116.postInit();
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        ModernMods.loadComplete();
        Mods116.loadComplete();
    }

    /**
     * The worlds are loaded by now, so their saved boss bars and data pack settings can be read. A dedicated server
     * also learns 1.13's messages here, for its console; clients do so whenever their language loads.
     */
    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        ModernMods.serverAboutToStart(event.getServer());
        Mods116.serverAboutToStart(event.getServer());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        ModernMods.serverStarting(event.getServer());
        Mods116.serverStarting(event.getServer());
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server.isDedicatedServer()) ModernLang.inject("en_us");
        BossBarCommand.load(server);
        if (PortiumConfig.compatibility.dataPacks) DataPackManager.start(server);
        ModernMods.serverStarted(server);
        Mods116.serverStarted(server);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        ModernMods.serverStopping(FMLCommonHandler.instance().getMinecraftServerInstance());
        Mods116.serverStopping(FMLCommonHandler.instance().getMinecraftServerInstance());
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        DataPackManager.stop();
        ModernMods.serverStopped(server);
        Mods116.serverStopped(server);
    }
}
