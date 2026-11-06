package com.michaelsebero.portium.client.dev;

import com.michaelsebero.portium.diagnostics.ProblemReport;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Bulk checks of one mod's content, by registry namespace, for {@link DevScript}: every item (sub-items, tooltip,
 * model, a draw in a screen), every block (placed in the world, its model, its block entity), every block entity's
 * screen (opened by a right click), and every entity (spawned, ticked, its renderer). Each logs a one-line result
 * starting {@code modtest}, and what failed, so a run over many mods reads at a glance.
 */
final class ModTest {

    private static final Logger LOGGER = LogManager.getLogger("Portium/DevScript");
    private static final int SHOWN = 12;

    /** Blocks the last {@code modblocks} placed that have block entities, for {@code modguis}. */
    private static final List<BlockPos> WITH_BLOCK_ENTITY = new ArrayList<>();
    /** Entities the last {@code modentities} spawned, by registry name. */
    private static final Map<String, Entity> SPAWNED = new LinkedHashMap<>();
    private static int problemsAtStart;

    /** Marks the start of a mod's checks: problems recorded from here on are the mod's. */
    static void begin(String namespace) {
        problemsAtStart = ProblemReport.count();
        LOGGER.info("  modtest {} begin: {} problems recorded before", namespace, problemsAtStart);
    }

    /** Logs the problems Portium recorded since {@link #begin}. */
    static void end(String namespace) {
        List<String> all = ProblemReport.summaries();
        List<String> ours = all.subList(Math.min(problemsAtStart, all.size()), all.size());
        LOGGER.info("  modtest {} end: {} new problems", namespace, ours.size());
        for (String line : ours.subList(0, Math.min(40, ours.size()))) LOGGER.info("    problem: {}", line.length() > 300 ? line.substring(0, 300) : line);
    }

    private static void report(String what, String namespace, int total, List<String> failures) {
        LOGGER.info("  modtest {} {}: {} checked, {} failed", namespace, what, total, failures.size());
        for (String failure : failures.subList(0, Math.min(SHOWN, failures.size()))) {
            LOGGER.info("    {}", failure.length() > 260 ? failure.substring(0, 260) : failure);
        }
        if (failures.size() > SHOWN) LOGGER.info("    ... and {} more", failures.size() - SHOWN);
    }

    /** Every item of the namespace: its sub-items, each stack's tooltip and model, and a draw as a screen draws it. */
    static void items(Minecraft mc, String namespace) {
        GuiScreen screen = new GuiScreen() {};
        screen.setWorldAndResolution(mc, 400, 300);
        IBakedModel missing = mc.getRenderItem().getItemModelMesher().getModelManager().getMissingModel();
        List<String> failures = new ArrayList<>();
        List<String> unmodelled = new ArrayList<>();
        int stacksChecked = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation name = item.getRegistryName();
            if (name == null || !name.getNamespace().equals(namespace)) continue;
            NonNullList<ItemStack> stacks = NonNullList.create();
            try {
                item.getSubItems(CreativeTabs.SEARCH, stacks);
                if (stacks.isEmpty()) stacks.add(new ItemStack(item));
            } catch (Throwable e) {
                failures.add(name + " (sub-items): " + e);
                continue;
            }
            // A mod with thousands of variants (every tool material) is checked by its first few of each item.
            for (ItemStack stack : stacks.subList(0, Math.min(4, stacks.size()))) {
                stacksChecked++;
                try {
                    for (String text : screen.getItemToolTip(stack)) text.length();
                    stack.getDisplayName();
                } catch (Throwable e) {
                    failures.add(name + " (tooltip): " + e);
                }
                try {
                    if (mc.getRenderItem().getItemModelWithOverrides(stack, mc.world, mc.player) == missing && !unmodelled.contains(name.toString())) {
                        unmodelled.add(name.toString());
                    }
                } catch (Throwable e) {
                    failures.add(name + " (model): " + e);
                }
                try {
                    GlStateManager.pushMatrix();
                    RenderHelper.enableGUIStandardItemLighting();
                    mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
                    mc.getRenderItem().renderItemOverlays(mc.fontRenderer, stack, 0, 0);
                } catch (Throwable e) {
                    failures.add(name + " (draw): " + e);
                } finally {
                    RenderHelper.disableStandardItemLighting();
                    GlStateManager.popMatrix();
                }
            }
        }
        report("items", namespace, stacksChecked, failures);
        report("item models missing", namespace, stacksChecked, unmodelled);
    }

    /** Every block of the namespace, placed in rows beside the player (on the server), and its model checked. */
    static void blocks(Minecraft mc, String namespace) {
        MinecraftServer server = mc.getIntegratedServer();
        if (server == null) return;
        WorldServer world = server.getWorld(mc.player.dimension);
        BlockPos origin = new BlockPos(mc.player).add(4, 0, 4);
        List<Block> blocks = new ArrayList<>();
        int technical = 0;
        for (Block block : ForgeRegistries.BLOCKS) {
            ResourceLocation name = block.getRegistryName();
            if (name == null || !name.getNamespace().equals(namespace)) continue;
            // Blocks without an item are made by other blocks or by the mod's code (a lectern holding a controller),
            // and may not stand on their own: only what a player can place is placed.
            if (Item.getItemFromBlock(block) == net.minecraft.init.Items.AIR) technical++;
            else blocks.add(block);
        }
        LOGGER.info("  modtest {} blocks: {} with items, {} without (not placed)", namespace, blocks.size(), technical);
        IBakedModel missing = mc.getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getMissingModel();
        List<String> unmodelled = new ArrayList<>();
        for (Block block : blocks) {
            try {
                IBlockState state = block.getDefaultState();
                if (state.getRenderType() == net.minecraft.util.EnumBlockRenderType.MODEL
                        && mc.getBlockRendererDispatcher().getModelForState(state) == missing) {
                    unmodelled.add(String.valueOf(block.getRegistryName()));
                }
            } catch (Throwable e) {
                unmodelled.add(block.getRegistryName() + " (model lookup): " + e);
            }
        }
        report("block models missing", namespace, blocks.size(), unmodelled);
        server.addScheduledTask(() -> {
            List<String> failures = new ArrayList<>();
            List<String> gone = new ArrayList<>();
            synchronized (WITH_BLOCK_ENTITY) {
                WITH_BLOCK_ENTITY.clear();
            }
            int index = 0;
            for (Block block : blocks) {
                BlockPos pos = origin.add((index % 24) * 2, 0, (index / 24) * 2);
                index++;
                try {
                    IBlockState state = block.getDefaultState();
                    world.setBlockState(pos.down(), net.minecraft.init.Blocks.STONE.getDefaultState(), 2);
                    world.setBlockState(pos, state, 3);
                    IBlockState now = world.getBlockState(pos);
                    if (now.getBlock() != block) {
                        gone.add(String.valueOf(block.getRegistryName()));
                        continue;
                    }
                    if (block.hasTileEntity(now)) {
                        if (world.getTileEntity(pos) == null) {
                            failures.add(block.getRegistryName() + ": no block entity");
                        } else {
                            synchronized (WITH_BLOCK_ENTITY) {
                                WITH_BLOCK_ENTITY.add(pos);
                            }
                        }
                    }
                    // What the game asks of every placed block sooner or later.
                    now.getActualState(world, pos);
                    now.getCollisionBoundingBox(world, pos);
                    now.getBoundingBox(world, pos);
                    block.getPickBlock(now, null, world, pos, null);
                    NonNullList<ItemStack> drops = NonNullList.create();
                    block.getDrops(drops, world, pos, now, 0);
                } catch (Throwable e) {
                    failures.add(block.getRegistryName() + ": " + e);
                }
            }
            report("blocks placed", namespace, blocks.size(), failures);
            report("blocks that didn't stay (may need support)", namespace, blocks.size(), gone);
        });
    }

    /** Script lines that open each block entity's screen as a player does, log it and close it. */
    static List<String> guis(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        synchronized (WITH_BLOCK_ENTITY) {
            for (BlockPos pos : WITH_BLOCK_ENTITY) {
                lines.add("cmd /tp @s " + (pos.getX() + 0.5) + " " + pos.getY() + " " + (pos.getZ() - 0.7) + " 0 40");
                lines.add("wait 3");
                lines.add("modgui " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
                lines.add("wait 6");
                lines.add("modguicheck " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
                lines.add("close");
                lines.add("wait 2");
            }
        }
        return lines;
    }

    private static final Map<String, Integer> SCREENS = new TreeMap<>();
    private static int opened;
    private static int clicked;

    static void gui(Minecraft mc, BlockPos pos) {
        clicked++;
        try {
            net.minecraft.util.math.Vec3d hit = new net.minecraft.util.math.Vec3d(pos).add(0.5D, 0.5D, 0.0D);
            mc.playerController.processRightClickBlock(mc.player, mc.world, pos, net.minecraft.util.EnumFacing.NORTH, hit, net.minecraft.util.EnumHand.MAIN_HAND);
        } catch (Throwable e) {
            LOGGER.info("  modtest click failed at {} ({}): {}", pos, mc.world.getBlockState(pos).getBlock().getRegistryName(), e.toString());
        }
    }

    static void guiCheck(Minecraft mc, BlockPos pos) {
        if (mc.currentScreen == null) return;
        opened++;
        String block = String.valueOf(mc.world.getBlockState(pos).getBlock().getRegistryName());
        SCREENS.merge(mc.currentScreen.getClass().getName(), 1, Integer::sum);
        LOGGER.info("  modtest screen for {}: {}", block, mc.currentScreen.getClass().getName());
    }

    static void guiSummary(String namespace) {
        LOGGER.info("  modtest {} screens: {} block entities clicked, {} opened a screen, {} screen classes", namespace, clicked, opened, SCREENS.size());
        clicked = 0;
        opened = 0;
        SCREENS.clear();
    }

    /** Every entity of the namespace, spawned in a row beside the player (on the server). */
    static void entities(Minecraft mc, String namespace) {
        MinecraftServer server = mc.getIntegratedServer();
        if (server == null) return;
        WorldServer world = server.getWorld(mc.player.dimension);
        BlockPos origin = new BlockPos(mc.player).add(4, 1, -6);
        server.addScheduledTask(() -> {
            List<String> failures = new ArrayList<>();
            synchronized (SPAWNED) {
                SPAWNED.clear();
            }
            int index = 0;
            for (EntityEntry entry : ForgeRegistries.ENTITIES) {
                ResourceLocation name = entry.getRegistryName();
                if (name == null || !name.getNamespace().equals(namespace)) continue;
                BlockPos pos = origin.add((index % 16) * 3, 0, -(index / 16) * 3);
                index++;
                try {
                    Entity entity = EntityList.createEntityByIDFromName(name, world);
                    if (entity == null) {
                        failures.add(name + ": not created");
                        continue;
                    }
                    entity.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
                    if (entity instanceof EntityLiving) {
                        ((EntityLiving) entity).onInitialSpawn(world.getDifficultyForLocation(pos), null);
                        ((EntityLiving) entity).enablePersistence();
                    }
                    if (!world.spawnEntity(entity)) {
                        failures.add(name + ": not spawned");
                        continue;
                    }
                    synchronized (SPAWNED) {
                        SPAWNED.put(name.toString(), entity);
                    }
                } catch (Throwable e) {
                    failures.add(name + ": " + e);
                    if (TRACED.add(e.toString())) LOGGER.info("  modtest trace for {}", name, e);
                }
            }
            report("entities spawned", namespace, index, failures);
        });
    }

    /** After the spawned entities have ticked a while: which are gone, which the client has, and their renderers. */
    static void entityCheck(Minecraft mc, String namespace) {
        List<String> failures = new ArrayList<>();
        int total;
        synchronized (SPAWNED) {
            total = SPAWNED.size();
            for (Map.Entry<String, Entity> entry : SPAWNED.entrySet()) {
                Entity server = entry.getValue();
                // Projectiles and the like end themselves; only the missing client copy or renderer of a live one counts.
                if (server.isDead) continue;
                Entity client = mc.world.getEntityByID(server.getEntityId());
                if (client == null) {
                    failures.add(entry.getKey() + ": the client never got it");
                    continue;
                }
                try {
                    Object renderer = mc.getRenderManager().getEntityRenderObject(client);
                    if (renderer == null) failures.add(entry.getKey() + ": no renderer");
                    else if (renderer.getClass() == net.minecraft.client.renderer.entity.RenderEntity.class) failures.add(entry.getKey() + ": drawn as a plain box (no renderer registered)");
                } catch (Throwable e) {
                    failures.add(entry.getKey() + " (renderer): " + e);
                }
            }
        }
        report("entities alive and drawn", namespace, total, failures);
    }

    /** Every particle type of the namespace that has a 1.16 factory: those without arguments are shown, in front of the player. */
    static void particles(Minecraft mc, String namespace) {
        List<String> failures = new ArrayList<>();
        int total = 0;
        int simple = 0;
        net.minecraft.util.math.Vec3d look = mc.player.getLookVec();
        for (com.michaelsebero.portium.modern.mc.particles.ParticleType<?> type : com.michaelsebero.portium.client.ParticleRendering116.types()) {
            ResourceLocation id = type.getId();
            if (id == null || !id.getNamespace().equals(namespace)) continue;
            total++;
            if (!(type instanceof com.michaelsebero.portium.modern.mc.particles.IParticleData)) continue;
            simple++;
            double x = mc.player.posX + look.x * 4.0D + (simple % 5 - 2);
            double y = mc.player.posY + 1.5D + (simple / 5) * 0.5D;
            double z = mc.player.posZ + look.z * 4.0D;
            try {
                Object made = com.michaelsebero.portium.client.ParticleRendering116.spawn((com.michaelsebero.portium.modern.mc.particles.IParticleData) type, mc.world,
                        true, false, x, y, z, 0.0D, 0.0D, 0.0D);
                if (made == null) failures.add(id + ": no particle made");
            } catch (Throwable e) {
                failures.add(id + ": " + e);
            }
        }
        LOGGER.info("  modtest {} particle types with factories: {} ({} without arguments, shown)", namespace, total, simple);
        report("particles shown", namespace, simple, failures);
    }

    /** After the particles have been drawn a while: which were switched off. */
    static void particleCheck(String namespace) {
        List<String> failures = new ArrayList<>(com.michaelsebero.portium.client.ParticleRendering116.switchedOff());
        report("particle classes and factories switched off", namespace, failures.size(), failures);
    }

    /** Failures whose stack has been logged once already. */
    private static final java.util.Set<String> TRACED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private ModTest() {}
}
