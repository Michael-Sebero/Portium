package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.block.building.Walls116;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

/** 1.16's wall heights, posts and pane connections (see Walls116). */
@Mixin(BlockWall.class)
public abstract class MixinBlockWall116 extends Block {

    private MixinBlockWall116(Material material) {
        super(material);
    }

    /** The four heights join 1.12's properties; they're worked out like the connections, never saved. */
    @ModifyArg(method = "createBlockState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/state/BlockStateContainer;<init>(Lnet/minecraft/block/Block;[Lnet/minecraft/block/properties/IProperty;)V"), index = 1)
    private IProperty<?>[] portium$heights(IProperty<?>[] properties) {
        IProperty<?>[] withHeights = Arrays.copyOf(properties, properties.length + 4);
        withHeights[properties.length] = Walls116.NORTH_TALL;
        withHeights[properties.length + 1] = Walls116.EAST_TALL;
        withHeights[properties.length + 2] = Walls116.SOUTH_TALL;
        withHeights[properties.length + 3] = Walls116.WEST_TALL;
        return withHeights;
    }

    /** A new property's first value is true; walls start low, as 1.12's start unconnected. */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void portium$lowByDefault(Block modelBlock, CallbackInfo ci) {
        IBlockState state = this.getDefaultState();
        if (!state.getPropertyKeys().contains(Walls116.NORTH_TALL)) return;
        this.setDefaultState(state.withProperty(Walls116.NORTH_TALL, false).withProperty(Walls116.EAST_TALL, false)
                .withProperty(Walls116.SOUTH_TALL, false).withProperty(Walls116.WEST_TALL, false));
    }

    @Inject(method = "getActualState", at = @At("RETURN"), cancellable = true)
    private void portium$heightsAndPost(IBlockState state, IBlockAccess world, BlockPos pos, CallbackInfoReturnable<IBlockState> cir) {
        cir.setReturnValue(Walls116.apply(cir.getReturnValue(), world, pos));
    }

    /** A wall reaching the top has a full height outline, as 1.16's. */
    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void portium$tallOutline(IBlockState state, IBlockAccess world, BlockPos pos, CallbackInfoReturnable<AxisAlignedBB> cir) {
        AxisAlignedBB box = cir.getReturnValue();
        if (box.maxY < 1.0 && Walls116.anyTall(this.getActualState(state, world, pos))) {
            cir.setReturnValue(new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX, 1.0, box.maxZ));
        }
    }

    /** Walls join glass panes and iron bars in 1.16. */
    @Inject(method = "canConnectTo", at = @At("RETURN"), cancellable = true)
    private void portium$toPanes(IBlockAccess world, BlockPos pos, EnumFacing facing, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && com.michaelsebero.portium.PortiumConfig.mechanics.walls116 && world.getBlockState(pos).getBlock() instanceof BlockPane) {
            cir.setReturnValue(true);
        }
    }
}
