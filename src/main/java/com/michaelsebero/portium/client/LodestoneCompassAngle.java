package com.michaelsebero.portium.client;

import com.michaelsebero.portium.item.LodestoneCompass;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Items;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * The compass needle (the {@code angle} model property): 1.12's own for plain compasses, 1.16's for lodestone ones,
 * which point at their lodestone in its dimension and spin elsewhere.
 */
public final class LodestoneCompassAngle implements IItemPropertyGetter {

    private static final ResourceLocation ANGLE = new ResourceLocation("angle");

    private final IItemPropertyGetter vanilla;
    /** 1.16's wobble for the holder's turning, and for a needle with nothing to point at. */
    private final Wobble held = new Wobble();
    private final Wobble spinning = new Wobble();

    private LodestoneCompassAngle(IItemPropertyGetter vanilla) {
        this.vanilla = vanilla;
    }

    public static void install() {
        IItemPropertyGetter vanilla = Items.COMPASS.getPropertyGetter(ANGLE);
        if (vanilla != null && !(vanilla instanceof LodestoneCompassAngle)) Items.COMPASS.addPropertyOverride(ANGLE, new LodestoneCompassAngle(vanilla));
    }

    @Override
    public float apply(ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
        if (!LodestoneCompass.hasLodestone(stack)) return this.vanilla.apply(stack, world, entity);
        Entity holder = entity != null ? entity : stack.getItemFrame();
        if (holder == null) return 0.0F;
        if (world == null) world = holder.world;
        BlockPos target = LodestoneCompass.target(world, stack.getTagCompound());
        long time = world.getTotalWorldTime();
        if (target != null && holder.getDistanceSq(target.getX() + 0.5D, holder.posY, target.getZ() + 0.5D) >= 1.0E-5D) {
            boolean user = entity != null && entity == Minecraft.getMinecraft().player;
            double yaw = 0.0D;
            if (user) {
                yaw = entity.rotationYaw;
            } else if (holder instanceof EntityItemFrame) {
                EntityItemFrame frame = (EntityItemFrame) holder;
                yaw = MathHelper.wrapDegrees(180 + frame.facingDirection.getHorizontalIndex() * 90 + frame.getRotation() * 45);
            } else if (entity != null) {
                yaw = entity.renderYawOffset;
            }
            yaw = MathHelper.positiveModulo(yaw / 360.0D, 1.0D);
            double towards = Math.atan2((double) target.getZ() + 0.5D - holder.posZ, (double) target.getX() + 0.5D - holder.posX) / (Math.PI * 2.0D);
            double angle;
            if (user) {
                if (this.held.shouldUpdate(time)) this.held.update(time, 0.5D - (yaw - 0.25D));
                angle = towards + this.held.rotation;
            } else {
                angle = 0.5D - (yaw - 0.25D - towards);
            }
            return MathHelper.positiveModulo((float) angle, 1.0F);
        }
        if (this.spinning.shouldUpdate(time)) this.spinning.update(time, Math.random());
        double angle = this.spinning.rotation + (double) ((float) stack.hashCode() / 2.14748365E9F);
        return MathHelper.positiveModulo((float) angle, 1.0F);
    }

    /** 1.16's {@code ItemModelsProperties.Angle}: eases towards the wanted angle once a tick. */
    private static final class Wobble {

        double rotation;
        private double speed;
        private long lastTick;

        boolean shouldUpdate(long time) {
            return this.lastTick != time;
        }

        void update(long time, double wanted) {
            this.lastTick = time;
            double change = MathHelper.positiveModulo(wanted - this.rotation + 0.5D, 1.0D) - 0.5D;
            this.speed += change * 0.1D;
            this.speed *= 0.8D;
            this.rotation = MathHelper.positiveModulo(this.rotation + this.speed, 1.0D);
        }
    }
}
