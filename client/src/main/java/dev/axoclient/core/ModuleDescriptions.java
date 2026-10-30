package dev.axoclient.core;

import java.util.Map;

/**
 * One-line descriptions shown when hovering a module card in the menu.
 *
 * Kept in one table rather than spread over every module class so the
 * wording reads as one voice and can be reviewed in a single place. A module
 * missing from here simply shows no description.
 */
public final class ModuleDescriptions {
    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
        // HUD
        Map.entry("fps_hud", "Frames per second."),
        Map.entry("coordinates", "Your X / Y / Z position and facing."),
        Map.entry("cps_counter", "Left and right clicks per second."),
        Map.entry("time_hud", "Real-world time, so you don't have to alt-tab."),
        Map.entry("clock_hud", "In-game time of day as a 24-hour clock."),
        Map.entry("module_list", "Lists the modules you have switched on."),
        Map.entry("memory_hud", "How much memory the game is using."),
        Map.entry("speed_hud", "How fast you're moving, in blocks per second."),
        Map.entry("ping_hud", "Your latency to the server."),
        Map.entry("day_counter", "Which in-game day it is."),
        Map.entry("uptime_hud", "How long this session has been running."),
        Map.entry("health_hud", "Exact health instead of counting hearts."),
        Map.entry("hunger_hud", "Exact food level instead of counting drumsticks."),
        Map.entry("experience_hud", "Your experience level as a number."),
        Map.entry("direction_hud", "Facing as N / E / S / W plus the exact angle."),
        Map.entry("armor_hud", "Durability left on each armour piece."),
        Map.entry("armor_icons", "Armour and held items as icons with durability left."),
        Map.entry("potion_hud", "Active potion effects and time remaining."),
        Map.entry("xp_progress_hud", "Percentage of the way to your next level."),
        Map.entry("server_info_hud", "Which server you're on and how many are online."),
        Map.entry("ping_spike", "Flags the moment your latency jumps."),
        Map.entry("durability_warning", "Warns before a tool or armour piece breaks."),
        Map.entry("death_coords", "Where you last died, so you can get your stuff back."),
        Map.entry("tps_hud", "How well the server is keeping up (20 is perfect)."),
        Map.entry("compass_hud", "A compass strip across the top of the screen."),
        Map.entry("biome_hud", "The biome you're standing in."),
        Map.entry("pickup_log", "A short list of what you just picked up."),
        Map.entry("crosshair", "A custom crosshair in your accent colour."),
        Map.entry("light_level", "Light where you stand; says when mobs can spawn."),
        Map.entry("nether_coords", "Your matching position in the Nether or Overworld."),
        Map.entry("session_stats", "Distance travelled and deaths this session."),
        Map.entry("chunk_info", "Your chunk, position inside it and region file."),
        Map.entry("weather", "Clear, rain or thunder, even when underground."),
        Map.entry("day_cycle", "Real-time countdown to nightfall or sunrise."),
        Map.entry("block_info", "Name and position of the block you're looking at."),
        Map.entry("hostile_counter", "How many hostile mobs are close by."),
        Map.entry("depth_guide", "Your Y level and which ores are richest there."),
        Map.entry("fall_warning", "How far you're falling and the worst-case damage."),
        Map.entry("inventory_full", "Warns when there's no free inventory slot left."),
        Map.entry("saturation_hud", "The hidden food stat that decides when you get hungry."),
        Map.entry("fps_graph", "Frame-time graph; stutters show up as spikes."),
        Map.entry("elytra_info", "While gliding: speed, pitch and elytra durability."),
        Map.entry("mount_info", "What you're riding, its real speed and health."),
        Map.entry("movement_state", "Whether you're sprinting, sneaking, swimming or flying."),
        Map.entry("hunger_warning", "Tells you to eat before you can't sprint."),
        Map.entry("armor_points", "Total armour and armour toughness."),
        Map.entry("hud_style", "Colour, background box and shadow for all HUD text."),
        Map.entry("minimap", "A north-up map of the area around you, with your waypoints."),
        // Cosmetic
        Map.entry("trails", "Particle trails and auras; pick one in settings."),
        Map.entry("halo", "A ring turning above your head."),
        Map.entry("wings", "Two arcs behind your shoulders that follow your facing."),
        Map.entry("capes", "Ten built-in cape colours; pick one in settings."),
        Map.entry("cape_custom", "Your own 64x32 PNG from the axoclient/capes folder."),
        // PvP
        Map.entry("keystrokes", "WASD and mouse buttons as an on-screen overlay."),
        Map.entry("target_hud", "Name, health and distance of what you're aiming at."),
        Map.entry("attack_cooldown", "How charged your next hit is."),
        Map.entry("damage_numbers", "Floating numbers for the damage you deal."),
        Map.entry("combo_counter", "Hits landed in a row on the same target."),
        Map.entry("reach_display", "How far away your last hit landed from."),
        Map.entry("item_counter", "Totems, pearls, golden apples and arrows left."),
        Map.entry("low_health_warning", "A pulsing red screen edge when your health is low."),
        Map.entry("bow_charge", "How drawn your bow is; FULL means a critical shot."),
        Map.entry("nearby_players", "The closest players, with distance, health and ping."),
        Map.entry("hit_sound", "A sound only you hear when your hit lands."),
        Map.entry("pearl_cooldown", "Time left before you can throw another pearl."),
        Map.entry("totem_warning", "Warns when no totem is in hand, and when one gets used."),
        Map.entry("kill_counter", "Kills this session, with players counted separately."),
        Map.entry("combat_timer", "Counts down from your last hit, to match a server's combat tag."),
        // Performance
        Map.entry("performance_mode", "Lighter video settings in one switch; yours come back when off."),
        Map.entry("stutter_alert", "A notification when a single frame freezes."),
        Map.entry("ping_graph", "Your ping over the last minute, so spikes stand out."),
        Map.entry("entity_count", "How many entities your client has loaded."),
        // Quality of life
        Map.entry("fullbright", "See in the dark without changing your video settings."),
        Map.entry("zoom", "Hold a key to zoom in, eased smoothly."),
        Map.entry("toggle_sprint", "Sprint stays on without holding the key."),
        Map.entry("copy_coords", "Copy your position to the clipboard with one key."),
        Map.entry("chat_timestamps", "A dim [HH:mm] in front of each chat line."),
        Map.entry("chat_anti_spam", "Hides a message repeated straight after itself."),
        Map.entry("chat_mentions", "Highlights and pings chat lines that say your name."),
        Map.entry("chat_history", "Remembers messages you sent across restarts."),
        Map.entry("freelook", "Hold a key to look around while moving the same way."),
        Map.entry("clear_liquids", "Removes the full-screen water and lava overlays."),
        Map.entry("durability_tooltip", "Exact durability on item tooltips, coloured by wear."),
        Map.entry("shulker_tooltip", "Shows what's inside a shulker box on its tooltip."),
        Map.entry("food_tooltip", "Hunger and saturation each food restores."),
        Map.entry("anvil_cost_tooltip", "An item's anvil work penalty, before it gets Too Expensive."),
        Map.entry("waypoints", "Mark spots with a key and follow an arrow back."),
        Map.entry("stopwatch", "A start / pause / reset timer on your HUD."),
        Map.entry("auto_walk", "Keeps walking forward for you; bind a key to toggle."),
        Map.entry("break_reminder", "A gentle reminder to take a break every so often."),
        Map.entry("sleep_reminder", "Tells you the moment beds start working."),
        Map.entry("effect_expiry", "A heads-up 10 seconds before a potion effect ends.")
    );

    private ModuleDescriptions() {
    }

    /** The description for a module id, or "" when there is none. */
    public static String of(String moduleId) {
        return DESCRIPTIONS.getOrDefault(moduleId, "");
    }
}
