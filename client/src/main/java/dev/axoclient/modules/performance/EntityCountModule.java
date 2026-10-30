package dev.axoclient.modules.performance;

import dev.axoclient.core.ModuleCategory;
import dev.axoclient.hud.HudAnchor;
import dev.axoclient.hud.HudModule;
import dev.axoclient.hud.HudPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * How many entities your client currently has loaded — the same figure F3
 * shows. A mob farm or a pile of dropped items is the usual reason a base
 * suddenly runs at half the frame rate, and this makes that visible.
 */
public final class EntityCountModule extends HudModule {

    public EntityCountModule() {
        super("entity_count", "Entity Count", ModuleCategory.PERFORMANCE, new HudPosition(HudAnchor.TOP_RIGHT, -4, 136), false);
    }

    @Override
    protected String hudText() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? null : level.getEntityCount() + " entities";
    }
}
