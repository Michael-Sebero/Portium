package com.michaelsebero.portium.mixin.item;

import com.michaelsebero.portium.item.ILockableMap;
import com.michaelsebero.portium.item.LockedMaps;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Locked maps (1.14) no longer draw the world, and a map taken from a cartography table with glass gets locked. The
 * tooltip is 1.16's: "Locked", and with advanced tooltips the map's id too.
 */
@Mixin(ItemMap.class)
public abstract class MixinItemMap {

    @Shadow
    public abstract MapData getMapData(ItemStack stack, World world);

    @SideOnly(Side.CLIENT)
    @Inject(method = "addInformation", at = @At("HEAD"), cancellable = true)
    private void portium$tooltip116(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag, CallbackInfo ci) {
        ci.cancel();
        MapData data = world == null ? null : this.getMapData(stack, world);
        if (LockedMaps.isLocked(data)) tooltip.add(TextFormatting.GRAY + I18n.format("filled_map.locked", stack.getMetadata()));
        if (!flag.isAdvanced()) return;
        if (data != null) {
            tooltip.add(TextFormatting.GRAY + I18n.format("filled_map.id", stack.getMetadata()));
            tooltip.add(TextFormatting.GRAY + I18n.format("filled_map.scale", 1 << data.scale));
            tooltip.add(TextFormatting.GRAY + I18n.format("filled_map.level", data.scale, 4));
        } else {
            tooltip.add(TextFormatting.GRAY + I18n.format("filled_map.unknown"));
        }
    }

    @Inject(method = "updateMapData", at = @At("HEAD"), cancellable = true)
    private void portium$locked(World world, Entity viewer, MapData data, CallbackInfo ci) {
        if (((ILockableMap) data).portium$isLocked()) ci.cancel();
    }

    /** 1.13: banners that have gone or changed lose their markers as the map is updated. */
    @Inject(method = "updateMapData", at = @At("TAIL"))
    private void portium$staleBanners(World world, Entity viewer, MapData data, CallbackInfo ci) {
        if (!world.isRemote && world.getTotalWorldTime() % 20L == 0L) ((com.michaelsebero.portium.item.IMapBanners) data).portium$removeStaleBanners(world);
    }

    @Inject(method = "onCreated", at = @At("HEAD"), cancellable = true)
    private void portium$lock(ItemStack stack, World world, EntityPlayer player, CallbackInfo ci) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.getBoolean("map_to_lock")) {
            LockedMaps.lock(world, stack);
            tag.removeTag("map_to_lock");
            ci.cancel();
        }
    }
}
