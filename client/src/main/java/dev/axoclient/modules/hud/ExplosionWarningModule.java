package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import dev.axoclient.util.WorldInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Warns before the two classic self-detonations: using a bed in the Nether or
 * the End, and using a respawn anchor anywhere but the Nether. Shows while
 * you're holding a bed or looking at either block in the wrong dimension —
 * the moment before the right-click that costs a hardcore world.
 */
public final class ExplosionWarningModule extends HudModule {

    public ExplosionWarningModule() {
        super("explosion_warning", "Explosion Warning", new HudPosition(HudAnchor.CENTER, 0, -38), false);
    }

    @Override
    protected String hudText() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        String dim = WorldInfo.dimensionId();
        if (player == null || minecraft.level == null || dim == null) {
            return null;
        }
        boolean nether = WorldInfo.NETHER.equals(dim);
        boolean bedsExplode = nether || WorldInfo.END.equals(dim);

        Block looking = null;
        if (minecraft.hitResult instanceof BlockHitResult hit) {
            looking = minecraft.level.getBlockState(hit.getBlockPos()).getBlock();
        }
        if (bedsExplode && (looking instanceof BedBlock || player.getMainHandItem().getItem() instanceof BedItem)) {
            return "Beds explode in " + (nether ? "the Nether" : "the End") + "!";
        }
        if (!nether && looking instanceof RespawnAnchorBlock) {
            return "Respawn anchors explode outside the Nether!";
        }
        return null;
    }
}
