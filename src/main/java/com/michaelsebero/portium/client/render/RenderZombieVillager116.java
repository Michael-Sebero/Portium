package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelZombieVillager116;
import com.michaelsebero.portium.entity.villager.IVillagerDataHolder;
import net.minecraft.client.model.ModelZombieVillager;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderZombieVillager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.util.ResourceLocation;

/**
 * Zombie villagers in 1.16.5 worlds as 1.14 draws them: a zombie villager skin dressed by biome type, profession and
 * level. Those without 1.14 data (other worlds) are drawn by 1.12's renderer.
 */
public class RenderZombieVillager116 extends RenderBiped<EntityZombieVillager> {

    private static final ResourceLocation SKIN = new ResourceLocation(Tags.MOD_ID, "textures/entity/zombie_villager/zombie_villager.png");

    private final RenderZombieVillager legacy;

    public RenderZombieVillager116(RenderManager manager) {
        super(manager, new ModelZombieVillager116(0.0F), 0.5F);
        this.legacy = new RenderZombieVillager(manager);
        this.addLayer(new RenderVillager116.LayerVillagerLevel<>(this, "zombie_villager"));
        this.addLayer(new LayerBipedArmor(this) {
            @Override
            protected void initArmor() {
                this.modelLeggings = new ModelZombieVillager(0.5F, 0.0F, true);
                this.modelArmor = new ModelZombieVillager(1.0F, 0.0F, true);
            }
        });
    }

    private static boolean modern(EntityZombieVillager zombie) {
        return zombie instanceof IVillagerDataHolder && ((IVillagerDataHolder) zombie).hasVillagerData();
    }

    @Override
    public void doRender(EntityZombieVillager zombie, double x, double y, double z, float yaw, float partialTicks) {
        if (modern(zombie)) {
            super.doRender(zombie, x, y, z, yaw, partialTicks);
        } else {
            this.legacy.doRender(zombie, x, y, z, yaw, partialTicks);
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityZombieVillager zombie) {
        return SKIN;
    }

    @Override
    protected void applyRotations(EntityZombieVillager zombie, float ageInTicks, float rotationYaw, float partialTicks) {
        if (zombie.isConverting()) rotationYaw += (float) (Math.cos((double) zombie.ticksExisted * 3.25D) * Math.PI * 0.25D);
        super.applyRotations(zombie, ageInTicks, rotationYaw, partialTicks);
    }
}
