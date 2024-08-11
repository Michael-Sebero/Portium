package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.arguments.BlockPosArgument;
import com.michaelsebero.portium.command.arguments.EntityArgument;
import com.michaelsebero.portium.command.arguments.ItemArgument;
import com.michaelsebero.portium.command.arguments.ItemInput;
import com.michaelsebero.portium.command.arguments.ItemPredicateArgument;
import com.michaelsebero.portium.command.arguments.MiscArguments;
import com.michaelsebero.portium.command.arguments.RegistryArguments;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.arguments.BoolArgumentType;
import com.michaelsebero.portium.command.brigadier.arguments.IntegerArgumentType;
import com.michaelsebero.portium.command.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.GameType;

import java.util.Collection;
import java.util.Collections;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code give}, {@code clear}, {@code effect}, {@code enchant}, {@code experience}, game modes, {@code spawnpoint}, {@code replaceitem} (1.13). */
public final class PlayerCommands {

    private static final CommandExceptionType.Dynamic CLEAR_FAILED_SINGLE = dynamic("clear.failed.single");
    private static final CommandExceptionType.Dynamic CLEAR_FAILED_MULTIPLE = dynamic("clear.failed.multiple");
    private static final CommandExceptionType.Simple EFFECT_GIVE_FAILED = simple("effect.give.failed");
    private static final CommandExceptionType.Simple EFFECT_CLEAR_ALL_FAILED = simple("effect.clear.everything.failed");
    private static final CommandExceptionType.Simple EFFECT_CLEAR_FAILED = simple("effect.clear.specific.failed");
    private static final CommandExceptionType.Dynamic ENCHANT_NOT_LIVING = dynamic("enchant.failed.entity");
    private static final CommandExceptionType.Dynamic ENCHANT_NO_ITEM = dynamic("enchant.failed.itemless");
    private static final CommandExceptionType.Dynamic ENCHANT_INCOMPATIBLE = dynamic("enchant.failed.incompatible");
    private static final CommandExceptionType.Dynamic2 ENCHANT_LEVEL = new CommandExceptionType.Dynamic2((a, b) -> TextMessage.translate("commands.enchant.failed.level", a, b));
    private static final CommandExceptionType.Simple ENCHANT_FAILED = simple("enchant.failed");
    private static final CommandExceptionType.Simple XP_POINTS_INVALID = simple("experience.set.points.invalid");
    private static final CommandExceptionType.Simple REPLACE_BLOCK_FAILED = simple("replaceitem.block.failed");
    private static final CommandExceptionType.Dynamic REPLACE_SLOT_FAILED = dynamic("replaceitem.slot.inapplicable");
    private static final CommandExceptionType.Dynamic2 REPLACE_ENTITY_FAILED = new CommandExceptionType.Dynamic2((a, b) -> TextMessage.translate("commands.replaceitem.entity.failed", a, b));

    private static CommandExceptionType.Simple simple(String key) {
        return new CommandExceptionType.Simple(TextMessage.translate("commands." + key));
    }

    private static CommandExceptionType.Dynamic dynamic(String key) {
        return new CommandExceptionType.Dynamic(arg -> TextMessage.translate("commands." + key, arg));
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("give").requires(source -> source.hasPermissionLevel(2)).then(argument("targets", EntityArgument.players())
                .then(argument("item", ItemArgument.item())
                        .executes(context -> give(context.getSource(), ItemArgument.getItem(context, "item"), EntityArgument.getPlayers(context, "targets"), 1))
                        .then(argument("count", IntegerArgumentType.integer(1)).executes(context -> give(context.getSource(), ItemArgument.getItem(context, "item"),
                                EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "count")))))));

        dispatcher.register(literal("clear").requires(source -> source.hasPermissionLevel(2))
                .executes(context -> clear(context.getSource(), Collections.singleton(context.getSource().asPlayer()), stack -> true, -1))
                .then(argument("targets", EntityArgument.players())
                        .executes(context -> clear(context.getSource(), EntityArgument.getPlayers(context, "targets"), stack -> true, -1))
                        .then(argument("item", ItemPredicateArgument.itemPredicate())
                                .executes(context -> clear(context.getSource(), EntityArgument.getPlayers(context, "targets"), ItemPredicateArgument.getItemPredicate(context, "item"), -1))
                                .then(argument("maxCount", IntegerArgumentType.integer(0)).executes(context -> clear(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                        ItemPredicateArgument.getItemPredicate(context, "item"), IntegerArgumentType.getInteger(context, "maxCount")))))));

        dispatcher.register(literal("effect").requires(source -> source.hasPermissionLevel(2))
                .then(literal("clear").then(argument("targets", EntityArgument.entities())
                        .executes(context -> clearEffects(context.getSource(), EntityArgument.getEntities(context, "targets")))
                        .then(argument("effect", RegistryArguments.effect()).executes(context ->
                                clearEffect(context.getSource(), EntityArgument.getEntities(context, "targets"), context.getArgument("effect", Potion.class))))))
                .then(literal("give").then(argument("targets", EntityArgument.entities()).then(argument("effect", RegistryArguments.effect())
                        .executes(context -> giveEffect(context.getSource(), EntityArgument.getEntities(context, "targets"), context.getArgument("effect", Potion.class), null, 0, true))
                        .then(argument("seconds", IntegerArgumentType.integer(1, 1000000))
                                .executes(context -> giveEffect(context.getSource(), EntityArgument.getEntities(context, "targets"), context.getArgument("effect", Potion.class),
                                        IntegerArgumentType.getInteger(context, "seconds"), 0, true))
                                .then(argument("amplifier", IntegerArgumentType.integer(0, 255))
                                        .executes(context -> giveEffect(context.getSource(), EntityArgument.getEntities(context, "targets"), context.getArgument("effect", Potion.class),
                                                IntegerArgumentType.getInteger(context, "seconds"), IntegerArgumentType.getInteger(context, "amplifier"), true))
                                        .then(argument("hideParticles", BoolArgumentType.bool()).executes(context -> giveEffect(context.getSource(),
                                                EntityArgument.getEntities(context, "targets"), context.getArgument("effect", Potion.class), IntegerArgumentType.getInteger(context, "seconds"),
                                                IntegerArgumentType.getInteger(context, "amplifier"), !BoolArgumentType.getBool(context, "hideParticles"))))))))));

        dispatcher.register(literal("enchant").requires(source -> source.hasPermissionLevel(2)).then(argument("targets", EntityArgument.entities())
                .then(argument("enchantment", RegistryArguments.enchantment())
                        .executes(context -> enchant(context.getSource(), EntityArgument.getEntities(context, "targets"), context.getArgument("enchantment", Enchantment.class), 1))
                        .then(argument("level", IntegerArgumentType.integer(0)).executes(context -> enchant(context.getSource(), EntityArgument.getEntities(context, "targets"),
                                context.getArgument("enchantment", Enchantment.class), IntegerArgumentType.getInteger(context, "level")))))));

        LiteralArgumentBuilder<CommandSource> experience = literal("experience").requires(source -> source.hasPermissionLevel(2))
                .then(literal("add").then(argument("targets", EntityArgument.players()).then(argument("amount", IntegerArgumentType.integer())
                        .executes(context -> addXp(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), false))
                        .then(literal("points").executes(context -> addXp(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), false)))
                        .then(literal("levels").executes(context -> addXp(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), true))))))
                .then(literal("set").then(argument("targets", EntityArgument.players()).then(argument("amount", IntegerArgumentType.integer(0))
                        .executes(context -> setXp(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), false))
                        .then(literal("points").executes(context -> setXp(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), false)))
                        .then(literal("levels").executes(context -> setXp(context.getSource(), EntityArgument.getPlayers(context, "targets"), IntegerArgumentType.getInteger(context, "amount"), true))))))
                .then(literal("query").then(argument("targets", EntityArgument.player())
                        .then(literal("points").executes(context -> queryXp(context.getSource(), EntityArgument.getPlayer(context, "targets"), false)))
                        .then(literal("levels").executes(context -> queryXp(context.getSource(), EntityArgument.getPlayer(context, "targets"), true)))));
        com.michaelsebero.portium.command.brigadier.tree.LiteralCommandNode<CommandSource> experienceNode = dispatcher.register(experience);
        dispatcher.register(literal("xp").requires(source -> source.hasPermissionLevel(2)).redirect(experienceNode));

        LiteralArgumentBuilder<CommandSource> gamemode = literal("gamemode").requires(source -> source.hasPermissionLevel(2));
        LiteralArgumentBuilder<CommandSource> defaultGamemode = literal("defaultgamemode").requires(source -> source.hasPermissionLevel(2));
        for (GameType type : GameType.values()) {
            if (type == GameType.NOT_SET) continue;
            gamemode.then(literal(type.getName())
                    .executes(context -> setGameMode(context.getSource(), Collections.singleton(context.getSource().asPlayer()), type))
                    .then(argument("target", EntityArgument.players()).executes(context -> setGameMode(context.getSource(), EntityArgument.getPlayers(context, "target"), type))));
            defaultGamemode.then(literal(type.getName()).executes(context -> {
                context.getSource().getServer().setGameType(type);
                if (context.getSource().getServer().getForceGamemode()) {
                    for (EntityPlayerMP player : context.getSource().getServer().getPlayerList().getPlayers()) player.setGameType(type);
                }
                context.getSource().sendFeedback(new TextComponentTranslation("commands.defaultgamemode.success", new TextComponentTranslation("gameMode." + type.getName())), true);
                return 0;
            }));
        }
        dispatcher.register(gamemode);
        dispatcher.register(defaultGamemode);

        dispatcher.register(literal("spawnpoint").requires(source -> source.hasPermissionLevel(2))
                .executes(context -> setSpawn(context.getSource(), Collections.singleton(context.getSource().asPlayer()), new BlockPos(context.getSource().getPos())))
                .then(argument("targets", EntityArgument.players())
                        .executes(context -> setSpawn(context.getSource(), EntityArgument.getPlayers(context, "targets"), new BlockPos(context.getSource().getPos())))
                        .then(argument("pos", BlockPosArgument.blockPos()).executes(context ->
                                setSpawn(context.getSource(), EntityArgument.getPlayers(context, "targets"), BlockPosArgument.getBlockPos(context, "pos"))))));

        dispatcher.register(literal("replaceitem").requires(source -> source.hasPermissionLevel(2))
                .then(literal("block").then(argument("pos", BlockPosArgument.blockPos()).then(argument("slot", MiscArguments.slot()).then(argument("item", ItemArgument.item())
                        .executes(context -> replaceBlockItem(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), context.getArgument("slot", Integer.class),
                                ItemArgument.getItem(context, "item").createStack(1)))
                        .then(argument("count", IntegerArgumentType.integer(1, 64)).executes(context -> replaceBlockItem(context.getSource(),
                                BlockPosArgument.getLoadedBlockPos(context, "pos"), context.getArgument("slot", Integer.class),
                                ItemArgument.getItem(context, "item").createStack(IntegerArgumentType.getInteger(context, "count")))))))))
                .then(literal("entity").then(argument("targets", EntityArgument.entities()).then(argument("slot", MiscArguments.slot()).then(argument("item", ItemArgument.item())
                        .executes(context -> replaceEntityItem(context.getSource(), EntityArgument.getEntities(context, "targets"), context.getArgument("slot", Integer.class),
                                ItemArgument.getItem(context, "item").createStack(1)))
                        .then(argument("count", IntegerArgumentType.integer(1, 64)).executes(context -> replaceEntityItem(context.getSource(),
                                EntityArgument.getEntities(context, "targets"), context.getArgument("slot", Integer.class),
                                ItemArgument.getItem(context, "item").createStack(IntegerArgumentType.getInteger(context, "count"))))))))));
    }

    private static int give(CommandSource source, ItemInput item, Collection<EntityPlayerMP> targets, int count) {
        int maxStack = item.createStack(1).getMaxStackSize();
        for (EntityPlayerMP player : targets) {
            int remaining = count;
            while (remaining > 0) {
                int amount = Math.min(maxStack, remaining);
                remaining -= amount;
                ItemStack stack = item.createStack(amount);
                boolean added = player.inventory.addItemStackToInventory(stack);
                if (added && stack.isEmpty()) {
                    stack.setCount(1);
                    EntityItem fake = player.dropItem(stack, false);
                    if (fake != null) fake.makeFakeItem();
                    player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F,
                            ((player.getRNG().nextFloat() - player.getRNG().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                    player.inventoryContainer.detectAndSendChanges();
                } else {
                    EntityItem drop = player.dropItem(stack, false);
                    if (drop != null) {
                        drop.setNoPickupDelay();
                        drop.setOwner(player.getName());
                    }
                }
            }
        }
        ItemStack display = item.createStack(count);
        if (targets.size() == 1) {
            source.sendFeedback(new TextComponentTranslation("commands.give.success.single", count, display.getTextComponent(), targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendFeedback(new TextComponentTranslation("commands.give.success.multiple", count, display.getTextComponent(), targets.size()), true);
        }
        return targets.size();
    }

    private static int clear(CommandSource source, Collection<EntityPlayerMP> targets, ItemPredicateArgument.Result predicate, int maxCount) throws CommandSyntaxException {
        int total = 0;
        for (EntityPlayerMP player : targets) {
            int[] removed = {0};
            for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
                ItemStack stack = player.inventory.getStackInSlot(slot);
                if (stack.isEmpty() || !predicate.test(stack)) continue;
                int take = maxCount < 0 ? stack.getCount() : Math.min(stack.getCount(), maxCount - removed[0]);
                if (maxCount != 0) stack.shrink(take);
                removed[0] += maxCount == 0 ? stack.getCount() : take;
                if (maxCount > 0 && removed[0] >= maxCount) break;
            }
            ItemStack held = player.inventory.getItemStack();
            if (!held.isEmpty() && predicate.test(held) && (maxCount < 0 || removed[0] < maxCount)) {
                int take = maxCount < 0 ? held.getCount() : Math.min(held.getCount(), maxCount - removed[0]);
                if (maxCount != 0) held.shrink(take);
                removed[0] += maxCount == 0 ? held.getCount() : take;
            }
            player.inventoryContainer.detectAndSendChanges();
            player.updateHeldItem();
            total += removed[0];
        }
        if (total == 0) {
            if (targets.size() == 1) throw CLEAR_FAILED_SINGLE.create(targets.iterator().next().getName());
            throw CLEAR_FAILED_MULTIPLE.create(targets.size());
        }
        String key = maxCount == 0 ? "commands.clear.test" : "commands.clear.success";
        if (targets.size() == 1) source.sendFeedback(new TextComponentTranslation(key + ".single", total, targets.iterator().next().getDisplayName()), true);
        else source.sendFeedback(new TextComponentTranslation(key + ".multiple", total, targets.size()), true);
        return total;
    }

    private static int giveEffect(CommandSource source, Collection<? extends Entity> targets, Potion effect, Integer seconds, int amplifier, boolean showParticles)
            throws CommandSyntaxException {
        int duration = seconds != null ? (effect.isInstant() ? seconds : seconds * 20) : (effect.isInstant() ? 1 : 600);
        int count = 0;
        for (Entity entity : targets) {
            if (!(entity instanceof EntityLivingBase)) continue;
            PotionEffect potionEffect = new PotionEffect(effect, duration, amplifier, false, showParticles);
            EntityLivingBase living = (EntityLivingBase) entity;
            if (!living.isPotionApplicable(potionEffect)) continue;
            living.addPotionEffect(potionEffect);
            count++;
        }
        if (count == 0) throw EFFECT_GIVE_FAILED.create();
        TextComponentTranslation name = new TextComponentTranslation(effect.getName());
        if (targets.size() == 1) source.sendFeedback(new TextComponentTranslation("commands.effect.give.success.single", name, targets.iterator().next().getDisplayName()), true);
        else source.sendFeedback(new TextComponentTranslation("commands.effect.give.success.multiple", name, targets.size()), true);
        return count;
    }

    private static int clearEffects(CommandSource source, Collection<? extends Entity> targets) throws CommandSyntaxException {
        int count = 0;
        for (Entity entity : targets) {
            if (entity instanceof EntityLivingBase && !((EntityLivingBase) entity).getActivePotionEffects().isEmpty()) {
                ((EntityLivingBase) entity).clearActivePotions();
                count++;
            }
        }
        if (count == 0) throw EFFECT_CLEAR_ALL_FAILED.create();
        if (targets.size() == 1) source.sendFeedback(new TextComponentTranslation("commands.effect.clear.everything.success.single", targets.iterator().next().getDisplayName()), true);
        else source.sendFeedback(new TextComponentTranslation("commands.effect.clear.everything.success.multiple", targets.size()), true);
        return count;
    }

    private static int clearEffect(CommandSource source, Collection<? extends Entity> targets, Potion effect) throws CommandSyntaxException {
        int count = 0;
        for (Entity entity : targets) {
            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).removeActivePotionEffect(effect) != null) count++;
        }
        if (count == 0) throw EFFECT_CLEAR_FAILED.create();
        TextComponentTranslation name = new TextComponentTranslation(effect.getName());
        if (targets.size() == 1) source.sendFeedback(new TextComponentTranslation("commands.effect.clear.specific.success.single", name, targets.iterator().next().getDisplayName()), true);
        else source.sendFeedback(new TextComponentTranslation("commands.effect.clear.specific.success.multiple", name, targets.size()), true);
        return count;
    }

    private static int enchant(CommandSource source, Collection<? extends Entity> targets, Enchantment enchantment, int level) throws CommandSyntaxException {
        if (level > enchantment.getMaxLevel()) throw ENCHANT_LEVEL.create(level, enchantment.getMaxLevel());
        int count = 0;
        for (Entity entity : targets) {
            if (!(entity instanceof EntityLivingBase)) {
                if (targets.size() == 1) throw ENCHANT_NOT_LIVING.create(entity.getName());
                continue;
            }
            ItemStack stack = ((EntityLivingBase) entity).getHeldItemMainhand();
            if (stack.isEmpty()) {
                if (targets.size() == 1) throw ENCHANT_NO_ITEM.create(entity.getName());
                continue;
            }
            if (!enchantment.canApply(stack) || !EnchantmentHelper.getEnchantments(stack).keySet().stream().allMatch(other -> other == enchantment || other.isCompatibleWith(enchantment))) {
                if (targets.size() == 1) throw ENCHANT_INCOMPATIBLE.create(stack.getItem().getItemStackDisplayName(stack));
                continue;
            }
            stack.addEnchantment(enchantment, level);
            count++;
        }
        if (count == 0) throw ENCHANT_FAILED.create();
        if (targets.size() == 1) {
            source.sendFeedback(new TextComponentTranslation("commands.enchant.success.single", enchantment.getTranslatedName(level), targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendFeedback(new TextComponentTranslation("commands.enchant.success.multiple", enchantment.getTranslatedName(level), targets.size()), true);
        }
        return count;
    }

    private static int addXp(CommandSource source, Collection<EntityPlayerMP> targets, int amount, boolean levels) {
        for (EntityPlayerMP player : targets) {
            if (levels) player.addExperienceLevel(amount);
            else player.addExperience(amount);
        }
        String key = "commands.experience.add." + (levels ? "levels" : "points") + ".success";
        if (targets.size() == 1) source.sendFeedback(new TextComponentTranslation(key + ".single", amount, targets.iterator().next().getDisplayName()), true);
        else source.sendFeedback(new TextComponentTranslation(key + ".multiple", amount, targets.size()), true);
        return targets.size();
    }

    private static int setXp(CommandSource source, Collection<EntityPlayerMP> targets, int amount, boolean levels) throws CommandSyntaxException {
        int count = 0;
        for (EntityPlayerMP player : targets) {
            if (levels) {
                player.addExperienceLevel(amount - player.experienceLevel);
                count++;
            } else if (amount < player.xpBarCap()) {
                player.experience = 0.0F;
                player.experienceTotal = 0;
                int level = player.experienceLevel;
                player.addExperience(amount);
                player.experienceLevel = level;
                player.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetExperience(player.experience, player.experienceTotal, player.experienceLevel));
                count++;
            }
        }
        if (count == 0) throw XP_POINTS_INVALID.create();
        String key = "commands.experience.set." + (levels ? "levels" : "points") + ".success";
        if (targets.size() == 1) source.sendFeedback(new TextComponentTranslation(key + ".single", amount, targets.iterator().next().getDisplayName()), true);
        else source.sendFeedback(new TextComponentTranslation(key + ".multiple", amount, targets.size()), true);
        return targets.size();
    }

    private static int queryXp(CommandSource source, EntityPlayerMP player, boolean levels) {
        int value = levels ? player.experienceLevel : Math.round(player.experience * player.xpBarCap());
        source.sendFeedback(new TextComponentTranslation("commands.experience.query." + (levels ? "levels" : "points"), player.getDisplayName(), value), false);
        return value;
    }

    private static int setGameMode(CommandSource source, Collection<EntityPlayerMP> targets, GameType type) {
        int count = 0;
        TextComponentTranslation mode = new TextComponentTranslation("gameMode." + type.getName());
        for (EntityPlayerMP player : targets) {
            if (player.interactionManager.getGameType() == type) continue;
            player.setGameType(type);
            if (source.getEntity() == player) source.sendFeedback(new TextComponentTranslation("commands.gamemode.success.self", mode), true);
            else {
                if (source.getServer().worlds[0].getGameRules().getBoolean("sendCommandFeedback")) player.sendMessage(new TextComponentTranslation("gameMode.changed", mode));
                source.sendFeedback(new TextComponentTranslation("commands.gamemode.success.other", player.getDisplayName(), mode), true);
            }
            count++;
        }
        return count;
    }

    private static int setSpawn(CommandSource source, Collection<EntityPlayerMP> targets, BlockPos pos) {
        for (EntityPlayerMP player : targets) player.setSpawnPoint(pos, true);
        if (targets.size() == 1) {
            source.sendFeedback(new TextComponentTranslation("commands.spawnpoint.success.single", pos.getX(), pos.getY(), pos.getZ(), targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendFeedback(new TextComponentTranslation("commands.spawnpoint.success.multiple", pos.getX(), pos.getY(), pos.getZ(), targets.size()), true);
        }
        return targets.size();
    }

    private static int replaceBlockItem(CommandSource source, BlockPos pos, int slot, ItemStack stack) throws CommandSyntaxException {
        TileEntity tile = source.getWorld().getTileEntity(pos);
        if (!(tile instanceof IInventory)) throw REPLACE_BLOCK_FAILED.create();
        IInventory inventory = (IInventory) tile;
        if (slot < 0 || slot >= inventory.getSizeInventory()) throw REPLACE_SLOT_FAILED.create(slot);
        inventory.setInventorySlotContents(slot, stack);
        source.sendFeedback(new TextComponentTranslation("commands.replaceitem.block.success", pos.getX(), pos.getY(), pos.getZ(), stack.getTextComponent()), true);
        return 1;
    }

    private static int replaceEntityItem(CommandSource source, Collection<? extends Entity> targets, int slot, ItemStack stack) throws CommandSyntaxException {
        int count = 0;
        for (Entity entity : targets) {
            if (entity instanceof EntityPlayerMP) ((EntityPlayerMP) entity).inventoryContainer.detectAndSendChanges();
            if (entity.replaceItemInInventory(slot, stack.copy())) {
                count++;
                if (entity instanceof EntityPlayerMP) ((EntityPlayerMP) entity).inventoryContainer.detectAndSendChanges();
            }
        }
        if (count == 0) throw REPLACE_ENTITY_FAILED.create(stack.getTextComponent(), slot);
        if (targets.size() == 1) {
            source.sendFeedback(new TextComponentTranslation("commands.replaceitem.entity.success.single", targets.iterator().next().getDisplayName(), stack.getTextComponent()), true);
        } else {
            source.sendFeedback(new TextComponentTranslation("commands.replaceitem.entity.success.multiple", targets.size(), stack.getTextComponent()), true);
        }
        return count;
    }

    private PlayerCommands() {}
}
