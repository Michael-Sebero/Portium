package com.michaelsebero.portium.mods116.shim;

import net.minecraft.item.EnumAction;
import net.minecraft.util.EnumActionResult;

/** 1.16 enum constants and helpers 1.12's versions of those enums lack, given their nearest 1.12 equivalents. */
public final class MiscShims116 {

    /** 1.16's CONSUME: an action that worked without the swing; 1.12 knows success. */
    public static net.minecraft.block.material.MapColor dyeMapColor(net.minecraft.item.EnumDyeColor color) {
        return net.minecraft.block.material.MapColor.getBlockColor(color);
    }

    public static int dyeFireworkColor(net.minecraft.item.EnumDyeColor color) {
        return net.minecraft.item.ItemDye.DYE_COLORS[color.getDyeDamage()];
    }

    public static int dyeTextColor(net.minecraft.item.EnumDyeColor color) {
        return color.getColorValue();
    }

    /** The name 1.12's inventory wants, for 1.16's that has none. */
    public static String inventoryName(int size) {
        return "container.inventory";
    }

    public static boolean notCustom(int size) {
        return false;
    }

    public static Throwable pauseDevMode(Throwable error) {
        return error;
    }

    public static double spawnX(net.minecraftforge.event.entity.living.LivingSpawnEvent event) {
        return event.getX();
    }

    public static double spawnY(net.minecraftforge.event.entity.living.LivingSpawnEvent event) {
        return event.getY();
    }

    public static double spawnZ(net.minecraftforge.event.entity.living.LivingSpawnEvent event) {
        return event.getZ();
    }

    public static EnumActionResult consume() {
        return EnumActionResult.SUCCESS;
    }

    /** 1.16's {@code sidedSuccess}: SUCCESS on the client, CONSUME on the server, both success here. */
    public static EnumActionResult sidedSuccess(boolean client) {
        return EnumActionResult.SUCCESS;
    }

    public static boolean isSuccessOrConsume(EnumActionResult result) {
        return result == EnumActionResult.SUCCESS;
    }

    public static boolean isSuccess(EnumActionResult result) {
        return result == EnumActionResult.SUCCESS;
    }

    /** 1.16's trident hold: drawn back like a bow. */
    public static EnumAction spear() {
        return EnumAction.BOW;
    }

    /** 1.16's crossbow hold, nearest to a bow's. */
    public static EnumAction crossbow() {
        return EnumAction.BOW;
    }

    /**
     * 1.16's attribute modifier operation (ADDITION, MULTIPLY_BASE, MULTIPLY_TOTAL, an enum 1.12 lacks) as 1.12's
     * operation number, which counts them in the same order.
     */
    public static int operation(Object operation) {
        return ((Enum<?>) operation).ordinal();
    }

    /** No format arguments, for 1.16's {@code new TranslationTextComponent(key)}: 1.12's takes them as varargs. */
    public static Object[] noArgs(String key) {
        return new Object[0];
    }

    /** 1.16's {@code potion.setItem(stack)}, which 1.16 has on its item projectiles (1.12's potion is its own). */
    public static void setPotionItem(net.minecraft.entity.projectile.EntityPotion potion, net.minecraft.item.ItemStack stack) {
        potion.setItem(stack);
    }

    /** 1.16's {@code server.execute(task)}: 1.12 schedules it. */
    public static void execute(net.minecraft.server.MinecraftServer server, Runnable task) {
        server.addScheduledTask(task);
    }

    /** 1.16's {@code minecraft.execute(task)}. */
    public static void executeClient(net.minecraft.client.Minecraft minecraft, Runnable task) {
        ((net.minecraft.util.IThreadListener) (Object) minecraft).addScheduledTask(task);
    }

    /** 1.16's hover event value: text for 1.12's (which only shows text). */
    public static net.minecraft.util.text.ITextComponent hoverValue(Object value) {
        return value instanceof net.minecraft.util.text.ITextComponent ? (net.minecraft.util.text.ITextComponent) value
                : new net.minecraft.util.text.TextComponentString(String.valueOf(value));
    }

    /**
     * 1.16's {@code shoot} on 1.12 entities that 1.16 makes projectiles (fireworks): 1.12's own aim for its
     * projectiles, else 1.16's.
     */
    public static void shoot(net.minecraft.entity.Entity entity, double x, double y, double z, float velocity, float inaccuracy) {
        if (entity instanceof net.minecraft.entity.IProjectile) {
            ((net.minecraft.entity.IProjectile) entity).shoot(x, y, z, velocity, inaccuracy);
            return;
        }
        java.util.Random random = entity.world == null ? new java.util.Random() : entity.world.rand;
        net.minecraft.util.math.Vec3d motion = new net.minecraft.util.math.Vec3d(x, y, z).normalize()
                .add(random.nextGaussian() * 0.0075D * inaccuracy, random.nextGaussian() * 0.0075D * inaccuracy, random.nextGaussian() * 0.0075D * inaccuracy)
                .scale(velocity);
        entity.motionX = motion.x;
        entity.motionY = motion.y;
        entity.motionZ = motion.z;
        float horizontal = net.minecraft.util.math.MathHelper.sqrt(motion.x * motion.x + motion.z * motion.z);
        entity.rotationYaw = (float) (net.minecraft.util.math.MathHelper.atan2(motion.x, motion.z) * (180D / Math.PI));
        entity.rotationPitch = (float) (net.minecraft.util.math.MathHelper.atan2(motion.y, horizontal) * (180D / Math.PI));
        entity.prevRotationYaw = entity.rotationYaw;
        entity.prevRotationPitch = entity.rotationPitch;
    }

    /** Forge 1.16's largest shaped recipe pattern, which mods raise (Create's mechanical crafting goes to 9 by 9). */
    private static volatile int craftingWidth = 3;
    private static volatile int craftingHeight = 3;

    /** Forge 1.16's {@code ShapedRecipe.setCraftingSize}: raises the largest pattern 1.16 recipes may have. */
    public static synchronized void setCraftingSize(int width, int height) {
        if (craftingWidth < width) craftingWidth = width;
        if (craftingHeight < height) craftingHeight = height;
    }

    public static int craftingWidth() {
        return craftingWidth;
    }

    public static int craftingHeight() {
        return craftingHeight;
    }

    /** 1.13's {@code map.add(value)}: under the next free number. */
    @SuppressWarnings("unchecked")
    public static void addIdentity(net.minecraft.util.ObjectIntIdentityMap map, Object value) {
        map.put(value, map.size());
    }

    /** No parent attribute: 1.16's attributes have none, 1.12's constructor takes one. */
    public static net.minecraft.entity.ai.attributes.IAttribute noParent(String name) {
        return null;
    }

    /** {@code new AxisAlignedBB(from, to)}: 1.12 has that constructor on clients only. */
    public static net.minecraft.util.math.AxisAlignedBB box(net.minecraft.util.math.Vec3d from, net.minecraft.util.math.Vec3d to) {
        return new net.minecraft.util.math.AxisAlignedBB(from.x, from.y, from.z, to.x, to.y, to.z);
    }

    /** 1.16's {@code server.getCommandManager().handleCommand(source, command)}: Portium's dispatcher runs it. */
    public static int handleCommand(Object commandManager, com.michaelsebero.portium.command.CommandSource source, String command) {
        return com.michaelsebero.portium.command.PortiumCommands.execute(source, command);
    }

    /** 1.16's {@code new Tessellator()}: 1.12's takes the buffer size 1.16's default is. */
    public static net.minecraft.client.renderer.Tessellator tessellator() {
        return new net.minecraft.client.renderer.Tessellator(2097152);
    }

    /** Forge 1.16's {@code event.getPlayer()}: Forge 1.12's {@code getEntityPlayer()}. */
    public static net.minecraft.entity.player.EntityPlayer eventPlayer(net.minecraftforge.event.entity.player.PlayerEvent event) {
        return event.getEntityPlayer();
    }

    public static net.minecraft.entity.player.EntityPlayer tooltipPlayer(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        return event.getEntityPlayer();
    }

    private MiscShims116() {}

    // Forge 1.16's IForge* interfaces give each object to itself ({@code block.getBlock()}); 1.12's classes don't.

    public static net.minecraft.block.Block self(net.minecraft.block.Block block) {
        return block;
    }

    public static net.minecraft.block.state.IBlockState self(net.minecraft.block.state.IBlockState state) {
        return state;
    }

    public static net.minecraft.item.Item self(net.minecraft.item.Item item) {
        return item;
    }

    public static net.minecraft.item.ItemStack self(net.minecraft.item.ItemStack stack) {
        return stack;
    }

    public static net.minecraft.tileentity.TileEntity self(net.minecraft.tileentity.TileEntity tileEntity) {
        return tileEntity;
    }

    public static net.minecraft.entity.Entity self(net.minecraft.entity.Entity entity) {
        return entity;
    }

    /** Forge 1.16's {@code BlockSnapshot.create(dimension, world, pos)}: Forge 1.12's snapshot of the world's block. */
    public static net.minecraftforge.common.util.BlockSnapshot blockSnapshot(Object dimension, net.minecraft.world.World world,
                                                                              net.minecraft.util.math.BlockPos pos) {
        return net.minecraftforge.common.util.BlockSnapshot.getBlockSnapshot(world, pos);
    }

    /** As {@link #blockSnapshot(Object, net.minecraft.world.World, net.minecraft.util.math.BlockPos)}, with its update flags. */
    public static net.minecraftforge.common.util.BlockSnapshot blockSnapshot(Object dimension, net.minecraft.world.World world,
                                                                              net.minecraft.util.math.BlockPos pos, int flag) {
        return net.minecraftforge.common.util.BlockSnapshot.getBlockSnapshot(world, pos, flag);
    }

    /** A 1.12 attribute modifier's operation number (for 1.16's {@code getOperation()}, made an enum by the glue). */
    public static int operationOf(Object modifier) {
        return ((net.minecraft.entity.ai.attributes.AttributeModifier) modifier).getOperation();
    }

    public static boolean isBadEffect(Object potion) {
        return ((net.minecraft.potion.Potion) potion).isBadEffect();
    }

    public static boolean isBeneficialEffect(Object potion) {
        return ((net.minecraft.potion.Potion) potion).isBeneficial();
    }

    /**
     * 1.16's {@code Attribute.getDescriptionId()}: 1.16 mods' attributes are named by it already; 1.12's own (named
     * {@code generic.maxHealth}) have 1.12's lang keys under {@code attribute.name.}.
     */
    public static String attributeDescription(net.minecraft.entity.ai.attributes.IAttribute attribute) {
        String name = attribute.getName();
        return name.startsWith("attribute.") ? name : "attribute.name." + name;
    }

    /** 1.16's {@code profiler.endStartSection(name)}. */
    public static void endStartSection(net.minecraft.profiler.Profiler profiler, String name) {
        profiler.endStartSection(name);
    }

    /** 1.16's {@code profiler.endStartSection(() -> name)}. */
    public static void endStartSection(net.minecraft.profiler.Profiler profiler, java.util.function.Supplier<String> name) {
        profiler.endSection();
        profiler.func_194340_a(name);
    }

    /** 1.16's {@code DyeColor.byId(id)}: 1.12's by metadata (the same numbers). */
    public static net.minecraft.item.EnumDyeColor dyeById(int id) {
        return net.minecraft.item.EnumDyeColor.byMetadata(id);
    }

    /** 1.16's {@code DyeColor.byTranslationKey(key, fallback)}: by 1.16's name (light_gray is 1.12's silver). */
    public static net.minecraft.item.EnumDyeColor dyeByName(String name, net.minecraft.item.EnumDyeColor fallback) {
        for (net.minecraft.item.EnumDyeColor color : net.minecraft.item.EnumDyeColor.values()) {
            String modern = color == net.minecraft.item.EnumDyeColor.SILVER ? "light_gray" : color.getName();
            if (modern.equals(name)) return color;
        }
        return fallback;
    }

    /** Forge 1.16's {@code IForgeBlock.getBlock()}: the block itself. */
    public static net.minecraft.block.Block selfBlock(Object block) {
        return (net.minecraft.block.Block) block;
    }

    /** Forge 1.16's {@code IForgeItem.getItem()}: the item itself. */
    public static net.minecraft.item.Item selfItem(Object item) {
        return (net.minecraft.item.Item) item;
    }

    /** 1.16's {@code entity.entityDropItem(item, offsetY)}: one of the item, dropped at the entity. */
    @javax.annotation.Nullable
    public static net.minecraft.entity.item.EntityItem dropItem(net.minecraft.entity.Entity entity, com.michaelsebero.portium.modern.mc.util.IItemProvider item, int offsetY) {
        return entity.entityDropItem(new net.minecraft.item.ItemStack(item.asItem()), offsetY);
    }
}
