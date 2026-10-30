package dev.axoclient.modules.qol;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.WaypointsScreen;
import dev.axoclient.gui.notify.Notification;
import dev.axoclient.gui.notify.Notifications;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import dev.axoclient.input.Keybinds;
import dev.axoclient.util.Keys;
import dev.axoclient.util.WorldInfo;
import dev.axoclient.waypoint.Waypoint;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

/**
 * Mark a spot with one key, then follow an arrow and a distance back to it.
 *
 * HUD-only on purpose: no beam or marker in the world, so no world rendering
 * and no mixin. Waypoints are kept per server or save and per dimension, so
 * the list you cycle through is only ever the one for where you stand.
 *
 * Config (module "waypoints"):
 *   points     — every saved waypoint, encoded by {@link Waypoint#encode}
 *   mark_key   — save your current position
 *   next_key   — cycle which waypoint the HUD points at
 *   remove_key — delete the waypoint the HUD points at (unbound by default)
 *   list_key   — open the waypoint list to rename, point to or delete
 *   mark_deaths — keep a "Death" waypoint where you last died (on by default)
 */
public final class WaypointsModule extends HudModule {
    private static final int DEFAULT_MARK_KEY = GLFW.GLFW_KEY_B;
    private static final int DEFAULT_NEXT_KEY = GLFW.GLFW_KEY_N;
    private static final int DEFAULT_LIST_KEY = GLFW.GLFW_KEY_J;
    /** Height difference below which the HUD doesn't bother showing it. */
    private static final int SHOW_DY_FROM = 3;
    private static final String DEATH_NAME = "Death";

    private final List<Waypoint> points = new ArrayList<>();
    private int selected;
    private boolean markWasDown;
    private boolean nextWasDown;
    private boolean removeWasDown;
    private boolean listWasDown;
    private boolean wasDead;

    public WaypointsModule() {
        super("waypoints", "Waypoints", ModuleCategory.QOL, new HudPosition(HudAnchor.TOP_CENTER, 0, 62), false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            ModuleSetting.key(id(), "mark_key", "Mark key", DEFAULT_MARK_KEY),
            ModuleSetting.key(id(), "next_key", "Next key", DEFAULT_NEXT_KEY),
            ModuleSetting.key(id(), "remove_key", "Remove key", 0),
            ModuleSetting.key(id(), "list_key", "List key", DEFAULT_LIST_KEY),
            ModuleSetting.choice(id(), "mark_deaths", "Mark deaths", List.of("On", "Off"), 0)
        );
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        points.clear();
        points.addAll(Waypoint.decode(ModuleManager.get().config().getModuleString(id(), "points", "")));
        selected = 0;
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        String world = WorldInfo.worldId();
        String dim = WorldInfo.dimensionId();
        if (player == null || world == null || dim == null) {
            return;
        }
        // Before the screen check: dying opens the death screen on the same tick.
        boolean dead = player.getHealth() <= 0.0F;
        if (dead && !wasDead && ModuleManager.get().config().getModuleInt(id(), "mark_deaths", 0) == 0) {
            markDeath(player, world, dim);
        }
        wasDead = dead;
        if (minecraft.screen != null) {
            return;
        }
        boolean markDown = isDown(keyFor("mark_key", DEFAULT_MARK_KEY));
        boolean nextDown = isDown(keyFor("next_key", DEFAULT_NEXT_KEY));
        boolean removeDown = isDown(keyFor("remove_key", 0));
        boolean listDown = isDown(keyFor("list_key", DEFAULT_LIST_KEY));
        boolean mark = markDown && !markWasDown;
        boolean next = nextDown && !nextWasDown;
        boolean remove = removeDown && !removeWasDown;
        boolean list = listDown && !listWasDown;
        markWasDown = markDown;
        nextWasDown = nextDown;
        removeWasDown = removeDown;
        listWasDown = listDown;

        if (list) {
            minecraft.setScreen(new WaypointsScreen(this));
            return;
        }
        List<Waypoint> here = pointsIn(world, dim);
        if (mark) {
            Waypoint point = markHere();
            Notifications.push(
                point.name() + " set at " + point.x() + ", " + point.y() + ", " + point.z(),
                Notification.Type.SUCCESS
            );
        } else if (next && !here.isEmpty()) {
            selected = (selected + 1) % here.size();
            Notifications.push("Pointing to " + here.get(selected).name(), Notification.Type.INFO);
        } else if (remove && !here.isEmpty()) {
            Waypoint gone = here.get(Math.min(selected, here.size() - 1));
            points.remove(gone);
            selected = 0;
            save();
            Notifications.push(gone.name() + " removed", Notification.Type.INFO);
        }
    }

    @Override
    protected String hudText() {
        LocalPlayer player = Minecraft.getInstance().player;
        String world = WorldInfo.worldId();
        String dim = WorldInfo.dimensionId();
        if (player == null || world == null || dim == null) {
            return null;
        }
        List<Waypoint> here = pointsIn(world, dim);
        if (here.isEmpty()) {
            int key = keyFor("mark_key", DEFAULT_MARK_KEY);
            return key > 0 ? "No waypoints · " + Keybinds.keyName(key) + " to mark" : null;
        }
        Waypoint target = here.get(Math.min(selected, here.size() - 1));
        float turn = target.turnFrom(player.getX(), player.getZ(), player.getYRot());
        StringBuilder line = new StringBuilder()
            .append(Waypoint.arrowFor(turn)).append(' ')
            .append(target.name()).append("  ")
            .append(Waypoint.formatDistance(target.horizontalDistance(player.getX(), player.getZ())));
        int dy = target.y() - (int) Math.floor(player.getY());
        if (Math.abs(dy) >= SHOW_DY_FROM) {
            line.append("  ").append(dy > 0 ? "+" : "").append(dy).append('y');
        }
        return line.toString();
    }

    /** Saves your current position as a new waypoint and points the HUD at it. Null outside a world. */
    public Waypoint markHere() {
        LocalPlayer player = Minecraft.getInstance().player;
        String world = WorldInfo.worldId();
        String dim = WorldInfo.dimensionId();
        if (player == null || world == null || dim == null) {
            return null;
        }
        List<Waypoint> here = pointsIn(world, dim);
        Waypoint point = new Waypoint(
            nextFreeName(world),
            world,
            dim,
            (int) Math.floor(player.getX()),
            (int) Math.floor(player.getY()),
            (int) Math.floor(player.getZ())
        );
        points.add(point);
        selected = here.size();
        save();
        return point;
    }

    /** Every waypoint saved for the world you're in, across all its dimensions. */
    public List<Waypoint> pointsInCurrentWorld() {
        String world = WorldInfo.worldId();
        List<Waypoint> out = new ArrayList<>();
        for (Waypoint point : points) {
            if (point.world().equals(world)) {
                out.add(point);
            }
        }
        return out;
    }

    /** The waypoint the HUD is pointing at, or null. */
    public Waypoint target() {
        String world = WorldInfo.worldId();
        String dim = WorldInfo.dimensionId();
        if (world == null || dim == null) {
            return null;
        }
        List<Waypoint> here = pointsIn(world, dim);
        return here.isEmpty() ? null : here.get(Math.min(selected, here.size() - 1));
    }

    /** Points the HUD at {@code point}. Only works for one in your current dimension. */
    public boolean pointTo(Waypoint point) {
        String world = WorldInfo.worldId();
        String dim = WorldInfo.dimensionId();
        if (world == null || dim == null || !point.isIn(world, dim)) {
            return false;
        }
        int index = pointsIn(world, dim).indexOf(point);
        if (index >= 0) {
            selected = index;
        }
        return index >= 0;
    }

    /** Replaces {@code point}'s name, keeping its place in the list. Blank names are ignored. */
    public Waypoint rename(Waypoint point, String name) {
        String trimmed = name == null ? "" : name.trim();
        int index = points.indexOf(point);
        if (index < 0 || trimmed.isEmpty()) {
            return point;
        }
        Waypoint renamed = new Waypoint(trimmed, point.world(), point.dim(), point.x(), point.y(), point.z());
        points.set(index, renamed);
        save();
        return renamed;
    }

    public void remove(Waypoint point) {
        if (points.remove(point)) {
            selected = 0;
            save();
        }
    }

    /** "Waypoint N" with the lowest N not already used in this world. */
    private String nextFreeName(String world) {
        for (int n = 1; ; n++) {
            String candidate = "Waypoint " + n;
            boolean taken = false;
            for (Waypoint point : points) {
                if (point.world().equals(world) && point.name().equals(candidate)) {
                    taken = true;
                    break;
                }
            }
            if (!taken) {
                return candidate;
            }
        }
    }

    /** One "Death" waypoint per world: the latest replaces the last, so the list doesn't fill up with graves. */
    private void markDeath(LocalPlayer player, String world, String dim) {
        points.removeIf(point -> point.world().equals(world) && DEATH_NAME.equals(point.name()));
        points.add(new Waypoint(
            DEATH_NAME,
            world,
            dim,
            (int) Math.floor(player.getX()),
            (int) Math.floor(player.getY()),
            (int) Math.floor(player.getZ())
        ));
        selected = pointsIn(world, dim).size() - 1;
        save();
    }

    private List<Waypoint> pointsIn(String world, String dim) {
        List<Waypoint> here = new ArrayList<>();
        for (Waypoint point : points) {
            if (point.isIn(world, dim)) {
                here.add(point);
            }
        }
        return here;
    }

    private void save() {
        ModuleManager.get().config().setModuleString(id(), "points", Waypoint.encode(points));
    }

    private int keyFor(String setting, int fallback) {
        return ModuleManager.get().config().getModuleInt(id(), setting, fallback);
    }

    private static boolean isDown(int key) {
        return key > 0 && Keys.isDown(key);
    }
}
