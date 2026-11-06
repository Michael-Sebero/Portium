package com.michaelsebero.portium.item;

import com.michaelsebero.portium.Tags;
import net.minecraft.block.BlockBanner;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.MapDecoration;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;

/**
 * 1.13's map markers: a banner marker in the banner's colour (and with its name, if it has one) for each banner a
 * map has been used on, and buried treasure's red X. 1.13 numbers these icons 10 to 26, after 1.12's ten; they're
 * added to 1.12's decoration types in that order, so a type's icon is its 1.13 number.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class MapBanners116 {

    private static final MapDecoration.Type[] BANNERS = new MapDecoration.Type[16];
    public static MapDecoration.Type RED_X;

    /** Adds the types (in preInit, before any map is read). */
    public static void init() {
        if (RED_X != null) return;
        Class<?>[] params = {boolean.class, int.class};
        for (EnumDyeColor color : EnumDyeColor.values()) {
            BANNERS[color.getMetadata()] = EnumHelper.addEnum(MapDecoration.Type.class, "BANNER_" + color.getName().toUpperCase(Locale.ROOT), params,
                    true, -1);
        }
        RED_X = EnumHelper.addEnum(MapDecoration.Type.class, "RED_X", params, true, -1);
    }

    public static MapDecoration.Type bannerType(EnumDyeColor color) {
        return BANNERS[color.getMetadata()];
    }

    /** A banner a map marks: where it stands, its base colour and its name. */
    public static final class Banner {
        public final BlockPos pos;
        public final EnumDyeColor color;
        @Nullable
        public final ITextComponent name;

        public Banner(BlockPos pos, EnumDyeColor color, @Nullable ITextComponent name) {
            this.pos = pos;
            this.color = color;
            this.name = name;
        }

        /** The banner standing at {@code pos} now, or null. */
        @Nullable
        public static Banner at(World world, BlockPos pos) {
            TileEntity tile = world.getTileEntity(pos);
            if (!(tile instanceof TileEntityBanner) || !(world.getBlockState(pos).getBlock() instanceof BlockBanner)) return null;
            TileEntityBanner banner = (TileEntityBanner) tile;
            // The base colour as saved (1.12's colour list is client-only); "Base" is a dye damage value.
            EnumDyeColor color = EnumDyeColor.byDyeDamage(banner.writeToNBT(new NBTTagCompound()).getInteger("Base"));
            return new Banner(pos, color, banner.hasCustomName() ? banner.getDisplayName() : null);
        }

        public String id() {
            return "banner-" + this.pos.getX() + "," + this.pos.getY() + "," + this.pos.getZ();
        }

        public NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            NBTTagCompound at = new NBTTagCompound();
            at.setInteger("X", this.pos.getX());
            at.setInteger("Y", this.pos.getY());
            at.setInteger("Z", this.pos.getZ());
            tag.setTag("Pos", at);
            tag.setString("Color", this.color.getName());
            if (this.name != null) tag.setString("Name", ITextComponent.Serializer.componentToJson(this.name));
            return tag;
        }

        public static Banner read(NBTTagCompound tag) {
            NBTTagCompound at = tag.getCompoundTag("Pos");
            EnumDyeColor color = EnumDyeColor.WHITE;
            for (EnumDyeColor candidate : EnumDyeColor.values()) {
                if (candidate.getName().equals(tag.getString("Color"))) color = candidate;
            }
            ITextComponent name = tag.hasKey("Name", 8) ? ITextComponent.Serializer.jsonToComponent(tag.getString("Name")) : null;
            return new Banner(new BlockPos(at.getInteger("X"), at.getInteger("Y"), at.getInteger("Z")), color, name);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Banner)) return false;
            Banner banner = (Banner) other;
            return this.pos.equals(banner.pos) && this.color == banner.color && Objects.equals(this.name, banner.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.pos, this.color, this.name);
        }
    }

    /** A filled map used on a banner adds its marker, or takes it away if it's there (1.13). */
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ItemMap)) return;
        World world = event.getWorld();
        if (!(world.getBlockState(event.getPos()).getBlock() instanceof BlockBanner)) return;
        if (!world.isRemote) {
            MapData data = Items.FILLED_MAP.getMapData(stack, world);
            if (data != null) ((IMapBanners) data).portium$toggleBanner(world, event.getPos());
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    private MapBanners116() {}
}
