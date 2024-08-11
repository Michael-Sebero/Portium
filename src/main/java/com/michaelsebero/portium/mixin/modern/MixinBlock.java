package com.michaelsebero.portium.mixin.modern;

import com.michaelsebero.portium.modern.adapter.IBlockPropertyHolder;
import com.michaelsebero.portium.modern.mc.block.Block$Properties;
import com.michaelsebero.portium.modern.mc.util.IItemProvider;
import com.michaelsebero.portium.modern.shim.BlockShims;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.13's block API on 1.12's blocks: blocks are item providers (their item, or air), and a block made by a 1.13 mod
 * takes the properties it was made with.
 */
@Mixin(Block.class)
public abstract class MixinBlock implements IItemProvider, IBlockPropertyHolder {

    @Shadow
    protected int lightValue;
    @Shadow
    protected float blockHardness;
    @Shadow
    protected float blockResistance;
    @Shadow
    protected boolean needsRandomTick;
    @Shadow
    protected SoundType blockSoundType;
    @Shadow
    public float slipperiness;
    @Shadow
    protected boolean translucent;

    @Inject(method = "<init>(Lnet/minecraft/block/material/Material;Lnet/minecraft/block/material/MapColor;)V", at = @At("TAIL"))
    private void portium$applyModernProperties(Material material, MapColor mapColor, CallbackInfo ci) {
        Block$Properties properties = BlockShims.takePending();
        if (properties != null) portium$applyBlockProperties(properties);
    }

    @Override
    public void portium$applyBlockProperties(Block$Properties properties) {
        this.blockSoundType = properties.getSoundType();
        this.lightValue = properties.getLightValue();
        this.blockHardness = properties.getHardness();
        // 1.12 stores five times the resistance 1.13 does; both mean the same explosion resistance.
        this.blockResistance = properties.getResistance() * 5.0F;
        this.needsRandomTick = properties.getNeedsRandomTick();
        this.slipperiness = properties.getSlipperiness();
        // 1.16's additions.
        this.portium$lightLevel = properties.getLightLevel();
        if (properties.getHarvestTool() != null) ((Block) (Object) this).setHarvestLevel(properties.getHarvestTool(), Math.max(0, properties.getHarvestLevel()));
        if (!properties.isSolid()) this.translucent = true;
    }

    /** 1.16's light level by state, if the block was made with one. */
    @org.spongepowered.asm.mixin.Unique
    private java.util.function.ToIntFunction<IBlockState> portium$lightLevel;

    @Inject(method = "getLightValue(Lnet/minecraft/block/state/IBlockState;)I", at = @At("HEAD"), cancellable = true)
    private void portium$lightByState(IBlockState state, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> cir) {
        if (this.portium$lightLevel != null) cir.setReturnValue(this.portium$lightLevel.applyAsInt(state));
    }

    @Override
    public Item asItem() {
        return Item.getItemFromBlock((Block) (Object) this);
    }
}
