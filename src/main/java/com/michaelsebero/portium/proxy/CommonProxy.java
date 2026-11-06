package com.michaelsebero.portium.proxy;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.advancements.PortiumTriggers;
import com.michaelsebero.portium.block.BlockUncarvedPumpkin;
import com.michaelsebero.portium.compat.ModernLang;
import com.michaelsebero.portium.datapack.DataPackRepository;
import com.michaelsebero.portium.datapack.VanillaDataPack;
import com.michaelsebero.portium.entity.monster.EntityDrowned;
import com.michaelsebero.portium.entity.monster.EntityPhantom;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.init.PortiumStats;
import com.michaelsebero.portium.item.ItemTurtleHelmet;
import com.michaelsebero.portium.network.CommandSuggestResponse;
import com.michaelsebero.portium.network.ParticleMessage;
import com.michaelsebero.portium.network.PortiumNetwork;
import com.michaelsebero.portium.network.RecipeSyncMessage;
import com.michaelsebero.portium.network.ServerFeaturesMessage;
import com.michaelsebero.portium.network.WaterlogChunkMessage;
import com.michaelsebero.portium.network.WaterlogUpdateMessage;
import com.michaelsebero.portium.world.ForcedChunks;
import com.michaelsebero.portium.world.OceanWorldGen;
import com.michaelsebero.portium.world.gen.carver.UnderwaterCarvers;
import com.michaelsebero.portium.world.gen.structure.StructureTemplates;
import com.michaelsebero.portium.world.buffet.WorldTypeBuffet;
import com.michaelsebero.portium.world.spawn.PortiumSpawns;
import com.michaelsebero.portium.world.waterlog.WaterlogDispensing;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.io.File;
import java.util.function.Supplier;

public class CommonProxy {

    public void construct(FMLConstructionEvent event) {
        StructureTemplates.fetch();
        com.michaelsebero.portium.world.gen.jigsaw.JigsawData.fetch();
        VanillaDataPack.fetch();
        com.michaelsebero.portium.recipe.StonecuttingRecipes.fetch();
        ModernLang.fetch(this.languages());
    }

    /** The languages whose 1.13 translations to fetch. */
    protected Collection<String> languages() {
        return Collections.singletonList("en_us");
    }

    public void preInit(FMLPreInitializationEvent event) {
        OceanWorldGen.registerTerrainEvents();
        UnderwaterCarvers.registerTerrainEvents();
        com.michaelsebero.portium.world.gen.jigsaw.JigsawStructures.registerTerrainEvents();
        com.michaelsebero.portium.item.MapBanners116.init();
        PortiumStats.init();
        PortiumLootTables.init();
        ForcedChunks.registerCallback();
        if (PortiumConfig.visuals.modernNames) ModernLang.splitSharedNames();
        PortiumNetwork.init();
        PortiumTriggers.init();
        WorldTypeBuffet.register();
        DataPackRepository.createGlobalFolder();
        com.michaelsebero.portium.world.PortiumDataFixes.register();
    }

    public void init(FMLInitializationEvent event) {
        if (PortiumItems.SCUTE != null) ItemTurtleHelmet.TURTLE.setRepairItem(new ItemStack(PortiumItems.SCUTE));
        if (PortiumBlocks.PUMPKIN != null) ((BlockUncarvedPumpkin) PortiumBlocks.PUMPKIN).allowEndermen();
        PortiumSpawns.registerPlacements();
        PortiumSpawns.addBiomeSpawns();
        WaterlogDispensing.register();
        com.michaelsebero.portium.world.gen.plant.Plants114.addBonemealFlowers();
        if (com.michaelsebero.portium.init.PortiumBlocks.RESPAWN_ANCHOR != null) com.michaelsebero.portium.handler.RespawnAnchors.registerDispenserBehavior();
        if (com.michaelsebero.portium.init.PortiumBlocks.BEEHIVE != null || com.michaelsebero.portium.init.PortiumBlocks.BEE_NEST != null) com.michaelsebero.portium.block.bee.HiveDispensing.register();
        EntityParrot.registerMimicSound(EntityDrowned.class, PortiumSounds.ENTITY_PARROT_IMITATE_DROWNED);
        EntityParrot.registerMimicSound(EntityPhantom.class, PortiumSounds.ENTITY_PARROT_IMITATE_PHANTOM);
    }

    public void postInit(FMLPostInitializationEvent event) {}

    /** Takes a chunk's waterlogged cells from the server; does nothing on a dedicated server. */
    public void receiveWaterlogChunk(WaterlogChunkMessage message) {}

    /** Takes one cell's water from the server; does nothing on a dedicated server. */
    public void receiveWaterlogUpdate(WaterlogUpdateMessage message) {}

    /** The player of this client, for client-side checks such as what's held while aiming; null on a server. */
    @javax.annotation.Nullable
    /** The next death screen is skipped: the player respawns at once (client only; 1.15's doImmediateRespawn). */
    public void respawnImmediately() {}

    /** Opens a jigsaw block's screen (client only). */
    public void openJigsaw(com.michaelsebero.portium.tile.TileEntityJigsaw jigsaw) {}

    public net.minecraft.entity.player.EntityPlayer clientPlayer() {
        return null;
    }

    /** Marks the client's copy of a map locked or not; does nothing on a dedicated server. */
    public void receiveMapLock(com.michaelsebero.portium.network.MapLockMessage message) {}

    /** Fills the open 1.14 trade screen; does nothing on a dedicated server. */
    public void receiveMerchantOffers(com.michaelsebero.portium.network.MerchantOffersMessage message) {}

    public void receiveModernStates(com.michaelsebero.portium.network.ModernStateChunkMessage message) {}

    public void receiveModernState(com.michaelsebero.portium.network.ModernStateUpdateMessage message) {}

    /** Takes the data pack recipes a server sent; does nothing on a dedicated server. */
    public void receiveRecipes(RecipeSyncMessage message) {}

    /** Learns what the server offers beyond 1.12's protocol; does nothing on a dedicated server. */
    public void receiveServerFeatures(ServerFeaturesMessage message) {}

    /** Shows the server's help for the command being typed; does nothing on a dedicated server. */
    public void receiveCommandSuggestions(CommandSuggestResponse message) {}

    /** Spawns the particles a {@link ParticleMessage} describes; does nothing on a dedicated server. */
    public void spawnParticles(ParticleMessage message) {}

    /** 1.13 mods' resources: on the client, their jars become resource packs. */
    public void addModernResources(List<File> jars, @javax.annotation.Nullable File minecraftJar) {}

    /** A 1.13 item's stack renderer (Forge 1.13's {@code setTEISR}): made on the client only. */
    public void setItemRenderer(Item item, Supplier<?> renderer) {}

    /** Spawns a client-side particle; does nothing on a dedicated server. */
    public void spawnParticle(ParticleKind kind, World world, double x, double y, double z, double dx, double dy, double dz) {}
}
