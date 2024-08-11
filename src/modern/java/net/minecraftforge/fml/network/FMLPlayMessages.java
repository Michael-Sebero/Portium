package net.minecraftforge.fml.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

/** Portium's replacement for Forge 1.13's: what a mod's GUI factory is given (see {@code ModernGuis}). */
public class FMLPlayMessages {

    public static class OpenContainer {

        private final ResourceLocation id;
        private final int windowId;
        private final PacketBuffer additionalData;

        public OpenContainer(ResourceLocation id, int windowId, PacketBuffer additionalData) {
            this.id = id;
            this.windowId = windowId;
            this.additionalData = additionalData;
        }

        public ResourceLocation getId() {
            return this.id;
        }

        public int getWindowId() {
            return this.windowId;
        }

        public PacketBuffer getAdditionalData() {
            return this.additionalData;
        }
    }
}
