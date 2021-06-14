package net.cavoj.skinpls;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import com.mojang.authlib.properties.Property;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DataManager {
    private static Path getRoot() {
        Path skinplsDir = FabricLoader.getInstance().getGameDir().resolve("skinpls");

        if (!Files.exists(skinplsDir)) {
            try {
                Files.createDirectories(skinplsDir);
            } catch (IOException e) {
                throw new RuntimeException("Creating skinpls directory", e);
            }
        }

        return skinplsDir;
    }

    private static Optional<Property> getTextures(String filename) {
        Path file = getRoot().resolve(filename + ".toml");

        if (!Files.exists(file)) {
            if (filename.equals("default")) {
                return Optional.empty();
            }
            return getTextures("default");
        }

        Toml toml = new Toml().read(file.toFile());
        String value = toml.getString("value");
        String signature = toml.getString("signature");

        return Optional.of(new Property("textures", value, signature));
    }

    public static Optional<Property> getTextures(UUID uuid) {
        return getTextures(uuid.toString());
    }

    public static void writeData(UUID uuid, String value, String signature) {
        TomlWriter writer = new TomlWriter();
        Map<String, Object> map = new HashMap<>();
        map.put("value", value);
        map.put("signature", signature);
        try {
            writer.write(map, getRoot().resolve(uuid.toString() + ".toml").toFile());
        } catch (IOException e) {
            throw new RuntimeException("Write skin config", e);
        }
    }
}
