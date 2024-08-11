package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.ITemplateProcessor;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Structure templates (structure blocks, data packs, generated structures) keep which blocks are waterlogged, in a
 * tag 1.12 ignores. As in 1.13, a block that can hold water also keeps the water source it's placed into.
 */
@Mixin(Template.class)
public abstract class MixinTemplate {

    @Unique
    private static final String PORTIUM_TAG = "PortiumWaterlogged";

    @Unique
    private final Set<BlockPos> portium$waterlogged = new HashSet<>();

    @Inject(method = "takeBlocksFromWorld", at = @At("TAIL"))
    private void portium$recordWater(World world, BlockPos start, BlockPos size, boolean takeEntities, @Nullable Block toIgnore, CallbackInfo ci) {
        this.portium$waterlogged.clear();
        if (size.getX() < 1 || size.getY() < 1 || size.getZ() < 1) return;
        for (BlockPos.MutableBlockPos pos : BlockPos.getAllInBoxMutable(start, start.add(size).add(-1, -1, -1))) {
            if (Waterlogging.isWaterlogged(world, pos)) this.portium$waterlogged.add(pos.subtract(start));
        }
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void portium$writeWater(NBTTagCompound nbt, CallbackInfoReturnable<NBTTagCompound> cir) {
        if (this.portium$waterlogged.isEmpty()) return;
        NBTTagList list = new NBTTagList();
        for (BlockPos pos : this.portium$waterlogged) list.appendTag(new NBTTagIntArray(new int[] {pos.getX(), pos.getY(), pos.getZ()}));
        nbt.setTag(PORTIUM_TAG, list);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void portium$readWater(NBTTagCompound nbt, CallbackInfo ci) {
        this.portium$waterlogged.clear();
        NBTTagList list = nbt.getTagList(PORTIUM_TAG, 11);
        for (int i = 0; i < list.tagCount(); i++) {
            int[] pos = list.getIntArrayAt(i);
            if (pos.length == 3) this.portium$waterlogged.add(new BlockPos(pos[0], pos[1], pos[2]));
        }
    }

    @Redirect(method = "addBlocksToWorld(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/structure/template/ITemplateProcessor;Lnet/minecraft/world/gen/structure/template/PlacementSettings;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z", ordinal = 1))
    private boolean portium$keepWater(World world, BlockPos pos, IBlockState state, int flags) {
        boolean intoWater = WaterUtil.isWaterSource(world.getBlockState(pos));
        boolean placed = world.setBlockState(pos, state, flags);
        if (intoWater) Waterlogging.setWaterloggedQuietly(world, pos);
        return placed;
    }

    @Inject(method = "addBlocksToWorld(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/structure/template/ITemplateProcessor;Lnet/minecraft/world/gen/structure/template/PlacementSettings;I)V",
            at = @At("TAIL"))
    private void portium$placeWater(World world, BlockPos origin, @Nullable ITemplateProcessor processor, PlacementSettings settings, int flags, CallbackInfo ci) {
        StructureBoundingBox box = settings.getBoundingBox();
        for (BlockPos local : this.portium$waterlogged) {
            BlockPos target = Template.transformedBlockPos(settings, local).add(origin);
            if (box == null || box.isVecInside(target)) Waterlogging.setWaterloggedQuietly(world, target);
        }
    }
}
