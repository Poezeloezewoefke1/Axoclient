package dev.axoclient.gui.menu;

import dev.axoclient.core.AxoModule;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.menu.Ui.Type;
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
 * The Cosmetics page: a 4-column grid of items on little stands, and a
 * preview panel with your own player wearing them.
 *
 * Items come from the cosmetic modules. A module whose first setting is a
 * pick-one choice (Capes, Trails) contributes one item per option; the others
 * (Halo, Wings, Custom Cape) are one item each. Picking an item puts it on —
 * the preview is your real player, so what you see is what you wear. There is
 * no coin store: Axo has no server, so everything is free.
 */
final class CosmeticsPage implements MenuPage {
    private static final String[] CHIPS = {"ALL", "CAPES", "TRAILS", "EXTRAS"};
    private static final int PANEL_W = 270;
    private static final int CARD_H = 150;
    private static final int GAP = 10;

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

    private float yaw = -28f;
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
            String slot = switch (module.id()) {
                case "capes", "cape_custom" -> "CAPE";
                case "trails" -> "TRAIL";
                default -> module.displayName().toUpperCase(Locale.ROOT);
            };
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
        return all.size() + " owned · " + all.stream().filter(Item::worn).count() + " equipped";
    }

    @Override
    public String searchPlaceholder() {
        return "Search cosmetics";
    }

    @Override
    public void render(GuiGraphics g, Palette p, Hits hits, int x, int y, int w, int h, int mx, int my, String query) {
        int bottom = y + h - 22;
        int gridW = w - PANEL_W - 16;

        int cx = x;
        for (int i = 0; i < CHIPS.length; i++) {
            int cw = Ui.chip(g, p, CHIPS[i], cx, y, chip == i, 12);
            int index = i;
            hits.add(cx, y, cw, 30, () -> {
                chip = index;
                scroll = 0;
            });
            cx += cw + 8;
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

        int top = y + 30 + 14;
        int viewH = bottom - top;
        int innerW = gridW - 4;
        int cardW = (innerW - GAP * 3) / 4;
        g.enableScissor(x, top, x + gridW, bottom);
        hits.clip(x, top, gridW, viewH);
        for (int i = 0; i < shown.size(); i++) {
            int ix = x + (i % 4) * (cardW + GAP);
            int iy = top - scroll + (i / 4) * (CARD_H + GAP);
            card(g, p, hits, shown.get(i), ix, iy, cardW, mx, my);
        }
        if (shown.isEmpty()) {
            Ui.midCentered(g, Type.R13, "Nothing matches your search.", x + innerW / 2, top + 75, p.sub());
        }
        hits.unclip();
        g.disableScissor();
        int rows = (shown.size() + 3) / 4;
        int contentH = rows * (CARD_H + GAP) - GAP;
        maxScroll = Math.max(0, contentH - viewH);
        scroll = Math.min(scroll, maxScroll);
        ModulesPage.scrollbar(g, x + gridW, top, viewH, contentH, scroll);

        renderPanel(g, p, hits, x + gridW + 16, y, PANEL_W, bottom - y, mx);
    }

    private void card(GuiGraphics g, Palette p, Hits hits, Item item, int x, int y, int w, int mx, int my) {
        boolean isSelected = item.same(selected);
        boolean hover = Hits.over(mx, my, x, y, w, CARD_H);
        y += hover ? -2 : 0;
        int bg = isSelected ? blend(p.card(), p.accent(), 0.08f) : p.card();
        Ui.box(g, x, y, w, CARD_H, 12, bg, isSelected ? p.accent() : p.border(), 2);

        // Name strip: padding 7 10, 12px medium, raised, along the bottom.
        int stripH = 29;
        int stripY = y + CARD_H - 2 - stripH;
        Ui.round(g, x + 2, stripY, w - 4, stripH, 10, p.raised());
        Ui.rect(g, x + 2, stripY, w - 4, 11, p.raised());
        Ui.mid(g, Type.M12, Ui.trim(Type.M12, item.name(), w - 24), x + 2 + 10, stripY + stripH / 2, p.text());

        // The stand: 58x22, radius 4, raised with a 3px top edge; 10px above the strip.
        int standY = stripY - 10 - 22;
        int standX = x + w / 2 - 29;
        Ui.round(g, standX, standY, 58, 22, 4, p.raised());
        Ui.round(g, standX, standY, 58, 6, 3, p.border());
        Ui.rect(g, standX, standY + 3, 58, 3, p.raised());

        // The item itself, resting on the stand.
        if (item.module() instanceof CapesModule) {
            // The cape's front face: 10x16 pixels at (1,1) of its 64x32 texture, at 4x.
            g.blit(RenderPipelines.GUI_TEXTURED, CapesModule.texture(item.index()),
                x + w / 2 - 20, standY - 64 + 4, 1.0F, 1.0F, 40, 64, 10, 16, 64, 32);
        } else {
            MenuIcon icon = switch (item.module().id()) {
                case "trails" -> MenuIcon.M_ACTIVITY;
                case "halo" -> MenuIcon.M_SUN;
                default -> MenuIcon.M_SHIRT;
            };
            Ui.icon(g, icon, x + w / 2 - 24, standY - 50, 48, item.worn() ? p.accent() : p.sub());
        }

        if (item.worn()) {
            int bx = x + w - 8 - 18;
            int by = stripY - 8 - 18;
            Ui.round(g, bx, by, 18, 18, 9, p.accent());
            Ui.icon(g, MenuIcon.CHECK, bx + 4, by + 4, 11, p.onAccent());
        }

        hits.add(x, y, w, CARD_H, () -> {
            selected = item;
            item.wear();
        });
    }

    private void renderPanel(GuiGraphics g, Palette p, Hits hits, int x, int y, int w, int h, int mx) {
        Ui.box(g, x, y, w, h, 14, p.card(), p.border());
        int ix = x + 17;
        int iw = w - 34;
        Ui.spaced(g, Type.S10, "PREVIEW", ix, y + 17 + 7, p.muted(), 0.12f);
        long worn = items().stream().filter(Item::worn).count();
        String count = worn + " equipped";
        int dotW = 10 + 5;
        Ui.midRight(g, Type.S12, count, x + w - 17, y + 17 + 7, p.sub());
        Ui.round(g, x + w - 17 - Ui.width(Type.S12, count) - dotW, y + 17 + 2, 10, 10, 5, p.accent());

        // Bottom-up: action button, controls row, name block; the preview fills the rest.
        int actionY = y + h - 17 - 38;
        int controlsY = actionY - 12 - 32;
        int nameY = controlsY - 12 - 34;
        previewX = ix;
        previewY = y + 17 + 14 + 12;
        previewW = iw;
        previewH = nameY - 12 - previewY;
        Ui.round(g, previewX, previewY, previewW, previewH, 10, p.raised());
        Ui.text(g, Type.R10, "Drag to rotate · scroll to zoom", previewX + 10, previewY + 9, p.muted());

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            Ui.midCentered(g, Type.R13, "Join a world to preview", previewX + previewW / 2, previewY + previewH / 2, p.sub());
        } else {
            if (spin && !dragging) {
                yaw = (float) Math.sin(System.currentTimeMillis() / 1500.0) * 65f;
            }
            // The vanilla renderer turns the player toward a point; aim it to get our yaw.
            int centreX = previewX + previewW / 2;
            int centreY = previewY + previewH / 2;
            float lookX = centreX - (float) Math.tan(Math.toRadians(yaw)) * 60f;
            int scale = Math.round(previewH * 0.36f * zoom);
            g.enableScissor(previewX, previewY, previewX + previewW, previewY + previewH);
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                g, previewX, previewY + 24, previewX + previewW, previewY + previewH - 10,
                scale, 0.0625F, lookX, centreY - 20, player
            );
            g.disableScissor();
            // Name tag: padding 4 8, radius 6, green online dot.
            String name = Ui.trim(Type.S10, player.getName().getString(), previewW - 40);
            int tagW = Ui.width(Type.S10, name) + 16 + 6 + 6;
            int tagY = previewY + previewH - 10 - 20;
            Ui.round(g, previewX + 10, tagY, tagW, 20, 6, p.card());
            Ui.round(g, previewX + 18, tagY + 7, 6, 6, 3, 0xFF4FBF7A);
            Ui.mid(g, Type.S10, name, previewX + 30, tagY + 10, p.sub());
        }
        hits.add(previewX, previewY, previewW, previewH, () -> {
            dragging = true;
            spin = false;
            dragStartX = mx;
            dragStartYaw = yaw;
        });

        if (selected != null) {
            Ui.text(g, Type.S15, Ui.trim(Type.S15, selected.name(), iw), ix, nameY, p.text());
            Ui.spaced(g, Type.S10, selected.slot(), ix, nameY + 18 + 3 + 6, p.muted(), 0.12f);
        }

        // Controls row: Options (when the item has settings) on the left, Spin / Reset on the right.
        int[] edges = Ui.segmented(g, p, x + w - 17, controlsY, 32, List.of("Spin", "Reset"), spin ? 0 : -1,
            Type.S11, 10, 6);
        hits.add(edges[0], controlsY, edges[1] - edges[0], 32, () -> spin = !spin);
        hits.add(edges[1], controlsY, edges[2] - edges[1], 32, () -> {
            yaw = -28f;
            zoom = 1f;
            spin = false;
        });
        if (selected != null && !selected.module().settings().isEmpty()) {
            int ow = Ui.width(Type.S11, "Options") + 20;
            Ui.box(g, ix, controlsY, ow, 32, 8, p.card(), p.border());
            Ui.midCentered(g, Type.S11, "Options", ix + ow / 2, controlsY + 16, p.sub());
            AxoModule module = selected.module();
            hits.add(ix, controlsY, ow, 32, () -> openOptions.accept(module));
        }

        if (selected != null) {
            boolean wearing = selected.worn();
            Item item = selected;
            String label = wearing ? "TAKE OFF" : "EQUIP";
            Ui.box(g, ix, actionY, iw, 38, 9, wearing ? p.raised() : p.accent(), wearing ? p.border() : p.accent());
            Ui.spacedCentered(g, Type.B11, label, ix + iw / 2, actionY + 19, wearing ? p.text() : p.onAccent(), 0.16f);
            hits.add(ix, actionY, iw, 38, () -> {
                if (item.worn()) {
                    ModuleManager.get().setEnabled(item.module(), false);
                } else {
                    item.wear();
                }
            });
        }
    }

    private static int blend(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int gr = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    @Override
    public void drag(double mx, double my) {
        if (dragging) {
            yaw = Math.max(-75f, Math.min(75f, dragStartYaw - (float) (mx - dragStartX) * 0.6f));
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
        scroll = Math.max(0, Math.min(maxScroll, scroll + (int) Math.round(-amount * 40)));
        return true;
    }
}
