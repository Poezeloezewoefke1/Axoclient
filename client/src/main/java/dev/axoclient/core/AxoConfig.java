package dev.axoclient.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.axoclient.AxoClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Versioned JSON config at config/axoclient.json. Structure:
 * { "configVersion": 1, "modules": { "<id>": { "enabled": bool } } }
 * Per-module free-form settings are added in roadmap task P1-04.
 * Saves are atomic (temp file + move) so a crash never truncates the config.
 */
public final class AxoConfig {
    private static final int CONFIG_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private final JsonObject root;

    private AxoConfig(Path path, JsonObject root) {
        this.path = path;
        this.root = root;
    }

    public static AxoConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("axoclient.json");
        JsonObject root = null;
        if (Files.exists(path)) {
            try {
                root = GSON.fromJson(Files.readString(path), JsonObject.class);
            } catch (IOException | RuntimeException e) {
                AxoClient.LOGGER.warn("Unreadable config at {}, starting fresh", path, e);
            }
        }
        if (root == null || !root.has("configVersion")) {
            root = new JsonObject();
            root.addProperty("configVersion", CONFIG_VERSION);
            root.add("modules", new JsonObject());
        }
        return new AxoConfig(path, root);
    }

    public String getModuleString(String moduleId, String key, String fallback) {
        JsonObject section = moduleSection(moduleId, false);
        try {
            if (section != null && section.has(key)) {
                return section.get(key).getAsString();
            }
        } catch (RuntimeException e) {
            AxoClient.LOGGER.warn("Bad config value {}.{}, using default", moduleId, key);
        }
        return fallback;
    }

    public int getModuleInt(String moduleId, String key, int fallback) {
        JsonObject section = moduleSection(moduleId, false);
        try {
            if (section != null && section.has(key)) {
                return section.get(key).getAsInt();
            }
        } catch (RuntimeException e) {
            AxoClient.LOGGER.warn("Bad config value {}.{}, using default", moduleId, key);
        }
        return fallback;
    }

    public boolean isEnabled(String moduleId, boolean fallback) {
        JsonObject section = moduleSection(moduleId, false);
        if (section == null || !section.has("enabled")) {
            return fallback;
        }
        return section.get("enabled").getAsBoolean();
    }

    public void setEnabled(String moduleId, boolean state) {
        moduleSection(moduleId, true).addProperty("enabled", state);
        save();
    }

    private JsonObject moduleSection(String moduleId, boolean create) {
        JsonObject modules = root.getAsJsonObject("modules");
        if (modules == null) {
            modules = new JsonObject();
            root.add("modules", modules);
        }
        JsonObject section = modules.getAsJsonObject(moduleId);
        if (section == null && create) {
            section = new JsonObject();
            modules.add(moduleId, section);
        }
        return section;
    }

    private void save() {
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(root));
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            AxoClient.LOGGER.error("Failed to save config to {}", path, e);
        }
    }
}
