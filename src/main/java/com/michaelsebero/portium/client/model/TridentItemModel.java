package com.michaelsebero.portium.client.model;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.Collections;
import java.util.List;

/**
 * Like 1.13, the trident is a flat icon in inventories, on the ground and in item frames, and the 3D model in hand.
 * This model picks between the two per camera transform; the 3D one is drawn by the item stack renderer.
 */
public class TridentItemModel implements IBakedModel {

    private final IBakedModel inHand;
    private final IBakedModel icon;
    private final ItemOverrideList overrides;

    public TridentItemModel(IBakedModel inHand, IBakedModel icon) {
        this.inHand = inHand;
        this.icon = icon;
        this.overrides = new ItemOverrideList(Collections.emptyList()) {
            @Override
            public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
                IBakedModel resolved = inHand.getOverrides().handleItemState(inHand, stack, world, entity);
                return resolved == inHand ? TridentItemModel.this : new TridentItemModel(resolved, icon);
            }
        };
    }

    private static boolean usesIcon(ItemCameraTransforms.TransformType type) {
        return type == ItemCameraTransforms.TransformType.GUI || type == ItemCameraTransforms.TransformType.GROUND || type == ItemCameraTransforms.TransformType.FIXED;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
        return usesIcon(type) ? this.icon.handlePerspective(type) : this.inHand.handlePerspective(type);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return this.icon.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.icon.getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return this.overrides;
    }
}
