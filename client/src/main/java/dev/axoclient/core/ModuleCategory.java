package dev.axoclient.core;

public enum ModuleCategory {
    PVP("PvP"),
    PERFORMANCE("Performance"),
    QOL("Quality of Life"),
    HUD("HUD");

    private final String displayName;

    ModuleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
