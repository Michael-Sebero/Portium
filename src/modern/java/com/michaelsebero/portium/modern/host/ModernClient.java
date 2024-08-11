package com.michaelsebero.portium.modern.host;

import com.google.common.util.concurrent.ListenableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.network.FMLPlayMessages;

/** What 1.13 mods' networking does on the client: work on the client's thread, and opening mods' GUIs. */
public final class ModernClient {

    public static ListenableFuture<Object> schedule(Runnable work) {
        return Minecraft.func_71410_x().func_152344_a(work);
    }

    /** A container the server opened: the mod's GUI factory makes its screen. */
    static void openContainer(ResourceLocation id, int windowId, PacketBuffer extra) {
        schedule(() -> {
            FMLPlayMessages.OpenContainer message = new FMLPlayMessages.OpenContainer(id, windowId, extra);
            ModList.get().getModContainerById(id.func_110624_b()).flatMap(container -> container.getCustomExtension(ExtensionPoint.GUIFACTORY))
                    .map(factory -> factory.apply(message)).ifPresent(gui -> {
                        Minecraft mc = Minecraft.func_71410_x();
                        mc.func_147108_a(gui);
                        if (gui instanceof GuiContainer) mc.field_71439_g.field_71070_bA.field_75152_c = windowId;
                    });
        });
    }

    private ModernClient() {}
}
