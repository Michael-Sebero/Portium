package com.michaelsebero.portium.modern.shim.client;

import com.michaelsebero.portium.modern.flat.Flattening;
import com.michaelsebero.portium.modern.mc.client.KeyboardListener;
import com.michaelsebero.portium.modern.mc.client.MainWindow;
import com.michaelsebero.portium.modern.mc.client.MouseHelper;
import com.michaelsebero.portium.modern.mc.client.renderer.tileentity.TileEntityRenderer;
import com.michaelsebero.portium.modern.mc.client.util.InputMappings;
import com.michaelsebero.portium.modern.mc.util.IItemProvider;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.client.registry.ClientRegistry;

/** 1.13 client calls that take 1.13's classes where 1.12's take their 1.12 counterparts. */
public final class ClientShims {

    /** 1.13's tile entity renderers are 1.12's special renderers (see {@link TileEntityRenderer}). */
    public static <T extends TileEntity> void bindTileEntitySpecialRenderer(Class<T> type, TileEntityRenderer<? super T> renderer) {
        ClientRegistry.bindTileEntitySpecialRenderer(type, renderer);
    }

    /** {@code Minecraft.mainWindow}. */
    public static MainWindow mainWindow(Minecraft mc) {
        return MainWindow.get();
    }

    /** {@code Minecraft.mouseHelper}, 1.13's. */
    public static MouseHelper mouseHelper(Minecraft mc) {
        return MouseHelper.get();
    }

    /** 1.13's {@code Minecraft.getResourceManager()}: 1.12's, seen as 1.13's (a reloadable one, as 1.13's is). */
    public static com.michaelsebero.portium.modern.mc.resources.IResourceManager getResourceManager(Minecraft mc) {
        return com.michaelsebero.portium.modern.mc.resources.ModernResources.get();
    }

    /** 1.13's quads from a baked model, which take a {@code Random}; 1.12's take a seed. */
    public static java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(net.minecraft.client.renderer.block.model.IBakedModel model,
            @javax.annotation.Nullable net.minecraft.block.state.IBlockState state, @javax.annotation.Nullable net.minecraft.util.EnumFacing side,
            @javax.annotation.Nullable java.util.Random random) {
        return model.getQuads(state, side, random == null ? 0L : random.nextLong());
    }

    /** 1.13's sprite names are resource locations; 1.12's are strings of the same form. */
    public static net.minecraft.util.ResourceLocation getSpriteName(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        return new net.minecraft.util.ResourceLocation(sprite.getIconName());
    }

    /** 1.13's {@code TextureMap.getSprite}: by location, the missing sprite for none. */
    public static net.minecraft.client.renderer.texture.TextureAtlasSprite getSprite(net.minecraft.client.renderer.texture.TextureMap map,
            @javax.annotation.Nullable net.minecraft.util.ResourceLocation location) {
        return location == null ? map.getMissingSprite() : map.getAtlasSprite(location.toString());
    }

    /**
     * 1.13's {@code GameRenderer.updateCameraAndRender}, which may leave the world out and draw only the screen (a
     * full-screen map does). 1.12's draws nothing at all while {@code skipRenderWorld} is set, so without the world
     * this sets up the 2D view and draws the screen as 1.12 does with no world loaded.
     */
    public static void updateCameraAndRender(net.minecraft.client.renderer.EntityRenderer renderer, float partialTicks, long nanoTime, boolean renderWorld) {
        Minecraft mc = Minecraft.getMinecraft();
        if (renderWorld) {
            boolean skipped = mc.skipRenderWorld;
            mc.skipRenderWorld = false;
            try {
                renderer.updateCameraAndRender(partialTicks, nanoTime);
            } finally {
                mc.skipRenderWorld = skipped;
            }
            return;
        }
        net.minecraft.client.gui.ScaledResolution scaled = new net.minecraft.client.gui.ScaledResolution(mc);
        int width = scaled.getScaledWidth();
        int height = scaled.getScaledHeight();
        int mouseX = org.lwjgl.input.Mouse.getX() * width / mc.displayWidth;
        int mouseY = height - org.lwjgl.input.Mouse.getY() * height / mc.displayHeight - 1;
        net.minecraft.client.renderer.GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
        net.minecraft.client.renderer.GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION);
        net.minecraft.client.renderer.GlStateManager.loadIdentity();
        net.minecraft.client.renderer.GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
        net.minecraft.client.renderer.GlStateManager.loadIdentity();
        renderer.setupOverlayRendering();
        if (mc.currentScreen != null) {
            net.minecraft.client.renderer.GlStateManager.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);
            net.minecraftforge.client.ForgeHooksClient.drawScreen(mc.currentScreen, mouseX, mouseY, partialTicks);
        }
    }

    /** {@code Minecraft.keyboardListener}. */
    public static KeyboardListener keyboardListener(Minecraft mc) {
        return KeyboardListener.get();
    }

    // Key bindings: 1.13 asks for GLFW inputs; 1.12 holds LWJGL 2 codes.

    public static InputMappings.Input getKey(KeyBinding binding) {
        return InputMappings.fromLegacy(binding.getKeyCode());
    }

    public static InputMappings.Input getDefault(KeyBinding binding) {
        return InputMappings.fromLegacy(binding.getKeyCodeDefault());
    }

    public static void bind(KeyBinding binding, InputMappings.Input input) {
        binding.setKeyCode(InputMappings.toLegacy(input));
    }

    public static void setKeyBindState(InputMappings.Input input, boolean held) {
        KeyBinding.setKeyBindState(InputMappings.toLegacy(input), held);
    }

    public static void onTick(InputMappings.Input input) {
        KeyBinding.onTick(InputMappings.toLegacy(input));
    }

    public static boolean conflicts(KeyBinding binding, KeyBinding other) {
        return binding.getKeyCode() == other.getKeyCode();
    }

    public static boolean isInvalid(KeyBinding binding) {
        return binding.getKeyCode() == 0;
    }

    public static boolean matchesKey(KeyBinding binding, int key, int scanCode) {
        int code = key == -1 ? scanCode : InputMappings.toLegacy(InputMappings.Type.KEYSYM.getOrMakeInput(key));
        return code != 0 && binding.getKeyCode() == code;
    }

    public static boolean matchesMouseKey(KeyBinding binding, int button) {
        return binding.getKeyCode() == button - 100;
    }

    public static String getLocalizedName(KeyBinding binding) {
        return GameSettings.getKeyDisplayString(binding.getKeyCode());
    }

    public static boolean isDefault(KeyBinding binding) {
        return binding.getKeyCode() == binding.getKeyCodeDefault();
    }

    public static String getTranslationKey(KeyBinding binding) {
        return getKey(binding).getTranslationKey();
    }

    /**
     * 1.13's {@code ItemColors.register}, for blocks and items alike: Forge 1.12's handlers, by block or by item (a
     * flattened 1.13 item is its 1.12 item, all its variants).
     */
    public static void registerItemColor(ItemColors colors, IItemColor color, IItemProvider... providers) {
        for (IItemProvider provider : providers) {
            if (provider instanceof Block) {
                colors.registerItemColorHandler(color, (Block) provider);
            } else {
                Item item = Flattening.stackItem(provider);
                if (item != null) colors.registerItemColorHandler(color, item);
            }
        }
    }

    private ClientShims() {}
}
