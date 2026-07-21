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
import java.time.Duration;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * In-game update check. On startup a daemon thread fetches the same
 * axo-manifest.json the launcher uses, compares the manifest's client
 * version against the running mod's version, and — if a newer build
 * exists — raises an in-game notification with the version and download
 * link. A running Fabric jar cannot replace itself, so applying an update
 * still means a restart (the launcher swaps the jar between runs); this
 * class is the "you're out of date" signal, done entirely in-game.
 */
public final class UpdateChecker {
    // Kept in sync with the launcher's MANIFEST_URLS (Pages first, raw branch fallback).
    private static final String[] MANIFEST_URLS = {
        "https://poezeloezewoefke1.github.io/Claud/manifest/axo-manifest.json",
        "https://raw.githubusercontent.com/Poezeloezewoefke1/Claud/"
            + "claude/axo-client-architecture-ih525q/manifest/axo-manifest.json"
    };
    private static final String CHANNEL = "stable";
    private static final Gson GSON = new Gson();

    private UpdateChecker() {
    }

    /** Fires a background check unless disabled in config (update.check). */
    public static void runAsync(AxoConfig config) {
        if (!config.getModuleBool("update", "check", true)) {
            return;
        }
        Thread thread = new Thread(UpdateChecker::check, "AxoUpdateCheck");
        thread.setDaemon(true);
        thread.start();
    }

    private static void check() {
        try {
            String current = currentVersion();
            Optional<Release> latest = fetchLatest();
            if (latest.isEmpty()) {
                return;
            }
            Release release = latest.get();
            if (isNewer(release.version, current)) {
                AxoClient.LOGGER.info(
                    "Axo Client update available: {} (running {}) — {}",
                    release.version,
                    current,
                    release.url
                );
                // Toasts touch the render list; raise it on the client thread.
                Minecraft.getInstance()
                    .execute(
                        () ->
                            Notifications.push(
                                "Update available: Axo Client " + release.version,
                                Notification.Type.INFO
                            )
                    );
            } else {
                AxoClient.LOGGER.info("Axo Client is up to date ({})", current);
            }
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
                    new Release(clientObj.get("version").getAsString(), clientObj.get("url").getAsString())
                );
            }
        }
        return Optional.empty();
    }

    /** True when {@code candidate} is a strictly higher dotted version than {@code current}. */
    static boolean isNewer(String candidate, String current) {
        int[] a = parseVersion(candidate);
        int[] b = parseVersion(current);
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int av = i < a.length ? a[i] : 0;
            int bv = i < b.length ? b[i] : 0;
            if (av != bv) {
                return av > bv;
            }
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        // Numeric dotted parts only; drop any build/pre-release suffix (e.g. "1.2.0+build").
        String core = version.split("[-+]", 2)[0];
        String[] parts = core.split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }

    private record Release(String version, String url) {
    }
}
