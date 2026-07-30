package dev.axoclient.gui.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Draws the Axo menu artwork full-screen behind a menu.
 *
 * Every texture-drawing call in the client goes through here. GuiGraphics#blit
 * is one of the least stable signatures in Minecraft — it has changed shape
 * several times inside 1.21 alone — so keeping it to a single call site means a
 * version bump touches one line instead of every screen.
 */
public final class MenuBackground {
    public static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("axoclient", "textures/gui/menu_background.png");

    private static final int TEX_W = 1024;
    private static final int TEX_H = 512;
    /** Darkens the art so white button text stays readable over the bright bits. */
    private static final int SCRIM = 0x66000000;

    private MenuBackground() {
    }

    /**
     * Cover the screen without distorting the art: the source rectangle is
     * cropped to the screen's aspect ratio (the same thing CSS
     * {@code object-fit: cover} does) rather than stretched to fit.
     */
    public static void render(GuiGraphics graphics, int screenW, int screenH) {
        if (screenW <= 0 || screenH <= 0) {
            return;
        }
        float screenAspect = screenW / (float) screenH;
        float texAspect = TEX_W / (float) TEX_H;

        float srcW = TEX_W;
        float srcH = TEX_H;
        if (screenAspect > texAspect) {
            srcH = TEX_W / screenAspect;
        } else {
            srcW = TEX_H * screenAspect;
        }
        float u = (TEX_W - srcW) / 2f;
        float v = (TEX_H - srcH) / 2f;

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TEXTURE,
            0,
            0,
            u,
            v,
            screenW,
            screenH,
            Math.max(1, Math.round(srcW)),
            Math.max(1, Math.round(srcH)),
            TEX_W,
            TEX_H
        );
        graphics.fill(0, 0, screenW, screenH, SCRIM);
    }
}
