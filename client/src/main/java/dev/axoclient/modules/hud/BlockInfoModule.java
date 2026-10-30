package dev.axoclient.modules.hud;

import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Name and position of the block under your crosshair. Handy for telling
 * deepslate variants apart, reading coordinates off a far wall, or checking
 * what that block in a build actually is.
 */
public final class BlockInfoModule extends HudModule {

    public BlockInfoModule() {
        super("block_info", "Block Info", new HudPosition(HudAnchor.TOP_CENTER, 0, 90), false);
    }

    @Override
    protected String hudText() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        if (minecraft.level.getBlockState(pos).isAir()) {
            return null;
        }
        String name = minecraft.level.getBlockState(pos).getBlock().getName().getString();
        return name + "  " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
