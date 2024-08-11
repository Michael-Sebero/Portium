package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.client.RenderBridge116;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

/**
 * 1.16's camera ({@code ActiveRenderInfo}, which 1.12 has as a static helper of another shape): where the view is,
 * which way it faces, and its rotation, read from 1.12's camera once a frame (RenderBridge116). 1.16 code gets the
 * game's camera from its renderers and events; ones it makes itself stay where they're put.
 */
public class ActiveRenderInfo116 {

    private static final ActiveRenderInfo116 GAME = new ActiveRenderInfo116();

    private final boolean game;
    private long frame = Long.MIN_VALUE;
    private Vector3d pos = Vector3d.field_186680_a;
    private BlockPos blockPos = BlockPos.field_177992_a;
    private float yaw;
    private float pitch;
    private boolean thirdPerson;
    private Entity entity;
    private final Quaternion rotation = new Quaternion(0.0F, 0.0F, 0.0F, 1.0F);
    private final Vector3f look = new Vector3f(0.0F, 0.0F, 1.0F);
    private final Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);

    public ActiveRenderInfo116() {
        this(false);
    }

    private ActiveRenderInfo116(boolean game) {
        this.game = game;
    }

    /** The game's camera, as of this frame. */
    public static ActiveRenderInfo116 game() {
        GAME.refresh();
        return GAME;
    }

    /** {@code gameRenderer.getActiveRenderInfo()}. */
    public static ActiveRenderInfo116 of(net.minecraft.client.renderer.GameRenderer renderer) {
        return game();
    }

    /** {@code tileEntityRendererDispatcher.renderInfo}. */
    public static ActiveRenderInfo116 of(net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher dispatcher) {
        return game();
    }

    /** {@code entityRendererManager.info}. */
    public static ActiveRenderInfo116 of(net.minecraft.client.renderer.entity.EntityRendererManager manager) {
        return game();
    }

    /** Forge's {@code EntityViewRenderEvent.getInfo()}. */
    public static ActiveRenderInfo116 of(net.minecraftforge.client.event.EntityViewRenderEvent event) {
        return game();
    }

    private void refresh() {
        long now = RenderBridge116.frame();
        if (now == this.frame) return;
        this.frame = now;
        double[] camera = RenderBridge116.camera();
        this.pos = new Vector3d(camera[0], camera[1], camera[2]);
        this.blockPos = new BlockPos(this.pos);
        this.thirdPerson = camera[5] != 0.0D;
        this.entity = Minecraft.func_71410_x().func_175606_aa();
        direction((float) camera[3], (float) camera[4]);
    }

    private void direction(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.rotation.func_227066_a_(0.0F, 0.0F, 0.0F, 1.0F);
        this.rotation.func_195890_a(new Quaternion(new Vector3f(0.0F, 1.0F, 0.0F), -yaw, true));
        this.rotation.func_195890_a(new Quaternion(new Vector3f(1.0F, 0.0F, 0.0F), pitch, true));
        this.look.func_195905_a(0.0F, 0.0F, 1.0F);
        this.look.func_214905_a(this.rotation);
        this.up.func_195905_a(0.0F, 1.0F, 0.0F);
        this.up.func_214905_a(this.rotation);
    }

    /** getProjectedView */
    public Vector3d func_216785_c() {
        return this.pos;
    }

    /** getBlockPos */
    public BlockPos func_216780_d() {
        return this.blockPos;
    }

    /** getPitch */
    public float func_216777_e() {
        return this.pitch;
    }

    /** getYaw */
    public float func_216778_f() {
        return this.yaw;
    }

    /** getRotation */
    public Quaternion func_227995_f_() {
        return this.rotation;
    }

    /** getRenderViewEntity */
    public Entity func_216773_g() {
        return this.entity;
    }

    /** isValid */
    public boolean func_216786_h() {
        return this.game ? this.entity != null : this.entity != null;
    }

    /** isThirdPerson */
    public boolean func_216770_i() {
        return this.thirdPerson;
    }

    /** getFluidState: the fluid the view is in. */
    public FluidState func_216771_k() {
        return (FluidState) RenderBridge116.fluidAt(this.blockPos);
    }

    /** getViewVector */
    public final Vector3f func_227996_l_() {
        return this.look;
    }

    /** getUpVector */
    public final Vector3f func_227997_m_() {
        return this.up;
    }

    /** update(world, entity, thirdPerson, reversed, partialTicks): for cameras 1.16 code makes; follows the entity's eyes. */
    public void func_216772_a(net.minecraft.world.IBlockReader world, Entity entity, boolean thirdPerson, boolean reversed, float partialTicks) {
        this.entity = entity;
        this.thirdPerson = thirdPerson;
        if (entity != null) {
            double[] eyes = RenderBridge116.eyes(entity, partialTicks);
            this.pos = new Vector3d(eyes[0], eyes[1], eyes[2]);
            this.blockPos = new BlockPos(this.pos);
            direction((float) eyes[3], (float) eyes[4]);
        }
    }
}
