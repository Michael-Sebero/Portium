package com.michaelsebero.portium.mixin.network;

import com.michaelsebero.portium.item.IMapDecorationName;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketMaps;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.storage.MapDecoration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The map packet's markers as 1.13 sends them: the icon as a whole number (1.12 packs it into four bits, and 1.13's
 * banner markers and red X are numbered 10 to 26) and a marker's name. The rest is 1.12's.
 */
@Mixin(SPacketMaps.class)
public abstract class MixinSPacketMaps {

    @Shadow
    private int mapId;
    @Shadow
    private byte mapScale;
    @Shadow
    private boolean trackingPosition;
    @Shadow
    private MapDecoration[] icons;
    @Shadow
    private int minX;
    @Shadow
    private int minZ;
    @Shadow
    private int columns;
    @Shadow
    private int rows;
    @Shadow
    private byte[] mapDataBytes;

    @Inject(method = "readPacketData", at = @At("HEAD"), cancellable = true)
    private void portium$read(PacketBuffer buf, CallbackInfo ci) throws java.io.IOException {
        ci.cancel();
        this.mapId = buf.readVarInt();
        this.mapScale = buf.readByte();
        this.trackingPosition = buf.readBoolean();
        this.icons = new MapDecoration[buf.readVarInt()];
        for (int i = 0; i < this.icons.length; i++) {
            MapDecoration.Type type = MapDecoration.Type.byIcon((byte) buf.readVarInt());
            byte x = buf.readByte();
            byte y = buf.readByte();
            byte rotation = (byte) (buf.readByte() & 15);
            MapDecoration icon = new MapDecoration(type, x, y, rotation);
            if (buf.readBoolean()) ((IMapDecorationName) icon).portium$setName(buf.readTextComponent());
            this.icons[i] = icon;
        }
        this.columns = buf.readUnsignedByte();
        if (this.columns > 0) {
            this.rows = buf.readUnsignedByte();
            this.minX = buf.readUnsignedByte();
            this.minZ = buf.readUnsignedByte();
            this.mapDataBytes = buf.readByteArray();
        }
    }

    @Inject(method = "writePacketData", at = @At("HEAD"), cancellable = true)
    private void portium$write(PacketBuffer buf, CallbackInfo ci) {
        ci.cancel();
        buf.writeVarInt(this.mapId);
        buf.writeByte(this.mapScale);
        buf.writeBoolean(this.trackingPosition);
        buf.writeVarInt(this.icons.length);
        for (MapDecoration icon : this.icons) {
            buf.writeVarInt(icon.getImage() & 255);
            buf.writeByte(icon.getX());
            buf.writeByte(icon.getY());
            buf.writeByte(icon.getRotation() & 15);
            ITextComponent name = ((IMapDecorationName) icon).portium$getName();
            buf.writeBoolean(name != null);
            if (name != null) buf.writeTextComponent(name);
        }
        buf.writeByte(this.columns);
        if (this.columns > 0) {
            buf.writeByte(this.rows);
            buf.writeByte(this.minX);
            buf.writeByte(this.minZ);
            buf.writeByteArray(this.mapDataBytes);
        }
    }
}
