package dev.axoclient.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

/**
 * Raw GLFW key polling. Interim input path until configurable keybinds
 * land (roadmap P1-05 rebinding); modules poll fixed defaults for now.
 * 1.21.11's InputConstants.isKeyDown takes the Window object directly.
 */
public final class Keys {
    private Keys() {
    }

    public static boolean isDown(int glfwKey) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), glfwKey);
    }
}
