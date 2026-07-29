package dev.axoclient.camera;

/**
 * Shared state for freelook: look around while your body keeps facing — and
 * keeps moving — the way it was.
 *
 * Two mixins cooperate here, both in the optional (non-fatal) config:
 * {@code EntityTurnMixin} swallows the mouse movement and banks it, and
 * {@code CameraMixin} applies the banked rotation to the camera.
 *
 * <h2>Why the alive flag matters</h2>
 * A mixin in the optional config is allowed to fail. If the turn hook applied
 * but the camera hook did not, the mouse would be swallowed with nothing
 * moving in its place — a frozen view, which is worse than the feature simply
 * not existing. So the camera mixin sets {@link #markCameraHookAlive()} the
 * first time it runs, and the turn hook refuses to swallow anything until it
 * has. Half-applied therefore degrades to "freelook does nothing", which is
 * the correct failure.
 */
public final class Freelook {
    private static final float MAX_PITCH = 90.0f;
    /** Matches vanilla's mouse-to-degrees conversion so the feel is identical. */
    private static final double SENSITIVITY_SCALE = 0.15;

    private static boolean active;
    private static boolean cameraHookAlive;
    private static float yaw;
    private static float pitch;

    private Freelook() {
    }

    /** Called by the camera mixin, proving it is applied and running. */
    public static void markCameraHookAlive() {
        cameraHookAlive = true;
    }

    /** True only when freelook is on AND the camera hook is known to work. */
    public static boolean engaged() {
        return active && cameraHookAlive;
    }

    /**
     * Begin freelook from the player's current view, so there is no jump at
     * the moment the key goes down.
     */
    public static void start(float fromYaw, float fromPitch) {
        if (!cameraHookAlive) {
            return;
        }
        yaw = fromYaw;
        pitch = fromPitch;
        active = true;
    }

    public static void stop() {
        active = false;
    }

    /** Bank a mouse movement instead of turning the player. */
    public static void applyMouse(double deltaYaw, double deltaPitch) {
        yaw += (float) (deltaYaw * SENSITIVITY_SCALE);
        pitch += (float) (deltaPitch * SENSITIVITY_SCALE);
        pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch));
    }

    public static float yaw() {
        return yaw;
    }

    public static float pitch() {
        return pitch;
    }
}
