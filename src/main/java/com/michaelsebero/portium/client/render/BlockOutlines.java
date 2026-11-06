package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.block.IOutlineShape;
import com.michaelsebero.portium.modern.adapter.IModernBlock;
import com.michaelsebero.portium.modern.mc.util.math.shapes.ISelectionContext;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShapes;
import com.michaelsebero.portium.mods116.adapter.Block116;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.14+'s block outlines: the selection is drawn along the block's shape (a lectern's slant, stairs' step, a bell's
 * frame, Create's shafts and cogs) instead of 1.12's one box around it. The shape is a 1.16 or 1.13 mod block's own,
 * a Portium block's {@link IOutlineShape}, or else the block's collision boxes cut to its 1.12 selection box (only when
 * there are several; one box is drawn by 1.12 as before). Inner edges where boxes meet aren't drawn, as in 1.16.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class BlockOutlines {

    /** Lines are drawn this much of the way to the camera, so they sit over the faces they lie on (1.12 grows its box). */
    private static final double TOWARD_CAMERA = 0.998D;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHighlight(DrawBlockHighlightEvent event) {
        RayTraceResult target = event.getTarget();
        if (event.isCanceled() || event.getSubID() != 0 || target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) return;
        EntityPlayer player = event.getPlayer();
        World world = player.world;
        BlockPos pos = target.getBlockPos();
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial() == Material.AIR || !world.getWorldBorder().contains(pos)) return;
        VoxelShape shape;
        try {
            shape = outline(state.getActualState(world, pos), world, pos, player);
        } catch (RuntimeException e) {
            return;
        }
        if (shape == null || shape.isEmpty()) return;
        event.setCanceled(true);
        float partialTicks = event.getPartialTicks();
        double cameraX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double cameraY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double cameraZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        double offsetX = pos.getX() - cameraX;
        double offsetY = pos.getY() - cameraY;
        double offsetZ = pos.getZ() - cameraZ;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        // The camera sits at the eyes; lines are pulled toward the eye point.
        double eye = player.getEyeHeight();
        shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
            vertex(buffer, offsetX + x1, offsetY + y1 - eye, offsetZ + z1, eye);
            vertex(buffer, offsetX + x2, offsetY + y2 - eye, offsetZ + z2, eye);
        });
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void vertex(BufferBuilder buffer, double x, double y, double z, double eye) {
        buffer.pos(x * TOWARD_CAMERA, y * TOWARD_CAMERA + eye, z * TOWARD_CAMERA).color(0.0F, 0.0F, 0.0F, 0.4F).endVertex();
    }

    /** The block's outline in block coordinates, or null to leave 1.12's box. */
    @Nullable
    private static VoxelShape outline(IBlockState state, World world, BlockPos pos, EntityPlayer player) {
        Block block = state.getBlock();
        if (block instanceof Block116) return ((Block116) block).getShape(state, world, pos, ISelectionContext.forEntity(player));
        if (block instanceof IModernBlock) return ((IModernBlock) block).getShape(state, world, pos);
        if (block instanceof IOutlineShape) return ((IOutlineShape) block).getOutlineShape(state, world, pos, player);
        AxisAlignedBB selected = state.getSelectedBoundingBox(world, pos);
        if (selected == null) return null;
        List<AxisAlignedBB> boxes = new ArrayList<>();
        state.addCollisionBoxToList(world, pos, selected.grow(0.01D), boxes, player, false);
        if (boxes.size() < 2) return null;
        AxisAlignedBB local = selected.offset(-pos.getX(), -pos.getY(), -pos.getZ());
        VoxelShape shape = VoxelShapes.empty();
        int parts = 0;
        for (AxisAlignedBB box : boxes) {
            AxisAlignedBB part = box.offset(-pos.getX(), -pos.getY(), -pos.getZ());
            if (!part.intersects(local)) continue;
            part = part.intersect(local);
            if (part.maxX - part.minX < 1.0E-4D || part.maxY - part.minY < 1.0E-4D || part.maxZ - part.minZ < 1.0E-4D) continue;
            shape = VoxelShapes.or(shape, VoxelShapes.create(part));
            parts++;
        }
        return parts < 2 ? null : shape;
    }

    private BlockOutlines() {}
}
