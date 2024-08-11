package com.michaelsebero.portium.client;

import com.michaelsebero.portium.PortiumConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Client-side camera smoothing and underwater adaptation, updated once per client tick.
 * <ul>
 *     <li>The camera height eases towards the eye height, so sneaking, swimming and crawling don't snap (1.14).</li>
 *     <li>Underwater vision improves over 30 seconds after diving (1.13's water brightness).</li>
 * </ul>
 */
public final class CameraState {

    private static Entity tracked;
    private static float eyeHeight;
    private static float prevEyeHeight;
    private static int ticksUnderwater;

    public static void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.getRenderViewEntity();
        if (viewer == null) {
            tracked = null;
            ticksUnderwater = 0;
            return;
        }
        float target = viewer.getEyeHeight();
        if (viewer != tracked) {
            tracked = viewer;
            eyeHeight = prevEyeHeight = target;
        } else {
            prevEyeHeight = eyeHeight;
            eyeHeight += (target - eyeHeight) * 0.5F;
        }
        if (viewer.isInsideOfMaterial(net.minecraft.block.material.Material.WATER)) {
            ticksUnderwater = Math.min(ticksUnderwater + 1, 600);
        } else if (ticksUnderwater > 0) {
            ticksUnderwater = Math.max(0, ticksUnderwater - 10);
        }
    }

    /** The eye height to place the camera at for this frame. */
    public static float getCameraEyeHeight(Entity entity, float partialTicks) {
        if (!PortiumConfig.visuals.smoothEyeHeight || entity != tracked || !(entity instanceof EntityPlayer)) return entity.getEyeHeight();
        return prevEyeHeight + (eyeHeight - prevEyeHeight) * partialTicks;
    }

    /** 0 just after diving, rising to 1 after 30 seconds under water (1.13's water brightness). */
    public static float getWaterVision() {
        if (tracked == null || !tracked.isInsideOfMaterial(net.minecraft.block.material.Material.WATER)) return 0.0F;
        if (ticksUnderwater >= 600) return 1.0F;
        float early = Math.min(1.0F, ticksUnderwater / 100.0F);
        float late = ticksUnderwater < 100 ? 0.0F : Math.min(1.0F, (ticksUnderwater - 100) / 500.0F);
        return early * 0.6F + late * 0.4F;
    }

    private CameraState() {}
}
