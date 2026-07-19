package dev.axoclient.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

/**
 * Raw GLFW key polling. Interim input path until configurable keybinds
 * land (roadmap P1-05 rebinding); modules poll fixed defaults for now.
 */
public final class Keys {
    private Keys() {
    }

    public static boolean isDown(int glfwKey) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), glfwKey);
    }
}
