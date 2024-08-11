package com.michaelsebero.portium.modern.mc.util.math.shapes;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.fluid.IFluidState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/** 1.16's {@code net.minecraft.util.math.shapes.EntitySelectionContext}: a shape asked for by an entity, or nobody. */
public class EntitySelectionContext implements ISelectionContext {

    @Srg("field_216379_a")
    public static final ISelectionContext DUMMY = new EntitySelectionContext(false, -Double.MAX_VALUE, Items.AIR);

    @Srg("field_227579_b_")
    private final boolean sneaking;
    @Srg("field_216381_c")
    private final double posY;
    @Srg("field_216382_d")
    private final Item item;
    @javax.annotation.Nullable
    private final Entity entity;

    protected EntitySelectionContext(boolean sneaking, double posY, Item item) {
        this(sneaking, posY, item, null);
    }

    private EntitySelectionContext(boolean sneaking, double posY, Item item, @javax.annotation.Nullable Entity entity) {
        this.sneaking = sneaking;
        this.posY = posY;
        this.item = item;
        this.entity = entity;
    }

    @Deprecated
    protected EntitySelectionContext(Entity entity) {
        this(entity.isSneaking(), entity.getEntityBoundingBox().minY,
                entity instanceof EntityLivingBase ? ((EntityLivingBase) entity).getHeldItemMainhand().getItem() : Items.AIR, entity);
    }

    @Override
    @javax.annotation.Nullable
    public Entity getEntity() {
        return this.entity;
    }

    @Override
    @Srg("func_216375_a")
    public boolean hasItem(Item item) {
        return this.item == item;
    }

    @Override
    @Srg("func_230426_a_")
    public boolean canStandOnFluid(IFluidState fluidState, Object fluid) {
        return false;
    }

    @Override
    @Srg("func_225581_b_")
    public boolean isSneaking() {
        return this.sneaking;
    }

    @Override
    @Srg("func_216378_a")
    public boolean isAbove(VoxelShape shape, BlockPos pos, boolean flag) {
        return this.posY > pos.getY() + shape.getEnd(EnumFacing.Axis.Y) - 1.0E-5F;
    }
}
