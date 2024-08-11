package net.minecraftforge.fml.network.simple;

/** Portium's replacement for Forge 1.16's: only what its {@code SimpleChannel} hands back. */
public class IndexedMessageCodec {

    /** A registered message's handle. */
    public static class MessageHandler<MSG> {

        final int index;

        MessageHandler(int index) {
            this.index = index;
        }
    }
}
