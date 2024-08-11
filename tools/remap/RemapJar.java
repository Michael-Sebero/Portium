import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Renames an obfuscated Minecraft jar to SRG names with an MCPConfig tsrg file, for compiling code against a newer
 * version's API by the names mods use. With an MCP mappings zip as well, SRG names become MCP's readable names, as
 * MCP Reborn does for reading the code. Usage: RemapJar IN.jar MCPCONFIG.zip OUT.jar [MCP_MAPPINGS.zip] (needs ASM 9).
 */
public class RemapJar {

    public static void main(String[] args) throws Exception {
        Map<String, String> mapping = new HashMap<>();
        String owner = null;
        try (ZipFile config = new ZipFile(args[1])) {
            String tsrg = read(config.getInputStream(config.getEntry("config/joined.tsrg")));
            for (String line : tsrg.split("\n")) {
                if (line.isEmpty()) continue;
                String[] parts = line.trim().split(" ");
                if (!line.startsWith("\t")) {
                    owner = parts[0];
                    mapping.put(parts[0], parts[1]);
                } else if (parts.length == 2) {
                    mapping.put(owner + "." + parts[0], parts[1]);
                } else {
                    mapping.put(owner + "." + parts[0] + parts[1], parts[2]);
                }
            }
        }
        if (args.length > 3) readable(mapping, args[3]);
        SimpleRemapper remapper = new SimpleRemapper(mapping);
        int count = 0;
        try (ZipFile in = new ZipFile(args[0]); ZipOutputStream out = new ZipOutputStream(new FileOutputStream(args[2]))) {
            Enumeration<? extends ZipEntry> entries = in.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                ClassReader reader = new ClassReader(readBytes(in.getInputStream(entry)));
                ClassWriter writer = new ClassWriter(0);
                reader.accept(new ClassRemapper(writer, remapper), 0);
                String name = remapper.map(reader.getClassName());
                out.putNextEntry(new ZipEntry((name == null ? reader.getClassName() : name) + ".class"));
                out.write(writer.toByteArray());
                out.closeEntry();
                count++;
            }
        }
        System.out.println("Remapped " + count + " classes to " + args[2]);
    }

    /** Replaces SRG method and field names with MCP's, from the mappings zip's methods.csv and fields.csv. */
    private static void readable(Map<String, String> mapping, String zip) throws Exception {
        Map<String, String> names = new HashMap<>();
        try (ZipFile mappings = new ZipFile(zip)) {
            for (String csv : new String[] {"methods.csv", "fields.csv"}) {
                ZipEntry entry = mappings.getEntry(csv);
                if (entry == null) continue;
                String[] lines = read(mappings.getInputStream(entry)).split("\n");
                for (int i = 1; i < lines.length; i++) {
                    String[] columns = lines[i].split(",", 3);
                    if (columns.length >= 2) names.put(columns[0], columns[1]);
                }
            }
        }
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String name = names.get(entry.getValue());
            if (name != null) entry.setValue(name);
        }
    }

    private static String read(InputStream stream) throws Exception {
        return new String(readBytes(stream), StandardCharsets.UTF_8);
    }

    private static byte[] readBytes(InputStream stream) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int n; (n = stream.read(buffer)) > 0; ) out.write(buffer, 0, n);
        stream.close();
        return out.toByteArray();
    }
}
