package com.michaelsebero.portium.moderntest;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A Forge 1.13.2 mod for testing Portium's 1.13 mod support; never shipped. Written against Minecraft 1.13.2 by SRG
 * names, as compiled 1.13 mods are. Registers a block through a static mod-bus subscriber and its item and another
 * through a generic listener, and reads them back through object holders.
 */
@Mod("portiumtest")
public class PortiumTest113 {

    private static final Logger LOGGER = LogManager.getLogger("PortiumTest113");

    @ObjectHolder("portiumtest:test_block")
    public static final Block TEST_BLOCK = null;
    @ObjectHolder("portiumtest:test_item")
    public static final Item TEST_ITEM = null;

    public PortiumTest113() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addGenericListener(Item.class, this::registerItems);
        bus.addListener(this::setup);
    }

    private void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                new ItemBlock(TEST_BLOCK, new Item.Properties().func_200916_a(ItemGroup.field_78030_b)).setRegistryName("portiumtest", "test_block"),
                new Item(new Item.Properties().func_200917_a(16).func_200916_a(ItemGroup.field_78026_f).func_208103_a(EnumRarity.RARE))
                        .setRegistryName("portiumtest", "test_item"));
    }

    private void setup(FMLCommonSetupEvent event) {
        ItemStack stack = new ItemStack(TEST_ITEM);
        LOGGER.info("Object holders: block {}, item {}; registry has block {}; item stacks to {} in tab {}, block item {}",
                TEST_BLOCK == null ? null : TEST_BLOCK.getRegistryName(), TEST_ITEM == null ? null : TEST_ITEM.getRegistryName(),
                ForgeRegistries.BLOCKS.containsKey(new ResourceLocation("portiumtest", "test_block")), stack.func_77976_d(),
                TEST_ITEM == null ? null : TEST_ITEM.func_77640_w(), ForgeRegistries.ITEMS.getValue(new ResourceLocation("portiumtest", "test_block")));
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {

        @SubscribeEvent
        public static void registerBlocks(RegistryEvent.Register<Block> event) {
            event.getRegistry().register(new Block(Block.Properties.func_200945_a(Material.field_151576_e).func_200948_a(2.0F, 6.0F))
                    .setRegistryName("portiumtest", "test_block"));
        }
    }
}
