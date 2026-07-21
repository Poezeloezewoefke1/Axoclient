package dev.axoclient.update;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.axoclient.AxoClient;
import dev.axoclient.core.AxoConfig;
import dev.axoclient.gui.notify.Notification;
import dev.axoclient.gui.notify.Notifications;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * In-game update check + staged download. On startup a daemon thread fetches
 * the same axo-manifest.json the launcher uses, compares the manifest's
 * client version against the running mod's version, and — if a newer build
 * exists — downloads the new jar into a staging folder
 * ({@code <gameDir>/axoclient-updates/}) after verifying its sha1, then
 * raises an in-game notification.
 *
 * <p>A running Fabric jar cannot replace itself (the file is locked on
 * Windows while the game runs), so applying the update means a restart: the
 * launcher re-syncs the mods folder from the manifest on its next launch, or
 * a non-launcher user drops the staged jar into {@code mods/} manually. This
 * class does the detection and the download entirely in-game.
 */
public final class UpdateChecker {
    // Kept in sync with the launcher's MANIFEST_URLS (Pages first, raw branch fallback).
    private static final String[] MANIFEST_URLS = {
        "https://poezeloezewoefke1.github.io/Claud/manifest/axo-manifest.json",
        "https://raw.githubusercontent.com/Poezeloezewoefke1/Claud/"
            + "claude/axo-client-architecture-ih525q/manifest/axo-manifest.json"
    };
    private static final String CHANNEL = "stable";
    private static final String STAGING_DIR = "axoclient-updates";
    private static final Gson GSON = new Gson();

    private UpdateChecker() {
    }

    /** Fires a background check unless disabled in config (update.check). */
    public static void runAsync(AxoConfig config) {
        if (!config.getModuleBool("update", "check", true)) {
            return;
        }
        boolean stage = config.getModuleBool("update", "download", true);
        Thread thread = new Thread(() -> check(stage), "AxoUpdateCheck");
        thread.setDaemon(true);
        thread.start();
    }

    private static void check(boolean stage) {
        try {
            String current = currentVersion();
            Optional<Release> latest = fetchLatest();
            if (latest.isEmpty()) {
                return;
            }
            Release release = latest.get();
            if (!Versions.isNewer(release.version(), current)) {
                AxoClient.LOGGER.info("Axo Client is up to date ({})", current);
                return;
            }
            AxoClient.LOGGER.info(
                "Axo Client update available: {} (running {}) — {}",
                release.version(),
                current,
                release.url()
            );

            String message;
            if (stage && stageUpdate(release)) {
                message = "Axo Client " + release.version() + " downloaded — restart to apply";
            } else {
                message = "Update available: Axo Client " + release.version();
            }
            // Toasts touch the render list; raise it on the client thread.
            Minecraft.getInstance()
                .execute(() -> Notifications.push(message, Notification.Type.INFO));
        } catch (RuntimeException e) {
            AxoClient.LOGGER.debug("Update check failed (offline or unreachable): {}", e.toString());
        }
    }

    private static String currentVersion() {
        return FabricLoader.getInstance()
            .getModContainer(AxoClient.MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("0.0.0");
    }

    /**
     * Downloads the release jar into the staging folder, verifying its sha1.
     * Idempotent: a correct staged file is kept and re-used. Returns true when
     * a verified jar is present afterwards.
     */
    private static boolean stageUpdate(Release release) {
        try {
            Path dir = FabricLoader.getInstance().getGameDir().resolve(STAGING_DIR);
            Files.createDirectories(dir);
            Path target = dir.resolve("axoclient-" + release.version() + ".jar");
            if (Files.exists(target) && sha1(Files.readAllBytes(target)).equals(release.sha1())) {
                return true; // already downloaded and verified
            }

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(release.url()))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", "AxoClient")
                .GET()
                .build();
            HttpResponse<byte[]> response =
                client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                return false;
            }
            byte[] bytes = response.body();
            if (!sha1(bytes).equals(release.sha1())) {
                AxoClient.LOGGER.warn("Staged update sha1 mismatch — discarding download");
                return false;
            }
            Path temp = dir.resolve(target.getFileName() + ".part");
            Files.write(temp, bytes);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            AxoClient.LOGGER.info("Staged update jar at {}", target);
            return true;
        } catch (Exception e) {
            AxoClient.LOGGER.debug("Could not stage update: {}", e.toString());
            return false;
        }
    }

    private static String sha1(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        } catch (Exception e) {
            return "";
        }
    }

    /** The manifest's client build for the target channel's default version. */
    private static Optional<Release> fetchLatest() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        for (String url : MANIFEST_URLS) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "AxoClient")
                    .GET()
                    .build();
                HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    continue;
                }
                Optional<Release> parsed = parse(response.body());
                if (parsed.isPresent()) {
                    return parsed;
                }
            } catch (Exception e) {
                // Try the next URL; a failed check is never fatal.
                AxoClient.LOGGER.debug("Manifest URL failed ({}): {}", url, e.toString());
            }
        }
        return Optional.empty();
    }

    private static Optional<Release> parse(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        JsonObject channels = root.getAsJsonObject("channels");
        if (channels == null || !channels.has(CHANNEL)) {
            return Optional.empty();
        }
        JsonObject channel = channels.getAsJsonObject(CHANNEL);
        String defaultId = channel.get("default").getAsString();
        for (var element : channel.getAsJsonArray("versions")) {
            JsonObject version = element.getAsJsonObject();
            if (defaultId.equals(version.get("id").getAsString())) {
                JsonObject clientObj = version.getAsJsonObject("client");
                return Optional.of(
                    new Release(
                        clientObj.get("version").getAsString(),
                        clientObj.get("url").getAsString(),
                        clientObj.get("sha1").getAsString()
                    )
                );
            }
        }
        return Optional.empty();
    }

    private record Release(String version, String url, String sha1) {
    }
}
