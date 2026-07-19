package dev.axoclient.mixin;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Raw write access to an OptionInstance value, bypassing its validator.
 * Needed for Fullbright: the gamma option clamps to [0,1], but the
 * lightmap honors larger raw values. Restore paths must always put a
 * legal value back (FullbrightModule does).
 */
@Mixin(OptionInstance.class)
public interface OptionInstanceAccessor<T> {
    @Accessor("value")
    void axoclient$setRawValue(T value);
}
