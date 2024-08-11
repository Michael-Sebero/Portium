package com.michaelsebero.portium.handler;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.block.water.IWaterloggable;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.network.PortiumParticles;
import com.michaelsebero.portium.proxy.ParticleKind;
import com.michaelsebero.portium.util.AquaticMobs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class CommonEvents {

    // ---- Buckets and waterloggable blocks ----

    /**
     * An empty bucket takes the water out of a waterlogged coral, sea pickle or conduit (1.13); seagrass and kelp
     * can't give up their water. A water bucket waterlogs one of those blocks when it's dry.
     */
    @SubscribeEvent
    public static void onFillBucket(FillBucketEvent event) {
        RayTraceResult target = event.getTarget();
        if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) return;
        World world = event.getWorld();
        BlockPos pos = target.getBlockPos();
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof IWaterloggable)) return;
        IWaterloggable block = (IWaterloggable) state.getBlock();
        ItemStack held = event.getEmptyBucket();

        if (held.getItem() == Items.BUCKET && block.isWaterlogged(state)) {
            IBlockState dry = block.withWaterlogged(state, false);
            if (dry == null) {
                event.setCanceled(true);
                return;
            }
            world.setBlockState(pos, dry, 11);
            event.getEntityPlayer().playSound(SoundEvents.ITEM_BUCKET_FILL, 1.0F, 1.0F);
            event.setFilledBucket(new ItemStack(Items.WATER_BUCKET));
            event.setResult(Event.Result.ALLOW);
        } else if (held.getItem() == Items.WATER_BUCKET && !block.isWaterlogged(state) && !world.provider.doesWaterVaporize()) {
            IBlockState wet = block.withWaterlogged(state, true);
            if (wet == null) return;
            world.setBlockState(pos, wet, 11);
            event.getEntityPlayer().playSound(SoundEvents.ITEM_BUCKET_EMPTY, 1.0F, 1.0F);
            event.setFilledBucket(new ItemStack(Items.BUCKET));
            event.setResult(Event.Result.ALLOW);
        }
    }

    // ---- Axe stripping ----

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!PortiumConfig.mechanics.axeStripping) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.getItem().getToolClasses(stack).contains("axe")) return;
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState stripped = Stripping.strip(world.getBlockState(pos));
        if (stripped == null) return;
        EntityPlayer player = event.getEntityPlayer();
        world.playSound(player, pos, PortiumSounds.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (!world.isRemote) {
            world.setBlockState(pos, stripped, 11);
            stack.damageItem(1, player);
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    // ---- Combat ----

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase target = event.getEntityLiving();
        DamageSource source = event.getSource();
        Entity attacker = source.getTrueSource();
        // Impaling on melee hits; thrown tridents add it themselves.
        if (attacker instanceof EntityLivingBase && source.getImmediateSource() == attacker && !source.isProjectile()) {
            float bonus = AquaticMobs.impalingBonus(((EntityLivingBase) attacker).getHeldItemMainhand(), target);
            if (bonus > 0.0F) event.setAmount(event.getAmount() + bonus);
        }
        // Squids squirt ink when something attacks them (1.13).
        if (target instanceof EntitySquid && PortiumConfig.mechanics.squidInk && target.world instanceof WorldServer
                && (attacker instanceof EntityLivingBase || target.getRevengeTarget() != null)) {
            squirtInk((EntitySquid) target);
        }
    }

    /** Thirty ink puffs shot out of the squid's underside, aimed by its body's pitch and yaw. */
    private static void squirtInk(EntitySquid squid) {
        Random random = squid.getRNG();
        squid.playSound(PortiumSounds.ENTITY_SQUID_SQUIRT, 0.4F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
        Vec3d mouth = squidVector(squid, new Vec3d(0.0D, -1.0D, 0.0D)).add(squid.posX, squid.posY, squid.posZ);
        for (int i = 0; i < 30; i++) {
            Vec3d direction = squidVector(squid, new Vec3d(random.nextFloat() * 0.6D - 0.3D, -1.0D, random.nextFloat() * 0.6D - 0.3D));
            Vec3d motion = direction.scale(0.3D + random.nextFloat() * 2.0F);
            PortiumParticles.spawn((WorldServer) squid.world, ParticleKind.SQUID_INK, mouth.x, mouth.y + 0.5D, mouth.z, motion.x, motion.y, motion.z, 0.1D, 0, false);
        }
    }

    private static Vec3d squidVector(EntitySquid squid, Vec3d vector) {
        return vector.rotatePitch(squid.prevSquidPitch * ((float) Math.PI / 180.0F)).rotateYaw(-squid.prevRenderYawOffset * ((float) Math.PI / 180.0F));
    }

    private CommonEvents() {}
}
