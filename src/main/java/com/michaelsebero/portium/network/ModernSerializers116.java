package com.michaelsebero.portium.network;

import com.michaelsebero.portium.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.DataSerializerEntry;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * 1.14+'s optional entity data (a mob's owner, home or carried block), which holds Java's {@code Optional} where 1.12's
 * serializers hold Guava's: serializers of Java's kind, synced as Forge's added serializers are.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModernSerializers116 {

    public static final DataSerializer<Optional<UUID>> OPTIONAL_UUID = new Base<Optional<UUID>>() {
        @Override
        public void write(PacketBuffer buf, Optional<UUID> value) {
            buf.writeBoolean(value.isPresent());
            value.ifPresent(buf::writeUniqueId);
        }

        @Override
        public Optional<UUID> read(PacketBuffer buf) {
            return buf.readBoolean() ? Optional.of(buf.readUniqueId()) : Optional.empty();
        }
    };

    public static final DataSerializer<Optional<BlockPos>> OPTIONAL_BLOCK_POS = new Base<Optional<BlockPos>>() {
        @Override
        public void write(PacketBuffer buf, Optional<BlockPos> value) {
            buf.writeBoolean(value.isPresent());
            value.ifPresent(buf::writeBlockPos);
        }

        @Override
        public Optional<BlockPos> read(PacketBuffer buf) {
            return buf.readBoolean() ? Optional.of(buf.readBlockPos()) : Optional.empty();
        }
    };

    public static final DataSerializer<Optional<IBlockState>> OPTIONAL_BLOCK_STATE = new Base<Optional<IBlockState>>() {
        @Override
        public void write(PacketBuffer buf, Optional<IBlockState> value) {
            buf.writeVarInt(value.map(Block::getStateId).orElse(0));
        }

        @Override
        public Optional<IBlockState> read(PacketBuffer buf) {
            int id = buf.readVarInt();
            return id == 0 ? Optional.empty() : Optional.of(Block.getStateById(id));
        }
    };

    public static final DataSerializer<Optional<ITextComponent>> OPTIONAL_TEXT = new Base<Optional<ITextComponent>>() {
        @Override
        public void write(PacketBuffer buf, Optional<ITextComponent> value) {
            buf.writeBoolean(value.isPresent());
            value.ifPresent(buf::writeTextComponent);
        }

        @Override
        public Optional<ITextComponent> read(PacketBuffer buf) throws IOException {
            return buf.readBoolean() ? Optional.of(buf.readTextComponent()) : Optional.empty();
        }
    };

    private abstract static class Base<T> implements DataSerializer<T> {

        @Override
        public DataParameter<T> createKey(int id) {
            return new DataParameter<>(id, this);
        }

        @Override
        public T copyValue(T value) {
            return value;
        }
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<DataSerializerEntry> event) {
        event.getRegistry().register(new DataSerializerEntry(OPTIONAL_UUID).setRegistryName(new ResourceLocation(Tags.MOD_ID, "optional_uuid_116")));
        event.getRegistry().register(new DataSerializerEntry(OPTIONAL_BLOCK_POS).setRegistryName(new ResourceLocation(Tags.MOD_ID, "optional_block_pos_116")));
        event.getRegistry().register(new DataSerializerEntry(OPTIONAL_BLOCK_STATE).setRegistryName(new ResourceLocation(Tags.MOD_ID, "optional_block_state_116")));
        event.getRegistry().register(new DataSerializerEntry(OPTIONAL_TEXT).setRegistryName(new ResourceLocation(Tags.MOD_ID, "optional_text_116")));
    }

    private ModernSerializers116() {}
}
