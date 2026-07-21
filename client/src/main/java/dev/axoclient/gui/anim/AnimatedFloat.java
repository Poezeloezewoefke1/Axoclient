package dev.axoclient.gui.anim;

/**
 * Time-based eased value for smooth UI transitions (hover, toggle, panel
 * expand). Frame-rate independent: it advances toward its target using the
 * real elapsed time, so animations look the same at any FPS.
 */
public final class AnimatedFloat {
    private float value;
    private float target;
    private final float speed; // units per second toward target (1.0 = full range/sec)
    private long lastMs = System.currentTimeMillis();

    public AnimatedFloat(float initial, float speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    /** Advance and return the current eased value. Call once per frame. */
    public float update() {
        long now = System.currentTimeMillis();
        float dt = (now - lastMs) / 1000.0f;
        lastMs = now;
        // Exponential smoothing toward the target — smooth and stable.
        float factor = 1.0f - (float) Math.exp(-speed * dt * 6.0);
        value += (target - value) * Math.max(0f, Math.min(1f, factor));
        if (Math.abs(target - value) < 0.001f) {
            value = target;
        }
        return value;
    }

    public float value() {
        return value;
    }
}
