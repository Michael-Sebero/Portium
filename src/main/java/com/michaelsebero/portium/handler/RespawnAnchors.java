package com.michaelsebero.portium.handler;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.block.nether.BlockRespawnAnchor;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketChangeGameState;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Respawning at respawn anchors (1.16). A player's anchor is remembered beside 1.12's per-dimension respawn point (in
 * the player's persisted data, which survives death); {@code MixinPlayerList} asks {@link #respawnDimension} where a
 * dying player goes: to the anchor's dimension while the anchor can take them, else (1.16's way) to the world spawn.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class RespawnAnchors {

    private static final String KEY = "portium:RespawnAnchor";
    /** Set by a failed anchor respawn, so the player is told after respawning; read on the server thread. */
    private static final String FAILED = "portium:RespawnAnchorFailed";

    /** Called when a player sets their respawn point at an anchor. */
    public static void remember(EntityPlayerMP player, int dimension, BlockPos pos) {
        NBTTagCompound anchor = new NBTTagCompound();
        anchor.setInteger("Dim", dimension);
        anchor.setInteger("X", pos.getX());
        anchor.setInteger("Y", pos.getY());
        anchor.setInteger("Z", pos.getZ());
        persisted(player).setTag(KEY, anchor);
    }

    /** Where a dying player respawns: the anchor's dimension if their anchor still takes them. */
    public static int respawnDimension(EntityPlayerMP player, int dimension) {
        NBTTagCompound anchor = persisted(player).getCompoundTag(KEY);
        if (anchor.isEmpty()) return dimension;
        int anchorDimension = anchor.getInteger("Dim");
        BlockPos pos = new BlockPos(anchor.getInteger("X"), anchor.getInteger("Y"), anchor.getInteger("Z"));
        if (player.getSpawnDimension() != anchorDimension || !pos.equals(player.getBedLocation(anchorDimension))) {
            // Something else set the respawn point since.
            persisted(player).removeTag(KEY);
            return dimension;
        }
        WorldServer world = player.server.getWorld(anchorDimension);
        if (world != null && BlockRespawnAnchor.canRespawnAt(world, pos)) return anchorDimension;
        // Out of charges, gone or blocked: 1.16 respawns the player at the world spawn and forgets the anchor.
        player.setSpawnChunk(null, false, anchorDimension);
        player.setSpawnDimension(null);
        persisted(player).removeTag(KEY);
        persisted(player).setBoolean(FAILED, true);
        return 0;
    }

    /** A bed (or anything else) set a new respawn point: the anchor's is replaced, as 1.16 has only one. */
    @SubscribeEvent
    public static void onSetSpawn(PlayerSetSpawnEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (event.getNewSpawn() == null || player.world.isRemote) return;
        NBTTagCompound anchor = persisted(player).getCompoundTag(KEY);
        if (anchor.isEmpty()) return;
        player.setSpawnChunk(null, false, anchor.getInteger("Dim"));
        player.setSpawnDimension(null);
        persisted(player).removeTag(KEY);
    }

    /** After respawning at an anchor its charge is heard going; after a failed one the player is told. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        NBTTagCompound data = persisted(player);
        if (data.getBoolean(FAILED)) {
            data.removeTag(FAILED);
            player.connection.sendPacket(new SPacketChangeGameState(0, 0.0F));
            return;
        }
        NBTTagCompound anchor = data.getCompoundTag(KEY);
        if (anchor.isEmpty() || anchor.getInteger("Dim") != player.dimension) return;
        BlockPos pos = new BlockPos(anchor.getInteger("X"), anchor.getInteger("Y"), anchor.getInteger("Z"));
        if (player.world.getBlockState(pos).getBlock() instanceof BlockRespawnAnchor) {
            player.connection.sendPacket(new SPacketSoundEffect(PortiumSounds.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.BLOCKS, pos.getX(), pos.getY(),
                    pos.getZ(), 1.0F, 1.0F));
        }
    }

    private static NBTTagCompound persisted(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG, 10)) data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    // ---- Dispensers charge anchors with glowstone (1.16) ----

    public static void registerDispenserBehavior() {
        Item glowstone = Item.getItemFromBlock(Blocks.GLOWSTONE);
        IBehaviorDispenseItem previous = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(glowstone);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(glowstone, new ChargeAnchor(previous));
    }

    /** Glowstone dispensed into an anchor charges it; anything else dispenses as before. */
    private static final class ChargeAnchor implements IBehaviorDispenseItem {

        private final IBehaviorDispenseItem previous;

        ChargeAnchor(IBehaviorDispenseItem previous) {
            this.previous = previous;
        }

        @Override
        public ItemStack dispense(IBlockSource source, ItemStack stack) {
            EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
            BlockPos pos = source.getBlockPos().offset(facing);
            World world = source.getWorld();
            IBlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof BlockRespawnAnchor)) return this.previous.dispense(source, stack);
            boolean charged = state.getValue(BlockRespawnAnchor.CHARGES) != 4;
            if (charged) {
                BlockRespawnAnchor.charge(world, pos, state);
                stack.shrink(1);
            }
            world.playEvent(charged ? 1000 : 1001, source.getBlockPos(), 0);
            world.playEvent(2000, source.getBlockPos(), facing.getIndex());
            return stack;
        }
    }

    private RespawnAnchors() {}
}
