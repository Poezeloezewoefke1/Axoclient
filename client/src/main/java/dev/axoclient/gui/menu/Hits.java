package dev.axoclient.gui.menu;

import java.util.ArrayList;
import java.util.List;

/**
 * Clickable areas, recorded while a page draws and checked when the mouse is
 * pressed. Because the list is rebuilt every frame from the same code that
 * draws, what you can click is always exactly what you can see.
 *
 * Inside a scrolling list, {@link #clip} limits new areas to the visible
 * viewport so a row scrolled out of sight can't be clicked through the edge.
 */
public final class Hits {
    private record Area(int x, int y, int w, int h, Runnable action) {}

    private final List<Area> areas = new ArrayList<>();
    private int clipX;
    private int clipY;
    private int clipW = Integer.MAX_VALUE;
    private int clipH = Integer.MAX_VALUE;
    private boolean clipped;

    public void clear() {
        areas.clear();
        unclip();
    }

    public void clip(int x, int y, int w, int h) {
        clipX = x;
        clipY = y;
        clipW = w;
        clipH = h;
        clipped = true;
    }

    public void unclip() {
        clipped = false;
    }

    public void add(int x, int y, int w, int h, Runnable action) {
        if (clipped) {
            int x1 = Math.max(x, clipX);
            int y1 = Math.max(y, clipY);
            int x2 = Math.min(x + w, clipX + clipW);
            int y2 = Math.min(y + h, clipY + clipH);
            if (x2 <= x1 || y2 <= y1) {
                return;
            }
            areas.add(new Area(x1, y1, x2 - x1, y2 - y1, action));
            return;
        }
        areas.add(new Area(x, y, w, h, action));
    }

    /** Runs the topmost (last drawn) area under the point. True if one was hit. */
    public boolean click(double mx, double my) {
        for (int i = areas.size() - 1; i >= 0; i--) {
            Area a = areas.get(i);
            if (mx >= a.x && mx < a.x + a.w && my >= a.y && my < a.y + a.h) {
                a.action.run();
                return true;
            }
        }
        return false;
    }

    public static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
