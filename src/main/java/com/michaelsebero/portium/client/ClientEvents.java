package com.michaelsebero.portium.client;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.api.IRiptideEntity;
import com.michaelsebero.portium.datapack.DataPackRecipes;
import com.michaelsebero.portium.init.PortiumPotions;
import com.michaelsebero.portium.item.ItemTrident;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.event.terraingen.BiomeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;


@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class ClientEvents {

    /** Recipes a server's data packs added don't carry over to the next server. */
    @SubscribeEvent
    public static void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        DataPackRecipes.forgetReceived();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isGamePaused()) return;
        CameraState.tick();
        UnderwaterAmbience.tick(mc);
        NetherAmbience.tick(mc);
    }

    // ---- 1.13 water colours and underwater fog ----

    @SubscribeEvent
    public static void onWaterColor(BiomeEvent.GetWaterColor event) {
        if (PortiumConfig.visuals.newWaterColors) {
            event.setNewColor(WaterColors.waterColor(event.getBiome(), event.getOriginalColor()));
        }
    }

    private static int targetFogColor = -1;
    private static int previousFogColor = -1;
    private static long fogChangeTime = -1L;

    /** The underwater fog takes the biome's colour, blending over five seconds when it changes (1.13). */
    @SubscribeEvent
    public static void onFogColors(EntityViewRenderEvent.FogColors event) {
        if (!PortiumConfig.visuals.newUnderwaterFog || event.getState().getMaterial() != Material.WATER) return;
        Entity entity = event.getEntity();
        Biome biome = entity.world.getBiome(new BlockPos(ActiveRenderInfo.projectViewFromEntity(entity, event.getRenderPartialTicks())));
        int biomeColor = WaterColors.fogColor(biome);
        long now = Minecraft.getSystemTime();
        if (fogChangeTime < 0L) {
            targetFogColor = previousFogColor = biomeColor;
            fogChangeTime = now;
        }
        float t = MathHelper.clamp((now - fogChangeTime) / 5000.0F, 0.0F, 1.0F);
        float red = lerp(t, previousFogColor >> 16 & 255, targetFogColor >> 16 & 255);
        float green = lerp(t, previousFogColor >> 8 & 255, targetFogColor >> 8 & 255);
        float blue = lerp(t, previousFogColor & 255, targetFogColor & 255);
        if (targetFogColor != biomeColor) {
            previousFogColor = MathHelper.floor(red) << 16 | MathHelper.floor(green) << 8 | MathHelper.floor(blue);
            targetFogColor = biomeColor;
            fogChangeTime = now;
        }
        red /= 255.0F;
        green /= 255.0F;
        blue /= 255.0F;
        // Adapting to the water (or Night Vision) brightens the fog towards its full colour.
        float vision = CameraState.getWaterVision();
        if (entity instanceof EntityPlayer && ((EntityPlayer) entity).isPotionActive(MobEffects.NIGHT_VISION)) vision = 1.0F;
        float boost = Math.min(1.0F / Math.max(red, 1.0E-4F), Math.min(1.0F / Math.max(green, 1.0E-4F), 1.0F / Math.max(blue, 1.0E-4F)));
        event.setRed(red * (1.0F - vision) + red * boost * vision);
        event.setGreen(green * (1.0F - vision) + green * boost * vision);
        event.setBlue(blue * (1.0F - vision) + blue * boost * vision);
    }

    /** Underwater fog is thinner, thins further as the player adapts, and is a bit thicker in swamps (1.13). */
    @SubscribeEvent
    public static void onFogDensity(EntityViewRenderEvent.FogDensity event) {
        if (!PortiumConfig.visuals.newUnderwaterFog || event.getState().getMaterial() != Material.WATER) return;
        Entity entity = event.getEntity();
        GlStateManager.setFog(GlStateManager.FogMode.EXP2);
        float density = 0.05F;
        if (entity instanceof EntityPlayerSP) {
            float vision = CameraState.getWaterVision();
            density -= vision * vision * 0.03F;
            if (net.minecraftforge.common.BiomeDictionary.hasType(entity.world.getBiome(new BlockPos(entity)), net.minecraftforge.common.BiomeDictionary.Type.SWAMP)) density += 0.005F;
            if (((EntityPlayerSP) entity).isPotionActive(PortiumPotions.CONDUIT_POWER)) density *= 0.5F;
        }
        event.setDensity(density);
        event.setCanceled(true);
    }

    private static float lerp(float t, float from, float to) {
        return from + t * (to - from);
    }

    // ---- Air bar ----

    /** 1.13 keeps showing the air bar while it refills above water. Under water vanilla already draws it. */
    @SubscribeEvent
    public static void onRenderAir(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.AIR || !PortiumConfig.visuals.airBarWhileRefilling) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.getRenderViewEntity() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) mc.getRenderViewEntity();
        int air = player.getAir();
        if (player.isInsideOfMaterial(Material.WATER) || air >= 300) return;
        mc.getTextureManager().bindTexture(Gui.ICONS);
        GlStateManager.enableBlend();
        int left = event.getResolution().getScaledWidth() / 2 + 91;
        int top = event.getResolution().getScaledHeight() - GuiIngameForge.right_height;
        int full = MathHelper.ceil((air - 2) * 10.0D / 300.0D);
        int partial = MathHelper.ceil(air * 10.0D / 300.0D) - full;
        for (int i = 0; i < full + partial; ++i) {
            mc.ingameGUI.drawTexturedModalRect(left - i * 8 - 9, top, i < full ? 16 : 25, 18, 9, 9);
        }
        GuiIngameForge.right_height += 10;
        GlStateManager.disableBlend();
    }

    // ---- First-person trident ----

    @SubscribeEvent
    public static void onRenderHand(RenderSpecificHandEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ItemTrident)) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        EnumHandSide side = event.getHand() == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        boolean right = side == EnumHandSide.RIGHT;
        int sign = right ? 1 : -1;
        boolean aiming = player.isHandActive() && player.getItemInUseCount() > 0 && player.getActiveHand() == event.getHand();
        boolean spinning = IRiptideEntity.of(player).portium$isSpinAttacking();
        if (!aiming && !spinning) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(sign * 0.56F, -0.52F + event.getEquipProgress() * -0.6F, -0.72F);
        if (aiming) {
            GlStateManager.translate(sign * -0.5F, 0.7F, 0.1F);
            GlStateManager.rotate(-55.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(sign * 35.3F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(sign * -9.785F, 0.0F, 0.0F, 1.0F);
            float useTime = stack.getMaxItemUseDuration() - (player.getItemInUseCount() - event.getPartialTicks() + 1.0F);
            float charge = Math.min(useTime / 10.0F, 1.0F);
            if (charge > 0.1F) {
                float shake = MathHelper.sin((useTime - 0.1F) * 1.3F) * (charge - 0.1F);
                GlStateManager.translate(0.0F, shake * 0.004F, 0.0F);
            }
            GlStateManager.translate(0.0F, 0.0F, charge * 0.2F);
            GlStateManager.scale(1.0F, 1.0F, 1.0F + charge * 0.2F);
            GlStateManager.rotate(sign * 45.0F, 0.0F, -1.0F, 0.0F);
        } else {
            GlStateManager.translate(sign * -0.4F, 0.8F, 0.3F);
            GlStateManager.rotate(sign * 65.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(sign * -85.0F, 0.0F, 0.0F, 1.0F);
        }
        mc.getItemRenderer().renderItemSide(player, stack,
                right ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !right);
        GlStateManager.popMatrix();
        event.setCanceled(true);
    }

    private ClientEvents() {}
}
