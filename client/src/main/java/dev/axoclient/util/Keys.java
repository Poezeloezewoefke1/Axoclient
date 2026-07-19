package dev.axoclient.util;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * Raw GLFW key polling. Interim input path until configurable keybinds
 * land (roadmap P1-05 rebinding); modules poll fixed defaults for now.
 * Uses the current GLFW context as the window handle — callers run on
 * the render thread (client tick), where the game window is current.
 * 1.21.11 mojmap removed Window#getWindow(); verify this path in the
 * P1-01 runClient check.
 */
public final class Keys {
    private Keys() {
    }

    public static boolean isDown(int glfwKey) {
        return InputConstants.isKeyDown(GLFW.glfwGetCurrentContext(), glfwKey);
    }
}
