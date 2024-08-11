package com.michaelsebero.portium.mods116.host;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * The client's half of opening a menu: the menu made from its type with the server's extra data (Forge's factories
 * take it; vanilla's don't), the screen from the factory the mod registered with {@code ScreenManager}, then both are
 * the player's. Minecraft 1.16.5's menu type and screen manager keep these privately, so they're read by reflection.
 */
final class MenuClientGlue {

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void open(ResourceLocation id, int windowId, String titleJson, PacketBuffer extra) {
        Minecraft minecraft = Minecraft.func_71410_x();
        if (minecraft.field_71439_g == null) return;
        try {
            ContainerType<?> type = (ContainerType<?>) ForgeRegistries.CONTAINERS.getValue(id);
            if (type == null) throw new IllegalStateException("No menu type " + id);
            Field factoryField = ContainerType.class.getDeclaredField("field_221530_x");
            factoryField.setAccessible(true);
            Object factory = factoryField.get(type);
            Container menu = factory instanceof IContainerFactory
                    ? ((IContainerFactory<?>) factory).create(windowId, minecraft.field_71439_g.field_71071_by, extra)
                    : type.func_221506_a(windowId, minecraft.field_71439_g.field_71071_by);
            Method screens = ScreenManager.class.getDeclaredMethod("func_216912_a", ContainerType.class);
            screens.setAccessible(true);
            Object screenFactory = screens.invoke(null, type);
            if (screenFactory == null) throw new IllegalStateException("No screen registered for menu type " + id);
            ITextComponent title = (ITextComponent) com.michaelsebero.portium.mods116.ModernNetwork116.textFromJson(titleJson);
            Method create = Class.forName("net.minecraft.client.gui.ScreenManager$IScreenFactory", true, MenuClientGlue.class.getClassLoader())
                    .getMethod("create", Container.class, net.minecraft.entity.player.PlayerInventory.class, ITextComponent.class);
            Screen screen = (Screen) create.invoke(screenFactory, menu, minecraft.field_71439_g.field_71071_by, title);
            minecraft.field_71439_g.field_71070_bA = menu;
            minecraft.func_147108_a(screen);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LogManager.getLogger("Portium/1.16.5").error("Couldn't open menu {} on the client", id, e);
            com.michaelsebero.portium.mods116.ModernFailures.add("Menu " + id + " couldn't be opened", e);
        }
    }

    private MenuClientGlue() {}
}
