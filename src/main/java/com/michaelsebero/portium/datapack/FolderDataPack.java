package com.michaelsebero.portium.datapack;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** A data pack that is an unzipped folder. */
public class FolderDataPack extends DataPack {

    private final File root;

    public FolderDataPack(String id, File root) {
        super(id);
        this.root = root;
    }

    @Nullable
    @Override
    public InputStream open(String path) throws IOException {
        File file = new File(this.root, path);
        return file.isFile() ? new FileInputStream(file) : null;
    }

    @Override
    public Collection<ResourceLocation> list(String folder, String extension) {
        List<ResourceLocation> ids = new ArrayList<>();
        File[] namespaces = new File(this.root, "data").listFiles(File::isDirectory);
        if (namespaces == null) return ids;
        for (File namespace : namespaces) {
            File base = new File(namespace, folder);
            this.walk(base, "data/" + namespace.getName() + "/" + folder, folder, extension, ids);
        }
        return ids;
    }

    private void walk(File dir, String path, String folder, String extension, List<ResourceLocation> out) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            String childPath = path + "/" + child.getName();
            if (child.isDirectory()) {
                this.walk(child, childPath, folder, extension, out);
            } else {
                ResourceLocation id = idFromPath(childPath, folder, extension);
                if (id != null) out.add(id);
            }
        }
    }
}
