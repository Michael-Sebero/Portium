package com.michaelsebero.portium.network;

import com.michaelsebero.portium.tile.TileEntityJigsaw;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawGenerate;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 1.16's {@code CUpdateJigsawBlockPacket} and {@code CJigsawBlockGeneratePacket} in one: the jigsaw screen's fields,
 * and (with {@code generate}) its Generate button's levels and "keep jigsaws". Operators in creative only, as in 1.16.
 */
public class JigsawUpdateMessage implements IMessage {

    private BlockPos pos;
    private String name;
    private String target;
    private String pool;
    private String finalState;
    private boolean rollable;
    private boolean generate;
    private int levels;
    private boolean keepJigsaws;

    public JigsawUpdateMessage() {}

    public JigsawUpdateMessage(BlockPos pos, String name, String target, String pool, String finalState, boolean rollable, boolean generate, int levels,
                               boolean keepJigsaws) {
        this.pos = pos;
        this.name = name;
        this.target = target;
        this.pool = pool;
        this.finalState = finalState;
        this.rollable = rollable;
        this.generate = generate;
        this.levels = levels;
        this.keepJigsaws = keepJigsaws;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        this.name = ByteBufUtils.readUTF8String(buf);
        this.target = ByteBufUtils.readUTF8String(buf);
        this.pool = ByteBufUtils.readUTF8String(buf);
        this.finalState = ByteBufUtils.readUTF8String(buf);
        this.rollable = buf.readBoolean();
        this.generate = buf.readBoolean();
        this.levels = buf.readByte();
        this.keepJigsaws = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.pos.toLong());
        ByteBufUtils.writeUTF8String(buf, this.name);
        ByteBufUtils.writeUTF8String(buf, this.target);
        ByteBufUtils.writeUTF8String(buf, this.pool);
        ByteBufUtils.writeUTF8String(buf, this.finalState);
        buf.writeBoolean(this.rollable);
        buf.writeBoolean(this.generate);
        buf.writeByte(this.levels);
        buf.writeBoolean(this.keepJigsaws);
    }

    public static class Handler implements IMessageHandler<JigsawUpdateMessage, IMessage> {
        @Override
        public IMessage onMessage(JigsawUpdateMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!player.canUseCommandBlock() || player.getDistanceSq(message.pos) > 64.0D * 64.0D) return;
                TileEntity tile = player.world.getTileEntity(message.pos);
                if (!(tile instanceof TileEntityJigsaw)) return;
                ((TileEntityJigsaw) tile).set(clip(message.name, 128), clip(message.target, 128), clip(message.pool, 128), clip(message.finalState, 256),
                        message.rollable);
                if (message.generate) JigsawGenerate.generate(player.getServerWorld(), message.pos, Math.min(7, message.levels), message.keepJigsaws);
            });
            return null;
        }

        private static String clip(String text, int length) {
            return text.length() > length ? text.substring(0, length) : text;
        }
    }
}
