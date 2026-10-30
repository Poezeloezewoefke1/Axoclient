package dev.axoclient.gui.menu;

import dev.axoclient.core.AxoModule;

/**
 * The line icons in textures/gui/menu_icons.png, in sheet order (8 per row,
 * 64px cells). The sheet is rendered from the design's own SVG paths.
 */
enum MenuIcon {
    LOGO,
    NAV_MODULES,
    NAV_COSMETICS,
    NAV_WAYPOINTS,
    NAV_THEMES,
    NAV_SETTINGS,
    SEARCH,
    LIST,
    GRID,
    SLIDERS,
    CHEVRON_RIGHT,
    CHEVRON_LEFT,
    PLUS,
    CHECK,
    EYE,
    EYE_OFF,
    TRASH,
    COPY,
    M_TAB,
    M_PING,
    M_LIGHT,
    M_CHAT,
    M_SPRINT,
    M_COORDS,
    M_KEYS,
    M_FPS,
    M_ARMOR,
    M_ZOOM,
    M_FREELOOK,
    M_CPS,
    M_CROSSHAIR,
    M_SWORD,
    M_CLOCK,
    M_SERVER,
    M_PIN,
    M_HEART,
    M_SUN,
    M_BELL,
    M_MONITOR,
    M_ACTIVITY,
    M_SHIRT,
    M_SLIDERS,
    M_MAP,
    M_BOX;

    /** The icon for a module card: the design's own where it has one, else by kind. */
    static MenuIcon of(AxoModule module) {
        String id = module.id();
        switch (id) {
            case "fps_hud", "fps_graph", "memory_hud", "entity_count" -> {
                return M_FPS;
            }
            case "ping_hud", "ping_spike", "ping_graph", "tps_hud" -> {
                return M_PING;
            }
            case "coordinates", "nether_coords", "copy_coords", "direction_hud" -> {
                return M_COORDS;
            }
            case "keystrokes" -> {
                return M_KEYS;
            }
            case "cps_counter" -> {
                return M_CPS;
            }
            case "zoom" -> {
                return M_ZOOM;
            }
            case "freelook" -> {
                return M_FREELOOK;
            }
            case "crosshair" -> {
                return M_CROSSHAIR;
            }
            case "fullbright", "light_level" -> {
                return M_LIGHT;
            }
            case "toggle_sprint", "auto_walk", "speed_hud", "movement_state" -> {
                return M_SPRINT;
            }
            case "module_list", "pickup_log", "nearby_players" -> {
                return M_TAB;
            }
            case "server_info_hud" -> {
                return M_SERVER;
            }
            case "armor_hud", "armor_icons", "armor_points", "durability_warning", "durability_tooltip", "totem_warning" -> {
                return M_ARMOR;
            }
            case "chat_timestamps", "chat_anti_spam", "chat_history", "chat_mentions" -> {
                return M_CHAT;
            }
            case "time_hud", "clock_hud", "stopwatch", "uptime_hud", "day_counter", "day_cycle",
                 "potion_hud", "effect_expiry", "combat_timer", "session_stats" -> {
                return M_CLOCK;
            }
            case "waypoints", "death_coords", "chunk_info", "compass_hud", "biome_hud", "depth_guide" -> {
                return M_PIN;
            }
            case "minimap" -> {
                return M_MAP;
            }
            case "health_hud", "hunger_hud", "saturation_hud", "low_health_warning", "hunger_warning" -> {
                return M_HEART;
            }
            case "weather", "sleep_reminder" -> {
                return M_SUN;
            }
            case "break_reminder", "stutter_alert", "fall_warning", "explosion_warning", "inventory_full" -> {
                return M_BELL;
            }
            case "shulker_tooltip", "item_counter", "food_tooltip", "anvil_cost_tooltip" -> {
                return M_BOX;
            }
            default -> {
                // fall through to the category icon
            }
        }
        return switch (module.category()) {
            case HUD -> M_MONITOR;
            case PVP -> M_SWORD;
            case PERFORMANCE -> M_ACTIVITY;
            case COSMETIC -> M_SHIRT;
            case QOL -> M_SLIDERS;
        };
    }
}
