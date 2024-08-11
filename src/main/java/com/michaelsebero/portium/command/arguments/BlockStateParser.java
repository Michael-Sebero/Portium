package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.nbt.SnbtParser;
import com.michaelsebero.portium.compat.LegacyBlockStates;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reads {@code name[prop=value,...]{nbt}} (or {@code #tag[...]} when tags are allowed) in 1.13's block syntax. */
public final class BlockStateParser {

    private final StringReader reader;
    private final boolean allowTags;
    @Nullable
    private ResourceLocation id;
    private boolean isTag;
    private final Map<String, String> properties = new LinkedHashMap<>();
    @Nullable
    private NBTTagCompound nbt;

    public BlockStateParser(StringReader reader, boolean allowTags) {
        this.reader = reader;
        this.allowTags = allowTags;
    }

    public BlockStateParser parse(boolean parseNbt) throws CommandSyntaxException {
        int start = this.reader.getCursor();
        if (this.allowTags && this.reader.canRead() && this.reader.peek() == '#') {
            this.reader.skip();
            this.isTag = true;
            this.id = ResourceLocationArgument.read(this.reader);
        } else {
            this.id = ResourceLocationArgument.read(this.reader);
            if (!LegacyBlockStates.exists(this.id.toString())) {
                this.reader.setCursor(start);
                throw CommandExceptions.BLOCK_ID_INVALID.createWithContext(this.reader, this.id.toString());
            }
        }
        if (this.reader.canRead() && this.reader.peek() == '[') this.readProperties();
        if (parseNbt && this.reader.canRead() && this.reader.peek() == '{') this.nbt = new SnbtParser(this.reader).readStruct();
        return this;
    }

    private void readProperties() throws CommandSyntaxException {
        this.reader.skip();
        this.reader.skipWhitespace();
        while (this.reader.canRead() && this.reader.peek() != ']') {
            this.reader.skipWhitespace();
            String key = this.reader.readString();
            this.reader.skipWhitespace();
            this.reader.expect('=');
            this.reader.skipWhitespace();
            this.properties.put(key, this.reader.readString());
            this.reader.skipWhitespace();
            if (this.reader.canRead() && this.reader.peek() == ',') {
                this.reader.skip();
                continue;
            }
            break;
        }
        this.reader.expect(']');
    }

    @Nullable
    public ResourceLocation getId() {
        return this.id;
    }

    public boolean isTag() {
        return this.isTag;
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }

    @Nullable
    public NBTTagCompound getNbt() {
        return this.nbt;
    }
}
