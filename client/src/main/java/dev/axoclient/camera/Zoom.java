package dev.axoclient.camera;

/**
 * Shared zoom state, eased on wall-clock time and read once per frame by
 * {@code FovMixin}.
 *
 * <h2>Why not just set options.fov()</h2>
 * The original zoom eased on the client tick and wrote {@code options.fov()},
 * which is an <em>integer</em> option. Two things made that visibly steppy:
 * the ramp only advanced 20 times a second, and every intermediate value was
 * rounded to a whole degree. Going 70 -> 30 in four ticks is four jumps of
 * ten degrees, which reads as a stutter rather than a zoom.
 *
 * Hooking the renderer's field-of-view lets us return a double that moves
 * every frame instead, so the ramp is as smooth as the frame rate allows and
 * the player's saved FOV is never written to at all.
 *
 * <h2>Degrading when the hook is missing</h2>
 * FovMixin lives in the optional config and is allowed to fail. Until it has
 * run once {@link #hookAlive()} stays false, and {@link
 * dev.axoclient.modules.qol.ZoomModule} falls back to the old option-writing
 * path — steppy, but working. So a renderer change costs smoothness, not the
 * feature.
 */
public final class Zoom {
    /** Below this the ramp is treated as finished, avoiding endless tiny steps. */
    private static final double EPSILON = 0.0005;

    private static boolean hookAlive;
    private static boolean active;
    private static double progress;
    private static long lastNanos;

    private static int zoomFov = 30;
    private static int easeMillis = 200;

    private Zoom() {
    }

    /** Called by the FOV hook, proving it is applied and running. */
    public static void markHookAlive() {
        hookAlive = true;
    }

    public static boolean hookAlive() {
        return hookAlive;
    }

    public static void configure(int fov, int millis) {
        zoomFov = fov;
        easeMillis = Math.max(1, millis);
    }

    public static void setActive(boolean value) {
        active = value;
    }

    /** True while the ramp is anywhere other than fully zoomed out. */
    public static boolean affectingFov() {
        return active || progress > 0.0;
    }

    public static void reset() {
        active = false;
        progress = 0.0;
        lastNanos = 0L;
    }

    /**
     * Advance the ramp and blend {@code baseFov} toward the zoom FOV.
     *
     * Time-based rather than frame-based so the ramp takes the same wall-clock
     * duration at 30fps and 240fps.
     */
    public static double apply(double baseFov) {
        advance();
        if (progress <= EPSILON) {
            return baseFov;
        }
        return baseFov + (zoomFov - baseFov) * ease(progress);
    }

    private static void advance() {
        long now = System.nanoTime();
        if (lastNanos == 0L) {
            lastNanos = now;
        }
        // Clamp the delta so a lag spike or a paused window doesn't teleport
        // the ramp to its end the moment rendering resumes.
        double deltaMs = Math.min(250.0, (now - lastNanos) / 1_000_000.0);
        lastNanos = now;

        double step = deltaMs / easeMillis;
        progress = active ? Math.min(1.0, progress + step) : Math.max(0.0, progress - step);
    }

    /** Ease-in-out cubic: no hard start or stop at either end of the ramp. */
    private static double ease(double t) {
        return t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3) / 2.0;
    }
}
