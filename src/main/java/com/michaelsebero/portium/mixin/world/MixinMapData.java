package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.item.ILockableMap;
import com.michaelsebero.portium.item.IMapBanners;
import com.michaelsebero.portium.item.IMapDecorationName;
import com.michaelsebero.portium.item.MapBanners116;
import com.michaelsebero.portium.network.MapLockMessage;
import com.michaelsebero.portium.network.PortiumNetwork;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.MapDecoration;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.network.Packet;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A map's lock (1.14's cartography table), saved as 1.14 saves it ({@code locked}) and sent to the players it's sent
 * to, as 1.16's map packet carries it ({@link MapLockMessage}). And 1.13's banner markers: the banners the map has been
 * used on, saved as 1.13 saves them ({@code banners}), each shown as a marker in its colour with its name.
 */
@Mixin(MapData.class)
public abstract class MixinMapData implements ILockableMap, IMapBanners {

    @Shadow
    public int xCenter;
    @Shadow
    public int zCenter;
    @Shadow
    public int dimension;
    @Shadow
    public byte scale;
    @Shadow
    public Map<String, MapDecoration> mapDecorations;

    @Unique
    private Map<String, MapBanners116.Banner> portium$banners;

    @Unique
    private boolean portium$locked;
    /** Players whose copy of this map knows it's locked: told with the first map packet each player entity gets. */
    @Unique
    private Set<EntityPlayer> portium$toldLocked;

    @Override
    public boolean portium$isLocked() {
        return this.portium$locked;
    }

    @Override
    public void portium$setLocked(boolean locked) {
        this.portium$locked = locked;
    }

    @Inject(method = "getMapPacket", at = @At("RETURN"))
    private void portium$sendLock(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<Packet<?>> cir) {
        if (!this.portium$locked || cir.getReturnValue() == null || !(player instanceof EntityPlayerMP)) return;
        if (this.portium$toldLocked == null) this.portium$toldLocked = Collections.newSetFromMap(new WeakHashMap<>());
        if (this.portium$toldLocked.add(player)) {
            PortiumNetwork.CHANNEL.sendTo(new MapLockMessage(stack.getMetadata(), true), (EntityPlayerMP) player);
        }
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void portium$readLock(NBTTagCompound nbt, CallbackInfo ci) {
        this.portium$locked = nbt.getBoolean("locked");
        this.portium$banners().clear();
        NBTTagList banners = nbt.getTagList("banners", 10);
        for (int i = 0; i < banners.tagCount(); i++) this.portium$mark(MapBanners116.Banner.read(banners.getCompoundTagAt(i)), null);
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void portium$writeLock(NBTTagCompound compound, CallbackInfoReturnable<NBTTagCompound> cir) {
        if (this.portium$locked) cir.getReturnValue().setBoolean("locked", true);
        if (this.portium$banners().isEmpty()) return;
        NBTTagList banners = new NBTTagList();
        for (MapBanners116.Banner banner : this.portium$banners().values()) banners.appendTag(banner.write());
        cir.getReturnValue().setTag("banners", banners);
    }

    @Override
    public Map<String, MapBanners116.Banner> portium$banners() {
        // Made on first use: a mixin's field initializer isn't run for every way MapData is constructed.
        if (this.portium$banners == null) this.portium$banners = new LinkedHashMap<>();
        return this.portium$banners;
    }

    @Override
    public void portium$toggleBanner(World world, BlockPos pos) {
        int size = 1 << this.scale;
        float x = (pos.getX() + 0.5F - this.xCenter) / size;
        float z = (pos.getZ() + 0.5F - this.zCenter) / size;
        if (x < -63.0F || z < -63.0F || x > 63.0F || z > 63.0F) return;
        MapBanners116.Banner banner = MapBanners116.Banner.at(world, pos);
        if (banner == null) return;
        String id = banner.id();
        if (banner.equals(this.portium$banners().get(id))) {
            this.portium$banners().remove(id);
            this.mapDecorations.remove(id);
        } else {
            this.portium$mark(banner, world);
        }
        ((MapData) (Object) this).markDirty();
    }

    @Override
    public void portium$removeStaleBanners(World world) {
        boolean changed = false;
        for (Iterator<MapBanners116.Banner> it = this.portium$banners().values().iterator(); it.hasNext(); ) {
            MapBanners116.Banner banner = it.next();
            if (!world.isBlockLoaded(banner.pos) || banner.equals(MapBanners116.Banner.at(world, banner.pos))) continue;
            it.remove();
            this.mapDecorations.remove(banner.id());
            changed = true;
        }
        if (changed) ((MapData) (Object) this).markDirty();
    }

    /** Adds a banner and its marker, pointing up the map (1.13's 180 degrees), named as the banner is. */
    @Unique
    private void portium$mark(MapBanners116.Banner banner, World world) {
        String id = banner.id();
        this.portium$banners().put(id, banner);
        int size = 1 << this.scale;
        float x = (banner.pos.getX() + 0.5F - this.xCenter) / size;
        float z = (banner.pos.getZ() + 0.5F - this.zCenter) / size;
        byte rotation = (byte) (int) ((180.0D + 8.0D) * 16.0D / 360.0D);
        if (this.dimension < 0 && world != null) {
            int spin = (int) (world.getWorldInfo().getWorldTime() / 10L);
            rotation = (byte) (spin * spin * 34187121 + spin * 121 >> 15 & 15);
        }
        MapDecoration decoration = new MapDecoration(MapBanners116.bannerType(banner.color), (byte) (int) (x * 2.0F + 0.5F), (byte) (int) (z * 2.0F + 0.5F),
                rotation);
        ((IMapDecorationName) decoration).portium$setName(banner.name);
        this.mapDecorations.put(id, decoration);
    }
}
