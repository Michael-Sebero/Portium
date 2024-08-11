package com.michaelsebero.portium.modern.mc.util.math.shapes;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.fluid.IFluidState;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

/**
 * 1.16's {@code net.minecraft.util.math.shapes.ISelectionContext}: who a block's shape is asked for (an entity, or
 * nobody), so a block can answer differently (scaffolding, for one, lets sneaking players through).
 */
public interface ISelectionContext {

    @Srg("func_216377_a")
    static ISelectionContext dummy() {
        return EntitySelectionContext.DUMMY;
    }

    @Srg("func_216374_a")
    static ISelectionContext forEntity(Entity entity) {
        return new EntitySelectionContext(entity);
    }

    @Srg("func_225581_b_")
    boolean isSneaking();

    /** Forge's: the entity asking, if any. */
    @javax.annotation.Nullable
    default Entity getEntity() {
        return null;
    }

    /** Whether whoever asks is above the shape at that position (1.16's unnamed check, for scaffolding). */
    @Srg("func_216378_a")
    boolean isAbove(VoxelShape shape, BlockPos pos, boolean flag);

    @Srg("func_216375_a")
    boolean hasItem(Item item);

    /** Whether whoever asks stands on that fluid (striders on lava); the fluid is a class of Minecraft 1.16.5's own. */
    @Srg("func_230426_a_")
    boolean canStandOnFluid(IFluidState fluidState, Object fluid);
}
