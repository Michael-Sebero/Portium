package com.michaelsebero.portium.client;

import com.michaelsebero.portium.entity.passive.bee.EntityBee;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.15's bee buzz: a looping sound that follows each bee, louder and higher the faster it flies, switching to the
 * aggressive loop while it's angry (and back).
 */
@Mod.EventBusSubscriber(modid = com.michaelsebero.portium.Tags.MOD_ID, value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public final class BeeSounds {

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote || !(event.getEntity() instanceof EntityBee)) return;
        EntityBee bee = (EntityBee) event.getEntity();
        Minecraft.getMinecraft().getSoundHandler().playSound(new Loop(bee, bee.isAngry()));
    }

    static final class Loop extends MovingSound {
        private final EntityBee bee;
        private final boolean angry;
        private boolean switched;

        Loop(EntityBee bee, boolean angry) {
            super(angry ? PortiumSounds.ENTITY_BEE_LOOP_AGGRESSIVE : PortiumSounds.ENTITY_BEE_LOOP, SoundCategory.NEUTRAL);
            this.bee = bee;
            this.angry = angry;
            this.xPosF = (float) bee.posX;
            this.yPosF = (float) bee.posY;
            this.zPosF = (float) bee.posZ;
            this.repeat = true;
            this.repeatDelay = 0;
            this.volume = 0.0F;
        }

        @Override
        public void update() {
            boolean shouldSwitch = this.bee.isAngry() != this.angry;
            if (shouldSwitch && !this.donePlaying && !this.switched) {
                Minecraft.getMinecraft().getSoundHandler().playDelayedSound(new Loop(this.bee, !this.angry), 1);
                this.switched = true;
            }
            if (this.bee.isDead || this.switched) {
                this.donePlaying = true;
                return;
            }
            this.xPosF = (float) this.bee.posX;
            this.yPosF = (float) this.bee.posY;
            this.zPosF = (float) this.bee.posZ;
            float speed = MathHelper.sqrt(this.bee.motionX * this.bee.motionX + this.bee.motionZ * this.bee.motionZ);
            if (speed >= 0.01F) {
                float min = this.bee.isChild() ? 1.1F : 0.7F;
                float max = this.bee.isChild() ? 1.5F : 1.1F;
                this.pitch = min + MathHelper.clamp(speed, min, max) * (max - min);
                this.volume = MathHelper.clamp(speed, 0.0F, 0.5F) * 1.2F;
            } else {
                this.pitch = 0.0F;
                this.volume = 0.0F;
            }
            if (this.bee.isSilent()) this.volume = 0.0F;
        }

        @Override
        public boolean canRepeat() {
            return true;
        }
    }

    private BeeSounds() {}
}
