package com.michaelsebero.portium.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.AbstractChestHorse;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * 1.15's horse model, on 64x64 textures: a body with the saddle drawn on it, a neck carrying the head, mane, muzzle,
 * ears and bridle, four legs (and a foal's longer pair), and a tail. Donkeys and mules ({@code chested}) have long
 * ears and chests at their sides. Foals draw the neck and body at 1.15's baby scales.
 */
public class ModelHorse116 extends ModelBase {

    private static final float BABY_HEAD_SCALE = 2.7272F;
    private static final float BABY_HEAD_Y = 16.2F;
    private static final float BABY_HEAD_Z = 1.36F;
    private static final float BABY_BODY_SCALE = 2.0F;
    private static final float BABY_BODY_Y = 20.0F;

    private final ModelRenderer body;
    private final ModelRenderer neck;
    private final ModelRenderer backLeftLeg;
    private final ModelRenderer backRightLeg;
    private final ModelRenderer frontLeftLeg;
    private final ModelRenderer frontRightLeg;
    private final ModelRenderer babyBackLeftLeg;
    private final ModelRenderer babyBackRightLeg;
    private final ModelRenderer babyFrontLeftLeg;
    private final ModelRenderer babyFrontRightLeg;
    private final ModelRenderer tail;
    private final ModelRenderer[] saddleParts;
    private final ModelRenderer[] reins;
    private final boolean chested;
    private ModelRenderer leftChest;
    private ModelRenderer rightChest;

    public ModelHorse116(float scale, boolean chested) {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.chested = chested;
        this.body = new ModelRenderer(this, 0, 32);
        this.body.addBox(-5.0F, -8.0F, -17.0F, 10, 10, 22, 0.05F);
        this.body.setRotationPoint(0.0F, 11.0F, 5.0F);
        this.neck = new ModelRenderer(this, 0, 35);
        this.neck.addBox(-2.05F, -6.0F, -2.0F, 4, 12, 7, 0.0F);
        this.neck.rotateAngleX = 0.5235988F;
        ModelRenderer head = new ModelRenderer(this, 0, 13);
        head.addBox(-3.0F, -11.0F, -2.0F, 6, 5, 7, scale);
        ModelRenderer mane = new ModelRenderer(this, 56, 36);
        mane.addBox(-1.0F, -11.0F, 5.01F, 2, 16, 2, scale);
        ModelRenderer upperMouth = new ModelRenderer(this, 0, 25);
        upperMouth.addBox(-2.0F, -11.0F, -7.0F, 4, 5, 5, scale);
        this.neck.addChild(head);
        this.neck.addChild(mane);
        this.neck.addChild(upperMouth);
        if (chested) {
            this.addLongEars();
        } else {
            this.addEars();
        }
        this.backLeftLeg = this.leg(true, -3.0F, -1.0F, 4.0F, 14.0F, 7.0F, scale, 0.0F);
        this.backRightLeg = this.leg(false, -1.0F, -1.0F, -4.0F, 14.0F, 7.0F, scale, 0.0F);
        this.frontLeftLeg = this.leg(true, -3.0F, -1.9F, 4.0F, 6.0F, -12.0F, scale, 0.0F);
        this.frontRightLeg = this.leg(false, -1.0F, -1.9F, -4.0F, 6.0F, -12.0F, scale, 0.0F);
        this.babyBackLeftLeg = this.leg(true, -3.0F, -1.0F, 4.0F, 14.0F, 7.0F, scale, 5.5F);
        this.babyBackRightLeg = this.leg(false, -1.0F, -1.0F, -4.0F, 14.0F, 7.0F, scale, 5.5F);
        this.babyFrontLeftLeg = this.leg(true, -3.0F, -1.9F, 4.0F, 6.0F, -12.0F, scale, 5.5F);
        this.babyFrontRightLeg = this.leg(false, -1.0F, -1.9F, -4.0F, 6.0F, -12.0F, scale, 5.5F);
        this.tail = new ModelRenderer(this, 42, 36);
        this.tail.addBox(-1.5F, 0.0F, 0.0F, 3, 14, 4, scale);
        this.tail.setRotationPoint(0.0F, -5.0F, 2.0F);
        this.tail.rotateAngleX = 0.5235988F;
        this.body.addChild(this.tail);
        ModelRenderer saddle = new ModelRenderer(this, 26, 0);
        saddle.addBox(-5.0F, -8.0F, -9.0F, 10, 9, 9, 0.5F);
        this.body.addChild(saddle);
        ModelRenderer leftMouth = new ModelRenderer(this, 29, 5);
        leftMouth.addBox(2.0F, -9.0F, -6.0F, 1, 2, 2, scale);
        this.neck.addChild(leftMouth);
        ModelRenderer rightMouth = new ModelRenderer(this, 29, 5);
        rightMouth.addBox(-3.0F, -9.0F, -6.0F, 1, 2, 2, scale);
        this.neck.addChild(rightMouth);
        ModelRenderer leftRein = new ModelRenderer(this, 32, 2);
        leftRein.addBox(3.1F, -6.0F, -8.0F, 0, 3, 16, scale);
        leftRein.rotateAngleX = -0.5235988F;
        this.neck.addChild(leftRein);
        ModelRenderer rightRein = new ModelRenderer(this, 32, 2);
        rightRein.addBox(-3.1F, -6.0F, -8.0F, 0, 3, 16, scale);
        rightRein.rotateAngleX = -0.5235988F;
        this.neck.addChild(rightRein);
        ModelRenderer headgear = new ModelRenderer(this, 1, 1);
        headgear.addBox(-3.0F, -11.0F, -1.9F, 6, 5, 6, 0.2F);
        this.neck.addChild(headgear);
        ModelRenderer bridle = new ModelRenderer(this, 19, 0);
        bridle.addBox(-2.0F, -11.0F, -4.0F, 4, 5, 2, 0.2F);
        this.neck.addChild(bridle);
        this.saddleParts = new ModelRenderer[] {saddle, leftMouth, rightMouth, headgear, bridle};
        this.reins = new ModelRenderer[] {leftRein, rightRein};
        if (chested) {
            this.leftChest = new ModelRenderer(this, 26, 21);
            this.leftChest.addBox(-4.0F, 0.0F, -2.0F, 8, 8, 3, 0.0F);
            this.rightChest = new ModelRenderer(this, 26, 21);
            this.rightChest.addBox(-4.0F, 0.0F, -2.0F, 8, 8, 3, 0.0F);
            this.leftChest.rotateAngleY = -1.5707964F;
            this.rightChest.rotateAngleY = 1.5707964F;
            this.leftChest.setRotationPoint(6.0F, -8.0F, 0.0F);
            this.rightChest.setRotationPoint(-6.0F, -8.0F, 0.0F);
            this.body.addChild(this.leftChest);
            this.body.addChild(this.rightChest);
        }
    }

    /** A horse's short ears. */
    private void addEars() {
        ModelRenderer left = new ModelRenderer(this, 19, 16);
        left.addBox(0.55F, -13.0F, 4.0F, 2, 3, 1, -0.001F);
        ModelRenderer right = new ModelRenderer(this, 19, 16);
        right.addBox(-2.55F, -13.0F, 4.0F, 2, 3, 1, -0.001F);
        this.neck.addChild(left);
        this.neck.addChild(right);
    }

    /** A donkey's or mule's long ears, splayed outwards. */
    private void addLongEars() {
        ModelRenderer left = new ModelRenderer(this, 0, 12);
        left.addBox(-1.0F, -7.0F, 0.0F, 2, 7, 1, 0.0F);
        left.setRotationPoint(1.25F, -10.0F, 4.0F);
        ModelRenderer right = new ModelRenderer(this, 0, 12);
        right.addBox(-1.0F, -7.0F, 0.0F, 2, 7, 1, 0.0F);
        right.setRotationPoint(-1.25F, -10.0F, 4.0F);
        left.rotateAngleX = 0.2617994F;
        left.rotateAngleZ = 0.2617994F;
        right.rotateAngleX = 0.2617994F;
        right.rotateAngleZ = -0.2617994F;
        this.neck.addChild(left);
        this.neck.addChild(right);
    }

    /** A leg; a foal's is {@code babyStretch} longer at both ends (its body is drawn at half size, its legs not). */
    private ModelRenderer leg(boolean mirror, float x, float z, float pointX, float pointY, float pointZ, float scale, float babyStretch) {
        ModelRenderer leg = new ModelRenderer(this, 48, 21);
        leg.mirror = mirror;
        if (babyStretch == 0.0F) {
            leg.addBox(x, -1.01F, z, 4, 11, 4, scale);
        } else {
            addGrownBox(leg, 48, 21, x, -1.01F, z, 4, 11, 4, scale, scale + babyStretch, scale);
        }
        leg.setRotationPoint(pointX, pointY, pointZ);
        return leg;
    }

    /** 1.15's box grown by a different amount along each axis (1.12's boxes grow evenly): the texture is unchanged. */
    private static void addGrownBox(ModelRenderer renderer, int u, int v, float x, float y, float z, int width, int height, int depth,
                                    float growX, float growY, float growZ) {
        ModelBox box = new ModelBox(renderer, u, v, x, y, z, width, height, depth, 0.0F, renderer.mirror);
        TexturedQuad[] quads = ObfuscationReflectionHelper.getPrivateValue(ModelBox.class, box, "field_78254_i");
        for (TexturedQuad quad : quads) {
            for (int i = 0; i < quad.vertexPositions.length; ++i) {
                PositionTextureVertex vertex = quad.vertexPositions[i];
                Vec3d at = vertex.vector3D;
                float vx = grow(at.x, x, width, growX);
                float vy = grow(at.y, y, height, growY);
                float vz = grow(at.z, z, depth, growZ);
                quad.vertexPositions[i] = new PositionTextureVertex(vx, vy, vz, vertex.texturePositionX, vertex.texturePositionY);
            }
        }
        renderer.cubeList.add(box);
    }

    private static float grow(double at, float min, int size, float grow) {
        return Math.abs(at - min) < Math.abs(at - (min + size)) ? min - grow : min + size + grow;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (this.isChild) {
            GlStateManager.pushMatrix();
            float headScale = 1.5F / BABY_HEAD_SCALE;
            GlStateManager.scale(headScale, headScale, headScale);
            GlStateManager.translate(0.0F, BABY_HEAD_Y / 16.0F, BABY_HEAD_Z / 16.0F);
            this.neck.render(scale);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            float bodyScale = 1.0F / BABY_BODY_SCALE;
            GlStateManager.scale(bodyScale, bodyScale, bodyScale);
            GlStateManager.translate(0.0F, BABY_BODY_Y / 16.0F, 0.0F);
            this.renderBody(scale);
            GlStateManager.popMatrix();
        } else {
            this.neck.render(scale);
            this.renderBody(scale);
        }
    }

    private void renderBody(float scale) {
        this.body.render(scale);
        this.backLeftLeg.render(scale);
        this.backRightLeg.render(scale);
        this.frontLeftLeg.render(scale);
        this.frontRightLeg.render(scale);
        this.babyBackLeftLeg.render(scale);
        this.babyBackRightLeg.render(scale);
        this.babyFrontLeftLeg.render(scale);
        this.babyFrontRightLeg.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        if (!(entity instanceof AbstractHorse)) return;
        AbstractHorse horse = (AbstractHorse) entity;
        boolean saddled = horse.isHorseSaddled();
        boolean ridden = horse.isBeingRidden();
        for (ModelRenderer part : this.saddleParts) part.showModel = saddled;
        for (ModelRenderer part : this.reins) part.showModel = ridden && saddled;
        this.body.rotationPointY = 11.0F;
        if (this.chested) {
            boolean chest = horse instanceof AbstractChestHorse && ((AbstractChestHorse) horse).hasChest();
            this.leftChest.showModel = chest;
            this.rightChest.showModel = chest;
        }
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        super.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
        if (!(entity instanceof AbstractHorse)) return;
        AbstractHorse horse = (AbstractHorse) entity;
        float bodyYaw = rotLerp(partialTicks, horse.prevRenderYawOffset, horse.renderYawOffset);
        float headYaw = rotLerp(partialTicks, horse.prevRotationYawHead, horse.rotationYawHead);
        float pitch = horse.prevRotationPitch + (horse.rotationPitch - horse.prevRotationPitch) * partialTicks;
        float yaw = MathHelper.clamp(headYaw - bodyYaw, -20.0F, 20.0F);
        float pitchRad = pitch * 0.017453292F;
        if (limbSwingAmount > 0.2F) pitchRad += MathHelper.cos(limbSwing * 0.4F) * 0.15F * limbSwingAmount;
        float eating = horse.getGrassEatingAmount(partialTicks);
        float rearing = horse.getRearingAmount(partialTicks);
        float standing = 1.0F - rearing;
        float mouth = horse.getMouthOpennessAngle(partialTicks);
        boolean swishing = horse.tailCounter != 0;
        float age = horse.ticksExisted + partialTicks;
        this.neck.rotationPointY = 4.0F;
        this.neck.rotationPointZ = -12.0F;
        this.body.rotateAngleX = 0.0F;
        this.neck.rotateAngleX = 0.5235988F + pitchRad;
        this.neck.rotateAngleY = yaw * 0.017453292F;
        float water = horse.isInWater() ? 0.2F : 1.0F;
        float swing = MathHelper.cos(water * limbSwing * 0.6662F + (float) Math.PI);
        float frontSwing = swing * 0.8F * limbSwingAmount;
        float rest = 1.0F - Math.max(rearing, eating);
        float idleNeck = rest * (0.5235988F + pitchRad + mouth * MathHelper.sin(age) * 0.05F);
        this.neck.rotateAngleX = rearing * (0.2617994F + pitchRad) + eating * (2.1816616F + MathHelper.sin(age) * 0.05F) + idleNeck;
        this.neck.rotateAngleY = rearing * yaw * 0.017453292F + rest * this.neck.rotateAngleY;
        this.neck.rotationPointY = rearing * -4.0F + eating * 11.0F + rest * this.neck.rotationPointY;
        this.neck.rotationPointZ = rearing * -4.0F + eating * -12.0F + rest * this.neck.rotationPointZ;
        this.body.rotateAngleX = rearing * -0.7853982F + standing * this.body.rotateAngleX;
        float backRear = 0.2617994F * rearing;
        float kick = MathHelper.cos(age * 0.6F + (float) Math.PI);
        this.frontLeftLeg.rotationPointY = 2.0F * rearing + 14.0F * standing;
        this.frontLeftLeg.rotationPointZ = -6.0F * rearing - 10.0F * standing;
        this.frontRightLeg.rotationPointY = this.frontLeftLeg.rotationPointY;
        this.frontRightLeg.rotationPointZ = this.frontLeftLeg.rotationPointZ;
        float frontLeft = (-1.0471976F + kick) * rearing + frontSwing * standing;
        float frontRight = (-1.0471976F - kick) * rearing - frontSwing * standing;
        this.backLeftLeg.rotateAngleX = backRear - swing * 0.5F * limbSwingAmount * standing;
        this.backRightLeg.rotateAngleX = backRear + swing * 0.5F * limbSwingAmount * standing;
        this.frontLeftLeg.rotateAngleX = frontLeft;
        this.frontRightLeg.rotateAngleX = frontRight;
        this.tail.rotateAngleX = 0.5235988F + limbSwingAmount * 0.75F;
        this.tail.rotationPointY = -5.0F + limbSwingAmount;
        this.tail.rotationPointZ = 2.0F + limbSwingAmount * 2.0F;
        this.tail.rotateAngleY = swishing ? MathHelper.cos(age * 0.7F) : 0.0F;
        copyPose(this.babyBackLeftLeg, this.backLeftLeg);
        copyPose(this.babyBackRightLeg, this.backRightLeg);
        copyPose(this.babyFrontLeftLeg, this.frontLeftLeg);
        copyPose(this.babyFrontRightLeg, this.frontRightLeg);
        boolean child = horse.isChild();
        this.backLeftLeg.showModel = !child;
        this.backRightLeg.showModel = !child;
        this.frontLeftLeg.showModel = !child;
        this.frontRightLeg.showModel = !child;
        this.babyBackLeftLeg.showModel = child;
        this.babyBackRightLeg.showModel = child;
        this.babyFrontLeftLeg.showModel = child;
        this.babyFrontRightLeg.showModel = child;
    }

    private static void copyPose(ModelRenderer to, ModelRenderer from) {
        to.rotationPointY = from.rotationPointY;
        to.rotationPointZ = from.rotationPointZ;
        to.rotateAngleX = from.rotateAngleX;
    }

    private static float rotLerp(float partialTicks, float from, float to) {
        return from + partialTicks * MathHelper.wrapDegrees(to - from);
    }
}
