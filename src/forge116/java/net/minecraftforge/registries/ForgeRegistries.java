package net.minecraftforge.registries;

import com.michaelsebero.portium.mods116.ModernRegistries;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.brain.schedule.Activity;
import net.minecraft.entity.ai.brain.schedule.Schedule;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.item.PaintingType;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.particles.ParticleType;
import net.minecraft.potion.Effect;
import net.minecraft.potion.Potion;
import net.minecraft.stats.StatType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.SoundEvent;
import net.minecraft.village.PointOfInterestType;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.blockplacer.BlockPlacerType;
import net.minecraft.world.gen.blockstateprovider.BlockStateProviderType;
import net.minecraft.world.gen.carver.WorldCarver;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.foliageplacer.FoliagePlacerType;
import net.minecraft.world.gen.placement.Placement;
import net.minecraft.world.gen.surfacebuilders.SurfaceBuilder;
import net.minecraft.world.gen.treedecorator.TreeDecoratorType;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.world.ForgeWorldType;

/**
 * Portium's replacement for Forge 1.16's: each registry by the class of what it holds, as Forge 1.16 finds them. Those
 * 1.12 has are 1.12's (blocks, items and so on); the rest Portium makes as Forge 1.12 registries before the mods
 * construct (see {@code ModernRegistries}). Null where neither exists yet (attributes, which are 1.12's own kind).
 */
@SuppressWarnings("rawtypes")
public class ForgeRegistries {

    public static final IForgeRegistry BLOCKS = get(Block.class);
    public static final IForgeRegistry FLUIDS = get(Fluid.class);
    public static final IForgeRegistry ITEMS = get(Item.class);
    public static final IForgeRegistry POTIONS = get(Effect.class);
    public static final IForgeRegistry SOUND_EVENTS = get(SoundEvent.class);
    public static final IForgeRegistry POTION_TYPES = get(Potion.class);
    public static final IForgeRegistry ENCHANTMENTS = get(Enchantment.class);
    public static final IForgeRegistry ENTITIES = get(EntityType.class);
    public static final IForgeRegistry TILE_ENTITIES = get(TileEntityType.class);
    public static final IForgeRegistry PARTICLE_TYPES = get(ParticleType.class);
    public static final IForgeRegistry CONTAINERS = get(ContainerType.class);
    public static final IForgeRegistry PAINTING_TYPES = get(PaintingType.class);
    public static final IForgeRegistry RECIPE_SERIALIZERS = get(IRecipeSerializer.class);
    public static final IForgeRegistry ATTRIBUTES = get(Attribute.class);
    public static final IForgeRegistry STAT_TYPES = get(StatType.class);
    public static final IForgeRegistry PROFESSIONS = get(VillagerProfession.class);
    public static final IForgeRegistry POI_TYPES = get(PointOfInterestType.class);
    public static final IForgeRegistry MEMORY_MODULE_TYPES = get(MemoryModuleType.class);
    public static final IForgeRegistry SENSOR_TYPES = get(SensorType.class);
    public static final IForgeRegistry SCHEDULES = get(Schedule.class);
    public static final IForgeRegistry ACTIVITIES = get(Activity.class);
    public static final IForgeRegistry WORLD_CARVERS = get(WorldCarver.class);
    public static final IForgeRegistry SURFACE_BUILDERS = get(SurfaceBuilder.class);
    public static final IForgeRegistry FEATURES = get(Feature.class);
    public static final IForgeRegistry DECORATORS = get(Placement.class);
    public static final IForgeRegistry CHUNK_STATUS = get(ChunkStatus.class);
    public static final IForgeRegistry STRUCTURE_FEATURES = get(Structure.class);
    public static final IForgeRegistry BLOCK_STATE_PROVIDER_TYPES = get(BlockStateProviderType.class);
    public static final IForgeRegistry BLOCK_PLACER_TYPES = get(BlockPlacerType.class);
    public static final IForgeRegistry FOLIAGE_PLACER_TYPES = get(FoliagePlacerType.class);
    public static final IForgeRegistry TREE_DECORATOR_TYPES = get(TreeDecoratorType.class);
    public static final IForgeRegistry DATA_SERIALIZERS = ModernRegistries.get("minecraft:dataserializers");
    public static final IForgeRegistry LOOT_MODIFIER_SERIALIZERS = get(GlobalLootModifierSerializer.class);
    public static final IForgeRegistry WORLD_TYPES = get(ForgeWorldType.class);

    private static IForgeRegistry get(Class<?> type) {
        return ModernRegistries.byType(type);
    }
}
