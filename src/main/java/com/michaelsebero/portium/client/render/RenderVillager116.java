package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelVillager116;
import com.michaelsebero.portium.entity.villager.EntityVillager116;
import com.michaelsebero.portium.entity.villager.IVillagerDataHolder;
import com.michaelsebero.portium.entity.villager.VillagerData116;
import com.michaelsebero.portium.entity.villager.VillagerProfession116;
import com.michaelsebero.portium.entity.villager.VillagerType116;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * 1.14's villager renderer: the bare villager skin, then its biome type's clothes, its profession's and (but for
 * nitwits) its level badge, with 1.14's hat rules; lying down in bed when asleep.
 */
public class RenderVillager116 extends RenderLiving<EntityVillager116> {

    private static final ResourceLocation SKIN = new ResourceLocation(Tags.MOD_ID, "textures/entity/villager/villager.png");

    public RenderVillager116(RenderManager manager) {
        super(manager, new ModelVillager116(0.0F), 0.5F);
        this.addLayer(new LayerCustomHead(((ModelVillager116) this.mainModel).villagerHead));
        this.addLayer(new LayerVillagerLevel<>(this, "villager"));
        this.addLayer(new RenderWanderingTrader.LayerCrossedArmsItem());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityVillager116 villager) {
        return SKIN;
    }

    @Override
    protected void preRenderCallback(EntityVillager116 villager, float partialTicks) {
        float scale = 0.9375F;
        if (villager.isChild()) {
            scale *= 0.5F;
            this.shadowSize = 0.25F;
        } else {
            this.shadowSize = 0.5F;
        }
        GlStateManager.scale(scale, scale, scale);
    }

    @Override
    protected void renderLivingAt(EntityVillager116 villager, double x, double y, double z) {
        Optional<EnumFacing> bed = bedDirection(villager);
        if (bed.isPresent()) {
            float offset = villager.getEyeHeight() - 0.1F;
            super.renderLivingAt(villager, x - bed.get().getXOffset() * offset, y, z - bed.get().getZOffset() * offset);
        } else {
            super.renderLivingAt(villager, x, y, z);
        }
    }

    @Override
    protected void applyRotations(EntityVillager116 villager, float ageInTicks, float rotationYaw, float partialTicks) {
        Optional<EnumFacing> bed = bedDirection(villager);
        if (bed.isPresent() && villager.isEntityAlive()) {
            GlStateManager.rotate(facingAngle(bed.get()), 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(this.getDeathMaxRotation(villager), 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(270.0F, 0.0F, 1.0F, 0.0F);
            return;
        }
        super.applyRotations(villager, ageInTicks, rotationYaw, partialTicks);
    }

    /** Which way the bed it sleeps in points (foot to head), if it's asleep in one. */
    private static Optional<EnumFacing> bedDirection(EntityVillager116 villager) {
        Optional<BlockPos> pos = villager.getSleepingPos();
        if (!pos.isPresent()) return Optional.empty();
        IBlockState state = villager.world.getBlockState(pos.get());
        return state.getPropertyKeys().contains(BlockHorizontal.FACING) ? Optional.of(state.getValue(BlockHorizontal.FACING)) : Optional.of(EnumFacing.NORTH);
    }

    private static float facingAngle(EnumFacing facing) {
        switch (facing) {
            case SOUTH:
                return 90.0F;
            case NORTH:
                return 270.0F;
            case EAST:
                return 180.0F;
            default:
                return 0.0F;
        }
    }

    /**
     * 1.14's {@code VillagerLevelPendantLayer}: the type, profession and level textures over the skin. A type's hat
     * shows unless the profession has a hat of its own (a full one, or a partial one over a type without a full hat).
     */
    public static class LayerVillagerLevel<T extends EntityLivingBase> implements LayerRenderer<T> {

        /** Textures whose metadata says they have a full hat or a partial one (1.16.5's villager .mcmeta files). */
        private static final Set<VillagerType116> FULL_HAT_TYPES = EnumSet.of(VillagerType116.DESERT, VillagerType116.SNOW);
        private static final Set<VillagerProfession116> FULL_HAT_PROFESSIONS = EnumSet.of(VillagerProfession116.FARMER, VillagerProfession116.SHEPHERD,
                VillagerProfession116.FLETCHER, VillagerProfession116.LIBRARIAN, VillagerProfession116.FISHERMAN);
        private static final Set<VillagerProfession116> PARTIAL_HAT_PROFESSIONS = EnumSet.of(VillagerProfession116.BUTCHER);
        private static final String[] LEVELS = {"stone", "iron", "gold", "emerald", "diamond"};
        private static final java.util.Map<String, ResourceLocation> TEXTURES = new java.util.HashMap<>();

        private final RenderLivingBase<T> renderer;
        private final String kind;

        public LayerVillagerLevel(RenderLivingBase<T> renderer, String kind) {
            this.renderer = renderer;
            this.kind = kind;
        }

        @Override
        public void doRenderLayer(T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scale) {
            if (entity.isInvisible() || !(entity instanceof IVillagerDataHolder)) return;
            VillagerData116 data = ((IVillagerDataHolder) entity).getVillagerData();
            VillagerProfession116 profession = data.getProfession();
            net.minecraft.client.model.ModelBase model = this.renderer.getMainModel();
            boolean professionHat = FULL_HAT_PROFESSIONS.contains(profession)
                    || PARTIAL_HAT_PROFESSIONS.contains(profession) && FULL_HAT_TYPES.contains(data.getType());
            ((com.michaelsebero.portium.client.model.IVillagerHatModel) model).setHatVisible(!professionHat);
            this.draw(entity, model, "type/" + data.getType().getName(), limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            ((com.michaelsebero.portium.client.model.IVillagerHatModel) model).setHatVisible(true);
            if (profession != VillagerProfession116.NONE && !entity.isChild()) {
                this.draw(entity, model, "profession/" + profession.getName(), limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                if (profession != VillagerProfession116.NITWIT) {
                    this.draw(entity, model, "profession_level/" + LEVELS[data.getLevel() - 1], limbSwing, limbSwingAmount, ageInTicks, netHeadYaw,
                            headPitch, scale);
                }
            }
        }

        private void draw(T entity, net.minecraft.client.model.ModelBase model, String texture, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                          float headPitch, float scale) {
            String path = "textures/entity/" + this.kind + "/" + texture + ".png";
            this.renderer.bindTexture(TEXTURES.computeIfAbsent(path, key -> new ResourceLocation(Tags.MOD_ID, key)));
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        @Override
        public boolean shouldCombineTextures() {
            return true;
        }
    }
}
