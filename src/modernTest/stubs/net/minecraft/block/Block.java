package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraftforge.registries.ForgeRegistryEntry;

/**
 * Compile-only stub of Forge's patched 1.13.2 {@code Block}, which the unpatched jar lacks the registry entry parts
 * of. Only what the test mod uses; left out of its jar.
 */
public class Block extends ForgeRegistryEntry<Block> {

    public Block(Properties properties) {
        throw new UnsupportedOperationException();
    }

    public static class Properties {

        public static Properties func_200945_a(Material material) {
            throw new UnsupportedOperationException();
        }

        public Properties func_200948_a(float hardness, float resistance) {
            throw new UnsupportedOperationException();
        }
    }
}
