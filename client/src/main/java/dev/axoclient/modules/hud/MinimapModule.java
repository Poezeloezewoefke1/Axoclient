package dev.axoclient.modules.hud;

import com.mojang.blaze3d.platform.NativeImage;
import dev.axoclient.core.AxoModule;
import dev.axoclient.core.HudRenderable;
import dev.axoclient.core.ModuleCategory;
import dev.axoclient.core.ModuleManager;
import dev.axoclient.core.ModuleSetting;
import dev.axoclient.gui.render.GuiRender;
import dev.axoclient.gui.theme.Themes;
import dev.axoclient.modules.qol.WaypointsModule;
import dev.axoclient.util.WorldInfo;
import dev.axoclient.waypoint.Waypoint;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * A north-up minimap in a screen corner, coloured like a vanilla map from the
 * top block of each column the client has loaded, with your position, facing
 * and waypoints marked.
 *
 * Scanning is spread over ticks — a few rows each — and a finished pass is
 * copied out before upload, so the map never tears and never costs a frame.
 * The image is one texture drawn with a single blit. Hidden in the Nether,
 * where the top block of every column is the bedrock roof.
 *
 * Config (module "minimap"):
 *   corner — which screen corner
 *   zoom   — blocks across: 64, 128 or 256
 *   size   — on-screen size in GUI pixels
 */
public final class MinimapModule extends AxoModule implements HudRenderable {
    private static final int RES = 64;
    private static final int ROWS_PER_TICK = 8;
    private static final int MARGIN = 4;
    private static final int UNLOADED = 0xFF101418;
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("axoclient", "minimap");
    private static final List<String> CORNERS = List.of("Bottom right", "Top right", "Top left", "Bottom left");
    private static final List<String> ZOOMS = List.of("64 blocks", "128 blocks", "256 blocks");

    private final int[] scan = new int[RES * RES];
    private final int[] ready = new int[RES * RES];
    private int scanRow;
    private int scanOriginX;
    private int scanOriginZ;
    private int scanStep = 1;
    private int shownOriginX;
    private int shownOriginZ;
    private int shownStep = 1;
    private boolean pendingUpload;
    private boolean hasImage;
    private DynamicTexture texture;

    public MinimapModule() {
        super("minimap", "Minimap", ModuleCategory.HUD, false);
    }

    @Override
    public List<ModuleSetting> settings() {
        return List.of(
            ModuleSetting.choice(id(), "corner", "Corner", CORNERS, 0),
            ModuleSetting.choice(id(), "zoom", "Zoom", ZOOMS, 0),
            new ModuleSetting(id(), "size", "Size", 48, 160, 8, 80, ModuleSetting.Format.PLAIN)
        );
    }

    @Override
    protected void onEnable() {
        scanRow = 0;
        hasImage = false;
        pendingUpload = false;
    }

    @Override
    protected void onDisable() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
            texture = null;
        }
        hasImage = false;
    }

    @Override
    public void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || WorldInfo.NETHER.equals(WorldInfo.dimensionId())) {
            scanRow = 0;
            hasImage = false;
            return;
        }
        if (scanRow == 0) {
            scanStep = 1 << settings().get(1).get();
            scanOriginX = (int) Math.floor(player.getX()) - RES / 2 * scanStep;
            scanOriginZ = (int) Math.floor(player.getZ()) - RES / 2 * scanStep;
        }
        int end = Math.min(RES, scanRow + ROWS_PER_TICK);
        for (int row = scanRow; row < end; row++) {
            int z = scanOriginZ + row * scanStep;
            for (int col = 0; col < RES; col++) {
                scan[row * RES + col] = colourAt(level, scanOriginX + col * scanStep, z, scanStep);
            }
        }
        scanRow = end;
        if (scanRow >= RES) {
            System.arraycopy(scan, 0, ready, 0, scan.length);
            shownOriginX = scanOriginX;
            shownOriginZ = scanOriginZ;
            shownStep = scanStep;
            pendingUpload = true;
            scanRow = 0;
        }
    }

    /** Map colour of the column's top block, shaded by the slope to the north like a vanilla map. */
    private static int colourAt(ClientLevel level, int x, int z, int step) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return UNLOADED;
        }
        int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        BlockPos top = new BlockPos(x, height - 1, z);
        int rgb = level.getBlockState(top).getMapColor(level, top).col;
        if (rgb == 0) {
            return UNLOADED;
        }
        int north = level.hasChunk(x >> 4, (z - step) >> 4)
            ? level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z - step)
            : height;
        float shade = height > north ? 1.12F : height < north ? 0.80F : 1.0F;
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * shade));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * shade));
        int b = Math.min(255, Math.round((rgb & 0xFF) * shade));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public void renderHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        if (pendingUpload) {
            upload(minecraft);
        }
        if (!hasImage) {
            return;
        }
        int size = settings().get(2).get();
        int corner = settings().get(0).get();
        boolean right = corner == 0 || corner == 1;
        boolean bottom = corner == 0 || corner == 3;
        int x = right ? graphics.guiWidth() - size - MARGIN : MARGIN;
        int y = bottom ? graphics.guiHeight() - size - MARGIN : MARGIN;
        int accent = Themes.current().accent;

        GuiRender.shadow(graphics, x, y, size, size);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, x, y, 0.0F, 0.0F, size, size, RES, RES, RES, RES);
        GuiRender.outline(graphics, x - 1, y - 1, size + 2, size + 2, accent);
        graphics.drawString(minecraft.font, "N", x + size / 2 - minecraft.font.width("N") / 2, y + 2, 0xFFFFFFFF, true);

        drawWaypoints(graphics, x, y, size);

        // Player: a dot plus a short heading tick. Yaw 0 faces south (+Z), which is down on a north-up map.
        float pixelsPerBlock = size / (float) (RES * shownStep);
        int px = x + Math.round((float) (player.getX() - shownOriginX) * pixelsPerBlock);
        int pz = y + Math.round((float) (player.getZ() - shownOriginZ) * pixelsPerBlock);
        double yaw = Math.toRadians(player.getYRot());
        double dx = -Math.sin(yaw);
        double dz = Math.cos(yaw);
        for (int i = 2; i <= 5; i++) {
            int hx = px + (int) Math.round(dx * i);
            int hz = pz + (int) Math.round(dz * i);
            graphics.fill(hx, hz, hx + 1, hz + 1, 0xFFFFFFFF);
        }
        graphics.fill(px - 1, pz - 1, px + 2, pz + 2, 0xFF000000);
        graphics.fill(px, pz, px + 1, pz + 1, 0xFFFFFFFF);
    }

    private void drawWaypoints(GuiGraphics graphics, int x, int y, int size) {
        if (!(ModuleManager.get().byId("waypoints").orElse(null) instanceof WaypointsModule waypoints)
            || !ModuleManager.get().isEnabled(waypoints)) {
            return;
        }
        String dim = WorldInfo.dimensionId();
        Waypoint target = waypoints.target();
        float pixelsPerBlock = size / (float) (RES * shownStep);
        for (Waypoint point : waypoints.pointsInCurrentWorld()) {
            if (!point.dim().equals(dim)) {
                continue;
            }
            // Off-map waypoints sit on the edge, so you can still see which way they are.
            int wx = x + Math.max(1, Math.min(size - 2, Math.round((point.x() + 0.5F - shownOriginX) * pixelsPerBlock)));
            int wz = y + Math.max(1, Math.min(size - 2, Math.round((point.z() + 0.5F - shownOriginZ) * pixelsPerBlock)));
            int colour = point.equals(target) ? Themes.current().accent : 0xFFFBBF24;
            graphics.fill(wx - 1, wz - 1, wx + 2, wz + 2, 0xFF000000);
            graphics.fill(wx, wz, wx + 1, wz + 1, colour);
        }
    }

    /** Copies the finished scan into the texture, creating it on first use (render thread only). */
    private void upload(Minecraft minecraft) {
        pendingUpload = false;
        if (texture == null) {
            texture = new DynamicTexture(TEXTURE_ID::toString, new NativeImage(RES, RES, false));
            minecraft.getTextureManager().register(TEXTURE_ID, texture);
        }
        NativeImage pixels = texture.getPixels();
        if (pixels == null) {
            return;
        }
        for (int i = 0; i < ready.length; i++) {
            pixels.setPixel(i % RES, i / RES, ready[i]);
        }
        texture.upload();
        hasImage = true;
    }
}
