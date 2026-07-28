package dev.axoclient.modules.cosmetic;

import com.mojang.blaze3d.platform.NativeImage;
import dev.axoclient.AxoClient;
import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.cosmetic.Capes;
import dev.axoclient.gui.notify.Notifications;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;

/**
 * Wear your own cape image.
 *
 * Drop a 64×32 PNG into <game dir>/axoclient/capes/ and enable this module.
 * The first PNG in the folder (alphabetically) is used, so naming one
 * "1-favourite.png" is enough to pick it.
 *
 * Local-only, like every other Axo cosmetic: other players see your normal
 * skin. Showing it to anyone else would need a server to host the texture,
 * which Axo deliberately does not have.
 */
public final class CustomCapeModule extends AxoModule {
    private static final int EXPECTED_WIDTH = 64;
    private static final int EXPECTED_HEIGHT = 32;

    private ClientAsset.ResourceTexture cape;
    /** Bumped per load so a re-enable after editing the file isn't served the old texture. */
    private int generation;

    public CustomCapeModule() {
        super("cape_custom", "Custom Cape", ModuleCategory.COSMETIC, false);
    }

    /** <game dir>/axoclient/capes — created on first enable so it's easy to find. */
    public static Path capeFolder() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("axoclient").resolve("capes");
    }

    @Override
    protected void onEnable() {
        Path folder = capeFolder();
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            Notifications.info("Could not create " + folder);
            return;
        }

        Path file = firstPng(folder);
        if (file == null) {
            Notifications.info("Put a 64x32 PNG in .minecraft/axoclient/capes");
            return;
        }

        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            if (image.getWidth() != EXPECTED_WIDTH || image.getHeight() != EXPECTED_HEIGHT) {
                Notifications.info(
                    "Cape must be " + EXPECTED_WIDTH + "x" + EXPECTED_HEIGHT
                        + " (yours is " + image.getWidth() + "x" + image.getHeight() + ")"
                );
                image.close();
                return;
            }
            generation++;
            Identifier location = Identifier.fromNamespaceAndPath(
                AxoClient.MOD_ID,
                "textures/capes/custom_" + generation + ".png"
            );
            // Registering under this id means the texture manager hands our
            // image back instead of trying to load one from a resource pack.
            Minecraft.getInstance().getTextureManager()
                .register(location, new DynamicTexture(location::toString, image));
            cape = new ClientAsset.ResourceTexture(location, location);
            Capes.set(cape);
            Notifications.success("Wearing " + file.getFileName());
        } catch (IOException e) {
            AxoClient.LOGGER.warn("Could not read custom cape {}", file, e);
            Notifications.info("That PNG could not be read");
        }
    }

    @Override
    protected void onDisable() {
        Capes.clear(cape);
        cape = null;
    }

    /** First .png in the folder, alphabetically, or null when there is none. */
    private static Path firstPng(Path folder) {
        try (Stream<Path> entries = Files.list(folder)) {
            List<Path> pngs = entries
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .toList();
            return pngs.isEmpty() ? null : pngs.get(0);
        } catch (IOException e) {
            return null;
        }
    }
}
