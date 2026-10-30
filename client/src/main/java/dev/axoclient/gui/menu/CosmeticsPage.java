package dev.axoclient.gui.menu;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.render.ModuleIcons;
import dev.axoclient.gui.theme.Palette;
import dev.axoclient.modules.cosmetic.CapesModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * The Cosmetics page: a grid of everything you can wear, and your own player
 * on the right wearing it.
 *
 * Items come from the cosmetic modules themselves. A module whose first
 * setting is a pick-one choice (Capes, Trails) contributes one item per
 * option; the others (Halo, Wings, Custom Cape) are one item each. Picking an
 * item puts it on straight away — the preview is your real player, so what
 * you see is what you're wearing. Everything is free: Axo has no store and no
 * server, so there is nothing to buy.
 */
final class CosmeticsPage implements MenuPage {
    private static final String[] CHIPS = {"All", "Capes", "Trails", "Extras"};
    private static final int CARD_MIN_W = 76;
    private static final int CARD_H = 82;
    private static final int GAP = 7;

    private record Item(AxoModule module, ModuleSetting variant, int index, String name, String slot) {
        boolean worn() {
            return ModuleManager.get().isEnabled(module) && (variant == null || variant.get() == index);
        }

        /** Same module and variant; settings objects are rebuilt per call, so equals() won't do. */
        boolean same(Item other) {
            return other != null && other.module == module && other.index == index;
        }

        void wear() {
            if (variant != null) {
                variant.set(index);
            }
            if (!ModuleManager.get().isEnabled(module)) {
                ModuleManager.get().setEnabled(module, true);
            }
        }
    }

    private final Consumer<AxoModule> openOptions;
    private int chip;
    private int scroll;
    private int maxScroll;
    private Item selected;

    // Player preview state.
    private float yaw = -25f;
    private float zoom = 1f;
    private boolean spin;
    private boolean dragging;
    private double dragStartX;
    private float dragStartYaw;
    private int previewX;
    private int previewY;
    private int previewW;
    private int previewH;

    CosmeticsPage(Consumer<AxoModule> openOptions) {
        this.openOptions = openOptions;
    }

    private static List<Item> items() {
        List<Item> out = new ArrayList<>();
        for (AxoModule module : ModuleManager.get().all()) {
            if (module.category() != ModuleCategory.COSMETIC) {
                continue;
            }
            ModuleSetting variant = null;
            for (ModuleSetting setting : module.settings()) {
                if (setting.format() == ModuleSetting.Format.CHOICE && !setting.isSwitch()) {
                    variant = setting;
                    break;
                }
            }
            String slot = slotOf(module).toUpperCase(Locale.ROOT);
            if (variant == null) {
                out.add(new Item(module, null, 0, module.displayName(), slot));
                continue;
            }
            List<String> names = variant.choices();
            for (int i = 0; i < names.size(); i++) {
                String name = module.id().equals("capes") ? names.get(i) + " Cape" : names.get(i);
                out.add(new Item(module, variant, i, name, slot));
            }
        }
        return out;
    }

    private static String slotOf(AxoModule module) {
        return switch (module.id()) {
            case "capes", "cape_custom" -> "Cape";
            case "trails" -> "Trail";
            default -> module.displayName();
        };
    }

    private boolean matchesChip(Item item) {
        return switch (chip) {
            case 1 -> item.slot().equals("CAPE");
            case 2 -> item.slot().equals("TRAIL");
            case 3 -> !item.slot().equals("CAPE") && !item.slot().equals("TRAIL");
            default -> true;
        };
    }

    @Override
    public String navLabel() {
        return "Cosmetics";
    }

    @Override
    public String navCount() {
        return Integer.toString(items().size());
    }

    @Override
    public String titleAccent() {
        return "Your";
    }

    @Override
    public String titleRest() {
        return "Cosmetics";
    }

    @Override
    public String subtitle() {
        List<Item> all = items();
        long worn = all.stream().filter(Item::worn).count();
        return all.size() + " cosmetics · " + worn + " equipped · only you can see them";
    }

    @Override
    public String searchPlaceholder() {
        return "Search cosmetics";
    }

    @Override
    public void render(GuiGraphics g, Palette p, Hits hits, int x, int y, int w, int h, int mx, int my, String query) {
        int panelW = Math.max(130, Math.min(170, w * 3 / 10));
        int gridW = w - panelW - 10;

        int cx = x;
        for (int i = 0; i < CHIPS.length; i++) {
            int cw = Draw.chip(g, CHIPS[i], cx, y, 16, chip == i, p);
            int index = i;
            hits.add(cx, y, cw, 16, () -> {
                chip = index;
                scroll = 0;
            });
            cx += cw + 4;
        }

        String needle = query.toLowerCase(Locale.ROOT).trim();
        List<Item> shown = new ArrayList<>();
        for (Item item : items()) {
            if (matchesChip(item) && (needle.isEmpty() || item.name().toLowerCase(Locale.ROOT).contains(needle))) {
                shown.add(item);
            }
        }
        if (selected == null) {
            selected = items().stream().filter(Item::worn).findFirst().orElse(shown.isEmpty() ? null : shown.get(0));
        }

        int top = y + 24;
        int viewH = h - 24;
        int cols = Math.max(2, Math.min(5, (gridW + GAP) / (CARD_MIN_W + GAP)));
        int cardW = (gridW - GAP * (cols - 1)) / cols;
        g.enableScissor(x, top, x + gridW, top + viewH);
        hits.clip(x, top, gridW, viewH);
        for (int i = 0; i < shown.size(); i++) {
            int ix = x + (i % cols) * (cardW + GAP);
            int iy = top - scroll + (i / cols) * (CARD_H + GAP);
            card(g, p, hits, shown.get(i), ix, iy, cardW, mx, my);
        }
        if (shown.isEmpty()) {
            Draw.centered(g, "Nothing matches your search.", x + gridW / 2, top + 30, p.sub());
        }
        hits.unclip();
        g.disableScissor();
        int rows = (shown.size() + cols - 1) / cols;
        maxScroll = Math.max(0, rows * (CARD_H + GAP) - GAP - viewH);
        scroll = Math.min(scroll, maxScroll);

        renderPanel(g, p, hits, x + gridW + 10, y, panelW, h, mx, my);
    }

    private void card(GuiGraphics g, Palette p, Hits hits, Item item, int x, int y, int w, int mx, int my) {
        boolean isSelected = item.same(selected);
        boolean hover = Hits.over(mx, my, x, y, w, CARD_H);
        Draw.card(g, x, y, w, CARD_H, 7, p.card(), isSelected ? p.accent() : (hover ? p.line() : p.border()));
        if (isSelected) {
            Draw.card(g, x + 1, y + 1, w - 2, CARD_H - 2, 6, p.card(), p.accent());
        }

        // Thumbnail on a little stand.
        int standY = y + CARD_H - 26;
        Draw.round(g, x + w / 2 - 16, standY, 32, 7, 2, p.raised());
        g.fill(x + w / 2 - 16, standY, x + w / 2 + 16, standY + 2, p.border());
        if (item.module() instanceof CapesModule) {
            // The real texture: the cape's front face, 10x16 pixels at (1,1) of a 64x32 sheet.
            g.blit(RenderPipelines.GUI_TEXTURED, CapesModule.texture(item.index()),
                x + w / 2 - 10, standY - 34, 1.0F, 1.0F, 20, 32, 10, 16, 64, 32);
        } else {
            int tint = item.worn() ? p.accent() : p.sub();
            ModuleIcons.draw(g, ModuleCategory.COSMETIC, x, standY - 34, w, 32, tint);
        }

        if (item.worn()) {
            int bx = x + w - 15;
            int by = y + 6;
            Draw.round(g, bx, by, 10, 10, 5, p.accent());
            // A small tick.
            g.fill(bx + 2, by + 5, bx + 3, by + 6, p.onAccent());
            g.fill(bx + 3, by + 6, bx + 4, by + 7, p.onAccent());
            g.fill(bx + 4, by + 5, bx + 5, by + 6, p.onAccent());
            g.fill(bx + 5, by + 4, bx + 6, by + 5, p.onAccent());
            g.fill(bx + 6, by + 3, bx + 7, by + 4, p.onAccent());
        }

        // Name strip along the bottom.
        int nameY = y + CARD_H - 15;
        Draw.round(g, x + 1, nameY, w - 2, 14, 6, p.raised());
        g.fill(x + 1, nameY, x + w - 1, nameY + 6, p.raised());
        Draw.text(g, Draw.trim(item.name(), w - 10), x + 5, nameY + 3, p.text());

        hits.add(x, y, w, CARD_H, () -> {
            selected = item;
            item.wear();
        });
    }

    private void renderPanel(GuiGraphics g, Palette p, Hits hits, int x, int y, int w, int h, int mx, int my) {
        Draw.card(g, x, y, w, h, 8, p.card(), p.border());
        Draw.label(g, "Preview", x + 9, y + 9, p.muted());

        previewX = x + 8;
        previewY = y + 21;
        previewW = w - 16;
        previewH = Math.max(60, h - 21 - 78);
        Draw.round(g, previewX, previewY, previewW, previewH, 6, p.raised());
        Draw.text(g, Draw.trim("Drag to rotate · scroll to zoom", previewW - 8), previewX + 4, previewY + 4, p.muted());

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            Draw.centered(g, "Join a world", previewX + previewW / 2, previewY + previewH / 2 - 4, p.sub());
        } else {
            if (spin && !dragging) {
                yaw = (float) Math.sin(System.currentTimeMillis() / 1400.0) * 60f;
            }
            // The vanilla renderer turns the player toward a point; aim that point
            // to get the yaw we want. It caps the turn, so this covers about ±70°.
            int centreX = previewX + previewW / 2;
            int centreY = previewY + previewH / 2;
            float lookX = centreX - (float) Math.tan(Math.toRadians(yaw)) * 40f;
            int scale = Math.round(Math.min(previewW, previewH) * 0.42f * zoom);
            g.enableScissor(previewX, previewY, previewX + previewW, previewY + previewH);
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                g, previewX, previewY + 12, previewX + previewW, previewY + previewH - 4,
                scale, 0.0625F, lookX, centreY - 10, player
            );
            g.disableScissor();
            String name = Draw.trim(player.getName().getString(), previewW - 24);
            int tagW = Draw.width(name) + 14;
            Draw.round(g, previewX + 4, previewY + previewH - 15, tagW, 11, 3, p.card());
            Draw.round(g, previewX + 7, previewY + previewH - 12, 4, 4, 2, 0xFF4FBF7A);
            Draw.text(g, name, previewX + 13, previewY + previewH - 13, p.sub());
        }
        hits.add(previewX, previewY, previewW, previewH, () -> {
            dragging = true;
            spin = false;
            dragStartX = mx;
            dragStartYaw = yaw;
        });

        int iy = previewY + previewH + 6;
        if (selected != null) {
            Draw.text(g, Draw.trim(selected.name(), w - 16), x + 9, iy, p.text());
            Draw.label(g, selected.slot(), x + 9, iy + 11, p.muted());
        }

        // Spin / Reset, plus a link to the item's module options.
        int by = iy + 24;
        int[] edges = Draw.segmented(g, x + 8, by, 15, List.of("Spin", "Reset"), spin ? 0 : -1, p, 5);
        hits.add(edges[0], by, edges[1] - edges[0], 15, () -> spin = !spin);
        hits.add(edges[1], by, edges[2] - edges[1], 15, () -> {
            yaw = -25f;
            zoom = 1f;
            spin = false;
        });
        if (selected != null && !selected.module().settings().isEmpty()) {
            String options = "Options";
            int ow = Draw.width(options) + 12;
            Draw.card(g, x + w - 8 - ow, by, ow, 15, 4, p.raised(), p.border());
            Draw.centered(g, options, x + w - 8 - ow / 2, by + 4, p.sub());
            AxoModule module = selected.module();
            hits.add(x + w - 8 - ow, by, ow, 15, () -> openOptions.accept(module));
        }

        // Equip / unequip.
        int ay = by + 21;
        if (selected != null) {
            boolean worn = selected.worn();
            Item item = selected;
            Draw.card(g, x + 8, ay, w - 16, 18, 5, worn ? p.raised() : p.accent(), worn ? p.border() : p.accent());
            Draw.boldCentered(g, worn ? "UNEQUIP" : "EQUIP", x + w / 2, ay + 5, worn ? p.sub() : p.onAccent());
            hits.add(x + 8, ay, w - 16, 18, () -> {
                if (item.worn()) {
                    ModuleManager.get().setEnabled(item.module(), false);
                } else {
                    item.wear();
                }
            });
        }
    }

    @Override
    public void drag(double mx, double my) {
        if (dragging) {
            yaw = Math.max(-70f, Math.min(70f, dragStartYaw - (float) (mx - dragStartX) * 0.8f));
        }
    }

    @Override
    public void release() {
        dragging = false;
    }

    @Override
    public boolean scroll(double mx, double my, double amount) {
        if (Hits.over(mx, my, previewX, previewY, previewW, previewH)) {
            zoom = Math.max(0.6f, Math.min(1.5f, zoom * (amount > 0 ? 1.08f : 0.92f)));
            return true;
        }
        scroll = Math.max(0, Math.min(maxScroll, scroll + (int) Math.round(-amount * 24)));
        return true;
    }
}
